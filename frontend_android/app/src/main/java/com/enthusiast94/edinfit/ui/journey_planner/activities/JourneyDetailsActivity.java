package com.enthusiast94.edinfit.ui.journey_planner.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models.Journey;
import com.enthusiast94.edinfit.ui.journey_planner.fragments.JourneyDetailsFragment;

/**
 * Created by manas on 30-01-2016.
 */
public class JourneyDetailsActivity extends AppCompatActivity {

    private static final String EXTRA_JOURNEY = "journey";

    private Toolbar toolbar;

    public static Intent getStartActivityIntent(Context context, Journey journey) {
        Intent intent = new Intent(context, JourneyDetailsActivity.class);
        intent.putExtra(EXTRA_JOURNEY, journey);

        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_journey_details);
        findViews();

        // setup app bar
        setSupportActionBar(toolbar);
        ActionBar appBar = getSupportActionBar();
        appBar.setDisplayHomeAsUpEnabled(true);
        appBar.setTitle(R.string.journey_details);

        Journey journey = getIntent().getParcelableExtra(EXTRA_JOURNEY);

        if (getSupportFragmentManager().findFragmentByTag(JourneyDetailsFragment.TAG) == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, JourneyDetailsFragment.newInstance(journey),
                            JourneyDetailsFragment.TAG)
                    .commit();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void findViews() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
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
