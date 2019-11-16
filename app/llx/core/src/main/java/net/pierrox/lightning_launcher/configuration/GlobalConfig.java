package net.pierrox.lightning_launcher.configuration;

import net.pierrox.lightning_launcher.data.EventAction;
import net.pierrox.lightning_launcher.data.JsonLoader;
import net.pierrox.lightning_launcher.data.Page;


public class GlobalConfig extends JsonLoader {
	public enum PageAnimation {
		NONE,
		FADE,
		SLIDE_H,
		SLIDE_V
	}



    public enum OverlayHandlePosition {
        LEFT_TOP,
        LEFT_MIDDLE,
        LEFT_BOTTOM,
        RIGHT_TOP,
        RIGHT_MIDDLE,
        RIGHT_BOTTOM,
        TOP_LEFT,
        TOP_CENTER,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_CENTER,
        BOTTOM_RIGHT
    }

    public enum OverlayAnimation {
        SLIDE,
        FADE
    }

	public static final int CATEGORY=-1;
	public static final int UNSET=0;
	public static final int NOTHING=1;
	public static final int APP_DRAWER=2;
	public static final int ZOOM_FULL_SCALE=3;
	public static final int ZOOM_TO_ORIGIN=4;
	public static final int SWITCH_FULL_SCALE_OR_ORIGIN=5;
	public static final int SHOW_HIDE_STATUS_BAR=6;
	public static final int LAUNCHER_MENU=7;
	public static final int EDIT_LAYOUT=8;
	public static final int CUSTOMIZE_LAUNCHER=9;
	public static final int CUSTOMIZE_DESKTOP =10;
	public static final int CUSTOMIZE_ITEM=11;
	public static final int ITEM_MENU=12;
	public static final int LAUNCH_ITEM=13;
	public static final int SEARCH=14;
	public static final int SHOW_HIDE_APP_MENU=15;
	public static final int SHOW_HIDE_APP_MENU_STATUS_BAR=16;
	public static final int SHOW_NOTIFICATIONS=17;
	public static final int PREVIOUS_DESKTOP =18;
	public static final int NEXT_DESKTOP =19;
	public static final int LAUNCH_APP=20;
	public static final int MOVE_ITEM=21;
	public static final int ADD_ITEM=22;
	public static final int LAUNCH_SHORTCUT=23;
	public static final int SELECT_WALLPAPER=24;
	public static final int GO_HOME=25;
	public static final int GO_HOME_ZOOM_TO_ORIGIN=26;
    public static final int SELECT_DESKTOP_TO_GO_TO=27;
    public static final int RESTART=28;
    public static final int CLOSE_TOPMOST_FOLDER=29;
    public static final int CLOSE_ALL_FOLDERS=30;
    public static final int SEARCH_APP=31;
    public static final int OPEN_FOLDER=32;
    public static final int GO_DESKTOP_POSITION=33;
    public static final int UNLOCK_SCREEN=34;
    public static final int RUN_SCRIPT=35;
    public static final int BACK=36;
    public static final int CUSTOM_MENU=37;
    public static final int USER_MENU=38;
    public static final int WALLPAPER_TAP=39;
    public static final int WALLPAPER_SECONDARY_TAP=40;
    public static final int SET_VARIABLE=41;
    public static final int SHOW_FLOATING_DESKTOP=42;
    public static final int HIDE_FLOATING_DESKTOP=43;
    public static final int OPEN_HIERARCHY_SCREEN=44;
    public static final int SHOW_APP_SHORTCUTS=45;
    public static final int CLOSE_APP_DRAWER=46;
    // update script EventHandler accordingly

    public int version=1;
	
	public EventAction homeKey = new EventAction(GO_HOME_ZOOM_TO_ORIGIN, null);
	public EventAction menuKey = new EventAction(SHOW_HIDE_APP_MENU_STATUS_BAR, null);
	public EventAction longMenuKey = EventAction.NOTHING();
    public EventAction backKey = new EventAction(BACK, null);;
	public EventAction longBackKey = EventAction.NOTHING();
	public EventAction searchKey = new EventAction(SEARCH, null);
	public EventAction itemTap = new EventAction(LAUNCH_ITEM, null);
	public EventAction itemLongTap = new EventAction(MOVE_ITEM, null);
	public EventAction bgTap = new EventAction(CLOSE_TOPMOST_FOLDER, null);
	public EventAction bgDoubleTap = new EventAction(SWITCH_FULL_SCALE_OR_ORIGIN, null);
	public EventAction bgLongTap = new EventAction(LAUNCHER_MENU, null);
	public EventAction swipeLeft = EventAction.NOTHING();
	public EventAction swipeRight = EventAction.NOTHING();
	public EventAction swipeUp = EventAction.NOTHING();
	public EventAction swipeDown = EventAction.NOTHING();
	public EventAction swipe2Left = new EventAction(PREVIOUS_DESKTOP, null);
	public EventAction swipe2Right = new EventAction(NEXT_DESKTOP, null);
	public EventAction swipe2Up = EventAction.NOTHING();
	public EventAction swipe2Down = EventAction.NOTHING();
	public EventAction screenOn = EventAction.NOTHING();
	public EventAction screenOff = EventAction.NOTHING();
    public EventAction orientationPortrait = EventAction.NOTHING();
    public EventAction orientationLandscape = EventAction.NOTHING();
    public EventAction itemAdded = EventAction.NOTHING();
    public EventAction itemRemoved = EventAction.NOTHING();
    public EventAction menu = EventAction.NOTHING();
    public EventAction startup = EventAction.NOTHING();


	public PageAnimation pageAnimation=PageAnimation.FADE;
	

	public int[] screensOrder=null;
	public String[] screensNames=null;
	public int homeScreen= Page.FIRST_DASHBOARD_PAGE;
	public int lockScreen= Page.NONE;
	public boolean launchUnlock=true;
	public boolean lockDisableOverlay=false;
	public boolean runScripts=true;


    public int overlayScreen = Page.NONE;
    public OverlayHandlePosition overlayShowHandlePosition = OverlayHandlePosition.LEFT_TOP;
    public float overlayShowHandleSize = 0.2f;
    public float overlayShowHandleWidth = 0.03f;
    public OverlayHandlePosition overlayHideHandlePosition = OverlayHandlePosition.RIGHT_MIDDLE;
    public float overlayHideHandleSize = 1f;
    public float overlayHideHandleWidth = 0.03f;
    public OverlayAnimation overlayAnimation = OverlayAnimation.SLIDE;
    public boolean overlayDisplayHandles = false;
    public boolean overlayLaunchHide = true;

    public int lwpScreen = Page.NONE;

    public int getPageIndex(int p) {
        for(int i=0; i<screensOrder.length; i++) {
            if(screensOrder[i] == p) return i;
        }
        return 0;
    }

    public boolean reAddScreenIfNeeded(int p) {
        if(!Page.isDashboard(p)) {
            return false;
        }

        int n = screensOrder.length;
        for(int i=0; i<n; i++) {
            if(screensOrder[i] == p) return false;
        }

        int[] newScreensOrder = new int[n+1];
        System.arraycopy(screensOrder, 0, newScreensOrder, 0, n);
        newScreensOrder[n] = p;
        screensOrder = newScreensOrder;
        
        String[] newScreensNames = new String[n+1];
        System.arraycopy(screensNames, 0, newScreensNames, 0, n);
        newScreensNames[n] = String.valueOf(p);
        screensNames = newScreensNames;

        return true;
    }
}
