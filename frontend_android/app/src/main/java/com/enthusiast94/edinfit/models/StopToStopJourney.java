package com.enthusiast94.edinfit.models;

import java.util.List;

/**
 * Created by manas on 11-01-2016.
 */
public class StopToStopJourney {

    private String serviceName;
    private String  destination;
    private List<Departure> departures;

    public StopToStopJourney(String serviceName, String destination, List<Departure> departures) {
        this.serviceName = serviceName;
        this.destination = destination;
        this.departures = departures;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getDestination() {
        return destination;
    }

    public List<Departure> getDepartures() {
        return departures;
    }

    public static class Departure {

        private Stop stop;
        private String name;
        private String time;

        public Departure(Stop stop, String name, String time) {
            this.stop = stop;
            this.name = name;
            this.time = time;
        }

        public Stop getStop() {
            return stop;
        }

        public String getName() {
            return name;
        }

        public String getTime() {
            return time;
        }
    }
}
