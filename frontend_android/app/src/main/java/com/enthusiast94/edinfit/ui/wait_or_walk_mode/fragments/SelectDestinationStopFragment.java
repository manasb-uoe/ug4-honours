package com.enthusiast94.edinfit.ui.wait_or_walk_mode.fragments;

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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models.Service;
import com.enthusiast94.edinfit.models.Service.Route;
import com.enthusiast94.edinfit.models.Stop;
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
    private LinearLayoutManager linearLayoutManager;
    private TextView routeTextView;
    private TextView changeRouteButton;
    private MapView mapView;
    private GoogleMap map;

    private String serviceName;
    private String originStopId;
    private List<String> destinationsWithOriginStop; // destination names for all routes that contain origin stop
    private String selectedRouteDestination;
    private List<Marker> stopMarkers;
    private Polyline routePolyline;
    private int currentlySelectedStopIndex;
    private Service service;
    private List<Route> routes;
    private Route selectedRoute;

    public static SelectDestinationStopFragment newInstance(String originStopId, String serviceName) {
        SelectDestinationStopFragment instance = new SelectDestinationStopFragment();
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_ORIGIN_STOP_ID, originStopId);
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
        View view = inflater.inflate(R.layout.fragment_select_destination_stop, container, false);

        RecyclerView routeStopsRecyclerView = (RecyclerView) view.findViewById(R.id.route_stops_recyclerview);
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_layout);
        mapView = (MapView) view.findViewById(R.id.map_view);
        routeTextView = (TextView) view.findViewById(R.id.route_textview);
        slidingUpPanelLayout = (SlidingUpPanelLayout) view.findViewById(R.id.sliding_layout);
        changeRouteButton = (TextView) view.findViewById(R.id.change_route_button);

        Bundle mapViewSavedInstanceState = savedInstanceState != null ?
                savedInstanceState.getBundle(MAPVIEW_SAVE_STATE) : null;
        mapView.onCreate(mapViewSavedInstanceState);

        map = mapView.getMap();
        map.getUiSettings().setMyLocationButtonEnabled(true);
        map.setMyLocationEnabled(true);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(Helpers.getEdinburghLatLng(getActivity()), 12));

        // Retrieve service name from arguments so that the data corresponding to its service
        // can be loaded. Also retrieve origin stop id so that only those stops can be made
        // available for selection that come AFTER the origin stop.
        Bundle bundle = getArguments();
        serviceName = bundle.getString(EXTRA_SERVICE_NAME);
        originStopId = bundle.getString(EXTRA_ORIGIN_STOP_ID);

        swipeRefreshLayout.setColorSchemeResources(R.color.accent);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {
                loadService();
            }
        });

        linearLayoutManager = new LinearLayoutManager(getActivity());
        routeStopsRecyclerView.setLayoutManager(linearLayoutManager);
        routeStopsAdapter = new RouteStopsAdapter();
        routeStopsRecyclerView.setAdapter(routeStopsAdapter);

        View.OnClickListener onClickListener = new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                int id = v.getId();

                if (id == changeRouteButton.getId()) {
                    if (service != null) {
                        AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                                .setSingleChoiceItems(destinationsWithOriginStop.toArray(new String[destinationsWithOriginStop.size()]),
                                        destinationsWithOriginStop.indexOf(selectedRouteDestination), null)
                                .setTitle(R.string.label_select_route)
                                .setPositiveButton(R.string.label_set, new DialogInterface.OnClickListener() {

                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        ListView listView = ((AlertDialog) dialog).getListView();
                                        selectedRouteDestination =
                                                destinationsWithOriginStop.get(listView.getCheckedItemPosition());
                                        updateRoute(selectedRouteDestination);
                                    }
                                })
                                .setNegativeButton(R.string.label_cancel, null)
                                .create();

                        alertDialog.show();
                    }
                }
            }
        };

        changeRouteButton.setOnClickListener(onClickListener);

        if (service == null) {
            loadService();
        } else {
            updateRoute(selectedRouteDestination);
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

        service = Service.findByName(serviceName);
        routes = service.getRoutes();

        // filter out routes that do not contain origin stop
        // or if they contain origin stop as their last stop
        destinationsWithOriginStop = new ArrayList<>();
        for (Route route : routes) {
            for (int i = 0; i < route.getStops().size(); i++) {
                Stop stop = route.getStops().get(i);
                if (stop.get_id().equals(originStopId) && i != route.getStops().size() - 1) {
                    destinationsWithOriginStop.add(route.getDestination());
                    break;
                }
            }
        }

        // set first route as the default route
        if (destinationsWithOriginStop.size() > 0) {
            selectedRouteDestination = destinationsWithOriginStop.get(0);
        }

        if (getActivity() != null) {
            setRefreshIndicatorVisiblity(false);

            currentlySelectedStopIndex = -1;
            updateRoute(selectedRouteDestination);
        }
    }

    public Stop getSelectedDestinationStop() {
        if (currentlySelectedStopIndex == -1) {
            return null;
        } else {
            return selectedRoute.getStops().get(currentlySelectedStopIndex);
        }
    }

    public Route getSelectedRoute() {
        return selectedRoute;
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
        for (Route route : routes) {
            if (route.getDestination().equals(destination)) {
                selectedRoute = route;

                addRouteToMap();

                routeStopsAdapter.notifyRouteChanged();

                routeTextView.setText(String.format(getString(R.string.label_towards),
                        selectedRouteDestination));

                break;
            }
        }
    }

    private void addRouteToMap() {
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

        List<Stop> stops = selectedRoute.getStops();

        Bitmap stopMarkerIcon = Helpers.getMarkerIcon(getActivity(), R.drawable.stop_marker);

        for (int i=0; i<stops.size(); i++) {
            Stop stop = stops.get(i);

            LatLng latLng = stop.getPosition();

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

        PolylineOptions polylineOptions = new PolylineOptions();

        int primaryColor = ContextCompat.getColor(getActivity(), R.color.primary);
        int polylineWidth = getResources().getDimensionPixelOffset(R.dimen.polyline_width);

        polylineOptions
                .addAll(selectedRoute.getLatLngs())
                .width(polylineWidth)
                .color(primaryColor);

        routePolyline = map.addPolyline(polylineOptions);
    }

    private class RouteStopsAdapter extends RecyclerView.Adapter {

        private LayoutInflater inflater;
        private List<Stop> stops = new ArrayList<>();
        private static final int HEADING_VIEW_TYPE = 0;
        private static final int STOP_VIEW_TYPE = 1;
        private int previouslySelectedStopIndex;

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (inflater == null) {
                inflater = LayoutInflater.from(getActivity());
            }

            if (viewType == HEADING_VIEW_TYPE) {
                return new HeadingViewHolder(inflater.inflate(R.layout.row_heading, parent, false));
            } else {
                return new RouteStopViewHolder(inflater.inflate(R.layout.row_selection_destination_stop,
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

        public void notifyRouteChanged() {
            stops = new ArrayList<>(selectedRoute.getStops());

            previouslySelectedStopIndex = currentlySelectedStopIndex;

            int originStopIndex = 0;

            // find index of origin stop so that all stops that come before it can be removed
            for (int i=0; i<stops.size(); i++) {
                if (stops.get(i).get_id().equals(originStopId)) {
                    originStopIndex = i;
                    break;
                }
            }

            stops = stops.subList(originStopIndex, stops.size());

            if (currentlySelectedStopIndex == -1) {
                // set currently selected stop as the stop right after origin stop
                currentlySelectedStopIndex = 1;
            }

            previouslySelectedStopIndex = currentlySelectedStopIndex;

            notifyDataSetChanged();
        }

        private class RouteStopViewHolder extends RecyclerView.ViewHolder
                implements View.OnClickListener {

            private TextView stopNameTextView;
            private View topIndicatorView;
            private View bottomIndicatorView;
            private ImageButton moreOptionsButton;
            private Stop stop;

            public RouteStopViewHolder(View itemView) {
                super(itemView);

                // find views
                stopNameTextView = (TextView) itemView.findViewById(R.id.stop_name_textview);
                topIndicatorView = itemView.findViewById(R.id.top_indicator_view);
                bottomIndicatorView = itemView.findViewById(R.id.bottom_indicator_view);
//                moreOptionsButton = (ImageButton) itemView.findViewById(R.id.more_options_button);

                // bind event listeners
                itemView.setOnClickListener(this);
//                moreOptionsButton.setOnClickListener(this);
            }

            public void bindItem(Stop stop) {
                this.stop = stop;

                stopNameTextView.setText(String.format(getString(
                        R.string.label_stop_name_with_direction), stop.getName(), stop.getDirection()));

                // show indicators for first/last stop in the route
                int adapterPosition = getAdapterPosition() - 1;

                if (adapterPosition == 0) {
                    topIndicatorView.setVisibility(View.INVISIBLE);
                    bottomIndicatorView.setVisibility(View.VISIBLE);
                } else if (adapterPosition == stops.size() - 1) {
                    topIndicatorView.setVisibility(View.VISIBLE);
                    bottomIndicatorView.setVisibility(View.INVISIBLE);
                } else {
                    topIndicatorView.setVisibility(View.VISIBLE);
                    bottomIndicatorView.setVisibility(View.VISIBLE);
                }

                // fade out origin stop since it is not available for selection
                if (adapterPosition == 0) {
                    stopNameTextView.setTextColor(
                            ContextCompat.getColor(getActivity(), R.color.secondary_text_light_2)
                    );

                    int indicatorColor = ContextCompat.getColor(getActivity(), R.color.primary_opaque_40);
                    topIndicatorView.setBackgroundColor(indicatorColor);
                    bottomIndicatorView.setBackgroundColor(indicatorColor);
                } else {
                    stopNameTextView.setTextColor(
                            ContextCompat.getColor(getActivity(), R.color.primary_text_light)
                    );

                    int indicatorColor = ContextCompat.getColor(getActivity(), R.color.primary);
                    topIndicatorView.setBackgroundColor(indicatorColor);
                    bottomIndicatorView.setBackgroundColor(indicatorColor);
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
                if (v.getId() == itemView.getId()) {
                    // only allow selection if selected stop comes after origin stop
                    if (getAdapterPosition() - 1 > 0) {
                        currentlySelectedStopIndex = getAdapterPosition() - 1;

                        notifyItemChanged(currentlySelectedStopIndex + 1);
                        notifyItemChanged(previouslySelectedStopIndex + 1);

                        previouslySelectedStopIndex = currentlySelectedStopIndex;

                        // lookup marker corresponding to selected stop, then show its info
                        // window and move map focus to it
                        LatLng stopLatLng = stop.getPosition();
                        for (Marker marker : stopMarkers) {
                            LatLng latLng = marker.getPosition();

                            if (latLng.latitude == stopLatLng.latitude && latLng.longitude == stopLatLng.longitude) {
                                marker.showInfoWindow();
                                map.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                                break;
                            }
                        }
                    }
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
