package net.pierrox.lightning_launcher.views.svg;

import android.graphics.Paint;
import android.graphics.Shader;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

abstract class SvgGradient extends SvgElement {
    private String mHref;
    private ArrayList<Float> mStopOffsets;
    private ArrayList<Integer> mStopColors;
    protected Paint mPaint;
    private Shader.TileMode mTileMode;
    private ArrayList<SvgElement> mUsers;

    SvgGradient(SvgSvg root, SvgGroup parent) {
        super(root, parent, true, null);
        mStopOffsets = new ArrayList<>();
        mStopColors = new ArrayList<>();
    }

    abstract void updatePaint();

    public Paint getPaint() {
        return mPaint;
    }

    public void setHref(String href) {
        mHref = href;
    }

    public String getHref() {
        return mHref;
    }

    void resolveHref(HashMap<String, SvgElement> elements_by_id) {
        if (mHref != null) {
            SvgGradient rg = (SvgGradient) elements_by_id.get(mHref.substring(1));
            if (rg != null) {
                mStopColors = rg.mStopColors;
                mStopOffsets = rg.mStopOffsets;
            } else {
                Log.e("XXX", mHref);
            }
        }
    }

    public void addStopColors(int stopColor) {
        this.mStopColors.add(stopColor);
    }

    int[] getStopColors() {
        int size = mStopColors.size();
        int[] a = new int[size];
        for (int i = 0; i < size; i++) {
            a[i] = mStopColors.get(i);
        }
        return a;
    }

    public void addStopOffset(float stopOffset) {
        this.mStopOffsets.add(stopOffset);
    }

    float[] getStopOffsets() {
        int size = mStopOffsets.size();
        float[] a = new float[size];
        for (int i = 0; i < size; i++) {
            a[i] = mStopOffsets.get(i);
        }
        return a;
    }

    public void setTileMode(Shader.TileMode tileMode) {
        this.mTileMode = tileMode;
    }

    Shader.TileMode getTileMode() {
        return mTileMode;
    }

    void addUser(SvgElement svgElement) {
        if(mUsers == null) {
            mUsers = new ArrayList<>();
        }
        mUsers.add(svgElement);
    }
}
