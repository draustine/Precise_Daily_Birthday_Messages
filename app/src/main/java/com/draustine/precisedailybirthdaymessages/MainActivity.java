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
    private SmsManager smsManager;
    private int maxSimCount, simCount, activeSim, simSlot;
    private TextView display1, display2;
    private EditText phoneNumber, message;
    private RadioGroup simSelector;
    private RadioButton sim1, sim2, selectedSim;
    private String carrier, carrier1, carrier2, activeCarrier, providers, shortCode, on, off;
    private String phone, phone1, phone2;
    private AlertDialog.Builder builder;
    private String messageTemplate, belatedTemplate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        phoneNumber = findViewById(R.id.phoneNumber);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

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

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                afterSimChange();
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        getPermissions();

        //get the providers list
        try {
            getProviders();
        } catch (IOException e) {
            e.printStackTrace();
        }
        subsManager = this.getSystemService(SubscriptionManager.class);
        simSelector.setOnCheckedChangeListener((group, checkedId) -> afterSimChange());

        startUp();


    }


    private int getSimCount() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            getPermissions();
        }
        int count = subsManager.getActiveSubscriptionInfoCount();
        return count;
    }


    private void startUp() {
        afterSimChange();
        setActiveSimProperties();
    }

    private void getProviders() throws IOException {
        String filename = "network_providers";
        providers = getStringFromRaw(filename);
        filename = "Belated Message Template";
        belatedTemplate = getStringFromAsset(filename);
    }

    // Requests for permissions
    private void getPermissions() {

        if (!hasPermissions(MainActivity.this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, 1);
        }
    }


    // Checks whether permissions have been granted
    private boolean hasPermissions(Context context, String... PERMISSIONS) {

        if (context != null && PERMISSIONS != null) {
            for (String permission : PERMISSIONS) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
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
            for (String s : PERMISSIONS) {
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

    private void fill_Display1(int content) {
        String comment = Integer.toString(content);
        display1.setText(comment);
    }

    private void fill_Display2(String content) {
        display2.setText(content);
    }

    private void fill_Display2(int content) {
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
        while ((line = br.readLine()) != null) {
            counter++;
            if (counter == 1) {
                result = line;
            } else {
                result = result + "\n" + line;
            }
        }
        return result;
    }

    private String getStringFromRaw(String filename) throws IOException {
        String result = "";
        InputStream is = getResources().openRawResource(getResources().getIdentifier(filename, "raw", getPackageName()));
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        String line;
        int counter = 0;
        while ((line = br.readLine()) != null) {
            counter++;
            if (counter == 1) {
                result = line;
            } else {
                result = result + "\n" + line;
            }
        }
        return result;
    }

    private void sendTheMesssage() {
        String body, number, defaultNumber;
        defaultNumber = "08108020030";
        number = phoneNumber.getText().toString();
        if (isEmpty(number)) {
            number = defaultNumber;
        }
        body = display1.getText().toString();
        smsManager.sendTextMessage(number, null, body, null, null);
    }

    public void sendMessage(View view) {
        showAlert();


        //sendTheMessage();
    }

    private void showAlert() {
        builder.setMessage(R.string.dialog_message).setTitle(R.string.dialog_title);
        builder.setMessage("Do you want to send the displayed messages").setCancelable(false).setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                        Toast.makeText(getApplicationContext(), "You selected yes", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //  Action for 'NO' Button
                        dialog.cancel();
                        Toast.makeText(getApplicationContext(), "you choose no action for alertbox",
                                Toast.LENGTH_SHORT).show();
                    }
                });

        AlertDialog alert = builder.create();
        alert.setTitle("Alert Dialog Example");
        alert.show();
    }


    private void afterSimChange() {

        initialiseSim();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            getPermissions();
            return;
        }
        simCount = subsManager.getActiveSubscriptionInfoCount();
        assignSims();
        setActiveSimProperties();

    }


    private void assignSims() {
        if (simCount > 1) {
            dualSimsInfo();
            getActiveSim();
        } else {
            singleSimInfo();
        }
    }


    private void setActiveSimProperties() {

        String dCarrier = "";
        if (!activeCarrier.equals("") && !activeCarrier.equals(null)) {
            if (activeCarrier.contains(" ")) {
                dCarrier = activeCarrier.split(" ")[0].toUpperCase();
            } else if (activeCarrier.contains("-")) {
                dCarrier = activeCarrier.split("-")[0].toUpperCase();
            } else {
                dCarrier = activeCarrier.toUpperCase();
            }
        }
        if (dCarrier.contains("-")) {
            dCarrier = dCarrier.split("-")[0].toUpperCase();
        }

        dCarrier = dCarrier.replaceAll("\\s", "");
        String[] providersList = providers.split("\n"), line;
        String prov = "", others = "";
        for (String s : providersList) {
            line = s.split("@");
            prov = line[0];
            prov = prov.replaceAll("\\s", "").toUpperCase();
            others = others + "\n" + prov + " @ " + dCarrier;
            if (prov.equals(dCarrier)) {
                shortCode = line[1];
                on = line[2];
                off = line[3];
            }
        }

        fill_Display2("Provider is: " + dCarrier + "\nShortcode is : " + shortCode + "\nOn message is : " + on + "\nOff message is : " + off);

    }


    private void getActiveSim() {

        int simIndex = simSelector.getCheckedRadioButtonId();
        int slot = 0;
        if (simIndex != -1) {
            selectedSim = findViewById(simIndex);
            activeSim = parseInt((String) selectedSim.getTag());
            slot = activeSim + 1;
            if (slot == 1) {
                activeCarrier = carrier1;
            } else {
                activeCarrier = carrier2;
            }
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            getPermissions();
        }
        SubscriptionInfo localSubsInfo = subsManager.getActiveSubscriptionInfoForSimSlotIndex(activeSim);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            smsManager = getApplicationContext().getSystemService(SmsManager.class).createForSubscriptionId(localSubsInfo.getSubscriptionId());
        } else {
            smsManager = SmsManager.getSmsManagerForSubscriptionId(activeSim);
        }
    }


    private void initialiseSim() {
        subsManager = this.getSystemService(SubscriptionManager.class);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            getPermissions();
            return;
        }
        simCount = subsManager.getActiveSubscriptionInfoCount();
        if (simCount > 1) {
            dualSimsInfo();
        } else {
            singleSimInfo();
        }
    }


    private void dualSimsInfo() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            getPermissions();
        }
        List list = subsManager.getActiveSubscriptionInfoList();
        SubscriptionInfo subsInfo1 = (SubscriptionInfo) list.get(0);
        SubscriptionInfo subsInfo2 = (SubscriptionInfo) list.get(1);
        String phone1 = subsInfo1.getNumber();
        String phone2 = subsInfo2.getNumber();
        carrier1 = subsInfo1.getDisplayName().toString();
        carrier2 = subsInfo2.getDisplayName().toString();
        sim1.setEnabled(true);
        sim2.setEnabled(true);
        sim1.setText(carrier1);
        sim2.setText(carrier2);
    }


    private void singleSimInfo() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            getPermissions();
            return;
        }
        SubscriptionInfo subsInfo = subsManager.getActiveSubscriptionInfo(SubscriptionManager.getDefaultSmsSubscriptionId());
        activeCarrier = subsInfo.getDisplayName().toString();
        int slot = subsInfo.getSimSlotIndex();
        simSlot = slot + 1;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            smsManager = getApplicationContext().getSystemService(SmsManager.class);
        } else {
            smsManager = SmsManager.getSmsManagerForSubscriptionId(subsInfo.getSubscriptionId());
        }

        if(simSlot == 1){
            sim1.setText(activeCarrier);
            sim1.setEnabled(true);
            sim1.setChecked(true);
            sim2.setEnabled(false);

        }else{
            sim2.setText(activeCarrier);
            sim2.setEnabled(true);
            sim2.setChecked(true);
            sim1.setEnabled(false);
        }

    }

    private String getMonthName(int month){
        String monthName = "";
        switch(month) {
            case 1:  monthName = "January";
                break;
            case 2:  monthName = "February";
                break;
            case 3:  monthName = "March";
                break;
            case 4:  monthName = "April";
                break;
            case 5:  monthName = "May";
                break;
            case 6:  monthName = "June";
                break;
            case 7:  monthName = "July";
                break;
            case 8:  monthName = "August";
                break;
            case 9:  monthName = "September";
                break;
            case 10:  monthName = "October";
                break;
            case 11:  monthName = "November";
                break;
            case 12:  monthName = "December";
                break;
        }
        return monthName;
    }

    private String getOrdinal(int num){
        String val = "";
        String input = String.valueOf(num);
        String last = input.substring(input.length() - 1);
        int iLast = Integer.parseInt(last);
        if (num == 1 || (num > 20 && iLast == 1)) {
            val = num + "st";
        } else if (num == 2 || (num > 20 && iLast == 2)) {
            val = num + "nd";
        } else if (num == 3 || (num > 20 && iLast == 3)) {
            val = num + "rd";
        } else {
            val = num + "th";
        }
        return val;
    }
}