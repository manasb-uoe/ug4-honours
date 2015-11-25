package com.enthusiast94.edinfit.ui.wait_or_walk_mode.events;

/**
 * Created by manas on 15-11-2015.
 */
public class OnCountdownTickEvent {

    private String humanizedRemainingTime;

    public OnCountdownTickEvent(String humanizedRemainingTime) {
        this.humanizedRemainingTime = humanizedRemainingTime;
    }

    public String getHumanizedRemainingTime() {
        return humanizedRemainingTime;
    }
}
