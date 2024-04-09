package com.example.wifiindoorpositioning.function;

import com.example.wifiindoorpositioning.ApDataManager;
import com.example.wifiindoorpositioning.DistanceInfo;

import java.util.ArrayList;

public class DistanceRateHighlightFunction implements ApDataManager.HighlightFunction {
    private Object[] obj = new Object[1];

    public DistanceRateHighlightFunction(float rate){
        obj[0] = rate;
    }

    public DistanceRateHighlightFunction(Object[] objRef){
        obj = objRef;
    }

    @Override
    public ArrayList<DistanceInfo> highlight(ArrayList<DistanceInfo> distances, int k) {
        ArrayList<DistanceInfo> highlight = new ArrayList<>(distances);

        highlight.sort(DistanceInfo.distanceComparable);

        if (highlight.size() <= 1) return highlight;

        float dis = highlight.get(0).distance;
        int maxK = Math.min(highlight.size(), 5);
        int i = 1;

        float rate = (float) obj[0];
        while (i < maxK){
            if ((highlight.get(i).distance / dis) - 1 > rate){
                break;
            }

            i++;
        }

        int dynamicK = i;

        // 回傳想要highlight的點
        return new ArrayList<>(highlight.subList(0, dynamicK));
    }
}
