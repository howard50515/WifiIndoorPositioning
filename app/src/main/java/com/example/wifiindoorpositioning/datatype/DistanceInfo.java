package com.example.wifiindoorpositioning.datatype;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

public class DistanceInfo {
    // 參考點名字
    public String rpName;

    // 參考點與目前點距離
    public float distance;

    // 參考點的 x y 座標
    public float coordinateX, coordinateY;

    // 參考點 loss rate = notFoundName / pastFoundNum
    public int pastFoundNum, notFoundNum;
    public int pastNotFoundNum, foundNum;
    public ArrayList<Overlap> newSameOverlapPercent = new ArrayList<>();

    public float weight;

    public DistanceInfo(String rpName, float distance, float coordinateX, float coordinateY){
        this(rpName, distance, coordinateX, coordinateY, 0, 0, 0, 0);
    }

    public DistanceInfo(String rpName, float distance, float coordinateX, float coordinateY, int pastFoundNum, int notFoundNum, int pastNotFoundNum, int foundNum){
        this.rpName = rpName;
        this.distance = distance;
        this.coordinateX = coordinateX;
        this.coordinateY = coordinateY;
        this.pastFoundNum = pastFoundNum;
        this.notFoundNum = notFoundNum;
        this.pastNotFoundNum = pastNotFoundNum;
        this.foundNum = foundNum;
    }

    public static Comparator<DistanceInfo> distanceComparable = (distanceInfo, t1) -> Float.compare(distanceInfo.distance, t1.distance);

    public static class Overlap{
        public String rpName;
        public int count;

        public Overlap(String rpName, int count){
            this.rpName = rpName;
            this.count = count;
        }

        @NonNull
        @Override
        public String toString() {
            return rpName + ": " + count;
        }
    }
}
