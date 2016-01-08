package com.enthusiast94.edinfit.ui.home.events;

import android.os.Bundle;

/**
 * Created by manas on 08-01-2016.
 */
public class OnStopsAndServicesPopulatedEvent {

    private Bundle savedInstanceState;

    public OnStopsAndServicesPopulatedEvent(Bundle savedInstanceState) {
        this.savedInstanceState = savedInstanceState;
    }

    public Bundle getSavedInstanceState() {
        return savedInstanceState;
    }
}
