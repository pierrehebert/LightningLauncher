package net.pierrox.lightning_launcher;

import android.app.Application;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.view.ViewGroup;

import net.pierrox.lightning_launcher.activities.ResourcesWrapperHelper;
import net.pierrox.lightning_launcher.configuration.SystemConfig;
import net.pierrox.lightning_launcher.data.FileUtils;
import net.pierrox.lightning_launcher.data.Item;
import net.pierrox.lightning_launcher.data.JsonLoader;
import net.pierrox.lightning_launcher.data.Page;
import net.pierrox.lightning_launcher.data.Utils;
import net.pierrox.lightning_launcher.engine.LightningEngine;
import net.pierrox.lightning_launcher.engine.Screen;
import net.pierrox.lightning_launcher.script.api.Property;
import net.pierrox.lightning_launcher.views.MyAppWidgetHostView;
import net.pierrox.lightning_launcher.views.NativeImage;
import net.pierrox.lightning_launcher.views.SharedAsyncGraphicsDrawable;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class LLApp extends Application {
    public interface SystemConfigListener {
        void onSystemConfigChanged(SystemConfig newSystemConfig);
    }


    public static final String LL_PKG_NAME="net.pierrox.lightning_launcher";
	public static final String LLX_PKG_NAME="net.pierrox.lightning_launcher_extreme";
    public static final String LKP_PKG_NAME="net.pierrox.lightning_locker_p";
	public static final String INTENT_ITEM_ACTION=LL_PKG_NAME+".ITEM_ACTION";

    private static final int SYSTEM_CONFIG_FILE_VERSION = 1;

    private static LLApp sThis;

    protected LightningEngine mAppEngine;
    private HashMap<String, LightningEngine> mLightningEngines = new HashMap<>();

    private MyAppWidgetHost mAppWidgetHost;
    protected SystemConfig mSystemConfig;

	private ArrayList<SystemConfigListener> mSystemConfigListeners;
    private ResourcesWrapperHelper mResourcesWrapperHelper;

	private String mLanguage;

    private int mActiveDashboardPage;


    private Typeface mIconsTypeface;

    private int mWidgetHostStartListeningCount;

    private ArrayList<Screen> mScreens = new ArrayList<>();
    protected Screen mBackgroundScreen;

	@Override
	public void onCreate() {
		super.onCreate();

        sThis = this;

		NativeImage.init(this);

        Utils.deleteDirectory(FileUtils.getCachedDrawablesDir(this), false);

        mAppWidgetHost=new MyAppWidgetHost(this, 1968);
        try {
            mAppWidgetHost.startListening();
        } catch (Throwable e) {
            // pass
        }

        mSystemConfigListeners=new ArrayList<>(10);

        loadSystemConfig();

        migrate();

		Utils.retrieveStandardIcon(this);

        SharedAsyncGraphicsDrawable.setPoolSize((long) (mSystemConfig.imagePoolSize * Runtime.getRuntime().maxMemory()));

//        Utils.setTheme(this, Utils.APP_THEME);
		

		if(mSystemConfig.language != null) {
            mLanguage = mSystemConfig.language;
		} else {
            List<ResolveInfo> r=getPackageManager().queryIntentActivities(new Intent("net.pierrox.lightning_launcher.lp.ENUMERATE"), 0);
            mLanguage = null;
            for(ResolveInfo ri : r) {
                String p = ri.activityInfo.packageName;
                if(!p.equals(LL_PKG_NAME) && !p.equals(LLX_PKG_NAME)) {
                    mLanguage = p;
                    break;
                }
            }
		}

        if(getPackageName().equals(mLanguage)) {
            mLanguage = null;
        }

        mResourcesWrapperHelper = new ResourcesWrapperHelper(this, super.getResources());

        Resources resources = mResourcesWrapperHelper.getResources();
        Property.buildLists(resources);
        Item.loadItemNames(resources);

        mAppEngine = getEngine(getFilesDir(), true); //new File("/sdcard/LightningLauncher/setup");

        mBackgroundScreen = new BackgroundScreen(this, 0);

        mAppEngine.getOrLoadPage(Page.APP_DRAWER_PAGE);

        IntentFilter intent_filter=new IntentFilter();
        intent_filter.addAction(Intent.ACTION_SCREEN_OFF);
        intent_filter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(mBroadcastReceiver, intent_filter);

        mBackgroundScreen.runAction(mAppEngine, "STARTUP", mAppEngine.getGlobalConfig().startup);
    }
	
	@Override
	public void onTerminate() {
		super.onTerminate();

        unregisterReceiver(mBroadcastReceiver);

        for (LightningEngine engine : mLightningEngines.values()) {
            engine.terminate();
            engine.saveData();
        }

        saveSystemConfig();

        mBackgroundScreen.destroy();

        sThis = null;
    }

    public void saveAllData() {
        for (LightningEngine engine : mLightningEngines.values()) {
            engine.saveData();
        }
        saveSystemConfig();
    }

	@Override
	public Resources getResources() {
		return mResourcesWrapperHelper == null ? super.getResources() : mResourcesWrapperHelper.getResources();
	}

    public SystemConfig getSystemConfig() {
        return mSystemConfig;
    }

    public LightningEngine getAppEngine() {
        // this could be getEngine(mAppBaseDir) as well
        return mAppEngine;
    }

    public LightningEngine getEngine(File baseDir, boolean doInit) {
        String key = baseDir.getAbsolutePath();
        LightningEngine engine = mLightningEngines.get(key);
        if(engine == null) {
            engine = new LightningEngine(this, baseDir);
            if(doInit) {
                engine.init();
            }
            mLightningEngines.put(key, engine);
        }

        return engine;
    }

	public void notifySystemConfigChanged() {
		for(SystemConfigListener l : mSystemConfigListeners) {
			l.onSystemConfigChanged(mSystemConfig);
		}
	}

    public void saveSystemConfig() {
        mSystemConfig.version = SYSTEM_CONFIG_FILE_VERSION;
        JsonLoader.saveObjectToFile(mSystemConfig, FileUtils.getSystemConfigFile(this));
    }
	
	public void registerSystemConfigListener(SystemConfigListener l) {
        mSystemConfigListeners.add(l);
	}
	
	public void unregisterSystemConfigListener(SystemConfigListener l) {
        mSystemConfigListeners.remove(l);
	}
	
	public static LLApp get() {
		return sThis;
	}
	
	public MyAppWidgetHost getAppWidgetHost() {
        return mAppWidgetHost;
	}

    public void appWidgetHostStartListening() {
        if(mWidgetHostStartListeningCount == 0) {
            mAppWidgetHost.startListening();
        }
        mWidgetHostStartListeningCount++;
    }

    public void appWidgetHostStopListening() {
        mWidgetHostStartListeningCount--;
        if(mWidgetHostStartListeningCount == 0) {
            mAppWidgetHost.stopListening();
        }
    }

    public abstract void displayPagerPage(int page, boolean reset_navigation_history);

    public void setActiveDashboardPage(int page) {
        mActiveDashboardPage = page;
    }

    public int getActiveDashboardPage() {
        return mActiveDashboardPage;
    }
	
	protected void loadSystemConfig() {
		mSystemConfig = JsonLoader.readObject(SystemConfig.class, FileUtils.getSystemConfigFile(this));
	}

    public void onScreenCreated(Screen screen) {
        int index;
        if(screen.getIdentity() == Screen.Identity.LIVE_WALLPAPER) {
            // never put the live wallpaper at the top of the stack, always the bottom
            index = 0;
        } else {
            index = mScreens.size();
        }
        mScreens.add(index, screen);
    }

    public void onScreenDestroyed(Screen screen) {
        mScreens.remove(screen);
    }

    public void onScreenResumed(Screen screen) {
        // never bring the LWP to the top of the stack
        if(screen.getIdentity() != Screen.Identity.LIVE_WALLPAPER && mScreens.size() > 1) {
            mScreens.remove(screen);
            mScreens.add(screen);
        }
    }

    public void onScreenPaused(Screen screen) {
        // the LWP is always at the bottom of the stack
        if(screen.getIdentity() != Screen.Identity.LIVE_WALLPAPER) {
            int size = mScreens.size();
            if (size > 2) {
                mScreens.remove(screen);
                mScreens.add(size - 1, screen);
            }
        }
    }

    public Screen getScreen(Screen.Identity identity) {
        for (Screen screen : mScreens) {
            if(screen.getIdentity() == identity) {
                return screen;
            }
        }
        return null;
    }

    public ArrayList<Screen> getScreens() {
        return mScreens;
    }

    public Screen getActiveScreen() {
        int l = mScreens.size();
        return l==0 ? null : mScreens.get(l-1);
    }

    public Typeface getIconsTypeface() {
        if(mIconsTypeface == null) {
            mIconsTypeface = Typeface.createFromAsset(getAssets(), "icons.ttf");
        }
        return mIconsTypeface;
    }


    public String getLanguage() {
        return mLanguage;
    }

    public abstract Intent getLockscreenServiceIntent();

    public abstract Intent getWindowServiceIntent();

    public abstract boolean isFreeVersion();

    public abstract boolean isTrialVersion();

    public abstract boolean isTrialVersionExpired();

    public abstract long getTrialLeft();

    public abstract void showFeatureLockedDialog(Context context);

    public abstract void startUnlockProcess(Context context);

    public abstract void installPromotionalIcons(Page dashboard);

    public abstract void checkLicense();

    public abstract boolean hasScriptEditor();

    public abstract void startScriptEditor(int script_id, int line);

    public abstract boolean isLockScreenLocked();

    public abstract void unlockLockScreen(boolean restore_previous_task);

    public abstract Class<?> getActivityClassForScriptExecutionTarget(int target);

    public abstract void restart(boolean relaunch);

    /**
     * Return true if this intent can be directly handled by the engine (lightning actions, bookmarks, etc)
     */
    public abstract boolean isLightningIntent(Intent intent);

//    private void listAllAppWidgets() {
//        PackageManager pm = getPackageManager();
//        AppWidgetManager awm = (AppWidgetManager) getSystemService(APPWIDGET_SERVICE);
//        try {
//            Method m = mAppWidgetHost.getClass().getMethod("getAppWidgetIds");
//            int[] ids = (int[]) m.invoke(mAppWidgetHost);
//            for(int i=0; i<ids.length; i++) {
//                Log.i("XXX", "id " + ids[i]);
//                AppWidgetProviderInfo infos = awm.getAppWidgetInfo(ids[i]);
//                Log.i("XXX", "  "+infos.loadLabel(pm));
//                Log.i("XXX", "  "+infos.loadLabel(pm));
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    private void migrate() {
        // migrate fields that were previously in the global config object
        if(mSystemConfig.version == 0) {
            File from = FileUtils.getGlobalConfigFile(getFilesDir());
            JSONObject json = FileUtils.readJSONObjectFromFile(from);

            if(json != null) {
                mSystemConfig.autoEdit = json.optBoolean("autoEdit", false);
                mSystemConfig.alwaysShowStopPoints = json.optBoolean("alwaysShowStopPoints", false);
                mSystemConfig.keepInMemory = json.optBoolean("keepInMemory", true);
                mSystemConfig.language = json.optString("language", null);
                mSystemConfig.expertMode = json.optBoolean("expertMode", false);
                mSystemConfig.hotwords = json.optBoolean("hotwords", false);
                if(json.has("appStyle")) mSystemConfig.appStyle = SystemConfig.AppStyle.valueOf(json.optString("appStyle"));
                mSystemConfig.hints = json.optInt("hints", 0);
                if(json.optBoolean("showHelpHint", true)) mSystemConfig.hints |= SystemConfig.HINT_CUSTOMIZE_HELP;
                if(json.optBoolean("showRateHint", true)) mSystemConfig.hints |= SystemConfig.HINT_RATE;
                if(json.optBoolean("myDrawerHint", true)) mSystemConfig.hints |= SystemConfig.HINT_MY_DRAWER;
                mSystemConfig.imagePoolSize = (float) json.optDouble("imagePoolSize", 0);
                mSystemConfig.switches = json.optInt("switches", SystemConfig.SWITCH_SNAP|SystemConfig.SWITCH_EDIT_BARS|SystemConfig.SWITCH_CONTENT_ZOOMED|SystemConfig.SWITCH_HONOUR_PINNED_ITEMS);
                mSystemConfig.editBoxMode = json.optInt("editBoxMode", SystemConfig.EDIT_BOX_NONE);
                mSystemConfig.editBoxPropHeight = json.optInt("editBoxPropHeight", 0);
            }
        }

        // update the version number now
        if(mSystemConfig.version < SYSTEM_CONFIG_FILE_VERSION) {
            saveSystemConfig();
        }
    }

    private BroadcastReceiver mBroadcastReceiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action=intent.getAction();
            if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                Screen screen = getActiveScreen();
                if(screen != null) {
                    screen.runAction(mAppEngine, "SCREEN_OFF", mAppEngine.getGlobalConfig().screenOff);
                }
            } else if (action.equals(Intent.ACTION_SCREEN_ON)) {
                Screen screen = getActiveScreen();
                if(screen != null) {
                    screen.runAction(mAppEngine, "SCREEN_ON", mAppEngine.getGlobalConfig().screenOn);
                }
            }
        }
    };

    private class BackgroundScreen extends Screen {

        public BackgroundScreen(Context context, int content_view) {
            super(context, content_view);
        }

        @Override
        public Identity getIdentity() {
            return Identity.BACKGROUND;
        }

        @Override
        protected Resources getRealResources() {
            return LLApp.super.getResources();
        }
    }

    public static final class MyAppWidgetHost extends AppWidgetHost {

        HashMap<Integer,AppWidgetHostView> mViews = new HashMap<>();

        public MyAppWidgetHost(Context context, int hostId) {
            super(context, hostId);
        }

        public AppWidgetHostView getWidgetView(Context context, int appWidgetId, AppWidgetProviderInfo appWidget) {
            AppWidgetHostView v = mViews.get(appWidgetId);
            if(v == null) {
                v = createView(context, appWidgetId, appWidget);
                mViews.put(appWidgetId, v);
            } else {
                ViewGroup parent = (ViewGroup) v.getParent();
                parent.removeView(v);
            }

            return v;
        }

        @Override
        protected AppWidgetHostView onCreateView(Context context, int appWidgetId, AppWidgetProviderInfo appWidget) {
            return new MyAppWidgetHostView(context);
        }
    }

}
