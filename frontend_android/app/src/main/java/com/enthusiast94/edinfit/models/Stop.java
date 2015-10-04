package com.enthusiast94.edinfit.models;

import java.util.List;

/**
 * Created by manas on 01-10-2015.
 */
public class Stop {
    private String stopId;
    private String name;
    private List<Double> location;
    private String serviceType;
    private List<String> destinations;
    private List<String> services;
    private List<Departure> departures;
    private Double distanceAway;

    public Stop(String id, String name, List<Double> location, String serviceType,
                List<String> destinations, List<String> services, List<Departure> departures,
                Double distanceAway) {
        this.stopId = id;
        this.name = name;
        this.location = location;
        this.destinations = destinations;
        this.services = services;
        this.serviceType = serviceType;
        this.departures = departures;
        this.distanceAway = distanceAway;
    }

    public String getId() {
        return stopId;
    }

    public String getName() {
        return name;
    }

    public List<Double> getLocation() {
        return location;
    }

    public String getServiceType() {
        return serviceType;
    }

    public List<String> getDestinations() {
        return destinations;
    }

    public List<String> getServices() {
        return services;
    }

    public List<Departure> getDepartures() {
        return departures;
    }

    public Double getDistanceAway() {
        return distanceAway;
    }
}
