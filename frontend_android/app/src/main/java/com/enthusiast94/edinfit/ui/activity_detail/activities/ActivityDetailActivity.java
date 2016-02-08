package com.enthusiast94.edinfit.ui.activity_detail.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models.Activity;
import com.enthusiast94.edinfit.ui.activity_detail.fragments.ActivityDetailFragment;
import com.enthusiast94.edinfit.utils.Helpers;

/**
 * Created by manas on 07-02-2016.
 */
public class ActivityDetailActivity extends AppCompatActivity {

    private static final String EXTRA_ACTIVITY_ID = "activityId";

    private ActionBar actionBar;
    private long activityId;

    public static void start(Context context, long activityId) {
        Intent intent = new Intent(context, ActivityDetailActivity.class);
        intent.putExtra(EXTRA_ACTIVITY_ID, activityId);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activity_detail);

        // setup app bar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(getString(R.string.activity_details));

        activityId = getIntent().getLongExtra(EXTRA_ACTIVITY_ID, -1);

        if (getSupportFragmentManager().findFragmentByTag(ActivityDetailFragment.TAG) == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, ActivityDetailFragment.newInstance(activityId),
                            ActivityDetailFragment.TAG)
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.action_delete) {
            AlertDialog alertDialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.confirmation)
                    .setMessage(R.string.confirm_delete_activity)
                    .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Activity activity = Activity.findById(activityId);
                            activity.delete();
                            Toast.makeText(ActivityDetailActivity.this, String.format(getString(R.string.activity_deleted_successfully_format),
                                    Helpers.getActivityTypeText(ActivityDetailActivity.this, activity.getType())), Toast.LENGTH_SHORT)
                                    .show();
                            dialog.dismiss();
                            finish();
                        }
                    })
                    .setNegativeButton(R.string.label_cancel, null)
                    .create();
            alertDialog.show();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_detail, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
