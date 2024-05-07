package com.example.wifiindoorpositioning;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.example.wifiindoorpositioning.manager.ConfigManager;
import com.google.android.material.slider.Slider;

public class SettingsView extends LinearLayout {
    private final Context context;
    private LinearLayout rootView;
    private Slider referencePointRadiusSlider, predictPointRadiusSlider, actualPointRadiusSlider, kNearestSlider, kMeansSlider, qClusterSlider;
    private CheckBox debugModeCheckBox, clusteringCheckBox;
    private HighlightButton btConfirm, btCancel;
    private FunctionView apValueView, highlightFunctionView, weightFunctionView;

    private boolean isActivate;

    public SettingsView(Context context) {
        super(context);
        this.context = context;
        initView();
    }

    @SuppressLint("DefaultLocale")
    private void initView(){
        inflate(context, R.layout.window_settingsview, this);
        rootView = findViewById(R.id.settingsRootView);
        referencePointRadiusSlider = findViewById(R.id.referencePointRadiusSlider);
        predictPointRadiusSlider = findViewById(R.id.predictPointRadiusSlider);
        actualPointRadiusSlider = findViewById(R.id.actualPointRadiusSlider);
        kNearestSlider = findViewById(R.id.kNearestSlider);
        kMeansSlider = findViewById(R.id.kMeansSlider);
        qClusterSlider = findViewById(R.id.qClusterSlider);
        clusteringCheckBox = findViewById(R.id.displayClustering);
        debugModeCheckBox = findViewById(R.id.displayReferencePoint);
        btConfirm = findViewById(R.id.btConfirm);
        btCancel = findViewById(R.id.btCancel);
        apValueView = findViewById(R.id.apValueView);
        highlightFunctionView = findViewById(R.id.highlightFunctionView);
        weightFunctionView = findViewById(R.id.weightFunctionView);

        btConfirm.setOnButtonDownListener(new HighlightButton.OnButtonDownListener() {
            @Override
            public void buttonDown() {
                saveChanged();
                closeView();
            }
        });

        btCancel.setOnButtonDownListener(new HighlightButton.OnButtonDownListener() {
            @Override
            public void buttonDown() {
                closeView();
            }
        });

        debugModeCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                ConfigManager.getInstance().isDebugMode = b;

                LinearLayout debugView = ConfigManager.getInstance().debugView;

                if (ConfigManager.getInstance().isDebugMode && getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE){
                    rootView.removeView(debugView);
                }
                else{
                    ViewGroup parent = (ViewGroup) debugView.getParent();
                    if (parent != null)
                        parent.removeView(debugView);
                    rootView.addView(debugView);
                }
            }
        });

        isActivate = false;
    }

    public void showView(Activity activity){
        if (isActivate) closeView();

        setView();
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        activity.addContentView(this, params);
        isActivate = true;
    }

    public void closeView(){
        if (!isActivate) return;

        resetView();
        ((ViewGroup) getParent()).removeView(this);
        isActivate = false;
    }

    private void saveChanged(){
        ConfigManager config = ConfigManager.getInstance();

        config.referencePointRadius = (int) referencePointRadiusSlider.getValue();
        config.predictPointRadius = (int) predictPointRadiusSlider.getValue();
        config.actualPointRadius = (int) actualPointRadiusSlider.getValue();
        config.kNearest = (int) kNearestSlider.getValue();
        config.kMeans = (int) kMeansSlider.getValue();
        config.qClusterNum = (int) qClusterSlider.getValue();
        config.isDebugMode = debugModeCheckBox.isChecked();
        config.displayClustering = clusteringCheckBox.isChecked();
        apValueView.setAllChecked(config.enableApValues);
        highlightFunctionView.setAllChecked(config.enableHighlightFunctions);
        weightFunctionView.setAllChecked(config.enableWeightFunctions);

        ConfigManager.getInstance().invokeConfigChangedListener();
    }

    @SuppressLint("DefaultLocale")
    private void setView(){
        ConfigManager config = ConfigManager.getInstance();

        referencePointRadiusSlider.setValue(config.referencePointRadius);
        predictPointRadiusSlider.setValue(config.predictPointRadius);
        actualPointRadiusSlider.setValue(config.actualPointRadius);
        kNearestSlider.setValue(config.kNearest);
        kMeansSlider.setValue(config.kMeans);
        qClusterSlider.setValue(config.qClusterNum);
        debugModeCheckBox.setChecked(config.isDebugMode);
        clusteringCheckBox.setChecked(config.displayClustering);
        apValueView.setFunctions(config.apValues, config.enableApValues);
        highlightFunctionView.setFunctions(config.getAllHighlightFunctionNames(), config.enableHighlightFunctions);
        weightFunctionView.setFunctions(config.getAllWeightFunctionNames(), config.enableWeightFunctions);
    }

    private void resetView(){

    }
}
