package com.enthusiast94.edinfit.activities;

import android.content.res.Configuration;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.enthusiast94.edinfit.R;
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
    private ActionBarDrawerToggle actionBarDrawerToggle;
    private int selectedNavMenuItemIndex;
    private static final String SELECTED_NAV_MENU_ITEM_INDEX_KEY = "selectedNavMenuItemIndex";

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
         * Handle navigation view menu item clicks by displaying appropriate fragments.
         */

        navView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {

            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                selectNavMenuItem(menuItem);
                return false;
            }
        });

        /**
         * Select navigation view menu item based on saved instance state, ensuring that the
         * right item is selected after configuration changes
         */

        if (savedInstanceState == null) {
            selectedNavMenuItemIndex = 0;
        } else {
            selectedNavMenuItemIndex = savedInstanceState.getInt(SELECTED_NAV_MENU_ITEM_INDEX_KEY);
        }

        selectNavMenuItem(navView.getMenu().getItem(selectedNavMenuItemIndex));
    }

    private void populateNavViewHeader() {
        User user = UserService.getAuthenticatedUser();
        if (user != null) {
            navNameTextVeiew.setText(user.getName());
            navEmailTextView.setText(user.getEmail());
        }
    }

    private void selectNavMenuItem(MenuItem menuItem) {
        menuItem.setChecked(true);
        drawerLayout.closeDrawers();
        setTitle(menuItem.getTitle());

        selectedNavMenuItemIndex = getMenuItemIndex(menuItem, navView.getMenu());
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

        savedInstanceState.putInt(SELECTED_NAV_MENU_ITEM_INDEX_KEY, selectedNavMenuItemIndex);
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

}
