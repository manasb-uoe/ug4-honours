package com.enthusiast94.edinfit.ui.wait_or_walk_mode.events;

import com.enthusiast94.edinfit.models.WaitOrWalkSuggestion;

/**
 * Created by manas on 04-11-2015.
 */
public class OnWaitOrWalkSuggestionSelected {

    private WaitOrWalkSuggestion waitOrWalkSuggestion;

    public OnWaitOrWalkSuggestionSelected(WaitOrWalkSuggestion waitOrWalkSuggestion) {
        this.waitOrWalkSuggestion = waitOrWalkSuggestion;
    }

    public WaitOrWalkSuggestion getWaitOrWalkSuggestion() {
        return waitOrWalkSuggestion;
    }
}
