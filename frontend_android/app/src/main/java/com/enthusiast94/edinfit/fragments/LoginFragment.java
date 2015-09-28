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
import com.enthusiast94.edinfit.events.ShowSignupFragmentEvent;
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
public class LoginFragment extends Fragment {

    private static final String TAG = LoginFragment.class.getSimpleName();
    @Bind(R.id.email_edittext) EditText emailEditText;
    @Bind(R.id.password_edittext) EditText passwordEditText;
    @Bind(R.id.login_button) Button loginButton;
    @BindString(R.string.error_required_field) String requiredFieldError;
    @BindString(R.string.label_loading) String loadingLabel;
    @BindString(R.string.action_login) String loginAction;
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
        ButterKnife.bind(this, view);


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
        ButterKnife.unbind(this);
    }

    private void setLoading(boolean isLoading) {
        LoginFragment.this.isLoading = isLoading;

        if (isLoading) {
            loginButton.setText(loadingLabel);
            loginButton.setEnabled(false);
        } else {
            loginButton.setText(loginAction);
            loginButton.setEnabled(true);
        }
    }


    /**
     * UI event handlers
     */

    @OnClick(R.id.login_button)
    public void login(Button loginButton) {
        Helpers.hideSoftKeyboard(getActivity(), loginButton.getWindowToken());

        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        String emailError = email.length() == 0 ? requiredFieldError : null;
        String passwordError = password.length() == 0 ? requiredFieldError : null;

        if (emailError != null) {
            emailEditText.setError(emailError);
        }

        if (passwordError != null) {
            passwordEditText.setError(passwordError);
        }

        // if both email and password are provided, initiate login
        if (emailError == null && passwordError == null) {
            setLoading(true);

            UserService.authenticate(email, password, new Callback<User>() {

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

    @OnClick(R.id.signup_button)
    public void showSignupFragment() {
        EventBus.getDefault().post(new ShowSignupFragmentEvent());
    }
}
