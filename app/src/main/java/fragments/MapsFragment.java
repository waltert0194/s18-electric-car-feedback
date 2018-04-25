package fragments;

import android.Manifest;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import Modules.DirectionFinder;
import Modules.DirectionFinderListener;
import Modules.Route;
import Modules.TrackingService;
import asc.clemson.electricfeedback.R;

public class MapsFragment extends Fragment implements OnMapReadyCallback, DirectionFinderListener {

    GoogleMap mMap;
    private Marker marker;
    private Button btnFeedback;
    private EditText etOrigin;
    private EditText etDestination;
    private List<Marker> originMarkers = new ArrayList<>();
    private List<Marker> destinationMarkers = new ArrayList<>();
    private List<Polyline> polylinePaths = new ArrayList<>();
    private ProgressDialog progressDialog;

    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private Boolean mLocationPermissionsGranted = false;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static View view;

    private int numOfRoutes = 2;
    //TODO set preferredRoute and bundle it up for sending to the FEEDBACK fragment
    private Polyline preferredRoute;
    double tolerance = 30; //meters
    private List<Route> backupRoutes = null;
    private Boolean directionsFound = false;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
         view = inflater.inflate(R.layout.fragment_maps, container, false);
         //end tracking service if user switches to manual tracking while service is active
        getActivity().stopService(new Intent(getActivity(), TrackingService.class));
        directionsFound = false;
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnFeedback = getView().findViewById(R.id.btnFeedback);
        etOrigin = (EditText) getView().findViewById(R.id.etOrigin);
        etDestination = (EditText) getView().findViewById(R.id.etDestination);
        MapFragment fragment = (MapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        fragment.getMapAsync(this);

        getLocationPermission();

        btnFeedback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (directionsFound) {
                    packageUpFeedback();
                    //Fragment fragment = new ManualFeedbackFragment();
//                       replaceFragment(fragment);
                }else{
                    sendRequest();
                    //Hide keyboard
                    InputMethodManager inputManager = (InputMethodManager)
                            getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(),
                            InputMethodManager.HIDE_NOT_ALWAYS);
                }

            }
        });
    }

    private void packageUpFeedback() {
        //bundle up the preferred route
            //default preferred route is set to the first generated route
        ArrayList<LatLng> preferredPoints = new ArrayList<>();
        preferredPoints.addAll(preferredRoute.getPoints());
        Bundle bigBundle = new Bundle();
        bigBundle.putParcelableArrayList("preferredPoints", preferredPoints);

        //bundle up the other route
        ArrayList<LatLng> otherPoints = new ArrayList<>();
        for (int i=0; i < polylinePaths.size(); i++) {
            if (polylinePaths.get(i) != (preferredRoute)){
                otherPoints.addAll(polylinePaths.get(i).getPoints());
                bigBundle.putParcelableArrayList("otherPoints",otherPoints);
            }
        }

        //switch to the ManualFeedbackFragment
        Fragment fragment =  new ManualFeedbackFragment();
        fragment.setArguments(bigBundle);
        replaceFragment(fragment);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(final GoogleMap googleMap) {
        mMap = googleMap;
        LatLng clemson = new LatLng(34.6834, -82.8374);

        marker = mMap.addMarker(new MarkerOptions().position(clemson).title("Marker in " + "Clemson"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(clemson));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(clemson, 10));
        if (ContextCompat.checkSelfPermission(getActivity().getApplicationContext(), FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }

        //TODO: set the clicked route/marker to the preferred route, when switching to feedback it is autoselected.
        mMap.setOnPolylineClickListener(new GoogleMap.OnPolylineClickListener() {
            @Override
            public void onPolylineClick(Polyline polyline) {
                preferredRoute = polyline;
                polyline.setColor(Color.CYAN);
                if (polyline.getTag()=="P1")
                {
                    ((TextView) getView().findViewById(R.id.tvDistance)).setText(backupRoutes.get(0).duration.text);   //For Distance
                    ((TextView) getView().findViewById(R.id.tvDuration)).setText(backupRoutes.get(0).duration.text);    //Show time
                }else if (polyline.getTag()=="P2") {
                    ((TextView) getView().findViewById(R.id.tvDistance)).setText(backupRoutes.get(1).duration.text);   //For Distance
                    ((TextView) getView().findViewById(R.id.tvDuration)).setText(backupRoutes.get(1).duration.text);    //Show time
                }

                for (Polyline pline :polylinePaths) {
                    if (!pline.equals(polyline)){ //if pline is not the one we clicked on
                        pline.setColor(Color.GRAY); //set all other polylines to grey
                    }
                }
            }
        });

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                for(int i = 0; i < polylinePaths.size(); i++){
                    if (PolyUtil.isLocationOnPath(marker.getPosition(), polylinePaths.get(i).getPoints(), true, tolerance)) {
                       preferredRoute = polylinePaths.get(i);
                       polylinePaths.get(i).setColor(Color.CYAN);
                        if (polylinePaths.get(i).getTag()=="P1")
                        {
                            ((TextView) getView().findViewById(R.id.tvDistance)).setText(backupRoutes.get(0).duration.text);   //For Distance
                            ((TextView) getView().findViewById(R.id.tvDuration)).setText(backupRoutes.get(0).duration.text);    //Show time
                        }else if (polylinePaths.get(i).getTag()=="P2") {
                            ((TextView) getView().findViewById(R.id.tvDistance)).setText(backupRoutes.get(1).duration.text);   //For Distance
                            ((TextView) getView().findViewById(R.id.tvDuration)).setText(backupRoutes.get(1).duration.text);    //Show time
                        }
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

    @Override
    public void onDirectionFinderStart() {
        progressDialog = ProgressDialog.show(getActivity(), "Please wait.",
                "Finding direction..!", true);

        if (originMarkers != null) {
            for (Marker marker : originMarkers) {
                marker.remove();
            }
        }

        if (destinationMarkers != null) {
            for (Marker marker : destinationMarkers) {
                marker.remove();
            }
        }

        if (polylinePaths != null) {
            for (Polyline polyline:polylinePaths ) {
                polyline.remove();
            }
        }
    }

    private void sendRequest() {
        String origin = etOrigin.getText().toString();
        String destination = etDestination.getText().toString();
        if (origin.isEmpty()) {
            //TODO: set origin to current location if it is left blank
            Toast.makeText(getActivity(), "Please enter origin address!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (destination.isEmpty()) {
            Toast.makeText(getActivity(), "Please enter destination address!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            new DirectionFinder(this, origin, destination).execute();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    //TODO: Inject our tracked Route into routes[0]
    @Override
    public void onDirectionFinderSuccess(List<Route> routes) {
        progressDialog.dismiss();
        polylinePaths = new ArrayList<>();
        originMarkers = new ArrayList<>();
        destinationMarkers = new ArrayList<>();
        backupRoutes = routes;

        //update button
        directionsFound = true;
        TextView btnFeedbackText = getView().findViewById(R.id.btnFeedback);
        btnFeedbackText.setText(R.string.leave_feedback);

        Toast.makeText(getActivity(), "Directions found!", Toast.LENGTH_SHORT).show();
        //CHANGE NUMBER OF ROUTES
        //Check that enough routes are generated
        if (routes.size()<2){
            Toast.makeText(getActivity(), "Could not generate Routes...", Toast.LENGTH_LONG).show();
            return;
        }
        for (int i = 0; i < numOfRoutes; i++) {
            Route route = routes.get(i);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(route.startLocation, 16));

            ((TextView) getView().findViewById(R.id.tvDistance)).setText(route.distance.text);   //For Distance
            ((TextView) getView().findViewById(R.id.tvDuration)).setText(route.duration.text);
            //show different labeled markers on each route
            //WARNING: this works for only 2 routes!
            if (i == 0){
                originMarkers.add(mMap.addMarker(new MarkerOptions()
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.start_blue))
                        .position(route.points.get(route.points.size()/2))));
            }
            if (i == 1){
                destinationMarkers.add(mMap.addMarker(new MarkerOptions()
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.end_green))
                        .position(route.points.get(route.points.size()/2))));
            }

            int color;
            if(i == 0){
                color = Color.CYAN;
            }else{
                color = Color.GRAY;
            }

            PolylineOptions polylineOptions = new PolylineOptions().
                    geodesic(true).color(color).width(15).clickable(true);

            for (int j = 0; j < route.points.size(); j++)
                polylineOptions.add(route.points.get(j));

            polylinePaths.add(mMap.addPolyline(polylineOptions));

            if (i == 0){
                polylinePaths.get(i).setTag("P1");
                preferredRoute = polylinePaths.get(i);
            }
            if (i == 1){
                polylinePaths.get(i).setTag("P2"); }
        }
    }

    private void getLocationPermission(){
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION};

        if (ContextCompat.checkSelfPermission(getActivity().getApplicationContext(),FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED){
            if (ContextCompat.checkSelfPermission(getActivity().getApplicationContext(),COURSE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED){
                mLocationPermissionsGranted = true;
            } else {
                ActivityCompat.requestPermissions(getActivity(),permissions,LOCATION_PERMISSION_REQUEST_CODE);
            }
        } else {
            ActivityCompat.requestPermissions(getActivity(),permissions,LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        mLocationPermissionsGranted = false;
        switch (requestCode){
            case LOCATION_PERMISSION_REQUEST_CODE:{
                if (grantResults.length > 0 ){
                    for (int i=0; i < grantResults.length; i++){
                        if(grantResults[i] != PackageManager.PERMISSION_GRANTED){
                            mLocationPermissionsGranted = false;
                            return;
                        }
                    }
                    mLocationPermissionsGranted = true;
                }
            }
        }
    }

    public void replaceFragment(Fragment someFragment) {
        android.app.FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.content_frame, someFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}
