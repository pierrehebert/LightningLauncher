package net.pierrox.lightning_launcher.engine;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.WallpaperManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherApps;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.view.Display;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.readystatesoftware.systembartint.SystemBarTintManager;

import net.pierrox.lightning_launcher.LLApp;
import net.pierrox.lightning_launcher.R;
import net.pierrox.lightning_launcher.api.ScreenIdentity;
import net.pierrox.lightning_launcher.configuration.FolderConfig;
import net.pierrox.lightning_launcher.configuration.GlobalConfig;
import net.pierrox.lightning_launcher.configuration.ItemConfig;
import net.pierrox.lightning_launcher.configuration.PageConfig;
import net.pierrox.lightning_launcher.data.Box;
import net.pierrox.lightning_launcher.data.ContainerPath;
import net.pierrox.lightning_launcher.data.EmbeddedFolder;
import net.pierrox.lightning_launcher.data.Error;
import net.pierrox.lightning_launcher.data.EventAction;
import net.pierrox.lightning_launcher.data.Folder;
import net.pierrox.lightning_launcher.data.Item;
import net.pierrox.lightning_launcher.data.LightningIntent;
import net.pierrox.lightning_launcher.data.Page;
import net.pierrox.lightning_launcher.data.Shortcut;
import net.pierrox.lightning_launcher.data.StopPoint;
import net.pierrox.lightning_launcher.data.Unlocker;
import net.pierrox.lightning_launcher.data.Utils;
import net.pierrox.lightning_launcher.data.Widget;
import net.pierrox.lightning_launcher.script.Script;
import net.pierrox.lightning_launcher.script.ScriptExecutor;
import net.pierrox.lightning_launcher.script.ScriptManager;
import net.pierrox.lightning_launcher.script.api.Container;
import net.pierrox.lightning_launcher.script.api.ImageBitmap;
import net.pierrox.lightning_launcher.engine.variable.Variable;
import net.pierrox.lightning_launcher.engine.variable.VariableManager;
import net.pierrox.lightning_launcher.script.api.Lightning;
import net.pierrox.lightning_launcher.views.FolderView;
import net.pierrox.lightning_launcher.views.HandleView;
import net.pierrox.lightning_launcher.views.ItemLayout;
import net.pierrox.lightning_launcher.views.NativeImage;
import net.pierrox.lightning_launcher.views.SystemUIHelper;
import net.pierrox.lightning_launcher.views.WallpaperView;
import net.pierrox.lightning_launcher.views.item.EmbeddedFolderView;
import net.pierrox.lightning_launcher.views.item.ItemView;
import net.pierrox.lightning_launcher.views.item.ShortcutView;

import org.mozilla.javascript.Function;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

public abstract class Screen implements ItemLayout.ItemLayoutListener, ItemView.ItemViewListener, Page.PageListener, FolderView.OnTapOutsideListener {

    public static final int PAGE_DIRECTION_HINT_BACKWARD = -1;
    public static final int PAGE_DIRECTION_HINT_FORWARD = 1;
    public static final int PAGE_DIRECTION_HINT_AUTO = 0;
    public static final int PAGE_DIRECTION_HINT_DONT_MOVE = 2;
    public static final int PAGE_DIRECTION_HINT_NO_ANIMATION = 3;

    private static final String SIS_TARGET_ITEM_LAYOUT="sa";
    private static final String SIS_LAST_TOUCHED_X="sb";
    private static final String SIS_LAST_TOUCHED_Y="sc";
    private static final String SIS_LAST_TOUCHED_ITEM_ID="sd";
    private static final String SIS_LAST_TOUCHED_ITEM_IL="se";

    protected Context mContext;
    private Window mWindow;
    private boolean mHasWindowFocus;
    protected SystemBarTintManager mSystemBarTintManager;

    private ViewGroup mContentView;
    private View mDesktopView;
    private WallpaperView mWallpaperView;

    private ViewGroup mFolderContainer;
    private ArrayList<FolderView> mFolderViews;

    private int mCustomScreenWidth;
    private int mCustomScreenHeight;

    // currently focused container / touch position
    private ItemLayout mTargetItemLayout = null;
    private int mLastTouchedAddX = Utils.POSITION_AUTO;
    private int mLastTouchedAddY = Utils.POSITION_AUTO;
    private int mLastTouchedMenuX = Utils.POSITION_AUTO;
    private int mLastTouchedMenuY = Utils.POSITION_AUTO;
    private ItemView mLastTouchedItemView;

    private boolean mItemSwipeCaught = true;

    private boolean mIsResumed;

    // in degrees, not Configuration.ORIENTATION_{LANDSCAPE/PORTRAIT}
    private int mDisplayOrientation;
    private boolean mIsPortrait;

    private boolean mStatusBarHide;         // as selected per configuration
    private boolean mIsStatusBarHidden;     // current status
    private boolean mForceDisplayStatusBar; // transcient forced visibility

    private int mCurrentOrientation = OrientationEventListener.ORIENTATION_UNKNOWN;
    private OrientationEventListener mOrientationEventListener;

    private ArrayList<ItemLayout> mRootItemLayouts = new ArrayList<>();
    private ItemLayout mCurrentRootItemLayout;

    private ArrayList<ItemLayout> mItemLayouts = new ArrayList<>();

    private ArrayList<Error> mErrors;

