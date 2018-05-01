package fragments;

import android.Manifest;
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
import android.widget.EditText;
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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.maps.android.PolyUtil;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import Modules.DirectionFinder;
import Modules.DirectionFinderListener;
import Modules.Route;
import asc.clemson.electricfeedback.R;

import static android.content.ContentValues.TAG;

//Fragment that finalizes the route selection and feedback gathering
//sends to Firebase Database
//Implements OnMapReadyCallback for rendering the map
//Implements DirectionFinderListener for generating the alternative route.

public class TrackingFeedbackFragment extends Fragment implements OnMapReadyCallback, DirectionFinderListener{
    //layout of fragment
    private static View view;
    //passed in arguments, preferred route is the one the user selected before pressing on the FAB
    private ArrayList <LatLng> routeArray;
    private ArrayList <LatLng> altArray = new ArrayList<>();
    //editable test box for optional text feedback submitting
    private EditText optionalTextView;
    //list of the generated routes from DirectionfinderListener
    private List<Route> routes;
    private int numOfRoutes = 2;
    //lines of routes to draw on the map
    private List<Polyline> polylinePaths = new ArrayList<>();
    //the map API fragment
    GoogleMap mMap;
    //user selected preferred Route
    private Polyline preferredRoute;
    //tolerance for lining up a marker to a polyline
    double tolerance = 30; //meters

    /** Called when the activity is first created. */
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        //inflate layout
        try {
            view = inflater.inflate(R.layout.fragment_feedback, container, false);
        } catch (android.view.InflateException e) {
            Toast.makeText(getActivity(), "PROBLEM", Toast.LENGTH_SHORT).show();
        }

        //catch for arguments
        Bundle bundle = getArguments();
        try{
            if (bundle != null){
                if (bundle.containsKey("trackedBundle")){
                    routeArray = bundle.getParcelableArrayList("trackedBundle");
                }
            }else
            {
                Toast.makeText(getActivity(), "no Args",Toast.LENGTH_LONG).show();
            }
        }catch(java.lang.NullPointerException e){
            Toast.makeText(getActivity(),"Could not create Route, please try again",Toast.LENGTH_LONG).show();
            Fragment fragment = new StartFragment();
            replaceFragment(fragment);
        }

