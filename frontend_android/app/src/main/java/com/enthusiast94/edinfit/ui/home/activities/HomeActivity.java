package com.enthusiast94.edinfit.ui.home.activities;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models.User;
import com.enthusiast94.edinfit.network.UserService;
import com.enthusiast94.edinfit.ui.find_a_bus.activities.FindABusActivity;
import com.enthusiast94.edinfit.ui.home.fragments.ActivityFragment;
import com.enthusiast94.edinfit.ui.home.fragments.NearMeFragment;
import com.enthusiast94.edinfit.ui.home.fragments.SavedStopsFragment;
import com.enthusiast94.edinfit.ui.login_and_signup.activities.LoginActivity;
import com.enthusiast94.edinfit.ui.user_profile.activities.UserProfileActivity;
import com.enthusiast94.edinfit.ui.user_profile.events.OnDeauthenticatedEvent;
import com.enthusiast94.edinfit.ui.wait_or_walk_mode.activities.NewActivityActivity;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;

import de.greenrobot.event.EventBus;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = HomeActivity.class.getSimpleName();
    private static final String SELECTED_PAGE_INDEX = "selectedPageIndex";

    private ViewPager viewPager;
    private NavigationView navView;
    private DrawerLayout drawerLayout;
    private TextView navNameTextView;
    private TextView navEmailTextView;
    private FloatingActionMenu fabMenu;
    private FloatingActionButton waitOrWalkFab;
    private FloatingActionButton journeyPlannerFab;

    private Handler handler;
    private ActionBarDrawerToggle actionBarDrawerToggle;
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

            drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
            navView = (NavigationView) findViewById(R.id.navigation_view);
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            View navHeaderContainer = navView.getHeaderView(0);
            navNameTextView = (TextView) navHeaderContainer.findViewById(R.id.name_textview);
            navEmailTextView = (TextView) navHeaderContainer.findViewById(R.id.email_textview);
            viewPager = (ViewPager) findViewById(R.id.viewpager);
            fabMenu = (FloatingActionMenu) findViewById(R.id.fab_menu);
            waitOrWalkFab = (FloatingActionButton) findViewById(R.id.wait_or_walk_fab);
            journeyPlannerFab = (FloatingActionButton) findViewById(R.id.journey_planner_fab);

            /**
             * Setup AppBar
             */

            setSupportActionBar(toolbar);

            ActionBar appBar = getSupportActionBar();
            if (appBar != null) {
                appBar.setHomeButtonEnabled(true);
                appBar.setDisplayHomeAsUpEnabled(true);
            }

            actionBarDrawerToggle = new ActionBarDrawerToggle(
                    this,
                    drawerLayout,
                    toolbar,
                    R.string.label_nav_open,
                    R.string.label_nav_close
            );
            actionBarDrawerToggle.syncState();

            /**
             * Setup floating action menu item clicks
             */

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
             * Populate nav view header with user details and bind event listeners
             */

            populateNavViewHeader();
            navHeaderContainer.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    Intent startActivityIntent = new Intent(HomeActivity.this, UserProfileActivity.class);
                    startActivity(startActivityIntent);
                }
            });

            /**
             * Handle navigation view menu item clicks by displaying appropriate fragments/activities.
             */

            handler = new Handler();

            navView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {

                @Override
                public boolean onNavigationItemSelected(MenuItem menuItem) {
                    switch (menuItem.getItemId()) {
                        case R.id.action_user_profile:
                            final Intent startActivityIntent =
                                    new Intent(HomeActivity.this, UserProfileActivity.class);

                            drawerLayout.closeDrawers();

                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    startActivity(startActivityIntent);
                                }
                            }, 300);

                            return true;

                        case R.id.action_activity:
                            navigateToPage(0);
                            return true;
                        case R.id.action_near_me:
                            navigateToPage(1);
                            return true;
                        case R.id.action_saved_stops:
                            navigateToPage(2);
                            return true;
                        default:
                            return false;
                    }
                }
            });

            /**
             * Setup view pager
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

        MenuItem menuItem = navView.getMenu().getItem(position + 1 /*exclude 'User Profile' item */);
        menuItem.setChecked(true);

        setTitle(menuItem.getTitle());
        drawerLayout.closeDrawers();

        // only show fab on ActivityFragment
        if (position == 0) {
            fabMenu.showMenuButton(true);
        } else {
            fabMenu.hideMenuButton(true);
        }
    }

    private void populateNavViewHeader() {
        User user = UserService.getInstance().getAuthenticatedUser();
        if (user != null) {
            navNameTextView.setText(user.getName());
            navEmailTextView.setText(user.getEmail());
        }
    }

    private void goToLogin() {
        Intent startLActivityIntent = new Intent(this, LoginActivity.class);
        finish();
        startActivity(startLActivityIntent);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        actionBarDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        // noinspection SimplifiableIfStatement
        if (actionBarDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) {
            case R.id.action_search:
                Intent startActivityIntent = new Intent(this, FindABusActivity.class);
                startActivity(startActivityIntent);
                return true;
            default:
                return false;
        }
    }

    public void onEventMainThread(OnDeauthenticatedEvent event) {
        goToLogin();
    }

    private class MainPagerAdapter extends FragmentPagerAdapter {

        private static final int FRAGMENT_COUNT = 3;

        public MainPagerAdapter() {
            super(getSupportFragmentManager());
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0: return new ActivityFragment();
                case 1: return new NearMeFragment();
                case 2: return new SavedStopsFragment();
                default: return null;
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0: return getString(R.string.label_activity);
                case 1: return getString(R.string.label_near_me);
                case 2: return getString(R.string.label_saved);
                default: return null;
            }
        }

        @Override
        public int getCount() {
            return FRAGMENT_COUNT;
        }
    }
}
