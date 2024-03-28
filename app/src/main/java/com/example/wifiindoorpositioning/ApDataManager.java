package com.example.wifiindoorpositioning;

import android.app.Activity;
import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;

public class ApDataManager {
    private static ApDataManager instance;

    public static void createInstance(Activity context){
        if (instance != null)
            return;

        instance = new ApDataManager(context);
    }

    public static ApDataManager getInstance(){
        return instance;
    }

    public ArrayList<String> accessPoints;
    public ArrayList<SamplePoint> fingerprint;
    public ArrayList<WifiResult> results;
    public ArrayList<DistanceInfo> originalDistances;
    public ArrayList<DistanceInfo> displayDistances;
    public ArrayList<DistanceInfo> highlightDistances;

    private ApDataManager(Context context) {

        BufferedReader apReader = new BufferedReader(new InputStreamReader(context.getResources().openRawResource(R.raw.ap)));
        StringBuilder apBuilder = new StringBuilder();

        try {
            String line;
            while ((line = apReader.readLine()) != null)
                apBuilder.append(line).append('\n');
        } catch (IOException e) {
            e.printStackTrace();
        }

        BufferedReader apVectorReader = new BufferedReader(new InputStreamReader(context.getResources().openRawResource(R.raw.ap_vector)));
        StringBuilder apVectorBuilder = new StringBuilder();

        try {
            String line;
            while ((line = apVectorReader.readLine()) != null)
                apVectorBuilder.append(line).append('\n');
        } catch (IOException e) {
            e.printStackTrace();
        }

        BufferedReader apCoordinateReader = new BufferedReader(new InputStreamReader(context.getResources().openRawResource(R.raw.ap_coordinate)));
        StringBuilder apCoordinateBuilder = new StringBuilder();

        try {
            String line;
            while ((line = apCoordinateReader.readLine()) != null)
                apCoordinateBuilder.append(line).append('\n');
        } catch (IOException e) {
            e.printStackTrace();
        }

        accessPoints = new Gson().fromJson(apBuilder.toString(), new TypeToken<ArrayList<String>>(){}.getType());

        fingerprint = new Gson().fromJson(apVectorBuilder.toString(), new TypeToken<ArrayList<SamplePoint>>(){}.getType());

        Dictionary<String, Coordinate> apCoordinate = new Gson().fromJson(apCoordinateBuilder.toString(), new TypeToken<Hashtable<String, Coordinate>>(){}.getType());

        for (int i = 0;i < fingerprint.size(); i++){
            SamplePoint sp = fingerprint.get(i);
            Coordinate c = apCoordinate.get(sp.samplePoint);
            if (c != null){
                sp.coordinateX = c.x;
                sp.coordinateY = c.y;
            }
            else{
                throw new RuntimeException("未提供" + sp.samplePoint + "座標");
            }
        }
    }

    public ArrayList<Float> getVector(ArrayList<WifiResult> results){
        ArrayList<Float> ssids = new ArrayList<>(accessPoints.size());
        for (int i = 0; i < accessPoints.size(); i++){
            float level = -100f;

            for (WifiResult result : results){
                if (accessPoints.get(i).equals(result.apId)){
                    level = result.level;
                    break;
                }
            }

            ssids.add(level);
        }

        return ssids;
    }

    public ArrayList<WifiResult> getSelectedApResults(ArrayList<WifiResult> results){
        ArrayList<WifiResult> output = new ArrayList<>();

        for (String apName : accessPoints){
            WifiResult r = null;

            for (WifiResult result : results){
                if (result.apId.equals(apName)){
                    r = result;
                    break;
                }
            }

            if (r == null)
                r = new WifiResult(apName, -100);
//test
            output.add(r);
        }

        return output;
    }

    public int k = 4;
    public HighlightAndDisplay defaultHighlightAndDisplay = new HighlightAndDisplay() {
        @Override
        public ArrayList<DistanceInfo> highlight(ArrayList<DistanceInfo> distances, int k) {
            ArrayList<DistanceInfo> copy = new ArrayList<>(distances);

            copy.sort(DistanceInfo.distanceComparable);

            return new ArrayList<>(copy.subList(0, k));
        }

        @Override
        public ArrayList<DistanceInfo> display(ArrayList<DistanceInfo> distances) {
            return getDistancesByDistance(distances);
        }
    };

