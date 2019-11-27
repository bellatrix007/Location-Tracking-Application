package com.bellatrix.trackerb;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;

import com.bellatrix.trackerb.Utils.FetchURL;
import com.bellatrix.trackerb.Utils.TaskLoadedCallback;
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

import android.util.Log;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import androidx.drawerlayout.widget.DrawerLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.Menu;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import static com.bellatrix.trackerb.Utils.CommonFunctions.getUrl;

public class CustomerActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback,
        TaskLoadedCallback, DialogInterface.OnDismissListener {

    private static final int PERMISSION_LOCATION = 1234;
    private static final int REQUEST_PHONE_CALL = 1235;

    private String user, user_name, prevOrder;
    private Intent trackerServiceIntent;
    private boolean distance_expanded,time_expanded;

    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;

    private GoogleMap mMap;
    private Polyline mPolyline;
    private LatLng currLocation, delLoc;
    private ValueEventListener markerListener, deliveryListener;
    private Marker mMarker;
    private String admin, delivery, status;
    private boolean oldOrder;

    private SubMenu viewOrders;
    private FloatingActionButton call;
    private TextView delivered;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        user = getSharedPreferences("login", MODE_PRIVATE).getString("user", "");
        prevOrder = "";
        oldOrder = false;

        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        final View headerLayout = navigationView.getHeaderView(0);
        navigationView.setNavigationItemSelectedListener(this);

        // check for new user
        databaseReference.child("customer").child(user).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(!dataSnapshot.exists() || dataSnapshot.child("name").getValue() == null) {
                    // show undismissable alert dialog to enter name
                    // pop up register user dialog
                    RegisterUserDialogFragment registerUserDialogFragment = RegisterUserDialogFragment.newInstance("customer", user);
                    registerUserDialogFragment.setCancelable(false);
                    registerUserDialogFragment.show(getSupportFragmentManager(),"registerUser");
                } else {
                    // fetch name and set in shared preferences
                    user_name = dataSnapshot.child("name").getValue().toString();
                    getSharedPreferences("login", MODE_PRIVATE).edit()
                            .putString("user_name", user_name).apply();

                    ((TextView)headerLayout.findViewById(R.id.tv_h1)).setText(user_name);
                    ((TextView)headerLayout.findViewById(R.id.tv_h2)).setText(user);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        call = findViewById(R.id.fab_call);
        call.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent;
                if(status.equals("0"))
                    intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + delivery));
                else
                    intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + admin));
                // first check for permission
                if (ContextCompat.checkSelfPermission(CustomerActivity.this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(CustomerActivity.this, new String[]{Manifest.permission.CALL_PHONE},REQUEST_PHONE_CALL);
                }
                else {
                    startActivity(intent);
                }
            }
        });

        delivered = findViewById(R.id.deliverd);
        delivered.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // remove listeners first
                databaseReference.child("order").child(prevOrder).removeEventListener(markerListener);
                if(deliveryListener!=null)
                    databaseReference.child("location").child(delivery).removeEventListener(deliveryListener);

                // delete the order from everywhere
                // customer
                databaseReference.child("customer").child(user).child("order").orderByKey().equalTo(prevOrder)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for(DataSnapshot ds : dataSnapshot.getChildren())
                            ds.getRef().removeValue();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
                // admin
                databaseReference.child("admin").child(admin).child("order").orderByKey().equalTo(prevOrder)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for(DataSnapshot ds : dataSnapshot.getChildren())
                            ds.getRef().removeValue();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
                // order
                databaseReference.child("order").orderByKey().equalTo(prevOrder)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for(DataSnapshot ds : dataSnapshot.getChildren())
                            ds.getRef().removeValue();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

                // UI changes
                call.hide();
                delivered.setVisibility(View.GONE);
                ((LinearLayout)findViewById(R.id.distanceLayout)).setVisibility(View.GONE);
                ((LinearLayout)findViewById(R.id.timeLayout)).setVisibility(View.GONE);

                Toast.makeText(CustomerActivity.this, "Order delivered!", Toast.LENGTH_SHORT).show();
            }
        });

        askPermission();

        distance_expanded = false;
        time_expanded = false;

        final LinearLayout distance = (LinearLayout) findViewById(R.id.distanceLayout);
        final TextView distanceHead = (TextView) findViewById(R.id.distanceHead);
        final TextView distanceText = (TextView) findViewById(R.id.distanceText);
        ViewGroup.LayoutParams layoutParams = distance.getLayoutParams();
