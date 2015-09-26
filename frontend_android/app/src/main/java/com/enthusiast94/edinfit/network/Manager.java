package com.enthusiast94.edinfit.network;

import com.loopj.android.http.AsyncHttpClient;

/**
 * Created by manas on 26-09-2015.
 */
public class Manager {

    protected static final String API_BASE = "http://10.0.3.2:4000/api";
    private static final String USER_AGENT = "android:com.enthusiast94.edinfit";

    protected static AsyncHttpClient getAsyncHttpClient() {
        AsyncHttpClient client = new AsyncHttpClient();
        client.setUserAgent(USER_AGENT);

        return client;
    }
}
