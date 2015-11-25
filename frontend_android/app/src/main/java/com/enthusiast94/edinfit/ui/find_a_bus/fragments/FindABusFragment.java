package com.enthusiast94.edinfit.ui.find_a_bus.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models.Service;
import com.enthusiast94.edinfit.network.BaseService;
import com.enthusiast94.edinfit.network.ServiceService;
import com.enthusiast94.edinfit.ui.find_a_bus.events.OnSearchEvent;
import com.enthusiast94.edinfit.ui.service_info.activities.ServiceActivity;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;

/**
 * Created by manas on 18-11-2015.
 */
public class FindABusFragment extends Fragment {

    public static final String TAG = FindABusFragment.class.getSimpleName();


    private SwipeRefreshLayout swipeRefreshLayout;
    private SearchAdapter searchAdapter;

    private List<Service> services;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_find_a_bus, container, false);

        /**
         * Find views
         */

        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_layout);
        RecyclerView searchResultsRecyclerView = (RecyclerView) view.findViewById(R.id.search_results_recyclerview);

        /**
         * Setup search results recycler view
         */

        searchAdapter = new SearchAdapter();
        searchResultsRecyclerView.setAdapter(searchAdapter);
        searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        /**
         * Setup swipe refresh layout to reload all services on being pulled down. Also set its
         * color scheme to match accent color.
         */

        swipeRefreshLayout.setColorSchemeResources(R.color.accent);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {
                loadAllServices();
            }
        });

        loadAllServices();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        EventBus.getDefault().unregister(this);
    }

    public void onEventMainThread(OnSearchEvent event) {
        searchAdapter.notifyFilterChanged(event.getFilter());
    }

    private void setRefreshIndicatorVisiblity(final boolean visibility) {
        swipeRefreshLayout.post(new Runnable() {

            @Override
            public void run() {
                swipeRefreshLayout.setRefreshing(visibility);
            }
        });
    }

    private void loadAllServices() {
        setRefreshIndicatorVisiblity(true);

        ServiceService.getInstance().getServices(null, new BaseService.Callback<List<Service>>() {

            @Override
            public void onSuccess(List<Service> data) {
                services = data;

                if (getActivity() != null) {
                    // pass null filter so that all services are shown
                    searchAdapter.notifyFilterChanged(null);

                    setRefreshIndicatorVisiblity(false);
                }

            }

            @Override
            public void onFailure(String message) {
                if (getActivity() != null) {
                    Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();

                    setRefreshIndicatorVisiblity(false);
                }
            }
        });
    }

    private class SearchAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private LayoutInflater inflater;
        private List<Service> filteredServices;

        public SearchAdapter() {
            inflater = LayoutInflater.from(getActivity());
            filteredServices = new ArrayList<>();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new SearchResultViewHolder(
                    inflater.inflate(R.layout.row_service_search_result, parent, false)
            );
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            ((SearchResultViewHolder) holder).bindItem(filteredServices.get(position));
        }

        @Override
        public int getItemCount() {
            return filteredServices.size();
        }

        public void notifyFilterChanged(@Nullable String filter) {
            filteredServices.clear();

            if (filter == null) {
                filteredServices.addAll(services);
            } else {
                filter = filter.toLowerCase();

                for (Service service : services) {
                    if (service.getName().toLowerCase().contains(filter) ||
                            service.getDescription().toLowerCase().contains(filter)) {

                        filteredServices.add(service);
                    }
                }
            }

            notifyDataSetChanged();
        }

        private class SearchResultViewHolder extends RecyclerView.ViewHolder
                implements View.OnClickListener {

            private TextView serviceNameTextView;
            private TextView destinationTextView;
            private Service service;

            public SearchResultViewHolder(View itemView) {
                super(itemView);

                // find views
                serviceNameTextView = (TextView) itemView.findViewById(R.id.service_name_textview);
                destinationTextView = (TextView) itemView.findViewById(R.id.destination_textview);

                // bind event listeners
                itemView.setOnClickListener(this);
            }

            public void bindItem(Service service) {
                this.service = service;

                serviceNameTextView.setText(service.getName());
                destinationTextView.setText(service.getDescription());
            }

            @Override
            public void onClick(View v) {
                Intent startActivityIntent = new Intent(getActivity(), ServiceActivity.class);
                startActivityIntent.putExtra(ServiceActivity.EXTRA_SERVICE_NAME, service.getName());
                startActivity(startActivityIntent);
            }
        }
    }
}
