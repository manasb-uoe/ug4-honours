package com.enthusiast94.edinfit.events;

import com.enthusiast94.edinfit.models.User;

/**
 * Created by manas on 26-09-2015.
 */
public class OnAuthenticateResponseEvent {

    private String error;
    private User user;

    public OnAuthenticateResponseEvent(String error, User user) {
        this.error = error;
        this.user = user;
    }

    public String getError() {
        return error;
    }

    public User getUser() {
        return user;
    }
}
