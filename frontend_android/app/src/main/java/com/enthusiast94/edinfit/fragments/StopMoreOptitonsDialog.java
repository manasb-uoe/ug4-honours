package com.enthusiast94.edinfit.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models.Stop;

/**
 * Created by manas on 11-10-2015.
 */
public abstract class StopMoreOptitonsDialog extends DialogFragment {

    private Stop stop;

    public StopMoreOptitonsDialog(Stop stop) {
        this.stop = stop;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String[] items = new String[]{
                getActivity().getString(R.string.label_save),
                getActivity().getString(R.string.label_show_on_map)
        };

        AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setItems(items, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                // TODO save/unsave stop
                                break;
                            case 1:
                                onShowStopOnMopOptionSelected();
                                break;
                        }
                    }
                })
                .create();

        return dialog;
    }

    public abstract void onShowStopOnMopOptionSelected();
}
