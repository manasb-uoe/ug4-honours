package com.enthusiast94.edinfit.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import java.util.List;

/**
 * Created by manas on 25-11-2015.
 */
public class Activity implements Parcelable {

    public enum Type {
        WAIT_OR_WALK("WAIT_OR_WALK");

        private String value;

        Type(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static @Nullable Type getTypeByValue(String value) {
            for (Type type : values()) {
                if (type.getValue().equals(value)) {
                    return type;
                }
            }

            return null;
        }
    }

    public static class Point implements Parcelable {

        private double latitude;
        private double longitude;
        private long timestamp;

        public Point(double latitude, double longitude, long timestamp) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.timestamp = timestamp;
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public long getTimestamp() {
            return timestamp;
        }

        /**
         * Parcelable implementation
         */

        protected Point(Parcel in) {
            latitude = in.readDouble();
            longitude = in.readDouble();
            timestamp = in.readLong();
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
            dest.writeDouble(latitude);
            dest.writeDouble(longitude);
            dest.writeLong(timestamp);
        }
    }

    private Type type;
    private long start;
    private long end;
    private List<Point> points;

    public Activity(Type type, long start, long end, List<Point> points) {
        this.type = type;
        this.start = start;
        this.end = end;
        this.points = points;
    }

    public Type getType() {
        return type;
    }

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
    }

    public List<Point> getPoints() {
        return points;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    /**
     * Parcelable implementation
     */

    protected Activity(Parcel in) {
        type = Type.getTypeByValue(in.readString());
        start = in.readLong();
        end = in.readLong();
        points = in.createTypedArrayList(Point.CREATOR);
    }

    public static final Creator<Activity> CREATOR = new Creator<Activity>() {
        @Override
        public Activity createFromParcel(Parcel in) {
            return new Activity(in);
        }

        @Override
        public Activity[] newArray(int size) {
            return new Activity[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(type.getValue());
        dest.writeLong(start);
        dest.writeLong(end);
        dest.writeTypedList(points);
    }
}
