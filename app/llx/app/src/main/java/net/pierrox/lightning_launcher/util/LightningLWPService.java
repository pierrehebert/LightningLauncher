package net.pierrox.lightning_launcher.util;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.IBinder;
import android.service.wallpaper.WallpaperService;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import net.pierrox.lightning_launcher.LLApp;
import net.pierrox.lightning_launcher.LLAppExtreme;
import net.pierrox.lightning_launcher.LWPSettings;
import net.pierrox.lightning_launcher.activities.ResourcesWrapperHelper;
import net.pierrox.lightning_launcher.api.ScreenIdentity;
import net.pierrox.lightning_launcher.configuration.GlobalConfig;
import net.pierrox.lightning_launcher.data.EventAction;
import net.pierrox.lightning_launcher.data.Page;
import net.pierrox.lightning_launcher.engine.LightningEngine;
import net.pierrox.lightning_launcher.engine.Screen;
import net.pierrox.lightning_launcher.views.EventFrameLayout;
import net.pierrox.lightning_launcher.views.ItemLayout;
import net.pierrox.lightning_launcher.views.item.ItemView;
import net.pierrox.lightning_launcher_extreme.R;

import java.lang.reflect.Field;

public class LightningLWPService extends WallpaperService {

    private ResourcesWrapperHelper mResourcesWrapperHelper;

