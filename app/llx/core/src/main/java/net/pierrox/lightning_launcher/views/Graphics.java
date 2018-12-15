package net.pierrox.lightning_launcher.views;

import android.graphics.Bitmap;

import net.pierrox.lightning_launcher.util.AnimationDecoder;
import net.pierrox.lightning_launcher.views.svg.SvgDrawable;

public class Graphics {
    private Bitmap mBitmap;
    private AnimationDecoder mAnimationDecoder;
    private SvgDrawable mSvgDrawable;
    private int mTargetWidth;
    private int mTargetHeight;

    public Graphics(Bitmap bitmap) {
        mBitmap = bitmap;
    }

    public Graphics(SvgDrawable svgDrawable) {
        mSvgDrawable = svgDrawable;
    }

    public Graphics(AnimationDecoder animationDecoder, int targetWidth, int targetHeight) {
        mAnimationDecoder = animationDecoder;
        mTargetWidth = targetWidth;
        mTargetHeight = targetHeight;
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }

    public SvgDrawable getSvgDrawable() {
        return mSvgDrawable;
    }

    public AnimationDecoder getAnimationDecoder() {
        return mAnimationDecoder;
    }

    public int getTargetWidth() {
        return mTargetWidth;
    }

    public int getTargetHeight() {
        return mTargetHeight;
    }
}
