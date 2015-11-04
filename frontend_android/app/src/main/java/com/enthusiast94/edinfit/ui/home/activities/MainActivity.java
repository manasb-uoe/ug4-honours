package com.enthusiast94.edinfit.ui.home.activities;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
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
import com.enthusiast94.edinfit.ui.home.fragments.ActivityFragment;
import com.enthusiast94.edinfit.ui.home.fragments.GoFragment;
import com.enthusiast94.edinfit.ui.home.fragments.NearbyFragment;
import com.enthusiast94.edinfit.ui.user_profile.events.OnDeauthenticatedEvent;
import com.enthusiast94.edinfit.models.User;
import com.enthusiast94.edinfit.services.UserService;
import com.enthusiast94.edinfit.ui.login_and_signup.activities.LoginActivity;
import com.enthusiast94.edinfit.ui.user_profile.activities.UserProfileActivity;

import de.greenrobot.event.EventBus;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navView;
    private Toolbar toolbar;
    private View navHeaderContainer;
    private TextView navNameTextVeiew;
    private TextView navEmailTextView;
    private ViewPager viewPager;
    private ActionBarDrawerToggle actionBarDrawerToggle;
    private int selectedPageIndex;
    private static final String SELECTED_PAGE_INDEX = "selectedPageIndex";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /**
         * Start login activity if user is not authenticated, else continue with this activity
         */

        if (!UserService.getInstance().isUserAuthenticated()) {
            goToLogin();
        } else {
            setContentView(R.layout.activity_main);

            /**
             * Register with default event bus
             */

            EventBus.getDefault().register(this);

            /**
             * Find views
             */

            drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
            navView = (NavigationView) findViewById(R.id.navigation_view);
            toolbar = (Toolbar) findViewById(R.id.toolbar);
            navHeaderContainer = findViewById(R.id.nav_header_container);
            navNameTextVeiew = (TextView) findViewById(R.id.name_textview);
            navEmailTextView = (TextView) findViewById(R.id.email_textview);
            viewPager = (ViewPager) findViewById(R.id.viewpager);

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
             * Populate nav view header with user details and bind event listeners
             */

            populateNavViewHeader();
            navHeaderContainer.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    Intent startActivityIntent = new Intent(MainActivity.this, UserProfileActivity.class);
                    startActivity(startActivityIntent);
                }
            });

            /**
             * Handle navigation view menu item clicks by displaying appropriate fragments/activities.
             */

            navView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {

                @Override
                public boolean onNavigationItemSelected(MenuItem menuItem) {
                    int menuItemIndex = getMenuItemIndex(menuItem, navView.getMenu());

                    if (menuItemIndex < MainPagerAdapter.FRAGMENT_COUNT) {
                        navigateToPage(menuItemIndex);
                    }

                    return false;
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
                    MainActivity.this.onPageSelected(position);
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
                selectedPageIndex = 1; /* default = Activity fragment */
            } else {
                selectedPageIndex = savedInstanceState.getInt(SELECTED_PAGE_INDEX);
            }

            navigateToPage(selectedPageIndex);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        Log.i("DESTROY", "destroyed");
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

        savedInstanceState.putInt(SELECTED_PAGE_INDEX, selectedPageIndex);
    }

    private void populateNavViewHeader() {
        User user = UserService.getInstance().getAuthenticatedUser();
        if (user != null) {
            navNameTextVeiew.setText(user.getName());
            navEmailTextView.setText(user.getEmail());
        }
    }

    private void navigateToPage(int position) {
        viewPager.setCurrentItem(position);
        onPageSelected(position);
    }

    private void onPageSelected(int position) {
        selectedPageIndex = position;

        MenuItem menuItem = navView.getMenu().getItem(position);
        menuItem.setChecked(true);

        setTitle(menuItem.getTitle());

        drawerLayout.closeDrawers();
    }

    private int getMenuItemIndex(MenuItem menuItem, Menu menu) {
        for (int i=0; i<menu.size(); i++) {
            MenuItem currentMenuItem = menu.getItem(i);
            if (currentMenuItem.getItemId() == menuItem.getItemId()) {
                return i;
            }
        }

        return -1;
    }

    private void goToLogin() {
        Intent startLActivityIntent = new Intent(this, LoginActivity.class);
        finish();
        startActivity(startLActivityIntent);
    }

    public void onEventMainThread(OnDeauthenticatedEvent event) {
        goToLogin();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        actionBarDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        // noinspection SimplifiableIfStatement
        if (actionBarDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        return super.onOptionsItemSelected(item);
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
                case 1: return new GoFragment();
                case 2: return new NearbyFragment();
                default: return null;
            }
        }

        @Override
        public int getCount() {
            return FRAGMENT_COUNT;
        }
    }
}
