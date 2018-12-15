package net.pierrox.lightning_launcher.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

public class EventFrameLayout extends FrameLayout {

    public EventFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public boolean mDispatchEvent = true;

    public void setDispatchEvent(boolean dispatch) {
        mDispatchEvent = dispatch;
    }

    public boolean getDispatchEvent() {
        return mDispatchEvent;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (getVisibility() == View.VISIBLE && mDispatchEvent) {
            return super.dispatchTouchEvent(ev);
        } else {
            return false;
        }
    }
}
