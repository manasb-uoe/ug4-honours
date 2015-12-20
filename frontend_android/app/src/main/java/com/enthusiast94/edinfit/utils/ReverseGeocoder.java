package com.enthusiast94.edinfit.utils;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import android.util.Log;

import com.enthusiast94.edinfit.R;

import java.io.IOException;
import java.util.List;

/**
 * Created by manas on 26-11-2015.
 */
public class ReverseGeocoder  {

    private static final String TAG = ReverseGeocoder.class.getSimpleName();

    private Geocoder geocoder;
    private Handler handler;
    private String errorMessage;

    public ReverseGeocoder(Context context) {
        geocoder = new Geocoder(context);
        handler = new Handler();
        errorMessage = context.getString(R.string.error_could_not_geocode_location);
    }

    public interface ReverseGeocodeCallback {
        void onSuccess(String placeName);
        void onFailure(String error);
    }

    public void getPlaceName(final double lat, final double lng,
                             final ReverseGeocodeCallback callback) {

        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                List<Address> list = null;

                try {
                    list = geocoder.getFromLocation(lat, lng, 1);
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                }

                if (list != null && list.size() > 0) {
                    final String placeName = list.get(0).getThoroughfare();

                    if (placeName != null) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(placeName);
                            }
                        });
                    } else {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFailure(errorMessage);
                            }
                        });
                    }

                    return;
                }

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onFailure(errorMessage);
                    }
                });
            }
        });
        thread.start();
    }
}
