package com.enthusiast94.edinfit.services;

import android.content.Context;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models.Service;
import com.google.gson.Gson;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

/**
 * Created by manas on 06-10-2015.
 */
public class ServiceService extends BaseService {

    public static final String TAG = ServiceService.class.getSimpleName();
    private static ServiceService instance;
    private String parsingErrorMessage;

    private ServiceService(Context context) {
        this.parsingErrorMessage = context.getString(R.string.error_parsing);
    }

    public static void init(Context context) {
        if (instance != null) {
            throw new IllegalStateException(TAG + " has already been initialized.");
        }

        instance = new ServiceService(context);
    }

    public static ServiceService getInstance() {
        if (instance == null) {
            throw new IllegalStateException(TAG + " has not been initialized yet.");
        }

        return instance;
    }

    public void getService(String serviceName,  final Callback<Service> callback) {
        RequestParams requestParams = new RequestParams();

        AsyncHttpClient client = getAsyncHttpClient(true);
        client.get(API_BASE + "/services/" + serviceName, requestParams, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    Gson gson = new Gson();
                    Service service = gson.fromJson(response.getJSONObject("data").toString(),
                            Service.class);

                    if (callback != null) callback.onSuccess(service);

                } catch (JSONException e) {
                    if (callback != null)
                        callback.onFailure(parsingErrorMessage);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                onFailureCommon(statusCode, errorResponse, callback);
            }
        });
    }
}
