package net.pierrox.lightning_launcher.activities;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.WallpaperManager;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.os.Process;
import android.os.SystemClock;
import android.os.UserHandle;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;
import android.view.Display;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewAnimator;

import com.boombuler.system.appwidgetpicker.AppWidgetPickerActivity;
import com.google.android.hotword.client.HotwordServiceClient;
import com.readystatesoftware.systembartint.SystemBarTintManager;

import net.pierrox.lightning_launcher.API;
import net.pierrox.lightning_launcher.LLApp;
import net.pierrox.lightning_launcher.Version;
import net.pierrox.lightning_launcher.api.ScreenIdentity;
import net.pierrox.lightning_launcher.configuration.DynamicTextConfig;
import net.pierrox.lightning_launcher.configuration.FolderConfig;
import net.pierrox.lightning_launcher.configuration.GlobalConfig;
import net.pierrox.lightning_launcher.configuration.ItemConfig;
import net.pierrox.lightning_launcher.configuration.PageConfig;
import net.pierrox.lightning_launcher.configuration.ShortcutConfig;
import net.pierrox.lightning_launcher.configuration.ShortcutConfigStylable;
import net.pierrox.lightning_launcher.configuration.SystemConfig;
import net.pierrox.lightning_launcher.data.Action;
import net.pierrox.lightning_launcher.data.Box;
import net.pierrox.lightning_launcher.data.ContainerPath;
import net.pierrox.lightning_launcher.data.CustomView;
import net.pierrox.lightning_launcher.data.DynamicText;
import net.pierrox.lightning_launcher.data.EmbeddedFolder;
import net.pierrox.lightning_launcher.data.Error;
import net.pierrox.lightning_launcher.data.EventAction;
import net.pierrox.lightning_launcher.data.FileUtils;
import net.pierrox.lightning_launcher.data.Folder;
import net.pierrox.lightning_launcher.data.Item;
import net.pierrox.lightning_launcher.data.JsonLoader;
import net.pierrox.lightning_launcher.data.LightningIntent;
import net.pierrox.lightning_launcher.data.Page;
import net.pierrox.lightning_launcher.data.PageIndicator;
import net.pierrox.lightning_launcher.data.SavedItemGeometry;
import net.pierrox.lightning_launcher.data.SelectionState;
import net.pierrox.lightning_launcher.data.Shortcut;
import net.pierrox.lightning_launcher.data.StopPoint;
import net.pierrox.lightning_launcher.data.UndoStack;
import net.pierrox.lightning_launcher.data.Unlocker;
import net.pierrox.lightning_launcher.data.Utils;
import net.pierrox.lightning_launcher.data.Widget;
import net.pierrox.lightning_launcher.engine.LightningEngine;
import net.pierrox.lightning_launcher.engine.Screen;
import net.pierrox.lightning_launcher.script.Script;
import net.pierrox.lightning_launcher.script.ScriptExecutor;
import net.pierrox.lightning_launcher.script.ScriptManager;
import net.pierrox.lightning_launcher.script.api.ImageBitmap;
import net.pierrox.lightning_launcher.util.AddItemDialog;
import net.pierrox.lightning_launcher.util.PhoneUtils;
import net.pierrox.lightning_launcher.util.Setup;
import net.pierrox.lightning_launcher.views.BubbleLayout;
import net.pierrox.lightning_launcher.views.CustomizeItemView;
import net.pierrox.lightning_launcher.views.EditBarHiderView;
import net.pierrox.lightning_launcher.views.FolderView;
import net.pierrox.lightning_launcher.views.HandleView;
import net.pierrox.lightning_launcher.views.HandleView.Handle;
import net.pierrox.lightning_launcher.views.HierarchyScreen;
import net.pierrox.lightning_launcher.views.ItemLayout;
import net.pierrox.lightning_launcher.views.MyViewPager;
import net.pierrox.lightning_launcher.views.SnappingContext;
import net.pierrox.lightning_launcher.views.TransformLayout;
import net.pierrox.lightning_launcher.views.item.EmbeddedFolderView;
import net.pierrox.lightning_launcher.views.item.ItemView;
import net.pierrox.lightning_launcher.views.item.StopPointView;
import net.pierrox.lightning_launcher.views.item.WidgetView;
import net.pierrox.lightning_launcher_extreme.BuildConfig;
import net.pierrox.lightning_launcher_extreme.R;

import org.mozilla.javascript.Function;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;




public class Dashboard extends ResourceWrapperActivity implements OnLongClickListener, OnClickListener, View.OnTouchListener, Setup.OnFirstTimeInitEvent, UndoStack.UndoListener, HierarchyScreen.HierarchyScreenListener {

    private static final int DIALOG_APP_NOT_INSTALLED=1;
    private static final int DIALOG_FIRST_USE=202;
    private static final int DIALOG_ADD=203;
    private static final int DIALOG_IMPORT_LL=204;
    private static final int DIALOG_STOP_POINT=206;
    private static final int DIALOG_WRAP=207;
    private static final int DIALOG_LAUNCHER_APPS_NO_HOST_PERMISSION =208;

	private static final String SIS_NAVIGATION="a";
	private static final String SIS_ALLOCATED_APP_WIDGET_ID="b";
	private static final String SIS_TMP_ITEM_ID="c";
	private static final String SIS_MODIFYING_WIDGET="d";
	private static final String SIS_TMP_COMPONENT_NAME ="i";
	private static final String SIS_ACTIVITY_RESULT_SCRIPT_ID ="j";
	private static final String SIS_ACTIVITY_RESULT_SCRIPT_TOKEN ="k";
	private static final String SIS_EDIT_MODE = "l";
	private static final String SIS_EDIT_ITEM_LAYOUT_PATH = "m";
	private static final String SIS_EDIT_MASTER_SELECTED_ITEM_ID = "n";
    private static final String SIS_HAVE_PROPERTIES_BOX = "o";
    private static final String SIS_PROPERTIES_BOX_VISIBLE = "p";
    private static final String SIS_EDIT_SELECTED_ITEMS_IDS = "q";
    private static final String SIS_SCRIPT_EXECUTOR_ENGINE_BASE_DIR_FOR_PICK = "r";
    private static final String SIS_EDIT_PAGE_ENGINE_DIR = "s";

//	private static final int EDIT_BACKGROUND_COLOR_SELECTED=0x80ffffff;
//	private static final int EDIT_BACKGROUND_COLOR_MOVE_OK=0xa000a000;
//	private static final int EDIT_BACKGROUND_COLOR_MOVE_KO=0xa0a00000;
//	private static final int EDIT_BACKGROUND_COLOR_FOLDER=0xa0008000;
	
	private static final int REQUEST_SELECT_APP_FOR_ADD = 0;
	private static final int REQUEST_SELECT_SHORTCUT_FOR_ADD1 = 1;
	private static final int REQUEST_SELECT_APP_WIDGET_FOR_ADD = 3;
//    private static final int REQUEST_SELECT_LL_WIDGET_FOR_ADD = 4;
    private static final int REQUEST_CREATE_APP_WIDGET = 5;
    private static final int REQUEST_SELECT_APP_FOR_PICK = 6;
    private static final int REQUEST_SELECT_SCREEN_FOR_MOVE = 7;
    private static final int REQUEST_SELECT_SCREEN_FOR_COPY = 8;
    private static final int REQUEST_SELECT_SCREEN_FOR_GOTO = 9;
    private static final int REQUEST_BIND_APPWIDGET = 10;
    private static final int REQUEST_SELECT_SHORTCUT_FOR_ADD2 = 11;
    private static final int REQUEST_SELECT_SHORTCUT_FOR_PICK1 = 12;
    private static final int REQUEST_SELECT_SHORTCUT_FOR_PICK2 = 13;
    private static final int REQUEST_SELECT_SCRIPT_TO_LOAD = 14;
    private static final int REQUEST_QUERY_SCRIPT_INFOS = 15;
    private static final int REQUEST_FOR_SCRIPT = 16;
    private static final int REQUEST_SCRIPT_PICK_IMAGE = 17;
    private static final int REQUEST_PICK_CUSTOM_ICON = 18;
    private static final int REQUEST_SCRIPT_CROP_IMAGE = 19;
    private static final int REQUEST_EDIT_LAUNCH_ACTION = 20;
    public static final int REQUEST_FROM_CUSTOMIZE_VIEW = 100;

    private static final String BROADCAST_ACTION_DISPLAY_PAGE = LLApp.LL_PKG_NAME+".ACTION_DISPLAY_PAGE";
	private static final String BROADCAST_ACTION_RELOAD = LLApp.LLX_PKG_NAME+".ACTION_RELOAD";
	public static final String BROADCAST_ACTION_LOCKED = LLApp.LLX_PKG_NAME+".ACTION_LOCKED";
	public static final String BROADCAST_ACTION_UNLOCKED = LLApp.LLX_PKG_NAME+".ACTION_UNLOCKED";


    private static final int BUBBLE_MODE_NONE = 0;
    private static final int BUBBLE_MODE_LIGHTNING_MENU_EM = 1;
    protected static final int BUBBLE_MODE_ITEM_EM = 2;
    private static final int BUBBLE_MODE_ITEM_EDIT = 3;
    protected static final int BUBBLE_MODE_ITEM_POSITION = 4;
    private static final int BUBBLE_MODE_ITEM_ACTIONS = 6;
    protected static final int BUBBLE_MODE_SETTINGS = 7;
    protected static final int BUBBLE_MODE_ITEMS = 8;
    private static final int BUBBLE_MODE_SCRIPTS = 9;
    private static final int BUBBLE_MODE_CUSTOM_MENU = 10;
    protected static final int BUBBLE_MODE_ITEM_NO_EM = 12;
    protected static final int BUBBLE_MODE_LIGHTNING_MENU_NO_EM = 13;
    protected static final int BUBBLE_MODE_SELECT = 14;
    protected static final int BUBBLE_MODE_APP_SHORTCUTS = 15;
    private static final int BUBBLE_MODE_HINT_APP_DRAWER = -1;
    private static final int BUBBLE_MODE_HINT_FOLDER = -2;
    private static final int BUBBLE_MODE_HINT_PANEL = -3;
    private static final int BUBBLE_MODE_HINT_DESKTOP = -4;
    private static final int BUBBLE_MODE_HINT_SIDE_BAR = -5;
    private static final int BUBBLE_MODE_HINT_PAGE_INDICATOR = -6;
    private static final int BUBBLE_MODE_HINT_STOP_POINT = -7;
    private static final int BUBBLE_MODE_HINT_BOOKMARK = -8;
    private static final int BUBBLE_MODE_HINT_CUSTOM_VIEW = -9;
    private static final int BUBBLE_MODE_HINT_LOCKED = -10;
    private static final int[][] BUBBLE_HINTS = new int[][] {
            { BUBBLE_MODE_HINT_APP_DRAWER, SystemConfig.HINT_APP_DRAWER, R.string.h_ad},
            { BUBBLE_MODE_HINT_FOLDER, SystemConfig.HINT_FOLDER, R.string.h_f},
            { BUBBLE_MODE_HINT_PANEL, SystemConfig.HINT_PANEL, R.string.h_p},
            { BUBBLE_MODE_HINT_DESKTOP, SystemConfig.HINT_DESKTOP, R.string.h_k},
            { BUBBLE_MODE_HINT_SIDE_BAR, SystemConfig.HINT_SIDE_BAR, R.string.h_sb},
            { BUBBLE_MODE_HINT_PAGE_INDICATOR, SystemConfig.HINT_PAGE_INDICATOR, R.string.h_pi},
            { BUBBLE_MODE_HINT_STOP_POINT, SystemConfig.HINT_STOP_POINT, R.string.h_sp},
            { BUBBLE_MODE_HINT_BOOKMARK, SystemConfig.HINT_BOOKMARK, R.string.h_b},
            { BUBBLE_MODE_HINT_CUSTOM_VIEW, SystemConfig.HINT_CUSTOM_VIEW, R.string.h_cv},
            { BUBBLE_MODE_HINT_LOCKED, SystemConfig.HINT_LOCKED, R.string.h_cl},
    };

    private static DecimalFormat sGeometryEditValueFormat = new DecimalFormat("0.###");

    protected LightningEngine mEngine;
    protected Screen mScreen;

    protected Handler mHandler;

    private boolean mSetupInProgress;

	private ViewAnimator mViewAnimator;
    private ViewGroup mEditControlsView;

    private HierarchyScreen mHierarchyScreen;

	protected SystemConfig mSystemConfig;

    // TODO: usage of this should be reduced as much as possible
	protected GlobalConfig mGlobalConfig;

    protected Stack<Integer> mNavigationStack;

    private Dialog mDialog;
    
	private long mOnPauseDate;
	private boolean mNewIntent;
	private boolean mEatNextHome;

//	private boolean mIsOptionsMenuOpened;
	
	private Animation mFadeInAnim;
	private Animation mFadeOutAnim;
	private AnimationSet mSlideHLeftInAnim;
	private AnimationSet mSlideHRightOutAnim;
	private AnimationSet mSlideHRightInAnim;
	private AnimationSet mSlideHLeftOutAnim;
	private AnimationSet mSlideVUpInAnim;
	private AnimationSet mSlideVDownOutAnim;
	private AnimationSet mSlideVDownInAnim;
	private AnimationSet mSlideVUpOutAnim;

    private ItemConfig.LaunchAnimation mLastLaunchAnimation;


    private Item mCopyStyleFromItem;
    
    private boolean mItemSwipeCaught=true;
    
    private int mAllocatedAppWidgetId;
//    private int mReplacedAppWidgetId=-1;
    
    protected boolean mEditMode;

    private Page mEditPage;
    protected ItemLayout mEditItemLayout;

    private SparseArray<SavedItemGeometry> mTrackedItemsGeometry = new SparseArray<>(1);

    private float mCurrentMoveItemDx;
    private float mCurrentMoveItemDy;
    private Handle mTrackedHandle;
    protected boolean mMoveStarted;
    private Item mItemLongTappedInNormalMode;

    private ItemView mDropFolderView;
    private ItemView mCandidateDropFolderView;
    private int mOldItemAlpha;
    private long mSelectDropFolderStartTime;
    private Runnable mSelectDropFolder=new Runnable() {
        @Override
        public void run() {
            long now = SystemClock.uptimeMillis();
            long delta = now - mSelectDropFolderStartTime;
            if(delta > DROP_FOLDER_DELAY) {
                mDropFolderView = mCandidateDropFolderView;
                mCandidateDropFolderView = null;
                mDropFolderView.setBackgroundColor(0);
                mDropFolderView.setFocused(true);
                // TODO SEL save (and restore) item alpha
//                mOldItemAlpha = mMasterSelectedItem.getAlpha();
//                mMasterSelectedItem.setAlpha(120);
            } else {
                int alpha = (int)(delta * 255 / DROP_FOLDER_DELAY);
                int color = Color.argb(alpha, 255, 255, 255);
                mCandidateDropFolderView.setBackgroundColor(color);
                mHandler.post(mSelectDropFolder);
            }
        }
    };
    private static final long DROP_FOLDER_DELAY = 1000;

    // indexed using the id of the item responsible for the swap
    private SparseArray<ArrayList<ItemView>> mSwappedItems = new SparseArray<>();

    protected SparseArray<SavedItemGeometry> mOriginalItemsGeometry = new SparseArray<>();

    private Item mTmpItem;

    private ComponentName mTmpComponentName;

    private int mActivityResultScriptId = Script.NO_ID;
    private String mActivityResultScriptToken;

    // the script executor to call back when a script issues a call that can be interrupted (activity destroyed/recreated while picking an image)
    private ScriptExecutor mScriptExecutorForCallback;

    // widget currently in modification (need to reload it on resume)
    private Item mModifyingWidget;

	protected Rect mTempRect=new Rect();
	protected RectF mTempRectF=new RectF();
	private int[] mTempCoords=new int[2];

    protected UndoStack mUndoStack;

    private LinearLayout mEditActionBox;
    private ViewGroup mEditPropertiesContainer;
    private View mEditPropertiesHandle;
    private TextView mEditPropertiesTitle;
    private TextView mEditPropertiesModePrevious;
    private TextView mEditPropertiesModeNext;
    private Button mEditPropertiesMode;
    private boolean mEditPropertiesOnTop;
    private CustomizeItemView mEditPropertiesBox;
    private View mEditBarRight;
    private View mEditBarBottom;
    private boolean mEditBarsVisible;
    private EditBarHiderView mEditBarHider;
    private float mEditBarsScale;
    private int mEditBarsWidth;
    private int mEditBarsHeight;
    private int mEditBarsSize;

    private View mGeometryBox;
    private int mGeometryMode;
    private Button mGeometryEdit1;
    private Button mGeometryEdit2;
    private int mGeometryEdit;
    private boolean mHasGeometryRepeat;
    private Runnable mGeometryRepeat = new Runnable() {
        @Override
        public void run() {
            boolean masterOnGrid = mEditItemLayout.getMasterSelectedItem().getItemConfig().onGrid;
            for (ItemView itemView : getSelectedItemViews()) {
                if(itemView.getItem().getItemConfig().onGrid == masterOnGrid) {
                    incrementCurrentGeometryValue(itemView.getItem(), mGeometryEdit);
                }
            }
            mGeometryBox.postDelayed(mGeometryRepeat, 50);
        }
    };


    private int mNoScriptCounter;

    private HotwordServiceClient mHotwordServiceClient;

    private boolean mPausedBecauseOfLaunch;

    protected int mBubbleMode = BUBBLE_MODE_NONE;
    private Animation mBubbleAnimIn;
    private Animation mBubbleAnimOut;
    protected BubbleLayout mBubble;
    private LinearLayout mBubbleContent;
    protected ItemLayout mBubbleItemLayout;
    protected ItemView mBubbleItemView;

    private Shortcut mNotValidShortcut;

    private WallpaperManager mWallpaperManager;

    protected boolean mIsAndroidActionBarDisplayed;
    protected Function mABOnCreateOptionsMenu;
    protected Function mABOnOptionsItemSelected;

