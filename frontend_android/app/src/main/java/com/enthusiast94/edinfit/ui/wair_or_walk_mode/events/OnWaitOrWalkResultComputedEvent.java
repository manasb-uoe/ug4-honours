package com.enthusiast94.edinfit.ui.wair_or_walk_mode.events;

import com.enthusiast94.edinfit.services.WaitOrWalkService;

/**
 * Created by manas on 04-11-2015.
 */
public class OnWaitOrWalkResultComputedEvent {

    private WaitOrWalkService.WaitOrWalkSuggestion waitOrWalkSuggestion;

    public OnWaitOrWalkResultComputedEvent(WaitOrWalkService.WaitOrWalkSuggestion waitOrWalkSuggestion) {
        this.waitOrWalkSuggestion = waitOrWalkSuggestion;
    }

    public WaitOrWalkService.WaitOrWalkSuggestion getWaitOrWalkSuggestion() {
        return waitOrWalkSuggestion;
    }
}
