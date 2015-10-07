package com.enthusiast94.edinfit.models;

import com.enthusiast94.edinfit.models.Point;

import java.util.List;

/**
 * Created by manas on 06-10-2015.
 */
public class Route {

    private String destination;
    private List<Stop> stops;
    private List<Point> points;

    public Route(String destination, List<Stop> stops, List<Point> points) {
        this.destination = destination;
        this.stops = stops;
        this.points = points;
    }

    public String getDestination() {
        return destination;
    }

    public List<Point> getPoints() {
        return points;
    }

    public List<Stop> getStops() {
        return stops;
    }
}
