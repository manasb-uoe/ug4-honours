package com.enthusiast94.edinfit.ui.service_info.fragments;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.arasthel.asyncjob.AsyncJob;
import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models_2.Departure;
import com.enthusiast94.edinfit.models_2.Stop;
import com.enthusiast94.edinfit.models_2.StopToStopJourney;
import com.enthusiast94.edinfit.network.BaseService;
import com.enthusiast94.edinfit.network.StopService;
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
 * Created by manas on 12-01-2016.
 */
public class ServiceTimetableFragment extends Fragment {

    public static final String TAG = ServiceTimetableFragment.class.getSimpleName();
    private static final String EXTRA_START_STOP_ID = "startStopId";
    private static final String EXTRA_FINISH_STOP_ID = "finishStopId";
    private static final String EXTRA_SERVICE_NAME = "serviceName";
    private static final String EXTRA_TIME = "time";
    private static final String MAPVIEW_SAVE_STATE = "mapViewSaveState";

    private SlidingUpPanelLayout slidingUpPanelLayout;
    private MapView mapView;

    private GoogleMap map;
    private DeparturesAdapter departuresAdapter;
    private StopToStopJourney journey;
    private List<Marker> stopMarkers;
    private Polyline routePolyline;

    public static ServiceTimetableFragment newInstance(
            String startStopId,
            String finishStopId,
            String serviceName,
            String time) {

        ServiceTimetableFragment instance = new ServiceTimetableFragment();
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_START_STOP_ID, startStopId);
        bundle.putString(EXTRA_FINISH_STOP_ID, finishStopId);
        bundle.putString(EXTRA_SERVICE_NAME, serviceName);
        bundle.putString(EXTRA_TIME, time);
        instance.setArguments(bundle);

