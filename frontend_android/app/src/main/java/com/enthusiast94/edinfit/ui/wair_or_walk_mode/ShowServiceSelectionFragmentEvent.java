package com.enthusiast94.edinfit.ui.wair_or_walk_mode;

import java.util.List;

/**
 * Created by manas on 25-10-2015.
 */
public class ShowServiceSelectionFragmentEvent {
    private List<String> serviceNames;

    public ShowServiceSelectionFragmentEvent(List<String> serviceNames) {
        this.serviceNames = serviceNames;
    }

    public List<String> getServiceNames() {
        return serviceNames;
    }
}
