package net.pierrox.lightning_launcher.views.item;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import net.pierrox.lightning_launcher.R;
import net.pierrox.lightning_launcher.data.PageIndicator;
import net.pierrox.lightning_launcher.views.MyTextView;

public class PageIndicatorView extends ItemView {
    private PageIndicator mPageIndicator;

    private IndicatorView mIndicatorView;

    private boolean mIsDummyPreview;
    private float mContainerX;
    private float mContainerY;
    private float mContainerScale;
    private float mContainerWidth;
    private float mContainerHeight;
    private Rect mContainerBounds;

    public PageIndicatorView(Context context, PageIndicator pi) {
        super(context, pi);

        mPageIndicator = pi;
    }

    @Override
    public void init() {
        IndicatorView v;
        int width, height;

        Context context = getContext();

        switch(mPageIndicator.style) {
            case DOTS:
                width = height = ViewGroup.LayoutParams.WRAP_CONTENT;
                v = new DotsView(context);
                break;

            case MINIMAP:
                width = height = ViewGroup.LayoutParams.WRAP_CONTENT;
                v = new MiniMapView(context);
                break;

            case RAW:
                width = height = ViewGroup.LayoutParams.WRAP_CONTENT;
                v = new RawView(context);
                break;

            case LINE_X:
                width = ViewGroup.LayoutParams.MATCH_PARENT;
                height = ViewGroup.LayoutParams.WRAP_CONTENT;
                v = new LineView(context);
                break;

            case LINE_Y:
                width = ViewGroup.LayoutParams.WRAP_CONTENT;
                height = ViewGroup.LayoutParams.MATCH_PARENT;
                v = new LineView(context);
                break;

            default:
                return;
        }

        mIndicatorView = v;
        if(mContainerBounds != null) {
            mIndicatorView.update();
        }

        View result;
        if(width==ViewGroup.LayoutParams.MATCH_PARENT && height==ViewGroup.LayoutParams.MATCH_PARENT) {
            result = (View)v;
        } else {
            FrameLayout c = new FrameLayout(context);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(width, height, Gravity.CENTER);
            c.addView((View) v, lp);
            result = c;
        }

        setView(result);
    }

    public void setIsDummyPreview(boolean is_dummy_preview) {
        mIsDummyPreview = is_dummy_preview;
        mContainerX = -600;
        mContainerY = -800;
        mContainerScale = 1;
        mContainerWidth = 600;
        mContainerHeight = 800;
        mContainerBounds = new Rect(0, 0, 1800, 2400);
    }

    public void onItemLayoutPositionChanged(float x, float y, float scale, float width, float height, Rect bounds) {
        if(!mIsDummyPreview) {
            mContainerX = x;
            mContainerY = y;
            mContainerScale = scale;
            mContainerWidth = width;
            mContainerHeight = height;
            mContainerBounds = bounds;
        }

        mIndicatorView.update();
    }

    private interface IndicatorView  {
        public void update();
    }

    private class DotsView extends View implements IndicatorView {

        private Paint mOuterCirclePaint;
        private Paint mInnerCirclePaint;

        private int mMinX, mMaxX;
        private int mMinY, mMaxY;

