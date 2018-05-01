package Modules;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

import asc.clemson.electricfeedback.MainActivity;
import asc.clemson.electricfeedback.R;
//tracking service for background GPS device tracking
//runs in background
// can be killed by notification or finishing a survey in app

public class TrackingService extends Service {
    //service name
    private static final String TAG = TrackingService.class.getSimpleName();

    //LatLng list of GPS coordinates to store the Route
    public ArrayList <LatLng> routeArray = new ArrayList<LatLng>();

    //Ask system for location
    private LocationRequest request;
    private int count = 0;
    FusedLocationProviderClient client;
    //if the location request came back with a location
    LocationCallback mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
            //check for location
            if (locationResult != null) {
                //get the coordinates
                double lat = locationResult.getLastLocation().getLatitude();
                double lng = locationResult.getLastLocation().getLongitude();
                LatLng coordinate = new LatLng(lat, lng);
                //add coordinate to the route
                routeArray.add(coordinate);

//                Toast.makeText(getBaseContext(), "Lat:Lng = "+coordinate.latitude+ " ::: "+coordinate.longitude , Toast.LENGTH_SHORT).show();

                // bundle up route for passing back to the main activity
                Bundle bundle = new Bundle();
                bundle.putSerializable("routeKey", routeArray);
                //intent for receiving the arguments on main activity
                Intent intent = new Intent("routeIntent");
                intent.putExtra("routeBundle", bundle);
                sendBroadcast(intent);
                count++;
            }
        }
    };

    public TrackingService() {
    }

    //when the service is ending
    @Override
    public void onDestroy() {
        super.onDestroy();
        //kill location listener
        client.removeLocationUpdates(mLocationCallback);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onCreate() {
        super.onCreate();

    }

    //as the service starts
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        //spawn the notification
        buildNotification();
        //ask for GPS location
        requestLocationUpdates();
        return START_NOT_STICKY;

    }

    private void buildNotification() {
        //create notification intent
        Intent notificationIntent = new Intent(this, StopServiceReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, (int) System.currentTimeMillis(), notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
       //create intent for returning directly to feedback fragment NOT Implemented anywhere else yet.
        //Intent feedbackIntent = new Intent(this, MainActivity.class);
//        PendingIntent feedbackPendingIntent = PendingIntent.getBroadcast(this, 0, feedbackIntent, 0);

        // Create the TaskStackBuilder and add the intent, which inflates the back stack
      //  TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
       // stackBuilder.addNextIntentWithParentStack(feedbackIntent);
       // PendingIntent feedbackPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        //create a notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String CHANNEL_ID = "notifiyChannel";
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
            mChannel.setDescription(description);
            mChannel.enableVibration(false);
            NotificationManager notificationManager = (NotificationManager) this.getSystemService(
                    NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(mChannel);

            //Build the notification
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(getString(R.string.tracking_enabled_notif))
//Make this notification ongoing so it can’t be dismissed by the user//
                    .setOngoing(true)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setSmallIcon(R.drawable.ic_distance);
                    //.addAction(R.mipmap.ic_launcher, "Leave Feedback", feedbackPendingIntent);

            // spawn the notification
            notificationManager.notify(1, mBuilder.build());
            startForeground(1, mBuilder.build());
        }
        else {//set up notification for older android builds
            Notification notification =
                    new Notification.Builder(this)
                            .setContentTitle(getText(R.string.channel_name))
                            .setContentText(getText(R.string.channel_description))
                            .setSmallIcon(R.drawable.ic_distance)
                            .setContentIntent(pendingIntent)
                            .build();

            startForeground(1, notification);
        }
    }

    private void requestLocationUpdates() {

//Specify how often your app should request the device’s location//
        request = LocationRequest.create();
        request.setInterval(350);

//Get the most accurate location data available//
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        client = LocationServices.getFusedLocationProviderClient(this);
        final String path = getString(R.string.firebase_path);
        int permission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);

//If the app currently has access to the location permission...//
        if (permission == PackageManager.PERMISSION_GRANTED) {
//...then request location updates//
            client.requestLocationUpdates(request, mLocationCallback , null);

        }
    }
}

