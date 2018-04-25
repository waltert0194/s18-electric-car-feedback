package fragments;

import android.Manifest;
import android.app.Fragment;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

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

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import Modules.DirectionFinder;
import Modules.Route;
import asc.clemson.electricfeedback.R;

import static android.content.ContentValues.TAG;

public class ManualFeedbackFragment extends Fragment implements OnMapReadyCallback {
    private static View view;
    private ArrayList<LatLng> preferredRouteArray;
    private ArrayList<LatLng> otherRouteArray;
    private EditText optionalText;
    private List<Route> routes;
    private int numOfRoutes = 2;
    private List<Polyline> polylinePaths = new ArrayList<>();
    GoogleMap mMap;
    private Polyline preferredRoute;
    double tolerance = 30; //meters

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
            if (bundle.containsKey("preferredPoints")){
                preferredRouteArray = bundle.getParcelableArrayList("preferredPoints");
            }
            if (bundle.containsKey("otherPoints")){
                otherRouteArray = bundle.getParcelableArrayList("otherPoints");
            }
        }else
        {
            Toast.makeText(getActivity(), "no Args",Toast.LENGTH_LONG).show();
        }

        return view;
    }

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
                    DatabaseReference userRoute = routeRef.child("Users' Route");
                    DatabaseReference altRoute = routeRef.child("Alternate Route");

                    userRoute.push().setValue(preferredRouteArray);
                    altRoute.push().setValue(otherRouteArray);
                    //winnerRef.push().setValue(BOOLEAN FOR BEST ROUTE);
                    //textRef.push().setValue(STRING FOR OPTIONAL TEXT);
                } else {
//If sign in fails, then log the error//
                    Log.d(TAG, "Firebase authentication failed");
                }
            }
        });
    }


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

        optionalText = getView().findViewById(R.id.optionalText);

        MapFragment fragment = (MapFragment) getChildFragmentManager().findFragmentById(R.id.mapFeedback);
        fragment.getMapAsync(this);
    }

    private void drawPolylines() {

        PolylineOptions preferredPolylineOptions = new PolylineOptions();
// Create polyline options with the already selected LatLng ArrayList
        preferredPolylineOptions.addAll(preferredRouteArray);
        preferredPolylineOptions
                .width(15)
                .color(Color.CYAN);
        mMap.addPolyline(preferredPolylineOptions);

 // add clickable marker to route
mMap.addMarker(new MarkerOptions()
        .icon(BitmapDescriptorFactory.fromResource(R.drawable.start_blue))
        .position(preferredRouteArray.get(preferredRouteArray.size()/2)));

// Create polyline options with the already selected LatLng ArrayList
        PolylineOptions otherPolylineOptions = new PolylineOptions();
        otherPolylineOptions.addAll(otherRouteArray);
        otherPolylineOptions
                .width(15)
                .color(Color.GRAY);
        mMap.addPolyline(otherPolylineOptions);
    // add clickable marker to route
    mMap.addMarker(new MarkerOptions()
            .icon(BitmapDescriptorFactory.fromResource(R.drawable.end_green))
            .position(otherRouteArray.get(otherRouteArray.size()/2)));


        polylinePaths.add(mMap.addPolyline(preferredPolylineOptions));
        polylinePaths.add(mMap.addPolyline(otherPolylineOptions));
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if(ContextCompat.checkSelfPermission(getActivity().getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED){
            mMap.setMyLocationEnabled(true);
        }

        //drawLines
        drawPolylines();

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
