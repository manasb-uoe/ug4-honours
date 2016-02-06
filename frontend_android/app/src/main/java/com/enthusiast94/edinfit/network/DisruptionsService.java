package com.enthusiast94.edinfit.network;

import com.enthusiast94.edinfit.models.Disruption;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by manas on 06-02-2016.
 */
public class DisruptionsService {

    private static final String TAG = DisruptionsService.class.getSimpleName();
    private BaseService baseService;
    private static DisruptionsService instance;

    private DisruptionsService() {
        baseService = BaseService.getInstance();
    }

    public static DisruptionsService getInstance() {
        if (instance == null) {
            instance = new DisruptionsService();
        }
        return instance;
    }

    public BaseService.Response<List<Disruption>> getDisriptions() {
        Request request = baseService.createTfeGetRequest("status");
        BaseService.Response<List<Disruption>> response = new BaseService.Response<>();

        try {
            Response okHttpResponse = baseService.getHttpClient().newCall(request).execute();

            if (!okHttpResponse.isSuccessful()) {
                response.setError(okHttpResponse.message());
                return response;
            }

            JSONArray disruptionsJsonArray =
                    new JSONObject(okHttpResponse.body().string()).getJSONArray("disruptions");
            List<Disruption> disruptions = new ArrayList<>();

            for (int i=0; i<disruptionsJsonArray.length(); i++) {
                JSONObject disruptionJson = disruptionsJsonArray.getJSONObject(i);

                List<String> servicesAffected = new ArrayList<>();
                JSONArray servicesAffectedJsonArray = disruptionJson.getJSONArray("services_affected");
                for (int j=0; j<servicesAffectedJsonArray.length(); j++) {
                    servicesAffected.add(servicesAffectedJsonArray.getString(j));
                }

                disruptions.add(new Disruption(
                        disruptionJson.getString("type"),
                        disruptionJson.getString("category"),
                        disruptionJson.getString("summary"),
                        servicesAffected,
                        disruptionJson.getString("web_link"),
                        disruptionJson.getLong("updated")
                ));
            }

            response.setBody(disruptions);
            return response;

        } catch (IOException | JSONException e) {
            e.printStackTrace();
            response.setError(e.getMessage());
            return response;
        }
    }
}
