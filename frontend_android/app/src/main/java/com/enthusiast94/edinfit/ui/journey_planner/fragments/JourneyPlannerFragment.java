package com.enthusiast94.edinfit.ui.journey_planner.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.arasthel.asyncjob.AsyncJob;
import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models.Journey;
import com.enthusiast94.edinfit.network.BaseService;
import com.enthusiast94.edinfit.network.JourneyPlannerService;
import com.enthusiast94.edinfit.ui.journey_planner.activities.ChooseJourneyActivity;
import com.enthusiast94.edinfit.ui.journey_planner.enums.RouteOption;
import com.enthusiast94.edinfit.ui.journey_planner.enums.TimeMode;
import com.enthusiast94.edinfit.utils.Helpers;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by manas on 27-01-2016.
 */
public class JourneyPlannerFragment extends Fragment {

    public static final String TAG = JourneyPlannerFragment.class.getSimpleName();
    private static final int ORIGIN_PICKER_REQUEST = 1;
    private static final int DESTINATION_PICKER_REQUEST = 2;

    private TextView originTextView;
    private TextView destinationTextView;
    private TextView dateAndTimeTextView;
    private TextView optionsTextView;
    private ImageButton swapButton;
    private Button getDirectionsButton;
    private ProgressDialog progressDialog;

