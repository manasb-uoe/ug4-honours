package com.enthusiast94.edinfit.ui.wair_or_walk_mode;

import com.enthusiast94.edinfit.models.Route;
import com.enthusiast94.edinfit.models.Stop;

/**
 * Created by manas on 25-10-2015.
 */
public class OnDestinationStopSelectedEvent {

    private Stop destinationStop;
    private Route route;

    public OnDestinationStopSelectedEvent(Stop originStop, Route route) {
        this.destinationStop = originStop;
        this.route = route;
    }

    public Stop getDestinationStop() {
        return destinationStop;
    }

    public Route getRoute() {
        return route;
    }
}
