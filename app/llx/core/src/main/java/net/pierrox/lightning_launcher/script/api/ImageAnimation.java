package net.pierrox.lightning_launcher.script.api;

import android.graphics.Bitmap;

import net.pierrox.lightning_launcher.data.Utils;
import net.pierrox.lightning_launcher.util.AnimationDecoder;
import net.pierrox.lightning_launcher.views.SharedAsyncGraphicsDrawable;

import java.io.File;

/**
 * A kind of image which can play an animation (often loaded from GIF files).
 *
 * <b>Note</b>: as of today backgrounds and icon layers do not support animations, only the first frame will be displayed.
 */
public class ImageAnimation extends Image {
    private SharedAsyncGraphicsDrawable mDrawable;

    /**
     * @hide
     */
    public ImageAnimation(Lightning lightning, SharedAsyncGraphicsDrawable sd) {
        super(lightning);

        mDrawable = sd;
    }

    @Override
    public int getWidth() {
        return mDrawable.getIntrinsicWidth();
    }

    @Override
    public int getHeight() {
        return mDrawable.getIntrinsicHeight();
    }

    /**
     * @hide
     */
    @Override
    public SharedAsyncGraphicsDrawable toDrawable() {
        return mDrawable;
    }

    public AnimationDecoder getDecoder() {
        return mDrawable.getAnimationDecoder();
    }

    /**
     * Start (or restart) the animation.
     */
    public void start() {
        mDrawable.startAnimation();
    }

    /**
     * Stop the animation.
     */
    public void stop() {
        mDrawable.stopAnimation();
    }

    /**
     * Return true when the animation is playing.
     */
    public boolean isPlaying() {
        return mDrawable.isAnimationPlaying();
    }

    /**
     * Return the number of frames in this animation.
     */
    public int getFrameCount() {
        return mDrawable.getAnimationFrameCount();
    }

    /**
     * Return the image at a given index.
     * If this image is modified using {@link ImageBitmap#draw()}, call {@link ImageBitmap#update()} to validate changes.
     *
     * @return an ImageBitmap, or null if index is out of bounds
     */
    public ImageBitmap getFrameImage(int index) {
        Bitmap frame = mDrawable.getAnimationFrameBitmap(index);
        return frame == null ? null : new ImageBitmap(mLightning, frame, this);
    }

    /**
     * Return the delay for the frame at a given index
     *
     * @return the frame duration, or 0 if index is out of bounds
     */
    public int getFrameDuration(int index) {
        return mDrawable.getAnimationFrameDelay(index);
    }

    /**
     * Set the duration for a frame, in milliseconds.
     */
    public void setFrameDuration(int index, int delay) {
        mDrawable.setAnimationFrameDelay(index, delay);
        setModified();
    }

    /**
     * Change the number of frames for this animation.
     * When increasing the number of frames, existing frames are kept while new one are created fully transparent, with a default delay of 100ms. Decreasing the number of frames will also keep frames whose index is below the count, other will be freed.
     * Warning: this will ensure that <i>count</i> images are allocated, this is a costly operation.
     *
     * @param count number of frames
     */
    public void setFrameCount(int count) {
        mDrawable.setAnimationFrameCount(count);
        setModified();
    }

    /**
     * Return the maximal number of loop set (can be modified with #start).
     */
    public int getLoopCount() {
        return mDrawable.getAnimationLoopCount();
    }

    /**
     * Set the maximum number of times to play the animation.
     * @param count number of loops to play, use 0 for infinite
     */
    public void setLoopCount(int count) {
        mDrawable.setAnimationLoopCount(count);
        setModified();
    }
    /**
     * Return the sum of all frames duration.
     */
    public int getTotalDuration() {
        return mDrawable.getAnimationTotalDuration();
    }


    /**
     * Save the animation to file using the GIF format.
     * Animated GIFs support a maximum of 256 colors and no transluceny (only binary tranparency): saved animations may appear differently when reloaded.
     * This is an experimental and sub-optimal feature.
     * @param path where to store the file
     * @return true if the operation succeeded
     */
    public boolean saveToFile(String path) {
        return Utils.encodeAnimationToGif(new File(path), mDrawable);
    }
}
