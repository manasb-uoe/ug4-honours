package com.enthusiast94.edinfit;

import android.app.Application;
import android.content.Context;

import com.activeandroid.ActiveAndroid;
import com.enthusiast94.edinfit.network.BaseService;
import com.enthusiast94.edinfit.network.DirectionsService;
import com.enthusiast94.edinfit.network.JourneyPlannerService;
import com.enthusiast94.edinfit.network.LiveBusService;
import com.enthusiast94.edinfit.network.ServiceService;
import com.enthusiast94.edinfit.network.StopService;
import com.enthusiast94.edinfit.network.UserService;
import com.enthusiast94.edinfit.network.WaitOrWalkService;
import com.enthusiast94.edinfit.utils.DisruptionAlarmReceiver;
import com.enthusiast94.edinfit.utils.PreferencesManager;

/**
 * Created by manas on 26-09-2015.
 */
public class App extends Application {

    public static final String TAG = App.class.getSimpleName();
    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();

        context = this;

        /* Init services */
        BaseService.init(this);
        PreferencesManager.init(this);
        DirectionsService.init(this);
        UserService.init(this);
        StopService.init(this);
        ServiceService.init(this);
        LiveBusService.init(this);
        WaitOrWalkService.init(this);
        JourneyPlannerService.init(this);

        /* Init ORM */
        ActiveAndroid.initialize(this);

        /* Setup disruption notification alarm */
        DisruptionAlarmReceiver.setAlarm(context);
    }

    public static Context getAppContext() {
        return context;
    }
}
