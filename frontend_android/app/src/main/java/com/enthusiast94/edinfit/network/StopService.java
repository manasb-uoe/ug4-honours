package com.enthusiast94.edinfit.network;

import android.content.Context;
import android.telecom.Call;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models.Stop;
import com.enthusiast94.edinfit.utils.Helpers;
import com.google.gson.Gson;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cz.msebera.android.httpclient.Header;

/**
 * Created by manas on 01-10-2015.
 */
public class StopService extends BaseService {

    public static final String TAG = StopService.class.getSimpleName();
    private static StopService instance;
    private String parsingErrorMessage;

    private StopService(Context context) {
        this.parsingErrorMessage = context.getString(R.string.error_parsing);
    }

    public static void init(Context context) {
        if (instance != null) {
            throw new IllegalStateException(TAG + " has already been initialized.");
        }

        instance = new StopService(context);
    }

    public static StopService getInstance() {
        if (instance == null) {
            throw new IllegalStateException(TAG + " has not been initialized yet.");
        }

        return instance;
    }

    public void getAllStops(final Callback<List<Stop>> callback) {
        AsyncHttpClient client = getAsyncHttpClient(true);
        client.get(API_BASE + "/stops", new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    Gson gson = new Gson();
                    Stop[] stopsArray = gson.fromJson(
                            response.getJSONArray("data").toString(), Stop[].class
                    );
                    List<Stop> stopsList = Arrays.asList(stopsArray);

                    if (callback != null) callback.onSuccess(stopsList);

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

    public void getStop(String stopId, Integer dayCode, String time,  final Callback<Stop> callback) {
        RequestParams requestParams = new RequestParams();
        requestParams.put("day", dayCode);
        requestParams.put("time", time);

        AsyncHttpClient client = getAsyncHttpClient(true);
        client.get(API_BASE + "/stops/" + stopId, requestParams, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    Gson gson = new Gson();
                    Stop stop = gson.fromJson(response.getJSONObject("data").toString(), Stop.class);

                    if (callback != null) callback.onSuccess(stop);

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

    public void getNearbyStops(Double latitude, Double longitude, int maxDistance,
                               Double nearDistance, String time,
                               int stopsLimit, int departuresLimit, final Callback<List<Stop>> callback) {

        RequestParams requestParams = new RequestParams();
        requestParams.put("latitude", latitude);
        requestParams.put("longitude", longitude);
        requestParams.put("stops_limit", stopsLimit);
        requestParams.put("departures_limit", departuresLimit);
        requestParams.put("time", time);
        requestParams.put("max_distance", maxDistance);
        requestParams.put("near_distance", nearDistance);

        AsyncHttpClient client = getAsyncHttpClient(true);
        client.get(API_BASE + "/stops/nearby", requestParams, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    Gson gson = new Gson();
                    Stop[] stopsArray = gson.fromJson(
                            response.getJSONArray("data").toString(), Stop[].class
                    );
                    List<Stop> stopsList = Arrays.asList(stopsArray);

                    if (callback != null) callback.onSuccess(stopsList);

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

    public void saveOrUnsaveStop(String stopId, boolean shouldSave, final Callback<Void> callback) {
        String url = API_BASE + "/stops/" + stopId + "/save";

        JsonHttpResponseHandler responseHandler = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                UserService.getInstance().updateCachedUser(new Callback<Void>() {

                    @Override
                    public void onSuccess(Void data) {
                        callback.onSuccess(null);
                    }

                    @Override
                    public void onFailure(String message) {
                        callback.onFailure(message);
                    }
                });
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                onFailureCommon(statusCode, errorResponse, callback);
            }
        };

        AsyncHttpClient client = getAsyncHttpClient(true);

        if (shouldSave) {
            client.post(url, responseHandler);
        } else {
            client.delete(url, responseHandler);
        }
    }

    public void getSavedStops(final Callback<List<Stop>> callback) {
        AsyncHttpClient client = getAsyncHttpClient(true);

        RequestParams requestParams = new RequestParams();
        requestParams.add("time", Helpers.getCurrentTime24h());

        client.get(API_BASE + "/stops/saved", requestParams, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    Gson gson = new Gson();
                    Stop[] stopsArray = gson.fromJson(
                            response.getJSONArray("data").toString(), Stop[].class
                    );
                    // return mutable list
                    List<Stop> stopsList = new ArrayList<>(Arrays.asList(stopsArray));

                    callback.onSuccess(stopsList);

                } catch (JSONException e) {
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
