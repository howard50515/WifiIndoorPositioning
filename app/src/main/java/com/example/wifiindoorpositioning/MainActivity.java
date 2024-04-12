package com.example.wifiindoorpositioning;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.graphics.PointF;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.wifiindoorpositioning.datatype.DistanceInfo;
import com.example.wifiindoorpositioning.function.DistanceRateHighlightFunction;
import com.example.wifiindoorpositioning.manager.ApDataManager;
import com.example.wifiindoorpositioning.manager.ConfigManager;
import com.example.wifiindoorpositioning.manager.SystemServiceManager;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private ImageView imgCompass;
    private TextView txtOrientation, txtStatus, txtDistance;
    // private EditText inputAcceptDifference;
    private ZoomableImageView mapImage;
    private Spinner debugModeSpinner, apValueModeSpinner, highlightModeSpinner, displayModeSpinner, weightModeSpinner;
    private ContentDebugView contentView;
    private HighlightButton btScan, btSettings;
    private SettingsView settingsView;

    // private final Float[] acceptDifference = new Float[] { 0.4f };

    @SuppressLint({"ClickableViewAccessibility", "SetTextI18n", "DefaultLocale"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ConfigManager.createInstance(this);
        ApDataManager.createInstance(this);
        SystemServiceManager.createInstance(this);

        setContentView(R.layout.activity_main);

        imgCompass = findViewById(R.id.imgCompass);
        txtOrientation = findViewById(R.id.orientation);
        txtStatus = findViewById(R.id.txtStatus);
        txtDistance = findViewById(R.id.txtDistance);
        mapImage = findViewById(R.id.zoomableView);
        btScan = findViewById(R.id.btScan);
        btSettings = findViewById(R.id.btSettings);
        // inputAcceptDifference = findViewById(R.id.inputAcceptDifference);
        debugModeSpinner = findViewById(R.id.debugModeSpinner);
        apValueModeSpinner = findViewById(R.id.apValueModeSpinner);
        highlightModeSpinner = findViewById(R.id.highlightModeSpinner);
        displayModeSpinner = findViewById(R.id.displayModeSpinner);
        weightModeSpinner = findViewById(R.id.weightModeSpinner);
        contentView = findViewById(R.id.contentView);
        settingsView = new SettingsView(this);

        ApDataManager.getInstance().addHighlightFunction("距離前20%", new DistanceRateHighlightFunction(0.2f));
        ApDataManager.getInstance().addHighlightFunction("距離前30%", new DistanceRateHighlightFunction(0.3f));
        ApDataManager.getInstance().addHighlightFunction("距離前40%", new DistanceRateHighlightFunction(0.4f));
        ApDataManager.getInstance().addHighlightFunction("取低loss rate", new ApDataManager.HighlightFunction() {
            @Override
            public ArrayList<DistanceInfo> highlight(ArrayList<DistanceInfo> distances, int k) {
                ArrayList<DistanceInfo> sortDistances = new ArrayList<>(distances);

                sortDistances.sort(DistanceInfo.distanceComparable);

//                for (int i = 0 ; i < sortDistances.size(); i++) {
//                    if (sortDistances.get(i).rpName.equals("102"))
//                        Log.i(sortDistances.get(i).rpName, String.valueOf(sortDistances.get(i).distance));
//                }
//
//                Log.i("", distances.get(0).rpName);

                if (sortDistances.size() <= 1) return sortDistances;

                //System.out.printf(distances.get(0).rpName);

                float[] LR = new float[sortDistances.size()];
                int notFoundNum = sortDistances.get(0).notFoundNum;
                int pastFoundNum = sortDistances.get(0).pastFoundNum;
                LR[0] = (float) notFoundNum / pastFoundNum;

                for (int i = sortDistances.size() - 1; i > 0; i--) {
                    notFoundNum = sortDistances.get(i).notFoundNum;
                    pastFoundNum = sortDistances.get(i).pastFoundNum;
                    LR[i] = (float) notFoundNum / pastFoundNum;
                    if(LR[i] > LR[0]) {
                        sortDistances.remove(i);
                    }
                }


                return new ArrayList<>(sortDistances.subList(0, Math.min(k, sortDistances.size())));
            }
        });
        ApDataManager.getInstance().addHighlightFunction("取低new rate", new ApDataManager.HighlightFunction() {
            @Override
            public ArrayList<DistanceInfo> highlight(ArrayList<DistanceInfo> distances, int k) {
                ArrayList<DistanceInfo> sortDistances = new ArrayList<>(distances);

                sortDistances.sort(DistanceInfo.distanceComparable);

                if (sortDistances.size() <= 1) return sortDistances;

                float[] NR = new float[sortDistances.size()];
                int foundNum = sortDistances.get(0).foundNum;
                int pastNotFoundNum = sortDistances.get(0).pastNotFoundNum;
                NR[0] = (float) foundNum / pastNotFoundNum;

                for (int i = sortDistances.size() - 1; i > 0; i--) {
                    foundNum = sortDistances.get(i).foundNum;
                    pastNotFoundNum = sortDistances.get(i).pastNotFoundNum;
                    NR[i] = (float) foundNum / pastNotFoundNum;
                    if(NR[i] > NR[0]) {
                        sortDistances.remove(i);
                    }
                }

                return new ArrayList<>(sortDistances.subList(0, Math.min(k, sortDistances.size())));
            }
        });
        // ApDataManager.getInstance().addHighlightFunction("距離前?%", new DistanceRateHighlightFunction(acceptDifference));
        ApDataManager.getInstance().addWeightFunction("自訂權重", highlights -> {
            ArrayList<Float> weights = new ArrayList<>();
            if (highlights.size() == 0) return weights;

            if (highlights.size() == 1){
                weights.add(1f);

                return weights;
            }

            float first = highlights.get(0).distance;
            float totalRate = 0;
            float min = Float.MAX_VALUE, max = 0;
            for (int i = 0; i < highlights.size(); i++){
                float distance = highlights.get(i).distance;

                float rate = first / distance;

                totalRate += rate;

                if (min > distance){
                    min = distance;
                }
                else if (max < distance){
                    max = distance;
                }
            }

            float avgRate = totalRate / highlights.size();

            float multiplier =  ((max / min) - 1) / 0.4f;

            float total = 0;
            for (int i = 0; i < highlights.size(); i++){
                float rate = first / highlights.get(i).distance;

                total += Math.abs(avgRate - rate);
            }

            float baseRate = 1f / highlights.size();

            for (int i = 0; i < highlights.size(); i++){
                DistanceInfo info = highlights.get(i);

                float diff = avgRate - first / info.distance;

                weights.add(baseRate - (Math.abs(diff) / total) * (diff > 0 ? 1 : -1) * baseRate * multiplier);
            }

            return weights;
        });
        ApDataManager.getInstance().addWeightFunction("WKNN", new ApDataManager.WeightFunction() {
            @Override
            public ArrayList<Float> weight(ArrayList<DistanceInfo> highlights) {
                ArrayList<Float> weights = new ArrayList<>();

                // 以下是WKNN
                float sum = 0; // 距離倒數的總和
                float sumOfSamplePoints_x = 0; // pi/Di
                float sumOfSamplePoints_y = 0;

                for (int i = 0; i < highlights.size(); i++){
                    DistanceInfo distance = highlights.get(i);
                    sum += 1/distance.distance;
//                    weights.add()
//                    sumOfSamplePoints_x += distance.coordinateX / distance.distance;
//                    sumOfSamplePoints_y += distance.coordinateY / distance.distance;
                    // predict.x += (distance.coordinateX / distance.distance) / sum;
                    // predict.y += (distance.coordinateY / distance.distance) / sum;
                }

                for (int i = 0; i < highlights.size(); i++){
                    DistanceInfo distance = highlights.get(i);
                    float weight = 1/distance.distance / sum;
                    weights.add(weight);
//                    sumOfSamplePoints_x += distance.coordinateX / distance.distance;
//                    sumOfSamplePoints_y += distance.coordinateY / distance.distance;
                    // predict.x += (distance.coordinateX / distance.distance) / sum;
                    // predict.y += (distance.coordinateY / distance.distance) / sum;
                }

//                predict.x = sumOfSamplePoints_x / sum;
//                predict.y = sumOfSamplePoints_y / sum;
//
//                // 回傳預測的座標
//                return predict;
                return weights;
            }
        });
        ApDataManager.getInstance().addWeightFunction("new WKNN", new ApDataManager.WeightFunction() {
            @Override
            public ArrayList<Float> weight(ArrayList<DistanceInfo> highlights) {
                ArrayList<Float> weights = new ArrayList<>();

                if (highlights.size() == 1){
                    weights.add(1f);

                    return weights;
                }

                // new WKNN
                float distance_sum = 0; // 距離差總和
                float threshold = 0; // 距離最遠的AP

                // 找出哪個AP距離最遠
                for(int i = 0; i < highlights.size(); i++){
                    if(highlights.get(i).distance > threshold){
                        threshold = highlights.get(i).distance;
                    }
                }

                for (int i = 0; i < highlights.size(); i++){
                    DistanceInfo distance = highlights.get(i);
                    distance_sum += (threshold - distance.distance);
                }

                for (int i = 0; i < highlights.size(); i++){
                    DistanceInfo distance = highlights.get(i);
                    float weight = (threshold - distance.distance) / distance_sum;
                    weights.add(weight);
                }

                return weights;
            }
        });

//        inputAcceptDifference.addTextChangedListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//
//            }
//
//            @Override
//            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//                try{
//                    acceptDifference[0] = Float.parseFloat(inputAcceptDifference.getText().toString());
//
//                    ApDataManager.getInstance().calculateResult(ApDataManager.HIGHLIGHT_FUNCTION_CHANGED);
//                } catch (NumberFormatException ignored) {}
//            }
//
//            @Override
//            public void afterTextChanged(Editable editable) {
//
//            }
//        });

        apValueModeSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, ConfigManager.getInstance().apValues));
        highlightModeSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, ApDataManager.getInstance().getAllHighlightFunctionNames()));
        displayModeSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, ApDataManager.getInstance().getAllDisplayFunctionNames()));
        weightModeSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, ApDataManager.getInstance().getAllWeightFunctionNames()));

        highlightModeSpinner.setSelection(ApDataManager.getInstance().getCurrentHighlightFunctionIndex());
        displayModeSpinner.setSelection(ApDataManager.getInstance().getCurrentDisplayFunctionIndex());
        weightModeSpinner.setSelection(ApDataManager.getInstance().getCurrentWeightFunctionIndex());

        apValueModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                ApDataManager.getInstance().loadApValueAtIndex(apValueModeSpinner.getSelectedItemPosition());
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        highlightModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                ApDataManager.getInstance().setHighlightFunction(highlightModeSpinner.getSelectedItem().toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        displayModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                ApDataManager.getInstance().setDisplayFunction(displayModeSpinner.getSelectedItem().toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        weightModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                ApDataManager.getInstance().setWeightFunction(weightModeSpinner.getSelectedItem().toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        contentView.setMainActivity(this);
        mapImage.setReferencePoints(ApDataManager.getInstance().fingerprint);
        mapImage.setNorthOffset(180);
        mapImage.setOnImagePointChangedListener(new ZoomableImageView.OnImagePointChangedListener() {
            @Override
            public void pointChange(float x, float y) {
                PointF p = mapImage.getFingerPoint();
                float diffX = x - p.x;
                float diffY = y - p.y;

                txtDistance.setText(String.format("%.2f, (%.2f, %.2f)", Math.sqrt(diffX * diffX + diffY * diffY), p.x, p.y));
            }
        });
        mapImage.setOnFingerPointChangedListener(new ZoomableImageView.OnFingerPointChangedListener() {
            @Override
            public void pointChange(float x, float y) {
                PointF p = mapImage.getImagePoint();
                float diffX = x - p.x;
                float diffY = y - p.y;

                txtDistance.setText(String.format("%.2f, (%.2f, %.2f)", Math.sqrt(diffX * diffX + diffY * diffY), x, y));

                contentView.setTestPoint(x, y);
            }
        });

        btScan.setOnButtonDownListener(() -> {
            txtStatus.setText("掃描中...");
            SystemServiceManager.getInstance().scan((code, results) -> {
                switch (code) {
                    case SystemServiceManager.CODE_SUCCESS:
                        txtStatus.setText("成功");
                        ApDataManager.getInstance().setResult(results);
                        break;
                    case SystemServiceManager.CODE_NO_LOCATION:
                        txtStatus.setText("未開啟位置");
                        break;
                    case SystemServiceManager.CODE_NO_PERMISSION:
                        txtStatus.setText("未提供權限");
                        break;
                    case SystemServiceManager.CODE_TOO_FREQUENT:
                        txtStatus.setText("過於頻繁");
                        break;
                    default:
                        txtStatus.setText("未知錯誤");
                        break;
                }
            });
        });
        btSettings.setOnButtonDownListener(() -> settingsView.showView(this));

        SystemServiceManager.getInstance().setOnOrientationChangedListener(degree -> {
            imgCompass.setRotation(degree);
            txtOrientation.setText(String.format("%.2f (%s)", degree, getDirection(degree)));
            mapImage.setLookAngle(degree);
        });

        debugModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String mode = debugModeSpinner.getSelectedItem().toString();

                contentView.setMode(mode);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    public void setApValueFunctions(String apValueName, String highlightFunctionName, String weightFunctionName){
        System.out.println(apValueName + " " + highlightFunctionName + " " + weightFunctionName);

        apValueModeSpinner.setSelection(ConfigManager.getInstance().getApValueIndex(apValueName));
        highlightModeSpinner.setSelection(ApDataManager.getInstance().getHighlightFunctionIndex(highlightFunctionName));
        weightModeSpinner.setSelection(ApDataManager.getInstance().getWeightFunctionIndex(weightFunctionName));
    }

    //region Sensor相關
    public String getDirection(float degree){
        float range = Math.abs(degree);
        if (range < 22.5){
            return "N";
        }
        else if (range < 67.5){
            return  (degree < 0) ? "NW" : "NE";
        }
        else if (range < 112.5){
            return  (degree < 0) ? "W" : "E";
        }
        else if (range < 135){
            return  (degree < 0) ? "W" : "E";
        }
        else if (range < 157.5){
            return  (degree < 0) ? "SW" : "SE";
        }

        return "S";
    }

    @Override
    protected void onResume() {
        super.onResume();

        SystemServiceManager.getInstance().registerSensor();
    }

    @Override
    protected void onPause() {
        super.onPause();

        SystemServiceManager.getInstance().unregisterSensor();
    }
    //endregion
}