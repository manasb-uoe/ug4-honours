package com.enthusiast94.edinfit.models_2;

import android.os.Parcel;
import android.os.Parcelable;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

/**
 * Created by manas on 09-01-2016.
 */
@Table(name = "Departures")
public class Departure extends Model implements Parcelable {

    @Column private Stop stop; // foreign key
    @Column private String serviceName;
    @Column private String time;
    @Column private String destination;
    @Column private int day;

    public Departure() {
        super();
    }

    public Departure(Stop stop, String serviceName, String time, String destination, int day) {
        this.stop = stop;
        this.serviceName = serviceName;
        // ensure time is formatted properly (eg: 5:01 must become 05:01)
        this.time = time.length() == 4  ? "0" + time : time;
        this.destination = destination;
        this.day = day;
    }

    public Stop getStop() {
        return stop;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getTime() {
        return time;
    }

    public String getDestination() {
        return destination;
    }

    public int getDay() {
        return day;
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
