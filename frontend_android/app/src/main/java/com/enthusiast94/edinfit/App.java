package com.enthusiast94.edinfit;

import android.app.Application;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.enthusiast94.edinfit.network.DirectionsService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.util.List;

/**
 * Created by manas on 26-09-2015.
 */
public class App extends Application implements GoogleApiClient.OnConnectionFailedListener {

    public static final String TAG = App.class.getSimpleName();
    private static Context context;
    private static GoogleApiClient googleApiClient;
    private static Geocoder geocoder;

    @Override
    public void onCreate() {
        super.onCreate();

        context = this;

        buildGoogleApiClient();
        googleApiClient.connect();

        geocoder = new Geocoder(this);

        // init services
        DirectionsService.init(this);
    }

    public static Context getAppContext() {
        return context;
    }

    public static Location getLastKnownUserLocation() {
        return LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
    }

    public static String getLastKnownUserLocationName() {
        Location location = getLastKnownUserLocation();

        List<Address> list = null;
        try {
            list = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
        } catch (IOException e) {
            Toast.makeText(getAppContext(), getAppContext().getString(
                    R.string.error_could_not_geocode_location), Toast.LENGTH_LONG).show();
        }

        if (list != null && list.size() > 0) {
            return list.get(0).getThoroughfare();
        }

        return null;
    }

    private synchronized void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, connectionResult.toString());
    }
}
