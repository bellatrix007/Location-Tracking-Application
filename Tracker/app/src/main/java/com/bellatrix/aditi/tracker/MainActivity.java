package com.bellatrix.aditi.tracker;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.amulyakhare.textdrawable.TextDrawable;
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

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback,
        DialogInterface.OnDismissListener, TaskLoadedCallback {

    private String user, user_name;

    private static final int PERMISSION_LOCATION = 1234;

    ExpandableUserListAdapter expandableListAdapter;
    ExpandableListView expandableListView;
    List<UserMenuModel> headerList = new ArrayList<>();
    HashMap<UserMenuModel, List<UserMenuModel>> childList = new HashMap<>();

    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;

    private Intent trackerServiceIntent;

    private GoogleMap mMap;
    private Marker mMarker;
    private String prevKey, prevRinger;
    private boolean oldUser;
    private LatLng prevLocation;
    private ValueEventListener markerListener;
    private Polyline mPolyline;

    private LatLng currLocation;

    private ImageButton refreshRinger, updateRinger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        user =  getSharedPreferences("login", MODE_PRIVATE).getString("user", "");
        prevKey = "";
        prevRinger = "";
        oldUser = false;

        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference();

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
                    ((TextView)findViewById(R.id.tv_h1)).setText(user_name);
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

        askPermission();

        refreshRinger = (ImageButton) findViewById(R.id.refresh_ringer);
        refreshRinger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // request for refresh ringer of a user in prevKey
                if(!prevKey.equals("")) {
                    databaseReference.child("users").child(prevKey).child("refresh_ringer").setValue(user);
                }
            }
        });

        updateRinger = (ImageButton) findViewById(R.id.update_ringer);
        updateRinger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // request for ringer update of a user in prevKey
                if(!prevKey.equals("")) {
                    databaseReference.child("users").child(prevKey).child("update_ringer").setValue(user);
                }
            }
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, user, Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        expandableListView = findViewById(R.id.expandableListView);
        prepareMenuData();
        populateExpandableList();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        View headerLayout = navigationView.getHeaderView(0);
        ImageView image = (ImageView) headerLayout.findViewById(R.id.avatar);
        TextDrawable drawable = TextDrawable.builder()
                .buildRect("A", Color.RED);
        image.setImageDrawable(drawable);

        // listener for refresh ringer request
        databaseReference.child("users").child(user).orderByKey().equalTo("refresh_ringer")
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

        // listener for refresh ringer request
        databaseReference.child("users").child(user).orderByKey().equalTo("update_ringer")
                .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()) {
                    Log.d("Ringer", "Update request by "
                            + dataSnapshot.child("update_ringer").getValue());

                    // change the volume to max
                    AudioManager audioManager = ((AudioManager)getSystemService(Context.AUDIO_SERVICE));
                    audioManager.setStreamVolume(AudioManager.STREAM_RING,
                            audioManager.getStreamMaxVolume(AudioManager.STREAM_RING),0);
                    // also remove the request
                    dataSnapshot.child("update_ringer").getRef().removeValue();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void askPermission() {
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

    private void prepareMenuData() {

        // initialize
        final UserMenuModel userMenuModel1 = new UserMenuModel("View Location",null, true, true);
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

        // TODO: experiment with child listener as well
        databaseReference.child("users").child(user).child("seeing_of").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // has a list of all the users seeing_of
                List<UserMenuModel> childModelsList = new ArrayList<>();
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    childModelsList.add(new UserMenuModel(ds.getKey(),null, false, false));
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
                            AddUserDialogFragment addUserDialogFragment = AddUserDialogFragment.newInstance(user);;
                            addUserDialogFragment.show(getSupportFragmentManager(),"addUser");

                        } else if(menu_item_name.equals("View request")) {
                            // show pending and sent requests
                            Intent intent = new Intent(MainActivity.this, ViewRequestsActivity.class);
                            intent.putExtra("user", user);
                            startActivity(intent);

                        } else {    // log out
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
                        setUpdates(model.menuName);
                        onBackPressed();
                    }
                }

                return true;
            }
        });
    }

    private void setUpdates(String key) {
        if(markerListener != null && !prevKey.equals("")) {
            databaseReference.child("locations").child(prevKey).removeEventListener(markerListener);
        } else {
            ((LinearLayout)findViewById(R.id.bottomLayout)).setVisibility(View.VISIBLE);
            if(mMap!=null)
                mMap.setPadding(0,0,0,104);
        }
        markerListener = databaseReference.child("locations").child(key)
                .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                String user_key = dataSnapshot.getKey();

                if(prevKey.equals(user_key))
                    oldUser = true;

                // check if it is a ringer update
                if(dataSnapshot.child("ringer").getValue()!=null) {

                    String ringer = dataSnapshot.child("ringer").getValue().toString();
                    if (!prevRinger.equals(ringer) || !oldUser)    // new ringer info
                    {
                        // update ringer info
                        prevRinger = ringer;
                        // TODO: hook up UI
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
                    setMarker(user_key);
                }

                prevKey = user_key;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    // TODO: see other direction modes also
    // TODO: distance and time
    private void setMarker(String key) {
        if(mMap==null)
            return;

        Log.d("Maps", key);

        if(mMarker == null)
        {
            mMarker = mMap.addMarker(new MarkerOptions().position(prevLocation).title(key));
        }
        else
        {
            mMarker.setTitle(key);
            mMarker.setPosition(prevLocation);
        }
        mMarker.showInfoWindow();
        updateCameraBounds();
        new FetchURL(MainActivity.this)
                .execute(getUrl(currLocation, prevLocation, "driving"), "driving");
    }

    private String getUrl(LatLng origin, LatLng dest, String directionMode) {
        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;
        // Mode
        String mode = "mode=" + directionMode;
        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + mode;
        // Output format
        String output = "json";
        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters + "&key=" + getString(R.string.google_maps_key);
        return url;
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
                } else {
                    askPermission();
                }
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
    }

    private void getCurrentLocation() {

        // Location manager to get location
//        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
//
//        try {
//            // Register the listener with the Location Manager to receive location updates
//            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 0.00001f, new LocationListener() {
//                @Override
//                public void onLocationChanged(Location location) {
//
//                }
//
//                @Override
//                public void onStatusChanged(String s, int i, Bundle bundle) {
//
//                }
//
//                @Override
//                public void onProviderEnabled(String s) {
//
//                }
//
//                @Override
//                public void onProviderDisabled(String s) {
//
//                }
//            });
//        } catch (SecurityException e) {
//            Log.d("Location manager","Locations permission denied: " + e.getMessage());
//        }

        LocationRequest request = new LocationRequest();
        request.setInterval(30000);
        request.setFastestInterval(10000);
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
                    Location location = locationResult.getLastLocation();
                    if (location != null) {
                        currLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        Log.d("Location", currLocation.latitude + " " + currLocation.longitude);
                        updateCameraBounds();

                        if(prevLocation!=null)
                            new FetchURL(MainActivity.this)
                                    .execute(getUrl(currLocation, prevLocation, "driving"), "driving");
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
                Log.d("animate", "true" + prevKey);
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                builder.include(currLocation);
                builder.include(prevLocation);
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 20));
            } else {
                Log.d("animate", "false" + prevKey);
                mMap.animateCamera(CameraUpdateFactory.newLatLng(prevLocation));
            }
        }
    }

    private void requestRingerUpdates() {

        AudioManager mobilemode = (AudioManager)getSystemService(Context.AUDIO_SERVICE);

        switch (mobilemode.getRingerMode()) {
            case AudioManager.RINGER_MODE_SILENT:
                databaseReference.child("locations").child(user).child("ringer").setValue("Silent mode");
                break;
            case AudioManager.RINGER_MODE_VIBRATE:
                databaseReference.child("locations").child(user).child("ringer").setValue("Vibrate mode");
                break;
            case AudioManager.RINGER_MODE_NORMAL:
                databaseReference.child("locations").child(user).child("ringer")
                        .setValue("Normal mode: " + mobilemode.getStreamVolume(AudioManager.STREAM_RING));
                break;
        }
    }

    @Override
    public void onTaskDone(Object... values) {
        if (mPolyline != null)
            mPolyline.remove();
        mPolyline = mMap.addPolyline((PolylineOptions) values[0]);

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
}