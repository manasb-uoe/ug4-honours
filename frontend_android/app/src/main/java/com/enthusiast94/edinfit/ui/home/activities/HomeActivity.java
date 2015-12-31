package com.enthusiast94.edinfit.ui.home.activities;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.network.UserService;
import com.enthusiast94.edinfit.ui.home.events.OnActivityClickedEvent;
import com.enthusiast94.edinfit.ui.home.events.OnDeauthenticatedEvent;
import com.enthusiast94.edinfit.ui.home.fragments.ActivityDetailFragment;
import com.enthusiast94.edinfit.ui.home.fragments.ActivityFragment;
import com.enthusiast94.edinfit.ui.home.fragments.NearMeFragment;
import com.enthusiast94.edinfit.ui.home.fragments.SavedStopsFragment;
import com.enthusiast94.edinfit.ui.home.fragments.SearchFragment;
import com.enthusiast94.edinfit.ui.home.fragments.UserProfileFragment;
import com.enthusiast94.edinfit.ui.login_and_signup.activities.LoginActivity;
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
    private final int[] tabIconsUnselected = new int[]{R.drawable.ic_directions_run_unselected,
            R.drawable.ic_near_me_unselected, R.drawable.ic_search_unselected, R.drawable.ic_toggle_star_unselected,
            R.drawable.ic_social_person_unselected};
    private final int[] tabIconsSelected = new int[]{R.drawable.ic_directions_run_selected,
            R.drawable.ic_near_me_selected, R.drawable.ic_search_selected, R.drawable.ic_toggle_star_selected,
            R.drawable.ic_social_person_selected};

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

    private void navigateToPage(int position) {
        viewPager.setCurrentItem(position);
        onPageSelected(position);
    }

    private void onPageSelected(int position) {
        // only highlight selected tab
        setTabSelected(selectedPageIndex, false);
        setTabSelected(position, true);

        // only show fab on ActivityFragment
        if (position == 0) {
            fabMenu.showMenuButton(true);
        } else {
            fabMenu.hideMenuButton(true);
        }

        selectedPageIndex = position;
    }

    private void setupTabs() {
        tabLayout.setupWithViewPager(viewPager);

        for (int i=0; i<tabLayout.getTabCount(); i++) {
            tabLayout.getTabAt(i).setIcon(tabIconsUnselected[i]);
        }
    }

    private void setTabSelected(int pos, boolean shouldSelect) {
        tabLayout.getTabAt(pos).setIcon(shouldSelect ? tabIconsSelected[pos] : tabIconsUnselected[pos]);
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

    private class MainPagerAdapter extends FragmentPagerAdapter {

        private static final int FRAGMENT_COUNT = 5;

        public MainPagerAdapter() {
            super(getSupportFragmentManager());
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0: return new ActivityFragment();
                case 1: return new NearMeFragment();
                case 2: return new SearchFragment();
                case 3: return new SavedStopsFragment();
                case 4: return new UserProfileFragment();
                default: return null;
            }
        }

        @Override
        public int getCount() {
            return FRAGMENT_COUNT;
        }
    }
}
