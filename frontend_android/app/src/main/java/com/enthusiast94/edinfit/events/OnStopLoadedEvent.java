package com.enthusiast94.edinfit.events;

import com.enthusiast94.edinfit.models.Stop;

/**
 * Created by manas on 06-10-2015.
 */
public class OnStopLoadedEvent {

    private Stop stop;

    public OnStopLoadedEvent(Stop stop) {
        this.stop = stop;
    }

    public Stop getStop() {
        return stop;
    }
}
