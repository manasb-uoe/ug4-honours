package com.enthusiast94.edinfit.ui.wait_or_walk_mode.events;

import com.enthusiast94.edinfit.models_2.Service;

/**
 * Created by manas on 22-10-2015.
 */
public class OnServiceSelectedEvent {

    private Service service;

    public OnServiceSelectedEvent(Service service) {
        this.service = service;
    }

    public Service getService() {
        return service;
    }
}
