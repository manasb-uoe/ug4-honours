package com.enthusiast94.edinfit.ui.service_info.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.ui.stop_info.activities.StopActivity;
import com.enthusiast94.edinfit.models.LiveBus;
import com.enthusiast94.edinfit.models.Point;
import com.enthusiast94.edinfit.models.Route;
import com.enthusiast94.edinfit.models.Service;
import com.enthusiast94.edinfit.models.Stop;
import com.enthusiast94.edinfit.services.BaseService;
import com.enthusiast94.edinfit.services.LiveBusService;
import com.enthusiast94.edinfit.services.ServiceService;
import com.enthusiast94.edinfit.utils.Helpers;
import com.enthusiast94.edinfit.utils.MoreStopOptionsDialog;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by manas on 06-10-2015.
 */
public class ServiceFragment extends Fragment {

    public static final String TAG = ServiceFragment.class.getSimpleName();
    public static final String EXTRA_SERVICE_NAME = "serviceName";
    private static final String MAPVIEW_SAVE_STATE = "mapViewSaveState";
    private Handler handler;
    private RecyclerView routeStopsRecyclerView;
    private RouteStopsAdapter routeStopsAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private SlidingUpPanelLayout slidingUpPanelLayout;
    private TextView routeTextView;
    private MapView mapView;
    private GoogleMap map;
    private String serviceName;
    private Service service;
    private String selectedRouteDestination;
    private List<Marker> stopMarkers;
    private Polyline routePolyline;
    private List<LiveBus> liveBuses;
    private Map<String, Marker> liveBusMarkersMap;

