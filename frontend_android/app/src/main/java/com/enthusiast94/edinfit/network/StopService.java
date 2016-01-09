package com.enthusiast94.edinfit.network;

import android.content.Context;

import com.arasthel.asyncjob.AsyncJob;
import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models_2.Departure;
import com.enthusiast94.edinfit.models_2.Stop;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.SyncHttpClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.Header;

/**
 * Created by manas on 01-10-2015.
 */
public class StopService extends BaseService {

    public static final String TAG = StopService.class.getSimpleName();
    private static StopService instance;
    private Context context;
    private String parsingErrorMessage;

    private StopService(Context context) {
        this.context = context;
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
        callback.onSuccess(Stop.getAll());
    }

    public void saveOrUnsaveStop(String stopId, boolean shouldSave, final Callback<Void> callback) {
        callback.onSuccess(null);

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

    public void getSavedStops(int departuresLimit, final Callback<List<Stop>> callback) {
        callback.onSuccess(new ArrayList<Stop>());

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

    public void populateStops(final Callback<Void> callback) {
        AsyncJob.doInBackground(new AsyncJob.OnBackgroundJob() {

            @Override
            public void doOnBackground() {
                // no need to proceed if stops already exist
                if (Stop.getCount() > 0) {
                    onSuccessMainThread(null, callback);
                    return;
                }

                SyncHttpClient client = getTfeSyncHttpClient(context);
                client.get(TFE_API_BASE + "/stops", new JsonHttpResponseHandler() {

                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                        try {
                            JSONArray stopsJsonArray = response.getJSONArray("stops");
                            for (int i=0; i<stopsJsonArray.length(); i++) {
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

                            onSuccessMainThread(null, callback);
                        } catch (JSONException e) {
                            onFailureMainThread(parsingErrorMessage, callback);
                        }
                    }

                    @Override
                    public void onFailure(final int statusCode, Header[] headers, Throwable throwable,
                                          final JSONObject errorResponse) {
                        onFailureMainThread(statusCode, errorResponse, callback);
                    }
                });
            }
        });
    }

    public void getDeparturesAsync(final Stop stop, final Integer dayCode, final String time,
                                   final Callback<List<Departure>> callback) {
        AsyncJob.doInBackground(new AsyncJob.OnBackgroundJob() {

            @Override
            public void doOnBackground() {
                List<Departure> departures = stop.getDepartures(dayCode, time);
                if (departures != null) {
                    onSuccessMainThread(departures, callback);
                    return;
                }

                SyncHttpClient client = getTfeSyncHttpClient(context);
                client.get(TFE_API_BASE + "/timetables/" + stop.get_id(), new JsonHttpResponseHandler() {

                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                        try {
                            JSONArray departuresJsonArray = response.getJSONArray("departures");

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

                            onSuccessMainThread(stop.getDepartures(dayCode, time), callback);
                        } catch (JSONException e) {
                            onFailureMainThread(parsingErrorMessage, callback);
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable throwable,
                                          JSONObject errorResponse) {
                        onFailureMainThread(statusCode, errorResponse, callback);
                    }
                });
            }
        });
    }
}