        return instance;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_service_timetable, container, false);

        // find views
        RecyclerView departuresRecyclerView =
                (RecyclerView) view.findViewById(R.id.departures_recyclerview);
        mapView = (MapView) view.findViewById(R.id.map_view);
        slidingUpPanelLayout = (SlidingUpPanelLayout) view.findViewById(R.id.sliding_layout);

        // Create MapView, get GoogleMap from it and then configure the GoogleMap
        Bundle mapViewSavedInstanceState = savedInstanceState != null ?
                savedInstanceState.getBundle(MAPVIEW_SAVE_STATE) : null;
        mapView.onCreate(mapViewSavedInstanceState);
        map = mapView.getMap();
        map.getUiSettings().setMyLocationButtonEnabled(true);
        map.setMyLocationEnabled(true);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(Helpers.getEdinburghLatLng(getActivity()), 12));

        // retrieve arguments data required to load stop-to-stop timetable
        Bundle bundle = getArguments();
        String startStopId = bundle.getString(EXTRA_START_STOP_ID);
        String finishStopId = bundle.getString(EXTRA_FINISH_STOP_ID);
        String serviceName = bundle.getString(EXTRA_SERVICE_NAME);
        String time = bundle.getString(EXTRA_TIME);

        // setup departures recycler view
        departuresRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        departuresAdapter = new DeparturesAdapter(getActivity()) {
            @Override
            public void onDepartureClicked(StopToStopJourney.Departure departure) {
                // lookup marker corresponding to provided stop, then show its info window and move
                // map focus to it
                LatLng stopLatLng = departure.getStop().getPosition();
                for (Marker marker : stopMarkers) {
                    LatLng latLng = marker.getPosition();

                    if (latLng.latitude == stopLatLng.latitude && latLng.longitude == stopLatLng.longitude) {
                        marker.showInfoWindow();
                        map.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                        slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                        break;
                    }
                }
            }
        };
        departuresRecyclerView.setAdapter(departuresAdapter);

        // load stop to stop journeys if they have already not been loaded
        if (journey == null) {
            loadJourneys(startStopId, finishStopId, serviceName, time);
        } else {
            departuresAdapter.notifyDeparturesChanged(journey.getDepartures());
            updateMap(journey.getDepartures());
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

    private void loadJourneys(final String startStopId, final String finishStopId, final String serviceName, final String time) {
        new AsyncJob.AsyncJobBuilder<BaseService.Response<StopToStopJourney>>()
                .doInBackground(new AsyncJob.AsyncAction<BaseService.Response<StopToStopJourney>>() {
                    @Override
                    public BaseService.Response<StopToStopJourney> doAsync() {
                        return StopService.getInstance().getStopToStopJourneys(startStopId,
                                finishStopId, serviceName, time);
                    }
                })
                .doWhenFinished(new AsyncJob.AsyncResultAction<BaseService.Response<StopToStopJourney>>() {
                    @Override
                    public void onResult(BaseService.Response<StopToStopJourney> response) {
                        if (getActivity() == null) {
                            return;
                        }

                        if (!response.isSuccessfull()) {
                            Toast.makeText(getActivity(), response.getError(), Toast.LENGTH_SHORT)
                                    .show();
                            return;
                        }

                        journey = response.getBody();
                        departuresAdapter.notifyDeparturesChanged(journey.getDepartures());
                        updateMap(journey.getDepartures());
                    }
                }).create().start();
    }

    private void updateMap(List<StopToStopJourney.Departure> departures) {
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

        // add stop markers and route polyline to map
        PolylineOptions polylineOptions = new PolylineOptions();
        polylineOptions.color(ContextCompat.getColor(getActivity(), R.color.primary));
        polylineOptions.width(getResources().getDimensionPixelOffset(R.dimen.polyline_width));

        Bitmap stopMarkerIcon = Helpers.getMarkerIcon(getActivity(), R.drawable.stop_marker);

        for (int i=0; i<departures.size(); i++) {
            StopToStopJourney.Departure departure = departures.get(i);
            Stop stop = departure.getStop();

            LatLng stopLatLng = stop.getPosition();

            Marker stopMarker = map.addMarker(new MarkerOptions()
                    .position(stopLatLng)
                    .anchor(.5f, .5f)
                    .title(stop.getName())
                    .snippet(String.format(getString(R.string.arrives_at_format), departure.getTime()))
                    .icon(BitmapDescriptorFactory.fromBitmap(stopMarkerIcon)));

            if (i == 0) {
                stopMarker.showInfoWindow();
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(stopLatLng, 13));
            }

            stopMarkers.add(stopMarker);

            polylineOptions.add(stop.getPosition());
        }

        routePolyline = map.addPolyline(polylineOptions);
    }

    private static abstract class DeparturesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private Context context;
        private LayoutInflater inflater;
        private List<StopToStopJourney.Departure> departures = new ArrayList<>();

        public DeparturesAdapter(Context context) {
            this.context = context;
            inflater = LayoutInflater.from(context);
            departures = new ArrayList<>();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new DepartureViewHolder(inflater.inflate(R.layout.row_sevice_timetable,
                    parent, false));
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            ((DepartureViewHolder) holder).bindItem(departures.get(position));
        }

        @Override
        public int getItemCount() {
            return departures.size();
        }

        public void notifyDeparturesChanged(List<StopToStopJourney.Departure> departures) {
            this.departures = departures;
            notifyDataSetChanged();
        }

        private class DepartureViewHolder extends RecyclerView.ViewHolder
                implements View.OnClickListener {

            private StopToStopJourney.Departure departure;
            private TextView stopNameTextView;
            private TextView timeTextView;
            private View topIndicatorView;
            private View bottomIndicatorView;

            public DepartureViewHolder(View itemView) {
                super(itemView);

                // find views
                stopNameTextView = (TextView) itemView.findViewById(R.id.stop_name_textview);
                timeTextView = (TextView) itemView.findViewById(R.id.time_textview);
                topIndicatorView = itemView.findViewById(R.id.top_indicator_view);
                bottomIndicatorView = itemView.findViewById(R.id.bottom_indicator_view);

                // bind event listeners
                itemView.setOnClickListener(this);
            }

            public void bindItem(StopToStopJourney.Departure departure) {
                this.departure = departure;

                Stop stop = departure.getStop();
                stopNameTextView.setText(String.format(context.getString(
                        R.string.label_stop_name_with_direction), stop.getName(), stop.getDirection()));
                timeTextView.setText(Helpers.humanizeLiveDepartureTime(departure.getTime()));

                // show indicators for first/last stop in the route
                if (getAdapterPosition() == 0) {
                    topIndicatorView.setVisibility(View.INVISIBLE);
                    bottomIndicatorView.setVisibility(View.VISIBLE);
                } else if (getAdapterPosition() == departures.size() - 1) {
                    topIndicatorView.setVisibility(View.VISIBLE);
                    bottomIndicatorView.setVisibility(View.INVISIBLE);
                } else {
                    topIndicatorView.setVisibility(View.VISIBLE);
                    bottomIndicatorView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onClick(View v) {
                if (v.getId() == itemView.getId())
                    onDepartureClicked(departure);
            }
        }

        public abstract void onDepartureClicked(StopToStopJourney.Departure departure);
    }
}
