/*
MIT License

Copyright (c) 2022 Pierre Hébert

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

package net.pierrox.lightning_launcher.views;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.ImageView;

import net.pierrox.lightning_launcher_extreme.R;

public class CropperView extends ImageView {
    public interface OnCropperViewEvent {
        public void onCropperViewSelectionChanged(Rect selection);
        public void onCropperViewClick();
    }

    private Bitmap mBitmap;
    private Rect mSelectionRect;
    private Paint mSelectionRectPaint;
    private Paint mOutHighlightPaint;
    private Bitmap mHandleWidthBitmap;
    private Bitmap mHandleHeightBitmap;

    private float mDensity;
    private int mHandleTouchSize;
    private int mTouchSlope;

    private boolean mIsDown;
    private int mDownX, mDownY;
    private int mCurrentMoveArea;
    private Rect mInitialSelectionRect;
    private long mLastClickDate;
    private boolean mMoving;

    private OnCropperViewEvent mListener;

    private Runnable mClickRunnable = new Runnable() {
        @Override
        public void run() {
            if(mListener != null) mListener.onCropperViewClick();
        }
    };

    public CropperView(Context context, AttributeSet attrs) {
        super(context, attrs);

        final Resources resources = getResources();

        mHandleWidthBitmap = BitmapFactory.decodeResource(resources, R.drawable.crop_width);
        mHandleHeightBitmap = BitmapFactory.decodeResource(resources, R.drawable.crop_height);

        mOutHighlightPaint = new Paint();
        mOutHighlightPaint.setColor(0x80000000);
        mOutHighlightPaint.setStyle(Paint.Style.FILL);

        mSelectionRectPaint = new Paint();
        mSelectionRectPaint.setColor(resources.getColor(R.color.color_primary));
        mSelectionRectPaint.setStyle(Paint.Style.STROKE);

        mDensity = resources.getDisplayMetrics().density;
        mHandleTouchSize = (int)(16*mDensity);
        mTouchSlope = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);

        mBitmap = bm;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if(w!=0 && h != 0) {
            RectF src = new RectF(0, 0, mBitmap.getWidth(), mBitmap.getHeight());
            RectF dst = new RectF(0, 0, w, h);
            Matrix m = new Matrix();
            m.setRectToRect(src, dst, Matrix.ScaleToFit.CENTER);
            m.mapRect(src);
            mSelectionRect = new Rect();
            src.round(mSelectionRect);
            sendSelectionChangedEvent();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        final int width = getWidth();
        final int height = getHeight();
        canvas.drawRect(0, 0, width, mSelectionRect.top, mOutHighlightPaint);
        canvas.drawRect(0, mSelectionRect.bottom, width, height, mOutHighlightPaint);
        canvas.drawRect(0, mSelectionRect.top, mSelectionRect.left, mSelectionRect.bottom, mOutHighlightPaint);
        canvas.drawRect(mSelectionRect.right, mSelectionRect.top, width, mSelectionRect.bottom, mOutHighlightPaint);

        canvas.drawRect(mSelectionRect, mSelectionRectPaint);
        final int handleWWidth = mHandleWidthBitmap.getWidth();
        final int handleWHeight = mHandleWidthBitmap.getHeight();
        final int centerX = mSelectionRect.centerX();
        final int handleHWidth = mHandleHeightBitmap.getWidth();
        final int handleHHeight = mHandleHeightBitmap.getHeight();
        canvas.drawBitmap(mHandleWidthBitmap, mSelectionRect.left - handleWWidth / 2, mSelectionRect.centerY() - handleWHeight / 2, null);
        canvas.drawBitmap(mHandleWidthBitmap, mSelectionRect.right - handleWWidth / 2, mSelectionRect.centerY() - handleWHeight / 2, null);
        canvas.drawBitmap(mHandleHeightBitmap, centerX - handleHWidth /2, mSelectionRect.top- handleHHeight /2, null);
        canvas.drawBitmap(mHandleHeightBitmap, centerX - handleHWidth /2, mSelectionRect.bottom- handleHHeight /2, null);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();

        final int dx = x - mDownX;
        final int dy = y - mDownY;

        final int width = getWidth();
        final int height = getHeight();

        switch(event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                removeCallbacks(mClickRunnable);
                mIsDown = true;
                mMoving = false;
                mDownX = x;
                mDownY = y;

                mCurrentMoveArea = Gravity.NO_GRAVITY;
                if(mDownX > mSelectionRect.left && mDownX < mSelectionRect.right && Math.abs(mDownY-mSelectionRect.top) < mHandleTouchSize) {
                    mCurrentMoveArea = Gravity.TOP;
                } else if(mDownX > mSelectionRect.left && mDownX < mSelectionRect.right && Math.abs(mDownY-mSelectionRect.bottom) < mHandleTouchSize) {
                    mCurrentMoveArea = Gravity.BOTTOM;
                } else if (Math.abs(mDownX - mSelectionRect.left) < mHandleTouchSize && mDownY > mSelectionRect.top && mDownY < mSelectionRect.bottom) {
                    mCurrentMoveArea = Gravity.LEFT;
                } else if(Math.abs(mDownX-mSelectionRect.right) < mHandleTouchSize && mDownY > mSelectionRect.top && mDownY < mSelectionRect.bottom) {
                    mCurrentMoveArea = Gravity.RIGHT;
                } else if(mSelectionRect.contains(mDownX, mDownY)) {
                    mCurrentMoveArea = Gravity.CENTER;
                }
                mInitialSelectionRect = new Rect(mSelectionRect);
                return true;

            case MotionEvent.ACTION_MOVE:
                if (mIsDown && (mMoving || Math.abs(dx) > mTouchSlope || Math.abs(dy) > mTouchSlope)) {
                    mMoving = true;
                    mSelectionRect.set(mInitialSelectionRect);
                    switch (mCurrentMoveArea) {
                        case Gravity.TOP:
                            mSelectionRect.top += dy;
                            if (mSelectionRect.top < 0) mSelectionRect.top = 0;
                            else if (mSelectionRect.top > mSelectionRect.bottom)
                                mSelectionRect.top = mSelectionRect.bottom;
                            break;

                        case Gravity.BOTTOM:
                            mSelectionRect.bottom += dy;
                            if (mSelectionRect.bottom > height) mSelectionRect.bottom = height;
                            else if (mSelectionRect.bottom < mSelectionRect.top)
                                mSelectionRect.bottom = mSelectionRect.top;
                            break;

                        case Gravity.LEFT:
                            mSelectionRect.left += dx;
                            if (mSelectionRect.left < 0) mSelectionRect.left = 0;
                            else if (mSelectionRect.left > mSelectionRect.right)
                                mSelectionRect.left = mSelectionRect.right;
                            break;

                        case Gravity.RIGHT:
                            mSelectionRect.right += dx;
                            if (mSelectionRect.right > width) mSelectionRect.right = width;
                            else if (mSelectionRect.right < mSelectionRect.left)
                                mSelectionRect.right = mSelectionRect.left;
                            break;

                        case Gravity.CENTER:
                            mSelectionRect.offset(dx, dy);
                            if (mSelectionRect.left < 0)
                                mSelectionRect.offset(-mSelectionRect.left, 0);
                            if (mSelectionRect.right > width)
                                mSelectionRect.offset(width - mSelectionRect.right, 0);
                            if (mSelectionRect.top < 0)
                                mSelectionRect.offset(0, -mSelectionRect.top);
                            if (mSelectionRect.bottom > height)
                                mSelectionRect.offset(0, height - mSelectionRect.bottom);
                            break;
                    }

                    sendSelectionChangedEvent();

                    invalidate();
                }
                return true;

            case MotionEvent.ACTION_UP:
                if(mIsDown) {
                    mIsDown = false;
                    removeCallbacks(mClickRunnable);
                    if (!mMoving) {
                        final long now = System.currentTimeMillis();
                        final int doubleTapTimeout = ViewConfiguration.getDoubleTapTimeout();
                        if (now - mLastClickDate < doubleTapTimeout) {
                            final Drawable drawable = getDrawable();
                            mTmpRectF.set(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
                            getImageMatrix().mapRect(mTmpRectF);
                            mTmpRectF.round(mSelectionRect);
                            sendSelectionChangedEvent();
                            invalidate();
                        } else {
                            mLastClickDate = now;
                            postDelayed(mClickRunnable, doubleTapTimeout);
                        }
                    }
                }
                return true;

            case MotionEvent.ACTION_CANCEL:
                mIsDown = false;
                removeCallbacks(mClickRunnable);
                return true;
        }

        return super.onTouchEvent(event);
    }

    @Override
    public void setImageURI(Uri uri) {
        super.setImageURI(uri);
        sendSelectionChangedEvent();
    }

    @Override
    protected boolean setFrame(int l, int t, int r, int b) {
        boolean res = super.setFrame(l, t, r, b);
        sendSelectionChangedEvent();
        return res;
    }

    public void setOnCropperViewEvent(OnCropperViewEvent listener) {
        mListener = listener;
    }


    private Matrix mTmpMatrix = new Matrix();
    RectF mTmpRectF = new RectF();
    Rect mTmpRect = new Rect();


    public void setSelection(Rect selection) {
        mTmpRectF.set(selection);
        mTmpMatrix.set(getImageMatrix());
        Drawable drawable = getDrawable();
        Bitmap image = ((BitmapDrawable) drawable).getBitmap();
        float sx = drawable.getIntrinsicWidth() / (float)image.getWidth();
        float sy = drawable.getIntrinsicHeight() / (float)image.getHeight();
        mTmpMatrix.preScale(sx, sy);
        mTmpMatrix.mapRect(mTmpRectF);
        mTmpRectF.round(mSelectionRect);
        if(mSelectionRect.left < 0) mSelectionRect.left = 0;
        if(mSelectionRect.right > getWidth()) mSelectionRect.right = getWidth();
        if(mSelectionRect.top < 0) mSelectionRect.bottom = 0;
        if(mSelectionRect.bottom > getHeight()) mSelectionRect.bottom = getHeight();
        sendSelectionChangedEvent();
        invalidate();
    }

    public Rect getSelection() {
        mTmpRectF.set(mSelectionRect);
        getImageMatrix().invert(mTmpMatrix);
        Drawable drawable = getDrawable();
        Bitmap image = ((BitmapDrawable) drawable).getBitmap();
        float sx = image.getWidth() / (float)drawable.getIntrinsicWidth();
        float sy = image.getHeight() / (float)drawable.getIntrinsicHeight();
        mTmpMatrix.postScale(sx, sy);
        mTmpMatrix.mapRect(mTmpRectF);
        mTmpRectF.round(mTmpRect);

        return mTmpRect;
    }

    private void sendSelectionChangedEvent() {
        if(mListener != null) {
            mListener.onCropperViewSelectionChanged(getSelection());
        }
    }
}
