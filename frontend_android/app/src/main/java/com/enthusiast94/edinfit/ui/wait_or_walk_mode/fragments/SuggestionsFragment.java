package com.enthusiast94.edinfit.ui.wait_or_walk_mode.fragments;

import android.app.ProgressDialog;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models.Route;
import com.enthusiast94.edinfit.models.Service;
import com.enthusiast94.edinfit.models.Stop;
import com.enthusiast94.edinfit.network.BaseService;
import com.enthusiast94.edinfit.network.WaitOrWalkService;
import com.enthusiast94.edinfit.ui.wait_or_walk_mode.events.OnCountdownTickEvent;
import com.enthusiast94.edinfit.ui.wait_or_walk_mode.events.OnWaitOrWalkSuggestionSelected;
import com.enthusiast94.edinfit.ui.wait_or_walk_mode.events.ShowWalkingDirectionsFragmentEvent;
import com.enthusiast94.edinfit.ui.wait_or_walk_mode.services.CountdownNotificationService;
import com.enthusiast94.edinfit.utils.LocationProvider;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;

/**
 * Created by manas on 01-11-2015.
 */
public class SuggestionsFragment extends Fragment
        implements LocationProvider.LastKnowLocationCallback {

    public static final String TAG = SuggestionsFragment.class.getSimpleName();

    private RecyclerView resultsRecyclerView;
    private ResultsAdapter resultsAdapter;

    private static final String EXTRA_SELECTED_ORIGIN_STOP = "selectedOriginStop";
    private static final String EXTRA_SELECTED_SERVICE = "selectedService";
    private static final String EXTRA_SELECTED_DESTINATION_STOP = "selectedDestinationStop";
    private static final String EXTRA_SELECTED_ROUTE = "selectedRoute";
    public static final String EXTRA_WAIT_OR_WALK_SELECTED_SUGGESTION = "waitOrWalkSelectedSuggestion";
    public static final String EXTRA_WAIT_OR_WALK_ALL_SUGGESTIONS = "waitOrWalkAllSuggestion";

    private Stop selectedOriginStop;
    private Service selectedService;
    private Stop selectedDestinationStop;
    private Route selectedRoute;
    private WaitOrWalkService.WaitOrWalkSuggestion waitOrWalkSelectedSuggestion;
    private List<WaitOrWalkService.WaitOrWalkSuggestion> waitOrWalkSuggestions;
    private LocationProvider locationProvider;

    /**
     * Used when a new wait or walk activity is started.
     */

    public static SuggestionsFragment newInstance(Stop selectedOriginStop, Service selectedService,
                                             Stop selectedDestinationStop, Route selectedRoute) {
        SuggestionsFragment suggestionsFragment = new SuggestionsFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(EXTRA_SELECTED_ORIGIN_STOP, selectedOriginStop);
        bundle.putParcelable(EXTRA_SELECTED_SERVICE, selectedService);
        bundle.putParcelable(EXTRA_SELECTED_DESTINATION_STOP, selectedDestinationStop);
        bundle.putParcelable(EXTRA_SELECTED_ROUTE, selectedRoute);
        suggestionsFragment.setArguments(bundle);

        return suggestionsFragment;
    }

    /**
     * Used when the directions action on a countdown notification is clicked (or the notification
     * itself is clicked).
     */

    public static SuggestionsFragment newInstance(ArrayList<WaitOrWalkService.WaitOrWalkSuggestion> waitOrWalkSuggestions,
                                             WaitOrWalkService.WaitOrWalkSuggestion waitOrWalkSelectedSuggestion) {

        SuggestionsFragment suggestionsFragment = new SuggestionsFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(EXTRA_WAIT_OR_WALK_ALL_SUGGESTIONS, waitOrWalkSuggestions);
        bundle.putParcelable(EXTRA_WAIT_OR_WALK_SELECTED_SUGGESTION, waitOrWalkSelectedSuggestion);
        suggestionsFragment.setArguments(bundle);

        return suggestionsFragment;
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
        waitOrWalkSuggestions = bundle.getParcelableArrayList(EXTRA_WAIT_OR_WALK_ALL_SUGGESTIONS);
        waitOrWalkSelectedSuggestion = bundle.getParcelable(EXTRA_WAIT_OR_WALK_SELECTED_SUGGESTION);
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
         * Setup location provider
         */

        locationProvider = new LocationProvider(getActivity(), this);
        locationProvider.connect();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDestroy() {
        locationProvider.disconnect();

        super.onDestroy();
    }

    /**
     * Updates remaining time countdown in the first item of the recycler view
     */
    public void onEventMainThread(OnCountdownTickEvent event) {
        updateRemainingTimeForSelectedSuggestion(event.getHumanizedRemainingTime());
    }

    private void updateRemainingTimeForSelectedSuggestion(String time) {
        RecyclerView.ViewHolder viewHolder =
                resultsRecyclerView.findViewHolderForAdapterPosition(0);

        if (viewHolder instanceof ResultsAdapter.WalkSuggestionSelectedViewHolder) {
            ((ResultsAdapter.WalkSuggestionSelectedViewHolder)
                    viewHolder).updateRemainingTime(time);
        } else if (viewHolder instanceof ResultsAdapter.WaitSuggestionSelectedViewHolder) {
            ((ResultsAdapter.WaitSuggestionSelectedViewHolder)
                    viewHolder).updateRemainingTime(time);
        }
    }

    private void showTimeRemainingCountdownNotification() {
        Intent startServiceIntent = new Intent(getActivity(), CountdownNotificationService.class);
        startServiceIntent.putParcelableArrayListExtra(CountdownNotificationService.EXTRA_WAIT_OR_WALK_ALL_SUGGESTIONS,
                (ArrayList<? extends Parcelable>) waitOrWalkSuggestions);
        startServiceIntent.putExtra(CountdownNotificationService.EXTRA_WAIT_OR_WALK_SELECTED_SUGGESTION,
                waitOrWalkSelectedSuggestion);
        getActivity().startService(startServiceIntent);
    }

    @Override
    public void onLastKnownLocationSuccess(Location location) {
        if (waitOrWalkSuggestions != null && waitOrWalkSelectedSuggestion != null) {
            resultsAdapter.notifySuggestionsChanged();

            EventBus.getDefault()
                    .post(new ShowWalkingDirectionsFragmentEvent());
        } else {
            // show indeterminate progress dialog before starting calculations
            final ProgressDialog progressDialog = new ProgressDialog(getActivity());
            progressDialog.setTitle(getString(R.string.label_please_waitt));
            progressDialog.setMessage(getString(R.string.label_doing_complex_stuff));
            progressDialog.setCancelable(false);
            progressDialog.show();

            WaitOrWalkService.getInstance().getWaitOrWalkSuggestions(
                    selectedRoute.getDestination(),
                    selectedService.getName(), selectedOriginStop.getId(),
                    selectedDestinationStop.getId(),
                    new LatLng(location.getLatitude(), location.getLongitude()),
                    new BaseService.Callback<List<WaitOrWalkService.WaitOrWalkSuggestion>>() {

                        @Override
                        public void onSuccess(List<WaitOrWalkService.WaitOrWalkSuggestion> waitOrWalkSuggestions) {
                            if (waitOrWalkSuggestions.size() > 0) {
                                SuggestionsFragment.this.waitOrWalkSuggestions = waitOrWalkSuggestions;

                                resultsAdapter.notifySuggestionsChanged();
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
    }

    @Override
    public void onLastKnownLocationFailure(String error) {
        Toast.makeText(getActivity(), error, Toast.LENGTH_SHORT).show();
    }

    private class ResultsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int WALK_SUGGESTION_ViEW_TYPE_SELECTED = 0;
        private static final int WAIT_SUGGESTION_VIEW_TYPE_SELECTED = 1;
        private static final int HEADING_VIEW_TYPE = 2;
        private static final int WALK_SUGGESTION_VIEW_TYPE = 3;
        private static final int WAIT_SUGGESTION_VIEW_TYPE = 4;
        private LayoutInflater inflater;
        private int currentlySelectedItemIndex;
        private int previouslySelectedItemIndex;

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (inflater == null) {
                inflater = LayoutInflater.from(getActivity());
            }

            if (viewType == WALK_SUGGESTION_ViEW_TYPE_SELECTED) {
                return new WalkSuggestionSelectedViewHolder(inflater.inflate(
                        R.layout.row_wait_or_walk_suggestion_walk_selected, parent, false));
            } else if (viewType == WAIT_SUGGESTION_VIEW_TYPE_SELECTED) {
                return new WaitSuggestionSelectedViewHolder(inflater.inflate(
                        R.layout.row_wait_or_walk_suggestion_wait_selected, parent, false));
            } else if (viewType == HEADING_VIEW_TYPE) {
                return new HeadingViewHolder(inflater.inflate(R.layout.row_heading, parent, false));
            } else if (viewType == WALK_SUGGESTION_VIEW_TYPE) {
                return new WalkSuggestionViewHolder(inflater.inflate(
                        R.layout.row_wait_or_walk_suggestion_walk, parent, false));
            } else {
                return new WaitSuggestionViewHolder(inflater.inflate(
                        R.layout.row_wait_or_walk_suggestion_wait, parent, false));
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof WalkSuggestionSelectedViewHolder) {
                ((WalkSuggestionSelectedViewHolder) holder).bindItem(waitOrWalkSelectedSuggestion);
            } else if (holder instanceof WaitSuggestionSelectedViewHolder) {
                ((WaitSuggestionSelectedViewHolder) holder).bindItem(waitOrWalkSelectedSuggestion);
            } else if (holder instanceof WalkSuggestionViewHolder) {
                ((WalkSuggestionViewHolder) holder).bindItem(waitOrWalkSuggestions.get(position - 2));
            } else if (holder instanceof WaitSuggestionViewHolder) {
                ((WaitSuggestionViewHolder) holder).bindItem(waitOrWalkSuggestions.get(position - 2));
            }
        }

        @Override
        public int getItemCount() {
            if (waitOrWalkSuggestions != null) {
                return  1 /* Selected suggestion */ + 1 /* Heading */ + waitOrWalkSuggestions.size();
            } else {
                return 0;
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0) {
                if (waitOrWalkSelectedSuggestion.getType() == WaitOrWalkService.WaitOrWalkSuggestionType.WALK) {
                    return WALK_SUGGESTION_ViEW_TYPE_SELECTED;
                } else {
                    return WAIT_SUGGESTION_VIEW_TYPE_SELECTED;
                }
            } else if (position == 1) {
                return HEADING_VIEW_TYPE;
            } else {
                if (waitOrWalkSuggestions.get(position - 2).getType() == WaitOrWalkService.WaitOrWalkSuggestionType.WALK) {
                    return WALK_SUGGESTION_VIEW_TYPE;
                } else {
                    return WAIT_SUGGESTION_VIEW_TYPE;
                }
            }
        }

        private void notifySuggestionsChanged() {
            if (waitOrWalkSelectedSuggestion != null) {
                // find index of selected suggestion
                // (can't use indexOf since object references changed after deserialization)
                for (int i=0; i<waitOrWalkSuggestions.size(); i++) {
                    WaitOrWalkService.WaitOrWalkSuggestion suggestion = waitOrWalkSuggestions.get(i);
                    if (suggestion.getStop().getId().equals(waitOrWalkSelectedSuggestion.getStop().getId())) {
                        selectSuggestion(i);
                        break;
                    }
                }
            } else {
                selectSuggestion(0);
            }

            notifyDataSetChanged();
        }

        private class WalkSuggestionSelectedViewHolder extends RecyclerView.ViewHolder {

            private TextView stopNameTextView;
            private TextView departureTextView;
            private TextView timeRemainingTextView;
            private TextView walkDurationTextView;
            private TextView distanceTextView;

            public WalkSuggestionSelectedViewHolder(View itemView) {
                super(itemView);

                // find views
                stopNameTextView = (TextView) itemView.findViewById(R.id.stop_name_textview);
                departureTextView = (TextView) itemView.findViewById(R.id.departure_textview);
                timeRemainingTextView = (TextView) itemView.findViewById(R.id.time_remaining_textview);
                walkDurationTextView = (TextView) itemView.findViewById(R.id.walk_duration_textview);
                distanceTextView = (TextView) itemView.findViewById(R.id.distance_textview);
            }

            public void bindItem(WaitOrWalkService.WaitOrWalkSuggestion suggestion) {
                stopNameTextView.setText(String.format(getString(R.string.label_stop_name_with_direction),
                        suggestion.getStop().getName(), suggestion.getStop().getDirection()));
                departureTextView.setText(suggestion.getUpcomingDeparture().getTime());
                walkDurationTextView.setText(suggestion.getWalkingDirections().getDurationText());
                distanceTextView.setText(suggestion.getWalkingDirections().getDistanceText());
            }

            /**
             * Called whenever an OnCountdownEvent is triggered. It simply updates the
             * remainingTimeTextView using the provided humanized remaining time.
             */
            public void updateRemainingTime(String humanizedRemainingTime) {
                timeRemainingTextView.setText(humanizedRemainingTime);
            }
        }

        private class WaitSuggestionSelectedViewHolder extends RecyclerView.ViewHolder {

            private TextView stopNameTextView;
            private TextView departureTextView;
            private TextView timeRemainingTextView;
            private TextView walkDurationTextView;
            private TextView distanceTextView;

            public WaitSuggestionSelectedViewHolder(View itemView) {
                super(itemView);

                // find views
                stopNameTextView = (TextView) itemView.findViewById(R.id.stop_name_textview);
                departureTextView = (TextView) itemView.findViewById(R.id.departure_textview);
                timeRemainingTextView = (TextView) itemView.findViewById(R.id.time_remaining_textview);
                walkDurationTextView = (TextView) itemView.findViewById(R.id.walk_duration_textview);
                distanceTextView = (TextView) itemView.findViewById(R.id.distance_textview);
            }

            public void bindItem(WaitOrWalkService.WaitOrWalkSuggestion suggestion) {
                stopNameTextView.setText(String.format(getString(R.string.label_stop_name_with_direction),
                        suggestion.getStop().getName(), suggestion.getStop().getDirection()));
                departureTextView.setText(suggestion.getUpcomingDeparture().getTime());
                walkDurationTextView.setText(suggestion.getWalkingDirections().getDurationText());
                distanceTextView.setText(suggestion.getWalkingDirections().getDistanceText());
            }

            /**
             * Called whenever an OnCountdownEvent is triggered. It simply updates the
             * remainingTimeTextView using the provided humanized remaining time.
             */
            public void updateRemainingTime(String humanizedRemainingTime) {
                timeRemainingTextView.setText(humanizedRemainingTime);
            }
        }

        private class HeadingViewHolder extends RecyclerView.ViewHolder {

            private TextView headingTextView;

            public HeadingViewHolder(View itemView) {
                super(itemView);

                // find views
                headingTextView = (TextView) itemView.findViewById(R.id.heading_textview);

                // set heading
                headingTextView.setText(getString(R.string.label_suggestions));
            }
        }

        private class WaitSuggestionViewHolder extends RecyclerView.ViewHolder {

            private TextView stopNameTextView;
            private TextView departureTextView;

            public WaitSuggestionViewHolder(View itemView) {
                super(itemView);

                // find views
                stopNameTextView = (TextView) itemView.findViewById(R.id.stop_name_textview);
                departureTextView = (TextView) itemView.findViewById(R.id.departure_textview);

                // bind event listeners
                itemView.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        selectSuggestion(getAdapterPosition() - 2);
                    }
                });
            }

            public void bindItem(WaitOrWalkService.WaitOrWalkSuggestion suggestion) {
                stopNameTextView.setText(String.format(getString(R.string.label_stop_name_with_direction),
                        suggestion.getStop().getName(), suggestion.getStop().getDirection()));
                departureTextView.setText(suggestion.getUpcomingDeparture().getTime());

                if (getAdapterPosition() == currentlySelectedItemIndex) {
                    itemView.setBackgroundResource(R.color.green_selection);
                } else {
                    itemView.setBackgroundResource(android.R.color.transparent);
                }
            }
        }

        private class WalkSuggestionViewHolder extends RecyclerView.ViewHolder {

            private TextView stopNameTextView;
            private TextView departureTextView;
            private TextView numStopsSkippedTextView;

            public WalkSuggestionViewHolder(View itemView) {
                super(itemView);

                // find views
                stopNameTextView = (TextView) itemView.findViewById(R.id.stop_name_textview);
                departureTextView = (TextView) itemView.findViewById(R.id.departure_textview);
                numStopsSkippedTextView = (TextView) itemView.findViewById(R.id.num_stops_skipped_textview);

                // bind event listeners
                itemView.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        selectSuggestion(getAdapterPosition() - 2);
                    }
                });
            }

            public void bindItem(WaitOrWalkService.WaitOrWalkSuggestion suggestion) {
                stopNameTextView.setText(String.format(getString(R.string.label_stop_name_with_direction),
                        suggestion.getStop().getName(), suggestion.getStop().getDirection()));
                departureTextView.setText(suggestion.getUpcomingDeparture().getTime());
                numStopsSkippedTextView.setText(String.valueOf(waitOrWalkSuggestions.indexOf(suggestion) + 1));

                if (getAdapterPosition() == currentlySelectedItemIndex) {
                    itemView.setBackgroundResource(R.color.green_selection);
                } else {
                    itemView.setBackgroundResource(android.R.color.transparent);
                }
            }
        }

        private void selectSuggestion(int suggestionIndex) {
            waitOrWalkSelectedSuggestion = waitOrWalkSuggestions.get(suggestionIndex);

            EventBus.getDefault()
                    .post(new OnWaitOrWalkSuggestionSelected(waitOrWalkSelectedSuggestion));

            showTimeRemainingCountdownNotification();

            notifyItemChanged(0);

            currentlySelectedItemIndex = suggestionIndex + 2;

            notifyItemChanged(currentlySelectedItemIndex);
            notifyItemChanged(previouslySelectedItemIndex);

            previouslySelectedItemIndex = currentlySelectedItemIndex;
        }
    }
}
