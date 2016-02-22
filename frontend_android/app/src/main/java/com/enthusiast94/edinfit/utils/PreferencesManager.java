package com.enthusiast94.edinfit.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.enthusiast94.edinfit.models.Disruption;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class PreferencesManager {

    private static final String TAG = PreferencesManager.class.getSimpleName();
    private static final String USER_ID_KEY = "userIdKey";
    private static final String DISRUPTION_IDS = "disruptionIds";
    private static PreferencesManager instance;
    private Context context;
    private SharedPreferences prefs;

    public PreferencesManager(Context context) {
        this.context = context;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static void init(Context context) {
        if (instance != null) {
            throw new IllegalStateException(TAG + " has already been initialized.");
        }
        instance = new PreferencesManager(context);
    }

    public static PreferencesManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException(TAG + " has not been initialized yet.");
        }
        return instance;
    }

    public void setCurrentlyAuthenticatedUserId(String userId) {
        prefs.edit().putString(USER_ID_KEY, userId).apply();
    }

    public String getCurrentlyAuthenticatedUserId() {
        return prefs.getString(USER_ID_KEY, null);
    }

    public void setDisruptionIds(List<Disruption> disruptions) {
        List<Integer> ids = new ArrayList<>();
        for (Disruption disruption : disruptions) {
            ids.add(disruption.getId());
        }

        Gson gson = new Gson();
        prefs.edit().putString(DISRUPTION_IDS, gson.toJson(ids)).apply();
    }

    public List<Integer> getDisruptionIds() {
        String idsString = prefs.getString(DISRUPTION_IDS, null);
        if (idsString == null) {
            return new ArrayList<>();
        }

        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<Integer>>(){}.getType();
        return gson.fromJson(idsString, type);
    }

    private void write(String key, String value) {
        prefs.edit().putString(key, value).apply();
    }

    private String read(String key) {
        return prefs.getString(key, null);
    }

    public void clearPrefs() {
        prefs.edit().clear().apply();
    }
}
