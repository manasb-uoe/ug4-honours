package com.enthusiast94.edinfit.ui.wair_or_walk_mode.fragments;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models.Departure;
import com.enthusiast94.edinfit.models.Route;
import com.enthusiast94.edinfit.models.Service;
import com.enthusiast94.edinfit.models.Stop;
import com.enthusiast94.edinfit.services.BaseService;
import com.enthusiast94.edinfit.services.DirectionsService;
import com.enthusiast94.edinfit.services.LocationProviderService;
import com.enthusiast94.edinfit.services.StopService;
import com.enthusiast94.edinfit.ui.wair_or_walk_mode.events.OnWaitOrWalkResultComputedEvent;
import com.enthusiast94.edinfit.utils.Helpers;
import com.google.android.gms.maps.model.LatLng;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.greenrobot.event.EventBus;

/**
 * Created by manas on 01-11-2015.
 */
public class ResultFragment extends Fragment {

    public static final String TAG = ResultFragment.class.getSimpleName();

    private RecyclerView resultsRecyclerView;
    private ResultsAdapter resultsAdapter;

    private static final String EXTRA_SELECTED_ORIGIN_STOP = "selectedOriginStop";
    private static final String EXTRA_SELECTED_SERVICE = "selectedService";
    private static final String EXTRA_SELECTED_DESTINATION_STOP = "selectedDestinationStop";
    private static final String EXTRA_SELECTED_ROUTE = "selectedRoute";

    private Stop selectedOriginStop;
    private Service selectedService;
    private Stop selectedDestinationStop;
    private Route selectedRoute;
    private WaitOrWalkResult mainResult;

    public static ResultFragment newInstance(Stop selectedOriginStop, Service selectedService,
                                             Stop selectedDestinationStop, Route selectedRoute) {
        ResultFragment resultFragment = new ResultFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(EXTRA_SELECTED_ORIGIN_STOP, selectedOriginStop);
        bundle.putParcelable(EXTRA_SELECTED_SERVICE, selectedService);
        bundle.putParcelable(EXTRA_SELECTED_DESTINATION_STOP, selectedDestinationStop);
        bundle.putParcelable(EXTRA_SELECTED_ROUTE, selectedRoute);
        resultFragment.setArguments(bundle);

        return resultFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wait_or_walk_result, container, false);

        /**
         * Get values from arguments
         */

        Bundle bundle = getArguments();
        selectedOriginStop = bundle.getParcelable(EXTRA_SELECTED_ORIGIN_STOP);
        selectedService = bundle.getParcelable(EXTRA_SELECTED_SERVICE);
        selectedDestinationStop = bundle.getParcelable(EXTRA_SELECTED_DESTINATION_STOP);
        selectedRoute = bundle.getParcelable(EXTRA_SELECTED_ROUTE);

        /**
         * Find views
         */

        resultsRecyclerView = (RecyclerView) view.findViewById(R.id.results_recyclerview);

        /**
         * Setup results recycler view
         */

        resultsAdapter = new ResultsAdapter();
        resultsRecyclerView.setAdapter(resultsAdapter);
        resultsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        /**
         * Make computations to decide whether to wait or walk
         */

        // show indeterminate progress dialog before starting calculations
        final ProgressDialog progressDialog = new ProgressDialog(getActivity());
        progressDialog.setMessage(getString(R.string.label_making_complex_calculations));
        progressDialog.show();

        // find next stop index
        int nextStopIndex = -1;

        for (int i=0; i<selectedRoute.getStops().size(); i++) {
            Stop currentStop = selectedRoute.getStops().get(i);
            if (currentStop.getId().equals(selectedOriginStop.getId())) {
                nextStopIndex = i + 1;
            }
        }

