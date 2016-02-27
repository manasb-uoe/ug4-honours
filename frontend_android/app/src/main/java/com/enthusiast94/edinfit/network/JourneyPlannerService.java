package com.enthusiast94.edinfit.network;

import android.content.Context;
import android.util.Log;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models.Directions;
import com.enthusiast94.edinfit.models.Journey;
import com.enthusiast94.edinfit.models.Service;
import com.enthusiast94.edinfit.models.Stop;
import com.enthusiast94.edinfit.ui.journey_planner.enums.RouteOption;
import com.enthusiast94.edinfit.ui.journey_planner.enums.TimeMode;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.PolyUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
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
                                                           long time /* seconds */, TimeMode timeMode,
                                                           RouteOption routeOption) {
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

        BaseService.Response<List<Journey>> response = new BaseService.Response<>();

        if (routeOption != RouteOption.ONLY_WALK) {
            Request request = baseService.createTfeGetRequest("directions/?start=" + startLatLng.latitude +
                    "," + startLatLng.longitude + "&finish=" + finishLatLng.latitude + "," +
                    finishLatLng.longitude + "&date=" + time + "&time_mode=" + timeModeText);

            Log.d(TAG, request.url().toString());

            try {
                Response okHttpResponse = baseService.getHttpClient().newCall(request).execute();

                if (!okHttpResponse.isSuccessful()) {
                    response.setError(okHttpResponse.message());
                    return response;
                }

                List<Journey> journeys = new ArrayList<>();
                JSONArray journeysJsonArray =
                        new JSONObject(okHttpResponse.body().string()).getJSONArray("journeys");

                journeysLoop:
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

                        // make sure that journeys that do not match user's timer requirements are omitted
                        Date requestedDate = new Date(time * 1000);
                        if (timeMode == TimeMode.LEAVE_AFTER) {
                            Date startDate = new Date(startPoint.getTimestamp() * 1000);
                            if (startDate.before(requestedDate)) {
                                continue journeysLoop;
                            }
                        } else {
                            Date finishDate = new Date(finishPoint.getTimestamp() * 1000);
                            if (finishDate.after(requestedDate)) {
                                continue journeysLoop;
                            }
                        }

                        if (mode.equals("walk")) {
                            legs.add(new Journey.WalkLeg(startPoint, finishPoint, null));
                        } else if (mode.equals("bus")) {
                            JSONObject serviceJson = legJson.getJSONObject("service");
                            JSONArray stopsOnRouteJsonArray = serviceJson.getJSONArray("stops_on_route");
                            List<String> stopsOnRoute = new ArrayList<>();
                            for (int k=0; k<stopsOnRouteJsonArray.length(); k++) {
                                stopsOnRoute.add(stopsOnRouteJsonArray.getString(k));
                            }

                            // Find polyline from bus leg start to finish using route info stored in db
                            // because the polyline provided by TFE API is incomplete. If such a polyline
                            // can not be found, only then use the one provided by the API.
                            Stop startStop = startPoint.getStop();
                            Stop finishStop = finishPoint.getStop();
                            List<LatLng> polylineLatLngs = new ArrayList<>();
                            Service service = Service.findByName(serviceJson.getString("name"));
                            for (Service.Route route : service.getRoutes()) {
                                if (route.getDestination().equals(serviceJson.getString("destination"))) {
                                    boolean shouldAdd = false;
                                    for (Service.Point point : route.getPoints()) {
                                        if (point.getStopId().equals(startStop.get_id())) {
                                            shouldAdd = true;
                                        }

                                        if (shouldAdd) {
                                            polylineLatLngs.add(point.getLatLng());
                                        }

                                        if (point.getStopId().equals(finishStop.get_id())) {
                                            shouldAdd = false;
                                        }
                                    }
                                }
                            }

                            legs.add(new Journey.BusLeg(
                                    startPoint,
                                    finishPoint,
                                    polylineLatLngs.size() > 0 ? PolyUtil.encode(polylineLatLngs) : serviceJson.getString("polyline"),
                                    serviceJson.getString("name"),
                                    serviceJson.getString("destination"),
                                    stopsOnRoute));
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
        } else {
            BaseService.Response<Directions> directionsResponse =
                    DirectionsService.getInstance().getWalkingDirections(startLatLng, finishLatLng);

            if (!directionsResponse.isSuccessfull()) {
                response.setError(directionsResponse.getError());
                return response;
            }

            List<Journey> journeys = new ArrayList<>();
            Directions directions = directionsResponse.getBody();
            Journey.Point startPoint = new Journey.Point(context.getString(R.string.label_start),
                    startLatLng.latitude, startLatLng.longitude, time, null);
            Journey.Point finishPoint = new Journey.Point(context.getString(R.string.finish),
                    finishLatLng.latitude, finishLatLng.longitude,
                    time + directions.getDuration(), null);

            if (timeMode == TimeMode.LEAVE_AFTER) {
                startPoint.setTimestamp(time);
                finishPoint.setTimestamp(time + directions.getDuration());
            } else {
                startPoint.setTimestamp(time - directions.getDuration());
                finishPoint.setTimestamp(time);
            }

            Journey.Leg leg = new Journey.WalkLeg(startPoint, finishPoint, directions.getPolyline());
            journeys.add(new Journey(Collections.singletonList(leg), startPoint.getTimestamp(),
                    finishPoint.getTimestamp(), (int) (directions.getDuration() / 60.0)));

            response.setBody(journeys);
            return response;
        }
    }
}
