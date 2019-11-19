package com.bellatrix.trackerb;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.bellatrix.trackerb.DatabaseClasses.Order;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AddOrderActivity extends AppCompatActivity {

    private String description, customer, delivery;
    private int success, flag;

    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;
    private ValueEventListener mListener;

    private EditText t1, t2, t3;
    private Button button;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_order);

        success = 0;
        flag = 0;

        t1 = findViewById(R.id.et_order_des);
        t2 = findViewById(R.id.et_cust);
        t3 = findViewById(R.id.et_delivery);
        button = findViewById(R.id.add_order_button);

        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference();

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                flag = 1;
                description = t1.getText().toString();
                customer = "+91" + t2.getText().toString();
                delivery = "+91" + t3.getText().toString();

                // check for valid entries
                check();
            }
        });
    }

    private void check() {
        // check if delivery person exists and idle
        mListener = databaseReference.child("delivery").orderByKey().equalTo(delivery).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if(!dataSnapshot.exists()) {
                    success = -1;
                }
                else if(Boolean.parseBoolean(dataSnapshot.child(delivery).child("idle").getValue().toString())) {
                    success = 1;
                } else {
                    success = 0;
                }
                // remove listener
                databaseReference.child("delivery").child(delivery).removeEventListener(mListener);
                if(flag == 1) {
                    flag = 0;
                    if (success == -1)
                        Toast.makeText(AddOrderActivity.this, "Invalid delivery person", Toast.LENGTH_SHORT).show();
                    else if (success == 0)
                        Toast.makeText(AddOrderActivity.this, "Delivery person is busy", Toast.LENGTH_SHORT).show();
                    else if (success == 1)
                        addOrder(customer, description);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void addOrder(String customer, String description) {
        String admin = getSharedPreferences("login", MODE_PRIVATE).getString("user", "");

        // add order in database
        String id = databaseReference.child("order").push().getKey();
        Order order = new Order(admin, customer, delivery, description);
        databaseReference.child("order").child(id).setValue(order);

        // add order to admin
        databaseReference.child("admin").child(admin).child("order").child(id).setValue(description);

        // make delivery person as busy
        databaseReference.child("delivery").child(delivery).child("idle").setValue("false");
        databaseReference.child("delivery").child(delivery).child("order").setValue(id);

        // add order to customer list
        databaseReference.child("customer").child(customer).child("order").child(id).setValue(description);

        Toast.makeText(AddOrderActivity.this, "Order added", Toast.LENGTH_SHORT).show();

        // finish Activity
        AddOrderActivity.this.finish();
    }
}
