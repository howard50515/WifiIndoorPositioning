package com.example.wifiindoorpositioning;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class HighlightButton extends androidx.appcompat.widget.AppCompatButton {
    private boolean isOutside;

    public HighlightButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        int action = motionEvent.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                isOutside = false;
                setBackgroundResource(R.drawable.background_rectangle_focus);
                break;
            case MotionEvent.ACTION_MOVE:
                isOutside = motionEvent.getX() > getWidth() || motionEvent.getX() < 0 ||
                        motionEvent.getY() > getHeight() || motionEvent.getY() < 0;
                if (!isOutside)
                    setBackgroundResource(R.drawable.background_rectangle_focus);
                else
                    setBackgroundResource(R.drawable.background_rectangle_transparent_unfocus);
                break;
            case MotionEvent.ACTION_UP:
                setBackgroundResource(R.drawable.background_rectangle_transparent_unfocus);
                if (!isOutside && listener != null) listener.buttonDown();
                break;
        }

        return true;
    }

    private OnButtonDownListener listener;

    public void setOnButtonDownListener(OnButtonDownListener listener){
        this.listener = listener;
    }

    public interface OnButtonDownListener{
        void buttonDown();
    }
}
