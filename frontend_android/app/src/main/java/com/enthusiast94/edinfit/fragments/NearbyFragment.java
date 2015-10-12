package com.enthusiast94.edinfit.fragments;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.activities.StopActivity;
import com.enthusiast94.edinfit.models.Departure;
import com.enthusiast94.edinfit.models.Stop;
import com.enthusiast94.edinfit.services.BaseService;
import com.enthusiast94.edinfit.services.LocationService;
import com.enthusiast94.edinfit.services.StopService;
import com.enthusiast94.edinfit.utils.Helpers;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by manas on 01-10-2015.
 */
public class NearbyFragment extends Fragment {

    public static final String TAG = NearbyFragment.class.getSimpleName();
    private static final String MAPVIEW_SAVE_STATE = "mapViewSaveState";
    private RecyclerView nearbyStopsRecyclerView;
    private NearbyStopsAdapter nearbyStopsAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView currentLocationTextView;
    private TextView lastUpdatedAtTextView;
    private TabLayout tabLayout;
    private MapView mapView;
    private GoogleMap map;
    private String lastKnownUserLocationName;
    private LatLng lastKnownUserLocation;
    private Date lastUpdatedAt;
    private List<Stop> stops = new ArrayList<>();

    // indicates whether nearest or saved stops are being shown
    private boolean isShowingNearestStops = true;

