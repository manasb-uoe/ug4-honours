package com.enthusiast94.edinfit.utils;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models_2.Activity;

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
    private static final long MINIMUM_ACTIVITY_DURATION = 60 * 2;

    private LocationProvider locationProvider;
    private Activity currentActivity;
    private List<Activity.Point> points;
    private ReverseGeocoder reverseGeocoder;
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

        locationProvider = new LocationProvider(this, this, UPDATE_INTERVAL);
        locationProvider.connect();

        reverseGeocoder = new ReverseGeocoder(this);
    }

    @Override
    public void onDestroy() {
        locationProvider.stopLocationUpdates();
        locationProvider.disconnect();

        currentActivity.setEnd(System.currentTimeMillis());
        currentActivity.setPoints(points);

        Log.d(TAG, "onDestroy: " + currentActivity.getPoints().size());

        if ((currentActivity.getEnd() - currentActivity.getStart()) > MINIMUM_ACTIVITY_DURATION &&
                currentActivity.getPoints().size() > 1) {

            currentActivity.setAverageSpeed(speedSum / Float.valueOf(currentActivity.getPoints().size()));
            currentActivity.setDistance(totalDistance);

            // fetch place names for start and end points in order to set activity description
            Activity.Point startPoint = currentActivity.getPoints().get(0);
            reverseGeocoder.getPlaceName(startPoint.getLatitude(), startPoint.getLongitude(),
                    new ReverseGeocoder.ReverseGeocodeCallback() {

                        @Override
                        public void onSuccess(final String startPlaceName) {
                            Activity.Point endPoint =
                                    currentActivity.getPoints().get(currentActivity.getPoints().size() - 1);
                            reverseGeocoder.getPlaceName(endPoint.getLatitude(), endPoint.getLongitude(),

                                    new ReverseGeocoder.ReverseGeocodeCallback() {
                                        @Override
                                        public void onSuccess(String endPlaceName) {
                                            currentActivity.setDescription(String.format(getString(
                                                    R.string.label_activity_description_format), startPlaceName, endPlaceName));
                                            currentActivity.save();

                                            Log.i(TAG, "New activity successfully sent to server");
                                        }

                                        @Override
                                        public void onFailure(String error) {
                                            Log.e(TAG, error);
                                        }
                                    });
                        }

                        @Override
                        public void onFailure(String error) {
                            Log.e(TAG, error);
                        }
                    });
        }

        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String type = intent.getStringExtra(EXTRA_ACTIVITY_TYPE);

        currentActivity = new Activity(null, Activity.Type.getTypeByValue(type),
                System.currentTimeMillis(), 0, new ArrayList<Activity.Point>(), 0, 0);

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onLocationUpdateSuccess(Location location) {
        if (currentActivity != null) {
            // add new point to list of points, compute distance covered so far and the total speed
            // so far (which will be later converted into an average).
            Activity.Point point = new Activity.Point(location.getLatitude(),
                    location.getLongitude(), location.getTime(), location.getSpeed());

            if (points == null ){
                points = new ArrayList<>();
            }
            points.add(point);

            if (points.size() > 1) {
                Activity.Point prevPoint = points.get(points.size() - 2);
                totalDistance += Helpers.getDistanceBetweenPoints(point.getLatitude(),
                        point.getLongitude(), prevPoint.getLatitude(), prevPoint.getLongitude());
            }

            speedSum += point.getSpeed();

            Log.i(TAG, "Distance covered so far: " + totalDistance);
        }
    }

    @Override
    public void onLocationUpdateFailure(String error) {
        Log.e(TAG, error);
    }
}
