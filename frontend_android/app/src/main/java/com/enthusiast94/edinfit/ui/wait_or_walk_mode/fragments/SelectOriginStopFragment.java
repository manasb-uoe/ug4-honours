package com.enthusiast94.edinfit.ui.wait_or_walk_mode.fragments;

import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.arasthel.asyncjob.AsyncJob;
import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models_2.Stop;
import com.enthusiast94.edinfit.ui.stop_info.activities.StopActivity;
import com.enthusiast94.edinfit.utils.Helpers;
import com.enthusiast94.edinfit.utils.LocationProvider;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by manas on 18-10-2015.
 */
public class SelectOriginStopFragment extends Fragment implements LocationProvider.LastKnowLocationCallback {

    public static final String TAG = SelectOriginStopFragment.class.getSimpleName();
    private static final String MAPVIEW_SAVE_STATE = "mapViewSaveState";
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView stopsRecyclerView;
    private StopsAdapter stopsAdapter;
    private MapView mapView;
    private GoogleMap map;
    private TextView selectedStopTextView;
    private int currentlySelectedStopIndex = -1;
    private List<Stop> stops = new ArrayList<>();
    private LocationProvider locationProvider;
    private LatLng userLatLng;

    // nearby stops api endpoint params
    private static final int NEARBY_STOPS_LIMIT = 5;
    private static final int MAX_DISTANCE = 3;
    private static final double NEAR_DISTANCE = 0;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        locationProvider = new LocationProvider(getActivity(), this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_select_origin_stop, container, false);

