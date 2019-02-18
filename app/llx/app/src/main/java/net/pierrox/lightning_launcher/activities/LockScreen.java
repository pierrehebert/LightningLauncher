package net.pierrox.lightning_launcher.activities;


import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import net.pierrox.lightning_launcher.LLApp;
import net.pierrox.lightning_launcher.api.ScreenIdentity;
import net.pierrox.lightning_launcher.configuration.GlobalConfig;
import net.pierrox.lightning_launcher.data.EventAction;
import net.pierrox.lightning_launcher.data.Item;
import net.pierrox.lightning_launcher.data.Page;
import net.pierrox.lightning_launcher.engine.LightningEngine;
import net.pierrox.lightning_launcher.engine.Screen;
import net.pierrox.lightning_launcher.views.FolderView;
import net.pierrox.lightning_launcher.views.ItemLayout;
import net.pierrox.lightning_launcher.views.item.ItemView;
import net.pierrox.lightning_launcher_extreme.R;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Stack;

public class LockScreen extends Dashboard {
	private static final int DIALOG_UNLOCK = 1;
	
    public static LockScreen sThis;
    public static Integer sMyTaskId;

    private ItemLayout mItemLayout;
    private Page mLockScreenPage;
    private boolean mNeedDashboardRestart;
    private Integer mLastTaskId;
    private boolean mRestorePreviousTask;

    private ActivityManager mActivityManager;
    private Method mMoveTaskToFront;

