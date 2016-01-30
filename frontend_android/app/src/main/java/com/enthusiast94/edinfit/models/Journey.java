package com.enthusiast94.edinfit.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.PolyUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by manas on 29-01-2016.
 */
public class Journey implements Parcelable {

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

    /**
     * Parcelable implementation
     */

    public Journey(Parcel in) {
        legs = new ArrayList<>();
        in.readList(legs, Leg.class.getClassLoader());
        startTime = in.readLong();
        finishTime = in.readLong();
        duration = in.readInt();
    }

    public static final Creator<Journey> CREATOR = new Creator<Journey>() {
        @Override
        public Journey createFromParcel(Parcel in) {
            return new Journey(in);
        }

        @Override
        public Journey[] newArray(int size) {
            return new Journey[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeList(legs);
        dest.writeLong(startTime);
        dest.writeLong(finishTime);
        dest.writeInt(duration);
    }

    public static abstract class Leg implements Parcelable {

        private Point startPoint;
        private Point finishPoint;

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

        /**
         * Parcelable implementation
         */

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeParcelable(startPoint, flags);
            dest.writeParcelable(finishPoint, flags);
        }

        protected Leg(Parcel in) {
            startPoint = in.readParcelable(Point.class.getClassLoader());
            finishPoint = in.readParcelable(Point.class.getClassLoader());
        }
    }

    public static class WalkLeg extends Leg {

        public WalkLeg(Point startPoint, Point finishPoint) {
            super(startPoint, finishPoint);
        }

        public static final Parcelable.Creator<WalkLeg> CREATOR = new Parcelable.Creator<WalkLeg>() {
            public WalkLeg createFromParcel(Parcel in) {
                return new WalkLeg(in);
            }

            public WalkLeg[] newArray(int size) {
                return new WalkLeg[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
        }

        private WalkLeg(Parcel in) {
            super(in);
        }
    }

    public static class BusLeg extends Leg {

        private String serviceName;
        private String destination;
        private String polyline;
        private List<String> stopsOnRoute;

        public BusLeg(Point startPoint, Point finishPoint, String serviceName, String destination,
                      List<String> stopsOnRoute, String polyline) {
            super(startPoint, finishPoint);
            this.serviceName = serviceName;
            this.destination = destination;
            this.stopsOnRoute = stopsOnRoute;
            this.polyline = polyline;
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

        public List<LatLng> getLatLngs() {
            return PolyUtil.decode(polyline);
        }

        public List<Stop> getStopsOnRoute() {
            List<Stop> stops = new ArrayList<>();
            for (String id : stopsOnRoute) {
                stops.add(Stop.findById(id));
            }

            return stops;
        }

        public static final Parcelable.Creator<BusLeg> CREATOR = new Parcelable.Creator<BusLeg>() {
            public BusLeg createFromParcel(Parcel in) {
                return new BusLeg(in);
            }

            public BusLeg[] newArray(int size) {
                return new BusLeg[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeString(serviceName);
            dest.writeString(destination);
            dest.writeString(polyline);
            dest.writeStringList(stopsOnRoute);
        }

        private BusLeg(Parcel in) {
            super(in);
            serviceName = in.readString();
            destination = in.readString();
            polyline = in.readString();
            stopsOnRoute = new ArrayList<>();
            in.readStringList(stopsOnRoute);
        }
    }

    public static class Point implements Parcelable {

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

        /**
         * Parcelable implementation
         */

        public Point(Parcel in) {
            name = in.readString();
            latitude = in.readDouble();
            longitude = in.readDouble();
            timestamp = in.readLong();
            stopId = in.readString();
        }

        public static final Creator<Point> CREATOR = new Creator<Point>() {
            @Override
            public Point createFromParcel(Parcel in) {
                return new Point(in);
            }

            @Override
            public Point[] newArray(int size) {
                return new Point[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(name);
            dest.writeDouble(latitude);
            dest.writeDouble(longitude);
            dest.writeLong(timestamp);
            dest.writeString(stopId);
        }
    }
}
