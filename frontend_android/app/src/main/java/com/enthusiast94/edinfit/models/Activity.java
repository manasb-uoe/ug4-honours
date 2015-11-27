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
        private double speed;

        public Point(double latitude, double longitude, long timestamp, double speed) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.timestamp = timestamp;
            this.speed = speed;
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

        public double getSpeed() {
            return speed;
        }

        /**
         * Parcelable implementation
         */

        protected Point(Parcel in) {
            latitude = in.readDouble();
            longitude = in.readDouble();
            timestamp = in.readLong();
            speed = in.readDouble();
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
            dest.writeDouble(speed);
        }
    }

    private String description;
    private Type type;
    private long start;
    private long end;
    private List<Point> points;
    private double distance;
    private double averageSpeed;

    public Activity(String description, Type type, long start, long end, List<Point> points,
                    double distance, double averageSpeed) {
        this.description = description;
        this.type = type;
        this.start = start;
        this.end = end;
        this.points = points;
        this.distance = distance;
        this.averageSpeed = averageSpeed;
    }

    public String getDescription() {
        return description;
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

    public double getAverageSpeed() {
        return averageSpeed;
    }

    public double getDistance() {
        return distance;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public void setAverageSpeed(double averageSpeed) {
        this.averageSpeed = averageSpeed;
    }

    /**
     * Parcelable implementation
     */

    protected Activity(Parcel in) {
        description = in.readString();
        type = Type.getTypeByValue(in.readString());
        start = in.readLong();
        end = in.readLong();
        points = in.createTypedArrayList(Point.CREATOR);
        distance = in.readDouble();
        averageSpeed = in.readDouble();
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
        dest.writeString(description);
        dest.writeString(type.getValue());
        dest.writeLong(start);
        dest.writeLong(end);
        dest.writeTypedList(points);
        dest.writeDouble(distance);
        dest.writeDouble(averageSpeed);
    }
}
