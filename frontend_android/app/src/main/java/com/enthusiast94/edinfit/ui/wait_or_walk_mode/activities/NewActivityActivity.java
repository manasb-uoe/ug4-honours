package com.enthusiast94.edinfit.ui.wait_or_walk_mode.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models.Service;
import com.enthusiast94.edinfit.models.Service.Route;
import com.enthusiast94.edinfit.models.Stop;
import com.enthusiast94.edinfit.ui.wait_or_walk_mode.fragments.SelectDestinationStopFragment;
import com.enthusiast94.edinfit.ui.wait_or_walk_mode.fragments.SelectOriginStopFragment;
import com.enthusiast94.edinfit.ui.wait_or_walk_mode.fragments.SelectServiceFragment;

/**
 * Created by manas on 18-10-2015.
 */
public class NewActivityActivity extends AppCompatActivity {

    private static final String TAG = NewActivityActivity.class.getSimpleName();
    private static final String EXTRA_CURRENT_FRAGMENT_TAG = "currentFragmentTag";
    private static final String EXTRA_SELECTED_ORIGIN_STOP = "selectedOriginStop";
    private static final String EXTRA_SELECTED_SERVICE = "selectedService";
    private static final String EXTRA_SELECTED_DESTINATION_STOP = "selectedDestinationStop";
    private static final String EXTRA_SELECTED_ROUTE = "selectedRoute";

    private Toolbar toolbar;
    private Button nextButton;
    private Button previousButton;
    private TabLayout tabLayout;

