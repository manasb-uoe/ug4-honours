package com.enthusiast94.edinfit.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by manas on 01-10-2015.
 */
public class Stop implements Parcelable {
    private String stopId;
    private String name;
    private List<Double> location;
    private String serviceType;
    private String direction;
    private List<String> destinations;
    private List<String> services;
    private List<Departure> departures;
    @Nullable private Double distanceAway;

    public Stop(String id, String name, List<Double> location, String serviceType, String direction,
                List<String> destinations, List<String> services, List<Departure> departures,
                @Nullable Double distanceAway) {
        this.stopId = id;
        this.name = name;
        this.location = location;
        this.destinations = destinations;
        this.services = services;
        this.serviceType = serviceType;
        this.direction = direction;
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

    public String getDirection() {
        return direction;
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

    /**
     * Parcelable implementation
     */

    public Stop(Parcel in) {
        stopId = in.readString();
        name = in.readString();
        location = new ArrayList<>();
        in.readList(location, Double.class.getClassLoader());
        serviceType = in.readString();
        direction = in.readString();
        destinations = in.createStringArrayList();
        services = in.createStringArrayList();
        departures = in.createTypedArrayList(Departure.CREATOR);
        distanceAway = in.readDouble();
        distanceAway = distanceAway == 0 ? null : distanceAway;
    }

    public static final Creator<Stop> CREATOR = new Creator<Stop>() {
        @Override
        public Stop createFromParcel(Parcel in) {
            return new Stop(in);
        }

        @Override
        public Stop[] newArray(int size) {
            return new Stop[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(stopId);
        dest.writeString(name);
        dest.writeList(location);
        dest.writeString(serviceType);
        dest.writeString(direction);
        dest.writeStringList(destinations);
        dest.writeStringList(services);
        dest.writeTypedList(departures);
        dest.writeDouble(distanceAway == null ? 0 : distanceAway);
    }
}
