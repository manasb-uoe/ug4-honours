package com.enthusiast94.edinfit.network;

import android.content.Context;

import com.enthusiast94.edinfit.models_2.Service;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by manas on 06-10-2015.
 */
public class ServiceService {

    public static final String TAG = ServiceService.class.getSimpleName();
    private static ServiceService instance;
    private String parsingErrorMessage;
    private BaseService baseService;

    private ServiceService(Context context) {
        baseService = BaseService.getInstance();
    }

    public static void init(Context context) {
        if (instance != null) {
            throw new IllegalStateException(TAG + " has already been initialized.");
        }

        instance = new ServiceService(context);
    }

    public static ServiceService getInstance() {
        if (instance == null) {
            throw new IllegalStateException(TAG + " has not been initialized yet.");
        }

        return instance;
    }

    public BaseService.Response<Void> populateServices() {
        // first delete all existing services
        Service.deleteAll();

        BaseService.Response<Void> response = new BaseService.Response<>();
        Request request = baseService.createTfeGetRequest("services");

        try {
            Response okHttpResponse = baseService.getHttpClient().newCall(request).execute();

            if (!okHttpResponse.isSuccessful()) {
                response.setError(okHttpResponse.message());
                return response;
            }

            JSONArray servicesJsonArray =
                    new JSONObject(okHttpResponse.body().string()).getJSONArray("services");

            for (int i=0; i<servicesJsonArray.length(); i++) {
                JSONObject serviceJson = servicesJsonArray.getJSONObject(i);
                Service service = new Service(
                        serviceJson.getString("name"),
                        serviceJson.getString("description"),
                        serviceJson.getString("service_type"),
                        serviceJson.getString("routes")
                );
                service.save();
            }

            return response;

        } catch (IOException | JSONException e) {
            e.printStackTrace();
            response.setError(e.getMessage());
            return response;
        }
    }

}
