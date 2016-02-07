package com.enthusiast94.edinfit.network;

import android.content.Context;
import android.util.Log;
import android.util.Pair;

import com.enthusiast94.edinfit.models.Departure;
import com.enthusiast94.edinfit.models.Directions;
import com.enthusiast94.edinfit.models.Service;
import com.enthusiast94.edinfit.models.Stop;
import com.enthusiast94.edinfit.models.WaitOrWalkSuggestion;
import com.enthusiast94.edinfit.utils.Helpers;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by manas on 11-11-2015.
 */
public class WaitOrWalkService {

    private static final String TAG = WaitOrWalkService.class.getSimpleName();
    private static WaitOrWalkService instance;
    private Context context;
    private BaseService baseService;

    private WaitOrWalkService(Context context) {
        this.context = context;
        baseService = BaseService.getInstance();
    }

    public static void init(Context context) {
        if (instance != null) {
            throw new IllegalStateException(TAG + " has already been initialized.");
        }

        instance = new WaitOrWalkService(context);
    }

    public static WaitOrWalkService getInstance() {
        if (instance == null) {
            throw new IllegalStateException(TAG + " has not been initialized yet.");
        }

        return instance;
    }

    public BaseService.Response<List<WaitOrWalkSuggestion>> getWaitOrWalkSuggestions(
            String routeDestination, String serviceName, String originStopId,
            String destinationStopId, LatLng userLocation) {

        BaseService.Response<List<WaitOrWalkSuggestion>> response = new BaseService.Response<>();

        // retrieve service corresponding to the provided service name
        Service service = Service.findByName(serviceName);

        if (service == null) {
            response.setError("No service with name: " + serviceName);
            return response;
        }

        // retrieve route corresponding to the provided route destination
        List<Service.Route> allRoutes = service.getRoutes();
        Service.Route route = null;

        for (Service.Route r : allRoutes) {
            if (r.getDestination().equals(routeDestination)) {
                route = r;
                break;
            }
        }

        if (route == null) {
            response.setError("No route with destination: " + routeDestination);
            return response;
        }

        // Find a list of potential walking destination stops. Note that the first element in
        // this list is the origin stop since it will be needed when the final result is to WAIT.
        List<Stop> stops = new ArrayList<>();
        List<Stop> allStopsInRoute = route.getStops();
        final int numberOfStopsToSkip = 5;

        for (int i=0; i<allStopsInRoute.size(); i++) {
            Stop currentStop = allStopsInRoute.get(i);

            if (currentStop.get_id().equals(originStopId)) {
                int counter = 0;

                while (counter < numberOfStopsToSkip) {
                    int index = i + counter;

                    if (index < allStopsInRoute.size()) {
                        stops.add(allStopsInRoute.get(index));
                    }

                    if (allStopsInRoute.get(index).get_id().equals(destinationStopId)) {
                        break;
                    }

                    counter++;
                }

                break;
            }
        }

        // retrieve stops with departures (for current day and time) for the corresponding stops
        String currentTime24h = Helpers.getCurrentTime24h();
        int currentDayCode = Helpers.getDayCode(Helpers.getCurrentDay());
        List<Pair<Stop, List<Departure>>> stopDeparturesPair = new ArrayList<>();

        for (Stop stop : stops) {
            BaseService.Response<List<Departure>> responseDepartures =
                    StopService.getInstance().getDepartures(stop,  currentDayCode, currentTime24h);

            if (!responseDepartures.isSuccessfull()) {
                response.setError(responseDepartures.getError());
                return response;
            }

            stopDeparturesPair.add(new Pair<>(stop, responseDepartures.getBody()));
        }


        boolean shouldStopProcessing = false;
        List<WaitOrWalkSuggestion> waitOrWalkSuggestions = new ArrayList<>();

        for (int i=0; i<stopDeparturesPair.size(); i++) {
            Pair<Stop, List<Departure>> currentPair =  stopDeparturesPair.get(i);
            Log.d(TAG, currentPair.first.getName());

            if (shouldStopProcessing || i == 0 /* exclude last stop */) {
                continue;
            }

            // retrieve upcoming departure corresponding to the provided service name
            Departure upcomingDeparture = null;

            for (Departure departure : currentPair.second) {
                if (departure.getServiceName().equals(serviceName)) {
                    upcomingDeparture = departure;
                    break;
                }
            }

            if (upcomingDeparture == null) {
                response.setError("No upcoming departures for service: " + serviceName +
                        " at stop: " + currentPair.first.getName());
                return response;
            }

            // retrieve walking directions from user's location to stop's location, along with
            // time remaining for upcoming departure
            long remainingTimeForDepartureMillis =
                    Helpers.getRemainingTimeMillisFromNow(upcomingDeparture.getTime());

            Log.d(TAG, "upcoming: " + upcomingDeparture.getTime());
            Log.d(TAG, "remaining time: " + Helpers.humanizeDurationInMillisToMinutes(remainingTimeForDepartureMillis));

            BaseService.Response<Directions> directionsResponse = DirectionsService.getInstance()
                    .getWalkingDirections(userLocation, currentPair.first.getPosition());

            if (!directionsResponse.isSuccessfull()) {
                response.setError(directionsResponse.getError());
                return response;
            }

            Directions walkingDirections = directionsResponse.getBody();
            Log.d(TAG, "walk duration: " + walkingDirections.getDurationText());
            if (walkingDirections.getDuration() * 1000 <= remainingTimeForDepartureMillis) {
                waitOrWalkSuggestions.add(new WaitOrWalkSuggestion(
                        WaitOrWalkSuggestion.WaitOrWalkSuggestionType.WALK,
                        currentPair.first, upcomingDeparture, walkingDirections));
            } else {
                if (i != 1) {
                    shouldStopProcessing = true;
                    continue;
                }

                Pair<Stop, List<Departure>> originStopDeparturesPair = stopDeparturesPair.get(0);
                Departure upcomingDepartureAtOriginStop = null;

                for (Departure departure : originStopDeparturesPair.second) {
                    if (departure.getServiceName().equals(serviceName)) {
                        upcomingDepartureAtOriginStop = departure;
                        break;
                    }
                }

                if (upcomingDepartureAtOriginStop == null) {
                    response.setError("No upcoming departures for service: " + serviceName +
                            " at stop: " + originStopDeparturesPair.first.getName());
                    return response;
                }

                BaseService.Response<Directions> directionsResponse2 = DirectionsService.getInstance()
                        .getWalkingDirections(userLocation, originStopDeparturesPair.first.getPosition());

                if (!directionsResponse2.isSuccessfull()) {
                    response.setError(directionsResponse2.getError());
                    return response;
                }

                waitOrWalkSuggestions.add(new WaitOrWalkSuggestion(
                        WaitOrWalkSuggestion.WaitOrWalkSuggestionType.WAIT, originStopDeparturesPair.first,
                        upcomingDepartureAtOriginStop, directionsResponse2.getBody()));

                shouldStopProcessing = true;
            }
        }

        response.setBody(waitOrWalkSuggestions);
        return response;
    }
}
