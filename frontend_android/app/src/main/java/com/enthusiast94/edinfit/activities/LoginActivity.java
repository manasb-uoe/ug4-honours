package com.enthusiast94.edinfit.activities;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.events.ShowSignupFragmentEvent;
import com.enthusiast94.edinfit.fragments.LoginFragment;
import com.enthusiast94.edinfit.fragments.SignupFragment;

import de.greenrobot.event.EventBus;

public class LoginActivity extends AppCompatActivity {

    private static final String LOGIN_FRAGMENT_TAG = "loginFragmentTag";
    private static final String SIGNUP_FRAGMENT_TAG = "signupFragmentTag";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if (getSupportFragmentManager().findFragmentByTag(LOGIN_FRAGMENT_TAG) == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container_framelayout, new LoginFragment(), LOGIN_FRAGMENT_TAG)
                    .commit();
        }
    }

    public void onEventMainThread(ShowSignupFragmentEvent event) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container_framelayout, new SignupFragment(), SIGNUP_FRAGMENT_TAG)
                .addToBackStack(null)
                .commit();
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
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }
}
