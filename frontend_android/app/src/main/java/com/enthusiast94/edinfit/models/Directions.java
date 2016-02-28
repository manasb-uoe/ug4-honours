package com.enthusiast94.edinfit.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.PolyUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by manas on 12-11-2015.
 */
public class Directions implements Parcelable {

    private int distance;
    private String distanceText;
    private long duration;
    private String durationText;
    private List<Step> steps;
    private String overviewPoints;

    public Directions() {}

    public Directions(int distance, String distanceText, long duration, String durationText,
                      List<Step> steps) {
        this.distance = distance;
        this.distanceText = distanceText;
        this.duration = duration;
        this.durationText = durationText;
        this.steps = steps;
    }

    public int getDistance() {
        return distance;
    }

    public String getDistanceText() {
        return distanceText;
    }

    public long getDuration() {
        return duration;
    }

    public String getDurationText() {
        return durationText;
    }

    public List<Step> getSteps() {
        return steps;
    }

    public List<Point> getOverviewPoints() {
        List<LatLng> latLngs = PolyUtil.decode(overviewPoints);
        List<Point> points = new ArrayList<>();
        for (LatLng latLng : latLngs) {
            points.add(new Directions.Point(latLng.latitude, latLng.longitude));
        }

        return points;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }

    public void setDistanceText(String distanceText) {
        this.distanceText = distanceText;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public void setDurationText(String durationText) {
        this.durationText = durationText;
    }

    public void setSteps(List<Step> steps) {
        this.steps = steps;
    }

    public void setOverviewPoints(String overviewPoints) {
        this.overviewPoints = overviewPoints;
    }

    public String getPolyline() {
        return overviewPoints;
    }

    /**
     * Parcelable implementation
     */

    protected Directions(Parcel in) {
        distance = in.readInt();
        distanceText = in.readString();
        duration = in.readLong();
        durationText = in.readString();
        steps = in.createTypedArrayList(Step.CREATOR);
        overviewPoints = in.readString();
    }

    public static final Creator<Directions> CREATOR = new Creator<Directions>() {
        @Override
        public Directions createFromParcel(Parcel in) {
            return new Directions(in);
        }

        @Override
        public Directions[] newArray(int size) {
            return new Directions[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(distance);
        dest.writeString(distanceText);
        dest.writeLong(duration);
        dest.writeString(durationText);
        dest.writeTypedList(steps);
        dest.writeString(overviewPoints);
    }

    public static class Step implements Parcelable {

        private int distance;
        private String distanceText;
        private long duration;
        private String durationText;
        private Point startLocation;
        private Point endLocation;
        private String instruction;
        private String maneuver;
        private String points;

        public Step(int distance, String distanceText, long duration, String durationText,
                    Point startLocation, Point endLocation, String instruction, String maneuver,
                    String points) {
            this.distance = distance;
            this.distanceText = distanceText;
            this.duration = duration;
            this.durationText = durationText;
            this.startLocation = startLocation;
            this.endLocation = endLocation;
            this.instruction = instruction;
            this.maneuver = maneuver;
            this.points = points;
        }

        public int getDistance() {
            return distance;
        }

        public String getDistanceText() {
            return distanceText;
        }

        public long getDuration() {
            return duration;
        }

        public String getDurationText() {
            return durationText;
        }

        public Point getStartLocation() {
            return startLocation;
        }

        public Point getEndLocation() {
            return endLocation;
        }

        public String getInstruction() {
            return instruction;
        }

        public String getManeuver() {
            return maneuver;
        }

        public List<Point> getPoints() {
            List<LatLng> latLngs = PolyUtil.decode(points);
            List<Point> points = new ArrayList<>();
            for (LatLng latLng : latLngs) {
                points.add(new Directions.Point(latLng.latitude, latLng.longitude));
            }

            return points;
        }

        /**
         * Parcelable implementation
         */

        public Step(Parcel in) {
            distance = in.readInt();
            distanceText = in.readString();
            duration = in.readLong();
            durationText = in.readString();
            startLocation = in.readParcelable(Point.class.getClassLoader());
            endLocation = in.readParcelable(Point.class.getClassLoader());
            instruction = in.readString();
            maneuver = in.readString();
            points = in.readString();
        }

        public static final Creator<Step> CREATOR = new Creator<Step>() {
            @Override
            public Step createFromParcel(Parcel in) {
                return new Step(in);
            }

            @Override
            public Step[] newArray(int size) {
                return new Step[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(distance);
            dest.writeString(distanceText);
            dest.writeLong(duration);
            dest.writeString(durationText);
            dest.writeParcelable(startLocation, flags);
            dest.writeParcelable(endLocation, flags);
            dest.writeString(instruction);
            dest.writeString(maneuver);
            dest.writeString(points);
        }
    }

    public static class Point implements Parcelable {

        private Double latitude;
        private Double longitude;

        public Point(Double latitude, Double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public Double getLatitude() {
            return latitude;
        }

        public Double getLongitude() {
            return longitude;
        }

        /**
         * Parcelable implementation
         */

        public Point(Parcel in) {
            latitude = in.readDouble();
            longitude = in.readDouble();
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
        }
    }
}
