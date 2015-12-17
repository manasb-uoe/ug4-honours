package com.enthusiast94.edinfit.ui.home.fragments;

import android.content.Context;
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
import com.enthusiast94.edinfit.network.BaseService;
import com.enthusiast94.edinfit.network.StopService;
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
    private List<Stop> savedStops;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_saved_stops, container, false);

        savedStopsRecyclerView = (RecyclerView) view.findViewById(R.id.saved_stops_recyclerview);
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_layout);

        swipeRefreshLayout.setColorSchemeResources(R.color.accent);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {
                loadSavedStops();
            }
        });

        savedStopsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        savedStopsAdapter = new SavedStopsAdapter(getActivity());
        savedStopsRecyclerView.setAdapter(savedStopsAdapter);

        if (savedStops == null) {
            loadSavedStops();
        } else {
            savedStopsAdapter.notifyStopsChanged(savedStops);
        }

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

                    savedStopsAdapter.notifyStopsChanged(savedStops);
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

    private static class SavedStopsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private Context context;
        private LayoutInflater inflater;
        private List<Stop> savedStops;

        public SavedStopsAdapter(Context context) {
            this.context = context;
            inflater = LayoutInflater.from(context);
            savedStops = new ArrayList<>();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
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

        public void notifyStopsChanged(List<Stop> stops) {
            this.savedStops = stops;
            notifyDataSetChanged();
        }

        private class SavedStopViewHolder extends RecyclerView.ViewHolder
                implements View.OnClickListener {

            private Stop stop;
            private TextView stopNameTextView;
            private TextView upcomingTextView;
            private ImageButton unsaveButton;

            public SavedStopViewHolder(View itemView) {
                super(itemView);

                // find views
                stopNameTextView = (TextView) itemView.findViewById(R.id.stop_name_textview);
                upcomingTextView = (TextView) itemView.findViewById(R.id.upcoming_textview);
                unsaveButton = (ImageButton) itemView.findViewById(R.id.unsave_button);

                // bind event listeners
                itemView.setOnClickListener(this);
                unsaveButton.setOnClickListener(this);
            }

            public void bindItem(Stop stop) {
                this.stop = stop;

                stopNameTextView.setText(String.format(context.getString(
                        R.string.label_stop_name_with_direction), stop.getName(), stop.getDirection()));

                if (stop.getDepartures().size() > 0) {
                    Departure departure = stop.getDepartures().get(0);
                    upcomingTextView.setText(String.format(
                            context.getString(R.string.label_upcoming_departure_info),
                            departure.getServiceName(), departure.getDestination(),
                            departure.getTime()));
                } else {
                    upcomingTextView.setText(context.getString(R.string.label_no_upcoming_departure));
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
                            if (context != null) {
                                // remove unsaved stop from saved stops list
                                for (int i=0; i<savedStops.size(); i++) {
                                    if (savedStops.get(i).equals(stop)) {
                                        savedStops.remove(stop);
                                        notifyItemRemoved(i);
                                        break;
                                    }
                                }

                                Toast.makeText(context, String.format(
                                        context.getString(R.string.success_stop_unsaved),
                                        stop.getName()), Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(String message) {
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }

        private void startStopActivity(Stop stop) {
            Intent startActivityIntent = new Intent(context, StopActivity.class);
            startActivityIntent.putExtra(StopActivity.EXTRA_STOP_ID, stop.getId());
            startActivityIntent.putExtra(StopActivity.EXTRA_STOP_NAME, stop.getName());
            context.startActivity(startActivityIntent);
        }
    }

}
