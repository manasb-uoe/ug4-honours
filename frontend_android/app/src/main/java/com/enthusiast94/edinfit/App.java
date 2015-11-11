package com.enthusiast94.edinfit;

import android.app.Application;
import android.content.Context;

import com.enthusiast94.edinfit.models.LiveBus;
import com.enthusiast94.edinfit.services.DirectionsService;
import com.enthusiast94.edinfit.services.LiveBusService;
import com.enthusiast94.edinfit.services.ServiceService;
import com.enthusiast94.edinfit.services.StopService;
import com.enthusiast94.edinfit.services.UserService;
import com.enthusiast94.edinfit.services.LocationProviderService;
import com.enthusiast94.edinfit.services.WaitOrWalkService;

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
        LocationProviderService.init(this);
        DirectionsService.init(this);
        UserService.init(this);
        StopService.init(this);
        ServiceService.init(this);
        LiveBusService.init(this);
        WaitOrWalkService.init(this);
    }

    public static Context getAppContext() {
        return context;
    }
}
