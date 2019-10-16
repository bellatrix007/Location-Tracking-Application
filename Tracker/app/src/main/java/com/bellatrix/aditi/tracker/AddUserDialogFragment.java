package com.bellatrix.aditi.tracker;

import android.app.Application;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.DialogFragment;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class AddUserDialogFragment extends DialogFragment {

    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;

    private Button neutral, positive;
    private EditText et_user_phonenumber;
    private TextView tv_user_text;
    private ImageView iv_user_sign;

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle("Add User");
        LayoutInflater inflater = getActivity().getLayoutInflater();
        builder.setView(inflater.inflate(R.layout.dialog_add_user, null))
                .setPositiveButton("Search", null)
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        AddUserDialogFragment.this.getDialog().cancel();
                    }
                })
                .setNeutralButton("Back", null);

        final AlertDialog mAlertDialog = builder.create();

        mAlertDialog.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
            public void onShow(DialogInterface dialog) {

                neutral = mAlertDialog.getButton(AlertDialog.BUTTON_NEUTRAL);
                positive = mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                et_user_phonenumber = ((EditText) AddUserDialogFragment.this.getDialog()
                        .findViewById(R.id.et_user_number));
                tv_user_text = ((TextView) AddUserDialogFragment.this.getDialog()
                        .findViewById(R.id.tv_user_text));
                iv_user_sign = ((ImageView) AddUserDialogFragment.this.getDialog()
                        .findViewById(R.id.iv_user_sign));

                // neutral button would be invisible initially
                neutral.setVisibility(View.GONE);

                neutral.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        neutral.setVisibility(View.GONE);
                        et_user_phonenumber.setVisibility(View.VISIBLE);
                        et_user_phonenumber.setText("");
                        tv_user_text.setVisibility(View.GONE);
                        iv_user_sign.setVisibility(View.GONE);
                        positive.setText("Search");
                    }
                });

                positive.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {

                        if(positive.getText().toString().equals("Search"))
                        {
                            searchForUser();
                        }
                        else
                        {
                            // add user, send request

                            // dismiss dialog
                            AddUserDialogFragment.this.getDialog().dismiss();
                        }

                    }
                });
            }
        });

        return mAlertDialog;
    }

    private void searchForUser() {

        // search for the user in the database
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference();
        Query searchQuery = databaseReference.child("users")
                .orderByKey().equalTo(et_user_phonenumber.getText().toString());
        searchQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                neutral.setVisibility(View.VISIBLE);
                et_user_phonenumber.setVisibility(View.GONE);
                tv_user_text.setVisibility(View.VISIBLE);
                iv_user_sign.setVisibility(View.VISIBLE);
                positive.setText("Add");

                if(dataSnapshot.exists()) {
                    //Key exists
                    // TODO: Also print the user name
                    Log.d("User","User exists");
                    tv_user_text.setText("User verified");
                    iv_user_sign.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_tick, null));

                } else {
                    Log.d("User","User does not exist");
                    tv_user_text.setText("User does not exist");
                    iv_user_sign.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_cross, null));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}
