package com.enthusiast94.edinfit.ui.home.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models.Activity;
import com.enthusiast94.edinfit.network.ActivityService;
import com.enthusiast94.edinfit.network.BaseService;
import com.enthusiast94.edinfit.utils.Helpers;
import com.enthusiast94.edinfit.utils.Triplet;
import com.viewpagerindicator.CirclePageIndicator;

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

    private static final String TAG = ActivityFragment.class.getSimpleName();

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

        RecyclerView activityRecyclerView = (RecyclerView) view.findViewById(R.id.activity_recyclerview);
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_layout);

        swipeRefreshLayout.setColorSchemeResources(R.color.accent);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {
                loadActivities();
            }
        });

        activityRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        activityAdapter = new ActivityAdapter(getActivity());
        activityRecyclerView.setAdapter(activityAdapter);

        if (activities == null) {
            loadActivities();
        } else {
            activityAdapter.notifyActivitiesChanged(activities);
        }

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
        private StatisticsSummary todaySummary;

        private static final int SPINNER_VIEW = 0;
        private static final int TODAY_SUMMARY_VIEW = 1;
        private static final int HEADER_VIEW = 2;
        private static final int DETAIL_VIEW = 3;
        private static final String TODAY_SUMMARY_ITEM = "todaySummaryItem";
        private static final String SPINNER_ITEM = "spinnerItem";


        public ActivityAdapter(Context context) {
            this.context = context;
            inflater = LayoutInflater.from(context);
            activityTimeSpansMap = new LinkedHashMap<>();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == TODAY_SUMMARY_VIEW) {
                return new SummaryViewHolder(
                        context, inflater.inflate(R.layout.row_activity_summary, parent, false));

            } else if (viewType == SPINNER_VIEW) {
                return new SpinnerViewHolder(context,
                        inflater.inflate(R.layout.row_activity_spinners, parent, false));

            } else if (viewType == HEADER_VIEW) {
                return new HeaderViewHolder(
                        inflater.inflate(R.layout.row_activity_header, parent, false));

            } else if (viewType == DETAIL_VIEW) {
                return new DetailViewHolder(context,
                        inflater.inflate(R.layout.row_activity_detail, parent, false));

            } else {
                throw new IllegalArgumentException("Invalid view type: " + viewType);
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof SummaryViewHolder) {
                ((SummaryViewHolder) holder).bindItem(todaySummary);

            } else if (holder instanceof SpinnerViewHolder) {
                // TODO bind current selection
            } else if (holder instanceof HeaderViewHolder) {
                String timeSpan = (String) getItem(position);
                ActivityTimeSpan activityTimeSpan = activityTimeSpansMap.get(timeSpan);
                ((HeaderViewHolder) holder).bindItem(timeSpan, activityTimeSpan);

            } else if (holder instanceof DetailViewHolder) {
                ((DetailViewHolder) holder).bindItem((Triplet<Integer, Integer, Activity>) getItem(position));

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

            // only show today's summary and spinners if user has at least 1 activity
            if (size > 0) {
                size += 2;
            }

            return size;
        }

        private Object getItem(int position) {
            if (position == 0) {
                return TODAY_SUMMARY_ITEM;
            }

            if (position == 1) {
                return SPINNER_ITEM;
            }

            int offset = position - 2;

            for (Map.Entry<String, ActivityTimeSpan> entry : activityTimeSpansMap.entrySet()) {
                if (offset == 0) {
                    return entry.getKey();
                }

                offset--;

                if (offset < entry.getValue().getActivities().size()) {
                    return new Triplet<>(offset, entry.getValue().getActivities().size(),
                            entry.getValue().getActivities().get(offset));
                }

                offset -= entry.getValue().getActivities().size();
            }

            throw new IllegalArgumentException("Invalid position: " + position);
        }

        @Override
        public int getItemViewType(int position) {
            Object item = getItem(position);

            if (item == TODAY_SUMMARY_ITEM) {
                return TODAY_SUMMARY_VIEW;
            } else if (item == SPINNER_ITEM) {
                return SPINNER_VIEW;
            } else if (item instanceof String) {
                return HEADER_VIEW;
            } else {
                return DETAIL_VIEW;
            }
        }

        private void notifyActivitiesChanged(List<Activity> activities) {
            activityTimeSpansMap.clear();

            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM EEEE", Locale.UK);

            // iterate in reverse order since the dates must appear in descending order
            for (int i=activities.size()-1; i>=0; i--) {
                Activity activity = activities.get(i);

                Date date = new Date(activity.getStart());
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

            // compute statistics for each time span
            for (Map.Entry<String, ActivityTimeSpan> entry : activityTimeSpansMap.entrySet()) {
                double totalDistance = 0;
                long totalTime = 0;

                for (Activity activity : entry.getValue().getActivities()) {
                    totalDistance += activity.getDistance();
                    totalTime += activity.getEnd() - activity.getStart();
                }

                entry.getValue().setSummary(new StatisticsSummary(totalDistance, totalTime, 0,
                        Helpers.getStepsFromDistance(totalDistance)));
            }

            // compute today's summary
            String todayKey = sdf.format(new Date());
            if (activityTimeSpansMap.containsKey(todayKey)) {
                todaySummary = activityTimeSpansMap.get(todayKey).getSummary();
            } else {
                // TODO
                todaySummary = new StatisticsSummary(0, 0, 0, 0);
            }

            notifyDataSetChanged();
        }

        private static class ActivityTimeSpan {

            private StatisticsSummary summary;
            private List<Activity> activities;

            public ActivityTimeSpan(StatisticsSummary summary, List<Activity> activities) {
                this.summary = summary;
                this.activities = activities;
            }

            public StatisticsSummary getSummary() {
                return summary;
            }

            public void setSummary(StatisticsSummary summary) {
                this.summary = summary;
            }

            public List<Activity> getActivities() {
                return activities;
            }
        }

        private static class StatisticsSummary {

            private double distance;
            private long time;
            private int calories;
            private int steps;

            public StatisticsSummary(double distance, long time, int calories, int steps) {
                this.distance = distance;
                this.time = time;
                this.calories = calories;
                this.steps = steps;
            }

            public double getDistance() {
                return distance;
            }

            public long getTime() {
                return time;
            }

            public int getCalories() {
                return calories;
            }

            public int getSteps() {
                return steps;
            }
        }

        private static class SummaryPagerAdapter extends PagerAdapter {

            private static final int NUM_PAGES = 4;
            private Context context;
            private StatisticsSummary summary;
            private LayoutInflater inflater;

            public SummaryPagerAdapter(Context context, StatisticsSummary summary) {
                this.context = context;
                this.summary = summary;

                inflater = LayoutInflater.from(context);

                if (this.summary == null) {
                    this.summary = new StatisticsSummary(0, 0, 0, 0);
                }
            }

            public void updateSummary(StatisticsSummary newSummary) {
                summary = newSummary;
                notifyDataSetChanged();
            }

            @Override
            public Object instantiateItem(ViewGroup container, int position) {
                View view = inflater.inflate(R.layout.row_activity_summary_item, container, false);
                TextView statisticTextView = (TextView) view.findViewById(R.id.statistic_textview);
                TextView averageStatTextView =
                        (TextView) view.findViewById(R.id.average_statistic_textview);

                String statisticText = null;

                switch (position) {
                    case 0:
                        statisticText = String.format(
                                context.getString(R.string.label_stat_today_format),
                                Helpers.humanizeDistance(summary.getDistance() / 1000)
                        );
                        break;
                    case 1:
                        statisticText = String.format(
                                context.getString(R.string.label_stat_today_format),
                                Helpers.humanizeDurationInMillisToMinutes(summary.getTime())
                        );
                        break;
                    case 2:
                        statisticText = String.format(
                                context.getString(R.string.label_steps_today_format),
                                summary.getSteps()
                        );
                        break;
                    case 3:
                        statisticText = String.format(
                                context.getString(R.string.label_calories_burned_format),
                                summary.getCalories()
                        );
                        break;
                }

                statisticTextView.setText(statisticText);

                container.addView(view);

                return view;
            }

            @Override
            public void destroyItem(ViewGroup container, int position, Object view) {
                container.removeView((View) view);
            }

            /**
             * Return POSITION_NONE so that when notifyDataSetChanged() is called, the view pager
             * will remove all views and reload them all.
             */

            @Override
            public int getItemPosition(Object object) {
                return POSITION_NONE;
            }

            @Override
            public int getCount() {
                return NUM_PAGES;
            }

            /**
             * This method checks whether a particular object belongs to a given position. The
             * second parameter is of type Object and is the same as the return value from the
             * instantiateItem method.
             */

            @Override
            public boolean isViewFromObject(View view, Object object) {
                return object == view;
            }
        }

        private static class SummaryViewHolder extends RecyclerView.ViewHolder {

            private ViewPager viewPager;
            private SummaryPagerAdapter adapter;
            private CirclePageIndicator circlePageIndicator;

            public SummaryViewHolder(Context context, View itemView) {
                super(itemView);

                // find views
                viewPager = (ViewPager) itemView.findViewById(R.id.viewpager);
                circlePageIndicator =
                        (CirclePageIndicator) itemView.findViewById(R.id.circle_pager_indicator);

                // bind pager adapter
                adapter = new SummaryPagerAdapter(context, null);
                viewPager.setAdapter(adapter);

                // bind pager indicator
                circlePageIndicator.setViewPager(viewPager);

            }

            public void bindItem(StatisticsSummary summary) {
                adapter.updateSummary(summary);
            }
        }

        private static class SpinnerViewHolder extends RecyclerView.ViewHolder {

            private Context context;
            private Spinner timespanSpinner;
            private Spinner statisticSpinner;

            public SpinnerViewHolder(Context context, View itemView) {
                super(itemView);

                this.context = context;

                // find views
                timespanSpinner = (Spinner) itemView.findViewById(R.id.timespan_spinner);
                statisticSpinner = (Spinner) itemView.findViewById(R.id.statistic_spinner);

                // populate spinners
                ArrayAdapter<CharSequence> timespanAdapter = ArrayAdapter.createFromResource(context,
                        R.array.timespans, android.R.layout.simple_spinner_dropdown_item);
                timespanAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                timespanSpinner.setAdapter(timespanAdapter);

                ArrayAdapter<CharSequence> statisticAdapter = ArrayAdapter.createFromResource(context,
                        R.array.statistics, android.R.layout.simple_spinner_dropdown_item);
                statisticAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                statisticSpinner.setAdapter(statisticAdapter);
            }

            public void bindItem() {

            }
        }

        private static class HeaderViewHolder extends RecyclerView.ViewHolder {

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
                statisticTextView.setText(
                        Helpers.humanizeDistance(timeSpan.getSummary().getDistance() / 1000));
            }
        }

        private static class DetailViewHolder extends RecyclerView.ViewHolder {

            private Context context;
            private TextView infoTextView;
            private TextView descriptionTextView;
            private View topIndicatorView;
            private View bottomIndicatorView;

            private SimpleDateFormat sdf;

            public DetailViewHolder(Context context, View itemView) {
                super(itemView);

                this.context = context;

                sdf = new SimpleDateFormat("HH:mm", Locale.UK);

                // find views
                descriptionTextView = (TextView) itemView.findViewById(R.id.description_textview);
                infoTextView = (TextView) itemView.findViewById(R.id.info_textview);
                topIndicatorView = itemView.findViewById(R.id.top_indicator_view);
                bottomIndicatorView = itemView.findViewById(R.id.bottom_indicator_view);
            }

            public void bindItem(Triplet<Integer, Integer, Activity> offsetNumActivitiesActivityTriplet) {
                String activityType;

                switch (offsetNumActivitiesActivityTriplet.c.getType()) {
                    case WAIT_OR_WALK:
                        activityType = context.getString(R.string.action_wait_or_walk);
                        break;

                    default:
                        throw new IllegalArgumentException("Invalid activity type: " +
                                offsetNumActivitiesActivityTriplet.c.getType().getValue());
                }

                infoTextView.setText(String.format(context.getString(R.string.label_activity_info),
                        activityType, sdf.format(offsetNumActivitiesActivityTriplet.c.getStart())));

                descriptionTextView.setText(offsetNumActivitiesActivityTriplet.c.getDescription());

                if (offsetNumActivitiesActivityTriplet.b == 1) {
                    topIndicatorView.setVisibility(View.INVISIBLE);
                    bottomIndicatorView.setVisibility(View.INVISIBLE);
                } else {
                    if (offsetNumActivitiesActivityTriplet.a == 0) {
                        topIndicatorView.setVisibility(View.INVISIBLE);
                        bottomIndicatorView.setVisibility(View.VISIBLE);
                    } else if (offsetNumActivitiesActivityTriplet.a ==
                            offsetNumActivitiesActivityTriplet.b - 1) {

                        topIndicatorView.setVisibility(View.VISIBLE);
                        bottomIndicatorView.setVisibility(View.INVISIBLE);
                    } else {
                        topIndicatorView.setVisibility(View.VISIBLE);
                        bottomIndicatorView.setVisibility(View.VISIBLE);
                    }
                }
            }
        }
    }
}
