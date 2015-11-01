package com.enthusiast94.edinfit.models;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by manas on 01-10-2015.
 */
public class Departure implements Parcelable {
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

    /**
     * Parcelable implementation
     */

    public Departure(Parcel in) {
        serviceName = in.readString();
        time = in.readString();
        destination = in.readString();
        day = in.readInt();
    }

    public static final Creator<Departure> CREATOR = new Creator<Departure>() {
        @Override
        public Departure createFromParcel(Parcel in) {
            return new Departure(in);
        }

        @Override
        public Departure[] newArray(int size) {
            return new Departure[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(serviceName);
        dest.writeString(time);
        dest.writeString(destination);
        dest.writeInt(day);
    }
}
