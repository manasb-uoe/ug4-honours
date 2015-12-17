package com.enthusiast94.edinfit.ui.search.activities;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.ui.search.fragments.SearchFragment;
import com.enthusiast94.edinfit.ui.search.events.OnSearchEvent;
import com.enthusiast94.edinfit.ui.user_profile.fragments.UserProfileFragment;
import com.enthusiast94.edinfit.utils.Helpers;

import de.greenrobot.event.EventBus;

/**
 * Created by manas on 18-11-2015.
 */
public class SearchActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_search);

        /**
         * FInd views
         */

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        EditText searchEditText = (EditText) findViewById(R.id.search_edittext);

        /**
         * Setup AppBar
         */

        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(getString(R.string.label_find_a_bus));
        }

        /**
         * Setup a text changed listener on search edit text to filter service search results.
         * Also ensure that the keyboard is hidden if search button on keyboard is pressed.
         */

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                EventBus.getDefault().post(new OnSearchEvent(s.toString()));
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        searchEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    Helpers.hideSoftKeyboard(SearchActivity.this, v.getWindowToken());
                    return true;
                }

                return false;
            }
        });

        /**
         * Add user profile fragment if it hasn't already been added
         */

        if (getSupportFragmentManager().findFragmentByTag(UserProfileFragment.TAG) == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, new SearchFragment(), UserProfileFragment.TAG)
                    .commit();
        }
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
}
