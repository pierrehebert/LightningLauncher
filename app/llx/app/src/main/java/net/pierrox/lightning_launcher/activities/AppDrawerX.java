package net.pierrox.lightning_launcher.activities;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewAnimator;

import net.pierrox.lightning_launcher.LLApp;
import net.pierrox.lightning_launcher.api.ScreenIdentity;
import net.pierrox.lightning_launcher.configuration.GlobalConfig;
import net.pierrox.lightning_launcher.configuration.ItemConfig;
import net.pierrox.lightning_launcher.configuration.PageConfig;
import net.pierrox.lightning_launcher.data.State;
import net.pierrox.lightning_launcher.configuration.SystemConfig;
import net.pierrox.lightning_launcher.data.ContainerPath;
import net.pierrox.lightning_launcher.data.EventAction;
import net.pierrox.lightning_launcher.data.FileUtils;
import net.pierrox.lightning_launcher.data.Folder;
import net.pierrox.lightning_launcher.data.Item;
import net.pierrox.lightning_launcher.data.JsonLoader;
import net.pierrox.lightning_launcher.data.LightningIntent;
import net.pierrox.lightning_launcher.data.Page;
import net.pierrox.lightning_launcher.data.PageIndicator;
import net.pierrox.lightning_launcher.data.Shortcut;
import net.pierrox.lightning_launcher.data.Utils;
import net.pierrox.lightning_launcher.engine.LightningEngine;
import net.pierrox.lightning_launcher.engine.Screen;
import net.pierrox.lightning_launcher.util.AddItemDialog;
import net.pierrox.lightning_launcher.util.PhoneUtils;
import net.pierrox.lightning_launcher.views.EditTextIme;
import net.pierrox.lightning_launcher.views.FolderView;
import net.pierrox.lightning_launcher.views.ItemLayout;
import net.pierrox.lightning_launcher.views.SharedAsyncGraphicsDrawable;
import net.pierrox.lightning_launcher.views.item.ItemView;
import net.pierrox.lightning_launcher.views.item.ShortcutView;
import net.pierrox.lightning_launcher_extreme.R;

import org.json.JSONObject;
import org.mozilla.javascript.Function;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;
import java.util.StringTokenizer;

public class AppDrawerX extends Dashboard implements EditTextIme.OnEditTextImeListener, TextView.OnEditorActionListener, TextWatcher {
    public static final String INTENT_EXTRA_SELECT_FOR_ADD="s";
    public static final String INTENT_EXTRA_SEARCH="l";

    private static final int BUBBLE_MODE_DRAWER_MENU = 100;
    private static final int BUBBLE_MODE_DRAWER_MODE = 101;
    private static final int BUBBLE_MODE_DRAWER_VISIBILITY = 102;

    private static final int DIALOG_MY_DRAWER=3;

    private static final int ACTION_BAR_HIDE_DELAY=2000;
    private static final long ACTION_BAR_ANIM_IN_DURATION=300;
    private static final long ACTION_BAR_ANIM_OUT_DURATION=500;

    private static final int ACTION_BAR_CHILD_DRAWER_ACTIONS = 0;
    private static final int ACTION_BAR_CHILD_SEARCH = 1;
    private static final int ACTION_BAR_CHILD_BATCH = 2;

    private int mLayoutMode;

    private boolean mSearchMode;
    private EditTextIme mSearchField;
    private ItemLayout mItemLayout;
    private ViewAnimator mActionBar;
    private TextView mModeIcon;
    private TextView mBatchCount;
    private TextView mBatchAdd;

    private Page mDrawerPage;
    private ArrayList<Integer> mAllDrawerPageIDs; // gather ids of all pages displayed in the app drawer, including folders
    private State mState;
    private int mVisibleItemsCount;
    private float mScaleBeforePinch;
    private int mLayoutModeBeforeSearch;
    private ItemView mSearchFocusedItemView;
    private int mPreviouslyDisplayedChild;
    private boolean mAndroidActionBarDisplayedBeforeBatch;
    private int mBatchCheckedCount;
    private boolean mBatchMode;
    private Drawable mActionBarBackground = null;
    private boolean mResumed;

    private static Matrix sIdentityTransform = new Matrix();
    private Matrix mSavedItemLayoutLocalTransformCustom = new Matrix();
    private Matrix mSavedItemLayoutLocalTransformByName = new Matrix();

    private ComponentName mThisCn;
    private ComponentName mDashboardCn;

    private Animation mLayoutModeSwitchAnimation;
    private int mNextLayoutMode;

