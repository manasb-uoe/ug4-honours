package com.enthusiast94.edinfit.network;

import android.content.Context;
import android.util.Log;

import com.enthusiast94.edinfit.models.Journey;
import com.enthusiast94.edinfit.ui.journey_planner.enums.TimeMode;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by manas on 29-01-2016.
 */
public class JourneyPlannerService {

    private static final String TAG = JourneyPlannerService.class.getSimpleName();
    private static JourneyPlannerService instance;
    private Context context;
    private BaseService baseService;

    private JourneyPlannerService(Context context) {
        this.context = context;
        baseService = BaseService.getInstance();
    }

    public static void init(Context context) {
        if (instance != null) {
            throw new IllegalStateException(TAG + " has already been initialized.");
        }

        instance = new JourneyPlannerService(context);
    }

    public static JourneyPlannerService getInstance() {
        if (instance == null) {
            throw new IllegalStateException(TAG + " has not been initialized yet.");
        }

        return instance;
    }

    public BaseService.Response<List<Journey>> getJourneys(LatLng startLatLng, LatLng finishLatLng,
                                                           long time /* seconds */, TimeMode timeMode) {
        String timeModeText;
        switch (timeMode) {
            case LEAVE_AFTER:
                timeModeText = "LeaveAfter";
                break;
            case ARRIVE_BY:
                timeModeText = "ArriveBy";
                break;
            default:
                throw new IllegalArgumentException("Invalid time mode: " + timeMode.toString());
        }

        Request request = baseService.createTfeGetRequest("directions/?start=" + startLatLng.latitude +
                "," + startLatLng.longitude + "&finish=" + finishLatLng.latitude + "," +
        finishLatLng.longitude + "&date=" + time + "&time_mode=" + timeModeText);
        BaseService.Response<List<Journey>> response = new BaseService.Response<>();
        Log.d(TAG, "directions/?start=" + startLatLng.latitude +
                "," + startLatLng.longitude + "&finish=" + finishLatLng.latitude + "," +
                finishLatLng.longitude + "&date=" + time + "&time_mode=" + timeModeText);

        try {
            Response okHttpResponse = baseService.getHttpClient().newCall(request).execute();

            if (!okHttpResponse.isSuccessful()) {
                response.setError(okHttpResponse.message());
                return response;
            }

            List<Journey> journeys = new ArrayList<>();
            JSONArray journeysJsonArray =
                    new JSONObject(okHttpResponse.body().string()).getJSONArray("journeys");

            for (int i=0; i<journeysJsonArray.length(); i++) {
                JSONObject journeyJson = journeysJsonArray.getJSONObject(i);
                JSONArray legsJsonArray = journeyJson.getJSONArray("legs");
                List<Journey.Leg> legs = new ArrayList<>();

                for (int j=0; j<legsJsonArray.length(); j++) {
                    JSONObject legJson = legsJsonArray.getJSONObject(j);
                    JSONObject startJson = legJson.getJSONObject("start");
                    JSONObject finishJson = legJson.getJSONObject("finish");
                    String mode = legJson.getString("mode");
                    Journey.Point startPoint = new Journey.Point(
                            startJson.getString("name"),
                            startJson.getDouble("latitude"),
                            startJson.getDouble("longitude"),
                            startJson.getLong("time"),
                            !startJson.isNull("stop_id") ? startJson.getString("stop_id") : null
                    );
                    Journey.Point finishPoint = new Journey.Point(
                            finishJson.getString("name"),
                            finishJson.getDouble("latitude"),
                            finishJson.getDouble("longitude"),
                            finishJson.getLong("time"),
                            !finishJson.isNull("stop_id") ? finishJson.getString("stop_id") : null
                    );

                    if (mode.equals("walk")) {
                        legs.add(new Journey.WalkLeg(startPoint, finishPoint));
                    } else if (mode.equals("bus")) {
                        JSONObject serviceJson = legJson.getJSONObject("service");
                        JSONArray stopsOnRouteJsonArray = serviceJson.getJSONArray("stops_on_route");
                        List<String> stopsOnRoute = new ArrayList<>();
                        for (int k=0; k<stopsOnRouteJsonArray.length(); k++) {
                            stopsOnRoute.add(stopsOnRouteJsonArray.getString(k));
                        }

                        legs.add(new Journey.BusLeg(startPoint, finishPoint,
                                serviceJson.getString("name"),
                                serviceJson.getString("destination"), stopsOnRoute));
                    } else {
                        break;
                    }
                }

                journeys.add(new Journey(legs, journeyJson.getLong("start_time"),
                        journeyJson.getLong("finish_time"), journeyJson.getInt("duration")));
            }

            response.setBody(journeys);
            return response;

        } catch (IOException | JSONException e) {
            e.printStackTrace();
            response.setError(e.getMessage());
            return response;
        }
    }
}
