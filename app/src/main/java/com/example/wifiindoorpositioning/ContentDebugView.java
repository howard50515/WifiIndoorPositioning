package com.example.wifiindoorpositioning;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import java.util.ArrayList;

public class ContentDebugView extends ScrollView {
    private final Context context;
    private final ArrayList<InfoDisplayView> displayViews = new ArrayList<>();

    private LinearLayout body;
    private String mode;

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

        ApDataManager.getInstance().registerOnResultChangedListener(this::refresh);
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
            case "距離":
                displayDistanceInfo();
                break;
            case "訊號強度":
                displayWifiResult();
                break;
        }
    }

    public void hideAllInfo(){
        for (InfoDisplayView displayView : displayViews){
            displayView.setVisibility(GONE);
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
                displayView.setBackgroundColor(isHighlight ? highlightColor : Color.WHITE);
                displayViews.add(displayView);
                body.addView(displayView);
            } else {
                displayViews.get(i).setInfo(distances.get(i));
                displayViews.get(i).setBackgroundColor(isHighlight ? highlightColor : Color.WHITE);
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
                displayView.setBackgroundColor(Color.WHITE);
                displayViews.add(displayView);
                body.addView(displayView);
            } else {
                displayViews.get(i).setInfo(results.get(i));
                displayViews.get(i).setBackgroundColor(Color.WHITE);
                displayViews.get(i).setVisibility(VISIBLE);
            }
        }
    }
}
