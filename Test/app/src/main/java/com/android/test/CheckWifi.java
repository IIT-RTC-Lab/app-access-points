package com.android.test;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Created by javier on 4/03/15.
 */
public class CheckWifi extends Activity {

    TextView wifiState;
    Button sendButton;
    WifiManager wifimg;
    List<ScanResult> scanResults;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wifi_activity);

        //we will write the WiFi information here
        String results = "\r\n";

        //TextView to display all the information
        wifiState = (TextView) findViewById(R.id.wifi_state);
        sendButton = (Button) findViewById(R.id.send_button);
        sendButton.setOnClickListener(new SendButtonListener());
        sendButton.setEnabled(false);

        wifimg = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        //STEP Nº1: check WiFi State and enable WiFi if it's not already enabled
        results += enableWifi();

        //Step Nº2: Getting the device's MAC address
        WifiInfo info = wifimg.getConnectionInfo();
        String macAddress = info.getMacAddress();
        results += "\r\nMAC Address: " + macAddress + "\r\n";

        //Step Nº3: Sending a WiFi probe
        results += "\r\n" + wifiProbe(15) + "\r\n";

        wifiState.setText(results);
        if(results != null) {
            sendButton.setEnabled(true);
        }
    }

    /*
     * Checks if WiFi is enabled
     * If it's not enables WiFi
     * Returns a message showing the results
     */
    public String enableWifi() {
        String result;

        if (wifimg.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
            result = "Wifi is enabled\r\n";
        } else {
            result = "Wifi is NOT enabled, enabling WiFi...\r\n";
            wifimg.setWifiEnabled(true);
        }

        return result;
    }

    /*
     * Makes a WiFi probe
     *
     * returns a string showing the available hotspots
     *
     * tolerance = max difference in the level of signal that we consider as valid
     */
    public String wifiProbe(int tolerance) {

        wifimg.startScan();

        WifiScanReceiver wifiReceiver = new WifiScanReceiver();
        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        List<ScanResult> wifiScanList = wifimg.getScanResults();
        String probeResult = "";

        //max level we can get from any hotspot
        int maxLevel = wifiScanList.get(0).level;

        //minimum signal level to consider a hotspot as valid
        int acceptedLevel = maxLevel - tolerance;

        scanResults = wifiScanList;

        //foreach loop to get all the hotspots
        for (ScanResult sr : wifiScanList) {
            if (sr.level >= acceptedLevel) {   //if(sr.level >= minLevel) {
                probeResult += "\r\n" + sr.toString() + "\r\n";
            }
        }
        return probeResult;
    }

    public void exportEmailInCSV(final List<ScanResult> scanResults) throws IOException {
        File file = null;
        File root = Environment.getExternalStorageDirectory();
        if (root.canWrite()) {
            File dir = new File(root.getAbsolutePath() + "/PersonData");
            dir.mkdirs();
            file   =   new File(dir, "Data.csv");
            CSVWriter writer = null;
            try {
                writer = new CSVWriter(new FileWriter(file), ',');
                for (ScanResult sc : scanResults) {
                    String[] entries = sc.toString().split(","); // array of your values
                    writer.writeNext(entries);
                }
                writer.close();
            } catch (IOException e) {
                //error
            }
        }
        Uri u1 = null;
        u1 = Uri.fromFile(file);

        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, "CSV file from AP probe App");
        sendIntent.putExtra(Intent.EXTRA_STREAM, u1);
        sendIntent.setType("text/html");
        startActivity(sendIntent);
    }

    private class SendButtonListener implements View.OnClickListener{

        @Override
        public void onClick(View v) {
            try {
                exportEmailInCSV(scanResults);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
