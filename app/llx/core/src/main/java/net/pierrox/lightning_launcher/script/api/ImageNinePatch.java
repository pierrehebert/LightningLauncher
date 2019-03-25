package net.pierrox.lightning_launcher.script.api;

import android.graphics.NinePatch;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;

/**
 * Wraps a Nine Patch image.
 *
 * An instance of this object can be retrieved with any function that returns an {@link Image} when that image is an ImageNinePatch.
 */
public class ImageNinePatch extends Image {

    private NinePatch mNinePatch;

    /**
     * @hide
     */
    public ImageNinePatch(Lightning lightning, NinePatch np) {
        super(lightning);

        mNinePatch = np;
    }

    @Override
    public int getWidth() {
        return mNinePatch.getWidth();
    }

    @Override
    public int getHeight() {
        return mNinePatch.getHeight();
    }

    /**
     * @hide
     */
    @Override
    public Drawable toDrawable() {
        return new NinePatchDrawable(mNinePatch);
    }

    /**
     * @hide
     */
    public NinePatch getNinePatch() {
        return mNinePatch;
    }
}
