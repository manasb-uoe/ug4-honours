package com.enthusiast94.edinfit.ui.home.fragments;

import android.content.Context;
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
import com.enthusiast94.edinfit.models.Activity;
import com.enthusiast94.edinfit.network.ActivityService;
import com.enthusiast94.edinfit.network.BaseService;
import com.enthusiast94.edinfit.utils.Helpers;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by manas on 01-10-2015.
 */
public class ActivityFragment extends Fragment {

    private SwipeRefreshLayout swipeRefreshLayout;

    private ActivityAdapter activityAdapter;
    private List<Activity> activities;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_activity, container, false);

        /**
         * Find views
         */

        RecyclerView activityRecyclerView = (RecyclerView) view.findViewById(R.id.activity_recyclerview);
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_layout);

        /**
         * Setup swipe refresh layout
         */

        swipeRefreshLayout.setColorSchemeResources(R.color.accent);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {
                loadActivities();
            }
        });

        /**
         * Setup saved stops recycler view
         */

        activityRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        activityAdapter = new ActivityAdapter(getActivity());
        activityRecyclerView.setAdapter(activityAdapter);

        loadActivities();

        return view;
    }

    private void loadActivities() {
        setRefreshIndicatorVisiblity(true);

        ActivityService.getInstance().getActivties(new BaseService.Callback<List<Activity>>() {

            @Override
            public void onSuccess(List<Activity> data) {
                if (getActivity() != null) {
                    activities = data;

                    activityAdapter.notifyActivitiesChanged(activities);

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

    private void setRefreshIndicatorVisiblity(final boolean visiblity) {
        swipeRefreshLayout.post(new Runnable() {

            @Override
            public void run() {
                swipeRefreshLayout.setRefreshing(visiblity);
            }
        });
    }

    private static class ActivityAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private Context context;
        private LayoutInflater inflater;
        private Map<String, ActivityTimeSpan> activityTimeSpansMap;

        private static final int HEADER_VIEW = 0;
        private static final int DETAIL_VIEW = 1;

        public ActivityAdapter(Context context) {
            this.context = context;
            inflater = LayoutInflater.from(context);
            activityTimeSpansMap = new LinkedHashMap<>();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == HEADER_VIEW) {
                return new HeaderViewHolder(
                        inflater.inflate(R.layout.row_activity_header, parent, false));
            } else if (viewType == DETAIL_VIEW) {
                return new DetailViewHolder(
                        inflater.inflate(R.layout.row_activity_detail, parent, false));
            } else {
                throw new IllegalArgumentException("Invalid view type: " + viewType);
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof HeaderViewHolder) {
                String timeSpan = (String) getItem(position);
                ActivityTimeSpan activityTimeSpan = activityTimeSpansMap.get(timeSpan);
                ((HeaderViewHolder) holder).bindItem(timeSpan, activityTimeSpan);
            } else if (holder instanceof DetailViewHolder) {
                ((DetailViewHolder) holder).bindItem((Activity) getItem(position));
            } else {
                throw new IllegalArgumentException("Invalid holder instance type: " +
                        holder.getClass().getSimpleName());
            }
        }

        @Override
        public int getItemCount() {
            int size = 0;

            for (Map.Entry<String, ActivityTimeSpan> entry : activityTimeSpansMap.entrySet()) {
                size += 1 + entry.getValue().getActivities().size();
            }

            return size;
        }

        private Object getItem(int position) {
            int offset = position;

            for (Map.Entry<String, ActivityTimeSpan> entry : activityTimeSpansMap.entrySet()) {
                if (offset == 0) {
                    return entry.getKey();
                }

                offset--;

                if (offset < entry.getValue().getActivities().size()) {
                    return entry.getValue().getActivities().get(offset);
                }

                offset -= entry.getValue().getActivities().size();
            }

            throw new IllegalArgumentException("Invalid position: " + position);
        }

        @Override
        public int getItemViewType(int position) {
            if (getItem(position) instanceof String) {
                return HEADER_VIEW;
            } else {
                return DETAIL_VIEW;
            }
        }

        private void notifyActivitiesChanged(List<Activity> activities) {
            activityTimeSpansMap.clear();

            // iterate in reverse order since the dates must appear in descending order
            for (int i=activities.size()-1; i>=0; i--) {
                Activity activity = activities.get(i);

                Date date = new Date(activity.getStart());
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM EEEE", Locale.UK);
                String timeSpanText = sdf.format(date);

                if (activityTimeSpansMap.containsKey(timeSpanText)) {
                    activityTimeSpansMap.get(timeSpanText).getActivities().add(activity);
                } else {
                    List<Activity> activitiesList = new ArrayList<>();
                    activitiesList.add(activity);
                    ActivityTimeSpan timeSpan = new ActivityTimeSpan(null, activitiesList);
                    activityTimeSpansMap.put(timeSpanText, timeSpan);
                }
            }

            // compute statistic for each time span
            for (Map.Entry<String, ActivityTimeSpan> entry : activityTimeSpansMap.entrySet()) {
                double totalDistance = 0;

                for (Activity activity : entry.getValue().getActivities()) {
                    totalDistance += activity.getDistance();
                }

                entry.getValue().setStatistic(Helpers.humanizeDistance(totalDistance/1000d));
            }

            notifyDataSetChanged();
        }

        private static class ActivityTimeSpan {

            private String statistic;
            private List<Activity> activities;

            public ActivityTimeSpan(String statistic, List<Activity> activities) {
                this.statistic = statistic;
                this.activities = activities;
            }

            public String getStatistic() {
                return statistic;
            }

            public void setStatistic(String statistic) {
                this.statistic = statistic;
            }

            public List<Activity> getActivities() {
                return activities;
            }
        }

        private class HeaderViewHolder extends RecyclerView.ViewHolder {

            private TextView timeSpanTextView;
            private TextView statisticTextView;

            public HeaderViewHolder(View itemView) {
                super(itemView);

                // find views
                timeSpanTextView = (TextView) itemView.findViewById(R.id.timespan_textview);
                statisticTextView = (TextView) itemView.findViewById(R.id.statistic_textview);
            }

            public void bindItem(String timeSpanText, ActivityTimeSpan timeSpan) {
                timeSpanTextView.setText(timeSpanText);
                statisticTextView.setText(timeSpan.getStatistic());
            }
        }

        private class DetailViewHolder extends RecyclerView.ViewHolder {

            private TextView infoTextView;
            private TextView descriptionTextView;

            private SimpleDateFormat sdf;

            public DetailViewHolder(View itemView) {
                super(itemView);

                sdf = new SimpleDateFormat("HH:mm", Locale.UK);

                // find views
                descriptionTextView = (TextView) itemView.findViewById(R.id.description_textview);
                infoTextView = (TextView) itemView.findViewById(R.id.info_textview);
            }

            public void bindItem(Activity activity) {
                String activityType;

                switch (activity.getType()) {
                    case WAIT_OR_WALK:
                        activityType = context.getString(R.string.action_wait_or_walk);
                        break;

                    default:
                        throw new IllegalArgumentException("Invalid activity type: " +
                                activity.getType().getValue());
                }

                infoTextView.setText(String.format(context.getString(R.string.label_activity_info),
                        activityType, sdf.format(activity.getStart())));

                descriptionTextView.setText(activity.getDescription());
            }
        }
    }
}
