package com.bellatrix.trackerb;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.bellatrix.trackerb.Utils.FetchURL;
import com.bellatrix.trackerb.Utils.TaskLoadedCallback;
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
import com.google.android.material.snackbar.Snackbar;

import android.util.Log;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.view.GravityCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

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
import android.widget.LinearLayout;
import android.widget.Toast;

import static com.bellatrix.trackerb.Utils.CommonFunctions.getUrl;

public class AdminActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback, TaskLoadedCallback {

    private String user, prevOrder, customer, delivery;
    private boolean oldUser;
    private GoogleMap mMap;
    private LatLng custLoc, delLoc;
    private Marker cMarker, dMarker;

    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;

    private SubMenu viewOrders;
    private AppBarConfiguration mAppBarConfiguration;
    private ValueEventListener markerListener, cusListener, delListener;
    private Polyline mPolyline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        user = getSharedPreferences("login", MODE_PRIVATE).getString("user", "");
        prevOrder = "";
        oldUser = false;

        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference();

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow,
                R.id.nav_tools, R.id.nav_share, R.id.nav_send)
                .setDrawerLayout(drawer)
                .build();
        navigationView.setNavigationItemSelectedListener(this);

        viewOrders = navigationView.getMenu().addSubMenu("Orders");
        // add the orders to the list
        databaseReference.child("admin").child(user).child("order").addValueEventListener(new ValueEventListener() {
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

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.admin, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        switch (item.getItemId()){
            case R.id.add_order:
                startActivity(new Intent(this, AddOrderActivity.class));
                break;
            case R.id.logout:
                goToLogin();
                break;
            default:    // view order
                if(cusListener!=null)
                    databaseReference.child("location").child(customer).removeEventListener(cusListener);
                custLoc = null;
                if(delListener!=null)
                    databaseReference.child("location").child(delivery).removeEventListener(delListener);
                delLoc = null;
                String title = item.getTitle().toString();
                String id = title.substring(0, title.indexOf(":"));
                setUpdates(id);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
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
                    oldUser = false;

                // add 2 listeners, one for customer and one for delivery
                customer = ds.child("customer").getValue().toString();
                cusListener = databaseReference.child("location").child(customer)
                        .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        // update customer location
                        double lat = Double.parseDouble(dataSnapshot.child("latitude").getValue().toString());
                        double lng = Double.parseDouble(dataSnapshot.child("longitude").getValue().toString());

                        custLoc = new LatLng(lat, lng);
                        updateMarker(1);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

                delivery = ds.child("delivery").getValue().toString();
                delListener = databaseReference.child("location").child(delivery)
                        .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        // update customer location
                        double lat = Double.parseDouble(dataSnapshot.child("latitude").getValue().toString());
                        double lng = Double.parseDouble(dataSnapshot.child("longitude").getValue().toString());

                        delLoc = new LatLng(lat, lng);
                        updateMarker(0);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

                prevOrder = user_key;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void updateMarker(int role) {

        if(mMap==null)
            return;

        if(role == 1) {  // customer
            if(cMarker == null) {
                cMarker = mMap.addMarker(new MarkerOptions().position(custLoc).title(customer));
            } else {
                cMarker.setTitle(customer);
                cMarker.setPosition(custLoc);
            }
            cMarker.showInfoWindow();
        } else {
            if(dMarker == null) {
                dMarker = mMap.addMarker(new MarkerOptions().position(delLoc).title(delivery));
            } else {
                dMarker.setTitle(delivery);
                dMarker.setPosition(delLoc);
            }
            dMarker.showInfoWindow();
        }
        // update camera bounds
        updateCameraBounds();

        if(custLoc!=null && delLoc!=null) {
            new FetchURL(AdminActivity.this)
                    .execute(getUrl(delLoc, custLoc, "driving", getString(R.string.google_maps_key)), "driving");
        }

    }

    private void updateCameraBounds() {
        if(mMap==null)
            return;

        // initial user's location only
        if(!oldUser && custLoc!=null && delLoc!=null) {
            oldUser = true;
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            builder.include(custLoc);
            builder.include(delLoc);
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 20));

        }
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
        mPolyline = mMap.addPolyline((PolylineOptions) values[0]);
    }
}
