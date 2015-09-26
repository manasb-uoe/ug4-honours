package com.enthusiast94.edinfit.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.events.AuthenticatedEvent;
import com.enthusiast94.edinfit.events.OnSignupResponseEvent;
import com.enthusiast94.edinfit.events.SignupEvent;
import com.enthusiast94.edinfit.utils.Helpers;

import butterknife.Bind;
import butterknife.BindString;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.greenrobot.event.EventBus;

/**
 * Created by manas on 26-09-2015.
 */
public class SignupFragment extends Fragment {

    @Bind(R.id.toolbar) Toolbar toolbar;
    @Bind(R.id.name_edittext) EditText nameEditText;
    @Bind(R.id.email_edittext) EditText emailEditText;
    @Bind(R.id.password_edittext) EditText passwordEditText;
    @Bind(R.id.confirm_password_edittext) EditText confirmPasswordEditText;
    @Bind(R.id.toolbar_circular_progress) ProgressBar toolarCircularProgress;
    @Bind(R.id.signup_button) Button signupButton;
    @BindString(R.string.error_passwords_do_not_match) String passwordsDoNotMatchError;
    @BindString(R.string.label_loading) String loadingLabel;
    @BindString(R.string.action_signup) String signupAction;
    private boolean isLoading;
    private static final String IS_LOADING_INSTANCE_STATE_KEY = "isLoadingInstanceStateKey";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_signup, container, false);
        ButterKnife.bind(this, view);

        /**
         * Setup toolbar
         */

        toolbar.setTitle(R.string.action_signup);
        toolbar.setNavigationIcon(R.drawable.ic_action_navigation_arrow_back);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });

        /**
         * Start or stop loading based on the value of isLoading, which is retrieved from instance
         * state
         */

        if (savedInstanceState != null) {
            isLoading = savedInstanceState.getBoolean(IS_LOADING_INSTANCE_STATE_KEY);
            setLoading(isLoading);
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(IS_LOADING_INSTANCE_STATE_KEY, isLoading);
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }

    private void setLoading(boolean isLoading) {
        SignupFragment.this.isLoading = isLoading;

        if (isLoading) {
            signupButton.setEnabled(false);
            signupButton.setText(loadingLabel);
            toolarCircularProgress.setVisibility(View.VISIBLE);
        } else {
            signupButton.setEnabled(true);
            signupButton.setText(signupAction);
            toolarCircularProgress.setVisibility(View.GONE);
        }
    }


    /**
     * UI event handlers.
     */

    @OnClick(R.id.signup_button)
    public void signup(Button signupButton) {
        Helpers.hideSoftKeyboard(getActivity(), signupButton.getWindowToken());

        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        String nameError = Helpers.validateName(name, getResources());
        String emailError = Helpers.validateEmail(email, getResources());
        String passwordError = Helpers.validatePassword(password, getResources());
        String confirmPasswordError = Helpers.validatePassword(password, getResources());

        if (nameError != null) {
            nameEditText.setError(nameError);
        }

        if (emailError != null) {
            emailEditText.setError(emailError);
        }

        if (passwordError != null) {
            passwordEditText.setError(passwordError);
        }

        if (confirmPasswordError != null) {
            confirmPasswordEditText.setError(confirmPasswordError);
        }

        boolean doPasswordsMatch = password.equals(confirmPassword);

        if (!doPasswordsMatch) {
            confirmPasswordEditText.setError(passwordsDoNotMatchError);
        }

        // if all input validations pass, initiate sign up
        if (nameError == null && emailError == null && passwordError == null && doPasswordsMatch) {
            setLoading(true);

            EventBus.getDefault().post(new SignupEvent(name, email, password));
        }
    }


    /**
     * EventBus event handling methods
     */

    public void onEventMainThread(OnSignupResponseEvent event) {
        setLoading(false);

        if (event.getError() == null) {
            EventBus.getDefault().post(new AuthenticatedEvent(event.getUser()));
        } else {
            Toast.makeText(getActivity(), event.getError(), Toast.LENGTH_SHORT).show();
        }
    }
}
