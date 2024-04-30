package com.example.wifiindoorpositioning;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.wifiindoorpositioning.datatype.ApDistanceInfo;
import com.example.wifiindoorpositioning.datatype.DistanceInfo;
import com.example.wifiindoorpositioning.datatype.ReferencePoint;
import com.example.wifiindoorpositioning.datatype.TestPoint;
import com.example.wifiindoorpositioning.datatype.TestPointInfo;
import com.example.wifiindoorpositioning.datatype.WifiResult;
import com.example.wifiindoorpositioning.manager.ApDataManager;
import com.example.wifiindoorpositioning.manager.ConfigManager;
import com.example.wifiindoorpositioning.manager.SystemServiceManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ContentDebugView extends ScrollView {
    private MainActivity activity;
    private final Context context;
    private final ArrayList<InfoDisplayView> displayViews = new ArrayList<>();
    private Spinner testPointSpinner, referencePointSpinner;

    private LinearLayout body, apDistanceInfoControlPanel, wifiResultControlPanel;
    private TextView txtWait, txtTestPoint, txtReferencePoint;
    private HighlightButton btCopy;

    private String mode = "無";
    private TestPoint testPoint;

    private TestPointInfo testPointInfo;

    private static final int maxDisplayCount = 30;

    public ContentDebugView(Context context) {
        super(context);
        this.context = context;
        initView();
    }

    public ContentDebugView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
//        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.WifiInfoView);
//        getValues(typedArray);
        initView();
    }

    private void initView() {
        inflate(context, R.layout.window_contentdisplayview, this);
        body = findViewById(R.id.body);
        apDistanceInfoControlPanel = findViewById(R.id.apDistanceInfoControlPanel);
        wifiResultControlPanel= findViewById(R.id.wifiResultControlPanel);
        testPointSpinner = findViewById(R.id.testPointSpinner1);
        referencePointSpinner = findViewById(R.id.referencePointSpinner);
        txtTestPoint = findViewById(R.id.txtTestPoint);
        txtReferencePoint = findViewById(R.id.txtReferencePoint);
        btCopy = findViewById(R.id.btCopy);

        txtWait = new TextView(context);
        txtWait.setText("計算中...");
        txtWait.setTextSize(16);
        txtWait.setGravity(Gravity.CENTER);
        txtWait.setPadding(0, 10, 0, 0);
        txtWait.setTextColor(Color.RED);
        txtWait.setVisibility(GONE);

        body.addView(txtWait);

        testPointSpinner.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, ConfigManager.getInstance().getAllTestPointNames()));
        testPointSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (ignoreChanged){
                    ignoreChanged = false;
                }
                else{
                    ignoreChanged = true;

                    setTestPoint(ConfigManager.getInstance().getTestPointAtIndex(testPointSpinner.getSelectedItemPosition()));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        setReferencePoints();
        setReferencePoint(ApDataManager.getInstance().fingerprint.get(0));
        referencePointSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (!fromApChanged) {
                    setReferencePoint(ApDataManager.getInstance().fingerprint.get(i));
                }
                else{
                    fromApChanged = false;
                    referencePointSpinner.setSelection(getReferencePointIndex(compareReferencePoint));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        btCopy.setOnButtonDownListener(new HighlightButton.OnButtonDownListener() {
            @Override
            public void buttonDown() {
                if (testPointInfo != null){
                    TestPointInfo output = new TestPointInfo(testPointInfo.testPoint, new ArrayList<>(testPointInfo.values), testPointInfo.results);

                    output.values.sort(ApDistanceInfo.nameComparator);

                    SystemServiceManager.getInstance().toClipBoard(new TPInfo(output));
                }
            }
        });

        ApDataManager.getInstance().registerOnResultChangedListener((code) -> {
            if (code == ApDataManager.AP_VALUE_CHANGED || code == ApDataManager.UNCERTAIN_CHANGED){
                setReferencePoints();
            }
            else{
                refresh(code);
            }
        });
    }

    public void setMainActivity(MainActivity activity){
        this.activity = activity;
    }

    @SuppressLint("DefaultLocale")
    public void setTestPoint(TestPoint testPoint){
        this.testPoint = testPoint;

        if (testPointInfo != null)
            testPointInfo.testPoint = testPoint;

        txtTestPoint.setText(String.format("%s\n(%.2f, %.2f)", testPoint.name, testPoint.coordinateX, testPoint.coordinateY));

        if (ignoreChanged) {
            ignoreChanged = false;
        }
        else if (!testPoint.name.equals(testPointSpinner.getSelectedItem())){
            testPointSpinner.setSelection(ConfigManager.getInstance().getTestPointIndex(testPoint.name));
            ignoreChanged = true;
        }

        refresh(ApDataManager.TEST_POINT_CHANGED);

        if (listener != null) listener.pointChange(testPoint.coordinateX, testPoint.coordinateY);
    }

    private boolean ignoreChanged = false;

    public void setTestPoint(float x, float y){
        setTestPoint(new TestPoint("自定義", x, y));
    }

    public void setMode(String mode){
        this.mode = mode;

        refresh();
    }

    public void refresh(){
        switch (mode){
            case "無":
                hideAllInfo();
                break;
            case "參考點距離":
                displayDistanceInfo();
                break;
            case "訊號強度":
                displayWifiResult();
                break;
            case "存取點距離":
                displayApDistanceInfo();
                break;
        }
    }

    public void refresh(int code){
        if (!mode.equals("存取點距離")){
            refresh();
        }
        else if (code == ApDataManager.WIFI_RESULT_CHANGED ||
                code == ApDataManager.UNCERTAIN_CHANGED){
            refresh();
        }
        else if (code == ApDataManager.TEST_POINT_CHANGED){
            recalculateApDistanceInfo();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    public void hideAllInfo(){
        wifiResultControlPanel.setVisibility(GONE);
        apDistanceInfoControlPanel.setVisibility(GONE);

        for (InfoDisplayView displayView : displayViews){
            displayView.setVisibility(GONE);
            displayView.setBackgroundColor(Color.WHITE);
        }
    }

    private static final int highlightColor = Color.valueOf(0, 1, 0, 0.6f).toArgb();

    public void displayDistanceInfo(){
        hideAllInfo();

        ArrayList<DistanceInfo> distances = ApDataManager.getInstance().displayDistances;
        ArrayList<DistanceInfo> highlights = ApDataManager.getInstance().highlightDistances;

        if (distances == null) return;

        for (int i = 0; i < distances.size(); i++){
            DistanceInfo distance = distances.get(i);

            boolean isHighlight = false;
            for (DistanceInfo highlight : highlights){
                if (distance.rpName.equals(highlight.rpName)){
                    isHighlight = true;
                    break;
                }
            }

            if (displayViews.size() <= i) {
                InfoDisplayView displayView = addInitInfoDisplayView();
                displayView.setInfo(distances.get(i));
                if (isHighlight) displayView.setBackgroundColor(highlightColor);
            } else {
                displayViews.get(i).setInfo(distances.get(i));
                if (isHighlight) displayViews.get(i).setBackgroundColor(highlightColor);
                displayViews.get(i).setVisibility(VISIBLE);
            }
        }
    }

    private ReferencePoint compareReferencePoint;
    private boolean fromApChanged;

    private void setReferencePoints(){
        referencePointSpinner.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item,
                ApDataManager.getInstance().fingerprint.stream().map(rp -> rp.name).toArray()));

        fromApChanged = true;

        if (compareReferencePoint == null) return;

        int index = getReferencePointIndex(compareReferencePoint);
        if (index != -1){
            setReferencePoint(ApDataManager.getInstance().fingerprint.get(index));
        }
        else{
            setReferencePoint(ApDataManager.getInstance().fingerprint.get(0));
        }
    }
    @SuppressLint("DefaultLocale")
    private void setReferencePoint(ReferencePoint referencePoint){
        int index = getReferencePointIndex(referencePoint);

        if (index == -1) return;

        compareReferencePoint = referencePoint;

        txtReferencePoint.setText(String.format("%s (%.2f, %.2f)", compareReferencePoint.name, compareReferencePoint.coordinateX, compareReferencePoint.coordinateY));

        refresh();
    }

    private int getReferencePointIndex(ReferencePoint rp){
        ArrayList<ReferencePoint> referencePoints = ApDataManager.getInstance().fingerprint;

        for (int i = 0; i < referencePoints.size(); i++){
            if (rp.name.equals(referencePoints.get(i).name)){
                return i;
            }
        }

        return -1;
    }

    public void displayWifiResult(){
        hideAllInfo();

        System.out.println("HI");

        if (ApDataManager.getInstance().results == null) return;

        wifiResultControlPanel.setVisibility(VISIBLE);

        ArrayList<WifiResult> results = new ArrayList<>(ApDataManager.getInstance().results);

        ArrayList<Float> rpLevels = compareReferencePoint.vector;

        for (int i = 0;i < results.size(); i++){
            WifiResult result = results.get(i);

            result.rpLevel = rpLevels.get(i);
        }

        results.sort((lhs, rhs) -> -Float.compare(Math.abs(lhs.level - lhs.rpLevel), Math.abs(rhs.level - rhs.rpLevel)));

        for (int i = 0; i < results.size(); i++){
            if (results.get(i).apId.contains("NCUCE_2.4G:"))
                continue;

            if (displayViews.size() <= i) {
                InfoDisplayView displayView = addInitInfoDisplayView();
                displayView.setInfo(results.get(i));
            } else {
                displayViews.get(i).setInfo(results.get(i));
                displayViews.get(i).setVisibility(VISIBLE);
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    public void displayApDistanceInfo(){
        hideAllInfo();

        txtWait.setText("計算中...");
        txtWait.setVisibility(VISIBLE);
        apDistanceInfoControlPanel.setVisibility(VISIBLE);

        Thread thread = new Thread(() ->{
            ArrayList<ApDistanceInfo> apDistances = ApDataManager.getInstance().getAllApDistances();

            if (apDistances != null){
                for (int i = 0; i < apDistances.size(); i++) {
                    ApDistanceInfo apDistance = apDistances.get(i);
                    float x = testPoint.coordinateX - apDistance.x;
                    float y = testPoint.coordinateY - apDistance.y;
                    apDistance.distance = (float) Math.sqrt(x * x + y * y);
                }

                apDistances.sort(ApDistanceInfo.distanceComparator);

                System.out.println(apDistances.size());

                testPointInfo = new TestPointInfo(testPoint, apDistances, ApDataManager.getInstance().originalResults);
            }

            activity.runOnUiThread(() ->{
                txtWait.setText("無資料");

                if (apDistances == null) return;

                txtWait.setVisibility(GONE);

                display(apDistances);
            });
        });

        thread.start();
    }

    @SuppressLint("ClickableViewAccessibility")
    public void recalculateApDistanceInfo(){
        if (testPointInfo == null) return;

        hideAllInfo();

//        txtWait.setText("計算中...");
//        txtWait.setVisibility(VISIBLE);
        apDistanceInfoControlPanel.setVisibility(VISIBLE);

        //new Thread(()->{
            ArrayList<ApDistanceInfo> apDistances = new ArrayList<>(testPointInfo.values);

            for (int i = 0; i < apDistances.size(); i++) {
                ApDistanceInfo apDistance = apDistances.get(i);
                float x = testPoint.coordinateX - apDistance.x;
                float y = testPoint.coordinateY - apDistance.y;
                apDistance.distance = (float) Math.sqrt(x * x + y * y);
            }

            apDistances.sort(ApDistanceInfo.distanceComparator);

            //activity.runOnUiThread(()->{
                // txtWait.setVisibility(GONE);

                display(apDistances);
            //});
        //}).start();
    }

    private void display(ArrayList<ApDistanceInfo> apDistances){
        /// if (!mode.equals("存取點距離")) return;

        int displayCount = Math.min(apDistances.size(), maxDisplayCount);

        for (int i = 0; i < displayCount; i++){
            ApDistanceInfo apDistance = apDistances.get(i);

            InfoDisplayView displayView;

            if (displayViews.size() <= i) {
                displayView = addInitInfoDisplayView();
            } else {
                displayView = displayViews.get(i);
                displayViews.get(i).setVisibility(VISIBLE);
            }

            displayView.setInfo(apDistance);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private InfoDisplayView addInitInfoDisplayView(){
        InfoDisplayView displayView = new InfoDisplayView(context);
        displayView.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_UP){
                if (mode.equals("存取點距離")){
                    activity.setApValueFunctions(displayView.apDistance.apValueName,
                            displayView.apDistance.highlightFunctionName, displayView.apDistance.weightFunctionName);
                }
            }

            return true;
        });
        displayViews.add(displayView);
        body.addView(displayView);

        return displayView;
    }

    private OnTestPointChangedListener listener;

    public void setOnTestPointChangedListener(OnTestPointChangedListener listener){
        this.listener = listener;
    }

    public interface OnTestPointChangedListener{
        void pointChange(float x, float y);
    }

    public class TPInfo{
        // 測試點資訊
        public TestPoint testPoint;

        // 不同 ap, highlightFunction, weightFunction 算出的結果
        public ArrayList<TestPointApInfo> values;

        // 原始 WiFi 數據
        public ArrayList<WifiResult> results;

        public TPInfo(TestPointInfo testPointInfo){
            this.testPoint = testPointInfo.testPoint;
            this.results = testPointInfo.results;

            values = new ArrayList<>();

            for (String apValueName : ConfigManager.getInstance().apValues){
                TestPointApInfo testPointApInfo = new TestPointApInfo(apValueName);

                for (ApDistanceInfo apDistance : testPointInfo.values){
                    if (apValueName.equals(apDistance.apValueName)){
                        testPointApInfo.functions.add(new TestPointFunctionInfo(apDistance.highlightFunctionName,
                                apDistance.weightFunctionName, apDistance.x, apDistance.y, apDistance.distance));
                    }
                }

                testPointApInfo.functions.sort(comparator);

                values.add(testPointApInfo);
            }
        }
    }

    public class TestPointApInfo{
        public String apValueName;

        public ArrayList<TestPointFunctionInfo> functions;

        public TestPointApInfo(String apValueName){
            this.apValueName = apValueName;

            functions = new ArrayList<>();
        }
    }

    public class TestPointFunctionInfo{
        public final String[] names = new String[2];
        public transient String highlightFunctionName, weightFunctionName;

        public transient float x, y;

        public float distance;

        public TestPointFunctionInfo(String highlightFunctionName, String weightFunctionName, float x, float y, float distance){
            this.highlightFunctionName = highlightFunctionName;
            this.weightFunctionName = weightFunctionName;
            this.x = x;
            this.y = y;
            this.distance = distance;

            names[0] = highlightFunctionName;
            names[1] = weightFunctionName;
        }
    }

    public static Comparator<TestPointFunctionInfo> comparator = new Comparator<TestPointFunctionInfo>() {
        @Override
        public int compare(TestPointFunctionInfo lhs, TestPointFunctionInfo rhs) {
            int c = lhs.highlightFunctionName.compareTo(rhs.highlightFunctionName);

            if (c != 0) return c;

            return lhs.weightFunctionName.compareTo(rhs.weightFunctionName);
        }
    };
}
