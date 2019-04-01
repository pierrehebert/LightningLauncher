package net.pierrox.lightning_launcher.views;

import net.pierrox.lightning_launcher_extreme.R;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Region;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

public class BoxEditorView extends View implements Runnable {
	public interface OnBoxEditorEventListener {
		public void onBoxSelectionChanged(Object token, int selection);
	}
	
	public static final int SEG_ML = 0;
	public static final int SEG_MT = 1;
	public static final int SEG_MR = 2;
	public static final int SEG_MB = 3;
	public static final int SEG_BL = 4;
	public static final int SEG_BT = 5;
	public static final int SEG_BR = 6;
	public static final int SEG_BB = 7;
	public static final int SEG_PL = 8;
	public static final int SEG_PT = 9;
	public static final int SEG_PR = 10;
	public static final int SEG_PB = 11;
	public static final int SEG_CONTENT = 12;

    private static final int ALL_MARGINGS = 1<<SEG_ML | 1<<SEG_MT | 1<<SEG_MR | 1<<SEG_MB;
    private static final int ALL_BORDERS = 1<<SEG_BL | 1<<SEG_BT | 1<<SEG_BR | 1<<SEG_BB;
    private static final int ALL_PADDINGS = 1<<SEG_PL | 1<<SEG_PT | 1<<SEG_PR | 1<<SEG_PB;

	public static final int SEG_SELECTION_COLOR_MASK = 1<<SEG_BL | 1<<SEG_BT | 1<<SEG_BR | 1<<SEG_BB | 1<<SEG_CONTENT;
	
	private static final int DIVIDER=8;
	
	private String mMargin, mBorder, mPadding, mContent, mLeft, mTop, mRight, mBottom;
	
	private Paint mCountourPlainPaint;
	private Paint mCountourDashPaint;
	private Paint mBorderAreaPaint;
	private Paint mSelectedAreaPaint;
	private Paint mTextPaint;
	private FontMetrics mTextMetrics;
	
	private RectF mOuterRect = new RectF();
	private RectF mMarginRect = new RectF();
	private RectF mBorderRect = new RectF();
	private RectF mPaddingRect = new RectF();
	
	private Path[] mAreas = new Path[13];
	
	private int mSelectedSegments;
    private int mTouchedSegment;
	
	private Object mOnBoxEditorEventToken;
	private OnBoxEditorEventListener mOnBoxEditorEventListener;
	
	private boolean mHasLongClicked;
	
	public BoxEditorView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		mMargin=context.getString(R.string.margin);
		mBorder=context.getString(R.string.border);
		mPadding=context.getString(R.string.padding);
		mContent=context.getString(R.string.content);
		mLeft=context.getString(R.string.left);
		mTop=context.getString(R.string.top);
		mRight=context.getString(R.string.right);
		mBottom=context.getString(R.string.bottom);
					
		mCountourPlainPaint = new Paint();
		mCountourPlainPaint.setStyle(Paint.Style.STROKE);
		mCountourPlainPaint.setColor(Color.BLACK);
		
		mCountourDashPaint = new Paint();
		mCountourDashPaint.setStyle(Paint.Style.STROKE);
		mCountourDashPaint.setColor(Color.BLACK);
		mCountourDashPaint.setPathEffect(new DashPathEffect(new float[] { 5, 5 }, 0));
		
		mBorderAreaPaint = new Paint();
		mBorderAreaPaint.setStyle(Paint.Style.FILL);
		mBorderAreaPaint.setColor(0xffb0b0b0);
		
		mSelectedAreaPaint = new Paint();
		mSelectedAreaPaint.setStyle(Paint.Style.FILL);
		mSelectedAreaPaint.setColor(Build.VERSION.SDK_INT<11 ? 0xff8080ff : 0x808080ff); // workaround for a drawing bug on some devices
		
		mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mTextPaint.setColor(Color.BLACK);
		mTextPaint.setTextAlign(Paint.Align.CENTER);
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int w = MeasureSpec.getSize(widthMeasureSpec);
        int max = getResources().getDimensionPixelSize(R.dimen.max_box_editor_width);
		if(w>max) w=max;
		setMeasuredDimension(w, w*2/3);
	}


	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		final int sw=(w<h ? w : h)/DIVIDER;
		final int sh=sw;
		
		int ol=sw/2;
		int ot=sh/2;
		int or=w-sw/2;
		int ob=h-sh/2;
		
		int ml=ol+sw;
		int mt=ot+sh;
		int mr=or-sw;
		int mb=ob-sh;
		
		int bl=ml+sw;
		int bt=mt+sh;
		int br=mr-sw;
		int bb=mb-sh;
		
		int pl=bl+sw;
		int pt=bt+sh;
		int pr=br-sw;
		int pb=bb-sh;
		
		mOuterRect.set(ol, ot, or, ob);
		mMarginRect.set(ml, mt, mr, mb);
		mBorderRect.set(bl, bt, br, bb);
		mPaddingRect.set(pl, pt, pr, pb);
		
		Path p;
		
		p = new Path(); p.moveTo(ol, ot); p.lineTo(ml, mt); p.lineTo(ml, mb); p.lineTo(ol, ob); p.close(); mAreas[SEG_ML] = p;
		p = new Path(); p.moveTo(ol, ot); p.lineTo(or, ot); p.lineTo(mr, mt); p.lineTo(ml, mt); p.close(); mAreas[SEG_MT] = p;
		p = new Path(); p.moveTo(or, ot); p.lineTo(or, ob); p.lineTo(mr, mb); p.lineTo(mr, mt); p.close(); mAreas[SEG_MR] = p;
		p = new Path(); p.moveTo(ol, ob); p.lineTo(ml, mb); p.lineTo(mr, mb); p.lineTo(or, ob); p.close(); mAreas[SEG_MB] = p;

		p = new Path(); p.moveTo(ml, mt); p.lineTo(bl, bt); p.lineTo(bl, bb); p.lineTo(ml, mb); p.close(); mAreas[SEG_BL] = p;
		p = new Path(); p.moveTo(ml, mt); p.lineTo(mr, mt); p.lineTo(br, bt); p.lineTo(bl, bt); p.close(); mAreas[SEG_BT] = p;
		p = new Path(); p.moveTo(mr, mt); p.lineTo(mr, mb); p.lineTo(br, bb); p.lineTo(br, bt); p.close(); mAreas[SEG_BR] = p;
		p = new Path(); p.moveTo(ml, mb); p.lineTo(bl, bb); p.lineTo(br, bb); p.lineTo(mr, mb); p.close(); mAreas[SEG_BB] = p;
		
		p = new Path(); p.moveTo(bl, bt); p.lineTo(pl, pt); p.lineTo(pl, pb); p.lineTo(bl, bb); p.close(); mAreas[SEG_PL] = p;
		p = new Path(); p.moveTo(bl, bt); p.lineTo(br, bt); p.lineTo(pr, pt); p.lineTo(pl, pt); p.close(); mAreas[SEG_PT] = p;
		p = new Path(); p.moveTo(br, bt); p.lineTo(br, bb); p.lineTo(pr, pb); p.lineTo(pr, pt); p.close(); mAreas[SEG_PR] = p;
		p = new Path(); p.moveTo(bl, bb); p.lineTo(pl, pb); p.lineTo(pr, pb); p.lineTo(br, bb); p.close(); mAreas[SEG_PB] = p;
		
		p = new Path(); p.moveTo(bl, bt); p.lineTo(br, bt); p.lineTo(br, bb); p.lineTo(bl, bb); p.close(); mAreas[SEG_CONTENT] = p;
		
		mTextPaint.setTextSize(sh/2);
		mTextMetrics = mTextPaint.getFontMetrics();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		final int w = getWidth();
		final int h = getHeight();
		
		final int sw=(w<h ? w : h)/DIVIDER;
		final int sh=sw;
		
		canvas.drawARGB(255, 255, 255, 255);
		
		canvas.drawPath(mAreas[SEG_BL], mBorderAreaPaint);
		canvas.drawPath(mAreas[SEG_BT], mBorderAreaPaint);
		canvas.drawPath(mAreas[SEG_BR], mBorderAreaPaint);
		canvas.drawPath(mAreas[SEG_BB], mBorderAreaPaint);
		
		for(int i=0; i<mAreas.length; i++) {
			drawArea(canvas, i);
		}
		
		canvas.drawRect(mOuterRect, mCountourDashPaint);
		canvas.drawRect(mMarginRect, mCountourPlainPaint);
		canvas.drawRect(mBorderRect, mCountourPlainPaint);
		canvas.drawRect(mPaddingRect, mCountourDashPaint);
		
		final float w2=w/2;
		final float h2=h/2;
		final float dh = (sh-mTextMetrics.ascent-mTextMetrics.descent)/2;
		
		canvas.drawText(mContent, w2, mPaddingRect.top+(mPaddingRect.height()-mTextMetrics.ascent-mTextMetrics.descent)/2, mTextPaint);
		
		canvas.drawText(mMargin+" "+mTop, w2, mOuterRect.top+dh, mTextPaint);
		canvas.drawText(mBorder+" "+mTop, w2, mMarginRect.top+dh, mTextPaint);
		canvas.drawText(mPadding+" "+mTop, w2, mBorderRect.top+dh, mTextPaint);
		
		canvas.drawText(mMargin+" "+mBottom, w2, mMarginRect.bottom+dh, mTextPaint);
		canvas.drawText(mBorder+" "+mBottom, w2, mBorderRect.bottom+dh, mTextPaint);
		canvas.drawText(mPadding+" "+mBottom, w2, mPaddingRect.bottom+dh, mTextPaint);
		
		canvas.save();
		final float rx_r=mOuterRect.right-dh;
		canvas.rotate(90, rx_r, h2);
		canvas.drawText(mMargin+" "+mRight, rx_r, h2, mTextPaint);
		canvas.drawText(mBorder+" "+mRight, rx_r, h2+sh, mTextPaint);
		canvas.drawText(/*mPadding+" "+*/mRight, rx_r, h2+sh*2, mTextPaint);
		canvas.restore();
		
		canvas.save();
		final float rx_l=mOuterRect.left+dh;
		canvas.rotate(-90, rx_l, h2);
		canvas.drawText(mMargin+" "+mLeft, rx_l, h2, mTextPaint);
		canvas.drawText(mBorder+" "+mLeft, rx_l, h2+sh, mTextPaint);
		canvas.drawText(/*mPadding+" "+*/mLeft, rx_l, h2+sh*2, mTextPaint);
		canvas.restore();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int action = event.getAction();
		switch(action) {
		case MotionEvent.ACTION_DOWN:
			mHasLongClicked = false;
			postDelayed(this, ViewConfiguration.getLongPressTimeout());

            mTouchedSegment = -1;
            Region r = new Region();
            Region clip = new Region(0, 0, getWidth(), getHeight());
            for(int i=0; i<mAreas.length; i++) {
                Path p = mAreas[i];
                r.setPath(p, clip);
                if(r.contains((int)event.getX(), (int)event.getY())) {
                    mTouchedSegment = i;
                    break;
                }
            }
			return true;
			
		case MotionEvent.ACTION_UP:
			if(!mHasLongClicked) {
				removeCallbacks(this);
			
				if(mTouchedSegment != -1) {
                    mSelectedSegments ^= (1<<mTouchedSegment);
                    invalidate();
                    mOnBoxEditorEventListener.onBoxSelectionChanged(mOnBoxEditorEventToken, mSelectedSegments);
				}
			}
			return true;
			
		case MotionEvent.ACTION_CANCEL:
			removeCallbacks(this);
			return true;
		}
		
		return super.onTouchEvent(event);
	}
	
	@Override
	public void run() {
		mHasLongClicked = true;
        if(mTouchedSegment >= SEG_ML && mTouchedSegment <= SEG_MB) {
            if((mSelectedSegments & ALL_MARGINGS) == ALL_MARGINGS) {
                mSelectedSegments &= ~ALL_MARGINGS;
            } else {
                mSelectedSegments |= ALL_MARGINGS;
            }
        } else if(mTouchedSegment >= SEG_BL && mTouchedSegment <= SEG_BB) {
            if((mSelectedSegments & ALL_BORDERS) == ALL_BORDERS) {
                mSelectedSegments &= ~ALL_BORDERS;
            } else {
                mSelectedSegments |= ALL_BORDERS;
            }
        } else if(mTouchedSegment >= SEG_PL && mTouchedSegment <= SEG_PB) {
            if((mSelectedSegments & ALL_PADDINGS) == ALL_PADDINGS) {
                mSelectedSegments &= ~ALL_PADDINGS;
            } else {
                mSelectedSegments |= ALL_PADDINGS;
            }
        } else {
            if(mSelectedSegments != 0) {
                mSelectedSegments = 0;
            } else {
                mSelectedSegments = 0xffffffff;
            }
        }
		mOnBoxEditorEventListener.onBoxSelectionChanged(mOnBoxEditorEventToken, mSelectedSegments);
		invalidate();
	}
	
	public void setOnBoxEditorEventListener(Object token, OnBoxEditorEventListener listener) {
		mOnBoxEditorEventToken = token;
		mOnBoxEditorEventListener = listener;
	}

	private void drawArea(Canvas canvas, int n) {
		Path p = mAreas[n];
		if(p != null) {
			if((mSelectedSegments & 1<<n)!=0) {
				canvas.drawPath(p, mSelectedAreaPaint);
			}
		}
	}
}
