package net.pierrox.lightning_launcher.views.svg;

import android.graphics.LinearGradient;
import android.graphics.Paint;

public class SvgLinearGradient extends SvgGradient {
    float x1, y1, x2, y2;

    SvgLinearGradient(SvgSvg root, SvgGroup parent) {
        super(root, parent);
    }

    void updatePaint() {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        LinearGradient lg = new LinearGradient(x1, y1, x2, y2, getStopColors(), getStopOffsets(), getTileMode());
        if (mTransform != null) {
            lg.setLocalMatrix(mTransform);
        }
        mPaint.setShader(lg);
    }
}
