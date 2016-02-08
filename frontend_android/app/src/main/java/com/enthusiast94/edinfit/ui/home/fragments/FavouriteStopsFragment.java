package com.enthusiast94.edinfit.ui.home.fragments;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.arasthel.asyncjob.AsyncJob;
import com.cocosw.bottomsheet.BottomSheet;
import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models.Departure;
import com.enthusiast94.edinfit.models.FavouriteStop;
import com.enthusiast94.edinfit.models.Stop;
import com.enthusiast94.edinfit.network.BaseService;
import com.enthusiast94.edinfit.network.StopService;
import com.enthusiast94.edinfit.network.UserService;
import com.enthusiast94.edinfit.ui.stop_info.activities.StopActivity;
import com.enthusiast94.edinfit.utils.Helpers;
import com.enthusiast94.edinfit.utils.LiveDepartureView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by manas on 18-11-2015.
 */
public class FavouriteStopsFragment extends Fragment {

    public static final String TAG = FavouriteStopsFragment.class.getSimpleName();

    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView updatedAtTextView;
    private TextView noFavouritesTextView;

    private FavouriteStopsAdapter favouriteStopsAdapter;
    private List<Pair<FavouriteStop, List<Departure>>> favouriteStopPairs;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favourite_stops, container, false);

        RecyclerView favouriteStopsRecyclerView =
                (RecyclerView) view.findViewById(R.id.favourite_stops_recyclerview);
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_layout);
        updatedAtTextView = (TextView) view.findViewById(R.id.updated_at_textview);
        noFavouritesTextView = (TextView) view.findViewById(R.id.no_favourite_stops_textview);

        swipeRefreshLayout.setColorSchemeResources(R.color.accent);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {
                loadFavouriteStops();
            }
        });

        favouriteStopsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        favouriteStopsAdapter = new FavouriteStopsAdapter(getActivity()) {
            @Override
            public void onFavouriteRemoved(int remainingCount) {
                if (remainingCount == 0) {
                    noFavouritesTextView.setVisibility(View.VISIBLE);
                } else {
                    noFavouritesTextView.setVisibility(View.GONE);
                }
            }
        };
        favouriteStopsRecyclerView.setAdapter(favouriteStopsAdapter);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFavouriteStops();
    }

    private void loadFavouriteStops() {
        setRefreshIndicatorVisiblity(true);

        new AsyncJob.AsyncJobBuilder<BaseService.Response<List<Pair<FavouriteStop, List<Departure>>>>>()
                .doInBackground(new AsyncJob.AsyncAction<BaseService.Response<List<Pair<FavouriteStop, List<Departure>>>>>() {
                    @Override
                    public BaseService.Response<List<Pair<FavouriteStop, List<Departure>>>> doAsync() {
                        List<FavouriteStop> favouriteStops =
                                FavouriteStop.getFavouriteStops(UserService.getInstance().getAuthenticatedUser());
                        return StopService.getInstance().getDeparturesForFavouriteStops(favouriteStops);
                    }
                })
                .doWhenFinished(new AsyncJob.AsyncResultAction<BaseService.Response<List<Pair<FavouriteStop, List<Departure>>>>>() {
                    @Override
                    public void onResult(BaseService.Response<List<Pair<FavouriteStop, List<Departure>>>> response) {
                        if (getActivity() == null) {
                            return;
                        }

                        setRefreshIndicatorVisiblity(false);

                        if (!response.isSuccessfull()) {
                            Toast.makeText(getActivity(), response.getError(), Toast.LENGTH_SHORT)
                                    .show();
                            return;
                        }

                        favouriteStopPairs = response.getBody();

                        if (favouriteStopPairs.size() == 0) {
                            noFavouritesTextView.setVisibility(View.VISIBLE);
                        } else {
                            noFavouritesTextView.setVisibility(View.GONE);
                        }

                        favouriteStopsAdapter.notifyFavouritesChanged(favouriteStopPairs);
                        updatedLastUpdatedTimestamp();
                    }
                })
                .create().start();
    }

    private void updatedLastUpdatedTimestamp() {
        updatedAtTextView.setText(Helpers.getCurrentTime24h());
    }

    private void setRefreshIndicatorVisiblity(final boolean visiblity) {
        swipeRefreshLayout.post(new Runnable() {

            @Override
            public void run() {
                swipeRefreshLayout.setRefreshing(visiblity);
            }
        });
    }

    private static abstract class FavouriteStopsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private Activity context;
        private LayoutInflater inflater;
        private List<Pair<FavouriteStop, List<Departure>>> favouriteStopPairs;

        public FavouriteStopsAdapter(Activity context) {
            this.context = context;
            inflater = LayoutInflater.from(context);
            favouriteStopPairs = new ArrayList<>();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new FavouriteStopViewHolder(inflater.inflate(R.layout.row_saved_stop, parent, false));
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            ((FavouriteStopViewHolder) holder).bindItem(favouriteStopPairs.get(position));
        }

        @Override
        public int getItemCount() {
            return favouriteStopPairs.size();
        }

        public void notifyFavouritesChanged(List<Pair<FavouriteStop, List<Departure>>> favouriteStopPairs) {
            this.favouriteStopPairs = favouriteStopPairs;

            // remove departures that are not due
            for (Pair<FavouriteStop, List<Departure>> pair : favouriteStopPairs) {
                Iterator<Departure> iterator = pair.second.iterator();
                while (iterator.hasNext()) {
                    Departure departure = iterator.next();
                    if (Helpers.getRemainingTimeMillisFromNow(departure.getTime()) < 0) {
                        iterator.remove();
                    } else {
                        break;
                    }
                }
            }

            notifyDataSetChanged();
        }

        private class FavouriteStopViewHolder extends RecyclerView.ViewHolder
                implements View.OnClickListener {

            private Pair<FavouriteStop, List<Departure>> favouruteStopPair;
            private TextView stopNameTextView;
            private TextView noUpcomingDeparturesTextView;
            private LiveDepartureView[] liveDepartureViews;

            public FavouriteStopViewHolder(View itemView) {
                super(itemView);

                // find views
                stopNameTextView = (TextView) itemView.findViewById(R.id.stop_name_textview);
                noUpcomingDeparturesTextView =
                        (TextView) itemView.findViewById(R.id.no_upcoming_departures_textview);
                liveDepartureViews = new LiveDepartureView[]{
                        (LiveDepartureView) itemView.findViewById(R.id.departure_container_1),
                        (LiveDepartureView) itemView.findViewById(R.id.departure_container_2),
                        (LiveDepartureView) itemView.findViewById(R.id.departure_container_3),
                        (LiveDepartureView) itemView.findViewById(R.id.departure_container_4)
                };

                // bind event listeners
                itemView.setOnClickListener(this);
            }

            public void bindItem(Pair<FavouriteStop, List<Departure>> favouruteStopPair) {
                this.favouruteStopPair = favouruteStopPair;

                Stop stop = favouruteStopPair.first.getStop();
                stopNameTextView.setText(String.format(context.getString(R.string.label_stop_name_with_direction),
                        stop.getName(), stop.getDirection()));

                List<Departure> departures = favouruteStopPair.second;
                if (departures.size() > 0) {
                    noUpcomingDeparturesTextView.setVisibility(View.GONE);

                    for (int i=0; i< liveDepartureViews.length; i++) {
                        LiveDepartureView liveDepartureView = this.liveDepartureViews[i];

                        if (i < departures.size()) {
                            liveDepartureView.bindItem(departures.get(i));
                            liveDepartureView.setVisibility(View.VISIBLE);
                        } else {
                            liveDepartureView.setVisibility(View.GONE);
                        }
                    }
                } else {
                    noUpcomingDeparturesTextView.setVisibility(View.VISIBLE);
                    for (LiveDepartureView liveDepartureView : this.liveDepartureViews) {
                        liveDepartureView.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onClick(View v) {
                int id = v.getId();

                if (id == itemView.getId()) {
                    final Stop stop = favouruteStopPair.first.getStop();
                    new BottomSheet.Builder(context)
                            .title(String.format(context.getString(R.string.label_stop_name_with_direction),
                                    stop.getName(), stop.getDirection()))
                            .sheet(R.menu.menu_favourite_stop)
                            .listener(new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    switch (which) {
                                        case R.id.action_view_departures:
                                            startStopActivity(stop);
                                            break;
                                        case R.id.action_remove_from_favourites:
                                            // remove unfavourited stop from recycler view and database
                                            for (int i=0; i<favouriteStopPairs.size(); i++) {
                                                if (favouriteStopPairs.get(i).equals(favouruteStopPair)) {
                                                    favouriteStopPairs.remove(favouruteStopPair);
                                                    notifyItemRemoved(i);
                                                    onFavouriteRemoved(favouriteStopPairs.size());
                                                    break;
                                                }
                                            }

                                            favouruteStopPair.first.delete();

                                            Toast.makeText(context, String.format(
                                                    context.getString(R.string.success_stop_removed_from_favourites),
                                                    stop.getName()), Toast.LENGTH_SHORT).show();
                                            break;
                                    }
                                }
                            }).show();
                }
            }
        }

        private void startStopActivity(Stop stop) {
            Intent startActivityIntent = new Intent(context, StopActivity.class);
            startActivityIntent.putExtra(StopActivity.EXTRA_STOP_ID, stop.get_id());
            context.startActivity(startActivityIntent);
        }

        public abstract void onFavouriteRemoved(int remainingCount);
    }

}
