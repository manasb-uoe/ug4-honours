package com.enthusiast94.edinfit.activities;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.events.OnStopSelectedEvent;
import com.enthusiast94.edinfit.fragments.SelectServiceFragment;
import com.enthusiast94.edinfit.fragments.SelectStopFragment;

import de.greenrobot.event.EventBus;

/**
 * Created by manas on 18-10-2015.
 */
public class WaitOrWalkActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wait_or_walk);

        /**
         * Add stop selection fragment
         */

        if (getSupportFragmentManager().findFragmentByTag(SelectStopFragment.TAG) == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, new SelectStopFragment(), SelectStopFragment.TAG)
                    .commit();
        }
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
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    public void onEventMainThread(OnStopSelectedEvent event) {
        // add service selection fragment
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.fade_in, 0, 0, R.anim.fade_out)
                .add(R.id.fragment_container, SelectServiceFragment.getInstance(
                        event.getSelectedStop().getServices()), SelectServiceFragment.TAG)
                .addToBackStack(null)
                .commit();
    }
}
