//package com.enthusiast94.edinfit.utils;
//
//import android.app.AlertDialog;
//import android.content.Context;
//import android.content.DialogInterface;
//import android.support.annotation.NonNull;
//
//import com.enthusiast94.edinfit.R;
//import com.enthusiast94.edinfit.models.Stop;
//import com.enthusiast94.edinfit.models.User;
//import com.enthusiast94.edinfit.network.BaseService;
//import com.enthusiast94.edinfit.network.StopService;
//import com.enthusiast94.edinfit.network.UserService;
//
///**
// * Created by manas on 11-10-2015.
// */
//public class MoreStopOptionsDialog {
//
//    private Context context;
//    private Stop stop;
//    private ResponseListener responseListener;
//    private AlertDialog alertDialog;
//
//    public interface ResponseListener {
//        void onShowStopOnMopOptionSelected();
//        void onStopSaved(String error);
//        void onStopUnsaved(String error);
//    }
//
//    public MoreStopOptionsDialog(@NonNull Context context, @NonNull Stop stop,
//                                 @NonNull ResponseListener responseListener) {
//        this.context = context;
//        this.stop = stop;
//        this.responseListener = responseListener;
//
//        createDialog();
//    }
//
//    private void createDialog() {
//        String[] items;
//
//        User user = UserService.getInstance().getAuthenticatedUser();
//
//        final boolean shouldSave;
//
//        if (user.getSavedStops().contains(stop.getId())) {
//            shouldSave = false;
//
//            items = new String[]{
//                    context.getString(R.string.label_unsave),
//                    context.getString(R.string.label_show_on_map)
//            };
//        } else {
//            shouldSave = true;
//
//            items = new String[]{
//                    context.getString(R.string.label_save),
//                    context.getString(R.string.label_show_on_map)
//            };
//        }
//
//        alertDialog = new AlertDialog.Builder(context)
//                .setItems(items, new DialogInterface.OnClickListener() {
//
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        switch (which) {
//                            case 0:
//                                StopService.getInstance().saveOrUnsaveStop(stop.getId(), shouldSave, new BaseService.Callback<Void>() {
//
//                                    @Override
//                                    public void onSuccess(Void data) {
//                                        if (shouldSave) {
//                                            responseListener.onStopSaved(null);
//                                        } else {
//                                            responseListener.onStopUnsaved(null);
//                                        }
//                                    }
//
//                                    @Override
//                                    public void onFailure(String message) {
//                                        if (shouldSave) {
//                                            responseListener.onStopSaved(message);
//                                        } else {
//                                            responseListener.onStopSaved(message);
//                                        }
//                                    }
//                                });
//                                break;
//                            case 1:
//                                responseListener.onShowStopOnMopOptionSelected();
//                                break;
//                        }
//                    }
//                })
//                .create();
//    }
//
//    public void show() {
//        alertDialog.show();
//    }
//}
