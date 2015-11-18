package com.enthusiast94.edinfit.ui.home.fragments;

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
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models.Departure;
import com.enthusiast94.edinfit.models.Stop;
import com.enthusiast94.edinfit.services.BaseService;
import com.enthusiast94.edinfit.services.StopService;
import com.enthusiast94.edinfit.ui.stop_info.activities.StopActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by manas on 18-11-2015.
 */
public class SavedStopsFragment extends Fragment {

    public static final String TAG = NearMeFragment.class.getSimpleName();
    private RecyclerView savedStopsRecyclerView;
    private SavedStopsAdapter savedStopsAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private List<Stop> savedStops = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_saved_stops, container, false);

        /**
         * Find views
         */

        savedStopsRecyclerView = (RecyclerView) view.findViewById(R.id.saved_stops_recyclerview);
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_layout);

        /**
         * Setup swipe refresh layout
         */

        swipeRefreshLayout.setColorSchemeResources(R.color.accent);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {
                loadSavedStops();
            }
        });

        /**
         * Setup saved stops recycler view
         */

        savedStopsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        savedStopsAdapter = new SavedStopsAdapter();
        savedStopsRecyclerView.setAdapter(savedStopsAdapter);

        loadSavedStops();

        return view;
    }

    private void loadSavedStops() {
        setRefreshIndicatorVisiblity(true);

        StopService.getInstance().getSavedStops(new BaseService.Callback<List<Stop>>() {

            @Override
            public void onSuccess(List<Stop> data) {
                savedStops = data;

                if (getActivity() != null) {
                    setRefreshIndicatorVisiblity(false);

                    savedStopsAdapter.notifyDataSetChanged();
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

    private class SavedStopsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private LayoutInflater inflater;

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (inflater == null) {
                inflater = LayoutInflater.from(getActivity());
            }

            return new SavedStopViewHolder(inflater.inflate(R.layout.row_saved_stop, parent, false));
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            ((SavedStopViewHolder) holder).bindItem(savedStops.get(position));
        }

        @Override
        public int getItemCount() {
            return savedStops.size();
        }

        private class SavedStopViewHolder extends RecyclerView.ViewHolder
                implements View.OnClickListener {

            private Stop stop;
            private TextView stopNameTextView;
            private TextView serviceNameTextView;
            private TextView destinationTextView;
            private TextView timeTextView;
            private ImageButton unsaveButton;

            public SavedStopViewHolder(View itemView) {
                super(itemView);

                // find views
                stopNameTextView = (TextView) itemView.findViewById(R.id.stop_name_textview);
                serviceNameTextView =
                        (TextView) itemView.findViewById(R.id.service_name_textview);
                destinationTextView =
                        (TextView) itemView.findViewById(R.id.destination_textview);
                timeTextView =
                        (TextView) itemView.findViewById(R.id.time_textview);
                unsaveButton = (ImageButton) itemView.findViewById(R.id.unsave_stop_button);

                // bind event listeners
                itemView.setOnClickListener(this);
                unsaveButton.setOnClickListener(this);
            }

            public void bindItem(Stop stop) {
                this.stop = stop;

                stopNameTextView.setText(stop.getName());

                if (stop.getDepartures().size() > 0) {
                    Departure departure = stop.getDepartures().get(0);

                    serviceNameTextView.setText(departure.getServiceName());
                    destinationTextView.setText(departure.getDestination());
                    timeTextView.setText(departure.getTime());
                } else {
                    serviceNameTextView.setText(getString(R.string.label_no_upcoming_departure));
                    destinationTextView.setText("");
                    timeTextView.setText("");
                }
            }

            @Override
            public void onClick(View v) {
                int id = v.getId();

                if (id == itemView.getId()) {
                    startStopActivity(stop);
                } else if (id == unsaveButton.getId()) {
                    StopService.getInstance().saveOrUnsaveStop(stop.getId(), false, new BaseService.Callback<Void>() {

                        @Override
                        public void onSuccess(Void data) {
                            if (getActivity() != null) {
                                // remove unsaved stop from saved stops list
                                for (int i=0; i<savedStops.size(); i++) {
                                    if (savedStops.get(i).equals(stop)) {
                                        savedStops.remove(stop);
                                        notifyItemRemoved(i);
                                        break;
                                    }
                                }

                                Toast.makeText(getActivity(), String.format(
                                        getActivity().getString(R.string.success_stop_unsaved),
                                        stop.getName()), Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(String message) {
                            Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }

        private void startStopActivity(Stop stop) {
            Intent startActivityIntent = new Intent(getActivity(), StopActivity.class);
            startActivityIntent.putExtra(StopActivity.EXTRA_STOP_ID, stop.getId());
            startActivityIntent.putExtra(StopActivity.EXTRA_STOP_NAME, stop.getName());
            startActivity(startActivityIntent);
        }
    }

}
