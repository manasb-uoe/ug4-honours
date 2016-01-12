package com.enthusiast94.edinfit.network;

import android.content.Context;
import android.util.Log;
import android.util.Pair;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models_2.Departure;
import com.enthusiast94.edinfit.models_2.FavouriteStop;
import com.enthusiast94.edinfit.models_2.Stop;
import com.enthusiast94.edinfit.models_2.StopToStopJourney;
import com.enthusiast94.edinfit.utils.Helpers;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public BaseService.Response<Void> populateStops() {
        // first delete all existing stops
        Stop.deleteAll();

        BaseService.Response<Void> response = new BaseService.Response<>();
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
                        departureJson.getInt("day"),
                        Stop.findById(departureJson.getString("destination_stop_id"))
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

    public BaseService.Response<List<Pair<FavouriteStop, List<Departure>>>> getDeparturesForFavouriteStops(
            List<FavouriteStop> favouriteStops) {

        BaseService.Response<List<Pair<FavouriteStop, List<Departure>>>> response =
                new BaseService.Response<>();

        List<Pair<FavouriteStop, List<Departure>>> listOfPairs = new ArrayList<>();

        for (FavouriteStop favouriteStop : favouriteStops) {
            BaseService.Response<List<Departure>> departuresResponse =
                    StopService.getInstance().getDepartures(favouriteStop.getStop(),
                            Helpers.getDayCode(Helpers.getCurrentDay()), Helpers.getCurrentTime24h());

            if (departuresResponse.isSuccessfull()) {
                listOfPairs.add(new Pair<>(favouriteStop, departuresResponse.getBody()));
            } else {
                response.setError(departuresResponse.getError());
                return response;
            }
        }

        response.setBody(listOfPairs);
        return response;
    }

    public BaseService.Response<StopToStopJourney> getStopToStopJourneys(
            String startStopId,
            String finishStopId,
            String serviceName,
            String time) {
        BaseService.Response<StopToStopJourney> response = new BaseService.Response<>();
        Request request = baseService.createTfeGetRequest("stoptostop-timetable/?start_stop_id=" +
                startStopId + "&finish_stop_id=" + finishStopId + "&date=" +
                Helpers.getEpochFromToday24hTime(time) + "&duration=120");

        Log.d(TAG, request.url().toString());

        try {
            Response okHttpResponse = baseService.getHttpClient().newCall(request).execute();

            if (!okHttpResponse.isSuccessful()) {
                response.setError(okHttpResponse.message());
                return response;
            }

            JSONArray journeysJsonArray =
                    new JSONObject(okHttpResponse.body().string()).getJSONArray("journeys");

            if (journeysJsonArray.length() == 0) {
                response.setError(context.getString(R.string.no_journey_found));
                return response;
            }

            JSONObject journeyJson = journeysJsonArray.getJSONObject(0);

            String serviceName1 = journeyJson.getString("service_name");
            // only proceed if required service name matches parsed service name
            if (!serviceName1.equals(serviceName)) {
                response.setError(context.getString(R.string.no_journey_found));
            }

            String destination = journeyJson.getString("destination");

            JSONArray departuresJsonArray = journeyJson.getJSONArray("departures");
            List<StopToStopJourney.Departure> departures = new ArrayList<>();
            for (int j=0; j<departuresJsonArray.length(); j++) {
                JSONObject departureJson = departuresJsonArray.getJSONObject(j);
                departures.add(new StopToStopJourney.Departure(
                        Stop.findById(departureJson.getString("stop_id")),
                        departureJson.getString("name"),
                        departureJson.getString("time")
                ));
            }

            StopToStopJourney journey = new StopToStopJourney(serviceName1, destination, departures);

            response.setBody(journey);
            return response;

        } catch (IOException | JSONException e) {
            e.printStackTrace();
            response.setError(e.getMessage());
            return response;
        }
    }
}
