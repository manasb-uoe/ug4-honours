package com.enthusiast94.edinfit.ui.find_a_bus.events;

/**
 * Created by manas on 19-11-2015.
 */
public class OnSearchEvent {

    private String filter;

    public OnSearchEvent(String filter) {
        this.filter = filter;
    }

    public String getFilter() {
        return filter;
    }
}
