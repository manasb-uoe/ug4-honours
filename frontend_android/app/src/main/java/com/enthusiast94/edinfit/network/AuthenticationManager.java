package com.enthusiast94.edinfit.network;

import android.util.Base64;

import com.enthusiast94.edinfit.App;
import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models.User;
import com.enthusiast94.edinfit.utils.Helpers;
import com.google.gson.Gson;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

/**
 * Created by manas on 26-09-2015.
 */
public class AuthenticationManager extends Manager {

    public static final String TAG = AuthenticationManager.class.getSimpleName();
    private static final String USER_PREFS_KEY = "userPrefKey";

    public static void authenticate(String email, final String password, final Callback<User> callback) {
        AsyncHttpClient client = getAsyncHttpClient();

        RequestParams requestParams = new RequestParams();
        requestParams.add("email", email);
        requestParams.add("password", password);

        client.post(API_BASE + "/authenticate", requestParams, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    // Decode JWT token's payload in order to retrieve user data as json
                    String token = response.getJSONObject("data").getString("token");
                    String decodedPayload =
                            new String(Base64.decode(token.split("\\.")[1].getBytes(), Base64.DEFAULT));

                    // parse the decoded payload and construct new user object
                    Gson gson = new Gson();
                    User user = gson.fromJson(decodedPayload, User.class);
                    user.setAuthToken(token);

                    // persist user object in shared preferences as json string
                    Helpers.writeToPrefs(App.getAppContext(), USER_PREFS_KEY, gson.toJson(user));

                    if (callback != null) callback.onSuccess(user);
                } catch (JSONException e) {
                    if (callback != null)
                        callback.onFailure(App.getAppContext().getString(R.string.error_parsing));
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                if (callback != null) {
                    try {
                        callback.onFailure(errorResponse.getJSONObject("error").getString("message"));
                    } catch (JSONException e) {
                        callback.onFailure(App.getAppContext().getString(R.string.error_parsing));
                    }
                }
            }
        });
    }

    public static void deauthenticate() {
        Helpers.clearPrefs(App.getAppContext());
    }

    public static User getAuthenticatedUser() {
        String userJson = Helpers.readFromPrefs(App.getAppContext(), USER_PREFS_KEY);

        if (userJson != null) {
            Gson gson = new Gson();
            return gson.fromJson(userJson, User.class);
        }

        return null;
    }

    public static boolean isUserAuthenticated() {
        return Helpers.readFromPrefs(App.getAppContext(), USER_PREFS_KEY) != null;
    }
}
