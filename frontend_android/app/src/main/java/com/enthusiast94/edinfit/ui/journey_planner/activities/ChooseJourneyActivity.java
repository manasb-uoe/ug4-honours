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
import com.enthusiast94.edinfit.ui.journey_planner.fragments.ChooseJourneyFragment;

import java.util.ArrayList;

/**
 * Created by manas on 31-01-2016.
 */
public class ChooseJourneyActivity extends AppCompatActivity {

    private static final String EXTRA_JOURNEYS = "journeys";

    private Toolbar toolbar;

    public static Intent getStartActivityIntent(Context context, ArrayList<Journey> journeys) {
        Intent intent = new Intent(context, ChooseJourneyActivity.class);
        intent.putParcelableArrayListExtra(EXTRA_JOURNEYS, journeys);

        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_journey);
        findViews();

        // setup app bar
        setSupportActionBar(toolbar);
        ActionBar appBar = getSupportActionBar();
        appBar.setDisplayHomeAsUpEnabled(true);
        appBar.setTitle(R.string.choose_an_option);

        Intent intent = getIntent();
        ArrayList<Journey> journeys = intent.getParcelableArrayListExtra(EXTRA_JOURNEYS);

        if (getSupportFragmentManager().findFragmentByTag(ChooseJourneyFragment.TAG) == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, ChooseJourneyFragment.newInstance(journeys),
                            ChooseJourneyFragment.TAG)
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
