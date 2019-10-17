package com.bellatrix.aditi.tracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ExpandableListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.bellatrix.aditi.tracker.DatabaseClasses.UserMenuModel;
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
        implements NavigationView.OnNavigationItemSelectedListener {

    private String user;

    ExpandableUserListAdapter expandableListAdapter;
    ExpandableListView expandableListView;
    List<UserMenuModel> headerList = new ArrayList<>();
    HashMap<UserMenuModel, List<UserMenuModel>> childList = new HashMap<>();

    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        user =  getSharedPreferences("login", MODE_PRIVATE).getString("user", "");

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, user, Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference();

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

                return false;
            }
        });
    }

    private void goToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        SharedPreferences.Editor spEdit = getSharedPreferences("login", MODE_PRIVATE).edit();
        spEdit.putBoolean("logged", false);
        spEdit.putString("user", "");
        spEdit.apply();
        finish();
    }
}
