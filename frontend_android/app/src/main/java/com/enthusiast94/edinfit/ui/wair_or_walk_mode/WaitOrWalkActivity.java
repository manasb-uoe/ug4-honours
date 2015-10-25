package com.enthusiast94.edinfit.ui.wair_or_walk_mode;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.enthusiast94.edinfit.R;

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
         * Add new activity fragment
         */

        if (getSupportFragmentManager().findFragmentByTag(SelectOriginStopFragment.TAG) == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, new NewActivityFragment(), NewActivityFragment.TAG)
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

    public void onEventMainThread(ShowOriginStopSelectionFragmentEvent event) {
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.fade_in, 0, 0, R.anim.fade_out)
                .add(R.id.fragment_container, new SelectOriginStopFragment(),
                        SelectOriginStopFragment.TAG)
                .addToBackStack(null)
                .commit();
    }

    public void onEventMainThread(ShowServiceSelectionFragmentEvent event) {
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.fade_in, 0, 0, R.anim.fade_out)
                .add(R.id.fragment_container, SelectServiceFragment.getInstance(
                        event.getServiceNames()), SelectServiceFragment.TAG)
                .addToBackStack(null)
                .commit();
    }

    public void onEventMainThread(ShowDestinationStopSelectionEvent event) {
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.fade_in, 0, 0, R.anim.fade_out)
                .add(R.id.fragment_container, SelectDestinationStopFragment.newInstance(
                        event.getOriginStopId(), event.getServiceName()),
                        SelectDestinationStopFragment.TAG)
                .addToBackStack(null)
                .commit();
    }
}