        public DotsView(Context context) {
            super(context);
            mOuterCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mOuterCirclePaint.setColor(mPageIndicator.dotsOuterColor);
            mOuterCirclePaint.setStyle(Paint.Style.STROKE);
            mOuterCirclePaint.setStrokeWidth(mPageIndicator.dotsOuterStrokeWidth);
            mInnerCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mInnerCirclePaint.setColor(mPageIndicator.dotsInnerColor);
            mInnerCirclePaint.setStyle(Paint.Style.FILL);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            final int px = mMaxX - mMinX;
            final int py = mMaxY - mMinY;
            float stroke_width = mPageIndicator.dotsOuterStrokeWidth == 0 ? 1 : mPageIndicator.dotsOuterStrokeWidth;
            float radius = Math.max(mPageIndicator.dotsInnerRadius, mPageIndicator.dotsOuterRadius + stroke_width);
            int width = (int) Math.ceil((radius * 2) * px + mPageIndicator.dotsMarginX * (px>0 ? px-1 : 0));
            int height = (int) Math.ceil((radius * 2) * py + mPageIndicator.dotsMarginY * (py>0 ? py-1 : 0));
            setMeasuredDimension(width, height);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if(mContainerWidth == 0 || mContainerHeight == 0) {
                return;
            }

            int num_dots_x = mMaxX - mMinX;
            int num_dots_y = mMaxY - mMinY;

            if(num_dots_x * num_dots_y > 250) {
                return;
            }


            float stroke_width = mPageIndicator.dotsOuterStrokeWidth == 0 ? 1 : mPageIndicator.dotsOuterStrokeWidth;

            // compute the anti aliasing delta : shift the drawing so that the outer circle is always perfectly aligned
            float ad = (stroke_width % 2) == 0 ? 0 : 0.5f;

            float radius = Math.max(mPageIndicator.dotsInnerRadius, mPageIndicator.dotsOuterRadius + stroke_width);
            float scaled_cont_x = -(mContainerX-mContainerWidth/2)/mContainerScale;
            float scaled_cont_y = -(mContainerY-mContainerHeight/2)/mContainerScale;
            float pos_x_f = scaled_cont_x / mContainerWidth - 0.5f;
            float pos_y_f = scaled_cont_y / mContainerHeight - 0.5f;

            float max_alpha = Color.alpha(mPageIndicator.dotsInnerColor) / 255f;
            float cy = radius;
            for(int y=mMinY; y<mMaxY; y++) {
                float cx = radius;
                for(int x=mMinX; x<mMaxX; x++) {
                    canvas.drawCircle(cx+ad, cy+ad, mPageIndicator.dotsOuterRadius, mOuterCirclePaint);

                    if(x == mMinX && pos_x_f > mMaxX-1) {
                        pos_x_f = x - mMaxX + pos_x_f;
                    } else if(x == mMaxX-1 && pos_x_f < mMinX) {
                        pos_x_f = x + 1 + pos_x_f - mMinX;
                    }
                    if(y == mMinY && pos_y_f > mMaxY-1) {
                        pos_y_f = y - mMaxY + pos_y_f;
                    } else if(y == mMaxY-1 && pos_y_f < mMinY) {
                        pos_y_f = y + 1 + pos_y_f - mMinY;
                    }
                    float dx = Math.abs(pos_x_f-x);
                    float dy = Math.abs(pos_y_f-y);
                    if(dx>1) dx=1; dx = 1-dx;
                    if(dy>1) dy=1; dy = 1-dy;
                    mInnerCirclePaint.setAlpha((int) (255*dx*dy*max_alpha));
                    canvas.drawCircle(cx+ad, cy+ad, mPageIndicator.dotsInnerRadius, mInnerCirclePaint);

                    cx += radius*2 + mPageIndicator.dotsMarginX;
                }
                cy += radius*2 + mPageIndicator.dotsMarginY;
            }
        }

        @Override
        public void update() {
            if(mContainerWidth !=0 && mContainerHeight != 0) {
                int min_x = (int) -Math.ceil(-mContainerBounds.left / mContainerWidth);
                int max_x = (int) Math.ceil(mContainerBounds.right / mContainerWidth);
                int min_y = (int) -Math.ceil(-mContainerBounds.top / mContainerHeight);
                int max_y = (int) Math.ceil(mContainerBounds.bottom / mContainerHeight);
                if (min_x!=mMinX || max_x!=mMaxX || min_y!=mMinY || max_y!=mMaxY) {
                    mMinX=min_x;
                    mMaxX=max_x;
                    mMinY=min_y;
                    mMaxY=max_y;
                    requestLayout();
                }
                invalidate();
            }
        }
    }


    private static RectF sTmpRectF1 = new RectF();
    private static RectF sTmpRectF2 = new RectF();
    private static Rect sTmpRect = new Rect();
    private class MiniMapView extends View implements IndicatorView {
        private Matrix mDrawMatrix = new Matrix();
        private int mWidth, mHeight;

        private Paint mOutStrokePaint;
        private Paint mOutFillPaint;
        private Paint mInStrokePaint;
        private Paint mInFillPaint;

