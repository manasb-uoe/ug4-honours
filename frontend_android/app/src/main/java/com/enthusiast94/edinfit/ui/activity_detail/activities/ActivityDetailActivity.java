package com.enthusiast94.edinfit.ui.activity_detail.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.ui.activity_detail.events.UpdateAppBarTitlesEvent;
import com.enthusiast94.edinfit.ui.activity_detail.fragments.ActivityDetailFragment;

import de.greenrobot.event.EventBus;

/**
 * Created by manas on 07-02-2016.
 */
public class ActivityDetailActivity extends AppCompatActivity {

    private static final String EXTRA_ACTIVITY_ID = "activityId";

    private ActionBar actionBar;

    public static void start(Context context, long activityId) {
        Intent intent = new Intent(context, ActivityDetailActivity.class);
        intent.putExtra(EXTRA_ACTIVITY_ID, activityId);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activity_detail);

        EventBus.getDefault().register(this);

        // setup app bar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        long activityId = getIntent().getLongExtra(EXTRA_ACTIVITY_ID, -1);

        if (getSupportFragmentManager().findFragmentByTag(ActivityDetailFragment.TAG) == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, ActivityDetailFragment.newInstance(activityId),
                            ActivityDetailFragment.TAG)
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    public void onEventMainThread(UpdateAppBarTitlesEvent event) {
        actionBar.setTitle(event.getTitle());
        actionBar.setSubtitle(event.getSubtitle());
    }
}
