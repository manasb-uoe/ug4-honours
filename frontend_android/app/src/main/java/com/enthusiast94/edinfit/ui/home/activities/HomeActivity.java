package com.enthusiast94.edinfit.ui.home.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.arasthel.asyncjob.AsyncJob;
import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models.Service;
import com.enthusiast94.edinfit.models.Stop;
import com.enthusiast94.edinfit.models.User;
import com.enthusiast94.edinfit.network.BaseService;
import com.enthusiast94.edinfit.network.ServiceService;
import com.enthusiast94.edinfit.network.StopService;
import com.enthusiast94.edinfit.network.UserService;
import com.enthusiast94.edinfit.ui.home.events.OnDeauthenticatedEvent;
import com.enthusiast94.edinfit.ui.home.events.OnStopsAndServicesPopulatedEvent;
import com.enthusiast94.edinfit.ui.home.fragments.ActivityFragment;
import com.enthusiast94.edinfit.ui.home.fragments.DisruptionsFragment;
import com.enthusiast94.edinfit.ui.home.fragments.FavouriteStopsFragment;
import com.enthusiast94.edinfit.ui.home.fragments.NearMeFragment;
import com.enthusiast94.edinfit.ui.home.fragments.UserProfileFragment;
import com.enthusiast94.edinfit.ui.login_and_signup.activities.LoginActivity;
import com.enthusiast94.edinfit.ui.search.activities.SearchActivity;

import de.greenrobot.event.EventBus;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = HomeActivity.class.getSimpleName();
    private static final String SELECTED_FRAGMENT_INDEX = "selectedFragmentIndex";

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    private ActionBarDrawerToggle actionBarDrawerToggle;
    private ProgressDialog progressDialog;
    private Handler handler;
    private int selectedFragmentIndex;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // start login activity if user is not authenticated
        if (!UserService.getInstance().isUserAuthenticated()) {
            goToLogin();
            return;
        }

        EventBus.getDefault().register(this);
        setContentView(R.layout.activity_home);
        handler = new Handler();

        // find views
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        navigationView = (NavigationView) findViewById(R.id.navigation_view);

        // setup toolbar with ActionBarDrawerToggle
        setSupportActionBar(toolbar);
        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.drawer_close, R.string.drawer_open);
        drawerLayout.setDrawerListener(actionBarDrawerToggle);

        // handle navigation drawer item clicks to show appropriate fragments
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {

            @Override
            public boolean onNavigationItemSelected(final MenuItem item) {
                // navigate to fragment after closing drawers in order to prevent lag
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        navigateToFragment(getMenuItemIndex(item));
                    }
                }, 300);

                drawerLayout.closeDrawers();
                return true;
            }
        });

        // populate database with stops and services
        // no need to proceed if stops and services already exist
        if (Stop.getCount() > 0 && Service.getCount() > 0) {
            EventBus.getDefault().post(
                    new OnStopsAndServicesPopulatedEvent(savedInstanceState));
            return;
        }

        setSettingThingsUpDialogEnabled(true);

        new AsyncJob.AsyncJobBuilder<BaseService.Response<Void>>()
                .doInBackground(new AsyncJob.AsyncAction<BaseService.Response<Void>>() {
                    @Override
                    public BaseService.Response<Void> doAsync() {
                        BaseService.Response<Void> response1 =
                                StopService.getInstance().populateStops();

                        if (!response1.isSuccessfull()) {
                            return response1;
                        }

                        return ServiceService.getInstance().populateServices();
                    }
                })
                .doWhenFinished(new AsyncJob.AsyncResultAction<BaseService.Response<Void>>() {
                    @Override
                    public void onResult(BaseService.Response<Void> response) {
                        setSettingThingsUpDialogEnabled(false);

                        if (!response.isSuccessfull()) {
                            Toast.makeText(HomeActivity.this, response.getError(),
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        EventBus.getDefault().post(
                                new OnStopsAndServicesPopulatedEvent(savedInstanceState));
                    }
                }).create().start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt(SELECTED_FRAGMENT_INDEX, selectedFragmentIndex);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawers();
            return;
        }

        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (actionBarDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        } else if (item.getItemId() == R.id.action_search) {
            Intent startActivityIntent = new Intent(this, SearchActivity.class);
            startActivity(startActivityIntent);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        actionBarDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggle
        actionBarDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home, menu);
        return true;
    }

    private void setSettingThingsUpDialogEnabled(boolean shouldEnable) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage(getString(R.string.setting_things_up));
            progressDialog.setCancelable(false);
        }

        if (shouldEnable) {
            progressDialog.show();
        } else {
            progressDialog.dismiss();
        }
    }

    private int getMenuItemIndex(MenuItem menuItem) {
        Menu menu = navigationView.getMenu();
        for (int i=0; i<menu.size(); i++) {
            if (menuItem == menu.getItem(i)) {
                return i;
            }
        }

        throw new IllegalArgumentException("Provided menuItem does not exist in the menu");
    }

    private void navigateToFragment(int position) {
        // replace current fragment with the one corresponding to provided position
        replaceFragment(position);

        // update app bar title
        setTitle(getFragmentTitle(position));

        // highlight selected navigation drawer item
        navigationView.getMenu().getItem(position).setChecked(true);

        selectedFragmentIndex = position;
    }

    private String getFragmentTitle(int position) {
        switch (position) {
            case 0:return getString(R.string.nearby_bus_stops);
            case 1: return getString(R.string.favourites);
            case 2: return getString(R.string.activity);
            case 3: return getString(R.string.disruptions);
            case 4: return getString(R.string.settings);
            default:
                throw new IllegalArgumentException("Invalid position: " + position);
        }
    }

    private void replaceFragment(int position) {
        Fragment fragment;
        String tag;

        switch (position) {
            case 0:
                fragment = new NearMeFragment();
                tag = NearMeFragment.TAG;
                break;
            case 1:
                fragment = new FavouriteStopsFragment();
                tag = FavouriteStopsFragment.TAG;
                break;
            case 2:
                fragment = new ActivityFragment();
                tag = ActivityFragment.TAG;
                break;
            case 3:
                fragment = new DisruptionsFragment();
                tag = DisruptionsFragment.TAG;
                break;
            case 4:
                fragment = new UserProfileFragment();
                tag = UserProfileFragment.TAG;
                break;
            default: throw new IllegalArgumentException("Invalid position: " + position);
        }

        if (getSupportFragmentManager().findFragmentByTag(tag) == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment, tag)
                    .commit();
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

    public void onEventMainThread(OnStopsAndServicesPopulatedEvent event) {
        setSettingThingsUpDialogEnabled(false);

        // update navigation view header with authenticated user info
        View headerView = navigationView.getHeaderView(0);
        TextView nameTextView = (TextView) headerView.findViewById(R.id.name_textview);
        TextView emailTextView = (TextView) headerView.findViewById(R.id.email_textview);
        User user = UserService.getInstance().getAuthenticatedUser();
        nameTextView.setText(user.getName());
        emailTextView.setText(user.getEmail());

        // Navigate to fragment based on the fragmnet index in saved instance state,
        // ensuring that the correct fragment is selected after configuration changes.
        if (event.getSavedInstanceState()== null) {
            selectedFragmentIndex = 0; /* default = Activity fragment */
        } else {
            selectedFragmentIndex = event.getSavedInstanceState().getInt(SELECTED_FRAGMENT_INDEX);
        }

        navigateToFragment(selectedFragmentIndex);
    }
}
