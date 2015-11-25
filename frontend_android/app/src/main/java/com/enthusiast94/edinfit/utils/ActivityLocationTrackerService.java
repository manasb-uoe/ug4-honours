package com.enthusiast94.edinfit.utils;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.enthusiast94.edinfit.models.Activity;
import com.enthusiast94.edinfit.network.ActivityService;
import com.enthusiast94.edinfit.network.BaseService;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

/**
 * Created by manas on 25-11-2015.
 */
public class ActivityLocationTrackerService extends Service
        implements LocationProvider.LocationUpdateCallback {

    private static final String TAG = ActivityLocationTrackerService.class.getSimpleName();
    public static final String EXTRA_ACTIVITY_TYPE = "activityType";
    private static final int UPDATE_INTERVAL = 2000;

    private LocationProvider locationProvider;
    private ActivityService activityService;
    private Activity currentActivity;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        activityService = ActivityService.getInstance();

        locationProvider = new LocationProvider(this, this, UPDATE_INTERVAL);
        locationProvider.connect();
    }

    @Override
    public void onDestroy() {
        locationProvider.stopLocationUpdates();
        locationProvider.disconnect();

        currentActivity.setEnd(System.currentTimeMillis());

        if ((currentActivity.getEnd() - currentActivity.getStart()) > (60 * 2 * 10)) {
            Log.i(TAG, currentActivity.getType().getValue());
            activityService.addNewActivity(currentActivity, new BaseService.Callback<Void>() {

                @Override
                public void onSuccess(Void data) {
                    Log.i(TAG, "New activity successfully sent to server");
                }

                @Override
                public void onFailure(String message) {
                    Log.e(TAG, message);
                }
            });
        }

        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String type = intent.getStringExtra(EXTRA_ACTIVITY_TYPE);

        currentActivity = new Activity(Activity.Type.getTypeByValue(type),
                System.currentTimeMillis(), 0, new ArrayList<Activity.Point>());

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onLocationUpdateSuccess(LatLng latLng) {
        Log.i(TAG, "LOCATION UPDATE");

        if (currentActivity != null) {
            currentActivity.getPoints().add(new Activity.Point(latLng.latitude, latLng.longitude,
                    System.currentTimeMillis()));
        }
    }

    @Override
    public void onLocationUpdateFailure(String error) {
        Log.e(TAG, error);
    }
}
