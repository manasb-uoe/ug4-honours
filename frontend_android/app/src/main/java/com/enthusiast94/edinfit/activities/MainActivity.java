package com.enthusiast94.edinfit.activities;

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
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.fragments.ActivityFragment;
import com.enthusiast94.edinfit.fragments.GoFragment;
import com.enthusiast94.edinfit.fragments.NearbyFragment;
import com.enthusiast94.edinfit.models.User;
import com.enthusiast94.edinfit.network.UserService;

import butterknife.Bind;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    @Bind(R.id.drawer_layout) DrawerLayout drawerLayout;
    @Bind(R.id.navigation_view) NavigationView navView;
    @Bind(R.id.toolbar) Toolbar toolbar;
    @Bind(R.id.name_textview) TextView navNameTextVeiew;
    @Bind(R.id.email_textview) TextView navEmailTextView;
    @Bind(R.id.viewpager) ViewPager viewPager;
    private ActionBarDrawerToggle actionBarDrawerToggle;
    private int selectedPageIndex;
    private static final String SELECTED_PAGE_INDEX = "selectedPageIndex";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

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
         * Populate nav view header with user details
         */

        populateNavViewHeader();

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
            selectedPageIndex = 0;
        } else {
            selectedPageIndex = savedInstanceState.getInt(SELECTED_PAGE_INDEX);
        }

        navigateToPage(selectedPageIndex);
    }

    private void populateNavViewHeader() {
        User user = UserService.getAuthenticatedUser();
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

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

        savedInstanceState.putInt(SELECTED_PAGE_INDEX, selectedPageIndex);
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
