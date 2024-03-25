package com.example.wifiindoorpositioning;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private SystemServiceManager systemServiceManager;

    private ImageView imgCompass;
    private TextView txtOrientation, txtStatus;
    private ZoomableImageView mapImage;
    private HighlightButton btScan;

    @SuppressLint({"ClickableViewAccessibility", "SetTextI18n", "DefaultLocale"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imgCompass = findViewById(R.id.imgCompass);
        txtOrientation = findViewById(R.id.orientation);
        txtStatus = findViewById(R.id.txtStatus);
        mapImage = findViewById(R.id.zoomableView);
        btScan = findViewById(R.id.btScan);

        ApDataManager.createInstance(this);

        //ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, countries);

        mapImage.setSamplePoints(ApDataManager.getInstance().fingerprint);

        systemServiceManager = new SystemServiceManager(this);

        btScan.setOnButtonDownListener(() -> {
            txtStatus.setText("掃描中...");
            systemServiceManager.scan((code, results) -> {
                switch (code) {
                    case SystemServiceManager.CODE_SUCCESS:
                        txtStatus.setText("成功");
                        mapImage.setHighlights(ApDataManager.getInstance().getDistances(results), 4);
                        break;
                    case SystemServiceManager.CODE_NO_LOCATION:
                        txtStatus.setText("未開啟位置");
                        break;
                    case SystemServiceManager.CODE_NO_PERMISSION:
                        txtStatus.setText("未提供權限");
                        break;
                    case SystemServiceManager.CODE_TOO_FREQUENT:
                        txtStatus.setText("過於頻繁");
                        break;
                    default:
                        txtStatus.setText("未知錯誤");
                        break;
                }
            });
        });

        systemServiceManager.setOnOrientationChangedListener(degree -> {
            imgCompass.setRotation(degree);
            txtOrientation.setText(String.format("%.2f (%s)", degree, getDirection(degree)));
            mapImage.setLookAngle(degree);
        });
    }

    //region Sensor相關
    public String getDirection(float degree){
        float range = Math.abs(degree);
        if (range < 22.5){
            return "N";
        }
        else if (range < 67.5){
            return  (degree < 0) ? "NW" : "NE";
        }
        else if (range < 112.5){
            return  (degree < 0) ? "W" : "E";
        }
        else if (range < 135){
            return  (degree < 0) ? "W" : "E";
        }
        else if (range < 157.5){
            return  (degree < 0) ? "SW" : "SE";
        }

        return "S";
    }

    @Override
    protected void onResume() {
        super.onResume();

        systemServiceManager.registerSensor();
    }

    @Override
    protected void onPause() {
        super.onPause();

        systemServiceManager.unregisterSensor();
    }
    //endregion
}