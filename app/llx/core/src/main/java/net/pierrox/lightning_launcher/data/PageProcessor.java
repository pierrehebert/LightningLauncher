package net.pierrox.lightning_launcher.data;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Matrix;
import android.util.Pair;
import android.util.SparseIntArray;

import net.pierrox.lightning_launcher.LLApp;
import net.pierrox.lightning_launcher.configuration.FolderConfig;
import net.pierrox.lightning_launcher.configuration.GlobalConfig;
import net.pierrox.lightning_launcher.configuration.ItemConfig;
import net.pierrox.lightning_launcher.configuration.JsonFields;
import net.pierrox.lightning_launcher.configuration.PageConfig;
import net.pierrox.lightning_launcher.engine.LightningEngine;
import net.pierrox.lightning_launcher.script.Script;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class PageProcessor {
    private ComponentName drawer_from;
    private ComponentName drawer_to;
    private String pkg_name_from;
    private String pkg_name_to;

    private boolean do_scale;
    private float sx;
    private float sy;
//    private boolean portrait;
//    private int from_screen_width;
//    private int from_screen_height;
//    private int from_sb_height;
//    private int to_screen_width;
//    private int to_screen_height;
//    private int to_sb_height;
    
    private boolean enforce_holo;
    private boolean process_global_config;

    private SparseIntArray translated_page_ids;
    private SparseIntArray translated_script_ids;

    public PageProcessor() {
        ComponentName ll_app_drawer = new ComponentName(LLApp.LL_PKG_NAME, LLApp.LL_PKG_NAME+".activities.AppDrawer");
        ComponentName llx_app_drawer = new ComponentName(LLApp.LLX_PKG_NAME, LLApp.LLX_PKG_NAME+".activities.AppDrawerX");

        String my_pkg_name = LLApp.get().getPackageName();
        boolean is_llx = my_pkg_name.equals(LLApp.LLX_PKG_NAME);
        drawer_from = is_llx ? ll_app_drawer : llx_app_drawer;
        drawer_to = is_llx ? llx_app_drawer : ll_app_drawer;
        pkg_name_from = is_llx ? LLApp.LL_PKG_NAME : LLApp.LLX_PKG_NAME;
        pkg_name_to = is_llx ? LLApp.LLX_PKG_NAME : LLApp.LL_PKG_NAME;
    }

    public void setTranslatedPageIds(SparseIntArray ids) {
        translated_page_ids = ids;
    }

    public void setTranslatedScriptIds(SparseIntArray ids) {
        translated_script_ids = ids;
    }

//    public void setPortrait(boolean portrait) {
//        this.portrait = portrait;
//    }

    public void setProcessGlobalConfig(boolean process_global_config) {
        this.process_global_config = process_global_config;
    }

    public void setScreenCharacteristics(int from_screen_width, int from_screen_height, int from_sb_height, int to_screen_width, int to_screen_height, int to_sb_height) {
//        this.from_screen_width = from_screen_width;
//        this.from_screen_height = from_screen_height;
//        this.from_sb_height = from_sb_height;
//        this.to_screen_width = to_screen_width;
//        this.to_screen_height = to_screen_height;
//        this.to_sb_height = to_sb_height;
        if(to_screen_width != 0) {
            do_scale = true;
            sx = to_screen_width / (float)from_screen_width;
            sy = (to_screen_height - to_sb_height) / (float)(from_screen_height - from_sb_height);
        }
    }
    
    public void setEnforceHoloSelection(boolean enforce_holo) {
        this.enforce_holo = enforce_holo;
    }

    public void postProcessPages(ArrayList<Page> pages) {
        for(Page page : pages) {
            postProcessPage(page);
            page.setModified();
            page.save();
        }

        if(process_global_config && pages.size() > 0) {
            // TODO see whether this can be replaced with direct access of engine.getGlobalConfigFile()
            File base_dir = pages.get(0).getEngine().getBaseDir();
            File global_config_file = FileUtils.getGlobalConfigFile(base_dir);
            JSONObject global_config_json = FileUtils.readJSONObjectFromFile(global_config_file);
            if (global_config_json != null) {
                GlobalConfig gc = new GlobalConfig();
                gc.loadFieldsFromJSONObject(global_config_json, gc);
                postProcessEventAction(gc.homeKey);
                postProcessEventAction(gc.menuKey);
                postProcessEventAction(gc.searchKey);
                postProcessEventAction(gc.itemTap);
                postProcessEventAction(gc.itemLongTap);
                postProcessEventAction(gc.bgTap);
                postProcessEventAction(gc.bgDoubleTap);
                postProcessEventAction(gc.bgLongTap);
                postProcessEventAction(gc.swipeLeft);
                postProcessEventAction(gc.swipeRight);
                postProcessEventAction(gc.swipeUp);
                postProcessEventAction(gc.swipeDown);
                postProcessEventAction(gc.swipe2Left);
                postProcessEventAction(gc.swipe2Right);
                postProcessEventAction(gc.swipe2Up);
                postProcessEventAction(gc.swipe2Down);
                postProcessEventAction(gc.screenOff);
                postProcessEventAction(gc.screenOn);
                postProcessEventAction(gc.orientationPortrait);
                postProcessEventAction(gc.orientationLandscape);
                postProcessEventAction(gc.itemAdded);
                postProcessEventAction(gc.itemRemoved);
                postProcessEventAction(gc.menu);

                // check that the home page is valid, otherwise fix it
                boolean valid = false;
                for(Page page : pages) {
                    if(page.id == gc.homeScreen) {
                        valid = true;
                        break;
                    }
                }
                if(!valid) {
                    // use the first dashboard page
                    if(pages.size() > 0) {
                        for(Page page : pages) {
                            int id = page.id;
                            if(Page.isDashboard(id)) {
                                gc.homeScreen = id;
                                break;
                            }
                        }
                    } else {
                        gc.homeScreen = Page.FIRST_DASHBOARD_PAGE;
                    }
                }

                try {
                    JSONObject o = JsonLoader.toJSONObject(gc, new GlobalConfig());
                    FileUtils.saveStringToFile(o.toString(), global_config_file);
                } catch (IOException e) {
                    // pass
                }
            }
        }
    }
    
    public void copyAndTranslatePage(Page pageFrom, LightningEngine engineTo, boolean keepAppWidgetId) {
        byte[] buffer = new byte[1024];

        int old_page_id = pageFrom.id;
        int new_page_id = translated_page_ids.get(old_page_id);

        File baseDirFrom = pageFrom.getEngine().getBaseDir();
        File baseDirTo = engineTo.getBaseDir();
        Page p = engineTo.getPageManager().getPage(new_page_id);
        if(p != null) {
            throw new RuntimeException("Attempt to overwrite an existing page during import");
        } else {
            // make sure that the place is clean
            Utils.deleteDirectory(Page.getPageDir(baseDirTo, new_page_id), true);
        }

        Page pageTo = new Page(engineTo, new_page_id);


        File old_icon_dir = pageFrom.getIconDir();
        File new_icon_dir = pageTo.getAndCreateIconDir();

        pageTo.config = new PageConfig();
        pageTo.config.copyFrom(pageFrom.config);
        pageTo.items = new ArrayList<>(pageFrom.items.size());

        for(Item old_item : pageFrom.items) {
            int old_item_id = old_item.getId();
            int new_item_id = Page.composeItemId(new_page_id, Page.getBaseItemId(old_item_id));

            FileUtils.copyItemFiles(old_item_id, old_icon_dir, new_item_id, new_icon_dir);

            Item new_item;
            try {
                JSONObject json = old_item.toJSONObject();
                json.put(JsonFields.ITEM_ID, new_item_id);
                if(!keepAppWidgetId && old_item instanceof Widget) {
                    json.put(JsonFields.WIDGET_APP_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
                }
                new_item = Item.loadItemFromJSONObject(pageTo, json);
            } catch (JSONException e) {
                // not likely to happen
                new_item = null;
            }

            if(new_item instanceof Folder) {
                Folder f = (Folder)new_item;
                f.setFolderPageId(translated_page_ids.get(f.getFolderPageId()));
            }

            pageTo.items.add(new_item);
        }

        // copy page box resources
        FileUtils.copyIcons(buffer, old_icon_dir, "", new_icon_dir, "");

        // copy page icon and page wallpaper
        Utils.copyFileSafe(buffer, Page.getWallpaperFile(baseDirFrom, old_page_id), Page.getWallpaperFile(baseDirTo, new_page_id));
        Utils.copyFileSafe(buffer, Page.getPageIconFile(baseDirFrom, old_page_id), Page.getPageIconFile(baseDirTo, new_page_id));

        // now translate ids
        postProcessPage(pageTo);

        // save the page
        pageTo.setModified();
        pageTo.save();
    }

    private void postProcessPage(Page page) {
        float[] matrix_values = new float[9];

        PageConfig c = page.config;

        if(do_scale) {
            c.gridLColumnSize *= sx;
            c.gridPColumnSize *= sx;
            c.gridLRowSize *= sy;
            c.gridPRowSize *= sy;
        }

        if(enforce_holo && c.defaultItemConfig.selectionEffect == ItemConfig.SelectionEffect.MATERIAL) {
            c.defaultItemConfig.selectionEffect = ItemConfig.SelectionEffect.HOLO;
        }

        postProcessEventAction(c.homeKey);
        postProcessEventAction(c.menuKey);
        postProcessEventAction(c.longMenuKey);
        postProcessEventAction(c.backKey);
        postProcessEventAction(c.longBackKey);
        postProcessEventAction(c.searchKey);
        postProcessEventAction(c.bgTap);
        postProcessEventAction(c.bgDoubleTap);
        postProcessEventAction(c.bgLongTap);
        postProcessEventAction(c.swipeLeft);
        postProcessEventAction(c.swipeRight);
        postProcessEventAction(c.swipeUp);
        postProcessEventAction(c.swipeDown);
        postProcessEventAction(c.swipe2Left);
        postProcessEventAction(c.swipe2Right);
        postProcessEventAction(c.swipe2Up);
        postProcessEventAction(c.swipe2Down);
        postProcessEventAction(c.screenOff);
        postProcessEventAction(c.screenOn);
        postProcessEventAction(c.orientationPortrait);
        postProcessEventAction(c.orientationLandscape);
        postProcessEventAction(c.posChanged);
        postProcessEventAction(c.load);
        postProcessEventAction(c.paused);
        postProcessEventAction(c.resumed);
        postProcessEventAction(c.itemAdded);
        postProcessEventAction(c.itemRemoved);
        postProcessEventAction(c.menu);

        ItemConfig pic = c.defaultItemConfig;
        postProcessEventAction(pic.tap);
        postProcessEventAction(pic.longTap);
        postProcessEventAction(pic.swipeLeft);
        postProcessEventAction(pic.swipeUp);
        postProcessEventAction(pic.swipeRight);
        postProcessEventAction(pic.swipeDown);
        postProcessEventAction(pic.touch);
        postProcessEventAction(pic.paused);
        postProcessEventAction(pic.resumed);
        postProcessEventAction(pic.menu);

        FolderConfig pfc = c.defaultFolderConfig;
        if(do_scale && (pfc.wAH == Box.AlignH.CUSTOM || pfc.wAV == Box.AlignV.CUSTOM || pfc.wW!=0 || pfc.wH!=0)) {
            pfc.wX *= sx;
            pfc.wY *= sy;
            pfc.wW *= sx;
            pfc.wH *= sy;
        }

        ArrayList<Item> items = page.items;
        for(Item i : items) {
            ItemConfig ic = i.getItemConfig();
            if(ic != pic) {
                postProcessEventAction(ic.tap);
                postProcessEventAction(ic.longTap);
                postProcessEventAction(ic.swipeLeft);
                postProcessEventAction(ic.swipeUp);
                postProcessEventAction(ic.swipeRight);
                postProcessEventAction(ic.swipeDown);
                postProcessEventAction(ic.touch);
                postProcessEventAction(ic.paused);
                postProcessEventAction(ic.resumed);
                postProcessEventAction(ic.menu);
            }

            if(!ic.onGrid && do_scale) {
                i.setViewWidth((int)(i.getViewWidth() * sx));
                i.setViewHeight((int) (i.getViewHeight() * sy));
                Matrix m = i.getTransform();
                m.getValues(matrix_values);
                float tx = matrix_values[Matrix.MTRANS_X];
                float ty = matrix_values[Matrix.MTRANS_Y];
                m.postTranslate(-tx, -ty);
                m.postTranslate(tx * sx, ty * sy);
            }

            Class<?> cls = i.getClass();
            if(cls == StopPoint.class) {
                postProcessEventAction(((StopPoint)i).getReachedAction());
            } else if(cls == CustomView.class) {
                CustomView customView = (CustomView) i;
                String newIdData = translateScriptAction(customView.onCreate);
                if(newIdData != null) customView.onCreate = newIdData;
                newIdData = translateScriptAction(customView.onDestroy);
                if(newIdData != null) customView.onDestroy = newIdData;
            }
            if(cls == Shortcut.class && page.id != Page.APP_DRAWER_PAGE) {
                Shortcut s = (Shortcut)i;
                Intent intent = s.getIntent();
                postProcessIntent(intent);
            } else if(cls == Folder.class && do_scale) {
                Folder f = (Folder)i;
                FolderConfig fc = f.getFolderConfig();
                if(fc != pfc) {
                    if(fc.wAH == Box.AlignH.CUSTOM || fc.wAV == Box.AlignV.CUSTOM || fc.wW!=0 || fc.wH!=0) {
                        fc.wX *= sx;
                        fc.wY *= sy;
                        fc.wW *= sx;
                        fc.wH *= sy;
                    }
                }
            }
        }
    }

    private String translateScriptAction(String data) {
        if(data == null || translated_script_ids == null) {
            return null;
        }

        try {
            Pair<Integer, String> id_data = Script.decodeIdAndData(data);
            int oldScriptId = id_data.first;
            int newScriptId = translated_script_ids.get(oldScriptId);
            return Script.encodeIdAndData(newScriptId, id_data.second);
        } catch(NumberFormatException e) {
            // pass
            return null;
        }
    }

    private String postProcessData(int action, String data) {
        if(action == GlobalConfig.OPEN_FOLDER && translated_page_ids != null) {
            try {
                int old_folder_page_id = Integer.parseInt(data);
                int new_folder_page_id = translated_page_ids.get(old_folder_page_id);
                return String.valueOf(new_folder_page_id);
            } catch(NumberFormatException e) {
                // pass
            }
        } else if(action == GlobalConfig.RUN_SCRIPT && translated_script_ids != null) {
            return translateScriptAction(data);
        }

        return null;
    }

    private boolean postProcessIntent(Intent intent) {
        boolean modified = false;

        ComponentName cn = intent.getComponent();
        if(cn != null) {
            if(cn.equals(drawer_from)) {
                intent.setComponent(drawer_to);
                modified = true;
            } else if(cn.getPackageName().equals(pkg_name_from)) {
                intent.setComponent(new ComponentName(pkg_name_to, cn.getClassName()));
                modified = true;
            }

            cn = intent.getComponent();
            if(cn.getPackageName().equals(pkg_name_to)) {
                if(intent.hasExtra(LightningIntent.INTENT_EXTRA_DESKTOP) && translated_page_ids != null) {
                    int old_bookmark_page = intent.getIntExtra(LightningIntent.INTENT_EXTRA_DESKTOP, Page.NONE);
                    intent.putExtra(LightningIntent.INTENT_EXTRA_DESKTOP, translated_page_ids.get(old_bookmark_page));
                    modified = true;
                } else {
                    EventAction ea = Utils.decodeEventActionFromLightningIntent(intent);
                    if(ea != null) {
                        if(postProcessEventAction(ea)) {
                            intent.removeExtra(LightningIntent.INTENT_EXTRA_ACTION);
                            intent.removeExtra(LightningIntent.INTENT_EXTRA_DATA);
                            intent.putExtra(LightningIntent.INTENT_EXTRA_EVENT_ACTION, JsonLoader.toJSONObject(ea, null).toString());
                            modified = true;
                        }
                    }
                }

                if(do_scale) {
                    if(intent.hasExtra(LightningIntent.INTENT_EXTRA_X)) {
                        boolean absolute = intent.getBooleanExtra(LightningIntent.INTENT_EXTRA_ABSOLUTE, true);
                        if(absolute) {
                            intent.putExtra(LightningIntent.INTENT_EXTRA_X, intent.getFloatExtra(LightningIntent.INTENT_EXTRA_X, 0) * sx);
                            intent.putExtra(LightningIntent.INTENT_EXTRA_Y, intent.getFloatExtra(LightningIntent.INTENT_EXTRA_Y, 0) * sy);
                            modified = true;
                        }
                    }
                }
            }
        }

        return modified;
    }

    private boolean postProcessEventAction(EventAction ea) {
        boolean res = false;
        if(ea.action == GlobalConfig.LAUNCH_APP || ea.action == GlobalConfig.LAUNCH_SHORTCUT || ea.action == GlobalConfig.GO_DESKTOP_POSITION) {
            try {
                Intent intent = Intent.parseUri(ea.data, 0);
                boolean modified = postProcessIntent(intent);
                if(modified) {
                    ea.data = intent.toUri(0);
                }
                res = modified;
            } catch (Exception e) {
                // pass
            }
        } else {
            String new_data = postProcessData(ea.action, ea.data);
            if(new_data != null) {
                ea.data = new_data;
                res = true;
            }
        }

        if(ea.next != null) {
            return postProcessEventAction(ea.next) || res;
        } else {
            return res;
        }
    }
}
