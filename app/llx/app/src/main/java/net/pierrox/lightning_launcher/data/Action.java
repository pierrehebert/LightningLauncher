package net.pierrox.lightning_launcher.data;

public class Action {
    public static final int FLAG_TYPE_DESKTOP = 1;
    public static final int FLAG_TYPE_APP_DRAWER = 2;
    public static final int FLAG_TYPE_SCRIPT = 4;
    public static final int FLAG_ITEM = 8;

    public static final int CAT_NONE = 0;
    public static final int CAT_LAUNCH_AND_APPS = 1;
    public static final int CAT_NAVIGATION = 2;
    public static final int CAT_MENU_STATUS_BAR = 3;
    public static final int CAT_FOLDERS = 4;
    public static final int CAT_EDITION = 5;
    public static final int CAT_EXTERNAL = 6;
    public static final int CAT_ADVANCED = 7;

    public int action;
    public int label;
    public int category;
    public int flags;
    public int minSdkVersion;

    public Action(int action, int label, int category, int flags, int minSdkVersion) {
        this.action = action;
        this.label = label;
        this.category = category;
        this.flags = flags;
        this.minSdkVersion = minSdkVersion;
    }
}
