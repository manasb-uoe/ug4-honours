package com.enthusiast94.edinfit.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.events.ShowSignupFragmentEvent;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.greenrobot.event.EventBus;

/**
 * Created by manas on 26-09-2015.
 */
public class LoginFragment extends Fragment {

    @Bind(R.id.email_edittext) EditText emailEditText;
    @Bind(R.id.password_edittext) EditText passwordEditText;
    @Bind(R.id.signup_textview) TextView signupTextView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);
        ButterKnife.bind(this, view);

        return view;
    }

    @OnClick(R.id.login_button)
    public void login() {
        Toast.makeText(getActivity(), "Login", Toast.LENGTH_SHORT).show();
    }

    @OnClick(R.id.signup_textview)
    public void showSignupFragment() {
        EventBus.getDefault().post(new ShowSignupFragmentEvent());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }
}
