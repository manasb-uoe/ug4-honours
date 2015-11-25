package com.enthusiast94.edinfit.network;

import android.content.Context;
import android.support.annotation.Nullable;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models.Service;
import com.google.gson.Gson;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

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
        AsyncHttpClient client = getAsyncHttpClient(true);
        client.get(API_BASE + "/services/" + serviceName, new JsonHttpResponseHandler() {

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

    // if serviceNames is null then all services are returned by the server
    public void getServices(@Nullable List<String> serviceNames, final Callback<List<Service>> callback) {
        RequestParams requestParams = new RequestParams();

        if (serviceNames != null) {
            // need to send unordered collection else the server throws an error if size of ordered
            // collection > 20 (i.e. services[21] = blabla causes error)
            requestParams.put("services", new HashSet<>(serviceNames));
        }

        AsyncHttpClient client = getAsyncHttpClient(true);
        client.get(API_BASE + "/services", requestParams, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    Gson gson = new Gson();
                    Service[] servicesArray = gson.fromJson(response.getJSONArray("data").toString(),
                            Service[].class);

                    callback.onSuccess(Arrays.asList(servicesArray));

                } catch (JSONException e) {
                    callback.onFailure(parsingErrorMessage);
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                throwable.printStackTrace();
                onFailureCommon(statusCode, errorResponse, callback);
            }
        });
    }
}
