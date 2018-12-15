package net.pierrox.lightning_launcher.views;

import android.appwidget.AppWidgetHostView;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import net.pierrox.lightning_launcher.data.Box;
import net.pierrox.lightning_launcher.R;

import java.lang.reflect.Method;

public class BoxLayout extends ViewGroup implements SharedAsyncGraphicsDrawable.SharedAsyncGraphicsDrawableListener {
	private View mChildView;
	
	private Box mBox;
	private int mChildLeft;
	private int mChildTop;

    // only used if API level < 11
    private int mAlpha = 255;
	
	// margin and border rects, including the view size
	private int ml, mt, mr, mb;
	private int bl, bt, br, bb;
	
	// total amount of padding
	private int pl, pt, pr, pb; 
	
	private int mCurrentContentColor;
	private int mCurrentBorderLeftColor;
	private int mCurrentBorderTopColor;
	private int mCurrentBorderRightColor;
	private int mCurrentBorderBottomColor;

    private Drawable mBoxBgNormal;
    private Drawable mBoxBgSelected;
    private Drawable mBoxBgFocused;
    private Drawable mBoxBgFolder;

	private boolean mSelected;
	private boolean mFocused;

    private float mDensity;

    private boolean mUseSelectedContentColor;

    private Path mBorderLeftPath;
    private Path mBorderTopPath;
    private Path mBorderRightPath;
    private Path mBorderBottomPath;

    private static boolean sUpdateAppWidgetSizeChecked;
    private static Method sUpdateAppWidgetSize;

	private static Rect sTmpRect=new Rect();
    private static RectF sTmpRectF=new RectF();
	private static Paint sContentPaint;
	private static Paint sBorderPaint;
	
	static {
		sContentPaint=new Paint();
		sContentPaint.setStyle(Paint.Style.FILL);
		sBorderPaint=new Paint(sContentPaint);
	}

    private Runnable mUpdateAppWidgetHostViewSize = new Runnable() {
        @Override
        public void run() {
            int w = (Integer)mChildView.getTag(R.id.w_w);
            int h = (Integer)mChildView.getTag(R.id.w_h);
            try {
                sUpdateAppWidgetSize.invoke(mChildView, null, w, h, w, h);
            } catch (Exception e) {
                // pass
            }
        }
    };

    public BoxLayout(Context context, AttributeSet attrs) {
        this(context, attrs, false);
    }

