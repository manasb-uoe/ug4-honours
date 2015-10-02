package com.enthusiast94.edinfit.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models.Departure;
import com.enthusiast94.edinfit.models.Stop;
import com.enthusiast94.edinfit.network.Callback;
import com.enthusiast94.edinfit.network.StopService;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by manas on 01-10-2015.
 */
public class NearbyFragment extends Fragment {

    public static final String TAG = NearbyFragment.class.getSimpleName();
    @Bind(R.id.nearby_stops_recyclerview) RecyclerView nearbyStopsRecyclerView;
    private NearbyStopsAdapter nearbyStopsAdapter;
    private List<Stop> nearbyStops = new ArrayList<>();

    // nearby stops api endpoint params
    private static final int NEARBY_STOPS_LIMIT = 15;
    private static final int NEAREST_STOPS_LIMIT = 3;
    private static final int NEAREST_STOPS_DEPARTURES_LIMIT = 3;
    private static final boolean ONLY_INCLUDE_UPCOMING_DEPARTURES = true;
    private static final int MAX_DISTANCE = 1;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_nearby, container, false);
        ButterKnife.bind(this, view);

        /**
         * Setup nearby stops recycler view
         */

        nearbyStopsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        nearbyStopsAdapter = new NearbyStopsAdapter();
        nearbyStopsRecyclerView.setAdapter(nearbyStopsAdapter);

        /**
         * Load nearby stops from network
         */

        if (savedInstanceState == null) {
            loadNearbyStops();
        }

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }

    private void loadNearbyStops() {
        StopService.getNearbyStops(55.9425366, -3.2010753, NEARBY_STOPS_LIMIT, NEAREST_STOPS_LIMIT,
                NEAREST_STOPS_DEPARTURES_LIMIT, ONLY_INCLUDE_UPCOMING_DEPARTURES, MAX_DISTANCE,
                new Callback<List<Stop>>() {

            @Override
            public void onSuccess(List<Stop> data) {
                nearbyStops = data;

                if (getActivity() != null) {
                    nearbyStopsAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(String message) {
                if (getActivity() != null) {
                    Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private class NearbyStopsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private LayoutInflater inflater;
        private final int HEADING_VIEW_TYPE = 0;
        private final int STOP_VIEW_TYPE = 1;

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (inflater == null) {
                inflater = LayoutInflater.from(getActivity());
            }

            if (viewType == HEADING_VIEW_TYPE) {
                return new HeadingViewHolder(inflater.inflate(R.layout.row_heading, parent, false));
            } else {
                return new NearbyStopViewHolder(inflater.inflate(R.layout.row_nearby_stop, parent, false));
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (getItemViewType(position) == HEADING_VIEW_TYPE) {
                ((HeadingViewHolder) holder).bindItem(position);
            } else {
                if (position <= NEAREST_STOPS_LIMIT) {
                    ((NearbyStopViewHolder) holder).bindItem(nearbyStops.get(position-1));
                } else {
                    ((NearbyStopViewHolder) holder).bindItem(nearbyStops.get(position-2));
                }
            }
        }

        @Override
        public int getItemCount() {
            return nearbyStops.size();
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0 || position == NEAREST_STOPS_LIMIT + 1) {
                return HEADING_VIEW_TYPE;
            } else {
                return STOP_VIEW_TYPE;
            }
        }

        private class NearbyStopViewHolder extends RecyclerView.ViewHolder {

            private TextView stopNameTextView;
            private LinearLayout departuresContainer;

            public NearbyStopViewHolder(View itemView) {
                super(itemView);

                stopNameTextView = (TextView) itemView.findViewById(R.id.stop_name_textview);
                departuresContainer = (LinearLayout) itemView.findViewById(R.id.departures_container);

                ButterKnife.bind(this, itemView);
            }

            public void bindItem(Stop stop) {
                stopNameTextView.setText(stop.getName());

                // add upcoming departures to departures container
                departuresContainer.removeAllViews();

                for (Departure departure : stop.getDepartures()) {
                    View departureView =
                            inflater.inflate(R.layout.row_departure, departuresContainer, false);

                    TextView serviceNameTextView =
                            (TextView) departureView.findViewById(R.id.service_name_textview);
                    TextView destinationTextView =
                            (TextView) departureView.findViewById(R.id.destination_textview);
                    TextView timeTextView =
                            (TextView) departureView.findViewById(R.id.time_textview);

                    serviceNameTextView.setText(departure.getServiceName());
                    destinationTextView.setText(departure.getDestination());
                    timeTextView.setText(departure.getTime());

                    departuresContainer.addView(departureView);
                }
            }
        }

        private class HeadingViewHolder extends RecyclerView.ViewHolder {

            private TextView headingTextView;

            public HeadingViewHolder(View itemView) {
                super(itemView);

                headingTextView = (TextView) itemView.findViewById(R.id.heading_textview);
            }

            public void bindItem(int position) {
                if (position == 0) {
                    headingTextView.setText(getString(R.string.label_nearest_bus_stops));
                } else {
                    headingTextView.setText(getString(R.string.label_farther_away));
                }
            }
        }
    }
}
