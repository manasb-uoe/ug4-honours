package com.enthusiast94.edinfit.ui.service_info.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.ui.service_info.fragments.ServiceFragment;
import com.enthusiast94.edinfit.ui.service_info.fragments.ServiceTimetableFragment;

/**
 * Created by manas on 12-01-2016.
 */
public class ServiceTimetableActivity extends AppCompatActivity {

    private static final String EXTRA_SERVICE_NAME = "serviceName";
    private static final String EXTRA_START_STOP_ID = "startStopId";
    private static final String EXTRA_FINISH_STOP_ID = "finishStopId";
    private static final String EXTRA_TIME = "time";

    public static Intent getStartActivityIntent(Context context, String startStopId,
                                                String finishStopId, String serviceName,
                                                String time) {
        Intent startActivityIntent = new Intent(context, ServiceTimetableActivity.class);
        startActivityIntent.putExtra(EXTRA_START_STOP_ID, startStopId);
        startActivityIntent.putExtra(EXTRA_FINISH_STOP_ID, finishStopId);
        startActivityIntent.putExtra(EXTRA_SERVICE_NAME, serviceName);
        startActivityIntent.putExtra(EXTRA_TIME, time);

        return startActivityIntent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_timetable);

        // find views
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        // retrieve intent data required to load stop-to-stop timetable
        Intent intent = getIntent();
        String startStopId = intent.getStringExtra(EXTRA_START_STOP_ID);
        String finishStopId = intent.getStringExtra(EXTRA_FINISH_STOP_ID);
        String serviceName = intent.getStringExtra(EXTRA_SERVICE_NAME);
        String time = intent.getStringExtra(EXTRA_TIME);

        // setup app bar
        setSupportActionBar(toolbar);
        ActionBar appBar = getSupportActionBar();
        if (appBar != null) {
            appBar.setHomeButtonEnabled(true);
            appBar.setDisplayHomeAsUpEnabled(true);
            appBar.setTitle(String.format(getString(R.string.service_format), serviceName));
        }

        // add ServiceTimetableFragment to fragment container
        if (getSupportFragmentManager().findFragmentByTag(ServiceFragment.TAG) == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container_framelayout,
                            ServiceTimetableFragment.newInstance(startStopId,
                                    finishStopId, serviceName, time), ServiceTimetableFragment.TAG)
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
