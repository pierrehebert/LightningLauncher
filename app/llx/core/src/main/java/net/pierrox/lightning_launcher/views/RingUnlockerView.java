package net.pierrox.lightning_launcher.views;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.util.FloatMath;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import net.pierrox.lightning_launcher.R;

import java.util.ArrayList;


public class RingUnlockerView extends View {
    public interface OnUnlockerListener {
        public void onUnlocked();
    }
    private static final int ANIMATION_TOUCH_DURATION = 300;
    private static final int ANIMATION_GLOW_DURATION = 1000;

    private static final int OUTER_RING_STROKE_WIDTH = 3;
    private static final int OUTER_RING_COLOR = Color.WHITE;
    private static final int OUTER_RING_ALPHA = 0xa0;

    private static final int INNER_RING_STROKE_WIDTH = 4;
    private static final int INNER_RING_COLOR = Color.WHITE;

    private static final int GLOW_CIRCLE_COLOR = Color.WHITE;
    private static final int GLOW_CIRCLE_ALPHA = 0xa0;

    private float mCenterX;
    private float mCenterY;
    private boolean mPressed;
    private long mAnimationTouchStartDate;
    private Paint mOuterRingPaint;
    private float mOuterRingRadius;
    private Paint mInnerRingPaint;
    private float mInnerRingRadius;
    private Bitmap mLockedBitmap;
    private Bitmap mUnlockedBitmap;
    private Paint mSnapCirclePaint;
    private Paint mGlowCirclePaint;
    private Path mCenterRegion;

    private boolean mSnapping;
    private float mSnapX;
    private float mSnapY;

    private boolean mTryClip = true;

    private static class GlowItem {
        float x, y;
        long date;

        private GlowItem(float x, float y, long date) {
            this.x = x;
            this.y = y;
            this.date = date;
        }
    }
    private ArrayList<GlowItem> mGlowItems;

    private OnUnlockerListener mListener;

    private static DecelerateInterpolator sInterpolator = new DecelerateInterpolator();

