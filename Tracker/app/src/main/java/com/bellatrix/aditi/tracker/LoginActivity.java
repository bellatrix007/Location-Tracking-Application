package com.bellatrix.aditi.tracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class LoginActivity extends AppCompatActivity {

    private SharedPreferences sp;

    private EditText phoneet, otpet;
    private Button otpb, loginb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sp = getSharedPreferences("login", MODE_PRIVATE);

        if(sp.getBoolean("logged", false)) {
            goToHome();
        }

        phoneet = (EditText) findViewById(R.id.textPhone);
        otpet = (EditText) findViewById(R.id.textOTP);
        otpb = (Button) findViewById(R.id.buttonOTP);
        loginb = (Button) findViewById(R.id.buttonLogin);

//        otpb.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                sendMsg(phoneet.getText().toString());
//            }
//        });

        loginb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                verify(otpet.getText().toString());
                goToHome();
            }
        });
    }

    private void goToHome() {
        startActivity(new Intent(this, MainActivity.class));
        sp.edit().putBoolean("logged", true).apply();
        finish();
    }
}
