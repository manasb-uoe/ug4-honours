package com.enthusiast94.edinfit.network;

import android.content.Context;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models.User;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * Created by manas on 26-09-2015.
 */
public class BaseService {

    private static final String TAG = BaseService.class.getSimpleName();
    public static final String API_BASE = "http://ec2-52-28-155-29.eu-central-1.compute.amazonaws.com:4000/api";
    public static final String TFE_API_BASE = "https://tfe-opendata.com/api/v1";
    public static final String USER_AGENT = "android:com.enthusiast94.edinfit";

    private static BaseService instance;
    private Context context;
    private OkHttpClient client;

    private BaseService(Context context) {
        this.context = context;
        client = new OkHttpClient();
    }

    public static void init(Context context) {
        if (instance != null) {
            throw new IllegalStateException(TAG + " has already been initialized.");
        }

        instance = new BaseService(context);
    }

    public static BaseService getInstance() {
        if (instance == null) {
            throw new IllegalStateException(TAG + " has not been initialized yet.");
        }

        return instance;
    }

    public OkHttpClient getHttpClient() {
        return client;
    }

    public Request createTfeGetRequest(String path) {
        return new Request.Builder()
                .url(TFE_API_BASE + "/" + path)
                .addHeader("Authorization", "Token " + context.getString(R.string.api_key_tfe))
                .build();
    }

    public Request createEdinfitGetRequest(String path) {
        return createEdinfitRequestBuilder(path).build();
    }

    public Request createEdinfitPostRequest(String path, RequestBody requestBody) {
        return createEdinfitRequestBuilder(path).post(requestBody).build();
    }

    public Request createEdinFitPutRequest(String path, RequestBody requestBody) {
        return createEdinfitRequestBuilder(path).put(requestBody).build();
    }

    private Request.Builder createEdinfitRequestBuilder(String path) {
        Request.Builder builder = new Request.Builder();
        builder.url(API_BASE + "/" + path);

        User user = UserService.getInstance().getAuthenticatedUser();
        if (user != null) {
            builder.addHeader("Authorization", "Bearer " + user.getAuthToken());
        }

        return builder;
    }

    public String extractEdinfitErrorMessage(okhttp3.Response response) {
        try {
            return new JSONObject(response.body().string()).getJSONObject("error").getString("message");
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static class Response<Type> {

        private Type body;
        private String error;

        public Response() {}

        public Response(Type body, String error) {
            this.body = body;
            this.error = error;
        }

        public Type getBody() {
            return body;
        }

        public void setBody(Type body) {
            this.body = body;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        public boolean isSuccessfull() {
            return error == null;
        }
    }
}
