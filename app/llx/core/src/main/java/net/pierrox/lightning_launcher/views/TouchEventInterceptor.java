package net.pierrox.lightning_launcher.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

public abstract class TouchEventInterceptor extends ViewGroup {

	private static final int sLongPressTimeout = ViewConfiguration.getLongPressTimeout();

	public interface OnInterceptorListener {
		public void onInterceptPressed();
		public void onInterceptUnpressed();
		public void onInterceptMove(float dx, float dy);
		public void onInterceptClicked();
		public void onInterceptLongClicked();
        public boolean onInterceptTouch(MotionEvent e);
	}
	
	private static int sTouchSlope;

	private OnInterceptorListener mOnInterceptorListener; 
	protected boolean mDispatchEventsToChild=true;
	private boolean mHasLongClicked;
	private boolean mKeepNextEvents;
	private boolean mMoveStarted;
	private float mMoveStartX;
	private float mMoveStartY;
	private float mCurrentDx, mCurrentDy;

	private Runnable mLongClickRunnable=new Runnable() {
		@Override
		public void run() {
			if(inSlopeArea()) {
				mHasLongClicked=true;
				if(mOnInterceptorListener!=null) mOnInterceptorListener.onInterceptLongClicked();
			}
		}
	};
	
	public TouchEventInterceptor(Context context, AttributeSet attrs) {
		super(context, attrs);
		if(sTouchSlope==0) sTouchSlope=ViewConfiguration.get(context).getScaledTouchSlop();
	}
	
	public void setOnInterceptorListener(OnInterceptorListener l) {
		mOnInterceptorListener=l;
	}

	public void setDispatchEventsToChild(boolean d) {
		mDispatchEventsToChild=d;
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		if(!isEnabled()) {
			return false;
		}

        if(mOnInterceptorListener != null && mOnInterceptorListener.onInterceptTouch(ev)) {
            return true;
        }
		
		if(mOnInterceptorListener!=null) {
			float x=ev.getX();
			float y=ev.getY();
			float abs_x=x+getLeft();
			float abs_y=y+getTop();
			mCurrentDx=abs_x-mMoveStartX;
			mCurrentDy=abs_y-mMoveStartY;
			int action=ev.getAction();
			switch(action) {
			case MotionEvent.ACTION_DOWN:
				mKeepNextEvents=false;
				mOnInterceptorListener.onInterceptPressed();
				mHasLongClicked=false;
				mMoveStarted=false;
				mMoveStartX=abs_x;
				mMoveStartY=abs_y;
				mCurrentDx=0;
				mCurrentDy=0;
				postDelayed(mLongClickRunnable, sLongPressTimeout);
				break;
				
			case MotionEvent.ACTION_MOVE:
				if(!mMoveStarted) {
					if(!inSlopeArea()) {
						removeCallbacks(mLongClickRunnable);
						mMoveStarted=true;
					}
				}
				if(mMoveStarted) {
					mOnInterceptorListener.onInterceptMove(mCurrentDx, mCurrentDy);
				}
				break;
				
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				mOnInterceptorListener.onInterceptUnpressed();
				removeCallbacks(mLongClickRunnable);
				if(!mHasLongClicked && (ev.getEventTime()-ev.getDownTime()) >= sLongPressTimeout) {
					mLongClickRunnable.run();
				}
				if(mHasLongClicked) {
					ev.setAction(MotionEvent.ACTION_CANCEL);
					try {
						super.dispatchTouchEvent(ev);
					} catch(Throwable e) {
					}
					return true;
				} else {
					if(inSlopeArea() && action==MotionEvent.ACTION_UP) mOnInterceptorListener.onInterceptClicked();
				}
				break;
			}
	
			if(mDispatchEventsToChild && !mKeepNextEvents) {
				boolean res;
				try {
					res=super.dispatchTouchEvent(ev);
				} catch(Throwable e) {
					res=false;
				}
				if(res==false) {
					mKeepNextEvents=true;
				}
			}
			return true;
		} else {
			try {
				return super.dispatchTouchEvent(ev);
			} catch(Throwable e) {
				return false;
			}
		}
	}
	
	private boolean inSlopeArea() {
		return Math.abs(mCurrentDx)<sTouchSlope && Math.abs(mCurrentDy)<sTouchSlope;
	}
}
