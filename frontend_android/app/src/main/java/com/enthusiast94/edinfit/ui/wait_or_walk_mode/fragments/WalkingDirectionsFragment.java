package com.enthusiast94.edinfit.ui.wait_or_walk_mode.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models.Directions;
import com.enthusiast94.edinfit.models.WaitOrWalkSuggestion;
import com.enthusiast94.edinfit.ui.wait_or_walk_mode.events.OnWaitOrWalkSuggestionSelected;
import com.enthusiast94.edinfit.utils.Helpers;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.List;

import de.greenrobot.event.EventBus;

/**
 * Created by manas on 04-11-2015.
 */
public class WalkingDirectionsFragment extends Fragment {

    public static final String TAG = WalkingDirectionsFragment.class.getSimpleName();
    private static final String MAPVIEW_SAVE_STATE = "mapViewSaveState";
    public static final String EXTRA_WAIT_OR_WALK_SELECTED_SUGGESTION = "waitOrWalkSelectedSuggestion";

    private RecyclerView directionsRecyclerView;
    private DirectionsAdapter directionsAdapter;
    private MapView mapView;
    private TextView noDirectionsFoundTextView;

    private GoogleMap map;
    private List<Directions.Step> directionSteps;
    private WaitOrWalkSuggestion waitOrWalkSelectedSuggestion;

    /**
     * The argument is non-null only when the directions action on a countdown
     * notification is clicked (or the notification itself is clicked).
     */

    public static WalkingDirectionsFragment newInstance(
            WaitOrWalkSuggestion waitOrWalkSelectedSuggestion) {

        WalkingDirectionsFragment walkingDirectionsFragment = new WalkingDirectionsFragment();

        if (waitOrWalkSelectedSuggestion != null) {
            Bundle bundle = new Bundle();
            bundle.putParcelable(EXTRA_WAIT_OR_WALK_SELECTED_SUGGESTION, waitOrWalkSelectedSuggestion);
            walkingDirectionsFragment.setArguments(bundle);
        }

        return walkingDirectionsFragment;
    }

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

        directionsRecyclerView = (RecyclerView) view.findViewById(R.id.directions_recyclerview);
        mapView = (MapView) view.findViewById(R.id.map_view);
        noDirectionsFoundTextView = (TextView) view.findViewById(R.id.nothing_found_textview);

        /**
         * Retrieve selected wait or walk suggestion from intent.
         * Note that this suggestion is only passed when the directions action on a countdown
         * notification is clicked (or the notification itself is clicked).
         */

        Bundle args = getArguments();

        if (args != null) {
            waitOrWalkSelectedSuggestion = args.getParcelable(EXTRA_WAIT_OR_WALK_SELECTED_SUGGESTION);
        }

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

        directionsAdapter = new DirectionsAdapter();
        directionsRecyclerView.setAdapter(directionsAdapter);
        directionsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        if (waitOrWalkSelectedSuggestion != null) {
            updateDirections(waitOrWalkSelectedSuggestion);
        }

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

    public void onEventMainThread(final OnWaitOrWalkSuggestionSelected event) {
        waitOrWalkSelectedSuggestion = event.getWaitOrWalkSuggestion();
        updateDirections(waitOrWalkSelectedSuggestion);
    }

    private void updateDirections(final WaitOrWalkSuggestion waitOrWalkSuggestion) {
        // clear all markers and polylines
        map.clear();

        // add stop marker with info window containing stop name
        LatLng stopLatLng = waitOrWalkSuggestion.getStop().getPosition();

        Marker marker= map.addMarker(new MarkerOptions()
                .position(stopLatLng)
                .icon(BitmapDescriptorFactory.fromBitmap(Helpers.getMarkerIcon(getActivity(),
                        R.drawable.stop_marker)))
                .title(waitOrWalkSuggestion.getStop().getName()));

        // move map focus to stop marker
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(stopLatLng, 16));

        // add walking route from user's last known location to stop
        Directions directions = waitOrWalkSuggestion.getWalkingDirections();

        if (directions != null) {
            PolylineOptions polylineOptions = new PolylineOptions();

            for (Directions.Point point : directions.getOverviewPoints()) {
                polylineOptions.add(new LatLng(point.getLatitude(), point.getLongitude()));
            }

            polylineOptions.color(ContextCompat.getColor(getActivity(), R.color.blue_500));
            polylineOptions.width(getResources().getDimensionPixelOffset(R.dimen.polyline_width));

            map.addPolyline(polylineOptions);

            marker.setSnippet(directions.getDistanceText());
            marker.showInfoWindow();

            // update directions list
            directionSteps = directions.getSteps();
            directionsAdapter.notifyDirectionsChanged();
        }
    }

    private class DirectionsAdapter extends RecyclerView.Adapter<DirectionsAdapter.DirectionSegmentViewHolder> {

        private LayoutInflater inflater;

        @Override
        public DirectionsAdapter.DirectionSegmentViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (inflater == null) {
                inflater = LayoutInflater.from(getActivity());
            }

            return new DirectionSegmentViewHolder(inflater.inflate(
                    R.layout.row_wait_or_walk_walking_directions, parent, false));
        }

        @Override
        public void onBindViewHolder(DirectionsAdapter.DirectionSegmentViewHolder holder, int position) {
            holder.bindItem(directionSteps.get(position));
        }

        @Override
        public int getItemCount() {
            if (directionSteps != null) {
                return directionSteps.size();
            } else {
                return 0;
            }
        }

        public void notifyDirectionsChanged() {
            if (directionSteps.size() > 0) {
                directionsRecyclerView.setVisibility(View.VISIBLE);
                noDirectionsFoundTextView.setVisibility(View.GONE);
            } else {
                directionsRecyclerView.setVisibility(View.GONE);
                noDirectionsFoundTextView.setVisibility(View.VISIBLE);
            }

            notifyDataSetChanged();
        }

        public class DirectionSegmentViewHolder extends RecyclerView.ViewHolder {

            private TextView instructionTextView;
            private TextView distanceTextView;

            public DirectionSegmentViewHolder(View itemView) {
                super(itemView);

                // find views
                instructionTextView = (TextView) itemView.findViewById(R.id.instruction_textview);
                distanceTextView = (TextView) itemView.findViewById(R.id.distance_textview);
            }

            public void bindItem(Directions.Step directionStep) {
                String instruction = directionStep.getInstruction()
                        .replaceAll("<div .*>(.*)</div>", "<br>$1");
                instructionTextView.setText(Helpers.trimTrailingWhitespace(Html.fromHtml(instruction)));
                distanceTextView.setText(directionStep.getDistanceText());
            }
        }
    }
}
