package com.example.wifiindoorpositioning;

import java.util.Comparator;

public class ApDistanceInfo {
    public String apValueName;
    public String highlightFunctionName;
    public String weightFunctionName;
    public ApDataManager.Coordinate predictCoordinate;
    public float distance;

    public ApDistanceInfo(String apValueName, String highlightFunctionName, String weightFunctionName, ApDataManager.Coordinate predictCoordinate){
        this.apValueName = apValueName;
        this.highlightFunctionName = highlightFunctionName;
        this.weightFunctionName = weightFunctionName;
        this.predictCoordinate = predictCoordinate;
    }

    public static Comparator<ApDistanceInfo> distanceComparator = (left, right) -> {
        if (Math.abs(left.distance - right.distance) < 0.0001) return 0;

        return (int) (left.distance - right.distance);
    };
}
