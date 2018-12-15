package net.pierrox.lightning_launcher.views.svg;

import android.graphics.Matrix;

public abstract class SvgElement {
    private String mId;
    private boolean mIsVisible = true;
    protected Matrix mTransform;

    protected SvgSvg mRoot;
    private SvgGroup mParent;

    public SvgElement(SvgSvg root, SvgGroup parent, boolean isVisible, Matrix transform) {
        mRoot = root;
        mParent = parent;
        mIsVisible = isVisible;
        mTransform = transform;
    }

    public SvgGroup getParent() {
        return mParent;
    }

    public void setId(String id) {
        mId = id;
    }

    public String getId() {
        return mId;
    }

    public boolean isVisible() {
        return mIsVisible;
    }

    public void setVisible(boolean visible) {
        mIsVisible = visible;
        invalidate();
    }

    public void setTransform(Matrix transform) {
        mTransform = transform;
        invalidate();
    }

    public boolean setTransform(String specification) {
        try {
            Matrix matrix = mRoot.parseTransform(specification);
            mTransform = matrix;
            invalidate();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Matrix getTransform() {
        return mTransform;
    }

    public void invalidate() {
        mRoot.invalidate();
    }
}
