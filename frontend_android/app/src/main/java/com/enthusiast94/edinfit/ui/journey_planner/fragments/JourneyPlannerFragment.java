package com.enthusiast94.edinfit.ui.journey_planner.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.enthusiast94.edinfit.R;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;

/**
 * Created by manas on 27-01-2016.
 */
public class JourneyPlannerFragment extends Fragment {

    public static final String TAG = JourneyPlannerFragment.class.getSimpleName();
    private static final int ORIGIN_PICKER_REQUEST = 1;
    private static final int DESTINATION_PICKER_REQUEST = 2;

    private TextView originTextView;
    private TextView destinationTextView;
    private TextView timeTextView;
    private TextView optionsTextView;
    private ImageButton swapButton;
    private Place originPlace;
    private Place destinationPlace;

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
                switch (v.getId()) {
                    case R.id.origin_textview:
                        showPlacePicker(ORIGIN_PICKER_REQUEST);
                        break;
                    case R.id.destination_textview:
                        showPlacePicker(DESTINATION_PICKER_REQUEST);
                        break;
                    case R.id.swap_button:
                        // swap places and update UI
                        Place temp;
                        temp = originPlace;
                        originPlace = destinationPlace;
                        destinationPlace = temp;
                        updatePlacesUi();
                        break;
                }
            }
        };
        originTextView.setOnClickListener(clickListener);
        destinationTextView.setOnClickListener(clickListener);
        timeTextView.setOnClickListener(clickListener);
        optionsTextView.setOnClickListener(clickListener);
        swapButton.setOnClickListener(clickListener);

        return view;
    }

    private void findViews(View view) {
        originTextView = (TextView) view.findViewById(R.id.origin_textview);
        destinationTextView = (TextView) view.findViewById(R.id.destination_textview);
        timeTextView = (TextView) view.findViewById(R.id.time_textview);
        optionsTextView = (TextView) view.findViewById(R.id.options_textview);
        swapButton = (ImageButton) view.findViewById(R.id.swap_button);
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
}
