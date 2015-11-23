package com.enthusiast94.edinfit.ui.stop_info.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
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
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models.Departure;
import com.enthusiast94.edinfit.models.Directions;
import com.enthusiast94.edinfit.models.Point;
import com.enthusiast94.edinfit.models.Stop;
import com.enthusiast94.edinfit.services.BaseService;
import com.enthusiast94.edinfit.services.DirectionsService;
import com.enthusiast94.edinfit.services.LocationProviderService;
import com.enthusiast94.edinfit.services.StopService;
import com.enthusiast94.edinfit.services.UserService;
import com.enthusiast94.edinfit.ui.service_info.activities.ServiceActivity;
import com.enthusiast94.edinfit.utils.Helpers;
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
public class StopFragment extends Fragment implements LocationProviderService.LastKnownLocationCallback {

    public static final String EXTRA_STOP_ID = "stopId";
    private static final String MAPVIEW_SAVE_STATE = "mapViewSaveState";
    private String stopId;
    private Stop stop;
    private RecyclerView departuresRecyclerView;
    private DeparturesAdapter departuresAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private MapView mapView;
    private GoogleMap map;
    private TextView walkDurationTextView;

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
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stop, container, false);

        setHasOptionsMenu(true);

        /**
         * Find views
         */

        departuresRecyclerView = (RecyclerView) view.findViewById(R.id.departures_recyclerview);
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_layout);
        mapView = (MapView) view.findViewById(R.id.map_view);
        walkDurationTextView = (TextView) view.findViewById(R.id.walk_duration_textview);

        /**
         * Retrieve stop id from arguments so that the data corresponding to its stop can be loaded
         */

        stopId = getArguments().getString(EXTRA_STOP_ID);

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
         * Setup swipe refresh layout
         */

        swipeRefreshLayout.setColorSchemeResources(R.color.accent);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {
                LocationProviderService.getInstance().requestLastKnownLocationInfo(false, StopFragment.this);
            }
        });

        /**
         * Setup departures recycler view
         */

        departuresRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        departuresAdapter = new DeparturesAdapter();
        departuresRecyclerView.setAdapter(departuresAdapter);

        /**
         * Finally, request user location in order to get things started
         */

        LocationProviderService.getInstance().requestLastKnownLocationInfo(false, this);

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

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_stop_fragment, menu);
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
            case R.id.action_select_time:
                showTimePickerDialog();
                return true;
            case R.id.action_save_or_unsave:
                final boolean shouldSave = !UserService.getInstance().getAuthenticatedUser().
                        getSavedStops().contains(stopId);

                StopService.getInstance().saveOrUnsaveStop(stopId, shouldSave,
                       new BaseService.Callback<Void>() {

                    @Override
                    public void onSuccess(Void data) {
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

                    @Override
                    public void onFailure(String message) {
                        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
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

                    departuresAdapter.notifyDeparturesChanged();

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

                        departuresAdapter.notifyDeparturesChanged();

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
                .icon(BitmapDescriptorFactory.fromBitmap(Helpers.getStopMarkerIcon(getActivity())))
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
    public void onLocationSuccess(LatLng latLng, String placeName) {
        if (getActivity() != null) {
            loadStop(latLng);
        }
    }

    @Override
    public void onLocationFailure(String error) {
        if (getActivity() != null) {
            setRefreshIndicatorVisiblity(false);
            Toast.makeText(getActivity(), error, Toast.LENGTH_LONG).show();
        }
    }

    private class DeparturesAdapter extends RecyclerView.Adapter {

        private LayoutInflater inflater;
        private List<Departure> departures = new ArrayList<>();
        private final int DAY_SELECTOR_TYPE = 0;
        private final int DEPARTURE_VIEW_TYPE = 1;

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (inflater == null) {
                inflater = LayoutInflater.from(getActivity());
            }

            if (viewType == DAY_SELECTOR_TYPE) {
                return new TimeSelectorViewHolder(inflater.inflate(R.layout.row_time_selector, parent, false));
            } else {
                return new DepartureViewHolder(inflater.inflate(R.layout.row_departure, parent, false));
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            int viewType = getItemViewType(position);

            if (viewType == DAY_SELECTOR_TYPE) {
                ((TimeSelectorViewHolder) holder).bindItem();
            } else {
                ((DepartureViewHolder) holder).bindItem((Departure) getItem(position));
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0) {
                return DAY_SELECTOR_TYPE;
            } else {
                return DEPARTURE_VIEW_TYPE;
            }
        }

        @Override
        public int getItemCount() {
            return departures.size();
        }

        private Object getItem(int position) {
            if (position == 0) {
                return null;
            } else {
                return departures.get(position - 1);
            }
        }

        private void notifyDeparturesChanged() {
            departures.clear();

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm", Locale.UK);

            for (Departure departure : stop.getDepartures()) {
                try {
                    Date selected = simpleDateFormat.parse(selectedTime);
                    Date due = simpleDateFormat.parse(departure.getTime());

                    if (departure.getDay() == Helpers.getDayCode(selectedDay) && due.after(selected)) {
                        departures.add(departure);
                    }
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            }

            notifyDataSetChanged();
        }

        private class DepartureViewHolder extends RecyclerView.ViewHolder
                implements View.OnClickListener{

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
                Intent startActivityIntent = new Intent(getActivity(), ServiceActivity.class);
                startActivityIntent.putExtra(ServiceActivity.EXTRA_SERVICE_NAME, departure.getServiceName());
                startActivity(startActivityIntent);
            }
        }

        private class TimeSelectorViewHolder extends RecyclerView.ViewHolder {

            private TextView timeTextView;

            public TimeSelectorViewHolder(View itemView) {
                super(itemView);

                // find views
                timeTextView = (TextView) itemView.findViewById(R.id.time_textview);
            }

            public void bindItem() {
                timeTextView.setText(selectedDay + ", " + selectedTime);
            }
        }
    }
}
