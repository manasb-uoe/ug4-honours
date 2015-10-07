package com.enthusiast94.edinfit.activities;

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

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.events.StartStopActivityEvent;
import com.enthusiast94.edinfit.fragments.ServiceRouteMapFragment;
import com.enthusiast94.edinfit.fragments.ServiceRouteStopsFragment;
import com.enthusiast94.edinfit.models.Stop;

import de.greenrobot.event.EventBus;

/**
 * Created by manas on 06-10-2015.
 */
public class ServiceActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    public static final String EXTRA_SERVICE_NAME = "serviceName";
    private String serviceName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service);

        /**
         * Find views
         */

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        tabLayout = (TabLayout) findViewById(R.id.tablayout);
        viewPager = (ViewPager) findViewById(R.id.viewpager);

        /**
         * Retrieve service name from intent, so that the corresponding service's data can be
         * fetched from the server
         */

        Intent intent = getIntent();
        serviceName = intent.getStringExtra(EXTRA_SERVICE_NAME);

        /**
         * Setup app bar
         */

        setSupportActionBar(toolbar);
        ActionBar appBar = getSupportActionBar();
        if (appBar != null) {
            appBar.setHomeButtonEnabled(true);
            appBar.setDisplayHomeAsUpEnabled(true);
            appBar.setTitle(serviceName);
        }

        /**
         * Setup viewpager to work with tabs.
         */

        viewPager.setAdapter(new ServicePagerAdapter());
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
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onEventMainThread(StartStopActivityEvent event) {
        Stop stop = event.getStop();

        Intent startActivityIntent = new Intent(this, StopActivity.class);
        startActivityIntent.putExtra(StopActivity.EXTRA_STOP_ID, stop.getId());
        startActivityIntent.putExtra(StopActivity.EXTRA_STOP_NAME, stop.getName());
        startActivity(startActivityIntent);
    }

    private class ServicePagerAdapter extends FragmentPagerAdapter {

        public ServicePagerAdapter() {
            super(getSupportFragmentManager());
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0: return ServiceRouteStopsFragment.newInstance(serviceName);
                case 1: return new ServiceRouteMapFragment();
                default: return null;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0: return getString(R.string.label_stops);
                case 1: return getString(R.string.label_map);
                default: return null;
            }
        }
    }
}
