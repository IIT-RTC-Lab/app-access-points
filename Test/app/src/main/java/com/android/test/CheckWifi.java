package com.android.test;

import android.app.Activity;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.widget.TextView;

import java.util.List;

/**
 * Created by javier on 4/03/15.
 */
public class CheckWifi extends Activity {

    TextView wifiState;
    WifiManager wifimg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wifi_activity);

        //we will write the WiFi information here
        String results = "\r\n";

        //TextView to display all the information
        wifiState = (TextView)findViewById(R.id.wifi_state);

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

    }

    /*
     * Checks if WiFi is enabled
     * If it's not enables WiFi
     * Returns a message showing the results
     */
    public String enableWifi() {
        String result;

        if(wifimg.getWifiState() == WifiManager.WIFI_STATE_ENABLED){
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

        //foreach loop to get all the hotspots
        for(ScanResult sr: wifiScanList) {
            if(sr.level >= acceptedLevel){   //if(sr.level >= minLevel) {
                probeResult += "\r\n" + sr.toString() + "\r\n";
            }
        }
        return probeResult;
    }
}
