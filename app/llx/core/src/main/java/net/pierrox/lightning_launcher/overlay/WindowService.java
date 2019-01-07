package net.pierrox.lightning_launcher.overlay;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SearchEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

import net.pierrox.lightning_launcher.LLApp;
import net.pierrox.lightning_launcher.R;
import net.pierrox.lightning_launcher.activities.ResourcesWrapperHelper;
import net.pierrox.lightning_launcher.configuration.GlobalConfig;
import net.pierrox.lightning_launcher.data.EventAction;
import net.pierrox.lightning_launcher.data.Page;
import net.pierrox.lightning_launcher.engine.LightningEngine;
import net.pierrox.lightning_launcher.engine.Screen;
import net.pierrox.lightning_launcher.views.FolderView;
import net.pierrox.lightning_launcher.views.ItemLayout;
import net.pierrox.lightning_launcher.views.item.ItemView;

import java.lang.reflect.Method;


@SuppressLint("NewApi")
public class WindowService extends Service implements LightningEngine.GlobalConfigListener {
    public static final String INTENT_ACTION_SHOW = "s";
    public static final String INTENT_ACTION_HIDE = "h";

    private static final int OPEN_CLOSE_ANIMATION_DURATION = 300;

    private WindowScreen mScreen;

    private ResourcesWrapperHelper mResourcesWrapperHelper;

    private WindowManager mWindowManager;
    private Window mWindow;
    private View mContentView;
    private View mWorkspaceView;
    private View mShowHandle;
    private View mHideHandle;
    private View mWallpaperView;

    private int mScreenWidth;
    private int mScreenHeight;

    private ItemLayout mItemLayout;
    private Page mMainPage;

    private int mOverlayScreen;
    private GlobalConfig.OverlayHandlePosition mOverlayShowHandlePosition;
    private float mOverlayShowHandleSize;
    private float mOverlayShowHandleWidth;
    private GlobalConfig.OverlayHandlePosition mOverlayHideHandlePosition;
    private float mOverlayHideHandleSize;
    private float mOverlayHideHandleWidth;
    private GlobalConfig.OverlayAnimation mOverlayAnimation;
    private boolean mOverlayDisplayHandles;

    private boolean mIsShown;

    public static boolean isPermissionAllowed(Context context) {
        return Build.VERSION.SDK_INT<23 || Settings.canDrawOverlays(context);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        LLApp app = LLApp.get();

        LightningEngine lightningEngine = app.getAppEngine();
        lightningEngine.registerGlobalConfigChangeListener(this);
        final GlobalConfig globalConfig = lightningEngine.getGlobalConfig();
        mOverlayScreen = globalConfig.overlayScreen;
        mOverlayShowHandlePosition =globalConfig.overlayShowHandlePosition;
        mOverlayShowHandleSize = globalConfig.overlayShowHandleSize;
        mOverlayShowHandleWidth = globalConfig.overlayShowHandleWidth;
        mOverlayHideHandlePosition = globalConfig.overlayHideHandlePosition;
        mOverlayHideHandleSize = globalConfig.overlayHideHandleSize;
        mOverlayHideHandleWidth = globalConfig.overlayHideHandleWidth;
        mOverlayAnimation = globalConfig.overlayAnimation;
        mOverlayDisplayHandles = globalConfig.overlayDisplayHandles;

        mScreen = new WindowScreen(this, R.layout.window);

        mContentView = mScreen.getContentView();
        mContentView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                int action = event.getAction();
                if (keyCode == KeyEvent.KEYCODE_MENU) {
                    if(action == KeyEvent.ACTION_UP) {
                        EventAction ea = mMainPage.config.menuKey;
                        if (ea.action == GlobalConfig.UNSET) {
                            ea = globalConfig.menuKey;
                        }
                        mScreen.runAction(mMainPage.getEngine(), "K_MENU", ea);
                    }
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_BACK) {
                    if(action == KeyEvent.ACTION_UP) {
                        FolderView fv = mScreen.findTopmostFolderView();
                        if (fv != null) {
                            mScreen.closeFolder(fv, true);
                        } else {
                            EventAction ea = mMainPage.config.backKey;
                            if(ea.action == GlobalConfig.UNSET) {
                                hideWorkspace();
                            } else {
                                mScreen.runAction(mMainPage.getEngine(), "K_BACK", ea);
                            }
                        }
                    }
                    return true;
                }

                return false;
            }
        });

        mWorkspaceView = mContentView.findViewById(R.id.workspace);
        mWorkspaceView.setVisibility(View.GONE);

        // visibility state of this surface view need to be manually managed, for unknown reason it doesn't get hidden when its container is hidden
        mWallpaperView = mContentView.findViewById(R.id.wp);

        mShowHandle = mContentView.findViewById(R.id.show_handle);
        mShowHandle.setVisibility(View.VISIBLE);

        mHideHandle = mContentView.findViewById(R.id.hide_handle);
        mHideHandle.setVisibility(View.GONE);

        mItemLayout=(ItemLayout) mWorkspaceView.findViewById(R.id.window_il);
        mScreen.takeItemLayoutOwnership(mItemLayout);
        mItemLayout.setHonourFocusChange(false);
    
        int windowType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
    
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                windowType,
                0