    private ItemLayout mEditItemLayoutBeforeLock;
    private SelectionState mSelectionStateBeforeLock;
	private BroadcastReceiver mBroadcastReceiver=new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action=intent.getAction();
			if(BROADCAST_ACTION_DISPLAY_PAGE.equals(action)) {
                // deprecated but still in use by pager widgets
				int page=intent.getIntExtra("page", Page.FIRST_DASHBOARD_PAGE);
				if(page< Page.FIRST_DASHBOARD_PAGE || page> Page.LAST_DASHBOARD_PAGE) {
					page = Page.FIRST_DASHBOARD_PAGE;
				}
				removeStickyBroadcast(intent);
				setPagerPage(page, Screen.PAGE_DIRECTION_HINT_AUTO);
            } else if(BROADCAST_ACTION_UNLOCKED.equals(action)) {
                if(mEditItemLayoutBeforeLock != null) {
                    enterEditMode(mEditItemLayout, null);
                    setSelectionState(mSelectionStateBeforeLock);
                    mEditItemLayoutBeforeLock = null;
                    mSelectionStateBeforeLock = null;
                }
            } else if(BROADCAST_ACTION_LOCKED.equals(action)) {
                if(mEditMode) {
                    // save edit mode state before to lock, it will be restablished after unlock
                    mEditItemLayoutBeforeLock = mEditItemLayout;
                    mSelectionStateBeforeLock = getSelectionState();
                    leaveEditMode();
                }
                closeBubble();
            }
		}
	};

    private static Method sBindAppWidgetIdIfAllowed;

    static {
        try {
            sBindAppWidgetIdIfAllowed = AppWidgetManager.class.getMethod("bindAppWidgetIdIfAllowed", int.class, ComponentName.class);
        } catch (NoSuchMethodException e) {
        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
//        Utils.setTheme(this, Utils.APP_THEME_NO_ACTION_BAR_WALLPAPER);

        super.onCreate(savedInstanceState);

        mHandler = new Handler();


        final LLApp app = LLApp.get();

        mSystemConfig = app.getSystemConfig();

        mEngine = app.getAppEngine();
        mGlobalConfig=mEngine.getGlobalConfig();

        mScreen = createScreen();
        mScreen.setWindow(getWindow());

        View content_view = mScreen.getContentView();

        mEditControlsView = (ViewGroup) content_view.findViewById(R.id.edit_controls);

        mHierarchyScreen = new HierarchyScreen(mEngine, mScreen, this);

        try {
            Method getActionBar=getClass().getMethod("getActionBar");
            Object action_bar=getActionBar.invoke(this, (Object[])null);
            action_bar.getClass().getMethod("hide").invoke(action_bar, (Object[])null);
        } catch(Exception e) {
            // pass, API level 11
        }

        setContentView(content_view);

        android.view.Window w = getWindow();
        w.setFormat(PixelFormat.RGBA_8888);


        Rect visibleFrame = new Rect();
        getWindow().getDecorView().getWindowVisibleDisplayFrame(visibleFrame);

        mWallpaperManager = WallpaperManager.getInstance(this);

        mEditBarsSize = getResources().getDimensionPixelSize(R.dimen.eb_bar_size);

        mUndoStack = new UndoStack(this, new File(getCacheDir(), "undo-"+this.getClass().getSimpleName()), 100);
        mUndoStack.setUndoListener(this);


        if(BuildConfig.IS_BETA) {
            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread paramThread, Throwable paramThrowable) {
                    paramThrowable.printStackTrace();

                    String dump;
                    try {
                        dump = Utils.dumpStats(Dashboard.this, paramThrowable);
                    } catch (IOException e) {
                        dump = "";
                    }

                    String version = "";
                    try {
                        PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
                        version = pi.versionName + " - " + pi.versionCode;
                    } catch (PackageManager.NameNotFoundException e) {
                    }

                    Intent email_intent = new Intent(android.content.Intent.ACTION_SEND, Uri.parse("mailto:"));
                    email_intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    email_intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"pierre@lightninglauncher.com"});
                    email_intent.putExtra(Intent.EXTRA_SUBJECT, "Lightning Launcher bug report - " + version);
                    email_intent.putExtra(Intent.EXTRA_TEXT, dump);
                    email_intent.setType("message/rfc822");
                    startActivity(Intent.createChooser(email_intent, "Lightning Launcher bug report"));

                    PhoneUtils.selectLauncher(Dashboard.this, false);

                    try {
                        app.saveAllData();
                    } catch (Throwable t) {
                        // pass
                    }

                    System.exit(1);
                }
            });
        }

        createActivity(savedInstanceState);

        if(savedInstanceState!=null) {
            if(savedInstanceState.getBoolean(SIS_EDIT_MODE)) {
                // FIXME: instead of SIS_EDIT_PAGE_ID there should be a full path in order to recover the edit mode in the right item layout
                String path = savedInstanceState.getString(SIS_EDIT_ITEM_LAYOUT_PATH);
                if(path != null) {
                    ItemLayout il = mScreen.prepareItemLayoutForPath(new ContainerPath(path));

                    int masterSelectedItemId = savedInstanceState.getInt(SIS_EDIT_MASTER_SELECTED_ITEM_ID);
                    Item masterSelectedItem = null;
                    if (masterSelectedItemId != Item.NO_ID) {
                        masterSelectedItem = il.getPage().findItemById(masterSelectedItemId);
                    }
                    ArrayList<Integer> selectedItemsIds = savedInstanceState.getIntegerArrayList(SIS_EDIT_SELECTED_ITEMS_IDS);
                    enterEditMode(il, masterSelectedItem);
                    for (Integer id : selectedItemsIds) {
                        il.getItemView(id).setSelected(true);
                    }

                    if (savedInstanceState.getBoolean(SIS_HAVE_PROPERTIES_BOX)) {
                        setupEditPropertiesBox();
                        mEditPropertiesBox.restoreInstanceState(savedInstanceState);
                        if (savedInstanceState.getBoolean(SIS_PROPERTIES_BOX_VISIBLE)) {
                            showEditPropertiesBox();
                        }
                        updateEditPropertiesBox(il.getPage(), masterSelectedItem);
                    }
                }
            }

            String baseDir = savedInstanceState.getString(SIS_SCRIPT_EXECUTOR_ENGINE_BASE_DIR_FOR_PICK);
            if(baseDir != null) {
                mScriptExecutorForCallback = app.getEngine(new File(baseDir), true).getScriptExecutor();
            }

        }

        if(savedInstanceState!=null) {
            mScreen.restoreInstanceState(savedInstanceState);
        }

        // apply activity configuration
        Page mainPage = mScreen.getCurrentRootPage();
        if(mainPage != null && !mNewIntent) {
            configureActivity(mainPage);
        }
    }

    protected Screen createScreen() {
        return new DashboardScreen(this, R.layout.dashboard);
    }

    public Screen getScreen() {
        return mScreen;
    }

    public LightningEngine getEngine() {
        return mEngine;
    }

    protected void createActivity(Bundle savedInstanceState) {
        createDesktopTransitionAnimations();

        mViewAnimator=(ViewAnimator) findViewById(R.id.pager);

        mNavigationStack=new Stack<>();
        if(savedInstanceState!=null) {
    		mNavigationStack.addAll(savedInstanceState.getIntegerArrayList(SIS_NAVIGATION));
        	mAllocatedAppWidgetId = savedInstanceState.getInt(SIS_ALLOCATED_APP_WIDGET_ID);
        	if(savedInstanceState.containsKey(SIS_TMP_ITEM_ID)) {
	        	int item_id = savedInstanceState.getInt(SIS_TMP_ITEM_ID);
	        	mTmpItem = mEngine.getItemById(item_id);
        	}
            if(savedInstanceState.containsKey(SIS_MODIFYING_WIDGET)) {
                int item_id = savedInstanceState.getInt(SIS_MODIFYING_WIDGET);
                mModifyingWidget = mEngine.getItemById(item_id);
            }
            mTmpComponentName = savedInstanceState.getParcelable(SIS_TMP_COMPONENT_NAME);
            mActivityResultScriptId = savedInstanceState.getInt(SIS_ACTIVITY_RESULT_SCRIPT_ID);
            mActivityResultScriptToken = savedInstanceState.getString(SIS_ACTIVITY_RESULT_SCRIPT_TOKEN);
        }


		IntentFilter intent_filter=new IntentFilter();
		intent_filter.addAction(BROADCAST_ACTION_DISPLAY_PAGE);
		intent_filter.addAction(BROADCAST_ACTION_RELOAD);
        intent_filter.addAction(BROADCAST_ACTION_LOCKED);
        intent_filter.addAction(BROADCAST_ACTION_UNLOCKED);
		registerReceiver(mBroadcastReceiver, intent_filter);

        if(mEngine.shouldDoFirstTimeInit()) {
            mSetupInProgress = true;
			Setup.firstTimeInit(this);
		} else {
			int page=mEngine.readCurrentPage(mGlobalConfig.homeScreen);
			setPagerPage(page, Screen.PAGE_DIRECTION_HINT_AUTO);
		}
        Intent intent = getIntent();
        if(LauncherApps.ACTION_CONFIRM_PIN_SHORTCUT.equals(intent.getAction())) {
            addPinnedShortcut(intent);
        } else if(LauncherApps.ACTION_CONFIRM_PIN_APPWIDGET.equals(intent.getAction())) {
            addAppWidget(intent);
        }
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

        if(mEditMode) {
            leaveEditMode();
        }

        if(mEditPropertiesBox != null) {
            mEditPropertiesBox.end();
        }

        mHierarchyScreen.destroy();

		destroyActivity();

        mScreen.destroy();

        mUndoStack.clear();
	}

    protected void destroyActivity() {
        unregisterReceiver(mBroadcastReceiver);
    }

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		ArrayList<Integer> l=new ArrayList<>();
		l.addAll(mNavigationStack);
		outState.putIntegerArrayList(SIS_NAVIGATION, l);
		outState.putInt(SIS_ALLOCATED_APP_WIDGET_ID, mAllocatedAppWidgetId);
		if(mTmpItem!=null) outState.putInt(SIS_TMP_ITEM_ID, mTmpItem.getId());
		if(mModifyingWidget!=null) outState.putInt(SIS_MODIFYING_WIDGET, mModifyingWidget.getId());
        if(mTmpComponentName != null) outState.putParcelable(SIS_TMP_COMPONENT_NAME, mTmpComponentName);
        outState.putInt(SIS_ACTIVITY_RESULT_SCRIPT_ID, mActivityResultScriptId);
        if(mActivityResultScriptToken !=null) outState.putString(SIS_ACTIVITY_RESULT_SCRIPT_TOKEN, mActivityResultScriptToken);
        outState.putBoolean(SIS_EDIT_MODE, mEditMode);
        if(mEditMode) {
            Item masterSelectedItem = mEditItemLayout.getMasterSelectedItem();
            outState.putString(SIS_EDIT_ITEM_LAYOUT_PATH, new ContainerPath(mEditItemLayout).toString());
            outState.putString(SIS_EDIT_PAGE_ENGINE_DIR, mEditPage.getEngine().getBaseDir().getAbsolutePath());
            outState.putInt(SIS_EDIT_MASTER_SELECTED_ITEM_ID, masterSelectedItem == null ? Item.NO_ID : masterSelectedItem.getId());
            ArrayList<Integer> selected_items_ids = new ArrayList<>();
            ArrayList<ItemView> selectedItemViews = getSelectedItemViews();
            for (ItemView itemView : selectedItemViews) {
                selected_items_ids.add(itemView.getItem().getId());
            }
            outState.putIntegerArrayList(SIS_EDIT_SELECTED_ITEMS_IDS, selected_items_ids);


            boolean have_properties_box = mEditPropertiesBox != null;
            outState.putBoolean(SIS_HAVE_PROPERTIES_BOX, have_properties_box);
            if (have_properties_box) {
                outState.putBoolean(SIS_PROPERTIES_BOX_VISIBLE, mEditPropertiesContainer.getVisibility() == View.VISIBLE);
                mEditPropertiesBox.onSaveInstanceState(outState);
            }
        }
        if(mScriptExecutorForCallback != null) {
            outState.putString(SIS_SCRIPT_EXECUTOR_ENGINE_BASE_DIR_FOR_PICK, mScriptExecutorForCallback.getEngine().getBaseDir().getAbsolutePath());
        }

        mScreen.saveInstanceState(outState);
	}

    @Override
    public void startActivity(Intent intent) {
        mPausedBecauseOfLaunch = true;
        super.startActivity(intent);
        applyLaunchAnimation(mLastLaunchAnimation, true);
    }

    @Override
    protected void onStart() {
        super.onStart();
//        LLApp.get().appWidgetHostStartListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        LLApp app = LLApp.get();
//        app.appWidgetHostStopListening();
        app.saveSystemConfig();
    }

    @Override
	public void onResume() {
		super.onResume();

        if(mPausedBecauseOfLaunch || mNewIntent) {
            ItemConfig.LaunchAnimation launchAnimation;
            if(mPausedBecauseOfLaunch) {
                launchAnimation = mLastLaunchAnimation;
            } else {
                Page page = mScreen.getTargetOrTopmostItemLayout().getPage();
                launchAnimation = page == null ? null : page.config.defaultItemConfig.launchAnimation;
            }
            applyLaunchAnimation(launchAnimation, false);
            mPausedBecauseOfLaunch = false;
        }

        mEngine.cancelDelayedSaveData();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && getClass() == Dashboard.class && mSystemConfig.hotwords && mHotwordServiceClient != null) {
            mHotwordServiceClient.requestHotwordDetection(true);
        }

		if(mModifyingWidget != null) {
            // reload widget that may have been changed in another activity/process (out of the launcher control)
            mModifyingWidget.notifyChanged();
		}

		// a pause followed by a resume soon after, with a call to onNewIntent is considered as a home key press
		if(mNewIntent) {
			if((SystemClock.uptimeMillis()-mOnPauseDate)<500) {
				if(mEatNextHome) {
					mEatNextHome=false;
				} else {
                    Page mainPage = mScreen.getCurrentRootPage();
                    EventAction ea = mainPage.config.homeKey;
                    mScreen.runAction(mainPage.getEngine(), "K_HOME", ea.action==GlobalConfig.UNSET ? mGlobalConfig.homeKey : ea);
				}
			}
            mNewIntent=false;
        }

        mScreen.resume();

        updateLightningLiveWallpaperVisibility();
	}

    @Override
	public void onPause() {
		super.onPause();

        if(mDialog != null) {
            if(mDialog.isShowing()) {
                mDialog.dismiss();
            }
            mDialog = null;
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && getClass() == Dashboard.class && mSystemConfig.hotwords && mHotwordServiceClient != null) {
            mHotwordServiceClient.requestHotwordDetection(false);
        }

        mScreen.pause();


        setLightningLiveWallpaperVisibility(true, 200);

        mEngine.saveDataDelayed();

		// this is used to detect home key press by filtering sequences of onPause/onNewIntent/onResume.
		mOnPauseDate=SystemClock.uptimeMillis();
	}

	@Override
	protected void onNewIntent(Intent intent) {
        if(mSetupInProgress) {
            return;
        }

        if(LockScreen.sThis != null) {
            startActivity(new Intent(this, LockScreen.class));
            mEatNextHome = true;
            return;
        }

        if(LauncherApps.ACTION_CONFIRM_PIN_SHORTCUT.equals(intent.getAction())) {
            addPinnedShortcut(intent);
        } else if(LauncherApps.ACTION_CONFIRM_PIN_APPWIDGET.equals(intent.getAction())) {
            addAppWidget(intent);
        } else if(intent.hasExtra(LightningIntent.INTENT_EXTRA_LOAD_SCRIPT_FROM_PACKAGE)) {
            loadScriptFromPackage((Intent) intent.getParcelableExtra(LightningIntent.INTENT_EXTRA_LOAD_SCRIPT_FROM_PACKAGE), false);
        } else {
            mScreen.setTargetItemLayout(null);
            mEatNextHome = executeNewIntent(intent);
            mNewIntent=true;
        }
	}

    protected boolean executeNewIntent(Intent intent) {
        boolean handled = false;

        EventAction eventAction = Utils.decodeEventActionFromLightningIntent(intent);
        if(eventAction != null) {
            mScreen.runAction(mEngine, "SHORTCUT", eventAction);
            handled = true;
        } else {
            if(intent.hasExtra(LightningIntent.INTENT_EXTRA_PAGE)) {
                mScreen.executeGoToDesktopPositionIntent(intent);
                handled = true;
            }
        }
        return handled;
    }

    private void applyLaunchAnimation(ItemConfig.LaunchAnimation launchAnimation, boolean forLaunch) {
        if(launchAnimation != null && launchAnimation != ItemConfig.LaunchAnimation.SYSTEM) {
            int in, out;
            switch (launchAnimation) {
                case SLIDE_UP: if (forLaunch) { in = R.anim.slide_up; out = R.anim.fade_out; } else { in = R.anim.fade_in; out = R.anim.slide_up_back; } break;
                case SLIDE_DOWN: if (forLaunch) { in = R.anim.slide_down; out = R.anim.fade_out; } else { in = R.anim.fade_in; out = R.anim.slide_down_back; } break;
                case SLIDE_RIGHT: if (forLaunch) { in = R.anim.slide_left; out = R.anim.fade_out; } else { in = R.anim.fade_in; out = R.anim.slide_left_back; } break;
                case SLIDE_LEFT: if (forLaunch) { in = R.anim.slide_right; out = R.anim.fade_out; } else { in = R.anim.fade_in; out = R.anim.slide_right_back; } break;
                case SCALE_CENTER: if (forLaunch) { in = R.anim.scale_in; out = R.anim.fade_out; } else { in = R.anim.fade_in; out = R.anim.scale_out; } break;
                case FADE: in = android.R.anim.fade_in; out = android.R.anim.fade_out; break;
                case NONE: default: in = 0; out = 0; break;
            }
            overridePendingTransition(in, out);
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && getClass() == Dashboard.class) {
            if(mSystemConfig.hotwords) {
                if (mHotwordServiceClient == null) {
                    mHotwordServiceClient = new HotwordServiceClient(this);
                }
                mHotwordServiceClient.onAttachedToWindow();
                mHotwordServiceClient.requestHotwordDetection(true);
            }
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && getClass() == Dashboard.class) {
            if(mSystemConfig.hotwords) {
                mHotwordServiceClient.requestHotwordDetection(false);
                mHotwordServiceClient.onDetachedFromWindow();
            }
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        mScreen.setHasWindowFocus(hasFocus);
    }

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode < REQUEST_FROM_CUSTOMIZE_VIEW) {
            if(resultCode==RESULT_OK) {
                ItemLayout il = mScreen.getTargetOrTopmostItemLayout();
                Page page = il.getPage();
                float scale = il.getCurrentScale();
                Item newItem;
                AppWidgetProviderInfo appWidget;
                AppWidgetManager app_widget_manager;
                switch(requestCode) {
                case REQUEST_SELECT_APP_FOR_ADD:
                    if(data.hasExtra(Utils.INTENT_EXTRA_ITEM_ID)) {
                        int itemId=data.getIntExtra(Utils.INTENT_EXTRA_ITEM_ID, 0);
                        Page itemPage = page.getEngine().getOrLoadPage(Utils.getPageForItemId(itemId));
                        Item item = itemPage.findItemById(itemId);
                        newItem=Utils.copyShortcut(item, page, mScreen.getLastTouchedAddX(), mScreen.getLastTouchedAddY(), scale);
                    } else {
                        newItem=Utils.addAndroidShortcutFromIntent(this, data, page, mScreen.getLastTouchedAddX(), mScreen.getLastTouchedAddY(), scale);
                    }
                    mUndoStack.storePageAddItem(newItem);
                    editItem(il, newItem);
                    break;

                case REQUEST_SELECT_SHORTCUT_FOR_ADD1:
                    ComponentName cn = data.getComponent();
                    if(cn != null && cn.getClassName().endsWith(".activities.ShortcutsS")) {
                        Intent shortcut = PhoneUtils.createDesktopBookmarkShortcut(this, il, null, null, null);
                        newItem=Utils.addAndroidShortcutFromIntent(this, shortcut, page, mScreen.getLastTouchedAddX(), mScreen.getLastTouchedAddY(), scale);
                        mUndoStack.storePageAddItem(newItem);
                    } else {
                        startActivityForResult(data, REQUEST_SELECT_SHORTCUT_FOR_ADD2);
                    }
                    break;


                case REQUEST_SELECT_SHORTCUT_FOR_ADD2:
                    newItem=Utils.addAndroidShortcutFromIntent(this, data, page, mScreen.getLastTouchedAddX(), mScreen.getLastTouchedAddY(), scale);
                    mUndoStack.storePageAddItem(newItem);
                    editItem(il, newItem);
                    break;

                case REQUEST_SELECT_APP_WIDGET_FOR_ADD:
                    app_widget_manager = AppWidgetManager.getInstance(LLApp.get());
                    appWidget = app_widget_manager.getAppWidgetInfo(mAllocatedAppWidgetId);

                    if(appWidget != null) {
                        if (appWidget.configure != null) {
                            // Launch over to configure widget, if needed
                            Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
                            intent.setComponent(appWidget.configure);
                            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAllocatedAppWidgetId);

                            try {
                                startActivityForResult(intent, REQUEST_CREATE_APP_WIDGET);
                            } catch(Exception e) {
                                LLApp.get().getAppWidgetHost().deleteAppWidgetId(mAllocatedAppWidgetId);
                            }
                        } else {
                            // Otherwise just add it
                            if(mTmpItem==null) {
                                newItem=Utils.addAppWidget(page, il, mAllocatedAppWidgetId, mScreen.getLastTouchedAddX(), mScreen.getLastTouchedAddY(), scale);
                                mUndoStack.storePageAddItem(newItem);
                            } else {
                                newItem = Utils.replaceAppWidget(mTmpItem.getPage(), mAllocatedAppWidgetId, mTmpItem.getId());
                            }
                            if(newItem != null) {
                                editItem(il, newItem);
                            }
                        }
                    }
                    break;

                case REQUEST_CREATE_APP_WIDGET:
                    app_widget_manager = AppWidgetManager.getInstance(LLApp.get());
                    appWidget = app_widget_manager.getAppWidgetInfo(mAllocatedAppWidgetId);
                    if(appWidget != null) {
                        if(mTmpItem==null) {
                            newItem=Utils.addAppWidget(page, il, mAllocatedAppWidgetId, mScreen.getLastTouchedAddX(), mScreen.getLastTouchedAddY(), scale);
                            mUndoStack.storePageAddItem(newItem);
                        } else {
                            newItem = mTmpItem;
                            Utils.replaceAppWidget(page, mAllocatedAppWidgetId, mTmpItem.getId());
                        }
                        if(newItem != null) {
                            editItem(il, newItem);
                        }
                    }
                    break;

                case REQUEST_SELECT_APP_FOR_PICK:
                case REQUEST_SELECT_SHORTCUT_FOR_PICK2:
                    Intent intent;
                    if(requestCode == REQUEST_SELECT_APP_FOR_PICK) {
                        intent = data;
                    } else {
                        intent = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
                    }

                    mUndoStack.storeGroupStart();

                    // TODO : hackish, the target items can either be the selected item in edit mode
                    // or mTmpItem for instance when selecting another app when a shortcut is not
                    // valid anymore.
                    ArrayList<Item> items = getSelectedItems();
                    if(items.size() == 0 && mTmpItem != null) {
                        items.add(mTmpItem);
                    }

                    for (Item item : items) {
                        final Class<? extends Item> itemClass = item.getClass();
                        if (itemClass == Shortcut.class || itemClass == DynamicText.class) {
                            mUndoStack.storeItemState(item);
                            ((Shortcut) item).setIntent(intent);
                            page.setModified();
                        }
                    }
                    mUndoStack.storeGroupEnd();
                    break;

                case REQUEST_SELECT_SHORTCUT_FOR_PICK1:
                    startActivityForResult(data, REQUEST_SELECT_SHORTCUT_FOR_PICK2);
                    break;

                case REQUEST_SELECT_SCREEN_FOR_MOVE:
                case REQUEST_SELECT_SCREEN_FOR_COPY:
                    boolean copy = requestCode == REQUEST_SELECT_SCREEN_FOR_COPY;
                    int page_to = data.getIntExtra(API.SCREEN_PICKER_INTENT_EXTRA_SCREEN, Page.FIRST_DASHBOARD_PAGE);

                    ArrayList<ItemView> selectedItemViews = getSelectedItemViews();

                    setPagerPage(page_to, Screen.PAGE_DIRECTION_HINT_AUTO);
                    ItemLayout ilTo = mScreen.getCurrentRootItemLayout();

                    mUndoStack.storeGroupStart();
                    for (ItemView itemView : selectedItemViews) {
                        Item item = itemView.getItem();
                        int page_from = item.getPage().id;
                        if (page_from != page_to || copy) {
                            int item_id = item.getId();
                            if (copy) {
                                // clone the item first, then move it
                                item = Utils.cloneItem(item);
                                itemView = itemView.getParentItemLayout().getItemView(item);
                                item_id = item.getId();
                                mUndoStack.storePageAddItem(item);
                            }
                            saveInitialItemViewGeometry(itemView);
                            newItem = Utils.moveItem(item, ilTo.getPage(), Utils.POSITION_AUTO, Utils.POSITION_AUTO, 1, Item.NO_ID);
                            mUndoStack.storePageItemMove(ilTo.getItemView(newItem), item_id, mOriginalItemsGeometry.get(item.getId()));
                        }
                    }
                    mUndoStack.storeGroupEnd();

//                        TODO restore selection of newly created/moved items if (mGlobalConfig.autoEdit) enterEditModeAndSelectItem(new_id);
                    break;

                case REQUEST_SELECT_SCREEN_FOR_GOTO:
                    setPagerPage(data.getIntExtra(API.SCREEN_PICKER_INTENT_EXTRA_SCREEN, Page.FIRST_DASHBOARD_PAGE), Screen.PAGE_DIRECTION_HINT_AUTO);
                    break;

                case REQUEST_BIND_APPWIDGET:
                    ((Widget)mTmpItem).setAppWidgetId(mAllocatedAppWidgetId);
                    mTmpItem.notifyChanged();
                    break;

                case REQUEST_SELECT_SCRIPT_TO_LOAD:
                    loadScriptFromPackage(data, false);
                    break;

                case REQUEST_QUERY_SCRIPT_INFOS:
                    boolean ok = false;
                    int raw_res_id = data.getIntExtra("i", 0);
                    if(raw_res_id != 0) {
                        Context remote_context = null;
                        try {
                            remote_context = createPackageContext(mTmpComponentName.getPackageName(), 0);
                            Resources rsrc = remote_context.getResources();
                            String script_text = FileUtils.readInputStreamContent(rsrc.openRawResource(raw_res_id));
                            if(script_text != null) {
                                boolean execute_on_load = data.getBooleanExtra("e", false);
                                boolean delete_after_load = data.getBooleanExtra("d", false);
                                int flags = data.getIntExtra("f", 0);
                                String name = data.getStringExtra("n");
                                String path = data.getStringExtra("p");

                                if(name == null) {
                                    PackageManager pm = getPackageManager();
                                    name = pm.getActivityInfo(mTmpComponentName, 0).loadLabel(pm).toString();
                                }

                                ScriptManager sm = mEngine.getScriptManager();
                                Script script;
                                script = sm.getOrLoadScript(path, name);
                                if(script == null) {
                                    script = sm.createScriptForFile(name, ScriptManager.sanitizeRelativePath(path));
                                }
                                script.flags = flags;
                                script.setSourceText(script_text);
                                sm.saveScript(script);

                                if(execute_on_load) {
                                    mEngine.getScriptExecutor().runScript(mScreen, script.id, "APK", null);
                                }
                                if(delete_after_load) {
                                    sm.deleteScript(script);
                                }
                                ok = true;
                            }
                        } catch (NameNotFoundException e) {
                            // pass
                        }
                    }
                    Toast.makeText(this, ok ? R.string.ls_ok : R.string.ls_ko, Toast.LENGTH_SHORT).show();
                    break;

                case REQUEST_FOR_SCRIPT:
                    mScriptExecutorForCallback.runScriptActivityResult(mScreen, resultCode, data, mActivityResultScriptId, mActivityResultScriptToken);
                    mScriptExecutorForCallback = null;
                    mActivityResultScriptId = Script.NO_ID;
                    mActivityResultScriptToken = null;
                    break;

                case REQUEST_SCRIPT_PICK_IMAGE:
                    mScriptExecutorForCallback.setFileForPickImage(Utils.getTmpImageFile());
                    mScriptExecutorForCallback = null;
                    break;

                case REQUEST_SCRIPT_CROP_IMAGE:
                    Bitmap bitmap = Utils.loadBitmap(Utils.getTmpImageFile(), 0, 0, 0);
                    ImageBitmap image = new ImageBitmap(mScriptExecutorForCallback.getLightning(), bitmap);
                    mScriptExecutorForCallback.setImageForCropImage(image);
                    mScriptExecutorForCallback = null;
                    break;

                case REQUEST_PICK_CUSTOM_ICON:
                    mUndoStack.storeGroupStart();
                    for (Item item : getSelectedItems()) {
                        mUndoStack.storeItemState(item);
                        if(item instanceof Shortcut) {
                            Shortcut shortcut = (Shortcut) item;
                            File icon_file = shortcut.getCustomIconFile();
                            File tmp_image_file = Utils.getTmpImageFile();
                            boolean copied = false;
                            if (tmp_image_file.exists()) {
                                FileOutputStream fos = null;
                                try {
                                    Utils.copyFile(new byte[1024], tmp_image_file, icon_file);
                                    if (shortcut.getClass() == Folder.class) {
                                        ((Folder) shortcut).modifyFolderConfig().iconStyle = FolderConfig.FolderIconStyle.NORMAL;
                                        // hack, refresh the edit properties box if needed
                                        if (isEditPropertiesBoxVisible() && mEditPropertiesBox.getItem() == shortcut) {
                                            mEditPropertiesBox.updatePreferences();
                                        }
                                    }
                                    copied = true;
                                } catch (Throwable e) {
                                    Toast.makeText(this, R.string.item_settings_icon_copy_failed, Toast.LENGTH_SHORT).show();
                                } finally {
                                    if (fos != null) try {
                                        fos.close();
                                    } catch (Exception e) {
                                    }
                                }
                            }
                            if (!copied) {
                                icon_file.delete();
                            }
                            shortcut.notifyChanged();
                        }
                    }
                    mUndoStack.storeGroupEnd();
                    break;

                case REQUEST_EDIT_LAUNCH_ACTION:
                    items = getSelectedItems();
                    if(items.size() == 0 && mTmpItem != null) {
                        items.add(mTmpItem);
                    }
                    mUndoStack.storeGroupStart();
                    for (Item item : items) {
                        final Class<? extends Item> itemClass = item.getClass();
                        if (itemClass == Shortcut.class || itemClass == DynamicText.class) {
                            mUndoStack.storeItemState(item);

                            Shortcut shortcut = (Shortcut) item;
                            EventAction ea = EventActionSetup.getEventActionFromIntent(data);
                            if((ea.action == GlobalConfig.LAUNCH_APP || ea.action == GlobalConfig.LAUNCH_SHORTCUT) && ea.next == null) {
                                try {
                                    shortcut.setIntent(Intent.parseUri(ea.data, 0));
                                } catch (URISyntaxException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                Intent i = new Intent(this, Dashboard.class);
                                i.putExtra(LightningIntent.INTENT_EXTRA_EVENT_ACTION, JsonLoader.toJSONObject(ea, null).toString());
                                shortcut.setIntent(i);
                            }

                            page.setModified();
                        }
                    }
                    mUndoStack.storeGroupEnd();
                    break;
                }
            } else {
                mTmpItem = null;
                if(requestCode==REQUEST_SELECT_APP_WIDGET_FOR_ADD || requestCode==REQUEST_CREATE_APP_WIDGET || requestCode==REQUEST_BIND_APPWIDGET) {
                    LLApp.get().getAppWidgetHost().deleteAppWidgetId(mAllocatedAppWidgetId);
                } else if(requestCode==REQUEST_FOR_SCRIPT) {
                    mScriptExecutorForCallback.runScriptActivityResult(mScreen, resultCode, data, mActivityResultScriptId, mActivityResultScriptToken);
                    mScriptExecutorForCallback = null;
                    mActivityResultScriptId = Script.NO_ID;
                    mActivityResultScriptToken = null;
                } else if(requestCode==REQUEST_SCRIPT_PICK_IMAGE) {
                    mScriptExecutorForCallback.setFileForPickImage(null);
                    mScriptExecutorForCallback = null;
                } else if(requestCode==REQUEST_SCRIPT_CROP_IMAGE) {
                    mScriptExecutorForCallback.setImageForCropImage(null);
                    mScriptExecutorForCallback = null;
                }
            }
        } else {
            // relay activity results to the customize item view
            mEditPropertiesBox.myOnActivityResult(requestCode, resultCode, data);
        }
	}

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if(requestCode == REQUEST_PERMISSION_FONT_PICKER) {
            // should be transmitted to the CustomizeItemView instance
            if(grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, R.string.pr_f2, Toast.LENGTH_LONG).show();
            }
        } else if(requestCode == REQUEST_PERMISSION_BASE) {
             if(grantResults[0] == PackageManager.PERMISSION_DENIED) {
                 String msg;
                 if(permissions[0].equals(Error.MISSING_PERMISSION_READ_CALL_LOG.getPermission()) ||
                    permissions[0].equals(Error.MISSING_PERMISSION_READ_SMS.getPermission())) {
                     msg = getString(R.string.pr_rcl_rms);
                 } else {
                     msg = getString(R.string.pr_f);
                     for (String p : permissions) {
                         msg += "\n - " + p;
                     }
                     msg += "\n" + getString(R.string.pr_inst);
                 }
                 AlertDialog.Builder builder = new AlertDialog.Builder(this);
                 builder.setMessage(msg);
                 builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                     @Override
                     public void onClick(DialogInterface dialogInterface, int i) {
                         startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Version.PERMISSIONS_INFO)));
                     }
                 });
                 builder.setNegativeButton(android.R.string.cancel, null);
                 builder.show();
            }
        }
    }

    private void loadScriptFromPackage(Intent script_intent, boolean isPlugin) {
        mTmpComponentName = script_intent.getComponent();
        if(mTmpComponentName.getPackageName().equals(getPackageName())) {
            startActivity(Intent.createChooser(new Intent(Intent.ACTION_VIEW, isPlugin ? Version.BROWSE_PLUGINS_URI : Version.BROWSE_SCRIPTS_URI), null));
        } else {
            startActivityForResult(script_intent, REQUEST_QUERY_SCRIPT_INFOS);
        }
    }

    @Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

        if(mSetupInProgress) {
            return;
        }

        mScreen.onOrientationChanged();

        configureSystemBarsPadding(mScreen.getCurrentRootPage().config);

        closeBubble();
	}

	@Override
	public void onClick(View v) {
		boolean close_bubble=true;
		int index, old_index = 0, new_index = 0;

        int view_id = v.getId();
        Page page;

        String targetItemPackageName = null;
        Class<? extends Item> targetItemClass = null;

        ItemLayout targetItemLayout;
        Item targetItem;
        ItemView targetItemView;
        if(mEditMode) {
            targetItemLayout = mEditItemLayout;
            targetItemView = mEditItemLayout.getMasterSelectedItemView();
            targetItem = mEditItemLayout.getMasterSelectedItem();
        } else {
            targetItemLayout = mBubbleItemLayout;
            targetItemView = mBubbleItemView;
            targetItem = mBubbleItemView==null ? null : mBubbleItemView.getItem();
        }
        if(targetItem != null) {
            targetItemClass = targetItem.getClass();
            targetItemPackageName = Utils.getPackageNameForItem(targetItem);
        }

        switch(view_id) {
            case R.id.mi_app_details:
                startPackageDetails(targetItemPackageName);
                break;

            case R.id.mi_app_store:
                Utils.startAppStore(this, targetItemPackageName);
                break;

            case R.id.mi_kill:
                if (LLApp.get().isFreeVersion()) {
                    LLApp.get().showFeatureLockedDialog(this);
                } else {
                    if (!checkPermissions(new String[]{Manifest.permission.KILL_BACKGROUND_PROCESSES}, new int[]{R.string.pr_r9}, REQUEST_PERMISSION_BASE)) {
                        Toast.makeText(this, R.string.pr_f5, Toast.LENGTH_LONG).show();
                        break;
                    }
                    final ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
                    final ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                    am.getMemoryInfo(mi);
                    final long a = mi.availMem;
                    boolean killMe = false;
                    for (Item item : getActionItems()) {
                        final String packageNameForItem = Utils.getPackageNameForItem(item);
                        if(packageNameForItem != null) {
                            if(packageNameForItem.equals(LLApp.LLX_PKG_NAME)) {
                                killMe = true;
                            } else {
                                am.restartPackage(packageNameForItem);
                            }
                        }
                    }
                    if(killMe) {
                        Toast.makeText(this, "I'll be back!", Toast.LENGTH_SHORT).show();
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mScreen.restart();
                            }
                        }, 500);
                    } else {
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                am.getMemoryInfo(mi);
                                int freed = (int) ((mi.availMem - a) / 1048576L);
                                String msg = freed > 0 ? getString(R.string.killed_freed, freed) : getString(R.string.killed);
                                Toast.makeText(Dashboard.this, msg, Toast.LENGTH_SHORT).show();
                            }
                        }, 1000);
                    }
                }
                break;

            case R.id.mi_uninstall:
                startActivity(new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + targetItemPackageName)));
                break;

            case R.id.mi_cfp:
                Utils.convertFolderToPanel((Folder) targetItem);
                break;

            case R.id.mi_cpf:
                Utils.convertPanelToFolder((EmbeddedFolder) targetItem);
                break;

            case R.id.hint:
                int h = (Integer) v.findViewById(R.id.dsa).getTag();
                String wiki;
                switch (h) {
                    case SystemConfig.HINT_APP_DRAWER:
                        wiki = "app_drawer";
                        break;

                    case SystemConfig.HINT_FOLDER:
                        wiki = "folders";
                        break;

                    case SystemConfig.HINT_PANEL:
                        wiki = "panels";
                        break;

                    case SystemConfig.HINT_DESKTOP:
                        wiki = "concepts";
                        break;

                    default:
                        return;
                }
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Version.WIKI_PREFIX + wiki)));
                } catch (ActivityNotFoundException e) {
                }
                break;

            case R.id.mi_edit:
                if (targetItemClass == StopPoint.class || targetItemClass == PageIndicator.class) {
                    editItem(targetItemLayout, targetItem);
                } else {
                    close_bubble = false;
                    openBubble(BUBBLE_MODE_ITEM_EDIT, targetItemView);
                }
                break;

            case R.id.mi_edit_icon:
                menuActionPickCustomIcon(targetItemLayout, (Shortcut) targetItem);
                break;

            case R.id.mi_edit_label:
                menuActionEditLabel((Shortcut) targetItem);
                break;

            case R.id.mi_style_copy:
                menuActionCopyStyle();
                break;

            case R.id.mi_style_paste:
                menuActionPasteStyle();
                break;

            case R.id.mi_edit_more:
                editItem(targetItemLayout, targetItem);
                break;

            case R.id.mi_position:
                close_bubble = false;
                openBubble(BUBBLE_MODE_ITEM_POSITION, targetItemView);
                break;

            case R.id.mi_move_to_screen:
            case R.id.mi_copy_to_screen:
                Intent intent = new Intent(this, ScreenManager.class);
                intent.setAction(Intent.ACTION_PICK);
                startActivityForResult(intent, view_id == R.id.mi_move_to_screen ? REQUEST_SELECT_SCREEN_FOR_MOVE : REQUEST_SELECT_SCREEN_FOR_COPY);
                break;

            case R.id.mi_edit_launch_action:
                editShortcutLaunchAction((Shortcut)targetItem);
                break;

            case R.id.mi_pick_widget:
                selectAppWidgetForReplace(targetItem);
                break;

            case R.id.mi_lm:
                menuActionToggleOnGrid();
                break;

            case R.id.mi_pin:
                menuActionTogglePin();
                break;

            case R.id.mi_move_out_of_folder:
                mUndoStack.storeGroupStart();
                ArrayList<ItemView> itemViews = getActionItemViews();
                ItemLayout ilFrom = itemViews.get(0).getParentItemLayout();
                ItemView openerItemView = ilFrom.getOpenerItemView();
                ItemLayout ilTo = openerItemView.getParentItemLayout();
                if((openerItemView instanceof EmbeddedFolderView)) {
                    FolderView fv = mScreen.findFolderView(ilFrom, true);
                    mScreen.closeFolder(fv, true);
                    if (mEditMode) {
                        leaveEditMode();
                    }
                }
                for (ItemView itemView : itemViews) {
                    saveInitialItemViewGeometry(itemView);
                    Item item = itemView.getItem();
                    int old_id = item.getId();
                    Item newItem = Utils.moveItem(item, ilTo.getPage(), Utils.POSITION_AUTO, Utils.POSITION_AUTO, ilTo.getCurrentScale(), Item.NO_ID);
                    mUndoStack.storePageItemMove(ilTo.getItemView(newItem), old_id, mOriginalItemsGeometry.get(old_id));
                }
                mUndoStack.storeGroupEnd();
            break;

        case R.id.mi_actions:
            close_bubble=false;
            openBubble(BUBBLE_MODE_ITEM_ACTIONS, targetItemView);
            break;

		case R.id.mi_remove: menuActionConfirmRemoveItem(); break;

		case R.id.mi_geometry: menuActionToggleGeometryBox(); break;

//		case R.id.mi_share:
//			Shortcut s=(Shortcut)mDialogSelectedItem;
//			Intent share_intent=new Intent(android.content.Intent.ACTION_SEND);
//			share_intent.setType("text/plain");
//			share_intent.putExtra(android.content.Intent.EXTRA_SUBJECT, s.getLabel());
//			share_intent.putExtra(android.content.Intent.EXTRA_TEXT, s.getIntent().getComponent().getPackageName());
//			break;

		case R.id.mi_widget_options:
            mModifyingWidget = targetItem;
            ((Widget) targetItem).onConfigure(this);
			break;

        case R.id.mi_ef_edit_layout:
            if(mEditMode) {
                // assume the master selected item is an embedded folder since this menu item is enabled
                enterEditMode(((EmbeddedFolderView)mEditItemLayout.getMasterSelectedItemView()).getEmbeddedItemLayout(), null);
            } else {
                enterEditMode(mScreen.getTargetOrTopmostItemLayout(), null);
            }
            break;

