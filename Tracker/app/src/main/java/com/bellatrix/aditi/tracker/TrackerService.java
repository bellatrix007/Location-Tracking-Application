package com.bellatrix.aditi.tracker;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.IBinder;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

// TODO: check for the lifecycle of the service
// explore ideas so that it runs indefinitely
public class TrackerService extends Service {

    private static final String TAG = TrackerService.class.getSimpleName();
    private String user;

    @Override
    public IBinder onBind(Intent intent) {return null; }

    @Override
    public void onCreate() {
        super.onCreate();

        user = getSharedPreferences("login", MODE_PRIVATE).getString("user", "");
        requestLocationUpdates();
    }

    private void requestLocationUpdates() {
        LocationRequest request = new LocationRequest();
        request.setInterval(10000);
        request.setFastestInterval(5000);
        request.setSmallestDisplacement(10);
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(this);
        int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (permission == PackageManager.PERMISSION_GRANTED) {
            // Request location updates and when an update is
            // received, store the location in Firebase
            client.requestLocationUpdates(request, new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("locations").child(user);
                    Location location = locationResult.getLastLocation();
                    if (location != null) {
                        Log.d(TAG, "location update " + location);
                        ref.child("latitude").setValue(location.getLatitude());
                        ref.child("longitude").setValue(location.getLongitude());
                    }
                }
            }, null);
        }
    }

}