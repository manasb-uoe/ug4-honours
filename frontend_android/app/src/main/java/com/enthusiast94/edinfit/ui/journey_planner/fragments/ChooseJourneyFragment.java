package com.enthusiast94.edinfit.ui.journey_planner.fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.arasthel.asyncjob.AsyncJob;
import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models.Directions;
import com.enthusiast94.edinfit.models.Journey;
import com.enthusiast94.edinfit.network.BaseService;
import com.enthusiast94.edinfit.network.DirectionsService;
import com.enthusiast94.edinfit.ui.journey_planner.activities.JourneyDetailsActivity;
import com.enthusiast94.edinfit.ui.journey_planner.services.CountdownNotificationService;
import com.enthusiast94.edinfit.utils.Helpers;
import com.enthusiast94.edinfit.utils.SimpleDividerItemDecoration;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.PolyUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by manas on 30-01-2016.
 */
public class ChooseJourneyFragment extends Fragment {

    public static final String TAG = ChooseJourneyFragment.class.getSimpleName();
    private static final String EXTRA_JOURNEYS = "journeys";

    private RecyclerView journeysRecyclerView;
    private List<Journey> journeys;

    public static ChooseJourneyFragment newInstance(ArrayList<Journey> journeys) {
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(EXTRA_JOURNEYS, journeys);
        ChooseJourneyFragment instance = new ChooseJourneyFragment();
        instance.setArguments(bundle);

        return instance;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_choose_journey, container, false);
        findViews(view);

        if (journeys == null) {
            Bundle arguments = getArguments();
            journeys = arguments.getParcelableArrayList(EXTRA_JOURNEYS);
        }

        journeysRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        JourneysAdapter journeyAdapter = new JourneysAdapter(getActivity());
        journeysRecyclerView.setAdapter(journeyAdapter);
        journeyAdapter.notifyJourneysChanged(journeys);
        journeysRecyclerView.addItemDecoration(new SimpleDividerItemDecoration(getActivity()));

