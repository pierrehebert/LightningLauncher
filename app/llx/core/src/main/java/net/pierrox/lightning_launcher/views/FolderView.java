package net.pierrox.lightning_launcher.views;

import net.pierrox.lightning_launcher.configuration.FolderConfig;
import net.pierrox.lightning_launcher.configuration.FolderConfig.FolderAnimation;
import net.pierrox.lightning_launcher.data.Box;
import net.pierrox.lightning_launcher.data.EmbeddedFolder;
import net.pierrox.lightning_launcher.data.Folder;
import net.pierrox.lightning_launcher.data.Page;
import net.pierrox.lightning_launcher.data.Utils;
import net.pierrox.lightning_launcher.R;
import net.pierrox.lightning_launcher.views.item.ItemView;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FolderView extends FrameLayout {
    public interface OnTapOutsideListener {
        void onTapOutside(FolderView fv);
    }
	private static final int ANIM_DURATION=400;

    // the innermost container where the item layout is attached (no title, no border)
    private FrameLayout mContentContainer;

	// the container for title and content (without borders)
	private LinearLayout mInnerContainer;

	// the outer container adds borders around the inner container
    private BoxLayout mOuterContainer;


	private TextView mEmptyMsg;
	private TextView mTitle;
	private View mSeparator;
	
	private Point mSourcePoint = new Point(0, 0);
    private boolean mWaitingForLayoutToBeginAnimation = true;

    private Folder mOpener;
	private FolderConfig mFolderConfig;

	private Page mPage;
	private ItemLayout mItemLayout;
	
	private boolean mOpen;
	
	private boolean mEditMode;

    private boolean mAllowEmptyMessage = true;

    private OnTapOutsideListener mOnTapOutsideListener;

	public FolderView(Context context) {
		super(context);

        setVisibility(View.GONE);
		
		mInnerContainer = new LinearLayout(context);
		mInnerContainer.setOrientation(LinearLayout.VERTICAL);
		mInnerContainer.setGravity(Gravity.CENTER_HORIZONTAL);
		
		mContentContainer = new FrameLayout(context);

        mItemLayout=new ItemLayout(context, null);
        mContentContainer.addView(mItemLayout, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		
		mEmptyMsg=new TextView(context);
		mEmptyMsg.setGravity(Gravity.CENTER);
        mEmptyMsg.setPadding(20, 20, 20, 20);
		FrameLayout.LayoutParams elp=new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		elp.gravity=Gravity.CENTER;
        mContentContainer.addView(mEmptyMsg, elp);
		
		mTitle=new MyTextView(context);
		mTitle.setGravity(Gravity.CENTER);
		mInnerContainer.addView(mTitle, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		mSeparator=new View(context);
		mSeparator.setBackgroundColor(0xffffffff);
		mInnerContainer.addView(mSeparator, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
		mInnerContainer.addView(mContentContainer, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        mOuterContainer = new BoxLayout(context, null, true);
        addView(mOuterContainer, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
	}

    /**
     * Setup the folder view
     * @param folder mandatory
     * @param folderItemView optional, some folders like the user menu can be opened without an item view
     * @param page
     */
	public void configure(Folder folder, ItemView folderItemView, Page page) {
		mItemLayout.setOpenerItemView(folderItemView);
        mOpener = folder;
        mFolderConfig = folder.getFolderConfig();

        mAllowEmptyMessage = folder.getClass() != EmbeddedFolder.class;
        mEmptyMsg.setText(Utils.getPageForItem(folder)== Page.APP_DRAWER_PAGE ? R.string.empty_folder_x : R.string.empty_folder);
        mEmptyMsg.setTextColor(page.config.defaultShortcutConfig.labelFontColor);
		
		Box box=mFolderConfig.box;
		if(mFolderConfig.animationGlitchFix) {
			if(box.size[Box.ML]==0) box.size[Box.ML]=1;
			if(box.size[Box.MT]==0) box.size[Box.MT]=1;
			if(box.size[Box.MR]==0) box.size[Box.MR]=1;
			if(box.size[Box.MB]==0) box.size[Box.MB]=1;
		}
		mOuterContainer.setChild(mInnerContainer, box);

		int visibility=mFolderConfig.titleVisibility ? View.VISIBLE : View.GONE; 
		mTitle.setVisibility(visibility);
		mSeparator.setVisibility(visibility);
		
		if(mFolderConfig.titleVisibility) {
			mTitle.setText(folder.getLabel());
			mTitle.setTextSize(mFolderConfig.titleFontSize);
			mTitle.setTextColor(mFolderConfig.titleFontColor);
		}
		
		setLayoutParams();
		
		setPage(page);
	}

    public void setTitle(String title) {
        mTitle.setText(title);
    }
	
	public void setPage(Page page) {
        if(mPage != page) {
            mItemLayout.setPage(page);
            if (mFolderConfig.autoFindOrigin) {
                mItemLayout.setAutoFindOrigin(mPage == null);
            }
            mPage = page;
            if (mPage != null) {
                updateEmptyMessageVisibility();
            }
        }
	}

    public void setOnTapOutsideListener(OnTapOutsideListener onTapOutsideListener) {
        mOnTapOutsideListener = onTapOutsideListener;
    }

    public void updateEmptyMessageVisibility() {
        if(mPage.items.size()==0 && mAllowEmptyMessage) {
            mEmptyMsg.setVisibility(View.VISIBLE);
        } else {
            mEmptyMsg.setVisibility(View.GONE);
        }
    }

    public void setEditMode(boolean edit_mode, int[] container_offset) {
		
		mEditMode=edit_mode;
		
		if(mEditMode) {
			int[] offset=new int[2];
	    	mOuterContainer.getLocationInWindow(offset);
	    	offset[0]-=container_offset[0];
	    	offset[1]-=container_offset[1];
	    	Matrix m=mItemLayout.getLocalTransform();
	    	m.postTranslate(offset[0], offset[1]);
	    	mItemLayout.setLocalTransform(m);
	    	
	    	mOuterContainer.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
	    	mContentContainer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
	    	mInnerContainer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
	    	mItemLayout.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		} else {
            if(mFolderConfig.autoFindOrigin) {
                mItemLayout.setAutoFindOrigin(true);
            }

	    	mOuterContainer.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
	    	mContentContainer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
	    	mInnerContainer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
	    	mItemLayout.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		}
		
		setLayoutParams();
	}
	
	public void open(Point source_point, boolean containerIsResumed) {
		mSourcePoint=source_point;
		setVisibility(View.VISIBLE);
        mWaitingForLayoutToBeginAnimation = true;
		mOpen=true;
        if(containerIsResumed) {
            resume();
        }
	}

    public void skipOpenAnimation() {
        mWaitingForLayoutToBeginAnimation = false;
    }
	
	public void close(boolean animate, boolean containerIsResumed) {
		mOpen=false;
        if(containerIsResumed) {
            pause();
        }
		if(animate) {
            Animation animation = getAnimation(false);
            if(animation != null) {
                mOuterContainer.startAnimation(animation);
                return;
            }
		}
        setVisibility(View.GONE);
    }

    public void resume() {
        mOuterContainer.resume();
        mItemLayout.resume();
    }

    public void pause() {
        mOuterContainer.pause();
        mItemLayout.pause();
    }

    public void destroy() {
	    mOuterContainer.destroy();
    }

    public Folder getOpener() {
        return mOpener;
    }

    public ItemView getOpenerItemView() {
		return mItemLayout.getOpenerItemView();
	}

    public void setOpenerItemView(ItemView openerItemView) {
        mItemLayout.setOpenerItemView(openerItemView);
    }

    public Page getPage() {
		return mPage;
	}
	
	public boolean isOpen() {
		return mOpen;
	}

    public ItemLayout getItemLayout() {
        return mItemLayout;
    }
	
	private void setLayoutParams() {
		// assume a FrameLayout container
        final int w = mFolderConfig.wW;
        final int h = mFolderConfig.wH;
        final Box.AlignV av = mFolderConfig.wAV;
        final Box.AlignH ah = mFolderConfig.wAH;

        int width=mEditMode ? ViewGroup.LayoutParams.MATCH_PARENT : w==0 ? ViewGroup.LayoutParams.WRAP_CONTENT : w;
        int height=mEditMode ? ViewGroup.LayoutParams.MATCH_PARENT : h==0 ? ViewGroup.LayoutParams.WRAP_CONTENT : h;

        FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(width, height);
        int gravity=0;
        switch (ah) {
            case LEFT: gravity |= Gravity.LEFT; break;
            case CENTER: gravity |= Gravity.CENTER_HORIZONTAL; break;
            case RIGHT: gravity |= Gravity.RIGHT; break;
            case CUSTOM: lp.leftMargin = mEditMode ? 0 : mFolderConfig.wX; break;
        }
        switch (av) {
            case TOP: gravity |= Gravity.TOP; break;
            case MIDDLE: gravity |= Gravity.CENTER_VERTICAL; break;
            case BOTTOM: gravity |= Gravity.BOTTOM; break;
            case CUSTOM: lp.topMargin = mEditMode ? 0 : mFolderConfig.wY; break;
        }
        lp.gravity = gravity;

		mOuterContainer.setLayoutParams(lp);

        requestLayout();
	}

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        if(mWaitingForLayoutToBeginAnimation) {
            Animation animation = getAnimation(true);
            if(animation != null) {
                mOuterContainer.startAnimation(animation);
            }
            mWaitingForLayoutToBeginAnimation = false;
        }
    }

    @Override
    public void setOnLongClickListener(View.OnLongClickListener l) {
        // redirect events on the real folder container
	    mOuterContainer.setOnLongClickListener(l);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
	    if(mFolderConfig.outsideTapClose && event.getAction() == MotionEvent.ACTION_DOWN) {
	        if(mOnTapOutsideListener != null) {
	            mOnTapOutsideListener.onTapOutside(this);
            }
            return true;
        }
        return false;
    }

    private Animation getAnimation(final boolean in) {
        AnimationSet a=new AnimationSet(true);
        
        FolderAnimation anim=in ? mFolderConfig.animationIn : mFolderConfig.animationOut;
        switch(anim) {
        case NONE:
        	break;
        	
        case OPEN_CLOSE:
        	final float from=in?0:1;
        	final float to=in?1:0;

            int cx, cy;
            Rect r = new Rect();
            getHitRect(r);
            if(r.isEmpty()) {
                return null;
            }

            cx = r.centerX();
            cy = r.centerY();
        	final float tx=(float)(mSourcePoint.x-cx);
        	final float ty=(float)(mSourcePoint.y-cy);
        	a.addAnimation(new ScaleAnimation(from, to, from, to, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f));
            a.addAnimation(new TranslateAnimation(in?tx:0, in?0:tx, in?ty:0, in?0:ty));
            break;

        case SLIDE_FROM_LEFT:
            if(in) {
                int type = mFolderConfig.wAH==Box.AlignH.LEFT ? Animation.RELATIVE_TO_SELF : Animation.RELATIVE_TO_PARENT;
                a.addAnimation(new TranslateAnimation(type, -1, type, 0, Animation.ABSOLUTE, 0, Animation.ABSOLUTE, 0));
            } else {
                int type = mFolderConfig.wAH==Box.AlignH.RIGHT ? Animation.RELATIVE_TO_SELF : Animation.RELATIVE_TO_PARENT;
                a.addAnimation(new TranslateAnimation(type, 0, type, 1, Animation.ABSOLUTE, 0, Animation.ABSOLUTE, 0));
            }
            break;

        case SLIDE_FROM_RIGHT:
            if(in) {
                int type = mFolderConfig.wAH==Box.AlignH.RIGHT ? Animation.RELATIVE_TO_SELF : Animation.RELATIVE_TO_PARENT;
                a.addAnimation(new TranslateAnimation(type, 1, type, 0, Animation.ABSOLUTE, 0, Animation.ABSOLUTE, 0));
            } else {
                int type = mFolderConfig.wAH==Box.AlignH.LEFT ? Animation.RELATIVE_TO_SELF : Animation.RELATIVE_TO_PARENT;
                a.addAnimation(new TranslateAnimation(type, 0, type, -1, Animation.ABSOLUTE, 0, Animation.ABSOLUTE, 0));
            }
            break;

        case SLIDE_FROM_TOP:
            if(in) {
                int type = mFolderConfig.wAV==Box.AlignV.TOP ? Animation.RELATIVE_TO_SELF : Animation.RELATIVE_TO_PARENT;
                a.addAnimation(new TranslateAnimation(Animation.ABSOLUTE, 0, Animation.ABSOLUTE, 0, type, -1, type, 0));
            } else {
                int type = mFolderConfig.wAV==Box.AlignV.BOTTOM? Animation.RELATIVE_TO_SELF : Animation.RELATIVE_TO_PARENT;
                a.addAnimation(new TranslateAnimation(Animation.ABSOLUTE, 0, Animation.ABSOLUTE, 0, type, 0, type, 1));
            }
            break;

        case SLIDE_FROM_BOTTOM:
            if(in) {
                int type = mFolderConfig.wAV==Box.AlignV.BOTTOM ? Animation.RELATIVE_TO_SELF : Animation.RELATIVE_TO_PARENT;
                a.addAnimation(new TranslateAnimation(Animation.ABSOLUTE, 0, Animation.ABSOLUTE, 0, type, 1, type, 0));
            } else {
                int type = mFolderConfig.wAV==Box.AlignV.TOP ? Animation.RELATIVE_TO_SELF : Animation.RELATIVE_TO_PARENT;
                a.addAnimation(new TranslateAnimation(Animation.ABSOLUTE, 0, Animation.ABSOLUTE, 0, type, 0, type, -1));
            }
        	break;
        }
        if(mFolderConfig.animFade) {
            a.addAnimation(new AlphaAnimation(in?0:1, in?1:0));
        }
        
        a.setDuration(ANIM_DURATION);
        a.setAnimationListener(new Animation.AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) { }
			
			@Override
			public void onAnimationRepeat(Animation animation) { }
			
			@Override
			public void onAnimationEnd(Animation animation) {
				if(!in) {
					setVisibility(View.GONE);
				}
			}
		});
        return a;
	}
}
