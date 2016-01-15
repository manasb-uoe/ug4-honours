package com.enthusiast94.edinfit.models_2;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.enthusiast94.edinfit.models.*;
import com.enthusiast94.edinfit.models.Departure;

/**
 * Created by manas on 14-01-2016.
 */
public class WaitOrWalkSuggestion implements Parcelable {

    private WaitOrWalkSuggestionType type;
    private com.enthusiast94.edinfit.models.Stop stop;
    private com.enthusiast94.edinfit.models.Departure upcomingDeparture;
    private Directions walkingDirections;

    public WaitOrWalkSuggestion(WaitOrWalkSuggestionType type, com.enthusiast94.edinfit.models.Stop stop, com.enthusiast94.edinfit.models.Departure upcomingDeparture,
                                Directions walkingDirections) {
        this.type = type;
        this.stop = stop;
        this.upcomingDeparture = upcomingDeparture;
        this.walkingDirections = walkingDirections;
    }

    public com.enthusiast94.edinfit.models.Departure getUpcomingDeparture() {
        return upcomingDeparture;
    }

    public Directions getWalkingDirections() {
        return walkingDirections;
    }

    public com.enthusiast94.edinfit.models.Stop getStop() {
        return stop;
    }

    public WaitOrWalkSuggestionType getType() {
        return type;
    }

    public void setStop(com.enthusiast94.edinfit.models.Stop stop) {
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
        stop = in.readParcelable(com.enthusiast94.edinfit.models.Stop.class.getClassLoader());
        upcomingDeparture = in.readParcelable(Departure.class.getClassLoader());
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
        dest.writeParcelable(stop, flags);
        dest.writeParcelable(upcomingDeparture, flags);
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
