package net.pierrox.lightning_launcher.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.util.DisplayMetrics;

import net.pierrox.lightning_launcher.LLApp;
import net.pierrox.lightning_launcher.Version;
import net.pierrox.lightning_launcher.activities.AppDrawerX;
import net.pierrox.lightning_launcher.activities.BackupRestore;
import net.pierrox.lightning_launcher.activities.Customize;
import net.pierrox.lightning_launcher.activities.Dashboard;
import net.pierrox.lightning_launcher.activities.RootSettings;
import net.pierrox.lightning_launcher.activities.ScriptEditor;
import net.pierrox.lightning_launcher.configuration.DynamicTextConfig;
import net.pierrox.lightning_launcher.configuration.FolderConfig;
import net.pierrox.lightning_launcher.configuration.GlobalConfig;
import net.pierrox.lightning_launcher.configuration.ItemConfig;
import net.pierrox.lightning_launcher.configuration.PageConfig;
import net.pierrox.lightning_launcher.configuration.ShortcutConfig;
import net.pierrox.lightning_launcher.data.Box;
import net.pierrox.lightning_launcher.data.DynamicText;
import net.pierrox.lightning_launcher.data.EventAction;
import net.pierrox.lightning_launcher.data.Folder;
import net.pierrox.lightning_launcher.data.Item;
import net.pierrox.lightning_launcher.data.LightningIntent;
import net.pierrox.lightning_launcher.data.Page;
import net.pierrox.lightning_launcher.data.PageIndicator;
import net.pierrox.lightning_launcher.engine.LightningEngine;
import net.pierrox.lightning_launcher.data.PageProcessor;
import net.pierrox.lightning_launcher.data.Shortcut;
import net.pierrox.lightning_launcher.data.Utils;
import net.pierrox.lightning_launcher.script.ScriptManager;
import net.pierrox.lightning_launcher_extreme.BuildConfig;
import net.pierrox.lightning_launcher_extreme.R;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class Setup {
    public interface OnFirstTimeInitEvent {
        public void onFirstTimeInitStart(boolean is_import);
        public void onFirstTimeInitEnd(boolean success, boolean was_import, Item all_apps_item);
    }

    @SuppressWarnings("deprecation")
    public static void firstTimeInit(final OnFirstTimeInitEvent listener) {
        final LightningEngine engine = LLApp.get().getAppEngine();

        Context tmp = null;
        if(!BuildConfig.IS_TRIAL) {
            try {
                tmp = LLApp.get().createPackageContext(LLApp.LL_PKG_NAME, 0);
            } catch (PackageManager.NameNotFoundException e) {
            }
        }
        final Context classic_ll_context = tmp;

        final boolean import_data = false; //classic_ll_context!=null;

        listener.onFirstTimeInitStart(import_data);

        final Handler handler = new Handler();
        new AsyncTask<Void, Void, Boolean>() {
            private boolean was_import = import_data;
            private Item mAllAppsItem;

            @Override
            protected Boolean doInBackground(Void... params) {
                was_import = import_data;

                if(!import_data) {
                    try {
                        Thread.sleep(4000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mAllAppsItem = defaultSetup(engine);

                    return true;
                } else {
                    byte[] buffer = new byte[4096];
                    try {

                        // first, copy files
                        Utils.copyDirectory(buffer, classic_ll_context.getFilesDir(), engine.getBaseDir());

                        // ll is here but no valid data
                        engine.reloadGlobalConfig();
                        if(engine.shouldDoFirstTimeInit()) {
                            was_import = false;
                            mAllAppsItem = defaultSetup(engine);
                            return true;
                        }

                        LightningEngine.PageManager pageManager = engine.getPageManager();
                        pageManager.clear();
                        ArrayList<Page> mImportedPages = new ArrayList<>();
                        for(int p : pageManager.getAllPagesIds()) {
                            mImportedPages.add(pageManager.getOrLoadPage(p));
                        }

                        PageProcessor page_processor = new PageProcessor();
                        page_processor.setProcessGlobalConfig(true);
                        page_processor.postProcessPages(mImportedPages);

                        Utils.refreshAppDrawerShortcuts(engine, handler);

                        return true;
                    } catch (IOException e) {
                        return false;
                    }
                }
            }

            @Override
            protected void onPostExecute(Boolean result) {
                listener.onFirstTimeInitEnd(result, was_import, mAllAppsItem);


            }


        }.execute((Void)null);
    }

    private static final String[] CN_DOCK_0 = new String[] {
            // phone
        "com.android.contacts/.activities.DialtactsActivity",
        "com.android.contacts/.DialtactsActivity",
        "com.android.contacts/com.android.dialer.DialtactsActivity",
        "com.android.dialer/.DialtactsActivity",
        "com.android.htccontacts/.DialerTabActivity",
        "com.google.android.dialer/.extensions.GoogleDialtactsActivity",
        "com.sec.android.app.dialertab/.DialerTabActivity",
        "com.sonyericsson.android.socialphonebook/.DialerEntryActivity",

            // camera
        "com.android.camera/.Camera",
        "com.android.camera/.CameraEntry",
        "com.asus.camera/.CameraApp",
        "com.cyngn.cameranext/com.android.camera.CameraLauncher",
        "com.google.android.GoogleCamera/com.android.camera.CameraLauncher",
        "com.motorola.camera/.Camera",
        "com.sec.android.app.camera/.Camera",
        "com.sonyericsson.android.camera/.CameraActivity",
};

    private static final String[] CN_DOCK_1 = new String[] {
        "com.android.settings/.Settings",
        "com.android.settings/.GridSettings",

};
//    private static final String[] CN_DOCK_2 = new String[] {
//            "net.pierrox.lightning_launcher_extreme/net.pierrox.lightning_launcher.activities.AppDrawerX"
//    };
    private static final String[] CN_DOCK_3 = new String[] {
    "com.android.chrome/com.google.android.apps.chrome.Main",
    "com.android.browser/.BrowserActivity",
    "com.asus.browser/com.android.browser.BrowserActivity",
    "com.opera.mini.android/.Browser",
    "com.sec.android.app.sbrowser/.SBrowserMainActivity",

};
    private static final String[] CN_DOCK_4 = new String[] {
        "com.android.vending/.AssetBrowserActivity"
    };

    private static Item defaultSetup(LightningEngine engine) {

        File dir = engine.getBaseDir();
        if(!dir.exists()) {
            dir.mkdirs();
        }
        Utils.deleteDirectory(dir, false);

        // install the app drawer
        LightningEngine.PageManager pm = engine.getPageManager();
        Page drawer = pm.getOrLoadPage(Page.APP_DRAWER_PAGE);
        setupAppDrawer(drawer);
        drawer.setModified();
        drawer.save();
        drawer.reload();

        GlobalConfig globalConfig = engine.getGlobalConfig();

        if(!BuildConfig.IS_TRIAL || LLApp.get().isTrialVersion()) {
            Page lockscreen = engine.getOrLoadPage(Page.FIRST_DASHBOARD_PAGE+1);
            setupLockScreen(lockscreen);

            globalConfig.screensOrder = new int[2];
            globalConfig.screensNames = new String[2];

            globalConfig.screensOrder[1] = lockscreen.id;
            globalConfig.screensNames[1] = engine.getContext().getString(R.string.ds_1);
        } else {
            globalConfig.screensOrder = new int[1];
            globalConfig.screensNames = new String[1];
        }

        // create dashboard default config
        int page= Page.FIRST_DASHBOARD_PAGE;
        Page dashboard = pm.getOrLoadPage(page);
        Item all_apps_item = setupDashboard(dashboard, drawer);

        globalConfig.screensOrder[0] = dashboard.id;
        globalConfig.screensNames[0] = engine.getContext().getString(R.string.ds_0);

        engine.saveData();

        return all_apps_item;
    }

    public static Item setupDashboard(Page dashboard, Page drawer) {
        Shortcut s;
        ShortcutConfig sc;

        LightningEngine engine = LLApp.get().getAppEngine();
        Context context = engine.getContext();
        final Resources resources = context.getResources();
        final DisplayMetrics displayMetrics = resources.getDisplayMetrics();
        float density = displayMetrics.density;
        int color_primary_dark = resources.getColor(R.color.color_primary_dark);



        final PageConfig dashboard_config = dashboard.config;
        dashboard_config.applyDefaultFolderConfig();
        dashboard_config.screenOrientation = PageConfig.ScreenOrientation.PORTRAIT;
        dashboard_config.gridPRowNum = 6;
        dashboard_config.menuKey = new EventAction(GlobalConfig.USER_MENU, null);
        dashboard_config.swipeUp = new EventAction(GlobalConfig.USER_MENU, null);
        dashboard_config.swipe2Up = new EventAction(GlobalConfig.USER_MENU, null);
        dashboard_config.bgTap = new EventAction(GlobalConfig.WALLPAPER_TAP, null);
        dashboard_config.statusBarColor = color_primary_dark;
        dashboard_config.navigationBarColor = Color.BLACK;
        installDockShortcut(dashboard, drawer, 0, 5, CN_DOCK_0);
        installDockShortcut(dashboard, drawer, 1, 5, CN_DOCK_1);
        Item all_apps_item = installDockShortcut(dashboard, drawer, 2, 5, new String[]{new ComponentName(context, AppDrawerX.class).flattenToShortString()});
        all_apps_item.modifyItemConfig().launchAnimation = ItemConfig.LaunchAnimation.SLIDE_UP;
        installDockShortcut(dashboard, drawer, 3, 5, CN_DOCK_3);
        installDockShortcut(dashboard, drawer, 4, 5, CN_DOCK_4);


        // create page indicator (dots)
        PageIndicator pi = Utils.addPageIndicator(dashboard, 0, 0, 1, true);
        pi.getCell().set(0, 4, 5, 5);
        ItemConfig ic = pi.modifyItemConfig();
        ic.copyFrom(dashboard_config.defaultItemConfig);
        ic.enabled = false;
        ic.pinMode = ItemConfig.PinMode.XY;
        ic.box.av = Box.AlignV.BOTTOM;
        ic.box.size[Box.MB] = (int) (10 * density);
        ic.box_s = ic.box.toString(dashboard_config.defaultItemConfig.box);
        pi.notifyChanged();

        // create page indicator (line)
        pi = Utils.addPageIndicator(dashboard, 0, 0, 1, true);
        pi.getCell().set(0, 4, 5, 5);
        pi.style = PageIndicator.Style.LINE_X;
        ic = pi.modifyItemConfig();
        ic.enabled = false;
        ic.pinMode = ItemConfig.PinMode.XY;
        ic.box.av = Box.AlignV.BOTTOM;
        ic.box.size[Box.ML] = ic.box.size[Box.MR] = (int) (10 * density);
        ic.box_s = ic.box.toString(dashboard_config.defaultItemConfig.box);
        pi.notifyChanged();

        if(!LLApp.get().isFreeVersion()) {
            // install date and time dynamic texts
            DynamicText dt = Utils.addDynamicText(dashboard, DynamicTextConfig.Source.DATE, true);
            dt.getCell().set(3, 4, 5, 5);
            ic = dt.modifyItemConfig();
            ic.box.ah = Box.AlignH.RIGHT;
            ic.box.size[Box.MR] = (int) (5 * density);
            ic.box_s = ic.box.toString(null);
            ic.pinMode = ItemConfig.PinMode.X;
            dt.getDynamicTextConfig().dateFormat = "HH:mm";
            sc = dt.modifyShortcutConfig();
            sc.labelFontSize = displayMetrics.widthPixels / (10 * density);
            sc.labelShadowRadius = sc.labelShadowOffsetX = sc.labelShadowOffsetY = 5;
            sc.labelShadowColor = 0x80000000;
            dt.notifyChanged();

            dt = Utils.addDynamicText(dashboard, DynamicTextConfig.Source.DATE, true);
            ic = dt.modifyItemConfig();
            ic.box.av = Box.AlignV.BOTTOM;
            ic.box.ah = Box.AlignH.RIGHT;
            ic.box.size[Box.MR] = (int) (10 * density);
            ic.box.size[Box.MB] = (int) (7 * density);
            ic.box_s = ic.box.toString(null);
            ic.pinMode = ItemConfig.PinMode.X;
            dt.getCell().set(3, 4, 5, 5);
            dt.getDynamicTextConfig().dateFormat = "yyyy-MM-dd";
            sc = dt.modifyShortcutConfig();
            sc.labelFontSize = displayMetrics.widthPixels / (25 * density);
            dt.notifyChanged();
        }

        // install promotional icons such as mgoid, baby games, mcompass, let's dance
        LLApp.get().installPromotionalIcons(dashboard);

        // various shortcuts (backup/restore, all settings, script editor)
        installShortcut(dashboard, -5, 0, "N", resources.getString(R.string.mi_es_settings), null, new Intent(context, RootSettings.class));
        installShortcut(dashboard, -4, 0, "x", resources.getString(R.string.ds_br), null, new Intent(context, BackupRestore.class));
        installShortcut(dashboard, 9, 0, "z", resources.getString(R.string.sc_editor), null, new Intent(context, ScriptEditor.class));
        installShortcut(dashboard, 8, 0, "P", resources.getString(R.string.um_t), Version.BROWSE_TEMPLATES_URI.toString(), null);

        Intent intent = new Intent(context, Dashboard.class);
        intent.putExtra(LightningIntent.INTENT_EXTRA_ACTION, GlobalConfig.SWITCH_FULL_SCALE_OR_ORIGIN);
        installShortcut(dashboard, 4, 0, "J", resources.getString(R.string.ds_z), null, intent);


        // hints
        intent = new Intent(Intent.ACTION_VIEW, Uri.parse(Version.WIKI_PREFIX+"desktops"));
        s = Utils.addShortcut(resources.getString(R.string.ds_hn), null, intent, dashboard, 6, 2, 1, true);
        s.getCell().set(6, 2, 9, 3);
        sc = s.modifyShortcutConfig();
        sc.iconVisibility = false;
        sc.labelMaxLines = 7;
        s.notifyChanged();

        intent = new Intent(Intent.ACTION_VIEW, Uri.parse(Version.WIKI_PREFIX+"concepts"));
        s = Utils.addShortcut(resources.getString(R.string.ds_um), null, intent, dashboard, 6, 2, 1, true);
        s.getCell().set(-4, 2, -1, 3);
        sc = s.modifyShortcutConfig();
        sc.iconVisibility = false;
        sc.labelMaxLines = 7;
        s.notifyChanged();



        // start here folder
        Folder start_here;
        Page start_here_page;
        start_here=Utils.addFolder(dashboard, 2, 1, 1, true, resources.getString(R.string.ds_sh));
        start_here.getCell().set(0, 0, 1, 1);

        FolderConfig fc = start_here.modifyFolderConfig();
        fc.iconStyle= FolderConfig.FolderIconStyle.NORMAL;
        fc.titleVisibility = false;
        fc.wAH = Box.AlignH.LEFT;
//        fc.wAV = Box.AlignV.TOP;
        fc.animationIn = FolderConfig.FolderAnimation.SLIDE_FROM_LEFT;
        fc.animationOut = FolderConfig.FolderAnimation.SLIDE_FROM_RIGHT;
        fc.autoClose = true;
        Bitmap icon= Utils.createIconFromText(start_here.getStdIconSize(), "q");
        Utils.saveIconToFile(start_here.getDefaultIconFile(), icon);


        start_here_page = start_here.getOrLoadFolderPage();
        PageConfig folder_config=start_here_page.config;
        folder_config.defaultShortcutConfig.labelMaxLines=2;
        folder_config.useDesktopSize = false;
//        folder_config.gridPRowMode = PageConfig.SizeMode.AUTO;
        folder_config.gridPRowNum = 6;
        folder_config.gridPColumnMode= PageConfig.SizeMode.AUTO;

        start_here.notifyChanged();

        start_here_page.saveConfig();
        final String youtube="http://www.youtube.com/watch?v=";
        installShortcut(start_here_page, 0, 0, "u", resources.getString(R.string.tut_concepts), Version.WIKI_PREFIX+"concepts", null);
        installShortcut(start_here_page, 0, 1, "a", resources.getString(R.string.tut_wiki), Version.WIKI_PREFIX+"start", null);
        installShortcut(start_here_page, 0, 2, "y", resources.getString(R.string.language_t), Version.LANGUAGE_PACK_INSTALL_URI.toString(), null);
        installShortcut(start_here_page, 0, 3, "r", resources.getString(R.string.ds_uc), Version.USER_COMMUNITY, null);
        installShortcut(start_here_page, 0, 4, "t", resources.getString(R.string.tut_intro), youtube+"muY61GMH3mQ", null);

        // start here tutorials folder
        Folder tutorials;
        Page tutorials_page;
        tutorials=Utils.addFolder(start_here_page, 0, 5, 1, true, resources.getString(R.string.tutorials));
        tutorials.getCell().set(0, 5, 1, 6);
        start_here_page.getAndCreateIconDir();
        icon = Utils.createIconFromText(Utils.getStandardIconSize(), "v");
        Utils.saveIconToFile(tutorials.getDefaultIconFile(), icon);

        fc = tutorials.modifyFolderConfig();
        fc.iconStyle= FolderConfig.FolderIconStyle.NORMAL;
        fc.titleVisibility = false;
        fc.wAH = Box.AlignH.LEFT;
//        fc.wAV = Box.AlignV.TOP;
        fc.animationIn = FolderConfig.FolderAnimation.SLIDE_FROM_LEFT;
        fc.animationOut = FolderConfig.FolderAnimation.SLIDE_FROM_RIGHT;
        fc.autoClose = true;

        tutorials_page = tutorials.getOrLoadFolderPage();
        tutorials_page.config.useDesktopSize = false;
        tutorials_page.config.gridPColumnMode= PageConfig.SizeMode.AUTO;

        tutorials.notifyChanged();

        installShortcut(tutorials_page, 0, 0, "s", resources.getString(R.string.ds_tt), Version.WIKI_PREFIX + "tips_tricks", null);
        installShortcut(tutorials_page, 0, 1, "v", resources.getString(R.string.tut_custo), youtube+"Gkh_RdH8FTk", null);
        installShortcut(tutorials_page, 0, 2, "v", resources.getString(R.string.tut_pin), youtube+"eUFgtyea5Ak", null);
        installShortcut(tutorials_page, 0, 3, "v", resources.getString(R.string.tut_scroll), youtube+"4Kic7av9eac", null);

        return all_apps_item;
    }

    public static void setupAppDrawer(Page drawer) {
        Resources resources = LLApp.get().getResources();
        final PageConfig config = drawer.config;
        config.applyDefaultFolderConfig();
        config.gridPRowNum = config.gridLRowNum = 4;
        config.scrollingDirection = PageConfig.ScrollingDirection.X;
        if(Build.VERSION.SDK_INT>=19) {
            config.statusBarColor = resources.getColor(R.color.color_primary_dark);
            config.navigationBarColor = Color.BLACK;
        }
        config.screenOrientation = PageConfig.ScreenOrientation.PORTRAIT;
//        config.defaultItemConfig.rotate = true;

        drawer.items = Utils.loadAppDrawerShortcuts(drawer);
        Utils.layoutItemsInTable(config, drawer.items, true);

        PageIndicator pi = Utils.addPageIndicator(drawer, 0, 0, 1, true);
        pi.getCell().set(0, 0, 5, 1);
        ItemConfig ic = pi.modifyItemConfig();
        ic.copyFrom(config.defaultItemConfig);
        ic.enabled = false;
        ic.pinMode = ItemConfig.PinMode.XY;
        ic.rotate = false;
        ic.box.av = Box.AlignV.TOP;
        ic.box.size[Box.MT] = (int) (4 * resources.getDisplayMetrics().density);
        ic.box_s = ic.box.toString(config.defaultItemConfig.box);

        drawer.items.remove(pi);
        drawer.items.add(0, pi);
    }

    private static void setupLockScreen(Page lockscreen) {
        Item unlocker = Utils.addUnlocker(lockscreen, 0, 0, 1, 1, 1, true);
        unlocker.getCell().set(1, 2, 4, 5);

        Context context = LLApp.get();
        Intent intent = new Intent(context, Customize.class);
        intent.putExtra(Customize.INTENT_EXTRA_GOTO, Customize.INTENT_EXTRA_GOTO_GENERAL_LOCK_SCREEN);
        Shortcut s = Utils.addShortcut(context.getString(R.string.ds_hls), null, intent, lockscreen, 0, 0, 1, true);
        s.getCell().set(0, 0, 5, 2);
        ShortcutConfig sc = s.modifyShortcutConfig();
        sc.iconVisibility = false;
        sc.labelMaxLines = 7;
        s.notifyChanged();

        lockscreen.setModified();
    }

    private static Item findItem(ArrayList<Item> items, String[] component_names) {
        // try to match package and class first, otherwise try to match the package only
        Item item = findItemByPackageName(items, component_names, false);
        if(item == null) {
            item = findItemByPackageName(items, component_names, true);
        }

        return item;
    }

    private static Item findItemByPackageName(ArrayList<Item> items, String[] component_names, boolean package_only) {
        for(Item i : items) {
            if(!(i instanceof Shortcut)) continue;

            Shortcut s=(Shortcut)i;
            Intent intent=s.getIntent();
            final ComponentName item_cn = intent.getComponent();
            if(item_cn != null) {
                final String item_pkg = item_cn.getPackageName();
                for (String cn_s : component_names) {
                    ComponentName cn = ComponentName.unflattenFromString(cn_s);
                    if ((package_only && item_pkg.equals(cn.getPackageName())) || cn.compareTo(item_cn) == 0) {
                        return s;
                    }
                }
            }
        }

        return null;
    }

    private static Item installDockShortcut(Page dashboard, Page app_drawer, int x, int y, String[] component_names) {
        Item item = findItem(app_drawer.items, component_names);
        if(item != null) {
            Shortcut shortcut = Utils.copyShortcut(item, dashboard, x, y, 1);

            ItemConfig ic_dock=shortcut.modifyItemConfig();
            ic_dock.pinMode=ItemConfig.PinMode.XY;
//            ic_dock.rotate=true;
            ic_dock.box.av = Box.AlignV.BOTTOM;
            ic_dock.box_s = ic_dock.box.toString(null);
            ShortcutConfig sc_dock = shortcut.modifyShortcutConfig();
            sc_dock.iconColorFilter = 0x00ffffff;
            sc_dock.iconReflection = true;
            sc_dock.iconReflectionOverlap = 0;
            sc_dock.iconReflectionSize = 0.7f;
            sc_dock.labelVisibility = false;
            shortcut.getCell().set(x, y, x+1, y+1);

            shortcut.notifyChanged();

            return shortcut;
        }

        return null;
    }

    private static void installShortcut(Page page, int x, int y, String icon_code, String label, String uri, Intent intent) {
        if(intent==null) {
            intent=new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        }
        Shortcut new_item=new Shortcut(page);
        int new_id=page.findFreeItemId();
        File icon_dir=page.getAndCreateIconDir();
        icon_dir.mkdirs();
        new_item.init(new_id, new Rect(x, y, x+1, y+1), null, label, intent);
        int size = Utils.getStandardIconSize();
        Bitmap icon = Utils.createIconFromText(size, icon_code);
        Utils.saveIconToFile(new_item.getCustomIconFile(), icon);
        icon.recycle();
        page.addItem(new_item);
    }

    public static Page createUserMenuPage(Context context) {
        LightningEngine engine = LLApp.get().getAppEngine();

        Resources resources = context.getResources();
        int large_icon_size = resources.getDimensionPixelSize(R.dimen.large_icon_text_size);
        int color_primary_dark = resources.getColor(R.color.color_primary_dark);

        Page page = engine.getOrLoadPage(Page.USER_MENU_PAGE);
        PageConfig page_config=page.config;
        page_config.gridPColumnNum = 5;
//        page_config.gridPRowNum = 7;
        page_config.gridPRowMode = PageConfig.SizeMode.AUTO;
//        page_config.gridPRowSize = (int)(large_icon_size * resources.getDisplayMetrics().scaledDensity * 1.5f);

        File icon_dir=page.getAndCreateIconDir();
        icon_dir.mkdirs();

        Folder folder = new Folder(page);
        folder.init(Utils.USER_MENU_ITEM_ID, new Rect(2, 1, 3, 2), null, resources.getString(R.string.an_um), Utils.getFolderIntent(Utils.USER_MENU_ITEM_ID), Page.USER_MENU_PAGE);
        folder.setAppDrawerHidden(true);

        ShortcutConfig shortcutConfig = page.config.defaultShortcutConfig;//getShortcutConfig();
        shortcutConfig.iconScale = 0.7f;
//        shortcutConfig.iconVisibility = false;
//        shortcutConfig.labelFontSize = large_icon_size;
//        shortcutConfig.labelFontTypeFace = API.SHORTCUT_ICON_FONT;
//        shortcutConfig.labelFontColor = Color.WHITE;
//        shortcutConfig.labelShadow = false;

        FolderConfig folderConfig = new FolderConfig();
        folderConfig.copyFrom(page_config.defaultFolderConfig);
        folder.setFolderConfig(folderConfig);
        folderConfig.animationIn = FolderConfig.FolderAnimation.SLIDE_FROM_BOTTOM;
        folderConfig.animationOut = FolderConfig.FolderAnimation.SLIDE_FROM_TOP;
        folderConfig.titleVisibility = false;
        folderConfig.wAV = Box.AlignV.BOTTOM;
        folderConfig.box.ccn = resources.getColor(R.color.color_primary);
        folderConfig.box.border_color[Box.BCT] = color_primary_dark;
//        folderConfig.box.border_color[Box.BCB] = color_primary_dark;
        folderConfig.box.size[Box.BT] = 1;
//        folderConfig.box.size[Box.BB] = 2;
        folderConfig.box_s = folderConfig.box.toString(null);

        page.addItem(folder);

        // wallpaper, theme, add, edit, settings
        createUserMenuItem(context, page, "X", context.getString(R.string.background_t), "wallpaper", 0);
        createUserMenuItem(context, page, "P", context.getString(R.string.um_t), "theme", 1);
        createUserMenuItem(context, page, "G", context.getString(R.string.an_ao), "add_item", 2);
        createUserMenuItem(context, page, "O", context.getString(R.string.an_el), "edit_layout", 3);
        createUserMenuItem(context, page, "N", context.getString(R.string.mi_es_settings), "settings", 4);

        page.save();

        return page;
    }

    private static void createUserMenuItem(Context context, Page page, String icon_code, String label, String name, int x) {
        int id;
        Shortcut shortcut;
        Intent intent;

        File icon_dir=page.getAndCreateIconDir();

        id = page.findFreeItemId();
        shortcut = new Shortcut(page);
        intent = new Intent(context, Dashboard.class);
        intent.putExtra(LightningIntent.INTENT_EXTRA_ACTION, GlobalConfig.RUN_SCRIPT);
        intent.putExtra(LightningIntent.INTENT_EXTRA_DATA, String.valueOf(ScriptManager.BUILTIN_USER_MENU));
        shortcut.init(id, new Rect(x, 0, x+1, 1), null, label, intent);
        shortcut.setName(name);
        Bitmap icon = Utils.createIconFromText(shortcut.getStdIconSize(), icon_code);
        Utils.saveIconToFile(shortcut.getDefaultIconFile(), icon);
        page.addItem(shortcut);
    }
}
