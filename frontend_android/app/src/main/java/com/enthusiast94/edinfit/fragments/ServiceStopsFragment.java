package com.enthusiast94.edinfit.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.enthusiast94.edinfit.R;

/**
 * Created by manas on 06-10-2015.
 */
public class ServiceStopsFragment extends Fragment {

    public static final String EXTRA_SERVICE_NAME = "serviceName";

    public static ServiceStopsFragment newInstance(String stopId) {
        ServiceStopsFragment instance = new ServiceStopsFragment();
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_SERVICE_NAME, stopId);
        instance.setArguments(bundle);

        return instance;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_service_stops, container, false);

        return view;
    }
}
