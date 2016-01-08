package com.enthusiast94.edinfit.network;

import android.content.Context;
import android.util.Base64;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models_2.User;
import com.enthusiast94.edinfit.utils.Helpers;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import cz.msebera.android.httpclient.Header;

/**
 * Created by manas on 26-09-2015.
 */
public class UserService extends BaseService {

    public static final String TAG = UserService.class.getSimpleName();
    private final String USER_ID_KEY = "userIdKey";
    private static UserService instance;
    private String parsingErrorMessage;
    private Context context;

    private UserService(Context context) {
        this.context = context;

        this.parsingErrorMessage = context.getString(R.string.error_parsing);
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

    public void authenticate(String email, final String password, final Callback<User> callback) {
        Map<String, String> userDetails = new HashMap<>();
        userDetails.put("email", email);
        userDetails.put("password", password);

        createOrAuthenticateUser(API_BASE + "/authenticate", userDetails, callback);
    }

    /**
     * POST api/authenticate/google
     */

    public void authenticateViaGoogle(String idToken, final Callback<User> callback) {
        Map<String, String> postData = new HashMap<>();
        postData.put("id_token", idToken);

        createOrAuthenticateUser(API_BASE + "/authenticate/google", postData, callback);
    }

    /**
     * POST api/authenticate/facebook
     */

    public void authentivateViaFacebook(String accessToken, Callback<User> callback) {
        Map<String, String> postDate = new HashMap<>();
        postDate.put("access_token", accessToken);

        createOrAuthenticateUser(API_BASE + "/authenticate/facebook", postDate, callback);
    }

    /**
     * POST api/users
     */

    public void createUser(String name, String email, String password, Callback<User> callback) {
        Map<String, String> userDetails = new HashMap<>();
        userDetails.put("name", name);
        userDetails.put("email", email);
        userDetails.put("password", password);

        createOrAuthenticateUser(API_BASE + "/users", userDetails, callback);
    }

    /**
     * Common method for creating/authenticating users.
     */

    private void createOrAuthenticateUser(String url, Map<String, String> userDetails,
                                          final Callback<User> callback) {
        RequestParams requestParams = new RequestParams(userDetails);

        AsyncHttpClient client = getAsyncHttpClient(false);
        client.post(url, requestParams, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    // Decode JWT token's payload in order to retrieve user id as json
                    String token = response.getJSONObject("data").getString("token");
                    String decodedPayload = new String(Base64.decode(token.split("\\.")[1].getBytes(),
                            Base64.DEFAULT));

                    // parse the decoded payload and construct new user object
                    JSONObject userJson = new JSONObject(decodedPayload);
                    User user = new User(
                            userJson.getString("id"),
                            null,
                            null,
                            0,
                            token
                    );
                    user.save();

                    // persist currently authenticated user's id
                    Helpers.writeToPrefs(context, USER_ID_KEY, user.getId().toString());

                    // now update the cached user with other user details by sending another request
                    updateCachedUser(new Callback<Void>() {

                        @Override
                        public void onSuccess(Void data) {
                            if (callback != null) callback.onSuccess(getAuthenticatedUser());
                        }

                        @Override
                        public void onFailure(String message) {
                            if (callback != null)
                                callback.onFailure(message);
                        }
                    });
                } catch (JSONException e) {
                    if (callback != null)
                        callback.onFailure(parsingErrorMessage);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable,
                                  JSONObject errorResponse) {
                onFailureCommon(statusCode, errorResponse, callback);
            }
        });
    }

    public void deauthenticate() {
        Helpers.clearPrefs(context);
    }

    public User getAuthenticatedUser() {
        return User.findById(Long.parseLong(Helpers.readFromPrefs(context, USER_ID_KEY)));
    }

    public void updateCachedUser(final Callback<Void> callback) {
        final User user = getAuthenticatedUser();

        AsyncHttpClient client = getAsyncHttpClient(true);
        client.get(API_BASE + "/users/" + user.get_id(), new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    User userToUpdate = User.findById(Long.parseLong(
                            Helpers.readFromPrefs(context, USER_ID_KEY)));

                    JSONObject userJson = response.getJSONObject("data");
                    userToUpdate.setName(userJson.getString("name"));
                    userToUpdate.setEmail(userJson.getString("email"));
                    userToUpdate.setCreatedAt(userJson.getLong("createdAt"));
                    userToUpdate.save();

                    callback.onSuccess(null);
                } catch (JSONException e) {
                    callback.onFailure(parsingErrorMessage);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable,
                                  JSONObject errorResponse) {
                onFailureCommon(statusCode, errorResponse, callback);
            }
        });
    }

    public boolean isUserAuthenticated() {
        return Helpers.readFromPrefs(context, USER_ID_KEY) != null;
    }
}
