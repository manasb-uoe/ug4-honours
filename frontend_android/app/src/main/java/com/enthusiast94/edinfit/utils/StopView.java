package com.enthusiast94.edinfit.utils;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models_2.Stop;
import com.google.android.gms.maps.model.LatLng;

/**
 * Created by manas on 04-01-2016.
 */
public class StopView extends CardView {

    private Context context;

    private TextView stopNameTextView;
    private TextView servicesTextView;
    private TextView destinationsTextView;
    private TextView idTextView;
    private TextView walkDurationTextView;
    private ImageView starImageView;

    public StopView(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.context = context;

        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.view_stop, this, true);

        stopNameTextView = (TextView) view.findViewById(R.id.stop_name_textview);
        servicesTextView = (TextView) view.findViewById(R.id.services_textview);
        destinationsTextView = (TextView) view.findViewById(R.id.destinations_textview);
        idTextView = (TextView) view.findViewById(R.id.stop_id_textview);
        walkDurationTextView = (TextView) view.findViewById(R.id.walk_duration_textview);
        starImageView = (ImageView) view.findViewById(R.id.star_imageview);
    }

    public void bindItem(Stop stop, boolean isFavourite, LatLng latLng) {
        stopNameTextView.setText(String.format(context.getString(
                R.string.label_stop_name_with_direction), stop.getName(), stop.getDirection()));

        // combine list of services into comma separated string
        String services = "";
        if (stop.getServices().size() > 0) {
            for (String service : stop.getServices()) {
                services += service + ", ";
            }
            services = services.substring(0, services.length() - 2);
        } else {
            services = context.getString(R.string.label_none);
        }
        servicesTextView.setText(services);

        // combine list of destinations into comma separated string
        String destinations = "";
        if (stop.getDestinations().size() > 0) {
            for (String destination : stop.getDestinations()) {
                destinations += destination + ", ";
            }
            destinations = destinations.substring(0, destinations.length() - 2);
        } else {
            destinations = context.getString(R.string.label_none);
        }
        destinationsTextView.setText(destinations);

        idTextView.setText(stop.get_id());

        if (latLng != null) {
            LatLng stopLatLng = stop.getPosition();
            Double distanceAway = Helpers.getDistanceBetweenPoints(stopLatLng.latitude,
                    stopLatLng.longitude, latLng.latitude, latLng.longitude) / 1000.0;
            walkDurationTextView.setVisibility(View.VISIBLE);
            walkDurationTextView.setText(Helpers.getWalkingDurationFromDistance(distanceAway));
        } else {
            walkDurationTextView.setVisibility(View.GONE);
        }

        if (isFavourite) {
            starImageView.setVisibility(View.VISIBLE);
        } else {
            starImageView.setVisibility(View.GONE);
        }
    }
}
