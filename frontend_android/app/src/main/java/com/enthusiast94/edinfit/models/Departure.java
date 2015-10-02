package com.enthusiast94.edinfit.models;

/**
 * Created by manas on 01-10-2015.
 */
public class Departure {
    private String serviceName;
    private String time;
    private String destination;
    private int day;

    public Departure(String serviceName, String time, String destination, int day) {
        this.serviceName = serviceName;
        this.time = time;
        this.destination = destination;
        this.day = day;
    }

    public int getDay() {
        return day;
    }

    public String getDestination() {
        return destination;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getTime() {
        return time;
    }
}
