package com.enthusiast94.edinfit.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

/**
 * Created by manas on 14-01-2016.
 */
public class WaitOrWalkSuggestion implements Parcelable {

    private WaitOrWalkSuggestionType type;
    private Stop stop;
    private Departure upcomingDeparture;
    private Directions walkingDirections;

    public WaitOrWalkSuggestion(WaitOrWalkSuggestionType type, Stop stop, Departure upcomingDeparture,
                                Directions walkingDirections) {
        this.type = type;
        this.stop = stop;
        this.upcomingDeparture = upcomingDeparture;
        this.walkingDirections = walkingDirections;
    }

    public Departure getUpcomingDeparture() {
        return upcomingDeparture;
    }

    public Directions getWalkingDirections() {
        return walkingDirections;
    }

    public Stop getStop() {
        return stop;
    }

    public WaitOrWalkSuggestionType getType() {
        return type;
    }

    public void setStop(Stop stop) {
        this.stop = stop;
    }

    public void setWalkingDirections(Directions walkingDirections) {
        this.walkingDirections = walkingDirections;
    }

    /**
     * Parcelable implementation
     */

    public WaitOrWalkSuggestion(Parcel in) {
        type = WaitOrWalkSuggestionType.getTypeByValue(in.readInt());
        stop = Stop.findById(in.readString());
        upcomingDeparture = Departure.findById(in.readLong());
        walkingDirections = in.readParcelable(Directions.class.getClassLoader());
    }

    public static final Creator<WaitOrWalkSuggestion> CREATOR = new Creator<WaitOrWalkSuggestion>() {
        @Override
        public WaitOrWalkSuggestion createFromParcel(Parcel in) {
            return new WaitOrWalkSuggestion(in);
        }

        @Override
        public WaitOrWalkSuggestion[] newArray(int size) {
            return new WaitOrWalkSuggestion[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(type.getValue());
        dest.writeString(stop.get_id());
        dest.writeLong(upcomingDeparture.getId());
        dest.writeParcelable(walkingDirections, flags);
    }

    public enum WaitOrWalkSuggestionType {
        WAIT(0), WALK(1);

        private int value;

        WaitOrWalkSuggestionType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static @Nullable
        WaitOrWalkSuggestionType getTypeByValue(int value) {
            for (WaitOrWalkSuggestionType type : values()) {
                if (type.getValue() == value) {
                    return type;
                }
            }

            return null;
        }
    }
}
