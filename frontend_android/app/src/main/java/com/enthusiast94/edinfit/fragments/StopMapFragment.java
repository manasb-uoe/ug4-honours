package com.enthusiast94.edinfit.fragments;

import android.graphics.Camera;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.events.OnStopLoadedEvent;
import com.enthusiast94.edinfit.models.Stop;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import de.greenrobot.event.EventBus;

/**
 * Created by manas on 04-10-2015.
 */
public class StopMapFragment extends Fragment {

    public static final String TAG = StopMapFragment.class.getSimpleName();
    private MapView mapView;
    private GoogleMap map;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stop_map, container, false);

        /**
         * Find and create map view
         */

        mapView = (MapView) view.findViewById(R.id.map_view);
        mapView.onCreate(savedInstanceState);

        /**
         * Get GoogleMap from MapView and initialize it
         */

        map = mapView.getMap();
        map.getUiSettings().setMyLocationButtonEnabled(true);
        map.setMyLocationEnabled(true);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        mapView.onResume();

        EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        mapView.onPause();

        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        mapView.onSaveInstanceState(outState);
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

    public void onEventMainThread(OnStopLoadedEvent event) {
        Stop stop = event.getStop();

        // add stop marker and zoom in
        LatLng latLng = new LatLng(stop.getLocation().get(1), stop.getLocation().get(0));
        map.addMarker(new MarkerOptions()
                .position(latLng)
                .title(stop.getName())).showInfoWindow();
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14));
    }
}
