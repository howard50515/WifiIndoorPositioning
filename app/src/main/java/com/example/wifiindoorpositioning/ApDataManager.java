package com.example.wifiindoorpositioning;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
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
    public ArrayList<ReferencePoint> fingerprint;
    public ArrayList<WifiResult> originalResults;
    public ArrayList<WifiResult> results;
    public ArrayList<DistanceInfo> originalDistances;
    public ArrayList<DistanceInfo> displayDistances;
    public ArrayList<DistanceInfo> highlightDistances;
    public String[] apChoices;

    public static final String apValuesDirectoryName = "ap_values";

    private final Dictionary<String, Coordinate> apCoordinate;
    private final AssetManager assetManager;

    private final Dictionary<String, HighlightFunction> highlightFunctions = new Hashtable<>();
    private final Dictionary<String, DisplayFunction> displayFunctions = new Hashtable<>();
    private final Dictionary<String, WeightFunction> weightFunctions = new Hashtable<>();

    @SuppressLint("DefaultLocale")
    private ApDataManager(Context context) {
        config = new Config();

        assetManager = context.getAssets();

        try{
            apChoices = assetManager.list(apValuesDirectoryName);
        } catch (IOException ex){
            throw new RuntimeException();
        }

        addHighlightFunction("距離排序k個", (distances, k) -> {
            ArrayList<DistanceInfo> copy = new ArrayList<>(distances);

            copy.sort(DistanceInfo.distanceComparable);

            return new ArrayList<>(copy.subList(0, k));
        });

        addDisplayFunction("按照距離排序", distances -> {
            ArrayList<DistanceInfo> copy = new ArrayList<>(distances);

            copy.sort(DistanceInfo.distanceComparable);

            return copy;
        });

        addWeightFunction("KNN", highlights -> {
            ArrayList<Float> weights = new ArrayList<>();

            float weight = 1f / highlights.size();

            for (int i = 0; i < highlights.size(); i++){
                weights.add(weight);
            }

            return weights;
        });

        setHighlightFunction("距離排序k個");
        setDisplayFunction("按照距離排序");
        setWeightFunction("KNN");
        loadApValueAtIndex(0);

        apCoordinate = new Gson().fromJson(
                getValue(context.getResources().openRawResource(R.raw.ap_coordinate)),
                new TypeToken<Hashtable<String, Coordinate>>(){}.getType());

        applyCoordinateToFingerPrint();

        registerOnConfigChangedListener(config -> calculateResult());
    }

    private void applyCoordinateToFingerPrint(){
        for (int i = 0;i < fingerprint.size(); i++){
            ReferencePoint rp = fingerprint.get(i);
            Coordinate c = apCoordinate.get(rp.name);
            if (c != null){
                rp.coordinateX = c.x;
                rp.coordinateY = c.y;
            }
            else{
                throw new RuntimeException("未提供" + rp.name + "座標");
            }
        }
    }

    private String getValue(InputStream stream){
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        StringBuilder builder = new StringBuilder();

        try {
            String line;
            while ((line = reader.readLine()) != null)
                builder.append(line).append('\n');
        } catch (IOException e) {
            e.printStackTrace();
        }

        return builder.toString();
    }

    public void loadApValue(String valueName){
        String apPath = apValuesDirectoryName + "/" + valueName + "/ap.txt";
        String apVectorPath = apValuesDirectoryName + "/" + valueName + "/ap_vector.txt";

        try {
            accessPoints = new Gson().fromJson(getValue(assetManager.open(apPath)), new TypeToken<ArrayList<String>>(){}.getType());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            fingerprint = new Gson().fromJson(getValue(assetManager.open(apVectorPath)), new TypeToken<ArrayList<ReferencePoint>>(){}.getType());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (apCoordinate != null){
            applyCoordinateToFingerPrint();
        }

        calculateResult();
    }

    public void loadApValueAtIndex(int index){
        loadApValue(apChoices[index]);
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

    public void calculateResult(){
        if (originalResults == null) return;

        this.results = getSelectedApResults(originalResults);

        ArrayList<Float> ssids = getVector(results);

        ArrayList<DistanceInfo> distances = new ArrayList<>();
        for (ReferencePoint rp : fingerprint){
            float sum = 0;
            int pastFoundNum = 0;
            int notFoundNum = 0;
            int pastNotFoundNum = 0;
            int foundNum = 0;

            for (int j = 0; j < rp.vector.size(); j++){
                if (rp.vector.get(j) != -100){
                    pastFoundNum++;

                    if (ssids.get(j) == -100){
                        notFoundNum++;
                    }
                }
                else{
                    pastNotFoundNum++;

                    if (ssids.get(j) != -100){
                        foundNum++;
                    }
                }

                float diff = rp.vector.get(j) - ssids.get(j);

                sum += diff * diff;
            }

            distances.add(new DistanceInfo(rp.name, (float)Math.sqrt(sum), rp.coordinateX, rp.coordinateY, pastFoundNum, notFoundNum, pastNotFoundNum, foundNum));
        }

        this.originalDistances = new ArrayList<>(distances);

        refresh();
    }

    public void setResult(ArrayList<WifiResult> results){
        this.originalResults = results;

        calculateResult();
    }

    private void updateFunction(){
        if (originalDistances != null){
            if (this.highlightDistances != null){
                for (int i = 0; i < highlightDistances.size(); i++){
                    highlightDistances.get(i).weight = 0;
                }
            }

            this.displayDistances = displayFunction.display(originalDistances);
            this.highlightDistances = highlightFunction.highlight(originalDistances, config.k);
            ArrayList<Float> weights = weightFunction.weight(highlightDistances);

            for (int i = 0; i < weights.size(); i++){
                highlightDistances.get(i).weight = weights.get(i);
            }
        }
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

        for (int i = 0; i < highlightDistances.size(); i++){
            DistanceInfo info = highlightDistances.get(i);

            predict.x += info.weight * info.coordinateX;
            predict.y += info.weight * info.coordinateY;
        }

        // 回傳預測的座標
        return predict;
    }

    public class Coordinate{
        public float x, y;

        public Coordinate() {}

        public Coordinate(float x, float y){
            this.x = x;
            this.y = y;
        }
    }

    //region OnResultChangedListener

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

    //endregion

    //region Function相關

    private HighlightFunction highlightFunction;
    private String highlightFunctionName;
    private DisplayFunction displayFunction;
    private String displayFunctionName;
    private WeightFunction weightFunction;
    private String weightFunctionName;

    private void refresh(){
        if (originalDistances == null) return;

        updateFunction();

        invokeResultChangedListeners();
    }

    public void addHighlightFunction(String name, HighlightFunction function){
        highlightFunctions.put(name, function);
    }

    public void setHighlightFunction(String name){
        highlightFunction = highlightFunctions.get(name);

        highlightFunctionName = name;

        refresh();
    }

    public void addDisplayFunction(String name, DisplayFunction function){
        displayFunctions.put(name, function);
    }

    public void setDisplayFunction(String name){
        displayFunction = displayFunctions.get(name);

        displayFunctionName = name;

        refresh();
    }

    public void addWeightFunction(String name, WeightFunction function){
        weightFunctions.put(name, function);
    }

    public void setWeightFunction(String name){
        weightFunction = weightFunctions.get(name);

        weightFunctionName = name;

        refresh();
    }

    public int getCurrentHighlightFunctionIndex(){
        Enumeration<String> keys = highlightFunctions.keys();

        int index = 0;
        while (keys.hasMoreElements()){
            if (highlightFunctionName.equals(keys.nextElement())){
                return index;
            }

            index++;
        }

        return -1;
    }

    public ArrayList<String> getAllHighlightFunctionNames(){
        Enumeration<String> keys = highlightFunctions.keys();

        ArrayList<String> names = new ArrayList<>();
        while (keys.hasMoreElements()){
            names.add(keys.nextElement());
        }

        return names;
    }

    public int getCurrentDisplayFunctionIndex(){
        Enumeration<String> keys = displayFunctions.keys();

        int index = 0;
        while (keys.hasMoreElements()){
            if (displayFunctionName.equals(keys.nextElement())){
                return index;
            }

            index++;
        }

        return -1;
    }

    public ArrayList<String> getAllDisplayFunctionNames(){
        Enumeration<String> keys = displayFunctions.keys();

        ArrayList<String> names = new ArrayList<>();
        while (keys.hasMoreElements()){
            names.add(keys.nextElement());
        }

        return names;
    }

    public int getCurrentWeightFunctionIndex(){
        Enumeration<String> keys = weightFunctions.keys();

        int index = 0;
        while (keys.hasMoreElements()){
            if (weightFunctionName.equals(keys.nextElement())){
                return index;
            }

            index++;
        }

        return -1;
    }

    public ArrayList<String> getAllWeightFunctionNames(){
        Enumeration<String> keys = weightFunctions.keys();

        ArrayList<String> names = new ArrayList<>();
        while (keys.hasMoreElements()){
            names.add(keys.nextElement());
        }

        return names;
    }

    public interface HighlightFunction{
        ArrayList<DistanceInfo> highlight(ArrayList<DistanceInfo> distances, int k);
    }

    public interface DisplayFunction{
        ArrayList<DistanceInfo> display(ArrayList<DistanceInfo> distances);
    }

    public interface WeightFunction{
        ArrayList<Float> weight(ArrayList<DistanceInfo> highlights);
    }

    //endregion

    private Config config;

    public Config getConfig(){
        return config;
    }

    public class Config{
        public int k = 4;
        public int referencePointRadius = 20;
        public boolean displayReferencePoint = true;
    }

    private final ArrayList<OnConfigChangedListener> configChangedListeners = new ArrayList<>();

    public void invokeConfigChangedListener(){
        for (OnConfigChangedListener listener : configChangedListeners){
            listener.onConfigChanged(config);
        }
    }

    public void registerOnConfigChangedListener(OnConfigChangedListener listener){
        configChangedListeners.add(listener);
    }

    public void unregisterOnConfigChangedListener(OnConfigChangedListener listener){
        configChangedListeners.remove(listener);
    }

    public interface OnConfigChangedListener{
        void onConfigChanged(Config config);
    }
}

