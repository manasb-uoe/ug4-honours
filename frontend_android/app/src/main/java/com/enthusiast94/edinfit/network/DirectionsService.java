package com.enthusiast94.edinfit.network;

import android.content.Context;

import com.enthusiast94.edinfit.models.Directions;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by manas on 10-10-2015.
 */
public class DirectionsService {

    public static final String TAG = DirectionsService.class.getSimpleName();
    private static DirectionsService instance;
    private Context context;
    private BaseService baseService = BaseService.getInstance();

    private DirectionsService(Context context) {
        this.context = context;
    }

    public static void init(Context context) {
        if (instance != null) {
            throw new IllegalStateException(TAG + " has already been initialized.");
        }

        instance = new DirectionsService(context);
    }

    public static DirectionsService getInstance() {
        if (instance == null) {
            throw new IllegalStateException(TAG + " has not been initialized yet.");
        }

        return instance;
    }

    public BaseService.Response<Directions> getWalkingDirections(LatLng origin, LatLng destination) {

        Request request = baseService.createEdinfitGetRequest("/walking-directions?origin=" +
                origin.latitude + "," + origin.longitude + "&destination=" + destination.latitude +
                "," + destination.longitude);
        BaseService.Response<Directions> response = new BaseService.Response<>();

        try {
            Response okHttpResponse = baseService.getHttpClient().newCall(request).execute();

            if (!response.isSuccessfull()) {
                response.setError(baseService.extractEdinfitErrorMessage(okHttpResponse));
                return response;
            }

            Gson gson = new Gson();
            Directions directions = gson.fromJson(new JSONObject(okHttpResponse.body().string())
                    .getJSONObject("data").toString(), Directions.class);

            response.setBody(directions);
            return response;

        } catch (IOException | JSONException e) {
            response.setError(e.getMessage());
            return response;
        }
    }
}
