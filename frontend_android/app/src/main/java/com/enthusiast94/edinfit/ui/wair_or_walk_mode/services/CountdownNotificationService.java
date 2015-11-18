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
import com.enthusiast94.edinfit.ui.wair_or_walk_mode.events.OnCountdownTickEvent;
import com.enthusiast94.edinfit.utils.Helpers;

import java.util.ArrayList;

import de.greenrobot.event.EventBus;

/**
 * Created by manas on 08-11-2015.
 */
public class CountdownNotificationService extends android.app.Service {

    public static final String EXTRA_WAIT_OR_WALK_SELECTED_SUGGESTION = "waitOrWalkSelectedSuggestion";
    public static final String EXTRA_WAIT_OR_WALK_ALL_SUGGESTIONS = "waitOrWalkAllSuggestion";
    private static final int NOTIFICATION_ID_COUNTDOWN = 0;
    private static final int REQUEST_CODE_STOP = 0;
    private static final int REQUEST_CODE_DIRECTIONS = 1;
    private static final String ACTION_STOP = "stop";

    private CountDownTimer countDownTimer;
    private NotificationManager notificationManager;
    private NotificationBroadcastReceiver receiver;
    private WaitOrWalkService.WaitOrWalkSuggestion selectedWaitOrWalkSuggestion;
    private ArrayList<WaitOrWalkService.WaitOrWalkSuggestion> waitOrWalkSuggestions;

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
        selectedWaitOrWalkSuggestion = intent.getParcelableExtra(EXTRA_WAIT_OR_WALK_SELECTED_SUGGESTION);
        waitOrWalkSuggestions = intent.getParcelableArrayListExtra(EXTRA_WAIT_OR_WALK_ALL_SUGGESTIONS);

        startCountdownNotification();

        return super.onStartCommand(intent, flags, startId);
    }

    private void startCountdownNotification() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer = new TimeRemainingCountdownTimer(1000);
        countDownTimer.start();
    }

    private class TimeRemainingCountdownTimer extends CountDownTimer {

        public TimeRemainingCountdownTimer(long countDownInterval) {
            super(Helpers.getRemainingTimeMillisFromNow(
                    selectedWaitOrWalkSuggestion.getUpcomingDeparture().getTime()), countDownInterval);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            String contentTitle = selectedWaitOrWalkSuggestion.getType() == WaitOrWalkService.WaitOrWalkSuggestionType.WALK
                    ? String.format(getString(R.string.label_walk_to_stop_by_time),
                    selectedWaitOrWalkSuggestion.getStop().getName(), selectedWaitOrWalkSuggestion.getUpcomingDeparture().getTime())
                    : String.format(getString(R.string.label_walk_to_stop_by_time),
                    selectedWaitOrWalkSuggestion.getStop().getName(), selectedWaitOrWalkSuggestion.getUpcomingDeparture().getTime());

            Intent stopIntent = new Intent(ACTION_STOP);
            PendingIntent stopPendingIntent = PendingIntent.getBroadcast(
                    CountdownNotificationService.this,
                    REQUEST_CODE_STOP,
                    stopIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
            );

            Intent directionsIntent =
                    new Intent(CountdownNotificationService.this, ResultActivity.class);
            directionsIntent.putParcelableArrayListExtra(ResultActivity.EXTRA_WAIT_OR_WALK_ALL_SUGGESTIONS, waitOrWalkSuggestions);
            directionsIntent.putExtra(ResultActivity.EXTRA_WAIT_OR_WALK_SELECTED_SUGGESTION, selectedWaitOrWalkSuggestion);
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
                    .setSmallIcon(R.mipmap.ic_launcher)
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
