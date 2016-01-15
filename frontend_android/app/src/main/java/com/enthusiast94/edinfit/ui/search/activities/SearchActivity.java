package com.enthusiast94.edinfit.ui.search.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.ui.search.fragments.SearchFragment;

/**
 * Created by manas on 10-01-2016.
 */
public class SearchActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        if (getSupportFragmentManager().findFragmentByTag(SearchFragment.TAG) == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, new SearchFragment(), SearchFragment.TAG)
                    .commit();
        }
    }
}
