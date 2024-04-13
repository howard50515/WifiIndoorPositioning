package com.example.wifiindoorpositioning.function;

import com.example.wifiindoorpositioning.datatype.DistanceInfo;

import java.util.ArrayList;

public interface DisplayFunction{
    ArrayList<DistanceInfo> display(ArrayList<DistanceInfo> distances);
}
