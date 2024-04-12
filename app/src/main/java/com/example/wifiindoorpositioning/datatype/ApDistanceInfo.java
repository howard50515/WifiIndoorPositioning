package com.example.wifiindoorpositioning.datatype;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import com.example.wifiindoorpositioning.manager.ApDataManager;

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

    public static Comparator<ApDistanceInfo> distanceComparator = (left, right) -> Float.compare(left.distance, right.distance);

    @SuppressLint("DefaultLocale")
    @NonNull
    @Override
    public String toString() {
        return String.format("%s %s %s (%.2f, %.2f), %.2f", apValueName, highlightFunctionName, weightFunctionName, predictCoordinate.x, predictCoordinate.y, distance);
    }
}
