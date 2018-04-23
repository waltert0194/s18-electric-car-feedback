package fragments;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.app.Fragment;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.util.Log;
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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import Modules.DirectionFinder;
import Modules.DirectionFinderListener;
import Modules.Route;
import asc.clemson.electricfeedback.R;

import static android.content.ContentValues.TAG;

public class FeedbackFragment extends Fragment implements OnMapReadyCallback, DirectionFinderListener{
    private static View view;
    private ArrayList <LatLng> routeArray;
    private EditText optionalText;
    private List<Route> routes;
    private int numOfRoutes = 2;
    private List<Polyline> polylinePaths = new ArrayList<>();
    GoogleMap mMap;
    private Polyline preferredRoute;
    double tolerance = 30; //meters
    //private List<Polyline> polylinePaths = new ArrayList<r>();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        try {
            view = inflater.inflate(R.layout.fragment_feedback, container, false);
        } catch (android.view.InflateException e) {
            Toast.makeText(getActivity(), "PROBLEM", Toast.LENGTH_SHORT).show();
        }

        //catch for arguments
        Bundle bundle = getArguments();
        if (bundle != null){
            if (bundle.containsKey("trackedBundle")){
                routeArray = bundle.getParcelableArrayList("trackedBundle");
            }
        }else
        {
            Toast.makeText(getActivity(), "no Args",Toast.LENGTH_LONG).show();
        }

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

        PolylineOptions trackedPolylineOptions = new PolylineOptions();

// Create polyline options with existing LatLng ArrayList
        trackedPolylineOptions.addAll(routeArray);
        trackedPolylineOptions
                .width(15)
                .color(Color.RED);
        mMap.addPolyline(trackedPolylineOptions);

        PolylineOptions generatedPolylineOptions = new PolylineOptions().
                geodesic(true)
                .color(Color.BLUE)
                .width(15)
                .clickable(true);

        for (int j = 0; j < route.points.size(); j++)
            generatedPolylineOptions.add(route.points.get(j));

        polylinePaths.add(mMap.addPolyline(generatedPolylineOptions));
        polylinePaths.add(mMap.addPolyline(trackedPolylineOptions));
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if(ContextCompat.checkSelfPermission(getActivity().getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED){
            mMap.setMyLocationEnabled(true);
        }

        //convert LatLng to plain text address
        String oriName = null;
        String destName = null;
        double oriLat = routeArray.get(0).latitude;
        double oriLng = routeArray.get(0).longitude;
        double destLat = routeArray.get(routeArray.size()-1).latitude;
        double destLng = routeArray.get(routeArray.size()-1).longitude;
        //Get origin address base on location
        try{
            Geocoder geo = new Geocoder(FeedbackFragment.this.getActivity(), Locale.getDefault());
            List<Address> addresses = geo.getFromLocation(oriLat, oriLng, 1);
            if (addresses.isEmpty()) {
                Toast.makeText(getActivity(), "address is empty", Toast.LENGTH_SHORT).show();
            }
            else {
                if (addresses.size() > 0) {
                    oriName = addresses.get(0).getFeatureName()
                            + ", "
                            + addresses.get(0).getLocality()
                            + ","
                            + addresses.get(0).getAdminArea()
                            + addresses.get(0).getCountryName();
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        //Get origin address base on location
        try{
            Geocoder geo = new Geocoder(FeedbackFragment.this.getActivity(), Locale.getDefault());
            List<Address> addresses = geo.getFromLocation(destLat, destLng, 1);
            if (addresses.isEmpty()) {
                Toast.makeText(getActivity(), "address is empty", Toast.LENGTH_SHORT).show();
            }
            else {
                if (addresses.size() > 0) {
                    destName = addresses.get(0).getFeatureName()
                            + ", "
                            + addresses.get(0).getLocality()
                            + ","
                            + addresses.get(0).getAdminArea()
                            + addresses.get(0).getCountryName();
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        try {
//            Log.d("ORI",oriName);
//            Log.d("DEST",destName);
            new DirectionFinder(this, oriName, destName).execute();

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), "Failed to find Directions", Toast.LENGTH_SHORT).show();
        }

        //TODO: set the clicked route/marker to the preferred route, when switching to feedback it is autoselected.
        mMap.setOnPolylineClickListener(new GoogleMap.OnPolylineClickListener() {
            @Override
            public void onPolylineClick(Polyline polyline) {
                preferredRoute = polyline;
                polyline.setColor(Color.CYAN);
                for (Polyline pline :polylinePaths) {
                    if (!pline.equals(polyline)){
                        pline.setColor(Color.GRAY);
                    }
                }
            }
        });

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                for(int i = 0; i < polylinePaths.size(); i++){
                    if (PolyUtil.isLocationOnPath(marker.getPosition(), polylinePaths.get(i).getPoints(), true, tolerance)) {
                        Toast.makeText(getActivity(), marker.getId(), Toast.LENGTH_SHORT).show();
                        polylinePaths.get(i).setColor(Color.CYAN);
                        for (Polyline pline :polylinePaths) {
                            if (!pline.equals(polylinePaths.get(i))){
                                pline.setColor(Color.GRAY);
                            }
                        }
                        return true;
                    }
                }
                return false;
            }
        });
    }
}