    public Screen(Context context, int content_view) {
        LLApp app = LLApp.get();
        app.onScreenCreated(this);

        mContext = context;
        mFolderViews=new ArrayList<>();

        if(content_view != 0) {
            mContentView = (ViewGroup) LayoutInflater.from(context).inflate(content_view, null);
            mDesktopView = mContentView.findViewById(R.id.desktop);

            mWallpaperView = (WallpaperView) mContentView.findViewById(R.id.wp);
            mFolderContainer = (ViewGroup) mContentView.findViewById(R.id.folder_container);
        } else {
            mFolderContainer = new FrameLayout(context);
        }

        mDisplayOrientation = getDegreesFromCurrentConfiguration();

        // TODO in theory this should be done for all distinct engines, having pages coming from several
        // engines in a single screen is possible
        app.getAppEngine().registerPageListener(this);

        if(mContentView != null) {
            mOrientationEventListener = new OrientationEventListener(mContext, SensorManager.SENSOR_DELAY_NORMAL) {
                @Override
                public void onOrientationChanged(int i) {
                    if(i != ORIENTATION_UNKNOWN) {
                        if(i > 360-45) i-=360;
                        i = ((i+45)/90)*90;

                        int rotation = mWindow == null ? Surface.ROTATION_0 : mWindow.getWindowManager().getDefaultDisplay().getRotation();
                        switch (rotation) {
                            case Surface.ROTATION_90: i+=90; break;
                            case Surface.ROTATION_180: i+=180; break;
                            case Surface.ROTATION_270: i+=270; break;
                        }

                        if(mCurrentOrientation != i) {
                            mCurrentOrientation = i;
                            mContentView.removeCallbacks(mApplyOrientationChangedRunnable);
                            mContentView.postDelayed(mApplyOrientationChangedRunnable, 800);
                        }
                    }
                }
            };

            mContentView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View view) {
                    // select API 28 because this is linked with the display cutout stuff
                    if (Build.VERSION.SDK_INT >= 28) {
                        WindowInsets insets = mContentView.getRootWindowInsets();
                        if (insets != null) {
                            mSystemBarTintManager.onConfigurationChanged(mWindow);
                            onSystemBarsSizeChanged();
                        }
                    }
                }
                @Override
                public void onViewDetachedFromWindow(View view) {

                }
            });
        }
    }

    public void destroy() {
        int last;
        while((last = mItemLayouts.size()) > 0) {
            ItemLayout il = mItemLayouts.remove(last - 1);
            releaseItemLayout(il);
        }

        LLApp app = LLApp.get();
        app.getAppEngine().unregisterPageListener(this);
        app.onScreenDestroyed(this);
    }

    public void pause() {
        if(mIsResumed) {
            mIsResumed = false;

            if(mOrientationEventListener != null) {
                mOrientationEventListener.disable();
            }

            ItemLayout il = getCurrentRootItemLayout();
            if(il != null) {
                il.pause();
            }

            if (mFolderViews != null) {
                for (FolderView fv : mFolderViews) {
                    if (fv.isOpen()) {
                        fv.pause();
                    }
                }
            }

            LLApp.get().onScreenPaused(this);
        }
    }

    public void resume() {
        if(!mIsResumed) {
            mIsResumed = true;

            // clear errors, they will be accumulated in an array, should they appear during resume
            mErrors = null;

            LLApp.get().onScreenResumed(this);

            updateOrientationOrRotation();

            ItemLayout il = getCurrentRootItemLayout();
            if(il != null) {
                il.resume();
            }

            if (mFolderViews != null) {
                for (FolderView fv : mFolderViews) {
                    if (fv.isOpen()) {
                        fv.resume();
                    }
                }
            }

            if(mOrientationEventListener != null) {
                mOrientationEventListener.enable();
            }

            if(mErrors != null) {
                // gather permission errors in a single set in order to bubble a single event
                ArrayList<Error> permission_errors = new ArrayList<>();
                for (Error e : mErrors) {
                    if (e.isPermission()) {
                        if(!permission_errors.contains(e)) {
                            permission_errors.add(e);
                        }
                    } else {
                        onUnhandledError(e);
                    }
                }
                int n = permission_errors.size();
                if (n > 0) {
                    String[] p = new String[n];
                    int[] m = new int[n];
                    n = 0;
                    for (Error e : permission_errors) {
                        p[n] = e.getPermission();
                        m[n] = e.getMsg();
                        n++;
                    }
                    onMissingPermissions(p, m);
                }
            }
        }
    }

    public boolean isPaused() {
        return !mIsResumed;
    }

    public abstract ScreenIdentity getIdentity();

    public Context getContext() {
        return mContext;
    }

    public void setWindow(Window window) {
        mWindow = window;
        if(Build.VERSION.SDK_INT>=19) {
            mSystemBarTintManager = new SystemBarTintManager(mWindow);
        }
    }

    public void setVisibility(boolean visible) {
        // pass
    }

    public void setHasWindowFocus(boolean hasWindowFocus) {
        if(mHasWindowFocus!=hasWindowFocus && hasWindowFocus) {
            SystemUIHelper.setStatusBarVisibility(mWindow, !mIsStatusBarHidden, mForceDisplayStatusBar);
        }
        mHasWindowFocus = hasWindowFocus;
    }

    protected abstract Resources getRealResources();

    /**
     * What to do when a widget has been clicked
     */
    public void onWidgetClicked() {

    }

    /**
     * Hook for the script executor
     * TODO document this, should be balanced with...
     * @param scriptExecutor
     */
    public void displayScriptPickImageDialog(ScriptExecutor scriptExecutor) {
        Toast.makeText(mContext, "Unable to pick an image in this context.", Toast.LENGTH_SHORT).show();
    }

    /**
     * Hook for the script executor
     */
    public void displayCropPickImageDialog(ScriptExecutor scriptExecutor, ImageBitmap image, boolean full_size) {
        Toast.makeText(mContext, "Unable to crop an image in this context.", Toast.LENGTH_SHORT).show();
    }

    /**
     * Hook for the script executor
     */
    public boolean startActivityForResultScript(ScriptExecutor scriptExecutor, Intent intent, int receiver_script_id, String token) {
        Toast.makeText(mContext, "Unable to start an activity for result in this context.", Toast.LENGTH_SHORT).show();
        return false;
    }

    /**
     * What to do on activity start error
     */
    public void onShortcutLaunchError(Shortcut shortcut) {
        Toast.makeText(mContext, "Unable to launch this item: "+shortcut.toString(), Toast.LENGTH_SHORT).show();
    }

    /**
     * No LauncherApps host permission (Android 7.1)
     */
    public void onLauncherAppsNoHostPermission(ItemView itemView) {
        Toast.makeText(mContext, R.string.as_no_hp_t, Toast.LENGTH_SHORT).show();
    }

    /**
     * App shortcut not found (Android 7.1)
     */
    public void onLauncherAppsShortcutNotFound(ItemView itemView, String msg) {
        if(msg == null) {
            msg = mContext.getString(R.string.as_nf);
        }
        Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
    }

    /**
     * Missing permission detected
     */
    protected void onMissingPermissions(String[] permissions, int[] msgs) {
        String msg = getContext().getString(R.string.pr_f);
        for (int m : msgs) {
            msg += "\n - "+getContext().getString(m);
        }
        Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
    }

    /**
     * Fallback for future errors that have not specific handling
     */
    public void onUnhandledError(Error error) {
        Toast.makeText(mContext, error.toString(), Toast.LENGTH_SHORT).show();
    }

    /**
     * Set the active ('focused') page
     */
    // FIXME: dashboard specific
    public void setActivePage(int page) {

    }

    // FIXME: dashboard specific
    public void setActivePageWithTopmostPage() {
        ItemLayout itemLayout = getTopmostItemLayout();
        if(itemLayout != null) {
            Page page = itemLayout.getPage();
            setActivePage(page.id);
        }
    }

    public void showAddItemDialog(ItemLayout il) {
        Toast.makeText(mContext, "Unable to add an item in this context.", Toast.LENGTH_SHORT).show();
    }

    public ViewGroup getContentView() {
        return mContentView;
    }

    public View getDesktopView() {
        return mDesktopView;
    }

    public SystemBarTintManager getSystemBarTintManager() {
        return mSystemBarTintManager;
    }

    public void saveInstanceState(Bundle to) {
        if(mTargetItemLayout != null) {
            to.putString(SIS_TARGET_ITEM_LAYOUT, new ContainerPath(mTargetItemLayout).toString());
        }
        to.putInt(SIS_LAST_TOUCHED_X, mLastTouchedAddX);
        to.putInt(SIS_LAST_TOUCHED_Y, mLastTouchedAddY);
        if(mLastTouchedItemView != null) {
            to.putInt(SIS_LAST_TOUCHED_ITEM_ID, mLastTouchedItemView.getItem().getId());
            to.putString(SIS_LAST_TOUCHED_ITEM_IL, new ContainerPath(mLastTouchedItemView.getParentItemLayout()).toString());
        }
    }

    public void restoreInstanceState(Bundle from) {
        String path = from.getString(SIS_TARGET_ITEM_LAYOUT);
        if(path != null) {
            mTargetItemLayout = prepareItemLayoutForPath(new ContainerPath(path));
        }

        mLastTouchedAddX = from.getInt(SIS_LAST_TOUCHED_X);
        mLastTouchedAddY = from.getInt(SIS_LAST_TOUCHED_Y);
        int lastTouchedItemId = from.getInt(SIS_LAST_TOUCHED_ITEM_ID, Item.NO_ID);
        if(lastTouchedItemId != Item.NO_ID) {
            path = from.getString(SIS_LAST_TOUCHED_ITEM_IL);
            ItemLayout il = prepareItemLayoutForPath(new ContainerPath(path));
            mLastTouchedItemView = il.getItemView(lastTouchedItemId);
       }
    }



    @Override
    public void onItemViewPressed(ItemView itemView) {
        itemView.setHighlightedLater(true);
        if(itemView.getClass() == EmbeddedFolderView.class) {
            setLastTouchEventForItemLayout(((EmbeddedFolderView)itemView).getEmbeddedItemLayout(), Utils.POSITION_AUTO, Utils.POSITION_AUTO);
        }
        ItemConfig ic= itemView.getItem().getItemConfig();
        if(ic.swipeLeft.action!=GlobalConfig.UNSET || ic.swipeUp.action!=GlobalConfig.UNSET || ic.swipeRight.action!=GlobalConfig.UNSET || ic.swipeDown.action!=GlobalConfig.UNSET) {
            itemView.getParentItemLayout().grabEvent(itemView);
            mItemSwipeCaught=false;
        }
    }

    @Override
    public void onItemViewUnpressed(ItemView itemView) {
        itemView.setHighlightedLater(false);
        itemView.getParentItemLayout().grabEvent(null);
    }

    @Override
    public void onItemViewMove(ItemView itemView, float dx, float dy) {
        if(!mItemSwipeCaught) {
            Item item = itemView.getItem();
            if(!(item instanceof EmbeddedFolder) && !(item instanceof Unlocker)) {
                float abs_dx=Math.abs(dx);
                float abs_dy=Math.abs(dy);
                ItemConfig ic= item.getItemConfig();
                EventAction ea;
                String source;
                if(abs_dx>abs_dy) {
                    if(dx>0) {
                        ea = ic.swipeRight;
                        source = "I_SWIPE_RIGHT";
                    } else {
                        ea = ic.swipeLeft;
                        source = "I_SWIPE_LEFT";
                    }
                } else {
                    if(dy>0) {
                        ea = ic.swipeDown;
                        source = "I_SWIPE_DOWN";
                    } else {
                        ea = ic.swipeUp;
                        source = "I_SWIPE_UP";
                    }
                }
                if(ea.action==GlobalConfig.UNSET) {
                    itemView.getParentItemLayout().grabEvent(null);
                    itemView.setHighlightedNow(false);
                } else {
                    Rect bounds = computeItemViewBounds(itemView);
                    setLastTouchEventForItemView(itemView, Utils.POSITION_AUTO, Utils.POSITION_AUTO, bounds.centerX(), bounds.centerY());
                    LightningEngine engine = item.getPage().getEngine();
                    runAction(engine, source, ea, itemView);
                }
                mItemSwipeCaught=true;
            }
        }
    }

    @Override
    public void onItemViewClicked(ItemView itemView) {
        Item item = itemView.getItem();
        Rect bounds = computeItemViewBounds(itemView);
        setLastTouchEventForItemView(itemView, Utils.POSITION_AUTO, Utils.POSITION_AUTO, bounds.centerX(), bounds.centerY());
        ItemConfig ic = item.getItemConfig();
        boolean ic_tap_unset = ic.tap.action == GlobalConfig.UNSET;
        boolean is_widget = item.getClass() == Widget.class;
        if(is_widget && ic_tap_unset) {
            // normal click on a widget (not overloaded): do nothing, let the widget handle it, not the launcher
            onWidgetClicked();
        } else {
            LightningEngine engine = item.getPage().getEngine();
            EventAction ea = ic_tap_unset ? engine.getGlobalConfig().itemTap : ic.tap;
            runAction(engine, "I_CLICK", ea, itemView);
        }
    }

    @Override
    public void onItemViewLongClicked(ItemView itemView) {
        itemView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);

        // don't handle long click for embedded folders
        Item item = itemView.getItem();
        if(item.getClass() != EmbeddedFolder.class) {
            Rect bounds = computeItemViewBounds(itemView);
            setLastTouchEventForItemView(itemView, Utils.POSITION_AUTO, Utils.POSITION_AUTO, bounds.centerX(), bounds.centerY());
            itemView.setHighlightedNow(false);
            ItemConfig ic = item.getItemConfig();
            LightningEngine engine = item.getPage().getEngine();
            EventAction ea = ic.longTap.action == GlobalConfig.UNSET ? engine.getGlobalConfig().itemLongTap : ic.longTap;
            runAction(engine, "I_LONG_CLICK", ea, itemView);
        }
    }

    @Override
    public void onItemViewAction(ItemView itemView, int action) {

    }

    @Override
    public boolean onItemViewTouch(ItemView itemView, MotionEvent event) {
        Item item = itemView.getItem();
        ItemConfig ic = item.getItemConfig();
        EventAction ea = ic.touch;
        if(ea.action == GlobalConfig.UNSET) {
            return false;
        } else {
            final String source = "I_TOUCH";
            if(ea.action == GlobalConfig.RUN_SCRIPT) {
                try {
                    Pair<Integer,String> id_data = Script.decodeIdAndData(ea.data);
                    item.getPage().getEngine().getScriptExecutor().runScriptTouchEvent(this, id_data.first, itemView, event);
                } catch(NumberFormatException e) {
                    // pass
                }
            } else {
                runAction(item.getPage().getEngine(), source, ea, itemView);
            }
            return true;
        }
    }

    @Override
    public void onItemViewSelectionChanged(ItemView itemView, boolean selected) {
        itemView.getParentItemLayout().onItemSelectionChanged(itemView.getItem(), selected);
    }

    /***************************************** 'FOCUSED' CONTAINER / TOUCH POSITION ***********************************/

    private void setLastTouchedItemAndPosition(ItemView itemView, ItemLayout targetItemLayout, int add_x, int add_y, int menu_x, int menu_y) {
        mLastTouchedItemView = itemView;
        setTargetItemLayout(targetItemLayout);
        mLastTouchedAddX = add_x;
        mLastTouchedAddY =  add_y;
        mLastTouchedMenuX = menu_x;
        mLastTouchedMenuY =  menu_y;
        if(targetItemLayout != null) {
            setActivePage(targetItemLayout.getPage().id);
        }
    }

    public void setLastTouchEventForItemView(ItemView itemView, int add_x, int add_y, int menu_x, int menu_y) {
        setLastTouchedItemAndPosition(itemView, itemView.getParentItemLayout(), add_x, add_y, menu_x, menu_y);
    }

    protected void setLastTouchEventForItemLayout(ItemLayout item_layout, int x, int y) {
        int[] coords= new int[2];
        if(x != Integer.MIN_VALUE && y != Integer.MIN_VALUE) {
            float[] values = translateItemLayoutCoordsIntoScreenCoords(item_layout, x, y);
            coords[0] = (int)values[0];
            coords[1] = (int)values[1];
        } else {
            coords[0] = Utils.POSITION_AUTO;
            coords[1] = Utils.POSITION_AUTO;
        }
        setLastTouchedItemAndPosition(null, item_layout, x, y, coords[0], coords[1]);
    }

    public void setLastTouchEventForMenuBottom(boolean use_main_page) {
        ItemLayout il = use_main_page ? getCurrentRootItemLayout() : getTopmostItemLayout();
        setLastTouchedItemAndPosition(null, il, Utils.POSITION_AUTO, Utils.POSITION_AUTO, Utils.POSITION_AUTO, Utils.POSITION_AUTO);
    }

    public void setTargetItemLayout(ItemLayout targetItemLayout) {
        mTargetItemLayout = targetItemLayout;
    }

    public ItemLayout getTargetItemLayout() {
        return mTargetItemLayout;
    }

