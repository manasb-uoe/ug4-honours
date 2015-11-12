package com.enthusiast94.edinfit.models;

import android.os.Parcel;
import android.os.Parcelable;

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
    private List<Point> overviewPoints;

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
        overviewPoints = in.createTypedArrayList(Point.CREATOR);
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
        dest.writeTypedList(overviewPoints);
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
        private List<Point> points;

        public Step(int distance, String distanceText, long duration, String durationText,
                    Point startLocation, Point endLocation, List<Point> points) {
            this.distance = distance;
            this.distanceText = distanceText;
            this.duration = duration;
            this.durationText = durationText;
            this.startLocation = startLocation;
            this.endLocation = endLocation;
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
            points = in.createTypedArrayList(Point.CREATOR);
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
            dest.writeTypedList(points);
        }
    }
}
