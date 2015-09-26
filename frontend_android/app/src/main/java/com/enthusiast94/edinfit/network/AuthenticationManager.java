package com.enthusiast94.edinfit.network;

import com.enthusiast94.edinfit.App;
import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models.User;
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

    public static void authenticate(String email, String password, final Callback<User> callback) {
        AsyncHttpClient client = getAsyncHttpClient();

        RequestParams requestParams = new RequestParams();
        requestParams.add("email", email);
        requestParams.add("password", password);

        client.post(API_BASE + "/authenticate", requestParams, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    User user = new User(null, null, null, 0, response.getJSONObject("data").getString("token"));
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
}
