package com.bellatrix.aditi.tracker;

import android.Manifest;
import android.app.ActivityManager;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.bellatrix.aditi.tracker.DatabaseClasses.UserMenuModel;
import com.bellatrix.aditi.tracker.Utils.FetchURL;
import com.bellatrix.aditi.tracker.Utils.TaskLoadedCallback;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.bellatrix.aditi.tracker.Utils.CommonFunctions.getUrl;

// TODO: Manage all permissions
public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback,
        DialogInterface.OnDismissListener, TaskLoadedCallback {

    private String user, user_name;
//    private String[] appPermissions = {
//            Manifest.permission.ACCESS_FINE_LOCATION,
//            Manifest.permission.ACCESS_COARSE_LOCATION,
//            Manifest.permission.SEND_SMS,
//            Manifest.permission.RECEIVE_SMS
//    };
    private static final int PERMISSION_LOCATION = 1234;
    private static final int PERMISSION_SEND_SMS = 1235;
    private static final int PERMISSION_SEND_SMS_ACT = 1236;
    private static final int ON_DO_NOT_DISTURB_CALLBACK_CODE = 1237;

    ExpandableUserListAdapter expandableListAdapter;
    ExpandableListView expandableListView;
    List<UserMenuModel> headerList = new ArrayList<>();
    HashMap<UserMenuModel, List<UserMenuModel>> childList = new HashMap<>();

    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;

    private Intent trackerServiceIntent;

    private GoogleMap mMap;
    private Marker mMarker;
    private String mDirectionMode, prevKey, prevKeyName, prevRinger;
    private boolean oldUser;
    private LatLng currLocation, prevLocation;
    private ValueEventListener markerListener;
    private Polyline mPolyline;
    private Boolean distance_expanded;
    private Boolean time_expanded;

    private ImageButton refreshRinger, updateRinger;

    private LocationSMSReceiver smsReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        user =  getSharedPreferences("login", MODE_PRIVATE).getString("user", "");
        mDirectionMode = "driving";
        prevKey = "";
        prevKeyName = "";
        prevRinger = "";
        oldUser = false;

        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        final View headerLayout = navigationView.getHeaderView(0);
        navigationView.setNavigationItemSelectedListener(this);

        // check for new user
        databaseReference.child("users").child(user).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(!dataSnapshot.exists()) {
                    // show undismissable alert dialog to enter name
                    // pop up register user dialog
                    RegisterUserDialogFragment registerUserDialogFragment = RegisterUserDialogFragment.newInstance(user);
                    registerUserDialogFragment.setCancelable(false);
                    registerUserDialogFragment.show(getSupportFragmentManager(),"registerUser");
                } else {
                    // fetch name and set in shared preferences
                    user_name = dataSnapshot.child("name").getValue().toString();
                    getSharedPreferences("login", MODE_PRIVATE).edit()
                            .putString("user_name", user_name).apply();

                    ((TextView)headerLayout.findViewById(R.id.tv_h1)).setText(user_name);
                   // user =  getSharedPreferences("login", MODE_PRIVATE).getString("user", "");
                    ((TextView)headerLayout.findViewById(R.id.tv_h2)).setText(user);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        // Check GPS is enabled
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // TODO: add in-app alert and push notification to enable GPS
            turnGPSOn();
            Toast.makeText(this, "Please enable location services", Toast.LENGTH_SHORT).show();
        }

        askLocationPermission();

        refreshRinger = (ImageButton) findViewById(R.id.refresh_ringer);
        refreshRinger.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        refreshRinger.setBackground(getDrawable(R.drawable.expanded_button_clicked));
                        if(!prevKey.equals("")) {
                            databaseReference.child("users").child(prevKey).child("refresh_ringer").setValue(user);
                        }
                        return true; // if you want to handle the touch event
                    case MotionEvent.ACTION_UP:
                        refreshRinger.setBackground(getDrawable(R.drawable.expanded_button));
                        return true; // if you want to handle the touch event
                }
                return false;
            }
        });

        updateRinger = (ImageButton) findViewById(R.id.update_ringer);
        updateRinger.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        updateRinger.setBackground(getDrawable(R.drawable.expanded_button_clicked));
                        if(!prevKey.equals("")) {
                            databaseReference.child("users").child(prevKey).child("update_ringer").setValue(user);
                        }
                        return true; // if you want to handle the touch event
                    case MotionEvent.ACTION_UP:
                        updateRinger.setBackground(getDrawable(R.drawable.expanded_button));
                        return true; // if you want to handle the touch event
                }
                return false;
            }
        });
