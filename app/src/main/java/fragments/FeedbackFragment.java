package fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.app.Fragment;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

import Modules.DirectionFinderListener;
import Modules.Route;
import asc.clemson.electricfeedback.R;

public class FeedbackFragment extends Fragment implements OnMapReadyCallback, DirectionFinderListener{
    private static View view;
    private ArrayList <LatLng> routeArray;
    private EditText optionalText;

    GoogleMap mMap;




    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        try {
            view = inflater.inflate(R.layout.fragment_feedback, container, false);
        } catch (android.view.InflateException e) {
            Toast.makeText(getActivity(), "PROBLEM", Toast.LENGTH_SHORT).show();
        }

        Bundle bundle = getArguments();
        routeArray = bundle.getParcelableArrayList("trackedBundle");

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FloatingActionButton fab = getView().findViewById(R.id.floatingActionButton);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //TODO: call database service
            }
        });

        optionalText = getView().findViewById(R.id.optionalText);

        MapFragment fragment = (MapFragment) getChildFragmentManager().findFragmentById(R.id.mapFeedback);
        fragment.getMapAsync(this);
    }


    @Override
    public void onDirectionFinderStart() {

    }

    @Override
    public void onDirectionFinderSuccess(List<Route> route) {

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;


    }
}
