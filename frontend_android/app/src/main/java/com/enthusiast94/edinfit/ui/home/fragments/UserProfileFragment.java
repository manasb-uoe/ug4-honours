package com.enthusiast94.edinfit.ui.home.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models.User;
import com.enthusiast94.edinfit.network.UserService;
import com.enthusiast94.edinfit.ui.home.events.OnDeauthenticatedEvent;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.greenrobot.event.EventBus;

/**
 * Created by manas on 03-10-2015.
 */
public class UserProfileFragment extends Fragment {

    public static final String TAG = UserProfileFragment.class.getSimpleName();
    private TextView nameTextView;
    private TextView emailTextView;
    private TextView memberSinceTextView;
    private Button logoutButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_profile, container, false);

        // find views
        nameTextView = (TextView) view.findViewById(R.id.name_textview);
        emailTextView = (TextView) view.findViewById(R.id.email_textview);
        memberSinceTextView = (TextView) view.findViewById(R.id.member_since_textview);
        logoutButton = (Button) view.findViewById(R.id.logout_button);

        logoutButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                logout();
            }
        });

        loadUserData();

        return view;
    }

    private void loadUserData() {
        User user = UserService.getInstance().getAuthenticatedUser();

        if (user != null) {
            nameTextView.setText(user.getName());
            emailTextView.setText(user.getEmail());
            memberSinceTextView.setText(new SimpleDateFormat("dd MMM yyyy", Locale.UK)
                    .format(new Date(user.getCreatedAt())));
        }
    }

    private void logout() {
        UserService.getInstance().deauthenticate();
        EventBus.getDefault().post(new OnDeauthenticatedEvent());
    }
}
