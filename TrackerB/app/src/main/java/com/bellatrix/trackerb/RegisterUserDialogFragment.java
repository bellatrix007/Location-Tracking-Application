package com.bellatrix.trackerb;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import static android.content.Context.MODE_PRIVATE;

public class RegisterUserDialogFragment  extends DialogFragment {

    private EditText et_user_name;

    private DatabaseReference databaseReference;

    private String userType, user, user_name;

    /**
     * Create a new instance of AddUserDialogFragment, providing "user"
     * as an argument.
     */
    static RegisterUserDialogFragment newInstance(String userType, String user) {
        RegisterUserDialogFragment f = new RegisterUserDialogFragment();

        // Supply num input as an argument.
        Bundle args = new Bundle();
        args.putString("userType", userType);
        args.putString("user", user);
        f.setArguments(args);

        return f;
    }

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle("New User?");
        LayoutInflater inflater = getActivity().getLayoutInflater();
        builder.setView(inflater.inflate(R.layout.dialog_register_user, null))
                .setPositiveButton("Ok", null);

        final AlertDialog mAlertDialog = builder.create();

        mAlertDialog.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
            public void onShow(DialogInterface dialog) {

                // firebase initialization
                databaseReference = FirebaseDatabase.getInstance().getReference();

                userType = getArguments().getString("userType","admin");
                user = getArguments().getString("user","");
                et_user_name = ((EditText) RegisterUserDialogFragment.this.getDialog()
                        .findViewById(R.id.et_user_name));

                Button b = mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE);

                b.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {

                        user_name = et_user_name.getText().toString();
                        databaseReference.child(userType).child(user).child("name").setValue(user_name);
                        databaseReference.child(userType).child(user).child("idle").setValue("true");

                        // add user name to shared preferences
                        if(getActivity()!=null && isAdded())
                            getActivity().getSharedPreferences("login", MODE_PRIVATE).edit()
                                    .putString("user_name", user_name).apply();

                        // dismiss dialog
                        RegisterUserDialogFragment.this.getDialog().dismiss();
                    }
                });
            }
        });

        return mAlertDialog;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        final Activity parentFragment = getActivity();
        if (parentFragment instanceof DialogInterface.OnDismissListener) {
            ((DialogInterface.OnDismissListener) parentFragment).onDismiss(dialog);
        }
    }
}

