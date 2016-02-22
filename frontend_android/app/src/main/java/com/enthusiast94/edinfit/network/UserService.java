package com.enthusiast94.edinfit.network;

import android.content.Context;
import android.util.Base64;

import com.enthusiast94.edinfit.models.User;
import com.enthusiast94.edinfit.utils.PreferencesManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by manas on 26-09-2015.
 */
public class UserService {

    public static final String TAG = UserService.class.getSimpleName();
    private final String USER_ID_KEY = "userIdKey";
    private static UserService instance;
    private Context context;
    private BaseService baseService;
    private PreferencesManager preferencesManager;

    private UserService(Context context) {
        this.context = context;
        baseService = BaseService.getInstance();
        preferencesManager = PreferencesManager.getInstance();
    }

    public static void init(Context context) {
        if (instance != null) {
            throw new IllegalStateException(TAG + " has already been initialized.");
        }

        instance = new UserService(context);
    }

    public static UserService getInstance() {
        if (instance == null) {
            throw new IllegalStateException(TAG + " has not been initialized yet.");
        }

        return instance;
    }

    /**
     * POST api/authenticate
     */

    public BaseService.Response<User> authenticate(String email, final String password) {
        FormBody formBody = new FormBody.Builder()
                .add("email", email)
                .add("password", password)
                .build();

        return createOrAuthenticateUser("/authenticate", formBody);
    }

    /**
     * POST api/authenticate/google
     */

    public BaseService.Response<User> authenticateViaGoogle(String idToken) {
        FormBody formBody = new FormBody.Builder()
                .add("id_token", idToken)
                .build();

        return createOrAuthenticateUser("authenticate/google", formBody);
    }

    /**
     * POST api/authenticate/facebook
     */

    public BaseService.Response<User> authentivateViaFacebook(String accessToken) {
        FormBody formBody = new FormBody.Builder()
                .add("access_token", accessToken)
                .build();

        return createOrAuthenticateUser("authenticate/facebook", formBody);
    }

    /**
     * POST api/users
     */

    public BaseService.Response<User> createUser(String name, String email, String password) {
        FormBody formBody = new FormBody.Builder()
                .add("name", name)
                .add("email", email)
                .add("password", password)
                .build();

        return createOrAuthenticateUser("users", formBody);
    }

    /**
     * Common method for creating/authenticating users.
     */

    private BaseService.Response<User> createOrAuthenticateUser(String path, RequestBody requestBody) {
        Request request = baseService.createEdinfitPostRequest(path, requestBody);
        BaseService.Response<User> response = new BaseService.Response<>();

        try {
            Response okHttpResponse = baseService.getHttpClient().newCall(request).execute();

            if (!okHttpResponse.isSuccessful()) {
                response.setError(baseService.extractEdinfitErrorMessage(okHttpResponse));
                return response;
            }

            // Decode JWT token's payload in order to retrieve user id as json
            String token = new JSONObject(okHttpResponse.body().string()).getJSONObject("data").getString("token");
            String decodedPayload = new String(Base64.decode(token.split("\\.")[1].getBytes(),
                    Base64.DEFAULT));

            // parse the decoded payload and construct new user object
            JSONObject userJson = new JSONObject(decodedPayload);
            User user = new User(
                    userJson.getString("id"),
                    null,
                    null,
                    0,
                    token,
                    0
            );
            user.save();

            // persist currently authenticated user's id
            preferencesManager.setCurrentlyAuthenticatedUserId(user.getId().toString());

            // now update the cached user with other user details by sending another request
            BaseService.Response<Void> updateUserResponse = updateCachedUser();
            if (!updateUserResponse.isSuccessfull()) {
                response.setError(updateUserResponse.getError());
                return response;
            }

            response.setBody(getAuthenticatedUser());
            return response;

        } catch (IOException | JSONException e) {
            e.printStackTrace();
            response.setError(e.getMessage());
            return response;
        }

    }

    public void deauthenticate() {
        preferencesManager.clearPrefs();
    }

    public User getAuthenticatedUser() {
        String userId = preferencesManager.getCurrentlyAuthenticatedUserId();
        if (userId == null) {
            return null;
        }

        return User.findById(Long.parseLong(userId));
    }

    public BaseService.Response<Void> updateUser(User user) {
        FormBody formBody = new FormBody.Builder()
                .add("name", user.getName())
                .add("weight", String.valueOf(user.getWeight()))
                .build();

        Request request = baseService.createEdinFitPutRequest("users/" + user.get_id(), formBody);
        BaseService.Response<Void> response = new BaseService.Response<>();

        try {
            Response okHttpResponse = baseService.getHttpClient().newCall(request).execute();

            if (!response.isSuccessfull()) {
                response.setError(baseService.extractEdinfitErrorMessage(okHttpResponse));
                return response;
            }

            BaseService.Response<Void> updateResponse = updateCachedUser();
            if (!updateResponse.isSuccessfull()) {
                response.setError(updateResponse.getError());
                return response;
            }

            return response;
        } catch (IOException e) {
            e.printStackTrace();
            response.setError(e.getMessage());
            return response;
        }
    }

    public BaseService.Response<Void> updateCachedUser() {
        final User user = getAuthenticatedUser();

        Request request = baseService.createEdinfitGetRequest("users/" + user.get_id());
        BaseService.Response<Void> response = new BaseService.Response<>();

        try {
            Response okHttpResponse = baseService.getHttpClient().newCall(request).execute();

            if (!response.isSuccessfull()) {
                response.setError(baseService.extractEdinfitErrorMessage(okHttpResponse));
                return response;
            }

            User userToUpdate = User.findById(Long.parseLong(
                    preferencesManager.getCurrentlyAuthenticatedUserId()));

            JSONObject userJson = new JSONObject(okHttpResponse.body().string()).getJSONObject("data");
            userToUpdate.setName(userJson.getString("name"));
            userToUpdate.setEmail(userJson.getString("email"));
            userToUpdate.setCreatedAt(userJson.getLong("createdAt"));
            userToUpdate.setWeight(userJson.has("weight") ? userJson.getInt("weight") : 0);
            userToUpdate.save();

            return response;

        } catch (IOException | JSONException e) {
            response.setError(e.getMessage());
            return response;
        }
    }

    public boolean isUserAuthenticated() {
        return preferencesManager.getCurrentlyAuthenticatedUserId() != null;
    }
}
