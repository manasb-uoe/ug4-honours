package com.enthusiast94.edinfit.ui.wair_or_walk_mode.activities;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models.Route;
import com.enthusiast94.edinfit.models.Service;
import com.enthusiast94.edinfit.models.Stop;
import com.enthusiast94.edinfit.services.DirectionsService;
import com.enthusiast94.edinfit.services.WaitOrWalkService;
import com.enthusiast94.edinfit.ui.wair_or_walk_mode.events.OnWaitOrWalkResultComputedEvent;
import com.enthusiast94.edinfit.ui.wair_or_walk_mode.events.ShowWalkingDirectionsFragmentEvent;
import com.enthusiast94.edinfit.ui.wair_or_walk_mode.fragments.ResultFragment;
import com.enthusiast94.edinfit.ui.wair_or_walk_mode.fragments.WalkingDirectionsFragment;
import com.enthusiast94.edinfit.utils.Helpers;

import de.greenrobot.event.EventBus;

/**
 * Created by manas on 04-11-2015.
 */
public class ResultActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TabLayout tabLayout;
    private ViewPager viewPager;

    public static final String EXTRA_SELECTED_ORIGIN_STOP = "selectedOriginStop";
    public static final String EXTRA_SELECTED_SERVICE = "selectedService";
    public static final String EXTRA_SELECTED_DESTINATION_STOP = "selectedDestinationStop";
    public static final String EXTRA_SELECTED_ROUTE = "selectedRoute";
    public static final String EXTRA_WAIT_OR_WALK_RESULT = "waitOrWalkResult";

    private Stop selectedOriginStop;
    private Service selectedService;
    private Stop selectedDestinationStop;
    private Route selectedRoute;
    private WaitOrWalkService.WaitOrWalkResult waitOrWalkResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wait_or_walk_result);

        /**
         * Get intent extras.
         *
         * A WaitOrWalk result would only be passed in if a the directions action on a countdown
         * notification is clicked, while user selections for service, stop and route will be
         * passed in if a new wait or walk activity is started.
         */

        Bundle bundle = getIntent().getExtras();
        waitOrWalkResult = bundle.getParcelable(EXTRA_WAIT_OR_WALK_RESULT);
        selectedOriginStop = bundle.getParcelable(EXTRA_SELECTED_ORIGIN_STOP);
        selectedService = bundle.getParcelable(EXTRA_SELECTED_SERVICE);
        selectedDestinationStop = bundle.getParcelable(EXTRA_SELECTED_DESTINATION_STOP);
        selectedRoute = bundle.getParcelable(EXTRA_SELECTED_ROUTE);

        /**
         * Find views
         */

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        tabLayout = (TabLayout) findViewById(R.id.tablayout);
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
                    if (waitOrWalkResult != null) {
                        return ResultFragment.newInstance(waitOrWalkResult);
                    }
                    return ResultFragment.newInstance(selectedOriginStop, selectedService,
                            selectedDestinationStop, selectedRoute);
                case 1:
                    return new WalkingDirectionsFragment();
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
                    return getString(R.string.label_result);
                case 1:
                    return getString(R.string.label_loading);
                default:
                    return null;
            }
        }
    }

    public void onEventMainThread(OnWaitOrWalkResultComputedEvent event) {
        // update tab 2 title depending on wait or walk result
        TabLayout.Tab tab = tabLayout.getTabAt(1);
        if (tab != null) {
            DirectionsService.DirectionsResult directionsResult = event.getWaitOrWalkResult().getWalkingDirections();
            if (directionsResult != null) {
                if (event.getWaitOrWalkResult().getType() == WaitOrWalkService.WaitOrWalkResultType.WALK) {
                    tab.setText(String.format(getString(R.string.label_walk_duration),
                            directionsResult.getRoute().getDurationText()));
                } else {
                    tab.setText(String.format(getString(R.string.label_wait_duration),
                            Helpers.humanizeDurationInMillisToMinutes(event.getWaitOrWalkResult().getRemainingTimeMillis())));
                }
            }
        }
    }

    public void onEventMainThread(ShowWalkingDirectionsFragmentEvent event) {
        viewPager.setCurrentItem(1);
    }
}
