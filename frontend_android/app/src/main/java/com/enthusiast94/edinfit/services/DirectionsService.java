package com.enthusiast94.edinfit.services;

import android.content.Context;

import com.directions.route.Route;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.enthusiast94.edinfit.R;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

/**
 * Created by manas on 10-10-2015.
 */
public class DirectionsService extends BaseService {

    public static final String TAG = DirectionsService.class.getSimpleName();
    private static DirectionsService instance;
    private String routingFailureErrorMessage;

    private DirectionsService(Context context) {
        this.routingFailureErrorMessage = context.getString(R.string.error_could_not_fetch_directions);
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
                                     final Callback<DirectionsResult> callback) {
        Routing routing = new Routing.Builder()
                .travelMode(Routing.TravelMode.WALKING)
                .waypoints(origin, destination)
                .withListener(new RoutingListener() {

                    @Override
                    public void onRoutingFailure() {
                        callback.onFailure(routingFailureErrorMessage);
                    }

                    @Override
                    public void onRoutingStart() {}

                    @Override
                    public void onRoutingSuccess(PolylineOptions polylineOptions, com.directions.route.Route route) {
                        callback.onSuccess(new DirectionsResult(polylineOptions, route));
                    }

                    @Override
                    public void onRoutingCancelled() {}
                })
                .build();
        routing.execute();
    }

    public static class DirectionsResult {

        private PolylineOptions polylineOptions;
        private Route route;

        public DirectionsResult(PolylineOptions polylineOptions, Route route) {
            this.polylineOptions = polylineOptions;
            this.route = route;
        }

        public PolylineOptions getPolylineOptions() {
            return polylineOptions;
        }

        public Route getRoute() {
            return route;
        }
    }
}
