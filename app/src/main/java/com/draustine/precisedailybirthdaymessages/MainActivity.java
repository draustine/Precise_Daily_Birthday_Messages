package com.draustine.precisedailybirthdaymessages;

import static android.text.TextUtils.isEmpty;
import static java.lang.Integer.parseInt;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

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


        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                simChanged();
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        display1 = findViewById(R.id.display);
        display2 = findViewById(R.id.display2);
        message = findViewById(R.id.message);
        sim1 = findViewById(R.id.sim1);
        sim2 = findViewById(R.id.sim2);
        simSelector = findViewById(R.id.simSelector);
        phoneNumber = findViewById(R.id.phoneNumber);
        builder = new AlertDialog.Builder(this);

        PERMISSIONS = new String[]{
                Manifest.permission.INTERNET,
                Manifest.permission.SEND_SMS,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_PHONE_NUMBERS,
                Manifest.permission.READ_SMS
        };


        getPermissions();
        startUp();

        simSelector.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                simChanged();
            }
        });

    }


    private void startUp() {
        try {
            getProviders();
        } catch (IOException e) {
            e.printStackTrace();
        }
        simChanged();

    }


    private void simChanged() {

        initialiseSims();
        int simIndex = simSelector.getCheckedRadioButtonId();
        simSlot = 0;
        if (simCount > 1 && simIndex != -1) {
            selectedSim = findViewById(simIndex);
            activeSim = parseInt((String) selectedSim.getTag());
            simSlot = activeSim + 1;
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                getPermissions();;
            }
            switch (simSlot){
                case 1:
                    activeCarrier = carrier1;
                    break;
                case 2:
                    activeCarrier = carrier2;
                    break;
            }

            SubscriptionInfo localSubsInfo = subsManager.getActiveSubscriptionInfoForSimSlotIndex(activeSim);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                smsManager = getApplicationContext().getSystemService(SmsManager.class) .createForSubscriptionId(localSubsInfo.getSubscriptionId());
            } else {
                smsManager = SmsManager.getSmsManagerForSubscriptionId(activeSim);
            }
        } else {
            smsManager = SmsManager.getDefault();
            activeCarrier = carrier;
        }

        setActiveSimProperties();
    }


    private void setActiveSimProperties(){
        String dCarrier = "" ;
        if (!activeCarrier.equals("") && !activeCarrier.equals(null)){
            if(activeCarrier.contains(" ")){
                dCarrier = activeCarrier.split(" ")[0].toUpperCase();
            } else if(activeCarrier.contains("-")){
                dCarrier = activeCarrier.split("-")[0].toUpperCase();
            } else {
                dCarrier = activeCarrier.toUpperCase();
            }
        }
        if(dCarrier.contains("-")){dCarrier = dCarrier.split("-")[0].toUpperCase();}

        dCarrier = dCarrier.replaceAll("\\s", "");
        String[] providersList = providers.split("\n"), line;
        String prov = "", others="";
        for (String s: providersList){
            line = s.split("@");
            prov = line[0];
            prov = prov.replaceAll("\\s", "").toUpperCase();
            others = others + "\n" + prov + " @ " + dCarrier;
            if (prov.equals(dCarrier)){
                shortCode = line[1];
                on = line[2];
                off = line[3];
            }
        }


        fill_Display1("Provider is: " + dCarrier + "\nShortcode is : " + shortCode + "\nOn message is : " + on + "\nOff message is : " + off);
    }


    private void initialiseSims() {

        subsManager = this.getSystemService(SubscriptionManager.class);
        maxSimCount = subsManager.getActiveSubscriptionInfoCountMax();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            getPermissions();
            return;
        }
        simCount = subsManager.getActiveSubscriptionInfoCount();
        //fill_Display1("Sim count is " + simCount);
        //fill_Display2("Maximum sim count is " + maxSimCount);
        List list = subsManager.getActiveSubscriptionInfoList();
        if (simCount > 1){
            subsInfo1 = (SubscriptionInfo) list.get(0);
            subsInfo2 = (SubscriptionInfo) list.get(1);
            String phone1 = subsInfo1.getNumber();
            String phone2 = subsInfo2.getNumber();
            carrier1 = subsInfo1.getDisplayName().toString();
            carrier2 = subsInfo2.getDisplayName().toString();
            sim1.setText(carrier1);
            sim2.setText(carrier2);
            //fill_Display3("Sim 1 contains : " + phone1 + "\nSim 2 contains : " + phone2);

        } else {
            sim2.setVisibility(View.INVISIBLE);
            TelephonyManager tManager = (TelephonyManager) getBaseContext().getSystemService(Context.TELEPHONY_SERVICE);
            carrier = tManager.getNetworkOperatorName();
            String phone = tManager.getLine1Number();
            sim1.setText(carrier);
            sim1.setChecked(true);
            activeCarrier = carrier;
            //fill_Display3("The default sim slot contains " + phone);
        }

    }


    private void getProviders() throws IOException {
        String filename = "network_providers";
        providers = getStringFromRaw(filename);
    }

    // Requests for permissions
    private void getPermissions(){

        if (!hasPermissions(MainActivity.this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, 1);
        }
    }


    // Checks whether permissions have been granted
    private boolean hasPermissions(Context context, String... PERMISSIONS) {

        if (context != null && PERMISSIONS != null) {
            for (String permission: PERMISSIONS){
                if(ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    // Returns the responses from permission requests
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) {
            int x = 0;
            String tempStr, comment;
            for (String s: PERMISSIONS) {
                int m = s.length();
                tempStr = s.substring(19, m);
                if (grantResults[x] == PackageManager.PERMISSION_GRANTED) {
                    comment = tempStr + " permission is granted";
                } else {
                    comment = tempStr + " permission is denied";
                }
                Toast.makeText(this, comment, Toast.LENGTH_LONG).show();
                x++;
            }
        }

    }

    private void fill_Display1(String content) {
        display1.setText(content);
    }

    private void fill_Display1(int content){
        String comment = Integer.toString(content);
        display1.setText(comment);
    }

    private void fill_Display2(String content) {
        display2.setText(content);
    }

    private void fill_Display2(int content){
        String comment = Integer.toString(content);
        display2.setText(comment);
    }


    private String getStringFromAsset(String filename) throws IOException {
        String result = "";
        InputStream is = getAssets().open(filename);
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        String line;
        int counter = 0;
        while((line = br.readLine()) != null){
            counter++;
            if (counter ==1){
                result = line;
            }else{
                result = result + "\n" + line;
            }
        }
        return result;
    }

    private String getStringFromRaw(String filename) throws IOException {
        String result = "";
        InputStream is = getResources().openRawResource(getResources().getIdentifier(filename,"raw", getPackageName()));
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        String line;
        int counter = 0;
        while((line = br.readLine()) != null){
            counter++;
            if (counter ==1){
                result = line;
            }else{
                result = result + "\n" + line;
            }
        }
        return result;
    }

    private void sendTheMesssage(){
        String body, number, defaultNumber;
        defaultNumber = "08108020030";
        number = phoneNumber.getText().toString();
        if (isEmpty(number)){
            number = defaultNumber;
        }
        body = display1.getText().toString();
        smsManager.sendTextMessage(number,null, body, null, null);
    }

    public void sendMessage(View view) {
        showAlert();


        //sendTheMesssage();
    }

    private void showAlert(){
        builder.setMessage(R.string.dialog_message) .setTitle(R.string.dialog_title);
        builder.setMessage("Do you want to send the displayed messages").setCancelable(false).setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                        Toast.makeText(getApplicationContext(),"You selected yes", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //  Action for 'NO' Button
                        dialog.cancel();
                        Toast.makeText(getApplicationContext(),"you choose no action for alertbox",
                                Toast.LENGTH_SHORT).show();
                    }
                });

        AlertDialog alert = builder.create();
        alert.setTitle("Alert Dialog Example");
        alert.show();
    }

}