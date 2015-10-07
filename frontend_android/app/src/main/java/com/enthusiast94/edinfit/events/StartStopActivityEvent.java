package com.enthusiast94.edinfit.events;

import com.enthusiast94.edinfit.models.Stop;

/**
 * Created by manas on 07-10-2015.
 */
public class StartStopActivityEvent {

    private Stop stop;

    public StartStopActivityEvent(Stop stop) {
        this.stop = stop;
    }

    public Stop getStop() {
        return stop;
    }
}
