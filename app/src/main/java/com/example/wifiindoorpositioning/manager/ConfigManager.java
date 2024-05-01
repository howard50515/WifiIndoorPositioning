package com.example.wifiindoorpositioning.manager;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.widget.LinearLayout;

import com.example.wifiindoorpositioning.R;
import com.example.wifiindoorpositioning.datatype.TestPoint;

import com.example.wifiindoorpositioning.datatype.TestPointInfo;
import com.example.wifiindoorpositioning.datatype.WifiResult;
import com.example.wifiindoorpositioning.function.DisplayFunction;
import com.example.wifiindoorpositioning.function.HighlightFunction;
import com.example.wifiindoorpositioning.function.WeightFunction;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Set;

public class ConfigManager {
    private static ConfigManager instance;

    public static void createInstance(Activity context){
        if (instance != null)
            return;

        instance = new ConfigManager(context);
    }

    public static ConfigManager getInstance(){
        return instance;
    }

    private ConfigManager(Context context){
        assetManager = context.getAssets();

        try{
            apValues = assetManager.list(ApDataManager.apValuesDirectoryName);
        } catch (IOException ex){
            throw new RuntimeException("Fail to Load Ap Values");
        }

        try{
            resultHistories = assetManager.list(resultHistoriesDirectoryName);
        } catch (IOException ex){
            throw new RuntimeException("Fail to Load Results Histories");
        }

        for (String name : apValues){
            enableApValues.put(name, true);
        }

        Hashtable<String, ApDataManager.Coordinate> testPointsCoordinate = new Gson().fromJson(
                ApDataManager.getValue(context.getResources().openRawResource(R.raw.tp_coordinate)),
                new TypeToken<Hashtable<String, ApDataManager.Coordinate>>(){}.getType());
        testPoints = new ArrayList<>();
        testPointsCoordinate.forEach((name, coordinate) -> testPoints.add(new TestPoint(name, coordinate)));
    }

    private final AssetManager assetManager;

    public final static String resultHistoriesDirectoryName = "results_history";

    public int k = 4;
    public int referencePointRadius = 50, actualPointRadius = 50, predictPointRadius = 50;
    public boolean isDebugMode = true;
    public LinearLayout debugView;

    public String[] resultHistories;
    public String[] apValues;
    public HashMap<String, Boolean> enableApValues = new LinkedHashMap<>();
    public HashMap<String, DisplayFunction> displayFunctions = new LinkedHashMap<>();
    public HashMap<String, HighlightFunction> highlightFunctions = new LinkedHashMap<>();
    public HashMap<String, Boolean> enableHighlightFunctions = new LinkedHashMap<>();
    public HashMap<String, WeightFunction> weightFunctions = new LinkedHashMap<>();
    public HashMap<String, Boolean> enableWeightFunctions = new LinkedHashMap<>();

    private final ArrayList<TestPoint> testPoints;

    public String[] getResultHistoriesName(){
        String[] names = new String[resultHistories.length];

        for (int i = 0; i < resultHistories.length; i++){
            names[i] = resultHistories[i].replace(".txt", "");
        }

        return names;
    }

    public TestPointInfo getResultHistory(int index){
        try (InputStream inputStream = assetManager.open(resultHistoriesDirectoryName + "/" + resultHistories[index])) {
            TestPointInfo testPointInfo = new Gson().fromJson(ApDataManager.getValue(inputStream),
                    new TypeToken<TestPointInfo>(){}.getType());

            for (WifiResult result : testPointInfo.results){
                result.applyApId();
            }

            return testPointInfo;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public int getApValueIndex(String name){
        for (int i = 0; i < apValues.length; i++){
            if (apValues[i].equals(name)){
                return i;
            }
        }

        return -1;
    }

    public ArrayList<String> getAllEnableApValueNames(){
        ArrayList<String> names = new ArrayList<>();

        for (String name : apValues){
            if (Boolean.TRUE.equals(enableApValues.get(name))){
                names.add(name);
            }
        }

        return names;
    }

    public TestPoint getTestPointAtIndex(int index) {
        return testPoints.get(index);
    }

    public int getTestPointIndex(String name){
        for (int i = 0; i < testPoints.size(); i++){
            if (name.equals(testPoints.get(i).name)){
                return i;
            }
        }

        return -1;
    }

    public ArrayList<String> getAllTestPointNames(){
        ArrayList<String> names = new ArrayList<>();


        for (int i = 0; i < testPoints.size(); i++){
            names.add(testPoints.get(i).name);
        }

        return names;
    }

    public ArrayList<String> getAllDisplayFunctionNames(){
        return new ArrayList<>(displayFunctions.keySet());
    }

    public int getHighlightFunctionIndex(String name){
        Set<String> keys = highlightFunctions.keySet();

        int index = 0;
        for (String key : keys){
            if (name.equals(key)){
                return index;
            }

            index++;
        }

        return -1;
    }

    public ArrayList<String> getAllHighlightFunctionNames(){
        return new ArrayList<>(highlightFunctions.keySet());
    }

    public ArrayList<String> getAllEnableHighlightFunctionNames(){
        ArrayList<String> names = new ArrayList<>();

        Set<String> keys = highlightFunctions.keySet();

        for (String name : keys){
            if (Boolean.TRUE.equals(enableHighlightFunctions.get(name))){
                names.add(name);
            }
        }

        return names;
    }

    public int getWeightFunctionIndex(String name){
        Set<String> keys = weightFunctions.keySet();

        int index = 0;
        for (String key : keys){
            if (name.equals(key)){
                return index;
            }

            index++;
        }

        return -1;
    }

    public ArrayList<String> getAllWeightFunctionNames(){
        return new ArrayList<>(weightFunctions.keySet());
    }

    public ArrayList<String> getAllEnableWeightFunctionNames(){
        ArrayList<String> names = new ArrayList<>();

        Set<String> keys = weightFunctions.keySet();

        for (String name : keys){
            if (Boolean.TRUE.equals(enableWeightFunctions.get(name))){
                names.add(name);
            }
        }

        return names;
    }

    public void addDisplayFunction(String name, DisplayFunction function){
        displayFunctions.put(name, function);
    }

    public void addHighlightFunction(String name, HighlightFunction function){
        addHighlightFunction(name, function, true);
    }

    public void addHighlightFunction(String name, HighlightFunction function, boolean enable){
        highlightFunctions.put(name, function);
        enableHighlightFunctions.put(name, enable);
    }

    public void addWeightFunction(String name, WeightFunction function){
        addWeightFunction(name, function, true);
    }

    public void addWeightFunction(String name, WeightFunction function, boolean enable){
        weightFunctions.put(name, function);
        enableWeightFunctions.put(name, enable);
    }

    private final ArrayList<OnConfigChangedListener> configChangedListeners = new ArrayList<>();

    public void invokeConfigChangedListener(){
        for (OnConfigChangedListener listener : configChangedListeners){
            listener.onConfigChanged();
        }
    }

    public void registerOnConfigChangedListener(OnConfigChangedListener listener){
        configChangedListeners.add(listener);
    }

    public void unregisterOnConfigChangedListener(OnConfigChangedListener listener){
        configChangedListeners.remove(listener);
    }

    public interface OnConfigChangedListener{
        void onConfigChanged();
    }
}
