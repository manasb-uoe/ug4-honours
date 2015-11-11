package com.enthusiast94.edinfit.services;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.util.Log;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models.Departure;
import com.enthusiast94.edinfit.models.Route;
import com.enthusiast94.edinfit.models.Service;
import com.enthusiast94.edinfit.models.Stop;
import com.enthusiast94.edinfit.utils.Helpers;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by manas on 11-11-2015.
 */
public class WaitOrWalkService extends BaseService {

    private static final String TAG = WaitOrWalkService.class.getSimpleName();
    private static WaitOrWalkService instance;
    private Context context;

    private WaitOrWalkService(Context context) {
        this.context = context;
    }

    public static void init(Context context) {
        if (instance != null) {
            throw new IllegalStateException(TAG + " has already been initialized.");
        }

        instance = new WaitOrWalkService(context);
    }

    public static WaitOrWalkService getInstance() {
        if (instance == null) {
            throw new IllegalStateException(TAG + " has not been initialized yet.");
        }

        return instance;
    }

    public void getWaitOrWalkSuggestions(
            Route selectedRoute,
            final Service selectedService,
            final Stop selectedOriginStop,
            final Callback<List<WaitOrWalkResult>> callback) {

        final List<WaitOrWalkResult> waitOrWalkResults = new ArrayList<>();

        // find next stop index
        int nextStopIndex = -1;

        for (int i=0; i<selectedRoute.getStops().size(); i++) {
            Stop currentStop = selectedRoute.getStops().get(i);
            if (currentStop.getId().equals(selectedOriginStop.getId())) {
                nextStopIndex = i + 1;
                break;
            }
        }

        if (nextStopIndex != -1) {
            final Stop nextStopWithoutDepartures = selectedRoute.getStops().get(nextStopIndex);

            // fetch next stop with upcoming departures for current day
            StopService.getInstance().getStop(nextStopWithoutDepartures.getId(),
                    Helpers.getDayCode(Helpers.getCurrentDay()),
                    Helpers.getCurrentTime24h(), new BaseService.Callback<Stop>() {

                        @Override
                        public void onSuccess(final Stop nextStopWithDepartures) {
                            // find the amount of time remaining until upcoming departure of
                            // selected service
                            Departure upcomingDeparture = null;
                            long remainingTimeMillis = 0;

                            for (int i=0; i<nextStopWithDepartures.getDepartures().size(); i++) {
                                Departure departure = nextStopWithDepartures.getDepartures().get(i);

                                if (departure.getServiceName().equals(selectedService.getName())) {
                                    upcomingDeparture = departure;
                                    remainingTimeMillis = Helpers.getRemainingTimeMillisFromNow(departure.getTime());

                                    Log.d(TAG, "upcoming departure at: " + departure.getTime());
                                    break;
                                }
                            }

                            // check if there's enough time left to walk to the next stop or not
                            if (upcomingDeparture != null) {
                                final long finalRemainingTimeMillis = remainingTimeMillis;
                                final Departure finalUpcomingDeparture = upcomingDeparture;

                                LocationProviderService.getInstance().requestLastKnownLocationInfo(false, new LocationProviderService.LocationCallback() {
                                    @Override
                                    public void onLocationSuccess(final LatLng userLatLng, String placeName) {
                                        LatLng nextStopLatLng = new LatLng(nextStopWithDepartures.getLocation().get(1), nextStopWithDepartures.getLocation().get(0));
                                        DirectionsService.getInstance().getWalkingDirections(userLatLng, nextStopLatLng, new BaseService.Callback<DirectionsService.DirectionsResult>() {

                                            @Override
                                            public void onSuccess(DirectionsService.DirectionsResult result) {
                                                com.directions.route.Route resultRoute = result.getRoute();

                                                long walkingTimeMillis = Helpers.parseDirectionsApiDurationToMillis(resultRoute.getDurationText());


                                                Log.d(TAG, "remaining time: " + finalRemainingTimeMillis / (1000*60));
                                                Log.d(TAG, "api duration: " + resultRoute.getDurationText());
                                                Log.d(TAG, "parsed api duration: " + walkingTimeMillis / (1000*60));

                                                if (finalRemainingTimeMillis >= walkingTimeMillis) {
                                                    waitOrWalkResults.add(new WaitOrWalkResult(
                                                            WaitOrWalkResultType.WALK,
                                                            nextStopWithoutDepartures,
                                                            finalUpcomingDeparture,
                                                            finalRemainingTimeMillis,
                                                            result
                                                    ));

                                                    callback.onSuccess(waitOrWalkResults);

                                                    Log.d(TAG, "result: WALK");
                                                } else {
                                                    // Fetch origin stop with upcoming departures for current day.
                                                    // The latest departure from this list is needed since there's
                                                    // not enough time left for the user to reach the next stop, and
                                                    // therefore they must wait at the origin stop until the latest
                                                    // upcoming departure.
                                                    StopService.getInstance().getStop(selectedOriginStop.getId(),
                                                            Helpers.getDayCode(Helpers.getCurrentDay()), Helpers.getCurrentTime24h(),
                                                            new BaseService.Callback<Stop>() {

                                                                @Override
                                                                public void onSuccess(final Stop selectedOriginStopWithDepartures) {
                                                                    LatLng originStopLatLng = new LatLng(
                                                                            selectedOriginStopWithDepartures.getLocation().get(1),
                                                                            selectedOriginStopWithDepartures.getLocation().get(0)
                                                                    );

                                                                    DirectionsService.getInstance().getWalkingDirections(userLatLng, originStopLatLng,
                                                                            new BaseService.Callback<DirectionsService.DirectionsResult>() {

                                                                                @Override
                                                                                public void onSuccess(DirectionsService.DirectionsResult data) {
                                                                                    Departure upcomingDepartureAtOriginStop =
                                                                                            selectedOriginStopWithDepartures.getDepartures().get(0);
                                                                                    long remainingTimeMillisForUpcomingDepartureAtOriginStop =
                                                                                            Helpers.getRemainingTimeMillisFromNow(upcomingDepartureAtOriginStop.getTime());

                                                                                    waitOrWalkResults.add(new WaitOrWalkResult(
                                                                                            WaitOrWalkResultType.WAIT,
                                                                                            selectedOriginStop,
                                                                                            upcomingDepartureAtOriginStop,
                                                                                            remainingTimeMillisForUpcomingDepartureAtOriginStop,
                                                                                            data
                                                                                    ));

                                                                                    callback.onSuccess(waitOrWalkResults);

                                                                                    Log.d(TAG, "result: WAIT");
                                                                                }

                                                                                @Override
                                                                                public void onFailure(String message) {
                                                                                    callback.onFailure(message);
                                                                                }
                                                                            });
                                                                }

                                                                @Override
                                                                public void onFailure(String message) {
                                                                    callback.onFailure(message);
                                                                }
                                                            });
                                                }
                                            }

                                            @Override
                                            public void onFailure(String message) {
                                                callback.onFailure(message);
                                            }
                                        });
                                    }

                                    @Override
                                    public void onLocationFailure(String error) {
                                        callback.onFailure(error);
                                    }
                                });

                            } else {
                                callback.onFailure(context.getString(R.string.label_no_upcoming_departure));
                            }
                        }

                        @Override
                        public void onFailure(String message) {
                            callback.onFailure(message);
                        }
                    });
        } else {
            callback.onFailure(context.getString(R.string.error_unexpected));
        }
    }

