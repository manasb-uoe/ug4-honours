package com.enthusiast94.edinfit.network;

import android.content.Context;
import android.util.Log;

import com.enthusiast94.edinfit.models_2.Departure;
import com.enthusiast94.edinfit.models_2.Stop;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by manas on 01-10-2015.
 */
public class StopService {

    public static final String TAG = StopService.class.getSimpleName();
    private static StopService instance;
    private Context context;
    private BaseService baseService;

    private StopService(Context context) {
        this.context = context;
        baseService = BaseService.getInstance();
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

    public void saveOrUnsaveStop(String stopId, boolean shouldSave) {
//        String url = API_BASE + "/stops/" + stopId + "/save";
//
//        JsonHttpResponseHandler responseHandler = new JsonHttpResponseHandler() {
//            @Override
//            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
//                UserService.getInstance().updateCachedUser(new Callback<Void>() {
//
//                    @Override
//                    public void onSuccess(Void data) {
//                        callback.onSuccess(null);
//                    }
//
//                    @Override
//                    public void onFailure(String message) {
//                        callback.onFailure(message);
//                    }
//                });
//            }
//
//            @Override
//            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
//                onFailureCommon(statusCode, errorResponse, callback);
//            }
//        };
//
//        AsyncHttpClient client = getAsyncHttpClient(true);
//
//        if (shouldSave) {
//            client.post(url, responseHandler);
//        } else {
//            client.delete(url, responseHandler);
//        }
    }

    public void getSavedStops(int departuresLimit) {
//        callback.onSuccess(new ArrayList<Stop>());

//        AsyncHttpClient client = getAsyncHttpClient(true);
//
//        RequestParams requestParams = new RequestParams();
//        requestParams.add("time", Helpers.getCurrentTime24h());
//        requestParams.add("departures_limit", String.valueOf(departuresLimit));
//
//        client.get(API_BASE + "/stops/saved", requestParams, new JsonHttpResponseHandler() {
//
//            @Override
//            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
//                try {
//                    Gson gson = new Gson();
//                    Stop[] stopsArray = gson.fromJson(
//                            response.getJSONArray("data").toString(), Stop[].class
//                    );
//                    // return mutable list
//                    List<Stop> stopsList = new ArrayList<>(Arrays.asList(stopsArray));
//
//                    callback.onSuccess(stopsList);
//
//                } catch (JSONException e) {
//                        callback.onFailure(parsingErrorMessage);
//                }
//            }
//
//            @Override
//            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
//                onFailureCommon(statusCode, errorResponse, callback);
//            }
//        });
    }

    public BaseService.Response<Void> populateStops() {
        BaseService.Response<Void> response = new BaseService.Response<>();

        // no need to proceed if stops already exist
        if (Stop.getCount() > 0) {
            return response;
        }

        Request request = baseService.createTfeGetRequest("stops");

        try {
            Response okHttpResponse = baseService.getHttpClient().newCall(request).execute();

            if (!okHttpResponse.isSuccessful()) {
                response.setError(okHttpResponse.message());
                return response;
            }

            JSONArray stopsJsonArray =
                    new JSONObject(okHttpResponse.body().string()).getJSONArray("stops");

            for (int i = 0; i < stopsJsonArray.length(); i++) {
                JSONObject stopJson = stopsJsonArray.getJSONObject(i);
                Stop stop = new Stop(
                        stopJson.getString("stop_id"),
                        stopJson.getString("name"),
                        stopJson.getDouble("latitude"),
                        stopJson.getDouble("longitude"),
                        stopJson.getString("direction"),
                        stopJson.getString("destinations"),
                        stopJson.getString("services")
                );

                stop.save();
            }

            return response;

        } catch (IOException | JSONException e) {
            e.printStackTrace();
            response.setError(e.getMessage());
            return response;
        }
    }

    public BaseService.Response<List<Departure>> getDepartures(
            final Stop stop,
            final Integer dayCode,
            final String time) {

        Request request = baseService.createTfeGetRequest("timetables/" + stop.get_id());
        BaseService.Response<List<Departure>> response = new BaseService.Response<>();

        List<Departure> departures = stop.getDepartures(dayCode, time);
        if (departures != null) {
            response.setBody(departures);
            return response;
        }

        try {
            Response okHttpResponse = baseService.getHttpClient().newCall(request).execute();

            if (!okHttpResponse.isSuccessful()) {
                response.setError(okHttpResponse.message());
                return response;
            }

            JSONArray departuresJsonArray =
                    new JSONObject(okHttpResponse.body().string()).getJSONArray("departures");

            for (int i = 0; i < departuresJsonArray.length(); i++) {
                JSONObject departureJson = departuresJsonArray.getJSONObject(i);
                Departure departure = new Departure(
                        stop,
                        departureJson.getString("service_name"),
                        departureJson.getString("time"),
                        departureJson.getString("destination"),
                        departureJson.getInt("day")
                );

                departure.save();
            }

            response.setBody(stop.getDepartures(dayCode, time));
            return response;

        } catch (IOException | JSONException e) {
            e.printStackTrace();
            response.setError(e.getMessage());
            return response;
        }
    }
}
