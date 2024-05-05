package com.example.wifiindoorpositioning.function;

import com.example.wifiindoorpositioning.datatype.DistanceInfo;

import java.util.ArrayList;

public class FirstKDistanceHighlightFunction implements HighlightFunction {
    private int k;
    private final boolean usingK;

    public FirstKDistanceHighlightFunction(){
        this.usingK = false;
    }

    public FirstKDistanceHighlightFunction(int k){
        this.k = k;
        this.usingK = true;
    }

    @Override
    public ArrayList<DistanceInfo> highlight(ArrayList<DistanceInfo> distances, int k) {
        ArrayList<DistanceInfo> copy = new ArrayList<>(distances);

        copy.sort(DistanceInfo.distanceComparable);

        return new ArrayList<>(copy.subList(0, usingK ? Math.min(distances.size(), this.k) : k));
    }
}