    public enum WaitOrWalkResultType {
        WAIT(0), WALK(1), ;

        private int value;

        WaitOrWalkResultType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static @Nullable
        WaitOrWalkResultType getTypeByValue(int value) {
            for (WaitOrWalkResultType type : values()) {
                if (type.getValue() == value) {
                    return type;
                }
            }

            return null;
        }
    }

    public static class WaitOrWalkResult implements Parcelable {

        private WaitOrWalkResultType type;
        private Stop stop;
        private Departure upcomingDeparture;
        private long remainingTimeMillis;
        @Nullable private DirectionsService.DirectionsResult walkingDirections;

        public WaitOrWalkResult(WaitOrWalkResultType type, Stop stop, Departure upcomingDeparture,
                                long remainingTimeMillis, @Nullable DirectionsService.DirectionsResult walkingDirections) {
            this.type = type;
            this.stop = stop;
            this.upcomingDeparture = upcomingDeparture;
            this.remainingTimeMillis = remainingTimeMillis;
            this.walkingDirections = walkingDirections;
        }

        public Departure getUpcomingDeparture() {
            return upcomingDeparture;
        }

        @Nullable
        public DirectionsService.DirectionsResult getWalkingDirections() {
            return walkingDirections;
        }

        public Stop getStop() {
            return stop;
        }

        public WaitOrWalkResultType getType() {
            return type;
        }

        public long getRemainingTimeMillis() {
            return remainingTimeMillis;
        }

        public void setType(WaitOrWalkResultType type) {
            this.type = type;
        }

        public void setStop(Stop stop) {
            this.stop = stop;
        }

        public void setUpcomingDeparture(Departure upcomingDeparture) {
            this.upcomingDeparture = upcomingDeparture;
        }

        public void setRemainingTimeMillis(long remainingTimeMillis) {
            this.remainingTimeMillis = remainingTimeMillis;
        }

        public void setWalkingDirections(@Nullable DirectionsService.DirectionsResult walkingDirections) {
            this.walkingDirections = walkingDirections;
        }

        /**
         * Parcelable implementation
         * Note that DirectionsService.DirectionsResult is not retained.
         */

        public WaitOrWalkResult(Parcel in) {
            type = WaitOrWalkResultType.getTypeByValue(in.readInt());
            stop = in.readParcelable(Stop.class.getClassLoader());
            upcomingDeparture = in.readParcelable(Departure.class.getClassLoader());
            remainingTimeMillis = in.readLong();
        }

        public static final Creator<WaitOrWalkResult> CREATOR = new Creator<WaitOrWalkResult>() {
            @Override
            public WaitOrWalkResult createFromParcel(Parcel in) {
                return new WaitOrWalkResult(in);
            }

            @Override
            public WaitOrWalkResult[] newArray(int size) {
                return new WaitOrWalkResult[size];
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
            dest.writeLong(remainingTimeMillis);
        }
    }
}
