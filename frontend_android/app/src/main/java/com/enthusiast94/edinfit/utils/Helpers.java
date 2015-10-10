package com.enthusiast94.edinfit.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Patterns;
import android.view.inputmethod.InputMethodManager;

import com.enthusiast94.edinfit.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by manas on 26-09-2015.
 */
public class Helpers {

    public static void hideSoftKeyboard(Context context, IBinder windowToken) {
        InputMethodManager inputMethodManager =
                (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(windowToken, 0);
    }

    public static String validateEmail(String email, Resources res) {
        if (email.length() == 0) {
            return res.getString(R.string.error_required_field);
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return res.getString(R.string.error_invalid_email);
        } else {
            return null;
        }
    }

    public static String validatePassword(String password, Resources res) {
        if (password.length() == 0) {
            return  res.getString(R.string.error_required_field);
        } else if (password.length() < 6) {
            return res.getString(R.string.error_short_password);
        } else {
            return null;
        }
    }

    public static String validateName(String name, Resources res) {
        if (name.length() == 0) {
            return res.getString(R.string.error_required_field);
        } else {
            return null;
        }
    }

    public static void writeToPrefs(Context context, String key, String value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString(key, value).apply();
    }

    public static String readFromPrefs(Context context, String key) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(key, null);
    }

    public static void clearPrefs(Context context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().apply();
    }

    public static String getCurrentTime24h() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.UK);
        return sdf.format(new Date());
    }

    public static int getDayCode(String day) {
        day = day.toLowerCase();

        switch (day) {
            case "monday":
            case "tuesday":
            case "wednesday":
            case "thursday":
            case "friday":
                return 0;
            case "saturday":
                return 5;
            case "sunday":
                return 6;
            default: throw new IllegalArgumentException("Invalid day.");
        }
    }

    public static String getCurrentDay() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE", Locale.UK);
        return sdf.format(new Date());
    }

    public static Bitmap getStopMarkerIcon(Context context) {
        Resources res = context.getResources();
        int px = res.getDimensionPixelSize(R.dimen.stop_marker_size);
        Bitmap iconBitmap = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(iconBitmap);
        Drawable shape = ResourcesCompat.getDrawable(res, R.drawable.stop_marker, null);
        shape.setBounds(0, 0, iconBitmap.getWidth(), iconBitmap.getHeight());
        shape.draw(canvas);

        return iconBitmap;
    }
}
