package com.enthusiast94.edinfit.services;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.List;

/**
 * Created by manas on 11-10-2015.
 */
public class LocationService {

    public static final String TAG = LocationService.class.getSimpleName();
    private static LocationService instance;
    private GoogleApiClient googleApiClient;
    private Geocoder geocoder;
    private boolean isGoogleApiClientAvailable = true;

    private LocationService(Context context) {
        googleApiClient = new GoogleApiClient.Builder(context)
                .addApi(LocationServices.API)
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {

                    @Override
                    public void onConnectionFailed(ConnectionResult connectionResult) {
                        isGoogleApiClientAvailable = false;
                    }
                })
                .build();
        googleApiClient.connect();

        geocoder = new Geocoder(context);
    }

    public static void init(Context context) {
        if (instance != null) {
            throw new IllegalStateException(TAG + " has already been initialized.");
        }

        instance = new LocationService(context);
    }

    public static LocationService getInstance() {
        if (instance == null) {
            throw new IllegalStateException(TAG + " has not been initialized yet.");
        }

        return instance;
    }

    public LatLng getLastKnownUserLocation() {
        if (!isGoogleApiClientAvailable) return null;

        Location location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        return new LatLng(location.getLatitude(), location.getLongitude());
    }

    public String getLastKnownUserLocationName() {
        if (!isGoogleApiClientAvailable) return null;

        LatLng latLng = getLastKnownUserLocation();

        List<Address> list = null;
        try {
            list = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (list != null && list.size() > 0) {
            return list.get(0).getThoroughfare();
        }

        return null;
    }
}
