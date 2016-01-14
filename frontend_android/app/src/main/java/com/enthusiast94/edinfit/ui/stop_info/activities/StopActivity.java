package com.enthusiast94.edinfit.ui.stop_info.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.ui.stop_info.fragments.StopFragment;

/**
 * Created by manas on 04-10-2015.
 */
public class StopActivity extends AppCompatActivity {

    public static final String EXTRA_STOP_ID = "stopId"; // TODO make private and use getStartActivityIntent instead

    public static Intent getStartActivityIntent(Context context, String stopId) {
        Intent startActivityIntent = new Intent(context, StopActivity.class);
        startActivityIntent.putExtra(EXTRA_STOP_ID, stopId);

        return startActivityIntent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stop);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        ActionBar appBar = getSupportActionBar();
        if (appBar != null) {
            appBar.setHomeButtonEnabled(true);
            appBar.setDisplayHomeAsUpEnabled(true);
            appBar.setTitle(getString(R.string.label_departure_times));
        }

        String stopId = getIntent().getStringExtra(EXTRA_STOP_ID);

        if (getSupportFragmentManager().findFragmentByTag(StopFragment.TAG) == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container_framelayout, StopFragment.newInstance(stopId),
                            StopFragment.TAG)
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
