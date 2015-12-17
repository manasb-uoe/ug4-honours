package com.enthusiast94.edinfit.ui.home.fragments;

import android.content.Intent;
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
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models.Departure;
import com.enthusiast94.edinfit.models.Stop;
import com.enthusiast94.edinfit.network.BaseService;
import com.enthusiast94.edinfit.network.StopService;
import com.enthusiast94.edinfit.ui.stop_info.activities.StopActivity;
import com.enthusiast94.edinfit.utils.Helpers;
import com.enthusiast94.edinfit.utils.LocationProvider;
import com.enthusiast94.edinfit.utils.MoreStopOptionsDialog;
import com.enthusiast94.edinfit.utils.ReverseGeocoder;
import com.enthusiast94.edinfit.utils.Triplet;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by manas on 01-10-2015.
 */
public class NearMeFragment extends Fragment implements LocationProvider.LastKnowLocationCallback {

    public static final String TAG = NearMeFragment.class.getSimpleName();
    private static final String MAPVIEW_SAVE_STATE = "mapViewSaveState";

    private SlidingUpPanelLayout slidingUpPanelLayout;
    private RecyclerView nearbyStopsRecyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView currentLocationTextView;
    private TextView lastUpdatedAtTextView;
    private MapView mapView;

    private NearbyStopsAdapter nearbyStopsAdapter;
    private GoogleMap map;
    private Date lastUpdatedAt;
    private List<Stop> stops = new ArrayList<>();
    private List<Marker> stopMarkers = new ArrayList<>();
    private LocationProvider locationProvider;
    private ReverseGeocoder reverseGeocoder;
    private LatLng userLocationLatLng;
    private String userLocationName;

    // nearby stops api endpoint params
    private static final int NEARBY_STOPS_LIMIT = 20;
    private static final int MAX_DISTANCE = 3;
    private static final double NEAR_DISTANCE = 0.3;
    private static final int DEPARTURES_LIMIT = 5;

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

