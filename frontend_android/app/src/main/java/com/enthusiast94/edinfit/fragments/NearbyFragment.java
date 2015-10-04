package com.enthusiast94.edinfit.fragments;

import android.content.Intent;
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
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.enthusiast94.edinfit.App;
import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.activities.StopActivity;
import com.enthusiast94.edinfit.models.Departure;
import com.enthusiast94.edinfit.models.Stop;
import com.enthusiast94.edinfit.network.Callback;
import com.enthusiast94.edinfit.network.StopService;
import com.enthusiast94.edinfit.utils.Helpers;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by manas on 01-10-2015.
 */
public class NearbyFragment extends Fragment {

    public static final String TAG = NearbyFragment.class.getSimpleName();
    private RecyclerView nearbyStopsRecyclerView;
    private NearbyStopsAdapter nearbyStopsAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView currentLocationTextView;
    private TextView lastUpdatedAtTextView;
    private String lastKnownUserLocationName;
    private Date lastUpdatedAt;
    private ImageButton refreshImageButton;
    private List<Stop> nearbyStops = new ArrayList<>();

    // nearby stops api endpoint params
    private static final int NEARBY_STOPS_LIMIT = 25;
    private static final boolean ONLY_INCLUDE_UPCOMING_DEPARTURES = true;
    private static final int MAX_DISTANCE = 3;
    private static final double NEAR_DISTANCE = 0.3;

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