    public void setResult(ArrayList<WifiResult> results){
        this.results = getSelectedApResults(results);

        ArrayList<Float> ssids = getVector(results);

        ArrayList<DistanceInfo> distances = new ArrayList<>();
        for (SamplePoint sp : fingerprint){
            float sum = 0;
            int num = 0;
            int notFoundNum = 0;

            for (int j = 0; j < sp.vector.size(); j++){
                if (sp.vector.get(j) != -100){
                    num++;

                    if (ssids.get(j) == -100){
                        notFoundNum++;
                    }
                }

                float diff = sp.vector.get(j) - ssids.get(j);

                sum += diff * diff;
            }

            distances.add(new DistanceInfo(sp.samplePoint, (float)Math.sqrt(sum), sp.coordinateX, sp.coordinateY, num, notFoundNum));
        }

        this.originalDistances = new ArrayList<>(distances);

        if (highlightAndDisplayFunction == null){
            this.displayDistances = defaultHighlightAndDisplay.display(distances);
            this.highlightDistances = defaultHighlightAndDisplay.highlight(distances, k);
        }
        else{
            this.displayDistances = highlightAndDisplayFunction.display(distances);
            this.highlightDistances = highlightAndDisplayFunction.highlight(distances, k);
        }

        invokeResultChangedListeners();
    }

    public Coordinate getPredictCoordinate(){
        // highlightDistances 為被選定的參考點
        // 利用 highlightDistances 計算權重位置
        // for (int i = 0; i < highlightDistances.size(); i++){
            // 可以到 DistanceInfo 看一下有什麼變數
            // DistanceInfo distance = highlightDistances.get(i); // 單一參考點資訊
            /* 計算距離倒數的總和、各個參考點的距離的權重等 */
        // }

        // 底下為KNN的範例
        Coordinate predict = new Coordinate();

        if (highlightDistances.size() == 0) return predict;

        // KNN每個人的比例相同 WKNN應該會根據距離改變權重
        float rate = 1f / highlightDistances.size();

        for (int i = 0; i < highlightDistances.size(); i++){
            predict.x += rate * highlightDistances.get(i).coordinateX;
            predict.y += rate * highlightDistances.get(i).coordinateY;
        }

        // 回傳預測的座標
        return predict;
    }

    public static ArrayList<DistanceInfo> getDistancesByDistance(ArrayList<DistanceInfo> distances){
        ArrayList<DistanceInfo> copy = new ArrayList<>(distances);

        copy.sort(DistanceInfo.distanceComparable);

        return copy;
    }

//    public static ArrayList<DistanceInfo> getDistancesByLossRate(ArrayList<DistanceInfo> distances){
//
//        ArrayList<DistanceInfo> output = new ArrayList<>();
//
//        output.add(distances.get(0));
//        output.add(distances.get(1));
//
//        return output;
//    }
//
//    public static ArrayList<DistanceInfo> getDistancesByK(ArrayList<DistanceInfo> distances){
//        ArrayList<DistanceInfo> output = new ArrayList<>();
//
//        output.add(distances.get(0));
//        output.add(distances.get(1));
//
//        return output;
//    }

    public class Coordinate{
        public float x, y;

        public Coordinate() {}

        public Coordinate(float x, float y){
            this.x = x;
            this.y = y;
        }
    }

    private final ArrayList<OnResultChangedListener> resultChangedListeners = new ArrayList<>();

    private void invokeResultChangedListeners(){
        for (OnResultChangedListener listener : resultChangedListeners){
            listener.resultChanged();
        }
    }

    public void registerOnResultChangedListener(OnResultChangedListener listener){
        resultChangedListeners.add(listener);
    }

    public void unregisterOnResultChangedListener(OnResultChangedListener listener){
        resultChangedListeners.remove(listener);
    }

    public interface OnResultChangedListener{
        void resultChanged();
    }

    private HighlightAndDisplay highlightAndDisplayFunction;

    public void setHighlightAndDisplayFunction(HighlightAndDisplay function){
        highlightAndDisplayFunction = function;
    }

    public interface HighlightAndDisplay{
        ArrayList<DistanceInfo> highlight(ArrayList<DistanceInfo> distances, int k);
        ArrayList<DistanceInfo> display(ArrayList<DistanceInfo> distances);
    }
}

