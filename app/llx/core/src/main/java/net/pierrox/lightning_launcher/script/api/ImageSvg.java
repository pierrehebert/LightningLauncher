package net.pierrox.lightning_launcher.script.api;

import android.graphics.drawable.Drawable;

import net.pierrox.lightning_launcher.script.api.svg.SvgSvg;
import net.pierrox.lightning_launcher.views.svg.SvgDrawable;

/**
 * An image backed by a SVG document
 */

public class ImageSvg extends Image {
    private SvgDrawable mSvgDrawable;

    /**
     * @hide
     */
    public ImageSvg(Lightning lightning, SvgDrawable svgDrawable) {
        super(lightning);
        mSvgDrawable = svgDrawable;
    }

    @Override
    public int getWidth() {
        return mSvgDrawable.getIntrinsicWidth();
    }

    @Override
    public int getHeight() {
        return mSvgDrawable.getIntrinsicHeight();
    }

    /**
     * @hide
     */
    @Override
    public Drawable toDrawable() {
        return mSvgDrawable;
    }

    public SvgSvg getSvgRoot() {
        return new SvgSvg(mSvgDrawable.getSvgRoot());
    }
}