        nearbyStopsRecyclerView = (RecyclerView) view.findViewById(R.id.nearby_stops_recyclerview);
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_layout);
        currentLocationTextView = (TextView) view.findViewById(R.id.current_location_textview);
        lastUpdatedAtTextView = (TextView) view.findViewById(R.id.last_updated_textview);
        refreshImageButton = (ImageButton) view.findViewById(R.id.refresh_button);

        /**
         * Setup swipe refresh layout
         */

        swipeRefreshLayout.setColorSchemeResources(R.color.accent);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {
                loadNearbyStops();
            }
        });

        /**
         * Setup nearby stops recycler view
         */

        nearbyStopsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        nearbyStopsAdapter = new NearbyStopsAdapter();
        nearbyStopsRecyclerView.setAdapter(nearbyStopsAdapter);

        /**
         * Set refresh button to reload nearby stops on click
         */

        refreshImageButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                loadNearbyStops();
            }
        });

        /**
         * Load nearby stops from network
         */

        if (nearbyStops.size() == 0) {
            loadNearbyStops();
        } else {
            nearbyStopsAdapter.notifyNearbyStopsChanged();
            updateCurrentLocationAndLastUpdated();
        }

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        nearbyStopsRecyclerView = null;
        nearbyStopsAdapter = null;
    }

    private void loadNearbyStops() {
        setRefreshIndicatorVisiblity(true);

        Location lastKnownUserLocation = App.getLastKnownUserLocation();

        if (lastKnownUserLocation != null) {
            StopService.getNearbyStops(55.94252110000001,
                    -3.2010698, MAX_DISTANCE, NEAR_DISTANCE, Helpers.getCurrentTime24h(),
                    NEARBY_STOPS_LIMIT, new Callback<List<Stop>>() {

                        @Override
                        public void onSuccess(List<Stop> data) {
                            nearbyStops = data;
                            lastUpdatedAt = new Date();

                            if (getActivity() != null) {
                                setRefreshIndicatorVisiblity(false);

                                nearbyStopsAdapter.notifyNearbyStopsChanged();

                                updateCurrentLocationAndLastUpdated();
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
        } else {
            Toast.makeText(getActivity(), getString(R.string.error_could_not_fetch_location),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void updateCurrentLocationAndLastUpdated() {
        lastKnownUserLocationName = App.getLastKnownUserLocationName();
        if (lastKnownUserLocationName != null) {
            currentLocationTextView.setText(lastKnownUserLocationName);
        } else {
            currentLocationTextView.setText(getString(R.string.label_not_found));
        }

        if (lastUpdatedAt != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.UK);
            lastUpdatedAtTextView.setText(sdf.format(lastUpdatedAt));
        } else {
            lastUpdatedAtTextView.setText(R.string.label_not_found);
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

    private class NearbyStopsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private LayoutInflater inflater;
        private final int HEADING_VIEW_TYPE = 0;
        private final int NEAREST_STOP_VIEW_TYPE = 1;
        private final int FARTHER_STOP_VIEW_TYPE = 2;
        private int nearestStopCount;

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (inflater == null) {
                inflater = LayoutInflater.from(getActivity());
            }

            if (viewType == HEADING_VIEW_TYPE) {
                return new HeadingViewHolder(inflater.inflate(R.layout.row_heading, parent, false));
            } else if (viewType == NEAREST_STOP_VIEW_TYPE) {
                return new NearestStopViewHolder(inflater.inflate(R.layout.row_nearest_stop, parent, false));
            } else {
                return new FartherStopViewHolder(inflater.inflate(R.layout.row_farther_stop, parent, false));
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            int viewType = getItemViewType(position);

            if (viewType == HEADING_VIEW_TYPE) {
                ((HeadingViewHolder) holder).bindItem((String) getItem(position));
            } else if (viewType == NEAREST_STOP_VIEW_TYPE) {
                ((NearestStopViewHolder) holder).bindItem((Stop) getItem(position));
            } else {
                ((FartherStopViewHolder) holder).bindItem((Stop) getItem(position));
            }
        }

        @Override
        public int getItemCount() {
            return nearbyStops.size();
        }

        @Override
        public int getItemViewType(int position) {
            Object item = getItem(position);

            if (item instanceof String) {
                return HEADING_VIEW_TYPE;
            } else {
                if (((Stop) item).getDistanceAway() < NEAR_DISTANCE) {
                    return NEAREST_STOP_VIEW_TYPE;
                } else {
                    return FARTHER_STOP_VIEW_TYPE;
                }
            }
        }

        private Object getItem(int position) {
            if (position == 0) {
                return getString(R.string.label_nearest_bus_stops);
            } else if (position == nearestStopCount + 1) {
                return getString(R.string.label_farther_away);
            } else {
                if (position < nearestStopCount) {
                    return nearbyStops.get(position - 1);
                } else {
                    return nearbyStops.get(position-2);
                }
            }
        }

        private void notifyNearbyStopsChanged() {
            // recalculate nearest stops count so that headings appear in the correct positions
            nearestStopCount = 0;
            for (Stop stop : nearbyStops) {
                if (stop.getDistanceAway() < NEAR_DISTANCE) {
                    nearestStopCount++;
                } else {
                    break;
                }
            }

            this.notifyDataSetChanged();
        }

        private class NearestStopViewHolder extends RecyclerView.ViewHolder
                implements View.OnClickListener{

            private Stop stop;
            private TextView stopNameTextView;
            private TextView serviceNameTextView;
            private TextView destinationTextView;
            private TextView timeTextView;

            public NearestStopViewHolder(View itemView) {
                super(itemView);

                // find views
                stopNameTextView = (TextView) itemView.findViewById(R.id.stop_name_textview);
                serviceNameTextView =
                        (TextView) itemView.findViewById(R.id.service_name_textview);
                destinationTextView =
                        (TextView) itemView.findViewById(R.id.destination_textview);
                timeTextView =
                        (TextView) itemView.findViewById(R.id.time_textview);

                // bind event listeners
                itemView.setOnClickListener(this);
            }

            public void bindItem(Stop stop) {
                this.stop = stop;

                stopNameTextView.setText(stop.getName());

                if (stop.getDepartures().size() > 0) {
                    Departure departure = stop.getDepartures().get(0);

                    serviceNameTextView.setText(departure.getServiceName());
                    destinationTextView.setText(departure.getDestination());
                    timeTextView.setText(departure.getTime());
                } else {
                    serviceNameTextView.setText(getString(R.string.label_no_upcoming_departure));
                    destinationTextView.setText("");
                    timeTextView.setText("");
                }
            }

            @Override
            public void onClick(View v) {
                if (stop != null) {
                    startStopActivity(stop);
                }
            }
        }

        private class FartherStopViewHolder extends RecyclerView.ViewHolder
                implements View.OnClickListener {

            private TextView stopNameTextView;
            private Stop stop;

            public FartherStopViewHolder(View itemView) {
                super(itemView);

                // find views
                stopNameTextView = (TextView) itemView.findViewById(R.id.stop_name_textview);

                // bind event listeners
                itemView.setOnClickListener(this);
            }

            public void bindItem(Stop stop) {
                this.stop = stop;

                stopNameTextView.setText(stop.getName());
            }

            @Override
            public void onClick(View v) {
                if (stop != null) {
                    startStopActivity(stop);
                }
            }
        }

        private class HeadingViewHolder extends RecyclerView.ViewHolder {

            private TextView headingTextView;

            public HeadingViewHolder(View itemView) {
                super(itemView);

                headingTextView = (TextView) itemView.findViewById(R.id.heading_textview);
            }

            public void bindItem(String heading) {
                headingTextView.setText(heading);
            }
        }

        private void startStopActivity(Stop stop) {
            Intent startActivityIntent = new Intent(getActivity(), StopActivity.class);
            startActivityIntent.putExtra(StopActivity.EXTRA_STOP_ID, stop.getId());
            startActivityIntent.putExtra(StopActivity.EXTRA_STOP_NAME, stop.getName());
            startActivity(startActivityIntent);
        }
    }
}
