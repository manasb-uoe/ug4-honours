package com.enthusiast94.edinfit.models;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by manas on 13-01-2016.
 */
@Table(name = "Activities")
public class Activity extends Model {

    @Column private String description;
    @Column private String type;
    @Column private long start;
    @Column private long end;
    @Column private String points;
    @Column private double distance;
    @Column private double averageSpeed;

    public Activity() {
        super();
    }

    public Activity(String description, Type type, long start, long end, List<Point> points,
                    double distance, double averageSpeed) {
        this.description = description;
        this.type = type.getValue();
        this.start = start;
        this.end = end;
        setPoints(points);
        this.distance = distance;
        this.averageSpeed = averageSpeed;
    }

    public String getDescription() {
        return description;
    }

    public Type getType() {
        return Type.getTypeByValue(type);
    }

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
    }

    public List<Point> getPoints() {
        Gson gson = new Gson();
        Point[] pointsArray = gson.fromJson(points, Point[].class);

        if (pointsArray == null) {
            return new ArrayList<>();
        }

        return new ArrayList<>(Arrays.asList(pointsArray));
    }

    public double getDistance() {
        return distance;
    }

    public double getAverageSpeed() {
        return averageSpeed;
    }

    public enum Type {
        WAIT_OR_WALK("WAIT_OR_WALK"), JOURNEY_PLANNER("JOURNEY_PLANNER");

        private String value;

        Type(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static Type getTypeByValue(String value) {
            for (Type type : values()) {
                if (type.getValue().equals(value)) {
                    return type;
                }
            }

            throw new IllegalArgumentException("No existing Type has value: " + value);
        }
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public void setPoints(List<Point> points) {
        Gson gson = new Gson();
        this.points = gson.toJson(points);
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public void setAverageSpeed(double averageSpeed) {
        this.averageSpeed = averageSpeed;
    }

    public static class Point {

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
    }

    /**
     * Statics
     */

    public static Activity findById(long id) {
        return new Select()
                .from(Activity.class)
                .where("Id = ?", id)
                .executeSingle();
    }

    public static List<Activity> getAll() {
        return new Select()
                .from(Activity.class)
                .execute();
    }
}
