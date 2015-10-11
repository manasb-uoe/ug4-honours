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
import com.enthusiast94.edinfit.services.BaseService;
import com.enthusiast94.edinfit.services.UserService;
import com.enthusiast94.edinfit.utils.Helpers;

import de.greenrobot.event.EventBus;

/**
 * Created by manas on 26-09-2015.
 */
public class SignupFragment extends Fragment implements View.OnClickListener {

    private EditText nameEditText;
    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText confirmPasswordEditText;
    private Button signupButton;
    private boolean isLoading;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_signup, container, false);

        /**
         * Find Views
         */

        nameEditText = (EditText) view.findViewById(R.id.name_edittext);
        emailEditText = (EditText) view.findViewById(R.id.email_edittext);
        passwordEditText = (EditText) view.findViewById(R.id.password_edittext);
        confirmPasswordEditText = (EditText) view.findViewById(R.id.confirm_password_edittext);
        signupButton = (Button) view.findViewById(R.id.signup_button);

        /**
         * Bind event listeners
         */

        signupButton.setOnClickListener(this);

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

        nameEditText = null;
        emailEditText = null;
        passwordEditText = null;
        confirmPasswordEditText = null;
        signupButton = null;
    }

    private void setLoading(boolean isLoading) {
        SignupFragment.this.isLoading = isLoading;

        if (isLoading) {
            signupButton.setEnabled(false);
            signupButton.setText(getString(R.string.label_loading));
        } else {
            signupButton.setEnabled(true);
            signupButton.setText(getString(R.string.action_signup));
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.signup_button:
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
                    confirmPasswordEditText.setError(getString(R.string.error_passwords_do_not_match));
                }

                // if all input validations pass, initiate sign up
                if (nameError == null && emailError == null && passwordError == null && doPasswordsMatch) {
                    setLoading(true);

                    UserService.getInstance().createUser(name, email, password, new BaseService.Callback<User>() {

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
                break;
        }
    }
}
