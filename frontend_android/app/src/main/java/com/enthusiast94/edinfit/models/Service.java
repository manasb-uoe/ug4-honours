package com.enthusiast94.edinfit.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

/**
 * Created by manas on 06-10-2015.
 */
public class Service implements Parcelable {

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

    /**
     * Parcelable implementation
     */

    public Service(Parcel in) {
        name = in.readString();
        description = in.readString();
        serviceType = in.readString();
        routes = in.createTypedArrayList(Route.CREATOR);
    }

    public static final Creator<Service> CREATOR = new Creator<Service>() {
        @Override
        public Service createFromParcel(Parcel in) {
            return new Service(in);
        }

        @Override
        public Service[] newArray(int size) {
            return new Service[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(description);
        dest.writeString(serviceType);
        dest.writeTypedList(routes);
    }
}