        return view;
    }

    /** Called when the activity has become visible. */
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FloatingActionButton fab = getView().findViewById(R.id.floatingActionButton);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loginToFirebase();
            }
        });

        optionalTextView = getView().findViewById(R.id.optionalText);

        MapFragment fragment = (MapFragment) getChildFragmentManager().findFragmentById(R.id.mapFeedback);
        fragment.getMapAsync(this);
    }

    //self contained database connection and data push
    private void loginToFirebase() {

//Call OnCompleteListener if the user is signed in successfully//
        FirebaseAuth.getInstance().signInAnonymously().addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(Task<AuthResult> task) {
//If the user has been authenticated...//
                if (task.isSuccessful()) {
//...then call requestLocationUpdates//
                    final String path = getString(R.string.firebase_path);

                    //Top level of database
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference(path);
                    DatabaseReference userRef = ref.push().child("Users");

                    //Children of Users Directory
                    DatabaseReference feedbackRef = userRef.child("Feedback");
                    DatabaseReference routeRef = userRef.child("Routes");

                    //Children of Feedback directory
                    DatabaseReference winnerRef = feedbackRef.child("Winning Route");
                    DatabaseReference textRef = feedbackRef.child("Optional Feedback");

                    //Children of Routes directory
                    DatabaseReference userRoute = routeRef.child("Winning Route");
                    DatabaseReference altRoute = routeRef.child("Losing Route");

                    userRoute.push().setValue(routeArray);
                    altRoute.push().setValue(altArray);
                    //winnerRef.push().setValue(BOOLEAN FOR BEST ROUTE);
                    String optionalText =  optionalTextView.getText().toString();
                    textRef.push().setValue(optionalText);
                    Fragment fragment = new StartFragment();
                    replaceFragment(fragment);
                } else {
//If sign in fails, then log the error//
                    Log.d(TAG, "Firebase authentication failed");
                }
            }
        });

    }



    //once directionfinder has started.
    @Override
    public void onDirectionFinderStart() {
    }

    //when directionfinder has returned with a list of routes.
    @Override
    public void onDirectionFinderSuccess(List<Route> routes) {
        //test check for the proper number of routes
        if (routes.size() == 0) {
            Toast.makeText(getActivity(), "Failed Route Generation", Toast.LENGTH_LONG).show();
            Fragment fragment = new StartFragment();
            replaceFragment(fragment);
        }
        Route route = routes.get(0);

        PolylineOptions trackedPolylineOptions = new PolylineOptions();

// Create polyline options with existing LatLng ArrayList
        trackedPolylineOptions.addAll(routeArray);
        trackedPolylineOptions
                .width(15)
                .color(Color.BLUE);
        mMap.addPolyline(trackedPolylineOptions);

        // add clickable marker to route
        mMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.start_blue))
                .position(routeArray.get(routeArray.size()/2)));

        //set up polyline
        PolylineOptions generatedPolylineOptions = new PolylineOptions().
                geodesic(true)
                .color(Color.RED)
                .width(15)
                .clickable(true);

        // add clickable marker to route
        mMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.end_green))
                .position(route.points.get(route.points.size()/2)));

        for (int j = 0; j < route.points.size(); j++) {
            generatedPolylineOptions.add(route.points.get(j));
            altArray.add(route.points.get(j));
        }

        polylinePaths.add(mMap.addPolyline(generatedPolylineOptions));
        polylinePaths.add(mMap.addPolyline(trackedPolylineOptions));

        //autozoom
        float zoomLevel = 10.0f; //This goes up to 21
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(routeArray.get(routeArray.size()/2), zoomLevel));
    }

    //once the map is ready to display ...
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if(ContextCompat.checkSelfPermission(getActivity().getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED){
            mMap.setMyLocationEnabled(true);
        }

        //convert LatLng to plain text address from coordinates
        String oriName = null;
        String destName = null;
        double oriLat = routeArray.get(0).latitude;
        double oriLng = routeArray.get(0).longitude;
        double destLat = routeArray.get(routeArray.size()-1).latitude;
        double destLng = routeArray.get(routeArray.size()-1).longitude;
        //Get origin address base on location
        try{
            Geocoder geo = new Geocoder(TrackingFeedbackFragment.this.getActivity(), Locale.getDefault());
            List<Address> addresses = geo.getFromLocation(oriLat, oriLng, 1);
            if (addresses.isEmpty()) {
                Toast.makeText(getActivity(), "address is empty", Toast.LENGTH_SHORT).show();
            }
            else {//concatenate addresses
                if (addresses.size() > 0) {
                    oriName = addresses.get(0).getFeatureName()
                            + " "
                            + addresses.get(0).getThoroughfare()
                            + " "
                            + addresses.get(0).getLocality()
                            + ", "
                            + addresses.get(0).getAdminArea()
                            + ", "
                            + addresses.get(0).getCountryName();
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        //Get destination address from coordinates
        try{
            Geocoder geo = new Geocoder(TrackingFeedbackFragment.this.getActivity(), Locale.getDefault());
            List<Address> addresses = geo.getFromLocation(destLat, destLng, 1);
            if (addresses.isEmpty()) {
                Toast.makeText(getActivity(), "address is empty", Toast.LENGTH_SHORT).show();
            }
            else {//concatenate addresses
                if (addresses.size() > 0) {
//Possbile better way of catching a miscalculated address
//                    if (!addresses.get(0).getFeatureName().isEmpty()){
//                        String featureName = addresses.get(0).getFeatureName();
//                    }
//
//                    if (!addresses.get(0).getThoroughfare().isEmpty()){
//                        String thoroughName = addresses.get(0).getThoroughfare();
//                    }
//
//                    if (!addresses.get(0).getLocality().isEmpty()){
//                        String localityName = addresses.get(0).getLocality();
//                    }
//
//                    if (!addresses.get(0).getAdminArea().isEmpty()){
//                        String adminName = addresses.get(0).getAdminArea();
//                    }
//
//                    if (!addresses.get(0).getCountryName().isEmpty()){
//                        String countryName = addresses.get(0).getCountryName();
//                    }

                    destName = addresses.get(0).getFeatureName()
                            + ", "
                            + addresses.get(0).getThoroughfare()
                            + " "
                            + addresses.get(0).getLocality()
                            + ", "
                            + addresses.get(0).getAdminArea()
                            + ", "
                            + addresses.get(0).getCountryName();
                }
            }
        }
        //catch for errors
        catch (NullPointerException e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), "FAILED", Toast.LENGTH_SHORT).show();
            Fragment fragment = new StartFragment();
            replaceFragment(fragment);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), "FAILED", Toast.LENGTH_SHORT).show();
            Fragment fragment = new StartFragment();
            replaceFragment(fragment);
        }
        try {
            new DirectionFinder(this, oriName, destName).execute();

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), "Failed to find Directions", Toast.LENGTH_SHORT).show();
        }

        //listen for clicks on the polylines
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

        //listen for clicks on the markers
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                //look though all the polylines
                for(int i = 0; i < polylinePaths.size(); i++){
                    if (PolyUtil.isLocationOnPath(marker.getPosition(), polylinePaths.get(i).getPoints(), true, tolerance)) {
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

    //Create a new fragment and switch the view
    public void replaceFragment(Fragment someFragment) {
        android.app.FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.content_frame, someFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}