    @Override
    protected void createActivity(Bundle savedInstanceState) {
        mThisCn = new ComponentName(this, AppDrawerX.class);
        mDashboardCn = new ComponentName(this, Dashboard.class);

        if(mEngine.shouldDoFirstTimeInit() && !isSelectForAdd()) {
            // redirect to home
            startActivity(new Intent(this, Dashboard.class));
            finish();
            return;
        }

        final Animation fadeIn = AnimationUtils.loadAnimation(AppDrawerX.this, android.R.anim.fade_in);
        fadeIn.setInterpolator(this, android.R.anim.accelerate_interpolator);
        long duration = fadeIn.getDuration()/4; // divided by 2 because there are two animations, then again by 2 to speed up things
        fadeIn.setDuration(duration);
        mLayoutModeSwitchAnimation = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
        mLayoutModeSwitchAnimation.setInterpolator(this, android.R.anim.accelerate_interpolator);
        mLayoutModeSwitchAnimation.setDuration(duration);
        mLayoutModeSwitchAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                setLayoutMode(mNextLayoutMode, false);
                mItemLayout.startAnimation(fadeIn);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        mNavigationStack=new Stack<Integer>();


        Typeface typeface = LLApp.get().getIconsTypeface();

        mModeIcon = (TextView)findViewById(R.id.drawer_mode_icon);
        mModeIcon.setTypeface(typeface);

        mSearchField=(EditTextIme)findViewById(R.id.drawer_search_field);
        mSearchField.setHint(getString(R.string.an_sa));
        mSearchField.setOnEditTextImeListener(this);
        mSearchField.addTextChangedListener(this);
        mSearchField.setOnEditorActionListener(this);

        final TextView batch_ok = (TextView) findViewById(R.id.batch_ok);
        batch_ok.setTypeface(typeface);
        batch_ok.setOnClickListener(this);
        mBatchCount = (TextView) findViewById(R.id.batch_count);
        mBatchCount.setOnClickListener(this);
        mBatchAdd = (TextView) findViewById(R.id.batch_add);
        mBatchAdd.setTypeface(typeface);
        mBatchAdd.setOnClickListener(this);

        mItemLayout=(ItemLayout)findViewById(R.id.drawer_il);
        mScreen.takeItemLayoutOwnership(mItemLayout);

        findViewById(R.id.drawer_mode_grp).setOnClickListener(this);
        TextView btn;

        btn = (TextView) findViewById(R.id.drawer_zoom);
        btn.setOnClickListener(this);
        btn.setTypeface(typeface);
        btn = (TextView) findViewById(R.id.drawer_search);
        btn.setOnClickListener(this);
        btn.setTypeface(typeface);
        btn = (TextView) findViewById(R.id.drawer_more);
        btn.setOnClickListener(this);
        btn.setTypeface(typeface);

        mActionBar = (ViewAnimator) findViewById(R.id.ab);

        loadState();

        mDrawerPage = mEngine.getOrLoadPage(Page.APP_DRAWER_PAGE);

        File items_file=mDrawerPage.getItemsFile();
        if(!items_file.exists() || mDrawerPage.items.size()==0) {
            mLayoutMode = mState.layoutMode;
            //firstTimeInit();
            refreshAppDrawerItems(false);
        } else {
            mLayoutMode = Utils.LAYOUT_MODE_UNDEFINED;
            setLayoutMode(mState.layoutMode, false);

            if(getIntent().getBooleanExtra(INTENT_EXTRA_SEARCH, false)) {
                mItemLayout.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        setSearchMode(true);
                    }
                }, 300);
            } else {
                //checkIfRefreshIsNeeded();
            }
        }
    }

    /*private void checkIfRefreshIsNeeded() {
        final ArrayList<Item> items = new ArrayList<Item>();
        addAppsFromPage(Utils.APP_DRAWER_PAGE, items);

        new AsyncTask<Void,Void,Boolean>() {

            @Override
            protected Boolean doInBackground(Void... params) {
                long t= SystemClock.uptimeMillis();
                PackageManager pm=getPackageManager();
                Intent intent_filter=new Intent(Intent.ACTION_MAIN, null);
                intent_filter.addCategory(Intent.CATEGORY_LAUNCHER);
                List<ResolveInfo> ris=pm.queryIntentActivities(intent_filter, 0);
                return ris.size() != items.size();
            }

            @Override
            protected void onPostExecute(Boolean differ) {
                if(differ) {
                    refreshAppDrawerItems(true);
                }
            }
        }.execute((Void)null);
    }*/

    @Override
    protected void destroyActivity() {
        // pass disable code from super
    }

    @Override
    protected Screen createScreen() {
        return new AppDrawerScreen(this, R.layout.app_drawer_x);
    }

    @Override
    public void onResume() {
        super.onResume();

        mResumed = true;
        if(mActionBarBackground instanceof SharedAsyncGraphicsDrawable) {
            ((SharedAsyncGraphicsDrawable)mActionBarBackground).resume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        mResumed = false;
        if(mActionBarBackground instanceof SharedAsyncGraphicsDrawable) {
            ((SharedAsyncGraphicsDrawable)mActionBarBackground).pause();
        }

        setBatchMode(false);
        setSearchMode(false);

        saveState();
    }

    @SuppressWarnings("deprecation")
    @Override
    protected Dialog onCreateDialog(int id) {
        AlertDialog.Builder builder;

        switch(id) {
            case DIALOG_MY_DRAWER:
                builder=new AlertDialog.Builder(this);
                builder.setTitle(R.string.my_drawer_title);
                builder.setMessage(R.string.my_drawer_message);
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setCancelable(false);
                return builder.create();
        }

        return super.onCreateDialog(id);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public void openOptionsMenu() {
        openBubble(BUBBLE_MODE_DRAWER_MENU);
    }

    @Override
    protected void openBubble(int mode, ItemLayout itemLayout, ItemView itemView, Rect focus, List shortcuts) {
        mItemLayout.cancelDelayedItemViewLoad();
        super.openBubble(mode, itemLayout, itemView, focus, shortcuts);
    }

    @Override
    protected boolean closeBubble() {
        mItemLayout.loadNextItemViewLater();
        return super.closeBubble();
    }

    @Override
    protected void configureActivity(Page page) {
        super.configureActivity(page);

        mActionBar.setVisibility(mDrawerPage.config.adHideActionBar || mIsAndroidActionBarDisplayed ? View.GONE : View.VISIBLE);
        if(Build.VERSION.SDK_INT>=19) {
            //noinspection ResourceType
            ((FrameLayout.LayoutParams) mActionBar.getLayoutParams()).topMargin = mDrawerPage.config.statusBarHide ? 0 : mScreen.getSystemBarTintManager().getConfig().getStatusBarHeight();
            mActionBar.requestLayout();
        }

        int ab_text_color = mDrawerPage.config.adActionBarTextColor;
        if(ab_text_color == 0) {
            int[] attrs = {android.R.attr.textColor};
            TypedArray ta = obtainStyledAttributes(R.style.ab_text, attrs);
            ab_text_color = ta.getColor(0, 0);
            ta.recycle();
        }
        int[] ids = new int[] { R.id.drawer_mode_icon, R.id.drawer_mode_value, R.id.drawer_zoom, R.id.drawer_search, R.id.drawer_more, R.id.drawer_search_field, R.id.batch_ok, R.id.batch_count, R.id.batch_add };
        for(int id : ids) {
            ((TextView)mActionBar.findViewById(id)).setTextColor(ab_text_color);
        }
        int hint_color = Color.argb(Color.alpha(ab_text_color)/2, Color.red(ab_text_color), Color.green(ab_text_color), Color.blue(ab_text_color));
        mSearchField.setHintTextColor(hint_color);

        if(mActionBarBackground instanceof SharedAsyncGraphicsDrawable) {
            ((SharedAsyncGraphicsDrawable) mActionBarBackground).unregisterListener(mActionBarSharedDrawableListener);
        }

        mActionBarBackground = null;
        File f = getAppDrawerActionBarBackgroundFile(page);
        if(f.exists()) {
            mActionBarBackground = Utils.loadDrawable(f);
        }

        if(mActionBarBackground == null) {
            int bg_res_id;
            if(Build.VERSION.SDK_INT>=21) bg_res_id = R.color.color_primary;
            else if(Build.VERSION.SDK_INT>=13) bg_res_id = R.drawable.ab_bg_v13;
            else bg_res_id = R.drawable.ab_bg_v9;
            mActionBarBackground = getResources().getDrawable(bg_res_id);
        }

        if(mActionBarBackground instanceof SharedAsyncGraphicsDrawable) {
            SharedAsyncGraphicsDrawable sd = (SharedAsyncGraphicsDrawable) mActionBarBackground;
            sd.registerListener(mActionBarSharedDrawableListener);
            if(mResumed) {
                sd.resume();
            }
        }
        mActionBar.setBackgroundDrawable(mActionBarBackground);
    }

    private SharedAsyncGraphicsDrawable.SharedAsyncGraphicsDrawableListener mActionBarSharedDrawableListener = new SharedAsyncGraphicsDrawable.SharedAsyncGraphicsDrawableListener() {
        @Override
        public void onSharedAsyncGraphicsDrawableInvalidated(SharedAsyncGraphicsDrawable drawable) {
            mActionBar.invalidate();
        }

        @Override
        public void onSharedAsyncGraphicsDrawableSizeChanged(SharedAsyncGraphicsDrawable drawable) {
            mActionBar.requestLayout();
        }
    };

    @Override
    protected int getActionBarHeight() {
        return mDrawerPage.config.adHideActionBar || mIsAndroidActionBarDisplayed ? 0 : getResources().getDimensionPixelSize(R.dimen.ab_height);
    }

//    @Override
//    protected void myStartActivityForResult(Intent intent, int requestCode) {
//        realStartActivityForResult(intent, requestCode);
//    }
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        myOnActivityResult(requestCode, resultCode, data);
//    }

    @Override
    protected boolean goBack() {
        if(mActionBar.getDisplayedChild() == ACTION_BAR_CHILD_BATCH) {
            doAddForBatchMode();
        } else {
            FolderView fv = mScreen.findTopmostFolderView();
            if (fv != null) {
                mScreen.closeFolder(fv, true);
                return true;
            } else {
                if(!mScreen.zoomToOrigin(mScreen.getTargetOrTopmostItemLayout())) {
                    finish();
                }
            }
        }
        return false;
    }

    public static File getAppDrawerActionBarBackgroundFile(Page drawer_page) {
        return new File(drawer_page.getIconDir(), FileUtils.SUFFIX_APP_DRAWER_AB_BACKGROUND);
    }

    private boolean isSelectForAdd() {
        return getIntent().hasExtra(INTENT_EXTRA_SELECT_FOR_ADD);
    }

    private boolean isSelectForPick() {
        return Intent.ACTION_PICK_ACTIVITY.equals(getIntent().getAction());
    }

    private void loadState() {
        File state_file = FileUtils.getStateFile(mEngine.getBaseDir());
        JSONObject json=FileUtils.readJSONObjectFromFile(state_file);
        if(json==null) {
            json=new JSONObject();
        }
        mState = new State();
        mState.loadFieldsFromJSONObject(json, null);

        if(mState.layoutTransformCustomS!=null) {
            boolean error = false;
            StringTokenizer st = new StringTokenizer(mState.layoutTransformCustomS);
            float[] values = new float[9];
            for(int i=0; i<9; i++) {
                values[i] = Float.parseFloat(st.nextToken());
                if(Float.isNaN(values[i])) {
                    error = true;
                }
            }
            if(!error) {
                mSavedItemLayoutLocalTransformCustom.setValues(values);
            }
        }


        if(mState.layoutTransformByNameS!=null) {
            boolean error = false;
            StringTokenizer st = new StringTokenizer(mState.layoutTransformByNameS);
            float[] values = new float[9];
            for(int i=0; i<9; i++) {
                values[i] = Float.parseFloat(st.nextToken());
                if(Float.isNaN(values[i])) {
                    error = true;
                }
            }
            if(!error) {
                mSavedItemLayoutLocalTransformByName.setValues(values);
            }
        }
    }

    private void saveState() {
        try {
            if(mLayoutMode == Utils.LAYOUT_MODE_CUSTOM) {
                mSavedItemLayoutLocalTransformCustom.set(mItemLayout.getLocalTransform());
            } else if(mLayoutMode == Utils.LAYOUT_MODE_BY_NAME) {
                mSavedItemLayoutLocalTransformByName.set(mItemLayout.getLocalTransform());
            }

            float[] values = new float[9];
            mSavedItemLayoutLocalTransformCustom.getValues(values);
            mState.layoutTransformCustomS = String.valueOf(values[0]);
            for(int i=1; i<9; i++) {
                mState.layoutTransformCustomS += " "+values[i];
            }

            mSavedItemLayoutLocalTransformByName.getValues(values);
            mState.layoutTransformByNameS = String.valueOf(values[0]);
            for(int i=1; i<9; i++) {
                mState.layoutTransformByNameS += " "+values[i];
            }

            JSONObject json = JsonLoader.toJSONObject(mState, new State());
            File out = FileUtils.getStateFile(mEngine.getBaseDir());
            FileUtils.saveStringToFile(json.toString(), out);
        } catch(Exception e) {
            // pass
        }
    }

    private void setLayoutModeAnimated(int mode) {
        if(mLayoutMode != mode) {
            mNextLayoutMode = mode;
            mLayoutModeSwitchAnimation.cancel();
            mItemLayout.startAnimation(mLayoutModeSwitchAnimation);
        }
    }

    private void setLayoutMode(int mode, boolean refresh) {
        setLayoutMode(mode, refresh, false);
    }

    private void setLayoutMode(int mode, boolean refresh, boolean force) {
        if(!force && !hasMode(mode)) {
            mode = findNextAvailableMode(mode);
        }

        if(mode==mLayoutMode && !refresh) return;

        if(mBatchMode) {
            setBatchMode(false);
        }

        if(mLayoutMode!= Utils.LAYOUT_MODE_UNDEFINED && !refresh) {
            mScreen.closeAllFolders(false);

            // when switching layouts the folder opener item view is destroyed, so remove folders too
            mScreen.removeAllFolders();
        }

        ActivityManager am;
//		PackageManager pm;
        ArrayList<Item> all_items;
//		File icon_dir = FileUtils.getIconDir(this, Utils.APP_DRAWER_PAGE);
        ArrayList<Item> items = null;
        boolean honour_pinned_items = false;

        mAllDrawerPageIDs = new ArrayList<>();

        switch(mode) {
            case Utils.LAYOUT_MODE_CUSTOM:
                mAllDrawerPageIDs.add(mDrawerPage.id);
                items = new ArrayList<Item>(mDrawerPage.items);
                for(Item i : items) {
                    i.setCellT(null);
                }
                honour_pinned_items = mSystemConfig.hasSwitch(SystemConfig.SWITCH_HONOUR_PINNED_ITEMS);
                break;

            case Utils.LAYOUT_MODE_BY_NAME:
                items = new ArrayList<Item>();
                addAppsFromPage(mDrawerPage, items, mAllDrawerPageIDs);
                Collections.sort(items, Utils.sItemComparatorByNameAsc);
                break;

            case Utils.LAYOUT_MODE_FREQUENTLY_USED:
                items = new ArrayList<Item>();
                addAppsFromPage(mDrawerPage, items, mAllDrawerPageIDs);
                for(Item i : items) {
                    if(i instanceof Shortcut) {
                        Shortcut s = (Shortcut)i;
                        ComponentName cn=s.getIntent().getComponent();
                        if(mThisCn.compareTo(cn) != 0) {
                            s.mLaunchCount = s.getPage().getEngine().getShortcutLaunchCount(s);
                        }
                    }
                }
                for(int i=items.size()-1; i>=0; i--) {
                    final Item item = items.get(i);
                    if(item instanceof Shortcut && item.mLaunchCount==0) items.remove(i);
                }
                Collections.sort(items, Utils.sItemComparatorByLaunchCount);
                break;

            case Utils.LAYOUT_MODE_RECENT_APPS:
                all_items = new ArrayList<Item>();
                items = new ArrayList<Item>();
                addAppsFromPage(mDrawerPage, all_items, mAllDrawerPageIDs);
                am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
                List<ActivityManager.RecentTaskInfo> recent = am.getRecentTasks(25, ActivityManager.RECENT_IGNORE_UNAVAILABLE);
                for(ActivityManager.RecentTaskInfo info : recent) {
                    ComponentName cn = info.baseIntent.getComponent();
                    if(cn != null && cn.compareTo(mThisCn) != 0) {
                        String pkg = cn.getPackageName();
                        String cls = cn.getClassName();
                        Item i = findItemByComponent(all_items, pkg, cls);
                        if(i!=null && findItemByComponent(items, pkg, cls)==null) {
                            items.add(i);
                        }
                    }
                }
                for(Item i : all_items) {
                    if(!(i instanceof Shortcut)) {
                        items.add(i);
                    }
                }
                break;

            case Utils.LAYOUT_MODE_RECENTLY_UPDATED:
                items = new ArrayList<Item>();
                addAppsFromPage(mDrawerPage, items, mAllDrawerPageIDs);
                Collections.sort(items, Utils.sItemComparatorByLastUpdateTime);
                break;

            case Utils.LAYOUT_MODE_RUNNING:
                all_items = new ArrayList<Item>();
                items = new ArrayList<Item>();
                addAppsFromPage(mDrawerPage, all_items, mAllDrawerPageIDs);
                am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
//			pm = getPackageManager();
                List<ActivityManager.RunningAppProcessInfo> running = am.getRunningAppProcesses();
                for(ActivityManager.RunningAppProcessInfo info : running) {

//				if(info.importance==ActivityManager.RunningAppProcessInfo.IMPORTANCE_EMPTY) continue;
//				if(info.importance==ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE) continue;
//				if(info.importance==ActivityManager.RunningAppProcessInfo.IMPORTANCE_BACKGROUND) continue;
                    for(String pkg : info.pkgList) {
                        Item i = findItemByComponent(all_items, pkg, null);
                        if(i!=null && findItemByComponent(items, pkg, null)==null) {
                            items.add(i);
                        }

//					try {
//						ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
//						Bitmap icon = ((BitmapDrawable)pm.getApplicationIcon(ai)).getBitmap();
//						String label = pm.getApplicationLabel(ai).toString();
//						Item i = findItemByComponent(items, pkg);
//						if(i==null) {
//							Shortcut s = new Shortcut();
//							Intent intent = new Intent();
//							intent.setComponent(new ComponentName(pkg, ""));
//							s.init(this, Utils.findFreeItemId(items, Utils.APP_DRAWER_PAGE), new Rect(), null, label, intent, mPageConfig);
//							s.buildView(this, icon_dir, icon);
//							items.add(s);
//						}
//					} catch (Exception e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}

//					Log.i("XXX", info+" "+pkg);
//					Item i = findItemByComponent(all_items, pkg);
//					if(i == null) {
//						// TODO add dummy item ?
//					} else {
//						items.add(i);
//					}
                    }
                }
//			List<ActivityManager.RunningTaskInfo> running = am.getRunningTasks(100);
//			for(ActivityManager.RunningTaskInfo info : running) {
//				Item i = findItemByComponent(all_items, info.baseActivity.getPackageName());
//				if(i == null) {
//					// TODO add dummy item ?
//				} else {
//					items.add(i);
//				}
//			}
                for(Item i : all_items) {
                    if(!(i instanceof Shortcut)) {
                        items.add(i);
                    }
                }

                Collections.sort(items, Utils.sItemComparatorByNameAsc);
                break;
        }


        mItemLayout.cancelFling();
        Page p;
        if(mode == Utils.LAYOUT_MODE_CUSTOM) {
            p = mDrawerPage;
        } else {
            p = new MergedPage(mDrawerPage.getEngine(), mDrawerPage.config, items);
        }
        mItemLayout.setAllowMergeViews(!refresh);
        mItemLayout.setPage(p);
        mItemLayout.setHonourPinnedItems(honour_pinned_items);
        for(Item i : items) {
            mItemLayout.getItemView(i).setAlwaysPinnedAndVisible(!(i instanceof Shortcut));
        }

        if(mode!= Utils.LAYOUT_MODE_CUSTOM) {
            layoutItemsInTable(true);
        }

        String icon_text = "";
        int label_res_id = 0;
        switch(mode) {
            case Utils.LAYOUT_MODE_BY_NAME:
                icon_text = "W";
                label_res_id=R.string.mi_mode_by_name;
                break;

            case Utils.LAYOUT_MODE_CUSTOM:
                icon_text = "V";
                label_res_id=R.string.mi_mode_custom;
                break;

            case Utils.LAYOUT_MODE_FREQUENTLY_USED:
                icon_text = "R";
                label_res_id=R.string.mi_mode_frequently_used;
                break;

            case Utils.LAYOUT_MODE_RECENT_APPS:
                icon_text = "Q";
                label_res_id=R.string.mi_mode_recent_apps;
                break;

            case Utils.LAYOUT_MODE_RECENTLY_UPDATED:
                icon_text = "T";
                label_res_id=R.string.mi_mode_recently_updated;
                break;

            case Utils.LAYOUT_MODE_RUNNING:
                icon_text = "S";
                label_res_id=R.string.mi_mode_running;
                break;
        }
        mModeIcon.setText(icon_text);
        ((TextView)findViewById(R.id.drawer_mode_value)).setText(label_res_id);

        if(!refresh) {
            if(mLayoutMode == Utils.LAYOUT_MODE_CUSTOM) {
                mSavedItemLayoutLocalTransformCustom.set(mItemLayout.getLocalTransform());
            } else if(mLayoutMode == Utils.LAYOUT_MODE_BY_NAME) {
                mSavedItemLayoutLocalTransformByName.set(mItemLayout.getLocalTransform());
            }

            if(mode == Utils.LAYOUT_MODE_CUSTOM) {
                mItemLayout.setLocalTransform(mSavedItemLayoutLocalTransformCustom);
            } else if(mode == Utils.LAYOUT_MODE_BY_NAME) {
                mItemLayout.setLocalTransform(mSavedItemLayoutLocalTransformByName);
            } else {
                mItemLayout.setLocalTransform(sIdentityTransform);
            }
        }

        mLayoutMode = mode;
        mState.layoutMode = mode;
    }

    private boolean hasMode(int mode) {
        return Utils.hasAppDrawerMode(mDrawerPage.config.adDisplayedModes, mode);
    }

    private int findNextAvailableMode(int start_mode) {
        for(int m=start_mode+1; m<=Utils.LAYOUT_MODE_LAST; m++) {
            if(hasMode(m)) {
                return m;
            }
        }
        for(int m=0; m<=start_mode; m++) {
            if(hasMode(m)) {
                return m;
            }
        }
        // shouldn't be reached
        return Utils.LAYOUT_MODE_BY_NAME;
    }

    private int findPreviousAvailableMode(int start_mode) {
        for(int m=start_mode-1; m>=0; m--) {
            if(hasMode(m)) {
                return m;
            }
        }
        for(int m=Utils.LAYOUT_MODE_LAST; m>=start_mode; m--) {
            if(hasMode(m)) {
                return m;
            }
        }
        // shouldn't be reached
        return Utils.LAYOUT_MODE_BY_NAME;
    }

    private void nextLayoutMode() {
        setLayoutModeAnimated(findNextAvailableMode(mLayoutMode));
    }

    private void previousLayoutMode() {
        setLayoutModeAnimated(findPreviousAvailableMode(mLayoutMode));
    }

    private void addAppsFromPage(Page page, ArrayList<Item> items, ArrayList<Integer> allPageIds) {
        if(allPageIds != null) {
            allPageIds.add(page.id);
        }
        for(Item i : page.items) {
            if(i.getClass()==Folder.class) {
                addAppsFromPage(((Folder)i).getOrLoadFolderPage(), items, allPageIds);
            } else {
                items.add(i);
            }
        }
    }

    private void gatherCheckedItems(Page page, ArrayList<Item> items) {
        ItemLayout[] ils = mScreen.getItemLayoutsForPage(page.id);
        if(ils.length == 0) {
            // view not built, nothing to do
            return;
        }
        ItemLayout il = ils[0];
        for(Item i : page.items) {
            ItemView itemView = il.getItemView(i);
            if(itemView != null) {
                // in alphabetical mode for instance, folders are not displayed and don't have views
                boolean checked = itemView.isChecked();
                if (i.getClass() == Folder.class) {
                    Page fp = ((Folder) i).getOrLoadFolderPage();
                    if (checked) {
                        addAppsFromPage(fp, items, null);
                    } else {
                        gatherCheckedItems(fp, items);
                    }
                } else {
                    if (checked) {
                        items.add(i);
                    }
                }
            }
        }
    }

    private Item findItemByComponent(ArrayList<Item> items, String pkg, String cls) {
        for(Item i : items) {
            if(i.getClass()==Shortcut.class) {
                ComponentName cn = ((Shortcut)i).getIntent().getComponent();
                if(cn.getPackageName().equals(pkg)) {
                    if(cls==null) {
                        return i;
                    } else {
                        if(cn.getClassName().equals(cls)) {
                            return i;
                        }
                    }
                }
            }
        }

        return null;
    }

    private void layoutItemsInTable(boolean full_layout) {
        ItemLayout il = mItemLayout;
        Page page = il.getPage();
        boolean horizontal = page.config.scrollingDirection==PageConfig.ScrollingDirection.X;

        final int width = il.getWidth();
        final int height = il.getHeight();
        if(width==0 || height==0) {
//            for(Item i : page.items) {
//                if (!(i instanceof Shortcut)) continue;
//                if (mItemLayout.getItemView(i).isViewVisible()) {
//                    i.setCellT(new Rect(0, 0, 1, 1));
//                }
//            }
            return;
        }

        int x_max=(int) (width /(il.getCurrentScale()*il.getCellWidth()));
        if(x_max<1) x_max=1;
        if(x_max>40) x_max=40;
        int y_max=(int) (height /(il.getCurrentScale()*il.getCellHeight()));
        if(y_max<1) y_max=1;

        int x=0, y=0, px=0;

        mVisibleItemsCount=0;


        for(Item i : page.items) {
            if(!(i instanceof Shortcut)) continue;
            if(mItemLayout.getItemView(i).isViewVisible()) {
                i.setCellT(new Rect(x, y, x+1, y+1));

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
            mVisibleItemsCount++;
        }

        if(full_layout) {
            il.requestLayout();
        } else {
            il.reLayoutItems();
        }
    }

    @SuppressLint("DefaultLocale")
    private void filterApps(String filter) {
        ArrayList<Item> items=mItemLayout.getPage().items;
        if(filter!=null) {
            final String filter_l=filter.toLowerCase();
            boolean empty = filter.equals("");
            for(Item i : items) {
                if(i instanceof Shortcut) {
                    Shortcut s = (Shortcut) i;
                    boolean match = !empty && s.getLabel().toLowerCase().contains(filter_l);
                    s.setVisible(match);
                    ShortcutView shortcutView = (ShortcutView) mItemLayout.getItemView(s);
                    shortcutView.highlightText(match ? filter_l : null);
                } else {
                    i.setVisible(false);
                }
            }
            if(mActionBar.getDisplayedChild() == ACTION_BAR_CHILD_SEARCH) {
                Collections.sort(items, new Comparator<Item>() {
                    @Override
                    public int compare(Item item1, Item item2) {
                        String s1 = item1 instanceof Shortcut  ? ((Shortcut)item1).getLabel().toLowerCase() : "";
                        String s2 = item2 instanceof Shortcut  ? ((Shortcut)item2).getLabel().toLowerCase() : "";
                        boolean s1_start = s1.startsWith(filter_l);
                        boolean s2_start = s2.startsWith(filter_l);
                        if(s1_start && !s2_start) {
                            return -1;
                        } else if(!s1_start && s2_start) {
                            return 1;
                        } else {
                            return Utils.sItemNameCollator.compare(s1, s2);
                        }
                    }
                });
            }
        } else {
            for(Item i : items) {
                i.setVisible(true);
                if(i instanceof Shortcut) {
                    ShortcutView shortcutView = (ShortcutView) mItemLayout.getItemView(i);
                    shortcutView.highlightText(null);
                }
            }
            Collections.sort(items, Utils.sItemComparatorByNameAsc);
        }
        if(mLayoutMode== Utils.LAYOUT_MODE_CUSTOM) {
            mItemLayout.computeBoundingBox(mItemLayout.getWidth(), mItemLayout.getHeight());
            mItemLayout.animateZoomTo(ItemLayout.POSITION_FULL_SCALE, 1);
        } else {
            layoutItemsInTable(true);
            mItemLayout.animateZoomTo(ItemLayout.POSITION_ORIGIN, 1);
        }
    }

    private void setSearchMode(boolean on) {
        final int displayedChild = mActionBar.getDisplayedChild();
        if(on && displayedChild == ACTION_BAR_CHILD_SEARCH) return;
        if(!on && displayedChild != ACTION_BAR_CHILD_SEARCH) return;

        mActionBar.setDisplayedChild(on ? ACTION_BAR_CHILD_SEARCH : ACTION_BAR_CHILD_DRAWER_ACTIONS);
        InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
        if(mSearchFocusedItemView != null) {
            mSearchFocusedItemView.setFocused(false);
        }
        filterApps(null);
        final ItemLayout il=mItemLayout;
        if(on) {
            if(mEditMode) leaveEditMode();
            closeBubble();
            mLayoutModeBeforeSearch=mLayoutMode;
            if(mLayoutMode != Utils.LAYOUT_MODE_BY_NAME) {
                setLayoutMode(Utils.LAYOUT_MODE_BY_NAME, false, true);
            }
            mSearchField.setText("");
            mSearchField.requestFocus();
            int w = il.getWidth();
            int h = il.getHeight();
            if(w != 0 && h != 0) {
                il.setLayoutParams(new FrameLayout.LayoutParams(w, h));
            }
            imm.showSoftInput(mSearchField, 0);
            showCustomActionBar(false);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScreen.hideStatusBarIfNeeded();
                }
            }, 1000);
            mSearchMode = true;
        } else {
            imm.hideSoftInputFromWindow(mSearchField.getWindowToken(), 0);
            if(mLayoutModeBeforeSearch != mLayoutMode) {
                setLayoutMode(mLayoutModeBeforeSearch, false);
            }
            il.animateZoomTo(ItemLayout.POSITION_ORIGIN, 1);
            il.postDelayed(new Runnable() {
                @Override
                public void run() {
                    il.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                }
            }, 1000);
            if(mDrawerPage.config.adHideActionBar) {
                hideCustomActionBar();
            }
            mScreen.hideStatusBarIfNeeded();
            mSearchMode = false;
        }
    }

    private void setBatchMode(boolean on) {
        mBatchMode = on;
        final int displayedChild = mActionBar.getDisplayedChild();
        if(on && displayedChild == ACTION_BAR_CHILD_BATCH) return;
        if(!on && displayedChild != ACTION_BAR_CHILD_BATCH) return;

        ArrayList<Item> items = new ArrayList<Item>();
        gatherCheckedItems(mDrawerPage, items);
        for(Item item : items) {
            for(ItemView itemView : mScreen.getItemViewsForItem(item)) {
                itemView.setChecked(false);
            }
        }
        mBatchCheckedCount = 0;

        if(on) {
            mPreviouslyDisplayedChild = mActionBar.getDisplayedChild();
            mActionBar.setDisplayedChild(ACTION_BAR_CHILD_BATCH);
            showCustomActionBar(false);
            mAndroidActionBarDisplayedBeforeBatch = mIsAndroidActionBarDisplayed;
            if(mIsAndroidActionBarDisplayed) {
                mScreen.hideAndroidActionBar();
            }
        } else {
            mActionBar.setDisplayedChild(mPreviouslyDisplayedChild);
            if(mDrawerPage.config.adHideActionBar) {
                hideCustomActionBar();
            }
            if(mAndroidActionBarDisplayedBeforeBatch) {
                mScreen.showAndroidActionBar(mABOnCreateOptionsMenu, mABOnOptionsItemSelected);
            }
        }
    }

    private void checkUncheckItemView(ItemView itemView) {
        boolean new_state = !itemView.isChecked();
        itemView.setChecked(new_state);
        mBatchCheckedCount += new_state ? 1 : -1;
        updateBatchCheckedCount(mBatchCheckedCount);
        if(mBatchCheckedCount == 0) {
            setBatchMode(false);
        }
    }

    private void updateBatchCheckedCount(int count) {
        mBatchCount.setText(getString(R.string.ad_bc)+" "+count);
    }

    @Override
    public void onEditTextImeBackPressed() {
        setSearchMode(false);
    }

    @Override
    protected void addFolder() {
        if(LLApp.get().isFreeVersion()) {
            LLApp.get().showFeatureLockedDialog(this);
            return;
        }

        ItemLayout il=mScreen.getTargetOrTopmostItemLayout();
        Page page = il.getPage();

        Item item = Utils.addFolder(page, mScreen.getLastTouchedAddX(), mScreen.getLastTouchedAddY(), il.getCurrentScale(), true, getString(net.pierrox.lightning_launcher.R.string.default_folder_name));
        item.setAppDrawerHidden(mItemLayout.getAppDrawerHiddenHandling() == Item.APP_DRAWER_HIDDEN_ONLY_HIDDEN);
        mUndoStack.storePageAddItem(item);
        enterEditMode(il, item);

        if(!page.isFolder()) {
            il.ensureCellVisible(item.getCell());
        }
    }

    @Override
    protected boolean canMoveOutOfFolder() {
        return mLayoutMode == Utils.LAYOUT_MODE_CUSTOM;
    }

    @Override
    protected void configureBubbleForItem(int mode, ItemView itemView, List shortcuts) {
        Item item = itemView.getItem();
        if(mode == BUBBLE_MODE_ITEM_EM) {
            boolean is_folder = item instanceof Folder;
            boolean is_page_indicator = item instanceof PageIndicator;

            if(mLayoutMode== Utils.LAYOUT_MODE_CUSTOM) {
                addBubbleItem(R.id.mi_edit, R.string.mi_customize);
                addBubbleItem(R.id.mi_position, R.string.mi_position);
            }
            if(is_folder || is_page_indicator) {
                addBubbleItem(R.id.mi_remove, R.string.mi_remove);
            } else {
                addBubbleItem(R.id.mi_actions, R.string.mi_actions);
            }
            if(item.getPage().isFolder()) addBubbleItem(R.id.mi_move_out_of_folder, R.string.mi_move_out_of_folder);
        } else if(mode == BUBBLE_MODE_ITEM_POSITION) {
            ItemConfig ic = item.getItemConfig();
            addBubbleItem(R.id.mi_pin, ic.pinMode!= ItemConfig.PinMode.NONE ? R.string.mi_unpin : R.string.mi_pin);
        } else {
            super.configureBubbleForItem(mode, itemView, shortcuts);
        }
    }

    @Override
    protected void configureBubbleForContainer(int mode, ItemLayout il) {
        if(mode == BUBBLE_MODE_DRAWER_MENU) {
            boolean is_layout_custom=(mLayoutMode== Utils.LAYOUT_MODE_CUSTOM);
            addBubbleItem(R.id.mi_es_refresh, R.string.mi_es_refresh);
            addBubbleItem(R.id.mi_v, R.string.v_m);
            if(is_layout_custom) {
                addBubbleItem(R.id.mi_i, R.string.mi_i);
            }
            if(mGlobalConfig.runScripts) {
                addBubbleItem(R.id.mi_s, R.string.mi_s);
            }
            addBubbleItem(R.id.mi_dm_customize, R.string.mi_es_settings);
        } else if(mode == BUBBLE_MODE_DRAWER_VISIBILITY) {
            int handling = il.getAppDrawerHiddenHandling();
            addBubbleItem(R.id.mi_va, toBold(R.string.v_a, handling == Item.APP_DRAWER_HIDDEN_ALL));
            addBubbleItem(R.id.mi_vov, toBold(R.string.v_ov, handling == Item.APP_DRAWER_HIDDEN_ONLY_VISIBLE));
            addBubbleItem(R.id.mi_voh, toBold(R.string.v_oh, handling == Item.APP_DRAWER_HIDDEN_ONLY_HIDDEN));
        } else if(mode == BUBBLE_MODE_DRAWER_MODE) {
            if(hasMode(Utils.LAYOUT_MODE_CUSTOM)) addBubbleItem(R.id.mi_mode_custom, R.string.mi_mode_custom);
            if(hasMode(Utils.LAYOUT_MODE_BY_NAME)) addBubbleItem(R.id.mi_mode_by_name, R.string.mi_mode_by_name);
            if(hasMode(Utils.LAYOUT_MODE_FREQUENTLY_USED)) addBubbleItem(R.id.mi_mode_frequently_used, R.string.mi_mode_frequently_used);
            if(hasMode(Utils.LAYOUT_MODE_RECENT_APPS)) addBubbleItem(R.id.mi_mode_recent_apps, R.string.mi_mode_recent_apps);
            if(hasMode(Utils.LAYOUT_MODE_RECENTLY_UPDATED)) addBubbleItem(R.id.mi_mode_recently_updated, R.string.mi_mode_recently_updated);
            if(hasMode(Utils.LAYOUT_MODE_RUNNING)) addBubbleItem(R.id.mi_mode_running, R.string.mi_mode_running);
        } else {
            super.configureBubbleForContainer(mode, il);
        }
    }

    private CharSequence toBold(int text, boolean bold) {
        CharSequence string = getString(text);
        if(bold) {
            SpannableString spannedLabel = new SpannableString(string);
            spannedLabel.setSpan(new StyleSpan(Typeface.BOLD), 0, string.length(), 0);
            string = spannedLabel;
        }

        return string;
    }

    @Override
    protected boolean isDialogAddItemEnabled(int id) {
        return id == AddItemDialog.AI_PAGE_INDICATOR || id == AddItemDialog.AI_FOLDER;
    }

    @Override
    protected boolean showPluginsInAddItemDialog() {
        return false;
    }

    @Override
    protected boolean displayBubbleTitleForMode(int mode) {
        return displayBubbleButtonsForMode(mode);
    }

    @Override
    protected boolean displayBubbleButtonsForMode(int mode) {
        switch(mode) {
            case BUBBLE_MODE_DRAWER_MENU: return true;
            case BUBBLE_MODE_DRAWER_MODE: return false;
            case BUBBLE_MODE_DRAWER_VISIBILITY: return false;
            case BUBBLE_MODE_ITEM_NO_EM: return mLayoutMode == Utils.LAYOUT_MODE_CUSTOM;
            default: return super.displayBubbleButtonsForMode(mode);
        }
    }

    @Override
    public void onClick(View v) {
        ItemLayout il;
        boolean close_bubble=true;

        int id = v.getId();
        switch(id) {
            case R.id.drawer_mode_grp:
                v.getHitRect(mTempRect);
                mTempRect.top = mTempRect.bottom; // hack, force the arrow to be positioned at the exact height
                if(!closeBubble() || mBubbleMode!= BUBBLE_MODE_DRAWER_MODE) {
                    openBubble(BUBBLE_MODE_DRAWER_MODE, mScreen.getTargetOrTopmostItemLayout(), mTempRect);
                }
                close_bubble = false;
                break;

            case R.id.drawer_zoom:
                mScreen.zoomInOrOut(mScreen.getTopmostItemLayout());
                break;

            case R.id.drawer_search:
                setSearchMode(true);
                break;

            case R.id.drawer_more:
                mScreen.setLastTouchEventForMenuBottom(false);
                v.getHitRect(mTempRect);
                mTempRect.top = mTempRect.bottom; // hack, force the arrow to be positioned at the exact height
                if(!closeBubble() || mBubbleMode!= BUBBLE_MODE_DRAWER_MENU) {
                    openBubble(BUBBLE_MODE_DRAWER_MENU, mScreen.getTopmostItemLayout(), mTempRect);
                }
                close_bubble = false;
                break;

            case R.id.mi_es_add_folder:
                addFolder();
                break;

            case R.id.mi_es_add_pi:
                addPageIndicator();
                break;

            case R.id.mi_es_edit_layout:
                if(!mEditMode) {
                    enterEditMode(mScreen.getTargetOrTopmostItemLayout(), null);
                } else {
                    leaveEditMode();
                }
                break;

            case R.id.mi_v:
                openBubble(BUBBLE_MODE_DRAWER_VISIBILITY, mBubbleItemLayout, null);
                close_bubble = false;
                break;

            case R.id.mi_va:
            case R.id.mi_vov:
            case R.id.mi_voh:
                int vis = 0;
                switch(id) {
                    case R.id.mi_va: vis = Item.APP_DRAWER_HIDDEN_ALL; break;
                    case R.id.mi_voh: vis = Item.APP_DRAWER_HIDDEN_ONLY_HIDDEN; break;
                    case R.id.mi_vov: vis = Item.APP_DRAWER_HIDDEN_ONLY_VISIBLE; break;
                }

                il = mScreen.getTargetOrTopmostItemLayout();
                il.setAppDrawerHiddenHandling(vis);
                if(mLayoutMode != Utils.LAYOUT_MODE_CUSTOM) {
                    layoutItemsInTable(true);

                    il.getHitRect(mTempRect);
                    mTempRectF.set(mTempRect);
                    il.getLocalInverseTransform().mapRect(mTempRectF);
                    mTempRectF.round(mTempRect);
                    il.computeBoundingBox(il.getWidth(), il.getHeight());
                    if(!il.getItemsBoundingBox().contains(mTempRect)) {
                        il.animateZoomTo(ItemLayout.POSITION_ORIGIN, 1);
                    }
                } else {
                    il.requestLayout();
                }
                if(mEditMode) {
                    for (Item item : mEditItemLayout.getPage().items) {
                        ItemView itemView = mEditItemLayout.getItemView(item);
                        if (itemView.isSelected() && !itemView.isViewVisible()) {
                            itemView.setSelected(false);
                        }
                    }
                }
                break;

            case R.id.mi_es_refresh:
                if(mLayoutMode== Utils.LAYOUT_MODE_RECENT_APPS || mLayoutMode== Utils.LAYOUT_MODE_RUNNING) {
                    setLayoutMode(mLayoutMode, true);
                } else {
                    refreshAppDrawerItems(true);
                }
                break;

            case R.id.mi_es_settings:
                il = mScreen.getTargetOrTopmostItemLayout();
                PhoneUtils.startSettings(this, new ContainerPath(il), !il.getPage().isFolder());
                break;

            case R.id.mi_mode_custom:
                if((mSystemConfig.hints&SystemConfig.HINT_MY_DRAWER) == 0) {
                    showDialog(DIALOG_MY_DRAWER);
                    mSystemConfig.hints |= SystemConfig.HINT_MY_DRAWER;
                }
                setLayoutModeAnimated(Utils.LAYOUT_MODE_CUSTOM);
                break;

            case R.id.mi_mode_by_name:
                if(mEditMode) leaveEditMode();
                setLayoutModeAnimated(Utils.LAYOUT_MODE_BY_NAME);
                break;

            case R.id.mi_mode_frequently_used:
                if(mEditMode) leaveEditMode();
                setLayoutModeAnimated(Utils.LAYOUT_MODE_FREQUENTLY_USED);
                break;

            case R.id.mi_mode_recent_apps:
                if(mEditMode) leaveEditMode();
                setLayoutModeAnimated(Utils.LAYOUT_MODE_RECENT_APPS);
                break;

            case R.id.mi_mode_recently_updated:
                if(mEditMode) leaveEditMode();
                setLayoutModeAnimated(Utils.LAYOUT_MODE_RECENTLY_UPDATED);
                break;

            case R.id.mi_mode_running:
                if(mEditMode) leaveEditMode();
                setLayoutModeAnimated(Utils.LAYOUT_MODE_RUNNING);
                break;

            case R.id.mi_hide_unhide:
                ArrayList<ItemView> itemViews = getActionItemViews();
                mUndoStack.storeGroupStart();
                for (ItemView itemView : itemViews) {
                    Item i = itemView.getItem();
                    mUndoStack.storeItemState(i);
                    boolean will_hide = !i.isAppDrawerHidden();
                    if(mEditMode && will_hide && itemView.isSelected()) {
                        itemView.setSelected(false);
                    }
                    i.setAppDrawerHidden(will_hide);
                    i.getPage().setModified();
                    if(mLayoutMode == Utils.LAYOUT_MODE_CUSTOM) {
                        itemView.getParentItemLayout().requestLayout();
                    }
                }
                mUndoStack.storeGroupEnd();

                if(mLayoutMode != Utils.LAYOUT_MODE_CUSTOM) {
                    layoutItemsInTable(true);
                }

                break;

            case R.id.mi_add_to_launcher:
                Shortcut shortcut = (Shortcut) mBubbleItemView.getItem();
                addItemToLauncher(shortcut);
                Toast.makeText(this, getString(R.string.app_added, shortcut.getLabel()), Toast.LENGTH_SHORT).show();
                break;

            case R.id.mi_kill:
                if(LLApp.get().isFreeVersion()) {
                    LLApp.get().showFeatureLockedDialog(this);
                } else {
                    // use super class behavior but in addition, trigger a refresh if in running view
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (mLayoutMode == Utils.LAYOUT_MODE_RUNNING) {
                                setLayoutMode(Utils.LAYOUT_MODE_RUNNING, true);
                            }
                        }
                    }, 2000);
                    super.onClick(v);
                }
                break;

            case R.id.mi_i:
                if(mScreen.getTargetItemLayout() == null) {
                    findViewById(R.id.drawer_more).getHitRect(mTempRect);
                    openBubble(BUBBLE_MODE_ITEMS, mBubbleItemLayout, mTempRect);
                } else {
                    super.onClick(v);
                }
                close_bubble = false;
                break;

            case R.id.batch_ok:
            case R.id.batch_count:
                setBatchMode(false);
                break;

            case R.id.batch_add:
                doAddForBatchMode();
                break;

            default:
                super.onClick(v);
                return;
        }

        if(close_bubble) {
            closeBubble();
        }
    }

    @Override
    protected void menuActionAddItem() {
        ensureMyDrawerMode();
        super.menuActionAddItem();
    }

    @Override
    protected void menuActionEdit() {
        ensureMyDrawerMode();
        super.menuActionEdit();
    }

    @Override
    protected void menuActionConfirmRemoveItem() {
        final ArrayList<Item> actionItems = getActionItems();
        if(actionItems.size()==1) {
            Item item = actionItems.get(0);
            if(item.getClass() == Shortcut.class) {
                startActivity(new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + Utils.getPackageNameForItem(item))));
                return;
            }
        }

        super.menuActionConfirmRemoveItem();
    }

    private void ensureMyDrawerMode() {
        if(mLayoutMode != Utils.LAYOUT_MODE_CUSTOM) {
            setLayoutMode(Utils.LAYOUT_MODE_CUSTOM, false, true);
            Toast toast = Toast.makeText(this, R.string.ad_mdh, Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }
    }

    private void doAddForBatchMode() {
        ArrayList<Item> items = new ArrayList<>();
        gatherCheckedItems(mDrawerPage, items);
        for(Item item : items) {
            addItemToLauncher(item);
        }
        setBatchMode(false);
        finish();
    }

    @Override
    protected void menuActionRemoveItem() {
        mUndoStack.storeGroupStart();
        ArrayList<ItemView> itemViews = getActionItemViews();
        for (ItemView itemView : itemViews) {
            Item item = itemView.getItem();
            // hack : in the app drawer, views are recreated because of the removal
            ItemView[] ivs = mScreen.getItemViewsForItem(item);
            itemView = ivs[0];
            if(item instanceof Folder) {
                moveFolderItemsToDrawer(itemView);
            }

            ivs = mScreen.getItemViewsForItem(item);
            itemView = ivs[0];
            if(item instanceof Folder || item instanceof PageIndicator) {
                itemView.setSelected(false);
                Page page = item.getPage();
                mUndoStack.storePageRemoveItem(item);
                page.removeItem(item, false);
            }
        }
        mUndoStack.storeGroupEnd();
    }

    @Override
    public int[] onHierarchyScreenGetRootPages() {
        return new int[] { Page.APP_DRAWER_PAGE };
    }

    private void addItemToLauncher(Item item) {
        Page pageTo = item.getPage().getEngine().getOrLoadPage(LLApp.get().getActiveDashboardPage());
        if(item.getClass() == Folder.class) {
            Folder f = (Folder) item;
            Intent intent = new Intent(this, Dashboard.class);
            intent.putExtra(LightningIntent.INTENT_EXTRA_ACTION, GlobalConfig.OPEN_FOLDER);
            intent.putExtra(LightningIntent.INTENT_EXTRA_DATA, String.valueOf(f.getFolderPageId()));
            Shortcut shortcut = Utils.copyShortcut(item, pageTo, Utils.POSITION_AUTO, Utils.POSITION_AUTO, 1);
            shortcut.setIntent(intent);
        } else {
            Utils.copyShortcut(item, pageTo, Utils.POSITION_AUTO, Utils.POSITION_AUTO, 1);
        }
    }

    private void refreshAppDrawerItems(boolean animate_hide) {
        mScreen.closeAllFolders(true);

        findViewById(R.id.drawer_progress).setVisibility(View.VISIBLE);
        mItemLayout.setVisibility(View.GONE);
        if(animate_hide) {
            mItemLayout.startAnimation(AnimationUtils.loadAnimation(AppDrawerX.this, android.R.anim.fade_out));
        }

        final View drawer_actions = findViewById(R.id.drawer_actions);
        drawer_actions.setVisibility(View.INVISIBLE);

        final Handler handler = new Handler();

        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                Utils.refreshAppDrawerShortcuts(mDrawerPage.getEngine(), handler);
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                if(mItemLayout != null) {
                    mSavedItemLayoutLocalTransformCustom.reset();
                    mSavedItemLayoutLocalTransformByName.reset();
                    mItemLayout.setLocalTransform(mSavedItemLayoutLocalTransformCustom);
                    drawer_actions.setVisibility(View.VISIBLE);
                    setLayoutMode(mLayoutMode, true);
                }
                if(mItemLayout != null) {
                    findViewById(R.id.drawer_progress).setVisibility(View.GONE);
                    mItemLayout.setVisibility(View.VISIBLE);
                    mItemLayout.startAnimation(AnimationUtils.loadAnimation(AppDrawerX.this, android.R.anim.fade_in));
                }
            }
        }.execute((Void) null);
    }

    @Override
    public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
        ItemView itemView = getFirstVisibleItemView();
        if(itemView != null) {
            mScreen.launchItem(itemView);
            return true;
        } else {
            setSearchMode(false);
            return false;
        }
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        // pass
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        filterApps(charSequence.toString());
        if(mSearchFocusedItemView != null) {
            mSearchFocusedItemView.setFocused(false);
        }
        ItemView itemView = getFirstVisibleItemView();
        if(itemView != null) {
            mSearchFocusedItemView = itemView;
            mSearchFocusedItemView.setFocused(true);
        }
    }

    @Override
    public void afterTextChanged(Editable editable) {
        // pass
    }

    private ItemView getFirstVisibleItemView() {
        for(Item item : mItemLayout.getPage().items) {
            if(item.isVisible()) {
                return mItemLayout.getItemView(item);
            }
        }
        return null;
    }

    private void showCustomActionBar(boolean hide_later) {
        if (mActionBar.getVisibility() != View.VISIBLE) {
            mActionBar.setVisibility(View.VISIBLE);
            TranslateAnimation ta = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, -1, Animation.RELATIVE_TO_SELF, 0);
            AlphaAnimation aa = new AlphaAnimation(0, 1);
            AnimationSet as = new AnimationSet(true);
            as.addAnimation(ta);
            as.addAnimation(aa);
            as.setDuration(ACTION_BAR_ANIM_IN_DURATION);
            mActionBar.startAnimation(as);
        }

        mHandler.removeCallbacks(mHideCustomActionBarRunnable);
        if(hide_later && mActionBar.getDisplayedChild() == ACTION_BAR_CHILD_DRAWER_ACTIONS) {
            mHandler.postDelayed(mHideCustomActionBarRunnable, ACTION_BAR_HIDE_DELAY);
        }
    }

    private void hideCustomActionBar() {
        mActionBar.setVisibility(View.GONE);
        TranslateAnimation ta = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, -1);
