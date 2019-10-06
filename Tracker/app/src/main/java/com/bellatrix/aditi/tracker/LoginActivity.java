package com.bellatrix.aditi.tracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.thrivecom.ringcaptcha.RingcaptchaAPIController;

public class LoginActivity extends AppCompatActivity {

    private String phone;
    private SharedPreferences sp;
    private RingcaptchaAPIController controller;

    private static final String APP_KEY = "7o7u6ugisi7ihu4e7e5i", API_KEY = "8d2460c22a50da072ec5b4a952215f500657882d";

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

        controller = new RingcaptchaAPIController(APP_KEY);

        otpb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                phone = phoneet.getText().toString();
//                sendMsg(phone);
            }
        });

        loginb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                verify(otpet.getText().toString());
                goToHome();
            }
        });
    }

    private void goToHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("phonenumber", phone);
        startActivity(intent);
        sp.edit().putBoolean("logged", true).apply();
        finish();
    }
}
