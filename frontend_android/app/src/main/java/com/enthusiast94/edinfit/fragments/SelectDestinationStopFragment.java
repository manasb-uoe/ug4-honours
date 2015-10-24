package com.enthusiast94.edinfit.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models.Point;
import com.enthusiast94.edinfit.models.Route;
import com.enthusiast94.edinfit.models.Service;
import com.enthusiast94.edinfit.models.Stop;
import com.enthusiast94.edinfit.services.BaseService;
import com.enthusiast94.edinfit.services.ServiceService;
import com.enthusiast94.edinfit.utils.Helpers;
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
import java.util.List;

/**
 * Created by manas on 22-10-2015.
 */
public class SelectDestinationStopFragment extends Fragment {

    public static final String TAG = SelectDestinationStopFragment.class.getSimpleName();
    public static final String EXTRA_SERVICE_NAME = "serviceName";
    public static final String EXTRA_ORIGIN_STOP_ID = "originStopId";
    private static final String MAPVIEW_SAVE_STATE = "mapViewSaveState";
    private RouteStopsAdapter routeStopsAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private SlidingUpPanelLayout slidingUpPanelLayout;
    private TextView routeTextView;
    private TextView changeRouteButton;
    private MapView mapView;
    private GoogleMap map;
    private String serviceName;
    private String selectedRouteDestination;
    private List<Marker> stopMarkers;
    private Polyline routePolyline;
    private int currentlySelectedStopIndex = -1;
    private Service service;

    public static SelectDestinationStopFragment newInstance(String originStopId, String serviceName) {
        SelectDestinationStopFragment instance = new SelectDestinationStopFragment();
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_ORIGIN_STOP_ID, originStopId);
        bundle.putString(EXTRA_SERVICE_NAME, serviceName);
        instance.setArguments(bundle);

        return instance;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_select_destination_stop, container, false);

        /**
         * Find views
         */

        Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        View actionNext = toolbar.findViewById(R.id.action_next);
        RecyclerView routeStopsRecyclerView = (RecyclerView) view.findViewById(R.id.route_stops_recyclerview);
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_layout);
        mapView = (MapView) view.findViewById(R.id.map_view);
        routeTextView = (TextView) view.findViewById(R.id.route_textview);
        slidingUpPanelLayout = (SlidingUpPanelLayout) view.findViewById(R.id.sliding_layout);
        changeRouteButton = (TextView) view.findViewById(R.id.change_route_button);

        /**
         * Setup toolbar
         */

        toolbar.setTitle(getString(R.string.action_select_final_stop));
        toolbar.setNavigationIcon(R.drawable.ic_action_navigation_arrow_back);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });

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
        routeStopsAdapter = new RouteStopsAdapter();
        routeStopsRecyclerView.setAdapter(routeStopsAdapter);

        /**
         * Setup 'change route' button
         */

        changeRouteButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
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
            }
        });

        /**
         * Load service from network
         */

        loadService();

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

    private class RouteStopsAdapter extends RecyclerView.Adapter {

        private LayoutInflater inflater;
        private List<Stop> stops = new ArrayList<>();
        private static final int HEADING_VIEW_TYPE = 0;
        private static final int STOP_VIEW_TYPE = 1;
        private int previouslySelectedStopIndex = -1;

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (inflater == null) {
                inflater = LayoutInflater.from(getActivity());
            }

            if (viewType == HEADING_VIEW_TYPE) {
                return new HeadingViewHolder(inflater.inflate(R.layout.row_heading, parent, false));
            } else {
                return new RouteStopViewHolder(inflater.inflate(R.layout.row_service_route_stop_2,
                        parent, false));
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            Object item = getItem(position);

            if (item instanceof Stop) {
                ((RouteStopViewHolder) holder).bindItem((Stop) item);
            }
        }

        @Override
        public int getItemCount() {
            if (stops.size() > 0) {
                return stops.size() + 1 /* 1 heading */;
            } else {
                return 0;
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

        public void notifyRouteChanged(Route route) {
            stops = new ArrayList<>(route.getStops());

            currentlySelectedStopIndex = 0;
            previouslySelectedStopIndex = 0;

            notifyDataSetChanged();
        }

        private class RouteStopViewHolder extends RecyclerView.ViewHolder
                implements View.OnClickListener {

            private TextView stopNameTextView;
            private View topIndicatorView;
            private View bottomIndicatorView;
            private ImageView downArrowImageView;
            private ImageButton showOnMapButton;
            private Stop stop;

            public RouteStopViewHolder(View itemView) {
                super(itemView);

                // find views
                stopNameTextView = (TextView) itemView.findViewById(R.id.stop_name_textview);
                topIndicatorView = itemView.findViewById(R.id.top_indicator_view);
                bottomIndicatorView = itemView.findViewById(R.id.bottom_indicator_view);
                downArrowImageView = (ImageView) itemView.findViewById(R.id.down_arrow_imageview);
                showOnMapButton = (ImageButton) itemView.findViewById(R.id.more_options_button);

                // bind event listeners
                itemView.setOnClickListener(this);
                showOnMapButton.setOnClickListener(this);
            }

            public void bindItem(Stop stop) {
                this.stop = stop;

                stopNameTextView.setText(stop.getName());

                // show indicators for first/last stop in the route
                int adapterPosition = getAdapterPosition() - 1;

                if (adapterPosition == 0) {
                    topIndicatorView.setVisibility(View.INVISIBLE);
                    bottomIndicatorView.setVisibility(View.VISIBLE);
                    downArrowImageView.setVisibility(View.VISIBLE);
                } else if (adapterPosition == stops.size() - 1) {
                    topIndicatorView.setVisibility(View.VISIBLE);
                    bottomIndicatorView.setVisibility(View.INVISIBLE);
                    downArrowImageView.setVisibility(View.INVISIBLE);
                } else {
                    topIndicatorView.setVisibility(View.VISIBLE);
                    bottomIndicatorView.setVisibility(View.VISIBLE);
                    downArrowImageView.setVisibility(View.INVISIBLE);
                }

                // highlight selected item
                if (adapterPosition == currentlySelectedStopIndex) {
                    itemView.setBackgroundResource(R.color.green_selection);
                } else {
                    itemView.setBackgroundResource(android.R.color.transparent);
                }
            }

            @Override
            public void onClick(View v) {
                int id = v.getId();
                if (id == itemView.getId()) {
                    currentlySelectedStopIndex = getAdapterPosition() - 1;

                    notifyItemChanged(currentlySelectedStopIndex + 1);
                    notifyItemChanged(previouslySelectedStopIndex + 1);

                    previouslySelectedStopIndex = currentlySelectedStopIndex;
                } else if (id == showOnMapButton.getId()) {
                    AlertDialog showOnMapDialog = new AlertDialog.Builder(getActivity())
                            .setItems(new String[]{getString(R.string.label_show_on_map)},
                                    new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    switch (which) {
                                        case 0:
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
                                            break;
                                    }
                                }
                            })
                            .create();
                    showOnMapDialog.show();
                }
            }
        }

        private class HeadingViewHolder extends RecyclerView.ViewHolder {

            private TextView headingTextView;

            public HeadingViewHolder(View itemView) {
                super(itemView);

                // find views
                headingTextView = (TextView) itemView.findViewById(R.id.heading_textview);

                // set heading
                headingTextView.setText(getString(R.string.label_where_will_you_get_off));
            }
        }
    }
}