//    public Page getTargetPage() {
//        return mTargetPage;
//    }

    // TODO rename as getLastTouchedPositionX
    public int getLastTouchedAddX() {
        return mLastTouchedAddX;
    }

    public int getLastTouchedAddY() {
        return mLastTouchedAddY;
    }

    public int getLastTouchedMenuX() {
        return mLastTouchedMenuX;
    }

    public int getLastTouchedMenuY() {
        return mLastTouchedMenuY;
    }

    public ItemView getLastTouchedItemView() {
        return mLastTouchedItemView;
    }

    /***************************************** FOLDER MANAGEMENT ***********************************/
    public ItemView[] getOpenFolders() {
        if(mFolderContainer == null) {
            return new ItemView[0];
        }

        int count = 0;
        for(FolderView fv : mFolderViews) {
            if(fv.isOpen()) count++;
        }

        ItemView[] folders = new ItemView[count];
        for(FolderView fv : mFolderViews) {
            if(fv.isOpen()) {
                folders[--count] = fv.getOpenerItemView();
            }
        }

        return folders;
    }

    public ArrayList<FolderView> getFolderViews() {
        return mFolderViews;
    }

    /**
     * Find a folder view given its opener item view
     * @param opener the item view used to open the folder
     * @param open true to look for open folder, false to look for closed folder, null to look for both
     * @return
     */
    public FolderView findFolderView(ItemView opener, Boolean open) {
        if(mFolderContainer == null) {
            return null;
        }

        for(FolderView fv : mFolderViews) {
            if((open==null || fv.isOpen()==open) && fv.getOpenerItemView() == opener) {
                return fv;
            }
        }

        return null;
    }
    public FolderView findFolderView(ItemLayout il, boolean open) {
        if(mFolderContainer == null) {
            return null;
        }

        for(FolderView fv : mFolderViews) {
            if(fv.isOpen()==open && fv.getItemLayout() == il) {
                return fv;
            }
        }

        return null;
    }

    public FolderView[] findFolderViews(int page, Boolean open) {
        if(mFolderContainer == null) {
            return new FolderView[0];
        }

        // loop two times: first to count folder views, second to build the array
        int count = 0;
        FolderView[] fvs = null;
        for(int i=0; i<2; i++) {
            for (FolderView fv : mFolderViews) {
                if ((open == null || fv.isOpen() == open) && fv.getPage().id == page) {
                    if(i==1) fvs[count] = fv;
                    count++;
                }
            }
            if(i==0) {
                fvs = new FolderView[count];
                count = 0;
            }
        }

        return fvs;
    }

    public FolderView findTopmostFolderView() {
        if(mFolderContainer == null) {
            return null;
        }

        for(int i=mFolderContainer.getChildCount()-1; i>=0; i--) {
            View v=mFolderContainer.getChildAt(i);
            if(v instanceof FolderView) {
                FolderView fv=(FolderView)v;
                if(fv.isOpen()) {
                    return fv;
                }
            }
        }

        return null;
    }

    public FolderView openFolder(ItemView folderItemView) {
        return openFolder(folderItemView, folderItemView);
    }

    private FolderView openFolder(ItemView folderItemView, ItemView withBounds) {
        Rect sourceBounds = computeItemViewBounds(withBounds);
        Point from = new Point(sourceBounds.centerX(), sourceBounds.centerY());
        return openFolder((Folder) folderItemView.getItem(), folderItemView, from, false);
    }

    /**
     * Open a folder with an optional item view as opener
     * @param folder mandatory folder to open
     * @param folderItemView optional item view used to open the folder
     * @param from optional starting point for the animation
     * @param prepareOnly make everything to prepare the folder, but don't actually open it
     * @return
     */
    public FolderView openFolder(Folder folder, ItemView folderItemView, Point from, boolean prepareOnly) {
        if(mFolderContainer == null) {
            return null;
        }

        final LightningEngine engine = folder.getPage().getEngine();

        int p=folder.getFolderPageId();
        FolderView fv;
        if(folderItemView == null) {
            FolderView[] fvs = findFolderViews(p, true);
            if(fvs.length > 0) {
                fv = fvs[0];
            } else {
                fv = null;
            }
        } else {
            fv = findFolderView(folderItemView, true);
        }
        if(fv != null) {
            if(!prepareOnly) {
                closeFolder(fv, true);
            }
        } else {
            if(getCurrentRootPage().config.defaultFolderConfig.closeOther && p!= Page.USER_MENU_PAGE) {
                closeAllFolders(true);
            }
            if(folderItemView == null) {
                FolderView[] fvs = findFolderViews(p, false);
                if(fvs.length > 0) {
                    fv = fvs[0];
                } else {
                    fv = null;
                }
            } else {
                fv = findFolderView(folderItemView, false);
            }
            boolean newFolderView = fv==null;
            if(newFolderView) {
                fv=new FolderView(mContext);
                takeItemLayoutOwnership(fv.getItemLayout());
                fv.setOpenerItemView(folderItemView);
                final FolderView finalFv = fv;
                fv.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        Rect rect = new Rect();
                        v.getHitRect(rect);
                        setLastTouchedItemAndPosition(null, finalFv.getItemLayout(), Utils.POSITION_AUTO, Utils.POSITION_AUTO, rect.centerX(), rect.centerY());
                        EventAction ea = finalFv.getPage().config.bgLongTap;
                        return runAction(engine, "C_LONG_CLICK", ea.action==GlobalConfig.UNSET ? engine.getGlobalConfig().bgLongTap : ea);
                    }
                });
                fv.setOnTapOutsideListener(this);
                mFolderContainer.addView(fv);
                mFolderViews.add(fv);
            }
            Page page = engine.getPageManager().getOrLoadPage(p);
            for(Item i : page.items) {
                i.setCellT(null);
            }
            fv.configure(folder, folderItemView, page);
            if(newFolderView) {
                page.notifyLoaded(fv.getItemLayout());
            }
            View root_view = getCurrentRootItemLayout();
            fv.getItemLayout().setDesktopSize(root_view.getWidth(), root_view.getHeight());
            if(!prepareOnly) {
                if (from == null) {
                    if(folderItemView == null) {
                        int x = mFolderContainer.getWidth() / 2;
                        int y = mFolderContainer.getHeight() / 2;
                        from = new Point(x, y);
                    } else {
                        Rect sourceBounds = computeItemViewBounds(folderItemView);
                        from = new Point(sourceBounds.centerX(), sourceBounds.centerY());
                    }
                }
                fv.open(from, mIsResumed);

                mFolderContainer.bringChildToFront(fv);
            }
        }
        if(!prepareOnly) {
            setActivePageWithTopmostPage();
        }
        return fv;
    }

    // a special kind of folder
    protected FolderView openUserMenu(boolean prepareOnly) {
        return null;
    }

    public void closeAllFolders(boolean animate) {
        if(mFolderContainer == null) {
            return;
        }

        for(FolderView fv : mFolderViews) {
            closeFolder(fv, animate);
        }
        setActivePageWithTopmostPage();
    }

    public void closeFolder(FolderView fv, boolean animate) {
        if(mFolderContainer == null) {
            return;
        }

        if(fv!=null && fv.isOpen()) {
            onFolderClosed(fv);
            fv.close(animate, mIsResumed);
            setActivePageWithTopmostPage();
            setTargetItemLayout(getTopmostItemLayout());
        }
    }

    public void closeFolder(ItemView folderItemView) {
        if(mFolderContainer == null) {
            return;
        }

        for(FolderView fv : mFolderViews) {
            if(fv.isOpen() && fv.getOpenerItemView() == folderItemView) {
                onFolderClosed(fv);
                fv.close(true, mIsResumed);
            }
        }
    }

    protected void onFolderClosed(FolderView fv) {

    }

    public boolean isFolderOpened(ItemView folderItemView) {
        return findFolderView(folderItemView, true) != null;
    }

    public ItemLayout setFolderEditMode(ItemLayout il, boolean edit_mode) {
        FolderView fv=findFolderView(il, true);
        if(fv==null) {
            // the folder has been closed for instance because the activity has been recreated (select shortcut, widget for add) and the folder has not been re-opened
            // or because back is pressed while long tapping on an item in a folder
            // or because editing a panel fullscreen using a FolderView
            // so re-open the folder
            if(il.getPage().id == Page.USER_MENU_PAGE) {
                fv = openUserMenu(false);
            }else {
                ItemView openerView = il.getOpenerItemView();
                Folder opener = (Folder) openerView.getItem();
                float panel_x = 0;
                float panel_y = 0;
                float panel_scale = 1;
                if (opener.getClass() == EmbeddedFolder.class) {
                    il.recenter();
                    panel_x = il.getCurrentX();
                    panel_y = il.getCurrentY();
                    panel_scale = il.getCurrentScale();
                    FolderConfig config = opener.modifyFolderConfig();
                    config.animationIn = FolderConfig.FolderAnimation.NONE;
                    config.animationOut = FolderConfig.FolderAnimation.NONE;
                    config.animFade = false;
                    config.autoFindOrigin = false;
                    config.box = new Box();
                    config.titleVisibility = false;
                }
                fv = openFolder(openerView);
                if (opener.getClass() == EmbeddedFolder.class) {
                    Box box = opener.getItemConfig().box;
                    ItemLayout parent_il = openerView.getParentItemLayout();
                    float scale = parent_il.getCurrentScale();
                    int[] s = box.size;
                    il.setForceUseDesktopSize(true);
                    il.setDesktopSize(
                            opener.getViewWidth() - s[Box.ML] - s[Box.BL] - s[Box.PL] - s[Box.MR] - s[Box.BR] - s[Box.PR],
                            opener.getViewHeight() - s[Box.MT] - s[Box.BT] - s[Box.PT] - s[Box.MB] - s[Box.BB] - s[Box.PB]
                    );
                    Rect r = computeItemViewBounds(openerView);
                    if (!getCurrentRootPage().config.statusBarHide && Build.VERSION.SDK_INT >= 11) {
                        // hackish and duplicate with Dashboard.leaveEditMode
                        Resources res = mContext.getResources();
                        int statusBarHeight = 0;
                        int resourceId = res.getIdentifier("status_bar_height", "dimen", "android");
                        if (resourceId > 0) {
                            statusBarHeight = res.getDimensionPixelSize(resourceId);
                        }
                        r.top -= statusBarHeight;
                        r.bottom -= statusBarHeight;
                    }
                    int dx = s[Box.ML] + s[Box.BL] + s[Box.PL];
                    int dy = s[Box.MT] + s[Box.BT] + s[Box.PT];

                    il.setAutoFindOrigin(false);
                    il.moveTo(r.left + (dx + panel_x) * scale, r.top + (dy + panel_y) * scale, scale * panel_scale);

                    il.setVirtualEditBorders(box, r, scale);
                }
            }
        }
        // translate the current transformation according to the item layout position on screen so that we can match the display
        int[] location_r=new int[2];
        mFolderContainer.getLocationInWindow(location_r);
        fv.setEditMode(edit_mode, location_r);

        return fv.getItemLayout();
    }

    public void removeFolders(Page page) {
        if(mFolderContainer != null) {
            FolderView[] fvs = findFolderViews(page.id, null);
            for (FolderView fv : fvs) {
                removeFolderView(fv);
            }
        }
    }

    public void removeAllFolders() {
        for (int i = mFolderViews.size() -1; i>=0; i--) {
            removeFolderView(mFolderViews.get(i));
        }
    }

    private void removeFolderView(FolderView fv) {
        if(fv.isOpen()) closeFolder(fv, false);
        releaseItemLayout(fv.getItemLayout());
        mFolderViews.remove(fv);
        mFolderContainer.removeView(fv);
        fv.destroy();
    }

    private void updateEmptyMessageVisibility(Page page) {
        FolderView[] fvs = findFolderViews(page.id, null);
        for (FolderView fv : fvs) {
            fv.updateEmptyMessageVisibility();
        }
    }

    @Override
    public void onTapOutside(FolderView fv) {
        closeFolder(fv, true);
    }

    /***************************************** GEOMETRY ***********************************/

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public Rect computeItemViewBounds(ItemView itemView) {
        ItemLayout il = itemView.getParentItemLayout();
        RectF bounds=new RectF();
        Utils.getItemViewBoundsInItemLayout(itemView, bounds);
        Matrix t = il.getTransformForItemView(itemView);
        if(t != null) t.mapRect(bounds);

        traverseViews((View)il.getParent(), bounds);
        if(il.getEditMode() && Build.VERSION.SDK_INT>=11) {
            // pivot is always 0x0
            float sx = mDesktopView.getScaleX();
            float sy = mDesktopView.getScaleY();
            bounds.left *= sx;
            bounds.right *= sx;
            bounds.top *= sy;
            bounds.bottom *= sy;
        }
        Rect rounded_bounds = new Rect();
        bounds.round(rounded_bounds);
        return rounded_bounds;
    }


    private void traverseViews(View v, RectF r) {
        if(v == null) {
            return;
        }
        if(v instanceof ItemView) {
            ItemView itemView = (ItemView) v;
            Item i = itemView.getItem();
            ItemLayout il = itemView.getParentItemLayout();
            if(i.getItemConfig().onGrid) {
                float cw = il.getCellWidth();
                float ch = il.getCellHeight();
                Rect cell = i.getCell();
                r.offset(cell.left * cw, cell.top * ch);
            } else {
                i.getTransform().mapRect(r);
            }
            Matrix t = il.getTransformForItemView(itemView);
            if(t != null) t.mapRect(r);
        } else {
            r.offset(v.getLeft(), v.getTop());
        }

        ViewParent vp = v.getParent();
        if(vp instanceof View) {
            traverseViews((View)vp, r);
        }
    }


    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public float[] translateItemLayoutCoordsIntoScreenCoords(ItemLayout il, float x, float y) {
        RectF r = new RectF(x, y, x, y);
        il.getTransformForRect(r).mapRect(r);
        traverseViews((View)(il.getParent()), r);
        float[] coords = new float[] {r.left, r.top};
        if(il.getEditMode() && Build.VERSION.SDK_INT>=11) {
            // pivot is always 0x0
            coords[0] *= mDesktopView.getScaleX();
            coords[1] *= mDesktopView.getScaleY();
        }
        return coords;
    }

    /***************************************** NAVIGATION ***********************************/
    public ItemLayout getCurrentRootItemLayout() {
        return mCurrentRootItemLayout;
    }

    /**
     * Return the currently displayed desktop (the root page)
     */
    public Page getCurrentRootPage() {
        ItemLayout itemLayout = getCurrentRootItemLayout();
        return itemLayout==null ? null : itemLayout.getPage();
    }

    /**
     * Change the current desktop (if possible)
     */
    public ItemLayout loadRootItemLayout(int pageId, boolean reset_navigation_history, boolean displayImmediately, boolean animate) {
        return loadRootItemLayoutOffscreen(pageId, reset_navigation_history, displayImmediately, animate);
    }

    protected ItemLayout loadRootItemLayoutOffscreen(int pageId, boolean reset_navigation_history, boolean displayImmediately, boolean animate) {
        ItemLayout itemLayout = null;
        for (ItemLayout il : mRootItemLayouts) {
            if(il.getPage().id == pageId) {
                itemLayout = il;
                break;
            }
        }

        if(itemLayout == null) {
            itemLayout = new ItemLayout(getContext(), null);
            mRootItemLayouts.add(itemLayout);
            takeItemLayoutOwnership(itemLayout);
            Page page = LLApp.get().getAppEngine().getOrLoadPage(pageId);
            itemLayout.setPage(page);
            page.notifyLoaded(itemLayout);
        }

        if(displayImmediately || mRootItemLayouts.size() == 1) {
            mCurrentRootItemLayout = itemLayout;
        }

        return itemLayout;
    }


    public void executeGoToDesktopPositionIntent(Intent intent) {
        int page = intent.getIntExtra(LightningIntent.INTENT_EXTRA_DESKTOP, Page.FIRST_DASHBOARD_PAGE);
        if(Page.isDashboard(page) && page != getCurrentRootPage().id) {
            loadRootItemLayout(page, false, true, true);
        }
        if(intent.hasExtra(LightningIntent.INTENT_EXTRA_X)) {
            float x = intent.getFloatExtra(LightningIntent.INTENT_EXTRA_X, 0);
            float y = intent.getFloatExtra(LightningIntent.INTENT_EXTRA_Y, 0);
            float s = intent.getFloatExtra(LightningIntent.INTENT_EXTRA_SCALE, 1);
            boolean absolute = intent.getBooleanExtra(LightningIntent.INTENT_EXTRA_ABSOLUTE, true);
            boolean animate = intent.getBooleanExtra(LightningIntent.INTENT_EXTRA_ANIMATE, true);

            goToDesktopPosition(page, x, y, s, animate, absolute);
        }
    }

    public void goToDesktopPosition(int page, float x, float y, float s, boolean animate, boolean absolute) {
        if(Page.isDashboard(page) && page != getCurrentRootPage().id) {
            ItemLayout il = loadRootItemLayout(page, false, true, animate);
            goToItemLayoutPosition(il, x, y, s, animate, absolute);
        } else {
            ItemLayout[] itemLayouts = getItemLayoutsForPage(page);
            for (ItemLayout il : itemLayouts) {
                goToItemLayoutPosition(il, x, y, s, animate, absolute);
            }
        }
    }

    public void goToItemLayoutPosition(ItemLayout il, float x, float y, float s, boolean animate, boolean absolute) {
        Page page = il.getPage();
        if(page.isDashboard() && page != getCurrentRootPage()) {
            loadRootItemLayout(page.id, false, true, true);
        }
        if(absolute) {
            if (animate) {
                il.animateZoomTo(x, y, s);
            } else {
                il.moveTo(x, y, s);
            }
        } else {
            il.goToPage(x, y, s, animate);
        }
    }

    public void zoomInOrOut(ItemLayout il) {
        if(il.getCurrentScale()==1) {
            zoomFullScale(il);
        } else {
            zoomToOrigin(il);
        }
    }

    public boolean zoomToOrigin(ItemLayout il) {
        if(il.getCurrentX()!=0 || il.getCurrentY()!=0 || il.getCurrentScale()!=1) {
            il.animateZoomTo(ItemLayout.POSITION_ORIGIN, 0);
            return true;
        } else {
            return false;
        }
    }

    protected void zoomFullScale(ItemLayout il) {
        il.animateZoomTo(ItemLayout.POSITION_FULL_SCALE, 0);
    }

    public void ensureItemLayoutVisible(ItemLayout il, boolean honour_scrolling_direction) {
        ItemView opener = il.getOpenerItemView();
        if(opener == null) {
            // this is (should be) a desktop
            loadRootItemLayout(il.getPage().id, false, true, true);
        } else {
            if(opener.getClass() == EmbeddedFolderView.class) {
                // this is a panel, make sure it is displayed in its container
                ItemLayout parentItemLayout = opener.getParentItemLayout();
                ensureItemLayoutVisible(parentItemLayout, honour_scrolling_direction);
                parentItemLayout.ensureChildViewVisible(opener, honour_scrolling_direction);
            } else {
                // this is a folder, open it and stop here
                FolderView folderView = findFolderView(opener, null);
                if(!folderView.isOpen()) {
                    openFolder(opener);
                }
            }
        }
    }

    public void ensureItemViewVisible(ItemView iv, boolean honour_scrolling_direction) {
        ItemLayout il = iv.getParentItemLayout();
        ensureItemLayoutVisible(il, honour_scrolling_direction);
        il.ensureChildViewVisible(iv, honour_scrolling_direction);
    }

    /***************************************** ITEM LAYOUT AND ITEM VIEWS MANAGEMENT ***********************************/
    public void takeItemLayoutOwnership(ItemLayout itemLayout) {
        mItemLayouts.add(itemLayout);
        itemLayout.setScreen(this);
    }

    public int getPageUseCount(int pageId) {
        int count = 0;
        for (ItemLayout il : mItemLayouts) {
            if(il.getPage().id == pageId) {
                count++;
            }
        }
        return count;
    }

    /**
     * Retrieve already loaded ItemLayout displaying a given page.
     */
    public ItemLayout[] getItemLayoutsForPage(int pageId) {
        int count = 0;
        ItemLayout[] ils = null;
        for(int i=0; i<2; i++) {
            for (ItemLayout il : mItemLayouts) {
                if (il.getPage().id == pageId) {
                    if(i==1) ils[count] = il;
                    count++;
                }
            }
            if(i==0) {
                ils = new ItemLayout[count];
                count = 0;
            }
        }

        return ils;
    }

    /**
     * Prepare all item layouts to display a given page, using the first path found
     * @param page
     * @return the item layout prepared for the page
     */
    public ItemLayout prepareFirstItemLayout(int page) {
        ItemLayout[] ils = getItemLayoutsForPage(page);
        if(ils.length > 0) {
            return ils[0];
        }
        ItemLayout itemLayout;

        if(Page.isDashboard(page) || page==Page.APP_DRAWER_PAGE) {
            itemLayout = loadRootItemLayout(page, false, false, true);
        } else {
            net.pierrox.lightning_launcher.data.Folder opener = LLApp.get().getAppEngine().findFirstFolderPageOpener(page);
            if (opener != null) {
                ItemLayout parentItemLayout = prepareFirstItemLayout(opener.getPage().id);
                if(parentItemLayout == null) {
                    return null;
                }
                ItemView openerItemView = parentItemLayout.getItemView(opener);
                if (openerItemView instanceof EmbeddedFolderView) {
                    itemLayout = ((EmbeddedFolderView) openerItemView).getEmbeddedItemLayout();
                } else {
                    FolderView folderView = openFolder(opener, openerItemView, null, true);
                    itemLayout = folderView.getItemLayout();
                }
            } else {
                return null;
            }
        }
        return itemLayout;
    }

    /**
     * Prepare all item layouts leading to a given page for a given path.
     */
    public ItemLayout prepareItemLayoutForPath(ContainerPath path) {
        ContainerPath parent = path.getParent();
        int last = path.getLast();
        if(parent == null) {
            if(last == Page.USER_MENU_PAGE) {
                FolderView folderView = openUserMenu(true);
                return folderView==null ? null : folderView.getItemLayout();
            } else {
                return loadRootItemLayout(last, false, false, true);
            }
        } else {
            ItemLayout il = prepareItemLayoutForPath(parent);
            if(il == null) {
                return null;
            }
            ItemView itemView = il.getItemView(last);
            if(itemView == null) {
                return null;
            }
            Item item = itemView.getItem();
            if(item.getClass() == Folder.class) {
                // this is a folder
                FolderView folderView = openFolder((Folder)item, itemView, null, true);
                return folderView.getItemLayout();
            } else if(item.getClass() == EmbeddedFolder.class) {
                // this is a panel
                return ((EmbeddedFolderView)itemView).getEmbeddedItemLayout();
            } else {
                // any other item
                return il;
            }

        }
    }

    /**
     * Prepare all item layouts leading to a given page (all possible paths).
     */
    public ArrayList<ItemLayout> prepareAllItemLayouts(int page) {
        ArrayList<ItemLayout> itemLayouts = new ArrayList<>();
        if(Page.isDashboard(page)) {
            itemLayouts.add(loadRootItemLayout(page, false, false, true));
        } else {
            ArrayList<net.pierrox.lightning_launcher.data.Folder> openers = LLApp.get().getAppEngine().findAllFolderPageOpeners(page);
            for (net.pierrox.lightning_launcher.data.Folder opener : openers) {
                ArrayList<ItemLayout> parentItemLayouts = prepareAllItemLayouts(opener.getPage().id);
                for (ItemLayout il : parentItemLayouts) {
                    ItemView openerItemView = il.getItemView(opener);
                    if (openerItemView instanceof EmbeddedFolderView) {
                        itemLayouts.add(((EmbeddedFolderView) openerItemView).getEmbeddedItemLayout());
                    } else {
                        FolderView folderView = openFolder(opener, openerItemView, null, true);
                        itemLayouts.add(folderView.getItemLayout());
                    }
                }
            }
        }

        return itemLayouts;
    }

    public void releaseItemLayout(ItemLayout il) {
        if(mIsResumed) il.pause();
        il.destroy();
        mItemLayouts.remove(il);
        if(mTargetItemLayout == il) {
            mTargetItemLayout = getTopmostItemLayout();
        }
    }

    public ItemView[] getItemViewsForItem(int itemId) {
        // the item view may not always be already constructed in each item view, so take care of this
        ItemLayout[] ils = getItemLayoutsForPage(Utils.getPageForItemId(itemId));
        int length = ils.length;
        int count = 0;
        ItemView[] itemViews = new ItemView[length];
        for(int i=0; i<length; i++) {
            ItemView itemView = ils[i].getItemView(itemId);
            if(itemView != null) {
                itemViews[count++] = itemView;
            }
        }
        ItemView[] result = new ItemView[count];
        System.arraycopy(itemViews, 0, result, 0, count);
        return result;
    }

    public ItemView[] getItemViewsForItem(Item item) {
        return getItemViewsForItem(item.getId());
    }

    /***************************************** VARIOUS PAGE ACCESSORS ***********************************/
    public ItemLayout getTopmostItemLayout() {
        FolderView fv = findTopmostFolderView();
        if(fv != null) {
            return fv.getItemLayout();
        }

        return getCurrentRootItemLayout();
    }

