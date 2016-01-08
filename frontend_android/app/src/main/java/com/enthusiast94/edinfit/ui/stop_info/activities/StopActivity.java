//package com.enthusiast94.edinfit.ui.stop_info.activities;
//
//import android.os.Bundle;
//import android.support.v7.app.ActionBar;
//import android.support.v7.app.AppCompatActivity;
//import android.support.v7.widget.Toolbar;
//import android.view.MenuItem;
//
//import com.enthusiast94.edinfit.R;
//import com.enthusiast94.edinfit.models.Stop;
//import com.enthusiast94.edinfit.ui.service_info.fragments.ServiceFragment;
//import com.enthusiast94.edinfit.ui.stop_info.fragments.StopFragment;
//
///**
// * Created by manas on 04-10-2015.
// */
//public class StopActivity extends AppCompatActivity {
//
//    public static final String EXTRA_STOP = "stop";
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_stop);
//
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//
//        setSupportActionBar(toolbar);
//        ActionBar appBar = getSupportActionBar();
//        if (appBar != null) {
//            appBar.setHomeButtonEnabled(true);
//            appBar.setDisplayHomeAsUpEnabled(true);
//            appBar.setTitle(getString(R.string.label_departure_times));
//        }
//
//        Stop stop = getIntent().getParcelableExtra(EXTRA_STOP);
//
//        if (getSupportFragmentManager().findFragmentByTag(ServiceFragment.TAG) == null) {
//            getSupportFragmentManager().beginTransaction()
//                    .add(R.id.fragment_container_framelayout, StopFragment.newInstance(stop), ServiceFragment.TAG)
//                    .commit();
//        }
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
//}
