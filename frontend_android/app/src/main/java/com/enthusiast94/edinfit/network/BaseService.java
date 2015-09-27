package com.enthusiast94.edinfit.network;

import com.enthusiast94.edinfit.App;
import com.enthusiast94.edinfit.R;
import com.loopj.android.http.AsyncHttpClient;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by manas on 26-09-2015.
 */
public class BaseService {

    protected static final String API_BASE = "http://10.0.3.2:4000/api";
    private static final String USER_AGENT = "android:com.enthusiast94.edinfit";

    protected static AsyncHttpClient getAsyncHttpClient() {
        AsyncHttpClient client = new AsyncHttpClient();
        client.setUserAgent(USER_AGENT);

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
}
