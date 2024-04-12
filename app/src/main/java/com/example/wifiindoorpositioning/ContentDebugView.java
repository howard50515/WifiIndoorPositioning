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
import com.example.wifiindoorpositioning.datatype.TestPoint;
import com.example.wifiindoorpositioning.datatype.TestPointInfo;
import com.example.wifiindoorpositioning.datatype.WifiResult;
import com.example.wifiindoorpositioning.manager.ApDataManager;
import com.example.wifiindoorpositioning.manager.ConfigManager;
import com.example.wifiindoorpositioning.manager.SystemServiceManager;

import java.util.ArrayList;
import java.util.Comparator;

public class ContentDebugView extends ScrollView {
    private MainActivity activity;
    private final Context context;
    private final ArrayList<InfoDisplayView> displayViews = new ArrayList<>();
    private Spinner testPointSpinner;

    private LinearLayout body, apDistanceInfoControlPanel;
    private TextView txtWait, txtTestPoint;
    private HighlightButton btCopy;

    private String mode = "無";
    private TestPoint testPoint;

    private TestPointInfo testPointInfo;

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
        testPointSpinner = findViewById(R.id.testPointSpinner1);
        txtTestPoint = findViewById(R.id.txtTestPoint);
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
                ConfigManager.getInstance().setTestPointAtIndex(testPointSpinner.getSelectedItemPosition());

                setTestPoint(ConfigManager.getInstance().testPoint);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        btCopy.setOnButtonDownListener(new HighlightButton.OnButtonDownListener() {
            @Override
            public void buttonDown() {
                if (testPointInfo != null){
                    SystemServiceManager.getInstance().toClipBoard(new TPInfo(testPointInfo));
                }
            }
        });

