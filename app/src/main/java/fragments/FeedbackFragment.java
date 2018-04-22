package fragments;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.app.Fragment;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

import Modules.DirectionFinder;
import Modules.DirectionFinderListener;
import Modules.Route;
import asc.clemson.electricfeedback.R;

public class FeedbackFragment extends Fragment implements OnMapReadyCallback, DirectionFinderListener{
    private static View view;
    private ArrayList <LatLng> routeArray;
    private EditText optionalText;
    private List<Route> routes;
    private int numOfRoutes = 2;
    private List<Polyline> polylinePaths = new ArrayList<>();
    GoogleMap mMap;
    //private List<Polyline> polylinePaths = new ArrayList<r>();




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
    public void onDirectionFinderSuccess(List<Route> routes) {
        Route route = routes.get(0);

        PolylineOptions polylineOptions = new PolylineOptions().
                geodesic(true)
                .color(Color.BLUE)
                .width(15)
                .clickable(true);

        for (int j = 0; j < route.points.size(); j++)
            polylineOptions.add(route.points.get(j));

        polylinePaths.add(mMap.addPolyline(polylineOptions));
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if(ContextCompat.checkSelfPermission(getActivity().getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED){
            mMap.setMyLocationEnabled(true);
        }
        //polylinePaths = new ArrayList<>();
        //ArrayList<LatLng> coordList = new ArrayList<LatLng>();

// Adding points to ArrayList
        //coordList.add(new LatLng(0, 0));
       // coordList.add(new LatLng(1, 1));
       // coordList.add(new LatLng(2, 2));
// etc...

// Find map fragment. This line work only with support library
        // GoogleMap gMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();

        PolylineOptions polylineOptions1 = new PolylineOptions();

// Create polyline options with existing LatLng ArrayList
        polylineOptions1.addAll(routeArray);
        polylineOptions1
                .width(50)
                .color(Color.RED);

// Adding multiple points in map using polyline and arraylist
        mMap.addPolyline(polylineOptions1);


        //PolylineOptions polylineOptions2 = new PolylineOptions();
        new DirectionFinder(this, routeArray.get(0).toString(), routeArray.get(routeArray.size()-1).toString());//.execute();
        //polylineOptions2.add(routeArray.get(0));
        //polylineOptions2.add(routeArray.get(routeArray.size()-1));
       // polylineOptions2
        //        .width(50)
        //        .color(Color.BLUE);

        //mMap.addPolyline(polylineOptions2);

       // PolylineOptions polylineOptions2 = new PolylineOptions()
        //        .geodesic(true)
         //       .color(Color.BLUE)
          //      .width(15)
          //      .clickable(true);

      //  Route route = routes.get(0);
      //  for (int j = 0; j < routes.points.size(); j++)
      //      polylineOptions2.add(routes.points.get(j));

       // polylinePaths.add(mMap.addPolyline(polylineOptions2));

    }


}
