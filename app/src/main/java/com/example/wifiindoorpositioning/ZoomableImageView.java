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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ZoomableImageView extends androidx.appcompat.widget.AppCompatImageView {
    private ScrollView scrollView;
    private final Matrix matrix;
    private Paint circlePaint, arcPaint, pointsPaint, highlightPaint, fingerPaint;
    private Mode mode;
    private final ArrayList<PointF> points = new ArrayList<>();
    private final PointF imagePoint = new PointF(100, 100);
    private final PointF fingerPoint = new PointF(2854, 1407);
    private float lookAngle = 0, northOffset = 0;

    private ArrayList<ReferencePoint> drawPoints;

    private List<DistanceInfo> highlights;

    public ZoomableImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        getScrollView();

        matrix = getImageMatrix();

        canvasSettings();

        zoomImageSettings();

        ApDataManager.getInstance().registerOnResultChangedListener(new ApDataManager.OnResultChangedListener() {
            @Override
            public void resultChanged() {
                ApDataManager.Coordinate c = ApDataManager.getInstance().getPredictCoordinate();
                setImagePoint(c.x * coordinateDensityScalar,
                        c.y * coordinateDensityScalar);
                setHighlights(ApDataManager.getInstance().highlightDistances);
            }
        });
        ApDataManager.getInstance().registerOnConfigChangedListener(config -> postInvalidate());
    }

    public void screenPointToImagePoint(PointF p, float pointX, float pointY){
        p.set((pointX - values[2]) / values[0],
                (pointY - values[5]) / values[4]);
    }

    public PointF screenPointToImagePoint(float pointX, float pointY){
        PointF p = new PointF();

        screenPointToImagePoint(p, pointX, pointY);

        return p;
    }

    public void ImagePointToScreenPoint(PointF p, float pointX, float pointY){
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

    public void setImagePoint(float x, float y){
        imagePoint.set(x, y);

        gradient = new RadialGradient(x, y, arcSize / 2, 0x700000FF, 0x10000055, Shader.TileMode.CLAMP);
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

    public void setHighlights(ArrayList<DistanceInfo> distances){
        highlights = distances;

        postInvalidate();
    }

    public PointF getFingerPoint(){
        return fingerPoint;
    }

    private void setFingerPoint(float x, float y){
        screenPointToImagePoint(fingerPoint, x, y);

        if (fingerPointChangedListener != null){
            fingerPointChangedListener.pointChange(fingerPoint.x, fingerPoint.y);
        }

        postInvalidate();
    }
    private final static float arcSize = 200;
    private RadialGradient gradient;
    private void canvasSettings(){
        circlePaint = new Paint();
        circlePaint.setColor(Color.BLUE);
        circlePaint.setStyle(Paint.Style.FILL);

        gradient = new RadialGradient(imagePoint.x, imagePoint.y, arcSize / 2, 0x700000FF, 0x10000055, Shader.TileMode.CLAMP);
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
        fingerPaint.setColor(Color.valueOf(1, 0, 1, 0.6f).toArgb());
        fingerPaint.setStyle(Paint.Style.FILL);
    }

    public float density, width, height, coordinateDensityScalar;
    public static final float defaultDensity = 2.625f;

    @SuppressLint("ClickableViewAccessibility")
    private void zoomImageSettings(){

        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();

        BitmapDrawable drawable = (BitmapDrawable) getDrawable();

        setScaleType(ScaleType.MATRIX);

        if (drawable != null){
            Bitmap bitmap = drawable.getBitmap();

            density = displayMetrics.density;
            coordinateDensityScalar = density / defaultDensity;
            width = bitmap.getWidth();
            height = bitmap.getHeight();

            float scale = Math.min(displayMetrics.widthPixels / (width * density),
                    displayMetrics.heightPixels / (height * density));

            matrix.setScale(scale, scale);
        }

        setImageMatrix(matrix);

        setOnTouchListener(new OnTouchListener() {
            private final Matrix changeMatrix = new Matrix(matrix);
            private float lastPointX, lastPointY, dis, midX, midY;
            private long time;
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        mode = Mode.Drag;
                        time = Calendar.getInstance().getTimeInMillis();
                        setFingerPoint(event.getX(), event.getY());
                        lastPointX = event.getX();
                        lastPointY = event.getY();
                        if (scrollView != null)
                            scrollView.requestDisallowInterceptTouchEvent(true);
                        break;
                    case MotionEvent.ACTION_UP:
                        mode = Mode.None;
                        changeMatrix.set(matrix);
                        if (scrollView != null)
                            scrollView.requestDisallowInterceptTouchEvent(false);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (mode == Mode.Drag) {
                            // 如果停留時間過短，判定為點擊而非移動則跳過
                            if (Calendar.getInstance().getTimeInMillis() - time > 100) {
                                float x = event.getX() - lastPointX;
                                float y = event.getY() - lastPointY;

                                matrix.set(changeMatrix);
                                matrix.postTranslate(x, y);
                            }
                        }
                        else if (mode == Mode.Zoom) {
                            float newDis = distance(event);

                            float scale = newDis / dis;

                            matrix.set(changeMatrix);
                            matrix.postScale(scale, scale, midX, midY);
                        }
                        break;
                    case MotionEvent.ACTION_POINTER_DOWN:
                        mode = Mode.Zoom;
                        changeMatrix.set(matrix);
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

        float x = imagePoint.x * values[0] + values[2];
        float y = imagePoint.y * values[4] + values[5];

        gradient.setLocalMatrix(matrix);

        float half = arcSize / 2 * values[0];
        canvas.drawArc(x - half, y - half, x + half, y + half,
                northOffset + lookAngle - 60, 120, true, arcPaint);

        canvas.drawColor(Color.TRANSPARENT);
        canvas.drawCircle(x, y, 10 * values[0], circlePaint);

        float fingerX = fingerPoint.x * values[0] + values[2];
        float fingerY = fingerPoint.y * values[4] + values[5];

        canvas.drawCircle(fingerX, fingerY, 20 * values[0], fingerPaint);

        ApDataManager.Config config = ApDataManager.getInstance().getConfig();

        float pointRadius = config.referencePointRadius * values[0];
        if (config.displayReferencePoint && drawPoints != null){
            for (ReferencePoint rp : drawPoints){
                float screenX = rp.coordinateX * coordinateDensityScalar * values[0] + values[2];
                float screenY = rp.coordinateY * coordinateDensityScalar * values[4] + values[5];

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
