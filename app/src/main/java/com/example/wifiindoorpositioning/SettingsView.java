package com.example.wifiindoorpositioning;

import android.app.Activity;
import android.content.Context;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class SettingsView extends RelativeLayout {
    private final Context context;
    private EditText inputReferencePointRadius, inputK;
    private CheckBox displayReferencePoint;
    private HighlightButton btConfirm, btCancel;

    private boolean isActivate;

    public SettingsView(Context context) {
        super(context);
        this.context = context;
        initView();
    }

    private void initView(){
        inflate(context, R.layout.window_settingsview, this);
        inputReferencePointRadius = findViewById(R.id.inputReferencePointRadius);
        inputK = findViewById(R.id.inputK);
        displayReferencePoint = findViewById(R.id.displayReferencePoint);
        btConfirm = findViewById(R.id.btConfirm);
        btCancel = findViewById(R.id.btCancel);

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
        ApDataManager.Config config = ApDataManager.getInstance().getConfig();

        config.referencePointRadius = Integer.parseInt(inputReferencePointRadius.getText().toString());
        config.displayReferencePoint = displayReferencePoint.isChecked();
        config.k = Integer.parseInt(inputK.getText().toString());

        ApDataManager.getInstance().invokeConfigChangedListener();
    }

    private void setView(){
        ApDataManager.Config config = ApDataManager.getInstance().getConfig();

        inputReferencePointRadius.setText(String.valueOf(config.referencePointRadius));
        displayReferencePoint.setChecked(config.displayReferencePoint);
        inputK.setText(String.valueOf(config.k));
    }

    private void resetView(){

    }
}