    @Override
    protected void createActivity(Bundle savedInstanceState) {
        try {
            mMoveTaskToFront = ActivityManager.class.getMethod("moveTaskToFront", int.class, int.class);
            mActivityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
            List<ActivityManager.RunningTaskInfo> tasks = mActivityManager.getRunningTasks(2);
            int size = tasks.size();
            if(size > 0) {
                sMyTaskId = tasks.get(0).id;
            }
            if(size == 2) {
                mLastTaskId = tasks.get(1).id;
            } else {
                mLastTaskId = null;
            }
        } catch (NoSuchMethodException e) {
            mMoveTaskToFront = null;
        }

        mNeedDashboardRestart = false;

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

        View decor_view = window.getDecorView();
        try {
            Method getSystemUiVisibility = decor_view.getClass().getMethod("getSystemUiVisibility");
            int newUiOptions = (Integer) getSystemUiVisibility.invoke(decor_view);

//            // Navigation bar hiding:  Backwards compatible to ICS.
//            if (Build.VERSION.SDK_INT >= 14) {
//                newUiOptions |= View.SYSTEM_UI_FLAG_LOW_PROFILE;
//            }

            if (Build.VERSION.SDK_INT >= 18) {
                newUiOptions |= 0x00001000; //View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            }

            Method setSystemUiVisibility = decor_view.getClass().getMethod("setSystemUiVisibility", int.class);
            setSystemUiVisibility.invoke(decor_view, newUiOptions);
        } catch (Exception e) {
            // pass, not supported
        }




        try {
            Method getActionBar=getClass().getMethod("getActionBar");
            Object action_bar=getActionBar.invoke(this, (Object[])null);
            action_bar.getClass().getMethod("hide").invoke(action_bar, (Object[])null);
        } catch(Exception e) {
            // pass, API level 11
        }



        mNavigationStack=new Stack<Integer>();

        LLApp llApp = LLApp.get();
        LightningEngine engine = llApp.getAppEngine();
        GlobalConfig globalConfig = engine.getGlobalConfig();
        int lockScreen = globalConfig.lockScreen;
        mLockScreenPage = engine.getOrLoadPage(lockScreen);

        mItemLayout=(ItemLayout)findViewById(R.id.drawer_il);
        mScreen.takeItemLayoutOwnership(mItemLayout);
        mItemLayout.setHonourFocusChange(false);
        mItemLayout.setPage(mLockScreenPage);
        configureActivity(mLockScreenPage);

        if(globalConfig.overlayScreen != Page.NONE && globalConfig.lockDisableOverlay) {
            stopService(llApp.getWindowServiceIntent());
        }

        sThis = this;
    }

    @Override
    protected Screen createScreen() {
        return new LockScreen1(this, R.layout.lockscreen);
    }

    @Override
    protected void destroyActivity() {
        sThis = null;
        sMyTaskId = null;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            mScreen.zoomToOrigin(mScreen.getTargetOrTopmostItemLayout());
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        mNeedDashboardRestart = true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        executeNewIntent(intent);
    }

    @Override
    protected void openBubble(int mode, ItemLayout itemLayout, ItemView itemView, Rect focus, List shortcuts) {
        // pass, disable super
    }

    @Override
    public void enterEditMode(ItemLayout il, Item selected_item) {
        // pass, disable super
    }

    @SuppressLint("NewApi")
    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog d = new Dialog(this, android.R.style.Theme_InputMethod);
        d.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        d.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                finish();
                try { removeDialog(DIALOG_UNLOCK); } catch(Exception e) {}
                if(mRestorePreviousTask) {
                    if(mLastTaskId != null) {
                        try {
                            mMoveTaskToFront.invoke(mActivityManager, mLastTaskId, 0);
                            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        if(mNeedDashboardRestart) {
                            Intent intent = new Intent(LockScreen.this, Dashboard.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        }
                    }
                }
            }
        });
        return d;
    }



    public void unlock(boolean restore_previous_task) {
        mRestorePreviousTask = restore_previous_task;
        try { showDialog(DIALOG_UNLOCK); } catch(Exception e) {}
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        LLApp llApp = LLApp.get();
        GlobalConfig globalConfig = llApp.getAppEngine().getGlobalConfig();
        if(globalConfig.overlayScreen != Page.NONE && globalConfig.lockDisableOverlay) {
            startService(llApp.getWindowServiceIntent());
        }
        Intent unlocked = new Intent(Dashboard.BROADCAST_ACTION_UNLOCKED);
        sendBroadcast(unlocked);
    }


    // FIXME: find a name that doesn't clash with the activity name
    private class LockScreen1 extends DashboardScreen {

        public LockScreen1(Context context, int content_view) {
            super(context, content_view);
        }

        @Override
        public ScreenIdentity getIdentity() {
            return ScreenIdentity.LOCK;
        }

        @Override
        public void onWidgetClicked() {
            if(mGlobalConfig.launchUnlock) {
                mItemLayout.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        unlock(false);
                    }
                }, 2000);
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
        public void setActivePage(int page) {
            // pass, no-op
        }

        @Override
        public void onItemViewAction(ItemView itemView, int action) {
            unlock(true);
        }

        @Override
        protected FolderView openUserMenu(boolean prepareOnly) {
            return null;
        }

        @Override
        public void showAddItemDialog(ItemLayout il) {
            // pass
        }

        @Override
        protected void launchIntent(Intent intent, ItemView itemView) {
            super.launchIntent(intent, itemView);
            if(mGlobalConfig.launchUnlock) {
                unlock(false);
            }
        }

        @Override
        public boolean runAction(LightningEngine engine, String source, EventAction ea, ItemLayout il, ItemView itemView) {
            switch (ea.action) {
                case GlobalConfig.UNLOCK_SCREEN:
                    if (itemView != null) {
                        itemView.setHighlightedNow(false);
                    }
                    unlock(true);
                    break;
                case GlobalConfig.OPEN_HIERARCHY_SCREEN:
                    // pass
                    break;
                case GlobalConfig.GO_HOME_ZOOM_TO_ORIGIN:
                    zoomToOrigin(getCurrentRootItemLayout());
                    break;
                default:
                    return super.runAction(engine, source, ea, il, itemView);
            }

            processNextAction(engine, source, ea, il, itemView);
            return true;
        }
    }

}