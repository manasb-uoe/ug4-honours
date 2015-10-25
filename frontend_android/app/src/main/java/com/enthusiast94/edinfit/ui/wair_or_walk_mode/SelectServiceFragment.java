package com.enthusiast94.edinfit.ui.wair_or_walk_mode;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.ui.service_info.ServiceActivity;
import com.enthusiast94.edinfit.models.Service;
import com.enthusiast94.edinfit.services.BaseService;
import com.enthusiast94.edinfit.services.ServiceService;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;

/**
 * Created by manas on 19-10-2015.
 */
public class SelectServiceFragment extends Fragment {

    public static final String TAG = SelectOriginStopFragment.class.getSimpleName();
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

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_select_service, container, false);
        setHasOptionsMenu(true);

        /**
         * Find views
         */

        servicesRecyclerView = (RecyclerView) view.findViewById(R.id.services_recyclerview);
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_layout);
        Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        View actionDone = toolbar.findViewById(R.id.action_done);

        /**
         * Setup toolbar
         */

        toolbar.setTitle(getString(R.string.action_select_service));
        toolbar.setNavigationIcon(R.drawable.ic_action_navigation_arrow_back);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });

        actionDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EventBus.getDefault().post(new OnServiceSelectedEvent(
                        services.get(currentlySelectedServiceIndex)));
                getActivity().onBackPressed();
            }
        });

        /**
         * Get service names from arguments so that the corresponding services can be fetched
         * from the server
         */

        final List<String> serviceNames = getArguments().getStringArrayList(BUNDLE_SERVICE_NAMES);

        /**
         * Setup swipe refresh layout
         */

        swipeRefreshLayout.setColorSchemeResources(R.color.accent);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {
                loadServices(serviceNames);
            }
        });

        /**
         * Setup services recycler view
         */

        servicesRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        servicesAdapter = new ServicesAdapter();
        servicesRecyclerView.setAdapter(servicesAdapter);

        /**
         * Load services from network
         */

        loadServices(serviceNames);

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

    private void loadServices(List<String> serviceNames) {
        setRefreshIndicatorVisiblity(true);

        ServiceService.getInstance().getServices(serviceNames, new BaseService.Callback<List<Service>>() {

            @Override
            public void onSuccess(List<Service> data) {
                services = data;

                if (getActivity() != null) {
                    setRefreshIndicatorVisiblity(false);

                    servicesAdapter.notifyServicesChanged();
                }
            }

            @Override
            public void onFailure(String message) {
                if (getActivity() != null) {
                    setRefreshIndicatorVisiblity(false);

                    Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private class ServicesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private LayoutInflater inflater;
        private int previouslySelectedServiceIndex;
        private static final int HINT_VIEW_TYPE = 0;
        private static final int HEADING_VIEW_TYPE = 1;
        private static final int SERVICE_VIEW_TYPE = 2;

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (inflater == null) {
                inflater = LayoutInflater.from(getActivity());
            }

            if (viewType == HINT_VIEW_TYPE) {
                return new HintViewHolder(inflater.inflate(R.layout.row_hint,
                        parent, false));
            } else if (viewType == HEADING_VIEW_TYPE) {
                return new HeadingViewHolder(inflater.inflate(R.layout.row_heading, parent, false));
            } else {
                return new SelectionServiceViewHolder(inflater.inflate(R.layout.row_selection_service,
                        parent, false));
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (getItem(position) instanceof Service) {
                ((SelectionServiceViewHolder) holder).bindItem((Service) getItem(position));
            }
        }

        @Override
        public int getItemCount() {
            if (services.size() == 0) {
                return 0;
            } else {
                return services.size() + 2 /* 1 hint + 1 heading */;
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0) {
                return HINT_VIEW_TYPE;
            } else if (position == 1) {
                return HEADING_VIEW_TYPE;
            } else {
                return SERVICE_VIEW_TYPE;
            }
        }

        private Object getItem(int position) {
            int viewType = getItemViewType(position);

            if (viewType == HINT_VIEW_TYPE || viewType == HEADING_VIEW_TYPE) {
                return null;
            } else {
                return services.get(position - 2);
            }
        }

        public void notifyServicesChanged() {
            currentlySelectedServiceIndex = 0;
            previouslySelectedServiceIndex = 0;

            notifyDataSetChanged();
        }

        private class SelectionServiceViewHolder extends RecyclerView.ViewHolder
                implements View.OnClickListener, View.OnLongClickListener {

            private TextView serviceNameTextView;
            private TextView destinationTextView;
            private Service service;

            public SelectionServiceViewHolder(View itemView) {
                super(itemView);

                // find views
                serviceNameTextView = (TextView) itemView.findViewById(R.id.service_name_textview);
                destinationTextView = (TextView) itemView.findViewById(R.id.destination_textview);

                // bind event listeners
                itemView.setOnClickListener(this);
                itemView.setOnLongClickListener(this);
            }

            public void bindItem(Service service) {
                this.service = service;

                serviceNameTextView.setText(service.getName());
                destinationTextView.setText(service.getDescription());

                if (getAdapterPosition() - 2 == currentlySelectedServiceIndex) {
                    itemView.setBackgroundResource(R.color.green_selection);
                } else {
                    itemView.setBackgroundResource(android.R.color.transparent);
                }
            }

            @Override
            public void onClick(View v) {
                currentlySelectedServiceIndex = getAdapterPosition() - 2;

                notifyItemChanged(currentlySelectedServiceIndex + 2);
                notifyItemChanged(previouslySelectedServiceIndex + 2);

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

        private class HintViewHolder extends RecyclerView.ViewHolder {

            private TextView hintTextView;

            public HintViewHolder(View itemView) {
                super(itemView);

                // find views
                hintTextView = (TextView) itemView.findViewById(R.id.hint_textview);

                // set hint
                hintTextView.setText(getString(R.string.label_hint_long_click_service_for_more_info));
            }
        }
    }
}
