package com.enthusiast94.edinfit.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.events.OnAuthenticatedEvent;
import com.enthusiast94.edinfit.models.User;
import com.enthusiast94.edinfit.network.Callback;
import com.enthusiast94.edinfit.network.UserService;
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

    @Bind(R.id.name_edittext) EditText nameEditText;
    @Bind(R.id.email_edittext) EditText emailEditText;
    @Bind(R.id.password_edittext) EditText passwordEditText;
    @Bind(R.id.confirm_password_edittext) EditText confirmPasswordEditText;
    @Bind(R.id.signup_button) Button signupButton;
    @BindString(R.string.error_passwords_do_not_match) String passwordsDoNotMatchError;
    @BindString(R.string.label_loading) String loadingLabel;
    @BindString(R.string.action_signup) String signupAction;
    private boolean isLoading;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_signup, container, false);
        ButterKnife.bind(this, view);

        /**
         * Start or stop loading based on the value of isLoading, which is retained on config
         * change.
         */

        setLoading(isLoading);

        return view;
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
        } else {
            signupButton.setEnabled(true);
            signupButton.setText(signupAction);
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

            UserService.createUser(name, email, password, new Callback<User>() {

                @Override
                public void onSuccess(User data) {
                    setLoading(false);

                    EventBus.getDefault().post(new OnAuthenticatedEvent(data));
                }

                @Override
                public void onFailure(String message) {
                    setLoading(false);

                    Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
