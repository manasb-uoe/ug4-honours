package com.enthusiast94.edinfit.utils;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.arasthel.asyncjob.AsyncJob;
import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models.Disruption;
import com.enthusiast94.edinfit.network.BaseService;
import com.enthusiast94.edinfit.network.DisruptionsService;

import java.util.List;

public class DisruptionAlarmReceiver extends BroadcastReceiver {

    private static String ACTION_DISRUPTION_ALARM = "disruptionAlarm";
    private static final String TAG = DisruptionAlarmReceiver.class.getSimpleName();

    public static void setAlarmEnabled(Context context, boolean isEnabled) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent alarmIntent = new Intent(context, DisruptionAlarmReceiver.class);
        alarmIntent.setAction(ACTION_DISRUPTION_ALARM);
        PendingIntent alarmPendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, 0);

        if (isEnabled) {
            manager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(),
                    AlarmManager.INTERVAL_HOUR, alarmPendingIntent);
        } else {
            manager.cancel(alarmPendingIntent);
        }
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (intent.getAction().equals(ACTION_DISRUPTION_ALARM)) {
            new AsyncJob.AsyncJobBuilder<BaseService.Response<List<Disruption>>>()
                    .doInBackground(new AsyncJob.AsyncAction<BaseService.Response<List<Disruption>>>() {
                        @Override
                        public BaseService.Response<List<Disruption>> doAsync() {
                            return DisruptionsService.getInstance().getDisriptions();
                        }
                    })
                    .doWhenFinished(new AsyncJob.AsyncResultAction<BaseService.Response<List<Disruption>>>() {
                        @Override
                        public void onResult(BaseService.Response<List<Disruption>> response) {
                            if (!response.isSuccessfull()) {
                                return;
                            }

                            PreferencesManager preferencesManager = PreferencesManager.getInstance();
                            NotificationManager notificationManager = (NotificationManager)
                                    context.getSystemService(Context.NOTIFICATION_SERVICE);
                            List<Disruption> disruptions = response.getBody();
                            List<Integer> previousDisruptionIds = preferencesManager.getDisruptionIds();

                            for (Disruption disruption : disruptions) {
                                if (!previousDisruptionIds.contains(disruption.getId())) {
                                    Intent viewInBrowserIntent = new Intent(Intent.ACTION_VIEW);
                                    viewInBrowserIntent.setData(Uri.parse(disruption.getWebLink()));
                                    PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, viewInBrowserIntent, 0);

                                    Notification.BigTextStyle bigTextStyle = new Notification.BigTextStyle();
                                    bigTextStyle.bigText(disruption.getSummary());

                                    Notification notification = new Notification.Builder(context)
                                            .setContentTitle(disruption.getType().substring(0, 1).toUpperCase() +
                                                    disruption.getType().substring(1, disruption.getType().length()) +
                                                    " " + disruption.getCategory())
                                            .setContentText(context.getString(R.string.services_affected) +
                                                    " " + disruption.getServicesAffected().toString())
                                            .setSmallIcon(R.mipmap.ic_launcher)
                                            .setContentIntent(pendingIntent)
                                            .setStyle(bigTextStyle)
                                            .setAutoCancel(true)
                                            .build();

                                    notification.defaults = Notification.DEFAULT_ALL;

                                    notificationManager.notify(disruption.getId(), notification);
                                }
                            }

                            preferencesManager.setDisruptionIds(disruptions);
                        }
                    }).create().start();
        }
    }
}
