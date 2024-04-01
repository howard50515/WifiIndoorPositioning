package com.example.wifiindoorpositioning;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.graphics.PointF;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private SystemServiceManager systemServiceManager;

    private ImageView imgCompass;
    private TextView txtOrientation, txtStatus, txtDistance;
    private EditText inputAcceptDifference;
    private ZoomableImageView mapImage;
    private Spinner debugModeSpinner, apValueModeSpinner, highlightModeSpinner, displayModeSpinner, weightModeSpinner;
    private ContentDebugView contentView;
    private HighlightButton btScan, btSettings;
    private SettingsView settingsView;

    private float acceptDifference = 0.4f;

    @SuppressLint({"ClickableViewAccessibility", "SetTextI18n", "DefaultLocale"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ApDataManager.createInstance(this);

        setContentView(R.layout.activity_main);

        imgCompass = findViewById(R.id.imgCompass);
        txtOrientation = findViewById(R.id.orientation);
        txtStatus = findViewById(R.id.txtStatus);
        txtDistance = findViewById(R.id.txtDistance);
        mapImage = findViewById(R.id.zoomableView);
        btScan = findViewById(R.id.btScan);
        btSettings = findViewById(R.id.btSettings);
        inputAcceptDifference = findViewById(R.id.inputAcceptDifference);
        debugModeSpinner = findViewById(R.id.debugModeSpinner);
        apValueModeSpinner = findViewById(R.id.apValueModeSpinner);
        highlightModeSpinner = findViewById(R.id.highlightModeSpinner);
        displayModeSpinner = findViewById(R.id.displayModeSpinner);
        weightModeSpinner = findViewById(R.id.weightModeSpinner);
        contentView = findViewById(R.id.contentView);
        settingsView = new SettingsView(this);

        ApDataManager.getInstance().addHighlightFunction("距離前40%", new ApDataManager.HighlightFunction() {
            @Override
            public ArrayList<DistanceInfo> highlight(ArrayList<DistanceInfo> distances, int k) {
                // distances 每個參考點到目前位置的預測資訊
                // k 也可以先不管就先挑你覺得效果好的
                // for (int i = 0; i < distances.size(); i++){
                    // 拿到單一參考點資訊
                    // 可以到 DistanceInfo 看一下有什麼變數
                    // DistanceInfo distance = distances.get(i);
                    /* 透過距離、loss rate 等，決定最後highlight的參考點 */
                // }

                ArrayList<DistanceInfo> highlight = new ArrayList<>(distances);

                highlight.sort(DistanceInfo.distanceComparable);

                if (highlight.size() <= 1) return highlight;

                float dis = highlight.get(0).distance;
                int maxK = Math.min(highlight.size(), 5);
                int i = 1;
                while (i < maxK){
                    if ((highlight.get(i).distance / dis) - 1 > acceptDifference){
                        break;
                    }

                    i++;
                }

                int dynamicK = i;

                // 回傳想要highlight的點
                return new ArrayList<>(highlight.subList(0, dynamicK));

                // 目前為預測距離最近的k個
                // return ApDataManager.getInstance().defaultHighlightAndDisplay.highlight(distances, k);
            }
        });
        ApDataManager.getInstance().setHighlightFunction("距離前40%");
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

            float multiplier =  ((max / min) - 1) / acceptDifference;

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

        inputAcceptDifference.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                try{
                    acceptDifference = Float.parseFloat(inputAcceptDifference.getText().toString());

                    ApDataManager.getInstance().calculateResult();
                } catch (NumberFormatException ignored) {}
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        apValueModeSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, ApDataManager.getInstance().apChoices));
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

        mapImage.setReferencePoints(ApDataManager.getInstance().fingerprint);
        mapImage.setNorthOffset(180);
        mapImage.setOnImagePointChangedListener(new ZoomableImageView.OnImagePointChangedListener() {
            @Override
            public void pointChange(float x, float y) {
                PointF p = mapImage.getFingerPoint();
                float diffX = x - p.x;
                float diffY = y - p.y;

                txtDistance.setText(String.format("%.2f", Math.sqrt(diffX * diffX + diffY * diffY)));
            }
        });
        mapImage.setOnFingerPointChangedListener(new ZoomableImageView.OnFingerPointChangedListener() {
            @Override
            public void pointChange(float x, float y) {
                PointF p = mapImage.getImagePoint();
                float diffX = x - p.x;
                float diffY = y - p.y;

                txtDistance.setText(String.format("%.2f", Math.sqrt(diffX * diffX + diffY * diffY)));
            }
        });

        systemServiceManager = new SystemServiceManager(this);

        btScan.setOnButtonDownListener(() -> {
            txtStatus.setText("掃描中...");
            systemServiceManager.scan((code, results) -> {
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

        systemServiceManager.setOnOrientationChangedListener(degree -> {
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

        systemServiceManager.registerSensor();
    }

    @Override
    protected void onPause() {
        super.onPause();

        systemServiceManager.unregisterSensor();
    }
    //endregion
}