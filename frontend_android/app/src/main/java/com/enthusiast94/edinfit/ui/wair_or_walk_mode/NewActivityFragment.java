package com.enthusiast94.edinfit.ui.wair_or_walk_mode;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models.Route;
import com.enthusiast94.edinfit.models.Service;
import com.enthusiast94.edinfit.models.Stop;

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
                    // TODO
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

    public void onEventMainThread(OnOriginStopSelectedEvent event) {
        selectedOriginStop = event.getOriginStop();

        originStopNameTextView.setText(selectedOriginStop.getName());
    }

    public void onEventMainThread(OnServiceSelectedEvent event) {
        selectedService = event.getService();

        serviceNameTextView.setText(String.format(getString(R.string.label_service_name_and_description),
                selectedService.getName(), selectedService.getDescription()));
    }

    public void onEventMainThread(OnDestinationStopSelectedEvent event) {
        selectedDestinationStop = event.getDestinationStop();
        selectedRoute = event.getRoute();

        destinationStopNameTextView.setText(String.format(getString(R.string.label_destination_stop_and_route),
                selectedDestinationStop.getName(), selectedRoute.getDestination()));
    }
}
