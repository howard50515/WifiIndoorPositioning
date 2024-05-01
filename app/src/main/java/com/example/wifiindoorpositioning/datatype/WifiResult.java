package com.example.wifiindoorpositioning.datatype;

import android.net.wifi.ScanResult;

public class WifiResult {
    public String SSID;
    public String BSSID;
    public int level;

    public transient String apId;

    public transient float rpLevel;

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

    public void applyApId(){
        this.apId = SSID + ":" + BSSID;
    }
}
