/*
MIT License

Copyright (c) 2022 Pierre Hébert

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

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
