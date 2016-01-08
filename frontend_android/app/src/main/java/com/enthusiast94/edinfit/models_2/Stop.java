package com.enthusiast94.edinfit.models_2;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import com.activeandroid.util.SQLiteUtils;
import com.enthusiast94.edinfit.utils.Helpers;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by manas on 08-01-2016.
 */

@Table(name = "Stops")
public class Stop extends Model {

    @Column private String _id; /* stop id returned from TFE API */
    @Column private String name;
    @Column private double latitude;
    @Column private double longitude;
    @Column private String direction;
    @Column private String destinations;
    @Column private String services;

    public Stop() {
        super();
    }

    public Stop(String _id, String name, double latitude, double longitude, String direction,
                String destinations, String services) {
        this._id = _id;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.direction = direction;
        this.destinations = destinations;
        this.services = services;
    }


    public String get_id() {
        return _id;
    }

    public String getName() {
        return name;
    }

    public LatLng getPosition() {
        return new LatLng(latitude, longitude);
    }

    public String getDirection() {
        return direction;
    }

    public List<String> getDestinations() {
        return Helpers.getListFromJsonString(destinations);
    }

    public List<String> getServices() {
        return Helpers.getListFromJsonString(services);
    }

    /**
     * Statics
     */

    public static int getCount() {
        return new Select()
                .from(Stop.class)
                .execute()
                .size();
    }

    public static List<Stop> getAll() {
        return new Select()
                .from(Stop.class)
                .execute();
    }

    public static List<Stop> getNearby(LatLng latLng, double maxDistance,
                                       int limit) {
        List<Stop> nearestStops = SQLiteUtils.rawQuery(Stop.class, "SELECT * FROM Stops S " +
                        "ORDER BY ABS(ABS(S.latitude - ?) + ABS(S.longitude - ?)) ASC LIMIT ?",
                new String[]{String.valueOf(latLng.latitude), String.valueOf(latLng.longitude),
                        String.valueOf(limit)});

        List<Stop> requiredStops = new ArrayList<>();
        for (Stop stop : nearestStops) {
            if (Helpers.getDistanceBetweenPoints(stop.latitude, stop.longitude, latLng.latitude,
                    latLng.longitude) <= maxDistance * 1000) {
                requiredStops.add(stop);
            } else {
                break;
            }
        }

        return requiredStops;
    }

    public static Stop findById(String id) {
        return new Select()
                .from(Stop.class)
                .where("_id = ?", id)
                .executeSingle();
    }
}
