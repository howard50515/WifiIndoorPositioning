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

    public ArrayList<DistanceInfo> getDistances(ArrayList<WifiResult> results){
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

        return distances;
    }

    public class Coordinate{
        public float x, y;
    }
}

