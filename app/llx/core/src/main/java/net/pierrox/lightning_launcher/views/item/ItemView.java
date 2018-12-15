package net.pierrox.lightning_launcher.views.item;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.Interpolator;
import android.view.animation.TranslateAnimation;

import net.pierrox.lightning_launcher.configuration.ItemConfig;
import net.pierrox.lightning_launcher.data.Box;
import net.pierrox.lightning_launcher.data.Folder;
import net.pierrox.lightning_launcher.data.Item;
import net.pierrox.lightning_launcher.data.Shortcut;
import net.pierrox.lightning_launcher.data.StopPoint;
import net.pierrox.lightning_launcher.engine.LightningEngine;
import net.pierrox.lightning_launcher.engine.Screen;
import net.pierrox.lightning_launcher.views.BoxLayout;
import net.pierrox.lightning_launcher.views.HandleView;
import net.pierrox.lightning_launcher.views.HolographicOutlineHelper;
import net.pierrox.lightning_launcher.views.ItemLayout;
import net.pierrox.lightning_launcher.views.TouchEventInterceptor;
import net.pierrox.lightning_launcher.views.TransformLayout;

public abstract class ItemView extends TransformLayout implements TouchEventInterceptor.OnInterceptorListener, View.OnKeyListener, View.OnFocusChangeListener {
    public interface ItemViewListener {
        public void onItemViewPressed(ItemView itemView);
        public void onItemViewUnpressed(ItemView itemView);
        public void onItemViewMove(ItemView itemView, float dx, float dy);
        public void onItemViewClicked(ItemView itemView);
//        public void onItemViewDoubleClicked(Item item);
        public void onItemViewLongClicked(ItemView itemView);
        public void onItemViewAction(ItemView itemView, int action);
        public boolean onItemViewTouch(ItemView itemView, MotionEvent event);
        public void onItemViewSelectionChanged(ItemView itemView, boolean selected);
    }

    private static final int[][] RIPPLE_STATES_LIST = {{android.R.attr.state_pressed}};
    private static final int[] STATE_PRESSED = new int[]{android.R.attr.state_pressed, android.R.attr.state_enabled};
    private static final int MINIMUM_ALPHA = 50;
    public static final long ANIMATION_TRANSLATE_DURATION = 200;
    private static final Box sStopPointBox=new Box();
    private static final Interpolator sAccelerateDecelerateInterpolator = new AccelerateDecelerateInterpolator();

    protected Item mItem;

    private ItemLayout mItemLayout;

    protected BoxLayout mSensibleView;

    protected int mAlpha;
    private boolean mOverrideVisible;
    private boolean mAlwaysPinnedAndVisible;

    protected boolean mHighlighted;
    protected boolean mFocused;
    private boolean mDragged;
    private boolean mSelected;
    private boolean mChecked;

    protected boolean mResumed;

    private Bitmap mDraggedBitmap;
    private int mDraggedBitmapScale;

    private boolean mDpadCenterDown;
    private boolean mHasLongDpadCenter;
    private Runnable mLongDpadCenterRunnable=new Runnable() {
        @Override
        public void run() {
            mHasLongDpadCenter=true;
            getScreen().onItemViewLongClicked(ItemView.this);
        }
    };

    protected boolean mDelayedHighlightedState;
    private Runnable mHighlightRunnable =new Runnable() {
        @Override
        public void run() {
            setHighlightedNow(mDelayedHighlightedState);
        }
    };

    protected HolographicOutlineHelper mOutlineHelper;

    // hack: used to prioritize view loading : those near to the viewport will be loaded first
    public float mDistanceToViewport;

    public ItemView(Context context, Item item) {
        super(context);

        mItem = item;

        // unlike other properties, visibility is applied at the root view and must be done early, before init
        updateViewVisibility();
    }

    public abstract void init();

    public boolean isInitDone() {
        return mSensibleView != null;
    }

    public void destroy() {
        LightningEngine engine = mItem.getPage().getEngine();
        engine.getVariableManager().updateBindings(this, null, true, null, true);
        engine.getScriptExecutor().getLightning().clearCachedItem(this);
        if(mSensibleView != null) {
            mSensibleView.destroy();
        }
    }

    public void pause() {
        mSensibleView.pause();
        mResumed = false;
        getParentItemLayout().getScreen().runAction(mItem.getPage().getEngine(), "I_PAUSED", mItem.getItemConfig().paused, this);
    }

