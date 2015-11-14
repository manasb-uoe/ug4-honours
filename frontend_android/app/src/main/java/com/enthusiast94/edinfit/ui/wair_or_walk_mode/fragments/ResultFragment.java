package com.enthusiast94.edinfit.ui.wair_or_walk_mode.fragments;

import android.app.ProgressDialog;
import android.content.Intent;
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
import com.enthusiast94.edinfit.models.Directions;
import com.enthusiast94.edinfit.models.Route;
import com.enthusiast94.edinfit.models.Service;
import com.enthusiast94.edinfit.models.Stop;
import com.enthusiast94.edinfit.services.BaseService;
import com.enthusiast94.edinfit.services.DirectionsService;
import com.enthusiast94.edinfit.services.LocationProviderService;
import com.enthusiast94.edinfit.services.WaitOrWalkService;
import com.enthusiast94.edinfit.ui.wair_or_walk_mode.events.OnWaitOrWalkResultComputedEvent;
import com.enthusiast94.edinfit.ui.wair_or_walk_mode.events.ShowWalkingDirectionsFragmentEvent;
import com.enthusiast94.edinfit.ui.wair_or_walk_mode.services.CountdownNotificationService;
import com.google.android.gms.maps.model.LatLng;

import java.util.List;

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
    private static final String EXTRA_WAIT_OR_WALK_RESULT = "waitOrWalkResult";

    private Stop selectedOriginStop;
    private Service selectedService;
    private Stop selectedDestinationStop;
    private Route selectedRoute;
    private WaitOrWalkService.WaitOrWalkSuggestion mainResult;

    /**
     * Used when a new wait or walk activity is started.
     */

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

    /**
     * Used when the directions action on a countdown notification is clicked.
     */

    public static ResultFragment newInstance(WaitOrWalkService.WaitOrWalkSuggestion waitOrWalkSuggestion) {
        ResultFragment resultFragment = new ResultFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(EXTRA_WAIT_OR_WALK_RESULT, waitOrWalkSuggestion);
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
         * Get bundle arguments.
         */

        Bundle bundle = getArguments();
        mainResult = bundle.getParcelable(EXTRA_WAIT_OR_WALK_RESULT);
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
         * If WaitOrWalkSuggestion is passed in as a bundle argument then just fetch and update
         * directions from current user location to the required stop, else make computations
         * to decide whether to wait or walk.
         */

        if (mainResult != null) {
            final ProgressDialog fetchingDirectionsProgressDialog = new ProgressDialog(getActivity());
            fetchingDirectionsProgressDialog
                    .setMessage(getString(R.string.label_fetching_walking_direction));
            fetchingDirectionsProgressDialog.show();

            LocationProviderService.getInstance().requestLastKnownLocationInfo(false,
                    new LocationProviderService.LocationCallback() {

                        @Override
                        public void onLocationSuccess(LatLng latLng, String placeName) {
                            LatLng stoplatLng = new LatLng(mainResult.getStop().getLocation().get(1),
                                    mainResult.getStop().getLocation().get(0));
                            DirectionsService.getInstance().getWalkingDirections(latLng, stoplatLng,
                                    new BaseService.Callback<Directions>() {

                                        @Override
                                        public void onSuccess(Directions data) {
                                            if (getActivity() != null) {
                                                mainResult.setWalkingDirections(data);

                                                resultsAdapter.notifyDataSetChanged();

                                                EventBus.getDefault()
                                                        .post(new OnWaitOrWalkResultComputedEvent(mainResult));
                                                EventBus.getDefault()
                                                        .post(new ShowWalkingDirectionsFragmentEvent());

                                                fetchingDirectionsProgressDialog.dismiss();
                                            }
                                        }

                                        @Override
                                        public void onFailure(String message) {
                                            if (getActivity() != null) {
                                                Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT)
                                                        .show();
                                                fetchingDirectionsProgressDialog.dismiss();
                                            }
                                        }
                                    });
                        }

                        @Override
                        public void onLocationFailure(String error) {
                            if (getActivity() != null) {
                                Toast.makeText(getActivity(), error, Toast.LENGTH_SHORT)
                                        .show();
                                fetchingDirectionsProgressDialog.dismiss();
                            }
                        }
                    });
        } else {
            // show indeterminate progress dialog before starting calculations
            final ProgressDialog progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage(getString(R.string.label_making_complex_calculations));
            progressDialog.show();

            LocationProviderService.getInstance().requestLastKnownLocationInfo(false, new LocationProviderService.LocationCallback() {

                @Override
                public void onLocationSuccess(LatLng latLng, String placeName) {
                    WaitOrWalkService.getInstance().getWaitOrWalkSuggestions(
                            selectedRoute.getDestination(),
                            selectedService.getName(), selectedOriginStop.getId(),
                            selectedDestinationStop.getId(),
                            latLng,
                            new BaseService.Callback<List<WaitOrWalkService.WaitOrWalkSuggestion>>() {

                                @Override
                                public void onSuccess(List<WaitOrWalkService.WaitOrWalkSuggestion> waitOrWalkSuggestions) {
                                    if (waitOrWalkSuggestions.size() > 0) {
                                        mainResult = waitOrWalkSuggestions.get(0);

                                        resultsAdapter.notifyDataSetChanged();

                                        EventBus.getDefault()
                                                .post(new OnWaitOrWalkResultComputedEvent(mainResult));

                                        showTimeRemainingCountdownNotification(mainResult);
                                    } else {
                                        Toast.makeText(getActivity(), getString(R.string.error_unexpected),
                                                Toast.LENGTH_SHORT).show();
                                    }

                                    progressDialog.dismiss();
                                }

                                @Override
                                public void onFailure(String message) {
                                    progressDialog.dismiss();

                                    Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
                                }
                            });
                }

                @Override
                public void onLocationFailure(String error) {
                    progressDialog.dismiss();

                    Toast.makeText(getActivity(), error, Toast.LENGTH_SHORT).show();
                }
            });
        }

        return view;
    }

    private void showTimeRemainingCountdownNotification(WaitOrWalkService.WaitOrWalkSuggestion waitOrWalkSuggestion) {
        Intent startServiceIntent = new Intent(getActivity(), CountdownNotificationService.class);
        startServiceIntent.putExtra(CountdownNotificationService.EXTRA_WAIT_OR_WALK_RESULT, waitOrWalkSuggestion);
        getActivity().startService(startServiceIntent);
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
                if (mainResult.getType() == WaitOrWalkService.WaitOrWalkSuggestionType.WALK) {
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

            public void bindItem(WaitOrWalkService.WaitOrWalkSuggestion result) {
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

            public void bindItem(WaitOrWalkService.WaitOrWalkSuggestion result) {
                stopNameTextview.setText(result.getStop().getName());
                departureTextView.setText(result.getUpcomingDeparture().getTime());
            }
        }
    }
}
