package com.example.wifiindoorpositioning;


import android.content.Context;
import android.util.AttributeSet;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;

public class FunctionView extends LinearLayout {
    private final Context context;

    private final ArrayList<CheckBox> checkBoxes = new ArrayList<>();

    public FunctionView(Context context) {
        super(context);
        this.context = context;
        initView();
    }

    public FunctionView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        initView();
    }

    public void initView(){
        inflate(context, R.layout.component_functionview, this);
        setOrientation(VERTICAL);
    }

    public void hideAllView(){
        for (CheckBox checkBox : checkBoxes){
            checkBox.setVisibility(GONE);
        }
    }

    public void setAllChecked(HashMap<String, Boolean> checks){
        for (int i = 0; i < checkBoxes.size(); i++){
            checks.put(checkBoxes.get(i).getText().toString(), checkBoxes.get(i).isChecked());
        }
    }

    public void setFunctions(ArrayList<String> names, HashMap<String, Boolean> checks){
        hideAllView();

        ArrayList<String> copy = new ArrayList<>(names);

        copy.sort(String::compareTo);

        int i = 0;
        for (String name : copy){
            if (checkBoxes.size() <= i) {
                CheckBox checkBox = new CheckBox(context);
                checkBox.setTextAlignment(TEXT_ALIGNMENT_CENTER);
                checkBox.setText(name);
                checkBox.setChecked(Boolean.TRUE.equals(checks.get(name)));
                checkBoxes.add(checkBox);
                addView(checkBox);
            } else {
                checkBoxes.get(i).setText(name);
                checkBoxes.get(i).setChecked(Boolean.TRUE.equals(checks.get(name)));
                checkBoxes.get(i).setVisibility(VISIBLE);
            }

            i++;
        }
    }

    public void setFunctions(String[] names, HashMap<String, Boolean> checks){
        hideAllView();

        for (int i = 0; i < names.length; i++){
            String name = names[i];

            if (checkBoxes.size() <= i) {
                CheckBox checkBox = new CheckBox(context);
                checkBox.setTextAlignment(TEXT_ALIGNMENT_CENTER);
                checkBox.setText(name);
                checkBox.setChecked(Boolean.TRUE.equals(checks.get(name)));
                checkBoxes.add(checkBox);
                addView(checkBox);
            } else {
                checkBoxes.get(i).setText(name);
                checkBoxes.get(i).setChecked(Boolean.TRUE.equals(checks.get(name)));
                checkBoxes.get(i).setVisibility(VISIBLE);
            }
        }
    }
}