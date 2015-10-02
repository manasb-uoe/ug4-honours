package com.enthusiast94.edinfit;

import android.app.Application;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

/**
 * Created by manas on 26-09-2015.
 */
public class App extends Application implements GoogleApiClient.OnConnectionFailedListener {

    public static final String TAG = App.class.getSimpleName();
    private static Context context;
    private static GoogleApiClient googleApiClient;

    @Override
    public void onCreate() {
        super.onCreate();

        context = this;

        buildGoogleApiClient();

        googleApiClient.connect();
    }

    public static Context getAppContext() {
        return context;
    }

    public static GoogleApiClient getGoogleApiClient() {
        return googleApiClient;
    }

    private synchronized void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, connectionResult.getErrorMessage());
    }
}
