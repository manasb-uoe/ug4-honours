package com.enthusiast94.edinfit.ui.journey_planner.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models.Activity;
import com.enthusiast94.edinfit.models.Journey;
import com.enthusiast94.edinfit.ui.journey_planner.activities.JourneyDetailsActivity;
import com.enthusiast94.edinfit.ui.journey_planner.events.OnCountdownFinishedOrCancelledEvent;
import com.enthusiast94.edinfit.utils.ActivityLocationTrackerService;
import com.enthusiast94.edinfit.utils.Helpers;
import com.enthusiast94.edinfit.utils.LocationProvider;
import com.enthusiast94.edinfit.utils.ReverseGeocoder;
import com.google.android.gms.maps.model.LatLng;

import java.util.Date;
import java.util.List;

import de.greenrobot.event.EventBus;

/**
 * Created by manas on 04-02-2016.
 */
public class CountdownNotificationService extends Service {

    public static final String TAG = CountdownNotificationService.class.getSimpleName();
    private static final String EXTRA_JOURNEY = "journey";
    private static final int NOTIFICATION_ID_COUNTDOWN = 0;
    private static final int NOTIFICATION_ID_SUCCESS = 1;
    private static final int NOTIFICATION_ID_FAILURE = 2;
    private static final int REQUEST_CODE_STOP = 0;
    private static final int REQUEST_CODE_VIEW_JOURNEY = 1;
    private static final String ACTION_STOP = "stop";

    private Journey journey;
    private Journey.Leg lastLeg;
    private CountDownTimer countDownTimer;
    private NotificationManager notificationManager;
    private NotificationBroadcastReceiver receiver;
    private LocationProvider locationProvider;
    private Intent activityLocationTrackerServiceIntent;
    private ReverseGeocoder reverseGeocoder;
    private String destinationName;

    public static Intent getStartServiceIntent(Context context, Journey journey) {
        Intent intent = new Intent(context, CountdownNotificationService.class);
        intent.putExtra(EXTRA_JOURNEY, journey);
        return intent;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        locationProvider = new LocationProvider(this, new CheckIfUserReachedDestinationOnLocationUpdateCallback(),
                1000);
        locationProvider.connect();

        reverseGeocoder = new ReverseGeocoder(this);

        receiver = new NotificationBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_STOP);
        registerReceiver(receiver, filter);

        activityLocationTrackerServiceIntent = new Intent(this, ActivityLocationTrackerService.class);
        activityLocationTrackerServiceIntent.putExtra(
                ActivityLocationTrackerService.EXTRA_ACTIVITY_TYPE,
                Activity.Type.JOURNEY_PLANNER.getValue());
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        journey = intent.getParcelableExtra(EXTRA_JOURNEY);
        List<Journey.Leg> legs = journey.getLegs();
        lastLeg = legs.get(legs.size()-1);

        final LatLng destinationLatLng = lastLeg.getFinishPoint().getLatLng();
        destinationName = "(" + destinationLatLng.latitude + ", " +
                destinationLatLng.longitude + ")";

        reverseGeocoder.getPlaceName(destinationLatLng.latitude, destinationLatLng.longitude,
                new ReverseGeocoder.ReverseGeocodeCallback() {
            @Override
            public void onSuccess(String placeName) {
                destinationName = placeName;
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, error);
            }
        });

        startCountdownNotification();

        locationProvider.stopLocationUpdates();
        locationProvider.startLocationUpdates();

        startService(activityLocationTrackerServiceIntent);

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterReceiver(receiver);

        countDownTimer.cancel();
        EventBus.getDefault().post(new OnCountdownFinishedOrCancelledEvent());

        notificationManager.cancel(NOTIFICATION_ID_COUNTDOWN);

        locationProvider.stopLocationUpdates();
        locationProvider.disconnect();

        stopService(activityLocationTrackerServiceIntent);
    }

    private void startCountdownNotification() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer = new TimeRemainingCountdownTimer(1000);
        countDownTimer.start();
    }

    public void showSuccessNotification() {
        Notification notification = new Notification.Builder(CountdownNotificationService.this)
                .setContentTitle(getString(R.string.label_success))
                .setContentText(getString(R.string.success_reached_destination_in_time))
                .setSmallIcon(R.drawable.ic_stat_maps_directions_bus)
                .build();

        notification.defaults = Notification.DEFAULT_ALL;

        notificationManager.notify(NOTIFICATION_ID_SUCCESS, notification);
    }

    public void showFailureNotification() {
        Notification notification = new Notification.Builder(CountdownNotificationService.this)
                .setContentTitle(getString(R.string.label_failure))
                .setContentText(getString(R.string.failure_could_not_reach_destination_in_time))
                .setSmallIcon(R.drawable.ic_stat_maps_directions_bus)
                .build();

        notification.defaults = Notification.DEFAULT_ALL;

        notificationManager.notify(NOTIFICATION_ID_FAILURE, notification);
    }

    private class TimeRemainingCountdownTimer extends CountDownTimer {

        public TimeRemainingCountdownTimer(long countDownInterval) {
            super(lastLeg.getFinishPoint().getTimestamp() * 1000 - new Date().getTime() ,
                    countDownInterval);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            String contentTitle = String.format(getString(R.string.journey_to_format), destinationName);

            Intent stopIntent = new Intent(ACTION_STOP);
            PendingIntent stopPendingIntent = PendingIntent.getBroadcast(
                    CountdownNotificationService.this,
                    REQUEST_CODE_STOP,
                    stopIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
            );

            Intent journeyDetailsIntent = JourneyDetailsActivity.getStartActivityIntent(
                    CountdownNotificationService.this, journey);
            PendingIntent journeyDetailsPendingIntent = PendingIntent.getActivity(
                    CountdownNotificationService.this,
                    REQUEST_CODE_VIEW_JOURNEY,
                    journeyDetailsIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
            );

            String humanizedRemainingTime = Helpers.humanizeDurationInMillis(millisUntilFinished);

            Notification notification = new Notification.Builder(CountdownNotificationService.this)
                    .setContentTitle(String.format(getString(R.string.label_time_remaining), humanizedRemainingTime))
                    .setContentText(contentTitle)
                    .setSmallIcon(R.drawable.ic_stat_maps_directions_bus)
                    .setOngoing(true)
                    .addAction(R.drawable.ic_action_av_stop, getString(R.string.label_stop), stopPendingIntent)
                    .setContentIntent(journeyDetailsPendingIntent)
                    .build();

            notificationManager.notify(NOTIFICATION_ID_COUNTDOWN, notification);
        }

        @Override
        public void onFinish() {
            showFailureNotification();

            stopSelf();
        }
    }

    private class NotificationBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case ACTION_STOP:
                    stopSelf();
                    break;
            }
        }
    }

    private class CheckIfUserReachedDestinationOnLocationUpdateCallback
            implements LocationProvider.LocationUpdateCallback {

        @Override
        public void onLocationUpdateSuccess(Location location) {
            LatLng finishLatLng = lastLeg.getFinishPoint().getLatLng();

            Log.d(TAG, "distance to dest: " + Helpers.getDistanceBetweenPoints(location.getLatitude(), location.getLongitude(),
                    finishLatLng.latitude, finishLatLng.longitude));

            if (Helpers.getDistanceBetweenPoints(location.getLatitude(), location.getLongitude(),
                    finishLatLng.latitude, finishLatLng.longitude) <= 20 /* 20 meters */) {

                showSuccessNotification();
                stopSelf();
            }
        }

        @Override
        public void onLocationUpdateFailure(String error) {
            Log.e(TAG, error);
        }
    }
}