    @Override
    public Engine onCreateEngine() {
        return new LightningLWPEngine();
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


    private class LightningLWPEngine extends Engine implements LightningEngine.GlobalConfigListener {
        private EventFrameLayout mContentView;
        private TextView mNoDesktopView;
        private LightningLWPScreen mScreen;
        private int mLWPDesktopId;
        private ItemLayout mItemLayout;
        private Page mMainPage;

        private int mScreenWidth;
        private int mScreenHeight;
        private boolean mVisible = true;

        private boolean mUseWindow;

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            LLApp app = LLApp.get();

            LightningEngine lightningEngine = app.getAppEngine();
            lightningEngine.registerGlobalConfigChangeListener(this);
            final GlobalConfig globalConfig = lightningEngine.getGlobalConfig();
            mLWPDesktopId =globalConfig.lwpScreen;

            mScreen = new LightningLWPScreen(LightningLWPService.this, R.layout.lwp);

            mContentView = (EventFrameLayout) mScreen.getContentView();
            mItemLayout = (ItemLayout) mContentView.findViewById(R.id.lwp_il);
            mScreen.takeItemLayoutOwnership(mItemLayout);

            mNoDesktopView = (TextView) mContentView.findViewById(R.id.empty);
            mNoDesktopView.setText(R.string.sd);
            mNoDesktopView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startSettings();
                }
            });

            try {
                IBinder token = null;
                final Field[] declaredFields = getClass().getSuperclass().getDeclaredFields();
                for (Field f : declaredFields) {
                    if(f.getName().equals("mWindowToken")) {
                        f.setAccessible(true);
                        token = (IBinder) f.get(this);
                        break;
                    }
                }

                if(token != null) {
                    WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
                    WindowManager.LayoutParams attrs = new WindowManager.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                            WindowManager.LayoutParams.TYPE_WALLPAPER,
                            0,
                            PixelFormat.TRANSLUCENT);
                    attrs.token = token;

                    wm.addView(mContentView, attrs);
//                    final Class<?> wmgc = getClassLoader().loadClass("android.view.WindowManagerGlobal");
//                    final Method getInstance = wmgc.getMethod("getInstance");
//                    Object wmg = getInstance.invoke(null);
//
//                    final Method addView = wmgc.getMethod("addView", View.class, ViewGroup.LayoutParams.class, Display.class, Window.class);
//                    addView.invoke(wmg, mContentView, attrs, wm.getDefaultDisplay(), null);

                    mUseWindow = true;
                }
            } catch (Exception e) {
                // pass
            }

            configurePage();
        }

        @Override
        public void onDestroy() {
            final LLApp app = LLApp.get();

            app.getAppEngine().unregisterGlobalConfigChangeListener(this);

            mScreen.destroy();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            setVisibility(visible);
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            setVisibility(false);
        }

        @Override
        public void onSurfaceChanged(final SurfaceHolder holder, int format, int width, int height) {
            mScreenWidth = width;
            mScreenHeight = height;

            mScreen.setCustomScreenSize(width, height);

            if(!mUseWindow) {
                layout();
                draw();
            }
        }

        @Override
        public void onSurfaceRedrawNeeded(SurfaceHolder holder) {
            if(!mUseWindow) {
                draw();
            }
        }

        @Override
        public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep, float yOffsetStep, int xPixelOffset, int yPixelOffset) {
            Rect boundingBox = mItemLayout.getItemsBoundingBox();
            float x = xOffset*(mItemLayout.getWidth()-boundingBox.width()) - boundingBox.left;
            float y = yOffset*(mItemLayout.getHeight()-boundingBox.height()) - boundingBox.top;
            if(mItemLayout.getCurrentX() != x || mItemLayout.getCurrentY() != y) {
                mItemLayout.moveTo(x, y, 1);
            }
        }

        @Override
        public Bundle onCommand(String action, int x, int y, int z, Bundle extras, boolean resultRequested) {
            boolean isTap = WallpaperManager.COMMAND_TAP.equals(action);
            boolean isSecondaryTap = WallpaperManager.COMMAND_SECONDARY_TAP.equals(action);
            if((isTap || isSecondaryTap) && !mMainPage.config.lwpStdEvents) {
                int[] offset = new int[2];
                mContentView.getLocationOnScreen(offset);
                x -= offset[0];
                y -= offset[1];

                if(mUseWindow) {
                    boolean dispatch = mContentView.getDispatchEvent();
                    mContentView.setDispatchEvent(true);

                    long now = System.currentTimeMillis();
                    MotionEvent event;
                    event = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, x, y, 0);
                    mContentView.dispatchTouchEvent(event);
                    event = MotionEvent.obtain(now, now + (isSecondaryTap ? ViewConfiguration.getLongPressTimeout() : 1), MotionEvent.ACTION_UP, x, y, 0);
                    mContentView.dispatchTouchEvent(event);

                    mContentView.setDispatchEvent(dispatch);
                } else if(mLWPDesktopId == Page.NONE){
                    // preview mode, events won't perform click because the view needs to be attached to post click events, hence handle it manually
                    Rect r = new Rect();
                    mNoDesktopView.getHitRect(r);
                    if(r.contains(x, y)) {
                        startSettings();
                    }
                }
            }
            return null;
        }

        private void startSettings() {
            final Intent intent = new Intent(LightningLWPService.this, LWPSettings.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }

        private void layout() {
            mContentView.measure(View.MeasureSpec.makeMeasureSpec(mScreenWidth, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(mScreenHeight, View.MeasureSpec.EXACTLY));
            mContentView.layout(0, 0, mScreenWidth, mScreenHeight);
        }

        private void draw() {
            SurfaceHolder holder = getSurfaceHolder();
            Canvas canvas = null;
            try {
                canvas = holder.lockCanvas();
                if (canvas != null) {
                    canvas.clipRect(0, 0, mScreenWidth, mScreenHeight);
                    canvas.drawColor(0, PorterDuff.Mode.SRC);
                    mContentView.draw(canvas);
                }
            } finally {
                if (canvas != null)
                    holder.unlockCanvasAndPost(canvas);
            }
        }

        private void setVisibility(boolean visible) {
            if(visible != mVisible) {
                mVisible = visible;
                if(mVisible) {
                    mScreen.resume();
                } else {
                    mScreen.pause();
                }
            }
        }

        private void configurePage() {
            if(!((LLAppExtreme)LLApp.get()).hasLWP()) {
                mLWPDesktopId = Page.NONE;
            }
            boolean hasPage = mLWPDesktopId != Page.NONE;
            mContentView.findViewById(R.id.empty_c).setVisibility(hasPage ? View.GONE : View.VISIBLE);
            LightningEngine lightningEngine = LLApp.get().getAppEngine();
            mMainPage = lightningEngine.getOrLoadPage(mLWPDesktopId);
            mItemLayout.setPage(mMainPage);
            mItemLayout.moveTo(0, 0, 1);
//            mItemLayout.enforceScollingDirection(PageConfig.ScrollingDirection.NONE);
            mContentView.setDispatchEvent(mMainPage.config.lwpStdEvents);
            mScreen.configureBackground(mMainPage);
        }

        @Override
        public void onGlobalConfigChanged(GlobalConfig newGlobalConfig) {
            if(newGlobalConfig.lwpScreen != mLWPDesktopId) {
                mLWPDesktopId = newGlobalConfig.lwpScreen;
                configurePage();

                if(!mUseWindow) {
                    layout();
                    draw();
                }
            }
        }

        private class LightningLWPScreen extends Screen {
            public LightningLWPScreen(Context context, int content_view) {
                super(context, content_view);
            }

            @Override
            public ScreenIdentity getIdentity() {
                return ScreenIdentity.LIVE_WALLPAPER;
            }

            @Override
            protected Resources getRealResources() {
                return LightningLWPService.this.getRealResources();
            }

            @Override
            public void goToDesktopPosition(int page, float x, float y, float s, boolean animate, boolean absolute) {
                if(Page.isDashboard(page) && page != getCurrentRootPage().id) {
                    LightningEngine engine = mMainPage.getEngine();
                    engine.getGlobalConfig().lwpScreen = page;
                    engine.notifyGlobalConfigChanged();
                    ItemLayout il = loadRootItemLayout(page, false, true, animate);
                    goToItemLayoutPosition(il, x, y, s, animate, absolute);
                } else {
                    ItemLayout[] itemLayouts = getItemLayoutsForPage(page);
                    for (ItemLayout il : itemLayouts) {
                        goToItemLayoutPosition(il, x, y, s, animate, absolute);
                    }
                }
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
            public boolean runAction(LightningEngine engine, String source, EventAction ea, ItemLayout il, ItemView itemView) {
                switch (ea.action) {
                    case GlobalConfig.WALLPAPER_TAP:
                    case GlobalConfig.WALLPAPER_SECONDARY_TAP:
                        // skip these actions
                        break;

                    case GlobalConfig.PREVIOUS_DESKTOP:
                        gotoPage(PAGE_DIRECTION_HINT_BACKWARD);
                        break;

                    case GlobalConfig.NEXT_DESKTOP:
                        gotoPage(PAGE_DIRECTION_HINT_FORWARD);
                        break;

                    default:
                        return super.runAction(engine, source, ea, il, itemView);
                }

                processNextAction(engine, source, ea, il, itemView);
                return true;
            }

            @Override
            public void onPageModified(Page page) {
                super.onPageModified(page);

                if(page == mMainPage) {
                    mContentView.setDispatchEvent(page.config.lwpStdEvents);
                }
            }

            @Override
            public void setVisibility(boolean visible) {
                int oldVisibility = mContentView.getVisibility();
                int newVisibility = visible ? View.VISIBLE : View.GONE;
                if(oldVisibility != newVisibility) {
                    mContentView.setVisibility(newVisibility);
                    mContentView.startAnimation(AnimationUtils.loadAnimation(LightningLWPService.this, visible ? android.R.anim.fade_in : android.R.anim.fade_out));
                }
            }

            private void gotoPage(int directionHint) {
                LightningEngine engine = LLApp.get().getAppEngine();
                GlobalConfig globalConfig = engine.getGlobalConfig();
                int nextPage = getNextPage(globalConfig, directionHint);
                globalConfig.lwpScreen = nextPage;
                engine.notifyGlobalConfigChanged();
            }
        }
    }
}
