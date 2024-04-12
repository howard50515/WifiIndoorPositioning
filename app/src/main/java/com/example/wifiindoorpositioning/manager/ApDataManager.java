package com.example.wifiindoorpositioning.manager;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;

import com.example.wifiindoorpositioning.R;
import com.example.wifiindoorpositioning.datatype.ApDistanceInfo;
import com.example.wifiindoorpositioning.datatype.DistanceInfo;
import com.example.wifiindoorpositioning.datatype.ReferencePoint;
import com.example.wifiindoorpositioning.datatype.WifiResult;

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

    public static final String apValuesDirectoryName = "ap_values";

    private final Dictionary<String, Coordinate> referencePointsCoordinate;

    private final AssetManager assetManager;

    private final Hashtable<String, DisplayFunction> displayFunctions = new Hashtable<>();

    public final static int UNCERTAIN_CHANGED = -1;
    public final static int WIFI_RESULT_CHANGED = 0;
    public final static int AP_VALUE_CHANGED = 1;
    public final static int HIGHLIGHT_FUNCTION_CHANGED = 2;
    public final static int DISPLAY_FUNCTION_CHANGED = 3;
    public final static int WEIGHT_FUNCTION_CHANGED = 4;

    public final static int TEST_POINT_CHANGED = 5;

    @SuppressLint("DefaultLocale")
    private ApDataManager(Context context) {
        assetManager = context.getAssets();

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

        referencePointsCoordinate = new Gson().fromJson(
                getValue(context.getResources().openRawResource(R.raw.rp_coordinate)),
                new TypeToken<Hashtable<String, Coordinate>>(){}.getType());

        loadApValueAtIndex(0);

        ConfigManager.getInstance().registerOnConfigChangedListener(() -> calculateResult(UNCERTAIN_CHANGED));
    }

    private void applyCoordinateToFingerPrint(ArrayList<ReferencePoint> fingerprint){
        for (int i = 0;i < fingerprint.size(); i++){
            ReferencePoint rp = fingerprint.get(i);
            Coordinate c = referencePointsCoordinate.get(rp.name);
            if (c != null){
                rp.coordinateX = c.x;
                rp.coordinateY = c.y;
            }
            else{
                throw new RuntimeException("未提供" + rp.name + "座標");
            }
        }
    }

    public static String getValue(InputStream stream){
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

        accessPoints = loadAccessPoints(assetManager, apPath);

        fingerprint = loadFingerPrint(assetManager, apVectorPath);

        applyCoordinateToFingerPrint(fingerprint);

        calculateResult(AP_VALUE_CHANGED);
    }

    public void loadApValueAtIndex(int index){
        loadApValue(ConfigManager.getInstance().apValues[index]);
    }

    public Coordinate getCoordinateWithValues(String apValueName, HighlightFunction highlightFunction, WeightFunction weightFunction){
        String apPath = apValuesDirectoryName + "/" + apValueName + "/ap.txt";
        String apVectorPath = apValuesDirectoryName + "/" + apValueName + "/ap_vector.txt";

        if (originalResults == null) return new Coordinate();

        ArrayList<String> accessPoints = loadAccessPoints(assetManager, apPath);

        ArrayList<ReferencePoint> fingerprint = loadFingerPrint(assetManager, apVectorPath);

        applyCoordinateToFingerPrint(fingerprint);

        ArrayList<DistanceInfo> distances = getDistances(fingerprint, getVector(getSelectedApResults(originalResults, accessPoints), accessPoints));

        ArrayList<DistanceInfo> highlights = getHighlights(distances, ConfigManager.getInstance().k, highlightFunction);

        ArrayList<Float> weights = getWeights(highlights, weightFunction);

        setHighlightWeights(highlights, weights);

        return getPredictCoordinate(highlights);
    }

    public ArrayList<ApDistanceInfo> getAllApDistances(){
        if (originalResults == null) return null;

        ArrayList<ApDistanceInfo> apDistances = new ArrayList<>();

        ConfigManager configManager = ConfigManager.getInstance();
        Dictionary<String, HighlightFunction> highlightFunctions = configManager.highlightFunctions;
        Dictionary<String, WeightFunction> weightFunctions = configManager.weightFunctions;
        ArrayList<String> apValueNames = configManager.getAllEnableApValueNames();
        ArrayList<String> highlightNames = configManager.getAllEnableHighlightFunctionNames();
        ArrayList<String> weightNames = configManager.getAllEnableWeightFunctionNames();

        for (String apValueName : apValueNames){
            for (String highlightName : highlightNames){
                for (String weightName : weightNames){
                    apDistances.add(new ApDistanceInfo(apValueName, highlightName, weightName,
                            getCoordinateWithValues(apValueName, highlightFunctions.get(highlightName), weightFunctions.get(weightName))));
                }
            }
        }

        return apDistances;
    }

    private static ArrayList<String> loadAccessPoints(AssetManager assetManager, String path){
        try {
            return new Gson().fromJson(getValue(assetManager.open(path)), new TypeToken<ArrayList<String>>(){}.getType());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static ArrayList<ReferencePoint> loadFingerPrint(AssetManager assetManager, String path){
        try {
            return new Gson().fromJson(getValue(assetManager.open(path)), new TypeToken<ArrayList<ReferencePoint>>(){}.getType());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static ArrayList<Float> getVector(ArrayList<WifiResult> results, ArrayList<String> accessPoints){
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

    public static ArrayList<WifiResult> getSelectedApResults(ArrayList<WifiResult> results, ArrayList<String> accessPoints){
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

    public static ArrayList<DistanceInfo> getDistances(ArrayList<ReferencePoint> rps, ArrayList<Float> ssids){
        ArrayList<DistanceInfo> distances = new ArrayList<>();

        for (ReferencePoint rp : rps){
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

        return distances;
    }

    public void calculateResult(int changeCode){
        if (originalResults == null) return;

        this.results = getSelectedApResults(originalResults, accessPoints);

        ArrayList<Float> ssids = getVector(results, accessPoints);

        ArrayList<DistanceInfo> distances = getDistances(fingerprint, ssids);

        this.originalDistances = new ArrayList<>(distances);

        refresh(changeCode);
    }

    public void setResult(ArrayList<WifiResult> results){
        this.originalResults = results;

        calculateResult(WIFI_RESULT_CHANGED);
    }

    private void updateFunction(){
        if (originalDistances != null){
            resetDistancesWeight();

            this.displayDistances = getDisplays(originalDistances, displayFunction);
            this.highlightDistances = getHighlights(originalDistances, ConfigManager.getInstance().k, highlightFunction);

            setHighlightWeights(highlightDistances, getWeights(highlightDistances, weightFunction));
        }
    }

    private void resetDistancesWeight(){
        if (this.highlightDistances != null){
            for (int i = 0; i < highlightDistances.size(); i++){
                highlightDistances.get(i).weight = 0;
            }
        }
    }

    private static void setHighlightWeights(ArrayList<DistanceInfo> highlights, ArrayList<Float> weights){
        for (int i = 0; i < weights.size(); i++){
            highlights.get(i).weight = weights.get(i);
        }
    }

    public static ArrayList<Float> getWeights(ArrayList<DistanceInfo> highlights, WeightFunction function){
        return function.weight(highlights);
    }

    public static ArrayList<DistanceInfo> getHighlights(ArrayList<DistanceInfo> distances, int k, HighlightFunction function){
        return function.highlight(distances, k);
    }

    public static ArrayList<DistanceInfo> getDisplays(ArrayList<DistanceInfo> distances, DisplayFunction function){
        return function.display(distances);
    }

    public static Coordinate getPredictCoordinate(ArrayList<DistanceInfo> highlights){
        Coordinate predict = new Coordinate();

        if (highlights.size() == 0) return predict;

        for (int i = 0; i < highlights.size(); i++){
            DistanceInfo info = highlights.get(i);

            if (Float.isNaN(info.weight)) System.out.println(highlights.size() + " " + info.distance);

            predict.x += info.weight * info.coordinateX;
            predict.y += info.weight * info.coordinateY;
        }

        // 回傳預測的座標
        return predict;
    }

    public static class Coordinate{
        public float x, y;

        public Coordinate() {}

        public Coordinate(float x, float y){
            this.x = x;
            this.y = y;
        }
    }

    //region OnResultChangedListener

    private final ArrayList<OnResultChangedListener> resultChangedListeners = new ArrayList<>();

    private void invokeResultChangedListeners(int changeCode){
        for (OnResultChangedListener listener : resultChangedListeners){
            listener.resultChanged(changeCode);
        }
    }

    public void registerOnResultChangedListener(OnResultChangedListener listener){
        resultChangedListeners.add(listener);
    }

    public void unregisterOnResultChangedListener(OnResultChangedListener listener){
        resultChangedListeners.remove(listener);
    }

    public interface OnResultChangedListener{
        void resultChanged(int changeCode);
    }

    //endregion

    //region Function相關

    private HighlightFunction highlightFunction;
    private String highlightFunctionName;
    private DisplayFunction displayFunction;
    private String displayFunctionName;
    private WeightFunction weightFunction;
    private String weightFunctionName;

    private void refresh(int changeCode){
        if (originalDistances == null) return;

        updateFunction();

        invokeResultChangedListeners(changeCode);
    }

    public void addHighlightFunction(String name, HighlightFunction function){
        ConfigManager.getInstance().addHighlightFunction(name, function);
    }

    public void setHighlightFunction(String name){
        highlightFunction = ConfigManager.getInstance().highlightFunctions.get(name);

        highlightFunctionName = name;

        refresh(HIGHLIGHT_FUNCTION_CHANGED);
    }

    public void addDisplayFunction(String name, DisplayFunction function){
        displayFunctions.put(name, function);
    }

    public void setDisplayFunction(String name){
        displayFunction = displayFunctions.get(name);

        displayFunctionName = name;

        refresh(DISPLAY_FUNCTION_CHANGED);
    }

    public void addWeightFunction(String name, WeightFunction function){
        ConfigManager.getInstance().addWeightFunction(name, function);
    }

    public void setWeightFunction(String name){
        weightFunction = ConfigManager.getInstance().weightFunctions.get(name);

        weightFunctionName = name;

        refresh(WEIGHT_FUNCTION_CHANGED);
    }

    public int getHighlightFunctionIndex(String name){
        Enumeration<String> keys = ConfigManager.getInstance().highlightFunctions.keys();

        int index = 0;
        while (keys.hasMoreElements()){
            if (name.equals(keys.nextElement())){
                return index;
            }

            index++;
        }

        return -1;
    }

    public int getCurrentHighlightFunctionIndex(){
        return getHighlightFunctionIndex(highlightFunctionName);
    }

    public ArrayList<String> getAllHighlightFunctionNames(){
        Enumeration<String> keys = ConfigManager.getInstance().highlightFunctions.keys();

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

    public int getWeightFunctionIndex(String name){
        Enumeration<String> keys = ConfigManager.getInstance().weightFunctions.keys();

        int index = 0;
        while (keys.hasMoreElements()){
            if (name.equals(keys.nextElement())){
                return index;
            }

            index++;
        }

        return -1;
    }

    public int getCurrentWeightFunctionIndex(){
        return getWeightFunctionIndex(weightFunctionName);
    }

    public ArrayList<String> getAllWeightFunctionNames(){
        Enumeration<String> keys = ConfigManager.getInstance().weightFunctions.keys();

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
}

