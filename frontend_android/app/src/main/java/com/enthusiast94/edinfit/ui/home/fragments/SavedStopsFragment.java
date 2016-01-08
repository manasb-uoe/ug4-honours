//package com.enthusiast94.edinfit.ui.home.fragments;
//
//import android.app.Activity;
//import android.content.DialogInterface;
//import android.content.Intent;
//import android.os.Bundle;
//import android.support.annotation.Nullable;
//import android.support.v4.app.Fragment;
//import android.support.v4.widget.SwipeRefreshLayout;
//import android.support.v7.widget.LinearLayoutManager;
//import android.support.v7.widget.RecyclerView;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import com.cocosw.bottomsheet.BottomSheet;
//import com.enthusiast94.edinfit.R;
//import com.enthusiast94.edinfit.models.Departure;
//import com.enthusiast94.edinfit.models.Stop;
//import com.enthusiast94.edinfit.network.BaseService;
//import com.enthusiast94.edinfit.network.StopService;
//import com.enthusiast94.edinfit.ui.stop_info.activities.StopActivity;
//import com.enthusiast94.edinfit.utils.LiveDepartureView;
//import com.enthusiast94.edinfit.utils.Helpers;
//
//import java.util.ArrayList;
//import java.util.Iterator;
//import java.util.List;
//
///**
// * Created by manas on 18-11-2015.
// */
//public class SavedStopsFragment extends Fragment {
//
//    public static final String TAG = NearMeFragment.class.getSimpleName();
//    private static final int DEPARTURES_LIMIT = 8;
//
//    private RecyclerView savedStopsRecyclerView;
//    private SwipeRefreshLayout swipeRefreshLayout;
//    private TextView updatedAtTextView;
//
//    private SavedStopsAdapter savedStopsAdapter;
//    private List<Stop> savedStops;
//
//
//    @Override
//    public void onCreate(@Nullable Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setRetainInstance(true);
//    }
//
//    @Nullable
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        View view = inflater.inflate(R.layout.fragment_saved_stops, container, false);
//
//        savedStopsRecyclerView = (RecyclerView) view.findViewById(R.id.saved_stops_recyclerview);
//        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_layout);
//        updatedAtTextView = (TextView) view.findViewById(R.id.updated_at_textview);
//
//        swipeRefreshLayout.setColorSchemeResources(R.color.accent);
//        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
//
//            @Override
//            public void onRefresh() {
//                loadSavedStops();
//            }
//        });
//
//        savedStopsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
//        savedStopsAdapter = new SavedStopsAdapter(getActivity());
//        savedStopsRecyclerView.setAdapter(savedStopsAdapter);
//
//        return view;
//    }
//
//    @Override
//    public void onResume() {
//        super.onResume();
//
//        if (savedStops == null) {
//            loadSavedStops();
//        } else {
//            savedStopsAdapter.notifyStopsChanged(savedStops);
//            updatedLastUpdatedTimestamp();
//        }
//    }
//
//    private void loadSavedStops() {
//        setRefreshIndicatorVisiblity(true);
//
//        StopService.getInstance().getSavedStops(DEPARTURES_LIMIT,
//                new BaseService.Callback<List<Stop>>() {
//
//                    @Override
//                    public void onSuccess(List<Stop> data) {
//                        savedStops = data;
//
//                        if (getActivity() != null) {
//                            setRefreshIndicatorVisiblity(false);
//
//                            savedStopsAdapter.notifyStopsChanged(savedStops);
//                            updatedLastUpdatedTimestamp();
//                        }
//                    }
//
//                    @Override
//                    public void onFailure(String message) {
//                        if (getActivity() != null) {
//                            setRefreshIndicatorVisiblity(false);
//
//                            Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
//                        }
//                    }
//                });
//    }
//
//    private void updatedLastUpdatedTimestamp() {
//        updatedAtTextView.setText(Helpers.getCurrentTime24h());
//    }
//
//    private void setRefreshIndicatorVisiblity(final boolean visiblity) {
//        swipeRefreshLayout.post(new Runnable() {
//
//            @Override
//            public void run() {
//                swipeRefreshLayout.setRefreshing(visiblity);
//            }
//        });
//    }
//
//    private static class SavedStopsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
//
//        private Activity context;
//        private LayoutInflater inflater;
//        private List<Stop> savedStops;
//
//        public SavedStopsAdapter(Activity context) {
//            this.context = context;
//            inflater = LayoutInflater.from(context);
//            savedStops = new ArrayList<>();
//        }
//
//        @Override
//        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
//            return new SavedStopViewHolder(inflater.inflate(R.layout.row_saved_stop, parent, false));
//        }
//
//        @Override
//        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
//            ((SavedStopViewHolder) holder).bindItem(savedStops.get(position));
//        }
//
//        @Override
//        public int getItemCount() {
//            return savedStops.size();
//        }
//
//        public void notifyStopsChanged(List<Stop> stops) {
//            this.savedStops = stops;
//
//            // remove departures that are not due
//            for (Stop stop : savedStops) {
//                Iterator<Departure> iterator = stop.getDepartures().iterator();
//                while (iterator.hasNext()) {
//                    Departure departure = iterator.next();
//                    if (Helpers.getRemainingTimeMillisFromNow(departure.getTime()) < 0) {
//                        iterator.remove();
//                    } else {
//                        break;
//                    }
//                }
//            }
//
//            notifyDataSetChanged();
//        }
//
//        private class SavedStopViewHolder extends RecyclerView.ViewHolder
//                implements View.OnClickListener {
//
//            private Stop stop;
//            private TextView stopNameTextView;
//            private TextView noUpcomingDeparturesTextView;
//            private LiveDepartureView[] liveDepartureViews;
//
//            public SavedStopViewHolder(View itemView) {
//                super(itemView);
//
//                // find views
//                stopNameTextView = (TextView) itemView.findViewById(R.id.stop_name_textview);
//                noUpcomingDeparturesTextView =
//                        (TextView) itemView.findViewById(R.id.no_upcoming_departures_textview);
//                liveDepartureViews = new LiveDepartureView[]{
//                        (LiveDepartureView) itemView.findViewById(R.id.departure_container_1),
//                        (LiveDepartureView) itemView.findViewById(R.id.departure_container_2),
//                        (LiveDepartureView) itemView.findViewById(R.id.departure_container_3),
//                        (LiveDepartureView) itemView.findViewById(R.id.departure_container_4)
//                };
//
//                // bind event listeners
//                itemView.setOnClickListener(this);
//            }
//
//            public void bindItem(Stop stop) {
//                this.stop = stop;
//
//                stopNameTextView.setText(String.format(context.getString(R.string.label_stop_name_with_direction),
//                        stop.getName(), stop.getDirection()));
//
//                List<Departure> departures = stop.getDepartures();
//                if (departures.size() > 0) {
//                    noUpcomingDeparturesTextView.setVisibility(View.GONE);
//
//                    for (int i=0; i< liveDepartureViews.length; i++) {
//                        LiveDepartureView liveDepartureView = this.liveDepartureViews[i];
//
//                        if (i < departures.size()) {
//                            liveDepartureView.bindItem(departures.get(i));
//                            liveDepartureView.setVisibility(View.VISIBLE);
//                        } else {
//                            liveDepartureView.setVisibility(View.GONE);
//                        }
//                    }
//                } else {
//                    noUpcomingDeparturesTextView.setVisibility(View.VISIBLE);
//                    for (LiveDepartureView liveDepartureView : this.liveDepartureViews) {
//                        liveDepartureView.setVisibility(View.GONE);
//                    }
//                }
//            }
//
//            @Override
//            public void onClick(View v) {
//                int id = v.getId();
//
//                if (id == itemView.getId()) {
//                    new BottomSheet.Builder(context)
//                            .title(String.format(context.getString(R.string.label_stop_name_with_direction),
//                                    stop.getName(), stop.getDirection()))
//                            .sheet(R.menu.menu_favourite_stop)
//                            .listener(new DialogInterface.OnClickListener() {
//                                @Override
//                                public void onClick(DialogInterface dialog, int which) {
//                                    switch (which) {
//                                        case R.id.action_view_departures:
//                                            startStopActivity(stop);
//                                            break;
//                                        case R.id.action_remove_from_favourites:
//                                            StopService.getInstance().saveOrUnsaveStop(stop.getId(), false, new BaseService.Callback<Void>() {
//
//                                                @Override
//                                                public void onSuccess(Void data) {
//                                                    if (context != null) {
//                                                        // remove unsaved stop from saved stops list
//                                                        for (int i=0; i<savedStops.size(); i++) {
//                                                            if (savedStops.get(i).equals(stop)) {
//                                                                savedStops.remove(stop);
//                                                                notifyItemRemoved(i);
//                                                                break;
//                                                            }
//                                                        }
//
//                                                        Toast.makeText(context, String.format(
//                                                                context.getString(R.string.success_stop_unsaved),
//                                                                stop.getName()), Toast.LENGTH_SHORT).show();
//                                                    }
//                                                }
//
//                                                @Override
//                                                public void onFailure(String message) {
//                                                    if (context != null) {
//                                                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
//                                                    }
//                                                }
//                                            });
//                                            break;
//                                    }
//                                }
//                            }).show();
//                }
//            }
//        }
//
//        private void startStopActivity(Stop stop) {
//            Intent startActivityIntent = new Intent(context, StopActivity.class);
//            startActivityIntent.putExtra(StopActivity.EXTRA_STOP, stop);
//            context.startActivity(startActivityIntent);
//        }
//    }
//
//}