        public MiniMapView(Context context) {
            super(context);

            // no anti alias
            mOutStrokePaint = new Paint();
            mOutStrokePaint.setStyle(Paint.Style.STROKE);
            mOutStrokePaint.setColor(mPageIndicator.miniMapOutStrokeColor);
            mOutStrokePaint.setStrokeWidth(mPageIndicator.miniMapOutStrokeWidth);

            mOutFillPaint = new Paint();
            mOutFillPaint.setStyle(Paint.Style.FILL);
            mOutFillPaint.setColor(mPageIndicator.miniMapOutFillColor);

            mInStrokePaint = new Paint();
            mInStrokePaint.setStyle(Paint.Style.STROKE);
            mInStrokePaint.setColor(mPageIndicator.miniMapInStrokeColor);
            mInStrokePaint.setStrokeWidth(mPageIndicator.miniMapInStrokeWidth);

            mInFillPaint = new Paint();
            mInFillPaint.setStyle(Paint.Style.FILL);
            mInFillPaint.setColor(mPageIndicator.miniMapInFillColor);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            if(mContainerBounds == null) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                return;
            }

            int w = MeasureSpec.getSize(widthMeasureSpec);
            int h = MeasureSpec.getSize(heightMeasureSpec);
            sTmpRectF1.set(mContainerBounds);
            sTmpRectF2.set(0, 0, w, h);
            mDrawMatrix.setRectToRect(sTmpRectF1, sTmpRectF2, Matrix.ScaleToFit.CENTER);
            mDrawMatrix.mapRect(sTmpRectF1);
            sTmpRectF1.roundOut(sTmpRect);
            setMeasuredDimension(sTmpRect.width(), sTmpRect.height());
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            sTmpRectF1.set(mContainerBounds);
            float s = mPageIndicator.miniMapInStrokeWidth ==0 ? 1 : mPageIndicator.miniMapOutStrokeWidth /2f;
            sTmpRectF2.set(s, s, right - left - s, bottom - top - s);
            mDrawMatrix.setRectToRect(sTmpRectF1, sTmpRectF2, Matrix.ScaleToFit.CENTER);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if(mContainerBounds == null) {
                return;
            }

            // do not use canvas.concat(mDrawMatrix); to keep stroke widths unscaled
            sTmpRectF1.set(mContainerBounds);
            mDrawMatrix.mapRect(sTmpRectF1);
            canvas.drawRect(sTmpRectF1, mOutFillPaint);
            canvas.drawRect(sTmpRectF1, mOutStrokePaint);
            sTmpRectF1.set(-mContainerX / mContainerScale, -mContainerY / mContainerScale, (mContainerWidth - mContainerX) / mContainerScale, (mContainerHeight - mContainerY) / mContainerScale);
            mDrawMatrix.mapRect(sTmpRectF1);
            canvas.drawRect(sTmpRectF1, mInFillPaint);
            canvas.drawRect(sTmpRectF1, mInStrokePaint);
        }

