package com.enthusiast94.edinfit.activities;

import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TableLayout;
import android.widget.Toast;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.events.OnAuthenticatedEvent;
import com.enthusiast94.edinfit.events.ShowSignupFragmentEvent;
import com.enthusiast94.edinfit.fragments.LoginFragment;
import com.enthusiast94.edinfit.fragments.SignupFragment;

import butterknife.Bind;
import butterknife.BindString;
import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;

public class LoginActivity extends AppCompatActivity {

    @Bind(R.id.viewpager) ViewPager viewPager;
    @Bind(R.id.tablayout) TabLayout tabLayout;
    @BindString(R.string.success_logged_in_as_base) String loggedInAsBaseSuccess;
    @BindString(R.string.action_login) String loginAction;
    @BindString(R.string.action_signup) String signupAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);

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
                case 0: return loginAction;
                case 1: return signupAction;
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
        Toast.makeText(this, loggedInAsBaseSuccess + event.getUser().getName(), Toast.LENGTH_LONG)
                .show();
    }
}