        stopsRecyclerView = (RecyclerView) view.findViewById(R.id.stops_recyclerview);
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_layout);
        mapView = (MapView) view.findViewById(R.id.map_view);
        selectedStopTextView = (TextView) view.findViewById(R.id.selected_stop_textview);

        Bundle mapViewSavedInstanceState = savedInstanceState != null ?
                savedInstanceState.getBundle(MAPVIEW_SAVE_STATE) : null;
        mapView.onCreate(mapViewSavedInstanceState);

        map = mapView.getMap();
        map.getUiSettings().setMyLocationButtonEnabled(true);
        map.setMyLocationEnabled(true);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(Helpers.getEdinburghLatLng(getActivity()), 12));

        swipeRefreshLayout.setColorSchemeResources(R.color.accent);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {
                locationProvider.requestLastKnownLocation();
            }
        });

        stopsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        stopsAdapter = new StopsAdapter();
        stopsRecyclerView.setAdapter(stopsAdapter);

        if (!locationProvider.isConnected()) {
            locationProvider.connect();
        } else {
            stopsAdapter.notifyStopsChanged();
            updateSlidingMapPanel();
        }

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
        locationProvider.disconnect();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    public Stop getSelectedOriginStop() {
        if (currentlySelectedStopIndex == -1) {
            return null;
        } else {
            return stops.get(currentlySelectedStopIndex);
        }
    }

    private void setRefreshIndicatorVisiblity(final boolean visiblity) {
        swipeRefreshLayout.post(new Runnable() {

            @Override
            public void run() {
                swipeRefreshLayout.setRefreshing(visiblity);
            }
        });
    }

    private void loadStops(final LatLng userLocationLatLng) {
        setRefreshIndicatorVisiblity(true);

        new AsyncJob.AsyncJobBuilder<List<Stop>>()
                .doInBackground(new AsyncJob.AsyncAction<List<Stop>>() {
                    @Override
                    public List<Stop> doAsync() {
                        return Stop.getNearby(userLocationLatLng, MAX_DISTANCE, NEARBY_STOPS_LIMIT);
                    }
                })
                .doWhenFinished(new AsyncJob.AsyncResultAction<List<Stop>>() {
                    @Override
                    public void onResult(List<Stop> stops) {
                        if (getActivity() == null) {
                            return;
                        }

                        SelectOriginStopFragment.this.stops = stops;

                        setRefreshIndicatorVisiblity(false);

                        currentlySelectedStopIndex = 0;

                        stopsAdapter.notifyStopsChanged();
                        updateSlidingMapPanel();
                    }
                }).create().start();
    }

    private void updateSlidingMapPanel() {
        // remove all existing markers
        map.clear();

        if (stops.size() > 0) {
            // update selected stop textview
            Stop selectedStop = stops.get(currentlySelectedStopIndex);
            selectedStopTextView.setText(String.format(getString(
                    R.string.label_stop_name_with_direction), selectedStop.getName(), selectedStop.getDirection()));

            // add stop markers to map
            for (int i=0; i< stops.size(); i++) {
                Stop stop = stops.get(i);

                Bitmap stopMarkerIcon = Helpers.getMarkerIcon(getActivity(), R.drawable.stop_marker);

                LatLng stopLatLng = stop.getPosition();

                MarkerOptions markerOptions = new MarkerOptions()
                        .position(stopLatLng)
                        .icon(BitmapDescriptorFactory.fromBitmap(stopMarkerIcon))
                        .title(stop.getName());

                Marker stopMarker = map.addMarker(markerOptions);

                // move map focus to selected stop and show its info window
                if (i == currentlySelectedStopIndex) {
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(stopLatLng, 16));
                    stopMarker.showInfoWindow();
                }
            }
        }
    }

    @Override
    public void onLastKnownLocationSuccess(Location location) {
        if (getActivity() != null) {
            userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
            loadStops(userLatLng);
        }
    }

    @Override
    public void onLastKnownLocationFailure(String error) {
        if (getActivity() != null) {
            setRefreshIndicatorVisiblity(false);
            Toast.makeText(getActivity(), error, Toast.LENGTH_LONG).show();
        }
    }

    private class StopsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private LayoutInflater inflater;
        private int previouslySelectedStopIndex;
        private static final int HEADING_VIEW_TYPE = 0;
        private static final int STOP_VIEW_TYPE = 1;

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (inflater == null) {
                inflater = LayoutInflater.from(getActivity());
            }

            if (viewType == HEADING_VIEW_TYPE) {
                return new HeadingViewHolder(inflater.inflate(R.layout.row_heading, parent, false));
            } else {
                return new SelectionStopViewHolder(inflater.inflate(R.layout.row_selection_stop,
                        parent, false));
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            Object item = getItem(position);

            if (item instanceof Stop) {
                ((SelectionStopViewHolder) holder).bindItem((Stop) getItem(position));
            }
        }

        @Override
        public int getItemCount() {
            if (stops.size() == 0) {
                return 0;
            } else {
                return stops.size() + 1 /* 1 heading */;
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0) {
                return HEADING_VIEW_TYPE;
            } else {
                return STOP_VIEW_TYPE;
            }
        }

        private Object getItem(int position) {
            int viewType = getItemViewType(position);

            if (viewType == HEADING_VIEW_TYPE) {
                return null;
            } else {
                return stops.get(position - 1);
            }
        }

        public void notifyStopsChanged() {
            previouslySelectedStopIndex = currentlySelectedStopIndex;

            notifyDataSetChanged();
        }

        private class SelectionStopViewHolder extends RecyclerView.ViewHolder
                implements View.OnClickListener, View.OnLongClickListener {

            private TextView stopNameTextView;
            private TextView distanceAwayTextView;
            private Stop stop;

            public SelectionStopViewHolder(View itemView) {
                super(itemView);

                // find views
                stopNameTextView = (TextView) itemView.findViewById(R.id.stop_name_textview);
                distanceAwayTextView = (TextView) itemView.findViewById(R.id.distance_away_textview);

                // bind event listeners
                itemView.setOnClickListener(this);
                itemView.setOnLongClickListener(this);
            }

            public void bindItem(Stop stop) {
                this.stop = stop;

                stopNameTextView.setText(String.format(getString(
                        R.string.label_stop_name_with_direction), stop.getName(), stop.getDirection()));

                LatLng stopLatLng = stop.getPosition();
                distanceAwayTextView.setText(Helpers.humanizeDistance(
                        Helpers.getDistanceBetweenPoints(userLatLng.latitude, userLatLng.longitude,
                                stopLatLng.latitude, stopLatLng.longitude)));

                if (getAdapterPosition() - 1 == currentlySelectedStopIndex) {
                    itemView.setBackgroundResource(R.color.green_selection);
                } else {
                    itemView.setBackgroundResource(android.R.color.transparent);
                }
            }

            @Override
            public void onClick(View v) {
                currentlySelectedStopIndex = getAdapterPosition() - 1;

                notifyItemChanged(currentlySelectedStopIndex + 1);
                notifyItemChanged(previouslySelectedStopIndex + 1);

                updateSlidingMapPanel();

                previouslySelectedStopIndex = currentlySelectedStopIndex;
            }

            @Override
            public boolean onLongClick(View v) {
                v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                startActivity(StopActivity.getStartActivityIntent(getActivity(), stop.get_id()));

                return true;
            }
        }

        private class HeadingViewHolder extends RecyclerView.ViewHolder {

            private TextView headingTextView;

            public HeadingViewHolder(View itemView) {
                super(itemView);

                // find views
                headingTextView = (TextView) itemView.findViewById(R.id.heading_textview);

                // set heading
                headingTextView.setText(getString(R.string.label_where_are_you_waiting));
            }
        }
    }
}
