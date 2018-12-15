package net.pierrox.lightning_launcher.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;

public class TransformLayout extends TouchEventInterceptor {
	private static final PaintFlagsDrawFilter sPaintFlagDrawFilter=new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
	
	private View mChildView;
	private boolean mFilterChildView;
	private Rect mTransformBoundingBox=new Rect();
	private Matrix mTransform;
	private Matrix mLocalTransform;
	private Matrix mLocalInverseTransform=new Matrix();
	
	private float[] mTempPts=new float[2];
	private Rect mTempRect=new Rect();
	private RectF mTempRectF=new RectF();
	
	public TransformLayout(Context context) {
		super(context, null);
	}

	public void setFilterChildView(boolean filterChildView) {
		mFilterChildView = filterChildView;
	}

	public void setChild(View v) {
		removeAllViews();
		mChildView=v;
		mTransform=null;
		addView(mChildView);
	}
	
	public void setTransform(Matrix transform, boolean fast) {
		mTransform=transform;
        if(mTransform != null) {
            mLocalTransform = new Matrix();
        }
        updateLocalTransform(mChildView.getWidth(), mChildView.getHeight());
        if(!fast) {
        	requestLayout();
        }
	}

    private float mRotation;
    public void setRotation(int rotation) {
        mRotation = rotation;
        if(mRotation == 0 && mTransform == null) {
            mLocalTransform = null;
        } else if(mRotation != 0 && mLocalTransform == null) {
            mLocalTransform = new Matrix();
        }
        requestLayout();
    }

	@Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		if(mChildView == null) {
			setMeasuredDimension(0, 0);
		} else {
			mChildView.measure(widthMeasureSpec, heightMeasureSpec);

			int width = mChildView.getMeasuredWidth();
			int height = mChildView.getMeasuredHeight();
			updateLocalTransform(width, height);

			setMeasuredDimension(mTransformBoundingBox.width(), mTransformBoundingBox.height());
		}
	}

	public boolean resolveRtlPropertiesIfNeeded() {
		return false;
	}

	private void updateLocalTransform(int width, int height) {
		if(mTransform==null && mRotation==0) {
			mTransformBoundingBox.set(0, 0, width, height);
		} else {
			mTempRectF.set(0, 0, width, height);
            if(mTransform != null) {
                mTransform.mapRect(mTempRectF);
            }
			mTempRectF.round(mTransformBoundingBox);

            if(mTransform != null) {
			    mLocalTransform.set(mTransform);
            } else {
                mLocalTransform.reset();
            }
			mLocalTransform.postTranslate(-mTempRectF.left, -mTempRectF.top);
			mLocalTransform.postRotate(mRotation, mTransformBoundingBox.width()/2, mTransformBoundingBox.height()/2);

			mLocalTransform.invert(mLocalInverseTransform);
		}
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		if(mChildView != null) {
			mChildView.layout(0, 0, mChildView.getMeasuredWidth(), mChildView.getMeasuredHeight());
		}
	}

	@Override
    public void dispatchDraw(Canvas canvas) {
        if(mLocalTransform==null) {
            if(Build.VERSION.SDK_INT < 11) {
                if(mChildView != null) {
					mChildView.draw(canvas);
				}
            } else {
                super.dispatchDraw(canvas);
            }
        } else {
            if(mFilterChildView) canvas.setDrawFilter(sPaintFlagDrawFilter);

            canvas.save();
            canvas.concat(mLocalTransform);
//			canvas.clipRect(0, 0, mChildView.getWidth(), mChildView.getHeight());
            if(Build.VERSION.SDK_INT < 11) {
                mChildView.draw(canvas);
            } else {
                super.dispatchDraw(canvas);
            }
            canvas.restore();

            if(mFilterChildView) canvas.setDrawFilter(null);
        }
	}
	
	@Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
		if(mLocalInverseTransform==null || !mDispatchEventsToChild) {
			return super.dispatchTouchEvent(ev);
		} else {
			float old_x=ev.getX();
			float old_y=ev.getY();
			
			mTempPts[0]=old_x;
			mTempPts[1]=old_y;
			
			mLocalInverseTransform.mapPoints(mTempPts);
			
			boolean result;
			boolean send;
			if(ev.getAction()!=MotionEvent.ACTION_DOWN) {
				// filtering on the child view area is done only on DOWN, because after this action events are sent to mMotionTarget
				send=true;
			} else {
				mTempRect.set(0, 0, mChildView.getWidth(), mChildView.getHeight());
				send=(mTempRect.contains((int)mTempPts[0], (int)mTempPts[1]));
			}
			
			if(send) {
//				mTempPts[0]+=mChildView.getLeft();
//				mTempPts[1]+=mChildView.getTop();
				ev.setLocation(mTempPts[0], mTempPts[1]);
				result=super.dispatchTouchEvent(ev);
				ev.setLocation(old_x, old_y);
			} else {
				result=false;
			}
			
			return result;
		}
	}
	
	@Override
	public ViewParent invalidateChildInParent(final int[] location, final Rect dirty) {
        if(Build.VERSION.SDK_INT < 11) {
            if (mTransform == null || mLocalTransform == null) {
                return super.invalidateChildInParent(location, dirty);
            } else {
                location[0] = getLeft();
                location[1] = getTop();

                mTempRectF.set(dirty);
                mLocalTransform.mapRect(mTempRectF);
                mTempRectF.roundOut(dirty);
                return getParent();
            }
        } else {
            return super.invalidateChildInParent(location, dirty);
        }
	}
	
	public Rect getTransformBoundingBox() {
		return mTransformBoundingBox;
	}

    public Matrix getLocalTransform() {
        return mLocalTransform;
    }
}