    private String currentFragmentTag;
    private Stop selectedOriginStop;
    private Service selectedService;
    private Stop selectedDestinationStop;
    private Route selectedRoute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wait_or_walk);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        tabLayout = (TabLayout) findViewById(R.id.tablayout);
        nextButton = (Button) findViewById(R.id.next_button);
        previousButton = (Button) findViewById(R.id.previous_button);

        // setup app bar
        setSupportActionBar(toolbar);
        ActionBar appBar = getSupportActionBar();
        if (appBar != null) {
            appBar.setHomeButtonEnabled(true);
            appBar.setDisplayHomeAsUpEnabled(true);
            appBar.setTitle(R.string.label_wait_or_walk_activity);
        }

        // change status bar color to complement toolbar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.green_700));
        }

        // setup tabs
        tabLayout.setClickable(false);
        tabLayout.addTab(tabLayout.newTab().setText(R.string.label_step_1));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.label_step_2));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.label_step_3));

        // disable tab clicks
        for (int i=0; i<tabLayout.getChildCount(); i++) {
            LinearLayout tabStrip = ((LinearLayout) tabLayout.getChildAt(i));
            for(int j = 0; j < tabStrip.getChildCount(); j++) {
                tabStrip.getChildAt(j).setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        return true;
                    }
                });
            }
        }

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentFragmentTag.equals(SelectOriginStopFragment.TAG)) {
                    SelectOriginStopFragment fragment = (SelectOriginStopFragment)
                            getSupportFragmentManager().findFragmentByTag(currentFragmentTag);
                    selectedOriginStop = fragment.getSelectedOriginStop();
                    selectedService = null;
                    selectedDestinationStop = null;
                    selectedRoute = null;

                    addFragment(SelectServiceFragment.TAG);

                } else if (currentFragmentTag.equals(SelectServiceFragment.TAG)) {
                    SelectServiceFragment fragment = (SelectServiceFragment)
                            getSupportFragmentManager().findFragmentByTag(currentFragmentTag);
                    selectedService = fragment.getSelectedService();
                    selectedDestinationStop = null;
                    selectedRoute = null;

                    if (selectedService != null) {
                        addFragment(SelectDestinationStopFragment.TAG);
                    }
                } else {
                    SelectDestinationStopFragment fragment = (SelectDestinationStopFragment)
                            getSupportFragmentManager().findFragmentByTag(currentFragmentTag);
                    selectedDestinationStop = fragment.getSelectedDestinationStop();
                    selectedRoute = fragment.getSelectedRoute();

                    if (selectedDestinationStop != null && selectedRoute != null) {
                        // start suggestions activity
                        Intent startActivityIntent = new Intent(NewActivityActivity.this, SuggestionsActivity.class);
                        startActivityIntent.putExtra(SuggestionsActivity.EXTRA_SELECTED_ORIGIN_STOP_ID, selectedOriginStop.get_id());
                        startActivityIntent.putExtra(SuggestionsActivity.EXTRA_SELECTED_SERVICE_NAME, selectedService.getName());
                        startActivityIntent.putExtra(SuggestionsActivity.EXTRA_SELECTED_DESTINATION_STOP_ID, selectedDestinationStop.get_id());
                        startActivityIntent.putExtra(SuggestionsActivity.EXTRA_SELECTED_ROUTE_DESTINATION, selectedRoute.getDestination());
                        startActivity(startActivityIntent);
                    }
                }
            }
        });

        previousButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                removeFragment(currentFragmentTag);
            }
        });

        if (savedInstanceState != null) {
            selectedOriginStop = savedInstanceState.getParcelable(EXTRA_SELECTED_ORIGIN_STOP);
            selectedService = savedInstanceState.getParcelable(EXTRA_SELECTED_SERVICE);
            selectedDestinationStop = savedInstanceState.getParcelable(EXTRA_SELECTED_DESTINATION_STOP);
            selectedRoute = savedInstanceState.getParcelable(EXTRA_SELECTED_ROUTE);


            addFragment(savedInstanceState.getString(EXTRA_CURRENT_FRAGMENT_TAG));
        } else {
            addFragment(SelectOriginStopFragment.TAG);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(EXTRA_CURRENT_FRAGMENT_TAG, currentFragmentTag);
        outState.putString(EXTRA_SELECTED_ORIGIN_STOP, selectedOriginStop.get_id());
        outState.putString(EXTRA_SELECTED_SERVICE, selectedService.getName());
        outState.putString(EXTRA_SELECTED_DESTINATION_STOP, selectedDestinationStop.get_id());
        outState.putString(EXTRA_SELECTED_ROUTE, selectedRoute.getDestination());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    private void addFragment(String tag) {
        if (getSupportFragmentManager().findFragmentByTag(tag) == null) {
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.setCustomAnimations(R.anim.fade_in, 0, 0, R.anim.fade_out);

            if (tag.equals(SelectOriginStopFragment.TAG)) {
                fragmentTransaction.add(R.id.fragment_container, new SelectOriginStopFragment(),
                        SelectOriginStopFragment.TAG);

            } else if (tag.equals(SelectServiceFragment.TAG)) {
                fragmentTransaction.add(R.id.fragment_container, SelectServiceFragment.getInstance(
                        selectedOriginStop.getServices()), SelectServiceFragment.TAG);

            } else if (tag.equals(SelectDestinationStopFragment.TAG)) {
                fragmentTransaction.add(R.id.fragment_container, SelectDestinationStopFragment.newInstance(
                                selectedOriginStop.get_id(), selectedService.getName()),
                        SelectDestinationStopFragment.TAG);

            } else {
                throw new IllegalArgumentException("Invalid tag: " + tag);
            }

            fragmentTransaction.commit();
        }

        currentFragmentTag = tag;

        updateUi(tag);
    }

    private void removeFragment(String tag) {
        if (getSupportFragmentManager().findFragmentByTag(tag) != null) {
            Log.d(TAG, "Removing: " + getSupportFragmentManager().findFragmentByTag(tag).getClass().getSimpleName());
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.fade_in, 0, 0, R.anim.fade_out)
                    .remove(getSupportFragmentManager().findFragmentByTag(tag))
                    .commit();
        }

        if (tag.equals(SelectServiceFragment.TAG)) {
            currentFragmentTag = SelectOriginStopFragment.TAG;
            updateUi(currentFragmentTag);
        } else if (tag.equals(SelectDestinationStopFragment.TAG)) {
            currentFragmentTag = SelectServiceFragment.TAG;
            updateUi(currentFragmentTag);
        }
    }

    private void updateUi(String tag) {
        if (tag.equals(SelectOriginStopFragment.TAG)) {
            previousButton.setEnabled(false);
            nextButton.setEnabled(true);

            tabLayout.getTabAt(0).select();

        } else if (tag.equals(SelectServiceFragment.TAG)) {
            previousButton.setEnabled(true);
            nextButton.setEnabled(true);

            tabLayout.getTabAt(1).select();

        } else {
            previousButton.setEnabled(true);
            nextButton.setEnabled(true);

            tabLayout.getTabAt(2).select();
        }
    }
}
