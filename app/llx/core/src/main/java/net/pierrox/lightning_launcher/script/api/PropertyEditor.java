package net.pierrox.lightning_launcher.script.api;

import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import net.pierrox.lightning_launcher.LLApp;
import net.pierrox.lightning_launcher.configuration.ItemConfig;
import net.pierrox.lightning_launcher.configuration.PageConfig;
import net.pierrox.lightning_launcher.configuration.ShortcutConfig;
import net.pierrox.lightning_launcher.configuration.ShortcutConfigStylable;
import net.pierrox.lightning_launcher.data.EventAction;
import net.pierrox.lightning_launcher.data.Page;
import net.pierrox.lightning_launcher.engine.Screen;
import net.pierrox.lightning_launcher.engine.variable.Value;
import net.pierrox.lightning_launcher.views.IconLabelView;
import net.pierrox.lightning_launcher.views.IconView;
import net.pierrox.lightning_launcher.views.ItemLayout;
import net.pierrox.lightning_launcher.views.SharedAsyncGraphicsDrawable;
import net.pierrox.lightning_launcher.views.item.ItemView;
import net.pierrox.lightning_launcher.views.item.ShortcutView;
import net.pierrox.android.lsvg.SvgElement;
import net.pierrox.android.lsvg.SvgGroup;
import net.pierrox.android.lsvg.SvgPath;
import net.pierrox.android.lsvg.SvgSvg;

