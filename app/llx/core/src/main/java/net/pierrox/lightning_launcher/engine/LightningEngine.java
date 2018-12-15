package net.pierrox.lightning_launcher.engine;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherApps;
import android.os.Build;
import android.os.Handler;
import android.os.Process;
import android.util.Pair;
import android.util.SparseIntArray;

import net.pierrox.lightning_launcher.LLApp;
import net.pierrox.lightning_launcher.configuration.GlobalConfig;
import net.pierrox.lightning_launcher.data.ContainerPath;
import net.pierrox.lightning_launcher.data.EventAction;
import net.pierrox.lightning_launcher.data.FileUtils;
import net.pierrox.lightning_launcher.data.Folder;
import net.pierrox.lightning_launcher.data.IconPack;
import net.pierrox.lightning_launcher.data.Item;
import net.pierrox.lightning_launcher.data.JsonLoader;
import net.pierrox.lightning_launcher.data.Page;
import net.pierrox.lightning_launcher.data.PageProcessor;
import net.pierrox.lightning_launcher.data.Shortcut;
import net.pierrox.lightning_launcher.data.Utils;
import net.pierrox.lightning_launcher.engine.variable.BuiltinDataCollectors;
import net.pierrox.lightning_launcher.engine.variable.VariableManager;
import net.pierrox.lightning_launcher.overlay.WindowService;
import net.pierrox.lightning_launcher.script.ScriptExecutor;
import net.pierrox.lightning_launcher.script.ScriptManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class LightningEngine implements Page.PageListener {
    public interface GlobalConfigListener {
        void onGlobalConfigChanged(GlobalConfig newGlobalConfig);
    }

    private static final int GLOBAL_CONFIG_FILE_VERSION = 7;


    protected Context mContext;
    private File mBaseDir;

    private PageManager mPageManager;
    private ScriptManager mScriptManager;
    private ScriptExecutor mScriptExecutor;
    private VariableManager mVariableManager;
    private BuiltinDataCollectors mBuiltinDataCollectors;


    private GlobalConfig mGlobalConfig;
    private ArrayList<GlobalConfigListener> mGlobalConfigListeners = new ArrayList<>();

    private ArrayList<Page.PageListener> mPageListeners = new ArrayList<>();

    private Handler mHandler;

    private int mResumedPagesCount;

    private JSONObject mLaunchStatistics;

    private JSONObject mAppShortcuts;

    public LightningEngine(Context context, File baseDir) {
        mContext = context;
        mBaseDir = baseDir;

        mHandler = new Handler();

        mPageManager = new PageManager();
        mScriptManager = new ScriptManager(this);
        mScriptExecutor = new ScriptExecutor(this);
        mVariableManager = new VariableManager(this, FileUtils.getVariablesFile(mBaseDir));
        mBuiltinDataCollectors = new BuiltinDataCollectors(mContext, mVariableManager);
    }

    public void init() {
        mScriptManager.init();

        loadAppShortcuts();

        loadLaunchStatistics();

        loadGlobalConfig();

        evaluateGlobalConfig();

        migrate();
    }

    public void terminate() {
        mBuiltinDataCollectors.end();

        mScriptExecutor.terminate();
    }

    /**
     *
     * @return true if upgrade of old free version
     */
    public void migrate() {
        if(shouldDoFirstTimeInit()) {
            //  new install, nothing to migrate
            return;
        }

		int from_version=mGlobalConfig.version;
		if(from_version == 1) {
			// from_version==1 && !first_time_install means global config never changed: unable to guess the migration path, but existing setup anyway
            // save the global config now to keep track of the version now
            saveGlobalConfig();
			return;
		}

        // migrate here (nothing yet)

        // update the version number now
        if(from_version<GLOBAL_CONFIG_FILE_VERSION) {
            saveGlobalConfig();
        }
    }

    public boolean shouldDoFirstTimeInit() {
        int h = mGlobalConfig.homeScreen;
        if(h != Page.FIRST_DASHBOARD_PAGE) {
            return false;
        }

        File items_file = Page.getItemsFile(mBaseDir, h);
        return !items_file.exists();
    }

    public File getBaseDir() {
        return mBaseDir;
    }

    public Context getContext() {
        return mContext;
    }

    public PageManager getPageManager() {
        return mPageManager;
    }

    public ScriptManager getScriptManager() {
        return mScriptManager;
    }

    public VariableManager getVariableManager() {
        return mVariableManager;
    }

    public ScriptExecutor getScriptExecutor() {
        return mScriptExecutor;
    }

    public GlobalConfig getGlobalConfig() {
        return mGlobalConfig;
    }

    public BuiltinDataCollectors getBuiltinDataCollectors() {
        return mBuiltinDataCollectors;
    }

    public Page getOrLoadPage(int id) {
        return mPageManager.getOrLoadPage(id);
    }

    public Item getItemById(int id) {
        int p = Utils.getPageForItemId(id);
        Page page = getOrLoadPage(p);
        return page.findItemById(id);
    }

    public Pair<Page,Folder> getPageAndOpenerFromPath(ContainerPath path) {
        int id = path.getLast();
        if(path.getParent() == null) {
            Page page = getOrLoadPage(id);
            if(id == Page.USER_MENU_PAGE) {
                return new Pair<>(page, (Folder)page.findItemById(Utils.USER_MENU_ITEM_ID));
            } else {
                return new Pair<>(page, null);
            }
        } else {
            Item item = getItemById(id);
            if(item instanceof Folder) {
                Folder folder = (Folder) item;
                return new Pair<>(folder.getOrLoadFolderPage(), folder);
            } else {
                // not a folder? maybe a shortcut with a open folder action
                if(item instanceof Shortcut) {
                    Intent intent = ((Shortcut) item).getIntent();
                    if(LLApp.get().isLightningIntent(intent)) {
                        EventAction eventAction = Utils.decodeEventActionFromLightningIntent(intent);
                        if (eventAction != null) {
                            int folderPage = Integer.parseInt(eventAction.data);
                            Folder folder = findFirstFolderPageOpener(folderPage);
                            return new Pair<>(folder.getOrLoadFolderPage(), folder);
                        }
                    }
                }
                return null;
            }
        }
    }

    public void registerPageListener(Page.PageListener listener) {
        mPageListeners.add(listener);
    }

    public void unregisterPageListener(Page.PageListener listener) {
        mPageListeners.remove(listener);
    }

    public boolean canRunScripts() {
        return mGlobalConfig.runScripts;
    }


    @Override
    public void onPageModified(Page page) {
        Utils.updateContainerIconIfNeeded(page);
        for (Page.PageListener listener : mPageListeners) listener.onPageModified(page);
    }

    @Override
    public void onPageItemLoaded(Item item) {

        for (Page.PageListener listener : mPageListeners) listener.onPageItemLoaded(item);
    }

    @Override
    public void onPageItemDestroyed(Item item) {
        for (Page.PageListener listener : mPageListeners) listener.onPageItemDestroyed(item);
    }

    @Override
    public void onPageItemAdded(Item item) {
        for (Page.PageListener listener : mPageListeners) listener.onPageItemAdded(item);

        Intent intent = getAppShortcutIntent(item);
        if(intent != null) {
            String pkg = intent.getStringExtra(Shortcut.INTENT_EXTRA_APP_SHORTCUT_PKG);
            String id = intent.getStringExtra(Shortcut.INTENT_EXTRA_APP_SHORTCUT_ID);
            pinAppShortcut(pkg, id);
        }

        Page page = item.getPage();
        Utils.updateContainerIconIfNeeded(page);
        if(item.getClass() == Shortcut.class && page.config.iconPack != null) {
            IconPack.applyIconPackSync(mContext, page.config.iconPack, page, item.getId());
        }
    }

    @Override
    public void onPageItemBeforeRemove(Item item) {
        for (Page.PageListener listener : mPageListeners) listener.onPageItemBeforeRemove(item);
    }

    @Override
    public void onPageItemRemoved(Page page, Item item) {
        Intent intent = getAppShortcutIntent(item);
        if(intent != null) {
            String pkg = intent.getStringExtra(Shortcut.INTENT_EXTRA_APP_SHORTCUT_PKG);
            String id = intent.getStringExtra(Shortcut.INTENT_EXTRA_APP_SHORTCUT_ID);
            unpinAppShortcut(pkg, id);
        }
        Utils.updateContainerIconIfNeeded(page);

        // really hackish: reverse order so that the Lightning script object (which is the first to register) is evaluated last
        // the reason is that scripts may be executed from other listeners and may need item data in the Lightning script object
        for (int i = mPageListeners.size()-1; i>=0; i--) {
            Page.PageListener listener = mPageListeners.get(i);
            listener.onPageItemRemoved(page, item);
        }
    }

    @Override
    public void onPageItemChanged(Page page, Item item) {
        Utils.updateContainerIconIfNeeded(page);
        for (Page.PageListener listener : mPageListeners) listener.onPageItemChanged(page, item);

        // TODO move this into Screen
        if(item instanceof Folder) {
            onPageFolderWindowChanged(page, (Folder)item);
        }
    }

    @Override
    public void onPageItemZIndexChanged(Page page, int old_index, int new_index) {
        Utils.updateContainerIconIfNeeded(page);
        for (Page.PageListener listener : mPageListeners) listener.onPageItemZIndexChanged(page, old_index, new_index);
    }

    @Override
    public void onPagePaused(Page page) {
        for (Page.PageListener listener : mPageListeners) listener.onPagePaused(page);

        mResumedPagesCount--;
        if(mResumedPagesCount == 0) {
            mBuiltinDataCollectors.pause();
        }
    }

    @Override
    public void onPageResumed(Page page) {
        for (Page.PageListener listener : mPageListeners) listener.onPageResumed(page);

        if(mResumedPagesCount == 0) {
            mBuiltinDataCollectors.resume();
        }
        mResumedPagesCount++;
    }

    @Override
    public void onPageFolderWindowChanged(Page page, Folder folder) {
        for (Page.PageListener listener : mPageListeners) listener.onPageFolderWindowChanged(page, folder);
    }

    @Override
    public void onPageEditModeEntered(Page page) {
        for (Page.PageListener listener : mPageListeners) listener.onPageEditModeEntered(page);

        mVariableManager.pauseUpdates();
    }

    @Override
    public void onPageEditModeLeaved(Page page) {
        mVariableManager.resumeUpdates();

        for (Page.PageListener listener : mPageListeners) listener.onPageEditModeLeaved(page);
    }

    @Override
    public void onItemPaused(Item item) {
        for (Page.PageListener listener : mPageListeners) listener.onItemPaused(item);
    }

    @Override
    public void onItemResumed(Item item) {
        for (Page.PageListener listener : mPageListeners) listener.onItemResumed(item);
    }

    @Override
    public void onItemVisibilityChanged(Item item) {
        for (Page.PageListener listener : mPageListeners) listener.onItemVisibilityChanged(item);
    }

    @Override
    public void onItemAlphaChanged(Item item) {
        for (Page.PageListener listener : mPageListeners) listener.onItemAlphaChanged(item);
    }

    @Override
    public void onItemTransformChanged(Item item, boolean fast) {
        for (Page.PageListener listener : mPageListeners) listener.onItemTransformChanged(item, fast);
    }

    @Override
    public void onItemCellChanged(Item item) {
        for (Page.PageListener listener : mPageListeners) listener.onItemCellChanged(item);
    }

    @Override
    public void onItemBindingsChanged(Item item, boolean apply) {
        for (Page.PageListener listener : mPageListeners) listener.onItemBindingsChanged(item, apply);
    }

    @Override
    public void onShortcutLabelChanged(Shortcut shortcut) {
        for (Page.PageListener listener : mPageListeners) listener.onShortcutLabelChanged(shortcut);
    }

    @Override
    public void onFolderPageIdChanged(Folder folder, int oldPageId) {
        Utils.updateFolderIcon(folder);
        for (Page.PageListener listener : mPageListeners) listener.onFolderPageIdChanged(folder, oldPageId);
    }

    @Override
    public void onPageRemoved(Page page) {
        // see comment for onPageItemRemoved
        for (int i = mPageListeners.size()-1; i>=0; i--) {
            Page.PageListener listener = mPageListeners.get(i);
            listener.onPageRemoved(page);
        }
    }

    @Override
    public void onPageLoaded(Page page) {
        for (Page.PageListener listener : mPageListeners) listener.onPageLoaded(page);
    }

    /***************************************** SAVE DATA ***********************************/

    private Runnable mSaveDataRunnable = new Runnable() {
        @Override
        public void run() {
            mVariableManager.saveValues();

            saveGlobalConfig();

            saveLaunchStatistics();

            saveAppShortcuts();

            mPageManager.savePagesSync();
        }
    };

    public void saveData() {
        cancelDelayedSaveData();
        mSaveDataRunnable.run();
    }

    public void saveDataDelayed() {
        cancelDelayedSaveData();
        mHandler.postDelayed(mSaveDataRunnable, 10000);
    }

    public void cancelDelayedSaveData() {
        mHandler.removeCallbacks(mSaveDataRunnable);
    }

    /*************************************** GLOBAL CONFIG**********************************/

    public void registerGlobalConfigChangeListener(GlobalConfigListener listener) {
        mGlobalConfigListeners.add(listener);
    }

    public void unregisterGlobalConfigChangeListener(GlobalConfigListener listener) {
        mGlobalConfigListeners.remove(listener);
    }


    private void loadGlobalConfig() {
        mGlobalConfig = JsonLoader.readObject(GlobalConfig.class, FileUtils.getGlobalConfigFile(mBaseDir));
    }

    private void evaluateGlobalConfig() {
        if(mGlobalConfig.screensOrder==null) {
            ArrayList<Integer> desktops = new ArrayList<>();

            for(int i = Page.FIRST_DASHBOARD_PAGE; i<= Page.LAST_DASHBOARD_PAGE; i++) {
                if(Page.getPageDir(mBaseDir, i).exists()) {
                    desktops.add(i);
                }
            }

            int n=desktops.size();
            int[] screens_order = new int[n];
            String[] screens_name = new String[n];
            for(int i=0; i<n; i++) {
                Integer page = desktops.get(i);
                screens_order[i] = page;
                screens_name[i] = String.valueOf(page);
            }

            mGlobalConfig.screensOrder = screens_order;
            mGlobalConfig.screensNames = screens_name;
        }

        // make sure that page directories are reserved
        for(int p : mGlobalConfig.screensOrder) {
            Page.getPageDir(mBaseDir, p).mkdirs();
        }

        Intent s=LLApp.get().getLockscreenServiceIntent();
        if(s != null) {
            if (mGlobalConfig.lockScreen != Page.NONE) {
                mContext.startService(s);
            } else {
                mContext.stopService(s);
            }
        }

        s = LLApp.get().getWindowServiceIntent();
        if(s != null && WindowService.isPermissionAllowed(mContext)) {
            if (mGlobalConfig.overlayScreen != Page.NONE) {
                mContext.startService(s);
            } else {
                mContext.stopService(s);
            }
        }
    }

    private void saveGlobalConfig() {
        mGlobalConfig.version = GLOBAL_CONFIG_FILE_VERSION;
        JsonLoader.saveObjectToFile(mGlobalConfig, FileUtils.getGlobalConfigFile(mBaseDir));
    }

    public void reloadGlobalConfig() {
        loadGlobalConfig();
        evaluateGlobalConfig();
    }

    public void notifyGlobalConfigChanged() {
        evaluateGlobalConfig();
        for(GlobalConfigListener l : mGlobalConfigListeners) {
            l.onGlobalConfigChanged(mGlobalConfig);
        }
    }

    /************************************** LAUNCH STATISTICS *************************************/

    public void updateLaunchStatisticsForShortcut(Shortcut shortcut) {
        Intent intent=new Intent(shortcut.getIntent());
        ComponentName cn=intent.getComponent();
        if(cn!=null) {
            String key=cn.flattenToShortString();
            int count = mLaunchStatistics.optInt(key);
            try { mLaunchStatistics.put(key, count+1); } catch (JSONException e) { }
        }
    }

    public int getShortcutLaunchCount(Shortcut s) {
        Intent intent = s.getIntent();
        if (intent != null) {
            ComponentName component = intent.getComponent();
            if (component != null) {
                String key = component.flattenToShortString();
                return mLaunchStatistics.optInt(key);
            }
        }
        return 0;
    }

    private void loadLaunchStatistics() {
        File statistics_file = FileUtils.getStatisticsFile(mBaseDir);
        mLaunchStatistics = FileUtils.readJSONObjectFromFile(statistics_file);
        if(mLaunchStatistics==null) {
            mLaunchStatistics = new JSONObject();
        }
    }

    private void saveLaunchStatistics() {
        try {
            File out = FileUtils.getStatisticsFile(mBaseDir);
            FileUtils.saveStringToFile(mLaunchStatistics.toString(), out);
        } catch(Exception e) {
            // pass
        }
    }

    /************************************** ANDROID 7.1 APP SHORTCUTS *************************************/

    private static final String TOKEN_PIN_COUNT = "pinCount";

    private void pinAppShortcut(String pkg, String id) {
        int pinCount = adjustAppShortcutPinCount(pkg, id, 1);
        if(pinCount == 1) {
            updatePinnedAppShortcuts(pkg);
        }
    }

    private void unpinAppShortcut(String pkg, String id) {
        int pinCount = adjustAppShortcutPinCount(pkg, id, -1);
        if(pinCount == 0) {
            updatePinnedAppShortcuts(pkg);
        }
    }

    @TargetApi(Build.VERSION_CODES.N_MR1)
    private void updatePinnedAppShortcuts(String pkg) {
        LauncherApps launcherApps = (LauncherApps) mContext.getSystemService(Context.LAUNCHER_APPS_SERVICE);
        List<String> pinnedAppShortcutIds = getPinnedAppShortcutIds(pkg);
        if(launcherApps.hasShortcutHostPermission()) {
            launcherApps.pinShortcuts(pkg, pinnedAppShortcutIds, Process.myUserHandle());
        }
    }

    private List<String> getPinnedAppShortcutIds(String pkg) {
        ArrayList<String> ids = new ArrayList<>();
        JSONObject appShortcutsForPackage = mAppShortcuts.optJSONObject(pkg);
        if(appShortcutsForPackage != null) {
            Iterator<String> keys = appShortcutsForPackage.keys();
            while(keys.hasNext()) {
                String id = keys.next();
                JSONObject appShortcut = appShortcutsForPackage.optJSONObject(id);
                int pinCount = appShortcut.optInt(TOKEN_PIN_COUNT, 0);
                if(pinCount > 0) {
                    ids.add(id);
                }
            }
        }
        return ids;
    }

    private int adjustAppShortcutPinCount(String pkg, String id, int increment) {
        try {
            JSONObject appShortcutsForPackage = mAppShortcuts.optJSONObject(pkg);
            if(appShortcutsForPackage == null) {
                appShortcutsForPackage = new JSONObject();
                mAppShortcuts.put(pkg, appShortcutsForPackage);
            }

            JSONObject appShortcut = appShortcutsForPackage.optJSONObject(id);
            if (appShortcut == null) {
                appShortcut = new JSONObject();
                appShortcutsForPackage.put(id, appShortcut);
            }

            int pinCount = appShortcut.optInt(TOKEN_PIN_COUNT, 0);
            pinCount += increment;
            appShortcut.put(TOKEN_PIN_COUNT, pinCount);
            return pinCount;
        } catch (JSONException e) {
            // pass, will never be reached
            return 0;
        }
    }

    private void loadAppShortcuts() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            File appShortcutsFile = FileUtils.getPinnedAppShortcutsFile(mBaseDir);
            mAppShortcuts = FileUtils.readJSONObjectFromFile(appShortcutsFile);
            if (mAppShortcuts == null) {
                mAppShortcuts = new JSONObject();
            }
        }
    }

    private void saveAppShortcuts() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            try {
                File out = FileUtils.getPinnedAppShortcutsFile(mBaseDir);
                FileUtils.saveStringToFile(mAppShortcuts.toString(), out);
            } catch (Exception e) {
                // pass
            }
        }
    }

    /** Return null if this isn't a Android 7.1 app shortcut */
    private Intent getAppShortcutIntent(Item item) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            if (item.getClass() == Shortcut.class) {
                Shortcut shortcut = (Shortcut) item;
                Intent intent = shortcut.getIntent();
                if (Shortcut.INTENT_ACTION_APP_SHORTCUT.equals(intent.getAction())) {
                    return intent;
                }
            }
        }

        return null;
    }

    /****************************************** HOME PAGE *****************************************/

    private File getCurrentPagerPageFile() {
        return new File(mBaseDir, "current");
    }

    public int readCurrentPage(int default_page) {
        String p=FileUtils.readFileContent(getCurrentPagerPageFile());
        int page;
        try {
            page=Integer.parseInt(p);
        } catch(Exception e) {
            // catch NPE (if p==null) and NumberFormatException
            page=default_page;
        }
        return page;
    }

    public void writeCurrentPage(int page) {
        try {
            FileUtils.saveStringToFile(String.valueOf(page), getCurrentPagerPageFile());
        } catch (IOException e) {
            // pass
        }
    }

    /******************************************** MISC *****************************************/

    public ArrayList<Folder> findAllFolderPageOpeners(int folder_page) {
        ArrayList<Folder> openers = new ArrayList<>();
        for(int n : mPageManager.getAllPagesIds()) {
            if(n == folder_page && folder_page != Page.USER_MENU_PAGE) continue;

            Page p = mPageManager.getOrLoadPage(n);

            for(Item item : p.items) {
                if(item instanceof Folder) {
                    Folder f=(Folder)item;
                    if(f.getFolderPageId()==folder_page) {
                        openers.add(f);
                    }
                }
            }
        }

        return openers;
    }

    public Folder findFirstFolderPageOpener(int folder_page) {
        if(!Page.isFolder(folder_page)) {
            return null;
        }

        ArrayList<Page> pages=mPageManager.getLoadedPages();
        for(Page p : pages) {
            // skip the page containing this item to avoid nasty recursive calls, except for the user menu which includes its opener
            if(p.id == folder_page && folder_page != Page.USER_MENU_PAGE) continue;

            if(p.items==null) {
                mPageManager.getOrLoadPage(p.id);
            }
            for(Item item : p.items) {
                if(item instanceof Folder) {
                    Folder f=(Folder)item;
                    if(f.getFolderPageId()==folder_page) {
                        return f;
                    }
                }
            }
        }

        // not found in currently loaded pages, try again with unloaded pages
        for(int n : mPageManager.getAllPagesIds()) {

            Page p = mPageManager.getPage(n);
            if(p != null) {
                // already traversed in the loop above
                continue;
            }

            p = mPageManager.getOrLoadPage(n);

            if(p.id == folder_page) continue;

            for(Item item : p.items) {
                if(item instanceof Folder) {
                    Folder f=(Folder)item;
                    if(f.getFolderPageId()==folder_page) {
                        return f;
                    }
                }
            }
        }

        return null;
    }

    public void setFloatingDesktopVisibility(boolean visible) {
        if(mGlobalConfig.overlayScreen != Page.NONE) {
            Intent f = LLApp.get().getWindowServiceIntent();
            f.setAction(visible ? WindowService.INTENT_ACTION_SHOW : WindowService.INTENT_ACTION_HIDE);
            mContext.startService(f);
        }
    }

    public class PageManager {
        private ArrayList<Page> mPages = new ArrayList<>();

        private PageManager() {
        }

        public ArrayList<Page> getLoadedPages() {
            return new ArrayList<>(mPages);
        }

        public int[] getAllPagesIds() {
            String[] allPageNames = FileUtils.getPagesDir(mBaseDir).list();
            if(allPageNames == null) {
                return new int[0];
            }

            int length = allPageNames.length;
            int[] allPagesIds = new int[length];
            for(int n=0; n<length; n++) {
                allPagesIds[n] = Integer.parseInt(allPageNames[n]);
            }
            return allPagesIds;
        }

        public boolean isPageCreated(int id) {
            return Page.exists(mBaseDir, id) || getPage(id) != null;
        }

        public void clear() {
            mPages.clear();
        }

        public Page getPage(int id) {
            for(Page page : mPages) {
                if(page.id == id) {
                    return page;
                }
            }

            return null;
        }

        public Page getOrLoadPage(int id) {
            Page page = getPage(id);
            if(page != null) {
                return page;
            }

            page = new Page(LightningEngine.this, id);
            mPages.add(page);

            page.create();

            return page;
        }

        public void savePagesSync() {
            for(Page p : new ArrayList<Page>(mPages)) {
                p.save();
            }
        }

        public void removePage(int p) {
            Page page = getOrLoadPage(p);
            page.remove();
            page.destroy();
            mPages.remove(page);
        }

        public Page clonePage(Page pageFrom, boolean keepAppWidgetId) {
            ArrayList<Page> pages = new ArrayList<>(1);
            pages.add(pageFrom);

            SparseIntArray translatedPageIds = clonePages(pages, keepAppWidgetId);

            return getOrLoadPage(translatedPageIds.get(pageFrom.id));
        }

        public SparseIntArray clonePages(ArrayList<Page> pagesFrom, boolean keepAppWidgetId) {
            // list all pages to copy
            ArrayList<Page> pages_to_copy = new ArrayList<>();
            for (Page pageFrom : pagesFrom) {
                pages_to_copy.add(pageFrom);
                Utils.findPagesInItems(pageFrom, pages_to_copy);
            }

            // reserve copied page ids
            SparseIntArray translated_page_ids = new SparseIntArray();
            for(Page to_translate : pages_to_copy) {
                int new_page_id = Page.reservePage(mBaseDir, to_translate.isFolder());
                translated_page_ids.put(to_translate.id, new_page_id);
            }

            // copy and translate pages
            PageProcessor page_processor = new PageProcessor();
            page_processor.setTranslatedPageIds(translated_page_ids);
            for(Page to_copy : pages_to_copy) {
                page_processor.copyAndTranslatePage(to_copy, LightningEngine.this, keepAppWidgetId);
            }

            return translated_page_ids;
        }
    }

}
