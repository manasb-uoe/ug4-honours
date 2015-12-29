package com.enthusiast94.edinfit.ui.login_and_signup.fragments;

import android.app.ProgressDialog;
import android.content.Intent;
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
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;

import de.greenrobot.event.EventBus;

/**
 * Created by manas on 26-09-2015.
 */
public class LoginFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = LoginFragment.class.getSimpleName();
    private static final int RC_GOOGLE_SIGN_IN = 1;

    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private Button signupButton;
    private SignInButton googleLoginButton;

    private GoogleApiClient googleApiClient;
    private ProgressDialog progressDialog;
    private boolean isLoading;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(getString(R.string.google_oauth_server_client_id))
                .build();

        // Build a GoogleApiClient with access to the Google Sign-In API and the
        // options specified by gso.
        googleApiClient = new GoogleApiClient.Builder(getActivity())
                .enableAutoManage(getActivity(), null)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        emailEditText = (EditText) view.findViewById(R.id.email_edittext);
        passwordEditText = (EditText) view.findViewById(R.id.password_edittext);
        loginButton = (Button) view.findViewById(R.id.login_button);
        signupButton = (Button) view.findViewById(R.id.signup_button);
        googleLoginButton = (SignInButton) view.findViewById(R.id.google_login_button);

        loginButton.setOnClickListener(this);
        signupButton.setOnClickListener(this);
        googleLoginButton.setOnClickListener(this);

        progressDialog = new ProgressDialog(getActivity());
        progressDialog.setMessage(getString(R.string.label_please_waitt));
        progressDialog.setCancelable(false);

        // Start or stop loading based on the value of isLoading, which was retained on config
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

                    UserService.getInstance().authenticate(email, password, new UserCallback());
                }
                break;

            case R.id.signup_button:
                EventBus.getDefault().post(new ShowSignupFragmentEvent());
                break;

            case R.id.google_login_button:
                Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient);
                startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_GOOGLE_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);

            if (result.isSuccess()) {
                setLoading(true);

                UserService.getInstance().authenticateViaGoogle(
                        result.getSignInAccount().getIdToken(), new UserCallback());

            } else {
                Toast.makeText(getActivity(), getString(R.string.error_google_login),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class UserCallback implements BaseService.Callback<User> {

        @Override
        public void onSuccess(User data) {
            if (getActivity() != null) {
                setLoading(false);
            }

            EventBus.getDefault().post(new OnAuthenticatedEvent(data));
        }

        @Override
        public void onFailure(String message) {
            if (getActivity() != null) {
                setLoading(false);
            }

            Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
        }
    }
}
