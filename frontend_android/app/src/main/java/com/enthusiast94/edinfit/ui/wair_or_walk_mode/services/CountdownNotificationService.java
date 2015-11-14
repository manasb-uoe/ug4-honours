package com.enthusiast94.edinfit.ui.wair_or_walk_mode.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.services.WaitOrWalkService;
import com.enthusiast94.edinfit.ui.wair_or_walk_mode.activities.ResultActivity;
import com.enthusiast94.edinfit.utils.Helpers;

/**
 * Created by manas on 08-11-2015.
 */
public class CountdownNotificationService extends android.app.Service {

    public static final String EXTRA_WAIT_OR_WALK_RESULT = "waitOrWalkSuggestion";
    private static final int NOTIFICATION_ID_COUNTDOWN = 0;
    private static final int REQUEST_CODE_STOP = 0;
    private static final int REQUEST_CODE_DIRECTIONS = 1;
    private static final String ACTION_STOP = "stop";

    private CountDownTimer countDownTimer;
    private NotificationManager notificationManager;
    private NotificationBroadcastReceiver receiver;

    @Override
    public void onCreate() {
        super.onCreate();

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        receiver = new NotificationBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_STOP);
        registerReceiver(receiver, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterReceiver(receiver);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // retrieve wait or walk result from intent and build notification accordingly
        WaitOrWalkService.WaitOrWalkSuggestion waitOrWalkSuggestion =
                intent.getParcelableExtra(EXTRA_WAIT_OR_WALK_RESULT);

        startCountdownNotification(waitOrWalkSuggestion);

        return super.onStartCommand(intent, flags, startId);
    }

    private void startCountdownNotification(final WaitOrWalkService.WaitOrWalkSuggestion waitOrWalkSuggestion) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer = new TimeRemainingCountdownTimer(waitOrWalkSuggestion, 1000);
        countDownTimer.start();
    }

    private class TimeRemainingCountdownTimer extends CountDownTimer {

        private WaitOrWalkService.WaitOrWalkSuggestion waitOrWalkSuggestion;

        public TimeRemainingCountdownTimer(WaitOrWalkService.WaitOrWalkSuggestion waitOrWalkSuggestion,
                                           long countDownInterval) {
            super(waitOrWalkSuggestion.getRemainingTimeMillis(), countDownInterval);

            this.waitOrWalkSuggestion = waitOrWalkSuggestion;
        }

        @Override
        public void onTick(long millisUntilFinished) {
            String contentTitle = waitOrWalkSuggestion.getType() == WaitOrWalkService.WaitOrWalkSuggestionType.WALK
                    ? String.format(getString(R.string.label_walk_to_stop_by_time),
                    waitOrWalkSuggestion.getStop().getName(), waitOrWalkSuggestion.getUpcomingDeparture().getTime())
                    : String.format(getString(R.string.label_walk_to_stop_by_time),
                    waitOrWalkSuggestion.getStop().getName(), waitOrWalkSuggestion.getUpcomingDeparture().getTime());

            Intent stopIntent = new Intent(ACTION_STOP);
            PendingIntent stopPendingIntent = PendingIntent.getBroadcast(
                    CountdownNotificationService.this,
                    REQUEST_CODE_STOP,
                    stopIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
            );

            Intent directionsIntent =
                    new Intent(CountdownNotificationService.this, ResultActivity.class);
            directionsIntent.putExtra(ResultActivity.EXTRA_WAIT_OR_WALK_RESULT, waitOrWalkSuggestion);
            PendingIntent directionsPendingIntent = PendingIntent.getActivity(
                    CountdownNotificationService.this,
                    REQUEST_CODE_DIRECTIONS,
                    directionsIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
            );

            Notification notification = new Notification.Builder(CountdownNotificationService.this)
                    .setContentTitle(String.format(getString(R.string.label_time_remaining),
                            Helpers.humanizeDurationInMillis(millisUntilFinished)))
                    .setContentText(contentTitle)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setOngoing(true)
                    .addAction(R.drawable.ic_action_av_stop, getString(R.string.label_stop), stopPendingIntent)
                    .addAction(R.drawable.ic_action_maps_directions, getString(R.string.label_directions), directionsPendingIntent)
                    .setContentIntent(directionsPendingIntent)
                    .build();

            notificationManager.notify(NOTIFICATION_ID_COUNTDOWN, notification);
        }

        @Override
        public void onFinish() {
            notificationManager.cancel(NOTIFICATION_ID_COUNTDOWN);
            stopSelf();
        }
    }

    private class NotificationBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case ACTION_STOP:
                    countDownTimer.cancel();
                    notificationManager.cancel(NOTIFICATION_ID_COUNTDOWN);
                    stopSelf();
                    break;
            }
        }
    }
}
