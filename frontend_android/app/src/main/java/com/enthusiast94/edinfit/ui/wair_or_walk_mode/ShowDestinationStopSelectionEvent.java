package com.enthusiast94.edinfit.ui.wair_or_walk_mode;

/**
 * Created by manas on 25-10-2015.
 */
public class ShowDestinationStopSelectionEvent {

    private String originStopId;
    private String serviceName;

    public ShowDestinationStopSelectionEvent(String originStopId, String serviceName) {
        this.originStopId = originStopId;
        this.serviceName = serviceName;
    }

    public String getOriginStopId() {
        return originStopId;
    }

    public String getServiceName() {
        return serviceName;
    }
}
