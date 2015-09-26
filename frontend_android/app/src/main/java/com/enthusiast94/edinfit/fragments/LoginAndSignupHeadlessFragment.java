package com.enthusiast94.edinfit.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.enthusiast94.edinfit.events.LoginEvent;
import com.enthusiast94.edinfit.events.OnLoginResponseEvent;
import com.enthusiast94.edinfit.models.User;
import com.enthusiast94.edinfit.network.AuthenticationService;
import com.enthusiast94.edinfit.network.Callback;

import de.greenrobot.event.EventBus;

/**
 * Created by manas on 26-09-2015.
 */
public class LoginAndSignupHeadlessFragment extends Fragment {

    public static final String TAG = LoginAndSignupHeadlessFragment.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    private void login(String email, String password) {
        AuthenticationService.authenticate(email, password, new Callback<User>() {

            @Override
            public void onSuccess(User user) {
                EventBus.getDefault().post(new OnLoginResponseEvent(null, user));
            }

            @Override
            public void onFailure(String message) {
                EventBus.getDefault().post(new OnLoginResponseEvent(message, null));
            }
        });
    }


    /**
     * EventBus event handling methods
     */

    public void onEventMainThread(LoginEvent event) {
        Log.i(TAG, "login event");
        login(event.getEmail(), event.getPassword());
    }
}
