package com.example.wifiindoorpositioning;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.wifiindoorpositioning.datatype.TestPoint;
import com.example.wifiindoorpositioning.manager.ConfigManager;

public class SettingsView extends LinearLayout {
    private final Context context;
    private EditText inputReferencePointRadius, inputPredictPointRadius, inputActualPointRadius, inputK;
    private CheckBox displayReferencePoint;
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
        inputReferencePointRadius = findViewById(R.id.inputReferencePointRadius);
        inputPredictPointRadius = findViewById(R.id.inputPredictPointRadius);
        inputActualPointRadius = findViewById(R.id.inputActualPointRadius);
        inputK = findViewById(R.id.inputK);
        displayReferencePoint = findViewById(R.id.displayReferencePoint);
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

        config.referencePointRadius = Integer.parseInt(inputReferencePointRadius.getText().toString());
        config.predictPointRadius = Integer.parseInt(inputPredictPointRadius.getText().toString());
        config.actualPointRadius = Integer.parseInt(inputActualPointRadius.getText().toString());
        config.displayReferencePoint = displayReferencePoint.isChecked();
        config.k = Integer.parseInt(inputK.getText().toString());
        // config.setTestPointAtIndex(testPointSpinner.getSelectedItemPosition());
        apValueView.setAllChecked(config.enableApValues);
        highlightFunctionView.setAllChecked(config.enableHighlightFunctions);
        weightFunctionView.setAllChecked(config.enableWeightFunctions);

        ConfigManager.getInstance().invokeConfigChangedListener();
    }

    @SuppressLint("DefaultLocale")
    private void setView(){
        ConfigManager config = ConfigManager.getInstance();

        inputReferencePointRadius.setText(String.valueOf(config.referencePointRadius));
        inputPredictPointRadius.setText(String.valueOf(config.predictPointRadius));
        inputActualPointRadius.setText(String.valueOf(config.actualPointRadius));
        displayReferencePoint.setChecked(config.displayReferencePoint);
        inputK.setText(String.valueOf(config.k));
        apValueView.setFunctions(config.apValues, config.enableApValues);
        highlightFunctionView.setFunctions(config.getAllHighlightFunctionNames(), config.enableHighlightFunctions);
        weightFunctionView.setFunctions(config.getAllWeightFunctionNames(), config.enableWeightFunctions);
    }

    private void resetView(){

    }
}
