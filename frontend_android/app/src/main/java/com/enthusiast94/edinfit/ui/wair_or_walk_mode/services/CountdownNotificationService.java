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
import com.enthusiast94.edinfit.ui.wair_or_walk_mode.activities.ResultActivity;
import com.enthusiast94.edinfit.ui.wair_or_walk_mode.fragments.ResultFragment;
import com.enthusiast94.edinfit.utils.Helpers;

/**
 * Created by manas on 08-11-2015.
 */
public class CountdownNotificationService extends android.app.Service {

    public static final String EXTRA_WAIT_OR_WALK_RESULT = "waitOrWalkResult";
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
        ResultFragment.WaitOrWalkResult waitOrWalkResult =
                intent.getParcelableExtra(EXTRA_WAIT_OR_WALK_RESULT);

        startCountdownNotification(waitOrWalkResult);

        return super.onStartCommand(intent, flags, startId);
    }

    private void startCountdownNotification(final ResultFragment.WaitOrWalkResult waitOrWalkResult) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer = new TimeRemainingCountdownTimer(waitOrWalkResult, 1000);
        countDownTimer.start();
    }

    private class TimeRemainingCountdownTimer extends CountDownTimer {

        private ResultFragment.WaitOrWalkResult waitOrWalkResult;

        public TimeRemainingCountdownTimer(ResultFragment.WaitOrWalkResult waitOrWalkResult,
                                           long countDownInterval) {
            super(waitOrWalkResult.getRemainingTimeMillis(), countDownInterval);

            this.waitOrWalkResult = waitOrWalkResult;
        }

        @Override
        public void onTick(long millisUntilFinished) {
            String contentTitle = waitOrWalkResult.getType() == ResultFragment.WaitOrWalkResultType.WALK
                    ? "Walk to " + waitOrWalkResult.getStop().getName() + " by " + waitOrWalkResult.getUpcomingDeparture().getTime()
                    : "Wait at " + waitOrWalkResult.getStop().getName() + " until " + waitOrWalkResult.getUpcomingDeparture().getTime();

            Intent stopIntent = new Intent(ACTION_STOP);
            PendingIntent stopPendingIntent = PendingIntent.getBroadcast(
                    CountdownNotificationService.this,
                    REQUEST_CODE_STOP,
                    stopIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
            );

            Intent directionsIntent =
                    new Intent(CountdownNotificationService.this, ResultActivity.class);
//            directionsIntent.putExtra(ResultActivity.EXTRA_WAIT_OR_WALK_RESULT, waitOrWalkResult);
            directionsIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            PendingIntent directionsPendingIntent = PendingIntent.getActivity(
                    CountdownNotificationService.this,
                    REQUEST_CODE_DIRECTIONS,
                    directionsIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
            );

            Notification notification = new Notification.Builder(CountdownNotificationService.this)
                    .setContentTitle(contentTitle)
                    .setContentText("Time remaining: " + Helpers.humanizeDurationInMillis(millisUntilFinished))
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setOngoing(true)
                    .addAction(R.drawable.ic_action_av_stop, "Stop", stopPendingIntent)
                    .addAction(R.drawable.ic_action_maps_directions, "Directions", directionsPendingIntent)
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