    public static ServiceFragment newInstance(String serviceName) {
        ServiceFragment instance = new ServiceFragment();
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_SERVICE_NAME, serviceName);
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
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_service_route_stops, container, false);

        setHasOptionsMenu(true);

        /**
         * Bind handler to UI thread. It will be used to update live bus locations on map at
         * frequent intervals.
         */

        handler = new Handler();

        /**
         * Find views
         */

        routeStopsRecyclerView = (RecyclerView) view.findViewById(R.id.route_stops_recyclerview);
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_layout);
        mapView = (MapView) view.findViewById(R.id.map_view);
        routeTextView = (TextView) view.findViewById(R.id.route_textview);
        slidingUpPanelLayout = (SlidingUpPanelLayout) view.findViewById(R.id.sliding_layout);

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
         * Retrieve service name from arguments so that the data corresponding to its service
         * can be loaded
         */

        serviceName = getArguments().getString(EXTRA_SERVICE_NAME);

        /**
         * Setup swipe refresh layout
         */

        swipeRefreshLayout.setColorSchemeResources(R.color.accent);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {
                loadService();
            }
        });

        /**
         * Setup route stops recycler view
         */

        routeStopsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        routeStopsAdapter = new RouteStopsAdapter(getActivity()) {

            @Override
            public void onShowStopOnMopOptionSelected(Stop stop) {
                // lookup marker corresponding to provided, then show its info window and move
                // map focus to it
                List<Double> stopLocation = stop.getLocation();
                for (Marker marker : stopMarkers) {
                    LatLng latLng = marker.getPosition();

                    if (latLng.latitude == stopLocation.get(1) && latLng.longitude == stopLocation.get(0)) {
                        marker.showInfoWindow();
                        map.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                        slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                        break;
                    }
                }
            }
        };
        routeStopsRecyclerView.setAdapter(routeStopsAdapter);

        /**
         * Load service if it hasn't already been loaded
         */

        if (service == null) {
            loadService();
        } else {
            updateRoute(selectedRouteDestination);
            addLiveBusesToMap();
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

    private void loadService() {
        setRefreshIndicatorVisiblity(true);

        ServiceService.getInstance().getService(serviceName, new BaseService.Callback<Service>() {

            @Override
            public void onSuccess(Service data) {
                service = data;

                // set first route as the default route (if it exists)
                if (service.getRoutes().size() > 1) {
                    selectedRouteDestination = service.getRoutes().get(0).getDestination();
                }

                if (getActivity() != null) {
                    setRefreshIndicatorVisiblity(false);

                    updateRoute(selectedRouteDestination);

                    handler.post(new Runnable() {

                        @Override
                        public void run() {
                            loadLiveBuses();
                            handler.postDelayed(this, 20000);
                        }
                    });
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

    private void setRefreshIndicatorVisiblity(final boolean visiblity) {
        swipeRefreshLayout.post(new Runnable() {

            @Override
            public void run() {
                swipeRefreshLayout.setRefreshing(visiblity);
            }
        });
    }

    private void updateRoute(String destination) {
        List<Route> routes = service.getRoutes();

        for (Route route : routes) {
            if (route.getDestination().equals(destination)) {
                addRouteToMap(route);

                routeStopsAdapter.notifyRouteChanged(route);

                routeTextView.setText(String.format(getString(R.string.label_towards),
                        selectedRouteDestination));

                break;
            }
        }
    }

    private void addRouteToMap(Route route) {
        if (stopMarkers == null) {
            stopMarkers = new ArrayList<>();
        }

        // remove all existing stop markers and polylines

        if (stopMarkers.size() > 0) {
            for (Marker stopMarker : stopMarkers) {
                stopMarker.remove();
            }

            stopMarkers = new ArrayList<>();
        }

        if (routePolyline != null) {
            routePolyline.remove();
        }

        // add stop markers to map

        List<Stop> stops = route.getStops();

        Bitmap stopMarkerIcon = Helpers.getStopMarkerIcon(getActivity());

        for (int i=0; i<stops.size(); i++) {
            Stop stop = stops.get(i);

            LatLng latLng = new LatLng(stop.getLocation().get(1), stop.getLocation().get(0));

            Marker stopMarker = map.addMarker(new MarkerOptions()
                    .position(latLng)
                    .anchor(.5f, .5f)
                    .title(stop.getName())
                    .icon(BitmapDescriptorFactory.fromBitmap(stopMarkerIcon)));

            if (i == 0) {
                stopMarker.setSnippet(getString(R.string.label_start));
            } else if (i == stops.size()-1) {
                stopMarker.setSnippet(getString(R.string.label_end));
                stopMarker.showInfoWindow();
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13));
            }

            stopMarkers.add(stopMarker);
        }

        // add route polyline to map

        List<Point> points = route.getPoints();

        PolylineOptions polylineOptions = new PolylineOptions();

        int redColor = ContextCompat.getColor(getActivity(), R.color.red);
        int polylineWidth = getResources().getDimensionPixelOffset(R.dimen.polyline_width);

        for (Point point : points) {
            polylineOptions
                    .add(new LatLng(point.getLatitude(), point.getLongitude()))
                    .width(polylineWidth)
                    .color(redColor);
        }

        routePolyline = map.addPolyline(polylineOptions);
    }

    private void loadLiveBuses() {
        LiveBusService.getInstance().getLiveBuses(serviceName, new BaseService.Callback<List<LiveBus>>() {

            @Override
            public void onSuccess(List<LiveBus> data) {
                if (getActivity() != null) {
                    liveBuses = data;
                    addLiveBusesToMap();
                    Toast.makeText(getActivity(), "Updated.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(String message) {
                if (getActivity() != null) {
                    Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void addLiveBusesToMap() {
        if (liveBusMarkersMap == null) {
            liveBusMarkersMap = new HashMap<>();
        }

        for (LiveBus liveBus : liveBuses) {
            Marker liveBusMarkerOld = null;

            if (liveBusMarkersMap.containsKey(liveBus.getVehicleId())) {
                liveBusMarkerOld = liveBusMarkersMap.get(liveBus.getVehicleId());
            }

            MarkerOptions markerOptions = new MarkerOptions()
                    .title("#" + liveBus.getVehicleId() + " to " + liveBus.getDestination())
                    .position(new LatLng(liveBus.getLatitude(), liveBus.getLongitude()))
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.bus_marker));
            Marker liveBusMarkerNew = map.addMarker(markerOptions);

            if (liveBusMarkerOld != null) {
                if (liveBusMarkerOld.isInfoWindowShown()) {
                    liveBusMarkerNew.showInfoWindow();
                }

                liveBusMarkerOld.remove();
            }

            liveBusMarkersMap.remove(liveBus.getVehicleId());
            liveBusMarkersMap.put(liveBus.getVehicleId(), liveBusMarkerNew);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_service_fragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_select_route) {
            if (service != null) {
                final List<String> destinations = new ArrayList<>();

                for (Route route : service.getRoutes()) {
                    destinations.add(route.getDestination());
                }

                AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                        .setSingleChoiceItems(destinations.toArray(new String[destinations.size()]),
                                destinations.indexOf(selectedRouteDestination), null)
                        .setTitle(R.string.label_select_route)
                        .setPositiveButton(R.string.label_set, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ListView listView = ((AlertDialog) dialog).getListView();
                                selectedRouteDestination =
                                        destinations.get(listView.getCheckedItemPosition());
                                updateRoute(selectedRouteDestination);
                            }
                        })
                        .setNegativeButton(R.string.label_cancel, null)
                        .create();

                alertDialog.show();
            }

            return true;
        }

        return false;
    }

    private static abstract class RouteStopsAdapter extends RecyclerView.Adapter {

        private Context context;
        private LayoutInflater inflater;
        private List<Stop> stops = new ArrayList<>();

        public RouteStopsAdapter(Context context) {
            this.context = context;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (inflater == null) {
                inflater = LayoutInflater.from(context);
            }

            return new RouteStopViewHolder(inflater.inflate(R.layout.row_route_stops, parent, false));
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            ((RouteStopViewHolder) holder).bindItem(stops.get(position));
        }

        @Override
        public int getItemCount() {
            return stops.size();
        }

        public void notifyRouteChanged(Route route) {
            stops = new ArrayList<>(route.getStops());

            notifyDataSetChanged();
        }

        private class RouteStopViewHolder extends RecyclerView.ViewHolder
                implements View.OnClickListener {

            private TextView stopNameTextView;
            private View topIndicatorView;
            private View bottomIndicatorView;
            private ImageView downArrowImageView;
            private ImageButton moreOptionsButton;
            private Stop stop;

            public RouteStopViewHolder(View itemView) {
                super(itemView);

                // find views
                stopNameTextView = (TextView) itemView.findViewById(R.id.stop_name_textview);
                topIndicatorView = itemView.findViewById(R.id.top_indicator_view);
                bottomIndicatorView = itemView.findViewById(R.id.bottom_indicator_view);
                downArrowImageView = (ImageView) itemView.findViewById(R.id.down_arrow_imageview);
                moreOptionsButton = (ImageButton) itemView.findViewById(R.id.more_options_button);

                // bind event listeners
                itemView.setOnClickListener(this);
                moreOptionsButton.setOnClickListener(this);
            }

            public void bindItem(Stop stop) {
                this.stop = stop;

                stopNameTextView.setText(stop.getName());

                // show indicators for first/last stop in the route
                if (getAdapterPosition() == 0) {
                    topIndicatorView.setVisibility(View.INVISIBLE);
                    bottomIndicatorView.setVisibility(View.VISIBLE);
                    downArrowImageView.setVisibility(View.VISIBLE);
                } else if (getAdapterPosition() == stops.size() - 1) {
                    topIndicatorView.setVisibility(View.VISIBLE);
                    bottomIndicatorView.setVisibility(View.INVISIBLE);
                    downArrowImageView.setVisibility(View.INVISIBLE);
                } else {
                    topIndicatorView.setVisibility(View.VISIBLE);
                    bottomIndicatorView.setVisibility(View.VISIBLE);
                    downArrowImageView.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onClick(View v) {
                int id = v.getId();
                if (id == itemView.getId()) {
                    Intent startActivityIntent = new Intent(context, StopActivity.class);
                    startActivityIntent.putExtra(StopActivity.EXTRA_STOP_ID, stop.getId());
                    startActivityIntent.putExtra(StopActivity.EXTRA_STOP_NAME, stop.getName());
                    context.startActivity(startActivityIntent);
                } else if (id == moreOptionsButton.getId()) {
                    MoreStopOptionsDialog moreStopOptionsDialog = new MoreStopOptionsDialog(
                            context, stop, new MoreStopOptionsDialog.ResponseListener() {


                        @Override
                        public void onShowStopOnMopOptionSelected() {
                            RouteStopsAdapter.this.onShowStopOnMopOptionSelected(stop);
                        }

                        @Override
                        public void onStopSaved(String error) {
                            if (error == null) {
                                Toast.makeText(context, String.format(
                                        context.getString(R.string.success_stop_saved),
                                        stop.getName()), Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onStopUnsaved(String error) {
                            if (error == null) {
                                Toast.makeText(context, String.format(
                                        context.getString(R.string.success_stop_unsaved),
                                        stop.getName()), Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                    moreStopOptionsDialog.show();
                }
            }
        }

        public abstract void onShowStopOnMopOptionSelected(Stop stop);
    }
}
