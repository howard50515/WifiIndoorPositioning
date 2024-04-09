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

public class SettingsView extends LinearLayout {
    private final Context context;
    private TextView txtTestPointCoordinate;
    private EditText inputReferencePointRadius, inputK;
    private CheckBox displayReferencePoint;
    private HighlightButton btConfirm, btCancel;
    private FunctionView highlightFunctionView, weightFunctionView;
    private Spinner testPointSpinner;

    private boolean isActivate;

    public SettingsView(Context context) {
        super(context);
        this.context = context;
        initView();
    }

    @SuppressLint("DefaultLocale")
    private void initView(){
        inflate(context, R.layout.window_settingsview, this);
        txtTestPointCoordinate = findViewById(R.id.txtTestPointCoordinate);
        inputReferencePointRadius = findViewById(R.id.inputReferencePointRadius);
        inputK = findViewById(R.id.inputK);
        displayReferencePoint = findViewById(R.id.displayReferencePoint);
        btConfirm = findViewById(R.id.btConfirm);
        btCancel = findViewById(R.id.btCancel);
        highlightFunctionView = findViewById(R.id.highlightFunctionView);
        weightFunctionView = findViewById(R.id.weightFunctionView);
        testPointSpinner = findViewById(R.id.testPointSpinner);

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

        testPointSpinner.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, ConfigManager.getInstance().getAllTestPointNames()));
        // testPointSpinner.setSelection(ConfigManager.getInstance().getCurrentTestPointIndex());
        testPointSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                ReferencePoint testPoint = ConfigManager.getInstance().getTestPointAtIndex(i);

                txtTestPointCoordinate.setText(String.format("(%.2f , %.2f)", testPoint.coordinateX, testPoint.coordinateY));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        isActivate = false;
    }

    public void showView(Activity activity){
        if (isActivate) closeView();

        setView();
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
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
        config.displayReferencePoint = displayReferencePoint.isChecked();
        config.k = Integer.parseInt(inputK.getText().toString());
        config.setTestPointAtIndex(testPointSpinner.getSelectedItemPosition());
        highlightFunctionView.getAllChecked(config.enableHighlightFunctions);
        weightFunctionView.getAllChecked(config.enableWeightFunctions);

        ConfigManager.getInstance().invokeConfigChangedListener();
    }

    @SuppressLint("DefaultLocale")
    private void setView(){
        ConfigManager config = ConfigManager.getInstance();

        inputReferencePointRadius.setText(String.valueOf(config.referencePointRadius));
        displayReferencePoint.setChecked(config.displayReferencePoint);
        inputK.setText(String.valueOf(config.k));
        testPointSpinner.setSelection(ConfigManager.getInstance().getCurrentTestPointIndex());
        txtTestPointCoordinate.setText(String.format("(%.2f, %.2f)", config.testPoint.coordinateX, config.testPoint.coordinateY));
        highlightFunctionView.setFunctions(config.highlightFunctions.keys(), config.enableHighlightFunctions);
        weightFunctionView.setFunctions(config.weightFunctions.keys(), config.enableWeightFunctions);
    }

    private void resetView(){

    }
}
