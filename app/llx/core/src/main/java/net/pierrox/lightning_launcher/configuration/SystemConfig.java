package net.pierrox.lightning_launcher.configuration;

import net.pierrox.lightning_launcher.data.JsonLoader;

public class SystemConfig extends JsonLoader {

    public static final int HINT_APP_DRAWER=1;
    public static final int HINT_FOLDER=2;
    public static final int HINT_PANEL=4;
    public static final int HINT_DESKTOP=8;
    public static final int HINT_SIDE_BAR=16;
    public static final int HINT_PAGE_INDICATOR=32;
    public static final int HINT_STOP_POINT=64;
    public static final int HINT_BOOKMARK=128;
    public static final int HINT_WRAP=256;
    public static final int HINT_CUSTOM_VIEW=512;
    public static final int HINT_WIDGET_DELETION=1024;
    public static final int HINT_LOCKED=2048;
    public static final int HINT_CUSTOMIZE_HELP=1<<12;
    public static final int HINT_RATE=1<<13;
    public static final int HINT_MY_DRAWER=1<<14;

    public enum AppStyle {
        LIGHT,
        DARK
    }

    public int version=0;

    public boolean autoEdit=false;
    public boolean alwaysShowStopPoints=false;

    public boolean keepInMemory=true;
    public String language=null;
    public boolean expertMode=false;
    public boolean hotwords=false;
    public AppStyle appStyle=AppStyle.LIGHT;

    public int hints=0;

    public float imagePoolSize=0.5f;

    public static final int SWITCH_SNAP = 1;
    public static final int SWITCH_EDIT_BARS = 2;
    public static final int SWITCH_PROPERTIES_BOX = 4;
    public static final int SWITCH_CONTENT_ZOOMED = 8;
    public static final int SWITCH_DISPLAY_INVISIBLE_ITEMS = 16;
    public static final int SWITCH_MULTI_SELECTION = 32;
    public static final int SWITCH_HONOUR_PINNED_ITEMS = 64;
    public int switches=SWITCH_SNAP|SWITCH_EDIT_BARS|SWITCH_CONTENT_ZOOMED|SWITCH_HONOUR_PINNED_ITEMS;

    public static final int EDIT_BOX_NONE = 0;
    public static final int EDIT_BOX_PROPERTIES = 1;
    public static final int EDIT_BOX_POSITION = 2;
    public static final int EDIT_BOX_ACTION = 3;
    public int editBoxMode = EDIT_BOX_NONE;
    public int editBoxPropHeight = 0;

    public boolean hasSwitch(int which) {
        return (switches & which) != 0;
    }

    public void setSwitch(int which, boolean on) {
        if(on) switches |= which; else switches &= ~which;
    }
}
