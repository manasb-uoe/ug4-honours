package com.enthusiast94.edinfit.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models.Service;

/**
 * Created by manas on 04-01-2016.
 */
public class ServiceView extends LinearLayout {

    private TextView serviceNameTextView;
    private TextView descriptionTextView;

    public ServiceView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);

        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.view_service, this, true);

        serviceNameTextView = (TextView) view.findViewById(R.id.service_name_textview);
        descriptionTextView = (TextView) view.findViewById(R.id.description_textview);
    }

    public void bindItem(Service service) {
        serviceNameTextView.setText(service.getName());
        descriptionTextView.setText(service.getDescription());
    }
}
