package com.enthusiast94.edinfit.models;

import java.util.List;

/**
 * Created by manas on 06-10-2015.
 */
public class Service {

    private String name;
    private String description;
    private String serviceType;
    private List<Route> routes;

    public Service(String name, String description, String serviceType, List<Route> routes) {
        this.name = name;
        this.description = description;
        this.serviceType = serviceType;
        this.routes = routes;
    }

    public List<Route> getRoutes() {
        return routes;
    }

    public String getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }

    public String getServiceType() {
        return serviceType;
    }
}