//		case R.id.mi_folder_options:
//            PhoneUtils.startSettings(this, ((Folder) targetItem).getFolderPageId(), false);
//			break;

		case R.id.move_bottom:
        case R.id.move_down:
        case R.id.move_up:
        case R.id.move_top:
            ArrayList<Item> pageItems = mEditPage.items;
            ArrayList<Item> selectedItems = getSelectedItems();
            if(view_id == R.id.move_top || view_id == R.id.move_down) {
                Collections.reverse(selectedItems);
            }
            mUndoStack.storeGroupStart();
            for (Item item : selectedItems) {
                switch (view_id) {
                    case R.id.move_bottom:
                        old_index = pageItems.indexOf(item);
                        new_index = 0;
                        break;

                    case R.id.move_down:
                        old_index = index = pageItems.indexOf(item);
                        new_index = index > 0 ? index - 1 : index;
                        break;

                    case R.id.move_up:
                        old_index = index = pageItems.indexOf(item);
                        new_index = index < pageItems.size() - 1 ? index + 1 : index;
                        break;

                    case R.id.move_top:
                        old_index = pageItems.indexOf(item);
                        new_index = pageItems.size() - 1;
                        break;
                }
                if(old_index != new_index) {
                    mEditPage.setItemZIndex(item, new_index);
                    mUndoStack.storePageItemZOrder(mEditPage, item, old_index);
                }
            }
            mUndoStack.storeGroupEnd();
            break;

        case R.id.mi_dm_add:
            menuActionAddItem();
            break;

        case R.id.mi_i:
            close_bubble=false;
            openBubble(BUBBLE_MODE_ITEMS, mBubbleItemLayout, null);
            break;

        case R.id.mi_h:
            mHierarchyScreen.show(new ContainerPath(targetItemLayout));
        	break;

        case R.id.mi_if:
        	Toast.makeText(this, "Not implemented yet", Toast.LENGTH_SHORT).show();
        	break;

        case R.id.mi_ic:
        case R.id.mi_isa:
        case R.id.mi_isd:
            page = targetItemLayout.getPage();
            Comparator c = null;
            switch(view_id) {
                case R.id.mi_isa: c = Utils.sItemComparatorByNameAsc; break;
                case R.id.mi_isd: c = Utils.sItemComparatorByNameDesc; break;
            }
            if(c != null) Collections.sort(page.items, c);
            Utils.layoutItemsInTable(page.config, page.items, mScreen.isPortrait());
            page.notifyModified();
            break;

        case R.id.mi_android_settings:
            Intent settings=new Intent(android.provider.Settings.ACTION_SETTINGS);
            settings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            try { startActivity(settings); } catch(ActivityNotFoundException e) {}
            break;

        case R.id.mi_dm_customize:
            close_bubble=false;
            openBubble(BUBBLE_MODE_SETTINGS, mBubbleItemLayout, null);
            break;

        case R.id.mi_dmc_r:
            menuActionSettingsGlobal();
            break;

        case R.id.mi_dmc_c:
            menuActionSettingsContainer(false);
            break;

        case R.id.mi_l:
            close_bubble = !menuActionLockUnlock();
            break;

        case R.id.mi_dm_edit_layout:
            if(mEditMode && mEditItemLayout == mBubbleItemLayout) {
                leaveEditMode();
            } else {
                enterEditMode(mBubbleItemLayout, null);
            }
            break;

        case R.id.gb_e1:
        case R.id.gb_e2:
            mGeometryEdit = view_id;
            final Item masterSelectedItem = mEditItemLayout.getMasterSelectedItem();
            if(masterSelectedItem != null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(getGeometryEditTitle(mGeometryEdit));
                final EditText edit_text = new EditText(this);
                edit_text.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
                edit_text.setText(getGeometryEditText(masterSelectedItem, mGeometryEdit));
                edit_text.setSelection(edit_text.length());
                FrameLayout l = new FrameLayout(this);
                l.setPadding(10, 10, 10, 10);
                l.addView(edit_text);
                builder.setView(l);
                builder.setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                float value;
                                try {
                                    String text = edit_text.getText().toString();
                                    text = text.replace(',', '.');
                                    value = Float.parseFloat(text);
                                } catch(Exception e) {
                                    value = getGeometryEditValue(mEditItemLayout.getMasterSelectedItem(), mGeometryEdit);
                                }

                                boolean is_x = mGeometryEdit == R.id.gb_e1;
                                mUndoStack.storeGroupStart();
                                for (ItemView itemView : getSelectedItemViews()) {
                                    Item item = itemView.getItem();
                                    saveInitialItemViewGeometry(itemView);
                                    updateGeometryValue(item, mGeometryMode, is_x ? value : getGeometryEditValue(item, R.id.gb_e1), is_x ? getGeometryEditValue(item, R.id.gb_e2) : value, false);
                                    storeUndoForGeometryBoxChange(itemView);
                                }
                                mUndoStack.storeGroupEnd();
                                updateGeometryBox();
                                arg0.dismiss();
                            }
                        });
                builder.setNegativeButton(android.R.string.cancel, null);
                mDialog = builder.create();
                mDialog.show();
            }
            break;

        case R.id.gb_hm:
        case R.id.gb_hp:
        case R.id.gb_vm:
        case R.id.gb_vp:
            mGeometryBox.removeCallbacks(mGeometryRepeat);
            if(!mHasGeometryRepeat) {
                mUndoStack.storeGroupStart();
            }
            boolean masterOnGrid = mEditItemLayout.getMasterSelectedItem().getItemConfig().onGrid;
            for (ItemView itemView : getSelectedItemViews()) {
                Item item = itemView.getItem();
                if(item.getItemConfig().onGrid == masterOnGrid) {
                    if (!mHasGeometryRepeat) {
                        saveInitialItemViewGeometry(itemView);
                    }
                    incrementCurrentGeometryValue(item, view_id);
                    storeUndoForGeometryBoxChange(itemView);
                }
            }
            mUndoStack.storeGroupEnd();
            mHasGeometryRepeat = false;
            break;

        case R.id.gb_m:
            if(targetItemClass != StopPoint.class) {
                if(targetItem.getItemConfig().onGrid) {
                    mGeometryMode = mGeometryMode == Item.GEOMETRY_CTRL_SIZE ? Item.GEOMETRY_CTRL_POSITION : Item.GEOMETRY_CTRL_SIZE;
                } else {
                    mGeometryMode = (mGeometryMode == Item.GEOMETRY_CTRL_SKEW ? Item.GEOMETRY_CTRL_SIZE : mGeometryMode + 1);
                }
                updateGeometryBox();
            }
            break;

        case R.id.mi_s:
        	close_bubble=false;
            if(LLApp.get().isFreeVersion()) {
                LLApp.get().showFeatureLockedDialog(this);
            } else {
                if (targetItem != null) {
                    openBubble(BUBBLE_MODE_SCRIPTS, targetItemView);
                } else {
                    openBubble(BUBBLE_MODE_SCRIPTS, mBubbleItemLayout, null);
                }
            }
            break;

        case R.id.mi_ls:
            Intent ip=new Intent(Intent.ACTION_PICK_ACTIVITY);
            ip.putExtra(Intent.EXTRA_TITLE, getString(R.string.mi_ls));
            Intent filter=new Intent("net.pierrox.lightning_launcher.script.ENUMERATE");
            ip.putExtra(Intent.EXTRA_INTENT, filter);
            startActivityForResult(ip, REQUEST_SELECT_SCRIPT_TO_LOAD);
            break;

        case R.id.mi_nos:
            int msg_res;
            mNoScriptCounter++;
            switch(mNoScriptCounter) {
                case 1: msg_res = R.string.mi_nos1; break;
                case 2: msg_res = R.string.mi_nos2; break;
                case 3: msg_res = R.string.mi_nos3; break;
                case 4: msg_res = R.string.mi_nos4; break;
                default:msg_res = R.string.mi_nos5; break;
            }
            Toast toast =Toast.makeText(this, msg_res, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            if(mNoScriptCounter == 5) {
                ScriptManager sm = mEngine.getScriptManager();
                Script easter_egg = sm.createScriptForFile(getString(R.string.mi_nost), "/"+getPackageName().replace('.', '/'));
                easter_egg.setSourceText("LL.bindClass('android.view.animation.AccelerateDecelerateInterpolator');\n" +
                        "LL.bindClass('android.view.animation.AnimationUtils');\n" +
                        "\n" +
                        "var item = LL.getEvent().getItem();\n" +
                        "\n" +
                        "var properties = item.getProperties();\n" +
                        "var was_on_grid = properties.getBoolean('i.onGrid');\n" +
                        "var old_cell = item.getCell();\n" +
                        "var interpolator = new AccelerateDecelerateInterpolator();\n" +
                        "\n" +
                        "if(was_on_grid) {\n" +
                        "    properties.edit().setBoolean('i.onGrid', false).commit();\n" +
                        "}\n" +
                        "\n" +
                        "var start = AnimationUtils.currentAnimationTimeMillis();\n" +
                        "var duration = 2000;\n" +
                        "var turn = 10;\n" +
                        "\n" +
                        "rotate();\n" +
                        "\n" +
                        "function rotate() {\n" +
                        "    var now = AnimationUtils.currentAnimationTimeMillis();\n" +
                        "    var s = (now-start)/duration;\n" +
                        "    if(s > 1) s = 1;\n" +
                        "\n" +
                        "    item.setRotation(interpolator.getInterpolation(s)*360*turn);\n" +
                        "\n" +
                        "    if(s<1) {\n" +
                        "        setTimeout(rotate, 0);\n" +
                        "    } else {\n" +
                        "      if(was_on_grid) {\n" +
                        "        properties.edit().setBoolean('i.onGrid', true).commit();\n" +
                        "        item.setCell(old_cell.getLeft(), old_cell.getTop(), old_cell.getRight(), old_cell.getBottom());\n" +
                        "      }\n" +
                        "    }\n" +
                        "}\n");
                easter_egg.setFlag(Script.FLAG_ITEM_MENU, true);
                sm.saveScript(easter_egg);
            }
            break;

        case R.id.mi_sel:
            close_bubble=false;
            openBubble(BUBBLE_MODE_SELECT, mBubbleItemLayout, null);
            break;

        case R.id.mi_sa:
            for(Item item : mEditPage.items) {
                mEditItemLayout.getItemView(item).setSelected(true);
            }
            break;

        case R.id.mi_sn:
            for(Item item : mEditPage.items) {
                mEditItemLayout.getItemView(item).setSelected(false);
            }
            break;

        case R.id.mi_ss:
            RectF itemBounds = new RectF();
            RectF ilBounds = new RectF(0, 0, mEditItemLayout.getWidth(), mEditItemLayout.getHeight());
            mEditItemLayout.getLocalInverseTransform().mapRect(ilBounds);
            for(Item item : mEditPage.items) {
                ItemView itemView = mEditItemLayout.getItemView(item);
                Utils.getItemViewBoundsInItemLayout(itemView, itemBounds);
                itemView.setSelected(itemBounds.intersect(ilBounds));
            }
            break;

        case R.id.mi_si:
            for(Item item : mEditPage.items) {
                ItemView itemView = mEditItemLayout.getItemView(item);
                itemView.setSelected(!itemView.isSelected());
            }
            break;

        case R.id.mi_sr:
            // TODO
            break;

		default:
			Object tag = v.getTag();
			if(tag != null && tag.getClass() == Script.class) {
				final Script f_script = (Script)tag;
				final Item f_item = targetItem;
				final ItemLayout f_il = targetItemLayout;
                final String f_source;
                if(mBubbleMode == BUBBLE_MODE_CUSTOM_MENU) {
                    f_source = "MENU_CUSTOM";
                } else {
                    f_source = f_item==null ? "MENU_APP":"MENU_ITEM";
                }
				// avoid closing a menu which has been open by the script
				mHandler.post(new Runnable() {
					@Override
					public void run() {
                        Page p = f_il.getPage();
                        p.getEngine().getScriptExecutor().runScript(mScreen, f_script, f_source, null, f_il, f_item==null ? null : f_il.getItemView(f_item));
					}
				});
			} else {
                // not a known id, must be a dynamically added item with the tag object being an intent to launch
                try { startActivity((Intent)v.getTag()); } catch(Exception e) {}
			}
			break;
		}

		if(close_bubble) {
			closeBubble();
		}
	}

    private void menuActionPickCustomIcon(ItemLayout il, Shortcut shortcut) {
        enterEditMode(il, shortcut);
        ImagePicker.startActivity(this, REQUEST_PICK_CUSTOM_ICON);
    }

    private void menuActionEditLabel(final Shortcut shortcut) {
        Utils.createTextInputDialog(this, R.string.lh, shortcut.getLabel(), new Utils.OnTextInputDialogDone() {
            @Override
            public void onTextInputDone(String value) {
                mUndoStack.storeGroupStart();
                ArrayList<Item> selectedItems = getSelectedItems();
                if(!selectedItems.contains(shortcut)) {
                    selectedItems.add(shortcut);
                }
                for (Item item : selectedItems) {
                    if(item instanceof Shortcut) {
                        mUndoStack.storeItemState(item);
                        ((Shortcut)item).setLabel(value);
                        item.notifyChanged();
                    }
                }
                mUndoStack.storeGroupEnd();
            }
        }).show();
    }

    private void menuActionCopyStyle() {
        mCopyStyleFromItem = mEditItemLayout.getMasterSelectedItem();
        Toast.makeText(this, R.string.st_d, Toast.LENGTH_SHORT).show();
    }

    private void menuActionPasteStyle() {
        Item item_from = mCopyStyleFromItem;

        if(item_from == null) {
            return;
        }

        ArrayList<ItemView> itemViews = getActionItemViews();
        mUndoStack.storeGroupStart();
        for (ItemView item_view_to : itemViews) {

            Item item_to = item_view_to.getItem();

            if(item_to == item_from) {
                continue;
            }

            mUndoStack.storeItemState(item_to);

            int id_to = item_to.getId();
            Page page_to =item_to.getPage();
            if (item_to instanceof StopPoint) {
                if (item_from instanceof StopPoint) {
                    StopPoint from = (StopPoint) item_from;
                    StopPoint to = (StopPoint) item_to;
                    to.copyFrom(from);
                }
            } else {
                ItemConfig ic = new ItemConfig();
                ic.copyFrom(item_from.getItemConfig());
                ItemConfig ic_to = item_to.getItemConfig();
                boolean old_grid_mode = ic_to.onGrid;
                ItemConfig.PinMode old_pin_mode = ic_to.pinMode;
                ic.swipeLeft = ic_to.swipeLeft.clone();
                ic.swipeRight = ic_to.swipeRight.clone();
                ic.swipeUp = ic_to.swipeUp.clone();
                ic.swipeDown = ic_to.swipeDown.clone();
                ic.tap = ic_to.tap.clone();
                ic.longTap = ic_to.longTap.clone();
                ic.touch = ic_to.touch.clone();
                item_to.setItemConfig(ic);

                if (item_to instanceof Folder && item_from instanceof Folder) {
                    FolderConfig fc = new FolderConfig();
                    FolderConfig fc_from = ((Folder) item_from).getFolderConfig();
                    fc.copyFrom(fc_from);
                    ((Folder) item_to).setFolderConfig(fc);
                }

                // as of today, don't copy these attributes because they are not style related, but linked with the behavior
//                if(item_to instanceof DynamicText && item_from instanceof DynamicText) {
//                    DynamicTextConfig dtc=new DynamicTextConfig();
//                    dtc.copyFrom(((DynamicText)item_from).getDynamicTextConfig());
//                    ((DynamicText)item_to).setDynamicTextConfig(dtc);
//                }

                if (item_to instanceof ShortcutConfigStylable && item_from instanceof ShortcutConfigStylable) {
                    ShortcutConfigStylable from = (ShortcutConfigStylable) item_from;
                    ShortcutConfigStylable to = (ShortcutConfigStylable) item_to;

                    ShortcutConfig sc = from.getShortcutConfig().clone();
                    to.setShortcutConfig(sc);
                }

                if (item_to instanceof PageIndicator && item_from instanceof PageIndicator) {
                    ((PageIndicator) item_to).copyFrom((PageIndicator) item_from);
                }

                int id_from = item_from.getId();
                Page page_from = item_from.getPage();

                File icon_dir_from = page_from.getIconDir();
                File icon_dir_to = page_to.getAndCreateIconDir();
                icon_dir_to.mkdirs();
                FileUtils.copyIcons(null, icon_dir_from, String.valueOf(id_from), icon_dir_to, String.valueOf(id_to));
                if (item_to.getClass() != Folder.class) {
                    Box.getBoxBackgroundFolder(icon_dir_to, id_to).delete();
                }

                if (old_grid_mode != ic.onGrid) {
                    // revert and translate the position
                    ic.onGrid = old_grid_mode;
                    item_view_to.setOnGrid(!old_grid_mode);
                }

                ic = item_to.getItemConfig(); // might have changed because of setItemOnGrid above
                if (old_pin_mode != ic.pinMode) {
                    ItemConfig.PinMode new_pin_mode = ic.pinMode;
                    ic.pinMode = old_pin_mode;
                    setItemPinMode(item_view_to, new_pin_mode);
                }
            }
            item_to.notifyChanged();
        }
        mUndoStack.storeGroupEnd();
    }

    private void menuActionTogglePin() {
        ItemConfig.PinMode new_pin_mode = mEditItemLayout.getMasterSelectedItem().getItemConfig().pinMode == ItemConfig.PinMode.NONE ? ItemConfig.PinMode.XY : ItemConfig.PinMode.NONE;

        ArrayList<ItemView> itemViews = getSelectedItemViews();
        mUndoStack.storeGroupStart();
        for (ItemView itemView : itemViews) {
            Item item = itemView.getItem();
            if(!(item instanceof StopPoint)) {
                saveInitialItemViewGeometry(itemView);

                ItemConfig ic = item.getItemConfig();
                ItemConfig.PinMode prev_pin_mode = ic.pinMode;

                if (prev_pin_mode != new_pin_mode) {
                    setItemPinMode(itemView, new_pin_mode);

                    // hack, refresh the edit properties box if needed
                    if (isEditPropertiesBoxVisible() && mEditPropertiesBox.getItem() == item) {
                        mEditPropertiesBox.updatePreferences();
                    }

                    mUndoStack.storeItemPinMode(itemView, prev_pin_mode, mOriginalItemsGeometry.get(item.getId()));
                }
            }
        }
        mUndoStack.storeGroupEnd();
    }

    private void menuActionToggleOnGrid() {
        boolean new_on_grid = !mEditItemLayout.getMasterSelectedItem().getItemConfig().onGrid;
        ArrayList<ItemView> itemViews = getSelectedItemViews();
        mUndoStack.storeGroupStart();
        for (ItemView itemView : itemViews) {
            saveInitialItemViewGeometry(itemView);
            Item item = itemView.getItem();
            boolean was_on_grid = item.getItemConfig().onGrid;
            if(was_on_grid != new_on_grid) {
                itemView.setOnGrid(!was_on_grid);
                mUndoStack.storeItemGridAttachment(itemView, was_on_grid, mOriginalItemsGeometry.get(item.getId()));
            }
        }
        mUndoStack.storeGroupEnd();
        updateEditActionBox();
    }

    protected void menuActionConfirmRemoveItem() {
        ArrayList<Item> items = getActionItems();
        boolean widget = false;
        for (Item item : items) {
            if (item instanceof Folder) {
                widget = ((Folder) item).hasWidget();
                if(widget) {
                    break;
                }
            } else if (item instanceof Widget) {
                widget = true;
                break;
            }
        }

        if(widget) {
            showConfirmWidgetDeletionDialog(new Runnable() {
                @Override
                public void run() {
                    menuActionRemoveItem();
                }
            });
        } else {
            menuActionRemoveItem();
        }
    }

    protected void menuActionRemoveItem() {
        ArrayList<ItemView> itemViews = getActionItemViews();
        mUndoStack.storeGroupStart();
        for (ItemView itemView : itemViews) {
            itemView.setSelected(false);
            Item item = itemView.getItem();
            Page page = item.getPage();
            mUndoStack.storePageRemoveItem(item);
            page.removeItem(item, false);
        }
        mUndoStack.storeGroupEnd();
    }

    protected void menuActionAddItem() {
        try { removeDialog(DIALOG_ADD); } catch(Exception e) {}
        showDialog(DIALOG_ADD);
    }

    protected void menuActionEdit() {
        if(mBubbleItemView == null) {
            if(mEditMode) {
                leaveEditMode();
            } else {
                enterEditMode(mScreen.getTargetOrTopmostItemLayout(), null);
            }
        } else {
            Class<?> cls = mBubbleItemView.getClass();
            enterEditMode(mBubbleItemLayout, mBubbleItemView.getItem());
            if(cls == StopPoint.class || cls == Unlocker.class || cls == PageIndicator.class || cls == CustomView.class) {
                showEditPropertiesBox();
            } else {
                final ItemView finalItemView = mBubbleItemView;
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        openBubble(BUBBLE_MODE_ITEM_EDIT, finalItemView);
                    }
                }, ANIMATE_EDIT_BAR_DURATION+10);
            }
        }
    }


    private void menuActionCloneItem() {
        ItemView mainItemView = mEditMode ? mEditItemLayout.getMasterSelectedItemView() : mBubbleItemView;
        if(mainItemView == null) {
            // object deleted in the meantime
            return;
        }

        final ArrayList<Item> actionItems = getActionItems();
        Item mainItem = mainItemView.getItem();
        Item mainNewItem = null;
        Item[] newItems = new Item[actionItems.size()];
        int i = 0;
        mUndoStack.storeGroupStart();
        for (Item item : actionItems) {
            final Item newItem = Utils.cloneItem(item);
            mUndoStack.storePageAddItem(newItem);
            if(mainItem == item) {
                mainNewItem = newItem;
            }
            newItems[i++] = newItem;
        }
        mUndoStack.storeGroupEnd();

        enterEditMode(mEditMode?mEditItemLayout:mBubbleItemLayout, mainNewItem);
        if(mEditMode) {
            for (Item item : newItems) {
                ItemView itemView = mEditItemLayout.getItemView(item);
                itemView.setSelected(true);
                if(item == mainNewItem) {
                    configureHandlesForItemView(itemView, HandleView.Mode.CONTENT_SIZE, true);
                }
            }
        }
        final ItemView mainNewItemView = mEditItemLayout.getItemView(mainNewItem);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mScreen.ensureItemViewVisible(mainNewItemView, true);
            }
        });
    }

    private boolean menuActionToggleGeometryBox() {
        boolean visibility = isGeometryBoxVisible();
        if(visibility) {
            hideGeometryBox();
        } else {
            showGeometryBox();
        }
        return !visibility;
    }

    private void menuActionSettingsGlobal() {
        PhoneUtils.startSettings(this, new ContainerPath(mScreen.getTargetOrTopmostItemLayout()), true);
    }

    private void menuActionSettingsContainer(boolean use_edit_page) {
        ContainerPath path;
        ItemLayout il;
        ItemView masterSelectedItemView = mEditMode ? mEditItemLayout.getMasterSelectedItemView() : null;
        if(masterSelectedItemView != null && masterSelectedItemView.getItem() instanceof Folder) {
            path = new ContainerPath(masterSelectedItemView);
        } else {
            path = new ContainerPath(use_edit_page ? mEditItemLayout : mScreen.getTargetOrTopmostItemLayout());
        }
        PhoneUtils.startSettings(this, path, false);
    }

    private boolean menuActionLockUnlock() {
        boolean isOpeningBubble = false;
        if(mGlobalConfig.itemLongTap.action != GlobalConfig.NOTHING) {
            mGlobalConfig.itemLongTap.action = GlobalConfig.NOTHING;
            leaveEditMode();
            if((mSystemConfig.hints & SystemConfig.HINT_LOCKED)==0) {
                mScreen.setLastTouchEventForMenuBottom(false);
                openBubble(BUBBLE_MODE_HINT_LOCKED);
                isOpeningBubble = true;
            }
        } else {
            mGlobalConfig.itemLongTap.action = GlobalConfig.MOVE_ITEM;
        }
        mEngine.notifyGlobalConfigChanged();
        updateEditBars();
        return isOpeningBubble;
    }

    private boolean menuActionToggleMultiSelection() {
        boolean multi_selection = !mSystemConfig.hasSwitch(SystemConfig.SWITCH_MULTI_SELECTION);
        mSystemConfig.setSwitch(SystemConfig.SWITCH_MULTI_SELECTION, multi_selection);
        if(!multi_selection) {
            ItemView masterSelectedItemView = mEditItemLayout.getMasterSelectedItemView();
            for (ItemView itemView : getSelectedItemViews()) {
                if(itemView != masterSelectedItemView) {
                    itemView.setSelected(false);
                }
            }
        }
        return multi_selection;
    }

    private boolean menuActionToggleSnap() {
        boolean snap = !mSystemConfig.hasSwitch(SystemConfig.SWITCH_SNAP);
        mSystemConfig.setSwitch(SystemConfig.SWITCH_SNAP, snap);
        return snap;
    }

    private boolean menuActionToggleDisplayInvisibleItems() {
        boolean display_invisible_items = !mSystemConfig.hasSwitch(SystemConfig.SWITCH_DISPLAY_INVISIBLE_ITEMS);
        mSystemConfig.setSwitch(SystemConfig.SWITCH_DISPLAY_INVISIBLE_ITEMS, display_invisible_items);
        mEditItemLayout.setDisplayInvisibleItems(display_invisible_items);
        return display_invisible_items;
    }

    public void setItemPinMode(ItemView itemView, ItemConfig.PinMode new_pin_mode) {
        Item item = itemView.getItem();
        ItemConfig ic = new ItemConfig();
        ic.copyFrom(item.getItemConfig());
        item.setItemConfig(ic);
        ItemConfig.PinMode prev_pin_mode = ic.pinMode;
        Page page = item.getPage();
        ItemLayout itemLayout = itemView.getParentItemLayout();
        if(mSystemConfig.hasSwitch(SystemConfig.SWITCH_HONOUR_PINNED_ITEMS)) {
            Utils.convertPinnedItemPosition(itemLayout, item, prev_pin_mode, new_pin_mode);
        }
        ic.pinMode=new_pin_mode;
        page.setModified();
        itemLayout.invalidate();
        itemLayout.requestLayout();
        updateGeometryBox();
        updateEditActionBox();
    }

	@Override
	public boolean onMenuOpened(int featureId, Menu menu) {
//		mIsOptionsMenuOpened=true;
		return super.onMenuOpened(featureId, menu);
	}

	@Override
	public void onOptionsMenuClosed(Menu menu) {
//		mIsOptionsMenuOpened=false;
		mScreen.hideStatusBarIfNeeded();
	}

	@Override
    protected Dialog onCreateDialog(int id) {
        AlertDialog.Builder builder;

        switch (id) {
        case DIALOG_APP_NOT_INSTALLED:
            if(mNotValidShortcut != null) {
                builder=new AlertDialog.Builder(this);
                builder.setMessage(R.string.app_not_valid);
                final ComponentName cn=mNotValidShortcut.getIntent().getComponent();
                if(cn != null) {
                    builder.setPositiveButton(R.string.app_store, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent=new Intent(Intent.ACTION_VIEW, Uri.parse(Version.APP_STORE_INSTALL_PREFIX+cn.getPackageName()));
                            Utils.startActivitySafely(Dashboard.this, intent, R.string.start_activity_error);
                            mNotValidShortcut=null;
                        }
                    });
                }
                builder.setNeutralButton(R.string.mi_pick_app, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        replaceShortcutApp(mNotValidShortcut);
                        mNotValidShortcut=null;
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mNotValidShortcut=null;
                    }
                });
                return builder.create();
            }
            break;

        case DIALOG_FIRST_USE:
        	builder=new AlertDialog.Builder(this);
        	builder.setTitle(R.string.first_use_title);
        	builder.setMessage(LLApp.get().isTrialVersion() && BuildConfig.IS_TRIAL ? R.string.tr_w : R.string.first_use_message);
        	builder.setPositiveButton(android.R.string.ok, null);
        	builder.setCancelable(false);
        	return builder.create();

        case DIALOG_IMPORT_LL:
        	builder=new AlertDialog.Builder(this);
        	builder.setTitle(R.string.ll_import_title);
        	builder.setMessage(R.string.ll_import_message);
        	builder.setPositiveButton(android.R.string.ok, null);
        	builder.setCancelable(false);
        	return builder.create();

        case DIALOG_ADD:
            return new AddItemDialog(this, showPluginsInAddItemDialog(), new AddItemDialog.AddItemDialogInterface() {
                @Override
                public boolean isDialogAddItemEnabled(int id) {
                    return Dashboard.this.isDialogAddItemEnabled(id);
                }

                @Override
                public void onBuiltinItemClicked(int id) {
                    switch(id) {
                        case AddItemDialog.AI_APP: selectAppForAdd(); break;
                        case AddItemDialog.AI_SHORTCUT: selectShortcutForAddOrPick(null); break;
                        case AddItemDialog.AI_TEXT: addItemText(); break;
                        case AddItemDialog.AI_ICON: addItemIcon(); break;
                        case AddItemDialog.AI_DUMMY: addDummyShortcut(); break;
                        case AddItemDialog.AI_FOLDER: addFolder(); break;
                        case AddItemDialog.AI_PANEL: addEmbeddedFolder(); break;
                        case AddItemDialog.AI_SIDE_BAR: addSideBar(); break;
                        case AddItemDialog.AI_WIDGET: selectAppWidgetForAdd(); break;
//                        case AI_LLWIDGET: selectLLWidgetForAdd(); break;
                        case AddItemDialog.AI_BADGE: addBadge(); break;
                        case AddItemDialog.AI_DYNAMIC_TEXT: addDynamicText(); break;
                        case AddItemDialog.AI_PAGE_INDICATOR: addPageIndicator(); break;
                        case AddItemDialog.AI_UNLOCKER: addUnlocker(); break;
                        case AddItemDialog.AI_STOP_POINT: addStopPoint(); break;
                        case AddItemDialog.AI_BOOKMARK: addBookmark(); break;
                        case AddItemDialog.AI_LIGHTNING_ACTION: addLightningAction(); break;
                        case AddItemDialog.AI_CUSTOM_VIEW: addCustomView(); break;
                    }
                }

                @Override
                public void onPluginClicked(AddItemDialog.Plugin plugin) {
                    loadScriptFromPackage(plugin.intent, true);
                }

                @Override
                public void onPluginLongClicked(AddItemDialog.Plugin plugin) {
                    final Uri uri = Uri.parse(Version.APP_STORE_INSTALL_PREFIX + plugin.intent.getComponent().getPackageName());
                    startActivity(Intent.createChooser(new Intent(Intent.ACTION_VIEW, uri), null));
                }
            });

        case DIALOG_STOP_POINT:
            builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.tab_stop_point);
            builder.setMessage(R.string.sp_w);
            builder.setPositiveButton(android.R.string.ok, null);
            return builder.create();

        case DIALOG_LAUNCHER_APPS_NO_HOST_PERMISSION:
            builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.as_no_hp_t);
            builder.setMessage(R.string.as_no_hp_m);
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    PhoneUtils.selectLauncher(Dashboard.this, true);
                }
            });
            builder.setNegativeButton(android.R.string.cancel, null);
            return builder.create();

        case DIALOG_WRAP:
            builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.dialog_help_hint_t);
            builder.setMessage(R.string.h_w);
            final CheckBox dsa = new CheckBox(this);
            dsa.setText(R.string.dialog_help_hint_dsa);
            builder.setView(dsa);
            builder.setPositiveButton(android.R.string.ok,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if(dsa.isChecked()) {
                                mSystemConfig.hints |= SystemConfig.HINT_WRAP;
                            }
                        }
                    });
            return builder.create();
        }

        return super.onCreateDialog(id);
	}

    private void showConfirmWidgetDeletionDialog(final Runnable action_ok) {
        if((mSystemConfig.hints & SystemConfig.HINT_WIDGET_DELETION) != 0) {
            action_ok.run();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.mi_remove);
            builder.setMessage(R.string.cdi);
            final CheckBox dsa = new CheckBox(this);
            dsa.setText(R.string.dialog_help_hint_dsa);
            builder.setView(dsa);
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(dsa.isChecked()) {
                        mSystemConfig.hints |= SystemConfig.HINT_WIDGET_DELETION;
                    }
                    action_ok.run();
                }
            });
            builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(dsa.isChecked()) {
                        mSystemConfig.hints |= SystemConfig.HINT_WIDGET_DELETION;
                    }
                }
            });
            builder.create().show();
        }
    }

    protected boolean isDialogAddItemEnabled(int id) {
        return true;
    }

    protected boolean showPluginsInAddItemDialog() {
        return true;
    }

    @Override
    public boolean onSearchRequested() {
        Page mainPage = mScreen.getCurrentRootPage();
        EventAction ea = mainPage.config.searchKey;
    	return mScreen.runAction(mainPage.getEngine(), "K_SEARCH", ea.action == GlobalConfig.UNSET ? mGlobalConfig.searchKey : ea);
    }

    /**
     * @return true if desktop changed
     */
    protected boolean goBack() {
        FolderView fv = mScreen.findTopmostFolderView();
        if (fv != null) {
            mScreen.closeFolder(fv, true);
            return true;
        } else {
            if (mNavigationStack.empty()) {
                zoomToOriginOrBubbleInContainer(mScreen.getTargetOrTopmostItemLayout());

                return false;
            } else {
                int page = mNavigationStack.pop();
                setPagerPage(page, Screen.PAGE_DIRECTION_HINT_AUTO);

                // displayWorkspacePage store the new page in the stack, so remove it again (hacky)
                if (!mNavigationStack.empty()) {
                    mNavigationStack.pop();
                }

                return true;
            }
        }
    }

    private void zoomToOriginOrBubbleInContainer(ItemLayout il) {
    	if(!mScreen.zoomToOrigin(il) && il.getPage().isFolder()) {
    		ItemView opener = il.getOpenerItemView();
    		if(opener != null) {
                ItemLayout parentIl = opener.getParentItemLayout();
    			if(parentIl != null) {
                    zoomToOriginOrBubbleInContainer(parentIl);
                }
    		}
    	}
    }

    private static boolean MANUAL_LONG_KEY_PRESS_HANDLING =  Build.VERSION.SDK_INT >= 24;
    private int mLongPressKeyCode;
    private boolean mHasLongKeyPress;
    private Runnable mLongKeyPressRunnable = !MANUAL_LONG_KEY_PRESS_HANDLING ? null : new Runnable() {
        @Override
        public void run() {
            mHasLongKeyPress = true;
            handleLongKeyPress(mLongPressKeyCode);
        }
    };

    private void startLongKeyPressTimer(int keyCode) {
        if(MANUAL_LONG_KEY_PRESS_HANDLING) {
            // only one key managed at a time
            mLongPressKeyCode = keyCode;
            mHandler.removeCallbacks(mLongKeyPressRunnable);
            mHandler.postDelayed(mLongKeyPressRunnable, ViewConfiguration.getLongPressTimeout());
        }
    }

    private void cancelLongKeyPressTimer() {
        if(MANUAL_LONG_KEY_PRESS_HANDLING) {
            mHandler.removeCallbacks(mLongKeyPressRunnable);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
        if(!mSetupInProgress) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                event.startTracking();
                startLongKeyPressTimer(keyCode);
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_MENU) {
                mScreen.setLastTouchEventForMenuBottom(false);
                startLongKeyPressTimer(keyCode);
                event.startTracking();
                return true;
            } else if (event.isPrintingKey()) {
                char c = (char) event.getUnicodeChar();
                startSearch(String.valueOf(c), false, null, true);
            }
        }

        return super.onKeyDown(keyCode, event);
    }

	@Override
    public boolean onKeyUp(int keyCode, KeyEvent event)  {
        if(mSetupInProgress) {
            return false;
        }

        if(!mHasLongKeyPress) {
            cancelLongKeyPressTimer();
            if (event.isTracking() && !event.isCanceled()) {
                Page mainPage = mScreen.getCurrentRootPage();
                PageConfig c = mainPage.config;
                if (keyCode == KeyEvent.KEYCODE_MENU) {
                    EventAction ea = c.menuKey;
                    mScreen.runAction(mainPage.getEngine(), "K_MENU", ea.action == GlobalConfig.UNSET ? mGlobalConfig.menuKey : ea);
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_BACK) {
                    if (mHierarchyScreen.isShown()) {
                        mHierarchyScreen.hide();
                    } else if (!closeBubble()) {
                        if (mEditMode) {
                            leaveEditMode();
                        } else {
                            mScreen.setForceDisplayStatusBar(false);
                            EventAction ea = c.backKey;
                            mScreen.runAction(mainPage.getEngine(), "K_BACK", ea.action == GlobalConfig.UNSET ? mGlobalConfig.backKey : ea);
                        }
                    }
                    return true;
                }
            }
        } else {
            mHasLongKeyPress = false;
        }
		return super.onKeyUp(keyCode, event);
	}

    private boolean handleLongKeyPress(int keyCode) {
        if(mSetupInProgress) {
            return false;
        }
        Page mainPage = mScreen.getCurrentRootPage();
        if(keyCode==KeyEvent.KEYCODE_MENU) {
            EventAction ea = mainPage.config.longMenuKey;
            mScreen.runAction(mainPage.getEngine(), "K_MENU_L", ea.action == GlobalConfig.UNSET ? mGlobalConfig.longMenuKey : ea);
            return true;
        } else if(keyCode==KeyEvent.KEYCODE_BACK) {
            EventAction ea = mainPage.config.longBackKey;
            mScreen.runAction(mainPage.getEngine(), "K_BACK_L", ea.action == GlobalConfig.UNSET ? mGlobalConfig.longBackKey : ea);
            return true;
        }
        return false;
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        return handleLongKeyPress(keyCode) || super.onKeyLongPress(keyCode, event);
    }

    @Override
	public boolean onLongClick(View v) {
        int view_id = v.getId();
        switch (view_id) {
            case R.id.gb_hm:
            case R.id.gb_hp:
            case R.id.gb_vm:
            case R.id.gb_vp:
                mUndoStack.storeGroupStart();
                boolean masterOnGrid = mEditItemLayout.getMasterSelectedItem().getItemConfig().onGrid;
                for (ItemView itemView : getSelectedItemViews()) {
                    if(itemView.getItem().getItemConfig().onGrid == masterOnGrid) {
                        saveInitialItemViewGeometry(itemView);
                    }
                }
                mHasGeometryRepeat = true;
                mGeometryEdit = view_id;
                mGeometryRepeat.run();
                return false;

            case R.id.gb_m:
                mGeometryMode = (mGeometryMode == Item.GEOMETRY_CTRL_SIZE ? Item.GEOMETRY_CTRL_SKEW : mGeometryMode-1);
                updateGeometryBox();
                return true;

            default:
                return false;
        }
	}

	private void createDesktopTransitionAnimations() {
		AlphaAnimation aa_in=new AlphaAnimation(0, 1);
		aa_in.setDuration(400);
		AlphaAnimation aa_out=new AlphaAnimation(1, 0);
		aa_out.setDuration(400);


        mFadeInAnim=new AlphaAnimation(0, 1);
        mFadeInAnim.setDuration(400);
        mFadeOutAnim=new AlphaAnimation(1, 0);
        mFadeOutAnim.setDuration(400);

        mSlideHLeftInAnim=generateSlide(true, -1f, 0, 0, 0);
        mSlideHRightOutAnim=generateSlide(false, 0, 1f, 0, 0);

        mSlideHRightInAnim=generateSlide(true, 1f, 0, 0, 0);
        mSlideHLeftOutAnim=generateSlide(false, 0, -1f, 0, 0);

        mSlideVUpInAnim=generateSlide(true, 0, 0, -1f, 0);
        mSlideVDownOutAnim=generateSlide(false, 0, 0, 0, 1f);

        mSlideVDownInAnim=generateSlide(true, 0, 0, 1f, 0);
        mSlideVUpOutAnim=generateSlide(false, 0, 0, 0, -1f);
	}

	private AnimationSet generateSlide(boolean in, float fx, float tx, float fy, float ty) {
		AnimationSet as=new AnimationSet(true);
        //as.addAnimation(in ? mFadeInAnim : mFadeOutAnim);
        TranslateAnimation t = new TranslateAnimation(Animation.RELATIVE_TO_SELF, fx, Animation.RELATIVE_TO_SELF, tx, Animation.RELATIVE_TO_SELF, fy, Animation.RELATIVE_TO_SELF, ty);
        t.setDuration(400);
        as.addAnimation(t);
        return as;
	}

    protected ItemLayout setPagerPage(int pageId, int direction_hint) {
        Page currentPage = mScreen.getCurrentRootPage();
        int currentPageId = currentPage==null ? Page.NONE : currentPage.id;
        if(currentPageId == pageId) {
            return mScreen.getCurrentRootItemLayout();
        }

        if(direction_hint != Screen.PAGE_DIRECTION_HINT_DONT_MOVE) {
            if(currentPageId != Page.NONE) {
                if(!mScreen.isPaused()) {
                    mScreen.getCurrentRootItemLayout().pause();
                }
                mNavigationStack.push(currentPageId);
                if(mEditMode) {
                    leaveEditMode();
                }
                closeBubble();
                mScreen.closeAllFolders(true);
            }

            int direction;
            switch (direction_hint) {
                case Screen.PAGE_DIRECTION_HINT_BACKWARD:
                    direction = PAGER_ANIMATION_BACKWARD;
                    break;
                case Screen.PAGE_DIRECTION_HINT_FORWARD:
                    direction = PAGER_ANIMATION_FORWARD;
                    break;
                case Screen.PAGE_DIRECTION_HINT_NO_ANIMATION:
                    direction = PAGER_ANIMATION_NONE;
                    break;
                case Screen.PAGE_DIRECTION_HINT_AUTO:
                default:
                    int ci = mGlobalConfig.getPageIndex(currentPageId);
                    int ni = mGlobalConfig.getPageIndex(pageId);
                    if (mGlobalConfig.screensOrder.length == 2) {
                        direction = ni > ci ? PAGER_ANIMATION_FORWARD : PAGER_ANIMATION_BACKWARD;
                    } else {
                        if (ci == mGlobalConfig.screensOrder.length - 1 && ni == 0) {
                            direction = PAGER_ANIMATION_FORWARD;
                        } else if (ni == mGlobalConfig.screensOrder.length - 1 && ci == 0) {
                            direction = PAGER_ANIMATION_BACKWARD;
                        } else {
                            direction = ni > ci ? PAGER_ANIMATION_FORWARD : PAGER_ANIMATION_BACKWARD;
                        }
                    }
                    break;
            }
            configureViewAnimator(direction);
        }

        LightningEngine.PageManager pm = mEngine.getPageManager();
        Page page = pm.getOrLoadPage(pageId);

        ItemLayout itemLayout = null;
        for(int i=mViewAnimator.getChildCount()-1; i>=0; i--) {
            ItemLayout il = (ItemLayout) mViewAnimator.getChildAt(i);
            if(il.getPage().id == pageId) {
                itemLayout = il;
                break;
            }
        }
        if(itemLayout == null) {
            ItemLayout il = new ItemLayout(this, null);
            mScreen.takeItemLayoutOwnership(il);
            mViewAnimator.addView(il, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            il.setPage(page);
            page.notifyLoaded(il);
            itemLayout = il;
        }

        if(direction_hint != Screen.PAGE_DIRECTION_HINT_DONT_MOVE) {
            int i = mViewAnimator.indexOfChild(itemLayout);
            if (i != mViewAnimator.getDisplayedChild()) {
                mViewAnimator.setDisplayedChild(i);
            }

            configureActivity(page);

            LLApp.get().setActiveDashboardPage(pageId);


            if (!mScreen.isPaused()) {
                mScreen.getCurrentRootItemLayout().resume();
            }

            mEngine.writeCurrentPage(pageId);
        }

        if(mGlobalConfig.reAddScreenIfNeeded(pageId)) {
            mEngine.notifyGlobalConfigChanged();
        }

        mScreen.setTargetItemLayout(itemLayout);

        updateLightningLiveWallpaperVisibility();

        return itemLayout;
    }

    private void updateLightningLiveWallpaperVisibility() {
        if(this.getClass() == Dashboard.class) {
            Page page = mScreen.getCurrentRootPage();
            if(page != null) {
                boolean visible = page.id == Page.NONE || page.id != page.getEngine().getGlobalConfig().lwpScreen;
                setLightningLiveWallpaperVisibility(visible, 0);
            }
        }
    }

    private void setLightningLiveWallpaperVisibility(final boolean visible, int delay) {
        if(this.getClass() == Dashboard.class) {
            if(mSetLiveWallpaperVisibility != null) {
                mHandler.removeCallbacks(mSetLiveWallpaperVisibility);
                mSetLiveWallpaperVisibility = null;
            }
            final Screen lwp = LLApp.get().getScreen(ScreenIdentity.LIVE_WALLPAPER);
            if(lwp != null) {
                if(delay != 0) {
                    mSetLiveWallpaperVisibility = new Runnable() {
                        @Override
                        public void run() {
                            lwp.setVisibility(visible);
                        }
                    };
                    mHandler.postDelayed(mSetLiveWallpaperVisibility, delay);
                } else {
                    lwp.setVisibility(visible);
                }
            }
        }
    }

    private Runnable mSetLiveWallpaperVisibility;

    @SuppressWarnings("deprecation")
    protected void configureActivity(Page page) {
        PageConfig c = page.config;
        Window w=getWindow();

        int width, height;

        Display display=getWindowManager().getDefaultDisplay();

        Class<?> display_class = Display.class;
        try {
            DisplayMetrics metrics = new DisplayMetrics();
            Method getRealMetrics = display_class.getMethod("getRealMetrics", DisplayMetrics.class);
            getRealMetrics.invoke(display, metrics);
            width = metrics.widthPixels;
            height = metrics.heightPixels;
        } catch (Exception e1) {
            // getRealMetrics starts at API 17
            width=display.getWidth();
            height=display.getHeight();
        }

        if(c.bgSystemWPWidth==0 || c.bgSystemWPHeight==0) {
            if(width>height) { int tmp=width; width=height; height=tmp; }
            width=height;
        } else {
            width=c.bgSystemWPWidth;
            height=c.bgSystemWPHeight;
        }
        mWallpaperManager.suggestDesiredDimensions(width, height);

        mScreen.configureStatusBarVisibility(c);

        int o;
        switch(c.screenOrientation) {
            case PORTRAIT: o = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT; break;
            case LANDSCAPE: o = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE; break;
            case SYSTEM: o=ActivityInfo.SCREEN_ORIENTATION_USER; break;
            default: o=ActivityInfo.SCREEN_ORIENTATION_SENSOR; break;
        }
        setRequestedOrientation(o);

        w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        mScreen.configureBackground(page);

        mScreen.configureSystemBarsColor(c);
        configureSystemBarsPadding(c);
    }

    private void configureSystemBarsPadding(PageConfig c) {
        if(Build.VERSION.SDK_INT>=19) {
            SystemBarTintManager.SystemBarConfig config = mScreen.getSystemBarTintManager().getConfig();
            int sbh = c.statusBarHide ? 0 : config.getStatusBarHeight();
            int abh = getActionBarHeight();
            int padding_top = c.statusBarOverlap ? 0 : sbh + abh;
            int padding_right = 0;
            int padding_bottom = 0;
            int nbh = config.getNavigationBarHeight();
            boolean navigationAtBottom = config.isNavigationAtBottom();
            if (!c.navigationBarOverlap) {
                if (navigationAtBottom) {
                    padding_bottom = nbh;
                } else {
                    padding_right = nbh;
                }
            }
            if(mIsAndroidActionBarDisplayed) {
                try {
                    View v = getWindow().getDecorView();
                    int resId = getResources().getIdentifier("action_bar_container", "id", "android");
                    ViewGroup v1 = v.findViewById(resId);
                    ((View)v1.getParent()).setPadding(0, sbh, 0, 0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                padding_top = 0;
                sbh = 0;
            }
            findViewById(R.id.sb_padding).setPadding(0, padding_top, padding_right, padding_bottom);
            mEditControlsView.setPadding(0, sbh + abh, navigationAtBottom ? 0 : nbh, navigationAtBottom ? nbh : 0);
        } else {
            int abh = getActionBarHeight();
            findViewById(R.id.sb_padding).setPadding(0, abh, 0, 0);
            mEditControlsView.setPadding(0, abh, 0, 0);
        }
    }

    protected int getActionBarHeight() {
        return 0;
    }

    private static final int PAGER_ANIMATION_NONE = 0;
    private static final int PAGER_ANIMATION_BACKWARD = 1;
    private static final int PAGER_ANIMATION_FORWARD = 2;
    private void configureViewAnimator(int direction) {
		Animation in, out;
        if(direction == PAGER_ANIMATION_NONE) {
            in = null;
            out = null;
        } else {
            boolean forward = direction == PAGER_ANIMATION_FORWARD;
            switch(mGlobalConfig.pageAnimation) {
            case FADE: in=mFadeInAnim; out=mFadeOutAnim; break;
            case SLIDE_H:
                if(forward) { in=mSlideHRightInAnim; out=mSlideHLeftOutAnim; }
                else { in=mSlideHLeftInAnim; out=mSlideHRightOutAnim; }
                break;
            case SLIDE_V:
                if(forward) { in=mSlideVUpInAnim; out=mSlideVDownOutAnim; }
                else { in=mSlideVDownInAnim; out=mSlideVUpOutAnim; }
                break;
            default: return;
            }
		}
		mViewAnimator.setInAnimation(in);
		mViewAnimator.setOutAnimation(out);
	}

	private void editItem(ItemLayout il, Item item) {
        if(mSystemConfig.autoEdit) {
            enterEditMode(il, item);
            if (!mEditBarsVisible) {
                showEditBars(false);
            }
        } else {
            if(mEditMode) {
                mEditItemLayout.setMasterSelectedItem(item);
                ItemView itemView = mEditItemLayout.getItemView(item);
                if (itemView != null) {
                    itemView.setSelected(true);
                    configureHandlesForItemView(itemView, HandleView.Mode.CONTENT_SIZE, true);
                }
            }
        }
        if(mEditMode) {
            showEditPropertiesBox();
            updateEditPropertiesBox(item.getPage(), item);
            updateEditBars();
        }
	}

    @Override
    public void openOptionsMenu() {
        openBubble(mEditMode ? BUBBLE_MODE_LIGHTNING_MENU_EM : BUBBLE_MODE_LIGHTNING_MENU_NO_EM);
    }

	private void gotoPage(int direction) {
        long t1 = BuildConfig.DEBUG ? SystemClock.uptimeMillis() : 0;
		int p = mScreen.getNextPage(mGlobalConfig, direction);
		setPagerPage(p, direction);
        if(BuildConfig.DEBUG) {
            Log.i("LL", "gotoPage " + (SystemClock.uptimeMillis() - t1)+"ms");
        }
	}

//	@SuppressLint("WorldReadableFiles")
//	private void shareScreenshot() {
//		mContentView.setDrawingCacheEnabled(true);
//		try {
//			FileOutputStream out=openFileOutput("screenshot.png", MODE_WORLD_READABLE);
//			mContentView.getDrawingCache().compress(CompressFormat.PNG, 100, out);
//			Intent intent = new Intent(Intent.ACTION_SEND);
//			intent.putExtra(Intent.EXTRA_SUBJECT, "Some Subject Line");
//			intent.putExtra(Intent.EXTRA_TEXT, "Body of the message, woot!");
//			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
//			intent.setType("image/png");
//		    Uri uri = Uri.parse(FileProvider.CONTENT_URI + FileProvider.SCREENSHOT_FILE);
//		    intent.putExtra(Intent.EXTRA_STREAM, uri);
//		    startActivity(Intent.createChooser(intent, "How do you want to share?"));
//		} catch(IOException e) {
//			e.printStackTrace();
//		}
//	}



    //	private int getPageWithRotation(int page) {
//		int p;
//		File f;
//		switch (mDisplayRotation) {
//		case Surface.ROTATION_90:
//		case Surface.ROTATION_180:
//			p=Utils.getPageForRotation(page, mDisplayRotation);
//			f=Utils.getWorkspaceConfigFile(this, p);
//			if(f.exists()) {
//				return p;
//			}
//			break;
//
//		case Surface.ROTATION_270:
//			p=Utils.getPageForRotation(page, mDisplayRotation);
//			f=Utils.getWorkspaceConfigFile(this, p);
//			if(f.exists()) {
//				return p;
//			}
//			p=Utils.getPageForRotation(page, Surface.ROTATION_90);
//			f=Utils.getWorkspaceConfigFile(this, p);
//			if(f.exists()) {
//				return p;
//			}
//		}
//
//		return Utils.getPageForRotation(page, Surface.ROTATION_0);
//	}

    protected void replaceShortcutApp(Item item) {
        mTmpItem=item;
        Intent picker = new Intent(this, AppDrawerX.class);
        picker.setAction(Intent.ACTION_PICK_ACTIVITY);
        startActivityForResult(picker, REQUEST_SELECT_APP_FOR_PICK);
    }

	private void selectAppForAdd() {
		Intent intent=new Intent(this, AppDrawerX.class);
        intent.putExtra(AppDrawerX.INTENT_EXTRA_SELECT_FOR_ADD, true);
        startActivityForResult(intent, REQUEST_SELECT_APP_FOR_ADD);
	}

    // item==null: add, item!=null: replace
    private void selectShortcutForAddOrPick(Item item) {
        mTmpItem=item;
        Intent i=new Intent(Intent.ACTION_PICK_ACTIVITY);
        i.putExtra(Intent.EXTRA_INTENT, new Intent(Intent.ACTION_CREATE_SHORTCUT));
        i.putExtra(Intent.EXTRA_TITLE, getString(R.string.tools_pick_shortcut));
        try { startActivityForResult(i, item==null ? REQUEST_SELECT_SHORTCUT_FOR_ADD1 : REQUEST_SELECT_SHORTCUT_FOR_PICK1); } catch(Exception e) {}
    }

    private void editShortcutLaunchAction(Shortcut shortcut) {
        mTmpItem = shortcut;
        Intent intent = shortcut.getIntent();
        EventAction eventAction;
        if(LLApp.get().isLightningIntent(intent)) {
            eventAction = Utils.decodeEventActionFromLightningIntent(intent);
            if(eventAction == null) {
                eventAction = new EventAction(GlobalConfig.LAUNCH_SHORTCUT, intent.toUri(0));
            }
        } else {
            boolean isApp = false;
            String uri = intent.toUri(0);
            Page appDrawerPage = mEngine.getOrLoadPage(Page.APP_DRAWER_PAGE);
            for (Item item : appDrawerPage.items) {
                if(item instanceof Shortcut) {
                    Intent i = ((Shortcut) item).getIntent();
                    if(i != null && i.toUri(0).equals(uri)) {
                        isApp = true;
                        break;
                    }
                }
            }

            eventAction = new EventAction(isApp ? GlobalConfig.LAUNCH_APP : GlobalConfig.LAUNCH_SHORTCUT, intent.toUri(0));
        }

        EventActionSetup.startActivityForResult(this, eventAction, true, Action.FLAG_TYPE_DESKTOP, false, REQUEST_EDIT_LAUNCH_ACTION);
    }

	private void selectAppWidgetForAdd() {
		selectAppWidgetForReplace(null);
	}

	private void selectAppWidgetForReplace(Item replaced_item) {
		mTmpItem = replaced_item;
		mAllocatedAppWidgetId=LLApp.get().getAppWidgetHost().allocateAppWidgetId();

		Intent pickIntent;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            pickIntent = new Intent(this, AppWidgetPickerActivity.class);
        } else {
            pickIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_PICK);
        }
		pickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAllocatedAppWidgetId);
		ArrayList<AppWidgetProviderInfo> customInfo = new ArrayList<AppWidgetProviderInfo>();
		pickIntent.putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_INFO, customInfo);
		ArrayList<Bundle> customExtras = new ArrayList<Bundle>();
		pickIntent.putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_EXTRAS, customExtras);

		try { startActivityForResult(pickIntent, REQUEST_SELECT_APP_WIDGET_FOR_ADD); } catch(Exception e) {}
	}

	protected void addFolder() {
        ItemLayout il = mScreen.getTargetOrTopmostItemLayout();
        Page page = il.getPage();
		Item item = Utils.addFolder(page, mScreen.getLastTouchedAddX(), mScreen.getLastTouchedAddY(), il.getCurrentScale(), true, getString(net.pierrox.lightning_launcher.R.string.default_folder_name));
        mUndoStack.storePageAddItem(item);
        boolean wasInEditMode = il.getEditMode();
		editItem(il, item);

        openBubbleHint(SystemConfig.HINT_FOLDER, il.getItemView(item), wasInEditMode);
	}

    protected void addPageIndicator() {
        if(LLApp.get().isFreeVersion()) {
            LLApp.get().showFeatureLockedDialog(this);
            return;
        }
        ItemLayout il = mScreen.getTargetOrTopmostItemLayout();
        Page page = il.getPage();
        PageIndicator item = Utils.addPageIndicator(page, mScreen.getLastTouchedAddX(), mScreen.getLastTouchedAddY(), il.getCurrentScale(), true);
        ItemConfig ic = new ItemConfig();
        ic.copyFrom(item.getItemConfig());
        ic.enabled = false;
        ic.pinMode = ItemConfig.PinMode.XY;
        item.setItemConfig(ic);

        Utils.convertPinnedItemPosition(il, item, ItemConfig.PinMode.NONE, ItemConfig.PinMode.XY);

        item.notifyChanged();
        mUndoStack.storePageAddItem(item);
        boolean wasInEditMode = il.getEditMode();
        enterEditMode(il, item);

        openBubbleHint(SystemConfig.HINT_PAGE_INDICATOR, il.getItemView(item), wasInEditMode);
    }

    private void addDynamicText() {
        if(LLApp.get().isFreeVersion()) {
            LLApp.get().showFeatureLockedDialog(this);
            return;
        }
        ItemLayout il = mScreen.getTargetOrTopmostItemLayout();
        Page page = il.getPage();
        DynamicText item = Utils.addDynamicText(page, DynamicTextConfig.Source.DATE, page.config.newOnGrid);
        Utils.setItemPosition(page, item, mScreen.getLastTouchedAddX(), mScreen.getLastTouchedAddY(), il.getCurrentScale(), true);
        mUndoStack.storePageAddItem(item);
        item.notifyChanged();
        item.setEditMode(il.getEditMode());
        enterEditMode(il, item);
        showEditPropertiesBox();
        updateEditPropertiesBox(item.getPage(), item);
        updateEditBars();
        mEditPropertiesBox.gotoPageDynamicText();
    }

	private void addStopPoint() {
        ItemLayout il = mScreen.getTargetOrTopmostItemLayout();
        Page page = il.getPage();

        boolean found = false;
        for(Item i : page.items) {
            if(i.getClass() == StopPoint.class) {
                found = true;
                break;
            }
        }
        if(!found && page.config.snapToPages) {
            page.config.snapToPages = false;
            page.setModified();
            showDialog(DIALOG_STOP_POINT);
        }

        float itemLayoutScale = il.getCurrentScale();
        Item item;
        if(il.getPage().config.newOnGrid) {
        	int x = mScreen.getLastTouchedAddX();
        	int y = mScreen.getLastTouchedAddY();
        	if(x != Utils.POSITION_AUTO) {
        		x += (int)(il.getCellWidth()/2);
        		y += (int)(il.getCellHeight()/2);
        	}
            item = Utils.addStopPoint(page, x, y, itemLayoutScale, true);
        } else {
            item = Utils.addStopPoint(page, mScreen.getLastTouchedAddX(), mScreen.getLastTouchedAddY(), itemLayoutScale, true);
        }
        mUndoStack.storePageAddItem(item);
        il.requestLayout();

        // FIXME hack, enforce enter edit mode, should be a parameter of enterEditModeAndSelectItem
        boolean old_autoEdit = mSystemConfig.autoEdit;
        mSystemConfig.autoEdit = true;
        boolean wasInEditMode = il.getEditMode();
        enterEditMode(il, item);
        mSystemConfig.autoEdit = old_autoEdit;

        openBubbleHint(SystemConfig.HINT_STOP_POINT, il.getItemView(item), wasInEditMode);
	}

    private void addEmbeddedFolder() {
        ItemLayout il = mScreen.getTargetOrTopmostItemLayout();
        Page page = il.getPage();
        int width = il.getWidth();
        if(width == 0) {
            // workaround if resuming activity while the add dialog was displayed but the views has not yet been layout
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            width = metrics.widthPixels;
        }
        Item item;
        float itemLayoutScale = il.getCurrentScale();
        int cellHeight = (int) il.getCellHeight();
        if(page.config.newOnGrid) {
            item = Utils.addEmbeddedFolder(page, (int) Math.floor(mScreen.getLastTouchedAddX() / (double) width) * width, mScreen.getLastTouchedAddY(), width, cellHeight, itemLayoutScale, true);
        } else {
            item = Utils.addEmbeddedFolder(page, mScreen.getLastTouchedAddX(), mScreen.getLastTouchedAddY(), width, cellHeight, itemLayoutScale, true);
        }
        mUndoStack.storePageAddItem(item);
        boolean wasInEditMode = il.getEditMode();
        editItem(il, item);

        openBubbleHint(SystemConfig.HINT_PANEL, il.getItemView(item), wasInEditMode);
    }

    private void addUnlocker() {
        if(LLApp.get().isFreeVersion()) {
            LLApp.get().showFeatureLockedDialog(this);
            return;
        }
        ItemLayout il = mScreen.getTargetOrTopmostItemLayout();
        Page page = il.getPage();
        float itemLayoutScale = il.getCurrentScale();
        float cw = page.getCurrentViewCellWidth();
        float ch = page.getCurrentViewCellHeight();
        Item item;
        if(page.config.newOnGrid) {
            item = Utils.addUnlocker(page, (int)(mScreen.getLastTouchedAddX()-cw), (int)(mScreen.getLastTouchedAddY()-ch), (int) (cw *3), (int) (ch *3), itemLayoutScale, true);
        } else {
            item = Utils.addUnlocker(page, mScreen.getLastTouchedAddX(), mScreen.getLastTouchedAddY(), (int) (cw *3), (int) (ch *3), itemLayoutScale, true);
        }
        mUndoStack.storePageAddItem(item);
        editItem(il, item);
    }

    private  Intent getDoNothingIntent() {
        Intent intent = new Intent(this, Dashboard.class);
        intent.putExtra(LightningIntent.INTENT_EXTRA_ACTION, GlobalConfig.NOTHING);
        return intent;
    }

    private void addItemText() {
        Shortcut shortcut = addShortcut(getString(R.string.ai_t), false, true);
        menuActionEditLabel(shortcut);
    }

    private void addItemIcon() {
        Shortcut shortcut = addShortcut("", true, false);
        menuActionPickCustomIcon(mScreen.getTargetOrTopmostItemLayout(), shortcut); // the item layout should be returned by addShortcut somehow
    }

    private void addDummyShortcut() {
        addShortcut(getString(R.string.ai_d), true, true);
    }

    private void addSideBar() {
        ItemLayout il = mScreen.getTargetOrTopmostItemLayout();
        Page page = il.getPage();
        boolean wasInEditMode = il.getEditMode();
        Folder folder = Utils.addFolder(page, mScreen.getLastTouchedAddX(), mScreen.getLastTouchedAddY(), il.getCurrentScale(), true, getString(R.string.ai_sb));
        FolderConfig fc = new FolderConfig();
        fc.copyFrom(page.config.defaultFolderConfig);
        fc.titleVisibility = false;
        fc.wAV = Box.AlignV.TOP;
        fc.animationIn = FolderConfig.FolderAnimation.SLIDE_FROM_TOP;
        fc.animationOut = FolderConfig.FolderAnimation.SLIDE_FROM_BOTTOM;
        fc.animFade = false;
        fc.wW = mScreen.getContentView().getWidth();
        folder.setFolderConfig(fc);
        folder.notifyChanged();
        mUndoStack.storePageAddItem(folder);
        editItem(il, folder);

        openBubbleHint(SystemConfig.HINT_SIDE_BAR, il.getItemView(folder), wasInEditMode);
    }

    private void addBadge() {
        if(LLApp.get().isFreeVersion()) {
            LLApp.get().showFeatureLockedDialog(this);
            return;
        }
        ItemLayout il = mScreen.getTargetOrTopmostItemLayout();
        Page page = il.getPage();
        DynamicText item = Utils.addDynamicText(page, DynamicTextConfig.Source.MISSED_CALLS, false);
        Resources resources = getResources();
        final File iconDir = page.getIconDir();
        final int id = item.getId();
        Utils.copyResourceToFile(resources, R.drawable.badge_red, Box.getBoxBackgroundNormal(iconDir, id));
        Utils.copyResourceToFile(resources, R.drawable.badge_red, Box.getBoxBackgroundSelected(iconDir, id));
        Utils.copyResourceToFile(resources, R.drawable.badge_red, Box.getBoxBackgroundFocused(iconDir, id));
        final Box box = item.getItemConfig().box;
        box.size[Box.PT] = box.size[Box.PL] =box.size[Box.PB] =box.size[Box.PR] = (int) (5 * resources.getDisplayMetrics().density);
        item.getItemConfig().box_s = box.toString(page.config.defaultItemConfig.box);
        box.loadAssociatedDrawables(iconDir, id, true);
        Utils.setItemPosition(page, item, mScreen.getLastTouchedAddX(), mScreen.getLastTouchedAddY(), il.getCurrentScale(), true);
        item.notifyChanged();
        item.setEditMode(il.getEditMode());
        mUndoStack.storePageAddItem(item);
        enterEditMode(il, item);
        showEditPropertiesBox();
        updateEditPropertiesBox(item.getPage(), item);
        updateEditBars();
        mEditPropertiesBox.gotoPageDynamicText();
    }

    private void addBookmark() {
        ItemLayout il = mScreen.getTargetOrTopmostItemLayout();
        Page page = il.getPage();
        Intent intent = PhoneUtils.createDesktopBookmarkShortcut(this, il, null, null, null);
        Item item = Utils.addAndroidShortcutFromIntent(this, intent, page, mScreen.getLastTouchedAddX(), mScreen.getLastTouchedAddY(), il.getCurrentScale());
        mUndoStack.storePageAddItem(item);
        boolean wasInEditMode = il.getEditMode();
        editItem(il, item);

        openBubbleHint(SystemConfig.HINT_BOOKMARK, il.getItemView(item), wasInEditMode);
    }

    private void addLightningAction() {
        Intent intent = new Intent(this, ShortcutsA.class);
        startActivityForResult(intent, REQUEST_SELECT_SHORTCUT_FOR_ADD2);
    }

    private void addCustomView() {
        ItemLayout il = mScreen.getTargetOrTopmostItemLayout();
        Page page = il.getPage();
        Item item = Utils.addCustomView(page, mScreen.getLastTouchedAddX(), mScreen.getLastTouchedAddY(), il.getCurrentScale());
        mUndoStack.storePageAddItem(item);
        boolean wasInEditMode = il.getEditMode();
        enterEditMode(il, item);
        showEditPropertiesBox();
        updateEditPropertiesBox(item.getPage(), item);
        updateEditBars();
        mEditPropertiesBox.gotoPageEvents();
        openBubbleHint(SystemConfig.HINT_CUSTOM_VIEW, il.getItemView(item), wasInEditMode);
    }

    private Shortcut addShortcut(String label, boolean icon_visibility, boolean label_visibility) {
        ItemLayout il = mScreen.getTargetOrTopmostItemLayout();
        Page page = il.getPage();
        Shortcut s = Utils.addShortcut(label, null, getDoNothingIntent(), page, mScreen.getLastTouchedAddX(), mScreen.getLastTouchedAddY(), il.getCurrentScale(), true);
        ShortcutConfig sc = s.modifyShortcutConfig();
        sc.iconVisibility = icon_visibility;
        sc.labelVisibility = label_visibility;
        s.notifyChanged();
        mUndoStack.storePageAddItem(s);
        editItem(il, s);
        return s;
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void addPinnedShortcut(Intent intent) {
        Parcelable extra = intent.getParcelableExtra(LauncherApps.EXTRA_PIN_ITEM_REQUEST);
        if(extra instanceof LauncherApps.PinItemRequest) {
            LauncherApps.PinItemRequest request = (LauncherApps.PinItemRequest) extra;
            if (request.getRequestType() == LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT) {
                final LauncherApps launcherApps = (LauncherApps) getSystemService(LAUNCHER_APPS_SERVICE);
                ShortcutInfo shortcutInfo = request.getShortcutInfo();

                final Drawable iconDrawable = launcherApps.getShortcutIconDrawable(shortcutInfo, Utils.getLauncherIconDensity());
                Bitmap icon = Utils.createBitmapFromDrawable(iconDrawable);

                Intent si = new Intent(Shortcut.INTENT_ACTION_APP_SHORTCUT);
                si.putExtra(Shortcut.INTENT_EXTRA_APP_SHORTCUT_ID, shortcutInfo.getId());
                si.putExtra(Shortcut.INTENT_EXTRA_APP_SHORTCUT_PKG, shortcutInfo.getPackage());
                si.putExtra(Shortcut.INTENT_EXTRA_APP_SHORTCUT_DISABLED_MSG, shortcutInfo.getDisabledMessage());

                final ItemLayout il = mScreen.getTargetOrTopmostItemLayout();
                Page page = il.getPage();
                float scale = il.getCurrentScale();
                final Item newItem = Utils.addShortcut(shortcutInfo.getShortLabel().toString(), icon, si, page, Utils.POSITION_AUTO, Utils.POSITION_AUTO, scale, true);

                mUndoStack.storePageAddItem(newItem);
                editItem(il, newItem);
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mScreen.ensureItemViewVisible(il.getItemView(newItem), false);
                    }
                }, 1000);

                request.accept();
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void addAppWidget(Intent intent) {
        Parcelable extra = intent.getParcelableExtra(LauncherApps.EXTRA_PIN_ITEM_REQUEST);
        if(extra instanceof LauncherApps.PinItemRequest) {
            LauncherApps.PinItemRequest request = (LauncherApps.PinItemRequest) extra;
            AppWidgetProviderInfo info = request.getAppWidgetProviderInfo(this);
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
            int allocatedAppWidgetId = LLApp.get().getAppWidgetHost().allocateAppWidgetId();
            appWidgetManager.bindAppWidgetIdIfAllowed(allocatedAppWidgetId, info.getProfile(), info.provider, null);
            final ItemLayout il = mScreen.getTargetOrTopmostItemLayout();
            Page page = il.getPage();
            float scale = il.getCurrentScale();
            final Widget newItem = Utils.addAppWidget(page, il, allocatedAppWidgetId, Utils.POSITION_AUTO, Utils.POSITION_AUTO, scale);
            mUndoStack.storePageAddItem(newItem);

            editItem(il, newItem);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScreen.ensureItemViewVisible(il.getItemView(newItem), false);
                }
            }, 1000);

            request.accept();
        }
    }

    private void configureHandlesForItemView(ItemView itemView, HandleView.Mode mode, boolean show_handles) {
        HandleView hv = mEditItemLayout.getHandleView();
        if (mode == null) {
            mode = hv.getMode();
        }
        hv.setMode(itemView.getClass() == StopPointView.class ? HandleView.Mode.NONE : mode);
        if (show_handles) {
            mEditItemLayout.showHandleViewForItemView(itemView);
        }
    }

    public void enterEditMode(ItemLayout il, Item selected_item) {
        // check that the selected belongs to the item layout, otherwise ignore it
        if(selected_item != null && il.getItemView(selected_item) == null) {
            selected_item = null;
        }

        if(mEditMode && mEditItemLayout == il) {
            unselectAllItems();
            if(selected_item != null) {
                mEditItemLayout.setMasterSelectedItem(selected_item);
                ItemView itemView = mEditItemLayout.getItemView(selected_item);
                if(itemView != null) {
                    itemView.setSelected(true);
                    configureHandlesForItemView(itemView, HandleView.Mode.CONTENT_SIZE, true);
                }
            }
            return;
        }

        final LLApp app = LLApp.get();
        if(app.isTrialVersionExpired()) {
            app.showFeatureLockedDialog(this);
            return;
        }

        boolean was_in_edit_mode = mEditMode;
        if(was_in_edit_mode) {
            leaveEditMode(false);
        }

        Page page = il.getPage();
        page.enterEditMode();

        boolean is_folder = il.getOpenerItemView() != null;
        boolean is_embedded_folder = il.getOpenerItemView() instanceof EmbeddedFolderView;
        if(is_folder) {
            if(is_embedded_folder) {
                // hackish : in case of a panel, open it like a folder and use the item layout of the folder window
                ItemView openerItemView = il.getOpenerItemView();
                ItemLayout newIl = mScreen.openFolder((Folder)openerItemView.getItem(), openerItemView, null, true).getItemLayout();
                newIl.setLocalTransform(il.getLocalTransform());
                il = newIl;
            }
            mEditItemLayout = mScreen.setFolderEditMode(il, true);
        } else {
            mEditItemLayout = il;
        }


		mEditMode=true;

        mOriginalItemsGeometry.clear();

		mEditPage = page;
        mEditItemLayout.setAllowWrap(false);
        mEditItemLayout.setEditMode(true, is_folder && !is_embedded_folder);
        mEditItemLayout.setDisplayInvisibleItems(mSystemConfig.hasSwitch(SystemConfig.SWITCH_DISPLAY_INVISIBLE_ITEMS));
        if(is_embedded_folder) {
            ItemLayout itemLayoutForPage = mScreen.getCurrentRootItemLayout();
            setAlphaOrVisibility(itemLayoutForPage, 0.1f);
            for (FolderView fv : mScreen.getFolderViews()) {
                if(fv.isOpen() && fv.getItemLayout() != mEditItemLayout) {
                    setAlphaOrVisibility(fv, 0.1f);
                }
            }
        }
        if((page.config.wrapX || page.config.wrapY) && (mSystemConfig.hints& SystemConfig.HINT_WRAP)==0) {
            showDialog(DIALOG_WRAP);
        }

        if(selected_item!=null) {
            mEditItemLayout.setMasterSelectedItem(selected_item);
            ItemView itemView = mEditItemLayout.getItemView(selected_item);
            itemView.setSelected(true);
            configureHandlesForItemView(itemView, HandleView.Mode.CONTENT_SIZE, true);
        }

        mScreen.setTargetItemLayout(mEditItemLayout);

        if(!was_in_edit_mode) {
            if (mSystemConfig.hasSwitch(SystemConfig.SWITCH_EDIT_BARS)) {
                showEditBars(true);
            } else {
                showEditBarsHider();
                mEditBarHider.setArrowDirection(true);
            }
        } else {
            updateGeometryBox();
        }

        if(isEditPropertiesBoxVisible()) {
            updateEditPropertiesBox(mEditPage, mEditItemLayout.getMasterSelectedItem());
        }
	}


	protected void leaveEditMode() {
        leaveEditMode(true);
    }

	protected void leaveEditMode(boolean hide_edit_controls) {
		if(mEditMode) {
            unselectAllItems();

            mEditMode=false;
            boolean is_folder = mEditPage.isFolder();
            boolean is_embedded_folder = mEditItemLayout.getOpenerItemView() instanceof EmbeddedFolderView;
            if(is_folder) {
                mScreen.setFolderEditMode(mEditItemLayout, false);
            }
            mEditItemLayout.setAllowWrap(true);
			mEditItemLayout.setEditMode(false, false);
            mEditItemLayout.setDisplayInvisibleItems(false);
//			mEditItemLayout.grabEvent(null);

			mEditItemLayout.hideHandleView();
			//mEditItemLayout.setTrackedItem(null);
            if(hide_edit_controls) {
                hideGeometryBox();
                if (mEditBarsVisible) {
                    hideEditBars(true);
                } else {
                    hideEditBarsHider();
                }
            }

            if(is_embedded_folder) {
                float x = 0, y = 0, scale = 1;
                ItemLayout mainItemLayout = mScreen.getCurrentRootItemLayout();
                setAlphaOrVisibility(mainItemLayout, 1);
                for (FolderView fv : mScreen.getFolderViews()) {
                    if(fv.getItemLayout() == mEditItemLayout) {
                        ItemLayout il = fv.getItemLayout();
                        x = il.getCurrentX();
                        y = il.getCurrentY();
                        scale = il.getCurrentScale();
                    } else {
                        if (fv.isOpen()) {
                            setAlphaOrVisibility(fv, 1);
                        }
                    }
                }
//                mEditPage.saveConfig();
                mScreen.removeFolders(mEditPage);

                // since closing the folder used to edit the panel reset the active page, set it again
                mScreen.setActivePage(mEditPage.id);

                // TODO: not needed anymore ?
                // restore the edit mode position into the original panel's item layout (hackish)
                /*Folder opener = mEditPage.findFirstOpener();
                Rect r = mScreen.computeItemBounds(opener);
                if(!mScreen.getCurrentRootPage().config.statusBarHide && Build.VERSION.SDK_INT>=11) {
                    // hackish, and duplicate with LightningEngine.setFolderEditMode
                    Resources res = getResources();
                    int statusBarHeight = 0;
                    int resourceId = res.getIdentifier("status_bar_height", "dimen", "android");
                    if (resourceId > 0) {
                        statusBarHeight = res.getDimensionPixelSize(resourceId);
                    }
                    r.top -= statusBarHeight;
                    r.bottom -= statusBarHeight;
                }
                Box box = opener.getItemConfig().box;
                int[] s = box.size;
                int dx = s[Box.ML] + s[Box.BL] + s[Box.PL];
                int dy = s[Box.MT] + s[Box.BT] + s[Box.PT];
                ItemLayout il = mScreen.getItemLayoutForPage(mEditPage);
                ItemLayout parent_il = mScreen.getItemLayoutForPage(opener.getPage());
                float parentScale = parent_il.getCurrentScale();
                il.moveTo((x - r.left)/parentScale - dx, (y - r.top)/parentScale - dy, scale/ parentScale);*/
            }

            mEditPage.leaveEditMode();
		}
	}

    private void setAlphaOrVisibility(View view, float to) {
        if(Build.VERSION.SDK_INT >= 11) {
            view.setAlpha(to);
        } else {
            view.setVisibility(to==1 ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Open the bubble for the currently "focused" container using the last touched position
     */
    protected void openBubble(int mode) {
        if(!closeBubble() || mBubbleMode!=mode) {
            Rect focus;
            if(mScreen.getLastTouchedMenuX() != Utils.POSITION_AUTO) {
                mTempRectF.set(mScreen.getLastTouchedMenuX(), mScreen.getLastTouchedMenuY(), mScreen.getLastTouchedMenuX(), mScreen.getLastTouchedMenuY());
                mTempRectF.round(mTempRect);
                focus = mTempRect;
            } else {
                int h = mEditControlsView.getHeight() - /*mEditControlsView.getPaddingTop() -*/ mEditControlsView.getPaddingBottom();
                mTempRect.set(0, h, mScreen.getContentView().getWidth(), h);
                focus = mTempRect;
            }
            openBubble(mode, mScreen.getTargetOrTopmostItemLayout(), focus);
        }
    }

    protected void openBubbleHint(final int hint, final ItemView itemView, boolean wasInEditMode) {
        if((mSystemConfig.hints & hint) != 0) {
            return;
        }

        int mode = BUBBLE_MODE_NONE;
        for (int[] h : BUBBLE_HINTS) {
            if (h[1] == hint) {
                mode = h[0];
                break;
            }
        }
        if(mode == BUBBLE_MODE_NONE) {
            // bubble mode not found, this is a coding error
            throw new RuntimeException("Bubble mode not found");
        }

        // if not in edit mode, wait a bit for the edit bar animation to finish
        long delay;
        if(!wasInEditMode && mSystemConfig.hasSwitch(SystemConfig.SWITCH_EDIT_BARS)) {
            delay = ANIMATE_EDIT_BAR_DURATION+10;
        } else {
            delay = 0;
        }
        final int finalMode = mode;
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                openBubble(finalMode, itemView);
            }
        }, delay);
    }

    /**
     * Open the bubble for a given ItemView
     */
    protected void openBubble(int mode, ItemView itemView) {
        if(itemView == null || itemView.getParentItemLayout() == null) {
            // view deleted in the meantime
            return;
        }
        openBubble(mode, itemView.getParentItemLayout(), itemView, mScreen.computeItemViewBounds(itemView), null);
    }

    /**
     * Open the bubble for a container and an optional focus
     * @param focus new bubble target point, reuse the previous one if null
     */
    protected void openBubble(int mode, ItemLayout itemLayout, Rect focus) {
        openBubble(mode, itemLayout, null, focus, null);
    }

    private void openBubbleAppShortcuts(ItemView itemView, List shortcuts) {
        openBubble(BUBBLE_MODE_APP_SHORTCUTS, itemView.getParentItemLayout(), itemView, mScreen.computeItemViewBounds(itemView), shortcuts);
    }

    /**
     * Do not use directly, use the other versions of openBubble
     */
    protected void openBubble(int mode, ItemLayout itemLayout, ItemView itemView, Rect focus, List shortcuts) {
        if(mBubble == null) {
            mBubble=(BubbleLayout) getLayoutInflater().inflate(R.layout.bubble, null);
            mEditControlsView.addView(mBubble);
            mBubbleContent=(LinearLayout)mBubble.findViewById(R.id.bubble_content);
            mBubble.findViewById(R.id.bbl_clone).setContentDescription(getString(R.string.eb_pr));
            mBubble.findViewById(R.id.bbl_rm).setContentDescription(getString(R.string.mi_remove));
            mBubble.findViewById(R.id.bbl_add).setContentDescription(getString(R.string.menu_add));
            mBubble.findViewById(R.id.bbl_settings).setContentDescription(getString(R.string.mi_es_settings));
            mBubble.findViewById(R.id.bbl_edit).setContentDescription(getString(R.string.menu_objects_layout));

            Typeface typeface = LLApp.get().getIconsTypeface();
            int[] bbl_buttons = new int[] { R.id.bbl_edit, R.id.bbl_add, R.id.bbl_clone, R.id.bbl_rm, R.id.bbl_settings};
            for(int id : bbl_buttons) {
                Button btn = (Button) mBubble.findViewById(id);
                btn.setTypeface(typeface);
                btn.setOnClickListener(mEditBarClick);
                btn.setOnLongClickListener(mEditBarLongClick);
            }

            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            mBubble.setDensity(metrics.density);

            try { mBubbleAnimIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in); } catch (Exception e) { mBubbleAnimIn = null; }
            try { mBubbleAnimOut = AnimationUtils.loadAnimation(this, android.R.anim.fade_out); } catch (Exception e) { mBubbleAnimOut = null; }
        }
        mBubbleMode = mode;

        mBubbleItemLayout = itemLayout;
        mBubbleItemView = itemView;

        Item item = itemView==null ? null : itemView.getItem();

        boolean for_item = item != null;
        boolean is_app_drawer = getClass() == AppDrawerX.class;
        mBubble.findViewById(R.id.bbl_add).setVisibility(for_item && !is_app_drawer ? View.GONE : View.VISIBLE);
        mBubble.findViewById(R.id.bbl_clone).setVisibility(for_item && !is_app_drawer ? View.VISIBLE : View.GONE);
        Button delete_button = (Button) mBubble.findViewById(R.id.bbl_rm);
        delete_button.setVisibility(for_item ? View.VISIBLE : View.GONE);
        boolean uninstall = item != null && item.getClass() == Shortcut.class && is_app_drawer;
        delete_button.setText(uninstall ? "E" : "4");
        delete_button.setContentDescription(getString(uninstall ? R.string.mi_uninstall : R.string.mi_remove));
        delete_button.setEnabled(mBubbleItemView != null);
        mBubble.findViewById(R.id.bbl_settings).setVisibility(for_item ? View.GONE : View.VISIBLE);


        mBubbleContent.removeAllViews();

        View titleGroup = mBubble.findViewById(R.id.bbl_title_group);
        View buttonsGroup = mBubble.findViewById(R.id.bbl_btns_group);
        titleGroup.setVisibility(displayBubbleTitleForMode(mode) ? View.VISIBLE : View.GONE);
        buttonsGroup.setVisibility(displayBubbleButtonsForMode(mode) ? View.VISIBLE : View.GONE);

        if(mode < 0)  {
            for (int[] hint : BUBBLE_HINTS) {
                if (hint[0] == mode) {
                    addBubbleHint(hint[1], hint[2]);
                    break;
                }
            }
            if(mode == BUBBLE_MODE_HINT_DESKTOP || mode == BUBBLE_MODE_HINT_APP_DRAWER) {
                mBubble.findViewById(R.id.dsa).setVisibility(View.GONE);
            }
        } else {
            String text;
            if(itemView == null) {
                text = Utils.formatItemLayoutName(itemLayout);
                configureBubbleForContainer(mode, itemLayout);
            } else {
                text = Utils.formatItemName(itemView.getItem(), 20, getSelectedItemViews().size());
                configureBubbleForItem(mode, itemView, shortcuts);
            }
            ((TextView)mBubble.findViewById(R.id.bbl_ttl)).setText(text);
        }

        mBubble.setScreenPadding(mEditControlsView.getPaddingTop());
        if(focus != null) {
            mBubble.setItemBounds(focus);
        }
        if(mBubble.getVisibility()==View.GONE) {
            if(mBubbleAnimIn != null) {
                mBubble.startAnimation(mBubbleAnimIn);
            }
            mBubble.setVisibility(View.VISIBLE);
        }
    }

    protected boolean displayBubbleTitleForMode(int mode) {
        return mode > 0;
    }

    protected boolean displayBubbleButtonsForMode(int mode) {
        return mode > 0 && mode != BUBBLE_MODE_APP_SHORTCUTS;
    }

    protected void addScriptBubbleItems(int criteria) {
        ArrayList<Script> scripts = mEngine.getScriptManager().getAllScriptMatching(criteria);
        if(scripts.size() == 0) {
            if(criteria == Script.FLAG_ITEM_MENU) {
                addBubbleItem(R.id.mi_nos, R.string.mi_nos);
            }
        } else {
            Utils.sortScripts(scripts);
            for (Script script : scripts) {
                Button b = (Button) getLayoutInflater().inflate(R.layout.bubble_item, null);
                b.setTag(script);
                b.setText(script.name);
                b.setOnClickListener(this);
                mBubbleContent.addView(b);
            }

        }
    }

    @TargetApi(Build.VERSION_CODES.N_MR1)
    protected void addAppShortcutBubbleItems(List shortcuts) {
        for (Object shortcut : shortcuts) {
            addAppShortcutBubbleItem(shortcut);
        }
    }

    @TargetApi(Build.VERSION_CODES.N_MR1)
    private void addAppShortcutBubbleItem(Object shortcutObject) {
        ShortcutInfo shortcut = (ShortcutInfo) shortcutObject;
        final LauncherApps launcherApps = (LauncherApps) getSystemService(LAUNCHER_APPS_SERVICE);
        final View view = getLayoutInflater().inflate(R.layout.two_lines_list_item, null);
        final Drawable iconDrawable = launcherApps.getShortcutIconDrawable(shortcut, Utils.getLauncherIconDensity());
        Bitmap icon = Utils.createBitmapFromDrawable(iconDrawable);
        ImageView iconView = (ImageView) view.findViewById(android.R.id.icon);
        iconView.setImageBitmap(icon);


        CharSequence label = shortcut.getLongLabel();
        if(label == null || label.length() == 0) {
            label = shortcut.getShortLabel();
        }
        ((TextView)view.findViewById(android.R.id.text1)).setText(label);
        view.findViewById(android.R.id.text2).setVisibility(View.GONE);
        Drawable background = getDrawable(R.drawable.bubble_item_bg);
        view.setBackground(background);

        if(mAppShortcutClickListener == null) {
            mAppShortcutClickListener = new OnClickListener() {
                @Override
                public void onClick(View view) {
                    closeBubble();

                    if(launcherApps.hasShortcutHostPermission()) {
                        ShortcutInfo shortcutInfo = (ShortcutInfo) view.getTag();
                        Rect bounds = new Rect();
                        view.getHitRect(bounds);
                        launcherApps.startShortcut(shortcutInfo, bounds, null);
                    } else {
                        showDialog(DIALOG_LAUNCHER_APPS_NO_HOST_PERMISSION);
                    }
                }
            };
        }

        if(mAppShortcutLongClickListener == null) {
            mAppShortcutLongClickListener = new OnLongClickListener() {
                @TargetApi(Build.VERSION_CODES.N_MR1)
                @Override
                public boolean onLongClick(View view) {
                    closeBubble();

                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);

                    ShortcutInfo shortcutInfo = (ShortcutInfo) view.getTag();
                    ClipData clipData = ClipData.newPlainText("id", shortcutInfo.getId());
                    View shadowView = view.findViewById(android.R.id.icon);
                    view.startDragAndDrop(clipData, new View.DragShadowBuilder(shadowView), shortcutInfo, 0);
                    return true;
                }
            };
        }

        view.setOnClickListener(mAppShortcutClickListener);
        view.setOnLongClickListener(mAppShortcutLongClickListener);
        view.setTag(shortcut);
        mBubbleContent.addView(view);
    }

    private View.OnClickListener mAppShortcutClickListener;
    private View.OnLongClickListener mAppShortcutLongClickListener;

    protected View addBubbleItem(int id, int title) {
        return addBubbleItem(id, getString(title));
    }

    protected View addBubbleItem(int id, CharSequence title) {
        Button b=(Button)getLayoutInflater().inflate(R.layout.bubble_item, null);
        b.setId(id);
        b.setText(title);
        b.setOnClickListener(this);
        mBubbleContent.addView(b);
        return b;
    }

    protected View addBubbleItem(CharSequence title, View.OnClickListener listener) {
        Button b=(Button)getLayoutInflater().inflate(R.layout.bubble_item, null);
        b.setText(title);
        b.setOnClickListener(listener);
        mBubbleContent.addView(b);
        return b;
    }

    /*
     * id in layout, link wiki, value mask
     */
    protected void addBubbleHint(int hint, int hint_text_id) {
        View v = getLayoutInflater().inflate(R.layout.hint, null);
        ((TextView)v.findViewById(R.id.h_ttl)).setText(R.string.h_t);
        ((TextView)v.findViewById(R.id.h_t)).setText(hint_text_id);
        TextView dsa = (TextView) v.findViewById(R.id.dsa);
        dsa.setText(R.string.dialog_help_hint_dsa);
        dsa.setTag(Integer.valueOf(hint));
        v.setOnClickListener(this);
        mBubbleContent.addView(v);
    }

    protected void addBubbleItemActions(Shortcut shortcut) {
        // as of today only shortcut is supported
        PackageManager pm=getPackageManager();
        Uri data=null;
        ComponentName cn=shortcut.getIntent().getComponent();
        if(cn!=null) {
            data=Uri.parse("pkg://"+cn.getPackageName());
        }
        Intent intent=new Intent(LLApp.INTENT_ITEM_ACTION, data);
        final String category="Shortcut";
        intent.addCategory(category);

        List<ResolveInfo> r=pm.queryIntentActivities(intent, 0);
        int s=r.size();
        for(int i=0; i<s; i++) {
            ResolveInfo ri=r.get(i);
            if(ri.activityInfo.packageName.equals("net.pierrox.lightning_launcher.item_action.app_killer")) continue;

            String label=ri.loadLabel(pm).toString();
            intent=new Intent(LLApp.INTENT_ITEM_ACTION, data);
            intent.addCategory(category);
            intent.setComponent(new ComponentName(ri.activityInfo.packageName, ri.activityInfo.name));

            Button btn=(Button) getLayoutInflater().inflate(R.layout.bubble_item, null);
            btn.setText(label);
            btn.setTag(intent);
            btn.setOnClickListener(this);
            mBubbleContent.addView(btn);
        }
    }

    private void addBubbleItemScript(final ItemLayout il, final ItemView itemView, final int mode, EventAction ea) {
        if(ea.action == GlobalConfig.RUN_SCRIPT) {
            final LightningEngine engine = il == null ? itemView.getItem().getPage().getEngine() : il.getPage().getEngine();
            Pair<Integer, String> pair = Script.decodeIdAndData(ea.data);
            int id = pair.first;
            Script script = engine.getScriptManager().getOrLoadScript(id);
            if(script != null && !script.hasFlag(Script.FLAG_DISABLED)) {
                if(mScriptMenuListener == null) {
                    mScriptMenuListener = new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ScriptExecutor se = engine.getScriptExecutor();
                            Function function = (Function) v.getTag();
                            se.runFunction(function, new Object[]{v}, true, true);
                        }
                    };
                }
                ScriptExecutor se = engine.getScriptExecutor();
                net.pierrox.lightning_launcher.script.api.Menu menu = new net.pierrox.lightning_launcher.script.api.Menu(new net.pierrox.lightning_launcher.script.api.Menu.MenuImpl() {
                    @Override
                    public ViewGroup getRootView() {
                        return mBubble;
                    }

                    @Override
                    public ViewGroup getMainItemsView() {
                        return mBubbleContent;
                    }

                    @Override
                    public int getMode() {
                        return mode;
                    }

                    @Override
                    public View addMainItem(String text, Function action) {
                        View view = addBubbleItem(text, mScriptMenuListener);
                        view.setTag(action);
                        return view;
                    }

                    @Override
                    public void close() {
                        closeBubble();
                    }
                });
                se.runScriptMenu(getScreen(), id, il, itemView, menu, pair.second);
            }
        }
        if(ea.next != null) {
            addBubbleItemScript(il, itemView, mode, ea.next);
        }
    }

    private View.OnClickListener mScriptMenuListener;

    protected boolean closeBubble() {
        if(mBubble!=null) {
            View dsa_view = mBubbleContent.findViewById(R.id.dsa);
            if(dsa_view != null) {
                boolean dsa = ((CheckBox)dsa_view).isChecked();
                if(dsa) {
                    int hint = (Integer) dsa_view.getTag();
                    mSystemConfig.hints |= hint;
                }
            }
            int l=mBubbleContent.getChildCount();
            for(int i=0; i<l; i++) {
                View v = mBubbleContent.getChildAt(i);
                v.setEnabled(false);
            }
            if(mBubbleMode == BUBBLE_MODE_HINT_APP_DRAWER) {
                Rect focus = new Rect();
                mScreen.getContentView().getHitRect(focus);
                int cx = focus.centerX();
                int cy = focus.centerY();
                focus.set(cx, cy, cx, cy);
                openBubble(BUBBLE_MODE_HINT_DESKTOP, mScreen.getCurrentRootItemLayout(), focus);
            } else {
                if(mBubble.getVisibility()==View.VISIBLE) {
                    mBubble.setVisibility(View.GONE);
                    if(mBubbleAnimOut != null) {
                        mBubble.startAnimation(mBubbleAnimOut);
                    }
                    return true;
                }
            }
            mBubbleItemView = null;
            mBubbleItemLayout = null;
        }
        return false;
    }

    protected boolean canMoveOutOfFolder() {
        return true;
    }

    protected void configureBubbleForItem(final int mode, final ItemView itemView, List shortcuts) {
        Item item = itemView.getItem();
        Class<?> item_class = item.getClass();

        if(mode == BUBBLE_MODE_ITEM_EM) {
            addBubbleItem(R.id.mi_edit, R.string.mi_customize);
            addBubbleItem(R.id.mi_position, R.string.mi_position);
            addBubbleItem(R.id.mi_actions, R.string.mi_actions);
        } else if(mode == BUBBLE_MODE_ITEM_NO_EM) {
            Page page=item.getPage();
            boolean is_folder_page = page.isFolder();
            boolean is_in_embedded_folder = itemView.getParentItemLayout().getOpenerItemView() instanceof EmbeddedFolderView;
            if(is_folder_page && page.id != Page.USER_MENU_PAGE && canMoveOutOfFolder()) addBubbleItem(R.id.mi_move_out_of_folder, is_in_embedded_folder ? R.string.mi_mop : R.string.mi_move_out_of_folder);

            if(getClass() == AppDrawerX.class && item_class != PageIndicator.class) {
                addBubbleItem(R.id.mi_add_to_launcher, R.string.mi_add_to_launcher);
                addBubbleItem(R.id.mi_hide_unhide, R.string.mi_hide_unhide);
            }
            if(item_class == Shortcut.class || item_class == Widget.class || item_class == DynamicText.class) {
                boolean has_widget_options = item_class == Widget.class && ((Widget)item).hasConfigurationScreen();
                if(has_widget_options) addBubbleItem(R.id.mi_widget_options, R.string.mi_widget_options);

                String pkg = Utils.getPackageNameForItem(item);
                if(pkg != null) {
                    addBubbleItem(R.id.mi_app_details, R.string.mi_app_details);
                    addBubbleItem(R.id.mi_app_store, R.string.mi_app_store);
                    addBubbleItem(R.id.mi_kill, pkg.equals(getPackageName()) ? R.string.an_re : R.string.mi_kill);
                    addBubbleItem(R.id.mi_uninstall, R.string.mi_uninstall);
                }
            } else if(getClass()!=AppDrawerX.class) {
                if(false) {
                    if (item_class == Folder.class) {
                        addBubbleItem(R.id.mi_cfp, R.string.mi_cfp);
                    } else if (item_class == EmbeddedFolder.class) {
                        addBubbleItem(R.id.mi_cpf, R.string.mi_cpf);
                    }
                }
            }

            if(mGlobalConfig.runScripts) {
                addBubbleItem(R.id.mi_s, R.string.mi_s);
            }
        } else if(mode == BUBBLE_MODE_ITEM_EDIT) {
            boolean is_shortcut=(item_class==Shortcut.class || item_class==Folder.class);
            boolean is_widget = item_class==Widget.class;
            boolean is_embedded_folder = itemView.getClass() == EmbeddedFolderView.class;
            boolean has_widget_options = is_widget && ((Widget)item).hasConfigurationScreen();

            if(is_shortcut) {
                addBubbleItem(R.id.mi_edit_icon, R.string.mi_edit_icon);
                addBubbleItem(R.id.mi_edit_label, R.string.mi_edit_label);
            }
            if((item_class==Shortcut.class || item_class==DynamicText.class) && this.getClass()!=AppDrawerX.class) {
                addBubbleItem(R.id.mi_edit_launch_action, R.string.mi_eda);
            }
            if(is_widget) addBubbleItem(R.id.mi_pick_widget, R.string.mi_pick_widget);
            if(has_widget_options) addBubbleItem(R.id.mi_widget_options, R.string.mi_widget_options);
            if(getClass()==AppDrawerX.class) {
                addBubbleItem(R.id.mi_hide_unhide, R.string.mi_hide_unhide);
            }
            if(is_embedded_folder) addBubbleItem(R.id.mi_ef_edit_layout, R.string.menu_objects_layout);
            addBubbleItem(R.id.mi_edit_more, R.string.mi_edit_more);
        } else if(mode == BUBBLE_MODE_ITEM_POSITION) {
            boolean is_stop_point = item_class==StopPoint.class;
            Page page = item.getPage();
            boolean is_folder_page = page.isFolder();
            boolean is_in_embedded_folder = itemView.getParentItemLayout().getOpenerItemView() instanceof EmbeddedFolderView;
            ItemConfig ic = item.getItemConfig();
            addBubbleItem(R.id.mi_lm, ic.onGrid ? R.string.mi_lmg : R.string.mi_lmf);
            if(!is_stop_point) addBubbleItem(R.id.mi_pin, ic.pinMode!= ItemConfig.PinMode.NONE ? R.string.mi_unpin : R.string.mi_pin);
            if(is_folder_page && page.id != Page.USER_MENU_PAGE) addBubbleItem(R.id.mi_move_out_of_folder, is_in_embedded_folder ? R.string.mi_mop : R.string.mi_move_out_of_folder);
            addBubbleItem(R.id.mi_move_to_screen, R.string.mi_move_to_screen);
            addBubbleItem(R.id.mi_copy_to_screen, R.string.mi_copy_to_screen);
        } else if(mode == BUBBLE_MODE_ITEM_ACTIONS) {
            if(getClass()!=AppDrawerX.class) {
                if(false) {
                    if (item_class == Folder.class) {
                        addBubbleItem(R.id.mi_cfp, R.string.mi_cfp);
                    } else if (item_class == EmbeddedFolder.class) {
                        addBubbleItem(R.id.mi_cpf, R.string.mi_cpf);
                    }
                }
            }
            String pkg = Utils.getPackageNameForItem(item);
            if(pkg != null) {
                addBubbleItem(R.id.mi_app_details, R.string.mi_app_details);
                addBubbleItem(R.id.mi_app_store, R.string.mi_app_store);
                addBubbleItem(R.id.mi_kill, pkg.equals(getPackageName()) ? R.string.an_re : R.string.mi_kill);
                addBubbleItem(R.id.mi_uninstall, R.string.mi_uninstall);
            }
            if(mGlobalConfig.runScripts) {
                addBubbleItem(R.id.mi_s, R.string.mi_s);
            }
        } else if(mode == BUBBLE_MODE_CUSTOM_MENU) {
            addScriptBubbleItems(Script.FLAG_CUSTOM_MENU);
            addBubbleItem(R.id.mi_dm_customize, R.string.menu_customize);
        } else if(mode == BUBBLE_MODE_SCRIPTS) {
            addScriptBubbleItems(Script.FLAG_ITEM_MENU);
        } else if(mode == BUBBLE_MODE_APP_SHORTCUTS) {
            addAppShortcutBubbleItems(shortcuts);
        }

        if(item!=null && item_class == Shortcut.class && mBubbleMode!= BUBBLE_MODE_ITEM_EDIT) {
            addBubbleItemActions((Shortcut)item);
        }

        EventAction ea = item.getItemConfig().menu;
        addBubbleItemScript(null, itemView, mode, ea);
    }

    protected void configureBubbleForContainer(int mode, ItemLayout il) {
        if(mode == BUBBLE_MODE_LIGHTNING_MENU_EM) {
            addBubbleItem(R.id.mi_l, mGlobalConfig.itemLongTap.action==GlobalConfig.NOTHING ? R.string.mi_ul : R.string.mi_l);
            addBubbleItem(R.id.mi_i, R.string.mi_i);
            if(mSystemConfig.hasSwitch(SystemConfig.SWITCH_MULTI_SELECTION)) {
                addBubbleItem(R.id.mi_sel, R.string.mi_sel);
            }
            if(mGlobalConfig.runScripts) {
                addBubbleItem(R.id.mi_s, R.string.mi_s);
            }
            addBubbleItem(R.id.mi_dm_customize, R.string.mi_es_settings);
        } else if(mode == BUBBLE_MODE_LIGHTNING_MENU_NO_EM) {
            addBubbleItem(R.id.mi_l, mGlobalConfig.itemLongTap.action==GlobalConfig.NOTHING ? R.string.mi_ul : R.string.mi_l);
            addBubbleItem(R.id.mi_i, R.string.mi_i);
            if(mGlobalConfig.runScripts) {
                addBubbleItem(R.id.mi_s, R.string.mi_s);
            }
            addBubbleItem(R.id.mi_dm_customize, R.string.mi_es_settings);
        } else if(mode == BUBBLE_MODE_SETTINGS) {
            int text_res_id;
            Page page = il.getPage();
            if(page.isFolder()) {
                boolean is_embedded_folder = il.getOpenerItemView() instanceof EmbeddedFolderView;
                text_res_id = is_embedded_folder ? R.string.mc_ef : R.string.menu_customize_folder;
            } else if(page.id == Page.APP_DRAWER_PAGE) {
                text_res_id = R.string.app_drawer_t;
            } else {
                text_res_id = R.string.dashboard_t;
            }
            addBubbleItem(R.id.mi_dmc_c, text_res_id);
            addBubbleItem(R.id.mi_dmc_r, R.string.lightning);
            addBubbleItem(R.id.mi_android_settings, R.string.menu_settings);
        } else if(mode == BUBBLE_MODE_ITEMS) {
            addBubbleItem(R.id.mi_h, R.string.mi_h);
            addBubbleItem(R.id.mi_ic, R.string.mi_ic);
            addBubbleItem(R.id.mi_isa, R.string.mi_isa);
            addBubbleItem(R.id.mi_isd, R.string.mi_isd);
        } else if(mode == BUBBLE_MODE_CUSTOM_MENU) {
            addScriptBubbleItems(Script.FLAG_CUSTOM_MENU);
            addBubbleItem(R.id.mi_dm_customize, R.string.menu_customize);
        } else if(mode == BUBBLE_MODE_SCRIPTS) {
            addBubbleItem(R.id.mi_ls, R.string.mi_ls);
            addScriptBubbleItems(Script.FLAG_APP_MENU);
        } else if(mode == BUBBLE_MODE_SELECT) {
            addBubbleItem(R.id.mi_sa, R.string.mi_sa);
            addBubbleItem(R.id.mi_sn, R.string.mi_sn);
            addBubbleItem(R.id.mi_ss, R.string.mi_ss);
            addBubbleItem(R.id.mi_si, R.string.mi_si);
//            addBubbleItem(R.id.mi_sr, R.string.mi_sr);
        }

        EventAction ea = il.getPage().config.menu;
        if(ea.action == GlobalConfig.UNSET) {
            ea = mGlobalConfig.menu;
        }
        addBubbleItemScript(il, null, mode, ea);
    }

    @TargetApi(Build.VERSION_CODES.N_MR1)
    private void showAppShortcuts(ItemView itemView) {
        LauncherApps launcherApps = (LauncherApps) getSystemService(LAUNCHER_APPS_SERVICE);
        if(!launcherApps.hasShortcutHostPermission()) {
            showDialog(DIALOG_LAUNCHER_APPS_NO_HOST_PERMISSION);
            return;
        }

        UserHandle userHandle = Process.myUserHandle();

        ComponentName activity = null;
        final Item item = itemView.getItem();
        if(item instanceof Shortcut) {
            activity = ((Shortcut)item).getIntent().getComponent();
        } else if(item instanceof Widget) {
            final ComponentName cn = ((Widget) item).getComponentName();
            if(cn != null) {
                final List<LauncherActivityInfo> activityList = launcherApps.getActivityList(cn.getPackageName(), userHandle);
                if(activityList.size() > 0) {
                    activity = activityList.get(0).getComponentName();
                }
            }
        }

        List<ShortcutInfo> shortcuts = null;
        if(activity != null) {
            LauncherApps.ShortcutQuery query = new LauncherApps.ShortcutQuery();
            query.setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST | LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC | LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED);
            query.setActivity(activity);
            shortcuts = launcherApps.getShortcuts(query, userHandle);
        }

        if(shortcuts == null || shortcuts.size() == 0) {
            Toast.makeText(this, R.string.as_no_s, Toast.LENGTH_SHORT).show();
        } else {
            openBubbleAppShortcuts(itemView, shortcuts);
        }
    }

    private void moveHandleGrid(ItemView itemView, float fdx, float fdy) {
        Item item = itemView.getItem();
        int dx=(int)fdx;
		int dy=(int)fdy;
		float cw=mEditItemLayout.getCellWidth();
		float ch=mEditItemLayout.getCellHeight();
        final SavedItemGeometry tig = mTrackedItemsGeometry.get(item.getId());
        mTempRect.set(tig.cell);
		switch(mTrackedHandle) {
		case TOP:
			mTempRect.top=(int) ((mTempRect.top*ch+dy+ch/2)/ch);
			if(mTempRect.top>=mTempRect.bottom) mTempRect.top=mTempRect.bottom-1;
			break;
			
		case RIGHT:
			mTempRect.right=(int) ((mTempRect.right*cw+dx+cw/2)/cw);
			if(mTempRect.right<=mTempRect.left) mTempRect.right=mTempRect.left+1;
			break;
			
		case BOTTOM:
			mTempRect.bottom=(int) ((mTempRect.bottom*ch+dy+ch/2)/ch);
			if(mTempRect.bottom<=mTempRect.top) mTempRect.bottom=mTempRect.top+1;
			break;
			
		case LEFT:
			mTempRect.left=(int) ((mTempRect.left*cw+dx+cw/2)/cw);
			if(mTempRect.left>=mTempRect.right) mTempRect.left=mTempRect.right-1;
			break;
		}
        item.getCell().set(mTempRect);
	}
	
	private float moveHandleFree(ItemView itemView, float dx, float dy, boolean master, float angle_or_scale) {
        Item item = itemView.getItem();
        SavedItemGeometry tig = mTrackedItemsGeometry.get(item.getId());
		Matrix m=new Matrix(tig.transform);
        HandleView hv = mEditItemLayout.getHandleView();
        final Rect bounds = tig.bounds;
        if(hv.getMode()==HandleView.Mode.SCALE) {
			float scale = master ? 0 : angle_or_scale;
            float tracked_item_width = bounds.width();
            float tracked_item_height = bounds.height();
            switch(mTrackedHandle) {
			case TOP:
				if(master) scale=(tracked_item_height -dy)/ tracked_item_height;
				m.postScale(1, scale, bounds.left, bounds.bottom);
				break;
				
			case RIGHT:
                if(master) scale=(tracked_item_width +dx)/tracked_item_width;
				m.postScale(scale, 1, bounds.left, bounds.top);
				break;
				
			case BOTTOM:
                if(master) scale=(tracked_item_height +dy)/ tracked_item_height;
				m.postScale(1, scale, bounds.left, bounds.top);
				break;
				
			case LEFT:
                if(master) scale=(tracked_item_width -dx)/tracked_item_width;
				m.postScale(scale, 1, bounds.right, bounds.top);
				break;

			case TOP_LEFT:
                if(master) scale=(tracked_item_height -dy)/ tracked_item_height;
				m.postScale(scale, scale, bounds.right, bounds.bottom);
				break;

			case TOP_RIGHT:
                if(master) scale=(tracked_item_width +dx)/tracked_item_width;
				m.postScale(scale, scale, bounds.left, bounds.bottom);
				break;

			case BOTTOM_LEFT:
                if(master) scale=(tracked_item_height +dy)/ tracked_item_height;
				m.postScale(scale, scale, bounds.right, bounds.top);
				break;

			case BOTTOM_RIGHT:
                if(master) scale=(tracked_item_width +dx)/tracked_item_width;
				m.postScale(scale, scale, bounds.left, bounds.top);
				break;
			}
            if(mSnappingContext != null && master) {
                mSnappingContext.computeSnaps(m);
                if(mSnappingContext.min_dx != Float.MAX_VALUE) {
                    float v = mSnappingContext.min_dx / mSnappingContext.item_layout.getCurrentScale();
                    m.set(tig.transform);
                    float sdx;
                    switch (mTrackedHandle) {
                        case RIGHT:
                            scale=(tracked_item_width+dx+v)/tracked_item_width;
                            m.postScale(scale, 1, bounds.left, bounds.top);
                            break;

                        case LEFT:
                            scale=(tracked_item_width-dx-v)/tracked_item_width;
                            m.postScale(scale, 1, bounds.right, bounds.top);
                            break;

                        case TOP_LEFT:
                            sdx = (tracked_item_height-dy)/tracked_item_height*tracked_item_width - tracked_item_width;
                            scale=(tracked_item_width+sdx-v)/tracked_item_width;
                            m.postScale(scale, scale, bounds.right, bounds.bottom);
                            break;

                        case TOP_RIGHT:
                            scale=(tracked_item_width+dx+v)/tracked_item_width;
                            m.postScale(scale, scale, bounds.left, bounds.bottom);
                            break;

                        case BOTTOM_LEFT:
                            sdx = (tracked_item_height+dy)/tracked_item_height*tracked_item_width - tracked_item_width;
                            scale=(tracked_item_width+sdx-v)/tracked_item_width;
                            m.postScale(scale, scale, bounds.right, bounds.top);
                            break;

                        case BOTTOM_RIGHT:
                            scale=(tracked_item_width +dx+v)/tracked_item_width;
                            m.postScale(scale, scale, bounds.left, bounds.top);
                            break;
                    }
                    item.setTransform(m, false);
                }

                if(mSnappingContext.min_dy != Float.MAX_VALUE) {
                    float v = mSnappingContext.min_dy / mSnappingContext.item_layout.getCurrentScale();
                    m.set(tig.transform);
                    float sdy;
                    switch (mTrackedHandle) {
                        case BOTTOM:
                            scale=(tracked_item_height+dy+v)/tracked_item_height;
                            m.postScale(1, scale, bounds.left, bounds.top);
                            break;

                        case TOP:
                            scale=(tracked_item_height-dy-v)/tracked_item_height;
                            m.postScale(1, scale, bounds.left, bounds.bottom);
                            break;

                        case TOP_LEFT:
                            scale=(tracked_item_height-dy-v)/ tracked_item_height;
                            m.postScale(scale, scale, bounds.right, bounds.bottom);
                            break;

                        case TOP_RIGHT:
                            sdy = (tracked_item_width+dx)/tracked_item_width*tracked_item_height - tracked_item_height;
                            scale=(tracked_item_height+sdy-v)/tracked_item_height;
                            m.postScale(scale, scale, bounds.left, bounds.bottom);
                            break;

                        case BOTTOM_LEFT:
                            scale=(tracked_item_height+dy+v)/ tracked_item_height;
                            m.postScale(scale, scale, bounds.right, bounds.top);
                            break;

                        case BOTTOM_RIGHT:
                            sdy = (tracked_item_width+dx)/tracked_item_width*tracked_item_height - tracked_item_height;
                            scale=(tracked_item_height+sdy+v)/tracked_item_height;
                            m.postScale(scale, scale, bounds.left, bounds.top);
                            break;
                    }
                    item.setTransform(m, false);
                }

                mSnappingContext.applySnaps(m);
            }
            if(master) {
                angle_or_scale = scale;
            }
		} else if(hv.getMode()==HandleView.Mode.ROTATE) {
            float angle;
            final int cx = bounds.centerX();
            final int cy = bounds.centerY();
            if(master) {
                int ax = 0, ay = 0;
                switch (mTrackedHandle) {
                    case TOP: ax = cx; ay = bounds.top; break;
                    case RIGHT: ax = bounds.right; ay = cy; break;
                    case BOTTOM: ax = cx; ay = bounds.bottom; break;
                    case LEFT: ax = bounds.left; ay = cy; break;
                }
                double a = Math.atan2(ay + dy - cy, ax + dx - cx) * 180 / Math.PI;
                switch (mTrackedHandle) {
                    case TOP: a += 90; break;
                    case RIGHT: break;
                    case BOTTOM: a -= 90; break;
                    case LEFT: a -= 180; break;
                }
                angle = Math.round(a / 5) * 5;
                angle_or_scale = angle;
            } else {
                angle = angle_or_scale;
            }
			m.postRotate(angle, cx, cy);
		} else if(hv.getMode()==HandleView.Mode.CONTENT_SIZE) {
            TransformLayout tl = itemView;

			int tx=0;
			int ty=0;
			int width=tig.transformedViewWidth;
			int height=tig.transformedViewHeight;
			switch(mTrackedHandle) {
			case TOP: 
				height-=dy;
				if(height<0) height=0;
				ty=tig.transformedViewHeight-height;
				break;
				
			case RIGHT: 
				width+=dx;
				if(width<0) width=0;
				break;
				
			case BOTTOM: 
				height+=dy;
				if(height<0) height=0;
				break;
				
			case LEFT:
				width-=dx;
				if(width<0) width=0;
				tx=tig.transformedViewWidth-width;
				break;
			}
			
			m.postTranslate(tx, ty);
            item.setViewWidth(width);
            item.setViewHeight(height);

            if(mSnappingContext != null && master) {
                mSnappingContext.computeSnaps(m);

                if(mSnappingContext.min_dx != Float.MAX_VALUE) {
                    float v = mSnappingContext.min_dx / mSnappingContext.item_layout.getCurrentScale();
                    if(mTrackedHandle == Handle.RIGHT) {
                        width += v;
                    } else if(mTrackedHandle == Handle.LEFT) {
                        m.postTranslate(v, 0);
                        width -= v;
                    }
                    item.setViewWidth(width);
                }

                if(mSnappingContext.min_dy != Float.MAX_VALUE) {
                    float v = mSnappingContext.min_dy / mSnappingContext.item_layout.getCurrentScale();
                    if(mTrackedHandle == Handle.BOTTOM) {
                        height += v;
                    } else if(mTrackedHandle == Handle.TOP) {
                        m.postTranslate(0, v);
                        height -= v;
                    }
                    item.setViewHeight(height);
                }

                mSnappingContext.applySnaps(m);
            }
			

			tl.requestLayout();
		}

        item.setTransform(m, false);

        return angle_or_scale;
	}

    protected void saveInitialItemViewGeometry(ItemView itemView) {
        mOriginalItemsGeometry.put(itemView.getItem().getId(), new SavedItemGeometry(itemView));
    }

    private SnappingContext mSnappingContext;


    private void startSnapping(ItemView itemView, int snap_what) {
        mSnappingContext = new SnappingContext(itemView, ViewConfiguration.get(this).getScaledTouchSlop(), snap_what);
    }

    private void stopSnapping() {
        if(mSnappingContext != null) {
            mSnappingContext.stop();
            mSnappingContext = null;
        }
    }

    private void cancelSelectDropFolder() {
        mHandler.removeCallbacks(mSelectDropFolder);
        if(mCandidateDropFolderView != null) {
            mCandidateDropFolderView.setBackgroundColor(0);
            mCandidateDropFolderView = null;
        }
    }

	private void dropItemView(final ItemView droppedItemView) {
        boolean is_embedded_folder = mDropFolderView instanceof EmbeddedFolderView;
        Item droppedItem = droppedItemView.getItem();
        boolean on_grid = droppedItem.getItemConfig().onGrid;


        final SavedItemGeometry oldGeometry = mOriginalItemsGeometry.get(droppedItem.getId());
        if(mDropFolderView == null) {
            if (on_grid) {
                ItemLayout il = droppedItemView.getParentItemLayout();
                getItemViewCell(droppedItemView, mCurrentMoveItemDx, mCurrentMoveItemDy, mTempCoords);
                Rect cell = droppedItem.getCell();
                int ox = cell.left;
                int oy = cell.top;
                cell.offsetTo(mTempCoords[0], mTempCoords[1]);
                if (mSystemConfig.hasSwitch(SystemConfig.SWITCH_HONOUR_PINNED_ITEMS) && droppedItem.getItemConfig().pinMode != ItemConfig.PinMode.NONE) {
                    if (cell.right <= 0 || cell.left * il.getCellWidth() >= il.getWidth() || cell.bottom <= 0 || (cell.top * il.getCellHeight() > il.getHeight())) {
                        cell.offsetTo(ox, oy);
                    }
                }
                droppedItem.notifyCellChanged();
                float anim_dx = (ox - cell.left) * il.getCellWidth() + mCurrentMoveItemDx;
                float anim_dy = (oy - cell.top) * il.getCellHeight() + mCurrentMoveItemDy;
                droppedItemView.animateTranslate(anim_dx, 0, anim_dy, 0, false);

                mUndoStack.storeItemSetCell(droppedItemView, oldGeometry);
            } else {
                mUndoStack.storeItemSetTransform(droppedItemView, oldGeometry);
            }
        } else {
            if(!is_embedded_folder) {
                final Rect dropFolderBounds = mScreen.computeItemViewBounds(mDropFolderView);
                final Rect droppedItemBounds = mScreen.computeItemViewBounds(droppedItemView);
                float dx1, dx2, dy1, dy2;
                if (on_grid) {
                    dx1 = mCurrentMoveItemDx;
                    dx2 = dropFolderBounds.centerX() - droppedItemBounds.centerX();
                    dy1 = mCurrentMoveItemDy;
                    dy2 = dropFolderBounds.centerY() - droppedItemBounds.centerY();
                } else {
                    dx1 = 0;
                    dx2 = dropFolderBounds.centerX() - droppedItemBounds.centerX();
                    dy1 = 0;
                    dy2 = dropFolderBounds.centerY() - droppedItemBounds.centerY();
                }
                droppedItemView.animateTranslate(dx1, dx2, dy1, dy2, true);
            }
        }

        stopTracking(droppedItemView);
	}
	
	private void getItemViewCell(ItemView itemView, float dx, float dy, int[] coords) {
        ItemLayout il = itemView.getParentItemLayout();
        Item item = itemView.getItem();
        float x= item.getCell().left+dx/ il.getCellWidth();
		float y= item.getCell().top+dy/ il.getCellHeight();
		coords[0]=Math.round(x);
		coords[1]=Math.round(y);
	}

    protected void unselectAllItems() {
        if(mEditMode) {
            for(int i=0; i<mEditItemLayout.getChildCount(); i++) {
                View v= mEditItemLayout.getChildAt(i);
                if(v instanceof ItemView) {
                    ItemView itemView = (ItemView) v;
                    if (itemView.isSelected()) {
                        itemView.setSelected(false);
                    }
                }
            }
        }
    }

    public ArrayList<Item> getSelectedItems() {
        ArrayList<ItemView> itemViews = getSelectedItemViews();
        int count = itemViews.size();
        ArrayList<Item> items = new ArrayList<>(count);
        for(int n=0; n<count; n++) {
            items.add(itemViews.get(n).getItem());
        }
        return items;
    }

    public ArrayList<ItemView> getSelectedItemViews() {
        if(mEditMode) {
            ArrayList<ItemView> itemViews = mEditItemLayout.getSelectedItems();
            final ArrayList<Item> pageItems = mEditItemLayout.getPage().items;


            // sort by z-order desc, this is needed when z-order operations with undo are involved (for instance moving items in a panel)
            Collections.sort(itemViews, new Comparator<ItemView>() {
                @Override
                public int compare(ItemView lhs, ItemView rhs) {
                    final int li = pageItems.indexOf(lhs.getItem());
                    final int ri = pageItems.indexOf(rhs.getItem());
                    if(ri < li) return -1;
                    if(li > ri) return 1;
                    return 0;
                }
            });

            return itemViews;
        } else {
            return new ArrayList<>(0);
        }
    }

    // returns the list of selected items or the bubble item, depending on whether we are in edit mode or not
    protected ArrayList<Item> getActionItems() {
        if(mEditMode) {
            return getSelectedItems();
        } else {
            ArrayList<Item> item = new ArrayList<>(1);
            if(mBubbleItemView != null) {
                item.add(mBubbleItemView.getItem());
            }
            return item;
        }
    }

    protected ArrayList<ItemView> getActionItemViews() {
        if(mEditMode) {
            return getSelectedItemViews();
        } else {
            ArrayList<ItemView> itemViews = new ArrayList<>(1);
            if(mBubbleItemView != null) {
                itemViews.add(mBubbleItemView);
            }
            return itemViews;
        }
    }

    public SelectionState getSelectionState() {
        if(mEditMode) {
            return new SelectionState(mEditItemLayout.getMasterSelectedItem(), getSelectedItemViews(), mEditItemLayout.getHandleView().getMode());
        } else {
            return new SelectionState(null, new ArrayList<ItemView>(0), null);
        }
    }

    public void setSelectionState(SelectionState state) {
        if(mEditMode) {
            // unselect items that are no more in the new selection state list
            for (ItemView itemView : getSelectedItemViews()) {
                int id = itemView.getItem().getId();
                boolean found = false;
                for (int sid : state.selectedItemsIds) {
                    if(id == sid) {
                        found = true;
                        break;
                    }
                }
                if(!found) {
                    itemView.setSelected(false);
                }
            }

            Item masterSelectedItem = mEditPage.findItemById(state.masterSelectedItemId);
            mEditItemLayout.setMasterSelectedItem(masterSelectedItem);
            for (int id : state.selectedItemsIds) {
                mEditItemLayout.getItemView(id).setSelected(true);
            }
            if (masterSelectedItem == null) {
                mEditItemLayout.hideHandleView();
            } else {
                configureHandlesForItemView(mEditItemLayout.getItemView(masterSelectedItem), state.handleMode, true);
            }
        }
    }

	private void startTracking(ItemView itemView) {
        ItemLayout il = itemView.getParentItemLayout();
        Item item = itemView.getItem();
		if(item.getPage().config.allowDualPosition) {
			item.differentiatePosition(mScreen.getResourcesOrientation());
		}

        mTrackedItemsGeometry.put(item.getId(), new SavedItemGeometry(itemView));

		il.trackItemView(itemView);
	}

    private void stopTracking(ItemView itemView) {
        mTrackedItemsGeometry.remove(itemView.getItem().getId());
        itemView.getParentItemLayout().untrackItemView(itemView);
    }

    private void rearrangeItems(Page page) {
        PageConfig c = page.config;
        boolean horizontal = c.scrollingDirection==PageConfig.ScrollingDirection.X;
        int x_max, y_max;
        if(mScreen.getResourcesOrientation() == Configuration.ORIENTATION_LANDSCAPE) {
            x_max = c.gridLColumnNum;
            y_max = c.gridLRowNum;
        } else {
            x_max = c.gridPColumnNum;
            y_max = c.gridPRowNum;
        }

        int min=Integer.MAX_VALUE;
        int max=Integer.MIN_VALUE;
        SparseArray<ArrayList<Item>> items = new SparseArray<>();
        for(Item i : page.items) {
            Class<? extends Item> cl = i.getClass();
            if(cl != Shortcut.class && cl != Folder.class) continue;
            if(!i.getItemConfig().onGrid) continue;
            if(!i.isVisible()) continue;
            if(i.getItemConfig().pinMode!= ItemConfig.PinMode.NONE) continue;
            Rect cell = i.getCell();
            int key;
            if(horizontal) {
                key = (cell.left/x_max) * (x_max*y_max) + cell.left%x_max + cell.top*x_max;
            } else {
                key = cell.left+cell.top*x_max;
            }
            ArrayList<Item> slot = items.get(key);
            if(slot==null) {
                slot = new ArrayList<Item>();
                items.put(key, slot);
            }
            slot.add(i);
            if(key>max) max=key;
            if(key<min) min=key;
        }

        int x=0, y=0, px=0;
        for(int key=min; key<=max; key++) {
            ArrayList<Item> slot = items.get(key);
            if(slot != null) {
                for(Item i : slot) {
                    i.setCellT(null);
                    i.getCell().set(new Rect(x, y, x+1, y+1));

                    if(horizontal) {
                        px++;
                        x++;
                        if(px==x_max) {
                            px=0;
                            x-=x_max;
                            y++;
                            if(y==y_max) {
                                y=0;
                                x+=x_max;
                            }
                        }
                    } else {
                        x++;
                        if(x==x_max) {
                            x=0;
                            y++;
                        }
                    }
                }
            }
        }
    }

    private void updateGeometryBox() {
        if(mGeometryBox != null && mGeometryBox.getVisibility() == View.VISIBLE) {
            Item masterSelectedItem = mEditItemLayout.getMasterSelectedItem();
            boolean e = false;
            if(masterSelectedItem != null) {
                e = true;
                if(masterSelectedItem.getClass() == StopPoint.class) {
                    if(mGeometryMode != Item.GEOMETRY_CTRL_POSITION) {
                        mGeometryMode = Item.GEOMETRY_CTRL_POSITION;
                    }
                } else if (masterSelectedItem.getItemConfig().onGrid) {
                    if (mGeometryMode != Item.GEOMETRY_CTRL_POSITION && mGeometryMode != Item.GEOMETRY_CTRL_SIZE) {
                        mGeometryMode = Item.GEOMETRY_CTRL_SIZE;
                    }
                }
            }
            boolean is_rotate = mGeometryMode== Item.GEOMETRY_CTRL_ROTATE;
            mGeometryEdit1.setText(e ? getGeometryEditLabel(masterSelectedItem, R.id.gb_e1) : "");
            mGeometryEdit2.setText(e ? getGeometryEditLabel(masterSelectedItem, R.id.gb_e2) : "");
            mGeometryEdit2.setVisibility(is_rotate ? View.GONE : View.VISIBLE);
            mGeometryEdit1.setEnabled(e);
            mGeometryEdit2.setEnabled(e);
            findViewById(R.id.gb_hm).setEnabled(e);
            findViewById(R.id.gb_hp).setEnabled(e);
            findViewById(R.id.gb_m).setEnabled(e);

            View v;
            v = findViewById(R.id.gb_vm);
            v.setEnabled(e);
            v.setVisibility(is_rotate ? View.INVISIBLE : View.VISIBLE);
            v = findViewById(R.id.gb_vp);
            v.setEnabled(e);
            v.setVisibility(is_rotate ? View.INVISIBLE : View.VISIBLE);

            final boolean zorder_enable= masterSelectedItem !=null;
            mGeometryBox.findViewById(R.id.move_bottom).setEnabled(zorder_enable);
            mGeometryBox.findViewById(R.id.move_down).setEnabled(zorder_enable);
            mGeometryBox.findViewById(R.id.move_up).setEnabled(zorder_enable);
            mGeometryBox.findViewById(R.id.move_top).setEnabled(zorder_enable);
        }
    }

    private String getGeometryEditLabel(Item item, int geometry_edit) {
        return getGeometryEditTitle(geometry_edit) + "\n" + getGeometryEditText(item, geometry_edit);
    }

    private String getGeometryEditTitle(int geometry_edit) {
        int label_res;
        boolean e1 = geometry_edit == R.id.gb_e1;
        switch (mGeometryMode) {
            case Item.GEOMETRY_CTRL_POSITION: label_res = e1 ? R.string.gb_l : R.string.gb_t; break;
            case Item.GEOMETRY_CTRL_SIZE: label_res = e1 ? R.string.gb_w : R.string.gb_h; break;
            case Item.GEOMETRY_CTRL_ROTATE: label_res = R.string.gb_a; break;
            case Item.GEOMETRY_CTRL_SCALE: label_res = e1 ? R.string.gb_sx : R.string.gb_sy; break;
            case Item.GEOMETRY_CTRL_SKEW: label_res = e1 ? R.string.gb_kx : R.string.gb_ky; break;
            default: return "";
        }

        return getString(label_res);
    }

    private String getGeometryEditText(Item item, int geometry_edit) {
        return sGeometryEditValueFormat.format(getGeometryEditValue(item, geometry_edit));
    }

    private float getGeometryEditValue(Item item, int geometry_edit) {
        float[] values = new float[9];

        boolean on_grid = item.getItemConfig().onGrid;
        Matrix m = item.getTransform();
        m.getValues(values);
        float value = 0;


        boolean e1 = geometry_edit == R.id.gb_e1;
        switch (mGeometryMode) {
            case Item.GEOMETRY_CTRL_POSITION:
                if(on_grid) {
                    Rect r = item.getCell();
                    value = e1 ? r.left : r.top;
                } else {
                    RectF r = Utils.getTransformedItemBoxforMatrix(item, m);
                    value = e1 ? r.left : r.top;
                }
                break;
            case Item.GEOMETRY_CTRL_SIZE:
                if(on_grid) {
                    Rect r = item.getCell();
                    value = e1 ? r.width() : r.height();
                } else {
                    value = e1 ? item.getViewWidth() : item.getViewHeight();
                }
                break;
            case Item.GEOMETRY_CTRL_ROTATE: value = Utils.getRotateForMatrix(m); break;
            case Item.GEOMETRY_CTRL_SCALE: value = Utils.getScaleforMatrix(m, e1); break;
            case Item.GEOMETRY_CTRL_SKEW: value = Utils.getSkewforMatrix(m, e1); break; //values[e1 ? Matrix.MSKEW_X : Matrix.MSKEW_Y]; break;
        }

        return value;
    }

    private void showGeometryBox() {
        if(mGeometryBox == null) {
            mGeometryBox = findViewById(R.id.gb);
            mGeometryBox.setOnTouchListener(this);
            mGeometryEdit1 = (Button)mGeometryBox.findViewById(R.id.gb_e1);
            mGeometryEdit1.setOnClickListener(this);
            mGeometryEdit2 = (Button)mGeometryBox.findViewById(R.id.gb_e2);
            mGeometryEdit2.setOnClickListener(this);
            mGeometryBox.findViewById(R.id.gb_hm).setOnClickListener(this);
            mGeometryBox.findViewById(R.id.gb_hm).setOnLongClickListener(this);
            mGeometryBox.findViewById(R.id.gb_hp).setOnClickListener(this);
            mGeometryBox.findViewById(R.id.gb_hp).setOnLongClickListener(this);
            mGeometryBox.findViewById(R.id.gb_vm).setOnClickListener(this);
            mGeometryBox.findViewById(R.id.gb_vm).setOnLongClickListener(this);
            mGeometryBox.findViewById(R.id.gb_vp).setOnClickListener(this);
            mGeometryBox.findViewById(R.id.gb_vp).setOnLongClickListener(this);
            mGeometryBox.findViewById(R.id.gb_m).setOnClickListener(this);
            mGeometryMode = Item.GEOMETRY_CTRL_SIZE;

            mGeometryBox.findViewById(R.id.move_bottom).setOnClickListener(this);
            mGeometryBox.findViewById(R.id.move_down).setOnClickListener(this);
            mGeometryBox.findViewById(R.id.move_up).setOnClickListener(this);
            mGeometryBox.findViewById(R.id.move_top).setOnClickListener(this);
        }
        mGeometryBox.setVisibility(View.VISIBLE);
        adjustGeometryBoxPosition();
        updateGeometryBox();
    }

    private void hideGeometryBox() {
        if(mGeometryBox != null) {
            mGeometryBox.setVisibility(View.GONE);
        }
    }

    private boolean isGeometryBoxVisible() {
        return mGeometryBox != null && mGeometryBox.getVisibility() == View.VISIBLE;
    }

    public void updateGeometryValue(Item item, int what, float value_x, float value_y, boolean fast) {
        if(item.getClass() == StopPoint.class && what != Item.GEOMETRY_CTRL_POSITION) {
            return;
        }

        item.updateGeometryValue(what, value_x, value_y, fast);
        // this should not be needed anymore
//        if(fast) {
//            ItemLayout il = mScreen.getItemLayoutForItem(item);
//            if(il != null) {
//                il.layoutItemView(item);
//            }
//        }
        item.getPage().setModified();
    }

    private void incrementCurrentGeometryValue(Item item, int view_id) {
        if(item.getClass() == StopPoint.class && mGeometryMode != Item.GEOMETRY_CTRL_POSITION) {
            return;
        }

        boolean on_grid = item.getItemConfig().onGrid;
        Matrix m = item.getTransform();
        Rect cell = item.getCell();
        float x=0, y=0;
        switch (mGeometryMode) {
            case Item.GEOMETRY_CTRL_POSITION:
                RectF r = Utils.getTransformedItemBoxforMatrix(item, m);
                if(on_grid) {
                    x = cell.left;
                    y = cell.top;
                } else {
                    x = r.left;
                    y = r.top;
                }
                switch(view_id) {
                    case R.id.gb_hm: x--; break;
                    case R.id.gb_hp: x++; break;
                    case R.id.gb_vm: y--; break;
                    case R.id.gb_vp: y++; break;
                }
                break;

            case Item.GEOMETRY_CTRL_SIZE:
                if(on_grid) {
                    x = cell.width();
                    y = cell.height();
                } else {
                    x = item.getViewWidth();
                    y = item.getViewHeight();
                }
                switch(view_id) {
                    case R.id.gb_hm: x--; break;
                    case R.id.gb_hp: x++; break;
                    case R.id.gb_vm: y--; break;
                    case R.id.gb_vp: y++; break;
                }
                break;

            case Item.GEOMETRY_CTRL_ROTATE:
                x = Utils.getRotateForMatrix(m);
                switch(view_id) {
                    case R.id.gb_hm: x--; break;
                    case R.id.gb_hp: x++; break;
                }
                break;

            case Item.GEOMETRY_CTRL_SCALE:
                x = Utils.getScaleforMatrix(m, true);
                y = Utils.getScaleforMatrix(m, false);
                switch(view_id) {
                    case R.id.gb_hm: x-=0.1f; break;
                    case R.id.gb_hp: x+=0.1f; break;
                    case R.id.gb_vm: y-=0.1f; break;
                    case R.id.gb_vp: y+=0.1f; break;
                }
                break;

            case Item.GEOMETRY_CTRL_SKEW:
        }
        updateGeometryValue(item, mGeometryMode, x, y, false);
        updateGeometryBox();
        mEditItemLayout.requestLayout();
    }

    private void storeUndoForGeometryBoxChange(ItemView itemView) {
        Item item = itemView.getItem();
        final SavedItemGeometry oldGeometry = mOriginalItemsGeometry.get(item.getId());
        if(item.getItemConfig().onGrid) {
            mUndoStack.storeItemSetCell(itemView, oldGeometry);
        } else {
            if(mGeometryMode == Item.GEOMETRY_CTRL_SIZE) {
                mUndoStack.storeItemSetViewSize(itemView, oldGeometry);
            } else {
                mUndoStack.storeItemSetTransform(itemView, oldGeometry);
            }
        }
    }


    public void adjustGeometryBoxPosition() {
        if(mGeometryBox != null) {
            boolean fix_position = false;
            FrameLayout.LayoutParams old_lp = (FrameLayout.LayoutParams) mGeometryBox.getLayoutParams();
            FrameLayout.LayoutParams new_lp = new FrameLayout.LayoutParams(old_lp.width, old_lp.height);
            new_lp.leftMargin = old_lp.leftMargin;
            new_lp.topMargin = old_lp.topMargin;
            new_lp.gravity = Gravity.NO_GRAVITY;
            View contentView = mScreen.getContentView();
            int width = contentView.getWidth();
            int height = contentView.getHeight();
            if(mGeometryBox.getRight()>width) {
                fix_position = true;
                new_lp.leftMargin = width - old_lp.width;
            }
            if(mGeometryBox.getBottom()>height) {
                fix_position = true;
                new_lp.topMargin = height - old_lp.height;
            }
            if(fix_position) {
                mGeometryBox.setLayoutParams(new_lp);
                mGeometryBox.requestLayout();
            }
        }
    }

    private int[] mTmpInt = new int[2];
    private int mInitialDownX;
    private int mInitialDownY;
    private int mInitialViewLeft;
    private int mInitialViewTop;
    private boolean mDraggingHandle;
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        // used to drag boxes
        boolean is_geometry_box = v == mGeometryBox;
        if (is_geometry_box || v == mEditPropertiesHandle) {
            v.getLocationOnScreen(mTmpInt);
            mTmpInt[0] += (int) event.getX();
            mTmpInt[1] += (int) event.getY();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mInitialDownX = mTmpInt[0];
                    mInitialDownY = mTmpInt[1];

                    if(is_geometry_box) {
                        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) v.getLayoutParams();
                        mInitialViewLeft = layoutParams.leftMargin;
                        mInitialViewTop = layoutParams.topMargin;
                    } else {
                        mInitialViewTop = mEditPropertiesContainer.getLayoutParams().height;
                    }
                    break;

                case MotionEvent.ACTION_MOVE:
                    int x = mInitialViewLeft + mTmpInt[0] - mInitialDownX;
                    if(x < 0) x = 0;
                    int y = mInitialViewTop + mTmpInt[1] - mInitialDownY;
                    if(y < 0) y = 0;
                    if(is_geometry_box) {
                        FrameLayout.LayoutParams fl = new FrameLayout.LayoutParams(mGeometryBox.getWidth(), mGeometryBox.getHeight());
                        fl.leftMargin = x;
                        fl.topMargin = y;
                        fl.gravity = Gravity.NO_GRAVITY;
                        mGeometryBox.setLayoutParams(fl);
                        mGeometryBox.requestLayout();
                    } else {
                        ViewGroup.LayoutParams lp = mEditPropertiesContainer.getLayoutParams();
                        int delta = mEditPropertiesOnTop ? mTmpInt[1] - mInitialDownY : mInitialDownY - mTmpInt[1];
                        if(mDraggingHandle || Math.abs(delta) > ViewConfiguration.get(this).getScaledTouchSlop()) {
                            lp.height = checkEditPropertiesHeight(mInitialViewTop + delta);
                            mEditPropertiesContainer.requestLayout();
                            mDraggingHandle = true;
                        }
                    }
