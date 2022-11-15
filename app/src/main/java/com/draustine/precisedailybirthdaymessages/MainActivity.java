package com.draustine.precisedailybirthdaymessages;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private SwipeRefreshLayout swipeRefreshLayout;
    private String[] PERMISSIONS;
    private SubscriptionManager subsManager;
    private SubscriptionInfo subsInfo1, subsInfo2;
    private SmsManager smsManager;
    private int maxSimCount, simCount, activeSim, simSlot;
    private TextView display1, display2;
    private EditText phoneNumber, message;
    private RadioGroup simSelector;
    private RadioButton sim1, sim2, selectedSim;
    private String carrier, carrier1, carrier2, activeCarrier, providers, shortCode, on, off;
    private String phone, phone1, phone2;
    private AlertDialog.Builder builder;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        phoneNumber = findViewById(R.id.phoneNumber);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        

    }
}