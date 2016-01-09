//package com.enthusiast94.edinfit.network;
//
//import android.content.Context;
//
//import com.enthusiast94.edinfit.R;
//import com.enthusiast94.edinfit.models.LiveBus;
//import com.google.gson.Gson;
//import com.loopj.android.http.AsyncHttpClient;
//import com.loopj.android.http.JsonHttpResponseHandler;
//
//import org.json.JSONException;
//import org.json.JSONObject;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import cz.msebera.android.httpclient.Header;
//
///**
// * Created by manas on 18-10-2015.
// */
//public class LiveBusService extends BaseService {
//
//    public static final String TAG = LiveBusService.class.getSimpleName();
//    private static final String TFE_API_BASE = "https://tfe-opendata.com/api/v1";
//    private static LiveBusService instance;
//    private String tfeApiKey;
//    private String parsingErrorMessage;
//
//    private LiveBusService(Context context) {
//        tfeApiKey = context.getString(R.string.api_key_tfe);
//        parsingErrorMessage = context.getString(R.string.error_parsing);
//    }
//
//    public static void init(Context context) {
//        if (instance != null) {
//            throw new IllegalStateException(TAG + " has already been initialized.");
//        }
//
//        instance = new LiveBusService(context);
//    }
//
//    public static LiveBusService getInstance() {
//        if (instance == null) {
//            throw new IllegalStateException(TAG + " has not been initialized yet.");
//        }
//
//        return instance;
//    }
//
//    public void getLiveBuses(final String serviceName, final Callback<List<LiveBus>> callback) {
//        AsyncHttpClient client = getAsyncHttpClient(true);
//        client.addHeader("Authorization", "Token " + tfeApiKey);
//        client.get(TFE_API_BASE + "/vehicle_locations", new JsonHttpResponseHandler() {
//
//            @Override
//            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
//                try {
//                    // parse api response in order to retrieve live buses
//                    String vehiclesJson = response.getJSONArray("vehicles").toString();
//                    Gson gson = new Gson();
//                    LiveBus[] liveBusesArray = gson.fromJson(vehiclesJson, LiveBus[].class);
//
//                    // filter out buses that do not belong to requested service
//                    ArrayList<LiveBus> liveBusesFiltered = new ArrayList<>();
//                    for (LiveBus liveBus : liveBusesArray) {
//                        if (liveBus.getServiceName() != null &&
//                                liveBus.getServiceName().equals(serviceName)) {
//                            liveBusesFiltered.add(liveBus);
//                        }
//                    }
//
//                    callback.onSuccess(liveBusesFiltered);
//                } catch (JSONException e) {
//                    callback.onFailure(parsingErrorMessage);
//                }
//            }
//
//            @Override
//            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
//                super.onFailure(statusCode, headers, throwable, errorResponse);
//            }
//        });
//    }
//}