//                    mContentView.requestLayout();
                    break;

                case MotionEvent.ACTION_UP:
                    if(!is_geometry_box) {
                        if(mDraggingHandle) {
                            mDraggingHandle = false;
                        } else {
                            Toast.makeText(this, mEditPropertiesTitle.getText(), Toast.LENGTH_LONG).show();
                        }
                    }
                    break;
            }

            return true;
        }
        return false;
    }

    private int checkEditPropertiesHeight(int height) {
        if(height <= 0) {
            // special values, don't interpret this as a number of pixels
            return height;
        }

        int max_height = mEditControlsView.getHeight() - mEditControlsView.getPaddingTop() - mEditControlsView.getPaddingBottom() - mEditBarsHeight;
        if (height > max_height) {
            height = max_height;
        } else {
            int min_height = mEditPropertiesHandle.getHeight() + mEditPropertiesContainer.getPaddingTop() + mEditPropertiesContainer.getPaddingBottom();
            if (height < min_height) {
                height = min_height;
            }
        }
        mSystemConfig.editBoxPropHeight = height;
        return height;
    }

    @Override
    public void onFirstTimeInitStart(boolean is_import) {
        showDialog(is_import ? DIALOG_IMPORT_LL : DIALOG_FIRST_USE);
        mScreen.getDesktopView().findViewById(R.id.setup_progress).setVisibility(View.VISIBLE);
    }

    @Override
    public void onFirstTimeInitEnd(boolean success, boolean was_import, final Item all_apps_item) {
        mSetupInProgress = false;
        Toast.makeText(Dashboard.this, success ? R.string.init_ok : R.string.init_ko, Toast.LENGTH_LONG).show();
        mScreen.getDesktopView().findViewById(R.id.setup_progress).setVisibility(View.GONE);
        setPagerPage(mGlobalConfig.homeScreen, Screen.PAGE_DIRECTION_HINT_AUTO);

        if(!was_import) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        openBubble(BUBBLE_MODE_HINT_APP_DRAWER, mScreen.getCurrentRootItemLayout().getItemView(all_apps_item));
                    } catch (Exception e) {
                        // pass
                    }
                }
            }, 500);
        }
    }

    private void startPackageDetails(String pkg) {
        // 2.3 style
        Intent intent=new Intent("android.settings.APPLICATION_DETAILS_SETTINGS");
        intent.setData(Uri.parse("package:"+pkg));
        if(!Utils.startActivitySafely(this, intent, 0)) {
            // <=2.2 style
            intent=new Intent();
            intent.setComponent(ComponentName.unflattenFromString("com.android.settings/.InstalledAppDetails"));
            intent.putExtra("pkg", pkg); // 2.2
            intent.putExtra("com.android.settings.ApplicationPkgName", pkg); // 1.5
            Utils.startActivitySafely(this, intent, R.string.start_activity_error);
        }
    }
