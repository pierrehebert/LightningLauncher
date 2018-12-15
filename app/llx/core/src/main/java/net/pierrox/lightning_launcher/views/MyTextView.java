package net.pierrox.lightning_launcher.views;

import android.content.Context;
import android.graphics.Canvas;
import android.text.Html;
import android.widget.TextView;

public class MyTextView extends TextView {
    private int mFixWidth;
    float mHalfFixWidth;

    public MyTextView(Context context) {
        super(context);
    }

    public void setfixWidth(boolean fix_width) {
        if(fix_width) {
            float w = getPaint().measureText(" ");
            mFixWidth = Math.round(w);
            mHalfFixWidth = Math.round(w/2);
        } else {
            mFixWidth = 0;
            mHalfFixWidth = 0;
        }

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if(mFixWidth != 0) {
            setMeasuredDimension(getMeasuredWidth() + mFixWidth, getMeasuredHeight());
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.translate(mHalfFixWidth, 0);
        super.onDraw(canvas);
        canvas.translate(-mHalfFixWidth, 0);
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        CharSequence t;
        if(type == BufferType.NORMAL && looksLikeHtml(text)) {
            t = Html.fromHtml(text.toString());
            if(t.length() == 0) {
                t = text;
            }
        } else {
            t = text;
        }
        super.setText(t, type);
    }

    private static boolean looksLikeHtml(CharSequence text) {
        if(text == null) {
            return false;
        }
        int l = text.length();
        for(int i = 0; i<l; i++) {
            if(text.charAt(i) == '<') {
                return true;
            }
        }
        return false;
    }
}
