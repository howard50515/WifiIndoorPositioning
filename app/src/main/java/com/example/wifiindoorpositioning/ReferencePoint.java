package com.example.wifiindoorpositioning;

import java.util.ArrayList;

public class ReferencePoint {
    public String name;
    public float coordinateX, coordinateY;
    public ArrayList<Float> vector;

    public ReferencePoint(String name, ApDataManager.Coordinate coordinate){
        this.name = name;
        this.coordinateX = coordinate.x;
        this.coordinateY = coordinate.y;
    }
}
