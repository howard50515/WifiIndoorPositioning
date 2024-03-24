package com.example.wifiindoorpositioning;

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
}