//    public Page getTopmostPage() {
//        return getTopmostItemLayout().getPage();
//    }

    public ItemLayout getTargetOrTopmostItemLayout() {
        if(mTargetItemLayout == null) {
            return getTopmostItemLayout();
        } else {
            return mTargetItemLayout;
        }
    }

    public int getNextPage(GlobalConfig globalConfig, int direction) {
        int p=globalConfig.homeScreen;
        int[] screens_order=globalConfig.screensOrder;
        int n=screens_order.length;
        for(int i=0; i<n; i++) {
            int rootPageId = getCurrentRootPage().id;
            if(screens_order[i]== rootPageId) {
                if(direction == PAGE_DIRECTION_HINT_BACKWARD) {
                    i--;
                    if(i<0) i=n-1;
                } else {
                    i++;
                    if(i==n) i=0;
                }
                p=screens_order[i];
                break;
            }
        }

        return p;
    }

    /***************************************** BACKGROUND WALLPAPER AND COLOR ***********************************/
    /**
     * This is called for desktop and app drawer pages only, not folders nor panels since they have their own box options.
     */
    public void onPageBackgroundColorChanged(Page page) {
        if(page == getCurrentRootPage()) {
            configureBackground(page);
        }
    }

    public void configureBackground(Page page) {
        if(mContentView == null) {
            return;
        }

        PageConfig c = page.config;
        if(NativeImage.isAvailable() && !LLApp.get().isFreeVersion() && mWallpaperView != null) {
            int alpha = Color.alpha(c.bgColor);
            if(alpha == 255) {
                mWallpaperView.setVisibility(View.GONE);
                mContentView.setBackgroundDrawable(new ColorDrawable(c.bgColor));
            } else {
                mContentView.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                File wp_file = page.getWallpaperFile();
                if(Color.alpha(c.bgColor)==0 && !wp_file.exists()) {
                    mWallpaperView.setVisibility(View.GONE);
                } else {
                    mWallpaperView.setVisibility(View.VISIBLE);
                    mWallpaperView.configure(page.id, wp_file, c.bgColor, c.bgScaleType);
                }
            }
        } else {
            if(mWallpaperView != null) {
                mWallpaperView.setVisibility(View.GONE);
            }
            mContentView.setBackgroundDrawable(new ColorDrawable(c.bgColor));
        }
    }

    /***************************************** SYSTEM BARS ***********************************/
    public void onPageSystemBarsColorChanged(Page page) {
        if(page == getCurrentRootPage()) {
            configureSystemBarsColor(page.config);
        }
    }

    public void onSystemBarsSizeChanged() {
        // pass, override in subclasses when needed
    }

    public void configureSystemBarsColor(PageConfig c) {
        if(Build.VERSION.SDK_INT>=19) {
            mSystemBarTintManager.setStatusBarTintEnabled(!c.statusBarHide || mForceDisplayStatusBar);
            mSystemBarTintManager.setNavigationBarTintEnabled(true);

            mSystemBarTintManager.setStatusBarTintColor(c.statusBarColor);
            mSystemBarTintManager.setNavigationBarTintColor(c.navigationBarColor);

            int flags = mWindow.getAttributes().flags;
            if(Build.VERSION.SDK_INT>=21) {
                flags |= WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS;
                flags &= ~(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
                mWindow.setFlags(flags, 0xffffffff);

                if (Build.VERSION.SDK_INT >= 23) {
                    int f = mContentView.getSystemUiVisibility();
                    if(c.statusBarLight) {
                        f |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                    } else {
                        f &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                    }
                    mContentView.setSystemUiVisibility(f);
                }
                if (Build.VERSION.SDK_INT >= 26) {
                    int f = mContentView.getSystemUiVisibility();
                    if(c.navigationBarLight) {
                        f |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                    } else {
                        f &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                    }
                    mContentView.setSystemUiVisibility(f);
                }
                try {
                    Method setStatusBarColor = mWindow.getClass().getMethod("setStatusBarColor", int.class);
                    setStatusBarColor.invoke(mWindow, 0);
                    Method setNavigationBarColor = mWindow.getClass().getMethod("setNavigationBarColor", int.class);
                    setNavigationBarColor.invoke(mWindow, 0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                flags |= WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION;
                mWindow.setFlags(flags, 0xffffffff);
            }
        } else {
            try {
                //noinspection ResourceType
                Object service = mContext.getSystemService("statusbar");
                Class<?> statusbarManager = Class.forName("android.app.StatusBarManager");
                Method setStatusBarTransparent = statusbarManager.getMethod("setStatusBarTransparent", boolean.class);
                setStatusBarTransparent.invoke(service, c.statusBarColor==0);
            } catch(Exception e1) {
            }

            try {
                Field field = View.class.getDeclaredField("SYSTEM_UI_FLAG_TRANSPARENT_BACKGROUND");
                Class<?> t = field.getType();
                if (t == int.class) {
                    int value = c.statusBarColor==0 ? field.getInt(null) : 0;
                    Method setSystemUiVisibility = View.class.getMethod("setSystemUiVisibility", int.class);
                    setSystemUiVisibility.invoke(mContentView, value);
                }
            } catch (Exception e) {
            }
        }
    }

    public void setForceDisplayStatusBar(boolean forceDisplayStatusBar) {
        mForceDisplayStatusBar = forceDisplayStatusBar;
        if(mForceDisplayStatusBar) {
            showStatusBarIfNeeded();
        } else {
            hideStatusBarIfNeeded();
        }
    }

    public void configureStatusBarVisibility(PageConfig c) {
        mStatusBarHide = c.statusBarHide;
        mIsStatusBarHidden = c.statusBarHide && !mForceDisplayStatusBar;
        SystemUIHelper.setStatusBarVisibility(mWindow, !mIsStatusBarHidden, mForceDisplayStatusBar);

    }

    protected void showStatusBarIfNeeded() {
        if(mStatusBarHide) {
            mIsStatusBarHidden=false;
            SystemUIHelper.setStatusBarVisibility(mWindow, !mIsStatusBarHidden, mForceDisplayStatusBar);
        }
    }

    public void hideStatusBarIfNeeded() {
        if(mStatusBarHide) {
            mIsStatusBarHidden=true;
            SystemUIHelper.setStatusBarVisibility(mWindow, !mIsStatusBarHidden, mForceDisplayStatusBar);
        }
    }

    protected void toggleHideStatusBar() {
        setForceDisplayStatusBar(mIsStatusBarHidden);
    }

    private void showNotifications() {
        setForceDisplayStatusBar(true);

        try {
            Object service=null;
            Class<?> statusbarManager=null;

            //noinspection ResourceType
            service  = mContext.getSystemService("statusbar");
            statusbarManager = Class.forName("android.app.StatusBarManager");
            Method expand = statusbarManager.getMethod(Build.VERSION.SDK_INT>=17 ? "expandNotificationsPanel" : "expand");
            expand.invoke(service);
        } catch(Exception e1) {
        }


        mContentView.postDelayed(new Runnable() {
            @Override
            public void run() {
                setForceDisplayStatusBar(false);
            }
        }, 1000);
    }

    /***************************************** INTENT AND LAUNCH ***********************************/

    public void launchItem(ItemView itemView) {
        if(itemView instanceof EmbeddedFolderView) {
            return;
        }

        Item item = itemView.getItem();
        Page page = item.getPage();
        LightningEngine engine = page.getEngine();

        if(page.isFolder()) {
            // auto-close the folder if appropriate (find the folder view associated with the opener item view of the item layout containing the launched item view)
            FolderView fv = null;
            ItemView openerItemView = itemView.getParentItemLayout().getOpenerItemView();
            if(openerItemView == null) {
                // no opener, this must be the user menu, try to retrieve the folder view throughs its page id and use the first available one
                FolderView[] folderViews = findFolderViews(page.id, true);
                if(folderViews.length > 0) {
                    fv = folderViews[0];
                }
            } else if (!(openerItemView instanceof EmbeddedFolderView)) {
                fv = findFolderView(openerItemView, true);
            }
            if(fv != null) {
                if(fv.getOpener().getFolderConfig().autoClose) {
                    closeFolder(fv, true);
                }
            }
//        } else {
//            if(getCurrentRootPage().config.defaultFolderConfig.autoClose) {
//                int folder_page = Page.NONE;
//                if(item instanceof Folder) {
//                    Folder f=(Folder) item;
//                    folder_page = f.getFolderPageId();
//                } else if(item instanceof Shortcut) {
//                    Intent intent = ((Shortcut) item).getIntent();
//                    int action = intent.getIntExtra(LightningIntent.INTENT_EXTRA_ACTION, GlobalConfig.NOTHING);
//                    if(action == GlobalConfig.OPEN_FOLDER) {
//                        String data = intent.getStringExtra(LightningIntent.INTENT_EXTRA_DATA);
//                        Folder opener = engine.findFirstFolderPageOpener(Integer.parseInt(data));
//                        if(opener != null) {
//                            folder_page = opener.getFolderPageId();
//                        }
//                    }
//                }
//                closeAllFolders(true, folder_page);
//            }
        }

        itemView.setHighlightedNow(true);
        itemView.setHighlightedLater(false);
        if(item.getClass() == Folder.class) {
            openFolder(itemView);
        } else if(item instanceof Shortcut) {
            Shortcut shortcut = (Shortcut) item;
            Intent intent = shortcut.getIntent();
            if(LLApp.get().isLightningIntent(intent)) {
                EventAction eventAction = Utils.decodeEventActionFromLightningIntent(intent);
                if(eventAction != null) {
                    runAction(engine, "SHORTCUT", eventAction, itemView.getParentItemLayout(), itemView);
                } else {
                    if(intent.hasExtra(LightningIntent.INTENT_EXTRA_DESKTOP)) {
                        executeGoToDesktopPositionIntent(intent);
                    }
                }
            } else {
                launchShortcut((ShortcutView)itemView);
            }
        }
    }

    protected void launchShortcut(ShortcutView shortcutView) {
        Shortcut shortcut = shortcutView.getShortcut();
        shortcut.getPage().getEngine().updateLaunchStatisticsForShortcut(shortcut);
        launchIntent(shortcut.getIntent(), shortcutView);
    }

    protected void launchIntent(Intent originalIntent, ItemView itemView) {
        Intent intent=new Intent(originalIntent);

        if(intent.getAction()==null) intent.setAction(Intent.ACTION_MAIN);

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        Rect sourceBounds = null;
        if(itemView != null) {
            sourceBounds = computeItemViewBounds(itemView);
            intent.setSourceBounds(sourceBounds);
        }

        try {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 && Shortcut.INTENT_ACTION_APP_SHORTCUT.equals(intent.getAction())) {
                LauncherApps launcherApps = (LauncherApps) mContext.getSystemService(Context.LAUNCHER_APPS_SERVICE);
                String id = intent.getStringExtra(Shortcut.INTENT_EXTRA_APP_SHORTCUT_ID);
                String pkg = intent.getStringExtra(Shortcut.INTENT_EXTRA_APP_SHORTCUT_PKG);
                try {
                    launcherApps.startShortcut(pkg, id, sourceBounds, null, Process.myUserHandle());
                } catch(SecurityException e) {
                    onLauncherAppsNoHostPermission(itemView);
                } catch (ActivityNotFoundException e) {
                    onLauncherAppsShortcutNotFound(itemView, intent.getStringExtra(Shortcut.INTENT_EXTRA_APP_SHORTCUT_DISABLED_MSG));
                }
            } else {
                mContext.startActivity(intent);
            }
            return;
        } catch(SecurityException e1) {
            try {
                Intent callIntent = new Intent(Intent.ACTION_CALL);
                callIntent.setData(intent.getData());
                mContext.startActivity(callIntent);
                return;
            }  catch(SecurityException e2) {
                if(Intent.ACTION_CALL.equals(intent.getAction())) {
                    onMissingPermissions(new String[]{Manifest.permission.CALL_PHONE}, new int[]{R.string.pr_r13});
                    return;
                }
            }  catch(Exception e2) {
                // pass
            }
        } catch(Exception e) {
            // continue
        }

        if(itemView != null && itemView.getItem().getClass() == Shortcut.class) {
            onShortcutLaunchError((Shortcut) itemView.getItem());
        }
    }

    public void restart() {
        LLApp.get().restart(true);
    }

    /***************************************** ACTION PROCESSING ***********************************/

    public boolean runAction(LightningEngine engine, String source, EventAction ea) {
        return runAction(engine, source, ea, getTargetOrTopmostItemLayout(), null);
    }

    public boolean runAction(LightningEngine engine, String source, EventAction ea, ItemView itemView) {
        return runAction(engine, source, ea, itemView.getParentItemLayout(), itemView);
    }

    public boolean runAction(LightningEngine engine, String source, EventAction ea, ItemLayout il, ItemView itemView) {
        Intent intent;
        FolderView fv;

        boolean res = true;

        switch (ea.action) {
            case GlobalConfig.NOTHING:
                res = false;
                break;

            case GlobalConfig.ZOOM_FULL_SCALE:
                if (il != null) zoomFullScale(il);
                break;

            case GlobalConfig.ZOOM_TO_ORIGIN:
                if (il != null) zoomToOrigin(il);
                break;

            case GlobalConfig.SWITCH_FULL_SCALE_OR_ORIGIN:
                if (il != null) zoomInOrOut(il);
                break;

            case GlobalConfig.LAUNCH_ITEM:
                launchItem(itemView);
                break;

            case GlobalConfig.OPEN_FOLDER:
                if (ea.data != null) {
                    try {
                        int folderPage = Integer.parseInt(ea.data);
                        Folder folder = engine.findFirstFolderPageOpener(folderPage);
                        ItemLayout folderItemLayout = prepareFirstItemLayout(folderPage);
                        ItemView opener = folderItemLayout.getOpenerItemView();
                        if (opener == null) {
                            // can't find the opener here, this is the case when opening a folder from the app drawer in the desktop
                            openFolder(folder, itemView, null, false);
                        } else {
                            openFolder(opener, itemView == null ? opener : itemView);
                        }
                        il = getTopmostItemLayout();
                    } catch (NumberFormatException e) {
                        // pass
                    }
                }
                break;

            case GlobalConfig.LAUNCH_APP:
            case GlobalConfig.LAUNCH_SHORTCUT:
                try {
                    intent = Intent.parseUri(ea.data, 0);
                    if (LLApp.get().isLightningIntent(intent)) {
                        EventAction eventAction = Utils.decodeEventActionFromLightningIntent(intent);
                        if (eventAction != null) {
                            runAction(engine, source, eventAction, il, itemView);
                        } else {
                            if (intent.hasExtra(LightningIntent.INTENT_EXTRA_DESKTOP)) {
                                executeGoToDesktopPositionIntent(intent);
                            }
                        }
                    } else {
                        launchIntent(intent, itemView);
                    }
                } catch (Exception e) {
                    // pass
                }
                break;

            case GlobalConfig.RUN_SCRIPT:
                if (ea.data != null) {
                    try {
                        Pair<Integer, String> id_data = Script.decodeIdAndData(ea.data);
                        Script script = engine.getScriptManager().getOrLoadScript(id_data.first);
                        engine.getScriptExecutor().runScript(this, script, source, id_data.second, il, itemView);
                    } catch (NumberFormatException e) {
                        // pass
                    }
                }
                break;

            case GlobalConfig.SET_VARIABLE:
                Variable v = Variable.decode(ea.data);
                if (v != null) {
                    VariableManager vm = engine.getVariableManager();
                    ScriptManager sm = engine.getScriptManager();

                    String formula = (String) v.value;

                    Pair<String, String[]> p = vm.convertFormulaToScript(formula);
                    String processed_text = p.first;
                    String[] variable_names = p.second;

                    Script script = sm.createScriptForSetVariable(itemView == null ? null : itemView.getItem(), formula);
                    script.setProcessedText(processed_text);

                    int l = variable_names.length;
                    Object[] arguments = new Object[l];
                    String parameters = "";
                    for (int i = 0; i < l; i++) {
                        if (i > 0) {
                            parameters += ",";
                        }
                        String variable_name = variable_names[i];
                        parameters += variable_name;
                        arguments[i] = vm.getVariable(variable_name).value;
                    }

                    Object result = engine.getScriptExecutor().runScriptAsFunction(this, script.id, parameters, arguments, false, false);

                    sm.deleteScript(script);

                    vm.edit();
                    if (result != null) {
                        vm.setVariable(v.name, result);
                    } else {
                        vm.setVariable(v.name, v.value);
                    }
                    vm.commit();
                }
                break;

            case GlobalConfig.GO_HOME_ZOOM_TO_ORIGIN:
                ItemLayout itemLayout = getCurrentRootItemLayout();
                if (itemLayout != null) {
                    zoomToOrigin(itemLayout);
                }
                break;

            case GlobalConfig.RESTART:
                restart();
                break;

            case GlobalConfig.CLOSE_TOPMOST_FOLDER:
                fv = findTopmostFolderView();
                if (fv != null) {
                    closeFolder(fv, true);
                    il = getTopmostItemLayout();
                }
                break;

            case GlobalConfig.CLOSE_ALL_FOLDERS:
                closeAllFolders(true);
                il = getTopmostItemLayout();
                break;

            case GlobalConfig.GO_DESKTOP_POSITION:
                try {
                    boolean ilWasTopmost = il==getTopmostItemLayout();
                    executeGoToDesktopPositionIntent(Intent.parseUri(ea.data, 0));
                    if(ilWasTopmost) {
                        il = getTopmostItemLayout();
                    }
                } catch (Exception e) {
                    // pass
                }
                break;

            case GlobalConfig.UNLOCK_SCREEN:
                LLApp.get().unlockLockScreen(true);
                break;

            case GlobalConfig.SHOW_FLOATING_DESKTOP:
                engine.setFloatingDesktopVisibility(true);
                break;

            case GlobalConfig.HIDE_FLOATING_DESKTOP:
                engine.setFloatingDesktopVisibility(false);
                break;

            case GlobalConfig.SHOW_HIDE_STATUS_BAR:
                toggleHideStatusBar();
                break;

            case GlobalConfig.SHOW_NOTIFICATIONS:
                showNotifications();
                break;

            case GlobalConfig.WALLPAPER_TAP:
            case GlobalConfig.WALLPAPER_SECONDARY_TAP:
                WallpaperManager.getInstance(mContext).sendWallpaperCommand(mWindow.getDecorView().getWindowToken(), ea.action == GlobalConfig.WALLPAPER_TAP ? WallpaperManager.COMMAND_TAP : WallpaperManager.COMMAND_SECONDARY_TAP, mLastTouchedMenuX, mLastTouchedMenuY, 0, null);
                break;
        }

        return processNextAction(engine, source, ea, il, itemView) || res;
    }

    protected boolean processNextAction(LightningEngine engine, String source, EventAction ea, ItemLayout il, ItemView itemView) {
        if(ea.next == null) {
            return false;
        } else {
            return runAction(engine, source, ea.next, il, itemView);
        }
    }

    /************************************** ACTION BAR MANAGEMENT FOR SCRIPTS *******************************/
    protected boolean isAndroidActionBarSupported() {
        return false;
    }

    public void showAndroidActionBar(Function onCreateOptionsMenu, Function onOptionsItemSelected) {
        // default is empty
    }

    public void hideAndroidActionBar() {
        // default is empty
    }

    /***************************************** UPDATE OF VIEWS REFERENCES ***********************************/
    private static class ItemViewUpdate {
        ItemView oldItemView;
        ItemView newItemView;
        net.pierrox.lightning_launcher.script.api.Item cachedItem;
        ArrayList<ItemViewUpdate> children;
        ItemLayout oldChildItemLayout;
        ItemLayout newChildItemLayout;
        Container cachedContainer;
    }

    /**
     * Build the list of old views
     * @param updates
     * @param itemView
     */
    private void addItemViewtoUpdateList(ArrayList<ItemViewUpdate> updates, ItemView itemView) {
        ItemViewUpdate update = new ItemViewUpdate();
        update.oldItemView = itemView;
        Lightning lightning = update.oldItemView.getItem().getPage().getEngine().getScriptExecutor().getLightning();
        update.cachedItem =  lightning.findCachedItem(itemView);
        if(itemView instanceof EmbeddedFolderView) {
            ItemLayout il = ((EmbeddedFolderView) itemView).getEmbeddedItemLayout();
            int count = il.getChildCount();
            update.children = new ArrayList<>(count);
            update.oldChildItemLayout = il;
            update.cachedContainer = lightning.findCachedContainer(il);
            for(int i=0; i<count; i++) {
                View view = il.getChildAt(i);
                if(view instanceof ItemView) {
                    addItemViewtoUpdateList(update.children, (ItemView) view);
                }
            }
        }
        updates.add(update);
    }

    /**
     * Match old item views with new ones
     * @param update
     * @param itemView
     */
    private void matchItemViewUpdateList(ItemViewUpdate update, ItemView itemView) {
        update.newItemView = itemView;
        if(itemView instanceof EmbeddedFolderView && update.children != null) {
            ItemLayout il = ((EmbeddedFolderView) itemView).getEmbeddedItemLayout();
            update.newChildItemLayout = il;
            for (ItemViewUpdate childUpdate : update.children) {
                matchItemViewUpdateList(childUpdate, il.getItemView(childUpdate.oldItemView.getItem()));
            }
        }
    }

    /**
     * Update references according to an update list
     */
    private void updateItemViewReferences(ArrayList<ItemViewUpdate> updates) {
        for (ItemViewUpdate update : updates) {
            // update the last touched item view
            if(mLastTouchedItemView == update.oldItemView) {
                mLastTouchedItemView = update.newItemView;
            }

            if(mTargetItemLayout != null && mTargetItemLayout == update.oldChildItemLayout) {
                mTargetItemLayout = update.newChildItemLayout;
            }

            // update script items and containers
            Lightning lightning = update.oldItemView.getItem().getPage().getEngine().getScriptExecutor().getLightning();
            if(update.cachedItem != null) {
                lightning.updateCachedItem(update.cachedItem, update.newItemView);
            }
            if(update.cachedContainer != null) {
                lightning.updateCachedContainer(update.cachedContainer, update.newChildItemLayout);
            }

            // update openers
            for (ItemLayout il : mItemLayouts) {
                ItemView openerItemView = il.getOpenerItemView();
                if(openerItemView != null && openerItemView == update.oldItemView) {
                    il.setOpenerItemView(update.newItemView);
                }
            }

            // restore item layout position
            if(update.oldChildItemLayout != null) {
                update.newChildItemLayout.setLocalTransform(update.oldChildItemLayout.getLocalTransform());
            }

            // update child item views if any
            if(update.children != null) {
                updateItemViewReferences(update.children);
            }

        }
    }

    /***************************************** SCREEN ORIENTATION ***********************************/
    public void onOrientationChanged() {
        if(mSystemBarTintManager != null) {
            mSystemBarTintManager.onConfigurationChanged(mWindow);
        }

        updateOrientationOrRotation();


        Page mainPage = getCurrentRootPage();
        LightningEngine lightningEngine = mainPage.getEngine();
        GlobalConfig globalConfig = lightningEngine.getGlobalConfig();
        if(mIsPortrait) {
            EventAction ea = mainPage.config.orientationPortrait;
            if(ea.action == GlobalConfig.UNSET) {
                ea = globalConfig.orientationPortrait;
            }
            runAction(lightningEngine, "PORTRAIT", ea);
        } else {
            EventAction ea = mainPage.config.orientationLandscape;
            if(ea.action == GlobalConfig.UNSET) {
                ea = globalConfig.orientationLandscape;
            }
            runAction(lightningEngine, "LANDSCAPE", ea);
        }
    }

    private void updateOrientationOrRotation() {
        int[] size = new int[2];
        getScreenSize(size);

        // FIXME: this should be done for all engines
        LLApp.get().getAppEngine().getBuiltinDataCollectors().setDisplayOrientationAndSize(getResourcesOrientation(), size[0], size[1]);

        int degrees=getDegreesFromCurrentConfiguration();
        mIsPortrait=(degrees==0);
        if(degrees!=mDisplayOrientation) {
            mDisplayOrientation=degrees;
            for (ItemLayout il : mItemLayouts) {
                il.updateOrientation();
            }
        }
    }

    public void setCustomScreenSize(int width, int height) {
        mCustomScreenWidth = width;
        mCustomScreenHeight = height;
    }

    private void getScreenSize(int[] out) {
        if(mWindow != null && (mCustomScreenWidth == 0 || mCustomScreenHeight == 0)) {
            DisplayMetrics dm = new DisplayMetrics();
            Display display = mWindow.getWindowManager().getDefaultDisplay();
            if (Build.VERSION.SDK_INT >= 17) {
                display.getRealMetrics(dm);
            } else {
                display.getMetrics(dm);
            }
            out[0] = dm.widthPixels;
            out[1] = dm.heightPixels;
        } else {
            out[0] = mCustomScreenWidth;
            out[1] = mCustomScreenHeight;
        }
    }

    public boolean isPortrait() {
        return mIsPortrait;
    }

    public int getResourcesOrientation() {
        return getRealResources().getConfiguration().orientation;
    }

    private int getDegreesFromCurrentConfiguration() {
        return getResourcesOrientation()==Configuration.ORIENTATION_LANDSCAPE ? 90 : 0;
    }

    private Runnable mApplyOrientationChangedRunnable = new Runnable() {
        @Override
        public void run() {
            onOrientationChanged(mCurrentOrientation);
        }
    };

    protected void onOrientationChanged(int orientation) {
        for (ItemLayout il : mItemLayouts) {
            il.updateItemsOrientation(orientation);
        }
    }

    /***************************************** PAGE LISTENER ***********************************/
    @Override
    public void onPageLoaded(Page page) {
    }

    @Override
    public void onPageRemoved(Page page) {
        if(page.isFolder()) {
            removeFolders(page);
        }

        ItemLayout[] ils = getItemLayoutsForPage(page.id);
        for (ItemLayout il : ils) {
            mRootItemLayouts.remove(il);
            if(mCurrentRootItemLayout == il) {
                mCurrentRootItemLayout = mRootItemLayouts.size() > 0 ? mRootItemLayouts.get(0) : null;
            }
            releaseItemLayout(il);
        }
    }

    @Override
    public void onPagePaused(Page page) {
    }

    @Override
    public void onPageResumed(Page page) {
    }

    @Override
    public void onPageModified(Page page) {
        if(page == getCurrentRootPage()) {
            // TODO this would be nice to use a more specific event handler in order to avoid useless background configuration
            configureBackground(page);
        }

        ItemLayout[] ils = getItemLayoutsForPage(page.id);
        for (ItemLayout il : ils) {

            // save the old view hierarchy
            int count = il.getChildCount();
            ArrayList<ItemViewUpdate> updates = new ArrayList<>(count);
            for(int i=0; i<count; i++) {
                View view = il.getChildAt(i);
                if(view instanceof ItemView) {
                    addItemViewtoUpdateList(updates, (ItemView) view);
                }
            }

            // apply view modifications
            il.onPageModified();

            // update references with the new views
            for (ItemViewUpdate update : updates) {
                matchItemViewUpdateList(update, il.getItemView(update.oldItemView.getItem()));
            }
            updateItemViewReferences(updates);
        }
    }

    @Override
    public void onPageEditModeEntered(Page page) {

    }

    @Override
    public void onPageEditModeLeaved(Page page) {

    }

    @Override
    public void onPageItemLoaded(Item item) {

    }

    @Override
    public void onPageItemDestroyed(Item item) {

    }

    @Override
    public void onPageItemAdded(Item item) {
        Page page = item.getPage();
        LightningEngine engine = page.getEngine();
        ItemLayout[] ils = getItemLayoutsForPage(page.id);
        for (ItemLayout il : ils) {
            il.onPageItemAdded(item);
        }
        updateEmptyMessageVisibility(page);

        EventAction ea = page.config.itemAdded;
        if(ea.action == GlobalConfig.UNSET) {
            ea = engine.getGlobalConfig().itemAdded;
        }
        ItemView[] itemViews = getItemViewsForItem(item);
        for (ItemView itemView : itemViews) {
            runAction(engine, "C_ITEM_ADDED", ea, itemView);
        }
    }

    @Override
    public void onPageItemBeforeRemove(Item item) {
        Page page = item.getPage();
        LightningEngine engine = page.getEngine();
        EventAction ea = page.config.itemRemoved;
        if(ea.action == GlobalConfig.UNSET) {
            ea = engine.getGlobalConfig().itemRemoved;
        }

        ItemView[] itemViews = getItemViewsForItem(item);
        for (ItemView itemView : itemViews) {
            runAction(engine, "C_ITEM_REMOVED", ea, itemView);
        }

        if(mLastTouchedItemView != null && mLastTouchedItemView.getItem() == item) {
            mLastTouchedItemView = null;
        }
    }

    @Override
    public void onPageItemRemoved(Page page, Item item) {
        ItemLayout[] ils = getItemLayoutsForPage(page.id);
        for (ItemLayout il : ils) {
            FolderView folderView = findFolderView(il.getItemView(item), null);
            if(folderView != null) {
                removeFolderView(folderView);
            }
            il.onPageItemRemoved(item);
        }
        updateEmptyMessageVisibility(page);
    }

    @Override
    public void onPageItemChanged(Page page, Item item) {
        // TODO: it would be nice to avoid a full view release/rebuild
        ItemLayout[] ils = getItemLayoutsForPage(page.id);
        for (ItemLayout il : ils) {
            // update the folder opener item view, if any
            ItemView oldItemView = il.getItemView(item);
            if(oldItemView != null) { // the old item view can be null if not displayed (case of a folder in the app drawer in alphabetical mode for instance), or not ready yet

                // save the old view hierarchy
                ArrayList<ItemViewUpdate> updates = new ArrayList<>(1);
                addItemViewtoUpdateList(updates, oldItemView);

                // apply view modifications
                il.onPageItemChanged(item);

                // update references with the new views
                ItemView newItemView = il.getItemView(item);
                matchItemViewUpdateList(updates.get(0), newItemView);
                updateItemViewReferences(updates);
            }
        }
    }

    @Override
    public void onItemPaused(Item item) {

    }

    @Override
    public void onItemResumed(Item item) {

    }

    @Override
    public void onItemVisibilityChanged(Item item) {
        ItemView[] itemViews = getItemViewsForItem(item);
        for (ItemView itemView : itemViews) {
            itemView.updateViewVisibility();
        }
    }

    @Override
    public void onItemAlphaChanged(Item item) {
        ItemView[] itemViews = getItemViewsForItem(item);
        for (ItemView itemView : itemViews) {
            itemView.updateViewAlpha();
        }
    }

    @Override
    public void onItemTransformChanged(Item item, boolean fast) {
        ItemView[] itemViews = getItemViewsForItem(item);
        for (ItemView itemView : itemViews) {
            itemView.setTransform(item.getTransform(), fast);
        }
    }

    @Override
    public void onItemCellChanged(Item item) {
        ItemView[] itemViews = getItemViewsForItem(item);
        for (ItemView itemView : itemViews) {
            itemView.updateViewSize();
            itemView.getParentItemLayout().onItemCellChanged(item);
        }
    }

    @Override
    public void onItemBindingsChanged(Item item, boolean apply) {
        ItemView[] itemViews = getItemViewsForItem(item);
        for (ItemView itemView : itemViews) {
            itemView.getItem().getPage().getEngine().getVariableManager().updateBindings(itemView, item.getItemConfig().bindings, apply, this, true);
        }
    }

    @Override
    public void onItemError(Item item, Error error) {
        // check that the item belongs to this screen
        if(getItemViewsForItem(item).length == 0) {
            return;
        }

        // accumulate errors, so that the consumer above can display a synthetic report
        // at the moment the source item is not used, and not kept
        if(mErrors == null) {
            mErrors = new ArrayList<>(3);
        }
        mErrors.add(error);
    }

    @Override
    public void onShortcutLabelChanged(Shortcut shortcut) {
        ItemView[] itemViews = getItemViewsForItem(shortcut);
        for (ItemView itemView : itemViews) {
            ((ShortcutView)itemView).updateLabelText();
        }
        if(shortcut.getClass() == Folder.class) {
            FolderView[] fvs = findFolderViews(((Folder)shortcut).getFolderPageId(), true);
            for (FolderView fv : fvs) {
                fv.setTitle(shortcut.getLabel());
            }
        }
    }

    @Override
    public void onFolderPageIdChanged(Folder folder, int oldPageId) {
        if(folder instanceof EmbeddedFolder) {
            ItemView[] itemViews = getItemViewsForItem(folder);
            for (ItemView itemView : itemViews) {
                itemView.getParentItemLayout().onPageItemChanged(folder);
            }
        } else {
            FolderView[] folderViews = findFolderViews(oldPageId, null);
            for (FolderView fv : folderViews) {
                boolean open = fv.isOpen();
                if(open) {
                    closeFolder(fv, false);
                    openFolder(fv.getOpenerItemView());
                    fv.skipOpenAnimation();
                } else {
                    removeFolderView(fv);
                    openFolder(folder, fv.getOpenerItemView(), null, true);
                }

            }
        }
    }

    @Override
    public void onPageItemZIndexChanged(Page page, int old_index, int new_index) {
        ItemLayout[] ils = getItemLayoutsForPage(page.id);
        for (ItemLayout il : ils) {
            il.onPageItemZIndexChanged(old_index, new_index);
        }
    }

    @Override
    public void onPageFolderWindowChanged(Page page, Folder folder) {
        for(FolderView fv : mFolderViews) {
            if(fv.isOpen() && fv.getOpener() == folder) {
                fv.configure(folder, fv.getOpenerItemView(), fv.getPage());
            }
        }
    }

    /***************************************** ITEM LAYOUT LISTENER ***********************************/
    @Override
    public void onItemLayoutPressed() {

    }

    @Override
    public void onItemLayoutClicked(ItemLayout item_layout, int x, int y) {
        runActionForItemLayout(item_layout, "C_CLICK", "bgTap", x, y);
    }

    @Override
    public void onItemLayoutDoubleClicked(ItemLayout item_layout, int x, int y) {
        runActionForItemLayout(item_layout, "C_DOUBLE_CLICK", "bgDoubleTap", x, y);
    }

    @Override
    public void onItemLayoutLongClicked(ItemLayout item_layout, int x, int y) {
        runActionForItemLayout(item_layout, "C_LONG_CLICK", "bgLongTap", x, y);
    }

    @Override
    public void onItemLayoutSwipeLeft(ItemLayout item_layout) {
        runActionForItemLayout(item_layout, "C_SWIPE_LEFT", "swipeLeft");
    }

    @Override
    public void onItemLayoutSwipeRight(ItemLayout item_layout) {
        runActionForItemLayout(item_layout, "C_SWIPE_RIGHT", "swipeRight");
    }

    @Override
    public void onItemLayoutSwipeUp(ItemLayout item_layout) {
        runActionForItemLayout(item_layout, "C_SWIPE_UP", "swipeUp");
    }

    @Override
    public void onItemLayoutSwipeDown(ItemLayout item_layout) {
        runActionForItemLayout(item_layout, "C_SWIPE_DOWN", "swipeDown");
    }

    @Override
    public void onItemLayoutSwipe2Left(ItemLayout item_layout) {
        runActionForItemLayout(item_layout, "C_SWIPE2_LEFT", "swipe2Left");
    }

    @Override
    public void onItemLayoutSwipe2Right(ItemLayout item_layout) {
        runActionForItemLayout(item_layout, "C_SWIPE2_RIGHT", "swipe2Right");
    }

    @Override
    public void onItemLayoutSwipe2Up(ItemLayout item_layout) {
        runActionForItemLayout(item_layout, "C_SWIPE2_UP", "swipe2Up");
    }

    @Override
    public void onItemLayoutSwipe2Down(ItemLayout item_layout) {
        runActionForItemLayout(item_layout, "C_SWIPE2_DOWN", "swipe2Down");
    }

    private void runActionForItemLayout(ItemLayout item_layout, String source, String field) {
        runActionForItemLayout(item_layout, source, field, Integer.MIN_VALUE, Integer.MIN_VALUE);
    }

    private void runActionForItemLayout(ItemLayout item_layout, String source, String field, int x, int y) {
        setLastTouchEventForItemLayout(item_layout, x, y);
        Page page = item_layout.getPage();
        LightningEngine engine = page.getEngine();
        try {
            EventAction ea;

            // try the config for the page in the item layout first
            ea = (EventAction) page.config.getClass().getField(field).get(page.config);

            if (ea.action == GlobalConfig.UNSET) {
                // then the config for the main page
                PageConfig mainPageConfig = getCurrentRootPage().config;
                ea = (EventAction) mainPageConfig.getClass().getField(field).get(mainPageConfig);
            }

            if (ea.action == GlobalConfig.UNSET) {
                // then the global config
                GlobalConfig globalConfig = engine.getGlobalConfig();
                ea = (EventAction) globalConfig.getClass().getField(field).get(globalConfig);
            }

            runAction(engine, source, ea, item_layout, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onItemLayoutZoomChanged(float scale) {

    }

    @Override
    public void onItemLayoutPinchStart() {

    }

    @Override
    public boolean onItemLayoutPinch(float scale) {
        return true;
    }

    @Override
    public void onItemLayoutPinchEnd(boolean from_user) {

    }

    @Override
    public void onItemLayoutOnLayoutDone(ItemLayout item_layout) {

    }

    @Override
    public void onItemLayoutSizeChanged(ItemLayout item_layout, int w, int h, int oldw, int oldh) {
        if(item_layout.getPage() == getCurrentRootPage()) {
            for(FolderView fv : mFolderViews) {
                fv.getItemLayout().setDesktopSize(w, h);
            }
        }
    }

    private long mPreviousPositionChangeDate;
    @Override
    public void onItemLayoutPositionChanged(ItemLayout il, float mCurrentDx, float mCurrentDy, float mCurrentScale) {
        long now = System.currentTimeMillis();
        Page page = il.getPage();
        EventAction ea = page.config.posChanged;
        if(ea.action==GlobalConfig.RUN_SCRIPT) {
            try {
                Pair<Integer,String> id_data = Script.decodeIdAndData(ea.data);

                page.getEngine().getScriptExecutor().runScript(this, id_data.first, "C_POSITION_CHANGED", id_data.second, il);
            } catch(Exception e) {
                e.printStackTrace();
            }
        } else if(now-mPreviousPositionChangeDate>300) {
            runAction(page.getEngine(), "C_POSITION_CHANGED", ea, il, null);
            mPreviousPositionChangeDate = now;
        }
    }

    @Override
    public void onItemLayoutStopPointReached(ItemLayout item_layout, StopPoint sp) {
        EventAction ea = sp.getReachedAction();
        if(ea.action != GlobalConfig.UNSET) {
            runAction(sp.getPage().getEngine(), "STOP_POINT", ea, item_layout.getItemView(sp));
        }
    }

    @Override
    public void onItemLayoutWindowSystemUiVisibility(ItemLayout il, int visibility) {
        if(mSystemBarTintManager != null && mHasWindowFocus) {
            mSystemBarTintManager.setStatusBarTintEnabled((visibility&View.SYSTEM_UI_FLAG_FULLSCREEN) == 0);
        }
    }

    @Override
    public void onItemLayoutMasterSelectedItemChanged(Item masterSelectedItem) {

    }

    @Override
    public void onItemLayoutPageLoaded(ItemLayout itemLayout, Page oldPage, Page newPage) {
        if(mLastTouchedItemView != null) {
            mLastTouchedItemView = itemLayout.getItemView(mLastTouchedItemView.getItem());
        }
    }

    @Override
    public void onItemLayoutAppShortcutDropped(ItemLayout itemLayout, Object shortcutInfo, float x, float y) {

    }

    @Override
    public void onHandlePressed(HandleView.Handle h) {

    }

    @Override
    public void onHandleMoved(HandleView.Handle h, float dx, float dy) {

    }

    @Override
    public void onHandleUnpressed(HandleView.Handle h, float dx, float dy) {

    }

    @Override
    public void onHandleClicked(HandleView.Handle h) {

    }

    @Override
    public void onHandleLongClicked(HandleView.Handle h) {

    }
}
