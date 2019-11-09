package com.bellatrix.aditi.tracker;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.media.AudioManager;
import android.os.Build;
import android.os.IBinder;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

// TODO: explore how to persist service is app is killed
public class TrackerService extends Service {

    private static final String TAG = TrackerService.class.getSimpleName();
    private static final String MESSAGE_BODY = "Please send your location. Sent by Tracker!";

    private String user;
    private DatabaseReference ref1, ref2;
    private double lat, lng;
    private SmsBroadcastReceiver smsBroadcastReceiver;
    private List<String> sharingTo;

    public TrackerService(Context applicationContext) {
        super();
        Log.i("Service", "In constructor!");
    }

    public TrackerService() {

    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.i("Service", "In oncreate!");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startMyOwnForeground();
        else
            startForeground(1, new Notification());

        user = getSharedPreferences("login", MODE_PRIVATE).getString("user", "");
        ref1 = FirebaseDatabase.getInstance().getReference().child("locations").child(user);
        ref2 = FirebaseDatabase.getInstance().getReference().child("users").child(user);

        sharingTo = new ArrayList<>();
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void startMyOwnForeground(){
        String NOTIFICATION_CHANNEL_ID = "com.bellatrix.aditi.tracker";
        String channelName = "Tracker Service";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_HIGH);
        chan.setLightColor(Color.BLUE);
//        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setContentTitle("Locations is tracking in background")
                .setPriority(NotificationManager.IMPORTANCE_HIGH)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(2, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.i("Service", "In onstart!" + user);

        ref2.child("seeing_of").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                sharingTo = new ArrayList<>();
                for(DataSnapshot ds: dataSnapshot.getChildren()) {
                    sharingTo.add(ds.getKey());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        locationUpdates();
        attachListeners();

        //SMS event receiver
        smsBroadcastReceiver = new SmsBroadcastReceiver();
//        mIntentFilter = new IntentFilter();
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
//            intentFilter.addAction(Telephony.Sms.Intents.DATA_SMS_RECEIVED_ACTION);
//        }
//        else mIntentFilter.addAction("android.provider.Telephony.SMS_RECEIVED");
        registerReceiver(smsBroadcastReceiver, new IntentFilter("android.provider.Telephony.SMS_RECEIVED"));

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {return null; }
    
    private void locationUpdates() {
        LocationRequest request = new LocationRequest();
        request.setInterval(10000);
        request.setFastestInterval(5000);
        request.setSmallestDisplacement(1);
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(this);
        int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (permission == PackageManager.PERMISSION_GRANTED) {
            // Request location updates and when an update is
            // received, store the location in Firebase
            client.requestLocationUpdates(request, new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    Location location = locationResult.getLastLocation();
                    if (location != null) {
                        Log.d(TAG, "location update " + location);

                        lat = location.getLatitude();
                        lng = location.getLongitude();

                        ref1.child("latitude").setValue(lat);
                        ref1.child("longitude").setValue(lng);
                    }
                }
            }, null);
        }
    }

    private void attachListeners() {

        // listener for refresh ringer request
        ref2.orderByKey().equalTo("refresh_ringer")
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(dataSnapshot.exists()) {
                        Log.d("Ringer", "Refresh request by "
                                + dataSnapshot.child("refresh_ringer").getValue());
                        requestRingerUpdates();
                        // also remove the request
                        dataSnapshot.child("refresh_ringer").getRef().removeValue();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

        // listener for update ringer request
        ref2.orderByKey().equalTo("update_ringer")
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(dataSnapshot.exists()) {
                        Log.d("Ringer", "Update request by "
                                + dataSnapshot.child("update_ringer").getValue());

                        // change the volume to max
                        AudioManager audioManager = ((AudioManager)getSystemService(Context.AUDIO_SERVICE));

                        // check for the ringer mode to be silent to check for DND permissions
                        if(audioManager.getRingerMode()==AudioManager.RINGER_MODE_SILENT)
                            checkAndRequestDNDAccess(audioManager);
                        else {
                            audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                            audioManager.setStreamVolume(AudioManager.STREAM_RING,
                                    audioManager.getStreamMaxVolume(AudioManager.STREAM_RING),0);
                        }

                        // also remove the request
                        dataSnapshot.child("update_ringer").getRef().removeValue();
                        // referesh ringer
                        requestRingerUpdates();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
    }

    private void requestRingerUpdates() {

        AudioManager mobilemode = (AudioManager)getSystemService(Context.AUDIO_SERVICE);

        switch (mobilemode.getRingerMode()) {
            case AudioManager.RINGER_MODE_SILENT:
                ref1.child("ringer").setValue("Silent mode");
                break;
            case AudioManager.RINGER_MODE_VIBRATE:
                ref1.child("ringer").setValue("Vibrate mode");
                break;
            case AudioManager.RINGER_MODE_NORMAL:
                ref1.child("ringer").setValue("Normal mode: " + mobilemode.getStreamVolume(AudioManager.STREAM_RING));
                break;
        }
    }

    private void checkAndRequestDNDAccess(AudioManager audioManager) {

        // check for permissions first
        NotificationManager n = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if(n.isNotificationPolicyAccessGranted()) {
            audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
            audioManager.setStreamVolume(AudioManager.STREAM_RING,
                    audioManager.getStreamMaxVolume(AudioManager.STREAM_RING),0);
        } else{
            // do nothing
            ref1.child("ringer").setValue("Silent mode: edit permissions denied");
        }
    }

    private class SmsBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)) {
                String smsSender = "";
                String smsBody = "";

                for (SmsMessage smsMessage : Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                    smsSender = smsMessage.getDisplayOriginatingAddress();
                    smsBody += smsMessage.getMessageBody();
                }

                if (checkInProviderList(smsSender) && smsBody.equals(MESSAGE_BODY)) {
                    // send location via sms
                    if (ContextCompat.checkSelfPermission(context,
                            Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {

                        String locationMessage = lat + " " + lng + "\nSent by Tracker!";
                        SmsManager smsManager = SmsManager.getDefault();
                        smsManager.sendTextMessage(smsSender,null,locationMessage,
                                    null, null);
                    }
                }
            }
        }

        private boolean checkInProviderList(String smsSender) {
            for(String s : sharingTo) {
                if(smsSender.equals(s))
                    return true;
            }
            return false;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i("Service", "ondestroy of service!");

        // Unregister the SMS receiver
        unregisterReceiver(smsBroadcastReceiver);

        Intent broadcastIntent = new Intent(this, TrackerRestarterBroadcastReceiver.class);
        sendBroadcast(broadcastIntent);
    }
}