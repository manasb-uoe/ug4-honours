package com.enthusiast94.edinfit.ui.home.fragments;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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

import com.arasthel.asyncjob.AsyncJob;
import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models.Disruption;
import com.enthusiast94.edinfit.network.BaseService;
import com.enthusiast94.edinfit.network.DisruptionsService;
import com.enthusiast94.edinfit.utils.SimpleDividerItemDecoration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by manas on 06-02-2016.
 */
public class DisruptionsFragment extends Fragment {

    private static final String TAG = DisruptionsFragment.class.getSimpleName();
    private List<Disruption> disruptions;
    private RecyclerView disruptionsRecyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private DisruptionsAdapter disruptionsAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_disruptions, container, false);
        findViews(view);

        // setup swipe refresh layout
        swipeRefreshLayout.setColorSchemeResources(R.color.accent);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {
                loadDisruptions();
            }
        });

        // setup disruptions recycler view
        disruptionsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        disruptionsRecyclerView.addItemDecoration(new SimpleDividerItemDecoration(getActivity()));
        disruptionsAdapter = new DisruptionsAdapter(getActivity());
        disruptionsRecyclerView.setAdapter(disruptionsAdapter);

        if (disruptions == null) {
            loadDisruptions();
        } else {
            disruptionsAdapter.notifyDisruptionsChanged(disruptions);
        }

        return view;
    }

    private void findViews(View view) {
        disruptionsRecyclerView = (RecyclerView) view.findViewById(R.id.disruptions_recyclervieew);
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_layout);
    }

    private void loadDisruptions() {
        new AsyncJob.AsyncJobBuilder<BaseService.Response<List<Disruption>>>()
                .doInBackground(new AsyncJob.AsyncAction<BaseService.Response<List<Disruption>>>() {
                    @Override
                    public BaseService.Response<List<Disruption>> doAsync() {
                        return DisruptionsService.getInstance().getDisriptions();
                    }
                }).doWhenFinished(new AsyncJob.AsyncResultAction<BaseService.Response<List<Disruption>>>() {
            @Override
            public void onResult(BaseService.Response<List<Disruption>> response) {
                if (getActivity() == null) {
                    return;
                }

                if (!response.isSuccessfull()) {
                    Toast.makeText(getActivity(), response.getError(), Toast.LENGTH_SHORT)
                            .show();
                    return;
                }

                disruptions = response.getBody();
                disruptionsAdapter.notifyDisruptionsChanged(disruptions);
            }
        }).create().start();
    }

    private static class DisruptionsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private List<Disruption> disruptions;
        private Context context;
        private LayoutInflater inflater;

        public DisruptionsAdapter(Context context) {
            this.context = context;
            inflater = LayoutInflater.from(context);
            disruptions = new ArrayList<>();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new DisruptionViewHolder(inflater.inflate(R.layout.row_disruption, parent, false));
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            ((DisruptionViewHolder) holder).bindItem(disruptions.get(position));
        }

        @Override
        public int getItemCount() {
            return disruptions.size();
        }

        public void notifyDisruptionsChanged(List<Disruption> disruptions) {
            this.disruptions = disruptions;
            notifyDataSetChanged();
        }

        private class DisruptionViewHolder extends RecyclerView.ViewHolder
                implements View.OnClickListener {

            private TextView typeTextView;
            private TextView categoryTextView;
            private TextView updatedAtTextView;
            private TextView summaryTextView;
            private TextView servicesAffectedTextView;
            private String webLink;
            private SimpleDateFormat sdf;

            public DisruptionViewHolder(View itemView) {
                super(itemView);

                sdf = new SimpleDateFormat("EEE, dd MMM", Locale.UK);

                // find views
                typeTextView = (TextView) itemView.findViewById(R.id.type_textview);
                categoryTextView = (TextView) itemView.findViewById(R.id.category_textview);
                updatedAtTextView = (TextView) itemView.findViewById(R.id.updated_at_textview);
                summaryTextView = (TextView) itemView.findViewById(R.id.summary_textview);
                servicesAffectedTextView = (TextView) itemView.findViewById(R.id.services_affected_textview);

                // bind event handlers
                itemView.setOnClickListener(this);
            }

            public void bindItem(Disruption disruption) {
                this.webLink = disruption.getWebLink();

                String type = disruption.getType().substring(0, 1).toUpperCase() +
                        disruption.getType().substring(1, disruption.getType().length());
                String category = disruption.getCategory().substring(0, 1).toUpperCase() +
                        disruption.getCategory().substring(1, disruption.getCategory().length());
                String updatedAt = sdf.format(new Date(disruption.getUpdatedAt() * 1000));
                String servicesAffected = context.getString(R.string.services_affected) + " ";
                List<String> servicesAffectedList = disruption.getServicesAffected();
                for (int i=0; i<servicesAffectedList.size(); i++) {
                    servicesAffected += servicesAffectedList.get(i);
                    if (i != servicesAffectedList.size()-1) {
                        servicesAffected += ", ";
                    }
                }

                typeTextView.setText(type);
                categoryTextView.setText(category);
                updatedAtTextView.setText(updatedAt);
                summaryTextView.setText(disruption.getSummary());
                servicesAffectedTextView.setText(servicesAffected);
            }

            @Override
            public void onClick(View v) {
                // open web link in browser
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(webLink));
                context.startActivity(intent);
            }
        }
    }
}
