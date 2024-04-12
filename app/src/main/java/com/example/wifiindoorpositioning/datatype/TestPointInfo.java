package com.example.wifiindoorpositioning.datatype;

import java.util.ArrayList;

public class TestPointInfo {
    // 測試點資訊
    public TestPoint testPoint;

    // 不同 ap, highlightFunction, weightFunction 算出的結果
    public ArrayList<ApDistanceInfo> values;

    // 原始 WiFi 數據
    public ArrayList<WifiResult> results;

    public TestPointInfo(TestPoint testPoint, ArrayList<ApDistanceInfo> values, ArrayList<WifiResult> results){
        this.testPoint = testPoint;
        this.values = values;
        this.results = results;
    }
}
