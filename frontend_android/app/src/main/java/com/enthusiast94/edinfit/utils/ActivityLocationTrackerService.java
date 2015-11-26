package com.enthusiast94.edinfit.utils;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.enthusiast94.edinfit.models.Activity;
import com.enthusiast94.edinfit.network.ActivityService;
import com.enthusiast94.edinfit.network.BaseService;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by manas on 25-11-2015.
 */
public class ActivityLocationTrackerService extends Service
        implements LocationProvider.LocationUpdateCallback {

    private static final String TAG = ActivityLocationTrackerService.class.getSimpleName();
    public static final String EXTRA_ACTIVITY_TYPE = "activityType";
    private static final int UPDATE_INTERVAL = 2000;
    private static final long MINIMUM_ACTIVITY_DURATION = 60 * 2 * 1000;

    private LocationProvider locationProvider;
    private ActivityService activityService;
    private Activity currentActivity;
    private double totalDistance;
    private double speedSum;

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
        currentActivity.setAverageSpeed(speedSum / Float.valueOf(currentActivity.getPoints().size()));
        currentActivity.setDistance(totalDistance);

        if ((currentActivity.getEnd() - currentActivity.getStart()) > MINIMUM_ACTIVITY_DURATION) {
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
                System.currentTimeMillis(), 0, new ArrayList<Activity.Point>(), 0, 0);

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onLocationUpdateSuccess(Location location) {
        Log.i(TAG, "LOCATION UPDATE");

        if (currentActivity != null) {
            // add new point to list of points, compute distance covered so far and the total speed
            // so far (which will be later converted into an average).
            Activity.Point point = new Activity.Point(location.getLatitude(),
                    location.getLongitude(), location.getTime(), location.getSpeed());

            List<Activity.Point> points = currentActivity.getPoints();
            points.add(point);

            if (points.size() > 1) {
                Activity.Point prevPoint = points.get(points.size() - 2);
                totalDistance += Helpers.getDistanceBetweenPoints(point.getLatitude(),
                        point.getLongitude(), prevPoint.getLatitude(), prevPoint.getLongitude());
            }

            speedSum += point.getSpeed();
        }
    }

    @Override
    public void onLocationUpdateFailure(String error) {
        Log.e(TAG, error);
    }
}
