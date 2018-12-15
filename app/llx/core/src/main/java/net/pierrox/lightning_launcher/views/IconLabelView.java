package net.pierrox.lightning_launcher.views;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import net.pierrox.lightning_launcher.configuration.ItemConfig;
import net.pierrox.lightning_launcher.configuration.ShortcutConfig;
import net.pierrox.lightning_launcher.data.Box;
import net.pierrox.lightning_launcher.data.Box.AlignH;
import net.pierrox.lightning_launcher.data.Box.AlignV;

public class IconLabelView extends ViewGroup {
	private MyTextView mTextView;
	private IconView mIconView;
	private ItemConfig mItemConfig;
	private ShortcutConfig mShortcutConfig;

	private int mStdIconWidth;
	private int mStdIconHeight;

	public IconLabelView(Context context, String label, int std_icon_width, int std_icon_height, SharedAsyncGraphicsDrawable sharedDrawable, ItemConfig ic, ShortcutConfig sc) {
		super(context);

		mItemConfig=ic;
		mShortcutConfig=sc;
		mStdIconWidth = std_icon_width;
		mStdIconHeight = std_icon_height;
		
		updateIconVisibility(sharedDrawable);
		
    	updateLabelVisibility(label);
	}

	public void destroy() {
		if(mIconView != null) {
			mIconView.destroy();
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		final int unspecified=MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
		int mode_w = MeasureSpec.getMode(widthMeasureSpec);
		int mode_h = MeasureSpec.getMode(heightMeasureSpec);
		
		if(mIconView!=null && mTextView!=null) {
			int size_w=MeasureSpec.getSize(widthMeasureSpec);
			int size_h=MeasureSpec.getSize(heightMeasureSpec);

            ShortcutConfig.LabelVsIconPosition pos = mShortcutConfig.labelVsIconPosition;
            View first_view, second_view;
            if(pos == ShortcutConfig.LabelVsIconPosition.LEFT || pos == ShortcutConfig.LabelVsIconPosition.TOP) {
                first_view = mTextView;
                second_view = mIconView;
            } else {
                first_view = mIconView;
                second_view = mTextView;
            }

			if(first_view == mIconView /*&& mShortcutConfig.iconSizeMode != ShortcutConfig.IconSizeMode.STANDARD*/) {
				// need to measure the textview to take into account its size
				mTextView.measure(unspecified, unspecified);
				int icon_size_w;
				int icon_size_h;
				switch (pos) {
					case LEFT:
					case RIGHT:
						icon_size_w = size_w - mTextView.getMeasuredWidth();
						if(icon_size_w < 0) icon_size_w = 0;
						icon_size_h = size_h;
						break;

					case TOP:
					case BOTTOM:
						icon_size_w = size_w;
						icon_size_h = size_h - mTextView.getMeasuredHeight();
						if(icon_size_h < 0) icon_size_h = 0;
						break;

					case CENTER:
					default:
						icon_size_w = size_w;
						icon_size_h = size_h;
						break;
				}

				measureIconView(MeasureSpec.makeMeasureSpec(icon_size_w, MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(icon_size_h, MeasureSpec.AT_MOST));
			} else {
				first_view.measure(widthMeasureSpec, heightMeasureSpec);
			}
			int first_view_mw=first_view.getMeasuredWidth();
			int first_view_mh=first_view.getMeasuredHeight();

			int tv_spec_w;
			int tv_spec_h;
			int size;
			int margin=mShortcutConfig.labelVsIconMargin;
			switch(pos) {
			case LEFT:
			case RIGHT:
				if(margin<-first_view_mw) margin=-first_view_mw;
				if(mode_w==unspecified) {
					tv_spec_w=unspecified;
				} else {
					size=size_w-first_view_mw-margin;
					if(size<0) size=0;
					tv_spec_w=MeasureSpec.makeMeasureSpec(size, mode_w);
                }
				if(second_view == mIconView) {
					measureIconView(tv_spec_w, heightMeasureSpec);
				} else {
					second_view.measure(tv_spec_w, heightMeasureSpec);
				}

				setMeasuredDimension(Math.max(first_view_mw+margin+second_view.getMeasuredWidth(), first_view_mw), Math.max(first_view_mh, second_view.getMeasuredHeight()));
				break;

			case TOP:
			case BOTTOM:
				if(margin<-first_view_mh) margin=-first_view_mh;
				if(mode_h==unspecified) {
					tv_spec_h=unspecified;
				} else {
					size=size_h-first_view_mh-margin;
					if(size<0) size=0;
					tv_spec_h=MeasureSpec.makeMeasureSpec(size, mode_h);
				}
				if(second_view == mIconView) {
					measureIconView(widthMeasureSpec, tv_spec_h);
				} else {
					second_view.measure(widthMeasureSpec, tv_spec_h);
				}
				setMeasuredDimension(Math.max(first_view_mw, second_view.getMeasuredWidth()), Math.max(first_view_mh+margin+second_view.getMeasuredHeight(), first_view_mh));
				break;

            case CENTER:
                second_view.measure(widthMeasureSpec, heightMeasureSpec);
                setMeasuredDimension(Math.max(first_view_mw, second_view.getMeasuredWidth()), Math.max(first_view_mh, second_view.getMeasuredHeight()));
                break;
			}

		} else if(mTextView!=null) {
			mTextView.measure(widthMeasureSpec, heightMeasureSpec);
			setMeasuredDimension(mTextView.getMeasuredWidth(), mTextView.getMeasuredHeight());
		} else if(mIconView!=null) {
			measureIconView(widthMeasureSpec, heightMeasureSpec);
		} else {
			setMeasuredDimension(0, 0);
		}
	}

	private void measureIconView(int widthMeasureSpec, int heightMeasureSpec) {
		SharedAsyncGraphicsDrawable d = mIconView.getSharedAsyncGraphicsDrawable();
		int width = MeasureSpec.getSize(widthMeasureSpec);
		int height = MeasureSpec.getSize(heightMeasureSpec);
		d.setMaxSizeHint(width, height);

		int mw = 0, mh = 0;
		switch (mShortcutConfig.iconSizeMode) {
			case STANDARD:
			case NORMALIZED:
				mw = MeasureSpec.makeMeasureSpec(mStdIconWidth, MeasureSpec.EXACTLY);
				mh = MeasureSpec.makeMeasureSpec(mStdIconHeight, MeasureSpec.EXACTLY);
				break;

			case REAL:
				if(d.needToLoadGraphics()) {
					// don't know the size yet, so use all the available area, when the graphics is loaded at some point in the future, another layout will be done, this time with exact size
					mw = widthMeasureSpec;
					mh = heightMeasureSpec;
				} else {
					int dw = d.getIntrinsicWidth();
					int dh = d.getIntrinsicHeight();
					mw = dw == -1 ? widthMeasureSpec : MeasureSpec.makeMeasureSpec(dw, MeasureSpec.EXACTLY);
					mh = dh == -1 ? heightMeasureSpec : MeasureSpec.makeMeasureSpec(dh, MeasureSpec.EXACTLY);
				}
				break;

			case FULL_SCALE_RATIO:
				if(d.needToLoadGraphics()) {
					// don't know the size yet, so use all the available area, when the graphics is loaded at some point in the future, another layout will be done, this time with exact size
					mw = widthMeasureSpec;
					mh = heightMeasureSpec;
				} else {
					int dw = d.getIntrinsicWidth();
					int dh = d.getIntrinsicHeight();
					if(dw == -1 || dh ==-1) {
						mw = widthMeasureSpec;
						mh = heightMeasureSpec;
					} else {
						float scale = Math.min(width / (float) dw, height / (float) dh);
						mw = MeasureSpec.makeMeasureSpec(Math.round(dw*scale), MeasureSpec.EXACTLY);
						mh = MeasureSpec.makeMeasureSpec(Math.round(dh*scale), MeasureSpec.EXACTLY);
					}
				}
				break;

			case FULL_SCALE:
				mw = widthMeasureSpec;
				mh = heightMeasureSpec;
				break;
		}

		mIconView.measure(mw, mh);
		setMeasuredDimension(mIconView.getMeasuredWidth(), mIconView.getMeasuredHeight());
	}
	
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		if(mTextView!=null && mIconView!=null) {
			int w=r-l;
			int h=b-t;
			int icon_mw=mIconView.getMeasuredWidth();
			int icon_mh=mIconView.getMeasuredHeight();
			int text_mw=mTextView.getMeasuredWidth();
			int text_mh=mTextView.getMeasuredHeight();
			int margin=mShortcutConfig.labelVsIconMargin;
			int g;
			Box.AlignH ah=mItemConfig.box.ah;
			Box.AlignV av=mItemConfig.box.av;
    		switch(mShortcutConfig.labelVsIconPosition) {
    		case LEFT:
    			g=(av==AlignV.MIDDLE?(h-text_mh)/2:(av==AlignV.BOTTOM?h-text_mh:0));
    			mTextView.layout(0, g, text_mw, g+text_mh);
    			g=(av==AlignV.MIDDLE?(h-icon_mh)/2:(av==AlignV.BOTTOM?h-icon_mh:0));
    			mIconView.layout(text_mw+margin, g, text_mw+margin+icon_mw, g+icon_mh);
    			break;
    			
    		case TOP:
    			if(margin<-icon_mh) margin=-icon_mh;
    			g=(ah==AlignH.CENTER?(w-text_mw)/2:(ah==AlignH.RIGHT?w-text_mw:0));
    			mTextView.layout(g, 0, g+text_mw, text_mh);
    			g=(ah==AlignH.CENTER?(w-icon_mw)/2:(ah==AlignH.RIGHT?w-icon_mw:0));
    			mIconView.layout(g, text_mh+margin, g+icon_mw, text_mh+margin+icon_mh);
    			break;
    			
    		case RIGHT:
    			g=(av==AlignV.MIDDLE?(h-icon_mh)/2:(av==AlignV.BOTTOM?h-icon_mh:0));
    			mIconView.layout(0, g, icon_mw, g+icon_mh);
    			g=(av==AlignV.MIDDLE?(h-text_mh)/2:(av==AlignV.BOTTOM?h-text_mh:0));
    			mTextView.layout(icon_mw+margin, g, w, g+text_mh);
    			break;
    			
    		case BOTTOM:
    			if(margin<-icon_mh) margin=-icon_mh;
    			g=(ah==AlignH.CENTER?(w-icon_mw)/2:(ah==AlignH.RIGHT?w-icon_mw:0));
    			mIconView.layout(g, 0, g+icon_mw, icon_mh);
    			g=(ah==AlignH.CENTER?(w-text_mw)/2:(ah==AlignH.RIGHT?w-text_mw:0));
    			mTextView.layout(g, icon_mh+margin, g+text_mw, icon_mh+margin+text_mh);
    			break;

            case CENTER:
                g = (w-icon_mw)/2;
                int i = (h-icon_mh)/2;
                mIconView.layout(g, i, icon_mw+g, icon_mh+i);
                g = (w-text_mw)/2;
                i = (h-text_mh)/2;
                mTextView.layout(g, i, text_mw+g, text_mh+i);
                break;

    		}
		} else if(mTextView!=null) {
			mTextView.layout(0, 0, mTextView.getMeasuredWidth(), mTextView.getMeasuredHeight());
		} else if(mIconView!=null) {
			mIconView.layout(0, 0, mIconView.getMeasuredWidth(), mIconView.getMeasuredHeight());
		}
	}
	
	public MyTextView getTextView() {
		return mTextView;
	}
	
	public IconView getIconView() {
		return mIconView;
	}

    public void configureBox() {
        if(mTextView != null) {
            mShortcutConfig.applyToTextView(mTextView, mItemConfig);
        }
    }

	public void updateLabelVisibility(String label) {
		if(mShortcutConfig.labelVisibility && mTextView==null) {
			MyTextView tv=new MyTextView(getContext());
			tv.setText(label);
			mShortcutConfig.applyToTextView(tv, mItemConfig);
			addView(tv);
			mTextView=tv;
			requestLayout();
		} else if(!mShortcutConfig.labelVisibility && mTextView!=null) {
			removeView(mTextView);
			mTextView = null;
			requestLayout();
		}
	}

	public void updateIconVisibility(SharedAsyncGraphicsDrawable sharedDrawable) {
		if(mShortcutConfig.iconVisibility && mIconView==null) {
			IconView iv=new IconView(getContext(), sharedDrawable);
			addView(iv, 0);
			mIconView=iv;
		} else if(!mShortcutConfig.iconVisibility && mIconView!=null) {
			removeView(mIconView);
			mIconView = null;
		}
	}

	public void updateSharedAsyncGraphicsDrawable(SharedAsyncGraphicsDrawable sharedDrawable, int std_icon_width, int std_icon_height) {
		mStdIconWidth = std_icon_width;
		mStdIconHeight = std_icon_height;
		mIconView.setSharedAsyncGraphicsDrawable(sharedDrawable);
	}
}
