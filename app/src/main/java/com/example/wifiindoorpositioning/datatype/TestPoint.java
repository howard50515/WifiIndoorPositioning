package com.example.wifiindoorpositioning.datatype;

import com.example.wifiindoorpositioning.manager.ApDataManager;

public class TestPoint {
    public String name;
    public float coordinateX, coordinateY;

    public TestPoint(String name, ApDataManager.Coordinate coordinate){
        this(name, coordinate.x, coordinate.y);
    }

    public TestPoint(String name, float x, float y){
        this.name = name;
        this.coordinateX = x;
        this.coordinateY = y;
    }

    public void set(float x, float y){
        coordinateX = x;
        coordinateY = y;
    }
}
