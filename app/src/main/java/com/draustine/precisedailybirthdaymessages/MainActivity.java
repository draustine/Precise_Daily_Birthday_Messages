package com.draustine.precisedailybirthdaymessages;

import static android.text.TextUtils.isEmpty;
import static java.lang.Integer.parseInt;

import android.Manifest;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private SwipeRefreshLayout swipeRefreshLayout;
    private String[] PERMISSIONS;
    private SubscriptionManager subsManager;
    private SmsManager smsManager;
    private int simCount;
    private int activeSim;
    private TextView display1, display2;
    private EditText phoneNumber;
    private RadioGroup simSelector;
    private RadioButton sim1;
    private RadioButton sim2;
    private String carrier, carrier1, carrier2, activeCarrier, providers, shortCode, on, off;
    private String phone, phone1, phone2;
    private AlertDialog.Builder builder;
    private String messageTemplate = "", belatedTemplate = "", clientsList = "", celebrantsList = "", messages = "";
    private SwipeRefreshLayout parent;
    private Button send_button, preview_button;
    private List<String> messageList = new ArrayList<>();
    private final LocalDate localDate = LocalDate.now();
    private LocalDate anniversaryDate = null;
    private static final String filename = "Upcoming_Birthdays.txt";
    private static final String messagesFilename = "message_template";
    private static final String belatedTFileName = "belated";
    private TextView dateView, celebsCount, countOfSms, costOfSms;
    private MaterialAlertDialogBuilder materialAlertDialogBuilder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        phoneNumber = findViewById(R.id.phoneNumber);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        display1 = findViewById(R.id.display);
        display2 = findViewById(R.id.display2);
        display1.setMovementMethod(new ScrollingMovementMethod());
        display2.setMovementMethod(new ScrollingMovementMethod());
        EditText message = findViewById(R.id.message);
        sim1 = findViewById(R.id.sim1);
        sim2 = findViewById(R.id.sim2);
        simSelector = findViewById(R.id.simSelector);
        phoneNumber = findViewById(R.id.phoneNumber);
        builder = new AlertDialog.Builder(this);
        materialAlertDialogBuilder = new MaterialAlertDialogBuilder(this, 1);
        dateView = findViewById(R.id.inputDate);
        celebsCount = findViewById(R.id.celebrantsCount);
        countOfSms = findViewById(R.id.smsCount);
        costOfSms = findViewById(R.id.smsCost);
        PERMISSIONS = new String[]{
                Manifest.permission.INTERNET,
                Manifest.permission.SEND_SMS,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_PHONE_NUMBERS,
                Manifest.permission.READ_SMS
        };
        carrier = carrier1 = carrier2 = activeCarrier = providers = shortCode = on = off = "";

        swipeRefreshLayout.setOnRefreshListener(() -> {
            afterSimChange();
            anniversaryDate = null;
            onDateChange();
            fill_Display1("");
            dateView.setText("Click to Select/Type date");
            swipeRefreshLayout.setRefreshing(false);
        });
        dateView.setOnClickListener(v -> showDatePicker());
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

    private void prepareMessageList(){
        String list[] = clientsList.split("\n");
        int cDay, cMonth, cYear, day, month, year;
        String name, phone, anniversary = "";
        if (anniversaryDate == null){
            cDay = localDate.getDayOfMonth();
            cMonth = localDate.getMonthValue();
            cYear = localDate.getYear();
        } else {
            cDay = anniversaryDate.getDayOfMonth();
            cMonth = anniversaryDate.getMonthValue();
            cYear = anniversaryDate.getYear();
        }
        messages = "Clients with birthday anniversary on the " + getOrdinal(cDay) + " of " +
                getMonthName(cMonth) + " " + cYear + "\n";
        int counter = 0, mCounter = 0, smsCounter = 0, smsMax = 160, smsConc = 153;
        String dDate;
        for(String line: list){
            counter++;
            if(counter > 1) {
                String[] thisLine = line.split("@");
                day = parseInt(thisLine[2]);
                month = parseInt(thisLine[3]);
                if(day == cDay && month == cMonth){
                    mCounter++;
                    if(mCounter == 1){celebrantsList = line;} else {celebrantsList = celebrantsList + "\n" + line;}
                    name = thisLine[0];
                    phone = thisLine[1];
                    year = Integer.parseInt(thisLine[4]);
                    anniversary = getOrdinal(cYear - year);
                    String message = messageTemplate.replace(" name,", " " + name + ",");
                    message = message.replace(" ord ", " " + anniversary + " ");
                    if(!(anniversaryDate == null) && localDate.isAfter(anniversaryDate)){
                        int d = (int) ChronoUnit.DAYS.between(anniversaryDate, localDate);
                        if(d == 1){
                            dDate = " yesterday ";
                        }else{
                            dDate = " " + d + " days ago ";
                        }
                        message = message.replace(" @day ", dDate);
                    }
                    int length= message.length();
                    if(length <= smsMax){
                        smsCounter++;
                    } else if (length % smsConc > 0 ) {
                        smsCounter = smsCounter + (length / smsConc) + 1;
                    } else {
                        smsCounter = smsCounter + (length / smsConc);
                    }
                    messages = messages + "\n\nMessage " + mCounter + "\n" + message;
                    messageList.add(phone + "@" + message);
                }
            }
        }
        fill_Display1(celebrantsList);
        fill_Display2(messages);
        String comment;
        if(mCounter == 0){
            comment = "None";
        } else if (mCounter > 1){
            comment = mCounter + " celebrants.";
        } else {
            comment = mCounter + " celebrant.";
        }
        celebsCount.setText(comment);
        if(smsCounter == 0){
            comment = "None";
        } else if (smsCounter > 1){
            comment = smsCounter + " messages.";
        } else {
            comment = smsCounter + " message.";
        }
        countOfSms.setText(comment);
        int cost = 4 * smsCounter;
        comment = "N" + cost;
        costOfSms.setText(comment);
    }

    private void prepareMessages(){
        String fileName = "";
        if(!(anniversaryDate == null) && localDate.isAfter(anniversaryDate)){
            fileName = belatedTFileName;
        } else if (!(anniversaryDate == null) && localDate.isEqual(anniversaryDate)){
            fileName = messagesFilename;
        } else {
            fileName = messagesFilename;
        }
        if(messageTemplate.equals("")){
            try {
                messageTemplate = getStringFromRaw(fileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(clientsList ==""){
            try {
                getClientsList();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        prepareMessageList();
    }

    private void onDateChange(){
        messageList.clear();
        messages = "";
        celebsCount.setText("");
        costOfSms.setText("");
        countOfSms.setText("");
        fill_Display2("");
        String fileName = "";
        if(!(anniversaryDate == null) && localDate.isAfter(anniversaryDate)){
            fileName = belatedTFileName;
        } else if (!(anniversaryDate == null) && localDate.isEqual(anniversaryDate)){
            fileName = messagesFilename;
        } else {
            fileName = messagesFilename;
        }
        try {
            messageTemplate = getStringFromRaw(fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startUp() {
        afterSimChange();
        setActiveSimProperties();
        try {
            getClientsList();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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

    private void sendTheMessage() {
        String body, number, defaultNumber;
        number = phoneNumber.getText().toString();
        String comment = "";
        if (!(isEmpty(number))) {
            body = display1.getText().toString();
            smsManager.sendTextMessage(number, null, body, null, null);
            comment =  "Message sent " + number + " via " + activeCarrier + " network";
        } else {
            int counter = 0;
            for(String line: messageList){
                counter++;
                String[] thisLine = line.split("@");
                body = thisLine[1];
                number = thisLine[0];
                smsManager.sendTextMessage(number, null, body, null, null);
                if (counter == 1 && !(shortCode.equals(""))){
                    smsManager.sendTextMessage(shortCode, null, off, null, null);
                } else if (counter == messageList.size() && !(shortCode.equals(""))){
                    smsManager.sendTextMessage(shortCode, null, on, null, null);
                }
            }
            comment = counter + " messages sent via " + activeCarrier + " network";
        }
        Toast.makeText(this, comment, Toast.LENGTH_LONG).show();
    }

    public void sendMessage(View view) {
        if(messageList.size() == 0){
            prepareMessages();
        }
        showMaterialAlert();
    }


    private void showMaterialAlert(){
        materialAlertDialogBuilder.setMessage("Do you want to send the displayed messages").setTitle(R.string.dialog_title);
        materialAlertDialogBuilder.setCancelable(false)
                .setPositiveButton("Yes", (dialog, which) -> {
                    sendTheMessage();
        })
                .setNegativeButton("No", (dialog, which) -> {
                    Toast.makeText(this, "Message sending aborted", Toast.LENGTH_LONG).show();
                    dialog.cancel();
                });
        AlertDialog alert = materialAlertDialogBuilder.create();
        alert.setTitle("Confirm to send messages");
        alert.show();
    }


    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager != null ? connectivityManager.getActiveNetworkInfo() : null;
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void getClientsList() throws IOException, InterruptedException{
        if(isNetworkAvailable()) {
            celebrantsDownloader();
            safeLocalList(filename, clientsList);
        }else{
            clientsList = listFromInternal(filename);
            //clientsList = getStringFromRaw(filename);
        }
        if (clientsList != "") {
            String tempStr = "No Internet connection\n List from File is\n" + clientsList;
        } else {
            fill_Display1("No clients with birthday today");
        }
    }


    private void safeLocalList(String fileName, String content){
        try {
            FileOutputStream fOut = openFileOutput(fileName,Context.MODE_PRIVATE);
            fOut.write(content.getBytes());
            fOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String listFromInternal(String fileName){
        String str = "";
        try {
            FileInputStream fileInputStream = openFileInput(fileName);
            InputStreamReader   inputStreamReader = new InputStreamReader(fileInputStream);
            StringWriter sw = new StringWriter();
            int DEFAULT_BUFFER_SIZE = 1024 * 4;
            char[] buffer = new char[DEFAULT_BUFFER_SIZE];
            int n = 0;
            while (-1 != (n = inputStreamReader.read(buffer))) {
                sw.write(buffer, 0, n);
            }
            inputStreamReader.close();
            str = sw.toString();
        } catch (IOException e) {
            e.printStackTrace();
            fill_Display2("There is no internet connection and no Local copy of file.\nEnsure you have data connection and try again");
//            System.exit(1);
        }
        return str;
    }

    private void celebrantsDownloader() throws InterruptedException, MalformedURLException {
        URL list = new URL(getString(R.string.file_url));
        Thread theList = new Thread(()->{
            try {
                listDownloader(list);
            } catch (IOException e) {
                e.printStackTrace();
                fill_Display1("File not found");
            }
        });
        theList.start();
        theList.join();
    }

    private void listDownloader(URL url) throws IOException{
        int counter = 0;
        String tempStr = "", line = "";
//        File path = getApplicationContext().getFilesDir();
        try{
            InputStream inp = url.openStream();
            InputStreamReader reader = new InputStreamReader(inp);
            BufferedReader br = new BufferedReader(reader);
            while((line = br.readLine()) != null) {
                counter++;
                if (counter == 1) {
                    tempStr = line;
                } else {
                    tempStr = tempStr + "\n" + line;
                }
            }
            clientsList = tempStr;
//            File outputFile = new File(path, filename);
//            FileOutputStream writer = new FileOutputStream(outputFile);
//            writer.write(tempStr.getBytes());
//            writer.close();
            br.close();
            reader.close();
            inp.close();
        } catch (IOException e) {
            e.printStackTrace();
            fill_Display1("Online file not found");
        }
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
            RadioButton selectedSim = findViewById(simIndex);
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
        List<SubscriptionInfo> list = subsManager.getActiveSubscriptionInfoList();
        SubscriptionInfo subsInfo1 = list.get(0);
        SubscriptionInfo subsInfo2 = list.get(1);
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
        int simSlot = slot + 1;
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

    public void previewMessages(View view) {
        prepareMessages();
    }

    private void showDatePicker() {
        MaterialDatePicker datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Date").build();
        datePicker.addOnPositiveButtonClickListener(selection -> {
            String result = datePicker.getHeaderText();
            DateTimeFormatter format = DateTimeFormatter.ofPattern("d MMM yyyy");
            dateView.setText("Selected date is\n" + result);
            anniversaryDate = LocalDate.parse(result, format);
            onDateChange();
        });
        datePicker.show(getSupportFragmentManager(), "TAG");
    }
}