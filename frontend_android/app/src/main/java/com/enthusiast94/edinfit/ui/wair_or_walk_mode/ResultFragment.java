package com.enthusiast94.edinfit.ui.wair_or_walk_mode;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.enthusiast94.edinfit.utils.Helpers;
import com.google.android.gms.maps.model.LatLng;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by manas on 01-11-2015.
 */
public class ResultFragment extends Fragment {

    public static final String TAG = ResultFragment.class.getSimpleName();

    private static final String EXTRA_SELECTED_ORIGIN_STOP = "selectedOriginStop";
    private static final String EXTRA_SELECTED_SERVICE = "selectedService";
    private static final String EXTRA_SELECTED_DESTINATION_STOP = "selectedDestinationStop";
    private static final String EXTRA_SELECTED_ROUTE = "selectedRoute";

    private Stop selectedOriginStop;
    private Service selectedService;
    private Stop selectedDestinationStop;
    private Route selectedRoute;

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
            Stop nextStopWithoutDepartures = selectedRoute.getStops().get(nextStopIndex);

            // fetch next stop with upcoming departures for current day
            StopService.getInstance().getStop(nextStopWithoutDepartures.getId(),
                    Helpers.getDayCode(Helpers.getCurrentDay()),
                    Helpers.getCurrentTime24h(), new BaseService.Callback<Stop>() {

                        @Override
                        public void onSuccess(final Stop nextStopWithDepartures) {
                            if (getActivity() != null) {
                                // find the amount of time remaining until upcoming departure
                                long remainingTimeMillis = -1;
                                for (Departure departure : nextStopWithDepartures.getDepartures()) {
                                    if (departure.getServiceName().equals(selectedService.getName())) {
                                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm", Locale.UK);

                                        try {
                                            Date now = simpleDateFormat.parse(Helpers.getCurrentTime24h());
                                            Date due = simpleDateFormat.parse(departure.getTime());

                                            if (due.after(now)) {
                                                remainingTimeMillis = due.getTime() - now.getTime();
                                                break;
                                            }
                                        } catch (ParseException e) {
                                            throw new RuntimeException(e);
                                        }
                                    }
                                }

                                // check if there's enough time left to walk to the next stop or not
                                if (remainingTimeMillis != -1) {
                                    LocationProviderService.getInstance().requestLastKnownLocationInfo(new LocationProviderService.LocationCallback() {
                                        @Override
                                        public void onLocationSuccess(LatLng latLng, String placeName) {
                                            LatLng nextStopLatLng = new LatLng(nextStopWithDepartures.getLocation().get(1), nextStopWithDepartures.getLocation().get(0));
                                            DirectionsService.getInstance().getWalkingDirections(latLng, nextStopLatLng, new BaseService.Callback<DirectionsService.DirectionsResult>() {

                                                @Override
                                                public void onSuccess(DirectionsService.DirectionsResult result) {
//                                                                    Toast.makeText(getActivity(),
//                                                                            TimeUnit.MINUTES.convert(remainingTimeMillis, TimeUnit.MILLISECONDS) + " minutes remaining",
//                                                                            Toast.LENGTH_LONG).show();
                                                    Toast.makeText(getActivity(), result.getRoute().getDistanceText() + " " + result.getRoute().getDurationText(), Toast.LENGTH_LONG)
                                                            .show();
                                                    progressDialog.dismiss();
                                                }

                                                @Override
                                                public void onFailure(String message) {
                                                    Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT)
                                                            .show();
                                                    progressDialog.dismiss();
                                                }
                                            });
                                        }

                                        @Override
                                        public void onLocationFailure(String error) {
                                            Toast.makeText(getActivity(), error, Toast.LENGTH_SHORT)
                                                    .show();
                                            progressDialog.dismiss();
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
}
