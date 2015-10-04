package com.enthusiast94.edinfit.fragments;

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
import android.widget.TextView;
import android.widget.Toast;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models.Departure;
import com.enthusiast94.edinfit.models.Stop;
import com.enthusiast94.edinfit.network.Callback;
import com.enthusiast94.edinfit.network.StopService;
import com.enthusiast94.edinfit.utils.Helpers;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by manas on 04-10-2015.
 */
public class StopDeparturesFragment extends Fragment {

    public static final String EXTRA_STOP_ID = "stopId";
    private String stopId;
    private Stop stop;
    private RecyclerView departuresRecyclerView;
    private DeparturesAdapter departuresAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;

    // default values for day and time
    private String selectedDay = Helpers.getCurrentDay();
    private String selectedTime = Helpers.getCurrentTime24h();

    public static StopDeparturesFragment newInstance(String stopId) {
        StopDeparturesFragment instance = new StopDeparturesFragment();
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
        View view = inflater.inflate(R.layout.fragment_stop_departures, container, false);

        /**
         * Find views
         */

        departuresRecyclerView = (RecyclerView) view.findViewById(R.id.departures_recyclerview);
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_layout);

        /**
         * Retrieve stop id from arguments so that the data corresponding to its stop can be loaded
         */

        stopId = getArguments().getString(EXTRA_STOP_ID);

        /**
         * Setup swipe refresh layout
         */

        swipeRefreshLayout.setColorSchemeResources(R.color.accent);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {
                loadStop();
            }
        });

        /**
         * Setup departures recycler view
         */

        departuresRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        departuresAdapter = new DeparturesAdapter();
        departuresRecyclerView.setAdapter(departuresAdapter);

        /**
         * Load stop if it hasn't already been loaded
         */

        if (stop == null) {
            loadStop();
        } else {
            departuresAdapter.notifyDeparturesChanged();
        }

        return view;
    }

    private void loadStop() {
        setRefreshIndicatorVisiblity(true);

        StopService.getStop(stopId, new Callback<Stop>() {

                    @Override
                    public void onSuccess(Stop data) {
                        stop = data;

                        if (getActivity() != null) {
                            setRefreshIndicatorVisiblity(false);

                            departuresAdapter.notifyDeparturesChanged();
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
                // TODO
            }
        }

        private class TimeSelectorViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

            private TextView timeTextView;
            private ImageButton selectTimeButton;

            public TimeSelectorViewHolder(View itemView) {
                super(itemView);

                // find views
                timeTextView = (TextView) itemView.findViewById(R.id.time_textview);
                selectTimeButton = (ImageButton) itemView.findViewById(R.id.select_time_button);

                // bind event listeners
                selectTimeButton.setOnClickListener(this);
            }

            public void bindItem() {
                timeTextView.setText(selectedDay + ", " + selectedTime);
            }

            @Override
            public void onClick(View v) {
                bindItem();
                notifyDeparturesChanged();
            }
        }
    }
}
