package com.enthusiast94.edinfit.ui.wait_or_walk_mode.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models.Route;
import com.enthusiast94.edinfit.models.Service;
import com.enthusiast94.edinfit.models.Stop;
import com.enthusiast94.edinfit.network.WaitOrWalkService;
import com.enthusiast94.edinfit.ui.wait_or_walk_mode.events.ShowWalkingDirectionsFragmentEvent;
import com.enthusiast94.edinfit.ui.wait_or_walk_mode.fragments.SuggestionsFragment;
import com.enthusiast94.edinfit.ui.wait_or_walk_mode.fragments.WalkingDirectionsFragment;
import com.enthusiast94.edinfit.ui.wait_or_walk_mode.services.CountdownNotificationService;

import java.util.ArrayList;

import de.greenrobot.event.EventBus;

/**
 * Created by manas on 04-11-2015.
 */
public class SuggestionsActivity extends AppCompatActivity {

    public static final String EXTRA_SELECTED_ORIGIN_STOP = "selectedOriginStop";
    public static final String EXTRA_SELECTED_SERVICE = "selectedService";
    public static final String EXTRA_SELECTED_DESTINATION_STOP = "selectedDestinationStop";
    public static final String EXTRA_SELECTED_ROUTE = "selectedRoute";
    public static final String EXTRA_WAIT_OR_WALK_SELECTED_SUGGESTION = "waitOrWalkSelectedSuggestion";
    public static final String EXTRA_WAIT_OR_WALK_ALL_SUGGESTIONS = "waitOrWalkAllSuggestion";

    private ViewPager viewPager;
    private View actionStop;

    private Stop selectedOriginStop;
    private Service selectedService;
    private Stop selectedDestinationStop;
    private Route selectedRoute;
    private WaitOrWalkService.WaitOrWalkSuggestion waitOrWalkSelectedSuggestion;
    private ArrayList<WaitOrWalkService.WaitOrWalkSuggestion> waitOrWalkSuggestions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wait_or_walk_suggestions);

        /**
         * Get intent extras.
         *
         * WaitOrWalk suggestions would only be passed in if the directions action on a countdown
         * notification is clicked (or the notification itself is clicked), while user selections
         * for service, stop and route will be passed in if a new wait or walk activity is started.
         */

        Bundle bundle = getIntent().getExtras();
        waitOrWalkSelectedSuggestion = bundle.getParcelable(EXTRA_WAIT_OR_WALK_SELECTED_SUGGESTION);
        waitOrWalkSuggestions = bundle.getParcelableArrayList(EXTRA_WAIT_OR_WALK_ALL_SUGGESTIONS);
        selectedOriginStop = bundle.getParcelable(EXTRA_SELECTED_ORIGIN_STOP);
        selectedService = bundle.getParcelable(EXTRA_SELECTED_SERVICE);
        selectedDestinationStop = bundle.getParcelable(EXTRA_SELECTED_DESTINATION_STOP);
        selectedRoute = bundle.getParcelable(EXTRA_SELECTED_ROUTE);

        /**
         * Find views
         */

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        actionStop = toolbar.findViewById(R.id.action_stop);
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tablayout);
        viewPager = (ViewPager) findViewById(R.id.viewpager);

        /**
         * Setup app bar
         */

        setSupportActionBar(toolbar);
        ActionBar appBar = getSupportActionBar();
        if (appBar != null) {
            appBar.setHomeButtonEnabled(true);
            appBar.setDisplayHomeAsUpEnabled(true);
            appBar.setTitle(R.string.title_activity_wait_or_walk_result);
        }

        /**
         * Set event listeners
         */

        actionStop.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent stopServiceIntent = new Intent(SuggestionsActivity.this,
                        CountdownNotificationService.class);
                stopService(stopServiceIntent);
                onBackPressed();
            }
        });

        /**
         * Setup viewpager to work with tabs
         */

        viewPager.setAdapter(new ResultsPagerAdapter());
        tabLayout.setupWithViewPager(viewPager);

    }

    @Override
    protected void onResume() {
        super.onResume();

        EventBus.getDefault().register(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        EventBus.getDefault().unregister(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return false;
        }
    }

    private class ResultsPagerAdapter extends FragmentPagerAdapter {

        public ResultsPagerAdapter() {
            super(getSupportFragmentManager());
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    if (waitOrWalkSuggestions != null && waitOrWalkSelectedSuggestion != null) {
                        return SuggestionsFragment.newInstance(waitOrWalkSuggestions,
                                waitOrWalkSelectedSuggestion);
                    }
                    return SuggestionsFragment.newInstance(selectedOriginStop, selectedService,
                            selectedDestinationStop, selectedRoute);
                case 1:
                    return WalkingDirectionsFragment.newInstance(waitOrWalkSelectedSuggestion);
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.label_suggestions);
                case 1:
                    return getString(R.string.label_directions);
                default:
                    return null;
            }
        }
    }

    public void onEventMainThread(ShowWalkingDirectionsFragmentEvent event) {
        viewPager.setCurrentItem(1);
    }
}
