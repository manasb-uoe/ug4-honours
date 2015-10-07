package com.enthusiast94.edinfit.models;

/**
 * Created by manas on 06-10-2015.
 */
public class Point {

    private Double latitude;
    private Double longitude;

    public Point(Double latitude, Double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }
}