    public BoxLayout(Context context, AttributeSet attrs, boolean hardware_accelerated) {
		super(context, attrs);

        if(Build.VERSION.SDK_INT >= 11) {
            if(hardware_accelerated) {
                setLayerType(LAYER_TYPE_HARDWARE, new Paint());
            }
        }

        mDensity = getResources().getDisplayMetrics().density;

        if(!sUpdateAppWidgetSizeChecked) {
            sUpdateAppWidgetSizeChecked = true;
            try {
                sUpdateAppWidgetSize = AppWidgetHostView.class.getMethod("updateAppWidgetSize", Bundle.class, int.class, int.class, int.class, int.class);
            } catch (Exception e) {
                // pass: API level 16
            }
        }
	}

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if(Build.VERSION.SDK_INT >= 11 && (w > 2048 || h > 2048)) {
            int layer_type = getLayerType();
            if(layer_type != LAYER_TYPE_NONE) {
                setLayerType(LAYER_TYPE_NONE, null);
            }
        }
    }

    public void setChild(View child, Box box) {
        if(child != mChildView) {
            removeAllViews();
            mChildView=child;
            addView(mChildView);
        }
		
		mBox=box;

        configureBox();
	}

    public void destroy() {
        unregisterDrawablesListeners();
    }

    public void pause() {
        if(mBoxBgNormal instanceof SharedAsyncGraphicsDrawable) ((SharedAsyncGraphicsDrawable)mBoxBgNormal).pause();
        if(mBoxBgSelected instanceof SharedAsyncGraphicsDrawable) ((SharedAsyncGraphicsDrawable)mBoxBgSelected).pause();
        if(mBoxBgFocused instanceof SharedAsyncGraphicsDrawable) ((SharedAsyncGraphicsDrawable)mBoxBgFocused).pause();
        if(mBoxBgFolder instanceof SharedAsyncGraphicsDrawable) ((SharedAsyncGraphicsDrawable)mBoxBgFolder).pause();
    }

    public void resume() {
        if(mBoxBgNormal instanceof SharedAsyncGraphicsDrawable) ((SharedAsyncGraphicsDrawable)mBoxBgNormal).resume();
        if(mBoxBgSelected instanceof SharedAsyncGraphicsDrawable) ((SharedAsyncGraphicsDrawable)mBoxBgSelected).resume();
        if(mBoxBgFocused instanceof SharedAsyncGraphicsDrawable) ((SharedAsyncGraphicsDrawable)mBoxBgFocused).resume();
        if(mBoxBgFolder instanceof SharedAsyncGraphicsDrawable) ((SharedAsyncGraphicsDrawable)mBoxBgFolder).resume();
    }

    private void registerDrawablesListeners() {
        if(mBoxBgNormal instanceof SharedAsyncGraphicsDrawable) ((SharedAsyncGraphicsDrawable)mBoxBgNormal).registerListener(this);
        if(mBoxBgSelected instanceof SharedAsyncGraphicsDrawable) ((SharedAsyncGraphicsDrawable)mBoxBgSelected).registerListener(this);
        if(mBoxBgFocused instanceof SharedAsyncGraphicsDrawable) ((SharedAsyncGraphicsDrawable)mBoxBgFocused).registerListener(this);
        if(mBoxBgFolder instanceof SharedAsyncGraphicsDrawable) ((SharedAsyncGraphicsDrawable)mBoxBgFolder).registerListener(this);
    }

    private void unregisterDrawablesListeners() {
        if(mBoxBgNormal instanceof SharedAsyncGraphicsDrawable) ((SharedAsyncGraphicsDrawable)mBoxBgNormal).unregisterListener(this);
        if(mBoxBgSelected instanceof SharedAsyncGraphicsDrawable) ((SharedAsyncGraphicsDrawable)mBoxBgSelected).unregisterListener(this);
        if(mBoxBgFocused instanceof SharedAsyncGraphicsDrawable) ((SharedAsyncGraphicsDrawable)mBoxBgFocused).unregisterListener(this);
        if(mBoxBgFolder instanceof SharedAsyncGraphicsDrawable) ((SharedAsyncGraphicsDrawable)mBoxBgFolder).unregisterListener(this);
    }

    public Box getBox() {
        return mBox;
    }

    public void setBox(Box box) {
        mBox = box;
        configureBox();
    }

    public void configureBox() {
        final int[] s=mBox.size;
        pl=s[Box.ML]+s[Box.BL]+s[Box.PL];
        pt=s[Box.MT]+s[Box.BT]+s[Box.PT];
        pr=s[Box.MR]+s[Box.BR]+s[Box.PR];
        pb=s[Box.MB]+s[Box.BB]+s[Box.PB];

        selectColors();

        mBorderLeftPath = null;
        mBorderTopPath = null;
        mBorderRightPath = null;
        mBorderBottomPath = null;

        unregisterDrawablesListeners();

        mBoxBgNormal = mBox.bgNormal;
        mBoxBgSelected = mBox.bgSelected;
        mBoxBgFocused = mBox.bgFocused;
        mBoxBgFolder = mBox.bgFolder;

        registerDrawablesListeners();

        if(mChildView instanceof IconLabelView) {
            ((IconLabelView)mChildView).configureBox();
        }

        requestLayout();
        invalidate();
    }

    @Override
    public void setAlpha(float alpha) {
        if(Build.VERSION.SDK_INT < 11) {
            mAlpha = (int) (255 * alpha);
            invalidate();
        } else {
            super.setAlpha(alpha);
        }
    }

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		final int size_w=MeasureSpec.getSize(widthMeasureSpec);
		final int size_h=MeasureSpec.getSize(heightMeasureSpec);
        if(mChildView == null) {
            setMeasuredDimension(size_w, size_h);
            return;
        }
		final int mode_w=MeasureSpec.getMode(widthMeasureSpec);
		final int mode_h=MeasureSpec.getMode(heightMeasureSpec);
		ViewGroup.LayoutParams lp=mChildView.getLayoutParams();


		int child_w=0, child_h=0;

		final int pw=pl+pr;
		final int ph=pt+pb;
		
		final int max_content_w=size_w-pw;
		final int max_content_h=size_h-ph;
		
		switch(mode_w) {
		case MeasureSpec.UNSPECIFIED: 
			child_w = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
			break;
			
		case MeasureSpec.AT_MOST:
		case MeasureSpec.EXACTLY:
			if(lp.width==ViewGroup.LayoutParams.MATCH_PARENT) {
				child_w = MeasureSpec.makeMeasureSpec(max_content_w, MeasureSpec.EXACTLY);
			} else {
				child_w = MeasureSpec.makeMeasureSpec(max_content_w, MeasureSpec.AT_MOST);
			}
			break;
		}
		
		switch(mode_h) {
		case MeasureSpec.UNSPECIFIED: 
			child_h = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
			break;
			
		case MeasureSpec.AT_MOST:
		case MeasureSpec.EXACTLY:
			if(lp.height==ViewGroup.LayoutParams.MATCH_PARENT) {
				child_h = MeasureSpec.makeMeasureSpec(max_content_h, MeasureSpec.EXACTLY);
			} else {
				child_h = MeasureSpec.makeMeasureSpec(max_content_h, MeasureSpec.AT_MOST);
			}
			break;
		}

		mChildView.measure(child_w, child_h);

		int my_w=0, my_h=0;

		switch(mode_w) {
		case MeasureSpec.UNSPECIFIED: 
		case MeasureSpec.AT_MOST:
			my_w = mChildView.getMeasuredWidth()+pw;
			break;
			
		case MeasureSpec.EXACTLY:
			my_w = size_w;
			break;
		}
		
		switch(mode_h) {
		case MeasureSpec.UNSPECIFIED: 
		case MeasureSpec.AT_MOST:
			my_h = mChildView.getMeasuredHeight()+ph;
			break;
			
		case MeasureSpec.EXACTLY:
			my_h = size_h;
			break;
		}
		
		setMeasuredDimension(my_w, my_h);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		ml=mBox.size[Box.ML];
		mt=mBox.size[Box.MT];
		mr=r-l-mBox.size[Box.MR];
		mb=b-t-mBox.size[Box.MB];

        int bs_l = mBox.size[Box.BL];
        int bs_t = mBox.size[Box.BT];
        int bs_r = mBox.size[Box.BR];
        int bs_b = mBox.size[Box.BB];
        bl=ml+ bs_l;
        bt=mt+ bs_t;
        br=mr- bs_r;
        bb=mb- bs_b;
        if(br < bl) {
            br = bl;
        }
        if(bb < bt) {
            bb = bt;
        }

        if(bs_l==0) {
            mBorderLeftPath = null;
        } else {
            if(mBorderLeftPath == null) mBorderLeftPath = new Path();
            mBorderLeftPath.rewind();
            mBorderLeftPath.moveTo(ml, mt);
            mBorderLeftPath.lineTo(bl, bt);
            mBorderLeftPath.lineTo(bl, bb);
            mBorderLeftPath.lineTo(ml, mb);
            mBorderLeftPath.close();
        }

        if(bs_t==0) {
            mBorderTopPath = null;
        } else {
            if(mBorderTopPath == null) mBorderTopPath = new Path();
            mBorderTopPath.rewind();
            mBorderTopPath.moveTo(ml, mt);
            mBorderTopPath.lineTo(bl, bt);
            mBorderTopPath.lineTo(br, bt);
            mBorderTopPath.lineTo(mr, mt);
            mBorderTopPath.close();
        }


        if(bs_r==0) {
            mBorderRightPath = null;
        } else {
            if(mBorderRightPath == null) mBorderRightPath = new Path();
            mBorderRightPath.rewind();
            mBorderRightPath.moveTo(mr, mt);
            mBorderRightPath.lineTo(br, bt);
            mBorderRightPath.lineTo(br, bb);
            mBorderRightPath.lineTo(mr, mb);
            mBorderRightPath.close();
        }


        if(bs_b==0) {
            mBorderBottomPath = null;
        } else {
            if(mBorderBottomPath == null) mBorderBottomPath = new Path();
            mBorderBottomPath.rewind();
            mBorderBottomPath.moveTo(ml, mb);
            mBorderBottomPath.lineTo(bl, bb);
            mBorderBottomPath.lineTo(br, bb);
            mBorderBottomPath.lineTo(mr, mb);
            mBorderBottomPath.close();
        }

        if(mChildView == null) {
            return;
        }

        l+=pl;
		t+=pt;
		r-=pr;
		b-=pb;
		
		int left=0, right, top=0, bottom;
		int cw=mChildView.getMeasuredWidth();
		int ch=mChildView.getMeasuredHeight();
		final int w=r-l;
		final int h=b-t;
		if(cw>w) cw=w;
		if(ch>h) ch=h;

		switch(mBox.ah) {
		case LEFT: left=0; break;
		case CENTER: left=(r-l-cw)/2; break;
		case RIGHT: left=r-l-cw; break;
		}
		switch (mBox.av) {
		case TOP: top=0; break;
		case MIDDLE: top=(b-t-ch)/2; break;
		case BOTTOM: top=b-t-ch; break;
		}
		left+=pl;
		top+=pt;
		right=left+cw;
		bottom=top+ch;

		
		mChildLeft=left;
		mChildTop=top;
        try {
		    mChildView.layout(left, top, right, bottom);
        } catch(Exception e) {
            // org.withouthat.acalendarplus/org.withouthat.acalendar.agenda.AgendaWidget
        }

        if(sUpdateAppWidgetSize!=null && mChildView instanceof AppWidgetHostView) {
            AppWidgetHostView v= (AppWidgetHostView)mChildView;
            int ww = convertToDp(cw);
            int wh = convertToDp(ch);
            Object a =  v.getTag(R.id.w_w);
            if(a != null) {
                int vw = (Integer)a;
                int vh = (Integer)v.getTag(R.id.w_h);
                if(vw!=ww || vh!=wh) {
                    v.setTag(R.id.w_w, ww);
                    v.setTag(R.id.w_h, wh);
                    removeCallbacks(mUpdateAppWidgetHostViewSize);
                    postDelayed(mUpdateAppWidgetHostViewSize, 100);
                }
            }
        }
	}

	@Override
	protected void dispatchDraw(Canvas canvas) {
        sTmpRect.set(bl, bt, br, bb);

        if(mAlpha!=255 && Build.VERSION.SDK_INT < 11) {
            sTmpRectF.set(0, 0, getWidth(), getHeight());
            canvas.saveLayerAlpha(sTmpRectF, mAlpha, Canvas.HAS_ALPHA_LAYER_SAVE_FLAG);
        }

        if(mCurrentContentColor!=0) {
            sContentPaint.setColor(mCurrentContentColor);
			canvas.drawRect(sTmpRect, sContentPaint);
		}

        if(mSelected) {
            if(mBoxBgSelected != null) {
                mBoxBgSelected.setBounds(sTmpRect);
                mBoxBgSelected.draw(canvas);
            }
        } else if(mFocused) {
            if(mBoxBgFocused != null) {
                mBoxBgFocused.setBounds(sTmpRect);
                mBoxBgFocused.draw(canvas);
            }
        } else {
            if(mBoxBgNormal != null) {
                mBoxBgNormal.setBounds(sTmpRect);
                mBoxBgNormal.draw(canvas);
            }
        }

        if(mBoxBgFolder != null) {
            mBoxBgFolder.setBounds(sTmpRect);
            mBoxBgFolder.draw(canvas);
        }

        if(mChildView != null) {
            canvas.save(Canvas.CLIP_SAVE_FLAG);
            canvas.clipRect(pl, pt, getWidth() - pr, getHeight() - pb);

            canvas.translate(mChildLeft, mChildTop);
            if (Build.VERSION.SDK_INT < 11) {
                if (mChildView.isDrawingCacheEnabled()) {
                    try {
                        Bitmap cache = mChildView.getDrawingCache();
                        canvas.drawBitmap(cache, 0, 0, null);
                    } catch (Exception e) {
                        // this will happen if something failed with the cache creation (bitmap too large, other ?)
                        mChildView.draw(canvas);
                    }
                } else {
                    mChildView.draw(canvas);
                }
            } else {
                mChildView.draw(canvas);
            }
            canvas.translate(-mChildLeft, -mChildTop);
            canvas.restore();
        }

		if(mBorderLeftPath!=null) {
			sBorderPaint.setColor(mCurrentBorderLeftColor);
            canvas.drawPath(mBorderLeftPath, sBorderPaint);
		}
		if(mBorderRightPath!=null) {
			sBorderPaint.setColor(mCurrentBorderRightColor);
            canvas.drawPath(mBorderRightPath, sBorderPaint);
		}
		if(mBorderTopPath!=null) {
			sBorderPaint.setColor(mCurrentBorderTopColor);
            canvas.drawPath(mBorderTopPath, sBorderPaint);
		}
		if(mBorderBottomPath!=null) {
			sBorderPaint.setColor(mCurrentBorderBottomColor);
            canvas.drawPath(mBorderBottomPath, sBorderPaint);
		}

        if(mAlpha!=255 && Build.VERSION.SDK_INT < 11) {
            canvas.restore();
        }
	}

    @Override
    public void setPressed(boolean pressed) {
        // hack, no op for this class only, not subclasses : avoid background state change in a specific case: window background pressed
        if(getClass() != BoxLayout.class) {
            super.setPressed(pressed);
        }
    }

    public void setItemSelected(boolean selected) {
		mSelected=selected;
		selectColors();
		invalidate();
	}
	
	public void setItemFocused(boolean focused) {
		mFocused=focused;
		selectColors();
		invalidate();
	}

    public void setUseSelectedContentColor(boolean use_selected_content_color) {
        mUseSelectedContentColor = use_selected_content_color;
    }
	
	private void selectColors() {
		int shift;
		if(mSelected) {
			shift = Box.COLOR_SHIFT_S;
			mCurrentContentColor=mUseSelectedContentColor ? mBox.ccs : mBox.ccn;
		} else if(mFocused) {
			shift = Box.COLOR_SHIFT_F;
			mCurrentContentColor=mBox.ccf;
		} else {
			shift = Box.COLOR_SHIFT_N;
			mCurrentContentColor=mBox.ccn;
		}
		final int[] s = mBox.border_color;
		mCurrentBorderLeftColor=s[shift+Box.BCL];
		mCurrentBorderTopColor=s[shift+Box.BCT];
		mCurrentBorderRightColor=s[shift+Box.BCR];
		mCurrentBorderBottomColor=s[shift+Box.BCB];
	}

    private int convertToDp(int px) {
        return (int) (px / mDensity + 0.5f);
    }

    @Override
    public void onSharedAsyncGraphicsDrawableInvalidated(SharedAsyncGraphicsDrawable drawable) {
        boolean doInvalidate;
        if(drawable == mBox.bgSelected) {
            doInvalidate = mSelected;
        } else if(drawable == mBox.bgFocused) {
            doInvalidate = mFocused;
        } else {
            doInvalidate = true;
        }
        if(doInvalidate) {
            invalidate();
        }
    }

    @Override
    public void onSharedAsyncGraphicsDrawableSizeChanged(SharedAsyncGraphicsDrawable drawable) {
        requestLayout();
    }
}
