package com.enthusiast94.edinfit.ui.login_and_signup.fragments;

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
import com.enthusiast94.edinfit.models.User;
import com.enthusiast94.edinfit.network.BaseService;
import com.enthusiast94.edinfit.network.UserService;
import com.enthusiast94.edinfit.ui.login_and_signup.events.OnAuthenticatedEvent;
import com.enthusiast94.edinfit.ui.login_and_signup.events.ShowSignupFragmentEvent;
import com.enthusiast94.edinfit.utils.Helpers;

import de.greenrobot.event.EventBus;

/**
 * Created by manas on 26-09-2015.
 */
public class LoginFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = LoginFragment.class.getSimpleName();
    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private Button signupButton;
    private boolean isLoading;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        /**
         * Find views
         */

        emailEditText = (EditText) view.findViewById(R.id.email_edittext);
        passwordEditText = (EditText) view.findViewById(R.id.password_edittext);
        loginButton = (Button) view.findViewById(R.id.login_button);
        signupButton = (Button) view.findViewById(R.id.signup_button);

        /**
         * Bind event listeners
         */

        loginButton.setOnClickListener(this);
        signupButton.setOnClickListener(this);


        /**
         * Start or stop loading based on the value of isLoading, which was retained on config
         * change.
         */

        setLoading(isLoading);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        emailEditText = null;
        passwordEditText = null;
        loginButton = null;
        signupButton = null;
    }

    private void setLoading(boolean isLoading) {
        LoginFragment.this.isLoading = isLoading;

        if (isLoading) {
            loginButton.setText(getString(R.string.label_loading));
            loginButton.setEnabled(false);
        } else {
            loginButton.setText(getString(R.string.action_login));
            loginButton.setEnabled(true);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.login_button:
                Helpers.hideSoftKeyboard(getActivity(), loginButton.getWindowToken());

                String email = emailEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();

                String requiredFieldErrorMessage = getString(R.string.error_required_field);

                String emailError = email.length() == 0 ? requiredFieldErrorMessage : null;
                String passwordError = password.length() == 0 ? requiredFieldErrorMessage : null;

                if (emailError != null) {
                    emailEditText.setError(emailError);
                }

                if (passwordError != null) {
                    passwordEditText.setError(passwordError);
                }

                // if both email and password are provided, initiate login
                if (emailError == null && passwordError == null) {
                    setLoading(true);

                    UserService.getInstance().authenticate(email, password, new BaseService.Callback<User>() {

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

            case R.id.signup_button:
                EventBus.getDefault().post(new ShowSignupFragmentEvent());
                break;
        }
    }
}