    public void resume() {
        mSensibleView.resume();
        mResumed = true;
        getParentItemLayout().getScreen().runAction(mItem.getPage().getEngine(), "I_RESUMED", mItem.getItemConfig().resumed, this);
    }

    public Item getItem() {
        return mItem;
    }

    protected Screen getScreen() {
        return getParentItemLayout().getScreen();
    }

    protected void setView(View itemView) {
        Context context = itemView.getContext();
        mOutlineHelper = HolographicOutlineHelper.obtain(context);

        final ItemConfig ic=mItem.getItemConfig();

        BoxLayout bl=new BoxLayout(context, null, ic.hardwareAccelerated);
        //bl.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        bl.setChild(itemView, mItem.getClass()==StopPoint.class ? sStopPointBox : ic.box);
        bl.setUseSelectedContentColor(ic.selectionEffect == ItemConfig.SelectionEffect.PLAIN);

        setFilterChildView(ic.filterTransformed);
        setChild(bl);
        mSensibleView=bl;

        setOnInterceptorListener(this);
        mSensibleView.setOnKeyListener(this);
        mSensibleView.setFocusable(true);
        mSensibleView.setOnFocusChangeListener(this);

        evaluateEnabledState();

        setTag(mItem); // TODO remove the use of the tag to link with the item

        mAlpha = mItem.getAlpha();
        updateViewAlpha();

        if(Build.VERSION.SDK_INT >= 21 && ic.selectionEffect == ItemConfig.SelectionEffect.MATERIAL) {
            ColorStateList colors = new ColorStateList(RIPPLE_STATES_LIST, new int[]{ic.box.ccs});
            RippleDrawable background = new RippleDrawable(colors, null, ic.selectionEffectMask ? new ColorDrawable(-1) : null);
            setBackgroundDrawable(background);
        }

        setHighlightedNow(mHighlighted);

        setDispatchEventsToChild(mDispatchEventsToChild);
    }

    /**
     * Only available when isInitDone() returns true.
     * @return
     */
    public BoxLayout getSensibleView() {
        return mSensibleView;
    }

    public void setItemLayout(ItemLayout itemLayout) {
        mItemLayout = itemLayout;
    }

    public ItemLayout getParentItemLayout() {
        return mItemLayout;//(ItemLayout)getParent();
    }

    public void evaluateEnabledState() {
        ItemLayout il = getParentItemLayout();
        if(il != null) {
            boolean enabled;
            boolean editMode = il.getEditMode();
            if (getClass() == StopPointView.class) {
                enabled = editMode || il.getAlwaysShowStopPoints();
            } else {
                enabled = editMode || mItem.getItemConfig().enabled;
            }
            setEnabled(enabled && il.isEnabled());
        }
    }

    protected void setPressedState(boolean pressed) {
        Drawable background = getBackground();
        if (background != null) {
            background.setState(pressed ? STATE_PRESSED : STATE_UNPRESSED);
        }
    }

    public void setOverrideVisible(boolean override_visible) {
        if(mOverrideVisible != override_visible) {
            mOverrideVisible = override_visible;
            updateViewAlpha();
            updateViewVisibility();
        }
    }

    public void updateViewVisibility() {
        int visibility;

        if(!mItem.isVisible()) {
            visibility = mOverrideVisible ? View.VISIBLE : View.INVISIBLE;
        } else if(mAlwaysPinnedAndVisible) {
            visibility = View.VISIBLE;
        } else {
            visibility = (mItem.isAppDrawerHidden() && mItem.getAppDrawerHiddenHandling() != Item.APP_DRAWER_HIDDEN_ONLY_VISIBLE)
                    || (!mItem.isAppDrawerHidden() && mItem.getAppDrawerHiddenHandling() != Item.APP_DRAWER_HIDDEN_ONLY_HIDDEN) ?
                    View.VISIBLE : View.INVISIBLE;
        }

        setVisibility(visibility);
    }

    public boolean isViewVisible() {
        return getVisibility() == View.VISIBLE;
    }

    public void setAlwaysPinnedAndVisible(boolean alwaysPinnedAndVisible) {
        if(alwaysPinnedAndVisible != mAlwaysPinnedAndVisible) {
            mAlwaysPinnedAndVisible = alwaysPinnedAndVisible;
            updateViewVisibility();
        }
    }

