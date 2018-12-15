package net.pierrox.lightning_launcher.views;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import net.pierrox.lightning_launcher.LLApp;
import net.pierrox.lightning_launcher.configuration.GlobalConfig;
import net.pierrox.lightning_launcher.configuration.ItemConfig;
import net.pierrox.lightning_launcher.configuration.PageConfig;
import net.pierrox.lightning_launcher.configuration.PageConfig.OverScrollMode;
import net.pierrox.lightning_launcher.configuration.PageConfig.SizeMode;
import net.pierrox.lightning_launcher.configuration.SystemConfig;
import net.pierrox.lightning_launcher.data.Box;
import net.pierrox.lightning_launcher.data.DynamicText;
import net.pierrox.lightning_launcher.data.EmbeddedFolder;
import net.pierrox.lightning_launcher.data.Folder;
import net.pierrox.lightning_launcher.data.Item;
import net.pierrox.lightning_launcher.data.Page;
import net.pierrox.lightning_launcher.data.PageIndicator;
import net.pierrox.lightning_launcher.data.Shortcut;
import net.pierrox.lightning_launcher.data.StopPoint;
import net.pierrox.lightning_launcher.data.Utils;
import net.pierrox.lightning_launcher.data.Widget;
import net.pierrox.lightning_launcher.BuildConfig;
import net.pierrox.lightning_launcher.R;
import net.pierrox.lightning_launcher.engine.Screen;
import net.pierrox.lightning_launcher.engine.variable.Binding;
import net.pierrox.lightning_launcher.views.item.CustomViewView;
import net.pierrox.lightning_launcher.views.item.EmbeddedFolderView;
import net.pierrox.lightning_launcher.views.item.ErrorItemView;
import net.pierrox.lightning_launcher.views.item.ItemView;
import net.pierrox.lightning_launcher.views.item.PageIndicatorView;
import net.pierrox.lightning_launcher.views.item.ShortcutView;
import net.pierrox.lightning_launcher.views.item.StopPointView;
import net.pierrox.lightning_launcher.views.item.UnlockerView;
import net.pierrox.lightning_launcher.views.item.WidgetView;

import android.annotation.TargetApi;
import android.app.WallpaperManager;
import android.content.ClipDescription;
import android.content.Context;
import android.content.pm.ShortcutInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.BounceInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.Transformation;
import android.widget.AbsListView;
import android.widget.CheckBox;
import android.widget.ScrollView;
import android.widget.Scroller;

public class ItemLayout extends ViewGroup {
	public static final int POSITION_FREE=0;
	public static final int POSITION_ORIGIN=1;
	public static final int POSITION_FULL_SCALE=2;
	
	private static final long ANIMATION_DURATION=400;

	public static final long ANIMATION_EDIT_MODE_DURATION = 200;

    public static final int MAX_EDIT_GRID_CROSS_COUNT = 500;
    public static final int MAX_EDIT_GRID_LINE_COUNT = 50;

    private static final int GESTURE_NONE=0;
    private static final int GESTURE_MAYBE_DRAG=1;
    private static final int GESTURE_DRAG=2;
    private static final int GESTURE_MAYBE_ZOOM=3;
    private static final int GESTURE_ZOOM=4;
    private static final int GESTURE_SWIPE=5;
    
    private static final Paint sHighlightedCellsPaint;
	private static Paint sGridDotPaint=new Paint();
	private static Bitmap sPinnedItemXY;
	private static Bitmap sPinnedItemX;
	private static Bitmap sPinnedItemY;
	private static Bitmap sCheckBox;

    static {
		sGridDotPaint.setStyle(Paint.Style.STROKE);
		sGridDotPaint.setColor(0x80ffffff);
		
		sHighlightedCellsPaint=new Paint();
		sHighlightedCellsPaint.setStyle(Paint.Style.FILL);
		sHighlightedCellsPaint.setColor(0x508080ff);
    }

    public interface ItemLayoutListener extends HandleView.OnHandleViewEventListener {
		public void onItemLayoutPressed();
		public void onItemLayoutClicked(ItemLayout item_layout, int x, int y);
		public void onItemLayoutDoubleClicked(ItemLayout item_layout, int x, int y);
		public void onItemLayoutLongClicked(ItemLayout item_layout, int x, int y);
		public void onItemLayoutSwipeLeft(ItemLayout item_layout);
		public void onItemLayoutSwipeRight(ItemLayout item_layout);
		public void onItemLayoutSwipeUp(ItemLayout item_layout);
		public void onItemLayoutSwipeDown(ItemLayout item_layout);
		public void onItemLayoutSwipe2Left(ItemLayout item_layout);
		public void onItemLayoutSwipe2Right(ItemLayout item_layout);
		public void onItemLayoutSwipe2Up(ItemLayout item_layout);
		public void onItemLayoutSwipe2Down(ItemLayout item_layout);
		public void onItemLayoutZoomChanged(float scale);
		public void onItemLayoutPinchStart();
		public boolean onItemLayoutPinch(float scale);
		public void onItemLayoutPinchEnd(boolean from_user);
		public void onItemLayoutOnLayoutDone(ItemLayout item_layout);
        public void onItemLayoutSizeChanged(ItemLayout item_layout, int w, int h, int oldw, int oldh);
		public void onItemLayoutPositionChanged(ItemLayout il, float mCurrentDx, float mCurrentDy, float mCurrentScale);
		public void onItemLayoutStopPointReached(ItemLayout item_layout, StopPoint sp);
		public void onItemLayoutWindowSystemUiVisibility(ItemLayout il, int visibility);
        public void onItemLayoutMasterSelectedItemChanged(Item masterSelectedItem);
        public void onItemLayoutPageLoaded(ItemLayout itemLayout, Page oldPage, Page newPage);
        public void onItemLayoutAppShortcutDropped(ItemLayout itemLayout, Object shortcutInfo, float x, float y);
	}
	
	private Page mPage;

    private boolean mIsResumed;

    private boolean mInvalidated;

    private ItemView mOpenerItemView;
    private SparseArray<ItemView> mItemViews = new SparseArray<>();

    private ArrayList<ItemView> mDelayedItemViews;
    private boolean mDelayedItemViewsSorted;
    private boolean mDelayedItemViewLoadScheduled;
    private boolean mAllowDelayedViewInit = true;
    private boolean mAllowMergeViews;

    private ArrayList<StopPoint> mStopPoints;
	private ArrayList<PageIndicatorView> mPageIndicators;
    private boolean mHaveStopPoints;

    private Screen mScreen;

	// only for grid layout mode
	private float mCellWidth;
	private float mCellHeight;
	
	private Rect mItemsBoundingBox=new Rect();
	private Rect mCustomItemBoundingBox;
	private boolean mItemsBoundingBoxChanged;
	
	// for scrolling wallpaper
	private WallpaperManager mWallpaperManager;
//    private float mCurrentWallpaperOffsetX;
//    private float mCurrentWallpaperOffsetY;
    
//	private Bitmap mWallpaper; // only for software wallpaper
	private Paint mGridLinePaint;
    private BoxLayout mVirtualEditBorders;
    private RectF mVirtualEditBordersBounds;
    private float mVirtualEditBordersScale;

	private boolean mAllowWrap = true;
	private boolean mEditMode;
	private boolean mEditFolder;
	private Bitmap mOriginIcon;

	private SparseArray<Rect> mHighlightedCells = new SparseArray<>();

    private float mHoloHighlightPadding;

    private int mAppDrawerHiddenHandling = Item.APP_DRAWER_HIDDEN_ONLY_VISIBLE;

    private boolean mIsEmbedded;

    private boolean mForceUseDesktopSize;
    private int mDesktopWidth;
    private int mDesktopHeight;

	private float mSnappingGuideX = Float.MAX_VALUE;
	private float mSnappingGuideY = Float.MAX_VALUE;
    private ArrayList<RectF> mSnappingBounds;

	private boolean mAutoFindOrigin;

    private boolean mIsPortrait;

    private long mAnimationDate;

    private static class ItemViewTracking {
        private ItemView mItemView;
        private float dx;
        private float dy;

        public ItemViewTracking(ItemView itemView) {
            this.mItemView = itemView;
            dx = dy = 0;
        }
    }

	private ArrayList<ItemViewTracking> mTrackedItems = new ArrayList<>(1);

	private ItemView mHandleItemView;
	private int mHandleViewPadding;
	private int mHandleViewPaddingX2;

    private boolean mHandleViewVisible;
    private HandleView mHandleView;

	private Item mSelectedItem;
    private Paint mSelectionOutlinePaint;
    private Paint mSelectionFillPaint;

    private boolean mHonourPinnedItems;

    private boolean mAlwaysShowStopPoints;

	private View mGrabEventTarget;

	private int mClickCount;
	private boolean mHasLongClicked;
	private float mDownX;
	private float mDownY;
	private Runnable mClickRunnable=new Runnable() {
		@Override
		public void run() {
			mClickCount=0;
            int[] pos = convertTapPositionToLayoutCoordinates();
			if(mScreen !=null)  mScreen.onItemLayoutClicked(ItemLayout.this, pos[0], pos[1]);
		}
	};
	private Runnable mLongClickRunnable=new Runnable() {
		@Override
		public void run() {
			int[] pos = convertTapPositionToLayoutCoordinates();
			mHasLongClicked=true;
			if(mScreen !=null)  mScreen.onItemLayoutLongClicked(ItemLayout.this, pos[0], pos[1]);
		}
	};


    private Matrix mLocalTransform=new Matrix();
    private Matrix mLocalTransformPinX=new Matrix();
    private Matrix mLocalTransformPinY=new Matrix();
	private Matrix mLocalInverseTransform=new Matrix();
    private Matrix mLocalInverseTransformPinX=new Matrix();
    private Matrix mLocalInverseTransformPinY=new Matrix();
    private float[] mTempPts=new float[2];
    private Rect mTempRect=new Rect();
    private RectF mTempRectF=new RectF();
    private Transformation mTempTransformation = new Transformation();
    private float[] mTempGridCrossPts;


    private View mMyMotionTarget;
    private ItemView mLastTouchedItemView;

    private int mGestureMode=GESTURE_NONE;

 // coordinates of the multitouch mid point in screen coordinates
    private float mMidPointX;
    private float mMidPointY;
    // coordinates of the multitouch mid point in real coordinates
    private float mMidPointRx;
    private float mMidPointRy;
    private float mInitialDist=1f;

    //private int mCurrentPosition;
    private static final DecelerateInterpolator mDecelerateInterpolator=new DecelerateInterpolator();
    private static final BounceInterpolator mBounceInterpolator=new BounceInterpolator();
    private Interpolator mOverScrollInterpolator;
    private Interpolator mAnimInterpolator;
    private Matrix mAnimTransform=new Matrix();
    private long mAnimDurationStartDate;
    private float mAnimNavigationToScale;
    private float mAnimNavigationFromScale;
    private float mAnimNavigationFromX;
    private float mAnimNavigationFromY;
    private float mAnimNavigationToX;
    private float mAnimNavigationToY;
    private long mAnimNavigationDuration;
    private boolean mAnimatingNavigation;

    private boolean mAnimatingEditMode;
    private long mAnimEditModeStartDate;

    private float[] mLocalTransformMatrixValues=new float[9];

    private boolean mAllowOverScroll; // ability to scroll without bounce effect (used for dashboard layout)
    private float mMinDx, mMaxDx, mMinDy, mMaxDy;
    private float mOverDx, mOverDy;

    private float mCurrentScale=1;
    private float mCurrentDx;
    private float mCurrentDy;

    // for script
    private float mCurrentX;
    private float mCurrentY;
    private float mCurrentScaleS=1;

    private float mDragDx, mDragDy;
    private float mMinDragDx, mMinDragDy;
    private float mMaxDragDx, mMaxDragDy;

    private boolean mAllowFling=true;
    private VelocityTracker mVelocityTracker;
    private float mScrollVelocityX;
    private float mScrollVelocityY;
    private float mScrollDx;
    private float mScrollDy;
    private boolean mFlinging;
    private int mTouchSlop;
	private int mTouchSlopSquare;
	private float mMinFlingVelocitySquare;
    private Scroller mFlingScroller;
    private StopPoint mSnapStopPoint;

    private RectF mMyRect=new RectF();
    private RectF mMyPrefetchRect=new RectF();
    private boolean mAutoScrolling;
    private int mAutoScrollMargin;
    private float mAutoScrollDx;
    private float mAutoScrollDy;
    private Rect mAutoScrollRect=new Rect();
	private boolean mAllowScrollX;
	private boolean mAllowScrollY;

	private Method mMotionEventGetX, mMotionEventGetY;

	public ItemLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        if(sPinnedItemXY==null) {
        	sPinnedItemXY=BitmapFactory.decodeResource(context.getResources(), R.drawable.pin_xy);
        	sPinnedItemX=BitmapFactory.decodeResource(context.getResources(), R.drawable.pin_x);
        	sPinnedItemY=BitmapFactory.decodeResource(context.getResources(), R.drawable.pin_y);
        }

        if(sCheckBox==null) {
            int size = Utils.getStandardIconSize() / 2;
            sCheckBox = Utils.createIconFromText(size, "p", context.getResources().getColor(R.color.color_primary));
        }

        mHandleView=new HandleView(context);
        mHandleViewPadding = mHandleView.getHandleSize();
        mHandleViewPaddingX2 = mHandleViewPadding * 2;
        mHandleView.setVisibility(View.GONE);

        mSelectionOutlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mSelectionOutlinePaint.setStyle(Paint.Style.STROKE);
        mSelectionOutlinePaint.setStrokeWidth(2);
        mSelectionFillPaint = new Paint();
        mSelectionFillPaint.setStyle(Paint.Style.FILL);

        final ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop=configuration.getScaledTouchSlop();
        mTouchSlopSquare=mTouchSlop*mTouchSlop;
        final int minimumFlingVelocity=configuration.getScaledMinimumFlingVelocity();
        mMinFlingVelocitySquare = minimumFlingVelocity*minimumFlingVelocity;
        mFlingScroller = new Scroller(context);
        mAutoScrollMargin = configuration.getScaledEdgeSlop();//Utils.getStandardIconSize()/3;

        try {
			mMotionEventGetX=MotionEvent.class.getMethod("getX", int.class);
			mMotionEventGetY=MotionEvent.class.getMethod("getY", int.class);
		} catch (Exception e) {
			mMotionEventGetX=null;
			mMotionEventGetY=null;
		}

		mGridLinePaint=new Paint();
		mGridLinePaint.setStyle(Paint.Style.STROKE);

		mOriginIcon=Utils.decodeScaledBitmapResource(getResources(), R.drawable.home, Utils.getStandardIconSize());

        mHoloHighlightPadding = HolographicOutlineHelper.obtain(getContext()).mMaxOuterBlurRadius;