    public RingUnlockerView(Context context) {
        super(context);

        mOuterRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mOuterRingPaint.setStrokeWidth(OUTER_RING_STROKE_WIDTH);
        mOuterRingPaint.setStyle(Paint.Style.STROKE);

        mInnerRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mInnerRingPaint.setStrokeWidth(INNER_RING_STROKE_WIDTH);
        mInnerRingPaint.setStyle(Paint.Style.STROKE);
        mInnerRingPaint.setColor(INNER_RING_COLOR);

        mGlowCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mGlowCirclePaint.setStyle(Paint.Style.STROKE);

        //mLockedBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.locked);
//        mUnlockedBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.unlocked);
        mLockedBitmap = ((BitmapDrawable)getResources().getDrawable(R.drawable.locked)).getBitmap();
        mUnlockedBitmap = ((BitmapDrawable)getResources().getDrawable(R.drawable.unlocked)).getBitmap();
        int bw = mLockedBitmap.getWidth();
        int bh = mLockedBitmap.getHeight();
        mInnerRingRadius = Math.min(bw, bh)*2;

        mSnapCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mSnapCirclePaint.setStyle(Paint.Style.FILL);
        mSnapCirclePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        mGlowItems = new ArrayList<GlowItem>();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float ex = event.getX();
        float ey = event.getY();
        float dx = ex-mCenterX;
        float dy = ey-mCenterY;
        float d = (float) Math.sqrt(dx * dx + dy * dy);
        int action = event.getAction();
        switch(action) {
            case MotionEvent.ACTION_DOWN:
                mSnapping = false;
                if(d < mInnerRingRadius) {
                    mPressed = true;
                    mAnimationTouchStartDate = AnimationUtils.currentAnimationTimeMillis();
                    invalidate();
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                boolean beyond = d > (mOuterRingRadius - mInnerRingRadius);
                if(!mSnapping && beyond) {
                    if(mPressed) mSnapping = true;
                    dx /= d;
                    dy /= d;
                    mSnapX = mCenterX + dx * mOuterRingRadius;
                    mSnapY = mCenterY + dy * mOuterRingRadius;
                } else if(mSnapping && !beyond) {
                    mSnapping = false;
                }
                mGlowItems.add(new GlowItem(beyond ? mSnapX : ex, beyond ? mSnapY : ey, AnimationUtils.currentAnimationTimeMillis()));
                invalidate();
                return true;

            case MotionEvent.ACTION_UP:
                if(mSnapping && mListener != null) {
                    postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mListener.onUnlocked();
                        }
                    }, 400);

                    postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mSnapping = false ;
                            invalidate() ;
                }
                    }, 1000);
                }
            case MotionEvent.ACTION_CANCEL:
                if(action==MotionEvent.ACTION_CANCEL && mSnapping) {
                    mSnapping = false;
                    invalidate();
                }
                mPressed = false;
                invalidate();
                return true;

        }

        return super.onTouchEvent(event);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mOuterRingRadius = Math.min(getWidth()-mUnlockedBitmap.getWidth(), getHeight()- mUnlockedBitmap.getHeight())/2 - OUTER_RING_STROKE_WIDTH/2;
        mCenterX = getWidth()/2;
        mCenterY = getHeight()/2;
        mCenterRegion = new Path();
        mCenterRegion.addCircle(mCenterX, mCenterY, mOuterRingRadius, Path.Direction.CCW);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        boolean need_invalidate = false;

        long now = AnimationUtils.currentAnimationTimeMillis();
        if(mAnimationTouchStartDate != 0) {
            long delta = now - mAnimationTouchStartDate;
            if(delta > ANIMATION_TOUCH_DURATION) {
                delta = ANIMATION_TOUCH_DURATION;
                mAnimationTouchStartDate = 0;
            }

            float s = sInterpolator.getInterpolation(delta/(float)ANIMATION_TOUCH_DURATION);
            mOuterRingPaint.setColor(getOuterRingColorForAlpha((int) (s * OUTER_RING_ALPHA), OUTER_RING_COLOR));
            canvas.drawCircle(mCenterX, mCenterY, s*mOuterRingRadius, mOuterRingPaint);
            need_invalidate = true;
        }
        if(mPressed) {
            if(mAnimationTouchStartDate == 0) {
                mOuterRingPaint.setColor(getOuterRingColorForAlpha(OUTER_RING_ALPHA, OUTER_RING_COLOR));
                canvas.drawCircle(mCenterX, mCenterY, mOuterRingRadius, mOuterRingPaint);
            }
            if(mSnapping) {
                canvas.drawCircle(mSnapX, mSnapY, mInnerRingRadius/2, mSnapCirclePaint);
                int bw = mUnlockedBitmap.getWidth();
                int bh = mUnlockedBitmap.getHeight();
                canvas.drawBitmap(mUnlockedBitmap, mSnapX - bw /2, mSnapY - bh /2, null);
            }
        } else if(!mSnapping) {
            int bw = mLockedBitmap.getWidth();
            int bh = mLockedBitmap.getHeight();
            canvas.drawBitmap(mLockedBitmap, mCenterX - bw /2, mCenterY - bh /2, null);
            canvas.drawCircle(mCenterX, mCenterY, mInnerRingRadius, mInnerRingPaint);
        }

        if(mGlowItems.size() != 0) {
            if(mPressed && mTryClip) {
                try {
                    canvas.clipPath(mCenterRegion);
                } catch(UnsupportedOperationException e) {
                    mTryClip = false;
                }
            }
            for(GlowItem i : mGlowItems) {
                long delta = now - i.date;
                if(delta > ANIMATION_GLOW_DURATION) {
                    delta = ANIMATION_GLOW_DURATION;
                    i.date = 0;
                }
                float s = 1-sInterpolator.getInterpolation(delta/(float)ANIMATION_GLOW_DURATION);
                mGlowCirclePaint.setColor(getOuterRingColorForAlpha((int) (s * GLOW_CIRCLE_ALPHA), GLOW_CIRCLE_COLOR));
                float r = mOuterRingRadius/3 * (1-s);
                canvas.drawCircle(i.x, i.y, r, mGlowCirclePaint);
            }
            for(int i=mGlowItems.size()-1; i>=0; i--) {
                if(mGlowItems.get(i).date==0) {
                    mGlowItems.remove(i);
                }
            }
            need_invalidate = true;
        }

        if(need_invalidate) {
            invalidate();
        }
    }

    public void setOnUnlockerListener(OnUnlockerListener l) {
        mListener = l;
    }

    private static int getOuterRingColorForAlpha(int alpha, int color) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }
}