        ApDataManager.getInstance().registerOnResultChangedListener(this::refresh);
        // ConfigManager.getInstance().registerOnConfigChangedListener(() -> testPointSpinner.setSelection(ConfigManager.getInstance().getCurrentTestPointIndex()));
    }

    public void setMainActivity(MainActivity activity){
        this.activity = activity;
    }

    @SuppressLint("DefaultLocale")
    public void setTestPoint(TestPoint testPoint){
        this.testPoint = testPoint;

        txtTestPoint.setText(String.format("%s\n(%.2f, %.2f)", testPoint.name, testPoint.coordinateX, testPoint.coordinateY));

        refresh(ApDataManager.TEST_POINT_CHANGED);
    }

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
                InfoDisplayView displayView = new InfoDisplayView(context);
                displayView.setInfo(distances.get(i));
                if (isHighlight) displayView.setBackgroundColor(highlightColor);
                displayViews.add(displayView);
                body.addView(displayView);
            } else {
                displayViews.get(i).setInfo(distances.get(i));
                if (isHighlight) displayViews.get(i).setBackgroundColor(highlightColor);
                displayViews.get(i).setVisibility(VISIBLE);
            }
        }
    }

    public void displayWifiResult(){
        hideAllInfo();

        ArrayList<WifiResult> results = ApDataManager.getInstance().results;

        if (results == null) return;

        for (int i = 0; i < results.size(); i++){
            if (displayViews.size() <= i) {
                InfoDisplayView displayView = new InfoDisplayView(context);
                displayView.setInfo(results.get(i));
                displayViews.add(displayView);
                body.addView(displayView);
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
                    float x = testPoint.coordinateX - apDistance.predictCoordinate.x;
                    float y = testPoint.coordinateY - apDistance.predictCoordinate.y;
                    apDistance.distance = (float) Math.sqrt(x * x + y * y);
                }


                apDistances.sort(ApDistanceInfo.distanceComparator);

                System.out.println(apDistances.size());

                testPointInfo = new TestPointInfo(ConfigManager.getInstance().testPoint, apDistances, ApDataManager.getInstance().originalResults);
            }

            activity.runOnUiThread(() ->{
                txtWait.setText("無資料");

                if (apDistances == null) return;

                txtWait.setVisibility(GONE);

                int displayCount = Math.min(apDistances.size(), 50);

                for (int i = 0; i < displayCount; i++){
                    ApDistanceInfo apDistance = apDistances.get(i);

                    InfoDisplayView displayView;

                    if (displayViews.size() <= i) {
                        displayView = new InfoDisplayView(context);
                        displayView.setOnTouchListener(new OnTouchListener() {
                            private boolean isOutside;

                            @Override
                            public boolean onTouch(View view, MotionEvent motionEvent) {
                                int action = motionEvent.getAction();
                                switch (action) {
                                    case MotionEvent.ACTION_DOWN:
                                        isOutside = false;
//                                    displayView.setBackgroundResource(R.drawable.background_rectangle_focus);
//                                    if (scrollView != null)
//                                        scrollView.requestDisallowInterceptTouchEvent(true);
                                        break;
                                    case MotionEvent.ACTION_MOVE:
                                        isOutside = motionEvent.getX() > displayView.getWidth() || motionEvent.getX() < 0 ||
                                                motionEvent.getY() > displayView.getHeight() || motionEvent.getY() < 0;
//                                    if (!isOutside)
//                                        displayView.setBackgroundResource(R.drawable.background_rectangle_focus);
//                                    else
//                                        displayView.setBackgroundResource(R.drawable.background_rectangle_transparent_unfocus);
                                        break;
                                    case MotionEvent.ACTION_UP:
//                                    displayView.setBackgroundResource(R.drawable.background_rectangle_transparent_unfocus);
//                                    if (scrollView != null)
//                                        scrollView.requestDisallowInterceptTouchEvent(false);
                                        if (!isOutside) {
                                            activity.setApValueFunctions(displayView.apDistance.apValueName, displayView.apDistance.highlightFunctionName, displayView.apDistance.weightFunctionName);
                                        }
                                        break;
                                }

                                return true;
                            }
                        });
                        displayViews.add(displayView);
                        body.addView(displayView);
                    } else {
                        displayView = displayViews.get(i);
                        displayViews.get(i).setVisibility(VISIBLE);
                    }

                    displayView.setInfo(apDistance);
                }
            });
        });

        thread.start();
    }

    @SuppressLint("ClickableViewAccessibility")
    public void recalculateApDistanceInfo(){
        ArrayList<ApDistanceInfo> apDistances;

        if (testPointInfo == null) return;

        apDistances = testPointInfo.values;

        txtWait.setVisibility(GONE);

        int displayCount = Math.min(apDistances.size(), 50);

        for (int i = 0; i < apDistances.size(); i++) {
            ApDistanceInfo apDistance = apDistances.get(i);
            float x = testPoint.coordinateX - apDistance.predictCoordinate.x;
            float y = testPoint.coordinateY - apDistance.predictCoordinate.y;
            apDistance.distance = (float) Math.sqrt(x * x + y * y);
        }

        apDistances.sort(ApDistanceInfo.distanceComparator);

        for (int i = 0; i < displayCount; i++){
            ApDistanceInfo apDistance = apDistances.get(i);

            InfoDisplayView displayView;

            if (displayViews.size() <= i) {
                displayView = new InfoDisplayView(context);
                displayView.setOnTouchListener(new OnTouchListener() {
                    private boolean isOutside;

                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent) {
                        int action = motionEvent.getAction();
                        switch (action) {
                            case MotionEvent.ACTION_DOWN:
                                isOutside = false;
//                                    displayView.setBackgroundResource(R.drawable.background_rectangle_focus);
//                                    if (scrollView != null)
//                                        scrollView.requestDisallowInterceptTouchEvent(true);
                                break;
                            case MotionEvent.ACTION_MOVE:
                                isOutside = motionEvent.getX() > displayView.getWidth() || motionEvent.getX() < 0 ||
                                        motionEvent.getY() > displayView.getHeight() || motionEvent.getY() < 0;
//                                    if (!isOutside)
//                                        displayView.setBackgroundResource(R.drawable.background_rectangle_focus);
//                                    else
//                                        displayView.setBackgroundResource(R.drawable.background_rectangle_transparent_unfocus);
                                break;
                            case MotionEvent.ACTION_UP:
//                                    displayView.setBackgroundResource(R.drawable.background_rectangle_transparent_unfocus);
//                                    if (scrollView != null)
//                                        scrollView.requestDisallowInterceptTouchEvent(false);
                                if (!isOutside) {
                                    activity.setApValueFunctions(displayView.apDistance.apValueName, displayView.apDistance.highlightFunctionName, displayView.apDistance.weightFunctionName);
                                }
                                break;
                        }

                        return true;
                    }
                });
                displayViews.add(displayView);
                body.addView(displayView);
            } else {
                displayView = displayViews.get(i);
                displayViews.get(i).setVisibility(VISIBLE);
            }

            displayView.setInfo(apDistance);
        }
    }

//    private ScrollView scrollView;
//
//    private void getScrollView(){
//        ViewTreeObserver observer = this.getViewTreeObserver();
//        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
//            @Override
//            public void onGlobalLayout() {
//                ContentDebugView.this.getViewTreeObserver().removeOnGlobalLayoutListener(this);
//                if (scrollView == null){
//                    ViewParent temp = getParent();
//                    ViewGroup parent = null;
//                    if (temp instanceof ViewGroup)
//                        parent = (ViewGroup) temp;
//                    while (parent != null){
//                        if (parent instanceof ScrollView){
//                            scrollView = (ScrollView) parent;
//                            break;
//                        }
//                        temp = parent.getParent();
//                        if (temp instanceof ViewGroup)
//                            parent = (ViewGroup) temp;
//                        else
//                            parent = null;
//                    }
//                }
//            }
//        });
//    }

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
                                apDistance.weightFunctionName, apDistance.predictCoordinate, apDistance.distance));
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
        public String highlightFunctionName, weightFunctionName;

        public ApDataManager.Coordinate coordinate;

        public float distance;

        public TestPointFunctionInfo(String highlightFunctionName, String weightFunctionName, ApDataManager.Coordinate coordinate, float distance){
            this.highlightFunctionName = highlightFunctionName;
            this.weightFunctionName = weightFunctionName;
            this.coordinate = coordinate;
            this.distance = distance;
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
