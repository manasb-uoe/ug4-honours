package com.enthusiast94.edinfit.services;

import com.enthusiast94.edinfit.App;
import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models.User;
import com.loopj.android.http.AsyncHttpClient;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by manas on 26-09-2015.
 */
public class BaseService {

    protected static final String API_BASE = "http://192.168.0.10:4000/api";
    private static final String USER_AGENT = "android:com.enthusiast94.edinfit";

    protected static AsyncHttpClient getAsyncHttpClient(boolean isAuthenticationRequired) {
        AsyncHttpClient client = new AsyncHttpClient();
        client.setUserAgent(USER_AGENT);

        if (isAuthenticationRequired) {
            // add authorization header if user is authenticated
            if (UserService.isUserAuthenticated()) {
                User user = UserService.getAuthenticatedUser();
                if (user != null) {
                    client.addHeader("Authorization", "Bearer " + user.getAuthToken());
                }
            }
        }

        return client;
    }

    /**
     * Common onFailure method for all API responses.
     */

    protected static void onFailureCommon(int statusCode, JSONObject errorResponse, Callback callback) {
        if (callback != null) {
            try {
                if (statusCode != 0) {
                    callback.onFailure(errorResponse.getJSONObject("error").getString("message"));
                } else {
                    callback.onFailure(App.getAppContext().getString(R.string.error_request_time_out));
                }
            } catch (JSONException e) {
                callback.onFailure(App.getAppContext().getString(R.string.error_parsing));
            }
        }
    }

    public interface Callback<T> {
        void onSuccess(T data);
        void onFailure(String message);
    }
}
