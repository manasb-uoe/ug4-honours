package com.enthusiast94.edinfit.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class PreferencesManager {

    private static final String TAG = PreferencesManager.class.getSimpleName();
    private final String USER_ID_KEY = "userIdKey";
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
