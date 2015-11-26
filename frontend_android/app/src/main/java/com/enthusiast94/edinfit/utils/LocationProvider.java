package com.enthusiast94.edinfit.utils;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;

import com.enthusiast94.edinfit.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

/**
 * Created by manas on 23-11-2015.
 */
public class LocationProvider implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private GoogleApiClient googleApiClient;
    private LastKnowLocationCallback lastKnowLocationCallback;
    private LocationUpdateCallback locationUpdateCallback;
    private long updateInterval;
    private boolean isConnected;
    private String locationFetchErrorMessage;

    public interface LastKnowLocationCallback {
        void onLastKnownLocationSuccess(Location location);
        void onLastKnownLocationFailure(String error);
    }

    public interface LocationUpdateCallback {
        void onLocationUpdateSuccess(Location location);
        void onLocationUpdateFailure(String error);
    }

    public LocationProvider(Context context, LastKnowLocationCallback lastKnowLocationCallback) {
        this(context, lastKnowLocationCallback, null, 0);
    }

    public LocationProvider(Context context, LocationUpdateCallback locationUpdateCallback,
                            long updateInterval) {
        this(context, null, locationUpdateCallback, updateInterval);
    }

    public LocationProvider(Context context, LastKnowLocationCallback lastKnowLocationCallback,
                            LocationUpdateCallback locationUpdateCallback, long updateInterval) {
        this.lastKnowLocationCallback = lastKnowLocationCallback;
        this.locationUpdateCallback = locationUpdateCallback;
        this.updateInterval = updateInterval;

        locationFetchErrorMessage = context.getString(R.string.error_could_not_fetch_location);

        googleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();
    }

    public void connect() {
        googleApiClient.connect();
    }

    public void disconnect() {
        if (googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
    }

    public void requestLastKnownLocation() {
        if (lastKnowLocationCallback != null) {
            if (isConnected) {
                Location location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);

                if (location != null) {
                    lastKnowLocationCallback.onLastKnownLocationSuccess(location);
                } else {
                    lastKnowLocationCallback.onLastKnownLocationFailure(locationFetchErrorMessage);
                }
            } else {
                lastKnowLocationCallback.onLastKnownLocationFailure(locationFetchErrorMessage);
            }
        }
    }

    public void startLocationUpdates() {
        if (!isConnected) {
            if (locationUpdateCallback != null) {
                locationUpdateCallback.onLocationUpdateFailure(locationFetchErrorMessage);
            }

            return;
        }

        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(updateInterval);
        locationRequest.setFastestInterval(updateInterval);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationServices.FusedLocationApi.requestLocationUpdates(
                googleApiClient, locationRequest, this);
    }

    public void stopLocationUpdates() {
        if (isConnected) {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        isConnected = true;

        if (lastKnowLocationCallback != null) {
            Location location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);

            if (location != null) {
                lastKnowLocationCallback.onLastKnownLocationSuccess(location);
            } else {
                lastKnowLocationCallback.onLastKnownLocationFailure(locationFetchErrorMessage);
            }
        }

        if (locationUpdateCallback != null) {
            startLocationUpdates();
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (lastKnowLocationCallback != null) {
            lastKnowLocationCallback.onLastKnownLocationFailure(locationFetchErrorMessage);
        }

        if (locationUpdateCallback != null) {
            locationUpdateCallback.onLocationUpdateFailure(locationFetchErrorMessage);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {}

    @Override
    public void onLocationChanged(Location location) {
        if (locationUpdateCallback != null) {
            if (location != null) {
                locationUpdateCallback.onLocationUpdateSuccess(location);
            } else {
                locationUpdateCallback.onLocationUpdateFailure(locationFetchErrorMessage);
            }
        }
    }
}
