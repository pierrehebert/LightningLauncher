package net.pierrox.lightning_launcher.configuration;

import net.pierrox.lightning_launcher.data.Box;
import net.pierrox.lightning_launcher.data.EventAction;
import net.pierrox.lightning_launcher.data.JsonLoader;

import java.util.HashMap;

public class PageConfig extends JsonLoader {
	public enum ScreenOrientation {
		AUTO,
		PORTRAIT,
		LANDSCAPE,
		SYSTEM
	}
	
	public enum SizeMode {
		AUTO,
		NUM,
		SIZE
	}
	
	public enum ScrollingDirection {
		AUTO,
		X,
		Y,
		XY,
		NONE
	}
	
	public enum OverScrollMode {
		DECELERATE,
		BOUNCE,
		NONE
	}
	
    public enum ScaleType {
        CENTER,
        FIT,
//        CENTER_CROP,
//        CENTER_INSIDE,
    }
	
	// label used for styles
	public String l;
	
	public ItemConfig defaultItemConfig=new ItemConfig();
	public ShortcutConfig defaultShortcutConfig=new ShortcutConfig();
	public FolderConfig defaultFolderConfig=new FolderConfig();
    public DynamicTextConfig defaultDynamicTextConfig=new DynamicTextConfig();

    public boolean newOnGrid=true;
	public boolean allowDualPosition=false;
	
	public SizeMode gridPColumnMode =SizeMode.NUM;
	public int gridPColumnNum=5;
	public int gridPColumnSize =100;
	public SizeMode gridPRowMode =SizeMode.NUM;
	public int gridPRowNum=5;
	public int gridPRowSize =100;

    public SizeMode gridLColumnMode=SizeMode.NUM;
    public int gridLColumnNum=5;
    public int gridLColumnSize=100;
    public SizeMode gridLRowMode=SizeMode.NUM;
    public int gridLRowNum=5;
    public int gridLRowSize=100;

    public boolean gridPL=true;

	public int gridLayoutModeHorizontalLineColor=0;
	public float gridLayoutModeHorizontalLineThickness=0;
	public int gridLayoutModeVerticalLineColor=0;
	public float gridLayoutModeVerticalLineThickness=0;
	public boolean gridAbove=false;
	
//	public int backgroundColor=0xff000000;
//	public boolean backgroundWallpaper=true;
//	public boolean backgroundWallpaperScroll=false;
//	public int backgroundWallpaperTintColor=0;
//	public boolean backgroundWallpaperSoftware=false;
//	public String backgroundWallpaperPath=null;
//	public int backgroundWallpaperWidth=0;
//	public int backgroundWallpaperHeight=0;
	
	public boolean bgSystemWPScroll=false;
	public int bgSystemWPWidth=0;
	public int bgSystemWPHeight=0;
	public int bgColor=0;
	public ScaleType bgScaleType=ScaleType.CENTER;

	
	public boolean statusBarHide=false;
	public boolean statusBarOverlap=false;
	public boolean navigationBarOverlap =false;
    public int statusBarColor=0;
    public int navigationBarColor=0;
    public boolean statusBarLight=false;
    public boolean navigationBarLight=false;

	public ScreenOrientation screenOrientation=ScreenOrientation.SYSTEM;
//	public ScreenRotationWhat screenRotationWhat=ScreenRotationWhat.ROTATE_WORKSPACE;
//	public ScreenRotationMode screenRotationMode=ScreenRotationMode.ANIMATED;
	
	public ScrollingDirection scrollingDirection=ScrollingDirection.AUTO;
	public OverScrollMode overScrollMode=OverScrollMode.DECELERATE;
	public float scrollingSpeed=2;
	public boolean noDiagonalScrolling=true;
	
	public boolean pinchZoomEnable=true;
	
	public boolean snapToPages=true;
	
	public boolean fitDesktopToItems=false;
	public boolean noScrollLimit=false;

    public boolean autoExit=false;

    public boolean rearrangeItems=false;
    public boolean swapItems=false;

    public boolean useDesktopSize=false;

    public boolean wrapX=false;
    public boolean wrapY=false;

    public boolean adHideActionBar=false;
    public boolean adDisplayABOnScroll =true;
    public int adDisplayedModes = -1;
    public int adActionBarTextColor = 0;

	public String iconPack;

	public boolean lwpStdEvents;

	public EventAction homeKey = EventAction.UNSET();
	public EventAction menuKey = EventAction.UNSET();
	public EventAction longMenuKey = EventAction.UNSET();
    public EventAction backKey = EventAction.UNSET();
	public EventAction longBackKey = EventAction.UNSET();
	public EventAction searchKey = EventAction.UNSET();
	public EventAction bgTap = EventAction.UNSET();
	public EventAction bgDoubleTap = EventAction.UNSET();
	public EventAction bgLongTap = EventAction.UNSET();
	public EventAction swipeLeft = EventAction.UNSET();
	public EventAction swipeRight = EventAction.UNSET();
	public EventAction swipeUp = EventAction.UNSET();
	public EventAction swipeDown = EventAction.UNSET();
	public EventAction swipe2Left = EventAction.UNSET();
	public EventAction swipe2Right = EventAction.UNSET();
	public EventAction swipe2Up = EventAction.UNSET();
	public EventAction swipe2Down = EventAction.UNSET();
	public EventAction screenOn = EventAction.UNSET();
	public EventAction screenOff = EventAction.UNSET();
    public EventAction orientationPortrait = EventAction.UNSET();
    public EventAction orientationLandscape = EventAction.UNSET();
    public EventAction posChanged = EventAction.UNSET();
    public EventAction load = EventAction.UNSET();
    public EventAction paused = EventAction.UNSET();
    public EventAction resumed = EventAction.UNSET();
	public EventAction itemAdded = EventAction.UNSET();
    public EventAction itemRemoved = EventAction.UNSET();
    public EventAction menu = EventAction.UNSET();

    public String tag;
    public HashMap<String,String> tags;

    public void applyDefaultFolderConfig() {
        Box box = defaultFolderConfig.box;
        box.ccn = 0xb0000000;
        box.size[Box.PL] = box.size[Box.PT] = box.size[Box.PR] = box.size[Box.PB] = 20;
    }
}