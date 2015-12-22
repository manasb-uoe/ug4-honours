package com.enthusiast94.edinfit.utils;

import android.app.ActivityManager;
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
            return String.format("%.1f", distanceInKm*1000) + " m";
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
        double minutes = ((millis / (1000.0 * 60.0)));
        return String.format("%.1f", minutes) + " min";
    }

    public static String humanizeLiveDepartureTime(String time) {
        long millis = getRemainingTimeMillisFromNow(time);
        double minutes = ((millis / (1000.0 * 60.0)));

        if (minutes > 60) {
            return time;
        } else if (minutes <= 1) {
            return "due";
        } else {
            return String.format("%.0f", minutes) + " min";
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

    public static CharSequence trimTrailingWhitespace(CharSequence text) {
        while (text.charAt(text.length() - 1) == '\n') {
            text = text.subSequence(0, text.length() - 1);
        }

        return text;
    }

    public static double getDistanceBetweenPoints(double lat1, double lng1, double lat2, double lng2) {
        int R = 6378137; // Earth’s mean radius in meter

        double dLat = Math.toRadians(lat2 - lat1);
        double dLong = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2)) * Math.sin(dLong / 2) * Math.sin(dLong / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 -a));

        double d = R * c;

        return d; // returns distance in meters
    }

    public static boolean isServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }

        return false;
    }

    public static int getStepsFromDistance(double distanceInM) {
        return (int) (distanceInM * 1.3123359580052494);
    }
}

