//package com.enthusiast94.edinfit.ui.service_info.activities;
//
//import android.content.Intent;
//import android.os.Bundle;
//import android.support.v7.app.ActionBar;
//import android.support.v7.app.AppCompatActivity;
//import android.support.v7.widget.Toolbar;
//import android.view.MenuItem;
//
//import com.enthusiast94.edinfit.R;
//import com.enthusiast94.edinfit.ui.service_info.fragments.ServiceFragment;
//
///**
// * Created by manas on 07-10-2015.
// */
//public class ServiceActivity extends AppCompatActivity {
//
//    private Toolbar toolbar;
//    public static final String EXTRA_SERVICE_NAME = "serviceName";
//    private String serviceName;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_service);
//
//        /**
//         * Find views
//         */
//
//        toolbar = (Toolbar) findViewById(R.id.toolbar);
//
//        /**
//         * Retrieve service name from intent, so that the corresponding service's data can be
//         * fetched from the server
//         */
//
//        Intent intent = getIntent();
//        serviceName = intent.getStringExtra(EXTRA_SERVICE_NAME);
//
//        /**
//         * Setup app bar
//         */
//
//        setSupportActionBar(toolbar);
//        ActionBar appBar = getSupportActionBar();
//        if (appBar != null) {
//            appBar.setHomeButtonEnabled(true);
//            appBar.setDisplayHomeAsUpEnabled(true);
//            appBar.setTitle(serviceName);
//        }
//
//        /**
//         * Add service fragment
//         */
//
//        if (getSupportFragmentManager().findFragmentByTag(ServiceFragment.TAG) == null) {
//            getSupportFragmentManager().beginTransaction()
//                    .add(R.id.fragment_container_framelayout, ServiceFragment.newInstance(serviceName), ServiceFragment.TAG)
//                    .commit();
//        }
//
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case android.R.id.home:
//                finish();
//                return true;
//            default:
//                return super.onOptionsItemSelected(item);
//        }
//    }
//
//}
