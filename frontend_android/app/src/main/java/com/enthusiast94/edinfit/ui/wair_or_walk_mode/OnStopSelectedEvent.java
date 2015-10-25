package com.enthusiast94.edinfit.ui.wair_or_walk_mode;

import com.enthusiast94.edinfit.models.Stop;

/**
 * Created by manas on 19-10-2015.
 */
public class OnStopSelectedEvent {

    private Stop selectedStop;

    public OnStopSelectedEvent(Stop seletedStop) {
        this.selectedStop = seletedStop;
    }

    public Stop getSelectedStop() {
        return selectedStop;
    }
}