//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                SmsManager smsManager = SmsManager.getDefault();
//                smsManager.sendTextMessage(prevKey,null,"Please send your location. Sent by Tracker!",
//                        null, null);
//                Snackbar.make(view, "Message sent to " + prevKey, Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//
//                smsReceiver = new LocationSMSReceiver();
//                registerReceiver(smsReceiver, new IntentFilter("android.provider.Telephony.SMS_RECEIVED"));
//
//
//            }
//        });

        expandableListView = findViewById(R.id.expandableListView);
        prepareMenuData();
        populateExpandableList();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        distance_expanded = false;
        time_expanded = false;

        final LinearLayout distance = (LinearLayout) findViewById(R.id.distanceLayout);
        final TextView distanceHead = (TextView) findViewById(R.id.distanceHead);
        final TextView distanceText = (TextView) findViewById(R.id.distanceText);
        ViewGroup.LayoutParams layoutParams = distance.getLayoutParams();
        layoutParams.height = 150;
        layoutParams.width = 150;
        distance.setLayoutParams(layoutParams);
        distance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(distance_expanded)
                {
                    ViewGroup.LayoutParams layoutParams = distance.getLayoutParams();
                    layoutParams.width = 150;
                    distance.setLayoutParams(layoutParams);
                    distance_expanded = false;
                    distanceHead.setVisibility(View.VISIBLE);
                    distanceText.setVisibility(View.GONE);
                }
                else
                {
                    ViewGroup.LayoutParams layoutParams = distance.getLayoutParams();
                    layoutParams.width = 300;
                    distance.setLayoutParams(layoutParams);
                    distance_expanded = true;
                    distance.setBackground(ContextCompat.getDrawable(MainActivity.this, R.drawable.expanded_button));
                    distanceHead.setVisibility(View.GONE);
                    distanceText.setVisibility(View.VISIBLE);
                }
            }
        });

        final LinearLayout time = (LinearLayout) findViewById(R.id.timeLayout);
        final TextView timeHead = (TextView) findViewById(R.id.timeHead);
        final TextView timeText = (TextView) findViewById(R.id.timeText);
        ViewGroup.LayoutParams timelayoutParams = time.getLayoutParams();
        timelayoutParams.height = 150;
        timelayoutParams.width = 150;
        time.setLayoutParams(timelayoutParams);
        time.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(time_expanded)
                {
                    ViewGroup.LayoutParams layoutParams = time.getLayoutParams();
                    layoutParams.width = 150;
                    time.setLayoutParams(layoutParams);
                    time_expanded = false;
                    timeHead.setVisibility(View.VISIBLE);
                    timeText.setVisibility(View.GONE);
                }
                else
                {
                    ViewGroup.LayoutParams layoutParams = time.getLayoutParams();
                    layoutParams.width = 300;
                    time.setLayoutParams(layoutParams);
                    time_expanded = true;
                    time.setBackground(ContextCompat.getDrawable(MainActivity.this, R.drawable.expanded_button));
                    timeHead.setVisibility(View.GONE);
                    timeText.setVisibility(View.VISIBLE);
                }
            }
        });

