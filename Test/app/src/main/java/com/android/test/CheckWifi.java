package com.android.test;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.opencsv.CSVWriter;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;


/**
 * Created by javier on 4/03/15.
 */
public class CheckWifi extends Activity {

    static final int ACCEPTABLE_LEVEL = -70;

    TextView wifiState;
    Button sendButton;
    Button sendURLButton;
    WifiManager wifimg;
    List<ScanResult> scanResults;
    private String data;

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
        sendURLButton = (Button) findViewById(R.id.send_button_url);
        sendURLButton.setOnClickListener(new SendURLButtonListener());
        sendURLButton.setEnabled(false);

        wifimg = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        //STEP Nº1: check WiFi State and enable WiFi if it's not already enabled
        results += enableWifi();

        //Step Nº2: Getting the device's MAC address
        WifiInfo info = wifimg.getConnectionInfo();
        String macAddress = info.getMacAddress();
        results += "\r\nMAC Address: " + macAddress + "\r\n";

        //Step Nº3: Sending a WiFi probe
        results += "\r\n" + wifiProbe() + "\r\n";

        wifiState.setText(results);
        if (results != null) {
            sendButton.setEnabled(true);
            sendURLButton.setEnabled(true);
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
    public String wifiProbe() {

        wifimg.startScan();

        WifiScanReceiver wifiReceiver = new WifiScanReceiver();
        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        List<ScanResult> wifiScanList = wifimg.getScanResults();
        String probeResult = "";

        scanResults = wifiScanList;

        //foreach loop to get all the hotspots
        for (ScanResult sr : wifiScanList) {
            if (sr.level < ACCEPTABLE_LEVEL) continue;
            probeResult += "\r\n" + sr.toString() + "\r\n";
        }
        return probeResult;
    }

    public void exportEmailInCSV(final List<ScanResult> scanResults) throws IOException {
        File file = null;
        File root = Environment.getExternalStorageDirectory();
        if (root.canWrite()) {
            File dir = new File(root.getAbsolutePath() + "/PersonData");
            dir.mkdirs();
            file = new File(dir, "Data.csv");
            CSVWriter writer = null;
            try {
                writer = new CSVWriter(new FileWriter(file), ',');
                for (ScanResult sr : scanResults) {
                    if (sr.level >= ACCEPTABLE_LEVEL) {
                        String[] entries = sr.toString().split(","); // array of your values
                        writer.writeNext(entries);
                    }
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

    private String formatJSON() {
        try {
            JSONObject parent = new JSONObject();
            JSONArray jsonArray = new JSONArray();
            for (ScanResult sr : scanResults) {
                if (sr.level < ACCEPTABLE_LEVEL) continue;
                JSONObject jsonObject = new JSONObject();
                String ssid = "Unknown";
                if (!sr.SSID.isEmpty()) {
                    ssid = sr.SSID;
                }
                jsonObject.put("SSID", ssid);
                jsonObject.put("BSSID", sr.BSSID);
                jsonObject.put("capabilities", sr.capabilities);
                jsonObject.put("level", String.valueOf(sr.level));
                jsonObject.put("frequency", String.valueOf(sr.frequency));
                jsonObject.put("timestamp", String.valueOf(sr.timestamp));
                jsonArray.put(jsonObject);
            }

            parent.put("aps", jsonArray);
            Log.d("output", parent.toString(2));
            return parent.toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    private class SendButtonListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            try {
                exportEmailInCSV(scanResults);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class SendURLButtonListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            data = formatJSON();
            PostDataTask task = new PostDataTask();
            task.execute();
        }
    }


    private class PostDataTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... parames) {
            HttpClient httpClient = new DefaultHttpClient();
            HttpResponse response = null;

            try {
                String uri = "http://64.131.109.56/?"
                        + "json=" + URLEncoder.encode(data, "UTF-8");
                Location location = getLastKnownLocation();
//                uri += "&lat=" + URLEncoder.encode(String.valueOf(location.getLatitude()), "UTF-8")
//                        + "&long=" + URLEncoder.encode(String.valueOf(location.getLongitude()), "UTF-8");
                HttpPost httppost = new HttpPost(uri);
                response = httpClient.execute(httppost);

            } catch (ClientProtocolException e) {
            } catch (IOException e) {
            }
            if (response != null) {
                return Integer.toString(response.getStatusLine().getStatusCode());
            } else {
                return "200";
            }
        }

        protected void onPostExecute(String responseCode) {
            onSaveFinished(responseCode);
        }
    }

    private Location getLastKnownLocation() {
        LocationManager mLocationManager;
        mLocationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
        List<String> providers = mLocationManager.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {
            Location l = mLocationManager.getLastKnownLocation(provider);
            if (l == null) {
                continue;
            }
            if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                // Found best last known location: %s", l);
                bestLocation = l;
            }
        }
        return bestLocation;
    }

    private void onSaveFinished(String code) {
        if (!code.equals("200")) {
            Toast.makeText(getApplicationContext(), "error saving data", Toast.LENGTH_LONG).show();
            sendURLButton.setEnabled(true);
        }
    }
}
