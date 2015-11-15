package com.enthusiast94.edinfit.ui.wair_or_walk_mode.events;

/**
 * Created by manas on 15-11-2015.
 */
public class OnCountdownTick {

    private String humanizedRemainingTime;

    public OnCountdownTick(String humanizedRemainingTime) {
        this.humanizedRemainingTime = humanizedRemainingTime;
    }

    public String getHumanizedRemainingTime() {
        return humanizedRemainingTime;
    }
}
