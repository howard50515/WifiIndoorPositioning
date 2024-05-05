package com.example.wifiindoorpositioning.manager;

import android.annotation.SuppressLint;
import android.content.res.AssetManager;

import com.example.wifiindoorpositioning.MainActivity;
import com.example.wifiindoorpositioning.R;
import com.example.wifiindoorpositioning.datatype.ApDistanceInfo;
import com.example.wifiindoorpositioning.datatype.DistanceInfo;
import com.example.wifiindoorpositioning.datatype.ReferencePoint;
import com.example.wifiindoorpositioning.datatype.WifiResult;

import com.example.wifiindoorpositioning.function.DisplayFunction;
import com.example.wifiindoorpositioning.function.FirstKDistanceHighlightFunction;
import com.example.wifiindoorpositioning.function.HighlightFunction;
import com.example.wifiindoorpositioning.function.WeightFunction;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Random;
import java.util.Set;

public class ApDataManager {
    private static ApDataManager instance;

    public static void createInstance(MainActivity context){
        if (instance != null)
            return;

        instance = new ApDataManager(context);
    }

    public static ApDataManager getInstance(){
        return instance;
    }

    public ArrayList<String> accessPoints;
    public ArrayList<ArrayList<ReferencePoint>> positionClusters;
    public ArrayList<ProtoCluster> signalClusters;
    public ArrayList<ReferencePoint> fingerprint;
    public ArrayList<WifiResult> originalResults;
    public ArrayList<WifiResult> results;
    public ArrayList<DistanceInfo> originalDistances;
    public ArrayList<DistanceInfo> displayDistances;
    public ArrayList<DistanceInfo> highlightDistances;

    public static final String apValuesDirectoryName = "ap_values";

    private final Dictionary<String, Coordinate> referencePointsCoordinate;

    private final AssetManager assetManager;

    private final MainActivity activity;

    public final static int UNCERTAIN_CHANGED = -1;
    public final static int WIFI_RESULT_CHANGED = 0;
    public final static int AP_VALUE_CHANGED = 1;
    public final static int HIGHLIGHT_FUNCTION_CHANGED = 2;
    public final static int DISPLAY_FUNCTION_CHANGED = 3;
    public final static int WEIGHT_FUNCTION_CHANGED = 4;
    public final static int TEST_POINT_CHANGED = 5;

    @SuppressLint("DefaultLocale")
    private ApDataManager(MainActivity context) {
        this.activity = context;

        assetManager = context.getAssets();

        ConfigManager.getInstance().addHighlightFunction("距離排序k個", new FirstKDistanceHighlightFunction(), false);

        ConfigManager.getInstance().addDisplayFunction("按照距離排序", distances -> {
            ArrayList<DistanceInfo> copy = new ArrayList<>(distances);

            copy.sort(DistanceInfo.distanceComparable);

            return copy;
        });

        ConfigManager.getInstance().addWeightFunction("KNN", highlights -> {
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
        StringBuilder builder = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))){
            String line;
            while ((line = reader.readLine()) != null)
                builder.append(line).append('\n');
        } catch (IOException e) {
            e.printStackTrace();
        }

