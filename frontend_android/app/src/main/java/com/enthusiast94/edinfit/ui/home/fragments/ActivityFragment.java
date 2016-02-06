package com.enthusiast94.edinfit.ui.home.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.arasthel.asyncjob.AsyncJob;
import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models.Activity;
import com.enthusiast94.edinfit.ui.home.events.OnActivityClickedEvent;
import com.enthusiast94.edinfit.ui.journey_planner.activities.JourneyPlannerActivity;
import com.enthusiast94.edinfit.ui.wait_or_walk_mode.activities.NewActivityActivity;
import com.enthusiast94.edinfit.utils.Helpers;
import com.enthusiast94.edinfit.utils.Triplet;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.viewpagerindicator.CirclePageIndicator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.greenrobot.event.EventBus;

/**
 * Created by manas on 01-10-2015.
 */
public class ActivityFragment extends Fragment {

    public static final String TAG = ActivityFragment.class.getSimpleName();

    private SwipeRefreshLayout swipeRefreshLayout;
    private ActivityAdapter activityAdapter;
    private List<Activity> activities;
    private ActivityAdapter.TimespanEnum selectedTimespan;
    private ActivityAdapter.StatisticEnum selectedStatistic;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_activity, container, false);

        final Handler handler = new Handler();

        // find views
        RecyclerView activityRecyclerView = (RecyclerView) view.findViewById(R.id.activity_recyclerview);
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_layout);
        final FloatingActionMenu fabMenu = (FloatingActionMenu) view.findViewById(R.id.fab_menu);
        final FloatingActionButton waitOrWalkFab =
                (FloatingActionButton) view.findViewById(R.id.wait_or_walk_fab);
        FloatingActionButton journeyPlannerFab =
                (FloatingActionButton) view.findViewById(R.id.journey_planner_fab);

        // setup swipe refresh layout
        swipeRefreshLayout.setColorSchemeResources(R.color.accent);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {
                loadActivities();
            }
        });

        // setup activity recycler view
        activityRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        activityAdapter = new ActivityAdapter(getActivity());
        activityRecyclerView.setAdapter(activityAdapter);

        // setup floating action menu item clicks
        waitOrWalkFab.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                final Intent startActivityIntent =
                        new Intent(getActivity(), NewActivityActivity.class);

                fabMenu.close(true);

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startActivity(startActivityIntent);
                    }
                }, 300);

            }
        });
        journeyPlannerFab.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                final Intent startActivityIntent =
                        new Intent(getActivity(), JourneyPlannerActivity.class);

                fabMenu.close(true);

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startActivity(startActivityIntent);
                    }
                }, 300);
            }
        });

        if (activities == null) {
            loadActivities();
        } else {
            activityAdapter.notifyActivitiesChanged(activities, selectedTimespan, selectedStatistic);
        }

        return view;
    }

    @Override
    public void onPause() {
        super.onPause();

        // retain selected time span and statistic so that it can be set again after config change
        selectedTimespan = activityAdapter.getSelectedTimeSpan();
        selectedStatistic = activityAdapter.getSelectedStatistic();
    }

    private void loadActivities() {
        new AsyncJob.AsyncJobBuilder<List<Activity>>()
                .doInBackground(new AsyncJob.AsyncAction<List<Activity>>() {
                    @Override
                    public List<Activity> doAsync() {
                        return Activity.getAll();
                    }
                })
                .doWhenFinished(new AsyncJob.AsyncResultAction<List<Activity>>() {
                    @Override
                    public void onResult(List<Activity> activities) {
                        if (getActivity() == null) {
                            return;
                        }

                        setRefreshIndicatorVisiblity(false);

                        ActivityFragment.this.activities = activities;

                        activityAdapter.notifyActivitiesChanged(activities, selectedTimespan,
                                selectedStatistic);
                    }
                }).create().start();
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
        private List<Activity> activities;
        private Map<String, ActivityTimeSpan> activityTimeSpansMap;
        private StatisticsSummary todaySummary;
        private TimespanEnum selectedTimeSpan;
        private StatisticEnum selectedStatistic;

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
                ((SpinnerViewHolder) holder).bindItem(selectedTimeSpan, selectedStatistic);
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

        private void notifyActivitiesChanged(List<Activity> activities,
                                             TimespanEnum timeSpan, StatisticEnum statistic) {
            this.activities = activities;
            if (timeSpan != null) {
                selectedTimeSpan = timeSpan;
            } else {

                selectedTimeSpan = TimespanEnum.DAY;
            }
            if (statistic != null) {
                selectedStatistic = statistic;
            } else {
                selectedStatistic = StatisticEnum.DISTANCE;
            }

            activityTimeSpansMap.clear();

            // iterate in reverse order since the dates must appear in descending order
            for (int i=activities.size()-1; i>=0; i--) {
                Activity activity = activities.get(i);
                String timeSpanText = null;

                switch (selectedTimeSpan) {
                    case DAY:
                        SimpleDateFormat sdfDay = new SimpleDateFormat("dd MMM EEEE", Locale.UK);
                        timeSpanText = sdfDay.format(activity.getStart());
                        break;
                    case MONTH:
                        SimpleDateFormat sdfMonth = new SimpleDateFormat("MMMM", Locale.UK);
                        timeSpanText = sdfMonth.format(activity.getStart());
                        break;
                    case WEEK:
                        timeSpanText = Helpers.getStartAndEndOfWeek(activity.getStart());
                        break;
                }

                if (activityTimeSpansMap.containsKey(timeSpanText)) {
                    activityTimeSpansMap.get(timeSpanText).getActivities().add(activity);
                } else {
                    List<Activity> activitiesList = new ArrayList<>();
                    activitiesList.add(activity);
                    ActivityTimeSpan activityTimeSpan =
                            new ActivityTimeSpan(null /* summary is computed later */, activitiesList);
                    activityTimeSpansMap.put(timeSpanText, activityTimeSpan);
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
            SimpleDateFormat sdfDay = new SimpleDateFormat("dd MMM EEEE", Locale.UK);
            String todayKey = sdfDay.format(new Date());
            if (activityTimeSpansMap.containsKey(todayKey)) {
                todaySummary = activityTimeSpansMap.get(todayKey).getSummary();
            } else {
                double totalDistance = 0;
                long totalTime = 0;

                for (int i=activities.size()-1; i>=0; i--) {
                    Activity activity = activities.get(i);
                    String day = sdfDay.format(activity.getStart());
                    if (day.equals(todayKey)) {
                        totalDistance += activity.getDistance();
                        totalTime += activity.getEnd() - activity.getStart();
                    } else {
                        // can break as soon as the day changes since today's activities will be
                        // at the start of the list
                        break;
                    }
                }

                todaySummary = new StatisticsSummary(totalDistance, totalTime, 0,
                        Helpers.getStepsFromDistance(totalDistance));
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

        private class SummaryViewHolder extends RecyclerView.ViewHolder {

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

        private class SpinnerViewHolder extends RecyclerView.ViewHolder {

            private Spinner timespanSpinner;
            private Spinner statisticSpinner;
            private TimespanEnum[] timespanEnums;
            private StatisticEnum[] statisticEnums;

            public SpinnerViewHolder(Context context, View itemView) {
                super(itemView);

                // find views
                timespanSpinner = (Spinner) itemView.findViewById(R.id.timespan_spinner);
                statisticSpinner = (Spinner) itemView.findViewById(R.id.statistic_spinner);

                // populate spinners
                timespanEnums = TimespanEnum.values();
                String[] timespans = new String[timespanEnums.length];
                for (int i=0; i<timespans.length; i++) {
                    timespans[i] = timespanEnums[i].getValue();
                }
                ArrayAdapter<CharSequence> timespanAdapter = new ArrayAdapter<CharSequence>(context,
                        android.R.layout.simple_spinner_dropdown_item, timespans);
                timespanSpinner.setAdapter(timespanAdapter);

                statisticEnums = StatisticEnum.values();
                String[] statistics = new String[statisticEnums.length];
                for (int i=0; i<statisticEnums.length; i++) {
                    statistics[i] = statisticEnums[i].getValue();
                }
                ArrayAdapter<CharSequence> statisticAdapter = new ArrayAdapter<CharSequence>(context,
                        android.R.layout.simple_spinner_dropdown_item, statistics);
                statisticAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                statisticSpinner.setAdapter(statisticAdapter);

                // bind event listeners
                timespanSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        selectedTimeSpan = timespanEnums[position];
                        notifyActivitiesChanged(activities, selectedTimeSpan, selectedStatistic);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {}
                });
                statisticSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        selectedStatistic = statisticEnums[position];
                        notifyActivitiesChanged(activities, selectedTimeSpan, selectedStatistic);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {}
                });
            }

            public void bindItem(TimespanEnum timespanEnum, StatisticEnum statisticEnum) {
                for (int i=0; i<timespanEnums.length; i++) {
                    if (timespanEnums[i] == timespanEnum) {
                        timespanSpinner.setSelection(i);
                        break;
                    }
                }

                for (int i=0; i<statisticEnums.length; i++) {
                    if (statisticEnums[i] == statisticEnum) {
                        statisticSpinner.setSelection(i);
                        break;
                    }
                }
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
                String statisticText = null;

                switch (selectedStatistic) {
                    case DISTANCE:
                        statisticText = Helpers.humanizeDistance(
                                timeSpan.getSummary().getDistance() / 1000);
                        break;
                    case TIME:
                        statisticText = Helpers.humanizeDurationInMillisToMinutes(
                                timeSpan.getSummary().getTime());
                        break;
                    case CALORIES:
                        statisticText = String.format(context.getString(
                                R.string.label_calories_format), timeSpan.getSummary().getCalories());
                        break;
                    case STEPS:
                        statisticText = String.format(context.getString(
                                R.string.label_steps_format), timeSpan.getSummary().getSteps());
                        break;
                }

                statisticTextView.setText(statisticText);
            }
        }

        private class DetailViewHolder extends RecyclerView.ViewHolder {

            private TextView infoTextView;
            private TextView descriptionTextView;
            private View topIndicatorView;
            private View bottomIndicatorView;

            private Context context;
            private Activity activity;
            private SimpleDateFormat sdf;

            public DetailViewHolder(final Context context, View itemView) {
                super(itemView);

                this.context = context;

                sdf = new SimpleDateFormat("HH:mm", Locale.UK);

                // find views
                descriptionTextView = (TextView) itemView.findViewById(R.id.description_textview);
                infoTextView = (TextView) itemView.findViewById(R.id.info_textview);
                topIndicatorView = itemView.findViewById(R.id.top_indicator_view);
                bottomIndicatorView = itemView.findViewById(R.id.bottom_indicator_view);


                // bind event listeners
                itemView.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        EventBus.getDefault().post(new OnActivityClickedEvent(activity));
                    }
                });
            }

            public void bindItem(Triplet<Integer, Integer, Activity> offsetNumActivitiesActivityTriplet) {
                this.activity = offsetNumActivitiesActivityTriplet.c;

                infoTextView.setText(String.format(context.getString(R.string.label_activity_info_format),
                        Helpers.getActivityTypeText(context, offsetNumActivitiesActivityTriplet.c.getType()),
                                sdf.format(offsetNumActivitiesActivityTriplet.c.getStart())));

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

        private enum TimespanEnum {
            MONTH("Month view"), WEEK("Week view"), DAY("Day view");

            private String value;

            TimespanEnum(String value) {
                this.value = value;
            }

            public String getValue() {
                return value;
            }
        }

        private enum StatisticEnum {
            TIME("Activity time"), DISTANCE("Distance"), CALORIES("Calories"), STEPS("Steps");

            private String value;

            StatisticEnum(String value) {
                this.value = value;
            }

            public String getValue() {
                return value;
            }
        }

        public TimespanEnum getSelectedTimeSpan() {
            return selectedTimeSpan;
        }

        public StatisticEnum getSelectedStatistic() {
            return selectedStatistic;
        }
    }
}
