package com.bellatrix.aditi.tracker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bellatrix.aditi.tracker.DatabaseClasses.Request;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.List;
import java.util.Queue;

public class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.FolderViewHolder> {

    private int type;
    private String user;
    private List<Request> requests;

    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;

    RequestAdapter(String user, int type, List<Request> requests)
    {
        this.user = user;
        this.type = type;
        this.requests = requests;
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference();
    }

    @Override
    public FolderViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.request_holder, parent, false);

        FolderViewHolder caseViewHolder = new FolderViewHolder(view);
        return caseViewHolder;
    }

    @Override
    public void onBindViewHolder(FolderViewHolder holder, int position) {
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    public class FolderViewHolder extends RecyclerView.ViewHolder {

        TextView tv_user, tv_name;
        Button cancel, accept;
        FolderViewHolder(View view)
        {
            super(view);
            tv_user = (TextView) view.findViewById(R.id.tv_user);
            tv_name = (TextView) view.findViewById(R.id.tv_name);
            cancel = (Button) view.findViewById(R.id.button_cancel);
            accept = (Button) view.findViewById(R.id.button_accept);
        }
        void bind(int position)
        {
            final String user1 = requests.get(position).getUser();
            this.tv_user.setText(user1);
            this.tv_name.setText(requests.get(position).getName());

            cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    // delete from pending requests
                    Query query1 = databaseReference.child("users").child(user).child("pending_req").orderByKey().equalTo(user1);
                    query1.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            for(DataSnapshot ds : dataSnapshot.getChildren())
                            {
                                ds.getRef().removeValue();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });

                    // delete from sent requests
                    Query query2 = databaseReference.child("users").child(user1).child("sent_req").orderByKey().equalTo(user);
                    query2.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            for(DataSnapshot ds : dataSnapshot.getChildren())
                            {
                                ds.getRef().removeValue();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }
            });

            accept.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // move the structures
                }
            });

            //sent requests
            if(type == 1) {
                cancel.setVisibility(View.GONE);
                accept.setVisibility(View.GONE);
            }
        }
    }

//    public void swapCursor(List<Request> requests)
//    {
//        if(requests!=null)
//            this.requests.clear();
//
//        this.requests = requests;
//        if(this.requests!=null)
//        {
//            this.notifyDataSetChanged();
//        }
//    }
}
