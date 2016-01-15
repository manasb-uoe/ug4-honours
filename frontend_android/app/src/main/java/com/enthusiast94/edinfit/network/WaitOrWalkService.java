package com.enthusiast94.edinfit.network;

import android.content.Context;

import com.enthusiast94.edinfit.models.WaitOrWalkSuggestion;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by manas on 11-11-2015.
 */
public class WaitOrWalkService {

    private static final String TAG = WaitOrWalkService.class.getSimpleName();
    private static WaitOrWalkService instance;
    private Context context;
    private BaseService baseService;

    private WaitOrWalkService(Context context) {
        this.context = context;
        baseService = BaseService.getInstance();
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

    public BaseService.Response<List<WaitOrWalkSuggestion>> getWaitOrWalkSuggestions(
            String routeDestination, String serviceName, String originStopId,
            String destinationStopId, LatLng userLocation) {

        BaseService.Response<List<WaitOrWalkSuggestion>> response = new BaseService.Response<>();
        Request request = baseService.createEdinfitGetRequest("wait-or-walk-suggestions/?origin_stop=" +
        originStopId + "&destination_stop=" + destinationStopId + "&service=" + serviceName + "&route=" +
                routeDestination + "&user_location=" + userLocation.latitude + "," + userLocation.longitude +
                "&max_number_of_stops_to_skip=5");

        try {
            Response okHttpResponse = baseService.getHttpClient().newCall(request).execute();

            if (!okHttpResponse.isSuccessful()) {
                response.setError(okHttpResponse.message());
                return response;
            }

            response.setBody(new ArrayList<WaitOrWalkSuggestion>());
            return response;

        } catch (IOException e) {
            e.printStackTrace();
            response.setError(e.getMessage());
            return response;
        }
    }
}
