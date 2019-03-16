package net.pierrox.lightning_launcher.script.api;

import android.content.res.Resources;
import android.util.Pair;

import net.pierrox.lightning_launcher.R;

import java.util.LinkedHashMap;

/**
 * Enumeration class. Represents a Property object {@see PropertySet} {@see PropertyEditor}.
 *
 * An instance of this object can be created with {@link #getByName(String)}.
 */
public class Property {
    public static final int TYPE_UNKNOWN = -1;
    public static final int TYPE_BOOLEAN = 0;
    public static final int TYPE_INTEGER = 1;
    public static final int TYPE_FLOAT = 2;
    public static final int TYPE_STRING = 3;
    
    private String mName;
    private String mLabel;
    private int mType;

    private static LinkedHashMap<String,Property> ALL;
    private static Pair<String,Property[]>[] ALL_FOR_ITEM;
    private static Pair<String,Property[]>[] ALL_FOR_SHORTCUT;
    private static Pair<String,Property[]>[] ALL_FOR_DYNAMIC_TEXT;
    private static Pair<String,Property[]>[] ALL_FOR_FOLDER;
    private static Pair<String,Property[]>[] ALL_FOR_PAGE_INDICATOR;

    public static final String PROP_VOID = "v";

    public static final String PROP_PAGE_BG_COLOR = "bgColor";
    public static final String PROP_PAGE_STATUS_BAR_COLOR = "statusBarColor";
    public static final String PROP_PAGE_NAV_BAR_COLOR = "navigationBarColor";
    public static final String PROP_PAGE_SCROLLING_DIRECTION = "scrollingDirection";
//    public static final String PROP_PAGE_ICON_PACK = "iconPack";

    public static final String PROP_CELL_LEFT = "c.l";
    public static final String PROP_CELL_TOP = "c.t";
    public static final String PROP_CELL_WIDTH = "c.w";
    public static final String PROP_CELL_HEIGHT = "c.h";

    public static final String PROP_TRANSFORM_X = "t.x";
    public static final String PROP_TRANSFORM_Y = "t.y";
    public static final String PROP_TRANSFORM_W = "t.w";
    public static final String PROP_TRANSFORM_H = "t.h";
    public static final String PROP_TRANSFORM_A = "t.a";
    public static final String PROP_TRANSFORM_SX = "t.sx";
    public static final String PROP_TRANSFORM_SY = "t.sy";
    public static final String PROP_TRANSFORM_KX = "t.kx";
    public static final String PROP_TRANSFORM_KY = "t.ky";

    public static final String PROP_ITEM_ALPHA = "i.alpha";
    public static final String PROP_ITEM_VISIBILITY = "i.visibility";
    public static final String PROP_ITEM_Z_INDEX = "i.zindex";
    public static final String PROP_ITEM_BOX = "i.box";
    public static final String PROP_ITEM_ENABLED = "i.enabled";
    public static final String PROP_ITEM_ON_GRID = "i.onGrid";

    public static final String PROP_FOLDER_TITLE_VISIBILITY = "f.titleVisibility";
    public static final String PROP_FOLDER_TITLE_FONT_COLOR = "f.titleFontColor";
    public static final String PROP_FOLDER_TITLE_FONT_SIZE = "f.titleFontSize";
    public static final String PROP_FOLDER_W_AH = "f.wAH";
    public static final String PROP_FOLDER_W_AV = "f.wAV";
    public static final String PROP_FOLDER_W_X = "f.wX";
    public static final String PROP_FOLDER_W_Y = "f.wY";
    public static final String PROP_FOLDER_W_W = "f.wW";
    public static final String PROP_FOLDER_W_H = "f.wH";
    public static final String PROP_FOLDER_BOX = "f.box";

    public static final String PROP_TEXT_LABEL = "s.label";
    public static final String PROP_TEXT_LABEL_VISIBILITY = "s.labelVisibility";
    public static final String PROP_TEXT_COLOR_NORMAl = "s.labelFontColor";
    public static final String PROP_TEXT_COLOR_SELECTED = "s.selectionColorLabel";
    public static final String PROP_TEXT_COLOR_FOCUSED = "s.focusColorLabel";
    public static final String PROP_TEXT_FONT_STYLE = "s.labelFontStyle";
    public static final String PROP_TEXT_FONT_SIZE = "s.labelFontSize";

    public static final String PROP_ICON_VISIBILITY = "s.iconVisibility";
    public static final String PROP_ICON_SCALE = "s.iconScale";
    public static final String PROP_ICON_REFLECTION = "s.iconReflection";
    public static final String PROP_ICON_COLOR_FILTER = "s.iconColorFilter";

