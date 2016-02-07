package com.enthusiast94.edinfit.ui.journey_planner.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models.Journey;
import com.enthusiast94.edinfit.ui.journey_planner.events.OnCountdownFinishedOrCancelledEvent;
import com.enthusiast94.edinfit.ui.journey_planner.fragments.JourneyDetailsFragment;
import com.enthusiast94.edinfit.ui.journey_planner.services.CountdownNotificationService;
import com.enthusiast94.edinfit.ui.wait_or_walk_mode.activities.SuggestionsActivity;
import com.enthusiast94.edinfit.utils.Helpers;

import de.greenrobot.event.EventBus;

/**
 * Created by manas on 30-01-2016.
 */
public class JourneyDetailsActivity extends AppCompatActivity {

    private static final String EXTRA_JOURNEY = "journey";

    private Toolbar toolbar;
    private FrameLayout actionStart;
    private FrameLayout actionStop;

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

        EventBus.getDefault().register(this);

        // setup app bar
        setSupportActionBar(toolbar);
        ActionBar appBar = getSupportActionBar();
        appBar.setDisplayHomeAsUpEnabled(true);
        appBar.setTitle(R.string.journey_details);

        final Journey journey = getIntent().getParcelableExtra(EXTRA_JOURNEY);

        if (getSupportFragmentManager().findFragmentByTag(JourneyDetailsFragment.TAG) == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, JourneyDetailsFragment.newInstance(journey),
                            JourneyDetailsFragment.TAG)
                    .commit();
        }

        // setup event handlers
        actionStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startService(CountdownNotificationService.getStartServiceIntent(
                        JourneyDetailsActivity.this, journey));

                setActionButtonEnabled(actionStart, false);
                setActionButtonEnabled(actionStop, true);
            }
        });
        actionStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent stopServiceIntent = new Intent(JourneyDetailsActivity.this,
                        CountdownNotificationService.class);
                stopService(stopServiceIntent);

                setActionButtonEnabled(actionStart, true);
                setActionButtonEnabled(actionStop, false);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (Helpers.isServiceRunning(this, CountdownNotificationService.class)) {
            setActionButtonEnabled(actionStart, false);
            setActionButtonEnabled(actionStop, true);
        } else {
            setActionButtonEnabled(actionStart, true);
            setActionButtonEnabled(actionStop, false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    private void findViews() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        actionStart = (FrameLayout) findViewById(R.id.action_start);
        actionStop = (FrameLayout) findViewById(R.id.action_stop);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setActionButtonEnabled(FrameLayout actionButton, boolean shouldEnable) {
        if (shouldEnable) {
            actionButton.setClickable(true);
            actionButton.setForeground(null);
            actionButton.setVisibility(View.VISIBLE);
        } else {
            actionButton.setClickable(false);
            actionButton.setForeground(
                    new ColorDrawable(ContextCompat.getColor(this, R.color.primary_opaque_65)));
            actionButton.setVisibility(View.GONE);
        }
    }

    public void onEventMainThread(OnCountdownFinishedOrCancelledEvent event) {
        setActionButtonEnabled(actionStart, true);
        setActionButtonEnabled(actionStop, false);
    }
}