    private Place originPlace;      // currently selected origin
    private Place destinationPlace; // currently selected destination
    private String time;            // currently selected journey time
    private TimeMode timeMode;      // currently selected journey time mode
    private Date date;              // currently selected journey date
    private SimpleDateFormat sdfDay;
    private RouteOption routeOption;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_journey_planner, container, false);
        findViews(view);

        // bind event listeners
        View.OnClickListener clickListener = new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                int id = v.getId();

                if (id == originTextView.getId()) {
                    showPlacePicker(ORIGIN_PICKER_REQUEST);

                } else if (id == destinationTextView.getId()) {
                    showPlacePicker(DESTINATION_PICKER_REQUEST);

                } else if (id == swapButton.getId()) {
                    // swap places and update UI
                    Place temp;
                    temp = originPlace;
                    originPlace = destinationPlace;
                    destinationPlace = temp;
                    updatePlacesUi();

                } else if (id == dateAndTimeTextView.getId()) {
                    showDateAndTimePickerDialog();

                } else if (id == getDirectionsButton.getId()) {
                    if (originPlace == null) {
                        Toast.makeText(getActivity(),
                                getString(R.string.must_select_origin), Toast.LENGTH_SHORT)
                                .show();
                    } else if (destinationPlace == null) {
                        Toast.makeText(getActivity(),
                                getString(R.string.must_select_destination), Toast.LENGTH_SHORT)
                                .show();
                    } else {
                        setProgressDialogEnabled(true);

                        new AsyncJob.AsyncJobBuilder<BaseService.Response<List<Journey>>>()
                                .doInBackground(new AsyncJob.AsyncAction<BaseService.Response<List<Journey>>>() {
                                    @Override
                                    public BaseService.Response<List<Journey>> doAsync() {
                                        return JourneyPlannerService.getInstance().getJourneys(
                                                originPlace.getLatLng(),
                                                destinationPlace.getLatLng(),
                                                Helpers.getTimeFrom24hTimeAndDate(time, date).getTime() / 1000,
                                                timeMode,
                                                routeOption
                                        );
                                    }
                                })
                                .doWhenFinished(new AsyncJob.AsyncResultAction<BaseService.Response<List<Journey>>>() {
                                    @Override
                                    public void onResult(BaseService.Response<List<Journey>> response) {
                                        if (getActivity() == null) {
                                            return;
                                        }

                                        setProgressDialogEnabled(false);

                                        if (!response.isSuccessfull()) {
                                            Toast.makeText(getActivity(), response.getError()
                                                    , Toast.LENGTH_SHORT)
                                                    .show();
                                            return;
                                        }

                                        List<Journey> journeys = response.getBody();

                                        if (journeys.size() == 0) {
                                            Toast.makeText(getActivity(), getString(R.string.no_journeys_found)
                                                    , Toast.LENGTH_SHORT)
                                                    .show();
                                            return;
                                        }

                                        startActivity(ChooseJourneyActivity.getStartActivityIntent(getActivity(),
                                                (ArrayList<Journey>) response.getBody()));
                                    }
                                }).create().start();
                    }
                } else if (id == optionsTextView.getId()) {
                    showRouteOptionsDialog();
                }
            }
        };
        originTextView.setOnClickListener(clickListener);
        destinationTextView.setOnClickListener(clickListener);
        dateAndTimeTextView.setOnClickListener(clickListener);
        optionsTextView.setOnClickListener(clickListener);
        swapButton.setOnClickListener(clickListener);
        getDirectionsButton.setOnClickListener(clickListener);

        // set default journey time and date
        time = Helpers.getCurrentTime24h();
        timeMode = TimeMode.LEAVE_AFTER;
        sdfDay = new SimpleDateFormat("dd MMM EEEE", Locale.UK);
        date = new Date();
        routeOption = RouteOption.MINIMUM_WALK;
        updateDateAndTimeUi();

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    private void findViews(View view) {
        originTextView = (TextView) view.findViewById(R.id.origin_textview);
        destinationTextView = (TextView) view.findViewById(R.id.destination_textview);
        dateAndTimeTextView = (TextView) view.findViewById(R.id.date_and_time_textview);
        optionsTextView = (TextView) view.findViewById(R.id.options_textview);
        swapButton = (ImageButton) view.findViewById(R.id.swap_button);
        getDirectionsButton = (Button) view.findViewById(R.id.get_directions_button);
    }

    private void showPlacePicker(int requestCode) {
        PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();

        try {
            startActivityForResult(builder.build(getActivity()), requestCode);
        } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == ORIGIN_PICKER_REQUEST) {
                originPlace = PlacePicker.getPlace(data, getActivity());
                updatePlacesUi();
            } else if (requestCode == DESTINATION_PICKER_REQUEST) {
                destinationPlace = PlacePicker.getPlace(data, getActivity());
                updatePlacesUi();
            } else {
                Log.e(TAG, "Invalid request code: " + requestCode);
            }
        }
    }

    private void updatePlacesUi() {
        originTextView.setText(originPlace == null ? getString(R.string.choose_origin) :
                originPlace.getName());
        destinationTextView.setText(destinationPlace == null ? getString(R.string.choose_destination) :
                destinationPlace.getName());
    }

    private void updateDateAndTimeUi() {
        String timeModeText;
        switch (timeMode) {
            case ARRIVE_BY:
                timeModeText = getString(R.string.arrive_by);
                break;
            case LEAVE_AFTER:
                timeModeText = getString(R.string.leave_after);
                break;
            default:
                throw new IllegalStateException("Invalid time mode: " + timeMode.toString());
        }

        dateAndTimeTextView.setText(String.format(getString(R.string.journey_date_and_time_format),
                timeModeText, sdfDay.format(date), time));
    }

    private void showDateAndTimePickerDialog() {
        View dialogView = LayoutInflater.from(getActivity())
                .inflate(R.layout.dialog_journey_time_picker, null);

        // find views
        final RadioButton arriveByRadio = (RadioButton) dialogView.findViewById(R.id.arrive_by_radio);
        final RadioButton leaveAfterRadio = (RadioButton) dialogView.findViewById(R.id.leave_after_radio);
        final NumberPicker hourPicker = (NumberPicker) dialogView.findViewById(R.id.hour_picker);
        final NumberPicker minutePicker = (NumberPicker) dialogView.findViewById(R.id.minute_picker);
        final Spinner dateSpinner = (Spinner) dialogView.findViewById(R.id.date_spinner);

        // set initial values for radio buttons
        switch (timeMode) {
            case ARRIVE_BY:
                arriveByRadio.setChecked(true);
                break;
            case LEAVE_AFTER:
                leaveAfterRadio.setChecked(true);
                break;
            default:
                throw new IllegalStateException("Invalid time mode: " + timeMode.toString());
        }

        // setup time pickers
        hourPicker.setMinValue(0);
        hourPicker.setMaxValue(23);
        hourPicker.setValue(Integer.parseInt(time.substring(0,
                time.indexOf(":"))));

        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);
        minutePicker.setValue(Integer.parseInt(time.substring(
                time.indexOf(":") + 1, time.length())));

        // collect dates for date spinner
        final Map<String, Date> datesMap = new LinkedHashMap<>();
        Calendar calendar = Calendar.getInstance();
        Date today = new Date();
        datesMap.put(sdfDay.format(today), today);
        calendar.setTime(today);
        for (int i=0; i<5; i++) {
            calendar.add(Calendar.DATE, 1);
            datesMap.put(sdfDay.format(calendar.getTime()), calendar.getTime());
        }

        // setup date spinner
        final ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_dropdown_item, new ArrayList(datesMap.keySet()));
        dateSpinner.setAdapter(spinnerAdapter);
        String selectedDateText = sdfDay.format(date);
        for (int i=0; i<spinnerAdapter.getCount(); i++) {
            if (spinnerAdapter.getItem(i).equals(selectedDateText)) {
                dateSpinner.setSelection(i);
                break;
            }
        }

        AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.select_date_and_time))
                .setView(dialogView)
                .setPositiveButton(getString(R.string.label_set), new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        time = hourPicker.getValue() + ":" + minutePicker.getValue();
                        timeMode = leaveAfterRadio.isChecked() ? TimeMode.LEAVE_AFTER :
                                TimeMode.ARRIVE_BY;
                        date = datesMap.get(dateSpinner.getSelectedItem().toString());

                        updateDateAndTimeUi();
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(getString(R.string.label_cancel), null)
                .create();

        dialog.show();
    }

    private void showRouteOptionsDialog() {
        final HashMap<RouteOption, Integer> routeOptionsMap = new HashMap<>();
        routeOptionsMap.put(RouteOption.MINIMUM_WALK, 0);
        routeOptionsMap.put(RouteOption.SOME_WALK, 1);
        routeOptionsMap.put(RouteOption.ONLY_WALK, 2);

        final List<String> routeOptions = Arrays.asList(getString(R.string.min_walk),
                getString(R.string.some_walk), getString(R.string.only_walk));

        int checkedItemIndex = routeOptionsMap.get(routeOption);

        AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                .setSingleChoiceItems(routeOptions.toArray(new String[routeOptions.size()]),
                        checkedItemIndex, null)
                .setTitle(R.string.route_options)
                .setPositiveButton(R.string.label_save, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ListView listView = ((AlertDialog) dialog).getListView();
                        for (Map.Entry<RouteOption, Integer> entry : routeOptionsMap.entrySet()) {
                            if (entry.getValue() == listView.getCheckedItemPosition()) {
                                routeOption = entry.getKey();
                                break;
                            }
                        }
                    }
                })
                .setNegativeButton(R.string.label_cancel, null)
                .create();

        alertDialog.show();
    }

    private void setProgressDialogEnabled(boolean isEnabled) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage(getString(R.string.label_please_waitt));
        }

        if (isEnabled) {
            progressDialog.show();
        } else {
            progressDialog.hide();
        }
    }
}
