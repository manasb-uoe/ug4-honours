package com.enthusiast94.edinfit.ui.wait_or_walk_mode.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import com.enthusiast94.edinfit.models.WaitOrWalkSuggestion;
import com.enthusiast94.edinfit.ui.wait_or_walk_mode.activities.SuggestionsActivity;
import com.enthusiast94.edinfit.ui.wait_or_walk_mode.events.OnCountdownFinishedOrCancelledEvent;
import com.enthusiast94.edinfit.ui.wait_or_walk_mode.events.OnCountdownTickEvent;
import com.enthusiast94.edinfit.utils.ActivityLocationTrackerService;
import com.enthusiast94.edinfit.utils.Helpers;
import com.enthusiast94.edinfit.utils.LocationProvider;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

import de.greenrobot.event.EventBus;

/**
 * Created by manas on 08-11-2015.
 */
public class CountdownNotificationService extends android.app.Service {

    public static final String TAG = CountdownNotificationService.class.getSimpleName();
    public static final String EXTRA_WAIT_OR_WALK_SELECTED_SUGGESTION = "waitOrWalkSelectedSuggestion";
    public static final String EXTRA_WAIT_OR_WALK_ALL_SUGGESTIONS = "waitOrWalkAllSuggestion";
    private static final int NOTIFICATION_ID_COUNTDOWN = 0;
    private static final int NOTIFICATION_ID_SUCCESS = 1;
    private static final int NOTIFICATION_ID_FAILURE = 2;
    private static final int REQUEST_CODE_STOP = 0;
    private static final int REQUEST_CODE_DIRECTIONS = 1;
    private static final String ACTION_STOP = "stop";
    private static final int LOCATION_UPDATE_INTERVAL = 10000;

    private CountDownTimer countDownTimer;
    private NotificationManager notificationManager;
    private NotificationBroadcastReceiver receiver;
    private WaitOrWalkSuggestion selectedWaitOrWalkSuggestion;
    private ArrayList<WaitOrWalkSuggestion> waitOrWalkSuggestions;
    private LocationProvider locationProvider;
    private Intent activityLocationTrackerServiceIntent;

    @Override
    public void onCreate() {
        super.onCreate();

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        locationProvider = new LocationProvider(this, new CheckIfUserReachedDestinationOnLocationUpdateCallback(),
                1000);
        locationProvider.connect();

        receiver = new NotificationBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_STOP);
        registerReceiver(receiver, filter);

        activityLocationTrackerServiceIntent = new Intent(this, ActivityLocationTrackerService.class);
        activityLocationTrackerServiceIntent.putExtra(
                ActivityLocationTrackerService.EXTRA_ACTIVITY_TYPE, Activity.Type.WAIT_OR_WALK.getValue());
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

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        selectedWaitOrWalkSuggestion = intent.getParcelableExtra(EXTRA_WAIT_OR_WALK_SELECTED_SUGGESTION);
        waitOrWalkSuggestions = intent.getParcelableArrayListExtra(EXTRA_WAIT_OR_WALK_ALL_SUGGESTIONS);

        startCountdownNotification();

        if (selectedWaitOrWalkSuggestion.getType() == WaitOrWalkSuggestion.WaitOrWalkSuggestionType.WALK) {
            locationProvider.stopLocationUpdates();
            locationProvider.startLocationUpdates();
        }


        startService(activityLocationTrackerServiceIntent);

        return super.onStartCommand(intent, flags, startId);
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
            super(Helpers.getRemainingTimeMillisFromNow(
                    selectedWaitOrWalkSuggestion.getUpcomingDeparture().getTime()), countDownInterval);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            String contentTitle = selectedWaitOrWalkSuggestion.getType() ==
                    WaitOrWalkSuggestion.WaitOrWalkSuggestionType.WALK
                    ? String.format(getString(R.string.label_walk_to_stop_by_time),
                    String.format(getString(R.string.label_stop_name_with_direction),
                            selectedWaitOrWalkSuggestion.getStop().getName(),
                            selectedWaitOrWalkSuggestion.getStop().getDirection()),
                    selectedWaitOrWalkSuggestion.getUpcomingDeparture().getTime())
                    : String.format(getString(R.string.label_walk_to_stop_by_time),
                    String.format(getString(R.string.label_stop_name_with_direction),
                            selectedWaitOrWalkSuggestion.getStop().getName(),
                            selectedWaitOrWalkSuggestion.getStop().getDirection()),
                    selectedWaitOrWalkSuggestion.getUpcomingDeparture().getTime());

            Intent stopIntent = new Intent(ACTION_STOP);
            PendingIntent stopPendingIntent = PendingIntent.getBroadcast(
                    CountdownNotificationService.this,
                    REQUEST_CODE_STOP,
                    stopIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
            );

            Intent directionsIntent =
                    new Intent(CountdownNotificationService.this, SuggestionsActivity.class);
            directionsIntent.putParcelableArrayListExtra(SuggestionsActivity.EXTRA_WAIT_OR_WALK_ALL_SUGGESTIONS, waitOrWalkSuggestions);
            directionsIntent.putExtra(SuggestionsActivity.EXTRA_WAIT_OR_WALK_SELECTED_SUGGESTION, selectedWaitOrWalkSuggestion);
            PendingIntent directionsPendingIntent = PendingIntent.getActivity(
                    CountdownNotificationService.this,
                    REQUEST_CODE_DIRECTIONS,
                    directionsIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
            );

            String humanizedRemainingTime = Helpers.humanizeDurationInMillis(millisUntilFinished);

            Notification notification = new Notification.Builder(CountdownNotificationService.this)
                    .setContentTitle(String.format(getString(R.string.label_time_remaining), humanizedRemainingTime))
                    .setContentText(contentTitle)
                    .setSmallIcon(R.drawable.ic_stat_maps_directions_bus)
                    .setOngoing(true)
                    .addAction(R.drawable.ic_action_av_stop, getString(R.string.label_stop), stopPendingIntent)
                    .addAction(R.drawable.ic_action_maps_directions, getString(R.string.label_directions), directionsPendingIntent)
                    .setContentIntent(directionsPendingIntent)
                    .build();

            notificationManager.notify(NOTIFICATION_ID_COUNTDOWN, notification);

            EventBus.getDefault().post(new OnCountdownTickEvent(humanizedRemainingTime));
        }

        @Override
        public void onFinish() {
            EventBus.getDefault().post(new OnCountdownTickEvent(getString(R.string.label_none)));

            if (selectedWaitOrWalkSuggestion.getType() == WaitOrWalkSuggestion.WaitOrWalkSuggestionType.WALK) {
                showFailureNotification();
            }

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
            Log.d(TAG, "onLocationUpdateSuccess");
            if (selectedWaitOrWalkSuggestion.getType() != WaitOrWalkSuggestion.WaitOrWalkSuggestionType.WALK) {
                return;
            }

            LatLng stopLatLng = selectedWaitOrWalkSuggestion.getStop().getPosition();

            Log.d(TAG, "distance to dest: " + Helpers.getDistanceBetweenPoints(location.getLatitude(), location.getLongitude(),
                    stopLatLng.latitude, stopLatLng.longitude));

            if (Helpers.getDistanceBetweenPoints(location.getLatitude(), location.getLongitude(),
                    stopLatLng.latitude, stopLatLng.longitude) <= 20 /* 20 meters */) {

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
