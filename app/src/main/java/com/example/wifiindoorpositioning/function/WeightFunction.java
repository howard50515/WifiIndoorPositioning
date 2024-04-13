package com.example.wifiindoorpositioning.function;


import com.example.wifiindoorpositioning.datatype.DistanceInfo;

import java.util.ArrayList;

public interface WeightFunction{
    ArrayList<Float> weight(ArrayList<DistanceInfo> highlights);
}
