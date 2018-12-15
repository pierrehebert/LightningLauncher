package net.pierrox.lightning_launcher.data;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.os.SystemClock;
import android.util.Log;

import net.pierrox.lightning_launcher.configuration.*;
import net.pierrox.lightning_launcher.BuildConfig;
import net.pierrox.lightning_launcher.engine.LightningEngine;
import net.pierrox.lightning_launcher.views.ItemLayout;
import net.pierrox.lightning_launcher.views.item.ItemView;
import net.pierrox.lightning_launcher.views.item.WidgetView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

public class Page implements Item.OnItemEventListener, ItemConfigStylable, ShortcutConfigStylable, FolderConfigStylable {

    // home pages: 0->98, app app_drawer:99, folders: 100->N
    // these ids are stored as string, even if only numeric values today
    public static final int NONE =-1;
    public static final int FIRST_DASHBOARD_PAGE=0;
    public static final int LAST_DASHBOARD_PAGE=98;
    public static final int APP_DRAWER_PAGE=99;
    public static final int FIRST_FOLDER_PAGE=100;
    public static final int LAST_FOLDER_PAGE=999;
    public static final int USER_MENU_PAGE=0x7fff;
    public static final int MERGED_APP_DRAWER_PAGE=0x7ffe;


    public interface PageListener extends Item.OnItemEventListener{
        public void onPageLoaded(Page page);
        public void onPageRemoved(Page page);
        public void onPagePaused(Page page);
        public void onPageResumed(Page page);

        public void onPageModified(Page page);

        // TODO move this to engine, this is not a page specific data
        public void onPageEditModeEntered(Page page);
        public void onPageEditModeLeaved(Page page);

        public void onPageItemLoaded(Item item);
        public void onPageItemDestroyed(Item item);
        public void onPageItemAdded(Item item);
        public void onPageItemBeforeRemove(Item item);
        public void onPageItemRemoved(Page page, Item item);
        public void onPageItemChanged(Page page, Item item);

        public void onPageItemZIndexChanged(Page page, int old_index, int new_index);

        public void onPageFolderWindowChanged(Page page, Folder folder);
    }

    public static class EmptyPageListener implements PageListener {
        @Override public void onPageLoaded(Page page) { }
        @Override public void onPageRemoved(Page page) { }
        @Override public void onPagePaused(Page page) { }
        @Override public void onPageResumed(Page page) { }
        @Override public void onPageModified(Page page) { }
        @Override public void onPageEditModeEntered(Page page) { }
        @Override public void onPageEditModeLeaved(Page page) { }
        @Override public void onPageItemLoaded(Item item) { }
        @Override public void onPageItemDestroyed(Item item) { }
        @Override public void onPageItemAdded(Item item) { }
        @Override public void onPageItemBeforeRemove(Item item) { }
        @Override public void onPageItemRemoved(Page page, Item item) { }
        @Override public void onPageItemChanged(Page page, Item item) { }
        @Override public void onPageItemZIndexChanged(Page page, int old_index, int new_index) { }
        @Override public void onPageFolderWindowChanged(Page page, Folder folder) { }
        @Override public void onItemPaused(Item item) { }
        @Override public void onItemResumed(Item item) { }
        @Override public void onItemVisibilityChanged(Item item) { }
        @Override public void onItemAlphaChanged(Item item) { }
        @Override public void onItemTransformChanged(Item item, boolean fast) { }
        @Override public void onItemCellChanged(Item item) { }
        @Override public void onItemBindingsChanged(Item item, boolean apply) { }
        @Override public void onShortcutLabelChanged(Shortcut shortcut) { }
        @Override public void onFolderPageIdChanged(Folder folder, int oldPageId) { }
    }

    // TODO both fields are the same
    private LightningEngine mLightningEngine;
    protected PageListener mListener;
    private File mIconDir;

	private boolean modified;
	public int id;
	public PageConfig config;
	public ArrayList<Item> items;

    private float mCurrentViewCellWidth;
    private float mCurrentViewCellHeight;

    private int mResumeCount;

