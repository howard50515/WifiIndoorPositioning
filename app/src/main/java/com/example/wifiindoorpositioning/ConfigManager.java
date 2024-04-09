package com.example.wifiindoorpositioning;

import android.app.Activity;
import android.content.Context;

import com.example.wifiindoorpositioning.function.DistanceRateHighlightFunction;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

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
        addHighlightFunction("距離前40%", new DistanceRateHighlightFunction(0.4f));
        addHighlightFunction("距離前30%", new DistanceRateHighlightFunction(0.3f));
        addHighlightFunction("距離前20%", new DistanceRateHighlightFunction(0.2f));

        Hashtable<String, ApDataManager.Coordinate> testPointsCoordinate = new Gson().fromJson(
                ApDataManager.getValue(context.getResources().openRawResource(R.raw.tp_coordinate)),
                new TypeToken<Hashtable<String, ApDataManager.Coordinate>>(){}.getType());
        testPoints = new ArrayList<>();
        testPointsCoordinate.forEach((name, coordinate) -> testPoints.add(new ReferencePoint(name, coordinate)));

        setTestPointAtIndex(0);
    }

    public int k = 4;
    public int referencePointRadius = 20;
    public boolean displayReferencePoint = true;
    public Dictionary<String, ApDataManager.HighlightFunction> highlightFunctions = new Hashtable<>();
    public Dictionary<String, Boolean> enableHighlightFunctions = new Hashtable<>();
    public Dictionary<String, ApDataManager.WeightFunction> weightFunctions = new Hashtable<>();
    public Dictionary<String, Boolean> enableWeightFunctions = new Hashtable<>();
    public ReferencePoint testPoint;

    private final ArrayList<ReferencePoint> testPoints;

    public void setTestPointAtIndex(int index) {
        this.testPoint = testPoints.get(index);
    }

    public ReferencePoint getTestPointAtIndex(int index) {
        return testPoints.get(index);
    }

    public ArrayList<String> getAllTestPointNames(){
        ArrayList<String> names = new ArrayList<>();


        for (int i = 0; i < testPoints.size(); i++){
            names.add(testPoints.get(i).name);
        }

        return names;
    }

    public int getCurrentTestPointIndex(){
        for (int i = 0; i < testPoints.size(); i++){
            if (testPoint.equals(testPoints.get(i))){
                return i;
            }
        }

        return -1;
    }

    public ArrayList<String> getAllEnableHighlightFunctionNames(){
        ArrayList<String> names = new ArrayList<>();

        Enumeration<String> keys = highlightFunctions.keys();

        while (keys.hasMoreElements()){
            String name = keys.nextElement();

            if (enableHighlightFunctions.get(name)){
                names.add(name);
            }
        }

        return names;
    }

    public ArrayList<String> getAllEnableWeightFunctionNames(){
        ArrayList<String> names = new ArrayList<>();

        Enumeration<String> keys = weightFunctions.keys();

        while (keys.hasMoreElements()){
            String name = keys.nextElement();

            if (enableWeightFunctions.get(name)){
                names.add(name);
            }
        }

        return names;
    }

    public void addHighlightFunction(String name, ApDataManager.HighlightFunction function){
        addHighlightFunction(name, function, true);
    }

    public void addHighlightFunction(String name, ApDataManager.HighlightFunction function, boolean enable){
        highlightFunctions.put(name, function);
        enableHighlightFunctions.put(name, enable);
    }

    public void addWeightFunction(String name, ApDataManager.WeightFunction function){
        addWeightFunction(name, function, true);
    }

    public void addWeightFunction(String name, ApDataManager.WeightFunction function, boolean enable){
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
