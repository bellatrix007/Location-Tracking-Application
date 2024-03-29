package com.bellatrix.trackerb;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.bellatrix.trackerb.Utils.CommonFunctions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
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

    private TextInputEditText phoneet,otpet;
    private MaterialButton otpb, loginb;
    private RadioGroup userRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sp = getSharedPreferences("login", MODE_PRIVATE);

        if(sp.getInt("logged", 0)!=0) {
            goToHome(sp.getInt("logged", 0));
        }

        phoneet = (TextInputEditText) findViewById(R.id.textPhone);
        otpet = (TextInputEditText) findViewById(R.id.textOTP);
        otpb = (MaterialButton) findViewById(R.id.buttonOTP);
        loginb = (MaterialButton) findViewById(R.id.buttonLogin);
        userRole = (RadioGroup) findViewById(R.id.radioGroup);

        controller = new RingcaptchaAPIController(APP_KEY);

        otpb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                phone = phoneet.getText().toString();
                if(phone.length()!=10)
                {
                    phoneet.setError("Enter Valid Phone number");
                }
                else {
                    phone = "+91" + phone;
                    sendMsg();
                }
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
                phoneet.setError("Number does not exists");
                //Toast.makeText(LoginActivity.this, "Please check the number and try again!" , Toast.LENGTH_SHORT).show();
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
                    goToHome(0);
                }
            }

            //Called when the response is unsuccessful
            @Override
            public void onError(Exception e) {
                Toast.makeText(LoginActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, API_KEY);
    }

    private void goToHome(int rolex) {
        Intent intent;

        SharedPreferences.Editor spEdit = sp.edit();
        if(sp.getString("user","").equals(""))
            spEdit.putString("user", phone);

        if(userRole == null)
        {
            switch (rolex) {
                case 1:
                    intent = new Intent(this, AdminActivity.class);
                    break;
                case 2:
                    intent = new Intent(this, DeliveryActivity.class);
                    break;
                case 3:
                    intent = new Intent(this, CustomerActivity.class);
                    break;
                default:
                    intent = new Intent(this, AdminActivity.class);
            }
        }
        else {
            int role = userRole.getCheckedRadioButtonId();
            switch (role) {
                case R.id.adminR:
                    intent = new Intent(this, AdminActivity.class);
                    spEdit.putInt("logged", 1);
                    break;
                case R.id.deliveryR:
                    intent = new Intent(this, DeliveryActivity.class);
                    spEdit.putInt("logged", 2);
                    break;
                case R.id.customerR:
                    intent = new Intent(this, CustomerActivity.class);
                    spEdit.putInt("logged", 3);
                    break;
                default:
                    intent = new Intent(this, AdminActivity.class);
                    spEdit.putInt("logged", 1);
            }
        }

        startActivity(intent);
        spEdit.apply();
        finish();
    }
}
