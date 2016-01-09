package com.enthusiast94.edinfit.ui.home.fragments;

import android.content.DialogInterface;
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
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.arasthel.asyncjob.AsyncJob;
import com.cocosw.bottomsheet.BottomSheet;
import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models_2.Stop;
import com.enthusiast94.edinfit.network.BaseService;
import com.enthusiast94.edinfit.network.StopService;
import com.enthusiast94.edinfit.ui.stop_info.activities.StopActivity;
import com.enthusiast94.edinfit.utils.Helpers;
import com.enthusiast94.edinfit.utils.LocationProvider;
import com.enthusiast94.edinfit.utils.ReverseGeocoder;
import com.enthusiast94.edinfit.utils.StopView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
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
    private TextView selectedLocationTextView;
    private TextView lastUpdatedAtTextView;
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
    private Marker userLocationMarker;

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
        selectedLocationTextView = (TextView) view.findViewById(R.id.selected_location_textview);
        lastUpdatedAtTextView = (TextView) view.findViewById(R.id.last_updated_at_textview);
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
        map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {

            @Override
            public void onMapClick(final LatLng latLng) {
                userLocationMarker.setPosition(latLng);

                reverseGeocoder.getPlaceName(latLng.latitude, latLng.longitude,
                        new ReverseGeocoder.ReverseGeocodeCallback() {

                            @Override
                            public void onSuccess(String placeName) {
                                userLocationLatLng = latLng;
                                userLocationName = placeName;

                                loadStops(userLocationLatLng, userLocationName);
                            }

                            @Override
                            public void onFailure(String error) {
                                selectedLocationTextView.setText(
                                        getString(R.string.label_unknown));
                            }
                        });
            }
        });

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
        nearbyStopsAdapter = new NearbyStopsAdapter(getActivity()) {
            @Override
            public void onShowStopOnMapOptionSelected(Stop stop) {
                // lookup marker corresponding to selected stop, then show its info
                // window and move map focus to it
                for (Marker marker : stopMarkers) {
                    LatLng latLng = marker.getPosition();
                    LatLng stopLatLng = stop.getPosition();

                    if (latLng.latitude == stopLatLng.latitude && latLng.longitude == stopLatLng.longitude) {
                        marker.showInfoWindow();
                        map.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                        slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                        break;
                    }
                }
            }
        };
        nearbyStopsRecyclerView.setAdapter(nearbyStopsAdapter);

        if (locationProvider == null) {
            locationProvider = new LocationProvider(getActivity(), this);
        }

        if (reverseGeocoder == null) {
            reverseGeocoder = new ReverseGeocoder(getActivity());
        }

        if (!locationProvider.isConnected()) {
            locationProvider.connect();
        } else {
            // reuse previously loaded nearby stops
            nearbyStopsAdapter.notifyStopsChanged(stops, userLocationLatLng);
            userLocationMarker = null; // set to null so that it is redrawn after config change
            updateSlidingMapPanel(userLocationName);
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

    private void loadStops(final LatLng userLocationLatLng, final String userLocationName) {
        setRefreshIndicatorVisiblity(true);
        setMapProgressBarEnabled(true);

        new AsyncJob.AsyncJobBuilder<List<Stop>>()
                .doInBackground(new AsyncJob.AsyncAction<List<Stop>>() {

                    @Override
                    public List<Stop> doAsync() {
                        return Stop.getNearby(userLocationLatLng, 3 /* km */, 20);
                    }
                })
                .doWhenFinished(new AsyncJob.AsyncResultAction<List<Stop>>() {

                    @Override
                    public void onResult(List<Stop> stops) {
                        NearMeFragment.this.stops = stops;

                        if (getActivity() != null) {
                            setRefreshIndicatorVisiblity(false);
                            setMapProgressBarEnabled(false);

                            nearbyStopsAdapter.notifyStopsChanged(stops, userLocationLatLng);

                            updateSlidingMapPanel(userLocationName);
                        }
                    }
                }).create().start();
    }

    private void updateSlidingMapPanel(String userLocationName) {
        // remove all existing stop markers
        for (Marker marker : stopMarkers) {
            marker.remove();
        }
        stopMarkers = new ArrayList<>();

        selectedLocationTextView.setText(userLocationName);

        lastUpdatedAtTextView.setText(String.format(getString(
                R.string.lalbe_last_updated_at_format), Helpers.getCurrentTime24h()));

        // add nearby stop markers to map

        for (int i=0; i< stops.size(); i++) {
            Stop stop = stops.get(i);

            Bitmap stopMarkerIcon = Helpers.getMarkerIcon(getActivity(), R.drawable.stop_marker);

            MarkerOptions markerOptions = new MarkerOptions()
                    .position(stop.getPosition())
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

        // update user location marker
        if (userLocationMarker == null || !userLocationMarker.isVisible()) {
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(userLocationLatLng);
            userLocationMarker = map.addMarker(markerOptions);
        }

        userLocationMarker.setTitle(userLocationName);
        userLocationMarker.setSnippet(
                getString(R.string.label_selected_location));

        if (map.getCameraPosition().zoom < 13) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocationLatLng, 15));
        } else {
            map.animateCamera(CameraUpdateFactory.newLatLng(userLocationLatLng));
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
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocationLatLng, 15));

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

        private android.app.Activity context;
        private LayoutInflater inflater;
        private List<Stop> nearbyStops;
        private List<String> savedStopIds;
        private LatLng userLocationLatLng;

        public NearbyStopsAdapter(android.app.Activity context /* BottomSheet only seems to work with Activity and not just Context */) {
            this.context = context;
            inflater = LayoutInflater.from(context);
            nearbyStops = new ArrayList<>();
//            savedStopIds = UserService.getInstance().getAuthenticatedUser().getSavedStops();
            savedStopIds = new ArrayList<>();
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

        private void notifyStopsChanged(List<Stop> nearbyStops, LatLng userLocationLatLng) {
            this.nearbyStops = nearbyStops;
            this.userLocationLatLng = userLocationLatLng;
            this.notifyDataSetChanged();
        }

        private class StopViewHolder extends RecyclerView.ViewHolder
                implements View.OnClickListener {

            private Stop stop;
            private StopView stopView;

            public StopViewHolder(View itemView) {
                super(itemView);

                stopView = (StopView) itemView;

                stopView.setOnClickListener(this);
            }

            public void bindItem(Stop stop) {
                this.stop = stop;

                stopView.bindItem(stop, savedStopIds.contains(stop.get_id()), userLocationLatLng);
            }

            @Override
            public void onClick(View v) {
                int id = v.getId();

                if (id == stopView.getId()) {
                    if (!savedStopIds.contains(stop.get_id())) {
                        new BottomSheet.Builder(context)
                                .title(String.format(context.getString(R.string.label_stop_name_with_direction),
                                        stop.getName(), stop.getDirection()))
                                .sheet(R.menu.menu_nearby_stop_1)
                                .listener(new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        switch (which) {
                                            case R.id.action_show_on_map:
                                                onShowStopOnMapOptionSelected(stop);
                                                break;
                                            case R.id.action_view_departures:
                                                startStopActivity(stop);
                                                break;
                                            case R.id.action_add_to_favourites:
                                                StopService.getInstance().saveOrUnsaveStop(stop.get_id(),
                                                        true, new BaseService.Callback<Void>() {

                                                            @Override
                                                            public void onSuccess(Void data) {
                                                                savedStopIds.add(stop.get_id());

                                                                notifyItemChanged(getAdapterPosition());

                                                                if (context != null) {
                                                                    Toast.makeText(context, String.format(
                                                                            context.getString(R.string.success_stop_saved),
                                                                            stop.getName()), Toast.LENGTH_SHORT).show();
                                                                }
                                                            }

                                                            @Override
                                                            public void onFailure(String message) {
                                                                if (context != null) {
                                                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                                                                }
                                                            }
                                                        });
                                                break;
                                        }
                                    }
                                }).show();
                    } else {
                        new BottomSheet.Builder(context)
                                .title(String.format(context.getString(R.string.label_stop_name_with_direction),
                                        stop.getName(), stop.getDirection()))
                                .sheet(R.menu.menu_nearby_stop_2)
                                .listener(new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        switch (which) {
                                            case R.id.action_show_on_map:
                                                onShowStopOnMapOptionSelected(stop);
                                                break;
                                            case R.id.action_view_departures:
                                                startStopActivity(stop);
                                                break;
                                            case R.id.action_remove_from_favourites:
                                                StopService.getInstance().saveOrUnsaveStop(stop.get_id(), false, new BaseService.Callback<Void>() {

                                                    @Override
                                                    public void onSuccess(Void data) {
                                                        savedStopIds.remove(stop.get_id());

                                                        if (context != null) {
                                                            notifyItemChanged(getAdapterPosition());
                                                            Toast.makeText(context, String.format(
                                                                    context.getString(R.string.success_stop_unsaved),
                                                                    stop.getName()), Toast.LENGTH_SHORT).show();
                                                        }
                                                    }

                                                    @Override
                                                    public void onFailure(String message) {
                                                        if (context != null) {
                                                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                                                        }
                                                    }
                                                });
                                                break;
                                        }
                                    }
                                }).show();
                    }
                }
            }
        }

        private void startStopActivity(Stop stop) {
                Intent startActivityIntent = new Intent(context, StopActivity.class);
                startActivityIntent.putExtra(StopActivity.EXTRA_STOP_ID, stop.get_id());
                context.startActivity(startActivityIntent);
        }

        public abstract void onShowStopOnMapOptionSelected(Stop stop);
    }
}
