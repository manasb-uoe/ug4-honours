package com.enthusiast94.edinfit.ui.stop_info.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models.Departure;
import com.enthusiast94.edinfit.models.Directions;
import com.enthusiast94.edinfit.models.Point;
import com.enthusiast94.edinfit.models.Stop;
import com.enthusiast94.edinfit.network.BaseService;
import com.enthusiast94.edinfit.network.DirectionsService;
import com.enthusiast94.edinfit.network.StopService;
import com.enthusiast94.edinfit.network.UserService;
import com.enthusiast94.edinfit.ui.service_info.activities.ServiceActivity;
import com.enthusiast94.edinfit.utils.Helpers;
import com.enthusiast94.edinfit.utils.LocationProvider;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by manas on 04-10-2015.
 */
public class StopFragment extends Fragment implements LocationProvider.LastKnowLocationCallback {

    private static final String TAG = StopFragment.class.getSimpleName();
    public static final String EXTRA_STOP_ID = "stopId";
    private static final String MAPVIEW_SAVE_STATE = "mapViewSaveState";

    private RecyclerView departuresRecyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private MapView mapView;
    private TextView walkDurationTextView;

    private String stopId;
    private Stop stop;
    private LatLng userLocationLatLng;
    private DeparturesAdapter departuresAdapter;
    private GoogleMap map;
    private LocationProvider locationProvider;

    // default values for day and time
    private String selectedDay = Helpers.getCurrentDay();
    private String selectedTime = Helpers.getCurrentTime24h();

    public static StopFragment newInstance(String stopId) {
        StopFragment instance = new StopFragment();
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_STOP_ID, stopId);
        instance.setArguments(bundle);

        return instance;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        locationProvider = new LocationProvider(getActivity(), this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stop, container, false);

        setHasOptionsMenu(true);

