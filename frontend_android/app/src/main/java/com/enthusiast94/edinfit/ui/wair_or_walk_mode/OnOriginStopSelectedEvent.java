package com.enthusiast94.edinfit.ui.wair_or_walk_mode;

import com.enthusiast94.edinfit.models.Stop;

/**
 * Created by manas on 19-10-2015.
 */
public class OnOriginStopSelectedEvent {

    private Stop originStop;

    public OnOriginStopSelectedEvent(Stop originStop) {
        this.originStop = originStop;
    }

    public Stop getOriginStop() {
        return originStop;
    }
}