        mHonourPinnedItems = true;
	}

    @Override
    public String toString() {
        return mPage==null ? "IL:no page yet ("+hashCode()+")" : "IL:"+mPage.id+" ("+hashCode()+")";
    }

    public void destroy() {
        if(mSetWallpaperOffsetThread != null) {
            mSetWallpaperOffsetThread.end();
        }

        grabEvent(null);

        if(mDelayedItemViews != null) {
            cancelDelayedItemViewLoad();
            mDelayedItemViews = null;
        }

        mAnimatingNavigation = false;
        mFlinging = false;

        for(int n=mItemViews.size()-1; n>=0; n--) {
            mItemViews.valueAt(n).destroy();
        }

        if(mVirtualEditBorders != null) {
            mVirtualEditBorders.destroy();
        }

        mPage.getEngine().getScriptExecutor().getLightning().clearCachedContainer(this);
    }

    public boolean isResumed() {
        return mIsResumed;
    }

    public void pause() {
        if(mIsResumed) {
            mIsResumed = false;
            mPage.pause();
            int count = getChildCount();
            for (int n = 0; n < count; n++) {
                View childView = getChildAt(n);
                if (childView instanceof ItemView) {
                    ItemView itemView = (ItemView) childView;
                    if(itemView.isInitDone()) {
                        itemView.pause();
                    }
                }
            }
            mScreen.runAction(mPage.getEngine(), "C_PAUSED", mPage.config.paused, this, null);
            if(mDelayedItemViews != null) {
                cancelDelayedItemViewLoad();
            }
        }
    }

    public void resume() {
        if(!mIsResumed) {
            mIsResumed = true;
            int count = getChildCount();
            for (int n = 0; n < count; n++) {
                View childView = getChildAt(n);
                if (childView instanceof ItemView) {
                    ItemView itemView = (ItemView) childView;
                    if(itemView.isInitDone()) {
                        itemView.resume();
                    }
                }
            }
            mPage.resume();
            mScreen.runAction(mPage.getEngine(), "C_RESUMED", mPage.config.resumed, this, null);
            if(mDelayedItemViews != null) {
                loadNextItemViewLater();
            }
        }
    }

    public void setScreen(Screen screen) {
        mScreen = screen;
        mHandleView.setOnHandleViewEventListener(screen);

        int orientation=mScreen.getResourcesOrientation();
        mIsPortrait = (orientation == Configuration.ORIENTATION_PORTRAIT);
    }

    public Screen getScreen() {
        return mScreen;
    }

    public void setAllowMergeViews(boolean allowMergeViews) {
        mAllowMergeViews = allowMergeViews;
    }

    public Page getPage() {
        return mPage;
    }

    public void setPage(Page page) {
        if(page == null) {
            throw new RuntimeException("ItemLayout.setPage null is not allowed");
        }

//        if(mPage == page) {
//            return;
//        }

        if(mPage != null) {
            mAnimatingNavigation = false;
            mFlinging = false;
            removeCallbacks(mLongClickRunnable);
            removeCallbacks(mClickRunnable);
            cancelDelayedItemViewLoad();

            if(!mAllowMergeViews) {
                if(mDelayedItemViews != null) {
                    mDelayedItemViews = null;
                }
                for (Item item : mPage.items) {
                    removeViewForItem(item);
                }
                if (mIsResumed) {
                    mPage.pause();
                }
            }

            mTrackedItems.clear();
            mHandleItemView = null;
            mHandleView.setVisibility(View.GONE);
            mHandleViewVisible = false;
            mHaveStopPoints = false;
            mStopPoints = null;
            mPageIndicators = null;
            mSnapStopPoint = null;
        }

        Page oldPage = mPage;
        mPage = page;

        mAlwaysShowStopPoints = LLApp.get().getSystemConfig().alwaysShowStopPoints;
        // TODO register for alwaysShowStopPoints changes

        loadPage(mAllowMergeViews);

        mScreen.onItemLayoutPageLoaded(this, oldPage, mPage);

        mScreen.runAction(page.getEngine(), "C_LOADED", page.config.load, this, null);

        if(!mAllowMergeViews) {
            if (mIsResumed) {
                mPage.resume();
            }
        }
    }

    private void loadPage(boolean mergeViews) {
        long t1 = BuildConfig.IS_BETA ? SystemClock.uptimeMillis() : 0;

        grabEvent(null);

        onLocalTransformModified();
        computeCurrentLocalTransformValues();
//		loadWallpaper();

        switch(mPage.config.overScrollMode) {
            case BOUNCE: mOverScrollInterpolator=mBounceInterpolator; break;
            case DECELERATE: mOverScrollInterpolator=mDecelerateInterpolator; break;
            default: mOverScrollInterpolator=mBounceInterpolator; break;
        }

        mStopPoints = new ArrayList<>();
        mPageIndicators = new ArrayList<>();

        removeView(mHandleView);

        if(mDelayedItemViews != null) {
            cancelDelayedItemViewLoad();
        }
        mDelayedItemViews = new ArrayList<>(mPage.items.size());
        mDelayedItemViewsSorted = false;

        int count = mPage.items.size();
        for(int i=0; i<count; i++) {
            Item item = mPage.items.get(i);
            setupItemView(item, i, mAllowDelayedViewInit, null);
        }

        if(mergeViews) {
            // remove older views that are not anymore in the layout for the new page
            for (int i = getChildCount() - 1; i >= 0; i--) {
                View view = getChildAt(i);
                Item item = ((ItemView) view).getItem();
                if (!mPage.items.contains(item)) {
                    removeViewForItem(item);
                }
            }
        }

        addView(mHandleView);

        // restore references
        mHandleItemView = mHandleItemView ==null ? null : getItemView(mHandleItemView.getItem());
        if(mHandleItemView == null) {
            mHandleView.setVisibility(View.GONE);
            mHandleViewVisible = false;
        }

//        try {
//            Method setFriction = Scroller.class.getMethod("setFriction", float.class);
//            setFriction.invoke(mFlingScroller, ViewConfiguration.getScrollFriction()*2/mPage.config.scrollingSpeed);
//        } catch (Exception e) {
//            // API 11
//        }

        onItemViewChanged(null);

        if(mDelayedItemViews != null && mDelayedItemViews.size() == 0) {
            mDelayedItemViews = null;
        } else {
            loadNextItemViewLater();
        }

        if(mPage.config.bgSystemWPScroll && !mPage.isFolder() && mPage.id != mPage.getEngine().getGlobalConfig().lwpScreen) {
            if(mWallpaperManager == null) {
                mWallpaperManager = WallpaperManager.getInstance(getContext());
            }
            if(mSetWallpaperOffsetThread == null) {
                mSetWallpaperOffsetThread = new SetWallpaperOffsetThread();
                mSetWallpaperOffsetThread.start();
            }
        } else {
            mWallpaperManager = null;
            if(mSetWallpaperOffsetThread != null) {
                mSetWallpaperOffsetThread.end();
                mSetWallpaperOffsetThread = null;
            }
        }

        if(BuildConfig.IS_BETA) {
            Log.i("LL", "ItemLayout.loadPage "+mPage.id+" in " + (SystemClock.uptimeMillis()-t1)+"ms");
        }
	}

    public void setAllowDelayedViewInit(boolean allow) {
        mAllowDelayedViewInit = allow;
    }

    public void cancelDelayedItemViewLoad() {
        if(mDelayedItemViewLoadScheduled) {
            mDelayedItemViewLoadScheduled = false;
            removeCallbacks(mLoadDelayedItemView);
        }
    }

    public void loadNextItemViewLater() {
        if(mDelayedItemViews != null && !mDelayedItemViewLoadScheduled && mIsResumed) {
            removeCallbacks(mLoadDelayedItemView);
            mDelayedItemViewLoadScheduled = true;
            postDelayed(mLoadDelayedItemView, DELAYED_VIEW_INIT_DELAY);
        }
    }

    private static final int DELAYED_VIEW_INIT_DELAY = 50;
    private Runnable mLoadDelayedItemView = new Runnable() {
        @Override
        public void run() {
            mDelayedItemViewLoadScheduled = false;
            if(mDelayedItemViews == null) {
                if(BuildConfig.DEBUG) {
                    throw new RuntimeException("mDelayedItemViews shouldn't be null");
                }
                return;
            }
            int l = mDelayedItemViews.size();
            int batch = l > 20 ? 20 : l;
            for(int i=0; i<batch; i++) {
                l--;
                ItemView itemView = mDelayedItemViews.remove(l);
                Item item = itemView.getItem();
                if(BuildConfig.IS_BETA) {
                    Log.i("LL", "delayed view init, "+item.toString()+", " + l + " left");
                }
                ensureItemViewReady(itemView);
                Class<? extends Item> cls = item.getClass();
                if(cls == Shortcut.class || cls == Folder.class) {
                    SharedAsyncGraphicsDrawable sbd = ((Shortcut) item).getSharedAsyncGraphicsDrawable();
                    if (sbd != null && sbd.needToLoadGraphics()) {
                        // the view has not been measured yet, so a maximum size for the drawable has not been computed yet
                        // in order to fix this, use the cell size to set the max size hint
                        Rect cell=item.getCell();
                        int icw=cell.width();
                        int ich=cell.height();
                        sbd.setMaxSizeHint(Math.round(0.5f + mCellWidth * icw), Math.round(0.5f + mCellHeight * ich));

                        sbd.loadGraphicsAsync();
                    }
                }
            }
            if(l == 0) {
                mDelayedItemViews = null;
                if(mDelayedItemViewLoadScheduled) {
                    cancelDelayedItemViewLoad();
                }
            } else {
                if(!mDelayedItemViewLoadScheduled) {
                    // it may have been scheduled in the meantime, because of the call to invalidate triggered by ensureItemViewReady
                    mDelayedItemViewLoadScheduled = true;
//                    mSkipNextMeasureAndLayout = true;
                    post(mLoadDelayedItemView);
                }
            }
        }
    };

    private ItemView setupItemView(Item i, int index, boolean allowDelayedInit, net.pierrox.lightning_launcher.script.api.Item cachedItem) {
        ItemView v = getItemView(i);
        if(v == null) {
            if(i instanceof EmbeddedFolder && recursionCount(((EmbeddedFolder)i).getFolderPageId()) >= 5) {
                v = new ErrorItemView(getContext(), i, "Maximum recursion count reached");
            } else {
                v = i.createView(getContext());
            }
            v.setItemLayout(this);
            if(cachedItem != null) {
                // this is an early cached script item update, it doesn't replace Screen.updateItemViewReferences because it does more work
                // but it solves some issues because the binding and resume events are sent before Screen.updateItemViewReferences
                i.getPage().getEngine().getScriptExecutor().getLightning().updateCachedItem(cachedItem, v);
            }
            Binding[] bindings = i.getItemConfig().bindings;
            boolean hasBindings = bindings != null;
            if(hasBindings) {
                i.getPage().getEngine().getVariableManager().updateBindings(v, bindings, true, mScreen, false);
            }
            mItemViews.put(i.getId(), v);
            int max = getChildCount() - 1;
            if(index > max) {
//                index = max;
            }
            addView(v, index);
            if(allowDelayedInit && (v instanceof ShortcutView || v instanceof WidgetView) && i.getItemConfig().onGrid && !hasBindings) {
                mDelayedItemViews.add(v);
            } else {
                v.init();
                if (mIsResumed) v.resume();
            }
        } else {
            bringChildToFront(v);
            if(!v.isInitDone() && mDelayedItemViews != null) {
                mDelayedItemViews.add(v);
            }
        }

        final Class<? extends ItemView> itemViewClass = v.getClass();
        boolean is_embedded_folder = itemViewClass == EmbeddedFolderView.class;
        if(is_embedded_folder) {
            EmbeddedFolderView embeddedFolderView = (EmbeddedFolderView) v;
            ItemLayout il = embeddedFolderView.getEmbeddedItemLayout();
            il.setDesktopSize(mDesktopWidth, mDesktopHeight);
        }
        v.setDispatchEventsToChild(!mEditMode);
        if(i.getItemConfig().rotate) {
            v.setRotation(-mCurrentItemOrientation);
        }
        i.setAppDrawerHiddenHandling(mAppDrawerHiddenHandling);


        if(itemViewClass == StopPointView.class) {
            mStopPoints.add((StopPoint)i);
            mHaveStopPoints = true;
        } else if(itemViewClass == PageIndicatorView.class) {
            mPageIndicators.add((PageIndicatorView) v);
        }

        return v;
    }

    private int recursionCount(int pageId) {
        ItemView opener = mOpenerItemView;
        int count = 0;
        while (opener != null) {
            ItemLayout il = opener.getParentItemLayout();
            if(il == null) {
                opener = null;
            } else {
                if (il.getPage().id == pageId) {
                    count++;
                }
                opener = il.mOpenerItemView;
            }
        }
        return count;
    }

    private void onItemViewChanged(Item item) {
        int orientation=mScreen.getResourcesOrientation();
        setItemsOrientation(orientation, item);
        mItemsBoundingBox.setEmpty();
        computeBoundingBox(getWidth(), getHeight());
        requestLayout();
    }

    public ItemView getOpenerItemView() {
        return mOpenerItemView;
    }

    public void setOpenerItemView(ItemView itemView) {
        mOpenerItemView = itemView;
    }

    public ItemView getItemView(Item item) {
        return getItemView(item.getId());
    }

    public ItemView getItemView(int itemId) {
        return mItemViews.get(itemId);
    }

    public void ensureItemViewReady(ItemView itemView) {
        if(!itemView.isInitDone()) {
            itemView.init();
            if(mIsResumed) {
                itemView.resume();
            }
        }
    }

    public void setAppDrawerHiddenHandling(int appDrawerHiddenHandling) {
        mAppDrawerHiddenHandling = appDrawerHiddenHandling;
        for(Item item : mPage.items) {
            item.setAppDrawerHiddenHandling(appDrawerHiddenHandling);
        }
    }

    public int getAppDrawerHiddenHandling() {
        return mAppDrawerHiddenHandling;
    }

    public void setDisplayInvisibleItems(boolean displayInvisibleItems) {
        int count = getChildCount();
        for(int n=0; n<count; n++) {
            View childView = getChildAt(n);
            if (childView instanceof ItemView) {
                ((ItemView)childView).setOverrideVisible(displayInvisibleItems);
            }
        }
    }

    public void setEmbedded(boolean is_embedded) {
        mIsEmbedded = is_embedded;
    }

    public boolean isEmbedded() {
        return mIsEmbedded;
    }

    public boolean allowScrollX() {
        return mAllowScrollX;
    }

    public boolean allowScrollY() {
        return mAllowScrollY;
    }

    public boolean canScroll(boolean vertical_motion, boolean deep) {
    	if(mEditMode) {
            return false;
    	}

    	if(deep && !vertical_motion && mAllowScrollX) return true;
    	if(deep && vertical_motion && mAllowScrollY) return true;

    	if(mLastTouchedItemView != null) {
    		Item item = mLastTouchedItemView.getItem();
    		if(item != null) {
                if(item.getItemConfig().touch.action != GlobalConfig.UNSET) {
                    return true;
                }
	    		Class<? extends ItemView> cls = mLastTouchedItemView.getClass();
	    		if(cls == EmbeddedFolderView.class) {
	                ItemLayout il = ((EmbeddedFolderView)mLastTouchedItemView).getEmbeddedItemLayout();
	                return il.canScroll(vertical_motion, true) || il.mEditMode;
	            } else if(cls == UnlockerView.class) {
                    return true;
                } else if(cls == CustomViewView.class) {
                    CustomViewView cv = (CustomViewView) mLastTouchedItemView;
                    if(vertical_motion && cv.hasVerticalGrab()) return true;
                    if(!vertical_motion && cv.hasHorizontalGrab()) return true;
                    return false;
	            } else if(vertical_motion && hasScrollableView(mLastTouchedItemView)) {
	            	return true;
	            } else {
	            	if(!vertical_motion && !mAllowScrollX) return false;
	            	if(vertical_motion && !mAllowScrollY) return false;
	        		return deep;
	            }
    		} else {
    			return false;
    		}
    	} else {
    		if(!vertical_motion && !mAllowScrollX) return false;
        	if(vertical_motion && !mAllowScrollY) return false;
    		return true;
    	}
    }

    private void setItemsOrientation(int orientation, Item item) {
        if(item == null) {
            for (Item i : mPage.items) {
                i.setOrientation(orientation);
            }
        } else {
            item.setOrientation(orientation);
        }
    }

	public void updateOrientation() {
		int orientation=mScreen.getResourcesOrientation();
        setItemsOrientation(orientation, null);
        mIsPortrait = (orientation == Configuration.ORIENTATION_PORTRAIT);
		requestLayout();
	}

    private static final long ANIMATION_ITEM_ROTATION = 200;
    private int mCurrentItemOrientation = 0;
    private long mAnimItemOrientationStartDate;
    private boolean mAnimatingItemOrientation;
    private int mAnimItemOrientationFrom;
    private int mAnimItemOrientationCurrent = 0;

    public void updateItemsOrientation(int orientation) {
        if(mPage == null) {
            return;
        }

        if(orientation != mCurrentItemOrientation) {
            mAnimatingItemOrientation = true;
            mAnimItemOrientationStartDate = AnimationUtils.currentAnimationTimeMillis();
            mAnimItemOrientationFrom = mAnimItemOrientationCurrent;
            mCurrentItemOrientation = orientation;
            invalidate();
        }
    }

    private void animateItemRotation() {
        long delta = AnimationUtils.currentAnimationTimeMillis() - mAnimItemOrientationStartDate;
        if(delta >= ANIMATION_ITEM_ROTATION) {
            delta = ANIMATION_ITEM_ROTATION;
            mAnimItemOrientationCurrent = mCurrentItemOrientation;
            mAnimatingItemOrientation = false;
        }

        float s = mDecelerateInterpolator.getInterpolation(delta / (float)ANIMATION_ITEM_ROTATION);
        int angle =  mCurrentItemOrientation - mAnimItemOrientationFrom;
        if(angle < -180) angle += 360;
        if(angle > 180) angle -= 360;
        int orientation = (int) (mAnimItemOrientationFrom + s * angle);

        mAnimItemOrientationCurrent = orientation;

        int count = getChildCount();
        for(int n=0; n<count; n++) {
            View childView = getChildAt(n);
            if (childView instanceof ItemView) {
                ItemView itemView = (ItemView) childView;
                if(itemView.getItem().getItemConfig().rotate) {
                    itemView.setRotation(-orientation);
                }
            }
        }
    }

    private static Field sDragEventX, sDragEventY;

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public boolean dispatchDragEvent(DragEvent event) {
        // really awful hack
        if(sDragEventX == null) {
            try {
                sDragEventX = DragEvent.class.getDeclaredField("mX");
                sDragEventX.setAccessible(true);
                sDragEventY = DragEvent.class.getDeclaredField("mY");
                sDragEventY.setAccessible(true);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        }
        mTempPts[0] = event.getX();
        mTempPts[1] = event.getY();
        mLocalInverseTransform.mapPoints(mTempPts);
        try {
            sDragEventX.set(event, mTempPts[0]);
            sDragEventY.set(event, mTempPts[1]);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return super.dispatchDragEvent(event);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public boolean onDragEvent(DragEvent event) {
        int action = event.getAction();

        switch (action) {
            case DragEvent.ACTION_DRAG_STARTED:
                if (event.getClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                    return true;
                }
                return false;

            case DragEvent.ACTION_DRAG_LOCATION:
                if(!mPage.isFolder()) {
                    mTempPts[0] = event.getX();
                    mTempPts[1] = event.getY();
                    mLocalTransform.mapPoints(mTempPts);
                    float ex = mTempPts[0];
                    float ey = mTempPts[1];
                    float ax = 0;
                    float ay = 0;
                    mTempRectF.set(mAutoScrollRect);
                    if (ex < mAutoScrollRect.left) {
                        ax = mAutoScrollMargin;
                    } else if (ex > mAutoScrollRect.right) {
                        ax = -mAutoScrollMargin;
                    }
                    if (ey < mAutoScrollRect.top) {
                        ay = mAutoScrollMargin;
                    } else if (ey > mAutoScrollRect.bottom) {
                        ay = -mAutoScrollMargin;
                    }
                    if (ay != 0 || ax != 0) {
                        if(!mAutoScrolling) {
                            mAutoScrolling = true;
                            mAutoScrollDx = ax / 4;
                            mAutoScrollDy = ay / 4;
                            invalidate();
                        }
                    } else {
                        mAutoScrolling = false;
                    }
                }
                return true;

            case DragEvent.ACTION_DROP:
                mAutoScrolling = false;
                mScreen.onItemLayoutAppShortcutDropped(this, (ShortcutInfo) event.getLocalState(), event.getX(), event.getY());
                return true;
        }
        return super.onDragEvent(event);
    }

    private ItemViewTracking getItemViewTracking(ItemView itemView) {
        if(mTrackedItems.size() == 0) {
            // optimization: don't create an iterator if size is 0 (seen on profiler)
            return null;
        }
        for (ItemViewTracking it : mTrackedItems) {
            if(it.mItemView == itemView) {
                return it;
            }
        }
        return null;
    }

    public void trackItemView(ItemView itemView) {
        mTrackedItems.add(new ItemViewTracking(itemView));
        itemView.setDispatchEventsToChild(false);
        requestLayout();
    }

    public void untrackItemView(ItemView itemView) {
        for(int n = mTrackedItems.size() - 1; n >= 0; n--) {
            if(mTrackedItems.get(n).mItemView == itemView) {
                itemView.setDispatchEventsToChild(!mEditMode);
                mTrackedItems.remove(n);
                break;
            }
        }

        requestLayout();
    }

    public void moveTrackedItemView(ItemView itemView, float dx, float dy) {
        ItemViewTracking t = getItemViewTracking(itemView);
        t.dx = dx;
        t.dy = dy;
        layoutTrackedItemViews();
    }

    public void showHandleViewForItemView(ItemView itemView) {
        mHandleItemView = itemView;
        if(!mHandleViewVisible) {
            mHandleView.setVisibility(View.VISIBLE);
//            mHandleView.startAnimation(new AlphaAnimation(0, 1));
            mHandleViewVisible = true;
        }
        requestLayout();
        invalidate();
    }

    public void hideHandleView() {
        if(mHandleViewVisible) {
            mHandleItemView = null;
            mHandleView.setVisibility(View.GONE);
//            mHandleView.startAnimation(new AlphaAnimation(1, 0));
            mHandleViewVisible = false;
            requestLayout();
            invalidate();
        }
    }

    public HandleView getHandleView() {
        return mHandleView;
    }

	public void setMasterSelectedItem(Item item) {
        if(mSelectedItem != item) {
            mSelectedItem = item;
            if (mSelectedItem == null) {
                hideHandleView();
            }
            invalidate();
            mScreen.onItemLayoutMasterSelectedItemChanged(item);
        }
	}

    public ItemView getMasterSelectedItemView() {
        return mSelectedItem==null ? null : mItemViews.get(mSelectedItem.getId());
    }

    public Item getMasterSelectedItem() {
        return mSelectedItem;
    }

    public ArrayList<ItemView> getSelectedItems() {
        ArrayList<ItemView> itemViews = new ArrayList<>();
        for(int i=mItemViews.size()-1; i>=0; i--) {
            ItemView itemView = mItemViews.valueAt(i);
            if(itemView.isSelected()) {
                itemViews.add(itemView);
            }
        }
        return itemViews;
    }

    public void setHonourPinnedItems(boolean honour) {
        mHonourPinnedItems=honour;
        requestLayout();
        invalidate();
    }

	public float getCellWidth() {
		return mCellWidth;
	}

	public float getCellHeight() {
		return mCellHeight;
	}

	public void grabEvent(View target) {
		mMyMotionTarget = null;
		mGrabEventTarget = target;
	}

	public Matrix getLocalTransform() {
		return mLocalTransform;
	}

	public Matrix getLocalInverseTransform() {
		return mLocalInverseTransform;
	}

	public void setLocalTransform(Matrix local_transform) {
		mLocalTransform.set(local_transform);
		onLocalTransformModified();
		computeCurrentLocalTransformValues();
		//mCurrentPosition=POSITION_FREE;
		requestLayout();
	}

	public Rect getItemsBoundingBox() {
		return mItemsBoundingBox;
	}

	public void zoomTo(float scale) {
		mAnimTransform.setScale(scale, scale);
		mLocalTransform.set(mAnimTransform);
		onLocalTransformModified();
		computeCurrentLocalTransformValues();
		mScreen.onItemLayoutZoomChanged(mCurrentScale);
	}

	public void animateZoomTo(int to, float scale) {

		float to_x, to_y, to_scale;
		if(to==POSITION_FULL_SCALE) {
			mTempRectF.set(mItemsBoundingBox);
			float sx=getWidth()/mTempRectF.width();
			float sy=getHeight()/mTempRectF.height();
			to_scale=Math.min(sx, sy);
			if(to_scale==0 || Float.isNaN(to_scale) || Float.isInfinite(to_scale)) to_scale=1;
			to_x=(getWidth()-mItemsBoundingBox.width()*to_scale)/2-mItemsBoundingBox.left*to_scale;
			to_y=(getHeight()-mItemsBoundingBox.height()*to_scale)/2-mItemsBoundingBox.top*to_scale;
		} else if(to==POSITION_ORIGIN) {
			to_x=0;
			to_y=0;
			to_scale=1;
		} else /*if(to==POSITION_FREE)*/ {
			to_x=0;
			to_y=0;
			to_scale=scale;
		}

		animateZoomTo(to_x, to_y, to_scale);
	}

    public void animateZoomTo(float to_x, float to_y, float to_scale) {
        float from_x=mLocalTransformMatrixValues[Matrix.MTRANS_X];
        float from_y=mLocalTransformMatrixValues[Matrix.MTRANS_Y];
        float from_scale=mLocalTransformMatrixValues[Matrix.MSCALE_X];

        animateNavigation(from_x, from_y, from_scale, to_x, to_y, to_scale, mDecelerateInterpolator, ANIMATION_DURATION);
    }

    public void moveTo(float to_x, float to_y, float to_scale) {
        mAnimatingNavigation = false;
    	mLocalTransform.reset();
    	mLocalTransform.postScale(to_scale, to_scale);
    	mLocalTransform.postTranslate(to_x, to_y);

    	onLocalTransformModified();
    	computeCurrentLocalTransformValues();
    }

    // recenter the view on the primary page after it has been scrolled multiple times in seamless mode
    public void recenter() {
        if(!mItemsBoundingBox.isEmpty()) { // this check does not apply if the ItemLayout has not been measured yet
            // move the view on the real page (to avoid empty screen in seamless mode)
            float x = mCurrentX;
            float y = mCurrentY;
            if (mPage.config.wrapX && mCurrentScale == 1) {
                while (x > mItemsBoundingBox.left) x -= mItemsBoundingBox.width();
                while (x <= -mItemsBoundingBox.right) x += mItemsBoundingBox.width();
            }
            if (mPage.config.wrapY && mCurrentScale == 1) {
                while (y > mItemsBoundingBox.top) y -= mItemsBoundingBox.height();
                while (y <= -mItemsBoundingBox.bottom) y += mItemsBoundingBox.height();
            }
            if (x != mCurrentX || y != mCurrentY) {
                moveTo(x, y, mCurrentScale);
            }
        }
    }

//	public int getCurrentPosition() {
//		return mCurrentPosition;
//	}

	public float getCurrentScale() {
		return mCurrentScaleS;
	}

	public float getCurrentX() {
		return mCurrentX;
	}

	public float getCurrentY() {
		return mCurrentY;
	}

    public void setAllowWrap(boolean allow_wrap) {
        mAllowWrap = allow_wrap;
    }

    public boolean getAlwaysShowStopPoints() {
        return mAlwaysShowStopPoints;
    }

	public boolean getEditMode() {
		return mEditMode;
	}

	public void setEditMode(boolean edit_mode, boolean is_folder) {
		if(edit_mode) {
			mAllowOverScroll=true;
			mAllowFling=false;
            mTempGridCrossPts = new float[MAX_EDIT_GRID_CROSS_COUNT*8];
		} else {
			mAllowOverScroll=false;
			mAllowFling=true;
		}

        mAnimEditModeStartDate = AnimationUtils.currentAnimationTimeMillis();
        mAnimatingEditMode = true;

        mEditMode=edit_mode;

        if(mPage != null) {
            configureScroll();
            recenter();

            int count = getChildCount();
            for(int n=0; n<count; n++) {
                View childView = getChildAt(n);
                if(!(childView instanceof ItemView)) continue;
                ItemView itemView = (ItemView) childView;
                itemView.setDispatchEventsToChild(!edit_mode);
                itemView.evaluateEnabledState();
            }

            for(Item item : mPage.items) {
                if(item.getClass() == DynamicText.class) {
                    ((DynamicText)item).setEditMode(edit_mode);
                }
            }
        }
		mEditFolder=is_folder;
        if(!LLApp.get().getSystemConfig().hasSwitch(SystemConfig.SWITCH_HONOUR_PINNED_ITEMS)) {
            setHonourPinnedItems(!edit_mode);
        }
		invalidate();
	}

    @Override
    public void setEnabled(boolean d) {
        if(d != isEnabled()) {
            super.setEnabled(d);
            int count = getChildCount();
            for (int n = 0; n < count; n++) {
                View childView = getChildAt(n);
                if (!(childView instanceof ItemView)) continue;
                ItemView itemView = (ItemView) childView;
                itemView.evaluateEnabledState();
            }
        }
    }

    public void setVirtualEditBorders(Box box, Rect bounds, float scale) {
        mVirtualEditBordersBounds = new RectF(bounds.left/scale, bounds.top/scale, bounds.right/scale, bounds.bottom/scale);
        mVirtualEditBordersScale = scale;
        int width = Math.round(mVirtualEditBordersBounds.width());
        int height = Math.round(mVirtualEditBordersBounds.height());
        mVirtualEditBorders = new BoxLayout(getContext(), null);
        mVirtualEditBorders.setChild(null, box);
        mVirtualEditBorders.measure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        );
        mVirtualEditBorders.layout(0, 0, width, height);
    }

    public RectF getVirtualEditBordersBounds() {
        return mVirtualEditBordersBounds;
    }

    public void clearHighlightedCells() {
        mHighlightedCells.clear();
        invalidate();
    }

	public void addHighlightedCell(Item item, Rect r) {
        mHighlightedCells.put(item.getId(), r);
        invalidate();
	}

    public void setSnappingData(float anchor_x, float anchor_y, ArrayList<RectF> bounds) {
        if(anchor_x == Float.MAX_VALUE) {
            mSnappingGuideX = Float.MAX_VALUE;
        } else {
            mSnappingGuideX = Math.round(anchor_x)-0.5f;
        }
        if(anchor_y == Float.MAX_VALUE) {
            mSnappingGuideY = Float.MAX_VALUE;
        } else {
            mSnappingGuideY = Math.round(anchor_y)-0.5f;
        }
        mSnappingBounds = bounds;
        invalidate();
    }

	public void setAutoFindOrigin(boolean a) {
        mAutoFindOrigin=a;
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mMyRect.set(0, 0, w, h);
        final float PREFETCH_MARGIN = 0.33f;
        mMyPrefetchRect.set(-w*PREFETCH_MARGIN, -h*PREFETCH_MARGIN, w*(1+PREFETCH_MARGIN), h*(1+PREFETCH_MARGIN));

        configureScroll();
		mAutoScrollRect.set(0, 0, w, h);
		mAutoScrollRect.inset(mAutoScrollMargin, mAutoScrollMargin);

        if(mPage != null && mPage.isDashboard()) {
            setDesktopSize(w, h);
        }

        adjustTranslationAfterSizeChange(w, h, oldw, oldh);

        if(mScreen != null) {
            mScreen.onItemLayoutSizeChanged(this, w, h, oldw, oldh);
        }

        updatePageIndicatorsLater();

        setWallpaperOffsetSteps();
        setWallpaperOffset();
	}

    public void setForceUseDesktopSize(boolean force) {
        mForceUseDesktopSize = force;
    }

    public void setDesktopSize(int width, int height) {
        mDesktopWidth = width;
        mDesktopHeight = height;
        if(mPage != null) {
            int count = getChildCount();
            for(int n=0; n<count; n++) {
                View childView = getChildAt(n);
                if(childView instanceof EmbeddedFolderView) {
                    ((EmbeddedFolderView)childView).getEmbeddedItemLayout().setDesktopSize(width, height);
                }
            }
        }
        post(new Runnable() {
                @Override
                public void run() {
                    requestLayout();
                }
            });
    }

	private static final int sMeasureUnspecified = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);

	@Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int measure_width = MeasureSpec.getSize(widthMeasureSpec);
        int measure_height = MeasureSpec.getSize(heightMeasureSpec);

        if(getVisibility()!=View.VISIBLE || mPage==null) {
			setMeasuredDimension(measure_width, measure_height);
			return;
		}

        int sw, sh;
        if(mForceUseDesktopSize) {
            sw = mPage.config.useDesktopSize ? measure_width : mDesktopWidth;
            sh = mPage.config.useDesktopSize ? measure_height : mDesktopHeight;
        } else {
            boolean do_not_use_desktop_size = !mPage.config.useDesktopSize;
            sw = mDesktopWidth == 0 || do_not_use_desktop_size ? measure_width : mDesktopWidth;
            sh = mDesktopHeight == 0 || do_not_use_desktop_size ? measure_height : mDesktopHeight;
        }

		final PageConfig c = mPage.config;

        // grid mode
        final SizeMode mode_w = mIsPortrait ? c.gridPColumnMode : c.gridLColumnMode;
        final SizeMode mode_h = mIsPortrait ? c.gridPRowMode : c.gridLRowMode;

        final int fixed_value_w = mIsPortrait ? c.gridPColumnSize : c.gridLColumnSize;
        final int fixed_value_h = mIsPortrait ? c.gridPRowSize : c.gridLRowSize;

        final int num_w = mIsPortrait ? c.gridPColumnNum : c.gridLColumnNum;
        final int num_h = mIsPortrait ? c.gridPRowNum : c.gridLRowNum;

        switch(mode_w) {
        case AUTO: mCellWidth=0; break;
        case SIZE: mCellWidth=fixed_value_w; break;
        case NUM: mCellWidth=sw/(float)num_w; break;
        }
        switch(mode_h) {
        case AUTO: mCellHeight=0; break;
        case SIZE: mCellHeight=fixed_value_h; break;
        case NUM: mCellHeight=sh/(float)num_h; break;
        }
        int count = getChildCount();
        for(int n=0; n<count; n++) {
            View childView = getChildAt(n);
            if(!(childView instanceof ItemView)) continue;

            ItemView itemView = (ItemView) childView;
            if(!itemView.isInitDone()) continue;
            Item item = itemView.getItem();
            if(!item.getItemConfig().onGrid) continue;

            final Rect cell=item.getCell();
            int icw=cell.width();
            int ich=cell.height();

            int ms_w=0;
            switch(mode_w) {
            case AUTO:
                // let the view tell how large it wants to be
                ms_w = sMeasureUnspecified;
                break;

            case SIZE:
            case NUM:
                // fixed and num behave the same way: we have a fixed cell size
                if(icw==0) {
                    // auto size, let the view tell how large it wants to be, we will compute the number of cells it needs later
                    ms_w = sMeasureUnspecified;
                } else {
                    // apply this fixed size
                    ms_w = MeasureSpec.makeMeasureSpec(Math.round(mCellWidth * cell.right)-Math.round(mCellWidth * cell.left), MeasureSpec.EXACTLY);
                }
                break;
            }

            int ms_h=0;
            switch(mode_h) {
            case AUTO:
                ms_h = sMeasureUnspecified;
                break;

            case SIZE:
            case NUM:
                if(ich==0) {
                    ms_h = sMeasureUnspecified;
                } else {
                    ms_h = MeasureSpec.makeMeasureSpec(Math.round(mCellHeight * cell.bottom)-Math.round(mCellHeight * cell.top), MeasureSpec.EXACTLY);
                }
                break;
            }

            if(!itemView.isLayoutRequested()) {
                itemView.forceLayout();
            }
            itemView.measure(ms_w, ms_h);

            // for auto size items, and when the cell size is known (not in auto mode), compute the number of cells it needs to fit
            if((mode_w!=SizeMode.AUTO && mode_h!=SizeMode.AUTO) && (icw==0 || ich==0)) {
                if(icw==0) {
                    icw = (int)((itemView.getMeasuredWidth()+mCellWidth-1) / mCellWidth);
                    cell.right = cell.left + icw;
                }
                if(ich==0) {
                    ich =  (int)((itemView.getMeasuredHeight()+mCellHeight-1) / mCellHeight);
                    cell.bottom = cell.top + ich;
                }
                // measure again so that the child updates measurement of its own children
                itemView.measure(MeasureSpec.makeMeasureSpec(Math.round(mCellWidth * cell.right)-Math.round(mCellWidth * cell.left), MeasureSpec.EXACTLY),
                          MeasureSpec.makeMeasureSpec(Math.round(mCellHeight * cell.bottom)-Math.round(mCellHeight * cell.top), MeasureSpec.EXACTLY));
            }
        }

        if(mode_w==SizeMode.AUTO || mode_h==SizeMode.AUTO) {
            // compute cell size as max of child cell sizes (excepting auto size items)
            count = getChildCount();
            for(int n=0; n<count; n++) {
                View childView = getChildAt(n);
                if(!(childView instanceof ItemView)) continue;

                ItemView itemView = (ItemView) childView;
                Item item = itemView.getItem();
                if(!item.getItemConfig().onGrid) continue;

                final Rect cell=item.getCell();
                if(mode_w==SizeMode.AUTO) {
                    final int icw=cell.width();

                    if(icw > 0) {
                        int w = itemView.getMeasuredWidth()/icw;
                        if(w > mCellWidth) mCellWidth = w;
                    }
                }
                if(mode_h==SizeMode.AUTO) {
                    final int ich=cell.height();

                    if(ich > 0) {
                        int h = itemView.getMeasuredHeight()/ich;
                        if(h > mCellHeight) mCellHeight = h;
                    }
                }
            }

            // fallback
            int n=Utils.getStandardIconSize();
            if(mCellWidth<n) mCellWidth=n;
            if(mCellHeight<n) mCellHeight=n;

            // now that both cell width and height are known, measure again items with these values
            count = getChildCount();
            for(int j=0; j<count; j++) {
                View childView = getChildAt(j);
                if(!(childView instanceof ItemView)) continue;

                ItemView itemView = (ItemView) childView;
                Item item = itemView.getItem();
                if(!item.getItemConfig().onGrid) continue;

                final Rect cell=item.getCell();
                int icw=cell.width();
                int ich=cell.height();

                // compute cell size for auto size items
                if(icw==0) {
                    icw = (int)((itemView.getMeasuredWidth()+mCellWidth-1) / mCellWidth);
                    cell.right = cell.left + icw;

                }
                if(ich==0) {
                    ich = (int)((itemView.getMeasuredHeight()+mCellHeight-1) / mCellHeight);
                    cell.bottom = cell.top + ich;
                }

                int cell_width=MeasureSpec.makeMeasureSpec(Math.round(mCellWidth * cell.right)-Math.round(mCellWidth * cell.left), MeasureSpec.EXACTLY);
                int cell_height=MeasureSpec.makeMeasureSpec(Math.round(mCellHeight * cell.bottom)-Math.round(mCellHeight * cell.top), MeasureSpec.EXACTLY);

                itemView.measure(cell_width, cell_height);
            }
        }

        if(mHandleViewVisible && mHandleItemView.getItem().getItemConfig().onGrid && mHandleItemView.isViewVisible()) {
            final Rect cell= mHandleItemView.getItem().getCell();
            measureHandleView(mCellWidth * cell.width(), mCellHeight * cell.height());
        }

        mPage.setCurrentViewSize(sw, sh, mCellWidth, mCellHeight);


        // free mode
        int max_right=0;
        int max_bottom=0;

        count = getChildCount();
        for(int j=0; j<count; j++) {
            View childView = getChildAt(j);
            if(!(childView instanceof ItemView)) continue;

            ItemView itemView = (ItemView) childView;
            Item item = itemView.getItem();
            if(item.getItemConfig().onGrid) continue;

            int w=item.getViewWidth();
            int h=item.getViewHeight();
            if(!itemView.isLayoutRequested()) {
                itemView.forceLayout();
            }
            itemView.measure(w==0 ? sMeasureUnspecified : MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY), h==0 ? sMeasureUnspecified : MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY));
            Rect bb=itemView.getTransformBoundingBox();
            if(bb.right>max_right) max_right=bb.right;
            if(bb.bottom>max_bottom) max_bottom=bb.bottom;

            if(itemView == mHandleItemView && mHandleViewVisible) {
                measureHandleView(itemView.getMeasuredWidth(), itemView.getMeasuredHeight());
            }
        }

		computeBoundingBox(sw, sh);
		
		int vw, vh;
		
		int mw = MeasureSpec.getMode(widthMeasureSpec);
		if(mw==MeasureSpec.UNSPECIFIED) {
			vw = mItemsBoundingBox.width();
		} else if(mw==MeasureSpec.EXACTLY) {
			vw = measure_width;
		} else { // AT_MOST
            if(mIsEmbedded) {
                vw = measure_width;
            } else {
			    int max = sw;
                vw = mItemsBoundingBox.width();
                if(vw > measure_width) vw = measure_width;
                if(vw>max) vw=max;
            }
		}
		
		int mh = MeasureSpec.getMode(heightMeasureSpec);
		if(mh==MeasureSpec.UNSPECIFIED) {
			vh = mItemsBoundingBox.height();
		} else if(mh==MeasureSpec.EXACTLY) {
			vh = measure_height;
		} else { // AT_MOST
            if(mIsEmbedded) {
                vh = measure_height;
            } else {
                int max = sh;
                vh = mItemsBoundingBox.height();
                if(vh > measure_height) vh = measure_height;
                if(vh>max || (mIsEmbedded && vh==0)) vh=max;
            }
		}
		
		setMeasuredDimension(vw, vh);
		
		if(mAutoFindOrigin) {
			mLocalTransform.setTranslate(-mItemsBoundingBox.left, -mItemsBoundingBox.top);
			onLocalTransformModified();
			computeCurrentLocalTransformValues();
			mAutoFindOrigin=false;
		}
	}

    private void measureHandleView(float w, float h) {
        mTempRectF.set(0, 0, w, h);
        Matrix t = getTransformForItemView(mHandleItemView);
        if(t != null) {
            t.mapRect(mTempRectF);
        }
        mTempRectF.round(mTempRect);
        int handle_width = Math.round(mTempRect.width() + mHandleViewPaddingX2 + 1);
        int handle_height = Math.round(mTempRectF.height() + mHandleViewPaddingX2 + 1);
        mHandleView.computeInnerHandleSize(handle_width, handle_height);
        mHandleView.measure(MeasureSpec.makeMeasureSpec(handle_width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(handle_height, MeasureSpec.EXACTLY));
    }

	public void reLayoutItems() {
		layoutItems(true, getLeft(), getTop(), getRight(), getBottom());
	}
	
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		if(getVisibility()!=View.VISIBLE) {
			return;
		}
		layoutItems(changed, l, t, r, b);
		if(mScreen !=null) mScreen.onItemLayoutOnLayoutDone(this);
	}
	
	private void layoutItems(boolean changed, int l, int t, int r, int b) {
		if(mPage==null) {
			return;
		}

        int count = getChildCount();
        for(int n=0; n<count; n++) {
            View childView = getChildAt(n);
            if(childView instanceof ItemView) {
                layoutItemView((ItemView) childView);
            }
        }

		if(mItemsBoundingBoxChanged) {
            updatePageIndicatorsLater();
			mItemsBoundingBoxChanged=false;
		}

        if(!mFlinging && mGestureMode!=GESTURE_DRAG) {
		    configureBounds();
        }
	}

    public void layoutItemView(ItemView itemView) {
        if(itemView.getItem().getItemConfig().onGrid) {
            layoutItemGrid(itemView);
        } else {
            layoutItemFree(itemView);
        }
    }

    private void layoutItemGrid(ItemView itemView) {
        Item item = itemView.getItem();

        final Rect cell=item.getCell();

        int left=Math.round(cell.left*mCellWidth);
        int top=Math.round(cell.top*mCellHeight);
        int right=Math.round(cell.right*mCellWidth);
        int bottom=Math.round(cell.bottom*mCellHeight);
        ItemViewTracking it = getItemViewTracking(itemView);
        if(it != null) {
            left += it.dx;
            right += it.dx;
            top += it.dy;
            bottom += it.dy;
        }

        if(item.getClass()==StopPoint.class) {
            StopPoint sp = (StopPoint)item;
            sp.mCurrentViewX = left;
            sp.mCurrentViewY = top;

            int half_width=Math.round(cell.width()*mCellWidth/2);
            left-=half_width;
            right-=half_width;

            int half_height=Math.round(cell.height()*mCellHeight/2);
            top-=half_height;
            bottom-=half_height;
        }
        itemView.layout(left, top, right, bottom);
        item.setViewWidth(right-left);
        item.setViewHeight(bottom-top);
        if(itemView == mHandleItemView && mHandleViewVisible) {
            layoutHandleView();
        }
    }

    private void layoutItemFree(ItemView itemView) {
        Item item = itemView.getItem();
        Rect bb=itemView.getTransformBoundingBox();
        int cl=bb.left;
        int ct=bb.top;
        int cr=bb.right;
        int cb=bb.bottom;
        if(item.getClass()==StopPoint.class) {
            StopPoint sp = (StopPoint)item;
            sp.mCurrentViewX = cl;
            sp.mCurrentViewY = ct;

            int w2=bb.width()/2;
            int h2=bb.height()/2;
            cl-=w2;
            cr-=w2;
            ct-=h2;
            cb-=h2;
        }
        itemView.layout(cl, ct, cr, cb);
        if(item.getViewWidth() == 0) {
            item.setViewWidth((int) ((cr-cl)*getCurrentScale()));
        }
        if(item.getViewHeight() == 0) {
            item.setViewHeight((int) ((cb-ct)*getCurrentScale()));
        }
        if(itemView == mHandleItemView && mHandleViewVisible) {
            layoutHandleView();
        }
    }

    private void layoutHandleView() {
        final View v = mHandleItemView;
        mTempRectF.set(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());

        Animation ia = mHandleItemView.getAnimation();
        if(ia != null && ia.hasStarted()) {
            ia.getTransformation(AnimationUtils.currentAnimationTimeMillis(), mTempTransformation);
            mTempTransformation.getMatrix().mapRect(mTempRectF);
        }
        Matrix transform = getTransformForItemView(mHandleItemView);
        if(transform != null) {
            transform.mapRect(mTempRectF);
        }
        mTempRectF.round(mTempRect);
        int left = mTempRect.left - mHandleViewPadding;
        int top = mTempRect.top - mHandleViewPadding;
        mHandleView.myLayout(left, top, (int) (mTempRectF.right + mHandleViewPadding), (int) (mTempRectF.bottom + mHandleViewPadding));
    }

    public void layoutTrackedItemViews() {
        for (ItemViewTracking it : mTrackedItems) {
            ItemView itemView = it.mItemView;
            Item item = itemView.getItem();
            if(item.getItemConfig().onGrid && Build.VERSION.SDK_INT>=16 && item.getClass() == Widget.class) {
                itemView.measure(MeasureSpec.makeMeasureSpec(itemView.getWidth(), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(itemView.getHeight(), MeasureSpec.EXACTLY));
            }
            layoutItemView(itemView);
            if(mHandleItemView == itemView) {
                mHandleView.requestLayout();
            }
        }
    }

    public void updateBoundingBox() {
        computeBoundingBox(getWidth(), getHeight());
        configureBounds();
        if(mItemsBoundingBoxChanged) {
            updatePageIndicatorsLater();
            mItemsBoundingBoxChanged=false;
        }
    }

    public void setBoundingBox(Rect bb) {
        mCustomItemBoundingBox = bb;
        updateBoundingBox();
    }

    public void computeBoundingBox(int view_width, int view_height) {
        if(mCustomItemBoundingBox != null) {
            mItemsBoundingBox.set(mCustomItemBoundingBox);
            return;
        }

    	mTempRect.setEmpty();

        int count = getChildCount();
        for(int n=0; n<count; n++) {
            View childView = getChildAt(n);
            if(!(childView instanceof ItemView)) continue;

            ItemView itemView = (ItemView) childView;
            Item item = itemView.getItem();

            if(!itemView.isViewVisible()) continue;
            if(item.getItemConfig().onGrid) {
                final Rect cell=item.getCell();
                int cl=cell.left;
                int ct=cell.top;
                final int cw=cell.width();
                final int ch=cell.height();
                int cr=cl+cw;
                int cb=ct+ch;
                cl*=mCellWidth;
                cr*=mCellWidth;
                ct*=mCellHeight;
                cb*=mCellHeight;
                mTempRect.union(cl, ct, cr, cb);
            } else {
                Rect bb=itemView.getTransformBoundingBox();
                final int cl=bb.left;
                final int ct=bb.top;
                final int cr=bb.right;
                final int cb=bb.bottom;
                mTempRect.union(cl, ct, cr, cb);
            }
        }

		if(!mTempRect.equals(mItemsBoundingBox) || mItemsBoundingBox.isEmpty()) {
			if(mTempRect.isEmpty()) {
				// failsafe
				int n=Utils.getStandardIconSize();
				mItemsBoundingBox.set(0, 0, n, n);
			}
			if(!mPage.config.fitDesktopToItems && (!mPage.isFolder() || mIsEmbedded)) {
				mTempRect.left=(int) (Math.floor(mTempRect.left/(float)view_width)*view_width);
				mTempRect.top=(int) (Math.floor(mTempRect.top/(float)view_height)*view_height);
				mTempRect.right=(int) (Math.ceil(mTempRect.right/(float)view_width)*view_width);
				mTempRect.bottom=(int) (Math.ceil(mTempRect.bottom/(float)view_height)*view_height);
			}

            // check again since the final bounding box may have been modified according to processing above
            if(!mTempRect.equals(mItemsBoundingBox)) {
                mItemsBoundingBox.set(mTempRect);
                mItemsBoundingBoxChanged = true;

                setWallpaperOffsetSteps();
                setWallpaperOffset();
            }
		}
	}

//    long last_draw_date;
//    long last_draw_time;

    public boolean computeAnimations() {
        boolean need_invalidate = false;
        if(mFlinging) {
            fling();
            need_invalidate = true;
        }
        if(mAnimatingNavigation) {
            animateNavigation();
            need_invalidate = true;
        }
        if(mAutoScrolling) {
            adjustPosition(mAutoScrollDx, mAutoScrollDy, 1, false);
            computeCurrentLocalTransformValues();
            need_invalidate = true;
        }
        if(mAnimatingEditMode) {
            need_invalidate = true;
        }
        if(mAnimatingItemOrientation) {
            animateItemRotation();
            need_invalidate = true;
        }
        if(need_invalidate) {
                invalidate();
        }
        return need_invalidate;
    }
    @Override
    public void dispatchDraw(Canvas canvas) {
//        long now = System.nanoTime();
//        long delta = now- last_draw_date;
//        Log.i("XXX", "delta " + delta + " last draw time " + last_draw_time);
//        last_draw_date = now;
        if(!mIsEmbedded) {
            canvas.translate((int) (-mCurrentX), (int) (-mCurrentY));
        }
        mInvalidated = false;

        if(mPage == null) {
            return;
        }

        mAnimationDate = AnimationUtils.currentAnimationTimeMillis();

        computeAnimations();

//		long t1=SystemClock.uptimeMillis();
		//canvas.clipRect(new Rect(0, 0, 480, 1000), Op.REPLACE);
//		if(mLocalTransformMatrixValues[Matrix.MSCALE_X]>1.1f || mLocalTransformMatrixValues[Matrix.MSCALE_X]<0.9f ) {
//			canvas.setDrawFilter(sPaintFlagDrawFilter);
//		}
		final PageConfig config=mPage.config;
		
		if(config==null) return;


        if(mEditMode && mVirtualEditBorders != null) {
            canvas.save(Canvas.MATRIX_SAVE_FLAG);
            canvas.scale(mVirtualEditBordersScale, mVirtualEditBordersScale);
            canvas.translate(mVirtualEditBordersBounds.left, mVirtualEditBordersBounds.top);
            mVirtualEditBorders.draw(canvas);
            canvas.restore();
        }

		canvas.save();
		canvas.concat(mLocalTransform);

        boolean selected_item_is_on_grid = mSelectedItem==null ? mPage.config.newOnGrid : mSelectedItem.getItemConfig().onGrid;
        if(!config.gridAbove) {
            drawGrid(canvas);
        }
        if(mEditMode) {
            if(selected_item_is_on_grid) {
                canvas.drawBitmap(mOriginIcon, (mCellWidth-mOriginIcon.getWidth())/2, (mCellHeight-mOriginIcon.getHeight())/2, null);
            } else {
                canvas.drawBitmap(mOriginIcon, 0, 0, null);
            }
        }
		
		canvas.restore();
		
        if(mEditMode || mAnimatingEditMode) {
            boolean selected_item_is_pinned = mSelectedItem!=null && mSelectedItem.getItemConfig().pinMode!= ItemConfig.PinMode.NONE;
            drawEditGrid(canvas, selected_item_is_on_grid, selected_item_is_pinned);
        }

		drawItems(canvas);
		
		if(config.gridAbove) {
			canvas.save();
			canvas.concat(mLocalTransform);
			drawGrid(canvas);
			canvas.restore();
		}

        if(mSnappingGuideX!=Float.MAX_VALUE || mSnappingGuideY!=Float.MAX_VALUE) {
            mGridLinePaint.setColor(0xffffffff);
            mGridLinePaint.setStrokeWidth(0);
            if(mSnappingGuideX!=Float.MAX_VALUE) {
                canvas.drawLine(mSnappingGuideX, 0, mSnappingGuideX, getHeight(), mGridLinePaint);
            }
            if(mSnappingGuideY!=Float.MAX_VALUE) {
                canvas.drawLine(0, mSnappingGuideY, getWidth(), mSnappingGuideY, mGridLinePaint);
            }


            for (RectF bounds : mSnappingBounds) {
                bounds.round(mTempRect);
                mTempRectF.set(mTempRect);
                canvas.drawRect(mTempRect, mGridLinePaint);
            }
            mGridLinePaint.setStrokeWidth(0);
        }

		if(mEditMode) {
            if(mHandleViewVisible) {
                layoutHandleView();
                drawChild(canvas, mHandleView, mAnimationDate);
            }
        }

//        last_draw_time = System.nanoTime() - now;
	}

	private void drawGrid(Canvas canvas) {
		final PageConfig config=mPage.config;
		final int left=(int) (Math.floor(mItemsBoundingBox.left/mCellWidth)*mCellWidth);
		final int right=(int) (Math.ceil(mItemsBoundingBox.right/mCellWidth)*mCellWidth);
		final int top=(int) (Math.floor(mItemsBoundingBox.top/mCellHeight)*mCellHeight);
		final int bottom=(int) (Math.ceil(mItemsBoundingBox.bottom/mCellHeight)*mCellHeight);
		final int hcolor=config.gridLayoutModeHorizontalLineColor;
		if(Color.alpha(hcolor)!=0) {
			mGridLinePaint.setColor(hcolor);
			mGridLinePaint.setStrokeWidth(config.gridLayoutModeHorizontalLineThickness);
            float count = (bottom - (float)top) / mCellHeight;
            if(count < 1000) {
                for (float y = top + mCellHeight; y < bottom; y += mCellHeight) {
                    canvas.drawLine(left, y, right, y, mGridLinePaint);
                }
            }
		}
		
		final int vcolor=config.gridLayoutModeVerticalLineColor;
		if(Color.alpha(vcolor)!=0) {
			mGridLinePaint.setColor(vcolor);
			mGridLinePaint.setStrokeWidth(config.gridLayoutModeVerticalLineThickness);
            float count = (right - (float)left) / mCellWidth;
            if(count < 1000) {
                for (float x = left + mCellWidth; x < right; x += mCellWidth) {
                    canvas.drawLine(x, top, x, bottom, mGridLinePaint);
                }
            }
		}
	}

	private void drawEditGrid(Canvas canvas, boolean grid_mode, boolean selected_item_is_pinned) {
		final int w=getWidth();
		final int h=getHeight();
		final float cw=mCellWidth;
		final float ch=mCellHeight;

        boolean do_transform = !selected_item_is_pinned || !mHonourPinnedItems;

        mTempPts[0]=0; mTempPts[1]=0;
        if(do_transform) mLocalInverseTransform.mapPoints(mTempPts);
		final float sl= (float) (Math.floor(mTempPts[0]/mDesktopWidth)*mDesktopWidth);
		final float st= (float) (Math.floor(mTempPts[1]/mDesktopHeight)*mDesktopHeight);
		final float l=Math.round((int)(mTempPts[0]/cw)*cw);
		final float t=Math.round((int)(mTempPts[1]/ch)*ch);
		
		mTempPts[0]=w; mTempPts[1]=h;
		if(do_transform) mLocalInverseTransform.mapPoints(mTempPts);
		final float r=Math.round((int)(mTempPts[0]/cw)*cw);
		final float b=Math.round((int)(mTempPts[1]/ch)*ch);
		final float sr= (float) (Math.ceil(mTempPts[0]/mDesktopWidth)*mDesktopWidth);
		final float sb= (float) (Math.ceil(mTempPts[1]/mDesktopHeight)*mDesktopHeight);

        int alpha;
        long delta = 0;
        if(mAnimatingEditMode) {
            delta = AnimationUtils.currentAnimationTimeMillis() - mAnimEditModeStartDate;
            if(delta > ANIMATION_EDIT_MODE_DURATION) {
                delta = ANIMATION_EDIT_MODE_DURATION;
            }
            alpha = (int) (255*delta/ANIMATION_EDIT_MODE_DURATION);
            if(!mEditMode) {
                alpha = 255 - alpha;
            }
        } else {
            alpha = 255;
        }

        int calpha;

        if((mAnimatingEditMode || mEditMode) && !mEditFolder) {
            float count = (sr - sl) / w;
            if(count >= MAX_EDIT_GRID_LINE_COUNT) {
                calpha = 0;
            } else {
                calpha = 255 - (int)(255*255*count/(MAX_EDIT_GRID_LINE_COUNT*alpha));
            }
            mGridLinePaint.setAntiAlias(false);
            mGridLinePaint.setColor((calpha<<24) | 0xffffff);
            if(calpha > 0) {
                int i=0;
                for (float x = sl; x <= sr; x += mDesktopWidth) {
                    mTempPts[0] = x;
                    mTempPts[1] = st;
                    if (do_transform) mLocalTransform.mapPoints(mTempPts);
                    float rx = Math.round(mTempPts[0]);
                    if (rx == 0) rx--;
                    mTempGridCrossPts[i++] = rx;
                    mTempGridCrossPts[i++] = 0;
                    mTempGridCrossPts[i++] = rx;
                    mTempGridCrossPts[i++] = h;
                }
                canvas.drawLines(mTempGridCrossPts, 0, i, mGridLinePaint);
            }
            count = (sb - st) / h;
            if(count >= MAX_EDIT_GRID_LINE_COUNT) {
                calpha = 0;
            } else {
                calpha = 255 - (int)(255*255*count/(MAX_EDIT_GRID_LINE_COUNT*alpha));
            }
            mGridLinePaint.setColor((calpha<<24) | 0xffffff);
            if(calpha > 0) {
                int i=0;
                for (float y = st; y <= sb; y += mDesktopHeight) {
                    mTempPts[0] = sl;
                    mTempPts[1] = y;
                    if (do_transform) mLocalTransform.mapPoints(mTempPts);
                    float ry = Math.round(mTempPts[1]);
                    if (ry == 0) ry--;
                    mTempGridCrossPts[i++] = 0;
                    mTempGridCrossPts[i++] = ry;
                    mTempGridCrossPts[i++] = w;
                    mTempGridCrossPts[i++] = ry;
                }
                canvas.drawLines(mTempGridCrossPts, 0, i, mGridLinePaint);
            }
            mGridLinePaint.setColor((alpha<<24) | 0x00ffffff);
            if(mVirtualEditBorders == null) {
                mTempRectF.set(-1, -1, mDesktopWidth+1, mDesktopHeight+1);
                if(do_transform) mLocalTransform.mapRect(mTempRectF);
                canvas.drawRect(mTempRectF, mGridLinePaint);
            }
        }

        final float cross_count=(1+(r-l)/cw)*(1+(b-t)/ch);
        if(cross_count >= MAX_EDIT_GRID_CROSS_COUNT) {
            calpha = 0;
        } else {
            calpha = 255 - (int)(255*255*cross_count/(MAX_EDIT_GRID_CROSS_COUNT*alpha));
        }

        if(calpha > 0) {
            int i = 0;
            mGridLinePaint.setAntiAlias(false);
            mGridLinePaint.setColor((calpha<<24) | (grid_mode ? 0xffffff : 0x0000ff));
            for(float x=l; x<=r; x+=cw) {
                for(float y=t; y<=b; y+=ch) {
                    mTempPts[0] = x;
                    mTempPts[1] = y;
                    if(do_transform) mLocalTransform.mapPoints(mTempPts);
                    float rx = Math.round(mTempPts[0])+0.5f;
                    float ry = Math.round(mTempPts[1])+0.5f;
                    mTempGridCrossPts[i++] = rx-6;
                    mTempGridCrossPts[i++] = ry;
                    mTempGridCrossPts[i++] = rx+5;
                    mTempGridCrossPts[i++] = ry;
                    mTempGridCrossPts[i++] = rx;
                    mTempGridCrossPts[i++] = ry-6;
                    mTempGridCrossPts[i++] = rx;
                    mTempGridCrossPts[i++] = ry+5;
                }
            }
            canvas.drawLines(mTempGridCrossPts, 0, i, mGridLinePaint);
        }

        if(mAnimatingEditMode && delta == ANIMATION_EDIT_MODE_DURATION) {
            mAnimatingEditMode = false;
            if(!mEditMode) {
                mTempGridCrossPts = null;
            }
        }
	}

	private void drawItems(Canvas canvas) {
        int count = getChildCount();
		for(int n=0; n<count; n++) {
            View childView = getChildAt(n);
            if(!(childView instanceof ItemView)) continue;

            ItemView itemView = (ItemView) childView;
            Item item = itemView.getItem();

            Class<?> cls = item.getClass();
            boolean is_shortcut_or_folder = cls == Shortcut.class || cls == Folder.class;

			if(!mEditMode && cls==StopPoint.class && !mAlwaysShowStopPoints) continue;
			//if(mHonourPinnedItems && item.getItemConfig().pinned != pinned) continue;
			
            if(!itemView.isViewVisible()) {
                continue;
            }

            // on API >= 11 views with alpha 0 need to be drawn so that View private flags are correctly handled
            if(Build.VERSION.SDK_INT < 11 && item.getAlpha() == 0) {
                continue;
            }

            Matrix transform = getTransformForItemView(itemView);

            itemView.getHitRect(mTempRect);
            mTempRectF.set(mTempRect);
            if(transform !=null) transform.mapRect(mTempRectF);
            if(!RectF.intersects(mMyRect, mTempRectF)) {
                if(itemView.isInitDone()) {
                    if (itemView.getClass() == EmbeddedFolderView.class) {
                        // don't draw the item, but allow it to continue its animation if needed
                        ItemLayout e_il = ((EmbeddedFolderView) itemView).getEmbeddedItemLayout();
                        if(e_il.computeAnimations()) {
                            invalidate();
                        }
                    }
                } else {
                    float dx = mMyRect.centerX() - mTempRectF.centerX();
                    float dy = mMyRect.centerY() - mTempRectF.centerY();
                    itemView.mDistanceToViewport = dx*dx + dy*dy;
                }
                continue;
            }

            if(!itemView.isInitDone()) {
                mDelayedItemViews.remove(itemView);
                if(mDelayedItemViews.size() == 0) {
                    mDelayedItemViews = null;
                    cancelDelayedItemViewLoad();
                }
                ensureItemViewReady(itemView);
                Rect cell=item.getCell();
                int icw=cell.width();
                int ich=cell.height();
                itemView.measure(MeasureSpec.makeMeasureSpec(Math.round(mCellWidth * icw), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(Math.round(mCellHeight * ich), MeasureSpec.EXACTLY));
                layoutItemGrid(itemView);
            }

            int save_count=canvas.save();


            Rect highlightedCell = mHighlightedCells.get(item.getId());
            if(highlightedCell!=null && itemView.isDragged()) {
                boolean selected_item_is_pinned = item.getItemConfig().pinMode != ItemConfig.PinMode.NONE;
                canvas.save();
                if((!(selected_item_is_pinned) || !mHonourPinnedItems) && !(itemView.isAlwaysPinnedAndVisible() && !mEditMode)) {
                    canvas.concat(mLocalTransform);
                }
                float left = highlightedCell.left * mCellWidth;
                float top = highlightedCell.top * mCellHeight;
                if(item.getItemConfig().selectionEffect == ItemConfig.SelectionEffect.PLAIN) {
                    canvas.drawRect(left, top, highlightedCell.right*mCellWidth, highlightedCell.bottom*mCellHeight, sHighlightedCellsPaint);
                } else {
                    Bitmap draggedBitmap = itemView.getDraggedBitmap();
                    if(draggedBitmap != null) {
                        canvas.save(Canvas.MATRIX_SAVE_FLAG);
                        View sensibleView = itemView.getSensibleView();
                        float px = left + sensibleView.getLeft();
                        float py = top + sensibleView.getTop();
                        float pl = 0;
                        float pt = 0;
                        if(is_shortcut_or_folder) {
                            View vd = ((ShortcutView)itemView).getIconLabelView();
                            pl = vd.getLeft();
                            pt = vd.getTop();
                        }

                        canvas.translate(px, py);
                        Matrix m = itemView.getLocalTransform();
                        if(m != null) {
                            canvas.concat(m);
                        }
                        canvas.translate(pl, pt);
                        int scale = itemView.getDraggedBitmapScale();
                        canvas.scale(scale, scale);
                        canvas.drawBitmap(draggedBitmap, 0, 0, null);
                        canvas.restore();
                    }
                }
                canvas.restore();
            }


            if(transform != null) {
                canvas.concat(transform);
            }

            int vl = itemView.getLeft();
            int vt = itemView.getTop();

            if(is_shortcut_or_folder && itemView.isHighlighted()) {
                ShortcutView shortcutView = (ShortcutView) itemView;
                Bitmap h = shortcutView.getHighlightBitmap();
                if(h != null) {
                    IconLabelView il = shortcutView.getIconLabelView();
                    if(il != null) {
                        canvas.save();
                        canvas.translate(vl, vt);
                        Matrix m = itemView.getLocalTransform();
                        if(m != null) {
                            canvas.concat(m);
                        }
                        canvas.drawBitmap(h, il.getLeft()-mHoloHighlightPadding, il.getTop()-mHoloHighlightPadding, null);
                        canvas.restore();
                    }
                }
            }
//            if(item.isDragged()) {
//                if(item.getItemConfig().onGrid) {
//                    long delta = AnimationUtils.currentAnimationTimeMillis() - mAnimDragSartDate;
//                    float delta_f=delta/(float)ANIMATION_DRAG_DURATION;
//                    boolean stop = false;
//                    if(delta_f > 1) {
//                        delta_f = 1;
//                        if(mAnimatingDrag == -1) {
//                            item.setDragged(false);
//                        }
//                        stop = true;
//                    }
//                    if(mAnimatingDrag == -1) {
//                        delta_f = 1 - delta_f;
//                    }
//                    if(stop) {
//                        mAnimatingDrag = 0;
//                    }
//
//                    float vw = v.getWidth();
//                    float vh = v.getHeight();
//                    float plus = mDecelerateInterpolator.getInterpolation(delta_f)*Utils.getStandardIconSize()/8;
//                    float s = Math.max((vw + plus) / vw, (vh + plus) / vh);
//                    canvas.translate(0, -plus);
//                    canvas.scale(s, s, vw / 2, vh / 2);
//                }
//            }
            drawChild(canvas, itemView, mAnimationDate);
			canvas.restoreToCount(save_count);

            if(mEditMode) {
                final Animation ia = itemView.getAnimation();
                if (itemView.isSelected()) {
                    boolean on_grid = item.getItemConfig().onGrid;
                    mSelectionOutlinePaint.setColor(on_grid ? Color.WHITE : Color.BLUE);
                    mSelectionFillPaint.setColor(on_grid ? 0x40ffffff : 0x400000ff);
                    final float radius = mCurrentScaleS * mTouchSlop / 2.0f;
                    if(ia != null) {
                        if(ia.hasStarted()) {
                            itemView.getHitRect(mTempRect);
                            mTempRectF.set(mTempRect);
                            mTempRectF.inset(2, 2);
                            ia.getTransformation(mAnimationDate, mTempTransformation);
                            mTempTransformation.getMatrix().mapRect(mTempRectF);
                            if(transform !=null) transform.mapRect(mTempRectF);
                            canvas.drawRoundRect(mTempRectF, radius, radius, mSelectionFillPaint);
                            canvas.drawRoundRect(mTempRectF, radius, radius, mSelectionOutlinePaint);
                        }
                    } else {
                        mTempRectF.inset(2, 2);
                        canvas.drawRoundRect(mTempRectF, radius, radius, mSelectionFillPaint);
                        canvas.drawRoundRect(mTempRectF, radius, radius, mSelectionOutlinePaint);
                    }
                }
            }

            ItemConfig.PinMode pinMode = item.getItemConfig().pinMode;
            if(mEditMode && pinMode != ItemConfig.PinMode.NONE) {
                canvas.save();
                if(transform != null) {
                    canvas.concat(transform);
                }
                canvas.clipRect(mTempRect);
                canvas.translate(vl, vt);
                Bitmap pin_icon;
                if(pinMode == ItemConfig.PinMode.XY) {
                    pin_icon = sPinnedItemXY;
                } else if(pinMode == ItemConfig.PinMode.X) {
                    pin_icon = sPinnedItemX;
                } else {
                    pin_icon = sPinnedItemY;
                }
                canvas.drawBitmap(pin_icon, 0, 0, null);
                canvas.restore();
            }
            if(itemView.isChecked()) {
                canvas.save();
                if(transform != null) {
                    canvas.concat(transform);
                }
                float dx=itemView.getRight()-sCheckBox.getWidth();
                canvas.drawBitmap(sCheckBox, dx, vt, null);
                canvas.restore();
            }
        }
	}

	@Override
	public ViewParent invalidateChildInParent(final int[] location, final Rect dirty) {
        invalidate();
        return null;
	}
	
	private boolean mDragHorizontal;

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if(mPage == null) {
            return false;
        }

    	final float ex=ev.getX();
    	final float ey=ev.getY();
    	final float touch_dx = ex-mDownX;
    	final float touch_dy = ey-mDownY;
    	float dx=mAllowScrollX ? touch_dx : 0;
    	float dy=mAllowScrollY ? touch_dy : 0;
    	final float width = getWidth();
    	final float height = getHeight();
    	final PageConfig c = mPage.config;
    	final int action = ev.getAction();

        int actionCode = action & MotionEvent.ACTION_MASK;
        if(actionCode == MotionEvent.ACTION_DOWN) {
            if(mDelayedItemViews != null) {
                cancelDelayedItemViewLoad();
            }
        } else if(actionCode == MotionEvent.ACTION_UP || actionCode == MotionEvent.ACTION_CANCEL) {
            loadNextItemViewLater();
        }

    	if(/*mEditMode &&*/ mTrackedItems.size()>0 && mGestureMode!=GESTURE_ZOOM && mGestureMode!=GESTURE_DRAG) {
    		if(mAutoScrolling && action==MotionEvent.ACTION_UP || action==MotionEvent.ACTION_CANCEL) {
    			mAutoScrolling = false;
    		} else {
    			float ax=0;
    			float ay=0;
	    		if(ex<mAutoScrollRect.left) {
	    			ax=mAutoScrollMargin;
	    		} else if(ex>mAutoScrollRect.right) {
	    			ax=-mAutoScrollMargin;
	    		}
	    		if(ey<mAutoScrollRect.top) {
	    			ay=mAutoScrollMargin;
	    		} else if(ey>mAutoScrollRect.bottom) {
	    			ay=-mAutoScrollMargin;
	    		}
	    		if(ay!=0 || ax!=0) {
                    mAutoScrolling = true;
                    mAutoScrollDx = ax / 4;
                    mAutoScrollDy = ay / 4;
	    		} else {
                    mAutoScrolling = false;
	    		}
    		}
    	}
    	
    	if(actionCode == MotionEvent.ACTION_POINTER_DOWN) {
    		if(mGrabEventTarget!=null) {
    			ev.setAction(MotionEvent.ACTION_CANCEL);
    			dispatchTransformedEventToMotionTarget(mGrabEventTarget, ev, true);
    			mGrabEventTarget=null;
    		}
    	}
    	
    	if(mGrabEventTarget!=null) {
    		boolean res=dispatchTransformedEventToMotionTarget(mGrabEventTarget, ev, true);
    		if(actionCode == MotionEvent.ACTION_UP || actionCode == MotionEvent.ACTION_CANCEL) {
    			mGrabEventTarget = null;
    			mLastTouchedItemView = null;
    		}
    		return res;
    	}
    	
    	
    	
        switch(actionCode) {
        case MotionEvent.ACTION_DOWN:
        	mScreen.onItemLayoutPressed();
            mDownX=mPrevTouchX=ex;
            mDownY=mPrevTouchY=ey;
            
        	if(mFlinging) {
        		mScrollVelocityX=0;
        		mScrollVelocityY=0;
        		computeCurrentLocalTransformValues();
        		mFlinging=false;
        	} else if(mAnimatingNavigation) {
        		computeCurrentLocalTransformValues();
        		mAnimatingNavigation =false;
        	}
        	
            mGestureMode=GESTURE_MAYBE_DRAG;
            
			final int count = getChildCount();
			for (int i = count - 1; i >= 0; i--) {
				View child = getChildAt(i);
				if(child.getVisibility()==View.VISIBLE) {
					if(dispatchTransformedEventToMotionTarget(child, ev, false)) {
						if(child.getParent() == null) {
                            // this is a specific case : it happens when an items is removed because of a script run in the down event
                            break;
                        } else {
                            removeCallbacks(mClickRunnable);
                            mMyMotionTarget = child;
                            if (child instanceof ItemView) {
                                mLastTouchedItemView = (ItemView) child;
                            }
                            return true;
                        }
					}
				}
			}
			
			// no child did want to process the down, so start our long press action for ourself
			mHasLongClicked=false;
			postDelayed(mLongClickRunnable, ViewConfiguration.getLongPressTimeout());
            return true;
            
        case MotionEvent.ACTION_POINTER_DOWN:
            if(mGestureMode!=GESTURE_MAYBE_ZOOM && mGestureMode!=GESTURE_ZOOM) {
                final float ex1=getMotionEventX(ev, 1);
                final float ey1=getMotionEventY(ev, 1);
                mInitialDist=spacing(ev, ex, ey);
                if(mInitialDist>mTouchSlop) {
                    mMidPointX=(ex+ex1)/2;
                    mMidPointY=(ey+ey1)/2;
                    mMidPointRx=(mMidPointX-mCurrentDx)/mCurrentScale;
                    mMidPointRy=(mMidPointY-mCurrentDy)/mCurrentScale;

                    mGestureMode=GESTURE_MAYBE_ZOOM;

                    if(mMyMotionTarget!=null) {
                        ev.setAction(MotionEvent.ACTION_CANCEL);
                        dispatchTransformedEventToMotionTarget(mMyMotionTarget, ev, false);
                        mMyMotionTarget=null;
                    }
                    removeCallbacks(mLongClickRunnable);
                }
            }
            return true;
        
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_CANCEL:
        	mLastTouchedItemView = null;
            mAutoScrolling = false;
            if(mMyMotionTarget!=null) {
            	dispatchTransformedEventToMotionTarget(mMyMotionTarget, ev, false);
            	mMyMotionTarget=null;
            } else {
            	removeCallbacks(mLongClickRunnable);
            	if(action==MotionEvent.ACTION_UP &&!mHasLongClicked && (mGestureMode==GESTURE_NONE || mGestureMode==GESTURE_MAYBE_DRAG || mGestureMode==GESTURE_MAYBE_ZOOM)) {
	            	if((touch_dx*touch_dx+touch_dy*touch_dy)<mTouchSlopSquare) {
	            		if(mClickCount==0) {
	            			mClickCount++;
	            			postDelayed(mClickRunnable, ViewConfiguration.getDoubleTapTimeout());
	            		} else {
	            			mClickCount=0;
	            			removeCallbacks(mClickRunnable);
                            int[] pos = convertTapPositionToLayoutCoordinates();
	            			mScreen.onItemLayoutDoubleClicked(this, pos[0], pos[1]);
	            		}
            		}
            	}
            }
            // don't break
        case MotionEvent.ACTION_POINTER_UP:
            if(mGestureMode==GESTURE_DRAG) {
    			mVelocityTracker.computeCurrentVelocity(1000);
    			mScrollVelocityX=mAllowScrollX ? mVelocityTracker.getXVelocity() : 0;
    			mScrollVelocityY=mAllowScrollY ? mVelocityTracker.getYVelocity() : 0;
    			if(!mAllowScrollX) mScrollVelocityX=0;
    			if(!mAllowScrollY) mScrollVelocityY=0;
    			float velocity_square=mScrollVelocityX*mScrollVelocityX+mScrollVelocityY*mScrollVelocityY;
    			
    			if(c.noDiagonalScrolling && !mEditMode) {
    				if(mDragHorizontal) mScrollVelocityY=0;
    				else mScrollVelocityX=0;
    			}
    			
    			mLocalTransform.getValues(mLocalTransformMatrixValues);
    			float scale = mLocalTransformMatrixValues[Matrix.MSCALE_X];
    	    	float from_x = mLocalTransformMatrixValues[Matrix.MTRANS_X];
    			float from_y = mLocalTransformMatrixValues[Matrix.MTRANS_Y];
    			float width_s = width*scale;
    			float height_s = height*scale;
    			float to_x = from_x/width_s;
    			float to_y = from_y/height_s;
    			if(mAllowFling && velocity_square>mMinFlingVelocitySquare) {
    				long duration=ANIMATION_DURATION;
    				boolean snap = false;
    				if(!mEditMode) {
                        if(c.snapToPages/* && !mHaveStopPoints*/) {
                            to_x = (float) (mScrollVelocityX<0 ? Math.floor(to_x) : Math.ceil(to_x));
                            to_x *= width_s;
                            to_y = (float) (mScrollVelocityY<0 ? Math.floor(to_y) : Math.ceil(to_y));
                            to_y *= height_s;
                            float to_x_s_m = -to_x/scale;
                            float to_y_s_m = -to_y/scale;
                            boolean wrapX = mPage.config.wrapX;
                            boolean wrapY = mPage.config.wrapY;
                            if(!wrapX && (to_x_s_m<mItemsBoundingBox.left || to_x_s_m>mItemsBoundingBox.right)) {
                                to_x=Math.round(from_x/width_s)*width_s;
                            }
                            if(!wrapY && (to_y_s_m<mItemsBoundingBox.top || to_y_s_m>mItemsBoundingBox.bottom)) {
                                to_y=Math.round(from_y/height_s)*height_s;
                            }

                            if((wrapX || wrapY) || mItemsBoundingBox.contains((int)to_x_s_m, (int)to_y_s_m)) {
                                snap = true;
                            } else {
                                snap = false;
                            }
                        } else {
                            mScreenCornersFrom[0] = 0;
                            mScreenCornersFrom[1] = 0;
                            mScreenCornersFrom[2] = getWidth();
                            mScreenCornersFrom[3] = getHeight();

                            mLocalInverseTransform.mapPoints(mScreenCornersFrom);

                            final float from_x_0 = mScreenCornersFrom[0];
                            final float from_y_0 = mScreenCornersFrom[1];
                            final float from_x_1 = mScreenCornersFrom[2];
                            final float from_y_1 = mScreenCornersFrom[3];

                            to_x = from_x;
                            to_y = from_y;
                            float min_d_x = Float.MAX_VALUE;
                            float min_d_y = Float.MAX_VALUE;
                            mSnapStopPoint = null;
                            for(StopPoint sp : mStopPoints) {
                                if(!sp.isSnapping()) {
                                    continue;
                                }

                                final int direction = sp.getDirection();
                                final boolean left_to_right = (direction&StopPoint.DIRECTION_LEFT_TO_RIGHT)!=0;
                                final boolean right_to_left = (direction&StopPoint.DIRECTION_RIGHT_TO_LEFT)!=0;
                                final boolean top_to_bottom = (direction&StopPoint.DIRECTION_TOP_TO_BOTTOM)!=0;
                                final boolean bottom_to_top = (direction&StopPoint.DIRECTION_BOTTOM_TO_TOP)!=0;
                                final boolean desktop_wide = sp.isDesktopWide();
                                final float sp_x = sp.mCurrentViewX;
                                final float sp_y = sp.mCurrentViewY;
                                final boolean in_x_range = desktop_wide || (from_x_0<=sp_x && sp_x<=from_x_1);
                                final boolean in_y_range = desktop_wide || (from_y_0<=sp_y && sp_y<=from_y_1);
                                final int match_edge = sp.getMatchEdge();
                                final boolean match_edge_left = (match_edge&StopPoint.MATCH_EDGE_LEFT)!=0;
                                final boolean match_edge_right = (match_edge&StopPoint.MATCH_EDGE_RIGHT)!=0;
                                final boolean match_edge_top = (match_edge&StopPoint.MATCH_EDGE_TOP)!=0;
                                final boolean match_edge_bottom = (match_edge&StopPoint.MATCH_EDGE_BOTTOM)!=0;

                                for(int i=0; i<2; i++) {
                                    final float fromx = i==0 ? from_x_0 : from_x_1;
                                    final float fromy = i==0 ? from_y_0 : from_y_1;
                                    float d;

                                    if(in_y_range && ((match_edge_left && i==0) || (match_edge_right && i==1))) {
                                        if(mScrollVelocityX>0 && left_to_right) {
                                            d = fromx - sp_x;
                                            if(d>=0 && d<min_d_x) {
                                                min_d_x = d;
                                                to_x = from_x + min_d_x*mCurrentScale;
                                                snap = true;
                                                mSnapStopPoint = sp;
                                            }
                                        } else if(mScrollVelocityX<0 && right_to_left) {
                                            d = sp_x - fromx;
                                            if(d>=0 && d<min_d_x) {
                                                min_d_x = d;
                                                to_x = from_x - min_d_x*mCurrentScale;
                                                snap = true;
                                                mSnapStopPoint = sp;
                                            }
                                        }
                                    }

                                    if(in_x_range && ((match_edge_top && i==0) || (match_edge_bottom && i==1))) {
                                        if(mScrollVelocityY>0 && top_to_bottom) {
                                            d = fromy - sp_y;
                                            if(d>=0 && d<min_d_y) {
                                                min_d_y = d;
                                                to_y = from_y + min_d_y*mCurrentScale;
                                                snap = true;
                                                mSnapStopPoint = sp;
                                            }
                                        } else if(mScrollVelocityY<0 && bottom_to_top) {
                                            d = sp_y - fromy;
                                            if(d>=0 && d<min_d_y) {
                                                min_d_y = d;
                                                to_y = from_y - min_d_y*mCurrentScale;
                                                snap = true;
                                                mSnapStopPoint = sp;
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if(snap) {
                            float anim_velocity_x=(to_x-from_x)*1000/ANIMATION_DURATION;
                            float anim_velocity_y=(to_y-from_y)*1000/ANIMATION_DURATION;

                            float r=1;
                            if(anim_velocity_x!=0) {
                                r=mScrollVelocityX/anim_velocity_x;
                                if(r<0) r=1;
                            }
                            if(anim_velocity_y!=0) {
                                float r2 = mScrollVelocityY/anim_velocity_y;
                                if(r2>r) {
                                    r=r2;
                                }
                            }
                            duration=(long) (duration / r);
                            if(duration>ANIMATION_DURATION) duration=ANIMATION_DURATION;
                            if(duration == 0) duration = 1;
                        }
                    }
    				if(snap) {
    					animateNavigation(from_x, from_y, scale, to_x, to_y, scale, mDecelerateInterpolator, duration);
    				} else {
                        if(mMinDragDx<mDragDx && mDragDx<mMaxDragDx && mMinDragDy<mDragDy && mDragDy<mMaxDragDy) {
                            mScrollDx=mDragDx;
                            mScrollDy=mDragDy;
                            mLastFlingScrollDx = 0;
                            mLastFlingScrollDy = 0;
                            mFlinging=true;
                            mFlingScroller.forceFinished(true);
                            mFlingScroller.fling((int) mScrollDx, (int) mScrollDy, (int) mScrollVelocityX, (int) (mScrollVelocityY), -Integer.MAX_VALUE, Integer.MAX_VALUE, -Integer.MAX_VALUE, Integer.MAX_VALUE);
                            invalidate();
                        } else {
                            computeCurrentLocalTransformValues();
                        }
    				}
    			} else {
    				if(!mEditMode) {
                        if(c.snapToPages/* && !mHaveStopPoints*/) {
                            // snap to page without fling
                            to_x = Math.round(to_x)*width_s;
                            to_y = Math.round(to_y)*height_s;
                            if(mItemsBoundingBox.contains(-(int)to_x, -(int)to_y)) {
                                animateNavigation(from_x, from_y, scale, to_x, to_y, scale, mDecelerateInterpolator, ANIMATION_DURATION);
                            } else {
                                checkOverscroll();
                            }
                        } else {
                            // snap to scroll point without fling
                            mScreenCornersFrom[0] = 0;
                            mScreenCornersFrom[1] = 0;
                            mScreenCornersFrom[2] = getWidth();
                            mScreenCornersFrom[3] = getHeight();

                            mLocalInverseTransform.mapPoints(mScreenCornersFrom);

                            final float from_x_0 = mScreenCornersFrom[0];
                            final float from_y_0 = mScreenCornersFrom[1];
                            final float from_x_1 = mScreenCornersFrom[2];
                            final float from_y_1 = mScreenCornersFrom[3];

                            to_x = from_x;
                            to_y = from_y;
                            float min_d_x = Float.MAX_VALUE;
                            float min_d_y = Float.MAX_VALUE;
                            for(StopPoint sp : mStopPoints) {
                                if(!sp.isSnapping()) {
                                    continue;
                                }

                                final int direction = sp.getDirection();
                                final boolean left_to_right = (direction&StopPoint.DIRECTION_LEFT_TO_RIGHT)!=0;
                                final boolean right_to_left = (direction&StopPoint.DIRECTION_RIGHT_TO_LEFT)!=0;
                                final boolean top_to_bottom = (direction&StopPoint.DIRECTION_TOP_TO_BOTTOM)!=0;
                                final boolean bottom_to_top = (direction&StopPoint.DIRECTION_BOTTOM_TO_TOP)!=0;
                                final boolean desktop_wide = sp.isDesktopWide();
                                final float sp_x = sp.mCurrentViewX;
                                final float sp_y = sp.mCurrentViewY;
                                final boolean in_x_range = desktop_wide || (from_x_0<=sp_x && sp_x<=from_x_1);
                                final boolean in_y_range = desktop_wide || (from_y_0<=sp_y && sp_y<=from_y_1);
                                final int match_edge = sp.getMatchEdge();
                                final boolean match_edge_left = (match_edge&StopPoint.MATCH_EDGE_LEFT)!=0;
                                final boolean match_edge_right = (match_edge&StopPoint.MATCH_EDGE_RIGHT)!=0;
                                final boolean match_edge_top = (match_edge&StopPoint.MATCH_EDGE_TOP)!=0;
                                final boolean match_edge_bottom = (match_edge&StopPoint.MATCH_EDGE_BOTTOM)!=0;

                                for(int i=0; i<2; i++) {
                                    final float fromx = i==0 ? from_x_0 : from_x_1;
                                    final float fromy = i==0 ? from_y_0 : from_y_1;
                                    final float d_x = fromx - sp_x;
                                    final float d_y = fromy - sp_y;

                                    if(in_y_range && ((match_edge_left && i==0) || (match_edge_right && i==1))) {
                                        if(d_x>0 && left_to_right) {
                                            if(d_x<min_d_x) {
                                                min_d_x = d_x;
                                                to_x = from_x + min_d_x*mCurrentScale;
                                            }
                                        } else if(d_x<0 && right_to_left) {
                                            float nd = -d_x;
                                            if(nd<min_d_x) {
                                                min_d_x = nd;
                                                to_x = from_x - min_d_x*mCurrentScale;
                                            }
                                        }
                                    }

                                    if(in_x_range && ((match_edge_top && i==0) || (match_edge_bottom && i==1))) {
                                        if(d_y>0 && top_to_bottom) {
                                            if(d_y<min_d_y) {
                                                min_d_y = d_y;
                                                to_y = from_y + min_d_y*mCurrentScale;
                                            }
                                        } else if(d_y<0 && bottom_to_top) {
                                            float nd = -d_y;
                                            if(nd<min_d_y) {
                                                min_d_y = nd;
                                                to_y = from_y - min_d_y*mCurrentScale;
                                            }
                                        }
                                    }
                                }
                            }
                            if(from_x != to_x || from_y != to_y) {
                                animateNavigation(from_x, from_y, scale, to_x, to_y, scale, mDecelerateInterpolator, ANIMATION_DURATION);
                            } else {
                                checkOverscroll();
                            }
                        }
    				} else {
    					checkOverscroll();
    				}
    			}
        		mVelocityTracker.recycle();
        		mVelocityTracker=null;
    		} else if(mGestureMode==GESTURE_ZOOM) {
    			computeCurrentLocalTransformValues();
    			mScreen.onItemLayoutPinchEnd(true);
    		}
            mGestureMode=GESTURE_NONE;
            if(mEditMode) {
                // update the tracking view
                requestLayout();
                invalidate();
            }
            return true;
            
        case MotionEvent.ACTION_MOVE:
        	if(mMyMotionTarget!=null) {
        		if(dispatchTransformedEventToMotionTarget(mMyMotionTarget, ev, false)) {
        			if(mMyMotionTarget instanceof ViewGroup) {
        				// transform the move into the widget coordinate space in order to safely detect a vertical move
                        ItemView itemView = mMyMotionTarget instanceof ItemView ? (ItemView) mMyMotionTarget : null;
        				
        				mTempPts[0]=ex;
        				mTempPts[1]=ey;
        				if(itemView != null) {
                            inverseMapPoints(itemView, mTempPts);
        					itemView.getItem().getTransform().mapPoints(mTempPts);
        				}
        				
        				float ex_t=mTempPts[0];
        				float ey_t=mTempPts[1];
        				mTempPts[0]=mDownX;
        				mTempPts[1]=mDownY;
        				if(itemView != null) {
                            inverseMapPoints(itemView, mTempPts);
                            itemView.getItem().getTransform().mapPoints(mTempPts);
        				}

                        boolean vertical_motion = Math.abs(mTempPts[1]-ey_t)>Math.abs(mTempPts[0]-ex_t);
                        boolean grab = canScroll(vertical_motion, false);
                        if(grab) {
        					grabEvent(mMyMotionTarget);
        					return true;
        				}
        			}
        			
        		}
        	}
        	
        	final float abs_touch_dx=Math.abs(touch_dx);
			final float abs_touch_dy=Math.abs(touch_dy);
        	if(mGestureMode==GESTURE_MAYBE_DRAG || mGestureMode==GESTURE_DRAG) {
            	if(mGestureMode==GESTURE_MAYBE_DRAG) {
            		if((ev.getEventTime()-ev.getDownTime())>ViewConfiguration.getLongPressTimeout()) {
            			// dragging is disabled after the long tap delay
            			mGestureMode=GESTURE_NONE;
            		} else if((touch_dx*touch_dx+touch_dy*touch_dy)>mTouchSlopSquare) {
            			removeCallbacks(mLongClickRunnable);
        				if(!mAllowScrollX && abs_touch_dx>abs_touch_dy) {
        					if(abs_touch_dx*4>width) {
        						if(touch_dx>0) mScreen.onItemLayoutSwipeLeft(this);
                				else mScreen.onItemLayoutSwipeRight(this);
        						mGestureMode=GESTURE_SWIPE;
        					}
        				} else if(!mAllowScrollY && abs_touch_dy>abs_touch_dx) {
        					if(abs_touch_dy*6>height) {
        						if(touch_dy>0) mScreen.onItemLayoutSwipeDown(this);
                				else mScreen.onItemLayoutSwipeUp(this);
        						mGestureMode=GESTURE_SWIPE;
        					}
        				} else if((dx*dx+dy*dy)>mTouchSlopSquare) {
		            		mGestureMode=GESTURE_DRAG;
		            		mDownX=ex;
		                    mDownY=ey;
		                    dx=0;
		                    dy=0;
		                    mDragHorizontal=abs_touch_dx>abs_touch_dy;
		            		mVelocityTracker=VelocityTracker.obtain();

                            mMinDragDx = -Float.MAX_VALUE;
                            mMaxDragDx = Float.MAX_VALUE;
                            mMinDragDy = -Float.MAX_VALUE;
                            mMaxDragDy = Float.MAX_VALUE;
            			}
                        if(mMyMotionTarget!=null) {
                            ev.setAction(MotionEvent.ACTION_CANCEL);
                            dispatchTransformedEventToMotionTarget(mMyMotionTarget, ev, true);
                            mMyMotionTarget=null;
                        }
                    }
            	}
            	
            	if(mGestureMode==GESTURE_DRAG) {
            		mVelocityTracker.addMovement(ev);
        			mDragDx=dx;
        			mDragDy=dy;
            		if(c.noDiagonalScrolling && !mEditMode) {
	            		if(mDragHorizontal) mDragDy=0;
	            		else mDragDx=0;
            		}

                    if(mEditMode || !mHaveStopPoints) {
                        adjustPosition(mDragDx, mDragDy, 1, !mAllowOverScroll);
                    } else {
                        mScreenCornersFrom[0] = mScreenCornersTo[0] = 0;
                        mScreenCornersFrom[1] = mScreenCornersTo[1] = 0;
                        mScreenCornersFrom[2] = mScreenCornersTo[2] = getWidth();
                        mScreenCornersFrom[3] = mScreenCornersTo[3] = getHeight();

                        mLocalInverseTransform.mapPoints(mScreenCornersFrom);

                        if(!mEditMode) {
                            // reset limit as soon as it does not apply anymore, so that coming back in the opposite direction re-evaluate the limit
                            // this is useful for stop points that are not desktop wide
                            if(mDragDx < mMinDragDx) mDragDx = mMinDragDx;
                            else if(ex > mPrevTouchX) mMinDragDx = -Float.MAX_VALUE;

                            if(mDragDx > mMaxDragDx)  mDragDx = mMaxDragDx;
                            else if(ex < mPrevTouchX) mMaxDragDx = Float.MAX_VALUE;

                            if(mDragDy < mMinDragDy) mDragDy = mMinDragDy;
                            else if(ey > mPrevTouchY) mMinDragDy = -Float.MAX_VALUE;

                            if(mDragDy > mMaxDragDy)  mDragDy = mMaxDragDy;
                            else if(ey < mPrevTouchY) mMaxDragDy = Float.MAX_VALUE;


                        }
                        mPrevTouchX = ex;
                        mPrevTouchY = ey;

                        adjustPosition(mDragDx, mDragDy, 1, !mAllowOverScroll);

                        mLocalInverseTransform.mapPoints(mScreenCornersTo);

                        final float from_x_0 = mScreenCornersFrom[0];
                        final float from_y_0 = mScreenCornersFrom[1];
                        final float from_x_1 = mScreenCornersFrom[2];
                        final float from_y_1 = mScreenCornersFrom[3];
                        final float to_x_0 = mScreenCornersTo[0];
                        final float to_y_0 = mScreenCornersTo[1];
                        final float to_x_1 = mScreenCornersTo[2];
                        final float to_y_1 = mScreenCornersTo[3];

                        boolean sp_hit_x=false, sp_hit_y=false;
                        for(StopPoint sp : mStopPoints) {
                            if((sp.getWhat()&StopPoint.STOP_DRAG)==0) continue;

                            final int direction = sp.getDirection();
                            final boolean left_to_right = (direction&StopPoint.DIRECTION_LEFT_TO_RIGHT)!=0;
                            final boolean right_to_left = (direction&StopPoint.DIRECTION_RIGHT_TO_LEFT)!=0;
                            final boolean top_to_bottom = (direction&StopPoint.DIRECTION_TOP_TO_BOTTOM)!=0;
                            final boolean bottom_to_top = (direction&StopPoint.DIRECTION_BOTTOM_TO_TOP)!=0;
                            final boolean desktop_wide = sp.isDesktopWide();
                            final boolean barrier = sp.isBarrier();
                            final float sp_x = sp.mCurrentViewX;
                            final float sp_y = sp.mCurrentViewY;
                            final boolean in_x_range = desktop_wide || (from_x_0<=sp_x && sp_x<=from_x_1);
                            final boolean in_y_range = desktop_wide || (from_y_0<=sp_y && sp_y<=from_y_1);
                            final int match_edge = sp.getMatchEdge();
                            final boolean match_edge_left = (match_edge&StopPoint.MATCH_EDGE_LEFT)!=0;
                            final boolean match_edge_right = (match_edge&StopPoint.MATCH_EDGE_RIGHT)!=0;
                            final boolean match_edge_top = (match_edge&StopPoint.MATCH_EDGE_TOP)!=0;
                            final boolean match_edge_bottom = (match_edge&StopPoint.MATCH_EDGE_BOTTOM)!=0;

                            for(int i=0; i<2; i++) {
                                final float from_x = i==0 ? from_x_0 : from_x_1;
                                final float from_y = i==0 ? from_y_0 : from_y_1;
                                final float to_x = i==0 ? to_x_0 : to_x_1;
                                final float to_y = i==0 ? to_y_0 : to_y_1;

                                if(in_y_range && ((match_edge_left && i==0) || (match_edge_right && i==1))) {
                                    if((from_x<sp_x || (from_x<=sp_x && barrier)) && sp_x < to_x) {
                                        if(right_to_left) {
                                            if(!sp_hit_x) {
                                                mMinDragDx = mDragDx + (to_x - sp_x) * mCurrentScale;
                                                mDragDx = mMinDragDx;
                                                sp_hit_x = true;
                                            }
                                            mScreen.onItemLayoutStopPointReached(this, sp);
                                        }
                                    } else if((from_x>sp_x || (from_x>=sp_x && barrier)) && sp_x > to_x) {
                                        if(left_to_right) {
                                            if(!sp_hit_x) {
                                                mMaxDragDx = mDragDx - (sp_x - to_x) * mCurrentScale;
                                                mDragDx = mMaxDragDx;
                                                sp_hit_x = true;
                                            }
                                            mScreen.onItemLayoutStopPointReached(this, sp);
                                        }
                                    }
                                }

                                if(in_x_range && ((match_edge_top && i==0) || (match_edge_bottom && i==1))) {
                                    if((from_y<sp_y || (from_y<=sp_y && barrier)) && sp_y < to_y) {
                                        if(bottom_to_top) {
                                            if(!sp_hit_y) {
                                                mMinDragDy = mDragDy + (to_y - sp_y) * mCurrentScale;
                                                mDragDy = mMinDragDy;
                                                sp_hit_y = true;
                                            }
                                            mScreen.onItemLayoutStopPointReached(this, sp);
                                        }
                                    } else if((from_y>sp_y || (from_y>=sp_y && barrier)) && sp_y > to_y) {
                                        if(top_to_bottom) {
                                            if(!sp_hit_y) {
                                                mMaxDragDy = mDragDy - (sp_y - to_y) * mCurrentScale;
                                                mDragDy = mMaxDragDy;
                                                sp_hit_y = true;
                                            }
                                            mScreen.onItemLayoutStopPointReached(this, sp);
                                        }
                                    }
                                }
                            }
                        }
                        if(sp_hit_x || sp_hit_y) {
                            adjustPosition(mDragDx, mDragDy, 1, !mAllowOverScroll);
                        }
                    }
            	}
            } else if(mGestureMode==GESTURE_MAYBE_ZOOM || mGestureMode==GESTURE_ZOOM) {
            	float newDist=spacing(ev, ex, ey);
            	if(mGestureMode==GESTURE_MAYBE_ZOOM) {
//	            	if((ev.getEventTime()-ev.getDownTime())>ViewConfiguration.getLongPressTimeout()) {
//	            		mGestureMode=GESTURE_NONE;
//	            	} else {
		                if((c.pinchZoomEnable || mEditMode) && Math.abs(newDist-mInitialDist)>mTouchSlop*3) {
		                	// reset the initial distance between pointers, so that the zoom starts when the slop distance is reached, better user effect 
		                	mInitialDist=newDist;
		                	mGestureMode=GESTURE_ZOOM;
		                	mScreen.onItemLayoutPinchStart();
		                } else if(mEditMode && (touch_dx*touch_dx+touch_dy*touch_dy)>mTouchSlopSquare*6) {
		            		mGestureMode=GESTURE_DRAG;
		            		mDragHorizontal=abs_touch_dx>abs_touch_dy;
		            		mDownX=ex;
		                    mDownY=ey;
		                    dx=0;
		                    dy=0;
		            		mVelocityTracker=VelocityTracker.obtain();
		            		if(mMyMotionTarget!=null) {
		            			ev.setAction(MotionEvent.ACTION_CANCEL);
		                    	dispatchTransformedEventToMotionTarget(mMyMotionTarget, ev, true);
		                    	mMyMotionTarget=null;
		            		}
            			} else {
		                	final float ex1_=getMotionEventX(ev, 1);
		    	        	final float ey1_=getMotionEventY(ev, 1);
	    	                float mid_point_x=(ex+ex1_)/2;
	    	                float mid_point_y=(ey+ey1_)/2;
	    	                float mt_dx=mid_point_x-mMidPointX;
	    	                float mt_dy=mid_point_y-mMidPointY;
	    	                final float abs_touch_mt_dx=Math.abs(mt_dx);
	        				final float abs_touch_mt_dy=Math.abs(mt_dy);
	    	                if(abs_touch_mt_dx>abs_touch_mt_dy) {
	        					if(abs_touch_mt_dx*4>width) {
	        						if(touch_dx>0) mScreen.onItemLayoutSwipe2Left(this);
	                				else mScreen.onItemLayoutSwipe2Right(this);
	        						mGestureMode=GESTURE_SWIPE;
	        					}
	        				} else if(abs_touch_mt_dy>abs_touch_mt_dx) {
	        					if(abs_touch_mt_dy*4>height) {
	        						if(touch_dy>0) mScreen.onItemLayoutSwipe2Down(this);
	                				else mScreen.onItemLayoutSwipe2Up(this);
	        						mGestureMode=GESTURE_SWIPE;
	        					}
	        				}
		                }
//	            	}
	            }
                if(mGestureMode==GESTURE_ZOOM) {
                    float scale=newDist/mInitialDist;
                    
                    if(mScreen.onItemLayoutPinch(scale)) {
	                    // de reel à ecran:
	                    // 	  e(rx)=rx*scale+dx
	                    // de écran à réél
	                    //    r(ex)=(ex-dx)/scale
	                    
	                    /*
	                    je veux que le point réel apparaisse au meme endroit à l'écran
	                    	mMidPointX = mMidPointRx*new_scale+new_dx
	                    */
	                    float scaled_dx=mMidPointX-(mMidPointRx*mCurrentScale*scale)-mCurrentDx;
	                    float scaled_dy=mMidPointY-(mMidPointRy*mCurrentScale*scale)-mCurrentDy;
	                    adjustPosition(scaled_dx, scaled_dy, scale, false);
                    }
                }
            }
        	
        	return true;
        }

		return false;
	}

    private float mPrevTouchX;
    private float mPrevTouchY;

    private float getMotionEventX(MotionEvent ev, int pointerIndex) {
    	if(mMotionEventGetX!=null) {
    		try {
				return (Float)mMotionEventGetX.invoke(ev, pointerIndex);
			} catch (Exception e) {
				return 0;
			}
    	} else {
    		return ev.getX();
    	}
    }
    
    private float getMotionEventY(MotionEvent ev, int pointerIndex) {
    	if(mMotionEventGetY!=null) {
    		try {
				return (Float)mMotionEventGetY.invoke(ev, pointerIndex);
			} catch (Exception e) {
				return 0;
			}
    	} else {
    		return ev.getY();
    	}
    }
    
    private float spacing(MotionEvent event, final float ex, final float ey) {
        final float dx=ex-getMotionEventX(event, 1);
        final float dy=ey-getMotionEventY(event, 1);
        return (float) Math.sqrt(dx*dx + dy*dy);
    }
    
    private void computeCurrentLocalTransformValues() {
        mCurrentDx=mLocalTransformMatrixValues[Matrix.MTRANS_X];
    	mCurrentDy=mLocalTransformMatrixValues[Matrix.MTRANS_Y];
    	mCurrentScale=mLocalTransformMatrixValues[Matrix.MSCALE_X];

    	configureBounds();
	}
    
    private void configureBounds() {
        boolean use_normal_bb_x = true;
        boolean use_normal_bb_y = true;
    	if(mPage!=null) {
            final PageConfig config = mPage.config;
            boolean no_scroll_limit = config.noScrollLimit;

            if(no_scroll_limit || config.wrapX) {
                mMaxDx = Integer.MAX_VALUE;
    		    mMinDx = Integer.MIN_VALUE;
                use_normal_bb_x = false;
            }

            if(no_scroll_limit || config.wrapY) {
                mMaxDy = Integer.MAX_VALUE;
    		    mMinDy = Integer.MIN_VALUE;
                use_normal_bb_y = false;
            }
    	}

        mTempRectF.set(mItemsBoundingBox);
        mLocalTransform.mapRect(mTempRectF);
        if(use_normal_bb_x) {
            if (mTempRectF.left < 0) mMaxDx = -mTempRectF.left; else mMaxDx = 0;
            if (mTempRectF.right > getWidth()) mMinDx = getWidth() - mTempRectF.right; else mMinDx = 0;
        }
        if(use_normal_bb_y) {
            if (mTempRectF.top < 0) mMaxDy = -mTempRectF.top; else mMaxDy = 0;
            if (mTempRectF.bottom > getHeight()) mMinDy = getHeight() - mTempRectF.bottom; else mMinDy = 0;
        }
    	configureScroll();
    }

    private float computeLogarithmicOver(float linear_over, float max_over) {
    	return (float) ((Math.log10(10+30*linear_over/max_over)-1)*0.3*max_over);
    }

    private float[] checkLimits(float dx, float dy) {
        if(mPage.config.overScrollMode==OverScrollMode.NONE) {
            if(dx>mMaxDx) dx=mMaxDx; else if(dx<mMinDx) dx=mMinDx;
            if(dy>mMaxDy) dy=mMaxDy; else if(dy<mMinDy) dy=mMinDy;
        } else {
            if(dx>mMaxDx) {
                final float max_over=getWidth();
                float over=dx-mMaxDx;
                over=computeLogarithmicOver(over, max_over);

                dx=mMaxDx+over;
                mOverDx=over;
            } else if(dx<mMinDx) {
                final float max_over=getWidth();
                float over=mMinDx-dx;

                over=-computeLogarithmicOver(over, max_over);
                dx=mMinDx+over;
                mOverDx=over;
            } else {
                mOverDx=0;
            }

            if(dy>mMaxDy) {
                final float max_over=getHeight();
                float over=dy-mMaxDy;
                over=computeLogarithmicOver(over, max_over);

                dy=mMaxDy+over;
                mOverDy=over;
            } else if(dy<mMinDy) {
                final float max_over=getHeight();
                float over=mMinDy-dy;

                over=-computeLogarithmicOver(over, max_over);
                dy=mMinDy+over;
                mOverDy=over;
            } else {
                mOverDy=0;
            }
        }

        mTempPts[0] = dx;
        mTempPts[1] = dy;

        return mTempPts;
    }

    private void adjustPosition(float dx, float dy, float scale, boolean honour_limits) {
    	if(honour_limits) {
            float[] pts = checkLimits(dx, dy);
            dx = pts[0];
            dy = pts[1];
    	}

    	float mx=mCurrentDx+dx;
    	float my=mCurrentDy+dy;
    	float ms=mCurrentScale*scale;

        if(mPage.config.wrapX && mAllowWrap) {
            float cx = -mx;
            if (cx < mItemsBoundingBox.left) mx = -mItemsBoundingBox.right + mItemsBoundingBox.left - cx;
            else if (cx > mItemsBoundingBox.right) mx = -mItemsBoundingBox.left - cx + mItemsBoundingBox.right;
        }
        if(mPage.config.wrapY && mAllowWrap) {
            float cy = -my;
            if (cy < mItemsBoundingBox.top) my = -mItemsBoundingBox.bottom + mItemsBoundingBox.top - cy;
            else if (cy > mItemsBoundingBox.bottom) my = -mItemsBoundingBox.top -cy + mItemsBoundingBox.bottom;
        }

    	mLocalTransform.reset();
    	mLocalTransform.setScale(ms, ms);
    	mLocalTransform.postTranslate(mx, my);
    	onLocalTransformModified();
    }
    
    private boolean dispatchTransformedEventToMotionTarget(View v, MotionEvent ev, boolean grab) {
    	if(v.getVisibility()==View.VISIBLE) {
	    	float old_x=ev.getX();
			float old_y=ev.getY();
			mTempPts[0]=old_x;
			mTempPts[1]=old_y;
            ItemView itemView = v instanceof ItemView ? (ItemView) v : null;
            if(itemView != null) {
                inverseMapPoints(itemView, mTempPts);
			}
			if(!grab) {
				v.getHitRect(mTempRect);
				grab=mTempRect.contains((int)mTempPts[0], (int)mTempPts[1]);
				if(!grab && v==mMyMotionTarget) {
					// this case is when an event is forwarded to a target, but the event is outside the view: send a cancel to this view, and continue processing this event for ourself
					int action=ev.getAction();
					ev.setAction(MotionEvent.ACTION_CANCEL);
					dispatchTransformedEventToMotionTarget(v, ev, true);
					ev.setAction(action);
					return false;
				}
			}
			if(grab) {
				mTempPts[0]-=v.getLeft();
				mTempPts[1]-=v.getTop();
				ev.setLocation(mTempPts[0], mTempPts[1]);
		    	boolean result=v.dispatchTouchEvent(ev);
		    	ev.setLocation(old_x, old_y);
		    	return result;
			}
    	}
    	
    	return false;
    }

    private Matrix mTempMatrix = new Matrix();
    private RectF mTempRectF2 = new RectF();

    public Matrix getTransformForRect(RectF rect) {
        final boolean wrapX = mPage.config.wrapX;
        final boolean wrapY = mPage.config.wrapY;
        if((wrapX || wrapY) && mAllowWrap) {
            mTempMatrix.set(mLocalTransform);
            float tx, ty;

            mTempRectF2.set(rect);
            mTempMatrix.mapRect(mTempRectF2);

            if(wrapX) {
                int bb_width = mItemsBoundingBox.width();
                float item_right = mTempRectF2.right - 1; // one pixel less to avoid wrapping for the last item on the grid
                float x = item_right % bb_width;
                if(x < 0) x+= bb_width;
                tx = x - item_right;
            } else {
                tx = 0;
            }

            if(wrapY) {
                int bb_height = mItemsBoundingBox.height();
                float item_bottom = mTempRectF2.bottom - 1;
                float y = item_bottom % bb_height;
                if(y < 0) y += bb_height;
                ty = y - item_bottom;
            } else {
                ty = 0;
            }

            mTempMatrix.postTranslate(tx, ty);

            return mTempMatrix;
        } else {
            return mLocalTransform;
        }
    }

    public Matrix getTransformForItemView(ItemView itemView) {
        Matrix m;
        if(!mHonourPinnedItems && !(itemView.isAlwaysPinnedAndVisible() && !mEditMode)) {
            m = getTransformForUnpinnedItemView(itemView);
        } else {
            switch(itemView.getItem().getItemConfig().pinMode) {
                case NONE:
                    m = getTransformForUnpinnedItemView(itemView);
                    break;
                case X: m = mLocalTransformPinX; break;
                case Y: m = mLocalTransformPinY; break;
                default: m = null; break;
            }
        }

        return m;
    }

    private Matrix getTransformForUnpinnedItemView(ItemView itemView) {
        int vl = itemView.getLeft();
        int vt = itemView.getTop();
        mTempRectF2.set(vl, vt, itemView.getRight(), itemView.getBottom());
        return getTransformForRect(mTempRectF2);
    }

    private Matrix mTempInverseMatrix = new Matrix();

    private void inverseMapPoints(ItemView itemView, float[] pts) {
        if(!mHonourPinnedItems && !(itemView.isAlwaysPinnedAndVisible() && !mEditMode)) {
            mLocalInverseTransform.mapPoints(pts);
        } else {
            switch(itemView.getItem().getItemConfig().pinMode) {
                case NONE:
                    if((mPage.config.wrapX || mPage.config.wrapY) && mAllowWrap) {
                        getTransformForItemView(itemView).invert(mTempInverseMatrix);
                        mTempInverseMatrix.mapPoints(pts);
                    } else {
                        mLocalInverseTransform.mapPoints(pts);
                    }
                    break;
                case X: mLocalInverseTransformPinX.mapPoints(pts); break;
                case Y: mLocalInverseTransformPinY.mapPoints(pts); break;
            }
        }
    }
    
//	@Override
//	protected void onAttachedToWindow() {
//		mCurrentWallpaperOffsetX=-1;
//		mCurrentWallpaperOffsetY=-1;
//		setWallpaperOffset();
//	}

//	private void loadWallpaper() {
//		if(mWallpaper!=null) {
//			//mWallpaper.recycle();
//			mWallpaper=null;
//			System.gc();
//		}
//        
//		final PageConfig config=mWorkspaceConfig;
//		
//        if(config.backgroundWallpaper && config.backgroundWallpaperSoftware) {
//        	if(config.backgroundWallpaperPath!=null) {
//        		try {
//        			mWallpaper = BitmapFactory.decodeFile(config.backgroundWallpaperPath, null);
//        		} catch(OutOfMemoryError e) {
//        			// will try to load the system wp
//        		}
//        	}
//        	if(mWallpaper==null) {	
//				Drawable d;
//	            try {
//	            	d=getContext().getWallpaper();
//	            } catch(Throwable e) {
//	            	d=null;
//	            }
//	            if(d!=null && d instanceof BitmapDrawable) {
//	            	int color=config.backgroundWallpaperTintColor;
//	                if(color!=0) {
//	                    Bitmap w=((BitmapDrawable)d).getBitmap();
//	                    mWallpaper=w.copy(w.getConfig(), true);
//	                    Canvas c=new Canvas(mWallpaper);
//	                    c.drawARGB(Color.alpha(color), Color.red(color), Color.green(color), Color.blue(color));
//	                } else {
//	                    mWallpaper=((BitmapDrawable)d).getBitmap();
//	                }
//	            }
//        	}
//        }
//    }
	
	private void setWallpaperOffsetSteps() {
        if(mWallpaperManager == null) {
            return;
        }

        int w = getWidth();
        int h = getHeight();
        IBinder window_token = getWindowToken();

        if (w != 0 && h != 0 && window_token != null) {
            int x = mItemsBoundingBox.width() / w;
            int y = mItemsBoundingBox.height() / h;

            float sx = x > 1 ? 1 / (float) (x - 1) : 1;
            float sy = y > 1 ? 1 / (float) (y - 1) : 1;
            mWallpaperManager.setWallpaperOffsetSteps(sx, sy);
        }
    }

	private void setWallpaperOffset() {
		if(mWallpaperManager == null) {
			return;
		}

        IBinder window_token = getWindowToken();
        int w = getWidth();
        int h = getHeight();
        if (w != 0 && h != 0 && window_token != null) {
            float x, y;

            mTempRectF.set(0, 0, w, h);
            mLocalInverseTransform.mapRect(mTempRectF);

            final float dw = mItemsBoundingBox.width() - mTempRectF.width();
            if(dw ==0) {
                x = 0.5f;
            } else {
                x = (mTempRectF.left - mItemsBoundingBox.left) / dw;

                if (x > 1) x = 1;
                else if (x < 0) x = 0;
            }

            final float dh = mItemsBoundingBox.height() - mTempRectF.height();
            if(dh == 0) {
                y = 0.5f;
            } else {
                y = (mTempRectF.top - mItemsBoundingBox.top) / dh;

                if (y > 1) y = 1;
                else if (y < 0) y = 0;
            }

            mSetWallpaperOffsetThread.setOffsets(x, y);
//                mWallpaperManager.setWallpaperOffsets(getWindowToken(), x, y);
        }
	}


    private SetWallpaperOffsetThread mSetWallpaperOffsetThread;
    private class SetWallpaperOffsetThread extends Thread {
        private boolean mHasCommand;
        private float mWallpaperOffsetX;
        private float mWallpaperOffsetY;
        private boolean mEnd;
        @Override
        public void run() {
            for(;;) {
                try {
                    synchronized (this) {
                        if (mHasCommand) {
                            mHasCommand = false;
                        } else {
                            this.wait();
                        }
                    }
                } catch (InterruptedException e) {
                    // pass
                }
                if(mEnd) {
                    break;
                }
                IBinder windowToken = getWindowToken();
                if(windowToken != null && mWallpaperManager != null) {
                    mWallpaperManager.setWallpaperOffsets(windowToken, mWallpaperOffsetX, mWallpaperOffsetY);
                }
            }
        }

        public void setOffsets(float x, float y) {
            mWallpaperOffsetX = x;
            mWallpaperOffsetY = y;
            synchronized (this) {
                mHasCommand = true;
                this.notify();
            }
        }


        public void end() {
            synchronized (this) {
                mEnd = true;
                this.notify();
            }
        }
    }


	private void onLocalTransformModified() {
		mLocalTransform.invert(mLocalInverseTransform);
		mLocalTransform.getValues(mLocalTransformMatrixValues);

        mTempPts[0]=mTempPts[1]=0;
        mLocalTransform.mapPoints(mTempPts);
        mLocalTransformPinX.setTranslate(0, mTempPts[1]);
        mLocalTransformPinY.setTranslate(mTempPts[0], 0);
        mLocalTransformPinX.invert(mLocalInverseTransformPinX);
        mLocalTransformPinY.invert(mLocalInverseTransformPinY);

		setWallpaperOffset();
        
		float x = mLocalTransformMatrixValues[Matrix.MTRANS_X];
		float y = mLocalTransformMatrixValues[Matrix.MTRANS_Y];
		float s = mLocalTransformMatrixValues[Matrix.MSCALE_X];
		if(x != mCurrentX || y != mCurrentY || s != mCurrentScaleS) {
			mCurrentX = x;
			mCurrentY = y;
			mCurrentScaleS = s;
			if(!mEditMode) {
                // do not call onItemLayoutPositionChanged now, but in another loop so that new layout requests are not issued in the middle of a onMeasure/onLayout cycle
//                removeCallbacks(mDelayedOnItemLayoutPositionChanged);
//                post(mDelayedOnItemLayoutPositionChanged);
                mScreen.onItemLayoutPositionChanged(ItemLayout.this, mCurrentX, mCurrentY, mCurrentScaleS);

			}
            updatePageIndicators();

            scrollTo((int) (-mCurrentX), (int) (-mCurrentY));
		}

		invalidate();
	}

    private void updatePageIndicators() {
        if(mPageIndicators != null) {
            for (PageIndicatorView piv : mPageIndicators) {
                piv.onItemLayoutPositionChanged(mCurrentX, mCurrentY, mCurrentScaleS, getWidth(), getHeight(), mItemsBoundingBox);
            }
        }
    }

    private Runnable mUpdatePageIndicatorsRunnable = new Runnable() {
        @Override
        public void run() {
            updatePageIndicators();
        }
    };

    private void updatePageIndicatorsLater() {
        removeCallbacks(mUpdatePageIndicatorsRunnable);
        post(mUpdatePageIndicatorsRunnable);
    }

	private void animateNavigation(float from_x, float from_y, float from_scale, float to_x, float to_y, float to_scale, Interpolator interpolator, long duration) {
        mFlinging = false;
        mGestureMode=GESTURE_NONE;
		mAnimNavigationFromX =from_x;
		mAnimNavigationFromY =from_y;
		mAnimNavigationFromScale =from_scale;
		mAnimNavigationToX =to_x;
		mAnimNavigationToY =to_y;
		mAnimNavigationToScale =to_scale;
		mAnimInterpolator=interpolator;
		mAnimDurationStartDate = AnimationUtils.currentAnimationTimeMillis();
		mAnimatingNavigation =true;
		mAnimNavigationDuration = duration;
		animateNavigation();
	}

	private void animateNavigation() {
		long delta=AnimationUtils.currentAnimationTimeMillis() - mAnimDurationStartDate;
		if(delta > mAnimNavigationDuration) {
			delta = mAnimNavigationDuration;
        }
		float delta_f=delta/(float) mAnimNavigationDuration;
		float interp=mAnimInterpolator.getInterpolation(delta_f);

		float tx=(mAnimNavigationToX - mAnimNavigationFromX)*interp+ mAnimNavigationFromX;
		float ty=(mAnimNavigationToY - mAnimNavigationFromY)*interp+ mAnimNavigationFromY;
		float s=(mAnimNavigationToScale - mAnimNavigationFromScale)*interp+ mAnimNavigationFromScale;
		mAnimTransform.setScale(s, s);
		mAnimTransform.postTranslate(tx, ty);

        if(s != mCurrentScaleS) {
            mScreen.onItemLayoutZoomChanged(s);
        }
		
		// bypass the use of setLocalTransform in order to avoid useless layout requests
		mLocalTransform.set(mAnimTransform);
		onLocalTransformModified();
		
		if(delta >= mAnimNavigationDuration) {
			mAnimatingNavigation =false;
			computeCurrentLocalTransformValues();
			mScreen.onItemLayoutPinchEnd(false);
            if(mSnapStopPoint != null) {
                mScreen.onItemLayoutStopPointReached(this, mSnapStopPoint);
                mSnapStopPoint = null;
            }
            if(mEditMode) {
                // update the tracking view
                requestLayout();
            }
		}
	}

	private float[] mScreenCornersFrom=new float[4];
	private float[] mScreenCornersTo=new float[4];
    private float mLastFlingScrollDx;
    private float mLastFlingScrollDy;

	private void fling() {
        boolean finished = !mFlingScroller.computeScrollOffset();
        float scroll_dx = mScrollVelocityX==0 ? mLastFlingScrollDx : mFlingScroller.getCurrX();
        float scroll_dy = mScrollVelocityY==0 ? mLastFlingScrollDy : mFlingScroller.getCurrY();

        float pts[] = checkLimits(scroll_dx, scroll_dy);
        scroll_dx = pts[0];
        scroll_dy = pts[1];

        if(mPage.config.wrapX) {
            scroll_dx = scroll_dx % mItemsBoundingBox.width();
        }
        if(mPage.config.wrapY) {
            scroll_dy = scroll_dy % mItemsBoundingBox.height();
        }

        if(!mHaveStopPoints) {
		    adjustPosition(scroll_dx, scroll_dy, 1, false);
        } else {
            boolean sp_hit_x = false;
            boolean sp_hit_y = false;

            mScreenCornersFrom[0] = mScreenCornersTo[0] = 0;
            mScreenCornersFrom[1] = mScreenCornersTo[1] = 0;
            mScreenCornersFrom[2] = mScreenCornersTo[2] = getWidth();
            mScreenCornersFrom[3] = mScreenCornersTo[3] = getHeight();

            mLocalInverseTransform.mapPoints(mScreenCornersFrom);

            adjustPosition(scroll_dx, scroll_dy, 1, false);

            mLocalInverseTransform.mapPoints(mScreenCornersTo);

            final float from_x_0 = mScreenCornersFrom[0];
            final float from_y_0 = mScreenCornersFrom[1];
            final float from_x_1 = mScreenCornersFrom[2];
            final float from_y_1 = mScreenCornersFrom[3];
            final float to_x_0 = mScreenCornersTo[0];
            final float to_y_0 = mScreenCornersTo[1];
            final float to_x_1 = mScreenCornersTo[2];
            final float to_y_1 = mScreenCornersTo[3];

            for(StopPoint sp : mStopPoints) {

                if((sp.getWhat()&StopPoint.STOP_SCROLL)==0) continue;

                final int direction = sp.getDirection();
                final boolean left_to_right = (direction&StopPoint.DIRECTION_LEFT_TO_RIGHT)!=0;
                final boolean right_to_left = (direction&StopPoint.DIRECTION_RIGHT_TO_LEFT)!=0;
                final boolean top_to_bottom = (direction&StopPoint.DIRECTION_TOP_TO_BOTTOM)!=0;
                final boolean bottom_to_top = (direction&StopPoint.DIRECTION_BOTTOM_TO_TOP)!=0;
                final boolean desktop_wide = sp.isDesktopWide();
                final float sp_x = sp.mCurrentViewX;
                final float sp_y = sp.mCurrentViewY;
                final boolean in_x_range = desktop_wide || (from_x_0<=sp_x && sp_x<=from_x_1);
                final boolean in_y_range = desktop_wide || (from_y_0<=sp_y && sp_y<=from_y_1);
                final int match_edge = sp.getMatchEdge();
                final boolean match_edge_left = (match_edge&StopPoint.MATCH_EDGE_LEFT)!=0;
                final boolean match_edge_right = (match_edge&StopPoint.MATCH_EDGE_RIGHT)!=0;
                final boolean match_edge_top = (match_edge&StopPoint.MATCH_EDGE_TOP)!=0;
                final boolean match_edge_bottom = (match_edge&StopPoint.MATCH_EDGE_BOTTOM)!=0;

                for(int i=0; i<2; i++) {
                    final float from_x = i==0 ? from_x_0 : from_x_1;
                    final float from_y = i==0 ? from_y_0 : from_y_1;
                    final float to_x = i==0 ? to_x_0 : to_x_1;
                    final float to_y = i==0 ? to_y_0 : to_y_1;

                    if(in_y_range && ((match_edge_left && i==0) || (match_edge_right && i==1))) {
                        if(from_x < sp_x && sp_x < to_x) {
                            if(right_to_left) {
                                if(!sp_hit_x) {
                                    scroll_dx = scroll_dx + (to_x - sp_x) * mCurrentScale;
                                    sp_hit_x = true;
                                }
                                mScreen.onItemLayoutStopPointReached(this, sp);

                            }
                        } else if(from_x > sp_x && sp_x > to_x) {
                            if(left_to_right) {
                                if(!sp_hit_x) {
                                    scroll_dx = scroll_dx - (sp_x - to_x) * mCurrentScale;
                                    sp_hit_x = true;
                                }
                                mScreen.onItemLayoutStopPointReached(this, sp);
                            }
                        }
                    }

                    if(in_x_range && ((match_edge_top && i==0) || (match_edge_bottom && i==1))) {
                        if(from_y < sp_y && sp_y < to_y) {
                            if(bottom_to_top) {
                                if(!sp_hit_y) {
                                    scroll_dy = scroll_dy + (to_y - sp_y) * mCurrentScale;
                                    sp_hit_y = true;
                                }
                                mScreen.onItemLayoutStopPointReached(this, sp);
                            }
                        } else if(from_y > sp_y && sp_y > to_y) {
                            if(top_to_bottom) {
                                if(!sp_hit_y) {
                                    scroll_dy = scroll_dy - (sp_y - to_y) * mCurrentScale;
                                    sp_hit_y = true;
                                }
                                mScreen.onItemLayoutStopPointReached(this, sp);
                            }
                        }
                    }
                }
            }

            if(sp_hit_x || sp_hit_y) {
                if(sp_hit_x) mScrollVelocityX = 0;
                if(sp_hit_y) mScrollVelocityY = 0;
                adjustPosition(scroll_dx, scroll_dy, 1, false);
                mOverDx = 0;
                mOverDy = 0;
                if(mScrollVelocityX==0 && mScrollVelocityY==0) {
                    finished = true;
                }
            }
		}

        if(mOverDx!=0 || mOverDy!=0) {
            int size = Utils.getStandardIconSize();
            if(Math.abs(mOverDx)>size || Math.abs(mOverDy)>size) {
                finished = true;
            } else {
                float threshold = mTouchSlop;
                if(Math.abs(mLastFlingScrollDx - scroll_dx)<threshold && Math.abs(mLastFlingScrollDy - scroll_dy)<threshold) {
                    finished = true;
                }
            }
        }
        mLastFlingScrollDx = scroll_dx;
        mLastFlingScrollDy = scroll_dy;

        if(finished) {
			mFlinging=false;
			checkOverscroll();
		}
	}

    public void cancelFling() {
        if(mFlinging) {
            mFlinging = false;
            checkOverscroll();
        }
    }
	
	private void checkOverscroll() {
		computeCurrentLocalTransformValues();
		if(!mAllowOverScroll) {
			if(mOverDx!=0 || mOverDy!=0) {
				animateNavigation(mCurrentDx, mCurrentDy, mCurrentScale, mCurrentDx - mOverDx, mCurrentDy - mOverDy, mCurrentScale, mOverScrollInterpolator, ANIMATION_DURATION);
				mOverDx=0;
				mOverDy=0;
			}
		}
	}

	private boolean hasScrollableView(ViewGroup v) {
		final int n=v.getChildCount();
		for(int i=0; i<n; i++) {
			final View c=v.getChildAt(i);
			if(c instanceof AbsListView || c instanceof ScrollView || c.getClass().getSimpleName().equals("StackView") ) {
				return true;
			}
			if(c instanceof ViewGroup) {
				if(hasScrollableView((ViewGroup)c)) {
					return true;
				}
			}
		}
		return false;
	}

    private PageConfig.ScrollingDirection mEnforcedScrollingDirection;

    public void enforceScollingDirection(PageConfig.ScrollingDirection direction) {
        mEnforcedScrollingDirection = direction;
    }

    private PageConfig.ScrollingDirection getScrollingDirection() {
        return mEnforcedScrollingDirection==null ? mPage.config.scrollingDirection : mEnforcedScrollingDirection;
    }
	
	private static final float sMinScrollThreshold=0.5f;
	public void configureScroll() {
		if(mAllowOverScroll) {
			mAllowScrollX=true;
			mAllowScrollY=true;
		} else {
			if(mPage!=null) {
                switch(getScrollingDirection()) {
				case AUTO:
					mAllowScrollX=mMinDx<-sMinScrollThreshold || mMaxDx>sMinScrollThreshold;//mItemsBoundingBox.width()>getWidth();
					mAllowScrollY=mMinDy<-sMinScrollThreshold || mMaxDy>sMinScrollThreshold;//mItemsBoundingBox.height()>getHeight();
					break;
				case X: mAllowScrollX=true; mAllowScrollY=false; break;
				case Y: mAllowScrollX=false; mAllowScrollY=true; break;
				case XY: mAllowScrollX=true; mAllowScrollY=true; break;
				case NONE: mAllowScrollX=false; mAllowScrollY=false; break;
				}
			}
		}
	}

    private static int[] sTmpInt = new int[2];
    private int[] convertTapPositionToLayoutCoordinates() {
        mTempPts[0]=mDownX;
        mTempPts[1]=mDownY;
        mLocalInverseTransform.mapPoints(mTempPts);
        int x = (int) mTempPts[0];
        int y = (int) mTempPts[1];

        if(mPage.config.wrapX && mAllowWrap) {
            if(x > mItemsBoundingBox.right) y = mItemsBoundingBox.left + x - mItemsBoundingBox.right;
            else if(x < mItemsBoundingBox.left) x = mItemsBoundingBox.right - mItemsBoundingBox.left + x;
        }
        if(mPage.config.wrapY && mAllowWrap) {
            if(y > mItemsBoundingBox.bottom) y = mItemsBoundingBox.top + y - mItemsBoundingBox.bottom;
            else if(y < mItemsBoundingBox.top) y = mItemsBoundingBox.bottom - mItemsBoundingBox.top + y;
        }

        sTmpInt[0] = x;
        sTmpInt[1] = y;

        return sTmpInt;
    }

    private boolean mHonourFocusChange = true;
    public void setHonourFocusChange(boolean h) {
        mHonourFocusChange = h;
    }

    private void ensureTempRectFVisible() {
        mLocalTransform.mapRect(mTempRectF);
        float dx=0, dy=0;

        if(mTempRectF.right>getWidth()) dx=getWidth()-mTempRectF.right;
        if(mTempRectF.left<0) dx=-mTempRectF.left;

        if(mTempRectF.bottom>getHeight()) dy=getHeight()-mTempRectF.bottom;
        if(mTempRectF.top<0) dy=-mTempRectF.top;

        float from_x=mLocalTransformMatrixValues[Matrix.MTRANS_X];
        float from_y=mLocalTransformMatrixValues[Matrix.MTRANS_Y];
        float from_scale=mLocalTransformMatrixValues[Matrix.MSCALE_X];

        animateNavigation(from_x, from_y, from_scale, from_x + dx, from_y + dy, from_scale, mDecelerateInterpolator, ANIMATION_DURATION);
    }

    public void ensureCellVisible(Rect cell) {
        // only works in grid mode
        computeCurrentLocalTransformValues();
        mTempRectF.set(cell.left, cell.top, cell.right, cell.bottom);
        mTempRectF.left*=mCellWidth;
        mTempRectF.right*=mCellWidth;
        mTempRectF.top*=mCellHeight;
        mTempRectF.bottom*=mCellHeight;
        ensureTempRectFVisible();
    }

	public void ensureChildViewVisible(View v, boolean honour_scrolling_direction) {
        if(!mHonourFocusChange || mPage == null) {
            // in some situations setting the page to null will remove views and will trigger a focus change and the call to this method
            return;
        }

		computeCurrentLocalTransformValues();
		mTempRectF.set(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
		mLocalTransform.mapRect(mTempRectF);
		float dx=0, dy=0;
		
		if(mTempRectF.right>getWidth()) dx=getWidth()-mTempRectF.right;
		if(mTempRectF.left<0) dx=-mTempRectF.left;
		
		if(mTempRectF.bottom>getHeight()) dy=getHeight()-mTempRectF.bottom;
		if(mTempRectF.top<0) dy=-mTempRectF.top;
		
		float from_x=mLocalTransformMatrixValues[Matrix.MTRANS_X];
		float from_y=mLocalTransformMatrixValues[Matrix.MTRANS_Y];
		float from_scale=mLocalTransformMatrixValues[Matrix.MSCALE_X];

        if(honour_scrolling_direction) {
            switch(getScrollingDirection()) {
                case NONE: dx = 0; dy = 0; break;
                case X: dy = 0; break;
                case Y: dx = 0;
            }
        }

		animateNavigation(from_x, from_y, from_scale, from_x + dx, from_y + dy, from_scale, mDecelerateInterpolator, ANIMATION_DURATION);
	}

    public void adjustTranslationAfterSizeChange(int w, int h, int oldw, int oldh) {
        if(oldw != 0 && oldh != 0 && mPage != null) {
            // the following test is to detect increase/decrease in size or rotation change for folders (increase/decrease in size is enter edit mode)
            if(!mPage.isFolder() || (w>oldw && h<oldh) || (w<oldw && h>oldh)) {
                mLocalTransform.postTranslate(-mCurrentDx, -mCurrentDy);
                mLocalTransform.postTranslate(mCurrentDx*w/oldw, mCurrentDy*h/oldh);
                onLocalTransformModified();
                computeCurrentLocalTransformValues();
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onWindowSystemUiVisibilityChanged(int visible) {
        if(mPage != null) {
            int id = mPage.id;
            if (Page.isDashboard(id) || id == Page.APP_DRAWER_PAGE) {
                mScreen.onItemLayoutWindowSystemUiVisibility(this, visible);
            }
        }
    }

    @Override
    public void invalidate() {
        if(!mInvalidated) {
            if(mDelayedItemViews != null) {
                cancelDelayedItemViewLoad();
                loadNextItemViewLater();
            }
            mInvalidated = true;
            View parent = (View) getParent();
            if (parent != null) parent.invalidate();
            super.invalidate();
        }
    }

    @Override
    public void invalidate(int l, int t, int r, int b) {
        if(!mInvalidated) {
            if (mDelayedItemViews != null) {
                cancelDelayedItemViewLoad();
                loadNextItemViewLater();
            }
            mInvalidated = true;
            super.invalidate(); // why not this: invalidate(l, t, r, b); ?
        }
    }


    /***************************************** VARIOUS EVENTS ***********************************/
    /*
    These handlers are called by the screen, which itself receives events either from item views or the engine.
     */
    public void onPageModified() {
        ArrayList<Integer> selectedItemIds = new ArrayList<>();
        int masterSelectedItemId = mSelectedItem==null ? Item.NO_ID : mSelectedItem.getId();
        for (int i=mItemViews.size()-1; i>=0; i--) {
            ItemView itemView = mItemViews.valueAt(i);
            if(itemView.isInitDone()) {
                if (itemView.isSelected()) {
                    selectedItemIds.add(itemView.getItem().getId());
                }
            }
            removeViewForItem(itemView.getItem());
        }

        loadPage(false);
        if(masterSelectedItemId != Item.NO_ID) {
            setMasterSelectedItem(mPage.findItemById(masterSelectedItemId));
        }
        for (Integer id : selectedItemIds) {
            ItemView itemView = getItemView(mPage.findItemById(id));
            if(itemView != null) {
                itemView.setSelected(true);
            }
        }
        mScreen.onItemLayoutPageLoaded(this, mPage, mPage);
    }

    public void onPageItemAdded(Item item) {
        setupItemView(item, mPage.items.indexOf(item), false, null);
        onItemViewChanged(item);
    }


    public void onPageItemRemoved(Item item) {
        if(mSelectedItem == item) {
            setMasterSelectedItem(null);
        }
        removeViewForItem(item);
        onItemViewChanged(item);
    }

    private void removeViewForItem(Item item) {
        if (mMyMotionTarget != null && (mMyMotionTarget.getTag() == item)) {
            MotionEvent event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_CANCEL, 0, 0, 0);
            mMyMotionTarget.dispatchTouchEvent(event);
            grabEvent(null);
        }
        ItemView itemView = getItemView(item);
        if(mHandleItemView == itemView) {
            hideHandleView();
            mHandleItemView = null;
        }
        if(mLastTouchedItemView == itemView) {
            mLastTouchedItemView = null;
        }
        final Class<? extends Item> itemClass = item.getClass();
        if (itemClass == StopPoint.class) {
            mStopPoints.remove(item);
            mHaveStopPoints = mStopPoints.size() > 0;
        } else if (itemClass == PageIndicator.class && itemView != null) {
            mPageIndicators.remove(itemView);
        }
        if(itemView != null) {
            if(mDelayedItemViews != null) {
                mDelayedItemViews.remove(itemView);
                if(mDelayedItemViews.size() == 0) {
                    mDelayedItemViews = null;
                }
            }
            if(itemView.isInitDone()) {
                if (mIsResumed) itemView.pause();
            }
            itemView.destroy();
            mItemViews.remove(item.getId());
            removeView(itemView);
            itemView.setItemLayout(null);
        }
    }

    public void onPageItemChanged(Item item) {
        // there is one instance where there is no itemview for the item : a folder is not displayed if not in the custom mode in the app drawer
        ItemView itemView = getItemView(item);

        boolean selected = itemView==null ? false : itemView.isSelected();
        boolean hadHandle = mHandleItemView==itemView;
        boolean wasLastTouchedItemView = mLastTouchedItemView == itemView;

        net.pierrox.lightning_launcher.script.api.Item cachedItem = itemView==null ? null : item.getPage().getEngine().getScriptExecutor().getLightning().getCachedItem(itemView);
        removeViewForItem(item);
        ItemView newItemView = setupItemView(item, mPage.items.indexOf(item), false, cachedItem);

        // don't send selection change event, because the view has only been rebuilt, selection is preserved
        newItemView.setSelected(selected, false);
        if(hadHandle) {
            showHandleViewForItemView(newItemView);
        }
        if(wasLastTouchedItemView) {
            mLastTouchedItemView = newItemView;
        }

        onItemViewChanged(item);
    }

    public void onPageItemZIndexChanged(int old_index, int new_index) {
        View v = getChildAt(old_index);
        if(v != null) {
            removeViewAt(old_index);
            addView(v, new_index);
        }
    }


    public void onItemSelectionChanged(Item item, boolean selected) {
        if(item == mSelectedItem && !selected) {
            setMasterSelectedItem(null);
        }
        invalidate();
    }

    public void onItemCellChanged(Item item) {
        onItemViewChanged(item);
    }
}
