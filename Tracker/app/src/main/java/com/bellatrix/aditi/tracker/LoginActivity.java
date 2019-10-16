package com.bellatrix.aditi.tracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.CountDownTimer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bellatrix.aditi.tracker.Utils.CommonFunctions;
import com.thrivecom.ringcaptcha.RingcaptchaAPIController;
import com.thrivecom.ringcaptcha.RingcaptchaService;
import com.thrivecom.ringcaptcha.lib.handlers.RingcaptchaHandler;
import com.thrivecom.ringcaptcha.lib.handlers.RingcaptchaSMSHandler;
import com.thrivecom.ringcaptcha.lib.models.RingcaptchaResponse;

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
                sendMsg();
            }
        });

        loginb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                verify(otpet.getText().toString());
            }
        });
    }

    private void sendMsg() {

        controller.sendCaptchaCodeToNumber(getApplicationContext(), phone, RingcaptchaService.SMS, new RingcaptchaHandler() {

            //Called when the response is successful
            @Override
            public void onSuccess(RingcaptchaResponse response) {

                Toast.makeText(LoginActivity.this, "OTP sent" , Toast.LENGTH_SHORT).show();

                new CountDownTimer(CommonFunctions.getTimeDifference(response.timeout), 1000) {

                    public void onTick(long millisUntilFinished) {
                        otpb.setText("Retry after " + (millisUntilFinished / 1000) + " s");
                        otpb.setEnabled(false);
                    }

                    public void onFinish() {
                        otpb.setText("Resend OTP");
                        otpb.setEnabled(true);
                    }
                }.start();

                //Handle SMS reception automatically (only valid for verification)
                RingcaptchaAPIController.setSMSHandler(new RingcaptchaSMSHandler() {

                    //Only called when SMS reception was detected
                    @Override
                    public boolean handleMessage(String s, String s1) {
                        //Automatically verify PIN code
                        return true;
                    }
                });
            }
            //Called when the response is unsuccessful
            @Override
            public void onError(Exception e) {
                Toast.makeText(LoginActivity.this, "Please check the number and try again!" , Toast.LENGTH_SHORT).show();
            }
        }, API_KEY);
    }

    private void verify(String pin) {

        if(pin.length()!=4)
        {
            Toast.makeText(this, "Invalid OTP", Toast.LENGTH_SHORT).show();
        }

        controller.verifyCaptchaWithCode(getApplicationContext(), pin, new RingcaptchaHandler() {

            //Called when the response is successful
            @Override
            public void onSuccess(RingcaptchaResponse ringcaptchaResponse) {
                //Clear SMS handler to avoid multiple verification attempts

                RingcaptchaAPIController.setSMSHandler(null);
                if(ringcaptchaResponse.status.equals("SUCCESS")) {
                    goToHome();
                }
            }

            //Called when the response is unsuccessful
            @Override
            public void onError(Exception e) {
                Toast.makeText(LoginActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, API_KEY);
    }

    private void goToHome() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        SharedPreferences.Editor spEdit = sp.edit();
        if(sp.getString("user","").equals(""))
            spEdit.putString("user", phone);
        spEdit.putBoolean("logged", true);
        spEdit.apply();
        finish();
    }
}
