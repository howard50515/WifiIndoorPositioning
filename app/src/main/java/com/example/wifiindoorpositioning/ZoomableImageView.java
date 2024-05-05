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
import com.example.wifiindoorpositioning.manager.ApDataManager;
import com.example.wifiindoorpositioning.manager.ConfigManager;

import java.util.ArrayList;
import java.util.Arrays;
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

    public ZoomableImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        onGlobalLayout();

        matrix = getImageMatrix();

        zoomImageSettings();

        canvasSettings();

        if (isInEditMode()) return;

        ApDataManager.getInstance().registerOnResultChangedListener(new ApDataManager.OnResultChangedListener() {
            @Override
            public void resultChanged(int code) {
                ApDataManager.Coordinate c = ApDataManager.getPredictCoordinate(ApDataManager.getInstance().highlightDistances);
                setImagePoint(c.x, c.y);
            }
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

    public PointF getFingerPoint(){
        return fingerPoint;
    }

    public void setFingerPoint(float x, float y){
        fingerPoint.set(x, y);

//        if (fingerPointChangedListener != null){
//            fingerPointChangedListener.pointChange(fingerPoint.x, fingerPoint.y);
//        }

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
        setBackgroundColor(Color.valueOf(0.8f, 0.8f, 0.8f, 0.3f).toArgb());

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
        highlightPaint.setColor(Color.BLACK);
        highlightPaint.setStyle(Paint.Style.STROKE);
        highlightPaint.setStrokeWidth(2);

        fingerPaint = new Paint();
        fingerPaint.setColor(Color.BLACK);
        fingerPaint.setStyle(Paint.Style.FILL);
    }

    public float density, imageWidth, imageHeight, coordinateDensityScalar;
    private float minScale, displayWidth, displayHeight;
    public static final float defaultDensity = 2.625f;

    private void zoomImageSettings(){
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();

        displayWidth = displayMetrics.widthPixels;

        BitmapDrawable drawable = (BitmapDrawable) getDrawable();

        setScaleType(ScaleType.MATRIX);

        if (drawable != null){
            Bitmap bitmap = drawable.getBitmap();

            density = displayMetrics.density;
            coordinateDensityScalar = density / defaultDensity;
            imageWidth = bitmap.getWidth();
            imageHeight = bitmap.getHeight();
        }

        setImageMatrix(matrix);
    }

    private void onGlobalLayout(){
        this.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                ZoomableImageView.this.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                getDisplaySize();

                getScrollView();
            }
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void getDisplaySize(){
        displayWidth = getWidth();
        displayHeight = getHeight();

        minScale = Math.min(displayWidth / (imageWidth * density),
                displayHeight / (imageHeight * density));

        matrix.setScale(minScale, minScale);

        matrix.postTranslate((displayWidth - (imageWidth * density * minScale)) / 2f, 0);

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
                        if (Calendar.getInstance().getTimeInMillis() - time <= 300) {
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
                                float diffX = displayWidth - (changeMatrixValues[0] * density * imageWidth);
                                float diffY = displayHeight - (changeMatrixValues[4] * density * imageHeight);

                                float allowSpace = 50 * density * changeMatrixValues[0];

                                if (diffX > 0){
                                    if (changeX < -allowSpace){
                                        x = -changeMatrixValues[2] - allowSpace;
                                    }
                                    else if (changeX > diffX + allowSpace){
                                        x = diffX - changeMatrixValues[2] + allowSpace;
                                    }
                                }
                                else{
                                    if (changeX < diffX - allowSpace){
                                        x = diffX - changeMatrixValues[2] - allowSpace;
                                    }
                                    else if (changeX > allowSpace){
                                        x = -changeMatrixValues[2] + allowSpace;
                                    }
                                }

                                if (diffY > 0){
                                    if (changeY < -allowSpace){
                                        y = -changeMatrixValues[5] - allowSpace;
                                    }
                                    else if (changeY > diffY + allowSpace){
                                        y = diffY - changeMatrixValues[5] + allowSpace;
                                    }
                                }
                                else{
                                    if (changeY < diffY - allowSpace){
                                        y = diffY - changeMatrixValues[5] - allowSpace;
                                    }
                                    else if (changeY >  + allowSpace){
                                        y = -changeMatrixValues[5] + allowSpace;
                                    }
                                }

                                matrix.set(changeMatrix);
                                matrix.postTranslate(x, y);
                            }
                        }
                        else if (mode == Mode.Zoom) {
                            float newDis = distance(event);

                            float scale = newDis / dis;

//                            if (changeMatrixValues[0] * scale < minScale * 0.9f){
//                                scale = minScale * 0.9f / changeMatrixValues[0];
//                            }

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

    private float distance(MotionEvent event){
        float x = event.getX(1) - event.getX(0);
        float y = event.getY(1) - event.getY(0);

        return (float) Math.sqrt(x * x + y * y);
    }

    private float midPoint(float x1, float x2){
        return (x1 + x2) / 2f;
    }

    private final float[] values = new float[9];

    private final float[] hsv = new float[]{0f, 1f, 1f};

    @Override
    protected void onDraw(Canvas canvas){
        super.onDraw(canvas);

        matrix.getValues(values);

        ConfigManager configManager = ConfigManager.getInstance();
        ApDataManager apDataManager = ApDataManager.getInstance();

        float scaleX = 1f;
        float scaleY = 1f;

        ArrayList<ApDataManager.ProtoCluster> pointsCluster = apDataManager.signalClusters;
        ArrayList<DistanceInfo> distances = ApDataManager.getInstance().originalDistances;
        ArrayList<DistanceInfo> highlights = ApDataManager.getInstance().highlightDistances;

        float pointRadius = configManager.referencePointRadius * coordinateDensityScalar * values[0];
        if (configManager.isDebugMode){
            if (configManager.displayClustering && pointsCluster != null){
                highlightPaint.setColor(Color.BLACK);
                highlightPaint.setStyle(Paint.Style.STROKE);

                for (int i = 0; i < pointsCluster.size(); i++){
                    ArrayList<ReferencePoint> rps = pointsCluster.get(i).rps;

                    hsv[0] = (float) i / pointsCluster.size() * 360;
                    pointsPaint.setColor(Color.HSVToColor(hsv));

                    for (ReferencePoint rp : rps){
                        float screenX = rp.coordinateX * scaleX * coordinateDensityScalar * values[0] + values[2];
                        float screenY = rp.coordinateY * scaleY * coordinateDensityScalar * values[4] + values[5];

                        canvas.drawCircle(screenX, screenY, pointRadius, pointsPaint);
                        if (highlights != null){
                            boolean find = false;
                            for (int j = 0; j < highlights.size(); j++){
                                if (rp.name.equals(highlights.get(j).rpName)){
                                    find = true;
                                    break;
                                }
                            }

                            if (find) canvas.drawCircle(screenX, screenY, pointRadius, highlightPaint);
                        }
                    }
                }
            }

            if (!configManager.displayClustering && distances != null) {
                pointsPaint.setColor(Color.RED);

                highlightPaint.setColor(Color.GREEN);
                highlightPaint.setStyle(Paint.Style.FILL);

                for (DistanceInfo distance : distances) {
                    float screenX = distance.coordinateX * scaleX * coordinateDensityScalar * values[0] + values[2];
                    float screenY = distance.coordinateY * scaleY * coordinateDensityScalar * values[4] + values[5];

                    if (highlights == null) {
                        canvas.drawCircle(screenX, screenY, pointRadius, pointsPaint);
                    } else {
                        boolean find = false;
                        for (int j = 0; j < highlights.size(); j++) {
                            if (distance.rpName.equals(highlights.get(j).rpName)) {
                                find = true;
                                break;
                            }
                        }

                        canvas.drawCircle(screenX, screenY, pointRadius, find ? highlightPaint : pointsPaint);
                    }
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

        if (configManager.isDebugMode){
            float fingerX = fingerPoint.x * scaleX * coordinateDensityScalar * values[0] + values[2];
            float fingerY = fingerPoint.y * scaleY * coordinateDensityScalar * values[4] + values[5];

            canvas.drawCircle(fingerX, fingerY, configManager.actualPointRadius * coordinateDensityScalar * values[0], fingerPaint);
        }
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