    public Page(LightningEngine lightningEngine, int id) {
        mLightningEngine = lightningEngine;
        mListener = lightningEngine;
        this.id = id;
        mIconDir = getIconDir(mLightningEngine.getBaseDir(), id);
    }

    @Override
    public String toString() {
        return "Page:"+id+" ("+hashCode()+")";
    }

    public static boolean isDashboard(int p) {
        return p>=FIRST_DASHBOARD_PAGE && p<=LAST_DASHBOARD_PAGE;
    }

    public boolean isDashboard() {
        return isDashboard(id);
    }

    public static boolean isFolder(int p) {
        return (p>=FIRST_FOLDER_PAGE && p<=LAST_FOLDER_PAGE) || p == USER_MENU_PAGE;
    }

    public boolean isFolder() {
        return isFolder(id);
    }


    public void setEngine(LightningEngine lightningEngine) {
        mLightningEngine = lightningEngine;
    }

    public LightningEngine getEngine() {
        return mLightningEngine;
    }

    public void pause() {
        mResumeCount--;
//        Log.i("XXX", "pause page "+id+" "+mResumeCount);
        if(mResumeCount == 0) {
//            Log.i("XXX", "pause page "+id);
            for(Item item : items) {
                item.pause();
            }
            mListener.onPagePaused(this);
        }
    }

    public void resume() {
        mResumeCount++;
//        Log.i("XXX", "resume page "+id+" "+mResumeCount);
        if(mResumeCount == 1) {
//            Log.i("XXX", "resume page "+id);
            for (Item item : items) {
                item.resume();
            }
            mListener.onPageResumed(this);
        }
    }

    public void enterEditMode() {
        mListener.onPageEditModeEntered(this);
    }

    public void leaveEditMode() {
        mListener.onPageEditModeLeaved(this);
    }

    public void create() {
//        Log.i("XXX", "create page "+page);

        long t1 = BuildConfig.IS_BETA ? SystemClock.uptimeMillis() : 0;

        loadConfig();

        if(isFolder(id)) {
            // use the default folder config from the first home page, which is not perfect but better to have multiple folder config per folder
            Page home = mLightningEngine.getOrLoadPage(FIRST_DASHBOARD_PAGE);
            config.defaultFolderConfig=home.config.defaultFolderConfig;
        }

        loadItems();

        if(BuildConfig.IS_BETA) {
            Log.i("LL", "page "+id+" created in " + (SystemClock.uptimeMillis()-t1)+"ms, "+items.size()+" items, json file size: "+getItemsFile().length()+", config file size: "+getPageConfigFile().length());
        }
    }

    public void notifyLoaded(ItemLayout from) {
        mListener.onPageLoaded(this);
    }

    public void destroy() {
//        Log.i("XXX", "destroy page "+page);
        if(mResumeCount > 0) {
            pause();
            mResumeCount = 0;
        }
        for(Item item : items) {
            mListener.onPageItemDestroyed(item);
            item.onDestroy();
        }
    }

    private boolean mIsBeingRemoved; // prevent endless recursion
    public void remove() {
        if(!mIsBeingRemoved) {
            mIsBeingRemoved = true;

            for (Item item : items) {
                item.onRemove(false);
            }

            mListener.onPageRemoved(this);

            getPageIconFile().delete();
            Utils.deleteDirectory(getPageDir(), true);

            mIsBeingRemoved = false;
        }
    }

