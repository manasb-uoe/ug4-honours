package com.enthusiast94.edinfit.ui.home.fragments;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models.Stop;
import com.enthusiast94.edinfit.network.BaseService;
import com.enthusiast94.edinfit.network.StopService;
import com.enthusiast94.edinfit.network.UserService;
import com.enthusiast94.edinfit.ui.stop_info.activities.StopActivity;
import com.enthusiast94.edinfit.utils.Helpers;
import com.enthusiast94.edinfit.utils.LocationProvider;
import com.enthusiast94.edinfit.utils.MoreStopOptionsDialog;
import com.enthusiast94.edinfit.utils.ReverseGeocoder;
import com.enthusiast94.edinfit.utils.SimpleDividerItemDecoration;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.ArrayList;
import java.util.List;

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
    private MapView mapView;
    private ProgressBar mapProgressBar;
    private ImageView mapImageView;

    private NearbyStopsAdapter nearbyStopsAdapter;
    private GoogleMap map;
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
        mapView = (MapView) view.findViewById(R.id.map_view);
        mapProgressBar = (ProgressBar) view.findViewById(R.id.map_progress_bar);
        mapImageView = (ImageView) view.findViewById(R.id.map_imageview);

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
        map.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {

            @Override
            public void onCameraChange(final CameraPosition cameraPosition) {
                LatLng target = cameraPosition.target;
                reverseGeocoder.getPlaceName(target.latitude, target.longitude,
                        new ReverseGeocoder.ReverseGeocodeCallback() {

                            @Override
                            public void onSuccess(String placeName) {
                                userLocationLatLng = new LatLng(cameraPosition.target.latitude,
                                        cameraPosition.target.longitude);
                                userLocationName = placeName;

                                loadStops(userLocationLatLng, userLocationName);
                            }

                            @Override
                            public void onFailure(String error) {
                                currentLocationTextView.setText(
                                        getString(R.string.label_unknown));
                            }
                        });
            }
        });

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
                loadStops(userLocationLatLng, userLocationName);
            }
        });

        /**
         * Setup nearby stops recycler view
         */

        nearbyStopsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        nearbyStopsRecyclerView.addItemDecoration(new SimpleDividerItemDecoration(getActivity()));
        nearbyStopsAdapter = new NearbyStopsAdapter(getActivity()) {
            @Override
            public void onShowStopOnMapOptionSelected(Stop stop) {
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
        };
        nearbyStopsRecyclerView.setAdapter(nearbyStopsAdapter);

        locationProvider.connect();

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

    private void loadStops(final LatLng userLocationLatLng, final String userLocationName) {
        setRefreshIndicatorVisiblity(true);
        setMapProgressBarEnabled(true);

        StopService.getInstance().getNearbyStops(userLocationLatLng.latitude,
                userLocationLatLng.longitude, MAX_DISTANCE, NEAR_DISTANCE, NEARBY_STOPS_LIMIT,
                new BaseService.Callback<List<Stop>>() {

                    @Override
                    public void onSuccess(List<Stop> data) {
                        stops = data;

                        if (getActivity() != null) {
                            setRefreshIndicatorVisiblity(false);
                            setMapProgressBarEnabled(false);

                            nearbyStopsAdapter.notifyStopsChanged(stops);

                            updateSlidingMapPanel(userLocationName);
                        }
                    }

                    @Override
                    public void onFailure(String message) {
                        if (getActivity() != null) {
                            setRefreshIndicatorVisiblity(false);
                            setMapProgressBarEnabled(false);

                            Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void updateSlidingMapPanel(String userLocationName) {
        // remove all existing markers
        stopMarkers = new ArrayList<>();
        map.clear();

        currentLocationTextView.setText(userLocationName);

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
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocationLatLng, 16));

            // simply reuse previously loaded nearby stops if user did not perform a manual refresh
            if (!swipeRefreshLayout.isRefreshing() && stops.size() != 0) {
                nearbyStopsAdapter.notifyStopsChanged(stops);
                updateSlidingMapPanel(userLocationName);
            } else {
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
                                    setMapProgressBarEnabled(false);
                                    Toast.makeText(getActivity(), error, Toast.LENGTH_LONG).show();
                                }
                            }
                        });
            }
        }
    }

    @Override
    public void onLastKnownLocationFailure(String error) {
        if (getActivity() != null) {
            setRefreshIndicatorVisiblity(false);
            setMapProgressBarEnabled(false);
            Toast.makeText(getActivity(), error, Toast.LENGTH_LONG).show();
        }
    }

    private void setMapProgressBarEnabled(boolean isEnabled) {
        if (isEnabled) {
            mapProgressBar.setVisibility(View.VISIBLE);
            mapImageView.setVisibility(View.INVISIBLE);
        } else {
            mapProgressBar.setVisibility(View.INVISIBLE);
            mapImageView.setVisibility(View.VISIBLE);
        }
    }

    private static abstract class NearbyStopsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private Context context;
        private LayoutInflater inflater;
        private List<Stop> nearbyStops;
        private List<String> savedStopIds;

        public NearbyStopsAdapter(Context context) {
            this.context = context;
            inflater = LayoutInflater.from(context);
            nearbyStops = new ArrayList<>();
            savedStopIds = UserService.getInstance().getAuthenticatedUser().getSavedStops();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new StopViewHolder(inflater.inflate(R.layout.row_nearby_stop, parent, false));
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            ((StopViewHolder) holder).bindItem(nearbyStops.get(position));
        }

        @Override
        public int getItemCount() {
            return nearbyStops.size();
        }

        private void notifyStopsChanged(List<Stop> nearbyStops) {
            this.nearbyStops = nearbyStops;
            this.notifyDataSetChanged();
        }

        private class StopViewHolder extends RecyclerView.ViewHolder
                implements View.OnClickListener {

            private Stop stop;
            private TextView stopNameTextView;
            private TextView directionTextView;
            private ImageButton moreOptionsButton;
            private TextView servicesTextView;
            private TextView destinationsTextView;
            private TextView idTextView;
            private TextView distanceAwayTextView;
            private ImageView starImageView;

            public StopViewHolder(View itemView) {
                super(itemView);

                // find views
                stopNameTextView = (TextView) itemView.findViewById(R.id.stop_name_textview);
                directionTextView = (TextView) itemView.findViewById(R.id.direction_textview);
                moreOptionsButton = (ImageButton) itemView.findViewById(R.id.more_options_button);
                servicesTextView = (TextView) itemView.findViewById(R.id.services_textview);
                destinationsTextView = (TextView) itemView.findViewById(R.id.destinations_textview);
                idTextView = (TextView) itemView.findViewById(R.id.stop_id_textview);
                distanceAwayTextView = (TextView) itemView.findViewById(R.id.distance_away_textview);
                starImageView = (ImageView) itemView.findViewById(R.id.star_imageview);

                // bind event listeners
                itemView.setOnClickListener(this);
                moreOptionsButton.setOnClickListener(this);
            }

            public void bindItem(Stop stop) {
                this.stop = stop;

                stopNameTextView.setText(stop.getName());
                directionTextView.setText(stop.getDirection());

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

                idTextView.setText(stop.getId());
                distanceAwayTextView.setText(Helpers.humanizeDistance(stop.getDistanceAway()));

                if (savedStopIds.contains(stop.getId())) {
                    starImageView.setVisibility(View.VISIBLE);
                } else {
                    starImageView.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onClick(View v) {
                int id = v.getId();

                if (id == itemView.getId()) {
                    startStopActivity(stop);
                } else if (id == moreOptionsButton.getId()) {
                    MoreStopOptionsDialog moreStopOptionsDialog = new MoreStopOptionsDialog(
                            context, stop, new MoreStopOptionsDialog.ResponseListener() {

                        @Override
                        public void onShowStopOnMopOptionSelected() {
                            NearbyStopsAdapter.this.onShowStopOnMapOptionSelected(stop);
                        }

                        @Override
                        public void onStopSaved(String error) {
                            savedStopIds.add(stop.getId());

                            notifyItemChanged(getAdapterPosition());

                            if (context != null) {
                                if (error == null) {
                                    Toast.makeText(context, String.format(
                                            context.getString(R.string.success_stop_saved),
                                            stop.getName()), Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
                                }
                            }
                        }

                        @Override
                        public void onStopUnsaved(String error) {
                            savedStopIds.remove(stop.getId());

                            notifyItemChanged(getAdapterPosition());

                            if (context != null) {
                                if (error == null) {
                                    Toast.makeText(context, String.format(
                                            context.getString(R.string.success_stop_unsaved),
                                            stop.getName()), Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    });

                    moreStopOptionsDialog.show();
                }
            }
        }

        private void startStopActivity(Stop stop) {
            Intent startActivityIntent = new Intent(context, StopActivity.class);
            startActivityIntent.putExtra(StopActivity.EXTRA_STOP_ID, stop.getId());
            startActivityIntent.putExtra(StopActivity.EXTRA_STOP_NAME, stop.getName());
            context.startActivity(startActivityIntent);
        }

        public abstract void onShowStopOnMapOptionSelected(Stop stop);
    }
}
