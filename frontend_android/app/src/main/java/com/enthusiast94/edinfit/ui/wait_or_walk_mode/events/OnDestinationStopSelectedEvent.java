package com.enthusiast94.edinfit.ui.wait_or_walk_mode.events;

import com.enthusiast94.edinfit.models.Service;
import com.enthusiast94.edinfit.models.Stop;

/**
 * Created by manas on 25-10-2015.
 */
public class OnDestinationStopSelectedEvent {

    private Stop destinationStop;
    private Service.Route route;

    public OnDestinationStopSelectedEvent(Stop originStop, Service.Route route) {
        this.destinationStop = originStop;
        this.route = route;
    }

    public Stop getDestinationStop() {
        return destinationStop;
    }

    public Service.Route getRoute() {
        return route;
    }
}
