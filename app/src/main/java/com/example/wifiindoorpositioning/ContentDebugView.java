package com.example.wifiindoorpositioning;

import android.content.Context;
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

    public void displayDistanceInfo(){
        hideAllInfo();

        ArrayList<DistanceInfo> distances = ApDataManager.getInstance().distances;

        if (distances == null) return;

        for (int i = 0; i < distances.size(); i++){
            if (displayViews.size() <= i) {
                InfoDisplayView displayView = new InfoDisplayView(context);
                displayView.setInfo(distances.get(i));
                displayViews.add(displayView);
                body.addView(displayView);
            } else {
                displayViews.get(i).setInfo(distances.get(i));
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
}
