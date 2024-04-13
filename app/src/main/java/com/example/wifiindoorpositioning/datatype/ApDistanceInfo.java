package com.example.wifiindoorpositioning.datatype;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import com.example.wifiindoorpositioning.manager.ApDataManager;

import java.util.Comparator;

public class ApDistanceInfo {
    public final String[] names = new String[3];

    public final transient String apValueName;
    public final transient String highlightFunctionName;
    public final transient String weightFunctionName;
    public transient float x, y;
    public float distance;

    public ApDistanceInfo(String apValueName, String highlightFunctionName, String weightFunctionName, ApDataManager.Coordinate predictCoordinate){
        this.apValueName = apValueName;
        this.highlightFunctionName = highlightFunctionName;
        this.weightFunctionName = weightFunctionName;
        this.x = predictCoordinate.x;
        this.y = predictCoordinate.y;

        names[0] = apValueName;
        names[1] = highlightFunctionName;
        names[2] = weightFunctionName;
    }

    public static Comparator<ApDistanceInfo> distanceComparator = (lhs, rhs) -> Float.compare(lhs.distance, rhs.distance);
    public static Comparator<ApDistanceInfo> nameComparator = (lhs, rhs) ->{
        int c1 = lhs.apValueName.compareTo(rhs.apValueName);

        if (c1 != 0) return c1;

        int c2 = lhs.highlightFunctionName.compareTo(rhs.highlightFunctionName);

        if (c2 != 0) return c2;

        return lhs.weightFunctionName.compareTo(rhs.weightFunctionName);
    };

    @SuppressLint("DefaultLocale")
    @NonNull
    @Override
    public String toString() {
        return String.format("%s %s %s (%.2f, %.2f), %.2f", apValueName, highlightFunctionName, weightFunctionName, x, y, distance);
    }
}