//            AlphaAnimation aa = new AlphaAnimation(1, 0);
        AnimationSet as = new AnimationSet(true);
//            as.addAnimation(aa);
        as.addAnimation(ta);
        as.setDuration(ACTION_BAR_ANIM_OUT_DURATION);
        mActionBar.startAnimation(as);
    }

    private Runnable mHideCustomActionBarRunnable = new Runnable() {
        @Override
        public void run() {
            hideCustomActionBar();
        }
    };

    private void moveFolderItemsToDrawer(ItemView folderItemView) {
        FolderView fv = mScreen.findFolderView(folderItemView, null);
        if(fv == null) {
            fv = mScreen.openFolder((Folder)folderItemView.getItem(), folderItemView, null, true);
        }
        ItemLayout ilFrom = fv.getItemLayout();
        Page folder_page = ilFrom.getPage();
        ArrayList<Item> items = folder_page.items;
        for(int j=items.size()-1; j>=0; j--) {
            Item i=items.get(j);
            ItemView itemView = ilFrom.getItemView(i);
            if(i.getClass()==Folder.class) {
                moveFolderItemsToDrawer(itemView);
            }
            saveInitialItemViewGeometry(itemView);
            int old_id = i.getId();
            Item newItem = Utils.moveItem (i, mDrawerPage, Utils.POSITION_AUTO, Utils.POSITION_AUTO, 1, Item.NO_ID);
            mUndoStack.storePageItemMove(mItemLayout.getItemView(newItem), old_id, mOriginalItemsGeometry.get(old_id));
        }
    }

    private class MergedPage extends Page {
        private HashSet<Integer> mPageIds;

        public MergedPage(LightningEngine engine, PageConfig c, ArrayList<Item> items) {
            super(engine, Page.MERGED_APP_DRAWER_PAGE);
            this.config = c;
            this.items = items;
            this.id = Page.APP_DRAWER_PAGE;
            mPageIds = new HashSet<>();

            for(Item i : items) {
                mPageIds.add(Utils.getPageForItem(i));
            }
        }

        @Override
        public void setCurrentViewSize(int width, int height, float cell_width, float cell_height) {
            super.setCurrentViewSize(width, height, cell_width, cell_height);
            for(int i : mPageIds) {
                mEngine.getOrLoadPage(i).setCurrentViewSize(width, height, cell_width, cell_height);
            }
        }
    }


    private class AppDrawerScreen extends DashboardScreen {
        public AppDrawerScreen(Context context, int content_view) {
            super(context, content_view);
        }

        @Override
        public ScreenIdentity getIdentity() {
            return ScreenIdentity.APP_DRAWER;
        }

        @Override
        public Page getCurrentRootPage() {
            return mDrawerPage;
        }

        @Override
        public ItemLayout loadRootItemLayout(int page, boolean reset_navigation_history, boolean displayImmediately, boolean animate) {
            if(page == mDrawerPage.id) {
                setLayoutMode(mLayoutMode, true);
                return mItemLayout;
            } else {
                return loadRootItemLayoutOffscreen(page, reset_navigation_history, displayImmediately, animate);
            }
        }

        @Override
        public ItemLayout getCurrentRootItemLayout() {
            return mItemLayout;
        }

        @Override
        public ItemLayout[] getItemLayoutsForPage(int pageId) {
            if(mLayoutMode == Utils.LAYOUT_MODE_CUSTOM) {
                return super.getItemLayoutsForPage(pageId);
            } else {
                // pretend that the main item layout fits all pages (it does because it includes views from item belonging to other folders)
                if(mAllDrawerPageIDs.contains(pageId)) {
                    return new ItemLayout[]{mItemLayout};
                } else {
                    return new ItemLayout[0];
                }
            }
        }

        @Override
        public void showAndroidActionBar(Function onCreateOptionsMenu, Function onOptionsItemSelected) {
            super.showAndroidActionBar(onCreateOptionsMenu, onOptionsItemSelected);
            if(isAndroidActionBarSupported()) {
                hideCustomActionBar();
            }
        }

        @Override
        public void hideAndroidActionBar() {
            super.hideAndroidActionBar();
            if(isAndroidActionBarSupported()) {
                if(!mDrawerPage.config.adHideActionBar) {
                    showCustomActionBar(false);
                }
            }
        }

        @Override
        public void setActivePage(int page) {
            // pass, no-op
        }

        @Override
        public boolean runAction(LightningEngine engine, String source, EventAction ea, ItemLayout il, ItemView itemView) {
            switch (ea.action) {
                case GlobalConfig.PREVIOUS_DESKTOP:
                case GlobalConfig.NEXT_DESKTOP:
                case GlobalConfig.GO_HOME:
                case GlobalConfig.SELECT_DESKTOP_TO_GO_TO:
                    // skip these actions
                    break;

                case GlobalConfig.GO_HOME_ZOOM_TO_ORIGIN:
                    zoomToOrigin(getCurrentRootItemLayout());
                    break;

                default:
                    return super.runAction(engine, source, ea, il, itemView);
            }

            processNextAction(engine, source, ea, il, itemView);
            return true;
        }

        @Override
        public void onItemLayoutLongClicked(ItemLayout item_layout, int x, int y) {
            setLastTouchEventForItemLayout(item_layout, x, y);
            if(mEditMode) {
                mEditItemLayout.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                unselectAllItems();
            }
            openBubble(BUBBLE_MODE_DRAWER_MENU);
        }

        @Override
        public void onItemLayoutZoomChanged(float scale) {
            if(mLayoutMode != Utils.LAYOUT_MODE_CUSTOM) {
                layoutItemsInTable(false);
            }
        }

        @Override
        public void onItemLayoutPinchStart() {
            if(mLayoutMode != Utils.LAYOUT_MODE_CUSTOM) {
                mScaleBeforePinch=mItemLayout.getCurrentScale();
            }
        }

        @Override
        public boolean onItemLayoutPinch(float scale) {
            if(mLayoutMode != Utils.LAYOUT_MODE_CUSTOM) {
                mItemLayout.zoomTo(mScaleBeforePinch * scale);
                return false;
            } else {
                return true;
            }
        }

        @Override
        public void onItemLayoutPinchEnd(boolean from_user) {
            if(mLayoutMode != Utils.LAYOUT_MODE_CUSTOM) {
                layoutItemsInTable(true);

                if(from_user) {
                    // need an epsilon to ensure that floating point rounding errors do not reach the upper cell
                    int x_max=(int) (mItemLayout.getWidth()/(mItemLayout.getCurrentScale()*mItemLayout.getCellWidth()));
                    if(x_max<1) x_max=1;
                    if(x_max>40) x_max=40;
                    float scale=mItemLayout.getWidth()/(float)(x_max*mItemLayout.getCellWidth())-0.001f;
                    mItemLayout.animateZoomTo(ItemLayout.POSITION_FREE, scale);
                }
            }
        }

        @Override
        public void onItemLayoutOnLayoutDone(ItemLayout item_layout) {
            if(mLayoutMode != Utils.LAYOUT_MODE_CUSTOM && item_layout.getPage().id == Page.APP_DRAWER_PAGE) {
                layoutItemsInTable(false);
            }
        }

        @Override
        public void onItemLayoutPositionChanged(ItemLayout il, float mCurrentDx, float mCurrentDy, float mCurrentScale) {
            if(mDrawerPage != null && mDrawerPage.config.adHideActionBar && mDrawerPage.config.adDisplayABOnScroll) {
                if(mActionBar.getDisplayedChild() != ACTION_BAR_CHILD_SEARCH && !mIsAndroidActionBarDisplayed) {
                    showCustomActionBar(true);
                }
            }
            super.onItemLayoutPositionChanged(il, mCurrentDx, mCurrentDy, mCurrentScale);
        }

        @Override
        public void onItemLayoutSwipeLeft(ItemLayout item_layout) {
            final PageConfig config = mDrawerPage.config;
            if(config.swipeRight.action == GlobalConfig.UNSET && config.scrollingDirection!=PageConfig.ScrollingDirection.X && !mSearchMode) {
                previousLayoutMode();
            } else {
                super.onItemLayoutSwipeLeft(item_layout);
            }
        }

        @Override
        public void onItemLayoutSwipe2Left(ItemLayout item_layout) {
            final PageConfig config = mDrawerPage.config;
            if(config.swipe2Left.action == GlobalConfig.UNSET && config.scrollingDirection!=PageConfig.ScrollingDirection.X && !mSearchMode) {
                previousLayoutMode();
            } else {
                super.onItemLayoutSwipe2Left(item_layout);
            }
        }

        @Override
        public void onItemLayoutSwipeRight(ItemLayout item_layout) {
            final PageConfig config = mDrawerPage.config;
            if(config.swipeRight.action == GlobalConfig.UNSET && config.scrollingDirection!=PageConfig.ScrollingDirection.X && !mSearchMode) {
                nextLayoutMode();
            } else {
                super.onItemLayoutSwipeRight(item_layout);
            }
        }

        @Override
        public void onItemLayoutSwipe2Right(ItemLayout item_layout) {
            final PageConfig config = mDrawerPage.config;
            if(config.swipe2Right.action == GlobalConfig.UNSET && config.scrollingDirection!=PageConfig.ScrollingDirection.X && !mSearchMode) {
                nextLayoutMode();
            } else {
                super.onItemLayoutSwipe2Right(item_layout);
            }
        }

        @Override
        public void onItemLayoutSwipeUp(ItemLayout item_layout) {
            final PageConfig config = mDrawerPage.config;
            if(config.swipeUp.action == GlobalConfig.UNSET && config.scrollingDirection==PageConfig.ScrollingDirection.X && !mSearchMode) {
                nextLayoutMode();
            } else {
                super.onItemLayoutSwipeUp(item_layout);
            }
        }

        @Override
        public void onItemLayoutSwipe2Up(ItemLayout item_layout) {
            final PageConfig config = mDrawerPage.config;
            if(config.swipe2Up.action == GlobalConfig.UNSET && config.scrollingDirection==PageConfig.ScrollingDirection.X && !mSearchMode) {
                nextLayoutMode();
            } else {
                super.onItemLayoutSwipe2Up(item_layout);
            }
        }

        @Override
        public void onItemLayoutSwipeDown(ItemLayout item_layout) {
            final PageConfig config = mDrawerPage.config;
            if(config.swipeDown.action == GlobalConfig.UNSET && config.scrollingDirection==PageConfig.ScrollingDirection.X && !mSearchMode) {
                previousLayoutMode();
            } else {
                super.onItemLayoutSwipeDown(item_layout);
            }
        }

        @Override
        public void onItemLayoutSwipe2Down(ItemLayout item_layout) {
            final PageConfig config = mDrawerPage.config;
            if(config.swipe2Down.action == GlobalConfig.UNSET && config.scrollingDirection==PageConfig.ScrollingDirection.X && !mSearchMode) {
                previousLayoutMode();
            } else {
                super.onItemLayoutSwipe2Down(item_layout);
            }
        }

        @Override
        public void onItemViewClicked(ItemView itemView) {
            Item item = itemView.getItem();
            Class<? extends Item> itemClass = item.getClass();
            boolean is_folder = itemClass ==Folder.class;
            if(isSelectForAdd() && !is_folder) {
                if(mActionBar.getDisplayedChild() == ACTION_BAR_CHILD_BATCH) {
                    checkUncheckItemView(itemView);
                } else {
                    Intent i = new Intent();
                    i.putExtra(Utils.INTENT_EXTRA_ITEM_ID, itemView.getItem().getId());
                    setResult(RESULT_OK, i);
                    finish();
                }
            } else if(isSelectForPick() && !is_folder) {
                Shortcut s=(Shortcut) item;
                setResult(RESULT_OK, s.getIntent());
                finish();
            } else {
                if(itemClass == Shortcut.class && !mEditMode) {
                    Intent intent = ((Shortcut) item).getIntent();
                    Bundle extras = intent.getExtras();
                    if(extras == null || extras.size() == 0) {
                        ComponentName cn = intent.getComponent();
                        if (mThisCn.compareTo(cn) == 0) {
                            PhoneUtils.startSettings(mContext, new ContainerPath(Page.APP_DRAWER_PAGE), false);
                            return;
                        }
                        if (mDashboardCn.compareTo(cn) == 0) {
                            PhoneUtils.startSettings(mContext, new ContainerPath(LLApp.get().getActiveDashboardPage()), true);
                            return;
                        }
                    }
                }
                super.onItemViewClicked(itemView);
            }
        }

        @Override
        public void onItemViewLongClicked(ItemView itemView) {
            if(!mEditMode && isSelectForAdd()) {
                setBatchMode(true);
                checkUncheckItemView(itemView);
                return;
            }
            if(mLayoutMode == Utils.LAYOUT_MODE_CUSTOM) {
                super.onItemViewLongClicked(itemView);
            } else {
                openBubble(BUBBLE_MODE_ITEM_NO_EM, itemView);
            }
        }

        @Override
        protected FolderView openUserMenu(boolean prepareOnly) {
            Toast.makeText(AppDrawerX.this, R.string.no_um_ad, Toast.LENGTH_SHORT).show();
            return null;
        }

        @Override
        protected void zoomFullScale(ItemLayout il) {
            if(mLayoutMode == Utils.LAYOUT_MODE_CUSTOM) {
                il.animateZoomTo(ItemLayout.POSITION_FULL_SCALE, 0);
            } else {
                final float CW=mItemLayout.getCellWidth();
                final float CH=mItemLayout.getCellHeight();

                int N=(int)(mItemLayout.getWidth() / CW); //mActivePage.config.gridLayoutModeNumColumns;

                float scale=1;
                float r=mItemLayout.getWidth()/(float)mItemLayout.getHeight();
                for(int x=N+1; x<100; x++) {
                    int y=(mVisibleItemsCount+x-1)/x;
                    if((x*CW)/((float)y*CH) >= r) {
                        scale=N/(float)x;
                        break;
                    }
                }


                mItemLayout.animateZoomTo(ItemLayout.POSITION_FREE, scale);
            }
        }

        @Override
        public void launchShortcut(ShortcutView shortcutView) {
            super.launchShortcut(shortcutView);

            if(mDrawerPage.config.autoExit) {
                finish();
            }
        }

        @Override
        public void onPageItemAdded(Item item) {
            super.onPageItemAdded(item);
            if(item.getPage().id == mItemLayout.getPage().id) {
                setLayoutMode(mLayoutMode, true);
            }
        }

        @Override
        public void onPageItemRemoved(Page page, Item item) {
            super.onPageItemRemoved(page, item);
            if(page.id == mItemLayout.getPage().id) {
                setLayoutMode(mLayoutMode, true);
            }
        }

        @Override
        public void onPageModified(Page page) {
            super.onPageModified(page);
            if(page.id == mItemLayout.getPage().id) {
                setLayoutMode(mLayoutMode, true);
            }
        }
    }
}
