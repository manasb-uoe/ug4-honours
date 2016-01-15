package com.enthusiast94.edinfit.network;

import android.content.Context;

import com.enthusiast94.edinfit.models_2.LiveBus;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by manas on 18-10-2015.
 */
public class LiveBusService {

    public static final String TAG = LiveBusService.class.getSimpleName();
    private static LiveBusService instance;
    private Context context;
    private BaseService baseService;

    private LiveBusService(Context context) {
        this.context = context;
        baseService = BaseService.getInstance();
    }

    public static void init(Context context) {
        if (instance != null) {
            throw new IllegalStateException(TAG + " has already been initialized.");
        }

        instance = new LiveBusService(context);
    }

    public static LiveBusService getInstance() {
        if (instance == null) {
            throw new IllegalStateException(TAG + " has not been initialized yet.");
        }

        return instance;
    }

    public BaseService.Response<List<LiveBus>> getLiveBuses(final String serviceName) {
        BaseService.Response<List<LiveBus>> response = new BaseService.Response<>();
        Request request = baseService.createTfeGetRequest("vehicle_locations");

        try {
            Response okHttpResponse = baseService.getHttpClient().newCall(request).execute();

            if (!okHttpResponse.isSuccessful()) {
                response.setError(okHttpResponse.message());
                return response;
            }

            // parse api response in order to retrieve live buses
            String vehiclesJson = new JSONObject(okHttpResponse.body().string())
                    .getJSONArray("vehicles").toString();
            Gson gson = new Gson();
            LiveBus[] liveBusesArray = gson.fromJson(vehiclesJson, LiveBus[].class);

            // filter out buses that do not belong to requested service
            ArrayList<LiveBus> liveBusesFiltered = new ArrayList<>();
            for (LiveBus liveBus : liveBusesArray) {
                if (liveBus.getServiceName() != null &&
                        liveBus.getServiceName().equals(serviceName)) {
                    liveBusesFiltered.add(liveBus);
                }
            }

            response.setBody(liveBusesFiltered);
            return response;

        } catch (IOException | JSONException e) {
            e.printStackTrace();
            response.setError(e.getMessage());
            return response;
        }
    }
}
