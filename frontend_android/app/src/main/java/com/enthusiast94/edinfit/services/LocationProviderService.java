package com.enthusiast94.edinfit.services;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.enthusiast94.edinfit.R;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.List;

/**
 * Created by manas on 13-10-2015.
 */
public class LocationProviderService implements GoogleApiClient.ConnectionCallbacks, LocationListener {

    public static final String TAG = LocationProviderService.class.getSimpleName();
    private static LocationProviderService instance;
    private Context context;
    private GoogleApiClient googleApiClient;
    private Geocoder geocoder;
    private Handler handler;
    private boolean isGoogleApiClientAvailable;
    private LocationUpdateCallback locationUpdateCallback;


    public interface LastKnownLocationCallback {
        void onLocationSuccess(LatLng latLng, String placeName);

        void onLocationFailure(String error);
    }

    public interface LocationUpdateCallback {
        void onLocationChanged(LatLng latLng);

        void onLocationFailure(String error);
    }

    private LocationProviderService(Context context) {
        this.context = context;

        handler = new Handler();

        googleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();

        googleApiClient.connect();

        geocoder = new Geocoder(context);
    }

    public static void init(Context context) {
        if (instance != null) {
            throw new IllegalStateException(TAG + " has already been initialized.");
        }

        instance = new LocationProviderService(context);
    }

    public static LocationProviderService getInstance() {
        if (instance == null) {
            throw new IllegalStateException(TAG + " has not been initialized yet.");
        }

        return instance;
    }

//    public void disconnect() {
//        if (googleApiClient.isConnected()) {
//            googleApiClient.disconnect();
//        }
//    }


    private String getPlaceName(LatLng latLng) {
        List<Address> list = null;

        try {
            list = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }

        if (list != null && list.size() > 0) {
            return list.get(0).getThoroughfare();
        }

        return null;
    }

    public void requestLastKnownLocationInfo(boolean shouldIncludePlaceName, LastKnownLocationCallback lastKnownLocationCallback) {
        if (isGoogleApiClientAvailable) {
            Thread thread = new Thread(new LocationInfoFetcherRunnable(shouldIncludePlaceName, lastKnownLocationCallback));
            thread.start();
        } else {
            lastKnownLocationCallback.onLocationFailure(context.getString(R.string.error_could_not_fetch_location));
        }
    }

    public void startLocationUpdates(long updateInterval, LocationUpdateCallback callback) {
        this.locationUpdateCallback = callback;

        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(updateInterval);
        locationRequest.setFastestInterval(updateInterval);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (isGoogleApiClientAvailable) {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    googleApiClient, locationRequest, this);
        } else {
            callback.onLocationFailure(context.getString(R.string.error_could_not_fetch_location));
        }
    }

    public void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
        locationUpdateCallback = null;
    }

    @Override
    public void onConnected(Bundle bundle) {
        isGoogleApiClientAvailable = true;
    }

    @Override
    public void onLocationChanged(Location location) {
        if (locationUpdateCallback != null) {
            locationUpdateCallback.onLocationChanged(
                    new LatLng(location.getLatitude(), location.getLongitude()));
        }
    }

    @Override
    public void onConnectionSuspended(int i) {}

    private class LocationInfoFetcherRunnable implements Runnable {

        private boolean shouldIncludePlaceName;
        private LastKnownLocationCallback lastKnownLocationCallback;

        public LocationInfoFetcherRunnable(boolean shouldIncludePlaceName, LastKnownLocationCallback lastKnownLocationCallback) {
            this.shouldIncludePlaceName = shouldIncludePlaceName;
            this.lastKnownLocationCallback = lastKnownLocationCallback;
        }

        @Override
        public void run() {
            Location location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);

            LatLng latLng = null;
            String placeName = null;

            if (location != null) {
                latLng = new LatLng(location.getLatitude(), location.getLongitude());
                if (shouldIncludePlaceName) {
                    placeName = getPlaceName(latLng);
                }
            }

            handler.post(new CallbackInvokerRunnable(latLng, placeName, lastKnownLocationCallback));
        }
    }

    private class CallbackInvokerRunnable implements Runnable {

        private LatLng latLng;
        private String placeName;
        private LastKnownLocationCallback lastKnownLocationCallback;

        public CallbackInvokerRunnable(LatLng latLng, String placeName, LastKnownLocationCallback lastKnownLocationCallback) {
            this.latLng = latLng;
            this.placeName = placeName;
            this.lastKnownLocationCallback = lastKnownLocationCallback;
        }

        @Override
        public void run() {
            if (latLng != null) {
                lastKnownLocationCallback.onLocationSuccess(latLng, placeName);
            } else {
                lastKnownLocationCallback.onLocationFailure(context.getString(R.string.error_could_not_fetch_location));
            }
        }
    }
}
