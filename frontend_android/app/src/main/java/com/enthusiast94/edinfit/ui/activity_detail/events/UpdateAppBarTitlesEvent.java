package com.enthusiast94.edinfit.ui.activity_detail.events;

/**
 * Created by manas on 07-02-2016.
 */
public class UpdateAppBarTitlesEvent {

    private String title;
    private String subtitle;

    public UpdateAppBarTitlesEvent(String title, String subtitle) {
        this.title = title;
        this.subtitle = subtitle;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getTitle() {
        return title;
    }
}
