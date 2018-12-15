package net.pierrox.lightning_launcher.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

// I was unable to create a layout were all child views would be sized with the maximum width
public class FillLinearLayout extends LinearLayout {
    public FillLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), heightMeasureSpec);

        int maxWidth = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
        int maxMeasuredWidth = 0;
        int childCount = getChildCount();
        for(int i=0; i< childCount; i++) {
            View v = getChildAt(i);
            int w = v.getMeasuredWidth();
            if(w > maxMeasuredWidth) {
                maxMeasuredWidth = w;
            }
        }

        if(maxMeasuredWidth > maxWidth) {
            maxMeasuredWidth = maxWidth;
        }

        int h = getPaddingTop() + getPaddingBottom();
        for(int i=0; i< childCount; i++) {
            View v = getChildAt(i);
            if(v.getVisibility() != View.GONE) {
                v.measure(MeasureSpec.makeMeasureSpec(maxMeasuredWidth, MeasureSpec.EXACTLY), heightMeasureSpec);
                h += v.getMeasuredHeight();
            }
        }
        setMeasuredDimension(maxMeasuredWidth, h);
    }
}
