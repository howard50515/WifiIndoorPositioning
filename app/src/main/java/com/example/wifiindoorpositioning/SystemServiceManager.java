package com.example.wifiindoorpositioning;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.rtt.WifiRttManager;
import android.os.Build;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;

public class SystemServiceManager implements SensorEventListener {
    public WifiManager wifiManager;
    public SensorManager sensorManager;
    public Sensor accelerateSensor, magneticSensor;
    public float[] accelerateValues = new float[3], magneticValues = new float[3];

    private Activity context;
    private BroadcastReceiver receiver;
    private boolean permission;

    public SystemServiceManager(AppCompatActivity context){
        this.context = context;

        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);


        permission = ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (!permission) {
            ActivityCompat.requestPermissions(context, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(context, new String[]{Manifest.permission.NEARBY_WIFI_DEVICES}, 0);
        }
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                    boolean success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
                    receiveScan(success);
                }
            }
        };


        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelerateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    public static final int CODE_SUCCESS = 0;
    public static final int CODE_NO_LOCATION = 1;
    public static final int CODE_NO_PERMISSION = 2;
    public static final int CODE_TOO_FREQUENT = 3;

    private OnScanCompleteCallback completeCallback;

    public void scan(OnScanCompleteCallback callback){
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            intentFilter.addAction(WifiRttManager.ACTION_WIFI_RTT_STATE_CHANGED);
        }
        context.registerReceiver(receiver, intentFilter);
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            callback.complete(CODE_NO_PERMISSION, null);
            return;
        }

        boolean scanSymbol = wifiManager.startScan();

        if (!scanSymbol){
            callback.complete(CODE_NO_LOCATION, null);
        }
        else{
            completeCallback = callback;
        }
    }

    private void receiveScan(boolean success) {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            completeCallback.complete(CODE_NO_PERMISSION, null);
            return;
        }
        if (!success) {
            completeCallback.complete(CODE_TOO_FREQUENT, null);
            return;
        }
        ArrayList<WifiResult> wifiResults = new ArrayList<>();
        List<ScanResult> results = wifiManager.getScanResults();
        results.sort((a, b) -> b.level - a.level);
        for (ScanResult result : results) {
            wifiResults.add(new WifiResult(result));
        }

        completeCallback.complete(CODE_SUCCESS, wifiResults);
    }

    public void registerSensor(){
        sensorManager.registerListener(this, accelerateSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, magneticSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void unregisterSensor(){
        sensorManager.unregisterListener(this, accelerateSensor);
        sensorManager.unregisterListener(this, magneticSensor);
    }

    private final float[] r = new float[9];
    private final float[] values = new float[3];

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            accelerateValues = event.values.clone();
        }
        else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            magneticValues = event.values.clone();
        }

        SensorManager.getRotationMatrix(r, null, accelerateValues, magneticValues);
        SensorManager.getOrientation(r, values);

        listener.orientationChanged((float)Math.toDegrees(values[0]));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {}

    public interface OnScanCompleteCallback {
        void complete(int code, ArrayList<WifiResult> results);
    }

    private OnOrientationChangedListener listener;

    public void setOnOrientationChangedListener(OnOrientationChangedListener listener){
        this.listener = listener;
    }

    public interface OnOrientationChangedListener{
        void orientationChanged(float degree);
    }
}
