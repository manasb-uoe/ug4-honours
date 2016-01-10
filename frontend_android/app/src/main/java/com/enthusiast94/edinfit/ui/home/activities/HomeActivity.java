package com.enthusiast94.edinfit.ui.home.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.arasthel.asyncjob.AsyncJob;
import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models_2.Service;
import com.enthusiast94.edinfit.models_2.Stop;
import com.enthusiast94.edinfit.network.BaseService;
import com.enthusiast94.edinfit.network.ServiceService;
import com.enthusiast94.edinfit.network.StopService;
import com.enthusiast94.edinfit.network.UserService;
import com.enthusiast94.edinfit.ui.home.events.OnActivityClickedEvent;
import com.enthusiast94.edinfit.ui.home.events.OnDeauthenticatedEvent;
import com.enthusiast94.edinfit.ui.home.events.OnStopsAndServicesPopulatedEvent;
import com.enthusiast94.edinfit.ui.home.fragments.ActivityDetailFragment;
import com.enthusiast94.edinfit.ui.home.fragments.FavouriteStopsFragment;
import com.enthusiast94.edinfit.ui.home.fragments.NearMeFragment;
import com.enthusiast94.edinfit.ui.home.fragments.UserProfileFragment;
import com.enthusiast94.edinfit.ui.login_and_signup.activities.LoginActivity;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;

import de.greenrobot.event.EventBus;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = HomeActivity.class.getSimpleName();
    private static final String SELECTED_FRAGMENT_INDEX = "selectedFragmentIndex";

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private FloatingActionMenu fabMenu;

    private ActionBarDrawerToggle actionBarDrawerToggle;
    private ProgressDialog progressDialog;
    private Handler handler;
    private ViewPager viewPager;
    private MainPagerAdapter adapter;
    private int selectedPageIndex;

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
        viewPager = (ViewPager) findViewById(R.id.viewpager);
        fabMenu = (FloatingActionMenu) findViewById(R.id.fab_menu);
        FloatingActionButton waitOrWalkFab =
                (FloatingActionButton) findViewById(R.id.wait_or_walk_fab);
        FloatingActionButton journeyPlannerFab =
                (FloatingActionButton) findViewById(R.id.journey_planner_fab);

        // setup toolbar with ActionBarDrawerToggle
        setSupportActionBar(toolbar);
        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.drawer_close, R.string.drawer_open);

        // handle navigation drawer item clicks to show appropriate fragments
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {

            @Override
            public boolean onNavigationItemSelected(final MenuItem item) {
                // close drawers after some delay to prevent lag
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        drawerLayout.closeDrawers();
                    }
                }, 300);

                int menuItemIndex = getMenuItemIndex(item);
                if (menuItemIndex < adapter.getCount()) {
                    navigateToPage(getMenuItemIndex(item));
                } else {
                    // Todo
                }

                return true;
            }
        });

        // setup floating action menu item clicks
        waitOrWalkFab.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
//                    final Intent startActivityIntent =
//                            new Intent(HomeActivity.this, NewActivityActivity.class);
//
//                    fabMenu.close(true);
//
//                    handler.postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            startActivity(startActivityIntent);
//                        }
//                    }, 300);

            }
        });
        journeyPlannerFab.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO
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
        savedInstanceState.putInt(SELECTED_FRAGMENT_INDEX, selectedPageIndex);
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();

            // unfix orientation in case one of the fragments fixed it
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (actionBarDrawerToggle.onOptionsItemSelected(item)) {
            return true;
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

    private void navigateToPage(int position) {
        viewPager.setCurrentItem(position);
        onPageSelected(position);
    }

    private void onPageSelected(int position) {
        // update app bar title
        setTitle(adapter.getPageTitle(position));

        // highlight selected navigation drawer item
        navigationView.getMenu().getItem(position).setChecked(true);

        // only show fab on ActivityFragment
        if (position == 0) {
            fabMenu.showMenuButton(true);
        } else {
            fabMenu.hideMenuButton(true);
        }

        selectedPageIndex = position;
    }

    private void goToLogin() {
        Intent startLActivityIntent = new Intent(this, LoginActivity.class);
        finish();
        startActivity(startLActivityIntent);
    }

    public void onEventMainThread(OnDeauthenticatedEvent event) {
        goToLogin();
    }

    public void onEventMainThread(OnActivityClickedEvent event) {
        getSupportFragmentManager().beginTransaction()
                .addToBackStack(null)
                .setCustomAnimations(R.anim.fade_in, 0, 0, R.anim.fade_out)
                .add(android.R.id.content, ActivityDetailFragment.newInstance(event.getActivity()),
                        ActivityDetailFragment.TAG)
                .commit();
    }

    public void onEventMainThread(OnStopsAndServicesPopulatedEvent event) {
        setSettingThingsUpDialogEnabled(false);

        // setup viewpager
        adapter = new MainPagerAdapter(this, getSupportFragmentManager());
        viewPager.setAdapter(adapter);
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

        // Navigate to viewpager page based on the page index in saved instance state,
        // ensuring that the correct page is selected after configuration changes.
        if (event.getSavedInstanceState()== null) {
            selectedPageIndex = 0; /* default = Activity fragment */
        } else {
            selectedPageIndex = event.getSavedInstanceState().getInt(SELECTED_FRAGMENT_INDEX);
        }

        viewPager.setCurrentItem(selectedPageIndex);
        navigateToPage(selectedPageIndex);
    }

    private static class MainPagerAdapter extends FragmentPagerAdapter {

        private static final int FRAGMENT_COUNT = 3;
        private Context context;

        public MainPagerAdapter(Context context, FragmentManager fragmentManager) {
            super(fragmentManager);
            this.context = context;
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0: return new NearMeFragment();
                case 1: return new FavouriteStopsFragment();
                case 2: return new UserProfileFragment();
                default: throw new IllegalArgumentException("Invalid position: " + position);
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:return context.getString(R.string.nearby_bus_stops);
                case 1: return context.getString(R.string.favourites);
                case 2: return context.getString(R.string.user_profile);
                default:
                    throw new IllegalArgumentException("Invalid position: " + position);
            }
        }

        @Override
        public int getCount() {
            return FRAGMENT_COUNT;
        }
    }
}
