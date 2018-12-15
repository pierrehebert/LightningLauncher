package net.pierrox.lightning_launcher.views.svg;

import android.graphics.Paint;
import android.graphics.RadialGradient;

public class SvgRadialGradient extends SvgGradient {
    float cx, cy, r;

    SvgRadialGradient(SvgSvg root, SvgGroup parent) {
        super(root, parent);
    }

    void updatePaint() {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        RadialGradient rg = new RadialGradient(cx, cy, r, getStopColors(), getStopOffsets(), getTileMode());
        if (mTransform != null) {
            rg.setLocalMatrix(mTransform);
        }
        mPaint.setShader(rg);
    }
}