//        layoutParams.height = 150;
//        layoutParams.width = 150;
        distance.setLayoutParams(layoutParams);
        distance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(distance_expanded)
                {
                    ViewGroup.LayoutParams layoutParams = distance.getLayoutParams();
//                    layoutParams.width = 300;
                    distance.setLayoutParams(layoutParams);
                    distance_expanded = false;
                    distanceHead.setVisibility(View.VISIBLE);
                    distanceText.setVisibility(View.GONE);
                }
                else
                {
                    ViewGroup.LayoutParams layoutParams = distance.getLayoutParams();
//                    layoutParams.width = 300;
                    distance.setLayoutParams(layoutParams);
                    distance_expanded = true;
                    distance.setBackground(ContextCompat.getDrawable(CustomerActivity.this, R.drawable.expanded_button));
                    distanceHead.setVisibility(View.GONE);
                    distanceText.setVisibility(View.VISIBLE);
                }
            }
        });

        final LinearLayout time = (LinearLayout) findViewById(R.id.timeLayout);
        final TextView timeHead = (TextView) findViewById(R.id.timeHead);
        final TextView timeText = (TextView) findViewById(R.id.timeText);
        ViewGroup.LayoutParams timelayoutParams = time.getLayoutParams();
//        timelayoutParams.height = 150;
//        timelayoutParams.width = 150;
        time.setLayoutParams(timelayoutParams);
        time.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(time_expanded)
                {
                    ViewGroup.LayoutParams layoutParams = time.getLayoutParams();
//                    layoutParams.width = 150;
                    time.setLayoutParams(layoutParams);
                    time_expanded = false;
                    timeHead.setVisibility(View.VISIBLE);
                    timeText.setVisibility(View.GONE);
                }
                else
                {
                    ViewGroup.LayoutParams layoutParams = time.getLayoutParams();
//                    layoutParams.width = 300;
                    time.setLayoutParams(layoutParams);
                    time_expanded = true;
                    time.setBackground(ContextCompat.getDrawable(CustomerActivity.this, R.drawable.expanded_button));
                    timeHead.setVisibility(View.GONE);
                    timeText.setVisibility(View.VISIBLE);
                }
            }
        });


        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
