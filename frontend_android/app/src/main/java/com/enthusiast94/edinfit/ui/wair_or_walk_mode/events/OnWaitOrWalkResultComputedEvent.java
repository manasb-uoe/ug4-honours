package com.enthusiast94.edinfit.ui.wair_or_walk_mode.events;

import com.enthusiast94.edinfit.ui.wair_or_walk_mode.fragments.ResultFragment;

/**
 * Created by manas on 04-11-2015.
 */
public class OnWaitOrWalkResultComputedEvent {

    private ResultFragment.WaitOrWalkResult waitOrWalkResult;

    public OnWaitOrWalkResultComputedEvent(ResultFragment.WaitOrWalkResult waitOrWalkResult) {
        this.waitOrWalkResult = waitOrWalkResult;
    }

    public ResultFragment.WaitOrWalkResult getWaitOrWalkResult() {
        return waitOrWalkResult;
    }
}
