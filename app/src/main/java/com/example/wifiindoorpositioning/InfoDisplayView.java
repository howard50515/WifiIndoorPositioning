package com.example.wifiindoorpositioning;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.example.wifiindoorpositioning.datatype.ApDistanceInfo;
import com.example.wifiindoorpositioning.datatype.DistanceInfo;
import com.example.wifiindoorpositioning.datatype.WifiResult;

public class InfoDisplayView extends LinearLayout {

    private final Context context;

    private TextView txtViewSSID;
    private TextView txtViewBSSID;
    private TextView txtViewLevel;
    private TextView txtViewFrequency;

    public ApDistanceInfo apDistance;

    public InfoDisplayView(Context context) {
        super(context);
        this.context = context;
        initView();
    }

    public InfoDisplayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
//        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.WifiInfoView);
//        getValues(typedArray);
        initView();
    }

//    private void getValues(TypedArray typedArray){
//        txtSSID = typedArray.getString(R.styleable.WifiInfoView_txtSSID);
//        txtLevel = typedArray.getString(R.styleable.WifiInfoView_txtLevel);
//        txtFrequency = typedArray.getString(R.styleable.WifiInfoView_txtFrequency);
//        typedArray.recycle();
//    }

    @SuppressLint("SetTextI18n")
    private void initView(){
        inflate(context, R.layout.component_infodisplayview, this);
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);
        txtViewSSID = findViewById(R.id.txtSSID);
        txtViewBSSID = findViewById(R.id.txtBSSID);
        txtViewLevel = findViewById(R.id.txtLevel);
        txtViewFrequency = findViewById(R.id.txtFrequency);
    }

    @SuppressLint("DefaultLocale")
    public void setInfo(DistanceInfo distanceInfo){
        txtViewSSID.setText(distanceInfo.rpName);
        txtViewBSSID.setText(String.valueOf(distanceInfo.distance));
        txtViewLevel.setText(String.format("(%.2f, %.2f)", distanceInfo.coordinateX, distanceInfo.coordinateY));
        txtViewFrequency.setText(String.format("loss rate: %d/%d (%.2f), new rate: %d/%d (%.2f)\nweight: %.4f",
                distanceInfo.notFoundNum, distanceInfo.pastFoundNum, (float)distanceInfo.notFoundNum / distanceInfo.pastFoundNum,
                distanceInfo.foundNum, distanceInfo.pastNotFoundNum, (float)distanceInfo.foundNum / distanceInfo.pastNotFoundNum,
                distanceInfo.weight));
    }

    public void setInfo(WifiResult result){
        txtViewSSID.setText(result.apId);
        txtViewBSSID.setText(String.valueOf(result.level));
        txtViewLevel.setText(result.level == -100 ? "notfound" : "");
        txtViewFrequency.setText("");
    }

    @SuppressLint("DefaultLocale")
    public void setInfo(ApDistanceInfo apDistance){
        this.apDistance = apDistance;

        txtViewSSID.setText(apDistance.apValueName);
        txtViewBSSID.setText(apDistance.highlightFunctionName);
        txtViewLevel.setText(apDistance.weightFunctionName);
        txtViewFrequency.setText(String.format("(%.2f ,%.2f) distance: %.2f", apDistance.x , apDistance.y, apDistance.distance));
    }
}
