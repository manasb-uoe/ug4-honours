package com.enthusiast94.edinfit.events;

/**
 * Created by manas on 22-10-2015.
 */
public class OnServiceSelectedEvent {

    private String serviceName;

    public OnServiceSelectedEvent(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getSelectedServiceName() {
        return serviceName;
    }
}
