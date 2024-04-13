package com.example.wifiindoorpositioning.function;


import com.example.wifiindoorpositioning.datatype.DistanceInfo;

import java.util.ArrayList;

public interface HighlightFunction{
    ArrayList<DistanceInfo> highlight(ArrayList<DistanceInfo> distances, int k);
}
