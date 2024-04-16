package com.example.wifiindoorpositioning;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.widget.ScrollView;

import androidx.annotation.Nullable;

import com.example.wifiindoorpositioning.datatype.DistanceInfo;
import com.example.wifiindoorpositioning.datatype.ReferencePoint;
import com.example.wifiindoorpositioning.datatype.TestPoint;
import com.example.wifiindoorpositioning.manager.ApDataManager;
import com.example.wifiindoorpositioning.manager.ConfigManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ZoomableImageView extends androidx.appcompat.widget.AppCompatImageView {
    private ScrollView scrollView;
    private final Matrix matrix;
    private Paint circlePaint, arcPaint, pointsPaint, testPointsPaint, highlightPaint, fingerPaint;
    private Mode mode;
    private final PointF imagePoint = new PointF(100, 100);
    private final PointF fingerPoint = new PointF(2854, 1407);
    private float lookAngle = 0, northOffset = 0;

    private ArrayList<ReferencePoint> drawPoints;

    private ArrayList<ReferencePoint> testPoints;

    private List<DistanceInfo> highlights;

    public ZoomableImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        getScrollView();

        matrix = getImageMatrix();

        zoomImageSettings();

        canvasSettings();

        setFingerPoint(ConfigManager.getInstance().testPoint.coordinateX, ConfigManager.getInstance().testPoint.coordinateY);

        ApDataManager.getInstance().registerOnResultChangedListener(new ApDataManager.OnResultChangedListener() {
            @Override
            public void resultChanged(int code) {
                ApDataManager.Coordinate c = ApDataManager.getPredictCoordinate(ApDataManager.getInstance().highlightDistances);
                setImagePoint(c.x, c.y);
                setHighlights(ApDataManager.getInstance().highlightDistances);
            }
        });
        ConfigManager.getInstance().registerOnConfigChangedListener(() -> {
            TestPoint testPoint = ConfigManager.getInstance().testPoint;

            setFingerPoint(testPoint.coordinateX, testPoint.coordinateY);
        });
    }

    public void screenPointToImagePoint(PointF p, float pointX, float pointY){
        p.set((pointX - values[2]) / values[0] / coordinateDensityScalar,
                (pointY - values[5]) / values[4] / coordinateDensityScalar);
    }

    public PointF screenPointToImagePoint(float pointX, float pointY){
        PointF p = new PointF();

        screenPointToImagePoint(p, pointX, pointY);

        return p;
    }

    public void imagePointToScreenPoint(PointF p, float pointX, float pointY){
        p.set(pointX * values[0] + values[2],
                pointY * values[4] + values[5]);
    }

    public void setScreenPoint(float x, float y){
        PointF p = screenPointToImagePoint(x, y);

        setImagePoint(p.x, p.y);
    }

    public PointF getImagePoint(){
        return imagePoint;
    }

    private static final int centerColor = Color.valueOf(0.4f, 0.4f, 0.8f, 0.8f).toArgb();
    private static final int edgeColor = Color.valueOf(0.4f, 0.4f, 1f, 0.2f).toArgb();
    public void setImagePoint(float x, float y){
        imagePoint.set(x, y);

        gradient = new RadialGradient(x * coordinateDensityScalar, y * coordinateDensityScalar, arcRadius, centerColor, edgeColor, Shader.TileMode.CLAMP);
        arcPaint.setShader(gradient);

        if (imagePointChangedListener != null)
            imagePointChangedListener.pointChange(x, y);

        postInvalidate();
    }
    public void setImagePoint(ApDataManager.Coordinate c){
        setImagePoint(c.x, c.y);
    }
    public void setLookAngle(float angle){
        lookAngle = angle;

        postInvalidate();
    }
    public void setNorthOffset(float offsetAngle){
        northOffset = offsetAngle;

        postInvalidate();
    }

    public void setReferencePoints(ArrayList<ReferencePoint> referencePoints){
        drawPoints = referencePoints;

        postInvalidate();
    }
    public void setTestPoints(ArrayList<ReferencePoint> testPoints){
        this.testPoints = testPoints;
    }

    public void setHighlights(ArrayList<DistanceInfo> distances){
        highlights = distances;

        postInvalidate();
    }

    public PointF getFingerPoint(){
        return fingerPoint;
    }

    private void setFingerPoint(float x, float y){
        fingerPoint.set(x, y);

        if (fingerPointChangedListener != null){
            fingerPointChangedListener.pointChange(fingerPoint.x, fingerPoint.y);
        }

        postInvalidate();
    }
    private void setFingerPointWithScreenPoint(float x, float y){
        screenPointToImagePoint(fingerPoint, x, y);

        if (fingerPointChangedListener != null){
            fingerPointChangedListener.pointChange(fingerPoint.x, fingerPoint.y);
        }

        postInvalidate();
    }
    private final static float arcRadius = 200;
    private RadialGradient gradient;
    private void canvasSettings(){
        setBackgroundColor(Color.valueOf(0.3f, 0.3f, 0.3f, 0.3f).toArgb());

        circlePaint = new Paint();
        circlePaint.setColor(Color.BLUE);
        circlePaint.setStyle(Paint.Style.FILL);

        gradient = new RadialGradient(imagePoint.x * coordinateDensityScalar, imagePoint.y * coordinateDensityScalar, arcRadius * coordinateDensityScalar, centerColor, edgeColor, Shader.TileMode.CLAMP);
        arcPaint = new Paint();
        arcPaint.setStyle(Paint.Style.FILL);
        arcPaint.setDither(true);
        arcPaint.setShader(gradient);

        pointsPaint = new Paint();
        pointsPaint.setColor(Color.RED);
        pointsPaint.setStyle(Paint.Style.FILL);

        highlightPaint = new Paint();
        highlightPaint.setColor(Color.GREEN);
        highlightPaint.setStyle(Paint.Style.FILL);

        fingerPaint = new Paint();
        fingerPaint.setColor(Color.BLACK);
        fingerPaint.setStyle(Paint.Style.FILL);
    }

    public float density, width, height, coordinateDensityScalar;
    private float minScale, displayWidth, displayHeight;
    public static final float defaultDensity = 2.625f;

    @SuppressLint("ClickableViewAccessibility")
    private void zoomImageSettings(){

        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();

        displayWidth = displayMetrics.widthPixels;

        BitmapDrawable drawable = (BitmapDrawable) getDrawable();

        setScaleType(ScaleType.MATRIX);

        if (drawable != null){
            Bitmap bitmap = drawable.getBitmap();

            density = displayMetrics.density;
            coordinateDensityScalar = density / defaultDensity;
            width = bitmap.getWidth();
            height = bitmap.getHeight();

            System.out.println(width+ "　" + height);

            minScale = Math.min(displayMetrics.widthPixels / (width * density),
                    displayMetrics.heightPixels / (height * density));

            matrix.setScale(minScale, minScale);
        }

        setImageMatrix(matrix);

        setOnTouchListener(new OnTouchListener() {
            private final Matrix changeMatrix = new Matrix(matrix);
            private final float[] changeMatrixValues = new float[9];
            private float lastPointX, lastPointY, dis, midX, midY;
            private long time;
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        mode = Mode.Drag;
                        time = Calendar.getInstance().getTimeInMillis();
                        changeMatrix.getValues(changeMatrixValues);
                        lastPointX = event.getX();
                        lastPointY = event.getY();
                        if (scrollView != null)
                            scrollView.requestDisallowInterceptTouchEvent(true);
                        break;
                    case MotionEvent.ACTION_UP:
                        mode = Mode.None;
                        if (Calendar.getInstance().getTimeInMillis() - time <= 100) {
                            setFingerPointWithScreenPoint(event.getX(), event.getY());
                        }
                        changeMatrix.set(matrix);
                        changeMatrix.getValues(changeMatrixValues);
                        if (scrollView != null)
                            scrollView.requestDisallowInterceptTouchEvent(false);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (mode == Mode.Drag) {
                            // 如果停留時間過短，判定為點擊而非移動則跳過
                            if (Calendar.getInstance().getTimeInMillis() - time > 100) {
                                float x = event.getX() - lastPointX;
                                float y = event.getY() - lastPointY;

                                float changeX = changeMatrixValues[2] + x;
                                float changeY = changeMatrixValues[5] + y;
                                float diffX = displayWidth * ((changeMatrixValues[0] / minScale) - 1);
                                float diffY = displayHeight * ((changeMatrixValues[4] / minScale) - 1);

                                if (diffX < 0){
                                    x = -changeMatrixValues[2] + (displayWidth - (width * density * changeMatrixValues[0])) / 2f;
                                }
                                else if (diffX + changeX < -50){
                                    x = -diffX - changeMatrixValues[2] - 50;
                                }
                                else if (changeX > 50){
                                    x = 50 - changeMatrixValues[2];
                                }
                                if (diffY < 0){
                                    y = -changeMatrixValues[5] + (displayHeight - (height * density * changeMatrixValues[4])) / 2f;;
                                }
                                else if (diffY + changeY < -50){
                                    y = -diffY - changeMatrixValues[5] - 50;
                                }
                                else if (changeY > 50){
                                    y = 50 - changeMatrixValues[5];
                                }

                                matrix.set(changeMatrix);
                                matrix.postTranslate(x, y);
                            }
                        }
                        else if (mode == Mode.Zoom) {
                            float newDis = distance(event);

                            float scale = newDis / dis;

                            if (changeMatrixValues[0] * scale < minScale * 0.9f){
                                scale = minScale * 0.9f / changeMatrixValues[0];
                            }

                            matrix.set(changeMatrix);
                            matrix.postScale(scale, scale, midX, midY);
                        }
                        break;
                    case MotionEvent.ACTION_POINTER_DOWN:
                        mode = Mode.Zoom;
                        changeMatrix.set(matrix);
                        changeMatrix.getValues(changeMatrixValues);
                        midX = midPoint(event.getX(1), event.getX(0));
                        midY = midPoint(event.getY(1), event.getY(0));
                        dis = distance(event);
                        break;
                    case MotionEvent.ACTION_POINTER_UP:
                        mode = Mode.None;
                        break;
                }

                setImageMatrix(matrix);

                return true;
            }
        });
    }

    private void getScrollView(){
        ViewTreeObserver observer = this.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                displayHeight = getHeight();

                System.out.println(displayHeight);

                ZoomableImageView.this.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                if (scrollView == null){
                    ViewParent temp = getParent();
                    ViewGroup parent = null;
                    if (temp instanceof ViewGroup)
                        parent = (ViewGroup) temp;
                    while (parent != null){
                        if (parent instanceof ScrollView){
                            scrollView = (ScrollView) parent;
                            break;
                        }
                        temp = parent.getParent();
                        if (temp instanceof ViewGroup)
                            parent = (ViewGroup) temp;
                        else
                            parent = null;
                    }
                }
            }
        });
    }

    private float distance(MotionEvent event){
        float x = event.getX(1) - event.getX(0);
        float y = event.getY(1) - event.getY(0);

        return (float) Math.sqrt(x * x + y * y);
    }

    private float midPoint(float x1, float x2){
        return (x1 + x2) / 2f;
    }

    private final float[] values = new float[9];

    @Override
    protected void onDraw(Canvas canvas){
        super.onDraw(canvas);

        matrix.getValues(values);

        ConfigManager configManager = ConfigManager.getInstance();

        // float scaleX = 2812 / 1291f;
        // float scaleY = 2140 / 973f;

        float scaleX = 1f;
        float scaleY = 1f;

        float pointRadius = configManager.referencePointRadius * coordinateDensityScalar * values[0];
        if (configManager.displayReferencePoint && drawPoints != null){
            for (ReferencePoint rp : drawPoints){
                float screenX = rp.coordinateX * scaleX * coordinateDensityScalar * values[0] + values[2];
                float screenY = rp.coordinateY * scaleY * coordinateDensityScalar * values[4] + values[5];

                if (highlights == null){
                    canvas.drawCircle(screenX, screenY, pointRadius, pointsPaint);
                }
                else{
                    boolean find = false;
                    for (int j = 0; j < highlights.size(); j++){
                        if (rp.name.equals(highlights.get(j).rpName)){
                            find = true;
                            break;
                        }
                    }

                    canvas.drawCircle(screenX, screenY, pointRadius, find ? highlightPaint : pointsPaint);
                }
            }
        }

        float x = imagePoint.x * scaleX * coordinateDensityScalar * values[0] + values[2];
        float y = imagePoint.y * scaleY * coordinateDensityScalar * values[4] + values[5];

        gradient.setLocalMatrix(matrix);

        float half = arcRadius * coordinateDensityScalar * values[0];
        canvas.drawArc(x - half, y - half, x + half, y + half,
                northOffset + lookAngle - 60, 120, true, arcPaint);

        canvas.drawColor(Color.TRANSPARENT);
        canvas.drawCircle(x, y, configManager.predictPointRadius * coordinateDensityScalar * values[0], circlePaint);

        float fingerX = fingerPoint.x * scaleX * coordinateDensityScalar * values[0] + values[2];
        float fingerY = fingerPoint.y * scaleY * coordinateDensityScalar * values[4] + values[5];

        canvas.drawCircle(fingerX, fingerY, configManager.actualPointRadius * coordinateDensityScalar * values[0], fingerPaint);
    }


    public enum Mode {
        None, Drag, Zoom
    }

    private OnImagePointChangedListener imagePointChangedListener = null;
    public interface OnImagePointChangedListener {
        void pointChange(float x, float y);
    }

    public void setOnImagePointChangedListener(OnImagePointChangedListener listener){
        imagePointChangedListener = listener;
    }

    private OnFingerPointChangedListener fingerPointChangedListener = null;
    public interface OnFingerPointChangedListener {
        void pointChange(float x, float y);
    }

    public void setOnFingerPointChangedListener(OnFingerPointChangedListener listener){
        fingerPointChangedListener = listener;
    }
}
