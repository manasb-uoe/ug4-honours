package com.enthusiast94.edinfit.ui.wair_or_walk_mode.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.services.DirectionsService;
import com.enthusiast94.edinfit.services.LocationProviderService;
import com.enthusiast94.edinfit.ui.wair_or_walk_mode.events.OnWaitOrWalkResultComputedEvent;
import com.enthusiast94.edinfit.utils.Helpers;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import de.greenrobot.event.EventBus;

/**
 * Created by manas on 04-11-2015.
 */
public class WalkingDirectionsFragment extends Fragment {

    private static final String MAPVIEW_SAVE_STATE = "mapViewSaveState";

    private RecyclerView directionsRecyclerView;
    //    private DirectionsAdapter departuresAdapter;
    private MapView mapView;

    private GoogleMap map;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wait_or_walk_walking_directions, container, false);

        /**
         * Find views
         */

//        directionsRecyclerView = (RecyclerView) view.findViewById(R.id.directions_recyclerview);
        mapView = (MapView) view.findViewById(R.id.map_view);

        /**
         * Create MapView, get GoogleMap from it and then configure the GoogleMap
         */

        Bundle mapViewSavedInstanceState = savedInstanceState != null ?
                savedInstanceState.getBundle(MAPVIEW_SAVE_STATE) : null;
        mapView.onCreate(mapViewSavedInstanceState);

        map = mapView.getMap();
        map.getUiSettings().setMyLocationButtonEnabled(true);
        map.setMyLocationEnabled(true);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(Helpers.getEdinburghLatLng(getActivity()), 12));

        /**
         * Setup directions recycler view
         */

//        directionsRecyclerView.setAdapter(null);
//        directionsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

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

    public void onEventMainThread(final OnWaitOrWalkResultComputedEvent event) {
        LocationProviderService.getInstance().requestLastKnownLocationInfo(new LocationProviderService.LocationCallback() {

            @Override
            public void onLocationSuccess(LatLng latLng, String placeName) {
                if (getActivity() != null) {
                    // clear all markers and polylines
                    map.clear();

                    // add stop marker with info window containing stop name
                    LatLng stopLatLng = new LatLng(
                            event.getWaitOrWalkResult().getStop().getLocation().get(1),
                            event.getWaitOrWalkResult().getStop().getLocation().get(0)
                    );

                    Marker marker= map.addMarker(new MarkerOptions()
                            .position(stopLatLng)
                            .icon(BitmapDescriptorFactory.fromBitmap(Helpers.getStopMarkerIcon(getActivity())))
                            .title(event.getWaitOrWalkResult().getStop().getName()));

                    // move map focus to stop marker
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(stopLatLng, 16));

                    if (event.getWaitOrWalkResult().getType() == ResultFragment.WaitOrWalkResultType.WALK) {
                        // add walking route from user's last known location to stop
                        DirectionsService.DirectionsResult directionsResult = event.getWaitOrWalkResult().getWalkingDirections();

                        if (directionsResult != null) {
                            PolylineOptions polylineOptions = directionsResult.getPolylineOptions()
                                    .color(ContextCompat.getColor(getActivity(), R.color.red))
                                    .width(getResources().getDimensionPixelOffset(R.dimen.polyline_width));
                            map.addPolyline(polylineOptions);

                            marker.setSnippet(directionsResult.getRoute().getDistanceText());
                            marker.showInfoWindow();
                        }
                    } else {
                        marker.setSnippet(getString(R.string.label_wait_here));
                        marker.showInfoWindow();
                    }
                }
            }

            @Override
            public void onLocationFailure(String error) {
                if (getActivity() != null) {
                    Toast.makeText(getActivity(), getString(R.string.error_could_not_fetch_directions),
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}
