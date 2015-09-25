package com.enthusiast94.edinfit.activities;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.fragments.LoginFragment;

public class LoginActivity extends AppCompatActivity {

    private static final String LOGIN_FRAGMENT_TAG = "loginFragmentTag";
    private static final String SIGNUP_FRAGMENT_TAG = "signupFragmentTag";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        LoginFragment loginFragment =
                (LoginFragment) getSupportFragmentManager().findFragmentByTag(LOGIN_FRAGMENT_TAG);
        if (loginFragment == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container_framelayout, new LoginFragment(), LOGIN_FRAGMENT_TAG)
                    .commit();
        }
    }
}