//                        |WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
//                        |WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
//                        |WindowManager.LayoutParams.FLAG_LOCAL_FOCUS_MODE
                        |WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        |0,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.LEFT | Gravity.TOP;
//        mWindowManager.addView(mContentView, lp);

        try {
            try {
                // before Android 6
                Class PolicyManager = getClass().getClassLoader().loadClass("com.android.internal.policy.PolicyManager");
                Method m = PolicyManager.getMethod("makeNewWindow", Context.class);
                mWindow = (Window) m.invoke(null, this);
            } catch(ClassNotFoundException e) {
                // starting at Android 6
                Class PhoneWindow = getClass().getClassLoader().loadClass("com.android.internal.policy.PhoneWindow");
                mWindow = (Window) PhoneWindow.getDeclaredConstructor(Context.class).newInstance(this);
            }

            mWindow.setWindowManager(mWindowManager, null, getString(R.string.app_name));
            mWindow.setBackgroundDrawable(new ColorDrawable(0));
            mWindow.requestFeature(Window.FEATURE_NO_TITLE);
            mWindow.setContentView(mContentView);
            mWindowManager.addView(mWindow.getDecorView(), lp);

            mWindow.setCallback(new Window.Callback() {
                @Override
                public boolean dispatchKeyEvent(KeyEvent event) {
                    return mContentView.dispatchKeyEvent(event);
                }

                @Override
                public boolean dispatchKeyShortcutEvent(KeyEvent event) {
                    return false;
                }

                @Override
                public boolean dispatchTouchEvent(MotionEvent event) {
                    return mContentView.dispatchTouchEvent(event);
                }

                @Override
                public boolean dispatchTrackballEvent(MotionEvent event) {
                    return false;
                }

                @Override
                public boolean dispatchGenericMotionEvent(MotionEvent event) {
                    return false;
                }

                @Override
                public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
                    return false;
                }

                @Override
                public View onCreatePanelView(int featureId) {
                    return null;
                }

                @Override
                public boolean onCreatePanelMenu(int featureId, Menu menu) {
                    return false;
                }

                @Override
                public boolean onPreparePanel(int featureId, View view, Menu menu) {
                    return false;
                }

                @Override
                public boolean onMenuOpened(int featureId, Menu menu) {
                    return false;
                }

                @Override
                public boolean onMenuItemSelected(int featureId, MenuItem item) {
                    return false;
                }

                @Override
                public void onWindowAttributesChanged(WindowManager.LayoutParams attrs) {

                }

                @Override
                public void onContentChanged() {

                }

                @Override
                public void onWindowFocusChanged(boolean hasFocus) {

                }

                @Override
                public void onAttachedToWindow() {

                }

                @Override
                public void onDetachedFromWindow() {

                }

                @Override
                public void onPanelClosed(int featureId, Menu menu) {

                }

                @Override
                public boolean onSearchRequested() {
                    return false;
                }

                @Override
                public boolean onSearchRequested(SearchEvent searchEvent) {
                    return false;
                }

                @Override
                public ActionMode onWindowStartingActionMode(ActionMode.Callback callback) {
                    return null;
                }

                @Override
                public ActionMode onWindowStartingActionMode(ActionMode.Callback callback, int type) {
                    return null;
                }

                @Override
                public void onActionModeStarted(ActionMode mode) {

                }

                @Override
                public void onActionModeFinished(ActionMode mode) {

                }
            });

            mScreen.setWindow(mWindow);
        } catch (Exception e) {
            e.printStackTrace();
        }

        configurePage();

        configureScreen();

        configureDrawer();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        final LLApp app = LLApp.get();

        app.getAppEngine().unregisterGlobalConfigChangeListener(this);

        mScreen.destroy();

        mWindowManager.removeViewImmediate(mWindow.getDecorView());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null) {
            String action = intent.getAction();
            if (INTENT_ACTION_SHOW.equals(action)) {
                showWorkspace();
            } else if (INTENT_ACTION_HIDE.equals(action)) {
                hideWorkspace();
            }
        }

        return START_STICKY;
    }

    @Override
    public final Resources getResources() {
        if(mResourcesWrapperHelper == null) {
            mResourcesWrapperHelper = new ResourcesWrapperHelper(this, super.getResources());
        }
        return mResourcesWrapperHelper.getResources();
    }

    public final Resources getRealResources() {
        return super.getResources();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        mScreen.onOrientationChanged();

        configureScreen();
        configureDrawer();
    }

    @Override
    public void onGlobalConfigChanged(GlobalConfig newGlobalConfig) {
        if(newGlobalConfig.overlayScreen != mOverlayScreen) {
            mOverlayScreen = newGlobalConfig.overlayScreen;
            configurePage();
        }

        if(newGlobalConfig.overlayAnimation != mOverlayAnimation ||
                newGlobalConfig.overlayShowHandlePosition != mOverlayShowHandlePosition ||
                newGlobalConfig.overlayShowHandleSize != mOverlayShowHandleSize ||
                newGlobalConfig.overlayShowHandleWidth != mOverlayShowHandleWidth ||
                newGlobalConfig.overlayHideHandlePosition != mOverlayHideHandlePosition ||
                newGlobalConfig.overlayHideHandleSize != mOverlayHideHandleSize ||
                newGlobalConfig.overlayHideHandleWidth != mOverlayHideHandleWidth ||
                newGlobalConfig.overlayDisplayHandles != mOverlayDisplayHandles) {
            mOverlayAnimation = newGlobalConfig.overlayAnimation;
            mOverlayShowHandlePosition = newGlobalConfig.overlayShowHandlePosition;
            mOverlayShowHandleSize = newGlobalConfig.overlayShowHandleSize;
            mOverlayShowHandleWidth = newGlobalConfig.overlayShowHandleWidth;
            mOverlayHideHandlePosition = newGlobalConfig.overlayHideHandlePosition;
            mOverlayHideHandleSize = newGlobalConfig.overlayHideHandleSize;
            mOverlayHideHandleWidth = newGlobalConfig.overlayHideHandleWidth;
            mOverlayDisplayHandles = newGlobalConfig.overlayDisplayHandles;
            configureScreen();
            configureDrawer();
        }
    }

    private void configurePage() {
        LightningEngine lightningEngine = LLApp.get().getAppEngine();
        mMainPage = lightningEngine.getOrLoadPage(mOverlayScreen);
        mItemLayout.setPage(mMainPage);
        mScreen.configureBackground(mMainPage);
    }

    private void configureHandle(View handle, GlobalConfig.OverlayHandlePosition position, float user_size, float user_width) {
        int gravity;
        int width, height;
        int sw_h = (int) (mScreenWidth * user_width);
        int sh_h = (int) (mScreenHeight * user_size);
        int sw_v = (int) (mScreenWidth * user_size);
        int sh_v = (int) (mScreenHeight * user_width);
        switch (position) {
            case LEFT_TOP:
                gravity = Gravity.LEFT | Gravity.TOP;
                width = sw_h;
                height = sh_h;
                break;

            case LEFT_MIDDLE:
                gravity = Gravity.LEFT | Gravity.CENTER_VERTICAL;
                width = sw_h;
                height = sh_h;
                break;

            case LEFT_BOTTOM:
                gravity = Gravity.LEFT | Gravity.BOTTOM;
                width = sw_h;
                height = sh_h;
                break;

            case RIGHT_TOP:
                gravity = Gravity.RIGHT | Gravity.TOP;
                width = sw_h;
                height = sh_h;
                break;

            case RIGHT_MIDDLE:
                gravity = Gravity.RIGHT | Gravity.CENTER_VERTICAL;
                width = sw_h;
                height = sh_h;
                break;

            case RIGHT_BOTTOM:
                gravity = Gravity.RIGHT | Gravity.BOTTOM;
                width = sw_h;
                height = sh_h;
                break;

            case TOP_LEFT:
                gravity = Gravity.TOP | Gravity.LEFT;
                width = sw_v;
                height = sh_v;
                break;

            case TOP_CENTER:
                gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
                width = sw_v;
                height = sh_v;
                break;

            case TOP_RIGHT:
                gravity = Gravity.TOP | Gravity.RIGHT;
                width = sw_v;
                height = sh_v;
                break;

            case BOTTOM_LEFT:
                gravity = Gravity.BOTTOM | Gravity.LEFT;
                width = sw_v;
                height = sh_v;
                break;

            case BOTTOM_CENTER:
                gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
                width = sw_v;
                height = sh_v;
                break;

            case BOTTOM_RIGHT:
                gravity = Gravity.BOTTOM | Gravity.RIGHT;
                width = sw_v;
                height = sh_v;
                break;

            default:
                return;
        }

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(width, height);
        lp.gravity = gravity;
        handle.setLayoutParams(lp);
        mContentView.requestLayout();
    }

    private void configureDrawer() {
        configureHandle(mShowHandle, mOverlayShowHandlePosition, mOverlayShowHandleSize, mOverlayShowHandleWidth);
        configureHandle(mHideHandle, mOverlayHideHandlePosition, mOverlayHideHandleSize, mOverlayHideHandleWidth);

        mShowHandle.setBackgroundColor(mOverlayDisplayHandles ? 0x8000a000 : 0);
        mHideHandle.setBackgroundColor(mOverlayDisplayHandles ? 0x80a00000 : 0);

        WindowManager.LayoutParams lp = ((WindowManager.LayoutParams)mWindow.getDecorView().getLayoutParams());
        lp.gravity = ((FrameLayout.LayoutParams)mShowHandle.getLayoutParams()).gravity;
        mWindowManager.updateViewLayout(mWindow.getDecorView(), lp);

//        mWindow.setGravity(((FrameLayout.LayoutParams)mShowHandle.getLayoutParams()).gravity);

        mShowHandle.setOnTouchListener(new View.OnTouchListener() {
            VelocityTracker mVelocityTracker;

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mVelocityTracker = VelocityTracker.obtain();
                        mWorkspaceView.setVisibility(View.VISIBLE);
                        mWallpaperView.setVisibility(View.VISIBLE);
                        overlayIsShown();
                        break;

                    case MotionEvent.ACTION_MOVE:
                        mVelocityTracker.addMovement(motionEvent);
                        if (mOverlayAnimation == GlobalConfig.OverlayAnimation.SLIDE) {
                            switch (mOverlayShowHandlePosition) {
                                case LEFT_TOP:
                                case LEFT_MIDDLE:
                                case LEFT_BOTTOM:
                                    mWorkspaceView.setTranslationX(-mWorkspaceView.getWidth() + motionEvent.getRawX());
                                    mWorkspaceView.setTranslationY(0);
                                    break;
                                case RIGHT_TOP:
                                case RIGHT_MIDDLE:
                                case RIGHT_BOTTOM:
                                    mWorkspaceView.setTranslationX(motionEvent.getRawX());
                                    mWorkspaceView.setTranslationY(0);
                                    break;
                                case TOP_LEFT:
                                case TOP_CENTER:
                                case TOP_RIGHT:
                                    mWorkspaceView.setTranslationY(-mWorkspaceView.getHeight() + motionEvent.getRawY());
                                    mWorkspaceView.setTranslationX(0);
                                    break;
                                case BOTTOM_LEFT:
                                case BOTTOM_CENTER:
                                case BOTTOM_RIGHT:
                                    mWorkspaceView.setTranslationY(motionEvent.getRawY());
                                    mWorkspaceView.setTranslationX(0);
                                    break;
                            }
                        } else {
                            float alpha = 1;
                            switch (mOverlayShowHandlePosition) {
                                case LEFT_TOP:
                                case LEFT_MIDDLE:
                                case LEFT_BOTTOM:
                                    alpha = motionEvent.getRawX() / (float) mScreenWidth;
                                    break;
                                case RIGHT_TOP:
                                case RIGHT_MIDDLE:
                                case RIGHT_BOTTOM:
                                    alpha = 1 - motionEvent.getRawX() / (float) mScreenWidth;
                                    break;
                                case TOP_LEFT:
                                case TOP_CENTER:
                                case TOP_RIGHT:
                                    alpha = motionEvent.getRawY() / (float) mScreenHeight;
                                    break;
                                case BOTTOM_LEFT:
                                case BOTTOM_CENTER:
                                case BOTTOM_RIGHT:
                                    alpha = 1 - motionEvent.getRawY() / (float) mScreenHeight;
                                    break;
                            }
                            mWorkspaceView.setAlpha(alpha);
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                        mVelocityTracker.computeCurrentVelocity(1);
                        float velocity;
                        long duration = 0;
                        switch (mOverlayShowHandlePosition) {
                            case LEFT_TOP:
                            case LEFT_MIDDLE:
                            case LEFT_BOTTOM:
                                velocity = mVelocityTracker.getXVelocity();
                                if (velocity > 0) {
                                    duration = (long) ((mScreenWidth - motionEvent.getRawX()) / velocity);
                                }
                                break;
                            case RIGHT_TOP:
                            case RIGHT_MIDDLE:
                            case RIGHT_BOTTOM:
                                velocity = mVelocityTracker.getXVelocity();
                                if (velocity < 0) {
                                    duration = (long) (-motionEvent.getRawX() / velocity);
                                }
                                break;
                            case TOP_LEFT:
                            case TOP_CENTER:
                            case TOP_RIGHT:
                                velocity = mVelocityTracker.getYVelocity();
                                if (velocity > 0) {
                                    duration = (long) ((mScreenHeight - motionEvent.getRawY()) / velocity);
                                }
                                break;
                            case BOTTOM_LEFT:
                            case BOTTOM_CENTER:
                            case BOTTOM_RIGHT:
                                velocity = mVelocityTracker.getYVelocity();
                                if (velocity < 0) {
                                    duration = (long) (-motionEvent.getRawY() / velocity);
                                }
                                break;
                        }

                        if (duration != 0) {
                            if (duration > 400) {
                                duration = 400;
                            } else if (duration < 0) {
                                duration = 0;
                            }
                            showWorkspace(mOverlayShowHandlePosition, true, duration);
                        } else {
                            hideWorkspace(mOverlayShowHandlePosition, true, OPEN_CLOSE_ANIMATION_DURATION);
                            overlayIsHidden();
                        }

                        mVelocityTracker.recycle();
                        break;

                    case MotionEvent.ACTION_CANCEL:
                        mVelocityTracker.recycle();
                        hideWorkspace(mOverlayShowHandlePosition, true, OPEN_CLOSE_ANIMATION_DURATION);
                        overlayIsHidden();
                        break;
                }
                return true;
            }
        });

        mHideHandle.setOnTouchListener(new View.OnTouchListener() {
            VelocityTracker mVelocityTracker;

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mVelocityTracker = VelocityTracker.obtain();
                        break;

                    case MotionEvent.ACTION_MOVE:
                        mVelocityTracker.addMovement(motionEvent);
                        if (mOverlayAnimation == GlobalConfig.OverlayAnimation.SLIDE) {
                            switch (mOverlayHideHandlePosition) {
                                case LEFT_TOP:
                                case LEFT_MIDDLE:
                                case LEFT_BOTTOM:
                                    mWorkspaceView.setTranslationX(motionEvent.getRawX());
                                    break;
                                case RIGHT_TOP:
                                case RIGHT_MIDDLE:
                                case RIGHT_BOTTOM:
                                    mWorkspaceView.setTranslationX(motionEvent.getRawX() - mWorkspaceView.getWidth());
                                    break;
                                case TOP_LEFT:
                                case TOP_CENTER:
                                case TOP_RIGHT:
                                    mWorkspaceView.setTranslationY(motionEvent.getRawY());
                                    break;
                                case BOTTOM_LEFT:
                                case BOTTOM_CENTER:
                                case BOTTOM_RIGHT:
                                    mWorkspaceView.setTranslationY(motionEvent.getRawY() - mWorkspaceView.getHeight());
                                    break;
                            }
                        } else {
                            float alpha = 1;
                            switch (mOverlayHideHandlePosition) {
                                case LEFT_TOP:
                                case LEFT_MIDDLE:
                                case LEFT_BOTTOM:
                                    alpha = motionEvent.getRawX() / (float) mScreenWidth;
                                    break;
                                case RIGHT_TOP:
                                case RIGHT_MIDDLE:
                                case RIGHT_BOTTOM:
                                    alpha = 1 - motionEvent.getRawX() / (float) mScreenWidth;
                                    break;
                                case TOP_LEFT:
                                case TOP_CENTER:
                                case TOP_RIGHT:
                                    alpha = motionEvent.getRawY() / (float) mScreenHeight;
                                    break;
                                case BOTTOM_LEFT:
                                case BOTTOM_CENTER:
                                case BOTTOM_RIGHT:
                                    alpha = 1 - motionEvent.getRawY() / (float) mScreenHeight;
                                    break;
                            }
                            mWorkspaceView.setAlpha(alpha);
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                        mVelocityTracker.computeCurrentVelocity(1);
                        float velocity;
                        long duration = 0;

                        switch (mOverlayHideHandlePosition) {
                            case LEFT_TOP:
                            case LEFT_MIDDLE:
                            case LEFT_BOTTOM:
                                velocity = mVelocityTracker.getXVelocity();
                                if (velocity > 0) {
                                    duration = (long) ((mWorkspaceView.getWidth() - motionEvent.getRawX()) / velocity);
                                }
                                break;
                            case RIGHT_TOP:
                            case RIGHT_MIDDLE:
                            case RIGHT_BOTTOM:
                                velocity = mVelocityTracker.getXVelocity();
                                if (velocity < 0) {
                                    duration = (long) (-motionEvent.getRawX() / velocity);
                                }
                                break;
                            case TOP_LEFT:
                            case TOP_CENTER:
                            case TOP_RIGHT:
                                velocity = mVelocityTracker.getYVelocity();
                                if (velocity > 0) {
                                    duration = (long) ((mWorkspaceView.getHeight() - motionEvent.getRawY()) / velocity);
                                }
                                break;
                            case BOTTOM_LEFT:
                            case BOTTOM_CENTER:
                            case BOTTOM_RIGHT:
                                velocity = mVelocityTracker.getYVelocity();
                                if (velocity < 0) {
                                    duration = (long) (-motionEvent.getRawY() / velocity);
                                }
                                break;
                        }

                        if (duration != 0) {
                            if (duration > 400) {
                                duration = 400;
                            } else if (duration < 0) {
                                duration = 0;
                            }
                            hideWorkspace(mOverlayHideHandlePosition, true, duration);
                            overlayIsHidden();
                        } else {
                            showWorkspace(mOverlayHideHandlePosition, true, OPEN_CLOSE_ANIMATION_DURATION);
                        }

                        mVelocityTracker.recycle();
                        break;

                    case MotionEvent.ACTION_CANCEL:
                        mVelocityTracker.recycle();
                        showWorkspace(mOverlayHideHandlePosition, true, OPEN_CLOSE_ANIMATION_DURATION);
                        break;
                }
                return true;
            }
        });
    }

    private void configureScreen() {
        Point size = new Point();
        mWindowManager.getDefaultDisplay().getSize(size);
        mScreenWidth = size.x;
        mScreenHeight = size.y;

        Resources res = getResources();
        int sb_height = 0;
        int resourceId = res.getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            sb_height = res.getDimensionPixelSize(resourceId);
        }
        mScreenHeight -= sb_height;

        mWorkspaceView.setLayoutParams(new FrameLayout.LayoutParams(mScreenWidth, mScreenHeight));

        mWorkspaceView.setTranslationX(0);
        mWorkspaceView.setTranslationY(0);
        if(mOverlayAnimation == GlobalConfig.OverlayAnimation.SLIDE) {
            boolean visible = mWorkspaceView.getVisibility() == View.VISIBLE;
            switch (mOverlayShowHandlePosition) {
                // FIXME I don't understand why X and Y are inverted, as if not everything was rotated at this time
                case LEFT_TOP: case LEFT_MIDDLE: case LEFT_BOTTOM:
                    mWorkspaceView.setTranslationX(visible ? 0 : -mScreenWidth);
                    break;
                case RIGHT_TOP: case RIGHT_MIDDLE: case RIGHT_BOTTOM:
                    mWorkspaceView.setTranslationX(visible ? 0 : mScreenWidth);
                    break;
                case TOP_LEFT: case TOP_CENTER: case TOP_RIGHT:
                    mWorkspaceView.setTranslationY(visible ? 0 : -mScreenHeight);
                    break;
                case BOTTOM_LEFT: case BOTTOM_CENTER: case BOTTOM_RIGHT:
                    mWorkspaceView.setTranslationY(visible ? 0 : mScreenHeight);
                    break;
            }
        }
    }

    private ViewPropertyAnimator createDrawerAnimation(GlobalConfig.OverlayHandlePosition from, boolean show, long duration) {
        ViewPropertyAnimator animator = mWorkspaceView.animate();
        if(show) {
            if(mOverlayAnimation == GlobalConfig.OverlayAnimation.FADE) {
                animator.alpha(1);
            } else {
                switch (from) {
                    case LEFT_TOP: case LEFT_MIDDLE: case LEFT_BOTTOM:
                    case RIGHT_TOP: case RIGHT_MIDDLE: case RIGHT_BOTTOM:
                        animator.translationX(0);
                        break;

                    case TOP_LEFT: case TOP_CENTER: case TOP_RIGHT:
                    case BOTTOM_LEFT: case BOTTOM_CENTER: case BOTTOM_RIGHT:
                        animator.translationY(0);
                        break;
                }
            }
        } else {
            if(mOverlayAnimation == GlobalConfig.OverlayAnimation.FADE) {
                animator.alpha(0);
            } else {
                boolean is_show_handle = from == mOverlayShowHandlePosition;
                switch (from) {
                    case LEFT_TOP: case LEFT_MIDDLE: case LEFT_BOTTOM:
                        animator.translationX(is_show_handle ? -mScreenWidth-1 : mScreenWidth+1);
                        break;

                    case RIGHT_TOP: case RIGHT_MIDDLE: case RIGHT_BOTTOM:
                        animator.translationX(is_show_handle ? mScreenWidth+1 : -mScreenWidth-1);
                        break;

                    case TOP_LEFT: case TOP_CENTER: case TOP_RIGHT:
                        animator.translationY(is_show_handle ? -mScreenHeight-1 : mScreenHeight+1);
                        break;

                    case BOTTOM_LEFT: case BOTTOM_CENTER: case BOTTOM_RIGHT:
                        animator.translationY(is_show_handle ? mScreenHeight+1 : -mScreenHeight-1);
                        break;
                }
            }
        }
        animator.setDuration(duration);
        animator.setInterpolator(new DecelerateInterpolator());
        return animator;
    }

    private void overlayIsShown() {
        mIsShown = true;
        mScreen.resume();
        mScreen.setHasWindowFocus(true);
    }

    private void overlayIsHidden() {
        mIsShown = false;
        mScreen.setHasWindowFocus(false);
        mScreen.pause();
    }

    protected void showWorkspace(GlobalConfig.OverlayHandlePosition from, boolean animate, long duration) {
        mShowHandle.setVisibility(View.GONE);
        mHideHandle.setVisibility(View.VISIBLE);
        mWorkspaceView.setVisibility(View.VISIBLE);
        mWallpaperView.setVisibility(View.VISIBLE);

        createDrawerAnimation(from, true, duration).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                mWorkspaceView.setTranslationX(0);

                WindowManager.LayoutParams lp = ((WindowManager.LayoutParams)mWindow.getDecorView().getLayoutParams());
                lp.flags = 0;
                mWindowManager.updateViewLayout(mWindow.getDecorView(), lp);

                mContentView.requestFocus();
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        }).start();
    }

    protected void hideWorkspace(GlobalConfig.OverlayHandlePosition from, boolean animate, long duration) {
        createDrawerAnimation(from, false, duration).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                mWorkspaceView.setVisibility(View.GONE);
                mWallpaperView.setVisibility(View.GONE);
                mShowHandle.setVisibility(View.VISIBLE);
                mHideHandle.setVisibility(View.GONE);

                WindowManager.LayoutParams lp = ((WindowManager.LayoutParams)mWindow.getDecorView().getLayoutParams());
                lp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
                mWindowManager.updateViewLayout(mWindow.getDecorView(), lp);

                mContentView.clearFocus();
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        }).start();
    }

    protected void showWorkspace() {
        if(!mIsShown) {
            overlayIsShown();
            showWorkspace(mOverlayShowHandlePosition, true, OPEN_CLOSE_ANIMATION_DURATION);
        }
    }

    protected void hideWorkspace() {
        if(mIsShown) {
            overlayIsHidden();
            hideWorkspace(mOverlayHideHandlePosition, true, OPEN_CLOSE_ANIMATION_DURATION);
        }
    }

    private class WindowScreen extends Screen {
        public WindowScreen(Context context, int content_view) {
            super(context, content_view);
        }

        @Override
        public Identity getIdentity() {
            return Identity.FLOATING;
        }

        @Override
        protected Resources getRealResources() {
            return WindowService.this.getRealResources();
        }

        @Override
        public ItemLayout loadRootItemLayout(int page, boolean reset_navigation_history, boolean displayImmediately, boolean animate) {
            if(mItemLayout.getPage().id == page) {
                return mItemLayout;
            } else {
                return loadRootItemLayoutOffscreen(page, reset_navigation_history, displayImmediately, animate);
            }
        }

        @Override
        public ItemLayout getCurrentRootItemLayout() {
            return mItemLayout;
        }

        @Override
        protected void launchIntent(Intent intent, ItemView itemView) {
            super.launchIntent(intent, itemView);
            if(LLApp.get().getAppEngine().getGlobalConfig().overlayLaunchHide) {
                hideWorkspace();
            }

        }
    }
}
