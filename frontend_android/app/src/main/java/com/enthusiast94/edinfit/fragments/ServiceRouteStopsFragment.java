package com.enthusiast94.edinfit.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.events.StartStopActivityEvent;
import com.enthusiast94.edinfit.models.Service;
import com.enthusiast94.edinfit.models.Stop;
import com.enthusiast94.edinfit.network.Callback;
import com.enthusiast94.edinfit.network.ServiceService;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;

/**
 * Created by manas on 06-10-2015.
 */
public class ServiceRouteStopsFragment extends Fragment {

    public static final String EXTRA_SERVICE_NAME = "serviceName";
    private String serviceName;
    private Service service;
    private RecyclerView routeStopsRecyclerView;
    private RouteStopsAdapter routeStopsAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;

    public static ServiceRouteStopsFragment newInstance(String stopId) {
        ServiceRouteStopsFragment instance = new ServiceRouteStopsFragment();
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_SERVICE_NAME, stopId);
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
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_service_route_stops, container, false);

        /**
         * Find views
         */

        routeStopsRecyclerView = (RecyclerView) view.findViewById(R.id.route_stops_recyclerview);
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_layout);

        /**
         * Retrieve service name from arguments so that the data corresponding to its service
         * can be loaded
         */

        serviceName = getArguments().getString(EXTRA_SERVICE_NAME);

        /**
         * Setup swipe refresh layout
         */

        swipeRefreshLayout.setColorSchemeResources(R.color.accent);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {
                loadService();
            }
        });

        /**
         * Setup route stops recycler view
         */

        routeStopsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        routeStopsAdapter = new RouteStopsAdapter();
        routeStopsRecyclerView.setAdapter(routeStopsAdapter);

        /**
         * Load service if it hasn't already been loaded
         */

        if (service == null) {
            loadService();
        } else {
            routeStopsAdapter.notifyStopsChanged();
        }

        return view;
    }

    private void loadService() {
        setRefreshIndicatorVisiblity(true);

        ServiceService.getService(serviceName, new Callback<Service>() {

            @Override
            public void onSuccess(Service data) {
                service = data;

                Gson gson = new Gson();
                Log.i("json", gson.toJson(service.getRoutes().get(0).getStops()));

                if (getActivity() != null) {
                    setRefreshIndicatorVisiblity(false);

                    routeStopsAdapter.notifyStopsChanged();
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

    private void setRefreshIndicatorVisiblity(final boolean visiblity) {
        swipeRefreshLayout.post(new Runnable() {

            @Override
            public void run() {
                swipeRefreshLayout.setRefreshing(visiblity);
            }
        });
    }

    private class RouteStopsAdapter extends RecyclerView.Adapter {

        private LayoutInflater inflater;
        private List<Stop> stops = new ArrayList<>();

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (inflater == null) {
                inflater = LayoutInflater.from(getActivity());
            }

            return new RouteStopViewHolder(inflater.inflate(R.layout.row_route_stops, parent, false));
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            ((RouteStopViewHolder) holder).bindItem(stops.get(position));
        }

        @Override
        public int getItemCount() {
            return stops.size();
        }

        private void notifyStopsChanged() {
            stops.clear();

            stops.addAll(service.getRoutes().get(0).getStops());

            notifyDataSetChanged();
        }

        private class RouteStopViewHolder extends RecyclerView.ViewHolder
                implements View.OnClickListener {

            private TextView stopNameTextView;
            private Stop stop;

            public RouteStopViewHolder(View itemView) {
                super(itemView);

                // find views
                stopNameTextView = (TextView) itemView.findViewById(R.id.stop_name_textview);

                // bind event listeners
                itemView.setOnClickListener(this);
            }

            public void bindItem(Stop stop) {
                this.stop = stop;

                stopNameTextView.setText(stop.getName());
            }

            @Override
            public void onClick(View v) {
                if (stop != null) {
                    EventBus.getDefault().post(new StartStopActivityEvent(stop));
                }
            }
        }
    }
}