    /**
     * @hide
     */
    public static void buildLists(Resources resources) {
        Property[] for_geometry_grid = new Property[]{
                new Property(resources.getString(R.string.gb_l), PROP_CELL_LEFT, TYPE_INTEGER),
                new Property(resources.getString(R.string.gb_t), PROP_CELL_TOP, TYPE_INTEGER),
                new Property(resources.getString(R.string.gb_w), PROP_CELL_WIDTH, TYPE_INTEGER),
                new Property(resources.getString(R.string.gb_h), PROP_CELL_HEIGHT, TYPE_INTEGER),
        };

        Property[] for_geometry_free = new Property[] {
                new Property(resources.getString(R.string.gb_l), PROP_TRANSFORM_X, TYPE_FLOAT),
                new Property(resources.getString(R.string.gb_t), PROP_TRANSFORM_Y, TYPE_FLOAT),
                new Property(resources.getString(R.string.gb_w), PROP_TRANSFORM_W, TYPE_FLOAT),
                new Property(resources.getString(R.string.gb_h), PROP_TRANSFORM_H, TYPE_FLOAT),
                new Property(resources.getString(R.string.gb_a), PROP_TRANSFORM_A, TYPE_FLOAT),
                new Property(resources.getString(R.string.gb_sx), PROP_TRANSFORM_SX, TYPE_FLOAT),
                new Property(resources.getString(R.string.gb_sy), PROP_TRANSFORM_SY, TYPE_FLOAT),
                new Property(resources.getString(R.string.gb_kx), PROP_TRANSFORM_KX, TYPE_FLOAT),
                new Property(resources.getString(R.string.gb_ky), PROP_TRANSFORM_KY, TYPE_FLOAT),
        };

        Property[] for_item = new Property[] {
                new Property(resources.getString(R.string.m_alpha_t), PROP_ITEM_ALPHA, TYPE_INTEGER),
                new Property(resources.getString(R.string.pv), PROP_ITEM_VISIBILITY, TYPE_BOOLEAN),
                new Property(resources.getString(R.string.pc), PROP_ITEM_ENABLED, TYPE_BOOLEAN),
                new Property(resources.getString(R.string.pz), PROP_ITEM_Z_INDEX, TYPE_INTEGER),
                new Property(resources.getString(R.string.pb), PROP_ITEM_BOX, TYPE_STRING),
                new Property(resources.getString(R.string.pd), PROP_VOID, TYPE_INTEGER),
        };

        Property[] for_text = new Property[] {
                new Property(resources.getString(R.string.pl), PROP_TEXT_LABEL, TYPE_STRING),
                new Property(resources.getString(R.string.l_display), PROP_TEXT_LABEL_VISIBILITY, TYPE_BOOLEAN),
                new Property(resources.getString(R.string.l_color_normal), PROP_TEXT_COLOR_NORMAl, TYPE_INTEGER),
                new Property(resources.getString(R.string.l_color_selected), PROP_TEXT_COLOR_SELECTED, TYPE_INTEGER),
                new Property(resources.getString(R.string.l_color_focused), PROP_TEXT_COLOR_FOCUSED, TYPE_INTEGER),
                new Property(resources.getString(R.string.l_size), PROP_TEXT_FONT_SIZE, TYPE_FLOAT),
                new Property(resources.getString(R.string.l_style_t), PROP_TEXT_FONT_STYLE, TYPE_STRING),
        };

        Property[] for_icon = new Property[] {
                new Property(resources.getString(R.string.i_display), PROP_ICON_VISIBILITY, TYPE_BOOLEAN),
                new Property(resources.getString(R.string.i_scale_t), PROP_ICON_SCALE, TYPE_FLOAT),
                new Property(resources.getString(R.string.i_reflect_enable), PROP_ICON_REFLECTION, TYPE_BOOLEAN),
                new Property(resources.getString(R.string.i_cf_t), PROP_ICON_COLOR_FILTER, TYPE_INTEGER),
        };

        Property[] for_folder = new Property[] {
                new Property(resources.getString(R.string.display_title), PROP_FOLDER_TITLE_VISIBILITY, TYPE_BOOLEAN),
                new Property(resources.getString(R.string.font_color), PROP_FOLDER_TITLE_FONT_COLOR, TYPE_INTEGER),
                new Property(resources.getString(R.string.font_size), PROP_FOLDER_TITLE_FONT_SIZE, TYPE_FLOAT),
                new Property(resources.getString(R.string.b_alignh_t), PROP_FOLDER_W_AH, TYPE_STRING),
                new Property(resources.getString(R.string.b_alignv_t), PROP_FOLDER_W_AV, TYPE_STRING),
                new Property(resources.getString(R.string.f_l), PROP_FOLDER_W_X, TYPE_INTEGER),
                new Property(resources.getString(R.string.f_t), PROP_FOLDER_W_Y, TYPE_INTEGER),
                new Property(resources.getString(R.string.wp_w_t), PROP_FOLDER_W_W, TYPE_INTEGER),
                new Property(resources.getString(R.string.wp_h_t), PROP_FOLDER_W_H, TYPE_INTEGER),
                new Property(resources.getString(R.string.pb), PROP_FOLDER_BOX, TYPE_STRING),
        };

        Property[] for_page_indicator = new Property[] {

        };

        Property[] all = gatherProperties(new Property[][] { for_geometry_free, for_geometry_grid, for_item, for_text, for_icon, for_folder, for_page_indicator});
        ALL = new LinkedHashMap<>();
        for(Property p : all) {
            ALL.put(p.mName, p);
        }

        ALL_FOR_ITEM = new Pair[] {
                new Pair(resources.getString(R.string.pg), for_geometry_grid),
                new Pair(resources.getString(R.string.pf), for_geometry_free),
                new Pair(resources.getString(R.string.pit), for_item),
        };

        ALL_FOR_SHORTCUT  = new Pair[] {
                new Pair(resources.getString(R.string.pg), for_geometry_grid),
                new Pair(resources.getString(R.string.pf), for_geometry_free),
                new Pair(resources.getString(R.string.pit), for_item),
                new Pair(resources.getString(R.string.tab_label), for_text),
                new Pair(resources.getString(R.string.tab_icon), for_icon),
        };

        ALL_FOR_DYNAMIC_TEXT   = new Pair[] {
                new Pair(resources.getString(R.string.pg), for_geometry_grid),
                new Pair(resources.getString(R.string.pf), for_geometry_free),
                new Pair(resources.getString(R.string.pit), for_item),
                new Pair(resources.getString(R.string.tab_label), for_text),
        };

        ALL_FOR_PAGE_INDICATOR   = new Pair[] {
                new Pair(resources.getString(R.string.pg), for_geometry_grid),
                new Pair(resources.getString(R.string.pf), for_geometry_free),
                new Pair(resources.getString(R.string.pit), for_item),
                new Pair(resources.getString(R.string.tab_label), for_text),
//                new Pair(resources.getString(R.string.tab_page_indicator), for_page_indicator),
        };

        ALL_FOR_FOLDER   = new Pair[] {
                new Pair(resources.getString(R.string.pg), for_geometry_grid),
                new Pair(resources.getString(R.string.pf), for_geometry_free),
                new Pair(resources.getString(R.string.pit), for_item),
                new Pair(resources.getString(R.string.tab_label), for_text),
                new Pair(resources.getString(R.string.tab_icon), for_icon),
                new Pair(resources.getString(R.string.folder), for_folder),
        };
    }

