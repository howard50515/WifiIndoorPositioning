package com.example.wifiindoorpositioning;

public class DistanceInfo {
    public String samplePoint;
    public float distance;
    public float coordinateX, coordinateY;
    public int pastFoundNum, notFoundNum;

    public DistanceInfo(String samplePoint, float distance, float coordinateX, float coordinateY){
        this(samplePoint, distance, coordinateX, coordinateY, 0, 0);
    }

    public DistanceInfo(String samplePoint, float distance, float coordinateX, float coordinateY, int pastFoundNum, int notFoundNum){
        this.samplePoint = samplePoint;
        this.distance = distance;
        this.coordinateX = coordinateX;
        this.coordinateY = coordinateY;
        this.pastFoundNum = pastFoundNum;
        this.notFoundNum = notFoundNum;
    }
}
