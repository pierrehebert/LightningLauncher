package net.pierrox.lightning_launcher.views;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AbsoluteLayout;

import net.pierrox.lightning_launcher_extreme.R;

@SuppressWarnings("deprecation")
public class BubbleLayout extends AbsoluteLayout {

	private float mDensity;
    private int mScreenPadding;
	private Rect mItemBounds;
    private int mBubbleArrowShift;
	
	public BubbleLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
        mBubbleArrowShift = getContext().getResources().getDimensionPixelSize(R.dimen.bubble_arrow_shift);
	}

    public void setScreenPadding(int padding) {
        mScreenPadding = padding;
    }
	
	public void setItemBounds(Rect item_bounds) {
		mItemBounds = new Rect(item_bounds);
		mItemBounds.offset(0, -mScreenPadding);
		requestLayout();
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		if(mItemBounds == null) {
			return;
		}
		
		View bubble = getChildAt(0);
		View arrow_down = getChildAt(1);
		View arrow_up = getChildAt(2);
		
		final int w = getWidth();
		final int h = getHeight();
		
		boolean above = (h-mItemBounds.bottom) < mItemBounds.top;
		
		View arrow;
		if(above) {
			arrow = arrow_down;
			arrow_up.layout(-1, -1, -1, -1);
		} else {
			arrow = arrow_up;
			arrow_down.layout(-1, -1, -1, -1);
		}
		
		int b_left, b_right, b_top, b_bottom;
		int a_left, a_right, a_top, a_bottom;
		
		
		
		final int bw = bubble.getMeasuredWidth();
		final int bh = bubble.getMeasuredHeight();
		
		final int aw = arrow.getMeasuredWidth();
		final int ah = arrow.getMeasuredHeight();
		
		final int i_center = mItemBounds.centerX();
		
		b_left = i_center - bw/2;
		if(b_left<0) b_left=0;
		b_right = b_left+bw;
		if(b_right>w) {
			b_left = w-bw;
			b_right = w;
		}
		
		a_left = i_center - aw/2;
		if(a_left<0) a_left = 0;
		a_right = a_left + aw;
		if(a_right > w) {
			a_left = w - aw;
			a_right = w;
		}
		
		int shift=Math.round(mDensity*mBubbleArrowShift/1.5f)-1;

        int anchor_y;
//        int max_height_delta = h/3;
//        if(mItemBounds.height() < max_height_delta) {
//            anchor_y =above ? mItemBounds.top : mItemBounds.bottom;
//        } else {
        int delta;
        if(mItemBounds.height()==0) {
            delta = 0;
        } else {
            int mh = mItemBounds.height() / 2;
            delta = Math.min(mh, ah * 2);
        }
        anchor_y = mItemBounds.centerY() + (above ? -delta : delta);
//        }

		if(above) {
			// above
			b_bottom = anchor_y - ah + shift;
			b_top = b_bottom - bh;
			
			a_bottom = anchor_y;
			a_top = a_bottom - ah;
			
		} else {
			// below
			b_top = anchor_y + ah - shift;
			b_bottom = b_top + bh;
			
			a_top = anchor_y;
			a_bottom = a_top + ah;
		}
		
		if(b_top<0) {
			b_top=above ? 0 : ah;
			int height_diff=bh-(b_bottom-b_top);
			if(height_diff>0) {
				b_bottom+=height_diff;
				a_top+=height_diff;
				a_bottom+=height_diff;
			}
            if(above) {
                int max_height = anchor_y;//h/2-ah;
                int height = b_bottom - b_top;
                if (height > max_height) {
                    height_diff = max_height - height;
                    b_bottom += height_diff;
                    a_top += height_diff;
                    a_bottom += height_diff;
                }
            }
		}
		if(b_bottom>h) b_bottom=h;
		
		bubble.layout(b_left, b_top, b_right, b_bottom);
		arrow.layout(a_left, a_top, a_right, a_bottom);
	}

	public void setDensity(float density) {
		mDensity = density;
	}
}
