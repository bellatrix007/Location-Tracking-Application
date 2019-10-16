package com.bellatrix.aditi.tracker;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bellatrix.aditi.tracker.DatabaseClasses.Request;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class TabFragment extends Fragment {

    private int type;
    private String user;
    private ArrayList<Request> requests= new ArrayList<>();

    private RecyclerView recyclerView;
    private RequestAdapter requestAdapter;

    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;

    public static Fragment getInstance(String user, int position) {
        Bundle bundle = new Bundle();
        bundle.putString("user", user);
        bundle.putInt("pos", position);
        TabFragment tabFragment = new TabFragment();
        tabFragment.setArguments(bundle);
        return tabFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        user = getArguments().getString("user");
        type = getArguments().getInt("pos");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tab, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = (RecyclerView) view.findViewById(R.id.rv_request);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                layoutManager.getOrientation());
        recyclerView.addItemDecoration(dividerItemDecoration);

        requestAdapter = new RequestAdapter(user, type, requests);
        recyclerView.setAdapter(requestAdapter);

        // get requests list according to type
        final String request_type;
        // pending request
        if(type == 0) {
            request_type = "pending_req";
        } else {    // sent request
            request_type = "sent_req";
        }

        // database hook up
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference();
        DatabaseReference query = databaseReference.child("users").child(user).child(request_type);

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                requests.clear();
                for (DataSnapshot ds : dataSnapshot.getChildren()) {

                    // get user
                    String pending_user = ds.getKey();
                    // user name
                    String pending_user_name = ds.getValue().toString();
                    // add the request
                    requests.add(new Request(pending_user, pending_user_name));
                }

                requestAdapter = new RequestAdapter(user, type, requests);
                recyclerView.setAdapter(requestAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }
}
