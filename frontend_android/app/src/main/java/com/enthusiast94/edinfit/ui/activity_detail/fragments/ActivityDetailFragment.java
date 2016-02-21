package com.enthusiast94.edinfit.ui.activity_detail.fragments;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models.Activity;
import com.enthusiast94.edinfit.network.UserService;
import com.enthusiast94.edinfit.utils.Helpers;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.ui.IconGenerator;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by manas on 31-12-2015.
 */
public class ActivityDetailFragment extends Fragment {

    public static final String TAG = ActivityDetailFragment.class.getSimpleName();
    private static final String EXTRA_ACTIVITY_ID = "activityId";
    private static final String MAPVIEW_SAVE_STATE = "mapViewSaveState";

    private MapView mapView;

    private GoogleMap map;

    public static ActivityDetailFragment newInstance(long activityId) {
        Bundle bundle = new Bundle();
        bundle.putLong(EXTRA_ACTIVITY_ID, activityId);

        ActivityDetailFragment instance = new ActivityDetailFragment();
        instance.setArguments(bundle);

        return instance;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_activity_detail, container, false);

        // find views
        mapView = (MapView) view.findViewById(R.id.map_view);
        TextView originDestinationTextView =
                (TextView) view.findViewById(R.id.origin_destination_textview);
        TextView infoTextView = (TextView) view.findViewById(R.id.info_textview);
        TextView distanceTextView = (TextView) view.findViewById(R.id.distance_textview);
        TextView timeTextView = (TextView) view.findViewById(R.id.time_textview);
        TextView speedTextView = (TextView) view.findViewById(R.id.speed_textview);
        TextView stepsTextView = (TextView) view.findViewById(R.id.steps_textview);
        TextView caloriesTextView = (TextView) view.findViewById(R.id.calories_textview);

        // setup map
        Bundle mapViewSavedInstanceState = savedInstanceState != null ?
                savedInstanceState.getBundle(MAPVIEW_SAVE_STATE) : null;
        mapView.onCreate(mapViewSavedInstanceState);
        map = mapView.getMap();
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(Helpers.getEdinburghLatLng(getActivity()), 12));

        // get selected activity using the id provided in the arguments
        Activity activity = Activity.findById(getArguments().getLong(EXTRA_ACTIVITY_ID));

        distanceTextView.setText(Helpers.humanizeDistance(activity.getDistance() / 1000));
        timeTextView.setText(Helpers.humanizeDurationInMillis(activity.getEnd() - activity.getStart()));
        stepsTextView.setText(String.valueOf(Helpers.getStepsFromDistance(activity.getDistance())));
        caloriesTextView.setText(String.format("%d kcal", Helpers.getCaloriesBurnt(
                UserService.getInstance().getAuthenticatedUser().getWeight(), activity.getDistance() / 1000.0)));
        speedTextView.setText(String.format(getString(R.string.label_speed_format),
                activity.getAverageSpeed() * (60 * 60 / (1000.0)))); // convert from m/s to km/h

        String dateAndTime = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.UK)
                .format(new Date(activity.getStart()));

        originDestinationTextView.setText(activity.getDescription());
        infoTextView.setText(String.format(getString(R.string.label_activity_info_format),
                Helpers.getActivityTypeText(getActivity(), activity.getType()), dateAndTime));

        PolylineOptions polylineOptions = new PolylineOptions();
        int polyLineColor = ContextCompat.getColor(getActivity(), R.color.blue_500);
        int polylineWidth = getResources().getDimensionPixelOffset(R.dimen.polyline_width);
        SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm", Locale.UK);
        for (int i=0; i<activity.getPoints().size(); i++) {
            Activity.Point point = activity.getPoints().get(i);
            polylineOptions
                    .add(new LatLng(point.getLatitude(), point.getLongitude()))
                    .width(polylineWidth)
                    .color(polyLineColor);

            if (i == 0 || i == activity.getPoints().size()-1) {
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(new LatLng(point.getLatitude(), point.getLongitude()));
                markerOptions.title(sdfTime.format(point.getTimestamp()));
                if (i ==0) {
                    IconGenerator iconGenerator = new IconGenerator(getActivity());
                    iconGenerator.setStyle(IconGenerator.STYLE_GREEN);
                    Bitmap startIconBitmap = iconGenerator.makeIcon(getString(R.string.label_start));
                    markerOptions.icon(BitmapDescriptorFactory.fromBitmap(startIconBitmap));
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(markerOptions.getPosition(),
                            14));
                }
                if (i == activity.getPoints().size()-1) {
                    IconGenerator iconGenerator = new IconGenerator(getActivity());
                    iconGenerator.setStyle(IconGenerator.STYLE_PURPLE);
                    Bitmap finishIconBitmap = iconGenerator.makeIcon(getString(R.string.finish));
                    markerOptions.icon(BitmapDescriptorFactory.fromBitmap(finishIconBitmap));
                }

                map.addMarker(markerOptions);
            }
        }
        map.addPolyline(polylineOptions);


        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        //This MUST be done before saving any of your own or your base class's variables
        Bundle mapViewSaveState = new Bundle(outState);
        mapView.onSaveInstanceState(mapViewSaveState);
        outState.putBundle(MAPVIEW_SAVE_STATE, mapViewSaveState);
        //Add any other variables here.
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}
