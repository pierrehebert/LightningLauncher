package net.pierrox.lightning_launcher.views.item;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import net.pierrox.lightning_launcher.data.StopPoint;
import net.pierrox.lightning_launcher.data.Utils;
import net.pierrox.lightning_launcher.views.IconView;

public class StopPointView extends ItemView {
    public StopPointView(Context context, StopPoint stopPoint) {
        super(context, stopPoint);
    }

    @Override
    public void init() {
        setView(createView(getContext()));
    }

    private final static int[] sDirectionsQuadrant = new int[] {
            StopPoint.DIRECTION_RIGHT_TO_LEFT,
            StopPoint.DIRECTION_BOTTOM_TO_TOP,
            StopPoint.DIRECTION_LEFT_TO_RIGHT,
            StopPoint.DIRECTION_TOP_TO_BOTTOM};

    private View createView(Context context) {
        int n= Utils.getStandardIconSize();
        int a=n/6;
        int n2=n/2;
        Path arrow = new Path();
        arrow.moveTo(a, -a);
        arrow.lineTo(0, 0);
        arrow.lineTo(a, a);
        arrow.moveTo(0, 0);
        arrow.lineTo(n2-6, 0);

        Bitmap b = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(b);
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setStrokeCap(Paint.Cap.ROUND);

        int direction = ((StopPoint)mItem).getDirection();
        for(int i=0; i<4; i++) {
            if((direction&sDirectionsQuadrant[i])==0) continue;
            canvas.save();
            canvas.rotate(i*90, n2, n2);
            canvas.translate(3, n2);
            p.setStrokeWidth(7);
            p.setColor(Color.WHITE);
            canvas.drawPath(arrow, p);
            p.setColor(Color.RED);
            p.setStrokeWidth(3);
            canvas.drawPath(arrow, p);
            canvas.restore();
        }

        FrameLayout fl = new FrameLayout(context);
        fl.addView(new IconView(context, b, false), new FrameLayout.LayoutParams(n, n, Gravity.CENTER));

        return fl;
    }
}
