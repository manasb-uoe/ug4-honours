package com.enthusiast94.edinfit.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.enthusiast94.edinfit.models.Point;

import java.util.List;

/**
 * Created by manas on 06-10-2015.
 */
public class Route implements Parcelable {

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

    /**
     * Parcelable implementation
     */

    public Route(Parcel in) {
        destination = in.readString();
        stops = in.createTypedArrayList(Stop.CREATOR);
        points = in.createTypedArrayList(Point.CREATOR);
    }

    public static final Creator<Route> CREATOR = new Creator<Route>() {
        @Override
        public Route createFromParcel(Parcel in) {
            return new Route(in);
        }

        @Override
        public Route[] newArray(int size) {
            return new Route[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(destination);
        dest.writeTypedList(stops);
        dest.writeTypedList(points);
    }
}