    public boolean isAlwaysPinnedAndVisible() {
        return mAlwaysPinnedAndVisible;
    }

    public void updateViewAlpha() {
        if(isInitDone()) {
            mAlpha = mItem.getAlpha();
            if (mOverrideVisible) {
                if (mAlpha < MINIMUM_ALPHA) {
                    mAlpha = MINIMUM_ALPHA;
                }
            }
            mSensibleView.setAlpha(mAlpha / 255f);
            mSensibleView.invalidate();
        }
    }

    public boolean isHighlighted() {
        return mHighlighted;
    }

    public void setHighlightedLater(boolean highlighted) {
        if(isInitDone()) {
            mDelayedHighlightedState = highlighted;
            mSensibleView.removeCallbacks(mHighlightRunnable);
            mSensibleView.postDelayed(mHighlightRunnable, 80);
        }
    }

    public void setHighlightedNow(boolean selected) {
        if(isInitDone()) {
            mSensibleView.removeCallbacks(mHighlightRunnable);
            mSensibleView.setItemSelected(selected);
        }
        mHighlighted = selected;
    }

    public void setSelected(boolean selected) {
        setSelected(selected, true);
    }

    public void setSelected(boolean selected, boolean sendSelectionChangeEvent) {
        if(selected != mSelected) {
            mSelected = selected;
            if (!mSelected && mDraggedBitmap != null) {
                mDraggedBitmap.recycle();
                mDraggedBitmap = null;
            }
            if(sendSelectionChangeEvent) {
                getScreen().onItemViewSelectionChanged(this, selected);
            }
        }
    }

    public boolean isSelected() {
        return mSelected;
    }

    public void setChecked(boolean checked) {
        mChecked = checked;
        invalidate();
    }

    public boolean isChecked() {
        return mChecked;
    }

    public void setDragged(boolean dragged) {
        mDragged = dragged;
    }

    public void animateTranslate(float dx1, float dx2, float dy1, float dy2, boolean fade) {
        Animation animation;

        Animation translateAnimation = new TranslateAnimation(dx1, dx2, dy1, dy2);

        if(fade) {
            AnimationSet as = new AnimationSet(true);
            as.addAnimation(translateAnimation);
            as.addAnimation(new AlphaAnimation(1, 0));
            animation = as;
        } else {
            animation = translateAnimation;
        }

        animation.setDuration(ANIMATION_TRANSLATE_DURATION);
        animation.setInterpolator(sAccelerateDecelerateInterpolator);
        animation.setFillEnabled(true);
        animation.setFillBefore(true);
        setAnimation(animation);
        animation.startNow();
    }

    public void prepareDraggedBitmap() {
        final ItemConfig ic = mItem.getItemConfig();
        if(ic.selectionEffect != ItemConfig.SelectionEffect.PLAIN) {
            try {
                Class<?> cls = mItem.getClass();
                boolean is_icon_label_view = cls == Shortcut.class || cls == Folder.class;
                View view = is_icon_label_view ? ((ShortcutView)this).getIconLabelView() : getSensibleView();
                Drawable background = view.getBackground();
                int w = view.getWidth();
                int h = view.getHeight();
                if(w == 0 || h == 0) {
                    // nothing to do
                    mDraggedBitmap = null;
                    return;
                }
                mDraggedBitmapScale = 1;
                while(w*h>512*512) {
                    mDraggedBitmapScale *= 2;
                    w /= 2;
                    h /= 2;
                }

                final Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                final Canvas canvas = new Canvas(b);
                float s = 1f / mDraggedBitmapScale;
                int count = canvas.save(Canvas.MATRIX_SAVE_FLAG);
                canvas.scale(s, s);
                if(background != null) background.setAlpha(0); // ripples do not handle setVisible(false, false); (always restart)
                view.draw(canvas);
                if(background != null) background.setAlpha(255);
                canvas.restoreToCount(count);

                new AsyncTask<Void,Void,Boolean>() {
                    @Override
                    protected Boolean doInBackground(Void... voids) {
                        int color = ic.box.ccs;
                        try {
                            mOutlineHelper.applyMediumExpensiveOutlineWithBlur(b, canvas, color, color);
                            return true;
                        } catch(Throwable e) {
                            return false;
                        }
                    }

                    @Override
                    protected void onPostExecute(Boolean ok) {
                        if(ok) {
                            mDraggedBitmap = b;
                        }
                    }
                }.execute((Void)null);
            } catch(Throwable e) {
                e.printStackTrace();
            }
        } else {
            mDraggedBitmap = null;
        }
    }

