package Modules;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import asc.clemson.electricfeedback.R;

import static android.content.ContentValues.TAG;

//Send to database service
public class SendToDatabase extends Service {
    public SendToDatabase() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        loginToFirebase();
    }
    private void loginToFirebase() {

//Call OnCompleteListener if the user is signed in successfully//
        FirebaseAuth.getInstance().signInAnonymously().addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(Task<AuthResult> task) {
//If the user has been authenticated...//
                if (task.isSuccessful()) {
//...then call requestLocationUpdates//
                    sendData();
                } else {
//If sign in fails, then log the error//
                    Log.d(TAG, "Firebase authentication failed");
                }
            }
        });
    }
    private void sendData(){
        final String path = getString(R.string.firebase_path);

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(path);
        //ref.push().setValue(routeArray);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