    /**
     * @hide
     */
    public Property(String code, String name, int type) {
        mName = name;
        mLabel = code;
        mType = type;
    }

    /**
     * @return the internal name of this property, as used with the {@link net.pierrox.lightning_launcher.script.api.PropertySet} and {@link net.pierrox.lightning_launcher.script.api.PropertyEditor} objects.
     */
    public String getName() {
        return mName;
    }

    /**
     * @return a short, human readable, possibly localized, text describing for this property.
     */
    public String getLabel() {
        return mLabel;
    }

    /**
     * @return type of this property (one of #TYPE_BOOLEAN, #TYPE_INTEGER, etc.).
     */
    public int getType() {
        return mType;
    }

    /**
     * @return a property identified by its name, or null if not a known property
     */
    public static Property getByName(String name) {
        if(name.startsWith("svg")) {
            return new Property(name, name, TYPE_STRING);
        }

        return ALL.get(name);
    }

    /**
     * Return the type of a property identified by its name.
     * @param name internal property name
     * @return the property's type, #TYPE_UNKNOWN if not a valid property
     */
    public static int getType(String name) {
        Property p = getByName(name);
        return p == null ? TYPE_UNKNOWN : p.getType();
    }

    /**
     * @hide
     */
    public static Pair<String,Property[]>[] getForItemClass(Class<?> cls) {
        if(cls == net.pierrox.lightning_launcher.data.Folder.class) {
            return ALL_FOR_FOLDER;
        } else if(cls == net.pierrox.lightning_launcher.data.Shortcut.class) {
            return ALL_FOR_SHORTCUT;
        } else if(cls == net.pierrox.lightning_launcher.data.DynamicText.class) {
            return ALL_FOR_DYNAMIC_TEXT;
        } else if(cls == net.pierrox.lightning_launcher.data.PageIndicator.class) {
            return ALL_FOR_PAGE_INDICATOR;
        } else {
            return ALL_FOR_ITEM;
        }
    }

    private static Property[] gatherProperties(Property[][] lists) {
        int total = 0;
        for(Property[] list : lists) {
            int n = list.length;
            total += n;
        }

        Property[] all = new Property[total];
        int i = 0;
        for(Property[] list : lists) {
            int n = list.length;
            System.arraycopy(list, 0, all, i, n);
            i += n;
        }

        return all;
    }

    /**
     * @hide
     */
    @Override
    public String toString() {
        return mLabel;
    }
}