        return view;
    }

    private void findViews(View view) {
        journeysRecyclerView = (RecyclerView) view.findViewById(R.id.journeys_recyclerview);
    }

    private static class JourneysAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private Context context;
        private List<Journey> journeys;
        private LayoutInflater inflater;

        public JourneysAdapter(Context context) {
            this.context = context;
            this.journeys = new ArrayList<>();
            this.inflater = LayoutInflater.from(context);
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new JourneyViewHolder(inflater.inflate(R.layout.row_journey_option, parent, false));
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            ((JourneyViewHolder) holder).bindItem(journeys.get(position));
        }

        @Override
        public int getItemCount() {
            return journeys.size();
        }

        public void notifyJourneysChanged(List<Journey> journeys) {
            this.journeys = journeys;
            notifyDataSetChanged();
        }

        private class JourneyViewHolder extends RecyclerView.ViewHolder
                implements View.OnClickListener {

            private Journey journey;
            private TextView startTimeTextView;
            private TextView finishTimeTextView;
            private TextView durationTextView;
            private TextView walkDurationTextView;
            private LinearLayout summaryContainer;
            private SimpleDateFormat sdf;

            public JourneyViewHolder(View itemView) {
                super(itemView);

                sdf = new SimpleDateFormat("HH:mm", Locale.UK);

                // find views
                startTimeTextView = (TextView) itemView.findViewById(R.id.start_time_textview);
                finishTimeTextView = (TextView) itemView.findViewById(R.id.finish_time_textview);
                durationTextView = (TextView) itemView.findViewById(R.id.duration_textview);
                walkDurationTextView = (TextView) itemView.findViewById(R.id.walk_duration_textview);
                summaryContainer = (LinearLayout) itemView.findViewById(R.id.summary_container);

                // bind event listeners
                itemView.setOnClickListener(this);
            }

            public void bindItem(Journey journey) {
                this.journey = journey;

                startTimeTextView.setText(sdf.format(new Date(journey.getStartTime() * 1000)));
                finishTimeTextView.setText(sdf.format(new Date(journey.getFinishTime() * 1000)));
                durationTextView.setText(Html.fromHtml("<strong>" + journey.getDuration() +
                        "</strong> min"));

                summaryContainer.removeAllViews();
                List<Journey.Leg> legs = journey.getLegs();
                long walkDuration = 0;
                for (int i=0; i<legs.size(); i++) {
                    Journey.Leg leg = legs.get(i);

                    if (leg instanceof Journey.WalkLeg) {
                        ImageView walkImageView = new ImageView(context);
                        walkImageView.setBackground(ContextCompat.getDrawable(context,
                                R.drawable.ic_directions_walk_black_24dp));
                        walkImageView.setLayoutParams(new RelativeLayout.LayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                (int) Helpers.convertDpToPixel(context, 20))
                        );
                        summaryContainer.addView(walkImageView);

                        walkDuration += leg.getFinishPoint().getTimestamp() -
                                leg.getStartPoint().getTimestamp();

                    } else if (leg instanceof Journey.BusLeg) {
                        TextView serviceTextView = new TextView(context);
                        serviceTextView.setText(((Journey.BusLeg) leg).getServiceName());
                        serviceTextView.setLayoutParams(new RelativeLayout.LayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT));
                        serviceTextView.setBackground(ContextCompat.getDrawable(context,
                                R.drawable.service_name_background));
                        serviceTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                        serviceTextView.setTextColor(ContextCompat.getColor(context,
                                android.R.color.black));
                        summaryContainer.addView(serviceTextView);

                    } else {
                        throw new RuntimeException("Invalid leg type: " +
                                leg.getClass().getSimpleName());
                    }

                    // add right arrow between walk/bus indicators
                    if (i != legs.size()-1) {
                        summaryContainer.addView(getArrowImageView());
                    }
                }

                walkDurationTextView.setText("(" + String.format(context.getString(R.string.label_walk_duration_format),
                        Helpers.humanizeDurationInMillisToMinutes(walkDuration * 1000)) + ")");
            }

            private ImageView getArrowImageView() {
                ImageView arrowImageView = new ImageView(context);
                arrowImageView.setImageResource(R.drawable.ic_keyboard_arrow_right_black_24dp);
                return arrowImageView;
            }

            @Override
            public void onClick(View v) {
                final ProgressDialog progressDialog = new ProgressDialog(context);
                progressDialog.setMessage(context.getString(R.string.label_please_waitt));
                progressDialog.show();

                // attach polyline to all WalkLegs in selected journey
                AsyncJob.doInBackground(new AsyncJob.OnBackgroundJob() {
                    @Override
                    public void doOnBackground() {
                        for (Journey.Leg leg : journey.getLegs()) {
                            if (leg instanceof Journey.WalkLeg) {
                                final BaseService.Response<Directions> directionsResponse =
                                        DirectionsService.getInstance().getWalkingDirections(
                                                leg.getStartPoint().getLatLng(), leg.getFinishPoint().getLatLng()
                                        );
                                if (!directionsResponse.isSuccessfull()) {
                                    AsyncJob.doOnMainThread(new AsyncJob.OnMainThreadJob() {
                                        @Override
                                        public void doInUIThread() {
                                            progressDialog.dismiss();
                                            Toast.makeText(context, directionsResponse.getError(), Toast.LENGTH_SHORT)
                                                    .show();
                                        }
                                    });

                                    return;
                                }

                                List<LatLng> latLngs = new ArrayList<>();
                                for (Directions.Point point : directionsResponse.getBody().getOverviewPoints()) {
                                    latLngs.add(new LatLng(point.getLatitude(), point.getLongitude()));
                                }

                                leg.setPolyline(PolyUtil.encode(latLngs));
                            }
                        }

                        AsyncJob.doOnMainThread(new AsyncJob.OnMainThreadJob() {
                            @Override
                            public void doInUIThread() {
                                progressDialog.dismiss();
                                context.startActivity(JourneyDetailsActivity.getStartActivityIntent(context, journey));
                            }
                        });
                    }
                });
            }
        }
    }
}