//        View headerLayout = navigationView.getHeaderView(0);
//        ImageView image = (ImageView) headerLayout.findViewById(R.id.avatar);
//        TextDrawable drawable = TextDrawable.builder()
//                .buildRect("A", Color.RED);
//        image.setImageDrawable(drawable);
    }

    private void askLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED) {
            // Start the service when the permission is granted
            startTrackerService();

            // also start location extraction here
            getCurrentLocation();

            if(mMap == null) {
                // start the maps fragment
                // Obtain the SupportMapFragment and get notified when the map is ready to be used.
                SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map);
                mapFragment.getMapAsync(this);
            }

            // now ask sms permission
            askSMSPermissions();

        } else {
            ActivityCompat.requestPermissions(this, new String[] {
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION },
                    PERMISSION_LOCATION);
        }
    }

    // TODO: Implement this
    private void turnGPSOn(){

    }

    private void askSMSPermissions() {

        // send sms
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions
                    (this, new String[] {Manifest.permission.SEND_SMS},PERMISSION_SEND_SMS);
        } else {
            // DND permission
            checkAndRequestDNDAccess();
        }
    }

    private void checkAndRequestDNDAccess() {
        // check for DND permissions
        NotificationManager n = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if(!n.isNotificationPolicyAccessGranted()) {
            startDNDPermissionActivity();
        }
    }

    private void prepareMenuData() {

        // initialize
        final UserMenuModel userMenuModel1 = new UserMenuModel("View Location",ContextCompat.getDrawable(getApplicationContext(),R.drawable.ic_location), true, true);
        headerList.add(userMenuModel1);
        childList.put(userMenuModel1, null);

        UserMenuModel userMenuModel = new UserMenuModel("Add user",
                ContextCompat.getDrawable(getApplicationContext(),R.drawable.ic_add_user),
                true, false);
        headerList.add(userMenuModel);
        childList.put(userMenuModel, null);

        userMenuModel = new UserMenuModel("View request",
                ContextCompat.getDrawable(getApplicationContext(),R.drawable.ic_view_req),
                true, false);
        headerList.add(userMenuModel);
        childList.put(userMenuModel, null);

        userMenuModel = new UserMenuModel("Log Out",
                ContextCompat.getDrawable(getApplicationContext(),R.drawable.ic_logout),
                true, false);
        headerList.add(userMenuModel);
        childList.put(userMenuModel, null);

        databaseReference.child("users").child(user).child("seeing_of").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // has a list of all the users seeing_of
                List<UserMenuModel> childModelsList = new ArrayList<>();
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    childModelsList.add(new UserMenuModel(ds.getKey(), ds.getValue().toString(),null, false, false));
                }

                childList.replace(userMenuModel1, childModelsList);
                populateExpandableList();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void populateExpandableList() {

        expandableListAdapter = new ExpandableUserListAdapter(this, headerList, childList);
        expandableListView.setAdapter(expandableListAdapter);

        expandableListView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {

                if (headerList.get(groupPosition).isGroup) {
                    if (!headerList.get(groupPosition).hasChildren) {

                        String menu_item_name = headerList.get(groupPosition).menuName;

                        if(menu_item_name.equals("Add user")) {
                            // pop up add user dialog
                            AddUserDialogFragment addUserDialogFragment = AddUserDialogFragment.newInstance(user,user_name);;
                            addUserDialogFragment.show(getSupportFragmentManager(),"addUser");

                        } else if(menu_item_name.equals("View request")) {
                            // show pending and sent requests
                            Intent intent = new Intent(MainActivity.this, ViewRequestsActivity.class);
                            intent.putExtra("user", user);
                            startActivity(intent);

                        } else {    // log out
                            stopService(trackerServiceIntent);
                            goToLogin();
                        }
                        onBackPressed();

                    }
                }

                return false;
            }
        });

        expandableListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {

                if (childList.get(headerList.get(groupPosition)) != null) {
                    UserMenuModel model = childList.get(headerList.get(groupPosition)).get(childPosition);
                    if (model.menuName.length() > 0) {

                        // unregister the previous receiver, if any
                        try {
                            unregisterReceiver(smsReceiver);
                        } catch (Exception e) {
                            Log.d("MainActivity", "Receiver cannot be unregistered");
                        }

                        setUpdates(model.menuName, model.displayName);
                        Log.d("marker",model.displayName);
                        onBackPressed();
                    }
                }

                return true;
            }
        });
    }

    private void setUpdates(String key, final String marker_title) {
        if(markerListener != null && !prevKey.equals("")) {
            databaseReference.child("locations").child(prevKey).removeEventListener(markerListener);
        } else {
            ((LinearLayout)findViewById(R.id.bottomLayout)).setVisibility(View.VISIBLE);
            ((LinearLayout)findViewById(R.id.timeLayout)).setVisibility(View.VISIBLE);
            ((LinearLayout)findViewById(R.id.distanceLayout)).setVisibility(View.VISIBLE);

            if(mMap!=null)
                mMap.setPadding(0,0,0,104);
        }
        markerListener = databaseReference.child("locations").child(key)
                .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                String user_key = dataSnapshot.getKey();
                if(!prevKey.equals(user_key))
                    oldUser = false;

                // check if it is a ringer update
                if(dataSnapshot.child("ringer").getValue()!=null) {

                    String ringer = dataSnapshot.child("ringer").getValue().toString();
                    if (!prevRinger.equals(ringer) || !oldUser)    // new ringer info
                    {
                        // update ringer info
                        prevRinger = ringer;
                        ((TextView)findViewById(R.id.RingerVolume)).setText(prevRinger);
                    }
                }

                // check if it is a location update
                double lat = Double.parseDouble(dataSnapshot.child("latitude").getValue().toString());
                double lng = Double.parseDouble(dataSnapshot.child("longitude").getValue().toString());
                if(prevLocation==null || !(prevLocation.latitude==lat && prevLocation.longitude==lng)
                        || !oldUser)    // new location info
                {
                    // update location info
                    prevLocation = new LatLng(lat, lng);
                    Log.d("setupdate",marker_title);
                    setMarker(user_key,marker_title);
                }

                prevKey = user_key;
                prevKeyName = marker_title;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    // TODO: see other direction modes also
    // TODO: distance and time
    private void setMarker(String key,String marker_title) {
        if(mMap==null)
            return;

        Log.d("Maps", key);

        if(mMarker == null)
        {
            mMarker = mMap.addMarker(new MarkerOptions().position(prevLocation).title(key));
        }
        else
        {
            mMarker.setTitle(marker_title);
            mMarker.setPosition(prevLocation);
        }
        mMarker.showInfoWindow();
        updateCameraBounds();
        new FetchURL(MainActivity.this)
                .execute(getUrl(currLocation, prevLocation, mDirectionMode, getString(R.string.google_maps_key)), mDirectionMode);
    }

    private void goToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        SharedPreferences.Editor spEdit = getSharedPreferences("login", MODE_PRIVATE).edit();
        spEdit.putBoolean("logged", false);
        spEdit.putString("user", "");
        spEdit.apply();
        finish();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        Log.d("Maps","Ready");
        mMap = googleMap;

        // update the user's location
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {

//                Toast.makeText(MainActivity.this, marker.getTitle(), Toast.LENGTH_SHORT).show();
                // zoom in
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 20));
                marker.showInfoWindow();

                return true;
            }
        });
    }

    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Start the service when the permission is granted
                    startTrackerService();

                    // also start location extraction here
                    getCurrentLocation();

                    if(mMap == null) {
                        // start the maps fragment
                        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
                        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                                .findFragmentById(R.id.map);
                        mapFragment.getMapAsync(this);
                    }

                    // now ask sms permission
                    askSMSPermissions();

                } else {
                    askLocationPermission();
                }

                return;
            }
            case PERMISSION_SEND_SMS: {

                // ask for DND permission
                checkAndRequestDNDAccess();
                return;
            }
            case PERMISSION_SEND_SMS_ACT: {
                // TODO: handle the offline mode
                // send sms to the prevkey to request location via SMS
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(user, null, "Hello!!", null, null);
                return;
            }
        }
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    private void startTrackerService() {
        trackerServiceIntent = new Intent(this, TrackerService.class);

        if (!isMyServiceRunning()) {
            startService(trackerServiceIntent);
        }
    }

    private boolean isMyServiceRunning() {
        TrackerService trackerService = new TrackerService(this);

        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (trackerService.getClass().equals(service.service.getClassName())) {
                Log.i ("isMyServiceRunning?", true+"");
                return true;
            }
        }
        Log.i ("isMyServiceRunning?", false+"");
        return false;
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        user_name = getSharedPreferences("login", MODE_PRIVATE).getString("user_name", "");

        ((TextView)findViewById(R.id.tv_h1)).setText(user_name);
        ((TextView)findViewById(R.id.tv_h2)).setText(user);

    }

    private void getCurrentLocation() {

        LocationRequest request = new LocationRequest();
        request.setInterval(30000);
        request.setFastestInterval(10000);
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
                        currLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        Log.d("Location", currLocation.latitude + " " + currLocation.longitude);
                        updateCameraBounds();

                        if(prevLocation!=null)
                            new FetchURL(MainActivity.this)
                                    .execute(getUrl(currLocation, prevLocation, mDirectionMode, getString(R.string.google_maps_key)),
                                            mDirectionMode);
                    }
                }
            }, null);
        }
    }

    private void updateCameraBounds() {
        if(mMap==null)
            return;

        // initial user's location only
        if(prevLocation==null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currLocation, 15));
        } else {
            // set bounds if a new user
            if(!oldUser) {

                oldUser = true;
                Log.d("animate", "true" + prevKey);
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                builder.include(currLocation);
                builder.include(prevLocation);
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 20));
            }
            // else do nothing
        }
    }

    private void startDNDPermissionActivity() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage("Tracker requires permission to change the DND mode of the device. Proceed?");

        alertDialogBuilder.setPositiveButton("Yes",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        // start the activity
                        Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                        startActivityForResult(intent, ON_DO_NOT_DISTURB_CALLBACK_CODE );
                    }
                });
        alertDialogBuilder.setNegativeButton("No",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        // dismiss
                        arg0.dismiss();

                    }
                });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        // Check which request we're responding to
