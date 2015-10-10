package com.enthusiast94.edinfit.activities;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.events.OnDeauthenticatedEvent;
import com.enthusiast94.edinfit.fragments.UserProfileFragment;
import com.enthusiast94.edinfit.services.UserService;

import de.greenrobot.event.EventBus;

/**
 * Created by manas on 03-10-2015.
 */
public class UserProfileActivity extends AppCompatActivity {

    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_user_profile);

        /**
         * FInd views
         */

        toolbar = (Toolbar) findViewById(R.id.toolbar);

        /**
         * Setup AppBar
         */

        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        /**
         * Add user profile fragment if it hasn't already been added
         */

        if (getSupportFragmentManager().findFragmentByTag(UserProfileFragment.TAG) == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, new UserProfileFragment(), UserProfileFragment.TAG)
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_user_profile, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;

            case R.id.action_logout:
                UserService.deauthenticate();
                finish();
                EventBus.getDefault().post(new OnDeauthenticatedEvent());
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
