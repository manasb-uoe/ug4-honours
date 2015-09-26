package com.enthusiast94.edinfit.activities;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.events.AuthenticatedEvent;
import com.enthusiast94.edinfit.events.LoginEvent;
import com.enthusiast94.edinfit.events.ShowSignupFragmentEvent;
import com.enthusiast94.edinfit.fragments.LoginAndSignupHeadlessFragment;
import com.enthusiast94.edinfit.fragments.LoginFragment;
import com.enthusiast94.edinfit.fragments.SignupFragment;

import butterknife.Bind;
import butterknife.BindString;
import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;

public class LoginActivity extends AppCompatActivity {

    private static final String LOGIN_FRAGMENT_TAG = "loginFragmentTag";
    private static final String SIGNUP_FRAGMENT_TAG = "signupFragmentTag";
    private static final String LOGIN_AND_SIGNUP_HEADLESS_FRAGMENT_TAG = "loginAndSignupHeadlessFragment";
    private LoginAndSignupHeadlessFragment loginAndSignupHeadlessFragment;
    @BindString(R.string.success_logged_in_as_base) String loggedInAsBaseSuccess;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);

        loginAndSignupHeadlessFragment = (LoginAndSignupHeadlessFragment)
                getSupportFragmentManager().findFragmentByTag(LOGIN_AND_SIGNUP_HEADLESS_FRAGMENT_TAG);
        if (loginAndSignupHeadlessFragment == null) {
            loginAndSignupHeadlessFragment = new LoginAndSignupHeadlessFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(loginAndSignupHeadlessFragment, LOGIN_AND_SIGNUP_HEADLESS_FRAGMENT_TAG)
                    .commit();
        }

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

    public void onEventMainThread(AuthenticatedEvent event) {
        Toast.makeText(this, loggedInAsBaseSuccess + event.getUser().getName(), Toast.LENGTH_LONG)
                .show();
    }
}
