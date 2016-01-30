package com.enthusiast94.edinfit.models;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by manas on 29-01-2016.
 */
public class Journey {

    private List<Leg> legs;
    private long startTime;     // unix
    private long finishTime;    // unix
    private int duration;       // minutes

    public Journey(List<Leg> legs, long startTime, long endTime, int duration) {
        this.legs = legs;
        this.startTime = startTime;
        this.finishTime = endTime;
        this.duration = duration;
    }

    public List<Leg> getLegs() {
        return legs;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getFinishTime() {
        return finishTime;
    }

    public int getDuration() {
        return duration;
    }

    public static class Leg {

        Point startPoint;
        Point finishPoint;

        public Leg(Point startPoint, Point finishPoint) {
            this.startPoint = startPoint;
            this.finishPoint = finishPoint;
        }

        public Point getFinishPoint() {
            return finishPoint;
        }

        public Point getStartPoint() {
            return startPoint;
        }
    }

    public static class WalkLeg extends Leg {

        public WalkLeg(Point startPoint, Point finishPoint) {
            super(startPoint, finishPoint);
        }
    }

    public static class BusLeg extends Leg {

        private String serviceName;
        private String destination;
        private List<String> stopOnRoute;

        public BusLeg(Point startPoint, Point finishPoint, String serviceName,
                      String destination, List<String> stopsOnRoute) {
            super(startPoint, finishPoint);
            this.serviceName = serviceName;
            this.destination = destination;
            this.stopOnRoute = stopsOnRoute;
        }

        public String getServiceName() {
            return serviceName;
        }

        public Service getService() {
            return Service.findByName(serviceName);
        }

        public String getDestination() {
            return destination;
        }

        public List<Stop> getStopsOnRoute() {
            List<Stop> stops = new ArrayList<>();
            for (String id : stopOnRoute) {
                stops.add(Stop.findById(id));
            }

            return stops;
        }
    }

    public static class Point {

        private String name;
        private double latitude;
        private double longitude;
        private long timestamp;
        private String stopId;

        public Point(String name, double latitude, double longitude, long timestamp, String stopId) {
            this.name = name;
            this.latitude = latitude;
            this.longitude = longitude;
            this.timestamp = timestamp;
            this.stopId = stopId;
        }

        public String getName() {
            return name;
        }

        public LatLng getLatLng() {
            return new LatLng(latitude, longitude);
        }

        public long getTimestamp() {
            return timestamp;
        }

        public Stop getStop() {
            return Stop.findById(stopId);
        }
    }
}