//        mAppBarConfiguration = new AppBarConfiguration.Builder(
//                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow,
//                R.id.nav_tools, R.id.nav_share, R.id.nav_send)
//                .setDrawerLayout(drawer)
//                .build();
        navigationView.setNavigationItemSelectedListener(this);

        viewOrders = navigationView.getMenu().addSubMenu("Orders");
        // add the orders to the list
        databaseReference.child("customer").child(user).child("order").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                viewOrders.clear();
                for(DataSnapshot ds: dataSnapshot.getChildren()) {
                    viewOrders.add(ds.getKey() + ": " + ds.getValue());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        switch (menuItem.getItemId()){
            case R.id.logout:
                goToLogin();
                break;
            default:    // view order
                if(call.getVisibility() == View.GONE)
                    call.show();
                if(deliveryListener!=null)
                    databaseReference.child("location").child(delivery).removeEventListener(deliveryListener);
                delLoc = null;
                String title = menuItem.getTitle().toString();
                String id = title.substring(0, title.indexOf(":"));
                ((LinearLayout)findViewById(R.id.distanceLayout)).setVisibility(View.VISIBLE);
                ((LinearLayout)findViewById(R.id.timeLayout)).setVisibility(View.VISIBLE);
                setUpdates(id);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void setUpdates(String id) {
        if(markerListener != null && !prevOrder.equals("")) {
            databaseReference.child("order").child(prevOrder).removeEventListener(markerListener);
        }
        markerListener = databaseReference.child("order").child(id).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot ds) {

                String user_key = ds.getKey();
                if(!prevOrder.equals(user_key))
                    oldOrder = false;

                // get admin
                if(ds.child("admin").getValue()==null)
                    return;

                admin = ds.child("admin").getValue().toString();

                // check if order is marked delivered by the delivery person
                status = ds.child("delivered").getValue().toString();

                if(status.equals("1")) {
                    // UI changes
                    delivered.setVisibility(View.VISIBLE);

                    // reinitialize map
                    if(mMarker!=null)
                    {
                        mMarker.remove();
                        mMarker = null;
                    }
                    if(mPolyline!=null) {
                        mPolyline.remove();
                    }
                }
                else {

                    // UI changes
                    delivered.setVisibility(View.GONE);

                    // add listener for delivery
                    delivery = ds.child("delivery").getValue().toString();
                    deliveryListener = databaseReference.child("location").child(delivery)
                            .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            // update customer location
                            double lat = Double.parseDouble(dataSnapshot.child("latitude").getValue().toString());
                            double lng = Double.parseDouble(dataSnapshot.child("longitude").getValue().toString());

                            delLoc = new LatLng(lat, lng);
                            updateMarker();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }

                prevOrder = user_key;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void updateMarker() {

        if(mMap==null)
            return;

        if(mMarker == null)
        {
            mMarker = mMap.addMarker(new MarkerOptions().position(delLoc).title(delivery));
        }
        else
        {
            mMarker.setTitle(delivery);
            mMarker.setPosition(delLoc);
        }
        mMarker.showInfoWindow();
        // update camera bounds
        updateCameraBounds();

        if(delLoc!=null) {
            new FetchURL(CustomerActivity.this)
                    .execute(getUrl(delLoc, currLocation, "driving", getString(R.string.google_maps_key)), "driving");
        }

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // update the user's location
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
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

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Start the service when the permission is granted
                    startTrackerService();

                    // also start location extraction here
                    getCurrentLocation();

                    if (mMap == null) {
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
            case REQUEST_PHONE_CALL: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startActivity(new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + delivery)));
                }
                return;
            }
        }
    }

    private void updateCameraBounds() {
        if(mMap==null)
            return;

        // initial user's location only
        if(delLoc == null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currLocation, 15));
        } else {
            // set bounds if a new user
            if(!oldOrder) {
                oldOrder = true;
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                builder.include(currLocation);
                builder.include(delLoc);
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 20));

            }
            // else do nothing
        }
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
                        updateCameraBounds();

                        if(delLoc != null)
                            new FetchURL(CustomerActivity.this)
                                .execute(getUrl(delLoc, currLocation, "driving", getString(R.string.google_maps_key)),
                                        "driving");
                    }
                }
            }, null);
        }
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

    private void goToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        SharedPreferences.Editor spEdit = getSharedPreferences("login", MODE_PRIVATE).edit();
        spEdit.putInt("logged", 0);
        spEdit.putString("user", "");
        spEdit.apply();
        finish();
    }

    @Override
    public void onTaskDone(Object... values) {
        if (mPolyline != null)
            mPolyline.remove();
        mPolyline = mMap.addPolyline((PolylineOptions) values[2]);
        ((TextView)findViewById(R.id.distanceText)).setText(values[0].toString());
        ((TextView)findViewById(R.id.timeText)).setText(values[1].toString());
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.customer, menu);
        return true;
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        user_name = getSharedPreferences("login", MODE_PRIVATE).getString("user_name", "");
        ((TextView)findViewById(R.id.tv_h1)).setText(user_name);
        ((TextView)findViewById(R.id.tv_h2)).setText(user);
    }
}