    public Bitmap getDraggedBitmap() {
        return mDraggedBitmap;
    }

    public int getDraggedBitmapScale() {
        return mDraggedBitmapScale;
    }

    public boolean isDragged() {
        return mDragged;
    }

    public void setFocused(boolean focused) {
        mFocused = focused;
        if(mSensibleView != null) {
            mSensibleView.setItemFocused(focused);
        }
    }

    public void updateViewSize() {
        if(mSensibleView != null) {
            mSensibleView.configureBox();
        }
    }

    public void setOnGrid(boolean on_grid) {
        ItemConfig old_ic = mItem.getItemConfig();
        if(old_ic.onGrid == on_grid) return;

        ItemConfig ic=mItem.modifyItemConfig();
        ic.onGrid = on_grid;
        ItemLayout il = getParentItemLayout();
        float cw = il.getCellWidth();
        float ch = il.getCellHeight();
        HandleView hv = il.getHandleView();
        if(ic.onGrid) {
            RectF bounds = new RectF();
            bounds.set(0, 0, mSensibleView.getWidth(), mSensibleView.getHeight());
            mItem.getTransform().mapRect(bounds);
            Rect mTempRect = new Rect();
            mTempRect.left = Math.round(bounds.left / cw);
            mTempRect.right = Math.round(bounds.right / cw);
            mTempRect.top = Math.round(bounds.top / ch);
            mTempRect.bottom = Math.round(bounds.bottom / ch);
            mItem.getCell().set(mTempRect);
            mItem.notifyCellChanged();
            hv.setMode(mItem.getClass() == StopPoint.class ? HandleView.Mode.NONE : HandleView.Mode.CONTENT_SIZE);
            mItem.setTransform(new Matrix(), false);
            setDragged(false);
        } else {
            Rect cell = mItem.getCell();
            mItem.setViewWidth(Math.round(cell.width() * cw));
            mItem.setViewHeight(Math.round(cell.height() * ch));
            Matrix m = new Matrix();
            m.postTranslate(Math.round(cell.left * cw), Math.round(cell.top * ch));
            mItem.setTransform(m, false);
        }
        mItem.notifyChanged();
    }

    @Override
    public void onInterceptPressed() {
        getScreen().onItemViewPressed(this);
        setPressedState(true);
    }

    private static final int[] STATE_UNPRESSED = new int[]{android.R.attr.state_enabled};
    @Override
    public void onInterceptUnpressed() {
        getScreen().onItemViewUnpressed(this);
        setPressedState(false);
    }

    @Override
    public void onInterceptMove(float dx, float dy) {
        getScreen().onItemViewMove(this, dx, dy);
        setPressedState(false);
    }

    @Override
    public void onInterceptClicked() {
        getScreen().onItemViewClicked(this);
    }

    @Override
    public void onInterceptLongClicked() {
        getScreen().onItemViewLongClicked(this);
    }

    @Override
    public boolean onInterceptTouch(MotionEvent e) {
        return getScreen().onItemViewTouch(this, e);
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if(keyCode==KeyEvent.KEYCODE_DPAD_CENTER || keyCode==KeyEvent.KEYCODE_ENTER) {
            if(event.getAction()==KeyEvent.ACTION_DOWN && !mDpadCenterDown) {
                mDpadCenterDown=true;
                mHasLongDpadCenter=false;
                getScreen().onItemViewPressed(this);
                v.postDelayed(mLongDpadCenterRunnable, ViewConfiguration.getLongPressTimeout());
            } else {
                mDpadCenterDown=false;
                if(!mHasLongDpadCenter) {
                    v.removeCallbacks(mLongDpadCenterRunnable);
                    getScreen().onItemViewClicked(this);
                }
                getScreen().onItemViewUnpressed(this);
            }
            return true;
        }

        return false;
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        setFocused(hasFocus);
        if(hasFocus) {
            ((ItemLayout)getParent()).ensureChildViewVisible(this, true);
        }
    }
}
