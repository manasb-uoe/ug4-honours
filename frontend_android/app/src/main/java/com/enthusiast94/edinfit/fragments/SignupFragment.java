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
import android.widget.Toast;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.utils.Helpers;

import butterknife.Bind;
import butterknife.BindString;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by manas on 26-09-2015.
 */
public class SignupFragment extends Fragment {

    @Bind(R.id.toolbar) Toolbar toolbar;
    @Bind(R.id.name_edittext) EditText nameEditText;
    @Bind(R.id.email_edittext) EditText emailEditText;
    @Bind(R.id.password_edittext) EditText passwordEditText;
    @Bind(R.id.confirm_password_edittext) EditText confirmPasswordEditText;
    @BindString(R.string.error_passwords_do_not_match) String errorPasswordsDoNotMatch;
    @BindString(R.string.signing_up) String signingUp;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_signup, container, false);
        ButterKnife.bind(this, view);

        /**
         * Setup toolbar
         */

        toolbar.setTitle(R.string.label_signup_for_appname);
        toolbar.setNavigationIcon(R.drawable.ic_action_navigation_arrow_back);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });

        return view;
    }

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
            confirmPasswordEditText.setError(errorPasswordsDoNotMatch);
        }

        // if all input validations pass, initiate sign up
        if (nameError == null && emailError == null && passwordError == null && doPasswordsMatch) {
            Toast.makeText(getActivity(), signingUp, Toast.LENGTH_SHORT).show();
        }
    }

        @Override
        public void onDestroyView() {
            super.onDestroyView();
            ButterKnife.unbind(this);
        }
    }
