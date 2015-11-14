package com.enthusiast94.edinfit.services;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models.Departure;
import com.enthusiast94.edinfit.models.Directions;
import com.enthusiast94.edinfit.models.Stop;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cz.msebera.android.httpclient.Header;

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

    public void getWaitOrWalkSuggestions(String routeDestination, String serviceName,
                                         String originStopId, String destinationStopId,
                                         LatLng userLocation,
                                         final Callback<List<WaitOrWalkSuggestion>> callback) {

        RequestParams requestParams = new RequestParams();
        requestParams.put("origin_stop", originStopId);
        requestParams.put("destination_stop", destinationStopId);
        requestParams.put("service", serviceName);
        requestParams.put("route", routeDestination);
        requestParams.put("user_location", userLocation.latitude + "," + userLocation.longitude);
        requestParams.put("max_number_of_stops_to_skip", 5);

        AsyncHttpClient client = getAsyncHttpClient(true);
        client.get(API_BASE + "/wait-or-walk-suggestions", requestParams, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    Gson gson = new Gson();
                    WaitOrWalkSuggestion[] waitOrWalkSuggestionsArray = gson.fromJson(
                            response.getJSONArray("data").toString(), WaitOrWalkSuggestion[].class
                    );
                    // return mutable list
                    List<WaitOrWalkSuggestion> waitOrWalkSuggestions =
                            new ArrayList<>(Arrays.asList(waitOrWalkSuggestionsArray));

                    callback.onSuccess(waitOrWalkSuggestions);

                } catch (JSONException e) {
                    callback.onFailure(context.getString(R.string.error_parsing));
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                onFailureCommon(statusCode, errorResponse, callback);
            }
        });
    }

    public enum WaitOrWalkSuggestionType {
        WAIT(0), WALK(1), ;

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

    public static class WaitOrWalkSuggestion implements Parcelable {

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
            stop = in.readParcelable(Stop.class.getClassLoader());
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
    }
}
