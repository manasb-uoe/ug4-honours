package com.enthusiast94.edinfit.models;

import java.util.List;

/**
 * Created by manas on 01-10-2015.
 */
public class Stop {
    private String id;
    private String name;
    private List<Double> location;
    private String serviceType;
    private List<String> destinations;
    private List<String> services;
    private List<Departure> departures;

    public Stop(String id, String name, List<Double> location, String serviceType,
                List<String> destinations, List<String> services, List<Departure> departures) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.destinations = destinations;
        this.services = services;
        this.serviceType = serviceType;
        this.departures = departures;
    }

    public String getId() {
        return id;
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
}
