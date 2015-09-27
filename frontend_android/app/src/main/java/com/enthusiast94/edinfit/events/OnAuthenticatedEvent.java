package com.enthusiast94.edinfit.events;

import com.enthusiast94.edinfit.models.User;

/**
 * Created by manas on 26-09-2015.
 */
public class OnAuthenticatedEvent {

    private User user;

    public OnAuthenticatedEvent(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }
}