    // nearby stops api endpoint params
    private static final int NEARBY_STOPS_LIMIT = 25;
    private static final int MAX_DISTANCE = 3;
    private static final double NEAR_DISTANCE = 0.3;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_nearby, container, false);

        /**
         * Find views
         */

        nearbyStopsRecyclerView = (RecyclerView) view.findViewById(R.id.nearby_stops_recyclerview);
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_layout);
        currentLocationTextView = (TextView) view.findViewById(R.id.current_location_textview);
        lastUpdatedAtTextView = (TextView) view.findViewById(R.id.last_updated_textview);
        tabLayout = (TabLayout) view.findViewById(R.id.tablayout);
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
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(Helpers.getEdinburghLatLng(getActivity()),
                12));

        /**
         * Setup tabs to switch between nearest and saved stops
         */

        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.label_nearest)));
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.label_saved)));
        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {

            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                isShowingNearestStops = tab.getPosition() == 0;
                loadStops();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // select tab based on retained value of isShowingNearestStops
        if (savedInstanceState != null) {
            TabLayout.Tab tab = isShowingNearestStops ? tabLayout.getTabAt(0) :
                    tabLayout.getTabAt(1);
            if (tab != null) {
                tab.select();
            }
        }

        /**
         * Setup swipe refresh layout
         */

        swipeRefreshLayout.setColorSchemeResources(R.color.accent);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {
                loadStops();
            }
        });

        /**
         * Setup nearby stops recycler view
         */

        nearbyStopsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        nearbyStopsAdapter = new NearbyStopsAdapter();
        nearbyStopsRecyclerView.setAdapter(nearbyStopsAdapter);

        /**
         * Load nearby stops from network
         */

        if (stops.size() == 0) {
            loadStops();
        } else {
            nearbyStopsAdapter.notifyStopsChanged();
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

        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();

        mapView.onLowMemory();
    }

    private void loadStops() {
        setRefreshIndicatorVisiblity(true);

        lastKnownUserLocation = LocationService.getInstance().getLastKnownUserLocation();
        lastKnownUserLocationName = LocationService.getInstance().getLastKnownUserLocationName();

        if (lastKnownUserLocation != null && lastKnownUserLocationName != null) {
            BaseService.Callback<List<Stop>> callback = new BaseService.Callback<List<Stop>>() {
                @Override
                public void onSuccess(List<Stop> data) {
                    stops = data;
                    lastUpdatedAt = new Date();

                    if (getActivity() != null) {
                        setRefreshIndicatorVisiblity(false);

                        nearbyStopsAdapter.notifyStopsChanged();

                        updateSlidingMapPanel();
                    }
                }

                @Override
                public void onFailure(String message) {
                    if (getActivity() != null) {
                        setRefreshIndicatorVisiblity(false);

                        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
                    }
                }
            };

            if (isShowingNearestStops) {
                StopService.getInstance().getNearbyStops(lastKnownUserLocation.latitude,
                        lastKnownUserLocation.longitude, MAX_DISTANCE, NEAR_DISTANCE,
                        Helpers.getCurrentTime24h(), NEARBY_STOPS_LIMIT, callback);
            } else {
                StopService.getInstance().getSavedStops(callback);
            }
        } else {
            Toast.makeText(getActivity(), getString(R.string.error_could_not_fetch_location),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void updateSlidingMapPanel() {
        // update current location and last update timestamp

        currentLocationTextView.setText(lastKnownUserLocationName);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.UK);
        lastUpdatedAtTextView.setText(sdf.format(lastUpdatedAt));

        // add nearby stop markers to map

        map.clear();

        for (int i=0; i< stops.size(); i++) {
            Stop stop = stops.get(i);

            Bitmap stopMarkerIcon = Helpers.getStopMarkerIcon(getActivity());

            MarkerOptions markerOptions = new MarkerOptions()
                    .position(new LatLng(stop.getLocation().get(1), stop.getLocation().get(0)))
                    .icon(BitmapDescriptorFactory.fromBitmap(stopMarkerIcon))
                    .title(stop.getName());

            if (isShowingNearestStops) {
                // only show info window for nearest stop
                if (i == 0) {
                    markerOptions.snippet(getString(R.string.label_nearest));
                    map.addMarker(markerOptions).showInfoWindow();
                }
            } else {
                map.addMarker(markerOptions);
            }
        }

        // move map focus to user's last known location
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(lastKnownUserLocation, 16));
    }

    private void setRefreshIndicatorVisiblity(final boolean visiblity) {
        swipeRefreshLayout.post(new Runnable() {

            @Override
            public void run() {
                swipeRefreshLayout.setRefreshing(visiblity);
            }
        });
    }

    private class NearbyStopsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private LayoutInflater inflater;
        private final int HEADING_VIEW_TYPE = 1;
        private final int NEAREST_STOP_VIEW_TYPE = 2;
        private final int FARTHER_STOP_VIEW_TYPE = 3;
        private int nearestStopCount;

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (inflater == null) {
                inflater = LayoutInflater.from(getActivity());
            }

            if (viewType == HEADING_VIEW_TYPE) {
                return new HeadingViewHolder(inflater.inflate(R.layout.row_heading, parent, false));
            } else if (viewType == NEAREST_STOP_VIEW_TYPE) {
                return new NearestStopViewHolder(
                        inflater.inflate(R.layout.row_nearest_stop, parent, false)
                );
            } else {
                return new FartherStopViewHolder(
                        inflater.inflate(R.layout.row_farther_stop, parent, false)
                );
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            int viewType = getItemViewType(position);

            if (viewType == HEADING_VIEW_TYPE) {
                ((HeadingViewHolder) holder).bindItem((String) getItem(position));
            } else if (viewType == NEAREST_STOP_VIEW_TYPE) {
                ((NearestStopViewHolder) holder).bindItem((Stop) getItem(position));
            } else {
                ((FartherStopViewHolder) holder).bindItem((Stop) getItem(position));
            }
        }

        @Override
        public int getItemCount() {
            if (stops.size() > 0) {
                if (isShowingNearestStops) {
                    return stops.size() + 2 /* 2 headings */;
                } else {
                    return stops.size();
                }
            }

            return 0;
        }

        @Override
        public int getItemViewType(int position) {
            Object item = getItem(position);

            if (item instanceof String) {
                return HEADING_VIEW_TYPE;
            } else {
                if (isShowingNearestStops) {
                    if (((Stop) item).getDistanceAway() < NEAR_DISTANCE) {
                        return NEAREST_STOP_VIEW_TYPE;
                    } else {
                        return FARTHER_STOP_VIEW_TYPE;
                    }
                } else {
                    return NEAREST_STOP_VIEW_TYPE;
                }
            }
        }

        private Object getItem(int position) {
            if (isShowingNearestStops) {
                if (position == 0) {
                    return getString(R.string.label_nearest_bus_stops);
                } else if (position == nearestStopCount + 1) {
                    return getString(R.string.label_farther_away);
                } else {
                    if (position < nearestStopCount) {
                        return stops.get(position - 1);
                    } else {
                        return stops.get(position-2);
                    }
                }
            } else {
                return stops.get(position);
            }
        }

        private void notifyStopsChanged() {
            if (isShowingNearestStops) {
                // recalculate nearest stops count so that headings appear in the correct positions
                nearestStopCount = 0;
                for (Stop stop : stops) {
                    if (stop.getDistanceAway() < NEAR_DISTANCE) {
                        nearestStopCount++;
                    } else {
                        break;
                    }
                }
            }

            this.notifyDataSetChanged();
        }

        private class NearestStopViewHolder extends RecyclerView.ViewHolder
                implements View.OnClickListener{

            private Stop stop;
            private TextView stopNameTextView;
            private TextView serviceNameTextView;
            private TextView destinationTextView;
            private TextView timeTextView;

            public NearestStopViewHolder(View itemView) {
                super(itemView);

                // find views
                stopNameTextView = (TextView) itemView.findViewById(R.id.stop_name_textview);
                serviceNameTextView =
                        (TextView) itemView.findViewById(R.id.service_name_textview);
                destinationTextView =
                        (TextView) itemView.findViewById(R.id.destination_textview);
                timeTextView =
                        (TextView) itemView.findViewById(R.id.time_textview);

                // bind event listeners
                itemView.setOnClickListener(this);
            }

            public void bindItem(Stop stop) {
                this.stop = stop;

                stopNameTextView.setText(stop.getName());

                if (stop.getDepartures().size() > 0) {
                    Departure departure = stop.getDepartures().get(0);

                    serviceNameTextView.setText(departure.getServiceName());
                    destinationTextView.setText(departure.getDestination());
                    timeTextView.setText(departure.getTime());
                } else {
                    serviceNameTextView.setText(getString(R.string.label_no_upcoming_departure));
                    destinationTextView.setText("");
                    timeTextView.setText("");
                }
            }

            @Override
            public void onClick(View v) {
                if (stop != null) {
                    startStopActivity(stop);
                }
            }
        }

        private class FartherStopViewHolder extends RecyclerView.ViewHolder
                implements View.OnClickListener {

            private TextView stopNameTextView;
            private Stop stop;

            public FartherStopViewHolder(View itemView) {
                super(itemView);

                // find views
                stopNameTextView = (TextView) itemView.findViewById(R.id.stop_name_textview);

                // bind event listeners
                itemView.setOnClickListener(this);
            }

            public void bindItem(Stop stop) {
                this.stop = stop;

                stopNameTextView.setText(stop.getName());
            }

            @Override
            public void onClick(View v) {
                if (stop != null) {
                    startStopActivity(stop);
                }
            }
        }

        private class HeadingViewHolder extends RecyclerView.ViewHolder {

            private TextView headingTextView;

            public HeadingViewHolder(View itemView) {
                super(itemView);

                headingTextView = (TextView) itemView.findViewById(R.id.heading_textview);
            }

            public void bindItem(String heading) {
                headingTextView.setText(heading);
            }
        }

        private void startStopActivity(Stop stop) {
            Intent startActivityIntent = new Intent(getActivity(), StopActivity.class);
            startActivityIntent.putExtra(StopActivity.EXTRA_STOP_ID, stop.getId());
            startActivityIntent.putExtra(StopActivity.EXTRA_STOP_NAME, stop.getName());
            startActivity(startActivityIntent);
        }
    }
}
