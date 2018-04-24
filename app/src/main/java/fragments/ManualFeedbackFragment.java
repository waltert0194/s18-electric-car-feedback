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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import Modules.DirectionFinder;
import Modules.Route;
import asc.clemson.electricfeedback.R;

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

    private void drawPolylines() {
        PolylineOptions preferredPolylineOptions = new PolylineOptions();
// Create polyline options with the already selected LatLng ArrayList
        preferredPolylineOptions.addAll(preferredRouteArray);
        preferredPolylineOptions
                .width(15)
                .color(Color.BLUE);
        mMap.addPolyline(preferredPolylineOptions);

        PolylineOptions otherPolylineOptions = new PolylineOptions();
// Create polyline options with the already selected LatLng ArrayList
        otherPolylineOptions.addAll(otherRouteArray);
        otherPolylineOptions
                .width(15)
                .color(Color.GRAY);
        mMap.addPolyline(otherPolylineOptions);

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
