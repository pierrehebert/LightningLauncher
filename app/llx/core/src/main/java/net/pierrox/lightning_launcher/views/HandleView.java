package net.pierrox.lightning_launcher.views;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.ImageView;

import net.pierrox.lightning_launcher.R;

public class HandleView extends ViewGroup implements View.OnTouchListener {
	public enum Mode {
		CONTENT_SIZE,
		SCALE,
		ROTATE,
        NONE
	}
	
	public enum Handle {
		TOP,
        TOP_RIGHT,
		RIGHT,
        BOTTOM_RIGHT,
		BOTTOM,
        BOTTOM_LEFT,
		LEFT,
        TOP_LEFT
	}
	
	public interface OnHandleViewEventListener {
		public void onHandlePressed(Handle h);
		public void onHandleMoved(Handle h, float dx, float dy);
		public void onHandleUnpressed(Handle h, float dx, float dy);
		public void onHandleClicked(Handle h);
		public void onHandleLongClicked(Handle h);
	}

	private int mDragThreshold;
	private Mode mMode;
	private OnHandleViewEventListener mOnHandleViewEventListener;
	
	private ImageView mTopHandle;
	private ImageView mTopLeftHandle;
	private ImageView mLeftHandle;
	private ImageView mBottomLeftHandle;
	private ImageView mBottomHandle;
	private ImageView mBottomRightHandle;
	private ImageView mRightHandle;
	private ImageView mTopRightHandle;
	private View mCenterHandle;
	
	private int mHandleSize;
    private int mMinContentSize;
    private int mInnerHandleSizeW;
    private int mInnerHandleSizeH;

	private Bitmap[][] mHandleBitmaps;
	private float mMoveStartX;
	private float mMoveStartY;
	
	private boolean mHasPressed;
	private boolean mMoveStarted;
	private Handle mPressedHandle;
	private boolean mHasLongPressed;
	private Runnable mLongClickRunnable=new Runnable() {
		@Override
		public void run() {
			mHasLongPressed=true;
			mOnHandleViewEventListener.onHandleLongClicked(mPressedHandle);
		}
	};

