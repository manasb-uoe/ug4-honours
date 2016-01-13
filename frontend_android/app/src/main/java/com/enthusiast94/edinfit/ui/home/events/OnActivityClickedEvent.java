package com.enthusiast94.edinfit.ui.home.events;

import com.enthusiast94.edinfit.models_2.Activity;

/**
 * Created by manas on 31-12-2015.
 */
public class OnActivityClickedEvent {

    private Activity activity;

    public OnActivityClickedEvent(Activity activity) {
        this.activity = activity;
    }

    public Activity getActivity() {
        return activity;
    }
}
