package com.example.wifiindoorpositioning;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;

public class ContentDebugView extends ScrollView {
    private MainActivity activity;
    private final Context context;
    private final ArrayList<InfoDisplayView> displayViews = new ArrayList<>();
    private Spinner testPointSpinner;

    private LinearLayout body, apDistanceInfoControlPanel;
    private TextView txtWait;
    private String mode = "無";
    private final PointF actualPoint = new PointF();

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

        txtWait = new TextView(context);
        txtWait.setText("計算中...");
        txtWait.setTextSize(16);
        txtWait.setGravity(Gravity.CENTER);
        txtWait.setPadding(0, 10, 0, 0);
        txtWait.setTextColor(Color.RED);
        txtWait.setVisibility(GONE);

        body.addView(txtWait);

        testPointSpinner.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, ConfigManager.getInstance().getAllTestPointNames()));
//      testPointSpinner.setSelection(ConfigManager.getInstance().getCurrentTestPointIndex());
        testPointSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                ConfigManager.getInstance().setTestPointAtIndex(testPointSpinner.getSelectedItemPosition());

                // actualPoint.set(ConfigManager.getInstance().testPoint.coordinateX, ConfigManager.getInstance().testPoint.coordinateY);
                setActualPoint(ConfigManager.getInstance().testPoint.coordinateX, ConfigManager.getInstance().testPoint.coordinateY);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        // getScrollView();

        ApDataManager.getInstance().registerOnResultChangedListener(this::refresh);
        // ConfigManager.getInstance().registerOnConfigChangedListener(() -> testPointSpinner.setSelection(ConfigManager.getInstance().getCurrentTestPointIndex()));
    }

    public void setMainActivity(MainActivity activity){
        this.activity = activity;
    }

    public void setActualPoint(float x, float y){
        actualPoint.set(x, y);

        refresh();
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
        if (!mode.equals("存取點距離") || code == ApDataManager.WIFI_RESULT_CHANGED ||
            code == ApDataManager.UNCERTAIN_CHANGED){
            refresh();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    public void hideAllInfo(){
        apDistanceInfoControlPanel.setVisibility(GONE);

        for (InfoDisplayView displayView : displayViews){
            displayView.setVisibility(GONE);
            displayView.setBackgroundColor(Color.WHITE);
            displayView.setOnTouchListener(null);
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

        txtWait.setVisibility(VISIBLE);

        Thread thread = new Thread(() ->{
            ArrayList<ApDistanceInfo> apDistances = ApDataManager.getInstance().getAllApValues();

            if (apDistances != null){
                for (int i = 0; i < apDistances.size(); i++) {
                    ApDistanceInfo apDistance = apDistances.get(i);
                    float x = actualPoint.x - apDistance.predictCoordinate.x;
                    float y = actualPoint.y - apDistance.predictCoordinate.y;
                    apDistance.distance = (float) Math.sqrt(x * x + y * y);
                }

                apDistances.sort(ApDistanceInfo.distanceComparator);
            }

            activity.runOnUiThread(() ->{
                txtWait.setVisibility(GONE);
                apDistanceInfoControlPanel.setVisibility(VISIBLE);

                if (apDistances == null) return;

                for (int i = 0; i < apDistances.size(); i++){
                    ApDistanceInfo apDistance = apDistances.get(i);

                    InfoDisplayView displayView;

                    if (displayViews.size() <= i) {
                        displayView = new InfoDisplayView(context);
                        displayViews.add(displayView);
                        body.addView(displayView);
                    } else {
                        displayView = displayViews.get(i);
                        displayViews.get(i).setVisibility(VISIBLE);
                    }

                    displayView.setInfo(apDistance);
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
                                        activity.setApValueFunctions(apDistance.apValueName, apDistance.highlightFunctionName, apDistance.weightFunctionName);
                                    }
                                    break;
                            }

                            return true;
                        }
                    });
                }
            });
        });

        thread.start();
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

}
