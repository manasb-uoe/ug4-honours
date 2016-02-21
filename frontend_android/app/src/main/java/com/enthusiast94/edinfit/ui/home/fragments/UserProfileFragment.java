package com.enthusiast94.edinfit.ui.home.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.arasthel.asyncjob.AsyncJob;
import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models.User;
import com.enthusiast94.edinfit.network.BaseService;
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
    private View editProfileContainer;
    private User user;

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
        editProfileContainer = view.findViewById(R.id.edit_profile_container);

        user = UserService.getInstance().getAuthenticatedUser();

        // bind event listeners
        View.OnClickListener onClickListener = new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                int id = v.getId();

                if (id == logoutButton.getId()) {
                    logout();
                } else if (id == editProfileContainer.getId()) {
                    showEditProfileDialog();
                }
            }
        };

        logoutButton.setOnClickListener(onClickListener);
        editProfileContainer.setOnClickListener(onClickListener);

        populateUserInfo();

        return view;
    }

    private void populateUserInfo() {
        nameTextView.setText(user.getName());
        emailTextView.setText(user.getEmail());
        memberSinceTextView.setText(String.format(getString(R.string.label_member_since_format),
                new SimpleDateFormat("dd MMM yyyy", Locale.UK).format(new Date(user.getCreatedAt()))));
    }

    private void showEditProfileDialog() {
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_edit_profile, null);
        final TextInputLayout nameTextInputLayout = (TextInputLayout) view.findViewById(R.id.name_text_input_layout);
        TextInputLayout weightTextInputLayout = (TextInputLayout) view.findViewById(R.id.weight_text_input_layout);
        final EditText nameEditText = (EditText) view.findViewById(R.id.name_edittext);
        final EditText weightEditText = (EditText) view.findViewById(R.id.weight_edittext);

        nameEditText.setText(user.getName());
        weightEditText.setText(user.getWeight() != 0 ? String.valueOf(user.getWeight()) : "");

        nameEditText.setText(user.getName());
        weightEditText.setText(String.valueOf(user.getWeight()));

        AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.edit_profile)
                .setView(view)
                .setPositiveButton(R.string.label_save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final String name = nameEditText.getText().toString();
                        final int weight = weightEditText.getText().toString().length() == 0 ? 0 : Integer.valueOf(weightEditText.getText().toString());

                        if (name.length() == 0) {
                            Toast.makeText(getActivity(), R.string.name_cannot_be_left_blank,
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        user.setName(name);
                        user.setWeight(weight);

                        new AsyncJob.AsyncJobBuilder<BaseService.Response<Void>>()
                                .doInBackground(new AsyncJob.AsyncAction<BaseService.Response<Void>>() {
                                    @Override
                                    public BaseService.Response<Void> doAsync() {
                                        return UserService.getInstance().updateUser(user);
                                    }
                                })
                                .doWhenFinished(new AsyncJob.AsyncResultAction<BaseService.Response<Void>>() {
                                    @Override
                                    public void onResult(BaseService.Response<Void> response) {
                                        if (getActivity() == null) {
                                            return;
                                        }

                                        if (!response.isSuccessfull()) {
                                            Toast.makeText(getActivity(), response.getError(),
                                                    Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(getActivity(), R.string.user_profile_updated_successfully,
                                                    Toast.LENGTH_SHORT).show();
                                        }

                                        user = UserService.getInstance().getAuthenticatedUser();
                                        populateUserInfo();
                                    }
                                }).create().start();
                    }
                })
                .setNegativeButton(R.string.label_cancel, null)
                .create();
        alertDialog.show();
    }

    private void logout() {
        UserService.getInstance().deauthenticate();
        EventBus.getDefault().post(new OnDeauthenticatedEvent());
    }
}
