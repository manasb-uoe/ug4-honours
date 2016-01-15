package com.enthusiast94.edinfit.models_2;

import java.util.List;

/**
 * Created by manas on 18-10-2015.
 */
public class LiveBus {

    private String vehicle_id; // fleet ID of the vehicle
    private long last_gps_fix; // last time the vehicle broadcasted its location
    private float latitude;
    private float longitude;
    private int speed; // in miles per hour
    private int heading; // in degrees, 0-360
    private String service_name; // service that the vehicle is currently operating on
    private String destination; // destination displayed on the vehicle's destination board

    public LiveBus(String vehicle_id, long last_gps_fix, float latitude, float longitude,
                   int speed, int heading, String service_name, String destination) {
        this.vehicle_id = vehicle_id;
        this.last_gps_fix = last_gps_fix;
        this.latitude = latitude;
        this.longitude = longitude;
        this.speed = speed;
        this.heading = heading;
        this.service_name = service_name;
        this.destination = destination;
    }

    public String getVehicleId() {
        return vehicle_id;
    }

    public long getLastGpsFix() {
        return last_gps_fix;
    }

    public float getLatitude() {
        return latitude;
    }

    public float getLongitude() {
        return longitude;
    }

    public int getSpeed() {
        return speed;
    }

    public int getHeading() {
        return heading;
    }

    public String getServiceName() {
        return service_name;
    }

    public String getDestination() {
        return destination;
    }
}
