package com.enthusiast94.edinfit.ui.stop_info;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.ui.service_info.ServiceFragment;

/**
 * Created by manas on 04-10-2015.
 */
public class StopActivity extends AppCompatActivity {

    public static final String EXTRA_STOP_ID = "stopId";
    public static final String EXTRA_STOP_NAME = "stopName";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stop);

        /**
         * Find views
         */

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        /**
         * Retrieve stop id from intent, so that the corresponding stop's data can be fetched
         * from the server
         */

        Intent intent = getIntent();
        String stopId = intent.getStringExtra(EXTRA_STOP_ID);
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
         * Add stop fragment
         */

        if (getSupportFragmentManager().findFragmentByTag(ServiceFragment.TAG) == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container_framelayout, StopFragment.newInstance(stopId), ServiceFragment.TAG)
                    .commit();
        }

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
}