        slidingUpPanelLayout = (SlidingUpPanelLayout) view.findViewById(R.id.sliding_panel_layout);
        nearbyStopsRecyclerView = (RecyclerView) view.findViewById(R.id.nearby_stops_recyclerview);
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_layout);
        currentLocationTextView = (TextView) view.findViewById(R.id.current_location_textview);
        lastUpdatedAtTextView = (TextView) view.findViewById(R.id.last_updated_textview);
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
         * Setup location provider and reverse geocoder
         */

        locationProvider = new LocationProvider(getActivity(), this);
        reverseGeocoder = new ReverseGeocoder(getActivity());

        /**
         * Setup swipe refresh layout
         */

        swipeRefreshLayout.setColorSchemeResources(R.color.accent);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {
                locationProvider.requestLastKnownLocation();
            }
        });

        /**
         * Setup nearby stops recycler view
         */

        nearbyStopsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        nearbyStopsAdapter = new NearbyStopsAdapter();
        nearbyStopsRecyclerView.setAdapter(nearbyStopsAdapter);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        locationProvider.connect();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();

        locationProvider.disconnect();
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

    private void loadStops(final LatLng userLocationLatLng, final String userLocationName) {
        if (stops.size() == 0) {
            setRefreshIndicatorVisiblity(true);
        }

        StopService.getInstance().getNearbyStops(userLocationLatLng.latitude,
                userLocationLatLng.longitude, MAX_DISTANCE, NEAR_DISTANCE,
                Helpers.getCurrentTime24h(), NEARBY_STOPS_LIMIT, DEPARTURES_LIMIT,
                new BaseService.Callback<List<Stop>>() {

                    @Override
                    public void onSuccess(List<Stop> data) {
                        stops = data;
                        lastUpdatedAt = new Date();

                        if (getActivity() != null) {
                            setRefreshIndicatorVisiblity(false);

                            nearbyStopsAdapter.notifyStopsChanged();

                            updateSlidingMapPanel(userLocationLatLng, userLocationName);
                        }
                    }

                    @Override
                    public void onFailure(String message) {
                        if (getActivity() != null) {
                            setRefreshIndicatorVisiblity(false);

                            Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void updateSlidingMapPanel(LatLng userLocationLatLng, String userLocationName) {
        // remove all existing markers
        stopMarkers = new ArrayList<>();
        map.clear();

        // update current location and last update timestamp

        currentLocationTextView.setText(userLocationName);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.UK);
        lastUpdatedAtTextView.setText(sdf.format(lastUpdatedAt));

        // add nearby stop markers to map

        for (int i=0; i< stops.size(); i++) {
            Stop stop = stops.get(i);

            Bitmap stopMarkerIcon = Helpers.getStopMarkerIcon(getActivity());

            MarkerOptions markerOptions = new MarkerOptions()
                    .position(new LatLng(stop.getLocation().get(1), stop.getLocation().get(0)))
                    .icon(BitmapDescriptorFactory.fromBitmap(stopMarkerIcon))
                    .title(stop.getName());

            Marker stopMarker = map.addMarker(markerOptions);

            // only show info window for nearest stop
            if (i == 0) {
                stopMarker.setSnippet(getString(R.string.label_nearest));
                stopMarker.showInfoWindow();
            }

            stopMarkers.add(stopMarker);
        }

        // move map focus to user's last known location
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocationLatLng, 16));
    }

    private void setRefreshIndicatorVisiblity(final boolean visiblity) {
        swipeRefreshLayout.post(new Runnable() {

            @Override
            public void run() {
                swipeRefreshLayout.setRefreshing(visiblity);
            }
        });
    }

    @Override
    public void onLastKnownLocationSuccess(Location location) {
        userLocationLatLng = new LatLng(location.getLatitude(), location.getLongitude());

        if (getActivity() != null) {
            // simply reuse previously loaded nearby stops if user did not perform a manual refresh
            if (!swipeRefreshLayout.isRefreshing() && stops.size() != 0) {
                nearbyStopsAdapter.notifyStopsChanged();
                updateSlidingMapPanel(userLocationLatLng, userLocationName);

                return;
            }

            reverseGeocoder.getPlaceName(userLocationLatLng.latitude, userLocationLatLng.longitude,
                    new ReverseGeocoder.ReverseGeocodeCallback() {

                        @Override
                        public void onSuccess(String placeName) {
                            userLocationName = placeName;

                            if (getActivity() != null) {
                                loadStops(userLocationLatLng, userLocationName);
                            }
                        }

                        @Override
                        public void onFailure(String error) {
                            if (getActivity() != null) {
                                setRefreshIndicatorVisiblity(false);
                                Toast.makeText(getActivity(), error, Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        }
    }

    @Override
    public void onLastKnownLocationFailure(String error) {
        if (getActivity() != null) {
            setRefreshIndicatorVisiblity(false);
            Toast.makeText(getActivity(), error, Toast.LENGTH_LONG).show();
        }
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
                return stops.size() + 2 /* 2 headings */;
            }

            return 0;
        }

        @Override
        public int getItemViewType(int position) {
            Object item = getItem(position);

            if (item instanceof String) {
                return HEADING_VIEW_TYPE;
            } else {
                if (((Stop) item).getDistanceAway() < NEAR_DISTANCE) {
                    return NEAREST_STOP_VIEW_TYPE;
                } else {
                    return FARTHER_STOP_VIEW_TYPE;
                }
            }
        }

        private Object getItem(int position) {
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
        }

        private void notifyStopsChanged() {
            // recalculate nearest stops count so that headings appear in the correct positions
            nearestStopCount = 0;
            for (Stop stop : stops) {
                if (stop.getDistanceAway() < NEAR_DISTANCE) {
                    nearestStopCount++;
                } else {
                    break;
                }
            }

            this.notifyDataSetChanged();
        }

        private class NearestStopViewHolder extends RecyclerView.ViewHolder
                implements View.OnClickListener {

            private Stop stop;
            private TextView stopNameTextView;
            private TextView directionTextView;
            private ImageButton moreOptionsButton;
            private Triplet<TextView, TextView, TextView>[] departureViews;
            private View upcomingDeparturesContainer;
            private View noUpcomingDeparturesView;

            public NearestStopViewHolder(View itemView) {
                super(itemView);

                // find views
                stopNameTextView = (TextView) itemView.findViewById(R.id.stop_name_textview);
                directionTextView = (TextView) itemView.findViewById(R.id.direction_textview);
                moreOptionsButton = (ImageButton) itemView.findViewById(R.id.more_options_button);
                upcomingDeparturesContainer = itemView.findViewById(R.id.upcoming_departures_container);
                noUpcomingDeparturesView = itemView.findViewById(R.id.no_upcoming_departures_textview);
                departureViews = new Triplet[]{
                        new Triplet(
                                itemView.findViewById(R.id.service_name_1_textview),
                                itemView.findViewById(R.id.destination_1_textview),
                                itemView.findViewById(R.id.time_1_textview)),
                        new Triplet(
                                itemView.findViewById(R.id.service_name_2_textview),
                                itemView.findViewById(R.id.destination_2_textview),
                                itemView.findViewById(R.id.time_2_textview)),
                        new Triplet(
                                itemView.findViewById(R.id.service_name_3_textview),
                                itemView.findViewById(R.id.destination_3_textview),
                                itemView.findViewById(R.id.time_3_textview))
                };

                // bind event listeners
                itemView.setOnClickListener(this);
                moreOptionsButton.setOnClickListener(this);
            }

            public void bindItem(Stop stop) {
                this.stop = stop;

                stopNameTextView.setText(stop.getName());
                directionTextView.setText(stop.getDirection());

                List<Departure> departures = stop.getDepartures();

                if (departures.size() > 0) {
                    upcomingDeparturesContainer.setVisibility(View.VISIBLE);
                    noUpcomingDeparturesView.setVisibility(View.GONE);

                    for (int i=0; i < departureViews.length; i++) {
                        Triplet<TextView, TextView, TextView> departureView = departureViews[i];

                        if (i < departures.size()) {
                            Departure departure = departures.get(i);
                            departureView.a.setText(departure.getServiceName());
                            departureView.b.setText(departure.getDestination());
                            departureView.c.setText(departure.getTime());
                        } else {
                            ((ViewGroup) departureView.a.getParent()).setVisibility(View.GONE);
                        }
                    }
                } else {
                    upcomingDeparturesContainer.setVisibility(View.GONE);
                    noUpcomingDeparturesView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onClick(View v) {
                int id = v.getId();

                if (id == itemView.getId()) {
                    startStopActivity(stop);
                } else if (id == moreOptionsButton.getId()) {
                    MoreStopOptionsDialog moreStopOptionsDialog = new MoreStopOptionsDialog(
                            getActivity(), stop, new MoreStopOptionsDialog.ResponseListener() {

                        @Override
                        public void onShowStopOnMopOptionSelected() {
                            NearbyStopsAdapter.this.onShowStopOnMapOptionSelected(stop);
                        }

                        @Override
                        public void onStopSaved(String error) {
                            NearbyStopsAdapter.this.onStopSaved(error, stop);
                        }

                        @Override
                        public void onStopUnsaved(String error) {
                            NearbyStopsAdapter.this.onStopUnsaved(error, stop);
                        }
                    });

                    moreStopOptionsDialog.show();
                }
            }
        }

        private class FartherStopViewHolder extends RecyclerView.ViewHolder
                implements View.OnClickListener {

            private Stop stop;
            private TextView stopNameTextView;
            private TextView directionTextView;
            private ImageButton moreOptionsImageButton;

            public FartherStopViewHolder(View itemView) {
                super(itemView);

                // find views
                stopNameTextView = (TextView) itemView.findViewById(R.id.stop_name_textview);
                directionTextView = (TextView) itemView.findViewById(R.id.direction_textview);
                moreOptionsImageButton = (ImageButton) itemView.findViewById(R.id.more_options_button);

                // bind event listeners
                itemView.setOnClickListener(this);
                moreOptionsImageButton.setOnClickListener(this);
            }

            public void bindItem(Stop stop) {
                this.stop = stop;

                stopNameTextView.setText(stop.getName());
                directionTextView.setText(stop.getDirection());
            }

            @Override
            public void onClick(View v) {
                int id = v.getId();

                if (id == itemView.getId()) {
                    startStopActivity(stop);
                } else if (id == moreOptionsImageButton.getId()) {
                    MoreStopOptionsDialog moreStopOptionsDialog = new MoreStopOptionsDialog(
                            getActivity(), stop, new MoreStopOptionsDialog.ResponseListener() {

                        @Override
                        public void onShowStopOnMopOptionSelected() {
                            NearbyStopsAdapter.this.onShowStopOnMapOptionSelected(stop);
                        }

                        @Override
                        public void onStopSaved(String error) {
                            NearbyStopsAdapter.this.onStopSaved(error, stop);
                        }

                        @Override
                        public void onStopUnsaved(String error) {
                            NearbyStopsAdapter.this.onStopUnsaved(error, stop);
                        }
                    });

                    moreStopOptionsDialog.show();
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

        private void onShowStopOnMapOptionSelected(Stop stop) {
            // lookup marker corresponding to selected stop, then show its info
            // window and move map focus to it
            List<Double> stopLocation = stop.getLocation();
            for (Marker marker : stopMarkers) {
                LatLng latLng = marker.getPosition();

                if (latLng.latitude == stopLocation.get(1) && latLng.longitude == stopLocation.get(0)) {
                    marker.showInfoWindow();
                    map.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                    slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
                    break;
                }
            }
        }

        private void onStopSaved(String error, Stop stop) {
            if (getActivity() != null) {
                if (error == null) {
                    Toast.makeText(getActivity(), String.format(
                            getActivity().getString(R.string.success_stop_saved),
                            stop.getName()), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), error, Toast.LENGTH_SHORT).show();
                }
            }
        }

        private void onStopUnsaved(String error, Stop stop) {
            if (getActivity() != null) {
                if (error == null) {
                    Toast.makeText(getActivity(), String.format(
                            getActivity().getString(R.string.success_stop_unsaved),
                            stop.getName()), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), error, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
