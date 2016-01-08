package com.enthusiast94.edinfit.network;

import android.content.Context;
import android.telecom.Call;

import com.arasthel.asyncjob.AsyncJob;
import com.enthusiast94.edinfit.App;
import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models_2.User;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.SyncHttpClient;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by manas on 26-09-2015.
 */
public class BaseService {

    protected static final String API_BASE = "http://ec2-52-28-155-29.eu-central-1.compute.amazonaws.com:4000/api";
    protected static final String TFE_API_BASE = "https://tfe-opendata.com/api/v1";
    private static final String USER_AGENT = "android:com.enthusiast94.edinfit";

    protected static AsyncHttpClient getAsyncHttpClient(boolean isAuthenticationRequired) {
        AsyncHttpClient client = new AsyncHttpClient();
        client.setUserAgent(USER_AGENT);
        client.setLoggingEnabled(false);

        if (isAuthenticationRequired) {
            // add authorization header if user is authenticated
            if (UserService.getInstance().isUserAuthenticated()) {
                User user = UserService.getInstance().getAuthenticatedUser();
                if (user != null) {
                    client.addHeader("Authorization", "Bearer " + user.getAuthToken());
                }
            }
        }

        return client;
    }

    protected static AsyncHttpClient getTfeSyncHttpClient(Context context) {
        SyncHttpClient client = new SyncHttpClient();
        client.setUserAgent(USER_AGENT);
        client.setLoggingEnabled(false);
        client.addHeader("Authorization", "Token " + context.getString(R.string.api_key_tfe));

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

    protected void onFailureMainThread(final int statusCode, final JSONObject errorResponse,
                                       final Callback callback) {
        AsyncJob.doOnMainThread(new AsyncJob.OnMainThreadJob() {

            @Override
            public void doInUIThread() {
                onFailureCommon(statusCode, errorResponse, callback);
            }
        });
    }

    protected void onFailureMainThread(final String error, final Callback callback) {
        AsyncJob.doOnMainThread(new AsyncJob.OnMainThreadJob() {

            @Override
            public void doInUIThread() {
                callback.onFailure(error);
            }
        });
    }

    protected void onSuccessMainThread(final Object object, final Callback callback) {
        AsyncJob.doOnMainThread(new AsyncJob.OnMainThreadJob() {

            @Override
            public void doInUIThread() {
                callback.onSuccess(object);
            }
        });
    }

    public interface Callback<T> {
        void onSuccess(T data);
        void onFailure(String message);
    }
}
