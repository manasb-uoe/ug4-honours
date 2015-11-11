package com.enthusiast94.edinfit.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Log;
import android.util.Patterns;
import android.view.inputmethod.InputMethodManager;

import com.enthusiast94.edinfit.R;
import com.google.android.gms.maps.model.LatLng;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

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

    public static LatLng getEdinburghLatLng(Context context) {
        return new LatLng(
                Float.valueOf(context.getString(R.string.edinburgh_lat)),
                Float.valueOf(context.getString(R.string.edinburgh_long))
        );
    }

    public static String humanizeDistance(Double distanceInKm) {
        if (distanceInKm < 1) {
            Log.d("UTIL", distanceInKm + "");
            return String.format("%.0f", distanceInKm*1000) + " m";
        } else {
            return String.format("%.1f", distanceInKm) + " km";
        }
    }

    // currently only parses durations of format "x mins"
    public static long parseDirectionsApiDurationToMillis(String durationText) {
        String[] split = durationText.split("\\s");
        return (Integer.valueOf(split[0]) * 60 * 1000);
    }

    public static String humanizeDurationInMillis(long millis) {
        int seconds = (int) ((millis / 1000) % 60);
        int minutes = (int) ((millis / (1000 * 60)) % 60);
        int hours = (int) (millis / (1000 * 60 * 60) % 24);

        if (hours != 0) {
            return hours + "h " + minutes + "m " + seconds + "s";
        } else if (minutes != 0) {
            return minutes + "m " + seconds + "s";
        } else {
            return seconds + "s";
        }
    }

    public static String humanizeDurationInMillisToMinutes(long millis) {
        int minutes = (int) ((millis / (1000 * 60)));

        if (minutes == 1) {
            return minutes + " min";
        } else {
            return minutes + " mins";
        }
    }

    // time must be in 24h form14 (eg. 08:55)
    public static long getRemainingTimeMillisFromNow(String time) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.UK);
        Calendar calendar = Calendar.getInstance();
        String dateText = simpleDateFormat.format(calendar.getTime());

        simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.UK);

        try {
            Date parsedDate = simpleDateFormat.parse(dateText + " " + time);
            Date now = new Date();

            return parsedDate.getTime() - now.getTime();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
}

