package com.enthusiast94.edinfit.ui.home.activities;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.network.UserService;
import com.enthusiast94.edinfit.ui.home.fragments.ActivityFragment;
import com.enthusiast94.edinfit.ui.home.fragments.NearMeFragment;
import com.enthusiast94.edinfit.ui.home.fragments.SavedStopsFragment;
import com.enthusiast94.edinfit.ui.login_and_signup.activities.LoginActivity;
import com.enthusiast94.edinfit.ui.search.activities.SearchActivity;
import com.enthusiast94.edinfit.ui.user_profile.events.OnDeauthenticatedEvent;
import com.enthusiast94.edinfit.ui.user_profile.fragments.UserProfileFragment;
import com.enthusiast94.edinfit.ui.wait_or_walk_mode.activities.NewActivityActivity;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;

import de.greenrobot.event.EventBus;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = HomeActivity.class.getSimpleName();
    private static final String SELECTED_PAGE_INDEX = "selectedPageIndex";

    private FloatingActionMenu fabMenu;
    private FloatingActionButton waitOrWalkFab;
    private FloatingActionButton journeyPlannerFab;
    private TabLayout tabLayout;

    private Handler handler;
    private ViewPager viewPager;
    private int selectedPageIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /**
         * Start login activity if user is not authenticated, else continue with this activity
         */

        if (!UserService.getInstance().isUserAuthenticated()) {
            goToLogin();
        } else {
            setContentView(R.layout.activity_home);

            /**
             * Register with default event bus
             */

            EventBus.getDefault().register(this);

            /**
             * Find views
             */

            viewPager = (ViewPager) findViewById(R.id.viewpager);
            fabMenu = (FloatingActionMenu) findViewById(R.id.fab_menu);
            waitOrWalkFab = (FloatingActionButton) findViewById(R.id.wait_or_walk_fab);
            journeyPlannerFab = (FloatingActionButton) findViewById(R.id.journey_planner_fab);
            tabLayout = (TabLayout) findViewById(R.id.tablayout);

            /**
             * Setup floating action menu item clicks
             */

            handler = new Handler();

            waitOrWalkFab.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    final Intent startActivityIntent =
                            new Intent(HomeActivity.this, NewActivityActivity.class);

                    fabMenu.close(true);

                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            startActivity(startActivityIntent);
                        }
                    }, 300);

                }
            });

            journeyPlannerFab.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    // TODO
                }
            });

            /**
             * Setup view pager with
             */

            viewPager.setAdapter(new MainPagerAdapter());
            viewPager.setOffscreenPageLimit(2);
            viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                }

                @Override
                public void onPageSelected(int position) {
                    HomeActivity.this.onPageSelected(position);
                }

                @Override
                public void onPageScrollStateChanged(int state) {
                }
            });
            setupTabs();

            /**
             * Navigate to viewpager page based on the page index in saved instance state, ensuring that
             * the correct page is selected after configuration changes.
             */

            if (savedInstanceState == null) {
                selectedPageIndex = 0; /* default = Activity fragment */
            } else {
                selectedPageIndex = savedInstanceState.getInt(SELECTED_PAGE_INDEX);
            }

            viewPager.setCurrentItem(selectedPageIndex);
            navigateToPage(selectedPageIndex);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

        savedInstanceState.putInt(SELECTED_PAGE_INDEX, selectedPageIndex);
    }

    private void navigateToPage(int position) {
        viewPager.setCurrentItem(position);
        onPageSelected(position);
    }

    private void onPageSelected(int position) {
        selectedPageIndex = position;

        // only show fab on ActivityFragment
        if (position == 0) {
            fabMenu.showMenuButton(true);
        } else {
            fabMenu.hideMenuButton(true);
        }
    }

    private void setupTabs() {
        int[] tabIcons = new int[]{R.drawable.ic_directions_run_black_24dp,
                R.drawable.ic_near_me_black_24dp, R.drawable.ic_star_black_24dp, R.drawable.ic_person_black_24dp};

        tabLayout.setupWithViewPager(viewPager);

        for (int i=0; i<tabLayout.getTabCount(); i++) {
            tabLayout.getTabAt(i).setIcon(tabIcons[i]);
        }
    }

    private void goToLogin() {
        Intent startLActivityIntent = new Intent(this, LoginActivity.class);
        finish();
        startActivity(startLActivityIntent);
    }

    public void onEventMainThread(OnDeauthenticatedEvent event) {
        goToLogin();
    }

    private class MainPagerAdapter extends FragmentPagerAdapter {

        private static final int FRAGMENT_COUNT = 4;

        public MainPagerAdapter() {
            super(getSupportFragmentManager());
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0: return new ActivityFragment();
                case 1: return new NearMeFragment();
                case 2: return new SavedStopsFragment();
                case 3: return new UserProfileFragment();
                default: return null;
            }
        }

        @Override
        public int getCount() {
            return FRAGMENT_COUNT;
        }
    }
}
