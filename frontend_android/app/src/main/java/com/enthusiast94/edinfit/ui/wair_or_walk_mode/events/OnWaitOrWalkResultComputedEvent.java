package com.enthusiast94.edinfit.ui.wair_or_walk_mode.events;

import com.enthusiast94.edinfit.services.WaitOrWalkService;

/**
 * Created by manas on 04-11-2015.
 */
public class OnWaitOrWalkResultComputedEvent {

    private WaitOrWalkService.WaitOrWalkResult waitOrWalkResult;

    public OnWaitOrWalkResultComputedEvent(WaitOrWalkService.WaitOrWalkResult waitOrWalkResult) {
        this.waitOrWalkResult = waitOrWalkResult;
    }

    public WaitOrWalkService.WaitOrWalkResult getWaitOrWalkResult() {
        return waitOrWalkResult;
    }
}
