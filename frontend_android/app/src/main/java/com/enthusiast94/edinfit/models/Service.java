package com.enthusiast94.edinfit.models;

import android.text.TextUtils;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by manas on 10-01-2016.
 */
@Table(name = "Services")
public class Service extends Model {

    @Column private String name;
    @Column private String description;
    @Column private String serviceType;
    @Column private String routes;

    public Service() {
        super();
    }

    public Service(String name, String description, String serviceType, String routes) {
        this.name = name;
        this.description = description;
        this.serviceType = serviceType;
        this.routes = routes;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getServiceType() {
        return serviceType;
    }

    public List<Route> getRoutes() {
        List<Route> routesList = new ArrayList<>();

        try {
            JSONArray routesJsonArray = new JSONArray(routes);
            for (int i=0; i<routesJsonArray.length(); i++) {
                JSONObject routeJson = routesJsonArray.getJSONObject(i);

                String destination = routeJson.getString("destination");

                JSONArray pointsJsonArray = routeJson.getJSONArray("points");
                List<Point> points = new ArrayList<>();
                for (int j=0; j<pointsJsonArray.length(); j++) {
                    JSONObject pointJson = pointsJsonArray.getJSONObject(j);
                    points.add(new Point(pointJson.getString("stop_id"),
                            pointJson.getDouble("latitude"), pointJson.getDouble("longitude")));
                }

                JSONArray stopsJsonArray = routeJson.getJSONArray("stops");
                List<Stop> stops = new ArrayList<>();
                for (int j=0; j<stopsJsonArray.length(); j++) {
                    stops.add(Stop.findById(stopsJsonArray.getString(j)));
                }

                routesList.add(new Route(destination, stops, points));
            }

            return routesList;

        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static class Route {

        private String destination;
        private List<Stop> stops;
        private List<Point> points;

        public Route(String destination, List<Stop> stops,
                     List<Point> points) {
            this.destination = destination;
            this.stops = stops;
            this.points = points;
        }

        public String getDestination() {
            return destination;
        }

        public List<Point> getPoints() {
            return points;
        }

        public List<Stop> getStops() {
            return stops;
        }

        // returns a list of LatLngs extracted from the list of Points
        public List<LatLng> getLatLngs() {
            List<LatLng> latLngs = new ArrayList<>();
            for (Point point : points) {
                latLngs.add(point.getLatLng());
            }
            return latLngs;
        }
    }

    public static class Point {

        private String stopId;
        private double latitude;
        private double longitude;

        public Point(String stopId, double latitude, double longitude) {
            this.stopId = stopId;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public String getStopId() {
            return stopId;
        }

        public LatLng getLatLng() {
            return new LatLng(latitude, longitude);
        }
    }

    /**
     * Statics
     */

    public static int getCount() {
        return new Select()
                .from(Service.class)
                .count();
    }

    public static List<Service> getAll() {
        return new Select("Id", "name", "description")
                .from(Service.class)
                .execute();
    }

    public static void deleteAll() {
        new Delete()
                .from(Service.class)
                .execute();
    }

    public static Service findByName(String name) {
        return new Select()
                .from(Service.class)
                .where("name = ?" , name)
                .executeSingle();
    }

    public static List<Service> findByNames(List<String> names) {
        return new Select()
                .from(Service.class)
                .where("name IN (" + TextUtils.join(",", Collections.nCopies(names.size(), "?")) + ")", names.toArray())
                .execute();
    }
}
