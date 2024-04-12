package com.example.wifiindoorpositioning.datatype;

import android.net.wifi.ScanResult;

public class WifiResult {
    public String SSID;
    public String BSSID;
    public int level;

    public String apId;

    public WifiResult(ScanResult result){
        BSSID = result.BSSID;
        SSID = result.SSID;
        level = result.level;
        apId = SSID + ":" + BSSID;
    }

    public WifiResult(String apId, int level){
        this.BSSID = "";
        this.SSID = "";
        this.level = level;
        this.apId = apId;
    }
}
