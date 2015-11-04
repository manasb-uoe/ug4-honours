package com.enthusiast94.edinfit.ui.home.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.ui.wair_or_walk_mode.activities.NewActivityActivity;

/**
 * Created by manas on 01-10-2015.
 */
public class GoFragment extends Fragment implements View.OnClickListener {

    private View waitOrWalkView;
    private View getToSomewhereView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_go, container, false);

        /**
         * Find views
         */

        waitOrWalkView = view.findViewById(R.id.wait_or_walk_view);
        getToSomewhereView = view.findViewById(R.id.get_to_somewhere_view);

        /**
         * Bind event listeners
         */

        waitOrWalkView.setOnClickListener(this);
        getToSomewhereView.setOnClickListener(this);

        return view;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.wait_or_walk_view:
                Intent goToActivityIntent = new Intent(getActivity(), NewActivityActivity.class);
                startActivity(goToActivityIntent);
                break;
            case R.id.get_to_somewhere_view:
                break;
        }
    }
}
