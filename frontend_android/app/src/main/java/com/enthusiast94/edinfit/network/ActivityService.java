package com.enthusiast94.edinfit.network;

import android.content.Context;

import com.enthusiast94.edinfit.models.Activity;
import com.google.gson.Gson;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

/**
 * Created by manas on 25-11-2015.
 */
public class ActivityService extends BaseService {

    public static final String TAG = ActivityService.class.getSimpleName();
    private static ActivityService instance;
    private Context context;

    public ActivityService(Context context) {
        this.context = context;
    }

    public static void init(Context context) {
        if (instance != null) {
            throw new IllegalStateException(TAG + " has already been initialized.");
        }

        instance = new ActivityService(context);
    }

    public static ActivityService getInstance() {
        if (instance == null) {
            throw new IllegalStateException(TAG + " has not been initialized yet.");
        }

        return instance;
    }

    public void addNewActivity(Activity activity, final Callback<Void> callback) {
        RequestParams requestParams = new RequestParams();
        Gson gson = new Gson();
        requestParams.put("activity", gson.toJson(activity));

        AsyncHttpClient client = getAsyncHttpClient(true);
        client.post(API_BASE + "/activities", requestParams, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                callback.onSuccess(null);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                onFailureCommon(statusCode, errorResponse, callback);
            }
        });
    }
}
