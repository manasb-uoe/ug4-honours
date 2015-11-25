package com.enthusiast94.edinfit.network;

import android.content.Context;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models.Directions;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

/**
 * Created by manas on 10-10-2015.
 */
public class DirectionsService extends BaseService {

    public static final String TAG = DirectionsService.class.getSimpleName();
    private static DirectionsService instance;
    private Context context;

    private DirectionsService(Context context) {
        this.context = context;
    }

    public static void init(Context context) {
        if (instance != null) {
            throw new IllegalStateException(TAG + " has already been initialized.");
        }

        instance = new DirectionsService(context);
    }

    public static DirectionsService getInstance() {
        if (instance == null) {
            throw new IllegalStateException(TAG + " has not been initialized yet.");
        }

        return instance;
    }

    public void getWalkingDirections(LatLng origin, LatLng destination,
                                     final Callback<Directions> callback) {

        RequestParams requestParams = new RequestParams();
        requestParams.put("origin", origin.latitude + "," + origin.longitude);
        requestParams.put("destination", destination.latitude + "," + destination.longitude);

        AsyncHttpClient client = getAsyncHttpClient(true);
        client.get(API_BASE + "/walking-directions", requestParams, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    Gson gson = new Gson();
                    Directions directions = gson.fromJson(response.getJSONObject("data").toString(),
                            Directions.class);

                    callback.onSuccess(directions);

                } catch (JSONException e) {
                    callback.onFailure(context.getString(R.string.error_parsing));
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                onFailureCommon(statusCode, errorResponse, callback);
            }
        });
    }
}