	public HandleView(Context context) {
		super(context);
		
		int s=ViewConfiguration.get(context).getScaledTouchSlop();
		mDragThreshold=s*s;

        mMinContentSize = getResources().getDimensionPixelSize(R.dimen.min_handle_content_size);
		
		mTopHandle=new ImageView(context);
		mRightHandle=new ImageView(context);
		mBottomHandle=new ImageView(context);
		mLeftHandle=new ImageView(context);
		mCenterHandle=new View(context);
		mCenterHandle.setVisibility(View.VISIBLE);
		mTopLeftHandle=new ImageView(context);
		mTopRightHandle=new ImageView(context);
		mBottomLeftHandle=new ImageView(context);
		mBottomRightHandle=new ImageView(context);

		mTopHandle.setOnTouchListener(this);
		mRightHandle.setOnTouchListener(this);
		mBottomHandle.setOnTouchListener(this);
		mLeftHandle.setOnTouchListener(this);
        mTopLeftHandle.setOnTouchListener(this);
        mTopRightHandle.setOnTouchListener(this);
        mBottomLeftHandle.setOnTouchListener(this);
        mBottomRightHandle.setOnTouchListener(this);
		//mCenterHandle.setOnTouchListener(this);
		
		addView(mTopHandle);
		addView(mRightHandle);
		addView(mBottomHandle);
		addView(mLeftHandle);
		addView(mTopLeftHandle);
		addView(mTopRightHandle);
		addView(mBottomLeftHandle);
		addView(mBottomRightHandle);
		addView(mCenterHandle);

		
		// all handles must have the same size
		mHandleBitmaps=new Bitmap[Handle.values().length][Mode.values().length];
		generateHandleBitmapVariant(Mode.CONTENT_SIZE, R.drawable.handle_size_top);
		generateHandleBitmapVariant(Mode.SCALE, R.drawable.handle_scale_top);
		generateHandleBitmapVariant(Mode.ROTATE, R.drawable.handle_rotate_top);
		
		mHandleSize=mHandleBitmaps[0][0].getHeight();

        setMode(Mode.CONTENT_SIZE);
	}

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int s = mHandleSize;
        final int sw = s + mInnerHandleSizeW;
        final int sh = s + mInnerHandleSizeH;
        final int w=r-l;
        final int h=b-t;
        mTopHandle.layout(s, 0, w-s, sh);
        mBottomHandle.layout(s, h-sh, w-s, h);
        mLeftHandle.layout(0, s, sw, h-s);
        mRightHandle.layout(w-sw, s, w, h-s);
        mCenterHandle.layout(s, s, w-s, h-s);
        mTopLeftHandle.layout(0, 0, sw, sh);
        mTopRightHandle.layout(w-sw, 0, w, sh);
        mBottomLeftHandle.layout(0, h-sh, sw, h);
        mBottomRightHandle.layout(w-sw, h-sh, w, h);
    }

    public void computeInnerHandleSize(int w, int h) {
        // deduce the size occupied by the external handles
        w -= mHandleSize*2;
        h -= mHandleSize*2;

        int pw, ph;
        if((w-mHandleSize*2) < mMinContentSize) {
            pw = (w - mMinContentSize) / 2;
            if(pw < 0) pw = 0;
        } else {
            pw = mHandleSize;
        }
        if((h-mHandleSize*2) < mMinContentSize) {
            ph = (h - mMinContentSize) / 2;
            if(ph < 0) ph = 0;
        } else {
            ph = mHandleSize;
        }
        mTopHandle.setPadding(0, 0, 0, ph);
        mBottomHandle.setPadding(0, ph, 0, 0);
        mLeftHandle.setPadding(0, 0, pw, 0);
        mRightHandle.setPadding(pw, 0, 0, 0);
        mTopLeftHandle.setPadding(0, 0, pw, ph);
        mTopRightHandle.setPadding(pw, 0, 0, ph);
        mBottomLeftHandle.setPadding(0, ph, pw, 0);
        mBottomRightHandle.setPadding(pw, ph, 0, 0);

        mInnerHandleSizeW = pw;
        mInnerHandleSizeH = ph;
    }

    private void generateHandleBitmapVariant(Mode m, int top_drawable) {
		int mo=m.ordinal();
		Resources rsrc=getResources();
		Bitmap top=null;
		int w=0, h=0;
		Matrix transform=new Matrix();
		for(Handle handle : Handle.values()) {
			Bitmap b;
			if(handle==Handle.TOP) {
				b=top=BitmapFactory.decodeResource(rsrc, top_drawable);
				w=top.getWidth();
				h=top.getHeight();
			} else {
				transform.postRotate(45, w/2, h/2);
				b=Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
				new Canvas(b).drawBitmap(top, transform, null);
			}
			mHandleBitmaps[handle.ordinal()][mo]=b;
		}
	}
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		int[] location=new int[2];
//		v.getLocationInWindow(location);
		location[0] = getLeft() + v.getLeft();
		location[1] = getTop() + v.getTop();

		// mMoveStartX and mMoveStartY may not have been set but this is not a problem because in this case dx and dy are not used
		float dx=event.getX()-mMoveStartX+location[0];
		float dy=event.getY()-mMoveStartY+location[1];

		switch(event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			mHasPressed=true;
			mMoveStarted=false;
			mPressedHandle=getHandleForView(v);
			mHasLongPressed=false;
			postDelayed(mLongClickRunnable, ViewConfiguration.getLongPressTimeout());
			mMoveStartX=event.getX()+location[0];
			mMoveStartY=event.getY()+location[1];
			mOnHandleViewEventListener.onHandlePressed(getHandleForView(v));
			v.setBackgroundColor(0x80ffffff);
			return true;
			
		case MotionEvent.ACTION_MOVE:
			if(!mMoveStarted /*&& !mHasLongPressed*/) {
				if((dx*dx+dy*dy)>mDragThreshold) {
					removeCallbacks(mLongClickRunnable);
					mMoveStarted=true;
				}
			}
			if(mMoveStarted) {
				mOnHandleViewEventListener.onHandleMoved(getHandleForView(v), dx, dy);
			}
			return true;
		
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			if(mHasPressed) {
				mHasPressed=false;
				if(event.getAction()==MotionEvent.ACTION_UP) {
					if(!mMoveStarted) {
						if(!mHasLongPressed) {
							removeCallbacks(mLongClickRunnable);
							mOnHandleViewEventListener.onHandleClicked(mPressedHandle);
						}
					}
				}
				mOnHandleViewEventListener.onHandleUnpressed(getHandleForView(v), dx, dy);
				v.setBackgroundColor(0);
			}
			return true;
		}
		return false;
	}
	
	public Mode getMode() {
		return mMode;
	}

	public void setMode(Mode m) {
		mMode=m;
        if(m == Mode.NONE) {
            mTopHandle.setVisibility(View.GONE);
            mRightHandle.setVisibility(View.GONE);
            mBottomHandle.setVisibility(View.GONE);
            mLeftHandle.setVisibility(View.GONE);
            mTopLeftHandle.setVisibility(View.GONE);
            mTopRightHandle.setVisibility(View.GONE);
            mBottomLeftHandle.setVisibility(View.GONE);
            mBottomRightHandle.setVisibility(View.GONE);
        } else {
            mTopHandle.setVisibility(View.VISIBLE);
            mRightHandle.setVisibility(View.VISIBLE);
            mBottomHandle.setVisibility(View.VISIBLE);
            mLeftHandle.setVisibility(View.VISIBLE);
            int o=m.ordinal();
            mTopHandle.setImageBitmap(mHandleBitmaps[Handle.TOP.ordinal()][o]);
            mRightHandle.setImageBitmap(mHandleBitmaps[Handle.RIGHT.ordinal()][o]);
            mBottomHandle.setImageBitmap(mHandleBitmaps[Handle.BOTTOM.ordinal()][o]);
            mLeftHandle.setImageBitmap(mHandleBitmaps[Handle.LEFT.ordinal()][o]);
            if(m == Mode.SCALE) {
                mTopLeftHandle.setVisibility(View.VISIBLE);
                mTopRightHandle.setVisibility(View.VISIBLE);
                mBottomLeftHandle.setVisibility(View.VISIBLE);
                mBottomRightHandle.setVisibility(View.VISIBLE);
                mTopLeftHandle.setImageBitmap(mHandleBitmaps[Handle.TOP_LEFT.ordinal()][o]);
                mTopRightHandle.setImageBitmap(mHandleBitmaps[Handle.TOP_RIGHT.ordinal()][o]);
                mBottomLeftHandle.setImageBitmap(mHandleBitmaps[Handle.BOTTOM_LEFT.ordinal()][o]);
                mBottomRightHandle.setImageBitmap(mHandleBitmaps[Handle.BOTTOM_RIGHT.ordinal()][o]);
            } else {
                mTopLeftHandle.setVisibility(View.GONE);
                mTopRightHandle.setVisibility(View.GONE);
                mBottomLeftHandle.setVisibility(View.GONE);
                mBottomRightHandle.setVisibility(View.GONE);
            }
        }
	}

	public void setOnHandleViewEventListener(OnHandleViewEventListener listener) {
		mOnHandleViewEventListener=listener;
	}
	
	public int getHandleSize() {
		return mHandleSize;
	}

	private Handle getHandleForView(View v) {
		if(v==mTopHandle) return Handle.TOP;
		if(v==mRightHandle) return Handle.RIGHT;
		if(v==mBottomHandle) return Handle.BOTTOM;
		if(v==mLeftHandle) return Handle.LEFT;
        if(v==mTopLeftHandle) return Handle.TOP_LEFT;
		if(v==mTopRightHandle) return Handle.TOP_RIGHT;
		if(v==mBottomLeftHandle) return Handle.BOTTOM_LEFT;
		if(v==mBottomRightHandle) return Handle.BOTTOM_RIGHT;
		return null;
	}

    public void myLayout(int l, int t, int r, int b) {
        layout(l, t, r, b);
    }
}
