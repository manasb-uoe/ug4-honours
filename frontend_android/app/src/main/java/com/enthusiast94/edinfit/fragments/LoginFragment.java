package com.enthusiast94.edinfit.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.events.ShowSignupFragmentEvent;
import com.enthusiast94.edinfit.utils.Helpers;

import java.util.HashMap;
import java.util.Map;

import butterknife.Bind;
import butterknife.BindString;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.greenrobot.event.EventBus;

/**
 * Created by manas on 26-09-2015.
 */
public class LoginFragment extends Fragment {

    @Bind(R.id.email_edittext) EditText emailEditText;
    @Bind(R.id.password_edittext) EditText passwordEditText;
    @BindString(R.string.error_required_field) String errorRequiredField;
    @BindString(R.string.loggin_in) String loggingIn;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);
        ButterKnife.bind(this, view);

        return view;
    }

    @OnClick(R.id.login_button)
    public void login(Button loginButton) {
        Helpers.hideSoftKeyboard(getActivity(), loginButton.getWindowToken());

        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        String emailError = email.length() == 0 ? errorRequiredField : null;
        String passwordError = password.length() == 0 ? errorRequiredField : null;

        if (emailError != null) {
            emailEditText.setError(emailError);
        }

        if (passwordError != null) {
            passwordEditText.setError(passwordError);
        }

        // if both email and password are provided, initiate login
        if (emailError == null && passwordError == null) {
            Toast.makeText(getActivity(), loggingIn, Toast.LENGTH_SHORT).show();
        }
    }

    @OnClick(R.id.signup_button)
    public void showSignupFragment() {
        EventBus.getDefault().post(new ShowSignupFragmentEvent());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }
}
