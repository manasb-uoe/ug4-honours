package com.enthusiast94.edinfit.ui.wait_or_walk_mode.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.arasthel.asyncjob.AsyncJob;
import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models.Service;
import com.enthusiast94.edinfit.ui.service_info.activities.ServiceActivity;
import com.enthusiast94.edinfit.utils.ServiceView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by manas on 19-10-2015.
 */
public class SelectServiceFragment extends Fragment {

    public static final String TAG = SelectServiceFragment.class.getSimpleName();
    private static final String BUNDLE_SERVICE_NAMES = "bundleServiceNames";
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView servicesRecyclerView;
    private ServicesAdapter servicesAdapter;
    private int currentlySelectedServiceIndex = -1;
    private List<Service> services = new ArrayList<>();

    public static SelectServiceFragment getInstance(List<String> serviceNames) {
        SelectServiceFragment instance = new SelectServiceFragment();
        Bundle bundle = new Bundle();
        bundle.putStringArrayList(BUNDLE_SERVICE_NAMES, (ArrayList<String>) serviceNames);
        instance.setArguments(bundle);

        return instance;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_select_service, container, false);
        setHasOptionsMenu(true);

        servicesRecyclerView = (RecyclerView) view.findViewById(R.id.services_recyclerview);
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_layout);

        final List<String> serviceNames = getArguments().getStringArrayList(BUNDLE_SERVICE_NAMES);

        swipeRefreshLayout.setColorSchemeResources(R.color.accent);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {
                loadServices(serviceNames);
            }
        });

        servicesRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        servicesAdapter = new ServicesAdapter();
        servicesRecyclerView.setAdapter(servicesAdapter);

        if (services.size() == 0) {
            loadServices(serviceNames);
        } else {
            servicesAdapter.notifyServicesChanged();
        }

        return view;
    }

    private void setRefreshIndicatorVisiblity(final boolean visiblity) {
        swipeRefreshLayout.post(new Runnable() {

            @Override
            public void run() {
                swipeRefreshLayout.setRefreshing(visiblity);
            }
        });
    }

    private void loadServices(final List<String> serviceNames) {
        setRefreshIndicatorVisiblity(true);

        new AsyncJob.AsyncJobBuilder<List<Service>>()
                .doInBackground(new AsyncJob.AsyncAction<List<Service>>() {
                    @Override
                    public List<Service> doAsync() {
                        Log.d(TAG, Arrays.toString(serviceNames.toArray()));
                        return Service.findByNames(serviceNames);
                    }
                })
                .doWhenFinished(new AsyncJob.AsyncResultAction<List<Service>>() {
                    @Override
                    public void onResult(List<Service> services) {
                        if (getActivity() == null) {
                            return;
                        }

                        SelectServiceFragment.this.services = services;

                        setRefreshIndicatorVisiblity(false);

                        currentlySelectedServiceIndex = 0;
                        servicesAdapter.notifyServicesChanged();
                    }
                }).create().start();
    }

    public Service getSelectedService() {
        if (currentlySelectedServiceIndex == -1) {
            return null;
        } else {
            return services.get(currentlySelectedServiceIndex);
        }
    }

    private class ServicesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private LayoutInflater inflater;
        private int previouslySelectedServiceIndex;
        private static final int HEADING_VIEW_TYPE = 0;
        private static final int SERVICE_VIEW_TYPE = 1;

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (inflater == null) {
                inflater = LayoutInflater.from(getActivity());
            }

            if (viewType == HEADING_VIEW_TYPE) {
                return new HeadingViewHolder(inflater.inflate(R.layout.row_heading, parent, false));
            } else {
                return new ServiceViewHolder(inflater.inflate(R.layout.row_selection_service,
                        parent, false));
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (getItem(position) instanceof Service) {
                ((ServiceViewHolder) holder).bindItem((Service) getItem(position));
            }
        }

        @Override
        public int getItemCount() {
            if (services.size() == 0) {
                return 0;
            } else {
                return services.size() + 1 /* 1 heading */;
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0) {
                return HEADING_VIEW_TYPE;
            } else {
                return SERVICE_VIEW_TYPE;
            }
        }

        private Object getItem(int position) {
            int viewType = getItemViewType(position);

            if (viewType == HEADING_VIEW_TYPE) {
                return null;
            } else {
                return services.get(position - 1);
            }
        }

        public void notifyServicesChanged() {
            previouslySelectedServiceIndex = currentlySelectedServiceIndex;

            notifyDataSetChanged();
        }

        private class ServiceViewHolder extends RecyclerView.ViewHolder
                implements View.OnClickListener, View.OnLongClickListener {

            private Service service;
            private ServiceView serviceView;

            public ServiceViewHolder(View itemView) {
                super(itemView);

                serviceView = (ServiceView) itemView.findViewById(R.id.service_view);

                serviceView.setOnClickListener(this);
            }

            public void bindItem(Service service) {
                this.service = service;

                serviceView.bindItem(service);

                if (getAdapterPosition() - 1 == currentlySelectedServiceIndex) {
                    itemView.setBackgroundResource(R.color.green_selection);
                } else {
                    itemView.setBackgroundResource(android.R.color.white);
                }
            }

            @Override
            public void onClick(View v) {
                currentlySelectedServiceIndex = getAdapterPosition() - 1;

                notifyItemChanged(currentlySelectedServiceIndex + 1);
                notifyItemChanged(previouslySelectedServiceIndex + 1);

                previouslySelectedServiceIndex = currentlySelectedServiceIndex;
            }

            @Override
            public boolean onLongClick(View v) {
                v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);

                Intent startActivityIntent = new Intent(getActivity(), ServiceActivity.class);
                startActivityIntent.putExtra(ServiceActivity.EXTRA_SERVICE_NAME, service.getName());
                startActivity(startActivityIntent);

                return true;
            }
        }

        private class HeadingViewHolder extends RecyclerView.ViewHolder {

            private TextView headingTextView;

            public HeadingViewHolder(View itemView) {
                super(itemView);

                // find views
                headingTextView = (TextView) itemView.findViewById(R.id.heading_textview);

                // set heading
                headingTextView.setText(getString(R.string.label_which_service_are_you_going_to_use));
            }
        }
    }
}
