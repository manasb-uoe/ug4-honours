package com.enthusiast94.edinfit.network;

import com.enthusiast94.edinfit.App;
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

    public static void getService(String serviceName,  final Callback<Service> callback) {
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
                        callback.onFailure(App.getAppContext().getString(R.string.error_parsing));
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                onFailureCommon(statusCode, errorResponse, callback);
            }
        });
    }
}
