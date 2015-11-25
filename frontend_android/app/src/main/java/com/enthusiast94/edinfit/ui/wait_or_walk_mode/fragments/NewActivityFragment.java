package com.enthusiast94.edinfit.ui.wait_or_walk_mode.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models.Route;
import com.enthusiast94.edinfit.models.Service;
import com.enthusiast94.edinfit.models.Stop;
import com.enthusiast94.edinfit.ui.wait_or_walk_mode.events.OnDestinationStopSelectedEvent;
import com.enthusiast94.edinfit.ui.wait_or_walk_mode.events.OnOriginStopSelectedEvent;
import com.enthusiast94.edinfit.ui.wait_or_walk_mode.events.OnServiceSelectedEvent;
import com.enthusiast94.edinfit.ui.wait_or_walk_mode.activities.SuggestionsActivity;
import com.enthusiast94.edinfit.ui.wait_or_walk_mode.events.ShowDestinationStopSelectionEvent;
import com.enthusiast94.edinfit.ui.wait_or_walk_mode.events.ShowOriginStopSelectionFragmentEvent;
import com.enthusiast94.edinfit.ui.wait_or_walk_mode.events.ShowServiceSelectionFragmentEvent;

import de.greenrobot.event.EventBus;

/**
 * Created by manas on 25-10-2015.
 */
public class NewActivityFragment extends Fragment {

    public static final String TAG = NewActivityFragment.class.getSimpleName();

    private Stop selectedOriginStop;
    private Service selectedService;
    private Stop selectedDestinationStop;
    private Route selectedRoute;

    private TextView originStopNameTextView;
    private TextView serviceNameTextView;
    private TextView destinationStopNameTextView;
    private ImageView step2ImageVIew;
    private ImageView step3ImageView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_new_wait_or_walk_activity, container, false);

        /**
         * Find views
         */

        Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        final View actionStart = toolbar.findViewById(R.id.action_start);
        final View selectOriginStopButton = view.findViewById(R.id.select_origin_stop_button);
        final View selectServiceButton = view.findViewById(R.id.select_service_button);
        final View selectDestinationStopButton = view.findViewById(R.id.select_destination_stop_button);
        originStopNameTextView = (TextView) view.findViewById(R.id.origin_stop_name_textview);
        serviceNameTextView = (TextView) view.findViewById(R.id.service_name_textview);
        destinationStopNameTextView = (TextView) view.findViewById(R.id.destination_stop_name_textview);
        step2ImageVIew = (ImageView) view.findViewById(R.id.step2_imageview);
        step3ImageView = (ImageView) view.findViewById(R.id.step3_imageview);

        /**
         * Setup toolbar title and back button icon
         */

        toolbar.setTitle(getString(R.string.label_new_activity));
        toolbar.setNavigationIcon(R.drawable.ic_action_navigation_arrow_back);

        /**
         * Bind event handlers
         */

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });

        View.OnClickListener onClickListener = new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                int id = v.getId();

                if (id == actionStart.getId()) {
                    if (selectedOriginStop != null && selectedService != null &&
                            selectedDestinationStop != null && selectedRoute != null) {
                        Intent startActivityIntent = new Intent(getActivity(), SuggestionsActivity.class);
                        startActivityIntent.putExtra(SuggestionsActivity.EXTRA_SELECTED_ORIGIN_STOP, selectedOriginStop);
                        startActivityIntent.putExtra(SuggestionsActivity.EXTRA_SELECTED_SERVICE, selectedService);
                        startActivityIntent.putExtra(SuggestionsActivity.EXTRA_SELECTED_DESTINATION_STOP, selectedDestinationStop);
                        startActivityIntent.putExtra(SuggestionsActivity.EXTRA_SELECTED_ROUTE, selectedRoute);
                        startActivity(startActivityIntent);
                    }
                } else if (id == selectOriginStopButton.getId()) {
                    EventBus.getDefault().post(new ShowOriginStopSelectionFragmentEvent());
                } else if (id == selectServiceButton.getId()) {
                    if (selectedOriginStop != null) {
                        EventBus.getDefault().post(new ShowServiceSelectionFragmentEvent(
                                selectedOriginStop.getServices()));
                    }
                } else if (id == selectDestinationStopButton.getId()) {
                    if (selectedOriginStop != null && selectedService != null) {
                        EventBus.getDefault().post(new ShowDestinationStopSelectionEvent(
                                selectedOriginStop.getId(), selectedService.getName()));
                    }
                }
            }
        };

        actionStart.setOnClickListener(onClickListener);
        selectOriginStopButton.setOnClickListener(onClickListener);
        selectServiceButton.setOnClickListener(onClickListener);
        selectDestinationStopButton.setOnClickListener(onClickListener);

        /**
         * Retain UI back to pre configuration change state
         */

        if (savedInstanceState != null) {
            updateUi();
        }

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


    private void updateUi() {
        if (selectedOriginStop != null) {
            originStopNameTextView.setText(selectedOriginStop.getName());

            step2ImageVIew.setImageResource(R.drawable.ic_image_looks_two);
        } else {
            step2ImageVIew.setImageResource(R.drawable.ic_image_looks_two_grey);
        }

        if (selectedService != null) {
            serviceNameTextView.setText(String.format(getString(R.string.label_service_name_and_description),
                    selectedService.getName(), selectedService.getDescription()));

            destinationStopNameTextView.setText(getString(R.string.label_click_here_to_select));
            step3ImageView.setImageResource(R.drawable.ic_image_looks_3);
        } else {
            serviceNameTextView.setText(getString(R.string.label_click_here_to_select));

            destinationStopNameTextView.setText(getString(R.string.label_click_here_to_select));
            step3ImageView.setImageResource(R.drawable.ic_image_looks_3_grey);
        }

        if (selectedDestinationStop != null && selectedRoute != null) {
            destinationStopNameTextView.setText(String.format(getString(R.string.label_destination_stop_and_route),
                    selectedDestinationStop.getName(), selectedRoute.getDestination()));
        }
    }

    public void onEventMainThread(OnOriginStopSelectedEvent event) {
        selectedOriginStop = event.getOriginStop();
        selectedService = null;
        selectedDestinationStop = null;
        selectedRoute = null;

        updateUi();
    }

    public void onEventMainThread(OnServiceSelectedEvent event) {
        selectedService = event.getService();
        selectedDestinationStop = null;
        selectedRoute = null;

        updateUi();
    }

    public void onEventMainThread(OnDestinationStopSelectedEvent event) {
        selectedDestinationStop = event.getDestinationStop();
        selectedRoute = event.getRoute();

        updateUi();
    }
}