        if (nextStopIndex != -1) {
            final Stop nextStopWithoutDepartures = selectedRoute.getStops().get(nextStopIndex);

            // fetch next stop with upcoming departures for current day
            StopService.getInstance().getStop(nextStopWithoutDepartures.getId(),
                    Helpers.getDayCode(Helpers.getCurrentDay()),
                    Helpers.getCurrentTime24h(), new BaseService.Callback<Stop>() {

                        @Override
                        public void onSuccess(final Stop nextStopWithDepartures) {
                            if (getActivity() != null) {
                                // find the amount of time remaining until upcoming departure of
                                // selected service
                                Departure upcomingDeparture = null;
                                long remainingTimeMillis = 0;

                                for (int i=0; i<nextStopWithDepartures.getDepartures().size(); i++) {
                                    Departure departure = nextStopWithDepartures.getDepartures().get(i);

                                    if (departure.getServiceName().equals(selectedService.getName())) {
                                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm", Locale.UK);

                                        try {
                                            Date now = simpleDateFormat.parse(Helpers.getCurrentTime24h());
                                            Date due = simpleDateFormat.parse(departure.getTime());

                                            upcomingDeparture = departure;
                                            remainingTimeMillis = due.getTime() - now.getTime();

                                            Log.d(TAG, "upcoming departure at: " + departure.getTime());
                                            break;
                                        } catch (ParseException e) {
                                            throw new RuntimeException(e);
                                        }
                                    }
                                }

                                // check if there's enough time left to walk to the next stop or not
                                if (upcomingDeparture != null) {
                                    final long finalRemainingTimeMillis = remainingTimeMillis;
                                    final Departure finalUpcomingDeparture = upcomingDeparture;

                                    LocationProviderService.getInstance().requestLastKnownLocationInfo(false, new LocationProviderService.LocationCallback() {
                                        @Override
                                        public void onLocationSuccess(LatLng latLng, String placeName) {
                                            LatLng nextStopLatLng = new LatLng(nextStopWithDepartures.getLocation().get(1), nextStopWithDepartures.getLocation().get(0));
                                            DirectionsService.getInstance().getWalkingDirections(latLng, nextStopLatLng, new BaseService.Callback<DirectionsService.DirectionsResult>() {

                                                @Override
                                                public void onSuccess(DirectionsService.DirectionsResult result) {
                                                    if (getActivity() != null) {
                                                        com.directions.route.Route resultRoute = result.getRoute();

                                                        long walkingTimeMillis = Helpers.parseDirectionsApiDurationToMillis(resultRoute.getDurationText());


                                                        Log.d(TAG, "remaining time: " + finalRemainingTimeMillis);
                                                        Log.d(TAG, "api duration: " + resultRoute.getDurationText());
                                                        Log.d(TAG, "parsed api duration: " + walkingTimeMillis);

                                                        if (finalRemainingTimeMillis > walkingTimeMillis) {
                                                            mainResult = new WaitOrWalkResult(
                                                                    WaitOrWalkResultType.WALK,
                                                                    nextStopWithoutDepartures,
                                                                    finalUpcomingDeparture,
                                                                    result
                                                            );

                                                            resultsAdapter.notifyDataSetChanged();

                                                            EventBus.getDefault().post(new OnWaitOrWalkResultComputedEvent(mainResult));

                                                            progressDialog.dismiss();

                                                            Log.d(TAG, "result: WALK");
                                                        } else {
                                                            // fetch origin stop with upcoming departures for current day
                                                            StopService.getInstance().getStop(selectedOriginStop.getId(),
                                                                    Helpers.getDayCode(Helpers.getCurrentDay()), Helpers.getCurrentTime24h(),
                                                                    new BaseService.Callback<Stop>() {

                                                                        @Override
                                                                        public void onSuccess(Stop selectedOriginStopWithDepartures) {
                                                                            if (getActivity() != null) {
                                                                                mainResult = new WaitOrWalkResult(
                                                                                        WaitOrWalkResultType.WAIT,
                                                                                        selectedOriginStop,
                                                                                        selectedOriginStopWithDepartures.getDepartures().get(0),
                                                                                        null
                                                                                );

                                                                                resultsAdapter.notifyDataSetChanged();

                                                                                EventBus.getDefault().post(new OnWaitOrWalkResultComputedEvent(mainResult));

                                                                                progressDialog.dismiss();

                                                                                Log.d(TAG, "result: WAIT");
                                                                            }
                                                                        }

                                                                        @Override
                                                                        public void onFailure(String message) {
                                                                            if (getActivity() != null) {
                                                                                Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT)
                                                                                        .show();
                                                                                progressDialog.dismiss();
                                                                            }
                                                                        }
                                                                    });
                                                        }
                                                    }
                                                }

                                                @Override
                                                public void onFailure(String message) {
                                                    if (getActivity() != null) {
                                                        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT)
                                                                .show();
                                                        progressDialog.dismiss();
                                                    }
                                                }
                                            });
                                        }

                                        @Override
                                        public void onLocationFailure(String error) {
                                            if (getActivity() != null) {
                                                Toast.makeText(getActivity(), error, Toast.LENGTH_SHORT)
                                                        .show();
                                                progressDialog.dismiss();
                                            }
                                        }
                                    });

                                } else {
                                    Toast.makeText(getActivity(), getString(R.string.label_no_upcoming_departure),
                                            Toast.LENGTH_LONG).show();
                                    progressDialog.dismiss();
                                }
                            }
                        }

                        @Override
                        public void onFailure(String message) {
                            if (getActivity() != null) {
                                progressDialog.dismiss();

                                Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT)
                                        .show();
                            }
                        }
                    });
        } else {
            progressDialog.dismiss();

            Toast.makeText(getActivity(), getString(R.string.error_unexpected), Toast.LENGTH_SHORT)
                    .show();
        }

        return view;
    }

    private class ResultsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int WALK_RESULT_ViEW_TYPE = 0;
        private static final int WAIT_RESULT_VIEW_TYPE = 1;
        private LayoutInflater inflater;

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (inflater == null) {
                inflater = LayoutInflater.from(getActivity());
            }

            if (viewType == WALK_RESULT_ViEW_TYPE) {
                return new WaitResultViewHolder(inflater.inflate(
                        R.layout.row_wait_or_walk_results_result_walk, parent, false));
            } else if (viewType == WAIT_RESULT_VIEW_TYPE) {
                return new WalkResultViewHolder(inflater.inflate(
                        R.layout.row_wait_or_walk_results_result_wait, parent, false));
            } else {
                return null;
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (position == 0) {
                if (holder instanceof WalkResultViewHolder) {
                    ((WalkResultViewHolder) holder).bindItem(mainResult);
                } else if (holder instanceof WaitResultViewHolder) {
                    ((WaitResultViewHolder) holder).bindItem(mainResult);
                }
            }
        }

        @Override
        public int getItemCount() {
            if (mainResult != null) {
                return 1;
            } else {
                return 0;
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0) {
                if (mainResult.getType() == WaitOrWalkResultType.WALK) {
                    return WALK_RESULT_ViEW_TYPE;
                } else {
                    return WAIT_RESULT_VIEW_TYPE;
                }
            }

            return -1;
        }

        private class WalkResultViewHolder extends RecyclerView.ViewHolder {

            private TextView stopNameTextview;
            private TextView departureTextView;

            public WalkResultViewHolder(View itemView) {
                super(itemView);

                // find views
                stopNameTextview = (TextView) itemView.findViewById(R.id.stop_name_textview);
                departureTextView = (TextView) itemView.findViewById(R.id.departure_textview);
            }

            public void bindItem(WaitOrWalkResult result) {
                stopNameTextview.setText(result.getStop().getName());
                departureTextView.setText(result.getUpcomingDeparture().getTime());
            }
        }

        private class WaitResultViewHolder extends RecyclerView.ViewHolder {

            private TextView stopNameTextview;
            private TextView departureTextView;

            public WaitResultViewHolder(View itemView) {
                super(itemView);

                // find views
                stopNameTextview = (TextView) itemView.findViewById(R.id.stop_name_textview);
                departureTextView = (TextView) itemView.findViewById(R.id.departure_textview);
            }

            public void bindItem(WaitOrWalkResult result) {
                stopNameTextview.setText(result.getStop().getName());
                departureTextView.setText(result.getUpcomingDeparture().getTime());
            }
        }
    }

    public enum WaitOrWalkResultType {
        WAIT, WALK
    }

    public static class WaitOrWalkResult {

        private WaitOrWalkResultType type;
        private Stop stop;
        private Departure upcomingDeparture;
        @Nullable private DirectionsService.DirectionsResult walkingDirections;

        public WaitOrWalkResult(WaitOrWalkResultType type, Stop stop, Departure upcomingDeparture,
                                @Nullable DirectionsService.DirectionsResult walkingDirections) {
            this.type = type;
            this.stop = stop;
            this.upcomingDeparture = upcomingDeparture;
            this.walkingDirections = walkingDirections;
        }

        public Departure getUpcomingDeparture() {
            return upcomingDeparture;
        }

        @Nullable
        public DirectionsService.DirectionsResult getWalkingDirections() {
            return walkingDirections;
        }

        public Stop getStop() {
            return stop;
        }

        public WaitOrWalkResultType getType() {
            return type;
        }
    }
}
