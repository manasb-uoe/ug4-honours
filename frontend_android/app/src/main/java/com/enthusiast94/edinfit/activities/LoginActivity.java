package com.enthusiast94.edinfit.activities;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.events.OnAuthenticatedEvent;
import com.enthusiast94.edinfit.events.ShowSignupFragmentEvent;
import com.enthusiast94.edinfit.fragments.LoginFragment;
import com.enthusiast94.edinfit.fragments.SignupFragment;

import butterknife.BindString;
import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;

public class LoginActivity extends AppCompatActivity {

    private static final String LOGIN_FRAGMENT_TAG = "loginFragmentTag";
    private static final String SIGNUP_FRAGMENT_TAG = "signupFragmentTag";
    private static final String LOGIN_AND_SIGNUP_HEADLESS_FRAGMENT_TAG = "loginAndSignupHeadlessFragment";
    @BindString(R.string.success_logged_in_as_base) String loggedInAsBaseSuccess;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);

        if (getSupportFragmentManager().findFragmentByTag(LOGIN_FRAGMENT_TAG) == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container_framelayout, new LoginFragment(), LOGIN_FRAGMENT_TAG)
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
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    /**
     * EventBus event handling methods
     */

    public void onEventMainThread(ShowSignupFragmentEvent event) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container_framelayout, new SignupFragment(), SIGNUP_FRAGMENT_TAG)
                .addToBackStack(null)
                .commit();
    }

    public void onEventMainThread(OnAuthenticatedEvent event) {
        Toast.makeText(this, loggedInAsBaseSuccess + event.getUser().getName(), Toast.LENGTH_LONG)
                .show();
    }
}
