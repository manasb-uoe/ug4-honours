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
import com.enthusiast94.edinfit.fragments.StopDeparturesFragment;
import com.enthusiast94.edinfit.fragments.StopMapFragment;

/**
 * Created by manas on 04-10-2015.
 */
public class StopActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    public static final String EXTRA_STOP_ID = "stopId";
    public static final String EXTRA_STOP_NAME = "stopName";
    private String stopId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stop);

        /**
         * Find views
         */

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        tabLayout = (TabLayout) findViewById(R.id.tablayout);
        viewPager = (ViewPager) findViewById(R.id.viewpager);

        /**
         * Retrieve stop id from intent, so that the corresponding stop's data can be fetched
         * from the server
         */

        Intent intent = getIntent();
        stopId = intent.getStringExtra(EXTRA_STOP_ID);
        String stopName = intent.getStringExtra(EXTRA_STOP_NAME);

        /**
         * Setup app bar
         */

        setSupportActionBar(toolbar);
        ActionBar appBar = getSupportActionBar();
        if (appBar != null) {
            appBar.setHomeButtonEnabled(true);
            appBar.setDisplayHomeAsUpEnabled(true);
            appBar.setTitle(stopName);
        }

        /**
         * Setup viewpager to work with tabs.
         */

        viewPager.setAdapter(new StopPagerAdapter());
        tabLayout.setupWithViewPager(viewPager);
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

    private class StopPagerAdapter extends FragmentPagerAdapter {

        public StopPagerAdapter() {
            super(getSupportFragmentManager());
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0: return new StopDeparturesFragment();
                case 1: return new StopMapFragment();
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
                case 0: return getString(R.string.label_departures);
                case 1: return getString(R.string.label_map);
                default: return null;
            }
        }
    }
}
