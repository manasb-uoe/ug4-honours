package com.enthusiast94.edinfit.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.enthusiast94.edinfit.events.LoginEvent;
import com.enthusiast94.edinfit.events.OnLoginResponseEvent;
import com.enthusiast94.edinfit.events.OnSignupResponseEvent;
import com.enthusiast94.edinfit.events.SignupEvent;
import com.enthusiast94.edinfit.models.User;
import com.enthusiast94.edinfit.network.UserService;
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


    /**
     * EventBus event handling methods
     */

    public void onEventMainThread(LoginEvent event) {
        UserService.authenticate(event.getEmail(), event.getPassword(), new Callback<User>() {

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

    public void onEventMainThread(SignupEvent event) {
        UserService.createUser(event.getName(), event.getEmail(), event.getPassword(),
                new Callback<User>() {

            @Override
            public void onSuccess(User user) {
                EventBus.getDefault().post(new OnSignupResponseEvent(null, user));
            }

            @Override
            public void onFailure(String message) {
                EventBus.getDefault().post(new OnSignupResponseEvent(message, null));
            }
        });
    }
}
