package com.bellatrix.aditi.tracker;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ExpandableListView;
import android.widget.ImageView;
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
import com.bellatrix.aditi.tracker.DatabaseClasses.RequiredLocation;
import com.bellatrix.aditi.tracker.DatabaseClasses.UserMenuModel;
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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
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
        DialogInterface.OnDismissListener {

    private String user, user_name;

    private static final int PERMISSION_LOCATION = 1234;

    ExpandableUserListAdapter expandableListAdapter;
    ExpandableListView expandableListView;
    List<UserMenuModel> headerList = new ArrayList<>();
    HashMap<UserMenuModel, List<UserMenuModel>> childList = new HashMap<>();

    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;

    private GoogleMap mMap;
    private Marker mMarker;
    private String prevKey;
    private ValueEventListener markerListener;

    private RequiredLocation currLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        user =  getSharedPreferences("login", MODE_PRIVATE).getString("user", "");
        prevKey = "";

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
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.add_user) {

        } else if (id == R.id.view_req) {

        } else if (id == R.id.logout) {

        }

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
        }
        markerListener = databaseReference.child("locations").child(key).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                setMarker(dataSnapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        prevKey = key;
    }

    // TODO: add cameraanimation for specific bounds
    // TODO: add path from current user to the marker
    private void setMarker(DataSnapshot dataSnapshot) {
        if(mMap==null)
            return;

        String key = dataSnapshot.getKey();
        Log.d("Maps", key);
        double lat = Double.parseDouble(dataSnapshot.child("latitude").getValue().toString());
        double lng = Double.parseDouble(dataSnapshot.child("longitude").getValue().toString());
        if(mMarker == null)
        {
            mMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lng)).title(key));
        }
        else
        {
            mMarker.setTitle(key);
            mMarker.setPosition(new LatLng(lat,lng));
        }
        mMarker.showInfoWindow();
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lng), 7));
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
        startService(new Intent(this, TrackerService.class));
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
                        currLocation = new RequiredLocation(location.getLatitude(), location.getLongitude());
                        Log.d("Location", currLocation.latitude + " " + currLocation.longitude);
                    }
                }
            }, null);
        }
    }
}