//
//
//    protected void customize(int page) {
//        // for folder, go directly to the workspace settings screen, instead of the general setting screen
//        PhoneUtils.startSettings(this, page, !Page.isFolder(page));
//    }


    private OnClickListener mEditBarClick = new OnClickListener() {
        @Override
        public void onClick(View v) {
            closeBubble();
//            mLastTouchedAddX = Utils.POSITION_AUTO;
//            mLastTouchedAddY = Utils.POSITION_AUTO;
//            mLightningEngine.setTouchPosition(mLastTouchedAddX, mLastTouchedAddY);
            switch(v.getId()) {
                case R.id.eb_undo:
                case R.id.eb_redo:
                    final boolean undo = v.getId() == R.id.eb_undo;
                    if(mUndoStack.willDeleteWidget(undo)) {
                        showConfirmWidgetDeletionDialog(new Runnable() {
                            @Override
                            public void run() {
                                if(undo) mUndoStack.undo(); else mUndoStack.redo();
                            }
                        });
                    } else {
                        if(undo) mUndoStack.undo(); else mUndoStack.redo();
                    }
                    break;
                case R.id.eb_edit: toggleEditActionBox(SystemConfig.EDIT_BOX_PROPERTIES); break;
                case R.id.eb_action: toggleEditActionBox(SystemConfig.EDIT_BOX_ACTION); break;
                case R.id.eb_pos: toggleEditActionBox(SystemConfig.EDIT_BOX_POSITION); break;
                case R.id.eb_clone: case R.id.bbl_clone: menuActionCloneItem(); break;
                case R.id.eb_add: case R.id.bbl_add: menuActionAddItem(); break;
                case R.id.eb_rm: case R.id.bbl_rm: menuActionConfirmRemoveItem(); break;
                case R.id.eb_ms: v.setSelected(menuActionToggleMultiSelection()); break;
                case R.id.eb_sh: v.setSelected(menuActionToggleDisplayInvisibleItems()); break;
                case R.id.eb_snap: v.setSelected(menuActionToggleSnap()); break;
                case R.id.eb_prop: v.setSelected(toggleEditPropertiesBox()); break;
                case R.id.eb_ge: v.setSelected(menuActionToggleGeometryBox()); break;
                case R.id.eb_lock: menuActionLockUnlock(); break;
                case R.id.eb_gs: menuActionSettingsGlobal(); break;
                case R.id.eb_cs: menuActionSettingsContainer(true); break;
                case R.id.eb_h: mHierarchyScreen.show(null); break;
                case R.id.eb_hider: toggleEditBars(); break;
                case R.id.eb_cstyle: menuActionCopyStyle(); break;
                case R.id.eb_pstyle: menuActionPasteStyle(); break;
                case R.id.edit_prop_mode: mEditPropertiesBox.showGotoPageDialog(); break;
                case R.id.bbl_settings: menuActionSettingsContainer(false); break;
                case R.id.bbl_edit: menuActionEdit(); break;
                case R.id.edit_prop_pos: setEditPropertiesPosition(!mEditPropertiesOnTop); break;
                case R.id.eb_hp:
                    boolean honor = !mSystemConfig.hasSwitch(SystemConfig.SWITCH_HONOUR_PINNED_ITEMS);
                    mEditItemLayout.setHonourPinnedItems(honor);
                    mSystemConfig.setSwitch(SystemConfig.SWITCH_HONOUR_PINNED_ITEMS, honor);
                    v.setSelected(honor);
                    break;
            }
        }
    };

    private OnLongClickListener mEditBarLongClick = new OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            CharSequence label;
            int label_res = 0;
            Item masterSelectedItem = mEditMode ? mEditItemLayout.getMasterSelectedItem() : null;
            switch(v.getId()) {
                case R.id.eb_lock: label_res = mGlobalConfig.itemLongTap.action==GlobalConfig.NOTHING ? R.string.mi_ul : R.string.mi_l; break;
                case R.id.eb_cs:
                case R.id.bbl_settings:
                    if(masterSelectedItem != null && masterSelectedItem instanceof Folder) {
                        label_res = masterSelectedItem.getClass() == EmbeddedFolder.class ? R.string.mc_ef : R.string.menu_customize_folder;
                    } else if(mScreen.getTargetItemLayout().getPage().isFolder()) {
                        boolean is_embedded_folder = mScreen.getTargetItemLayout().getOpenerItemView() instanceof EmbeddedFolderView;
                        label_res = is_embedded_folder ? R.string.mc_ef : R.string.menu_customize_folder;
                    } else if(mScreen.getTargetItemLayout().getPage().id == Page.APP_DRAWER_PAGE) {
                        label_res = R.string.ads;
                    } else {
                        label_res = R.string.ds;
                    }
                    break;

                case R.id.bbl_edit:
                    if(masterSelectedItem == null) {
                        ItemLayout il = mScreen.getTargetOrTopmostItemLayout();
                        label_res = il.getEditMode() ? R.string.mi_exel : R.string.menu_objects_layout;
                    } else {
                        label_res = R.string.mi_customize;
                    }
                    break;

                case R.id.eb_hider:
                    if(mEditBarsVisible) {
                        boolean content_zoomed = !mSystemConfig.hasSwitch(SystemConfig.SWITCH_CONTENT_ZOOMED);
                        animateEditBars(content_zoomed ? true : false, false, false, true);
                        mSystemConfig.setSwitch(SystemConfig.SWITCH_CONTENT_ZOOMED, content_zoomed);
                    }
                    break;
            }
            if(label_res == 0) {
                label = v.getContentDescription();
            } else {
                label = getString(label_res);
            }
            if(label != null) {
                int[] location = new int[2];
                v.getLocationOnScreen(location);
                Toast toast = Toast.makeText(Dashboard.this, label, Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.TOP|Gravity.LEFT, location[0], location[1]);
                toast.show();
            }
            return true;
        }
    };

    private void setupEditControlsIfNeeded() {
        if(mEditControlsView.findViewById(R.id.eb_top) == null) {
            getLayoutInflater().inflate(R.layout.edit_controls, mEditControlsView);
        }
    }

    private void showEditBars(boolean with_hider) {
        if(mEditBarRight == null) {
            setupEditControlsIfNeeded();
            mEditBarRight = mEditControlsView.findViewById(R.id.eb_top);
            mEditBarBottom = mEditControlsView.findViewById(R.id.eb_bottom);

            computeEditBarsSize();

            setupEditBar(mEditBarRight);
            setupEditBar(mEditBarBottom);

            mEditBarRight.findViewById(R.id.eb_undo).setContentDescription(getString(R.string.eb_u));
            mEditBarRight.findViewById(R.id.eb_redo).setContentDescription(getString(R.string.eb_r));
            mEditBarRight.findViewById(R.id.eb_edit).setContentDescription(getString(R.string.mi_customize));
            mEditBarRight.findViewById(R.id.eb_action).setContentDescription(getString(R.string.mi_actions));
            mEditBarRight.findViewById(R.id.eb_pos).setContentDescription(getString(R.string.mi_position));
            mEditBarRight.findViewById(R.id.eb_clone).setContentDescription(getString(R.string.eb_pr));
            mEditBarRight.findViewById(R.id.eb_add).setContentDescription(getString(R.string.menu_add));
            mEditBarRight.findViewById(R.id.eb_rm).setContentDescription(getString(R.string.mi_remove));
            mEditBarRight.findViewById(R.id.eb_pstyle).setContentDescription(getString(R.string.st_p));
            mEditBarRight.findViewById(R.id.eb_cstyle).setContentDescription(getString(R.string.st_c));
            mEditBarBottom.findViewById(R.id.eb_ms).setContentDescription(getString(R.string.eb_ms));
            mEditBarBottom.findViewById(R.id.eb_sh).setContentDescription(getString(R.string.eb_sh));
            mEditBarBottom.findViewById(R.id.eb_snap).setContentDescription(getString(R.string.eb_snap));
            mEditBarBottom.findViewById(R.id.eb_prop).setContentDescription(getString(R.string.eb_prop));
            mEditBarBottom.findViewById(R.id.eb_ge).setContentDescription(getString(R.string.mi_geometry));
            mEditBarBottom.findViewById(R.id.eb_gs).setContentDescription(getString(R.string.menu_customize));
            mEditBarBottom.findViewById(R.id.eb_hp).setContentDescription(getString(R.string.gc_hpie_t));
            mEditBarBottom.findViewById(R.id.eb_cs).setContentDescription(getString(R.string.mi_es_settings));
            mEditBarBottom.findViewById(R.id.eb_cs).setContentDescription(getString(R.string.mi_h));
            mEditBarBottom.findViewById(R.id.eb_lock).setContentDescription(getString(R.string.mi_l));
            mEditBarBottom.findViewById(R.id.eb_h).setContentDescription(getString(R.string.an_ohs));
        }
        if(mEditBarHider == null) {
            setupEditBarHider();
        }
        updateEditBarsOrientation();
        if(!mEditBarsVisible) {
            mEditBarRight.setVisibility(View.VISIBLE);
            mEditBarBottom.setVisibility(View.VISIBLE);
            if (with_hider) {
                mEditBarHider.setVisibility(View.VISIBLE);
            }
            animateEditBars(true, with_hider, true, mSystemConfig.hasSwitch(SystemConfig.SWITCH_CONTENT_ZOOMED));
        }
//        if(mSystemConfig.hasSwitch(SystemConfig.SWITCH_PROPERTIES_BOX)) {
//            showEditPropertiesBox();
//        }
        if(mSystemConfig.editBoxMode != SystemConfig.EDIT_BOX_NONE) {
            showEditActionBox();
        }
        updateEditBars();
    }

    private void hideEditBars(boolean hide_hider) {
        if(mEditBarsVisible) {
            animateEditBars(false, hide_hider, true, mSystemConfig.hasSwitch(SystemConfig.SWITCH_CONTENT_ZOOMED));
            hideEditActionBox();
            hideEditPropertiesBox();
        }
    }

    private void toggleEditBars() {
        if(mEditBarsVisible) {
            hideEditBars(false);
        } else {
            showEditBars(false);
        }
        mEditBarHider.setArrowDirection(!mEditBarsVisible);
        mSystemConfig.setSwitch(SystemConfig.SWITCH_EDIT_BARS, mEditBarsVisible);
    }

    private void updateEditBarsOrientation() {
        computeEditBarsSize();

        FrameLayout.LayoutParams lp;

        if(mEditBarsVisible) {
            lp = (FrameLayout.LayoutParams) mEditBarRight.getLayoutParams();
            lp.width = mEditBarsWidth;
            lp.bottomMargin = mEditBarsHeight;
            lp = (FrameLayout.LayoutParams) mEditBarBottom.getLayoutParams();
            lp.height = mEditBarsHeight;
            lp.rightMargin = mEditBarsWidth;
            mEditBarRight.requestLayout();
            mEditBarBottom.requestLayout();
        }

        if(mEditActionBox != null) {
            lp = (FrameLayout.LayoutParams) mEditActionBox.getLayoutParams();
            lp.rightMargin = mEditBarsWidth;
            mEditActionBox.requestLayout();
            setupEditActionBoxAnimations();
        }
        if(mEditPropertiesContainer != null) {
            lp = (FrameLayout.LayoutParams) mEditPropertiesContainer.getLayoutParams();
            lp.rightMargin = mEditBarsWidth;
            lp.bottomMargin = mEditBarsHeight;
            mEditPropertiesContainer.requestLayout();
            updateEditPropertiesPosition();
        }

        lp = (FrameLayout.LayoutParams) mEditBarHider.getLayoutParams();
        lp.width = mEditBarsWidth;
        lp.height = mEditBarsHeight;
        mEditBarHider.requestLayout();

        if(mEditBarsVisible && mSystemConfig.hasSwitch(SystemConfig.SWITCH_CONTENT_ZOOMED) && Build.VERSION.SDK_INT >= 11) {
            View desktopView = mScreen.getDesktopView();
            desktopView.setScaleX(mEditBarsScale);
            desktopView.setScaleY(mEditBarsScale);
        }
    }

    private void animateEditBars(boolean show, boolean with_hider, boolean animate_bars, boolean animate_zoom) {
        mAnimateEditBarHider = with_hider;
        mAnimateBars = animate_bars;
        mAnimateZoom = animate_zoom;
        mAnimateEditBarStart = AnimationUtils.currentAnimationTimeMillis();
        if(animate_bars) {
            mEditBarsVisible = show;
        }
        mHandler.removeCallbacks(mAnimateEditBarRunnable);
        mAnimateEditBarRunnable.run();
    }

    private boolean mAnimateBars;
    private boolean mAnimateZoom;
    private boolean mAnimateEditBarHider;
    private long mAnimateEditBarStart;
    private static final long ANIMATE_EDIT_BAR_DURATION = 200;
    private Runnable mAnimateEditBarRunnable = new Runnable() {
        @Override
        public void run() {
            long delta = AnimationUtils.currentAnimationTimeMillis() - mAnimateEditBarStart;
            if(delta> ANIMATE_EDIT_BAR_DURATION) {
                delta = ANIMATE_EDIT_BAR_DURATION;
            }
            float s = delta/(float) ANIMATE_EDIT_BAR_DURATION;

            View desktopView = mScreen.getDesktopView();
            int w = desktopView.getWidth();
            int h = desktopView.getHeight();
            int size = getResources().getDimensionPixelSize(R.dimen.eb_bar_size);
            float size_anim;
            if((mAnimateBars && mEditBarsVisible) || (!mAnimateBars && mAnimateZoom && mSystemConfig.hasSwitch(SystemConfig.SWITCH_CONTENT_ZOOMED))) {
                size_anim =  size*s;
            } else {
                size_anim = size*(1-s);

            }
            float sx = (w-size_anim)/(float)w;
            float sy = (h-size_anim)/(float)h;
            int bw, bh;
            if(sx<sy) {
                bw = Math.round(size_anim);
                bh = (int) Math.ceil(h-sx*h);
                sy = sx;
            } else {
                bw = (int) Math.ceil(w-sy*w);
                bh = Math.round(size_anim);
                sx = sy;
            }

            if(mAnimateBars) {
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mEditBarRight.getLayoutParams();
                lp.width = bw;
                lp.bottomMargin = bh;
                lp = (FrameLayout.LayoutParams) mEditBarBottom.getLayoutParams();
                lp.height = bh;
                lp.rightMargin = bw;
                mEditBarRight.requestLayout();
                mEditBarBottom.requestLayout();
                if (mAnimateEditBarHider) {
                    lp = (FrameLayout.LayoutParams) mEditBarHider.getLayoutParams();
                    lp.width = bw;
                    lp.height = bh;
                    mEditBarHider.requestLayout();
                }
            }

            if(mAnimateZoom && Build.VERSION.SDK_INT >= 11) {
                desktopView.setScaleX(sx);
                desktopView.setScaleY(sy);
            }

            if(s<1) {
                mHandler.post(mAnimateEditBarRunnable);
            } else {
                if(mAnimateBars && bw==0 && bh==0) {
                    mEditBarRight.setVisibility(View.GONE);
                    mEditBarBottom.setVisibility(View.GONE);
                    if (mAnimateEditBarHider) {
                        mEditBarHider.setVisibility(View.GONE);
                    }
                }
            }
        }
    };

    private void showEditBarsHider() {
        if(mEditBarHider == null) {
            setupEditBarHider();
        }
        mEditBarHider.setVisibility(View.VISIBLE);

        int eb_bar_size = getResources().getDimensionPixelSize(R.dimen.eb_bar_size);
        View desktopView = mScreen.getDesktopView();
        int w = desktopView.getWidth();
        int h = desktopView.getHeight();
        float sx = (w-eb_bar_size)/(float)w;
        float sy = (h-eb_bar_size)/(float)h;
        int bw, bh;
        if(sx<sy) {
            bw = eb_bar_size;
            bh = (int) Math.ceil(h-sx*h);
        } else {
            bw = (int) Math.ceil(w-sy*w);
            bh = eb_bar_size;
        }

        ViewGroup.LayoutParams lp = mEditBarHider.getLayoutParams();
        lp.width = bw;
        lp.height = bh;
        mEditBarHider.requestLayout();
        mEditBarHider.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
    }

    private void hideEditBarsHider() {
        mEditBarHider.setVisibility(View.GONE);
        mEditBarHider.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out));
    }



    private void setupEditBar(View bar) {
        ViewGroup vg = (ViewGroup) bar;
        Typeface typeface = LLApp.get().getIconsTypeface();
        for(int n=vg.getChildCount()-1; n>=0; n--) {
            View v = vg.getChildAt(n);
            if(v instanceof TextView) {
                ((TextView)v).setTypeface(typeface);
                v.setOnClickListener(mEditBarClick);
                v.setOnLongClickListener(mEditBarLongClick);
            }
        }
    }

    private void setupEditBarHider() {
        setupEditControlsIfNeeded();
        computeEditBarsSize();
        mEditBarHider = (EditBarHiderView) mEditControlsView.findViewById(R.id.eb_hider);
        mEditBarHider.setOnClickListener(mEditBarClick);
        mEditBarHider.setOnLongClickListener(mEditBarLongClick);
        mEditBarHider.setContentDescription(getString(R.string.eb_bh));
    }

    private void updateEditBars() {
        if(mEditBarRight != null) {
            Item masterSelectedItem = mEditItemLayout.getMasterSelectedItem();
            boolean is_app_drawer = getClass() == AppDrawerX.class;
            boolean has_selected_item = masterSelectedItem != null;
            mEditBarRight.findViewById(R.id.eb_undo).setEnabled(mUndoStack.canUndo());
            mEditBarRight.findViewById(R.id.eb_redo).setEnabled(mUndoStack.canRedo());
            mEditBarRight.findViewById(R.id.eb_clone).setEnabled(has_selected_item && !is_app_drawer);
            mEditBarRight.findViewById(R.id.eb_rm).setEnabled(has_selected_item && (!is_app_drawer || masterSelectedItem.getClass()==Folder.class || masterSelectedItem.getClass()==PageIndicator.class));
            mEditBarRight.findViewById(R.id.eb_cstyle).setEnabled(has_selected_item);
            mEditBarRight.findViewById(R.id.eb_pstyle).setEnabled(has_selected_item);
            mEditBarBottom.findViewById(R.id.eb_hp).setSelected(mSystemConfig.hasSwitch(SystemConfig.SWITCH_HONOUR_PINNED_ITEMS));
            mEditBarBottom.findViewById(R.id.eb_ms).setSelected(mSystemConfig.hasSwitch(SystemConfig.SWITCH_MULTI_SELECTION));
            mEditBarBottom.findViewById(R.id.eb_sh).setSelected(mSystemConfig.hasSwitch(SystemConfig.SWITCH_DISPLAY_INVISIBLE_ITEMS));
            mEditBarBottom.findViewById(R.id.eb_snap).setSelected(mSystemConfig.hasSwitch(SystemConfig.SWITCH_SNAP));
            mEditBarBottom.findViewById(R.id.eb_prop).setSelected(isEditPropertiesBoxVisible());
            mEditBarBottom.findViewById(R.id.eb_ge).setSelected(isGeometryBoxVisible());
            ((TextView) mEditBarBottom.findViewById(R.id.eb_lock)).setText(mGlobalConfig.itemLongTap.action == GlobalConfig.NOTHING ? "<" : "b");
        }
    }

    private void computeEditBarsSize() {
        int eb_bar_size = getResources().getDimensionPixelSize(R.dimen.eb_bar_size);
        View desktopView = mScreen.getDesktopView();
        int w = desktopView.getWidth();
        int h = desktopView.getHeight();
        float sx = (w-eb_bar_size)/(float)w;
        float sy = (h-eb_bar_size)/(float)h;
        if(sx<sy) {
            mEditBarsWidth = eb_bar_size;
            mEditBarsHeight = (int) Math.ceil(h-sx*h);
            mEditBarsScale = sx;
        } else {
            mEditBarsWidth = (int) Math.ceil(w-sy*w);
            mEditBarsHeight = eb_bar_size;
            mEditBarsScale = sy;
        }
    }

    private void showEditActionBox() {
        if(mEditActionBox == null) {
            setupEditActionBox();
        }

        int mode = mSystemConfig.editBoxMode;
        mEditBarRight.findViewById(R.id.eb_edit).setSelected(mode == SystemConfig.EDIT_BOX_PROPERTIES);
        mEditBarRight.findViewById(R.id.eb_pos).setSelected(mode == SystemConfig.EDIT_BOX_POSITION);
        mEditBarRight.findViewById(R.id.eb_action).setSelected(mode == SystemConfig.EDIT_BOX_ACTION);

        if(mEditActionBox.getVisibility() != View.VISIBLE) {
            mEditActionBox.setVisibility(View.VISIBLE);
            mEditActionBox.startAnimation(mEditActionBoxAnimIn);
            updateEditPropertiesPosition();
        }

        updateEditActionBox();
    }

    private void hideEditActionBox() {
        if(mEditActionBox != null && mEditActionBox.getVisibility() == View.VISIBLE) {
            mEditBarRight.findViewById(R.id.eb_edit).setSelected(false);
            mEditBarRight.findViewById(R.id.eb_pos).setSelected(false);
            mEditBarRight.findViewById(R.id.eb_action).setSelected(false);
            mEditActionBox.setVisibility(View.GONE);
            mEditActionBox.startAnimation(mEditActionBoxAnimOut);
            updateEditPropertiesPosition();
        }
    }

    private boolean toggleEditActionBox(int mode) {
        int current_mode = mSystemConfig.editBoxMode;
        if(current_mode == SystemConfig.EDIT_BOX_NONE || current_mode != mode) {
            mSystemConfig.editBoxMode = mode;
            showEditActionBox();
        } else {
            mSystemConfig.editBoxMode = SystemConfig.EDIT_BOX_NONE;
            hideEditActionBox();
        }
        return mode != SystemConfig.EDIT_BOX_NONE;
    }

    private void updateEditActionBox() {
        if(mEditActionBox == null || mEditActionBox.getVisibility() != View.VISIBLE) {
            return;
        }

        mEditActionBox.removeAllViews();

        boolean is_app_drawer = getClass() == AppDrawerX.class;

        ItemView masterSelectedItemView = mEditItemLayout.getMasterSelectedItemView();
        Item masterSelectedItem = masterSelectedItemView == null ? null : masterSelectedItemView.getItem();
        Class<?> item_class = masterSelectedItem == null ? null : masterSelectedItem.getClass();

        if (mSystemConfig.editBoxMode == SystemConfig.EDIT_BOX_PROPERTIES) {
            boolean is_shortcut=(item_class==Shortcut.class || item_class==Folder.class);
            boolean is_widget = item_class==Widget.class;
            boolean is_embedded_folder = masterSelectedItemView != null && masterSelectedItemView.getClass() == EmbeddedFolderView.class;
            if(is_shortcut) {
                addEditActionBoxButton(R.id.mi_edit_icon, R.string.mi_edit_icon);
                addEditActionBoxButton(R.id.mi_edit_label, R.string.mi_edit_label);
            }
            if((item_class==Shortcut.class || item_class==DynamicText.class) && !is_app_drawer) {
                addEditActionBoxButton(R.id.mi_edit_launch_action, R.string.mi_eda);
            }
            if(is_widget) {
                addEditActionBoxButton(R.id.mi_pick_widget, R.string.mi_pick_widget);
                if(((Widget) masterSelectedItem).hasConfigurationScreen()) addEditActionBoxButton(R.id.mi_widget_options, R.string.mi_widget_options);
            }
            if(is_embedded_folder) {
                addEditActionBoxButton(R.id.mi_ef_edit_layout, R.string.menu_objects_layout);
            }
            if(is_app_drawer && is_shortcut) {
                addEditActionBoxButton(R.id.mi_hide_unhide, R.string.mi_hide_unhide);
            }
        } else if (mSystemConfig.editBoxMode == SystemConfig.EDIT_BOX_POSITION) {
            if(masterSelectedItem != null) {
                boolean is_stop_point = item_class == StopPoint.class;
                Page page = masterSelectedItem.getPage();
                boolean is_folder_page = page.isFolder();
                boolean is_in_embedded_folder = masterSelectedItemView.getParentItemLayout().getOpenerItemView() instanceof EmbeddedFolderView;
                ItemConfig ic = masterSelectedItem.getItemConfig();
                if(!is_app_drawer) {
                    addEditActionBoxButton(R.id.mi_lm, ic.onGrid ? R.string.mi_lmg : R.string.mi_lmf);
                }
                if (!is_stop_point) {
                    addEditActionBoxButton(R.id.mi_pin, ic.pinMode != ItemConfig.PinMode.NONE ? R.string.mi_unpin : R.string.mi_pin);
                }
                if(!is_app_drawer) {
                    if (is_folder_page && page.id != Page.USER_MENU_PAGE)
                        addEditActionBoxButton(R.id.mi_move_out_of_folder, is_in_embedded_folder ? R.string.mi_mop : R.string.mi_move_out_of_folder);
                    addEditActionBoxButton(R.id.mi_move_to_screen, R.string.mi_move_to_screen);
                    addEditActionBoxButton(R.id.mi_copy_to_screen, R.string.mi_copy_to_screen);
                }
            }
        } else if (mSystemConfig.editBoxMode == SystemConfig.EDIT_BOX_ACTION) {
            if(masterSelectedItem != null) {

                String pkg = Utils.getPackageNameForItem(masterSelectedItem);
                if(pkg != null) {
                    addEditActionBoxButton(R.id.mi_app_details, R.string.mi_app_details);
                    addEditActionBoxButton(R.id.mi_app_store, R.string.mi_app_store);
                    addEditActionBoxButton(R.id.mi_kill, pkg.equals(getPackageName()) ? R.string.an_re : R.string.mi_kill);
                    addEditActionBoxButton(R.id.mi_uninstall, R.string.mi_uninstall);
                }

                if(mGlobalConfig.runScripts) {
                    addEditActionBoxButton(R.id.mi_s, R.string.mi_s);
                }
                if(getClass()!=AppDrawerX.class) {
                    if(false) {
                        if (item_class == Folder.class) {
                            addEditActionBoxButton(R.id.mi_cfp, R.string.mi_cfp);
                        } else if (item_class == EmbeddedFolder.class) {
                            addEditActionBoxButton(R.id.mi_cpf, R.string.mi_cpf);
                        }
                    }
                }
            }
        }
        if(mEditActionBox.getChildCount() == 0) {
            TextView empty = new TextView(this);
            empty.setText(R.string.nois);
//            empty.setTextSize(getResources().getDimensionPixelSize(R.dimen.eb_text_size));
            empty.setTextColor(getResources().getColor(R.color.eb_btn_text));
            empty.setGravity(Gravity.CENTER);
            empty.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            mEditActionBox.addView(empty);
        }
        mEditActionBox.setWeightSum(mEditActionBox.getChildCount());
    }


    private void setupEditActionBox() {
        mEditActionBox = (LinearLayout) findViewById(R.id.edit_action_box);
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mEditActionBox.getLayoutParams();
        lp.rightMargin = mEditBarsWidth;
        mEditActionBox.requestLayout();

        setupEditActionBoxAnimations();
    }

    private Animation mEditActionBoxAnimIn;
    private Animation mEditActionBoxAnimOut;
    private void setupEditActionBoxAnimations() {
        mEditActionBoxAnimIn = new TranslateAnimation(0, 0, -mEditBarsHeight, 0);
        mEditActionBoxAnimIn.setDuration(200);
        mEditActionBoxAnimOut = new TranslateAnimation(0, 0, 0, -mEditBarsHeight);
        mEditActionBoxAnimOut.setDuration(200);
    }

    private Button addEditActionBoxButton(int id, int label_res_id) {
        Button button = (Button) getLayoutInflater().inflate(R.layout.edit_box_item, mEditActionBox, false);

        button.setId(id);
        button.setText(label_res_id);
        button.setOnClickListener(this);

        mEditActionBox.addView(button);

        return button;
    }

    private void showEditPropertiesBox() {
        if(mEditPropertiesContainer == null) {
            setupEditPropertiesBox();
        }

        if(!mEditBarsVisible) {
            showEditBars(true);
        }
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mEditPropertiesContainer.getLayoutParams();
        if(lp.height == 0) {
            lp.height = getResources().getDisplayMetrics().heightPixels / 3;
        }
        if(mEditPropertiesContainer.getVisibility() != View.VISIBLE) {
            updateEditPropertiesBox(mEditPage, mEditItemLayout.getMasterSelectedItem());
            updateEditPropertiesMode();

            mEditPropertiesContainer.setVisibility(View.VISIBLE);
            mEditPropertiesContainer.startAnimation(mEditPropertiesBoxAnimIn);
        }
    }

    private void hideEditPropertiesBox() {
        if(mEditPropertiesContainer != null && mEditPropertiesContainer.getVisibility() == View.VISIBLE) {
            mEditPropertiesContainer.setVisibility(View.GONE);
            mEditPropertiesContainer.startAnimation(mEditPropertiesBoxAnimOut);
        }
    }

    private boolean isEditPropertiesBoxVisible() {
        return mEditPropertiesContainer != null && mEditPropertiesContainer.getVisibility() == View.VISIBLE;
    }

    private boolean toggleEditPropertiesBox() {
        boolean show = isEditPropertiesBoxVisible();
        if(show) {
            hideEditPropertiesBox();
        } else {
            showEditPropertiesBox();
        }
        mSystemConfig.setSwitch(SystemConfig.SWITCH_PROPERTIES_BOX, !show);
        return !show;
    }

    private void setupEditPropertiesBox() {
        mEditPropertiesContainer = (ViewGroup) findViewById(R.id.edit_prop_cont);
        mEditPropertiesHandle = mEditPropertiesContainer.findViewById(R.id.edit_prop_handle);
        mEditPropertiesBox = (CustomizeItemView) mEditPropertiesContainer.findViewById(R.id.edit_prop_box);
        mEditPropertiesTitle = (TextView) mEditPropertiesContainer.findViewById(R.id.edit_prop_title);
        mEditPropertiesMode = (Button) mEditPropertiesContainer.findViewById(R.id.edit_prop_mode);
        mEditPropertiesModePrevious = (TextView) mEditPropertiesContainer.findViewById(R.id.edit_prop_mode_p);
        mEditPropertiesModeNext = (TextView) mEditPropertiesContainer.findViewById(R.id.edit_prop_mode_n);
        Button position_toggle = (Button) mEditPropertiesContainer.findViewById(R.id.edit_prop_pos);

        Typeface icon_typeface = LLApp.get().getIconsTypeface();
        position_toggle.setTypeface(icon_typeface);
        mEditPropertiesModePrevious.setTypeface(icon_typeface);
        mEditPropertiesModeNext.setTypeface(icon_typeface);
        position_toggle.setOnClickListener(mEditBarClick);
        mEditPropertiesHandle.setOnTouchListener(this);
        mEditPropertiesMode.setOnClickListener(mEditBarClick);

        mEditPropertiesBox.init();
        mEditPropertiesBox.setOnPageChangeListener(new MyViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                // pass
            }

            @Override
            public void onPageSelected(int position) {
                updateEditPropertiesMode();
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                // pass
            }
        });

        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mEditPropertiesContainer.getLayoutParams();
        lp.rightMargin = mEditBarsWidth;
        lp.bottomMargin = mEditBarsHeight;
        lp.height = mSystemConfig.editBoxPropHeight;
        mEditPropertiesContainer.requestLayout();

        updateEditPropertiesPosition();
    }

    private void updateEditPropertiesBox(Page page, Item item) {
        if(mEditPropertiesBox != null) {
            mEditPropertiesBox.setTarget(mEditItemLayout, page, item);

            String handle_text;
            if(item == null) {
                handle_text = Utils.formatItemLayoutName(mEditItemLayout);
            } else {
                handle_text = Utils.formatItemName(item, 20, getSelectedItemViews().size());
            }
            mEditPropertiesTitle.setText(handle_text);
            updateEditPropertiesMode();

            if(item != null) {
                Rect bounds = mScreen.computeItemViewBounds(mEditItemLayout.getItemView(item));
                if(bounds != null) {
                    setEditPropertiesPosition(bounds.centerY() > mScreen.getDesktopView().getHeight() / 2);
                }
            }
        }
    }

    private void updateEditPropertiesMode() {
        mEditPropertiesModePrevious.setVisibility(mEditPropertiesBox.isFirst() ? View.INVISIBLE : View.VISIBLE);
        mEditPropertiesMode.setText(mEditPropertiesBox.getCurrentPageName());
        mEditPropertiesModeNext.setVisibility(mEditPropertiesBox.isLast() ? View.INVISIBLE : View.VISIBLE);
    }

    private void setEditPropertiesPosition(boolean top) {
        mEditPropertiesOnTop = top;
        updateEditPropertiesPosition();
    }

    private Animation mEditPropertiesBoxAnimIn;
    private Animation mEditPropertiesBoxAnimOut;
    private void updateEditPropertiesPosition() {
        if(mEditPropertiesContainer != null) {
            // hackish
            View separator = mEditPropertiesContainer.getChildAt(1);
            mEditPropertiesContainer.removeAllViews();
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mEditPropertiesContainer.getLayoutParams();
            if (mEditPropertiesOnTop) {
                mEditPropertiesContainer.addView(mEditPropertiesBox);
                mEditPropertiesContainer.addView(separator);
                mEditPropertiesContainer.addView(mEditPropertiesHandle);
                lp.gravity = Gravity.TOP;
            } else {
                mEditPropertiesContainer.addView(mEditPropertiesHandle);
                mEditPropertiesContainer.addView(separator);
                mEditPropertiesContainer.addView(mEditPropertiesBox);
                lp.gravity = Gravity.BOTTOM;
            }

            lp.height = checkEditPropertiesHeight(lp.height);

//            lp.topMargin = mEditActionBox != null && mEditActionBox.getVisibility() == View.VISIBLE && mEditPropertiesOnTop ? mEditBarsSize : 0;
            mEditPropertiesContainer.requestLayout();
        }

        int anim_direction = mEditPropertiesOnTop ? -1 : 1;

        mEditPropertiesBoxAnimIn = new TranslateAnimation(
                TranslateAnimation.RELATIVE_TO_PARENT, 0, TranslateAnimation.RELATIVE_TO_PARENT, 0,
                TranslateAnimation.RELATIVE_TO_SELF, anim_direction, TranslateAnimation.RELATIVE_TO_SELF, 0);
        mEditPropertiesBoxAnimOut = new TranslateAnimation(
                TranslateAnimation.RELATIVE_TO_PARENT, 0, TranslateAnimation.RELATIVE_TO_PARENT, 0,
                TranslateAnimation.RELATIVE_TO_SELF, 0, TranslateAnimation.RELATIVE_TO_SELF, anim_direction);
        mEditPropertiesBoxAnimIn.setDuration(ANIMATE_EDIT_BAR_DURATION);
        mEditPropertiesBoxAnimOut.setDuration(ANIMATE_EDIT_BAR_DURATION);
    }


    private void updateEditControls() {
        updateGeometryBox();
        updateEditBars();
        updateEditActionBox();
        updateEditPropertiesBox(mEditPage, mEditItemLayout.getMasterSelectedItem());
    }

    public UndoStack getUndoStack() {
        return mUndoStack;
    }

    @Override
    public void onUndoStackStateChanged(boolean can_undo, boolean can_redo) {
        if(mEditBarRight != null) {
            mEditBarRight.findViewById(R.id.eb_undo).setEnabled(can_undo);
            mEditBarRight.findViewById(R.id.eb_redo).setEnabled(can_redo);
        }
    }

    @Override
    public void onUndoStackItemChanged(Item item) {
        item.notifyChanged();
    }

    @Override
    public void onUndoStackPageChanged(Page page) {
        if(mEditPropertiesBox != null && (mEditPropertiesBox.getPage() == page)) {
            mEditPropertiesBox.updatePreferences();
        }
    }

    @Override
    public void onHierarchyScreenItemEdit(ContainerPath path, Item item) {
        ItemLayout il = mScreen.prepareItemLayoutForPath(path);
        if(item instanceof Folder && item.getId() == path.getLast()) {
            // edit the opener item, not the container itself
            ItemView opener = il.getOpenerItemView();
            mScreen.ensureItemViewVisible(opener, false);
            il = opener.getParentItemLayout();
        } else {
            if (item == null) {
                mScreen.ensureItemLayoutVisible(il, false);
            } else {
                mScreen.ensureItemViewVisible(il.getItemView(item), false);
            }
        }
        if(item != null) {
            enterEditMode(il, item);
            if(!mSystemConfig.hasSwitch(SystemConfig.SWITCH_MULTI_SELECTION)) {
                unselectAllItems();
            }
            ItemView itemView = mEditItemLayout.getItemView(item);
            itemView.setSelected(true);
            mEditItemLayout.setMasterSelectedItem(item);
            configureHandlesForItemView(itemView, HandleView.Mode.CONTENT_SIZE, true);
        } else {
            enterEditMode(il, null);
        }

        mHierarchyScreen.hide();

        if(item != null) {
            final ItemView itemView = mEditItemLayout.getItemView(item);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    openBubble(BUBBLE_MODE_ITEM_EM, itemView);
                }
            }, 400);
        }
    }

    @Override
    public void onHierarchyScreenContainerSettings(ContainerPath path) {
        PhoneUtils.startSettings(this, path, false);
    }

    @Override
    public void onHierarchyScreenItemMoved(HierarchyScreen.HierarchyItem which, HierarchyScreen.HierarchyItem before, HierarchyScreen.HierarchyItem after) {
        if(which.item != null && before != null && after != null && before != after && before.item == null && after.item == null) {
            // move of an item between 2 desktops, nothing to do
            return;
        }

        LightningEngine engine = mHierarchyScreen.getLightningEngine();

        if(which.item == null) {
            if(after == null || after.item == null) {
                // move desktop
                GlobalConfig gc = engine.getGlobalConfig();

                // convert to array list
                ArrayList<Integer> screensOrder = new ArrayList<>();
                for (Integer i : gc.screensOrder) {
                    screensOrder.add(i);
                }
                List<String> screensName = new ArrayList<>(Arrays.asList(gc.screensNames));


                // move
                int pos = screensOrder.indexOf(which.page);
                int page = screensOrder.remove(pos);
                int newPos = after == null ? screensOrder.size() : screensOrder.indexOf(after.page);
                screensOrder.add(newPos, page);
                String name = screensName.remove(pos);
                screensName.add(newPos, name);

                // convert back to array
                for (int i = 0; i < screensOrder.size(); i++) {
                    gc.screensOrder[i] = screensOrder.get(i);
                }
                screensName.toArray(gc.screensNames);

                engine.notifyGlobalConfigChanged();

                mHierarchyScreen.refresh();
            }

            return;
        }

        if(before == null) {
            // move before a desktop
            return;
        }

        if(after == null) {
            // move at the last place of the parent container of before
            moveAtLastPagePlace(which.item, before.getParentContainerId());
        } else if (before == after) {
            // move in another container (or at the end of the current container)
            moveAtLastPagePlace(which.item, before.getContainerId());
//        } else if (before.getParentContainerId() == after.getParentContainerId()) {
//            // move in that container (if needed) between before and after
//            Item item;
//            Page p = mLightningEngine.getPageById(before.getParentContainerId());
//            if(which.getParentContainerId() != before.getParentContainerId()) {
//                int new_id = Utils.moveItem(this, which.item.getId(), before.getParentContainerId(), Utils.POSITION_AUTO, Utils.POSITION_AUTO, 1, Item.NONE);
//                item = p.findItemById(new_id);
//            } else {
//                item = which.item;
//            }
//
//            int currentIndex = p.items.indexOf(item);
//            int afterIndex = p.items.indexOf(after.item);
//            p.setItemZIndex(item, currentIndex>afterIndex ? afterIndex : afterIndex - 1);
        } else if(before.isContainer() && before.getContainerId() == after.getParentContainerId()) {
            // move at the first place of the container of before
            Item item;
            Page p = engine.getOrLoadPage(before.getContainerId());
            if(which.getParentContainerId() != before.getContainerId()) {
                item = Utils.moveItem(which.item, p, Utils.POSITION_AUTO, Utils.POSITION_AUTO, 1, Item.NO_ID);
            } else {
                item = which.item;
            }

            p.setItemZIndex(item, 0);
        } else if(after.item == null) {
            // between an item and a desktop
            // move at the last place of the parent container of before
            moveAtLastPagePlace(which.item, before.getParentContainerId());
        } else {
            // move in the parent container of after, before after
            Item item;
            Page p = engine.getOrLoadPage(after.getParentContainerId());
            if(which.getParentContainerId() != after.getParentContainerId()) {
                item = Utils.moveItem(which.item, p, Utils.POSITION_AUTO, Utils.POSITION_AUTO, 1, Item.NO_ID);
            } else {
                item = which.item;
            }

            int currentIndex = p.items.indexOf(item);
            int afterIndex = p.items.indexOf(after.item);
            p.setItemZIndex(item, currentIndex>afterIndex ? afterIndex : afterIndex - 1);
        }

        mHierarchyScreen.refresh();
    }

    @Override
    public int[] onHierarchyScreenGetRootPages() {
        return mGlobalConfig.screensOrder;
    }

    private void moveAtLastPagePlace(Item item, int toPage) {
        Page p = item.getPage().getEngine().getOrLoadPage(toPage);
        if(item.getPage() == p) {
            // move at the last place
            p.setItemZIndex(item, p.items.size()-1);
        } else {
            // move in the other container and at the last place
            Utils.moveItem(item, p, Utils.POSITION_AUTO, Utils.POSITION_AUTO, 1, Item.NO_ID);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if(mABOnCreateOptionsMenu != null) {
            Object result = mEngine.getScriptExecutor().runFunction(mABOnCreateOptionsMenu, new Object[]{menu}, false, true);
            if(result instanceof Boolean) {
                return (Boolean) result;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(mABOnOptionsItemSelected != null) {
            Object result = mEngine.getScriptExecutor().runFunction(mABOnOptionsItemSelected, new Object[]{item}, true, true);
            if(result instanceof Boolean) {
                return (Boolean) result;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    protected class DashboardScreen extends Screen {
        private boolean mClosingBubble;

        public DashboardScreen(Context context, int content_view) {
            super(context, content_view);
        }

        @Override
        public ScreenIdentity getIdentity() {
            return ScreenIdentity.HOME;
        }

        @Override
        protected boolean isAndroidActionBarSupported() {
            // action bar introduced in API 11 but support in LL only starting at 21 because of additional options (see configureSystemBarsPadding)
            return Build.VERSION.SDK_INT >= 21;
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        @Override
        public void showAndroidActionBar(Function onCreateOptionsMenu, Function onOptionsItemSelected) {
            if(isAndroidActionBarSupported() && !mIsAndroidActionBarDisplayed) {
                getActionBar().show();
                mIsAndroidActionBarDisplayed = true;
                mABOnCreateOptionsMenu = onCreateOptionsMenu;
                mABOnOptionsItemSelected = onOptionsItemSelected;
                configureSystemBarsPadding(getCurrentRootPage().config);
                invalidateOptionsMenu();
            }
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        @Override
        public void hideAndroidActionBar() {
            if(isAndroidActionBarSupported() && mIsAndroidActionBarDisplayed) {
                getActionBar().hide();
                mIsAndroidActionBarDisplayed = false;
                mABOnCreateOptionsMenu = null;
                mABOnOptionsItemSelected = null;
                configureSystemBarsPadding(getCurrentRootPage().config);
            }
        }

        @Override
        public float[] translateItemLayoutCoordsIntoScreenCoords(ItemLayout il, float x, float y) {
            float[] coords = super.translateItemLayoutCoordsIntoScreenCoords(il, x, y);
            if(mIsAndroidActionBarDisplayed) {
                SystemBarTintManager.SystemBarConfig config = getSystemBarTintManager().getConfig();
                coords[1] -= (config.getActionBarHeight() + config.getStatusBarHeight());
            }
            return coords;
        }

        @Override
        protected Resources getRealResources() {
            return Dashboard.this.getRealResources();
        }

        @Override
        public void onPageModified(Page page) {
            super.onPageModified(page);

            if(page == getCurrentRootPage()) {
                configureActivity(page);
            }
        }

        @Override
        public void onPageRemoved(Page page) {
            if(page.isDashboard() && mViewAnimator != null) {
                for(int l = mViewAnimator.getChildCount()-1;l>=0; l--) {
                    ItemLayout il = (ItemLayout) mViewAnimator.getChildAt(l);
                    if(il.getPage() == page) {
                        if(il.isResumed()) {
                            il.pause();
                        }
                        mViewAnimator.removeViewAt(l);
                    }
                }
                while(mNavigationStack.removeElement(page.id));
            }

            super.onPageRemoved(page);
        }

        @Override
        public void onPageItemChanged(Page page, Item item) {
            super.onPageItemChanged(page, item);

            if(isEditPropertiesBoxVisible() && (mEditPropertiesBox.getPage() == page && mEditPropertiesBox.getItem() == item)) {
                mEditPropertiesBox.updatePreferences();
            }

            updateGeometryAndActionBoxesIfNeeded(item);
        }

        @Override
        public void onItemTransformChanged(Item item, boolean fast) {
            super.onItemTransformChanged(item, fast);
            updateGeometryAndActionBoxesIfNeeded(item);
        }

        @Override
        public void onItemCellChanged(Item item) {
            super.onItemCellChanged(item);
            updateGeometryAndActionBoxesIfNeeded(item);
        }

        private void updateGeometryAndActionBoxesIfNeeded(Item item) {
            if(mEditMode && mEditItemLayout.getMasterSelectedItem() == item) {
                if(mEditActionBox != null && mEditActionBox.getVisibility() == View.VISIBLE) {
                    updateEditActionBox();
                }
                if (isGeometryBoxVisible()) {
                    updateGeometryBox();
                }
            }
        }

        @Override
        public void displayScriptPickImageDialog(ScriptExecutor scriptExecutor) {
            mScriptExecutorForCallback = scriptExecutor;
            startActivityForResult(new Intent(mContext, ImagePicker.class), REQUEST_SCRIPT_PICK_IMAGE);
        }

        @Override
        public void displayCropPickImageDialog(final ScriptExecutor scriptExecutor, final ImageBitmap image, final boolean full_size) {
            if(image == null) {
                return;
            }
            if(!checkPermissions(
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    new int[] { R.string.pr_r5},
                    REQUEST_PERMISSION_BASE)) {
                return;
            }

            final File file = Utils.getTmpImageFile();

            final ProgressDialog d = new ProgressDialog(Dashboard.this);
            d.setMessage(getString(R.string.please_wait));
            d.setCancelable(false);
            d.show();
            new AsyncTask<Void,Void,Boolean>() {

                @Override
                protected Boolean doInBackground(Void... params) {
                    FileOutputStream fos = null;
                    try {
                        fos = new FileOutputStream(file);
                        image.getBitmap().compress(Bitmap.CompressFormat.PNG, 100, fos);
                        return true;
                    } catch (IOException e) {
                        return false;
                    } finally {
                        if(fos != null) try { fos.close(); } catch (IOException e) {}
                    }
                }

                @Override
                protected void onPostExecute(Boolean success) {
                    if(success) {
                        mScriptExecutorForCallback = scriptExecutor;
                        Intent intent = new Intent(Dashboard.this, ImageCropper.class);
                        intent.putExtra(ImageCropper.INTENT_EXTRA_IMAGE, file.getAbsolutePath());
                        intent.putExtra(ImageCropper.INTENT_EXTRA_FULL_SIZE, full_size);
                        startActivityForResult(intent, REQUEST_SCRIPT_CROP_IMAGE);
                    } else {
                        scriptExecutor.setImageForCropImage(null);
                    }
                    d.dismiss();
                }
            }.execute((Void)null);
        }

        @Override
        public boolean startActivityForResultScript(ScriptExecutor scriptExecutor, Intent intent, int receiver_script_id, String token) {
            try {
                mScriptExecutorForCallback = scriptExecutor;
                mActivityResultScriptId = receiver_script_id;
                mActivityResultScriptToken = token;
                startActivityForResult(intent, REQUEST_FOR_SCRIPT);
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        @Override
        public ItemLayout prepareFirstItemLayout(int page) {
            if(page == Page.APP_DRAWER_PAGE) {
                // special case when opening a folder from the app drawer in the desktop
                return getCurrentRootItemLayout();
            } else {
                return super.prepareFirstItemLayout(page);
            }
        }

        @Override
        public ItemLayout loadRootItemLayout(int page, boolean reset_navigation_history, boolean displayImmediately, boolean animate) {
            if(!Page.isDashboard(page)) return null;

            int hint;
            if(displayImmediately) {
                hint = animate ? PAGE_DIRECTION_HINT_AUTO : PAGE_DIRECTION_HINT_NO_ANIMATION;
            } else {
                hint = PAGE_DIRECTION_HINT_DONT_MOVE;
            }
            ItemLayout itemLayout = setPagerPage(page, hint);
            if(reset_navigation_history) {
                mNavigationStack.clear();
            }

            return itemLayout;
        }

        @Override
        public ItemLayout getCurrentRootItemLayout() {
            if(mViewAnimator.getChildCount() == 0) {
                return null;
            } else {
                return (ItemLayout) mViewAnimator.getChildAt(mViewAnimator.getDisplayedChild());
            }
        }

        @Override
        public void onShortcutLaunchError(Shortcut shortcut) {
            mNotValidShortcut = shortcut;
            try { removeDialog(DIALOG_APP_NOT_INSTALLED); } catch(Exception e2) {}
            showDialog(DIALOG_APP_NOT_INSTALLED);
        }

        @Override
        public void onLauncherAppsNoHostPermission(ItemView itemView) {
            showDialog(DIALOG_LAUNCHER_APPS_NO_HOST_PERMISSION);
        }

        @Override
        public void onMissingPermissions(String[] permissions, int[] msgs) {
            checkPermissions(permissions, msgs, REQUEST_PERMISSION_BASE);
        }

        @Override
        public void setActivePage(int page) {
            LLApp.get().setActiveDashboardPage(page);
        }

        @Override
        public void showAddItemDialog(ItemLayout il) {
            setTargetItemLayout(il);
            menuActionAddItem();
        }

        @Override
        public void onItemLayoutPressed() {
            super.onItemLayoutPressed();

            mClosingBubble = closeBubble();
        }

        @Override
        public void onItemLayoutClicked(ItemLayout item_layout, int x, int y) {
            if(mEditMode && mEditItemLayout == item_layout) {
                setLastTouchEventForItemLayout(item_layout, x, y);
                if(!mClosingBubble) {
                    unselectAllItems();
                }
            } else {
                super.onItemLayoutClicked(item_layout, x, y);
            }
        }

        @Override
        public void onItemLayoutDoubleClicked(ItemLayout item_layout, int x, int y) {
            if(mEditMode && mEditItemLayout == item_layout) {
                setLastTouchEventForItemLayout(item_layout, x, y);
                zoomInOrOut(mEditItemLayout);
            } else {
                super.onItemLayoutDoubleClicked(item_layout, x, y);
            }
        }

        @Override
        public void onItemLayoutLongClicked(ItemLayout item_layout, int x, int y) {
            if(mEditMode && mEditItemLayout == item_layout) {
                setLastTouchEventForItemLayout(item_layout, x, y);
                mEditItemLayout.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                unselectAllItems();
                openBubble(BUBBLE_MODE_LIGHTNING_MENU_EM);
            } else {
                super.onItemLayoutLongClicked(item_layout, x, y);
            }
        }

        @Override
        public void onItemLayoutSwipeLeft(ItemLayout item_layout) {
            if(!mEditMode) {
                super.onItemLayoutSwipeLeft(item_layout);
            }
        }

        @Override
        public void onItemLayoutSwipeRight(ItemLayout item_layout) {
            if(!mEditMode) {
                super.onItemLayoutSwipeRight(item_layout);
            }
        }

        @Override
        public void onItemLayoutSwipeUp(ItemLayout item_layout) {
            if(!mEditMode) {
                super.onItemLayoutSwipeUp(item_layout);
            }
        }

        @Override
        public void onItemLayoutSwipeDown(ItemLayout item_layout) {
            if(!mEditMode) {
                super.onItemLayoutSwipeDown(item_layout);
            }
        }

        @Override
        public void onItemLayoutSwipe2Left(ItemLayout item_layout) {
            if(!mEditMode) {
                super.onItemLayoutSwipe2Left(item_layout);
            }
        }

        @Override
        public void onItemLayoutSwipe2Right(ItemLayout item_layout) {
            if(!mEditMode) {
                super.onItemLayoutSwipe2Right(item_layout);
            }
        }

        @Override
        public void onItemLayoutSwipe2Up(ItemLayout item_layout) {
            if(!mEditMode) {
                super.onItemLayoutSwipe2Up(item_layout);
            }
        }

        @Override
        public void onItemLayoutSwipe2Down(ItemLayout item_layout) {
            if(!mEditMode) {
                super.onItemLayoutSwipe2Down(item_layout);
            }
        }

        @Override
        public void onItemLayoutSizeChanged(ItemLayout il, int w, int h, int oldw, int oldh) {
            adjustGeometryBoxPosition();

            // hackish: update edit bars in the next round because otherwise layout requests would be lost
            // also don't do this for folders because folders are windowed and do not reflect actual screen orientation changes
            if(mEditMode && !il.getPage().isFolder()) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        updateEditBarsOrientation();
                    }
                });

            }

            super.onItemLayoutSizeChanged(il, w, h, oldw, oldh);
        }

        @Override
        public void onItemLayoutPositionChanged(ItemLayout il, float mCurrentDx, float mCurrentDy, float mCurrentScale) {
            if (!mEditMode) {
                super.onItemLayoutPositionChanged(il, mCurrentDx, mCurrentDy, mCurrentScale);
            }
        }

        @Override
        public void onItemLayoutMasterSelectedItemChanged(Item masterSelectedItem) {
            updateEditControls();
        }

        @Override
        public void onItemLayoutAppShortcutDropped(ItemLayout itemLayout, Object shortcutInfoObject, float x, float y) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                ShortcutInfo shortcutInfo = (ShortcutInfo) shortcutInfoObject;
                CharSequence label = shortcutInfo.getLongLabel();
                if(label == null || label.length() == 0) {
                    label = shortcutInfo.getShortLabel();
                }
                LauncherApps launcherApps = (LauncherApps) getSystemService(LAUNCHER_APPS_SERVICE);
                Drawable drawable = launcherApps.getShortcutIconDrawable(shortcutInfo, Utils.getLauncherIconDensity());
                Bitmap icon = Utils.createBitmapFromDrawable(drawable);
                Intent intent = new Intent(Shortcut.INTENT_ACTION_APP_SHORTCUT);
                intent.putExtra(Shortcut.INTENT_EXTRA_APP_SHORTCUT_ID, shortcutInfo.getId());
                intent.putExtra(Shortcut.INTENT_EXTRA_APP_SHORTCUT_PKG, shortcutInfo.getPackage());
                intent.putExtra(Shortcut.INTENT_EXTRA_APP_SHORTCUT_DISABLED_MSG, shortcutInfo.getDisabledMessage());
                Utils.addShortcut(label.toString(), icon, intent, itemLayout.getPage(), x, y, 1, true);
            }
        }

        @Override
        public void onHandlePressed(Handle h) {
            mTrackedHandle=h;
            for (ItemView itemView : getSelectedItemViews()) {
                startTracking(itemView);
                saveInitialItemViewGeometry(itemView);
            }
            if(mSystemConfig.hasSwitch(SystemConfig.SWITCH_SNAP)) {
                int snap = SnappingContext.SNAP_ALL;
                switch(h) {
                    case LEFT: snap = SnappingContext.SNAP_LEFT; break;
                    case RIGHT: snap = SnappingContext.SNAP_RIGHT; break;
                    case TOP: snap = SnappingContext.SNAP_TOP; break;
                    case BOTTOM: snap = SnappingContext.SNAP_BOTTOM; break;
                    case TOP_LEFT: snap = SnappingContext.SNAP_TOP | SnappingContext.SNAP_LEFT; break;
                    case TOP_RIGHT: snap = SnappingContext.SNAP_TOP | SnappingContext.SNAP_RIGHT; break;
                    case BOTTOM_LEFT: snap = SnappingContext.SNAP_BOTTOM | SnappingContext.SNAP_LEFT; break;
                    case BOTTOM_RIGHT: snap = SnappingContext.SNAP_BOTTOM | SnappingContext.SNAP_RIGHT; break;
                }
                startSnapping(mEditItemLayout.getMasterSelectedItemView(), snap);
            }
            mEditItemLayout.grabEvent(mEditItemLayout.getHandleView());
        }

        @Override
        public void onHandleMoved(Handle h, float dx, float dy) {
            dx /= mEditItemLayout.getCurrentScale();
            dy /= mEditItemLayout.getCurrentScale();
            final ItemView masterSelectedItemView = mEditItemLayout.getMasterSelectedItemView();

            if(masterSelectedItemView.getItem().getItemConfig().onGrid) {
                for (ItemView itemView : getSelectedItemViews()) {
                    Item item = itemView.getItem();
                    if(item.getClass() == StopPoint.class) {
                        continue;
                    }
                    if (item.getItemConfig().onGrid) {
                        moveHandleGrid(itemView, dx, dy);
                    } else {
                        moveHandleFree(itemView, dx, dy, false, 0);
                    }
                }
            } else {
                boolean is_size = mEditItemLayout.getHandleView().getMode() == HandleView.Mode.CONTENT_SIZE;
                float angle_or_scale = moveHandleFree(masterSelectedItemView, dx, dy, true, 0);
                for (ItemView itemView : getSelectedItemViews()) {
                    if(itemView == masterSelectedItemView || itemView.getClass() == StopPointView.class) {
                        continue;
                    }
                    if (itemView.getItem().getItemConfig().onGrid) {
                        if (is_size) {
                            moveHandleGrid(itemView, dx, dy);
                        }
                    } else {
                        moveHandleFree(itemView, dx, dy, false, angle_or_scale);
                    }
                }
            }
            mEditItemLayout.requestLayout();
            updateGeometryBox();

            mEditPage.setModified();
        }

        @Override
        public void onHandleUnpressed(Handle h, float dx, float dy) {
            mTrackedHandle=null;
            mEditItemLayout.grabEvent(null);
            if(mSystemConfig.hasSwitch(SystemConfig.SWITCH_SNAP)) {
                stopSnapping();
            }
            mUndoStack.storeGroupStart();
            for (ItemView itemView : getSelectedItemViews()) {
                Item item = itemView.getItem();
                mEditItemLayout.untrackItemView(itemView);
                boolean size_changed= false;
                final SavedItemGeometry oldGeometry = mOriginalItemsGeometry.get(item.getId());
                ItemConfig itemConfig = item.getItemConfig();
                if(itemConfig.onGrid) {
                    if(!oldGeometry.cell.equals(item.getCell())) {
                        mUndoStack.storeItemSetCell(itemView, oldGeometry);
                        size_changed = true;
                    }
                } else {
                    if(!item.getTransform().equals(oldGeometry.transform)) {
                        mUndoStack.storeItemSetTransform(itemView, oldGeometry);
                    }
                    if(oldGeometry.viewWidth != item.getViewWidth() || oldGeometry.viewHeight != item.getViewHeight()) {
                        mUndoStack.storeItemSetViewSize(itemView, oldGeometry);
                        size_changed = true;
                    }
                }
                if(size_changed) {
                    item.notifyCellChanged();
                }
            }
            mUndoStack.storeGroupEnd();
            updateGeometryBox();
        }

        @Override
        public void onItemViewPressed(ItemView itemView) {
            mItemLongTappedInNormalMode = null;
            closeBubble();
            mMoveStarted=false;
            mCurrentMoveItemDx=0;
            mCurrentMoveItemDy=0;
            itemView.setHighlightedLater(true);
            if(mEditMode && mEditPage == itemView.getItem().getPage()) {
                if(itemView == mEditItemLayout.getMasterSelectedItemView()) {
                    mEditItemLayout.grabEvent(itemView);
                }
            } else {
                super.onItemViewPressed(itemView);
            }
        }

        @Override
        public void onItemViewUnpressed(final ItemView itemView) {
            Item item = itemView.getItem();
            ItemLayout parentItemLayout = itemView.getParentItemLayout();
            itemView.setHighlightedLater(false);
            if (mEditMode || mItemLongTappedInNormalMode == item) {
                ItemLayout il = parentItemLayout;
                if(mItemLongTappedInNormalMode == item) {
                    il.setAllowWrap(true);
                    mItemLongTappedInNormalMode = null;
                }

                cancelSelectDropFolder();

                if(mMoveStarted) {
                    if(mSystemConfig.hasSwitch(SystemConfig.SWITCH_SNAP)) {
                        stopSnapping();
                    }
                    final ArrayList<ItemView> movedItemViews = getSelectedItemViews();

                    // in normal mode there is no selected item, only the currently moved item
                    if(!mEditMode) {
                        movedItemViews.add(itemView);
                    }

                    // the list of coordinates to use when moving items in a panel
                    final SparseArray<Point> movedItemsLocation = new SparseArray<>(movedItemViews.size());
                    final boolean is_embedded_folder = mDropFolderView instanceof EmbeddedFolderView;

                    if(mDropFolderView == null) {
                        mUndoStack.storeGroupStart();
                    }
                    for(int l = mSwappedItems.size()-1; l>=0; l--) {
                        final ArrayList<ItemView> swappedItemViews = mSwappedItems.valueAt(l);
                        for (ItemView swappedView : swappedItemViews) {
                            int id = swappedView.getItem().getId();
                            final SavedItemGeometry oldGeometry = mOriginalItemsGeometry.get(id);
                            mUndoStack.storeItemSetCell(swappedView, oldGeometry);
                            mOriginalItemsGeometry.remove(id);
                        }
                    }
                    mSwappedItems.clear();
                    for (ItemView iv : movedItemViews) {
                        Item i = iv.getItem();
                        int add_x = Utils.POSITION_AUTO;
                        int add_y = Utils.POSITION_AUTO;
                        if(is_embedded_folder) {
                            boolean on_grid = i.getItemConfig().onGrid;
                            RectF item_bounds = new RectF();
                            Utils.getItemViewBoundsInItemLayout(iv, item_bounds);
                            if (on_grid) {
                                // when using the free mode, the matrix is directly modified, no need to use mCurrentMoveItemDx/y
                                item_bounds.offset(mCurrentMoveItemDx, mCurrentMoveItemDy);
                            }

                            RectF drop_folder_bounds = new RectF();
                            Utils.getItemViewBoundsInItemLayout(mDropFolderView, drop_folder_bounds);

                            item_bounds.offset(-drop_folder_bounds.left, -drop_folder_bounds.top);
                            ItemLayout il_folder = ((EmbeddedFolderView)mDropFolderView).getEmbeddedItemLayout();
                            il_folder.getLocalInverseTransform().mapRect(item_bounds);
                            if (on_grid) {
                                add_x = (int) item_bounds.centerX();
                                add_y = (int) item_bounds.centerY();
                            } else {
                                add_x = (int) -drop_folder_bounds.left;
                                add_y = (int) -drop_folder_bounds.top;
                            }
                        }

                        movedItemsLocation.put(i.getId(), new Point(add_x, add_y));

                        dropItemView(iv);
                    }
                    if(mDropFolderView == null) {
                        mUndoStack.storeGroupEnd();
                    }
                    il.clearHighlightedCells();
                    il.grabEvent(null);
                    il.getPage().setModified();

                    updateGeometryBox();

                    if(mDropFolderView != null) {
                        mDropFolderView.setFocused(false);

                        final SelectionState selectionState = getSelectionState();

                        unselectAllItems();

                        if(is_embedded_folder) {
                            moveItemsIntoFolder(mDropFolderView, movedItemViews, movedItemsLocation, selectionState);
                        } else {
                            final ItemView finalDropFolderView = mDropFolderView;
                            mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    moveItemsIntoFolder(finalDropFolderView, movedItemViews, movedItemsLocation, selectionState);
                                }
                            }, ItemView.ANIMATION_TRANSLATE_DURATION);
                        }
                    } else {
                        Page page = item.getPage();
                        Utils.updateContainerIconIfNeeded(page);
                        if(page.config.rearrangeItems) rearrangeItems(page);
                    }
                }

                if(!mEditMode) {
                    if(mMoveStarted && mSystemConfig.autoEdit) {
                        enterEditMode(parentItemLayout, item);
                    }
                }
                mDropFolderView = null;
            } else {
                parentItemLayout.grabEvent(null);
            }
        }

        private void moveItemsIntoFolder(ItemView dropFolderItemView, ArrayList<ItemView> movedItemViews, SparseArray<Point> movedItemsLocation, SelectionState selectionState) {
            Folder dropFolder = (Folder) dropFolderItemView.getItem();
            boolean is_embedded_folder = dropFolder instanceof EmbeddedFolder;
            ItemLayout ilTo;
            if(is_embedded_folder) {
                ilTo = ((EmbeddedFolderView)dropFolderItemView).getEmbeddedItemLayout();
            } else {
                // make sure the folder view is ready (need its item layout), if it doesn't exist create it
                FolderView fv = findFolderView(dropFolderItemView, null);
                if(fv == null) {
                    fv = openFolder(dropFolder, dropFolderItemView, null, true);
                }
                ilTo = fv.getItemLayout();
            }
            Page to_page = ilTo.getPage();
            final float scale = ilTo.getCurrentScale();
            mUndoStack.storeGroupStart(selectionState);
            for (ItemView iv : movedItemViews) {
                Item i = iv.getItem();
                Matrix transform;
                if(is_embedded_folder && !i.getItemConfig().onGrid) {
                    transform = new Matrix(i.getTransform());
                } else {
                    transform = null;
                }

                int old_id = i.getId();
                Point location = movedItemsLocation.get(old_id);

                Item newItem = Utils.moveItem(i, to_page, location.x, location.y, scale, Item.NO_ID);

                if(transform != null) {
                    transform.postScale(1 / scale, 1 / scale);
                    transform.postTranslate(location.x, location.y);
                    newItem.setTransform(transform, false);
                }

                mUndoStack.storePageItemMove(ilTo.getItemView(newItem), old_id, mOriginalItemsGeometry.get(old_id));
            }
            mUndoStack.storeGroupEnd();
            Page page = movedItemViews.get(0).getItem().getPage();
            if(page.config.rearrangeItems) rearrangeItems(page);
        }

        @Override
        public void onItemViewMove(ItemView movedItemView, float dx, float dy) {
            Item movedItem = movedItemView.getItem();
            ItemLayout il = movedItemView.getParentItemLayout();
            if(mEditMode || mItemLongTappedInNormalMode == movedItem) {
                if((mEditMode && movedItem == mEditItemLayout.getMasterSelectedItem()) || !mEditMode) {
                    final ArrayList<ItemView> selectedItems = getSelectedItemViews();
                    if(mItemLongTappedInNormalMode == movedItem) {
                        // in normal mode, items are not selected (only in edit mode) : add it to the list to include it in the processings below
                        selectedItems.add(movedItemView);
                    }
                    if(!mMoveStarted) {
                        PageConfig c = il.getPage().config;
                        if(mItemLongTappedInNormalMode == null && (c.wrapX || c.wrapY) && (mSystemConfig.hints& SystemConfig.HINT_WRAP)==0) {
                            il.setAllowWrap(false);
                            Toast.makeText(mContext, R.string.h_w, Toast.LENGTH_SHORT).show();
                        }
                        for (ItemView itemView : selectedItems) {
                            itemView.prepareDraggedBitmap();
                            startTracking(itemView);
                            Item item = itemView.getItem();
                            if(item.getItemConfig().onGrid && !itemView.isDragged()) {
                                itemView.setDragged(true);
                            }
                            itemView.setHighlightedNow(false);
                            saveInitialItemViewGeometry(itemView);
                        }
                        il.grabEvent(movedItemView);
                        if(mSystemConfig.hasSwitch(SystemConfig.SWITCH_SNAP)) {
                            startSnapping(movedItemView, movedItem.getClass() == StopPoint.class ? SnappingContext.SNAP_CENTER : SnappingContext.SNAP_ALL);
                        }
                        closeBubble();
                        mMoveStarted = true;
                    }

                    boolean is_stop_point = movedItem.getClass()==StopPoint.class;

                    mCurrentMoveItemDx=dx;
                    mCurrentMoveItemDy=dy;

                    final boolean swapItems = il.getPage().config.swapItems;
                    final ArrayList<Item> all_items = il.getPage().items;

                    il.clearHighlightedCells();
                    for (ItemView iv : selectedItems) {
                        Item i = iv.getItem();
                        if(i.getItemConfig().onGrid) {
                            il.moveTrackedItemView(iv, dx, dy);

                            if (!is_stop_point) {
                                getItemViewCell(iv, dx, dy, mTempCoords);
                                Rect r = new Rect(i.getCell());
                                r.offsetTo(mTempCoords[0], mTempCoords[1]);
                                il.addHighlightedCell(i, r);

                                if(swapItems) {
                                    // check for overlap
                                    int cw=(int) il.getCellWidth();
                                    int ch=(int) il.getCellHeight();
                                    mTempRect.set(i.getCell());
                                    mTempRect.left*=cw;
                                    mTempRect.right*=cw;
                                    mTempRect.top*=ch;
                                    mTempRect.bottom*=ch;
                                    int mx= (int) (mTempRect.centerX()+dx);
                                    int my= (int) (mTempRect.centerY()+dy);

                                    ItemConfig.PinMode pin_mode = i.getItemConfig().pinMode;
                                    for(Item i2 : all_items) {
                                        ItemConfig ic = i2.getItemConfig();
                                        if (!ic.onGrid) continue;
                                        if (ic.pinMode != pin_mode) continue;
                                        if (i2.getClass() == StopPoint.class) continue;
                                        if (i2 == i) continue;
                                        ItemView iv2 = il.getItemView(i2);
                                        if (iv2.isSelected()) continue;
                                        if (i.isAppDrawerHidden()) continue;

                                        final Rect cell = i2.getCell();
                                        mTempRect.set(cell);
                                        mTempRect.left *= cw;
                                        mTempRect.right *= cw;
                                        mTempRect.top *= ch;
                                        mTempRect.bottom *= ch;

                                        ArrayList<ItemView> swapped_items = mSwappedItems.get(i.getId());

                                        boolean overlap = mTempRect.contains(mx, my);
                                        if (overlap && !(i2 instanceof Folder)) {
                                            if (swapped_items == null) {
                                                swapped_items = new ArrayList<>();
                                                mSwappedItems.put(i.getId(), swapped_items);
                                            }
                                            if(!swapped_items.contains(iv2)) {
                                                swapped_items.add(iv2);
                                                saveInitialItemViewGeometry(iv2);
                                                Rect old_cell = new Rect(cell);
                                                Rect to_cell = i.getCell();
                                                cell.offsetTo(to_cell.left, to_cell.top);
                                                iv2.animateTranslate((old_cell.left - to_cell.left) * cw, 0, (old_cell.top - to_cell.top) * ch, 0, false);
                                                il.reLayoutItems();
                                            }
                                        }

                                        if (swapped_items != null) {
                                            for (int l=swapped_items.size()-1; l>=0; l--) {
                                                ItemView swappedView = swapped_items.get(l);
                                                Item swapped = swappedView.getItem();
                                                final Rect original_cell = mOriginalItemsGeometry.get(swapped.getId()).cell;
                                                mTempRect.set(original_cell);
                                                mTempRect.left *= cw;
                                                mTempRect.right *= cw;
                                                mTempRect.top *= ch;
                                                mTempRect.bottom *= ch;
                                                if (!mTempRect.contains(mx, my)) {
                                                    mTempRect.set(swapped.getCell());
                                                    swapped.getCell().set(original_cell);
                                                    swappedView.animateTranslate((mTempRect.left-original_cell.left)*cw, 0, (mTempRect.top-original_cell.top)*ch, 0, false);
                                                    il.reLayoutItems();
                                                    swapped_items.remove(swappedView);
                                                    mOriginalItemsGeometry.remove(swapped.getId());
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            SavedItemGeometry tig = mTrackedItemsGeometry.get(i.getId());
                            Matrix m=new Matrix(tig.transform);
                            m.postTranslate(dx, dy);
                            i.setTransform(m, false);
                            updateGeometryBox();
                        }
                    }

                    // check snapping in detached mode
                    if(!movedItem.getItemConfig().onGrid) {
                        if(mSnappingContext != null) {
                            final Matrix m = movedItem.getTransform();
                            mSnappingContext.computeSnaps(m);

                            if(mSnappingContext.min_dx != Float.MAX_VALUE) {
                                for (ItemView iv : selectedItems) {
                                    Item i = iv.getItem();
                                    if (!i.getItemConfig().onGrid) {
                                        final Matrix m2 = i.getTransform();
                                        m2.postTranslate(mSnappingContext.min_dx / mSnappingContext.item_layout.getCurrentScale(), 0);
                                    }
                                }
                            }
                            if(mSnappingContext.min_dy != Float.MAX_VALUE) {
                                for (ItemView iv : selectedItems) {
                                    Item i = iv.getItem();
                                    if (!i.getItemConfig().onGrid) {
                                        final Matrix m2 = i.getTransform();
                                        m2.postTranslate(0, mSnappingContext.min_dy / mSnappingContext.item_layout.getCurrentScale());
                                    }
                                }
                            }

                            mSnappingContext.applySnaps(m);
                        }
                    }

                    // check for folder drag & drop (except for stop points)
                    if(!is_stop_point) {
                        movedItemView.getHitRect(mTempRect);
                        mTempRectF.set(mTempRect);
                        Matrix t = il.getTransformForItemView(movedItemView);
                        if(t != null) {
                            t.mapRect(mTempRectF);
                        }
                        mTempCoords[0]= (int) mTempRectF.centerX();
                        mTempCoords[1]= (int) mTempRectF.centerY();
                        for(Item i : il.getPage().items) {
                            if(i != movedItem && i instanceof Folder) {
                                ItemView iv = il.getItemView(i);
                                iv.getHitRect(mTempRect);
                                mTempRectF.set(mTempRect);
                                t = il.getTransformForItemView(iv);
                                if(t != null) {
                                    t.mapRect(mTempRectF);
                                }
                                if(mTempRectF.contains(mTempCoords[0], mTempCoords[1])) {
                                    if(mCandidateDropFolderView == null && mDropFolderView == null) {
                                        mCandidateDropFolderView = iv;
                                        mSelectDropFolderStartTime = SystemClock.uptimeMillis();
                                        mSelectDropFolder.run();
                                    }
                                } else {
                                    if(iv == mCandidateDropFolderView) {
                                        cancelSelectDropFolder();
                                    } else if(iv == mDropFolderView) {
                                        mDropFolderView.setFocused(false);
                                        mDropFolderView = null;
                                        // TODO SEL restore all selected items alpha
//                            item.setAlpha(mOldItemAlpha);
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                super.onItemViewMove(movedItemView, dx, dy);
            }
        }

        @Override
        public void onItemViewClicked(ItemView itemView) {
            Item item = itemView.getItem();
            if(mEditMode && mEditPage == item.getPage()) {
                Rect bounds = computeItemViewBounds(itemView);
                setLastTouchEventForItemView(itemView, Utils.POSITION_AUTO, Utils.POSITION_AUTO, bounds.centerX(), bounds.centerY());
                HandleView hv = mEditItemLayout.getHandleView();
                boolean is_stop_point = item.getClass() == StopPoint.class;
                Item masterSelectedItem = mEditItemLayout.getMasterSelectedItem();
                if(!itemView.isSelected()) {
                    if(mSystemConfig.hasSwitch(SystemConfig.SWITCH_MULTI_SELECTION)) {
                        mEditItemLayout.setMasterSelectedItem(item);
                        itemView.setSelected(true);
                        configureHandlesForItemView(itemView, HandleView.Mode.CONTENT_SIZE, true);
                    } else {
                        if(masterSelectedItem != null) {
                            mEditItemLayout.getMasterSelectedItemView().setSelected(false);
                            mEditItemLayout.setMasterSelectedItem(null);
                        }
                        mEditItemLayout.setMasterSelectedItem(item);
                        itemView.setSelected(true);
                        configureHandlesForItemView(itemView, HandleView.Mode.CONTENT_SIZE, true);
                    }
                } else {
                    if(!mMoveStarted) {
                        if(item == masterSelectedItem) {
                            if (item.getItemConfig().onGrid) {
                                // a single mode allowed in grid mode
                                mEditItemLayout.setMasterSelectedItem(null);
                                itemView.setSelected(false);
                            } else {
                                HandleView.Mode mode = hv.getMode();
                                if (mode == HandleView.Mode.ROTATE) {
                                    mEditItemLayout.setMasterSelectedItem(null);
                                    itemView.setSelected(false);
                                } else if (!is_stop_point) {
                                    HandleView.Mode[] modes = HandleView.Mode.values();
                                    HandleView.Mode current_mode = mode;
                                    int n = current_mode.ordinal() + 1;
                                    if (n == modes.length) n = 0;
                                    hv.setMode(modes[n]);
                                }
                            }
                        } else {
                            mEditItemLayout.setMasterSelectedItem(item);
                            configureHandlesForItemView(itemView, HandleView.Mode.CONTENT_SIZE, true);
                        }
                    }
                }
            } else {
                boolean normal_path = true;
                boolean is_widget = item.getClass() == Widget.class;
                if(is_widget) {
                    WidgetView widgetView = (WidgetView)itemView;
                    Widget w = (Widget) item;
                    if(!widgetView.isGood()) {
                        normal_path = false;
                        ComponentName cn = w.getComponentName();
                        if(cn != null) {
                            String pkg_name = cn.getPackageName();
                            try {
                                getPackageManager().getPackageInfo(pkg_name, 0);
                                if(sBindAppWidgetIdIfAllowed != null) {
                                    int old_id = w.getAppWidgetId();
                                    LLApp.get().getAppWidgetHost().deleteAppWidgetId(old_id);
                                    int new_id = LLApp.get().getAppWidgetHost().allocateAppWidgetId();
                                    boolean ok;
                                    try {
                                        ok = (Boolean)sBindAppWidgetIdIfAllowed.invoke(AppWidgetManager.getInstance(mContext), old_id, cn);
                                    } catch (Exception e) {
                                        ok = false;
                                    }
                                    if(ok) {
                                        w.setAppWidgetId(new_id);
                                        item.notifyChanged();
                                    } else {
                                        mTmpItem = item;
                                        mAllocatedAppWidgetId = new_id;
                                        Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND);
                                        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, new_id);
                                        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, cn);
                                        startActivityForResult(intent, REQUEST_BIND_APPWIDGET);
                                    }
                                } else {
                                    selectAppWidgetForReplace(item);
                                }
                            } catch (NameNotFoundException e) {
                                Utils.startAppStore(mContext, pkg_name);
                            }
                        } else {
                            selectAppWidgetForReplace(item);
                        }
                    }
                }
                if(normal_path & item.getClass()!=EmbeddedFolder.class) {
                    super.onItemViewClicked(itemView);
                }
            }
        }

        @Override
        public void onItemViewLongClicked(ItemView itemView) {
            Item item = itemView.getItem();
            itemView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            if(mEditMode && mEditPage == item.getPage()) {
                Rect bounds = computeItemViewBounds(itemView);
                setLastTouchEventForItemView(itemView, Utils.POSITION_AUTO, Utils.POSITION_AUTO, bounds.centerX(), bounds.centerY());
                final boolean multi_selection = mSystemConfig.hasSwitch(SystemConfig.SWITCH_MULTI_SELECTION);
                if(itemView.isSelected() && multi_selection) {
                    itemView.setSelected(false);
                } else {
                    itemView.setDragged(true);
                    if (!mMoveStarted) {
                        itemView.setHighlightedNow(false);

                        if (item != mEditItemLayout.getMasterSelectedItem()) {
                            mEditItemLayout.setMasterSelectedItem(item);
                            itemView.setSelected(true);
                            configureHandlesForItemView(itemView, HandleView.Mode.CONTENT_SIZE, true);
                            if(!multi_selection) {
                                for (Item i : mEditPage.items) {
                                    ItemView iv = mEditItemLayout.getItemView(i);
                                    if (iv.isSelected() && iv != itemView) {
                                        iv.setSelected(false);
                                    }
                                }
                            }
                        }
                        openBubble(BUBBLE_MODE_ITEM_EM, itemView);
                    }
                }
            } else {
                super.onItemViewLongClicked(itemView);
            }
        }

        @Override
        public void onItemViewAction(ItemView itemView, int action) {
            Toast.makeText(mContext, R.string.nly, Toast.LENGTH_SHORT).show();
        }

        @Override
        public boolean onItemViewTouch(ItemView itemView, MotionEvent event) {
            return mEditMode ? false : super.onItemViewTouch(itemView, event);
        }

        @Override
        public void onItemViewSelectionChanged(ItemView itemView, boolean selected) {
            super.onItemViewSelectionChanged(itemView, selected);
            updateEditControls();
        }

        @Override
        protected FolderView openUserMenu(boolean prepareOnly) {
            LightningEngine.PageManager pm = getCurrentRootPage().getEngine().getPageManager();
            Page page;
            if(pm.isPageCreated(Page.USER_MENU_PAGE)) {
                page = pm.getOrLoadPage(Page.USER_MENU_PAGE);
            } else {
                page = Setup.createUserMenuPage(Dashboard.this);
            }

            Folder folder = (Folder) page.findItemById(Utils.USER_MENU_ITEM_ID);
            FolderView fv = openFolder(folder, null, null, prepareOnly);
            return fv;
        }


        @Override
        protected void onFolderClosed(FolderView fv) {
            if(mEditMode && mEditItemLayout==fv.getItemLayout()) {
                leaveEditMode();
            }
        }

        @Override
        public boolean runAction(LightningEngine engine, String source, EventAction ea, ItemLayout il, ItemView itemView) {
            Intent intent;

            switch(ea.action) {
                case GlobalConfig.APP_DRAWER:
                    startActivity(new Intent(mContext, AppDrawerX.class));
                    break;

                case GlobalConfig.LAUNCHER_MENU:
                    openOptionsMenu();
                    break;

                case GlobalConfig.CUSTOM_MENU:
                    openBubble(BUBBLE_MODE_CUSTOM_MENU);
                    break;

                case GlobalConfig.USER_MENU:
                    openUserMenu(false);
                    break;

                case GlobalConfig.EDIT_LAYOUT:
                    if(mEditMode) {
                        if(itemView == null) {
                            leaveEditMode();
                        } else {
                            unselectAllItems();
                            mEditItemLayout.setMasterSelectedItem(itemView.getItem());
                            itemView.setSelected(true);
                            configureHandlesForItemView(itemView, HandleView.Mode.CONTENT_SIZE, true);
                        }
                    } else {
                        enterEditMode(il, itemView==null ? null : itemView.getItem());
                    }
                    break;

                case GlobalConfig.ADD_ITEM:
                    try { removeDialog(DIALOG_ADD); } catch(Exception e) {}
                    showDialog(DIALOG_ADD);
                    break;

                case GlobalConfig.MOVE_ITEM:
                    if(itemView != null) {
                        mItemLongTappedInNormalMode = itemView.getItem();
                        openBubble(BUBBLE_MODE_ITEM_NO_EM, itemView);
                    }
                    break;

                case GlobalConfig.CUSTOMIZE_LAUNCHER:
                    PhoneUtils.startSettings(mContext, new ContainerPath(getTargetOrTopmostItemLayout()), true);
                    break;

                case GlobalConfig.CUSTOMIZE_DESKTOP:
                    PhoneUtils.startSettings(mContext, new ContainerPath(getTargetOrTopmostItemLayout()), false);
                    break;

                case GlobalConfig.CUSTOMIZE_ITEM:
                    editItem(il, itemView==null ? null : itemView.getItem());
                    break;

                case GlobalConfig.ITEM_MENU:
                    openBubble(mEditMode ? BUBBLE_MODE_ITEM_EM : BUBBLE_MODE_ITEM_NO_EM, itemView);
                    break;

                case GlobalConfig.SHOW_APP_SHORTCUTS:
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                        showAppShortcuts(itemView);
                    }
                    break;

                case GlobalConfig.SEARCH:
                    startSearch(null, false, null, true);
                    break;

                case GlobalConfig.SEARCH_APP:
                    intent = new Intent(mContext, AppDrawerX.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra(AppDrawerX.INTENT_EXTRA_SEARCH, true);
                    startActivity(intent);
                    break;

                case GlobalConfig.SHOW_HIDE_APP_MENU:
                    openOptionsMenu();
                    break;

                case GlobalConfig.SHOW_HIDE_APP_MENU_STATUS_BAR:
                    openOptionsMenu();
                    toggleHideStatusBar();
                    break;

                case GlobalConfig.PREVIOUS_DESKTOP:
                    gotoPage(PAGE_DIRECTION_HINT_BACKWARD);
                    il = getTopmostItemLayout();
                    break;

                case GlobalConfig.NEXT_DESKTOP:
                    gotoPage(PAGE_DIRECTION_HINT_FORWARD);
                    il = getTopmostItemLayout();
                    break;

                case GlobalConfig.SELECT_WALLPAPER:
                    startActivity(new Intent(Intent.ACTION_SET_WALLPAPER));
                    break;

                case GlobalConfig.GO_HOME:
                    setPagerPage(mGlobalConfig.homeScreen, PAGE_DIRECTION_HINT_AUTO);
                    mNavigationStack.clear();
                    il = getTopmostItemLayout();
                    break;

                case GlobalConfig.GO_HOME_ZOOM_TO_ORIGIN:
                    int currentPageId = getCurrentRootPage().id;
                    if(currentPageId == mGlobalConfig.homeScreen) {
                        zoomToOrigin(getCurrentRootItemLayout());
                    } else {
                        setPagerPage(mGlobalConfig.homeScreen, PAGE_DIRECTION_HINT_AUTO);
                        mNavigationStack.clear();
                        il = getTopmostItemLayout();
                    }
                    break;

                case GlobalConfig.SELECT_DESKTOP_TO_GO_TO:
                    intent = new Intent(mContext, ScreenManager.class);
                    intent.setAction(Intent.ACTION_PICK);
                    startActivityForResult(intent, REQUEST_SELECT_SCREEN_FOR_GOTO);
                    break;

                case GlobalConfig.BACK:
                    if(goBack()) {
                        il = getTopmostItemLayout();
                    }
                    break;

                case GlobalConfig.OPEN_HIERARCHY_SCREEN:
                    mHierarchyScreen.show(null);
                    break;

                default:
                    return super.runAction(engine, source, ea, il, itemView);
            }

            processNextAction(engine, source, ea, il, itemView);
            return true;
        }


        @Override
        protected void launchIntent(Intent intent, ItemView itemView) {
            ComponentName cn = intent.getComponent();
            ComponentName app_drawer =  new ComponentName(mContext, AppDrawerX.class);
            if(BuildConfig.IS_TRIAL && cn != null && cn.getClassName().equals(LLApp.LL_PKG_NAME+".activities.AppDrawer")) {
                // translate app drawer intent in the trial version into app drawer intent in the extreme version
                intent.setComponent(app_drawer);
            }

            if(itemView != null) {
                mLastLaunchAnimation = itemView.getItem().getItemConfig().launchAnimation;
            } else {
                Page page = mScreen.getTargetOrTopmostItemLayout().getPage();
                mLastLaunchAnimation = page.config.defaultItemConfig.launchAnimation;
            }

            super.launchIntent(intent, itemView);
        }

        @Override
        protected void onOrientationChanged(int orientation) {
            super.onOrientationChanged(orientation);

            if(mEditMode && mEditItemLayout.getOpenerItemView() instanceof EmbeddedFolderView) {
                ItemLayout il = mEditItemLayout;
                Item item = il.getMasterSelectedItem();
                leaveEditMode();
                enterEditMode(il, item);
            }
        }

        @Override
        public void restart() {
            for (Screen screen : LLApp.get().getScreens()) {
                screen.pause();
            }
            LLApp.get().saveAllData();
            finish();
            startActivity(new Intent(Dashboard.this, Dashboard.class));
            System.exit(0);
        }
    }
}