        return builder.toString();
    }

    public String apValuesName;

    public void loadApValue(String valueName){
        apValuesName = valueName;
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

        ArrayList<ArrayList<ReferencePoint>> positionCluster = getCoordinateClustering(fingerprint);

        ArrayList<ProtoCluster> signalCluster = getSignalClustering(fingerprint, positionCluster);

        ArrayList<Float> ssids = getVector(getSelectedApResults(originalResults, accessPoints), accessPoints);

        ArrayList<ReferencePoint> nearestRps = getNearestClusterRps(signalCluster, ssids);

        ArrayList<DistanceInfo> distances = getDistances(nearestRps, ssids);

        ArrayList<DistanceInfo> highlights = getHighlights(distances, ConfigManager.getInstance().kNearest, highlightFunction);

        ArrayList<Float> weights = getWeights(highlights, weightFunction);

        setHighlightWeights(highlights, weights);

        return getPredictCoordinate(highlights);
    }

    public ArrayList<ApDistanceInfo> getCoordinateWithValues(String apValueName){
        String apPath = apValuesDirectoryName + "/" + apValueName + "/ap.txt";
        String apVectorPath = apValuesDirectoryName + "/" + apValueName + "/ap_vector.txt";

        if (originalResults == null) return null;

        ArrayList<String> accessPoints = loadAccessPoints(assetManager, apPath);

        ArrayList<ReferencePoint> fingerprint = loadFingerPrint(assetManager, apVectorPath);

        applyCoordinateToFingerPrint(fingerprint);

        ArrayList<ArrayList<ReferencePoint>> positionCluster = getCoordinateClustering(fingerprint);

        ArrayList<ProtoCluster> signalCluster = getSignalClustering(fingerprint, positionCluster);

        ArrayList<Float> ssids = getVector(getSelectedApResults(originalResults, accessPoints), accessPoints);

        ArrayList<ReferencePoint> nearestRps = getNearestClusterRps(signalCluster, ssids);

        ArrayList<DistanceInfo> distances = getDistances(nearestRps, ssids);

        ConfigManager configManager = ConfigManager.getInstance();
        HashMap<String, HighlightFunction> highlightFunctions = configManager.highlightFunctions;
        HashMap<String, WeightFunction> weightFunctions = configManager.weightFunctions;
        ArrayList<String> highlightNames = configManager.getAllEnableHighlightFunctionNames();
        ArrayList<String> weightNames = configManager.getAllEnableWeightFunctionNames();

        ArrayList<ApDistanceInfo> apDistances = new ArrayList<>();
        int k = ConfigManager.getInstance().kNearest;
        for (String highlightName : highlightNames){
            for (String weightName : weightNames){
                ArrayList<DistanceInfo> highlights = getHighlights(distances, k, highlightFunction);

                ArrayList<Float> weights = getWeights(highlights, weightFunction);

                setHighlightWeights(highlights, weights);

                apDistances.add(new ApDistanceInfo(apValueName, highlightName, weightName,
                        getCoordinateWithValues(apValueName, highlightFunctions.get(highlightName), weightFunctions.get(weightName))));
            }
        }

        return apDistances;
    }

    public ArrayList<ApDistanceInfo> getAllApDistances(){
        if (originalResults == null) return null;

        ArrayList<ApDistanceInfo> apDistances = new ArrayList<>();

        ConfigManager configManager = ConfigManager.getInstance();
        ArrayList<String> apValueNames = configManager.getAllEnableApValueNames();

//        HashMap<String, HighlightFunction> highlightFunctions = configManager.highlightFunctions;
//        HashMap<String, WeightFunction> weightFunctions = configManager.weightFunctions;
//        ArrayList<String> highlightNames = configManager.getAllEnableHighlightFunctionNames();
//        ArrayList<String> weightNames = configManager.getAllEnableWeightFunctionNames();
//
//        for (String apValueName : apValueNames){
//            for (String highlightName : highlightNames){
//                for (String weightName : weightNames){
//                    apDistances.add(new ApDistanceInfo(apValueName, highlightName, weightName,
//                            getCoordinateWithValues(apValueName, highlightFunctions.get(highlightName), weightFunctions.get(weightName))));
//                }
//            }
//        }

        for (String apValueName : apValueNames){
            apDistances.addAll(getCoordinateWithValues(apValueName));
        }

        return apDistances;
    }

    private static ArrayList<String> loadAccessPoints(AssetManager assetManager, String path){
        try (InputStream inputStream = assetManager.open(path)) {
            return new Gson().fromJson(getValue(inputStream), new TypeToken<ArrayList<String>>(){}.getType());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static ArrayList<ReferencePoint> loadFingerPrint(AssetManager assetManager, String path){
        try (InputStream inputStream = assetManager.open(path)){
            return new Gson().fromJson(getValue(inputStream), new TypeToken<ArrayList<ReferencePoint>>(){}.getType());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static ArrayList<ArrayList<ReferencePoint>> getCoordinateClustering(ArrayList<ReferencePoint> rps){
        int kMeans = ConfigManager.getInstance().kMeans;
        Random rng = new Random(123);

        ArrayList<Coordinate> means = new ArrayList<>();
        int gap = rps.size() / kMeans;
        for (int i = 0; i < kMeans; i++){
            int index = rng.nextInt(gap) + gap * i;

            Coordinate coordinate = new Coordinate(rps.get(index).coordinateX, rps.get(index).coordinateY);

            means.add(coordinate);
        }

        int count = 0;
        ArrayList<ArrayList<ReferencePoint>> clusters;
        do {
            clusters = new ArrayList<>();

            for (int i = 0; i < kMeans; i++) {
                clusters.add(new ArrayList<>());
            }

            for (int i = 0; i < rps.size(); i++) {
                ReferencePoint rp = rps.get(i);

                float minDis = Float.MAX_VALUE;
                int clusterIndex = 0;
                for (int j = 0; j < kMeans; j++) {
                    Coordinate coordinate = means.get(j);
                    float x = coordinate.x - rp.coordinateX;
                    float y = coordinate.y - rp.coordinateY;

                    float dis = (float) Math.sqrt(x * x + y * y);

                    if (minDis > dis) {
                        minDis = dis;
                        clusterIndex = j;
                    }
                }

                clusters.get(clusterIndex).add(rp);
            }

            boolean hasChange = false;
            for (int i = 0; i < clusters.size(); i++) {
                Coordinate coordinate = means.get(i);
                ArrayList<ReferencePoint> clusterRps = clusters.get(i);

                float sumX = 0;
                float sumY = 0;
                for (int k = 0; k < clusterRps.size(); k++) {
                    ReferencePoint clusterRp = clusterRps.get(k);

                    sumX += clusterRp.coordinateX;
                    sumY += clusterRp.coordinateY;
                }

                float avgX = sumX / clusterRps.size();
                float avgY = sumY / clusterRps.size();

                if (coordinate.x != avgX || coordinate.y != avgY){
                    hasChange = true;
                }
                coordinate.x = avgX;
                coordinate.y = avgY;
            }

            if (!hasChange){
                // System.out.println("break at: " + count);
                break;
            }

            count++;

        } while (count <= 10);

        return clusters;
    }

    public static ArrayList<ArrayList<ReferencePoint>> getVectorClustering(ArrayList<String> accessPoints, ArrayList<ReferencePoint> rps){
        int kMeans = ConfigManager.getInstance().kMeans;
        Random rng = new Random(123);

        ArrayList<float[]> means = new ArrayList<>();
        for (int i = 0; i < kMeans; i++){
            float[] mean = new float[accessPoints.size()];

            for (int j = 0; j < accessPoints.size(); j++){
                mean[j] = rng.nextFloat() % 100f - 100;
            }

            means.add(mean);
        }

        int count = 0;
        ArrayList<ArrayList<ReferencePoint>> clusters;
        do {
            clusters = new ArrayList<>();

            for (int i = 0; i < kMeans; i++) {
                clusters.add(new ArrayList<>());
            }

            for (int i = 0; i < rps.size(); i++) {
                ReferencePoint rp = rps.get(i);
                ArrayList<Float> vector = rp.vector;

                float minDis = Float.MAX_VALUE;
                int clusterIndex = 0;
                for (int j = 0; j < kMeans; j++) {
                    float[] mean = means.get(j);

                    float sum = 0;
                    for (int k = 0; k < vector.size(); k++) {
                        float diff = vector.get(k) - mean[k];

                        sum += diff * diff;
                    }

                    float dis = (float) Math.sqrt(sum);

                    if (minDis > dis) {
                        minDis = dis;
                        clusterIndex = j;
                    }
                }

                clusters.get(clusterIndex).add(rp);
            }

            for (int i = 0; i < clusters.size(); i++) {
                float[] mean = means.get(i);
                ArrayList<ReferencePoint> clusterRps = clusters.get(i);

                for (int j = 0; j < mean.length; j++) {
                    float sum = 0;
                    for (int k = 0; k < clusterRps.size(); k++) {
                        ReferencePoint clusterRp = clusterRps.get(k);

                        sum += clusterRp.vector.get(j);
                    }

                    float avg = sum / clusterRps.size();

                    mean[j] = avg;
                }
            }

            count++;

        } while (count <= 10);

        return clusters;
    }

    public static ArrayList<ProtoCluster> getSignalClustering(ArrayList<ReferencePoint> rps, ArrayList<ArrayList<ReferencePoint>> positionCluster){
        int k = ConfigManager.getInstance().kMeans;
        int q = ConfigManager.getInstance().qClusterNum;
        Random rng = new Random(123);

        if (k > q){
            throw new RuntimeException("q must equals or greater than k");
        }

        int randomSelectNum = q - k;

        ArrayList<Integer> labels = new ArrayList<>();
        ArrayList<ProtoCluster> protoVectors = new ArrayList<>();
        for (int i = 0; i < k; i++){
            int rpIndex = rng.nextInt(positionCluster.get(i).size());

            labels.add(i);
            protoVectors.add(new ProtoCluster(new ArrayList<>(), new ArrayList<>(positionCluster.get(i).get(rpIndex).vector)));
        }

        for (int i = 0; i < randomSelectNum; i++){
            int clusterIndex = rng.nextInt(positionCluster.size());
            int rpIndex = rng.nextInt(positionCluster.get(clusterIndex).size());

            labels.add(clusterIndex);
            protoVectors.add(new ProtoCluster(new ArrayList<>(), new ArrayList<>(positionCluster.get(clusterIndex).get(rpIndex).vector)));
        }

        long t = System.currentTimeMillis();

        int iterationCount = 0;
        float learningRate = 0.1f;
        do {
            int signalClusterIndex = rng.nextInt(protoVectors.size());
            int positionClusterIndex = labels.get(signalClusterIndex);
            int rpIndex = rng.nextInt(positionCluster.get(positionClusterIndex).size());


            ReferencePoint rp = positionCluster.get(positionClusterIndex).get(rpIndex);
            ArrayList<Float> vector = rp.vector;

            float minDis = Float.MAX_VALUE;
            int minProtoVectorIndex = 0;
            for (int i = 0; i < protoVectors.size(); i++) {
                ArrayList<Float> protoVector = protoVectors.get(i).vector;

                float sum = 0;
                for (int j = 0; j < protoVector.size(); j++) {
                    float diff = protoVector.get(j) - vector.get(j);

                    sum += diff * diff;
                }

                float dis = (float) Math.sqrt(sum);

                if (minDis > dis) {
                    minDis = dis;
                    minProtoVectorIndex = i;
                }
            }

            ArrayList<Float> minProtoVector = protoVectors.get(minProtoVectorIndex).vector;

            float sameCluster = positionClusterIndex == labels.get(minProtoVectorIndex) ? 1f : -1f;
            for (int i = 0; i < minProtoVector.size(); i++) {
                minProtoVector.set(i, minProtoVector.get(i) +
                        sameCluster * (vector.get(i) - minProtoVector.get(i)) * learningRate);
            }

            iterationCount++;

        } while (iterationCount <= 100);

        System.out.println(System.currentTimeMillis() - t);

        for (ReferencePoint rp : rps){
            ArrayList<Float> vector = rp.vector;

            float minDis = Float.MAX_VALUE;
            int minProtoVector = 0;
            for (int i = 0; i < protoVectors.size(); i++){
                ArrayList<Float> protoVector = protoVectors.get(i).vector;

                float sum = 0;
                for (int j = 0; j < protoVector.size(); j++){
                    float diff = protoVector.get(j) - vector.get(j);

                    sum += diff * diff;
                }

                float dis = (float) Math.sqrt(sum);

                if (minDis > dis){
                    minDis = dis;
                    minProtoVector = i;
                }
            }

            protoVectors.get(minProtoVector).rps.add(rp);
        }

        return protoVectors;
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

    public static ArrayList<ReferencePoint> getNearestClusterRps(ArrayList<ProtoCluster> signalCluster, ArrayList<Float> ssids){
//        float minDis = Float.MAX_VALUE;
//        int minClusterIndex = 0;
        IntFloatPair[] distances = new IntFloatPair[signalCluster.size()];
        for (int i = 0; i < signalCluster.size(); i++){
            ArrayList<Float> protoVector = signalCluster.get(i).vector;

            float sum = 0;
            for (int j = 0; j < protoVector.size(); j++){
                float diff = protoVector.get(j) - ssids.get(j);

                sum += diff * diff;
            }

            float dis = (float) Math.sqrt(sum);

            distances[i] = new IntFloatPair(i, dis);

            ArrayList<ReferencePoint> rps = signalCluster.get(i).rps;
            System.out.print(dis + " with: ");
            for (int j = 0; j < rps.size(); j++){
                System.out.print(rps.get(j).name + " ");
            }
            System.out.println();

//            if (minDis > dis){
//                minDis = dis;
//                minClusterIndex = i;
//            }
        }

        Arrays.sort(distances, (intFloatPair, t1) -> Float.compare(intFloatPair.floatVal, t1.floatVal));

        float minDis = distances[0].floatVal;
        ArrayList<ReferencePoint> rps = new ArrayList<>(signalCluster.get(distances[0].intVal).rps);

        if (signalCluster.size() > 1){
            float secDis = distances[1].floatVal;

            if (secDis / minDis < 1.4f){
                rps.addAll(signalCluster.get(distances[1].intVal).rps);
            }
        }

        return rps;
        // return signalCluster.get(minClusterIndex).rps;
    }

    private static class IntFloatPair{
        int intVal;
        float floatVal;

        public IntFloatPair(int intVal, float floatVal){
            this.intVal = intVal;
            this.floatVal = floatVal;
        }
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

            DistanceInfo distance = new DistanceInfo(rp.name, (float)Math.sqrt(sum), rp.coordinateX, rp.coordinateY, pastFoundNum, notFoundNum, pastNotFoundNum, foundNum);

            for (ReferencePoint other : rps){
                if (!other.equals(rp)){
                    int overlapCount = 0;

                    for (int j = 0; j < rp.vector.size(); j++){
                        if (rp.vector.get(j) == -100 && ssids.get(j) != -100 &&
                                other.vector.get(j) == -100){
                            overlapCount++;
                        }
                    }

                    distance.newSameOverlapPercent.add(new DistanceInfo.Overlap(other.name, overlapCount));
                }
            }

            distance.newSameOverlapPercent.sort((lhs, rhs) -> -Integer.compare(lhs.count, rhs.count));

            distances.add(distance);
        }

        return distances;
    }

    public void calculateResult(int changeCode){
        if (originalResults == null) return;

        //new Thread(() ->{
            this.results = getSelectedApResults(originalResults, accessPoints);

            positionClusters = getCoordinateClustering(fingerprint);

            signalClusters = getSignalClustering(fingerprint, positionClusters);

            ArrayList<Float> ssids = getVector(results, accessPoints);

            ArrayList<ReferencePoint> nearestRps = getNearestClusterRps(signalClusters, ssids);

            ArrayList<DistanceInfo> distances = getDistances(nearestRps, ssids);

            this.originalDistances = new ArrayList<>(distances);

            //activity.runOnUiThread(() ->{
                refresh(changeCode);
            //});
        //}).start();
    }

    public void setResult(ArrayList<WifiResult> results){
        this.originalResults = results;

        calculateResult(WIFI_RESULT_CHANGED);
    }

    private void updateFunction(){
        if (originalDistances != null){
            resetDistancesWeight();

            this.displayDistances = getDisplays(originalDistances, displayFunction);
            this.highlightDistances = getHighlights(originalDistances, ConfigManager.getInstance().kNearest, highlightFunction);

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
        return function.highlight(distances, Math.min(distances.size(), k));
    }

    public static ArrayList<DistanceInfo> getDisplays(ArrayList<DistanceInfo> distances, DisplayFunction function){
        return function.display(distances);
    }

    public static Coordinate getPredictCoordinate(ArrayList<DistanceInfo> highlights){
        Coordinate predict = new Coordinate();

        if (highlights.size() == 0) return predict;

        for (int i = 0; i < highlights.size(); i++){
            DistanceInfo info = highlights.get(i);

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

    public void setHighlightFunction(String name){
        highlightFunction = ConfigManager.getInstance().highlightFunctions.get(name);

        highlightFunctionName = name;

        refresh(HIGHLIGHT_FUNCTION_CHANGED);
    }

    public void setDisplayFunction(String name){
        displayFunction = ConfigManager.getInstance().displayFunctions.get(name);

        displayFunctionName = name;

        refresh(DISPLAY_FUNCTION_CHANGED);
    }

    public void setWeightFunction(String name){
        weightFunction = ConfigManager.getInstance().weightFunctions.get(name);

        weightFunctionName = name;

        refresh(WEIGHT_FUNCTION_CHANGED);
    }


    public String getCurrentMethodName(){
        return String.format("%s/%s/%s", apValuesName, highlightFunctionName, weightFunctionName);
    }

    public int getCurrentHighlightFunctionIndex(){
        return ConfigManager.getInstance().getHighlightFunctionIndex(highlightFunctionName);
    }

    public int getCurrentDisplayFunctionIndex(){
        Set<String> keys = ConfigManager.getInstance().displayFunctions.keySet();

        int index = 0;
        for (String key : keys){
            if (displayFunctionName.equals(key)){
                return index;
            }

            index++;
        }

        return -1;
    }

    public int getCurrentWeightFunctionIndex(){
        return ConfigManager.getInstance().getWeightFunctionIndex(weightFunctionName);
    }

    //endregion

    public static class ProtoCluster{
        public ArrayList<ReferencePoint> rps;
        public ArrayList<Float> vector;

        public ProtoCluster(ArrayList<ReferencePoint> rps, ArrayList<Float> vector){
            this.rps = rps;
            this.vector = vector;
        }
    }
}

