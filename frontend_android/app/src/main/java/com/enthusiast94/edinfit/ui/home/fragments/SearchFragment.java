package com.enthusiast94.edinfit.ui.home.fragments;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.arasthel.asyncjob.AsyncJob;
import com.cocosw.bottomsheet.BottomSheet;
import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models_2.FavouriteStop;
import com.enthusiast94.edinfit.models_2.Service;
import com.enthusiast94.edinfit.models_2.Stop;
import com.enthusiast94.edinfit.network.BaseService;
import com.enthusiast94.edinfit.network.UserService;
import com.enthusiast94.edinfit.ui.stop_info.activities.StopActivity;
import com.enthusiast94.edinfit.utils.Helpers;
import com.enthusiast94.edinfit.utils.ServiceView;
import com.enthusiast94.edinfit.utils.StopView;
import com.enthusiast94.edinfit.utils.Triplet;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
    private EditText searchEditText;

    private List<Service> services;
    private List<Stop> stops;
    private String filter;
    private List<FavouriteStop> favouriteStops;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        searchEditText = (EditText) view.findViewById(R.id.search_edittext);
        progressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
        hintView = view.findViewById(R.id.hint_view);
        noResultsTextView = (TextView) view.findViewById(R.id.no_results_textview);
        searchResultsRecyclerView = (RecyclerView) view.findViewById(R.id.search_results_recyclerview);

        // enable toolbar back navigation
        toolbar.setNavigationIcon(R.drawable.ic_action_navigation_arrow_back);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });

        // setup recycler view
        searchAdapter = new SearchAdapter();
        searchResultsRecyclerView.setAdapter(searchAdapter);
        searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        // Setup a text changed listener on search edit text to filter service search results.
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                onSearch(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Also ensure that the keyboard is hidden if search button on keyboard is pressed.
        searchEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    Helpers.hideSoftKeyboard(getActivity(), v.getWindowToken());
                    return true;
                }

                return false;
            }
        });


        if (services == null && stops == null) {
            loadAllStopsAndServices();
        } else {
            searchAdapter.notifyFilterChanged(filter);
        }

        return view;
    }

    private void onSearch(String filter) {
        this.filter = filter;
        searchAdapter.notifyFilterChanged(filter);
    }

    private void loadAllStopsAndServices() {
        progressBar.setVisibility(View.VISIBLE);
        searchResultsRecyclerView.setVisibility(View.GONE);

        new AsyncJob.AsyncJobBuilder<BaseService.Response<Triplet<List<Stop>, List<Service>, List<FavouriteStop>>>>()
                .doInBackground(new AsyncJob.AsyncAction<BaseService.Response<Triplet<List<Stop>, List<Service>, List<FavouriteStop>>>>() {
                    @Override
                    public BaseService.Response<Triplet<List<Stop>, List<Service>, List<FavouriteStop>>> doAsync() {
                        BaseService.Response<Triplet<List<Stop>, List<Service>, List<FavouriteStop>>> response =
                                new BaseService.Response<>();
                        response.setBody(new Triplet<>(Stop.getAll(), Service.getAll(),
                                FavouriteStop.getFavouriteStops(UserService.getInstance().getAuthenticatedUser())));

                        return response;
                    }
                })
                .doWhenFinished(new AsyncJob.AsyncResultAction<BaseService.Response<Triplet<List<Stop>, List<Service>, List<FavouriteStop>>>>() {
                    @Override
                    public void onResult(BaseService.Response<Triplet<List<Stop>, List<Service>, List<FavouriteStop>>> response) {
                        if (getActivity() == null) {
                            return;
                        }

                        stops = response.getBody().a;
                        services = response.getBody().b;
                        favouriteStops = response.getBody().c;

                        searchAdapter.notifyFilterChanged(null);
                        progressBar.setVisibility(View.GONE);
                        searchResultsRecyclerView.setVisibility(View.VISIBLE);
                    }
                }).create().start();
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

                if (services != null) {
                    for (Service service : services) {
                        if (service.getName().toLowerCase().contains(filter) ||
                                service.getDescription().toLowerCase().contains(filter)) {

                            filteredServices.add(service);
                        }
                    }
                }

                if (stops != null) {
                    for (Stop stop : stops) {
                        if (stop.getName().toLowerCase().contains(filter)) {
                            filteredStops.add(stop);
                        }
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

            private Service service;
            private ServiceView serviceView;

            public ServiceViewHolder(View itemView) {
                super(itemView);

                serviceView = (ServiceView) itemView;

                serviceView.setOnClickListener(this);
            }

            public void bindItem(Service service) {
                this.service = service;

                serviceView.bindItem(service);
            }

            @Override
            public void onClick(View v) {
//                Intent startActivityIntent = new Intent(getActivity(), ServiceActivity.class);
//                startActivityIntent.putExtra(ServiceActivity.EXTRA_SERVICE_NAME, service.getName());
//                startActivity(startActivityIntent);
            }
        }

        private class StopViewHolder extends RecyclerView.ViewHolder
                implements View.OnClickListener {

            private Stop stop;
            private StopView stopView;

            public StopViewHolder(View itemView) {
                super(itemView);

                stopView = (StopView) itemView;

                stopView.setOnClickListener(this);
            }

            public void bindItem(Stop stop) {
                this.stop = stop;

                stopView.bindItem(stop, isStopFavourite(stop), null);
            }

            @Override
            public void onClick(View v) {
                int id = v.getId();

                if (id == stopView.getId()) {
                    if (!isStopFavourite(stop)) {
                        new BottomSheet.Builder(getActivity())
                                .title(String.format(getString(R.string.label_stop_name_with_direction),
                                        stop.getName(), stop.getDirection()))
                                .sheet(R.menu.menu_search_stop_1)
                                .listener(new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        switch (which) {
                                            case R.id.action_view_departures:
                                                startStopActivity(stop);
                                                break;
                                            case R.id.action_add_to_favourites:
                                                FavouriteStop favouriteStop = new FavouriteStop(stop,
                                                        UserService.getInstance().getAuthenticatedUser());
                                                favouriteStop.save();

                                                favouriteStops.add(favouriteStop);

                                                notifyItemChanged(getAdapterPosition());

                                                Toast.makeText(getActivity(), String.format(
                                                        getString(R.string.success_stop_added_to_favourites),
                                                        stop.getName()), Toast.LENGTH_SHORT).show();
                                                break;
                                        }
                                    }
                                }).show();
                    } else {
                        new BottomSheet.Builder(getActivity())
                                .title(String.format(getString(R.string.label_stop_name_with_direction),
                                        stop.getName(), stop.getDirection()))
                                .sheet(R.menu.menu_search_stop_2)
                                .listener(new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        switch (which) {
                                            case R.id.action_view_departures:
                                                startStopActivity(stop);
                                                break;
                                            case R.id.action_remove_from_favourites:
                                                Iterator<FavouriteStop> iterator = favouriteStops.iterator();

                                                while (iterator.hasNext()) {
                                                    FavouriteStop favouriteStop = iterator.next();
                                                    if (favouriteStop.getStop().getId().equals(stop.getId())) {
                                                        favouriteStop.delete();
                                                        iterator.remove();

                                                        Toast.makeText(getActivity(), String.format(
                                                                getString(R.string.label_remove_from_favourites),
                                                                stop.getName()), Toast.LENGTH_SHORT).show();

                                                        notifyItemChanged(getAdapterPosition());
                                                        break;
                                                    }
                                                }
                                                break;
                                        }
                                    }
                                }).show();
                    }
                }
            }

            private void startStopActivity(Stop stop) {
                Intent startActivityIntent = new Intent(getActivity(), StopActivity.class);
                startActivityIntent.putExtra(StopActivity.EXTRA_STOP_ID, stop.get_id());
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

        private boolean isStopFavourite(Stop stop) {
            for (FavouriteStop favouriteStop : favouriteStops) {
                if (favouriteStop.getStop().getId().equals(stop.getId())) {
                    return true;
                }
            }

            return false;
        }
    }
}
