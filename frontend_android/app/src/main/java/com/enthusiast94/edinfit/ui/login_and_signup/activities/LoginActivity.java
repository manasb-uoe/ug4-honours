package com.enthusiast94.edinfit.ui.login_and_signup.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.ui.home.activities.HomeActivity;
import com.enthusiast94.edinfit.ui.login_and_signup.fragments.LoginFragment;
import com.enthusiast94.edinfit.ui.login_and_signup.events.OnAuthenticatedEvent;
import com.enthusiast94.edinfit.ui.login_and_signup.events.ShowSignupFragmentEvent;
import com.enthusiast94.edinfit.ui.login_and_signup.fragments.SignupFragment;

import de.greenrobot.event.EventBus;

public class LoginActivity extends AppCompatActivity {

    private ViewPager viewPager;
    private TabLayout tabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        /**
         * Find views
         */

        viewPager = (ViewPager) findViewById(R.id.viewpager);
        tabLayout = (TabLayout) findViewById(R.id.tablayout);

        /**
         * Setup viewpager to work with tabs.
         */

        viewPager.setAdapter(new LoginPagerAdapter());
        tabLayout.setupWithViewPager(viewPager);
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

    private class LoginPagerAdapter extends FragmentPagerAdapter {

        public LoginPagerAdapter() {
            super(getSupportFragmentManager());
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0: return new LoginFragment();
                case 1: return new SignupFragment();
                default: return null;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0: return getString(R.string.action_login);
                case 1: return getString(R.string.action_signup);
                default: return null;
            }
        }
    }


    /**
     * EventBus event handling methods
     */

    public void onEventMainThread(ShowSignupFragmentEvent event) {
        viewPager.setCurrentItem(1);
    }

    public void onEventMainThread(OnAuthenticatedEvent event) {
        Toast.makeText(this, getString(R.string.success_logged_in_as_base) + event.getUser().getName(), Toast.LENGTH_LONG)
                .show();
        Intent startActivityIntent = new Intent(this, HomeActivity.class);
        finish();
        startActivity(startActivityIntent);
    }
}
