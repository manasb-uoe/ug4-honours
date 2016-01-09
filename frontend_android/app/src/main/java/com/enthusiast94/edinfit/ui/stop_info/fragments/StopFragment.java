package com.enthusiast94.edinfit.ui.stop_info.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import com.enthusiast94.edinfit.models.Directions;
import com.enthusiast94.edinfit.models.Point;
import com.enthusiast94.edinfit.models_2.Departure;
import com.enthusiast94.edinfit.models_2.Stop;
import com.enthusiast94.edinfit.network.BaseService;
import com.enthusiast94.edinfit.network.DirectionsService;
import com.enthusiast94.edinfit.network.StopService;
import com.enthusiast94.edinfit.utils.DepartureView;
import com.enthusiast94.edinfit.utils.Helpers;
import com.enthusiast94.edinfit.utils.LiveDepartureView;
import com.enthusiast94.edinfit.utils.LocationProvider;
import com.enthusiast94.edinfit.utils.StopView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
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

    public static final String TAG = StopFragment.class.getSimpleName();
    public static final String EXTRA_STOP_ID = "stop";
    private static final String MAPVIEW_SAVE_STATE = "mapViewSaveState";

    private SwipeRefreshLayout swipeRefreshLayout;
    private MapView mapView;

    private Stop stop;
    private List<Departure> departures;
    private Marker stopMarker;
    private LatLng userLocationLatLng;
    private DeparturesAdapter departuresAdapter;
    private GoogleMap map;
    private LocationProvider locationProvider;
    private List<String> selectedServices;

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

        RecyclerView departuresRecyclerView = (RecyclerView) view.findViewById(R.id.departures_recyclerview);
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_layout);
        mapView = (MapView) view.findViewById(R.id.map_view);

        Bundle mapViewSavedInstanceState = savedInstanceState != null ?
                savedInstanceState.getBundle(MAPVIEW_SAVE_STATE) : null;
        mapView.onCreate(mapViewSavedInstanceState);

        map = mapView.getMap();
        map.getUiSettings().setMyLocationButtonEnabled(true);
        map.setMyLocationEnabled(true);

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

        if (savedInstanceState == null) {
            stop = Stop.findById(getArguments().getString(EXTRA_STOP_ID));
        }

        // add stop marker to map
        LatLng stopLatLng = stop.getPosition();
        stopMarker = map.addMarker(new MarkerOptions()
                .position(stopLatLng)
                .icon(BitmapDescriptorFactory.fromBitmap(Helpers.getMarkerIcon(getActivity(),
                        R.drawable.stop_marker)))
                .title(stop.getName()));
        stopMarker.showInfoWindow();
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(stopLatLng, 15));

        if (!locationProvider.isConnected()) {
            locationProvider.connect();
        } else {
            departuresAdapter.notifyDeparturesChanged(stop, departures, selectedServices,
                    selectedDay, selectedTime);
            addWalkingDirectionsToMap(userLocationLatLng);
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();

        // keep live departures up to date
        if (departures != null && selectedServices != null) {
            departuresAdapter.notifyDeparturesChanged(stop, departures, selectedServices,
                    selectedDay, selectedTime);
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

//        if (UserService.getInstance().getAuthenticatedUser().getSavedStops().contains(stop.getId())) {
//            saveOrUnsaveItem.setIcon(R.drawable.ic_action_toggle_star);
//        } else {
//            saveOrUnsaveItem.setIcon(R.drawable.ic_action_toggle_star_outline);
//        }

        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_filter_departures:
                showServiceSelectorDialog();
                return true;

            case R.id.action_save_or_unsave:
//                final boolean shouldSave = !UserService.getInstance().getAuthenticatedUser().
//                        getSavedStops().contains(stop.getId());
//
//                StopService.getInstance().saveOrUnsaveStop(stop.getId(), shouldSave,
//                        new BaseService.Callback<Void>() {
//
//                            @Override
//                            public void onSuccess(Void data) {
//                                if (getActivity() != null) {
//                                    if (shouldSave) {
//                                        Toast.makeText(getActivity(), String.format(
//                                                getActivity().getString(R.string.success_stop_saved),
//                                                stop.getName()), Toast.LENGTH_SHORT).show();
//                                    } else {
//                                        Toast.makeText(getActivity(), String.format(
//                                                getActivity().getString(R.string.success_stop_unsaved),
//                                                stop.getName()), Toast.LENGTH_SHORT).show();
//                                    }
//
//                                    // invalidate options menu so that it can be redrawn and therefore change
//                                    // save/unsave button icon accordingly
//                                    getActivity().invalidateOptionsMenu();
//                                }
//                            }
//
//                            @Override
//                            public void onFailure(String message) {
//                                if (getActivity() != null) {
//                                    Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
//                                }
//                            }
//                        });
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void loadDepartures(final LatLng userLocationLatLng) {
        setRefreshIndicatorVisiblity(true);

        StopService.getInstance().getDeparturesAsync(stop, null, null,
                new BaseService.Callback<List<Departure>>() {

                    @Override
                    public void onSuccess(List<Departure> departures) {
                        StopFragment.this.departures = departures;

                        if (selectedServices == null) {
                            selectedServices = stop.getServices();
                        }

                        if (getActivity() != null) {
                            setRefreshIndicatorVisiblity(false);

                            departuresAdapter.notifyDeparturesChanged(stop, departures,
                                    selectedServices, selectedDay, selectedTime);

                            addWalkingDirectionsToMap(userLocationLatLng);
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

                        departuresAdapter.notifyDeparturesChanged(stop, departures, selectedServices,
                                selectedDay, selectedTime);

                        dialog.dismiss();
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();
    }

    private void showServiceSelectorDialog() {
        final List<String> allServices = stop.getServices();
        boolean[] checkedItems = new boolean[allServices.size()];
        for (int i=0; i<checkedItems.length; i++) {
            if (selectedServices.contains(allServices.get(i))) {
                checkedItems[i] = true;
            }
        }

        final List<String> selectedServicesTemp = new ArrayList<>(selectedServices);

        AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.label_select_desired_services))
                .setMultiChoiceItems(allServices.toArray(new String[allServices.size()]),
                        checkedItems, new DialogInterface.OnMultiChoiceClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                if (selectedServicesTemp.contains(allServices.get(which))) {
                                    selectedServicesTemp.remove(
                                            selectedServicesTemp.indexOf(allServices.get(which)));
                                } else {
                                    selectedServicesTemp.add(allServices.get(which));
                                }
                            }
                        })
                .setNegativeButton(R.string.label_cancel, null)
                .setPositiveButton(R.string.label_save, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        selectedServices = selectedServicesTemp;

                        departuresAdapter.notifyDeparturesChanged(stop, departures, selectedServices,
                                selectedDay, selectedTime);
                    }
                })
                .create();

        alertDialog.show();
    }

    private void addWalkingDirectionsToMap(final LatLng userLocationLatLng) {
        LatLng stopLatLng = stopMarker.getPosition();

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

                            stopMarker.setSnippet(String.format(getString(
                                    R.string.label_walk_duration_format), directions.getDurationText()));
                            stopMarker.showInfoWindow(); // refresh info window since text was changed
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
            loadDepartures(userLocationLatLng);
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
        private List<Departure> allDepartures;
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
            allDepartures = new ArrayList<>();
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
                return allDepartures.size() + liveDepartures.size() + 3
                /* 1 stop info + 1 selected time heading + 1 live allDepartures heading*/;
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
                    return allDepartures.get(position - 3 - liveDepartures.size());

                default:
                    throw new IllegalArgumentException("Invalid view type: " + viewType);
            }
        }

        public void notifyDeparturesChanged(Stop stop, List<Departure> departures,
                                            List<String> selectedServices, String selectedDay,
                                            String selectedTime) {
            this.stop = stop;
            this.selectedDay = selectedDay;
            this.selectedTime = selectedTime;

            allDepartures.clear();
            liveDepartures.clear();

            try {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm", Locale.UK);
                Date now = simpleDateFormat.parse(Helpers.getCurrentTime24h());

                for (Departure departure : departures) {
                    if (selectedServices.contains(departure.getServiceName())) {
                        Date due = simpleDateFormat.parse(departure.getTime());

                        // add live departures
                        if (departure.getDay() == Helpers.getDayCode(Helpers.getCurrentDay()) &&
                                due.after(now) && liveDepartures.size() < 5) {
                            liveDepartures.add(departure);
                        }

                        // add other departures
                        Date selected = simpleDateFormat.parse(selectedTime);

                        if (departure.getDay() == Helpers.getDayCode(selectedDay) && due.after(selected)) {
                            allDepartures.add(departure);
                        }
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
            private DepartureView departureView;

            public DepartureViewHolder(View itemView) {
                super(itemView);

                departureView = (DepartureView) itemView;
                departureView.setOnClickListener(this);
            }

            public void bindItem(Departure departure) {
                this.departure = departure;

                departureView.bindItem(departure);
            }

            @Override
            public void onClick(View v) {
//                Intent startActivityIntent = new Intent(context, ServiceActivity.class);
//                startActivityIntent.putExtra(ServiceActivity.EXTRA_SERVICE_NAME, departure.getServiceName());
//                context.startActivity(startActivityIntent);
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

            private StopView stopView;

            public StopViewHolder(View itemView) {
                super(itemView);

                stopView = (StopView) itemView.findViewById(R.id.stop_view);
            }

            public void bindItem(Stop stop) {
                // isFavourite is always set as false since favourite star is already shown in
                // the app bar
                stopView.bindItem(stop, false, null);
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
            private LiveDepartureView liveDepartureView;

            public LiveDepartureViewHolder(View itemView) {
                super(itemView);

                liveDepartureView = (LiveDepartureView) itemView;

                liveDepartureView.setOnClickListener(this);
            }

            public void bindItem(Departure departure) {
                this.departure = departure;

                liveDepartureView.bindItem(departure);
            }

            @Override
            public void onClick(View v) {
//                Intent startActivityIntent = new Intent(context, ServiceActivity.class);
//                startActivityIntent.putExtra(ServiceActivity.EXTRA_SERVICE_NAME, departure.getServiceName());
//                context.startActivity(startActivityIntent);
            }
        }

        public abstract void onSetTimeClicked();
    }
}
