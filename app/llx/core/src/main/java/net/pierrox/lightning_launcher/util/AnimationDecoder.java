package net.pierrox.lightning_launcher.util;

import android.graphics.Bitmap;

import java.io.File;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.ArrayList;

public class AnimationDecoder {
    private static class GifFrame {
        public GifFrame(Bitmap im, int del) {
            image = im;
            delay = del;
        }

        public Bitmap image;
        public int delay;
    }

    private ArrayList<GifFrame> frames;
    private int mLoopCount;
    private int mTotalDuration;
    private int mWidth, mHeight;

    public AnimationDecoder() {

    }

    public AnimationDecoder(int width, int height, int count, int delay, int loopCount) {
        mWidth = width;
        mHeight = height;
        mLoopCount = loopCount;
        mTotalDuration = count * delay;
        frames = new ArrayList<>(count);
        for(int i=0; i<count; i++) {
            frames.add(new GifFrame(Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888), delay));
        }
    }

    public boolean read(File f) {

        AnimatedGifDecoder decoder = new AnimatedGifDecoder(f);

        try {
            frames = new ArrayList<>();

            decoder.start();

            mWidth = decoder.getWidth();
            mHeight = decoder.getHeight();

            for(;;) {
                int[] pixels = decoder.readFrame();
                if(pixels == null) {
                    break;
                }

                Bitmap bitmap;
                try {
                    bitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
                    bitmap.copyPixelsFromBuffer(IntBuffer.wrap(pixels));
                } catch (Throwable t) {
                    bitmap = null;
                }

                if(bitmap == null) {
                    for (GifFrame frame : frames) {
                        frame.image.recycle();
                        frame.image = null;
                    }
                    frames = null;
                    return false;
                }

                int delay = decoder.getDelay();

                frames.add(new GifFrame(bitmap, delay));

                mTotalDuration += delay;
            }

            mLoopCount = decoder.getLoopCount();

            return true;
        } catch (IOException e) {
            return false;
        } finally {
            decoder.stop();
        }
    }

    public int getDelay(int n) {
        return frames.get(n).delay;
    }

    public void setDelay(int index, int delay) {
        GifFrame frame = frames.get(index);
        mTotalDuration -= frame.delay;
        frame.delay = delay;
        mTotalDuration += delay;
    }

    public int getTotalDuration() {
        return mTotalDuration;
    }

    public int getFrameCount() {
        return frames.size();
    }

    public void setFrameCount(int count) {
        int n = frames.size();
        if(count > n) {
            final int duration = 100;
            for(int i=n; i<count; i++) {
                frames.add(new GifFrame(Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888), duration));
            }
            mTotalDuration += duration * (count - n);
        } else {
            for(int i=n-1; i>=count; i--) {
                mTotalDuration -= frames.remove(i).delay;
            }
        }
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public Bitmap getBitmap() {
        return frames.get(0).image;
    }

    public int getLoopCount() {
        return mLoopCount;
    }

    public void setLoopCount(int count) {
        mLoopCount = count;
    }

    public Bitmap getFrame(int n) {
        return frames.get(n).image;
    }
}