//        if (requestCode == ON_DO_NOT_DISTURB_CALLBACK_CODE) {
//            Toast.makeText(this, "DND permissions", Toast.LENGTH_SHORT).show();
//        }
//    }

    @Override
    public void onTaskDone(Object... values) {
        if (mPolyline != null)
            mPolyline.remove();
        mPolyline = mMap.addPolyline((PolylineOptions) values[2]);

        // TODO: show distance and duration
        Log.d("mylog", "In main" + values[0] + " " + values[1]);
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            unregisterReceiver(smsReceiver);
        } catch (Exception e) {
            Log.d("MainActivity", "Receiver cannot be unregistered");
        }
    }

    // if we do not stop it, the service will die with our app.
    // Instead, by stopping the service,
    // we will force the service to call its own onDestroy which will force it to recreate itself
    // after the app is dead.
    @Override
    protected void onDestroy() {
        stopService(trackerServiceIntent);
        Log.d("Service", "ondestroy of activity!");
        super.onDestroy();
    }

    private class LocationSMSReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)) {
                String smsSender = "";
                String smsBody = "";

                for (SmsMessage smsMessage : Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                    smsSender = smsMessage.getDisplayOriginatingAddress();
                    smsBody += smsMessage.getMessageBody();
                }
                smsBody = smsBody.trim();

                if (smsSender.equals(user) && smsBody.endsWith("Sent by Tracker!")) {
                    // read the coordinates
                    try {
                        double lat = Double.parseDouble(smsBody.substring(0, smsBody.indexOf(" ")));
                        double lng = Double.parseDouble(smsBody.substring(smsBody.indexOf(" ") + 1,
                                smsBody.indexOf("Sent by Tracker!")));

                        // update location info
                        prevLocation = new LatLng(lat, lng);
                        setMarker(prevKey, prevKeyName);

                        unregisterReceiver(smsReceiver);

                    } catch (NumberFormatException e) {
                        Log.d("LocationSMSReceiver", "Not the required sms");
                    }
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // No nedd to Handle navigation view item.

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}