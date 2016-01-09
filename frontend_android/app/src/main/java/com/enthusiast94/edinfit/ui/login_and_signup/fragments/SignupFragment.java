package com.enthusiast94.edinfit.ui.login_and_signup.fragments;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.arasthel.asyncjob.AsyncJob;
import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models_2.User;
import com.enthusiast94.edinfit.network.BaseService;
import com.enthusiast94.edinfit.network.UserService;
import com.enthusiast94.edinfit.ui.login_and_signup.events.OnAuthenticatedEvent;
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

    private ProgressDialog progressDialog;
    private boolean isLoading;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_signup, container, false);

        nameEditText = (EditText) view.findViewById(R.id.name_edittext);
        emailEditText = (EditText) view.findViewById(R.id.email_edittext);
        passwordEditText = (EditText) view.findViewById(R.id.password_edittext);
        confirmPasswordEditText = (EditText) view.findViewById(R.id.confirm_password_edittext);
        signupButton = (Button) view.findViewById(R.id.signup_button);

        signupButton.setOnClickListener(this);

        progressDialog = new ProgressDialog(getActivity());
        progressDialog.setMessage(getString(R.string.label_please_waitt));
        progressDialog.setCancelable(false);

        // Start or stop loading based on the value of isLoading, which is retained on config
        // change.
        setLoading(isLoading);

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        progressDialog.dismiss();
    }

    private void setLoading(boolean isLoading) {
        this.isLoading = isLoading;

        if (isLoading) {
            progressDialog.show();

        } else {
            progressDialog.hide();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.signup_button:
                Helpers.hideSoftKeyboard(getActivity(), signupButton.getWindowToken());

                final String name = nameEditText.getText().toString().trim();
                final String email = emailEditText.getText().toString().trim();
                final String password = passwordEditText.getText().toString().trim();
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

                    new AsyncJob.AsyncJobBuilder<BaseService.Response<User>>()
                            .doInBackground(new AsyncJob.AsyncAction<BaseService.Response<User>>() {
                                @Override
                                public BaseService.Response<User> doAsync() {
                                    return UserService.getInstance().createUser(name, email, password);
                                }
                            })
                            .doWhenFinished(new AsyncJob.AsyncResultAction<BaseService.Response<User>>() {
                                @Override
                                public void onResult(BaseService.Response<User> response) {
                                    if (getActivity() == null) {
                                        return;
                                    }

                                    setLoading(false);

                                    if (!response.isSuccessfull()) {
                                        Toast.makeText(getActivity(), response.getError(),
                                                Toast.LENGTH_SHORT).show();
                                        return;
                                    }

                                    EventBus.getDefault().post(new OnAuthenticatedEvent(response.getBody()));
                                }
                            }).create().start();
                }
                break;
        }
    }
}