        departuresRecyclerView = (RecyclerView) view.findViewById(R.id.departures_recyclerview);
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_layout);
        mapView = (MapView) view.findViewById(R.id.map_view);
        walkDurationTextView = (TextView) view.findViewById(R.id.walk_duration_textview);

        // retrieve stop id from arguments so that the data corresponding to its stop can be loaded
        stopId = getArguments().getString(EXTRA_STOP_ID);

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

        departuresRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        departuresAdapter = new DeparturesAdapter(getActivity()) {

            @Override
            public void onSetTimeClicked() {
                showTimePickerDialog();
            }
        };
        departuresRecyclerView.setAdapter(departuresAdapter);

        if (!locationProvider.isConnected()) {
            locationProvider.connect();
        } else {
            departuresAdapter.notifyDeparturesChanged(stop, selectedDay, selectedTime);
            updateMapSlidingPanel(userLocationLatLng);
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();

        // keep live departures up to date
        if (stop != null) {
            departuresAdapter.notifyDeparturesChanged(stop, selectedDay, selectedTime);
        }
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

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_stop_info, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        // set save/unsave button icon depending on whether the current stop is saved or not
        MenuItem saveOrUnsaveItem = menu.findItem(R.id.action_save_or_unsave);

        if (UserService.getInstance().getAuthenticatedUser().getSavedStops().contains(stopId)) {
            saveOrUnsaveItem.setIcon(R.drawable.ic_action_toggle_star);
        } else {
            saveOrUnsaveItem.setIcon(R.drawable.ic_action_toggle_star_outline);
        }

        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save_or_unsave:
                final boolean shouldSave = !UserService.getInstance().getAuthenticatedUser().
                        getSavedStops().contains(stopId);

                StopService.getInstance().saveOrUnsaveStop(stopId, shouldSave,
                        new BaseService.Callback<Void>() {

                            @Override
                            public void onSuccess(Void data) {
                                if (getActivity() != null) {
                                    if (shouldSave) {
                                        Toast.makeText(getActivity(), String.format(
                                                getActivity().getString(R.string.success_stop_saved),
                                                stop.getName()), Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(getActivity(), String.format(
                                                getActivity().getString(R.string.success_stop_unsaved),
                                                stop.getName()), Toast.LENGTH_SHORT).show();
                                    }

                                    // invalidate options menu so that it can be redrawn and therefore change
                                    // save/unsave button icon accordingly
                                    getActivity().invalidateOptionsMenu();
                                }
                            }

                            @Override
                            public void onFailure(String message) {
                                if (getActivity() != null) {
                                    Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void loadStop(final LatLng userLocationLatLng) {
        setRefreshIndicatorVisiblity(true);

        StopService.getInstance().getStop(stopId, null, null, new BaseService.Callback<Stop>() {

            @Override
            public void onSuccess(Stop data) {
                stop = data;

                if (getActivity() != null) {
                    setRefreshIndicatorVisiblity(false);

                    departuresAdapter.notifyDeparturesChanged(stop, selectedDay, selectedTime);

                    updateMapSlidingPanel(userLocationLatLng);
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

    private void showTimePickerDialog() {
        View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_time_picker, null);

        // find views
        final NumberPicker dayPicker = (NumberPicker) dialogView.findViewById(R.id.day_picker);
        final NumberPicker hourPicker = (NumberPicker) dialogView.findViewById(R.id.hour_picker);
        final NumberPicker minutePicker = (NumberPicker) dialogView.findViewById(R.id.minute_picker);

        // set value ranges and current values for pickers
        final String[] daysOfTheWeek = getResources().getStringArray(R.array.days_of_the_week);
        dayPicker.setMinValue(0);
        dayPicker.setMaxValue(daysOfTheWeek.length - 1);
        dayPicker.setDisplayedValues(daysOfTheWeek);
        for (int i=0; i<daysOfTheWeek.length; i++) {
            if (daysOfTheWeek[i].equals(selectedDay)) {
                dayPicker.setValue(i);
            }
        }

        hourPicker.setMinValue(0);
        hourPicker.setMaxValue(23);
        hourPicker.setValue(Integer.parseInt(selectedTime.substring(0,
                selectedTime.indexOf(":"))));

        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);
        minutePicker.setValue(Integer.parseInt(selectedTime.substring(
                selectedTime.indexOf(":") + 1, selectedTime.length())));

        AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setTitle("Select time")
                .setView(dialogView)
                .setPositiveButton("Set", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        selectedDay = daysOfTheWeek[dayPicker.getValue()];
                        selectedTime = hourPicker.getValue() + ":" +
                                minutePicker.getValue();

                        departuresAdapter.notifyDeparturesChanged(stop, selectedDay, selectedTime);

                        dialog.dismiss();
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();
    }

    private void updateMapSlidingPanel(LatLng userLocationLatLng) {
        LatLng stopLatLng = new LatLng(stop.getLocation().get(1), stop.getLocation().get(0));

        // add stop marker with info window containing stop name
        map.addMarker(new MarkerOptions()
                .position(stopLatLng)
                .icon(BitmapDescriptorFactory.fromBitmap(Helpers.getMarkerIcon(getActivity(),
                        R.drawable.stop_marker)))
                .title(stop.getName())).showInfoWindow();

        // move map focus to user's last known location
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(stopLatLng, 16));

        DirectionsService.getInstance().getWalkingDirections(userLocationLatLng, stopLatLng,
                new BaseService.Callback<Directions>() {

                    @Override
                    public void onSuccess(Directions directions) {
                        if (getActivity() != null) {
                            // add walking route from user to stop
                            PolylineOptions polylineOptions = new PolylineOptions();

                            for (Point point : directions.getOverviewPoints()) {
                                polylineOptions.add(new LatLng(point.getLatitude(), point.getLongitude()));
                            }

                            polylineOptions.color(ContextCompat.getColor(getActivity(), R.color.red));
                            polylineOptions.width(getResources().getDimensionPixelOffset(R.dimen.polyline_width));

                            map.addPolyline(polylineOptions);

                            // update drag view title with travel duration
                            walkDurationTextView.setText(String.format(
                                    getString(R.string.label_walk_duration_base), directions.getDurationText()));
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
        if (getActivity() != null) {
            this.userLocationLatLng = new LatLng(location.getLatitude(), location.getLongitude());
            loadStop(userLocationLatLng);
        }
    }

    @Override
    public void onLastKnownLocationFailure(String error) {
        if (getActivity() != null) {
            setRefreshIndicatorVisiblity(false);
            Toast.makeText(getActivity(), error, Toast.LENGTH_LONG).show();
        }
    }

    private static abstract class DeparturesAdapter extends RecyclerView.Adapter {

        private Context context;
        private LayoutInflater inflater;
        private Stop stop;
        private List<Departure> liveDepartures;
        private List<Departure> departures;
        private String selectedDay;
        private String selectedTime;

        private final int STOP_VIEW_TYPE = 0;
        private final int LIVE_DEPARTURES_HEADING_VIEW_TYPE = 1;
        private final int LIVE_DEPARTURE_VIEW_TYPE = 2;
        private final int TIME_SELECTOR_VIEW_TYPE = 3;
        private final int DEPARTURE_VIEW_TYPE = 4;

        public DeparturesAdapter(Context context) {
            this.context = context;
            inflater = LayoutInflater.from(context);
            departures = new ArrayList<>();
            liveDepartures = new ArrayList<>();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            switch (viewType) {
                case STOP_VIEW_TYPE:
                    return new StopViewHolder(inflater.inflate(
                            R.layout.row_stop_info, parent, false));

                case LIVE_DEPARTURES_HEADING_VIEW_TYPE:
                    return new LiveDepartureHeadingViewHolder(inflater.inflate(
                            R.layout.row_stop_info_live_departures_heading, parent, false));

                case LIVE_DEPARTURE_VIEW_TYPE:
                    return new LiveDepartureViewHolder(inflater.inflate(
                            R.layout.row_stop_info_live_departure, parent, false));

                case TIME_SELECTOR_VIEW_TYPE:
                    return new TimeSelectorViewHolder(inflater.inflate(
                            R.layout.row_stop_info_time_selector_heading, parent, false));

                case DEPARTURE_VIEW_TYPE:
                    return new DepartureViewHolder(inflater.inflate(
                            R.layout.row_stop_info_departure, parent, false));

                default:
                    throw new IllegalArgumentException("Invalid view type: " + viewType);
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof StopViewHolder) {
                ((StopViewHolder) holder).bindItem((Stop) getItem(position));
            } else if (holder instanceof TimeSelectorViewHolder) {
                ((TimeSelectorViewHolder) holder).bindItem((Pair<String, String>) getItem(position));
            } else if (holder instanceof LiveDepartureViewHolder) {
                ((LiveDepartureViewHolder) holder).bindItem((Departure) getItem(position));
            }
            else if (holder instanceof DepartureViewHolder) {
                ((DepartureViewHolder) holder).bindItem((Departure) getItem(position));
            } else {
                if (!(holder instanceof LiveDepartureHeadingViewHolder)) {
                    throw new IllegalArgumentException("Invalid holder type: " +
                            holder.getClass().getSimpleName());
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0) {
                return STOP_VIEW_TYPE;
            } else if (position == 1) {
                return LIVE_DEPARTURES_HEADING_VIEW_TYPE;
            } else if (position > 1 && position < 2 + liveDepartures.size()) {
                return LIVE_DEPARTURE_VIEW_TYPE;
            } else if (position == 2 + liveDepartures.size()) {
                return TIME_SELECTOR_VIEW_TYPE;
            } else {
                return DEPARTURE_VIEW_TYPE;
            }
        }

        @Override
        public int getItemCount() {
            if (stop == null) {
                return 0;
            } else {
                return departures.size() + liveDepartures.size() + 3
                /* 1 stop info + 1 selected time heading + 1 live departures heading*/;
            }
        }

        private Object getItem(int position) {
            int viewType = getItemViewType(position);

            switch (viewType) {
                case STOP_VIEW_TYPE:
                    return stop;

                case LIVE_DEPARTURES_HEADING_VIEW_TYPE:
                    return null;

                case LIVE_DEPARTURE_VIEW_TYPE:
                    return liveDepartures.get(position - 2);

                case TIME_SELECTOR_VIEW_TYPE:
                    return new Pair<>(selectedDay, selectedTime);

                case DEPARTURE_VIEW_TYPE:
                    return departures.get(position - 3 - liveDepartures.size());

                default:
                    throw new IllegalArgumentException("Invalid view type: " + viewType);
            }
        }

        public void notifyDeparturesChanged(Stop stop, String selectedDay,
                                            String selectedTime) {
            this.stop = stop;
            this.selectedDay = selectedDay;
            this.selectedTime = selectedTime;

            departures.clear();
            liveDepartures.clear();

            try {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm", Locale.UK);
                Date now = simpleDateFormat.parse(Helpers.getCurrentTime24h());

                for (Departure departure : stop.getDepartures()) {
                    Date due = simpleDateFormat.parse(departure.getTime());

                    // add live departures
                    if (departure.getDay() == Helpers.getDayCode(Helpers.getCurrentDay()) &&
                            due.after(now) && liveDepartures.size() < 5) {
                        liveDepartures.add(departure);
                    }

                    // add other departures
                    Date selected = simpleDateFormat.parse(selectedTime);

                    if (departure.getDay() == Helpers.getDayCode(selectedDay) && due.after(selected)) {
                        departures.add(departure);
                    }
                }
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }

            notifyDataSetChanged();
        }

        private class DepartureViewHolder extends RecyclerView.ViewHolder
                implements View.OnClickListener {

            private Departure departure;
            private TextView serviceNameTextView;
            private TextView destinationTextView;
            private TextView timeTextView;

            public DepartureViewHolder(View itemView) {
                super(itemView);

                // find views
                serviceNameTextView =
                        (TextView) itemView.findViewById(R.id.service_name_textview);
                destinationTextView =
                        (TextView) itemView.findViewById(R.id.destination_textview);
                timeTextView =
                        (TextView) itemView.findViewById(R.id.time_textview);

                // bind event listeners
                itemView.setOnClickListener(this);
            }

            public void bindItem(Departure departure) {
                this.departure = departure;

                serviceNameTextView.setText(departure.getServiceName());
                destinationTextView.setText(departure.getDestination());
                timeTextView.setText(departure.getTime());
            }

            @Override
            public void onClick(View v) {
                Intent startActivityIntent = new Intent(context, ServiceActivity.class);
                startActivityIntent.putExtra(ServiceActivity.EXTRA_SERVICE_NAME, departure.getServiceName());
                context.startActivity(startActivityIntent);
            }
        }

        private class TimeSelectorViewHolder extends RecyclerView.ViewHolder {

            private TextView selectedTimeTextView;
            private TextView setTimeTextView;

            public TimeSelectorViewHolder(View itemView) {
                super(itemView);

                // find views
                selectedTimeTextView = (TextView) itemView.findViewById(R.id.selected_time_textview);
                setTimeTextView = (TextView) itemView.findViewById(R.id.set_time_textview);

                // bind event listeners
                setTimeTextView.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        onSetTimeClicked();
                    }
                });
            }

            public void bindItem(Pair<String, String> pair) {
                selectedTimeTextView.setText(String.format(context.getString(
                        R.string.label_timetable_format), pair.first + ", " + pair.second));
            }
        }

        private class StopViewHolder extends RecyclerView.ViewHolder {

            private TextView stopNameTextView;
            private TextView directionTextView;
            private TextView servicesTextView;
            private TextView destinationsTextView;
            private TextView idTextView;

            public StopViewHolder(View itemView) {
                super(itemView);

                // find views
                stopNameTextView = (TextView) itemView.findViewById(R.id.stop_name_textview);
                directionTextView = (TextView) itemView.findViewById(R.id.direction_textview);
                servicesTextView = (TextView) itemView.findViewById(R.id.services_textview);
                destinationsTextView = (TextView) itemView.findViewById(R.id.destinations_textview);
                idTextView = (TextView) itemView.findViewById(R.id.stop_id_textview);
            }

            public void bindItem(Stop stop) {
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
            }
        }

        private class LiveDepartureHeadingViewHolder extends RecyclerView.ViewHolder {

            public LiveDepartureHeadingViewHolder(View itemView) {
                super(itemView);
            }
        }

        private class LiveDepartureViewHolder extends RecyclerView.ViewHolder
                implements View.OnClickListener {

            private Departure departure;
            private TextView serviceNameTextView;
            private TextView destinationTextView;
            private TextView timeTextView;

            public LiveDepartureViewHolder(View itemView) {
                super(itemView);

                // find views
                serviceNameTextView =
                        (TextView) itemView.findViewById(R.id.service_name_textview);
                destinationTextView =
                        (TextView) itemView.findViewById(R.id.destination_textview);
                timeTextView =
                        (TextView) itemView.findViewById(R.id.time_textview);

                // bind event listeners
                itemView.setOnClickListener(this);
            }

            public void bindItem(Departure departure) {
                this.departure = departure;

                serviceNameTextView.setText(departure.getServiceName());
                destinationTextView.setText(departure.getDestination());
                timeTextView.setText(Helpers.humanizeLiveDepartureTime(departure.getTime()));
            }

            @Override
            public void onClick(View v) {
                Intent startActivityIntent = new Intent(context, ServiceActivity.class);
                startActivityIntent.putExtra(ServiceActivity.EXTRA_SERVICE_NAME, departure.getServiceName());
                context.startActivity(startActivityIntent);
            }
        }

        public abstract void onSetTimeClicked();
    }
}