        @Override
        public void update() {
            int w = mContainerBounds.width();
            int h = mContainerBounds.height();
            if(w!=mWidth || h!=mHeight) {
                mWidth = w;
                mHeight = h;
                requestLayout();
            } else {
                invalidate();
            }
        }
    }

    private class RawView extends MyTextView implements IndicatorView {

        public RawView(Context context) {
            super(context);

            mPageIndicator.getShortcutConfig().applyToTextView(this, mPageIndicator.getItemConfig());
        }

        @Override
        public void update() {
            if(mContainerWidth !=0 && mContainerHeight != 0) {
                int min_x = (int) -Math.ceil(-mContainerBounds.left / mContainerWidth);
                int max_x = (int) Math.ceil(mContainerBounds.right / mContainerWidth);
                int min_y = (int) -Math.ceil(-mContainerBounds.top / mContainerHeight);
                int max_y = (int) Math.ceil(mContainerBounds.bottom / mContainerHeight);
                float scaled_cont_x = -(mContainerX - mContainerWidth / 2) / mContainerScale;
                float scaled_cont_y = -(mContainerY - mContainerHeight / 2) / mContainerScale;
                float pos_x_f = scaled_cont_x / mContainerWidth - 0.5f;
                float pos_y_f = scaled_cont_y / mContainerHeight - 0.5f;
                int width = max_x - min_x;
                int height = max_y - min_y;
                if(pos_x_f >= (max_x-0.5f)) pos_x_f -= width;
                if(pos_y_f >= (max_y-0.5f)) pos_y_f -= height;
                String text;
                try {
                    text = String.format(mPageIndicator.rawFormat, pos_x_f, pos_y_f, mContainerScale);
                } catch(Exception e) {
                    text = getContext().getString(R.string.dt_format_error);
                }
                setText(text);
                requestLayout();
            }
        }
    }

    private class LineView extends View implements IndicatorView {

        private int mLineWidth;
        private Matrix mDrawMatrix = new Matrix();

        private Paint mBgPaint;
        private Paint mFgPaint;

        public LineView(Context context) {
            super(context);
            mLineWidth = Math.max(mPageIndicator.lineBgWidth, mPageIndicator.lineFgWidth);

            mBgPaint = new Paint();
            mBgPaint.setStyle(Paint.Style.FILL);
            mBgPaint.setColor(mPageIndicator.lineBgColor);

            mFgPaint = new Paint();
            mFgPaint.setStyle(Paint.Style.FILL);
            mFgPaint.setColor(mPageIndicator.lineFgColor);

        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            switch (mPageIndicator.style) {
                case LINE_X:
                    setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), mLineWidth);
                    break;

                case LINE_Y:
                    setMeasuredDimension(mLineWidth, MeasureSpec.getSize(heightMeasureSpec));
                    break;

                default:
                    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            if(mContainerBounds == null) {
                return;
            }

            sTmpRectF1.set(mContainerBounds);
            sTmpRectF2.set(0, 0, right - left, bottom - top);
            mDrawMatrix.setRectToRect(sTmpRectF1, sTmpRectF2, Matrix.ScaleToFit.CENTER);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if(mContainerBounds == null) {
                return;
            }

            int w = getWidth();
            int h = getHeight();

            float bg_shift=0, fg_shift=0; // for LEFT_TOP
            switch(mPageIndicator.lineGravity) {
                case RIGHT_BOTTOM:
                    if(mPageIndicator.lineBgWidth > mPageIndicator.lineFgWidth) {
                        bg_shift = 0;
                        fg_shift = mPageIndicator.lineBgWidth - mPageIndicator.lineFgWidth;
                    } else {
                        fg_shift = 0;
                        bg_shift = mPageIndicator.lineFgWidth - mPageIndicator.lineBgWidth;
                    }
                    break;

                case CENTER:
                    if(mPageIndicator.lineBgWidth > mPageIndicator.lineFgWidth) {
                        bg_shift = 0;
                        fg_shift = (mPageIndicator.lineBgWidth - mPageIndicator.lineFgWidth) / 2;
                    } else {
                        fg_shift = 0;
                        bg_shift = (mPageIndicator.lineFgWidth - mPageIndicator.lineBgWidth) / 2;
                    }
                    break;
            }


            sTmpRectF1.set(mContainerBounds);
            sTmpRectF2.set(0, 0, w, h);
            mDrawMatrix.setRectToRect(sTmpRectF1, sTmpRectF2, Matrix.ScaleToFit.FILL);

            mDrawMatrix.mapRect(sTmpRectF1);
            sTmpRectF2.set(-mContainerX / mContainerScale, -mContainerY / mContainerScale, (mContainerWidth - mContainerX) / mContainerScale, (mContainerHeight - mContainerY) / mContainerScale);
            mDrawMatrix.mapRect(sTmpRectF2);

            switch(mPageIndicator.style) {
                case LINE_X:
                    sTmpRectF1.top = bg_shift;
                    sTmpRectF1.bottom = bg_shift+ mPageIndicator.lineBgWidth;
                    sTmpRectF2.top = fg_shift;
                    sTmpRectF2.bottom = fg_shift+ mPageIndicator.lineFgWidth;
                    break;

                case LINE_Y:
                    sTmpRectF1.left = bg_shift;
                    sTmpRectF1.right = bg_shift+ mPageIndicator.lineBgWidth;
                    sTmpRectF2.left = fg_shift;
                    sTmpRectF2.right = fg_shift+ mPageIndicator.lineFgWidth;
                    break;
            }


            canvas.drawRect(sTmpRectF1, mBgPaint);
            canvas.drawRect(sTmpRectF2, mFgPaint);

        }

        @Override
        public void update() {
            invalidate();
        }
    }
}