import org.mozilla.javascript.ScriptRuntime;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Utility to modify one or more configuration settings at once. Modify one or more properties using the same PropertyEditor, then call {@link #commit()} to validate changes.
 * An instance of this object can be retrieved with {@link PropertySet#edit()}. You can chain calls this way:
 * <pre><code>
 * 	property_set.edit()
 * 		.setInteger("someProperty", some_value)
 * 		.setFloat("someOtherProperty", some_other_value)
 * 		.commit();
 * </code></pre>
 */
public class PropertyEditor {

    private static final int[] border_codes = new int[]{
            net.pierrox.lightning_launcher.data.Box.BCL,
            net.pierrox.lightning_launcher.data.Box.BCT,
            net.pierrox.lightning_launcher.data.Box.BCR,
            net.pierrox.lightning_launcher.data.Box.BCB
    };
    private static final char[] border_letters = new char[] {'l', 't', 'r', 'b'};

	private PropertySet mPropertySet;
	private HashMap<String, Object> mBundle;

	/*package*/ PropertyEditor(PropertySet ps) {
		mPropertySet = ps;
		mBundle = new HashMap<String, Object>();
	}
	
	public PropertyEditor setBoolean(String name, boolean value) {
		mBundle.put(name, Boolean.valueOf(value));
		return this;
	}
	
	public PropertyEditor setInteger(String name, long value) {
		mBundle.put(name, Integer.valueOf((int) value));
		return this;
	}
	
	public PropertyEditor setFloat(String name, float value) {
        if(Float.isNaN(value)) {
            throw ScriptRuntime.constructError("setFloat", "Bad argument");
        }

		mBundle.put(name, Float.valueOf(value));
		return this;
	}
	
	public PropertyEditor setString(String name, String value) {
		mBundle.put(name, value);
		return this;
	}

    /**
     * @param data optional data, can be null. See {@link net.pierrox.lightning_launcher.script.api.EventHandler#getData()}
     * @return
     */
    public PropertyEditor setEventHandler(String name, int action, String data) {
        mBundle.put(name, new EventAction(action, data));
        return this;
    }

    public PropertyEditor setEventHandler(String name, EventHandler eventHandler) {
        mBundle.put(name, eventHandler.getEventAction());
        return this;
    }

	/**
	 * Returns a box object allowing modifications.
     * @param name box property to retrieve, for instance i.box or f.box
	 */
	public Box getBox(String name) {
		net.pierrox.lightning_launcher.data.Box box = (net.pierrox.lightning_launcher.data.Box) mPropertySet.getProperty(name, net.pierrox.lightning_launcher.data.Box.class);
		return new Box(mPropertySet.mLightning, box, name, this);
	}

	/**
	 * Validate all changes at once.
     * Warning: when commiting property changes on a container, items may be reloaded to propagate changes and as a consequence references to items in this container may become invalid and may need to be updated.
	 */
	public void commit() {
        if(mBundle.size() == 0) {
            return;
        }

        Page page = null;
        PropertySet.Type type = mPropertySet.getType();
        boolean isForItem = type == PropertySet.Type.ITEM;
        Item scriptItem = null;
        net.pierrox.lightning_launcher.data.Item item = null;
        PageConfig page_config = null;
        switch (type) {
            case ITEM:
                scriptItem = mPropertySet.getScriptItem();
                item = scriptItem.getItem();
                page = item.getPage();
                page_config = page.config;
                break;
            case CONTAINER:
                page = mPropertySet.getScriptContainer().getPage();
                if (page.isModified()) {
                    // save the page before to modify it, it may be reloaded and modifications to item would be lost otherwise
                    page.save();
                }
                page_config = page.config;
                break;
            case GLOBAL_CONFIG:
                // pass
                break;
        }

        // fast path properties / attributes
        ArrayList<String> keys = new ArrayList<>(mBundle.keySet());
        if(type == PropertySet.Type.CONTAINER) {
            for(int i = keys.size()-1; i>=0; i--) {
                String key = keys.get(i);
                boolean handled = true;
                Object value = mBundle.get(key);
                if (key.equals(Property.PROP_PAGE_BG_COLOR)) {
                    page_config.bgColor = (int) value;
                    for (Screen screen : LLApp.get().getScreens()) {
                        screen.onPageBackgroundColorChanged(page);
                    }
                } else if (key.equals(Property.PROP_PAGE_STATUS_BAR_COLOR)) {
                    page_config.statusBarColor = (int) value;
                    for (Screen screen : LLApp.get().getScreens()) {
                        screen.onPageSystemBarsColorChanged(page);
                    }
                } else if (key.equals(Property.PROP_PAGE_STATUS_BAR_LIGHT)) {
                    page_config.statusBarLight = (boolean) value;
                    for (Screen screen : LLApp.get().getScreens()) {
                        screen.onPageSystemBarsColorChanged(page);
                    }
                } else if (key.equals(Property.PROP_PAGE_NAV_BAR_COLOR)) {
                    page_config.navigationBarColor = (int) value;
                    for (Screen screen : LLApp.get().getScreens()) {
                        screen.onPageSystemBarsColorChanged(page);
                    }
                } else if (key.equals(Property.PROP_PAGE_NAV_BAR_LIGHT)) {
                    page_config.navigationBarLight = (boolean) value;
                    for (Screen screen : LLApp.get().getScreens()) {
                        screen.onPageSystemBarsColorChanged(page);
                    }
                } else if (key.equals(Property.PROP_PAGE_SCROLLING_DIRECTION)) {
                    try {
                        page_config.scrollingDirection = PageConfig.ScrollingDirection.valueOf((String) value);
                    } catch(IllegalArgumentException e) {
                        // pass
                    }
                    for (Screen screen : LLApp.get().getScreens()) {
                        for (ItemLayout il : screen.getItemLayoutsForPage(page.id)) {
                            il.configureScroll();
                        }
                    }
                } else if (key.equals(Property.PROP_ITEM_BOX)) {
                    net.pierrox.lightning_launcher.data.Box box = page_config.defaultItemConfig.box;
                    decodeEncodedBox(box, (String) value);
                    page_config.defaultItemConfig.box_s = box.toString(null);
                    for (net.pierrox.lightning_launcher.data.Item it : page.items) {
                        net.pierrox.lightning_launcher.data.Box item_box = it.getItemConfig().box;
                        if(item_box != box) {
                            item_box.loadFromString(it.getItemConfig().box_s, box);
                        }
                        updateItemBox(it);
                    }
                } else if(key.equals(Property.PROP_TEXT_COLOR_NORMAl)) {
                    page_config.defaultShortcutConfig.labelFontColor = (int) value;
                    updatePageItemViews(page, mUpdateLabelColor);
                } else if(key.equals(Property.PROP_TEXT_COLOR_SELECTED)) {
                    page_config.defaultShortcutConfig.selectionColorLabel = (int) value;
                    updatePageItemViews(page, mUpdateLabelColor);
                } else if(key.equals(Property.PROP_TEXT_COLOR_FOCUSED)) {
                    page_config.defaultShortcutConfig.focusColorLabel = (int) value;
                    updatePageItemViews(page, mUpdateLabelColor);
                } else if(key.equals(Property.PROP_TEXT_LABEL_VISIBILITY)) {
                    page_config.defaultShortcutConfig.labelVisibility = (boolean) value;
                    updatePageItemViews(page, mUpdateLabelVisibility);
                } else if(key.equals(Property.PROP_TEXT_FONT_SIZE)) {
                    page_config.defaultShortcutConfig.labelFontSize = (float) value;
                    updatePageItemViews(page, mUpdateLabelFontSize);
                } else if(key.equals(Property.PROP_TEXT_FONT_STYLE)) {
                    try {
                        ShortcutConfig.FontStyle style = ShortcutConfig.FontStyle.valueOf((String)value);
                        page_config.defaultShortcutConfig.labelFontStyle = style;
                        updatePageItemViews(page, mUpdateLabelFontStyle);
                    } catch(IllegalArgumentException e) {
                        // pass
                    }
                } else if(key.equals(Property.PROP_ICON_VISIBILITY)) {
                    page_config.defaultShortcutConfig.iconVisibility = (boolean) value;
                    updatePageItemViews(page, mUpdateIconVisibility);
                } else if(key.equals(Property.PROP_ICON_SCALE)) {
                    page_config.defaultShortcutConfig.iconScale = (float) value;
                    updatePageItemViews(page, mUpdateIconGraphics);
                } else if(key.equals(Property.PROP_ICON_REFLECTION)) {
                    page_config.defaultShortcutConfig.iconReflection = (boolean) value;
                    updatePageItemViews(page, mUpdateIconGraphics);
                } else if(key.equals(Property.PROP_ICON_COLOR_FILTER)) {
                    page_config.defaultShortcutConfig.iconColorFilter = (int) value;
                    updatePageItemViews(page, mUpdateIconGraphics);
                } else {
                    handled = false;
                }
                if(handled) {
                    mBundle.remove(key);
                    keys.remove(i);
                }
            }
        }
        if(isForItem && item instanceof net.pierrox.lightning_launcher.data.Folder) {
            net.pierrox.lightning_launcher.data.Folder f = (net.pierrox.lightning_launcher.data.Folder) item;
            for(int i = keys.size()-1; i>=0; i--) {
                String key = keys.get(i);
                boolean handled = true;
                Object value = mBundle.get(key);
                if(key.equals(Property.PROP_FOLDER_TITLE_VISIBILITY)) {
                    f.modifyFolderConfig().titleVisibility = (boolean) value;
                    f.getPage().notifyFolderWindowChanged(f);
                } else if (key.equals(Property.PROP_FOLDER_TITLE_FONT_COLOR)) {
                    f.modifyFolderConfig().titleFontColor = (int) value;
                    f.getPage().notifyFolderWindowChanged(f);
                } else if(key.equals(Property.PROP_FOLDER_TITLE_FONT_SIZE)) {
                    f.modifyFolderConfig().titleFontSize = (float) value;
                    f.getPage().notifyFolderWindowChanged(f);
                } else if(key.equals(Property.PROP_FOLDER_W_AH)) {
                    try {
                        f.modifyFolderConfig().wAH = net.pierrox.lightning_launcher.data.Box.AlignH.valueOf((String) value);
                        f.getPage().notifyFolderWindowChanged(f);
                    } catch(IllegalArgumentException e) {
                        // pass
                    }
                } else if(key.equals(Property.PROP_FOLDER_W_AV)) {
                    try {
                        f.modifyFolderConfig().wAV = net.pierrox.lightning_launcher.data.Box.AlignV.valueOf((String) value);
                        f.getPage().notifyFolderWindowChanged(f);
                    } catch(IllegalArgumentException e) {
                        // pass
                    }
                } else if(key.equals(Property.PROP_FOLDER_W_X)) {
                    f.modifyFolderConfig().wX = (int) value;
                    f.getPage().notifyFolderWindowChanged(f);
                } else if(key.equals(Property.PROP_FOLDER_W_Y)) {
                    f.modifyFolderConfig().wY = (int) value;
                    f.getPage().notifyFolderWindowChanged(f);
                } else if(key.equals(Property.PROP_FOLDER_W_W)) {
                    f.modifyFolderConfig().wW = (int) value;
                    f.getPage().notifyFolderWindowChanged(f);
                } else if(key.equals(Property.PROP_FOLDER_W_H)) {
                    f.modifyFolderConfig().wH = (int) value;
                    f.getPage().notifyFolderWindowChanged(f);
                } else if(key.equals(Property.PROP_FOLDER_BOX)) {
                    net.pierrox.lightning_launcher.data.Box box = f.modifyFolderConfig().box;
                    decodeEncodedBox(box, (String) value);
                    f.getFolderConfig().box_s = box.toString(null);
                    f.getPage().notifyFolderWindowChanged(f);
                } else {
                    handled = false;
                }
                if(handled) {
                    mBundle.remove(key);
                    keys.remove(i);
                }
            }
        }
        if(isForItem && item instanceof ShortcutConfigStylable) {
            ShortcutConfigStylable stylable = (ShortcutConfigStylable) item;
            for(int i = keys.size()-1; i>=0; i--) {
                String key = keys.get(i);
                boolean handled = true;
                Object value = mBundle.get(key);
                if(key.equals(Property.PROP_TEXT_LABEL)) {
                    if(scriptItem instanceof Shortcut) {
                        Shortcut script_shortcut=(Shortcut) scriptItem;
                        script_shortcut.setLabel((String) value, true);
                    }
                } else if(key.equals(Property.PROP_TEXT_COLOR_NORMAl)) {
                    stylable.modifyShortcutConfig().labelFontColor = (int) value;
                    updateItemView(item, mUpdateLabelColor);
                } else if(key.equals(Property.PROP_TEXT_COLOR_SELECTED)) {
                    stylable.modifyShortcutConfig().selectionColorLabel = (int) value;
                    updateItemView(item, mUpdateLabelColor);
                } else if(key.equals(Property.PROP_TEXT_COLOR_FOCUSED)) {
                    stylable.modifyShortcutConfig().focusColorLabel = (int) value;
                    updateItemView(item, mUpdateLabelColor);
                } else if(key.equals(Property.PROP_TEXT_LABEL_VISIBILITY)) {
                    stylable.modifyShortcutConfig().labelVisibility = (boolean) value;
                    updateItemView(item, mUpdateLabelVisibility);
                } else if(key.equals(Property.PROP_TEXT_FONT_SIZE)) {
                    stylable.modifyShortcutConfig().labelFontSize = (float) value;
                    updateItemView(item, mUpdateLabelFontSize);
                } else if(key.equals(Property.PROP_TEXT_FONT_STYLE)) {
                    try {
                        stylable.modifyShortcutConfig().labelFontStyle = ShortcutConfig.FontStyle.valueOf((String)value);
                        updateItemView(item, mUpdateLabelFontStyle);
                    } catch(IllegalArgumentException e) {
                        // pass
                    }
                } else if(key.equals(Property.PROP_ICON_VISIBILITY)) {
                    stylable.modifyShortcutConfig().iconVisibility = (boolean) value;
                    updateItemView(item, mUpdateIconVisibility);
                } else if(key.equals(Property.PROP_ICON_SCALE)) {
                    stylable.modifyShortcutConfig().iconScale = (float) value;
                    updateItemView(item, mUpdateIconGraphics);
                } else if(key.equals(Property.PROP_ICON_REFLECTION)) {
                    stylable.modifyShortcutConfig().iconReflection = (boolean) value;
                    updateItemView(item, mUpdateIconGraphics);
                } else if(key.equals(Property.PROP_ICON_COLOR_FILTER)) {
                    stylable.modifyShortcutConfig().iconColorFilter = (int) value;
                    updateItemView(item, mUpdateIconGraphics);
                } else {
                    handled = false;
                }
                if(handled) {
                    mBundle.remove(key);
                    keys.remove(i);
                }
            }
        }
        if(isForItem) {
            Item script_item = scriptItem;
            for(int i = keys.size()-1; i>=0; i--) {
                String key = keys.get(i);
                boolean handled = true;
                Object value = mBundle.get(key);
                if(key.equals(Property.PROP_VOID)) {
                } else if(key.equals(Property.PROP_ITEM_VISIBILITY)) {
                    script_item.setVisibility((Boolean)value);
                } else if(key.equals(Property.PROP_ITEM_Z_INDEX)) {
                    Integer new_index = (Integer) value;
                    int size = page.items.size();
                    if(new_index >=0 && new_index < size) {
                        page.setItemZIndex(item, new_index);
                    } else {
                        Toast.makeText(LLApp.get().getActiveScreen().getContext(), "z-index out of bounds for "+item+" ("+new_index+" not between 0 and "+size+")", Toast.LENGTH_SHORT).show();
                    }
                } else if(key.equals(Property.PROP_ITEM_ALPHA)) {
                    int a = (Integer) value;
                    if(a<0) a=0; else if(a>255) a=255;
                    item.setAlpha(a);
                    item.getItemConfig().alpha = a;
                } else if(key.equals(Property.PROP_ITEM_BOX)) {
                    ItemConfig ic = item.modifyItemConfig();
                    net.pierrox.lightning_launcher.data.Box box = ic.box;
                    decodeEncodedBox(box, (String)value);
                    ic.box_s = box.toString(page_config.defaultItemConfig.box);
                    updateItemBox(item);
                } else if (key.equals(Property.PROP_CELL_LEFT)) {
                    RectL r = script_item.getCell();
                    if(r != null) {
                        int d = (Integer) value - r.getLeft();
                        script_item.setCell(r.getLeft() + d, r.getTop(), r.getRight() + d, r.getBottom());
                    }
                } else if (key.equals(Property.PROP_CELL_TOP)) {
                    RectL r = script_item.getCell();
                    if(r != null) {
                        int d = (Integer) value - r.getTop();
                        script_item.setCell(r.getLeft(), r.getTop() + d, r.getRight(), r.getBottom() + d);
                    }
                } else if (key.equals(Property.PROP_CELL_WIDTH)) {
                    RectL r = script_item.getCell();
                    if(r != null) {
                        script_item.setCell(r.getLeft(), r.getTop(), r.getLeft() + (Integer) value, r.getBottom());
                    }
                } else if (key.equals(Property.PROP_CELL_HEIGHT)) {
                    RectL r = script_item.getCell();
                    if(r != null) {
                        script_item.setCell(r.getLeft(), r.getTop(), r.getRight(), r.getTop() + (Integer) value);
                    }
                } else if (key.equals(Property.PROP_TRANSFORM_X)) {
                    script_item.setPosition((Float) value, script_item.getPositionY());
                } else if (key.equals(Property.PROP_TRANSFORM_Y)) {
                    script_item.setPosition(script_item.getPositionX(), (Float) value);
                } else if (key.equals(Property.PROP_TRANSFORM_W)) {
                    script_item.setSize((Float) value, script_item.getHeight());
                } else if (key.equals(Property.PROP_TRANSFORM_H)) {
                    script_item.setSize(script_item.getWidth(), (Float) value);
                } else if (key.equals(Property.PROP_TRANSFORM_A)) {
                    script_item.setRotation((Float) value);
                } else if (key.equals(Property.PROP_TRANSFORM_SX)) {
                    script_item.setScale((Float) value, script_item.getScaleY());
                } else if (key.equals(Property.PROP_TRANSFORM_SY)) {
                    script_item.setScale(script_item.getScaleX(), (Float) value);
                } else if (key.equals(Property.PROP_TRANSFORM_KX)) {
                    script_item.setSkew((Float) value, script_item.getSkewY());
                } else if (key.equals(Property.PROP_TRANSFORM_KY)) {
                    script_item.setSkew(script_item.getSkewX(), (Float) value);
                } else if (key.equals(Property.PROP_ITEM_ON_GRID)) {
                    script_item.mItemView.setOnGrid((Boolean)value);
                } else {
                    handled = false;
                }

                if(handled) {
                    mBundle.remove(key);
                    keys.remove(i);
                }
            }
        }

        // process dynamic svg properties
        for(int i = keys.size()-1; i>=0; i--) {
            String key = keys.get(i);
            if(key.startsWith("svg/")) {
                String value = mBundle.get(key).toString();
                String[] tokens = key.split("/");
                try {
                    String which = tokens[1];
                    String id = tokens[2];
                    String attribute = tokens[3];

                    ItemView itemView = mPropertySet.getScriptItem().mItemView;
                    if (!itemView.isInitDone()) {
                        itemView.init();
                    }

                    SvgSvg svg = null;
                    if (which.equals("icon")) {
                        if (itemView instanceof ShortcutView) {
                            IconLabelView il = ((ShortcutView) itemView).getIconLabelView();
                            if(il != null) {
                                IconView iv = il.getIconView();
                                if(iv != null) {
                                    SharedAsyncGraphicsDrawable sd = iv.getSharedAsyncGraphicsDrawable();
                                    if(sd != null) {
                                        if(sd.getType() == SharedAsyncGraphicsDrawable.TYPE_NOT_YET_KNOWN) {
                                            sd.loadGraphicsSync();
                                        }
                                        if(sd.getType() == SharedAsyncGraphicsDrawable.TYPE_SVG) {
                                            svg = sd.getSvgDrawable().getSvgRoot();
                                        }
                                    }
                                }
                            }
                        }
                    } else if(which.equals("bgn")) {
                        Drawable d = item.getItemConfig().box.bgNormal;
                        if(d instanceof SharedAsyncGraphicsDrawable && ((SharedAsyncGraphicsDrawable)d).getType() == SharedAsyncGraphicsDrawable.TYPE_SVG) {
                            svg = ((SharedAsyncGraphicsDrawable)d).getSvgDrawable().getSvgRoot();
                        }
                    } else if(which.equals("bgs")) {
                        Drawable d = item.getItemConfig().box.bgSelected;
                        if(d instanceof SharedAsyncGraphicsDrawable && ((SharedAsyncGraphicsDrawable)d).getType() == SharedAsyncGraphicsDrawable.TYPE_SVG) {
                            svg = ((SharedAsyncGraphicsDrawable)d).getSvgDrawable().getSvgRoot();
                        }
                    } else if(which.equals("bgf")) {
                        Drawable d = item.getItemConfig().box.bgFocused;
                        if(d instanceof SharedAsyncGraphicsDrawable && ((SharedAsyncGraphicsDrawable)d).getType() == SharedAsyncGraphicsDrawable.TYPE_SVG) {
                            svg = ((SharedAsyncGraphicsDrawable)d).getSvgDrawable().getSvgRoot();
                        }
                    }

                    if(svg == null) {
                        throw new Exception("not a SVG image");
                    }

                    SvgElement element = svg.getElementById(id);
                    if(element == null) {
                        throw new Exception("not element with id #"+id);
                    }

                    boolean ok = false;
                    if (attribute.equals("transform")) {
                        if (element instanceof SvgGroup) {
                            ok = ((SvgGroup) element).setTransform(value);
                        } else if (element instanceof SvgPath) {
                            ok = ((SvgPath) element).setTransform(value);
                        }
                    } else if(attribute.equals("style")) {
                        if (element instanceof SvgPath) {
                            ok = ((SvgPath) element).setStyle(value);
                        }
                    } else if(attribute.equals("path")) {
                        if (element instanceof SvgPath) {
                            ok = ((SvgPath) element).setPath(value);
                        }
                    }

                    if (!ok) {
                        throw new Exception("");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    mPropertySet.mLightning.scriptError("bad binding key or value for '"+key+"' ("+e.getMessage()+")");
                }
                mBundle.remove(key);
                keys.remove(i);
            }
        }

        if(mBundle.size() == 0) {
            // all keys have been removed: fast path
            page.setModified();
            return;
        }


        // check all properties before to modify something
        for(String key : keys) {
			Pair<Object,String> c_p = mPropertySet.getConfigObject(key, null);
			if(c_p == null) {
				mPropertySet.mLightning.scriptError("no property named '"+key+"'");
				return;
			}
		}

        boolean modified = false;
//        boolean apply_icon_pack = false;
		for(String key : keys) {
			Pair<Object,String> c_p = mPropertySet.getConfigObject(key, page_config);
			Object config = c_p.first;
			String property = c_p.second;
			try {
                Class<?> config_class = config.getClass();
                Field f = config_class.getField(property);
				Class<?> cls = f.getType();
				if(cls==boolean.class) {
                    f.setBoolean(config, (Boolean)mBundle.get(key));
				} else if(cls==int.class) {
					f.setInt(config, (Integer)mBundle.get(key));
				} else if(cls==float.class) {
					f.setFloat(config, (Float)mBundle.get(key));
				} else if(cls==String.class) {
                    String value = (String) mBundle.get(key);
//                    if (property.equals(Property.PROP_PAGE_ICON_PACK) && value != null) {
//                        apply_icon_pack = true;
//                    }
                    f.set(config, value);
                } else if(cls==EventAction.class) {
                    f.set(config, (EventAction)mBundle.get(key));
				} else if(cls.isEnum()) {
					f.set(config, Enum.valueOf((Class<Enum>)cls, (String)mBundle.get(key)));
				}
                modified = true;
			} catch (Exception e) {
                Log.i("LLX", "property error: "+property);
			}
		}

        if(modified) {
            switch (type) {
                case ITEM:
                    item.notifyChanged();
                    break;
                case CONTAINER:
                    page.setModified();
                    page.saveConfig();
                    page.reload();
                    break;
                case GLOBAL_CONFIG:
                    // pass
                    break;
            }
        }
	}

    private interface ItemViewUpdate {
        enum Type {
            ITEM,
            SHORTCUT,
            FOLDER
        }
        void update(ItemView itemView);
        Type getType();
    }

    /**
     * Update all views for an item on all screens
     * @param item
     * @param update
     */
    private void updateItemView(net.pierrox.lightning_launcher.data.Item item, ItemViewUpdate update) {
        for (Screen screen : LLApp.get().getScreens()) {
            ItemView[] itemViews = screen.getItemViewsForItem(item);
            for (ItemView itemView : itemViews) {
                if(itemView.isInitDone()) {
                    update.update(itemView);
                }
            }
        }
    }

    /**
     * Update all views for items on a page on all screens, provided that some default config is shared
     * @param page
     * @param update
     */
    private void updatePageItemViews(Page page, ItemViewUpdate update) {
        for(net.pierrox.lightning_launcher.data.Item item : page.items) {
            switch (update.getType()) {
                case ITEM:
                    if (item.hasSharedItemConfig()) updateItemView(item, update);
                    break;
                case SHORTCUT:
                    if (item instanceof net.pierrox.lightning_launcher.data.Shortcut && ((net.pierrox.lightning_launcher.data.Shortcut)item).hasSharedShortcutConfig()) updateItemView(item, update);
                    break;
                case FOLDER:
                    if (item instanceof net.pierrox.lightning_launcher.data.Folder && ((net.pierrox.lightning_launcher.data.Folder)item).hasSharedFolderConfig()) updateItemView(item, update);
                    break;
            }
        }
    }

    private ItemViewUpdate mUpdateLabelVisibility = new ItemViewUpdate() {
        @Override
        public void update(ItemView itemView) {
            ((ShortcutView)itemView).updateLabelVisibility();
        }

        @Override
        public Type getType() {
            return Type.SHORTCUT;
        }
    };

    private ItemViewUpdate mUpdateLabelColor = new ItemViewUpdate() {
        @Override
        public void update(ItemView itemView) {
            ((ShortcutView)itemView).updateLabelColor();
        }

        @Override
        public Type getType() {
            return Type.SHORTCUT;
        }
    };

    private ItemViewUpdate mUpdateLabelFontSize = new ItemViewUpdate() {
        @Override
        public void update(ItemView itemView) {
            ((ShortcutView)itemView).updateLabelFontSize();
        }

        @Override
        public Type getType() {
            return Type.SHORTCUT;
        }
    };

    private ItemViewUpdate mUpdateLabelFontStyle = new ItemViewUpdate() {
        @Override
        public void update(ItemView itemView) {
            ((ShortcutView)itemView).updateLabelFontStyle();
        }

        @Override
        public Type getType() {
            return Type.SHORTCUT;
        }
    };

    private void updateItemBox(net.pierrox.lightning_launcher.data.Item item) {
        for (Screen screen : LLApp.get().getScreens()) {
            ItemView[] itemViews = screen.getItemViewsForItem(item);
            for (ItemView itemView : itemViews) {
                if(itemView.isInitDone()) {
                    itemView.getSensibleView().configureBox();
                }
            }
        }
    }

    private ItemViewUpdate mUpdateIconVisibility = new ItemViewUpdate() {
        @Override
        public void update(ItemView itemView) {
            ((ShortcutView)itemView).updateIconVisibility();
        }

        @Override
        public Type getType() {
            return Type.SHORTCUT;
        }
    };

    private ItemViewUpdate mUpdateIconGraphics = new ItemViewUpdate() {
        @Override
        public void update(ItemView itemView) {
            ((ShortcutView)itemView).updateIconGraphics();
        }

        @Override
        public Type getType() {
            return Type.SHORTCUT;
        }
    };

    private static void decodeEncodedBox(net.pierrox.lightning_launcher.data.Box box, String encoded_box) {
        for(String token : encoded_box.split(String.valueOf(Box.LIST_SEP))) {
            int p = token.indexOf(Box.TOK_SEP);
            if(p != -1) {
                String name = token.substring(0, p);
                String data = token.substring(p + 1);
                int int_value = Value.parseIntOrColor(data);
                char c0 = name.charAt(0);
                int l = name.length();
                if(c0 == 'c') {
                    if(l==1 || name.indexOf('n')!=-1) box.ccn = int_value;
                    if(l==1 || name.indexOf('s')!=-1) box.ccs = int_value;
                    if(l==1 || name.indexOf('f')!=-1) box.ccf = int_value;
                } else if(c0 == 'm') {
                    if(l==1 || name.indexOf('l') != -1) box.size[net.pierrox.lightning_launcher.data.Box.ML] = int_value;
                    if(l==1 || name.indexOf('t') != -1) box.size[net.pierrox.lightning_launcher.data.Box.MT] = int_value;
                    if(l==1 || name.indexOf('r') != -1) box.size[net.pierrox.lightning_launcher.data.Box.MR] = int_value;
                    if(l==1 || name.indexOf('b') != -1) box.size[net.pierrox.lightning_launcher.data.Box.MB] = int_value;
                } else if(c0 == 'p') {
                    if(l==1 || name.indexOf('l') != -1) box.size[net.pierrox.lightning_launcher.data.Box.PL] = int_value;
                    if(l==1 || name.indexOf('t') != -1) box.size[net.pierrox.lightning_launcher.data.Box.PT] = int_value;
                    if(l==1 || name.indexOf('r') != -1) box.size[net.pierrox.lightning_launcher.data.Box.PR] = int_value;
                    if(l==1 || name.indexOf('b') != -1) box.size[net.pierrox.lightning_launcher.data.Box.PB] = int_value;
                } else if(name.startsWith("bs")) {
                    if(l==2 || name.indexOf('l', 2) != -1) box.size[net.pierrox.lightning_launcher.data.Box.BL] = int_value;
                    if(l==2 || name.indexOf('t', 2) != -1) box.size[net.pierrox.lightning_launcher.data.Box.BT] = int_value;
                    if(l==2 || name.indexOf('r', 2) != -1) box.size[net.pierrox.lightning_launcher.data.Box.BR] = int_value;
                    if(l==2 || name.indexOf('b', 2) != -1) box.size[net.pierrox.lightning_launcher.data.Box.BB] = int_value;
                } else if(name.startsWith("bc")) {
                    boolean no_border = true;
                    for(int n=0; n<4; n++) {
                        if (name.indexOf(border_letters[n], 2) != -1) {
                            no_border = false;
                            break;
                        }
                    }
                    boolean no_mode = name.indexOf('n', 2) == -1 && name.indexOf('s', 2) == -1 && name.indexOf('f', 2) == -1;
                    for(int n=0; n<4; n++) {
                        if(no_border || name.indexOf(border_letters[n], 2) != -1) {
                            int bc = border_codes[n];
                            if (no_mode || name.indexOf('n', 2) != -1) box.border_color[net.pierrox.lightning_launcher.data.Box.COLOR_SHIFT_N + bc] = int_value;
                            if (no_mode || name.indexOf('s', 2) != -1) box.border_color[net.pierrox.lightning_launcher.data.Box.COLOR_SHIFT_S + bc] = int_value;
                            if (no_mode || name.indexOf('f', 2) != -1) box.border_color[net.pierrox.lightning_launcher.data.Box.COLOR_SHIFT_F + bc] = int_value;
                        }
                    }
                } else if("ah".equals(name)) {
                    box.ah = net.pierrox.lightning_launcher.data.Box.AlignH.valueOf(data);
                } else if("av".equals(name)) {
                    box.av = net.pierrox.lightning_launcher.data.Box.AlignV.valueOf(data);
                }
            }
        }
    }
}