    public boolean isResumed() {
        return mResumeCount > 0;
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified() {
        this.modified = true;
    }

    public void save() {
        if(modified && id != Page.NONE) {
            saveConfig();
            saveItems();
            modified=false;
        }
    }
    
    public static int composeItemId(int page, int item_id) {
    	return page<<16 | (item_id&0xffff);
    }
    
    public static int getBaseItemId(int item_id) {
    	return item_id&0xffff;
    }
    
    public int findFreeItemId() {
        // linked with Utils.getPageForItemId
        int max=0;
        for(Item i : items) {
            if(i.getId()>max) max=i.getId();
        }
        return composeItemId(id, max+1);
    }

    public Item findItemById(int id) {
        for(Item i : items) {
            if(i.getId()==id) return i;
        }
        return null;
    }

    public File getPageDir() {
        return getPageDir(mLightningEngine.getBaseDir(), id);
    }

    public File getItemsFile() {
        return getItemsFile(mLightningEngine.getBaseDir(), id);
    }

    public File getPageIconFile() {
        return getPageIconFile(mLightningEngine.getBaseDir(), id);
    }

    public File getPageConfigFile() {
        return getPageConfigFile(mLightningEngine.getBaseDir(), id);
    }

    public File getWallpaperFile() {
        return getWallpaperFile(mLightningEngine.getBaseDir(), id);
    }

    public File getIconDir() {
        return mIconDir;
    }
    
    public File getAndCreateIconDir() {
    	return getAndCreateIconDir(mLightningEngine.getBaseDir(), id);
    }

    public static boolean exists(File base_dir, int id) {
        return getPageDir(base_dir, id).exists();
    }

    // static versions
    // TODO: remove when possible
    public static File getPageDir(File base_dir, int id) {
        return new File(FileUtils.getPagesDir(base_dir), String.valueOf(id));
    }

    public static File getIconDir(File base_dir, int id) {
    	return new File(getPageDir(base_dir, id), "icon");
    }
    
    public static File getAndCreateIconDir(File base_dir, int id) {
    	File icon_dir = getIconDir(base_dir, id);
        if(!icon_dir.exists()) {
            icon_dir.mkdirs();
        }
        return icon_dir;
    }

    // TODO: remove when possible, or move to PageManager
    public static File getItemsFile(File base_dir, int id) {
        return new File(getPageDir(base_dir, id), "items");
    }

    public static File getPageIconFile(File base_dir, int id) {
        return new File(getPageDir(base_dir, id), "i");
    }

    public static File getWallpaperFile(File base_dir, int id) {
    	return new File(getPageDir(base_dir, id), "wp");
    }
    
    public static File getPageConfigFile(File base_dir, int id) {
        return new File(getPageDir(base_dir, id), "conf");
    }

    // TODO: move this into PageManager
    public static int reservePage(File base_dir, boolean folder) {
    	int start, end;
    	if(folder) {
    		start = FIRST_FOLDER_PAGE;
    		end = LAST_FOLDER_PAGE;
    	} else {
    		start = FIRST_DASHBOARD_PAGE;
    		end = LAST_DASHBOARD_PAGE;
    	}
    	
        for(int i=start; i<=end; i++) {
            File page_dir=getPageDir(base_dir, i);
            if(!page_dir.exists()) {
                page_dir.mkdirs();
                return i;
            }
        }

        return NONE;
    }

    @Override
    public void setItemConfig(ItemConfig c) {
        config.defaultItemConfig = c;
    }

    @Override
    public ItemConfig getItemConfig() {
        return config.defaultItemConfig;
    }

    @Override
    public ItemConfig modifyItemConfig() {
        return config.defaultItemConfig;
    }

    @Override
    public ShortcutConfig getShortcutConfig() {
        return config.defaultShortcutConfig;
    }

    @Override
    public void setShortcutConfig(ShortcutConfig c) {
        config.defaultShortcutConfig = c;
    }

    @Override
    public ShortcutConfig modifyShortcutConfig() {
        return config.defaultShortcutConfig;
    }

    @Override
    public FolderConfig getFolderConfig() {
        return config.defaultFolderConfig;
    }

    @Override
    public void setFolderConfig(FolderConfig fc) {
        config.defaultFolderConfig = fc;
    }

    @Override
    public FolderConfig modifyFolderConfig() {
        return config.defaultFolderConfig;
    }


    @Override
    public void onItemPaused(Item item) {
        mListener.onItemPaused(item);
    }

    @Override
    public void onItemResumed(Item item) {
        mListener.onItemResumed(item);
    }

    @Override
    public void onItemVisibilityChanged(Item item) {
        mListener.onItemVisibilityChanged(item);
    }

    @Override
    public void onItemAlphaChanged(Item item) {
        mListener.onItemAlphaChanged(item);
    }

    @Override
    public void onItemTransformChanged(Item item, boolean fast) {
        mListener.onItemTransformChanged(item, fast);
    }

    @Override
    public void onItemCellChanged(Item item) {
        mListener.onItemCellChanged(item);
    }

    @Override
    public void onItemBindingsChanged(Item item, boolean apply) {
        mListener.onItemBindingsChanged(item, apply);
    }

    @Override
    public void onShortcutLabelChanged(Shortcut shortcut) {
        mListener.onShortcutLabelChanged(shortcut);
    }

    @Override
    public void onFolderPageIdChanged(Folder folder, int oldPageId) {
        if(items.contains(folder)) {
            mListener.onFolderPageIdChanged(folder, oldPageId);
        }
    }

    public void notifyModified() {
        modified = true;
        mListener.onPageModified(this);
    }

    public void setItemZIndex(Item item, int new_index) {
        int old_index = items.indexOf(item);
        if(old_index != new_index && new_index >=0 && new_index < items.size()) {
            items.remove(old_index);
            items.add(new_index, item);

            modified = true;
            mListener.onPageItemZIndexChanged(this, old_index, new_index);
        }
    }

    public void addItem(Item item) {
        addItem(item, null);
    }

    public void addItem(Item item, Integer index) {
        modified = true;
        if(index == null) {
            items.add(item);
        } else {
            items.add(index, item);
        }
        mListener.onPageItemAdded(item);

        if(mResumeCount > 0) {
            item.resume();
        }
    }

    public void removeItem(Item item, boolean keepResources) {
        mListener.onPageItemBeforeRemove(item);

        modified = true;
        if(mResumeCount > 0) item.pause();
        item.onDestroy();
        item.onRemove(keepResources);
        items.remove(item);
        ArrayList<File> icons = new ArrayList<>();
        item.getIconFiles(getIconDir(), icons);
        for(File f : icons) {
            f.delete();
        }
        mListener.onPageItemRemoved(this, item);
    }

    public void startItemChange(Item item) {
        modified = true;
        if (mResumeCount > 0) item.pause();
        item.onDestroy();
    }

    public void endItemChange(Item item) {
        item.onCreate();

        mListener.onPageItemChanged(this, item);

        if(mResumeCount > 0) {
            item.resume();
        }
    }

    public void notifyItemChanged(Item item) {
        modified = true;
        if(mResumeCount > 0) item.pause();
        item.onDestroy();
        item.onCreate();
        mListener.onPageItemChanged(this, item);

        if(mResumeCount > 0) {
            item.resume();
        }
    }

    public void notifyFolderWindowChanged(Folder folder) {
        mListener.onPageFolderWindowChanged(this, folder);
    }

    public void setCurrentViewSize(int width, int height, float cell_width, float cell_height) {
        mCurrentViewCellWidth = cell_width;
        mCurrentViewCellHeight = cell_height;
    }

    public float getCurrentViewCellWidth() {
        return mCurrentViewCellWidth==0 ? Utils.getStandardIconSize() : mCurrentViewCellWidth;
    }

    public float getCurrentViewCellHeight() {
        return mCurrentViewCellHeight==0 ? Utils.getStandardIconSize() : mCurrentViewCellHeight;
    }

    public void reload() {
        int count = mResumeCount;
	    destroy();
	    create();
        if(count > 0) {
            resume();
            mResumeCount = count;
        }
	    notifyModified();
    }

    private static final String _backgroundColor = "backgroundColor";
    private static final String _backgroundWallpaper = "backgroundWallpaper";
    private static final String _backgroundWallpaperScroll = "backgroundWallpaperScroll";
    private static final String _backgroundWallpaperTintColor = "backgroundWallpaperTintColor";
    private static final String _backgroundWallpaperWidth = "backgroundWallpaperWidth";
    private static final String _backgroundWallpaperHeight = "backgroundWallpaperHeight";
    private static final String _gridColumnMode = "gridColumnMode";
    private static final String _gridColumnNum = "gridColumnNum";
    private static final String _gridColumnSize = "gridColumnSize";
    private static final String _gridRowMode = "gridRowMode";
    private static final String _gridRowNum = "gridRowNum";
    private static final String _gridRowSize = "gridRowSize";
    private static final String _layoutMode = "layoutMode";
    private static final String _transpBarOverlap = "transpBarOverlap";
    private static final String _statusBarTransparent = "statusBarTransparent";
    private static final String _navigationBarTransparent = "navigationBarTransparent";
    private static final String _wrap = "wrap";

    private void loadConfig() {
        File json_file = getPageConfigFile();
        JSONObject json=FileUtils.readJSONObjectFromFile(json_file);
        if(json==null) {
            json=new JSONObject();
        }

        PageConfig c=new PageConfig();
        c.applyDefaultFolderConfig();

        // for low dpi devices, use icon filter by default
        if(Utils.getStandardIconSize()==36) {
            c.defaultShortcutConfig.iconFilter=true;
        }

        c.loadFieldsFromJSONObject(json, c);

        File icon_dir=getIconDir();
        c.defaultItemConfig.loadAssociatedIcons(icon_dir, Item.NO_ID);
        c.defaultShortcutConfig.loadAssociatedIcons(icon_dir, Item.NO_ID);
        c.defaultFolderConfig.loadAssociatedIcons(icon_dir, Item.NO_ID);

        // legacy with < 90
        if(json.has(_backgroundColor) || json.has(_backgroundWallpaper) || json.has(_backgroundWallpaperScroll) || json.has(_backgroundWallpaperTintColor) || json.has(_backgroundWallpaperWidth) || json.has(_backgroundWallpaperHeight)) {
            int backgroundColor = json.optInt(_backgroundColor, 0xff000000);
            boolean backgroundWallpaper = json.optBoolean(_backgroundWallpaper, true);
            boolean backgroundWallpaperScroll = json.optBoolean(_backgroundWallpaperScroll, false);
            int backgroundWallpaperTintColor = json.optInt(_backgroundWallpaperTintColor, 0);
            int backgroundWallpaperWidth = json.optInt(_backgroundWallpaperWidth, 0);
            int backgroundWallpaperHeight = json.optInt(_backgroundWallpaperHeight, 0);

            if(backgroundWallpaper) {
                c.bgColor = backgroundWallpaperTintColor;
            } else {
                c.bgColor = backgroundColor;
            }
            c.bgSystemWPScroll = backgroundWallpaperScroll;
            c.bgSystemWPWidth = backgroundWallpaperWidth;
            c.bgSystemWPHeight = backgroundWallpaperHeight;
        }

        if(json.has(_gridColumnMode) || json.has(_gridColumnNum) || json.has(_gridColumnSize) || json.has(_gridRowMode) || json.has(_gridRowNum) || json.has(_gridRowSize)) {
            c.gridPColumnMode = c.gridLColumnMode = PageConfig.SizeMode.valueOf(json.optString(_gridColumnMode, PageConfig.SizeMode.NUM.name()));
            c.gridPColumnNum = c.gridLColumnNum = json.optInt(_gridColumnNum, 5);
            c.gridPColumnSize = c.gridLColumnSize = json.optInt(_gridColumnSize, 100);
            c.gridPRowMode = c.gridLRowMode = PageConfig.SizeMode.valueOf(json.optString(_gridRowMode, PageConfig.SizeMode.NUM.name()));
            c.gridPRowNum = c.gridLRowNum = json.optInt(_gridRowNum, 5);
            c.gridPRowSize = c.gridLRowSize = json.optInt(_gridRowSize, 100);
        }

        // legacy with < 135
        if(json.has(_layoutMode)) {
            c.newOnGrid = c.defaultItemConfig.onGrid = "GRID".equals(json.optString(_layoutMode));
        }

        // legacy with < 208
        boolean has_statusBarTransparent = true;
        if(json.has(_statusBarTransparent)) {
            has_statusBarTransparent = json.optBoolean(_statusBarTransparent);
            c.statusBarColor = has_statusBarTransparent ? 0 : Color.BLACK;
        }
        boolean has_navigationBarTransparent = true;
        if(json.has(_navigationBarTransparent)) {
            has_navigationBarTransparent = json.optBoolean(_navigationBarTransparent);
            c.navigationBarColor = has_navigationBarTransparent ? 0 : Color.BLACK;
        }

        if(json.has(_transpBarOverlap)) {
            boolean overlap = json.optBoolean(_transpBarOverlap);
            c.statusBarOverlap = overlap && has_statusBarTransparent;
            c.navigationBarOverlap = overlap && has_navigationBarTransparent;
        }

        if(json.has(_wrap)) {
            boolean wrap = json.optBoolean(_wrap);
            c.wrapX = wrap;
            c.wrapY = wrap;
        }


        config = c;
    }

    private void loadItems() {

        items=null;

        JSONObject json_data=FileUtils.readJSONObjectFromFile(getItemsFile());
        long t1 = BuildConfig.IS_BETA ? SystemClock.uptimeMillis() : 0;
        if(json_data!=null) {
            try {
                JSONArray json_items=json_data.getJSONArray(JsonFields.ITEMS);
                int l=json_items.length();
                items=new ArrayList<>(l);
                for(int i=0; i<l; i++) {
                    JSONObject o=json_items.getJSONObject(i);
                    Item item=Item.loadItemFromJSONObject(this, o);
                    if(item!=null) {
                        items.add(item);
                        mListener.onPageItemLoaded(item);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        if(items == null) {
            items=new ArrayList<>();
        }
        if(BuildConfig.IS_BETA) {
            Log.i("LL", "loadItems for page "+id+" in "+(SystemClock.uptimeMillis()-t1));
        }
    }

    public JSONObject getConfigAsJSONObject() {
        try {
            JSONObject o = JsonLoader.toJSONObject(config, new PageConfig());
            o.put("defaultItemConfig", JsonLoader.toJSONObject(config.defaultItemConfig, new ItemConfig()));
            o.put("defaultShortcutConfig", JsonLoader.toJSONObject(config.defaultShortcutConfig, new ShortcutConfig()));
            o.put("defaultFolderConfig", JsonLoader.toJSONObject(config.defaultFolderConfig, new FolderConfig()));
            return o;
        } catch (JSONException e) {
            return null;
        }
    }

    public void saveConfig() {
        try {
            JSONObject o = getConfigAsJSONObject();
            FileUtils.saveStringToFile(o.toString(), getPageConfigFile());
        } catch (IOException e) {
            // pass
        }
    }

    public ArrayList<Folder> findAllOpeners() {
        return mLightningEngine.findAllFolderPageOpeners(id);
    }

    public Folder findFirstOpener() {
        return mLightningEngine.findFirstFolderPageOpener(id);
    }

    private void saveItems() {
        //Log.i("XXX", "saveItems start for page " + page);
        //long t1= SystemClock.uptimeMillis();

        try {
            JSONArray json_items=new JSONArray();

            getAndCreateIconDir();
            for(Item item : items) {
                JSONObject json_item=item.toJSONObject();
                json_items.put(json_item);

                if(item.getClass() == Widget.class) {
                    ItemView v = Utils.findItemViewInAppScreens(item);
                    if(v != null && ((WidgetView)v).isGood()) {
                        OutputStream os = null;
                        try {
                            os = new FileOutputStream(item.getDefaultIconFile());
                            Bitmap b = Bitmap.createBitmap(item.getViewWidth(), item.getViewHeight(), Bitmap.Config.ARGB_8888);
                            Canvas canvas = new Canvas(b);
                            v.draw(canvas);
                            b.compress(Bitmap.CompressFormat.PNG, 100, os);
                        } catch(Throwable e) {
                            e.printStackTrace();
                        } finally {
                            if(os != null) try { os.close(); } catch(IOException e) {}
                        }
                    }
                }
            }

            JSONObject data=new JSONObject();
            data.put(JsonFields.ITEMS, json_items);

            FileUtils.saveStringToFile(data.toString(), getItemsFile());
        } catch(Exception e) {
            e.printStackTrace();
        }
        //Log.i("LL", "saveItems in "+(SystemClock.uptimeMillis()-t1));
    }
}