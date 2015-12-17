package com.enthusiast94.edinfit.ui.search.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models.Service;
import com.enthusiast94.edinfit.models.Stop;
import com.enthusiast94.edinfit.network.BaseService;
import com.enthusiast94.edinfit.network.ServiceService;
import com.enthusiast94.edinfit.network.StopService;
import com.enthusiast94.edinfit.ui.search.events.OnSearchEvent;
import com.enthusiast94.edinfit.ui.service_info.activities.ServiceActivity;
import com.enthusiast94.edinfit.ui.stop_info.activities.StopActivity;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;

/**
 * Created by manas on 18-11-2015.
 */
public class SearchFragment extends Fragment {

    public static final String TAG = SearchFragment.class.getSimpleName();

    private ProgressBar progressBar;
    private View hintView;
    private TextView noResultsTextView;
    private RecyclerView searchResultsRecyclerView;
    private SearchAdapter searchAdapter;

    private List<Service> services;
    private List<Stop> stops;
    private String filter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        progressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
        hintView = view.findViewById(R.id.hint_view);
        noResultsTextView = (TextView) view.findViewById(R.id.no_results_textview);
        searchResultsRecyclerView = (RecyclerView) view.findViewById(R.id.search_results_recyclerview);

        searchAdapter = new SearchAdapter();
        searchResultsRecyclerView.setAdapter(searchAdapter);
        searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        if (services == null && stops == null) {
            loadAllStopsAndServices();
        } else {
            searchAdapter.notifyFilterChanged(filter);
        }

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
        filter = event.getFilter();
        searchAdapter.notifyFilterChanged(filter);
    }

    private void loadAllStopsAndServices() {
        progressBar.setVisibility(View.VISIBLE);
        searchResultsRecyclerView.setVisibility(View.GONE);

        ServiceService.getInstance().getServices(null, new BaseService.Callback<List<Service>>() {

            @Override
            public void onSuccess(List<Service> data) {
                services = data;

                    StopService.getInstance().getAllStops(new BaseService.Callback<List<Stop>>() {

                        @Override
                        public void onSuccess(List<Stop> data) {
                            stops = data;

                            if (getActivity() != null) {
                                searchAdapter.notifyFilterChanged(null);

                                progressBar.setVisibility(View.GONE);
                                searchResultsRecyclerView.setVisibility(View.VISIBLE);
                            }
                        }

                        @Override
                        public void onFailure(String message) {
                            if (getActivity() != null) {
                                Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();

                                progressBar.setVisibility(View.GONE);
                                searchResultsRecyclerView.setVisibility(View.VISIBLE);
                            }
                        }
                    });
            }

            @Override
            public void onFailure(String message) {
                if (getActivity() != null) {
                    Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();

                    progressBar.setVisibility(View.GONE);
                    searchResultsRecyclerView.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private class SearchAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int HEADING_VIEW_TYPE = 0;
        private static final int SERVICE_VIEW_TYPE = 1;
        private static final int STOP_VIEW_TYPE = 2;

        private LayoutInflater inflater;
        private List<Service> filteredServices;
        private List<Stop> filteredStops;

        public SearchAdapter() {
            inflater = LayoutInflater.from(getActivity());
            filteredServices = new ArrayList<>();
            filteredStops = new ArrayList<>();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == HEADING_VIEW_TYPE) {
                return new HeadingViewHolder(inflater.inflate(R.layout.row_heading_search, parent, false));
            } else if (viewType == SERVICE_VIEW_TYPE) {
                return new ServiceViewHolder(
                        inflater.inflate(R.layout.row_service_search_result, parent, false)
                );
            } else if (viewType == STOP_VIEW_TYPE) {
                return new StopViewHolder(
                        inflater.inflate(R.layout.row_stop_search_result, parent, false)
                );
            } else {
                throw new IllegalArgumentException("Invalid viewType: " + viewType);
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            Object item = getItem(position);

            if (holder instanceof HeadingViewHolder) {
                ((HeadingViewHolder) holder).bindItem((Pair) item);
            } else if (holder instanceof ServiceViewHolder) {
                ((ServiceViewHolder) holder).bindItem((Service) item);
            } else if (holder instanceof StopViewHolder) {
                ((StopViewHolder) holder).bindItem((Stop) item);
            } else {
                throw new IllegalArgumentException("Invalid holder type: " +
                        holder.getClass().getSimpleName());
            }
        }

        @Override
        public int getItemViewType(int position) {
            Object item = getItem(position);

            if (item instanceof Pair) {
                return HEADING_VIEW_TYPE;
            } else if (item instanceof Stop) {
                return STOP_VIEW_TYPE;
            } else if (item instanceof Service) {
                return SERVICE_VIEW_TYPE;
            } else {
                throw new IllegalArgumentException("Invalid position: " + position);
            }
        }

        private Object getItem(int position) {
            if (filteredServices.size() != 0 && filteredStops.size() != 0) {
                if (position == 0) {
                    return new Pair<>(getString(R.string.label_services), filteredServices.size());
                } else if (position > 0 && position < (filteredServices.size() + 1)) {
                    return filteredServices.get(position - 1);
                } else if (position == filteredServices.size() + 1) {
                    return new Pair<>(getString(R.string.label_stops), filteredStops.size());
                } else {
                    return filteredStops.get(position - (filteredServices.size() + 2));
                }
            } else if (filteredServices.size() != 0) {
                if (position == 0) {
                    return new Pair<>(getString(R.string.label_services), filteredServices.size());
                } else {
                    return filteredServices.get(position - 1);
                }
            } else {
                if (position == 0) {
                    return new Pair<>(getString(R.string.label_stops), filteredStops.size());
                } else {
                    return filteredStops.get(position - 1);
                }
            }
        }

        @Override
        public int getItemCount() {
            if (filteredServices.size() == 0 && filteredStops.size() == 0) {
                return 0;
            } else if (filteredServices.size() != 0 && filteredStops.size() != 0) {
                return filteredServices.size() + filteredStops.size() + 2;
            } else if (filteredServices.size() != 0) {
                return filteredServices.size() + 1;
            } else {
                return filteredStops.size() + 1;
            }
        }

        public void notifyFilterChanged(@Nullable String filter) {
            filteredServices.clear();
            filteredStops.clear();

            if (filter == null) {
                hintView.setVisibility(View.VISIBLE);
            } else {
                hintView.setVisibility(View.GONE);
                noResultsTextView.setVisibility(View.GONE);

                filter = filter.toLowerCase();

                for (Service service : services) {
                    if (service.getName().toLowerCase().contains(filter) ||
                            service.getDescription().toLowerCase().contains(filter)) {

                        filteredServices.add(service);
                    }
                }

                for (Stop stop : stops) {
                    if (stop.getName().toLowerCase().contains(filter)) {
                        filteredStops.add(stop);
                    }
                }

                if (filteredStops.size() == 0 && filteredServices.size() == 0) {
                    noResultsTextView.setVisibility(View.VISIBLE);
                }
            }

            notifyDataSetChanged();
        }

        private class ServiceViewHolder extends RecyclerView.ViewHolder
                implements View.OnClickListener {

            private TextView serviceNameTextView;
            private TextView destinationTextView;
            private Service service;

            public ServiceViewHolder(View itemView) {
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

        private class StopViewHolder extends RecyclerView.ViewHolder
                implements View.OnClickListener {

            private TextView stopNameTextView;
            private TextView stopDirectionTextView;
            private Stop stop;

            public StopViewHolder(View itemView) {
                super(itemView);

                // find views
                stopNameTextView = (TextView) itemView.findViewById(R.id.stop_name_textview);
                stopDirectionTextView = (TextView) itemView.findViewById(R.id.stop_direction_textview);

                // bind event listeners
                itemView.setOnClickListener(this);
            }

            public void bindItem(Stop stop) {
                this.stop = stop;

                stopNameTextView.setText(stop.getName());
                stopDirectionTextView.setText(stop.getDirection());
            }

            @Override
            public void onClick(View v) {
                Intent startActivityIntent = new Intent(getActivity(), StopActivity.class);
                startActivityIntent.putExtra(StopActivity.EXTRA_STOP_ID, stop.getId());
                startActivityIntent.putExtra(StopActivity.EXTRA_STOP_NAME, stop.getName());
                startActivity(startActivityIntent);
            }
        }

        private class HeadingViewHolder extends RecyclerView.ViewHolder {

            private TextView headingTextView;
            private TextView numResultsTextView;

            public HeadingViewHolder(View itemView) {
                super(itemView);

                // find views
                headingTextView = (TextView) itemView.findViewById(R.id.heading_textview);
                numResultsTextView = (TextView) itemView.findViewById(R.id.num_results_textview);
            }

            public void bindItem(Pair<String, Integer> pair) {
                headingTextView.setText(pair.first);
                if (pair.second == 1) {
                    numResultsTextView.setText(String.format(
                            getString(R.string.label_num_results_singular), pair.second));
                } else {
                    numResultsTextView.setText(String.format(
                            getString(R.string.label_num_results_plural), pair.second));
                }
            }
        }
    }
}
