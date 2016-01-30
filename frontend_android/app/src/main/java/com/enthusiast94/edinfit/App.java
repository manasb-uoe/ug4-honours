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

        // init services
        BaseService.init(this);
        DirectionsService.init(this);
        UserService.init(this);
        StopService.init(this);
        ServiceService.init(this);
        LiveBusService.init(this);
        WaitOrWalkService.init(this);
        JourneyPlannerService.init(this);

        // init orm
        ActiveAndroid.initialize(this);
    }

    public static Context getAppContext() {
        return context;
    }
}
