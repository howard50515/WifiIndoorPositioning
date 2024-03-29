package com.example.wifiindoorpositioning;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private SystemServiceManager systemServiceManager;

    private ImageView imgCompass;
    private TextView txtOrientation, txtStatus;
    private ZoomableImageView mapImage;
    private Spinner displayModeSpinner;
    private ContentDebugView contentView;
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
        displayModeSpinner = findViewById(R.id.displayModeSpinner);
        contentView = findViewById(R.id.contentView);

        ApDataManager.createInstance(this);

        ApDataManager.getInstance().setHighlightAndDisplayFunction(new ApDataManager.HighlightAndDisplay() {
            @Override
            public ArrayList<DistanceInfo> highlight(ArrayList<DistanceInfo> distances, int k) {
                // distances 每個參考點到目前位置的預測資訊
                // k 也可以先不管就先挑你覺得效果好的
                // for (int i = 0; i < distances.size(); i++){
                    // 拿到單一參考點資訊
                    // 可以到 DistanceInfo 看一下有什麼變數
                    // DistanceInfo distance = distances.get(i);
                    /* 透過距離、loss rate 等，決定最後highlight的參考點 */
                // }

                ArrayList<DistanceInfo> highlight = new ArrayList<>();

                // 回傳想要highlight的點
                // return highlight;

                // 目前為預設的距離最近的k個
                return ApDataManager.getInstance().defaultHighlightAndDisplay.highlight(distances, k);
            }

            @Override
            public ArrayList<DistanceInfo> display(ArrayList<DistanceInfo> distances) {
                // distances 每個參考點到目前位置的預測資訊

                // 放到這裏的話可以顯示在底下的下滑欄位
                // 也可以先不動 看你想不想看debug結果
                // 回傳越接近的點放在 ArrayList 的越前面
                // 目前為預設根據距離排序
                return ApDataManager.getInstance().defaultHighlightAndDisplay.display(distances);
            }
        });

        mapImage.setSamplePoints(ApDataManager.getInstance().fingerprint);

        systemServiceManager = new SystemServiceManager(this);

        btScan.setOnButtonDownListener(() -> {
            txtStatus.setText("掃描中...");
            systemServiceManager.scan((code, results) -> {
                switch (code) {
                    case SystemServiceManager.CODE_SUCCESS:
                        txtStatus.setText("成功");
                        ApDataManager.getInstance().setResult(results);
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

        ApDataManager.getInstance().registerOnResultChangedListener(new ApDataManager.OnResultChangedListener() {
            @Override
            public void resultChanged() {
                ApDataManager.Coordinate c = ApDataManager.getInstance().getPredictCoordinate();
                mapImage.setImagePoint(c.x * mapImage.coordinateDensityScalar,
                        c.y * mapImage.coordinateDensityScalar);
                mapImage.setHighlights(ApDataManager.getInstance().highlightDistances);
                contentView.refresh();
            }
        });

        systemServiceManager.setOnOrientationChangedListener(degree -> {
            imgCompass.setRotation(degree);
            txtOrientation.setText(String.format("%.2f (%s)", degree, getDirection(degree)));
            mapImage.setLookAngle(degree);
        });

        displayModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String mode = displayModeSpinner.getSelectedItem().toString();

                contentView.setMode(mode);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
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