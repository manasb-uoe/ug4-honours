package com.enthusiast94.edinfit.ui.journey_planner.fragments;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models.Journey;
import com.enthusiast94.edinfit.models.Stop;
import com.enthusiast94.edinfit.utils.Helpers;
import com.enthusiast94.edinfit.utils.SimpleDividerItemDecoration;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.ui.IconGenerator;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by manas on 30-01-2016.
 */
public class JourneyDetailsFragment extends Fragment {

    public static final String TAG = JourneyDetailsFragment.class.getSimpleName();
    private static final String MAPVIEW_SAVE_STATE = "mapViewSaveState";
    private static final String EXTRA_JOURNEY = "journey";
    private static final int VIEW_WALK_LEG = 0;
    private static final int VIEW_BUS_LEG = 1;

    private RecyclerView journeyLegsRecyclerView;
    private MapView mapView;

    private GoogleMap map;

    public static JourneyDetailsFragment newInstance(Journey journey) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(EXTRA_JOURNEY, journey);
        JourneyDetailsFragment instance = new JourneyDetailsFragment();
        instance.setArguments(bundle);
        return instance;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_journey_details, container, false);
        findViews(view);

        final Journey journey = getArguments().getParcelable(EXTRA_JOURNEY);

        // setup map
        Bundle mapViewSavedInstanceState = savedInstanceState != null ?
                savedInstanceState.getBundle(MAPVIEW_SAVE_STATE) : null;
        mapView.onCreate(mapViewSavedInstanceState);
        map = mapView.getMap();
        map.getUiSettings().setMyLocationButtonEnabled(true);
        map.setMyLocationEnabled(true);
        map.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                updateMap(journey.getLegs());
            }
        });
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(Helpers.getEdinburghLatLng(getActivity()),
                12));

        // setup recycler view
        journeyLegsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        JourneyLegsAdapter adapter = new JourneyLegsAdapter(getActivity());
        journeyLegsRecyclerView.setAdapter(adapter);
        journeyLegsRecyclerView.addItemDecoration(new SimpleDividerItemDecoration(getActivity()));
        adapter.notifyLegsChanged(journey.getLegs());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        //This MUST be done before saving any of your own or your base class's variables
        Bundle mapViewSaveState = new Bundle(outState);
        mapView.onSaveInstanceState(mapViewSaveState);
        outState.putBundle(MAPVIEW_SAVE_STATE, mapViewSaveState);
        //Add any other variables here.
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    private void findViews(View view) {
        journeyLegsRecyclerView = (RecyclerView) view.findViewById(R.id.journey_legs_recyclerview);
        mapView = (MapView) view.findViewById(R.id.map_view);
    }

    private void updateMap(List<Journey.Leg> legs) {
        map.clear();

        int busLegPolylineColor = ContextCompat.getColor(getActivity(), R.color.primary);
        int walkLegPolylineColor = ContextCompat.getColor(getActivity(), R.color.blue_500);
        float polylineWidth = getActivity().getResources().getDimension(R.dimen.polyline_width);
        Bitmap stopMarkerIcon = Helpers.getMarkerIcon(getActivity(), R.drawable.stop_marker);

        for (int i=0; i<legs.size(); i++) {
            Journey.Leg leg = legs.get(i);

            if (i == 0 && legs.size() == 1 && leg instanceof Journey.WalkLeg) {
                IconGenerator iconGenerator = new IconGenerator(getActivity());
                iconGenerator.setStyle(IconGenerator.STYLE_GREEN);
                Bitmap startIconBitmap = iconGenerator.makeIcon(getString(R.string.label_start));
                map.addMarker(new MarkerOptions()
                        .icon(BitmapDescriptorFactory.fromBitmap(startIconBitmap))
                        .position(leg.getStartPoint().getLatLng()));

                IconGenerator iconGenerator2 = new IconGenerator(getActivity());
                iconGenerator2.setStyle(IconGenerator.STYLE_PURPLE);
                Bitmap finishIconBitmap = iconGenerator2.makeIcon(getString(R.string.finish));;
                map.addMarker(new MarkerOptions()
                        .icon(BitmapDescriptorFactory.fromBitmap(finishIconBitmap))
                        .position(leg.getFinishPoint().getLatLng()));

                map.animateCamera(CameraUpdateFactory.newLatLngZoom(leg.getStartPoint().getLatLng(), 14));
            } else if (i == 0) {
                IconGenerator iconGenerator = new IconGenerator(getActivity());
                iconGenerator.setStyle(IconGenerator.STYLE_GREEN);
                Bitmap startIconBitmap = iconGenerator.makeIcon(getString(R.string.label_start));
                map.addMarker(new MarkerOptions()
                        .icon(BitmapDescriptorFactory.fromBitmap(startIconBitmap))
                        .position(leg.getStartPoint().getLatLng()));

                map.animateCamera(CameraUpdateFactory.newLatLngZoom(leg.getStartPoint().getLatLng(), 14));
            } else if (i == legs.size()-1) {
                IconGenerator iconGenerator = new IconGenerator(getActivity());
                iconGenerator.setStyle(IconGenerator.STYLE_PURPLE);
                Bitmap finishIconBitmap = iconGenerator.makeIcon(getString(R.string.finish));
                MarkerOptions markerOptions = new MarkerOptions()
                        .icon(BitmapDescriptorFactory.fromBitmap(finishIconBitmap))
                        .position(leg.getFinishPoint().getLatLng());
                map.addMarker(markerOptions);
            }

            PolylineOptions polylineOptions = new PolylineOptions()
                    .addAll(leg.getLatLngs())
                    .width(polylineWidth);

            if (leg instanceof Journey.BusLeg) {
                polylineOptions.color(busLegPolylineColor);

                // add start and finish stop markers
                Stop startStop = leg.getStartPoint().getStop();
                Stop finishStop = leg.getFinishPoint().getStop();

                map.addMarker(new MarkerOptions()
                        .position(startStop.getPosition())
                        .icon(BitmapDescriptorFactory.fromBitmap(stopMarkerIcon))
                        .title(startStop.getName()));
                map.addMarker(new MarkerOptions()
                        .position(finishStop.getPosition())
                        .icon(BitmapDescriptorFactory.fromBitmap(stopMarkerIcon))
                        .title(finishStop.getName()));

                // add markers for stops on route
                for (Stop stop : ((Journey.BusLeg) leg).getStopsOnRoute()) {
                    map.addMarker(new MarkerOptions()
                            .position(stop.getPosition())
                            .icon(BitmapDescriptorFactory.fromBitmap(stopMarkerIcon))
                            .title(stop.getName()));
                }

            } else if (leg instanceof Journey.WalkLeg) {
                polylineOptions.color(walkLegPolylineColor);
            }

            map.addPolyline(polylineOptions);
        }
    }

    private static class JourneyLegsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private List<Journey.Leg> legs;
        private LayoutInflater inflater;
        private SimpleDateFormat sdf;

        public JourneyLegsAdapter(Context context) {
            inflater = LayoutInflater.from(context);
            sdf = new SimpleDateFormat("HH:mm", Locale.UK);
        }

        @Override
        public int getItemViewType(int position) {
            Journey.Leg leg = legs.get(position);
            if (leg instanceof Journey.WalkLeg) {
                return VIEW_WALK_LEG;
            } else if (leg instanceof Journey.BusLeg) {
                return VIEW_BUS_LEG;
            } else {
                throw new IllegalArgumentException("Invalid position: " + position);
            }
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == VIEW_WALK_LEG) {
                return new WalkLegViewHolder(inflater.inflate(R.layout.row_walk_leg, parent, false));
            } else {
                return new BusLegViewHolder(inflater.inflate(R.layout.row_bus_leg, parent, false));
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof WalkLegViewHolder) {
                ((WalkLegViewHolder) holder).bindItem((Journey.WalkLeg) legs.get(position));
            } else {
                ((BusLegViewHolder) holder).bindItem((Journey.BusLeg) legs.get(position));
            }
        }

        @Override
        public int getItemCount() {
            return legs.size();
        }

        public void notifyLegsChanged(List<Journey.Leg> legs) {
            this.legs = legs;
            notifyDataSetChanged();
        }

        private class WalkLegViewHolder extends RecyclerView.ViewHolder {

            private TextView startTimeTextView;
            private TextView durationTextView;
            private TextView instructionTetView;

            public WalkLegViewHolder(View itemView) {
                super(itemView);

                // find views
                startTimeTextView = (TextView) itemView.findViewById(R.id.start_time_textview);
                durationTextView = (TextView) itemView.findViewById(R.id.duration_textview);
                instructionTetView = (TextView) itemView.findViewById(R.id.instruction_textview);
            }

            public void bindItem(Journey.WalkLeg walkLeg) {
                Journey.Point startPoint = walkLeg.getStartPoint();
                Journey.Point finishPoint = walkLeg.getFinishPoint();

                startTimeTextView.setText(sdf.format(new Date(startPoint.getTimestamp() * 1000)));
                durationTextView.setText(Helpers.humanizeDurationInMillisToMinutes(
                        (finishPoint.getTimestamp() - startPoint.getTimestamp()) * 1000));
                instructionTetView.setText(Html.fromHtml("Walk from <strong> " + startPoint.getName() +
                        "</strong> to <strong> " + finishPoint.getName() + "</strong>."));
            }
        }

        private class BusLegViewHolder extends RecyclerView.ViewHolder {

            private TextView startTimeTextView;
            private TextView durationTextView;
            private TextView instructionTetView;

            public BusLegViewHolder(View itemView) {
                super(itemView);

                // find views
                startTimeTextView = (TextView) itemView.findViewById(R.id.start_time_textview);
                durationTextView = (TextView) itemView.findViewById(R.id.duration_textview);
                instructionTetView = (TextView) itemView.findViewById(R.id.instruction_textview);
            }

            public void bindItem(Journey.BusLeg busLeg) {
                Journey.Point startPoint = busLeg.getStartPoint();
                Journey.Point finishPoint = busLeg.getFinishPoint();

                startTimeTextView.setText(sdf.format(new Date(startPoint.getTimestamp() * 1000)));
                durationTextView.setText(Helpers.humanizeDurationInMillisToMinutes(
                        (finishPoint.getTimestamp() - startPoint.getTimestamp()) * 1000));
                instructionTetView.setText(Html.fromHtml("Board service <strong> " +
                        busLeg.getServiceName() + "</strong>. Stay on bus for <strong>" +
                busLeg.getStopsOnRoute().size() + " stops</strong> and then leave at <strong>" +
                finishPoint.getStop().getName() + "</strong>."));
            }
        }
    }
}
