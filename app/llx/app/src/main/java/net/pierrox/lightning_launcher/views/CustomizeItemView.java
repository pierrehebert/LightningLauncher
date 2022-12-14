/*
MIT License

Copyright (c) 2022 Pierre Hébert

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

package net.pierrox.lightning_launcher.views;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import net.pierrox.lightning_launcher.API;
import net.pierrox.lightning_launcher.LLApp;
import net.pierrox.lightning_launcher.activities.AppDrawerX;
import net.pierrox.lightning_launcher.activities.Dashboard;
import net.pierrox.lightning_launcher.activities.EventActionSetup;
import net.pierrox.lightning_launcher.activities.ImagePicker;
import net.pierrox.lightning_launcher.activities.ResourceWrapperActivity;
import net.pierrox.lightning_launcher.activities.ScriptEditor;
import net.pierrox.lightning_launcher.configuration.DynamicTextConfig;
import net.pierrox.lightning_launcher.configuration.FolderConfig;
import net.pierrox.lightning_launcher.configuration.FolderConfigStylable;
import net.pierrox.lightning_launcher.configuration.ItemConfig;
import net.pierrox.lightning_launcher.configuration.ItemConfigStylable;
import net.pierrox.lightning_launcher.configuration.ShortcutConfig;
import net.pierrox.lightning_launcher.configuration.ShortcutConfigStylable;
import net.pierrox.lightning_launcher.configuration.SystemConfig;
import net.pierrox.lightning_launcher.data.Action;
import net.pierrox.lightning_launcher.data.ActionsDescription;
import net.pierrox.lightning_launcher.data.Box;
import net.pierrox.lightning_launcher.data.CustomView;
import net.pierrox.lightning_launcher.data.DynamicText;
import net.pierrox.lightning_launcher.data.EmbeddedFolder;
import net.pierrox.lightning_launcher.data.EventAction;
import net.pierrox.lightning_launcher.data.FileUtils;
import net.pierrox.lightning_launcher.data.Folder;
import net.pierrox.lightning_launcher.data.GmailContract;
import net.pierrox.lightning_launcher.data.Item;
import net.pierrox.lightning_launcher.data.Page;
import net.pierrox.lightning_launcher.data.PageIndicator;
import net.pierrox.lightning_launcher.data.Shortcut;
import net.pierrox.lightning_launcher.data.StopPoint;
import net.pierrox.lightning_launcher.data.UndoStack;
import net.pierrox.lightning_launcher.data.Utils;
import net.pierrox.lightning_launcher.prefs.LLPreference;
import net.pierrox.lightning_launcher.prefs.LLPreferenceBinding;
import net.pierrox.lightning_launcher.prefs.LLPreferenceBox;
import net.pierrox.lightning_launcher.prefs.LLPreferenceCategory;
import net.pierrox.lightning_launcher.prefs.LLPreferenceCheckBox;
import net.pierrox.lightning_launcher.prefs.LLPreferenceColor;
import net.pierrox.lightning_launcher.prefs.LLPreferenceEventAction;
import net.pierrox.lightning_launcher.prefs.LLPreferenceList;
import net.pierrox.lightning_launcher.prefs.LLPreferenceListView;
import net.pierrox.lightning_launcher.prefs.LLPreferenceSlider;
import net.pierrox.lightning_launcher.prefs.LLPreferenceText;
import net.pierrox.lightning_launcher.script.Script;
import net.pierrox.lightning_launcher.util.ScriptPickerDialog;
import net.pierrox.lightning_launcher.util.BindingEditDialog;
import net.pierrox.lightning_launcher.util.FileAndDirectoryPickerDialog;
import net.pierrox.lightning_launcher.util.PhoneUtils;
import net.pierrox.lightning_launcher.engine.variable.Binding;
import net.pierrox.lightning_launcher_extreme.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;


public class CustomizeItemView extends MyViewPager implements LLPreferenceListView.OnLLPreferenceListViewEventListener, LLApp.SystemConfigListener {

    private static final int REQUEST_EDIT_EVENT_ACTION = Dashboard.REQUEST_FROM_CUSTOMIZE_VIEW + 1;
    private static final int REQUEST_PICK_ICON_EFFECT_B = Dashboard.REQUEST_FROM_CUSTOMIZE_VIEW + 5;
    private static final int REQUEST_PICK_ICON_EFFECT_O = Dashboard.REQUEST_FROM_CUSTOMIZE_VIEW + 6;
    private static final int REQUEST_PICK_ICON_EFFECT_M = Dashboard.REQUEST_FROM_CUSTOMIZE_VIEW + 7;
    private static final int REQUEST_PICK_IMAGE_BOX_N = Dashboard.REQUEST_FROM_CUSTOMIZE_VIEW + 9;
    private static final int REQUEST_PICK_IMAGE_BOX_S = Dashboard.REQUEST_FROM_CUSTOMIZE_VIEW + 10;
    private static final int REQUEST_PICK_IMAGE_BOX_F = Dashboard.REQUEST_FROM_CUSTOMIZE_VIEW + 11;
    private static final int REQUEST_PICK_IMAGE_BOX_B = Dashboard.REQUEST_FROM_CUSTOMIZE_VIEW + 12;


    private static final String SIS_PICK_EVENT_ACTION_PREFERENCE_ID = "civ_pid";

    // list views
    private LLPreferenceListView[] mAllPreferenceListViews;
    private LLPreferenceListView mItemPrefsLabel;
    private LLPreferenceListView mItemPrefsIcon;
    private LLPreferenceListView mItemPrefsDtData;
    private LLPreferenceListView mItemPrefsLayout;
    private LLPreferenceListView mItemPrefsBox;
    private LLPreferenceListView mItemPrefsMisc;
    private LLPreferenceListView mItemPrefsEvent;
    private LLPreferenceListView mItemPrefsStopPoint;
    private LLPreferenceListView mItemPrefsPageIndicator;
    private LLPreferenceListView mItemPrefsBindings;

    // label
    private LLPreferenceCheckBox mItemLabelDisplay;
    private LLPreferenceSlider mItemLabelSize;
    private LLPreferenceList mItemLabelStyle;
    private LLPreference mItemLabelFont;
    private LLPreferenceSlider mItemLabelNumLines;
    private LLPreferenceColor mItemLabelColorNormal;
    private LLPreferenceColor mItemLabelColorSelected;
    private LLPreferenceColor mItemLabelColorFocused;
    private LLPreferenceColor mItemLabelColorBasic;
    private LLPreferenceCheckBox mItemLabelShadow;
    private LLPreferenceSlider mItemLabelShadowRadius;
    private LLPreferenceSlider mItemLabelShadowOffsetX;
    private LLPreferenceSlider mItemLabelShadowOffsetY;
    private LLPreferenceColor mItemLabelShadowColor;

    // icon
    private LLPreferenceCheckBox mItemIconDisplay;
    private LLPreferenceList mItemIconFolder;
    private LLPreferenceCheckBox mItemIconSmooth;
    private LLPreferenceSlider mItemIconScale;
    private LLPreferenceList mItemIconSizeMode;
    private LLPreferenceColor mItemIconColorFilter;
    private LLPreference mItemIconEffectBack;
    private LLPreference mItemIconEffectOver;
    private LLPreference mItemIconEffectMask;
    private LLPreferenceCategory mItemIconEffectCategory;
    private LLPreferenceSlider mItemIconEffectScale;
    private LLPreferenceCheckBox mItemIconReflection;
    private LLPreferenceSlider mItemIconReflectionOverlap;
    private LLPreferenceSlider mItemIconReflectionSize;
    private LLPreferenceSlider mItemIconReflectionScale;

    // dynamic text
    private LLPreferenceList mItemDtSource;
    private LLPreferenceCheckBox mItemDtDisplayEmpty;
    private LLPreferenceList mItemDtDateEasyFormat;
    private LLPreferenceText mItemDtDateExpertFormat;
    private LLPreferenceText mItemDtCountFormat;
    private LLPreferenceText mItemDtTextFormat;
    private LLPreferenceList mItemDtStorageSource;
    private LLPreferenceList mItemDtStorageFormat;
    private LLPreferenceList mItemDtStorageWhat;
    private LLPreferenceList mItemDtGmailLabel;

    // page indicator
    private LLPreferenceList mItemPIStyle;
    private LLPreferenceSlider mItemPIDotsMarginX;
    private LLPreferenceSlider mItemPIDotsMarginY;
    private LLPreferenceSlider mItemPIDotsOuterRadius;
    private LLPreferenceSlider mItemPIDotsInnerRadius;
    private LLPreferenceSlider mItemPIDotsOuterStrokeWidth;
    private LLPreferenceColor mItemPIDotsOuterColor;
    private LLPreferenceColor mItemPIDotsInnerColor;
    private LLPreferenceText mItemPIRawFormat;
    private LLPreferenceColor mItemPIMiniMapOutStrokeColor;
    private LLPreferenceColor mItemPIMiniMapOutFillColor;
    private LLPreferenceSlider mItemPIMiniMapOutStrokeWidth;
    private LLPreferenceColor mItemPIMiniMapInStrokeColor;
    private LLPreferenceColor mItemPIMiniMapInFillColor;
    private LLPreferenceSlider mItemPIMiniMapInStrokeWidth;
    private LLPreferenceSlider mItemPILineBgWidth;
    private LLPreferenceColor mItemPILineBgColor;
    private LLPreferenceSlider mItemPILineFgWidth;
    private LLPreferenceColor mItemPILineFgColor;
    private LLPreferenceList mItemPILineGravity;

    // position
    private LLPreferenceSlider mItemLayoutMargin;
    private LLPreferenceList mItemLayoutPosition;

    // box
    private LLPreferenceList mItemBoxAlignH;
    private LLPreferenceList mItemBoxAlignV;
    private LLPreferenceBox mItemBoxBox;
    private LLPreferenceSlider mItemBoxSize;
    private LLPreferenceColor mItemBoxColorNormal;
    private LLPreferenceColor mItemBoxColorSelected;
    private LLPreferenceColor mItemBoxColorFocused;
    private LLPreferenceColor mItemBoxColorBasic;
    private LLPreference mItemBoxNpNormal;
    private LLPreference mItemBoxNpSelected;
    private LLPreference mItemBoxNpFocused;
    private LLPreference mItemBoxNpBasic;

    // misc
    private LLPreference mItemMiscName;
    private LLPreferenceList mItemMiscLaunchAnimation;
    private LLPreferenceList mItemMiscSelectionEffect;
    private LLPreferenceCheckBox mItemMiscSelectionEffectMask;
    private LLPreferenceList mItemMiscPinMode;
    private LLPreferenceCheckBox mItemMiscRotate;
    private LLPreferenceCheckBox mItemMiscEnabled;
    private LLPreferenceSlider mItemMiscAlpha;
    private LLPreferenceCheckBox mItemMiscSmoothTransformed;
    private LLPreferenceCheckBox mItemMiscHardwareAccelerated;

    // event
    private LLPreferenceEventAction mItemMiscEventSwipeLeft;
    private LLPreferenceEventAction mItemMiscEventSwipeRight;
    private LLPreferenceEventAction mItemMiscEventSwipeUp;
    private LLPreferenceEventAction mItemMiscEventSwipeDown;
    private LLPreferenceEventAction mItemMiscEventTap;
    private LLPreferenceEventAction mItemMiscEventLongTap;
    private LLPreferenceEventAction mItemMiscEventTouch;
    private LLPreferenceEventAction mItemMiscEventPaused;
    private LLPreferenceEventAction mItemMiscEventResumed;
    private LLPreferenceEventAction mItemMiscEventMenu;
    private LLPreferenceEventAction mItemSpReachedEvent;
    private LLPreference mItemMiscEventCVCreate;
    private LLPreference mItemMiscEventCVDestroy;

    // stop point
    private LLPreferenceCheckBox mItemSpStopScroll;
    private LLPreferenceCheckBox mItemSpStopDrag;
    private LLPreferenceCheckBox mItemSpBarrier;
    private LLPreferenceCheckBox mItemSpDesktopWide;
    private LLPreferenceCheckBox mItemSpSnapping;
    private LLPreferenceCheckBox mItemSpDirLeftToRight;
    private LLPreferenceCheckBox mItemSpDirRightToLeft;
    private LLPreferenceCheckBox mItemSpDirTopToBottom;
    private LLPreferenceCheckBox mItemSpDirBottomToTop;
    private LLPreferenceCheckBox mItemSpMatchEdgeLeft;
    private LLPreferenceCheckBox mItemSpMatchEdgeRight;
    private LLPreferenceCheckBox mItemSpMatchEdgeTop;
    private LLPreferenceCheckBox mItemSpMatchEdgeBottom;

    // bindings
    private LLPreference mItemBindingsAdd;

    private Dashboard mDashboard;
    private UndoStack mUndoStack;

    private boolean mSetupDone;

    private boolean mExpertMode;
    private ItemLayout mItemLayout;
    private Page mPage;
    private Item mItem;
    private boolean mForPage;

    private int mPickEventActionPreferenceId;
    private File mPreviouslyUsedDir;
    private String[] mFonts;

    private ItemConfig ic_def;
    private ShortcutConfig sc_def;
    private FolderConfig fc_def;
    private DynamicTextConfig dtc;
    private ActionsDescription mActionsDescriptions;

    public CustomizeItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void init() {
        LLApp app = LLApp.get();
        mExpertMode = app.getSystemConfig().expertMode;
        app.registerSystemConfigListener(this);

        mDashboard = (Dashboard) getContext();
        mUndoStack = mDashboard.getUndoStack();
    }

    public void end() {
        LLApp.get().unregisterSystemConfigListener(this);
    }

    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(SIS_PICK_EVENT_ACTION_PREFERENCE_ID, mPickEventActionPreferenceId);
    }

    public void restoreInstanceState(Bundle savedInstanceState) {
        mPickEventActionPreferenceId = savedInstanceState.getInt(SIS_PICK_EVENT_ACTION_PREFERENCE_ID);
    }

    public void setTarget(ItemLayout il, Page page, Item item) {
        // as of today, don't filter: the target data may have changed through script
//        if(mPage != page || mItem != item) {
            mItemLayout = il;
            mPage = page;
            mItem = item;
            mForPage = mItem == null;
            updatePreferences();
//        }
    }

    public Page getPage() {
        return mPage;
    }

    public Item getItem() {
        return mItem;
    }

    public String getCurrentPageName() {
        return getPageName(getCurrentView());
    }

    private String getPageName(View v) {
        int res;
        if(v == mItemPrefsLabel) res = R.string.tab_label;
        else if(v == mItemPrefsIcon) res = R.string.tab_icon;
        else if(v == mItemPrefsDtData) res = R.string.tab_dt_data;
        else if(v == mItemPrefsLayout) res = R.string.tab_layout;
        else if(v == mItemPrefsBox) res = R.string.tab_box;
        else if(v == mItemPrefsMisc) res = R.string.tab_misc;
        else if(v == mItemPrefsEvent) res = R.string.m_events;
        else if(v == mItemPrefsStopPoint) res = R.string.tab_stop_point;
        else if(v == mItemPrefsPageIndicator) res = R.string.tab_page_indicator;
        else if(v == mItemPrefsBindings) res = R.string.tab_bind;
        else res = 0;

        return res==0 ? "" : getContext().getString(res);
    }

    public void showGotoPageDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        String[] items = new String[mItems.size()];
        for(ItemInfo ii : mItems) {
            items[ii.position] = getPageName(ii.v);
        }
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                setCurrentView(mItems.get(which).v);
            }
        });
        builder.create().show();
    }

    public void gotoPageDynamicText() {
        setCurrentView(mItemPrefsDtData);
    }

    public void gotoPageEvents() {
        setCurrentView(mItemPrefsEvent);
    }

    private boolean isPreferenceAvailableForItem(Item item, int preferenceId) {
        Class<? extends Item> itemClass = item.getClass();
        boolean isShortcutConfigStylable = item instanceof ShortcutConfigStylable;
        boolean isDynamicText = itemClass == DynamicText.class;
        boolean isPageIndicator = itemClass == PageIndicator.class;
        boolean isShortcut = itemClass == Shortcut.class;
        boolean isFolder = itemClass == Folder.class;
        boolean isEmbeddedFolder = itemClass == EmbeddedFolder.class;
        boolean isStopPoint = itemClass == StopPoint.class;

        switch(preferenceId) {
            case ID_mItemLabelDisplay:
                return isShortcutConfigStylable && !isDynamicText && !isPageIndicator;

            case ID_mItemMiscLaunchAnimation:
                return isShortcut;

            case ID_mItemLabelSize:
            case ID_mItemLabelStyle:
            case ID_mItemLabelNumLines:
            case ID_mItemLabelColorNormal:
            case ID_mItemLabelColorSelected:
            case ID_mItemLabelColorFocused:
            case ID_mItemTextColorBasic:
            case ID_mItemLabelShadow:
            case ID_mItemLabelShadowRadius:
            case ID_mItemLabelShadowOffsetX:
            case ID_mItemLabelShadowOffsetY:
            case ID_mItemLabelShadowColor:
            case ID_mItemIconSmooth:
            case ID_mItemIconSizeMode:
            case ID_mItemIconScale:
            case ID_mItemIconColorFilter:
            case ID_mItemIconEffectScale:
            case ID_mItemIconReflection:
            case ID_mItemIconReflectionOverlap:
            case ID_mItemIconReflectionSize:
            case ID_mItemIconReflectionScale:
                return isShortcutConfigStylable;

            case ID_mItemIconDisplay:
            case ID_mItemLayoutMargin:
            case ID_mItemLayoutPosition:
                return isShortcutConfigStylable && !isDynamicText && !isPageIndicator;

            case ID_mItemIconFolder:
                return isFolder;

            case ID_mItemDtSource:
            case ID_mItemDtDateEasyFormat:
            case ID_mItemDtDateExpertFormat:
            case ID_mItemDtStorageSource:
            case ID_mItemDtStorageWhat:
            case ID_mItemDtStorageFormat:
            case ID_mItemDtTextFormat:
            case ID_mItemDtCountFormat:
            case ID_mItemDtDisplayEmpty:
            case ID_mItemDtGmailLabel:
            return isDynamicText;

            case ID_mItemBoxAlignH:
            case ID_mItemBoxAlignV:
                return !isEmbeddedFolder && !isStopPoint;

            case ID_mItemBoxBox:
            case ID_mItemBoxSize:
            case ID_mItemBoxColorBasic:
            case ID_mItemBoxColorNormal:
            case ID_mItemBoxColorSelected:
            case ID_mItemBoxColorFocused:
            case ID_mItemMiscAlpha:
            case ID_mItemMiscSelectionEffect:
            case ID_mItemMiscSelectionEffectMask:
            case ID_mItemMiscPinMode:
            case ID_mItemMiscRotate:
            case ID_mItemMiscEnabled:
            case ID_mItemMiscSmoothTransformed:
            case ID_mItemMiscHardwareAccelerated:
                return !isStopPoint;

            case ID_mItemSpStopScroll:
            case ID_mItemSpStopDrag:
            case ID_mItemSpBarrier:
            case ID_mItemSpDesktopWide:
            case ID_mItemSpSnapping:
            case ID_mItemSpDirLeftToRight:
            case ID_mItemSpDirRightToLeft:
            case ID_mItemSpDirTopToBottom:
            case ID_mItemSpDirBottomToTop:
            case ID_mItemSpMatchEdgeLeft:
            case ID_mItemSpMatchEdgeRight:
            case ID_mItemSpMatchEdgeTop:
            case ID_mItemSpMatchEdgeBottom:
            case ID_mItemSpReachedEvent:
                return isStopPoint;

            case ID_mItemMiscEventSwipeLeft:
            case ID_mItemMiscEventSwipeRight:
            case ID_mItemMiscEventSwipeUp:
            case ID_mItemMiscEventSwipeDown:
            case ID_mItemMiscEventTap:
            case ID_mItemMiscEventLongTap:
            case ID_mItemMiscEventTouch:
            case ID_mPageEventPaused:
            case ID_mPageEventResumed:
                return !isEmbeddedFolder && !isStopPoint && getContext().getClass()!=AppDrawerX.class;

            case ID_mItemPIStyle:
            case ID_mItemPIDotsOuterColor:
            case ID_mItemPIDotsOuterRadius:
            case ID_mItemPIDotsOuterStrokeWidth:
            case ID_mItemPIDotsInnerColor:
            case ID_mItemPIDotsInnerRadius:
            case ID_mItemPIDotsMarginX:
            case ID_mItemPIDotsMarginY:
            case ID_mItemPIRawFormat:
            case ID_mItemPIMiniMapOutStrokeColor:
            case ID_mItemPIMiniMapOutStrokeWidth:
            case ID_mItemPIMiniMapOutFillColor:
            case ID_mItemPIMiniMapInStrokeColor:
            case ID_mItemPIMiniMapInStrokeWidth:
            case ID_mItemPIMiniMapInFillColor:
            case ID_mItemPILineBgWidth:
            case ID_mItemPILineBgColor:
            case ID_mItemPILineFgWidth:
            case ID_mItemPILineFgColor:
            case ID_mItemPILineGravity:
                return isPageIndicator;

            case ID_mItemMiscEventMenu:
                return true;

            default:
                throw new RuntimeException();
        }
    }

    private void setupPreferences() {
        final Context context = getContext();

        buildFontsList();

        mItemPrefsLabel = new LLPreferenceListView(context, null);
        mItemPrefsIcon = new LLPreferenceListView(context, null);
        mItemPrefsDtData = new LLPreferenceListView(context, null);
        mItemPrefsLayout = new LLPreferenceListView(context, null);
        mItemPrefsBox = new LLPreferenceListView(context, null);
        mItemPrefsMisc = new LLPreferenceListView(context, null);
        mItemPrefsEvent = new LLPreferenceListView(context, null);
        mItemPrefsStopPoint = new LLPreferenceListView(context, null);
        mItemPrefsPageIndicator = new LLPreferenceListView(context, null);
        mItemPrefsBindings = new LLPreferenceListView(context, null);

        mAllPreferenceListViews = new LLPreferenceListView[] {
            mItemPrefsLabel,
            mItemPrefsIcon,
            mItemPrefsDtData,
            mItemPrefsLayout,
            mItemPrefsBox,
            mItemPrefsMisc,
            mItemPrefsEvent,
            mItemPrefsStopPoint,
            mItemPrefsPageIndicator,
            mItemPrefsBindings,
        };

        for(LLPreferenceListView l : mAllPreferenceListViews) {
            l.setCompactMode(true);
            l.setListener(this);
            l.setDisplayOverride(true);
        }

        addView(mItemPrefsLabel);
        addView(mItemPrefsIcon);
        addView(mItemPrefsDtData);
        addView(mItemPrefsLayout);
        addView(mItemPrefsBox);
        addView(mItemPrefsEvent);
        addView(mItemPrefsStopPoint);
        addView(mItemPrefsPageIndicator);
        addView(mItemPrefsMisc);
        addView(mItemPrefsBindings);

        setupPreferencesLabel();
        setupPreferencesIcon();
        setupPreferencesLayout();
        setupPreferencesBox();
        setupPreferencesMisc();
        setupPreferencesEvent();
        setupPreferencesStopPoint();

        initPagerItems();

        mSetupDone = true;
    }

    private void setupPreferencesLabel() {
        final Context context = getContext();

        // label
        ArrayList<LLPreference> prefs_label = new ArrayList<>();
        prefs_label.add(new LLPreferenceCategory(context, R.string.l_main));
        prefs_label.add(mItemLabelDisplay = new LLPreferenceCheckBox(context, ID_mItemLabelDisplay, R.string.l_display, 0));
        prefs_label.add(mItemLabelSize = new LLPreferenceSlider(context, ID_mItemLabelSize, R.string.l_size, 0, LLPreferenceSlider.ValueType.FLOAT, 4, 100, 1, null));
        prefs_label.add(mItemLabelStyle = new LLPreferenceList(context, ID_mItemLabelStyle, R.string.l_style_t, R.array.l_style_e, 0));
        prefs_label.add(mItemLabelFont = new LLPreference(context, ID_mItemLabelFont, R.string.l_type_face, 0));
        prefs_label.add(mItemLabelNumLines = new LLPreferenceSlider(context, ID_mItemLabelNumLines, R.string.l_num_lines, 0, LLPreferenceSlider.ValueType.INT, 1, 5, 1, null));

        mItemLabelColorNormal = new LLPreferenceColor(context, ID_mItemLabelColorNormal, R.string.l_color_normal, 0, true);
        mItemLabelColorSelected = new LLPreferenceColor(context, ID_mItemLabelColorSelected, R.string.l_color_selected, 0, true);
        mItemLabelColorFocused = new LLPreferenceColor(context, ID_mItemLabelColorFocused, R.string.l_color_focused, 0, true);
        mItemLabelColorBasic = new LLPreferenceColor(context, ID_mItemTextColorBasic, R.string.bm_c, 0, true);
        prefs_label.add(new LLPreferenceCategory(context, R.string.l_colors));
        prefs_label.add(mItemLabelColorNormal);
        prefs_label.add(mItemLabelColorSelected);
        prefs_label.add(mItemLabelColorFocused);
        prefs_label.add(mItemLabelColorBasic);

        prefs_label.add(new LLPreferenceCategory(context, R.string.l_shadow));
        prefs_label.add(mItemLabelShadow = new LLPreferenceCheckBox(context, ID_mItemLabelShadow, R.string.l_shadow_enable, 0));
        prefs_label.add(mItemLabelShadowRadius = new LLPreferenceSlider(context, ID_mItemLabelShadowRadius, R.string.l_shadow_radius, 0, LLPreferenceSlider.ValueType.FLOAT, 0, 10, 0.2f, null));
        prefs_label.add(mItemLabelShadowOffsetX = new LLPreferenceSlider(context, ID_mItemLabelShadowOffsetX, R.string.l_shadow_offset_x, 0, LLPreferenceSlider.ValueType.FLOAT, -10, 10, 0.2f, null));
        prefs_label.add(mItemLabelShadowOffsetY = new LLPreferenceSlider(context, ID_mItemLabelShadowOffsetY, R.string.l_shadow_offset_y, 0, LLPreferenceSlider.ValueType.FLOAT, -10, 10, 0.2f, null));
        prefs_label.add(mItemLabelShadowColor = new LLPreferenceColor(context, ID_mItemLabelShadowColor, R.string.l_shadow_color, 0, true));
        mItemPrefsLabel.setPreferences(prefs_label);

        mItemLabelDisplay.setDependencies(new LLPreference[]{
                mItemLabelSize, mItemLabelStyle,
                mItemLabelFont, mItemLabelNumLines, mItemLabelColorNormal,
                mItemLabelColorSelected, mItemLabelColorFocused,
                mItemLabelShadow}, null);
        mItemLabelShadow.setDependencies(new LLPreference[]{
                mItemLabelShadowRadius, mItemLabelShadowOffsetX,
                mItemLabelShadowOffsetY, mItemLabelShadowColor}, null);
    }

    private void setupPreferencesIcon() {
        final Context context = getContext();

        ArrayList<LLPreference> prefs_icon = new ArrayList<>();
        prefs_icon.add(new LLPreferenceCategory(context, R.string.i_main));
        prefs_icon.add(mItemIconDisplay = new LLPreferenceCheckBox(context, ID_mItemIconDisplay, R.string.i_display, 0));
        prefs_icon.add(mItemIconFolder = new LLPreferenceList(context, ID_mItemIconFolder, R.string.i_folder_t, R.array.i_folder_e, 0));
        prefs_icon.add(mItemIconSmooth = new LLPreferenceCheckBox(context, ID_mItemIconSmooth, R.string.i_smooth, 0));
        prefs_icon.add(mItemIconSizeMode = new LLPreferenceList(context, ID_mItemIconSizeMode, R.string.ism_t, R.array.ism_e, 0));
        prefs_icon.add(mItemIconScale = new LLPreferenceSlider(context, ID_mItemIconScale, R.string.i_scale_t, R.string.i_scale_s, LLPreferenceSlider.ValueType.FLOAT, 0.1f, 4, 0.1f, "%"));
        prefs_icon.add(mItemIconColorFilter = new LLPreferenceColor(context, ID_mItemIconColorFilter, R.string.i_cf_t, R.string.i_cf_s, true));
        mItemIconEffectBack = new LLPreference(context, ID_mItemIconEffectBack, R.string.i_e_back, 0);
        mItemIconEffectOver = new LLPreference(context, ID_mItemIconEffectOver, R.string.i_e_over, 0);
        mItemIconEffectMask = new LLPreference(context, ID_mItemIconEffectMask, R.string.i_e_mask, 0);
        mItemIconEffectScale = new LLPreferenceSlider(context, ID_mItemIconEffectScale, R.string.i_e_scale, 0, LLPreferenceSlider.ValueType.FLOAT, 0.1f, 4, 0.1f, "%");
        prefs_icon.add(mItemIconEffectCategory = new LLPreferenceCategory(context, R.string.i_effect));
        prefs_icon.add(mItemIconEffectBack);
        prefs_icon.add(mItemIconEffectOver);
        prefs_icon.add(mItemIconEffectMask);
        prefs_icon.add(mItemIconEffectScale);

        prefs_icon.add(new LLPreferenceCategory(context, R.string.i_reflect));
        prefs_icon.add(mItemIconReflection = new LLPreferenceCheckBox(context, ID_mItemIconReflection, R.string.i_reflect_enable, 0));
        prefs_icon.add(mItemIconReflectionOverlap = new LLPreferenceSlider(context, ID_mItemIconReflectionOverlap, R.string.i_reflect_overlap, 0, LLPreferenceSlider.ValueType.FLOAT, 0, 1, 0.1f, "%"));
        mItemIconReflectionSize = new LLPreferenceSlider(context, ID_mItemIconReflectionSize, R.string.i_reflect_size, 0, LLPreferenceSlider.ValueType.FLOAT, 0, 1, 0.1f, "%");
        mItemIconReflectionScale = new LLPreferenceSlider(context, ID_mItemIconReflectionScale, R.string.i_reflect_scale, 0, LLPreferenceSlider.ValueType.FLOAT, 0, 5, 0.1f, "%");
        prefs_icon.add(mItemIconReflectionSize);
        prefs_icon.add(mItemIconReflectionScale);

        mItemPrefsIcon.setPreferences(prefs_icon);

        mItemIconDisplay.setDependencies(new LLPreference[]{mItemIconFolder, mItemIconSmooth, mItemIconReflection}, null);
        mItemIconReflection.setDependencies(new LLPreference[]{mItemIconReflectionOverlap, mItemIconReflectionSize, mItemIconReflectionScale}, null);
    }

    private void setupPreferencesLayout() {
        final Context context = getContext();

        ArrayList<LLPreference> prefs_layout = new ArrayList<>();
        prefs_layout.add(mItemLayoutMargin = new LLPreferenceSlider(context, ID_mItemLayoutMargin, R.string.la_margin, 0, LLPreferenceSlider.ValueType.INT, -100, 100, 1, null));
        prefs_layout.add(mItemLayoutPosition = new LLPreferenceList(context, ID_mItemLayoutPosition, R.string.la_position_t, R.array.la_position_e, 0));
        mItemPrefsLayout.setPreferences(prefs_layout);
    }

    private void setupPreferencesBox() {
        final Context context = getContext();

        ArrayList<LLPreference> prefs_box = new ArrayList<>();
        prefs_box.add(mItemBoxAlignH = new LLPreferenceList(context, ID_mItemBoxAlignH, R.string.b_alignh_t, R.array.b_alignh_e, 3));
        prefs_box.add(mItemBoxAlignV = new LLPreferenceList(context, ID_mItemBoxAlignV, R.string.b_alignv_t, R.array.b_alignv_e, 3));
        mItemBoxNpNormal = new LLPreference(context, ID_ninePatch, R.string.np_n, R.string.np_d);
        mItemBoxNpSelected = new LLPreference(context, ID_ninePatch, R.string.np_s, R.string.np_d);
        mItemBoxNpFocused = new LLPreference(context, ID_ninePatch, R.string.np_f, R.string.np_d);
        mItemBoxNpBasic = new LLPreference(context, ID_ninePatch, R.string.bm_bg, R.string.np_d);
        prefs_box.add(new LLPreferenceCategory(context, R.string.np));
        prefs_box.add(mItemBoxNpNormal);
        prefs_box.add(mItemBoxNpSelected);
        prefs_box.add(mItemBoxNpFocused);
        prefs_box.add(mItemBoxNpBasic);
        prefs_box.add(mItemBoxBox = new LLPreferenceBox(ID_mItemBoxBox));
        prefs_box.add(mItemBoxSize = new LLPreferenceSlider(context, ID_mItemBoxSize, R.string.b_size, 0, 0, null, LLPreferenceSlider.ValueType.INT, -100, 100, 1, null));
        mItemBoxColorNormal = new LLPreferenceColor(context, ID_mItemBoxColorNormal, R.string.b_color_normal, R.string.b_color_s, true);
        mItemBoxColorSelected = new LLPreferenceColor(context, ID_mItemBoxColorSelected, R.string.b_color_selected, R.string.b_color_s, true);
        mItemBoxColorFocused = new LLPreferenceColor(context, ID_mItemBoxColorFocused, R.string.b_color_focused, R.string.b_color_s, true);
        mItemBoxColorBasic = new LLPreferenceColor(context, ID_mItemBoxColorBasic, R.string.b_color_s, 0, true);
        prefs_box.add(mItemBoxColorNormal);
        prefs_box.add(mItemBoxColorSelected);
        prefs_box.add(mItemBoxColorFocused);
        prefs_box.add(mItemBoxColorBasic);

        mItemBoxSize.setDisabled(true);
        mItemBoxColorNormal.setDisabled(true);
        mItemBoxColorSelected.setDisabled(true);
        mItemBoxColorFocused.setDisabled(true);
        mItemBoxColorBasic.setDisabled(true);

        mItemPrefsBox.setPreferences(prefs_box);
    }

    private void setupPreferencesMisc() {
        final Context context = getContext();

        ArrayList<LLPreference> prefs_misc = new ArrayList<>();
        prefs_misc.add(mItemMiscName = new LLPreference(context, ID_mItemMiscName, R.string.in_t, R.string.in_s));
        prefs_misc.add(mItemMiscLaunchAnimation = new LLPreferenceList(context, ID_mItemMiscLaunchAnimation, R.string.m_la_t, R.array.m_la_e, 0));
        prefs_misc.add(mItemMiscSelectionEffect = new LLPreferenceList(context, ID_mItemMiscSelectionEffect, R.string.m_sel_e_t, R.array.m_sel_e_e, Build.VERSION.SDK_INT >= 21 ? 3 : 2));
        prefs_misc.add(mItemMiscSelectionEffectMask = new LLPreferenceCheckBox(context, ID_mItemMiscSelectionEffectMask, R.string.i_rm_t, R.string.i_rm_s));
        prefs_misc.add(mItemMiscPinMode = new LLPreferenceList(context, ID_mItemMiscPinMode, R.string.m_pin_mode, R.array.m_pin_mode_e, 0));

        prefs_misc.add(mItemMiscRotate = new LLPreferenceCheckBox(context, ID_mItemMiscRotate, R.string.i_rot_t, R.string.i_rot_s));

        prefs_misc.add(mItemMiscEnabled = new LLPreferenceCheckBox(context, ID_mItemMiscEnabled, R.string.m_enabled_t, R.string.m_enabled_s));
        prefs_misc.add(mItemMiscAlpha = new LLPreferenceSlider(context, ID_mItemMiscAlpha, R.string.m_alpha_t, R.string.m_alpha_s, LLPreferenceSlider.ValueType.INT, 0, 255, 1, null));
        prefs_misc.add(mItemMiscSmoothTransformed = new LLPreferenceCheckBox(context, ID_mItemMiscSmoothTransformed, R.string.m_smooth_transform_t, R.string.m_smooth_transform_s));
        prefs_misc.add(mItemMiscHardwareAccelerated = new LLPreferenceCheckBox(context, ID_mItemMiscHardwareAccelerated, R.string.ha_t, 0));

        mItemPrefsMisc.setPreferences(prefs_misc);
    }

    private void setupPreferencesEvent() {
        final Context context = getContext();
        ArrayList<LLPreference> prefs_event = new ArrayList<>();
        prefs_event.add(new LLPreferenceCategory(context, R.string.m_events));
        prefs_event.add(mItemMiscEventTap = new LLPreferenceEventAction(context, ID_mItemMiscEventTap, R.string.ev_tap, mActionsDescriptions));
        prefs_event.add(mItemMiscEventLongTap = new LLPreferenceEventAction(context, ID_mItemMiscEventLongTap, R.string.ev_long_tap, mActionsDescriptions));
        prefs_event.add(mItemMiscEventSwipeLeft = new LLPreferenceEventAction(context, ID_mItemMiscEventSwipeLeft, R.string.ev_swipe_r, mActionsDescriptions));
        prefs_event.add(mItemMiscEventSwipeRight = new LLPreferenceEventAction(context, ID_mItemMiscEventSwipeRight, R.string.ev_swipe_l, mActionsDescriptions));
        prefs_event.add(mItemMiscEventSwipeUp = new LLPreferenceEventAction(context, ID_mItemMiscEventSwipeUp, R.string.ev_swipe_u, mActionsDescriptions));
        prefs_event.add(mItemMiscEventSwipeDown = new LLPreferenceEventAction(context, ID_mItemMiscEventSwipeDown, R.string.ev_swipe_d, mActionsDescriptions));

        mItemMiscEventTouch = new LLPreferenceEventAction(context, ID_mItemMiscEventTouch, R.string.ev_touch, mActionsDescriptions);
        mItemMiscEventPaused = new LLPreferenceEventAction(context, ID_mPageEventPaused, R.string.ev_paused, mActionsDescriptions);
        mItemMiscEventResumed = new LLPreferenceEventAction(context, ID_mPageEventResumed, R.string.ev_resumed, mActionsDescriptions);
        mItemMiscEventMenu = new LLPreferenceEventAction(context, ID_mItemMiscEventMenu, R.string.ev_m, new ActionsDescription(getContext(), Action.FLAG_TYPE_SCRIPT, false));
        prefs_event.add(mItemMiscEventTouch);
        prefs_event.add(mItemMiscEventPaused);
        prefs_event.add(mItemMiscEventResumed);
        prefs_event.add(mItemMiscEventMenu);

        mItemMiscEventCVCreate = new LLPreference(context, ID_mItemMiscEventCVCreate, R.string.cv_c, 0);
        mItemMiscEventCVDestroy = new LLPreference(context, ID_mItemMiscEventCVDestroy, R.string.cv_d, 0);
        prefs_event.add(mItemMiscEventCVCreate);
        prefs_event.add(mItemMiscEventCVDestroy);

        mItemPrefsEvent.setPreferences(prefs_event);
    }

    private void setupPreferencesStopPoint() {
        final Context context = getContext();

        ArrayList<LLPreference> prefs_sp = new ArrayList<>();
        prefs_sp.add(new LLPreferenceCategory(context, R.string.sp_b));
        prefs_sp.add(mItemSpStopScroll = new LLPreferenceCheckBox(context, ID_mItemSpStopScroll, R.string.sp_ss, 0));
        prefs_sp.add(mItemSpStopDrag = new LLPreferenceCheckBox(context, ID_mItemSpStopDrag, R.string.sp_sd, 0));
        prefs_sp.add(mItemSpBarrier = new LLPreferenceCheckBox(context, ID_mItemSpBarrier, R.string.sp_ba, 0));
        prefs_sp.add(mItemSpDesktopWide = new LLPreferenceCheckBox(context, ID_mItemSpDesktopWide, R.string.sp_dw, 0));
        prefs_sp.add(mItemSpSnapping = new LLPreferenceCheckBox(context, ID_mItemSpSnapping, R.string.sp_n, 0));

        prefs_sp.add(mItemSpReachedEvent = new LLPreferenceEventAction(context, ID_mItemSpReachedEvent, R.string.sp_re, mActionsDescriptions));

        prefs_sp.add(new LLPreferenceCategory(context, R.string.sp_d));
        prefs_sp.add(mItemSpDirLeftToRight = new LLPreferenceCheckBox(context, ID_mItemSpDirLeftToRight, R.string.sp_dlr, 0));
        prefs_sp.add(mItemSpDirRightToLeft = new LLPreferenceCheckBox(context, ID_mItemSpDirRightToLeft, R.string.sp_drl, 0));
        prefs_sp.add(mItemSpDirTopToBottom = new LLPreferenceCheckBox(context, ID_mItemSpDirTopToBottom, R.string.sp_dtb, 0));
        prefs_sp.add(mItemSpDirBottomToTop = new LLPreferenceCheckBox(context, ID_mItemSpDirBottomToTop, R.string.sp_dbt, 0));

        prefs_sp.add(new LLPreferenceCategory(context, R.string.sp_e));
        prefs_sp.add(mItemSpMatchEdgeLeft = new LLPreferenceCheckBox(context, ID_mItemSpMatchEdgeLeft, R.string.sp_el, 0));
        prefs_sp.add(mItemSpMatchEdgeRight = new LLPreferenceCheckBox(context, ID_mItemSpMatchEdgeRight, R.string.sp_er, 0));
        prefs_sp.add(mItemSpMatchEdgeTop = new LLPreferenceCheckBox(context, ID_mItemSpMatchEdgeTop, R.string.sp_et, 0));
        prefs_sp.add(mItemSpMatchEdgeBottom = new LLPreferenceCheckBox(context, ID_mItemSpMatchEdgeBottom, R.string.sp_eb, 0));

        mItemPrefsStopPoint.setPreferences(prefs_sp);
    }

    public void updatePreferences() {
        if (mItem == null && mPage == null) return;
        mActionsDescriptions = new ActionsDescription(getContext(), mPage.id==Page.APP_DRAWER_PAGE ? Action.FLAG_TYPE_APP_DRAWER : Action.FLAG_TYPE_DESKTOP, true);
        if (!mSetupDone) {
            setupPreferences();
        }

        boolean is_shortcut = false, is_folder = false, is_embedded_folder = false, is_dynamic_text = false, is_stop_point = false, is_page_indicator = false;
        if (mItem == null) {
            ic_def = null;
            sc_def = null;
            fc_def = null;
            is_shortcut = true;
            is_folder = true;
        } else {
            dtc = null;
            if (mItem.getClass() == EmbeddedFolder.class) {
                is_embedded_folder = true;
            } else if (mItem.getClass() == Folder.class) {
                is_folder = true;
                Folder f = (Folder) mItem;
            } else if (mItem instanceof DynamicText) {
                is_dynamic_text = true;
                DynamicText dt = (DynamicText) mItem;
                dtc = dt.modifyDynamicTextConfig();
            } else if (mItem instanceof Shortcut) {
                is_shortcut = true;
            } else if (mItem instanceof StopPoint) {
                is_stop_point = true;
            } else if (mItem instanceof PageIndicator) {
                is_page_indicator = true;
            }
            ic_def = mPage.config.defaultItemConfig;
            sc_def = mPage.config.defaultShortcutConfig;
            fc_def = mPage.config.defaultFolderConfig;
        }

        View previously_selected_view = getCurrentView();

        mItemPrefsLabel.setVisibility(View.GONE);
        mItemPrefsIcon.setVisibility(View.GONE);
        mItemPrefsDtData.setVisibility(View.GONE);
        mItemPrefsLayout.setVisibility(View.GONE);
        mItemPrefsBox.setVisibility(View.GONE);
        mItemPrefsMisc.setVisibility(View.GONE);
        mItemPrefsEvent.setVisibility(View.GONE);
        mItemPrefsStopPoint.setVisibility(View.GONE);
        mItemPrefsPageIndicator.setVisibility(View.GONE);
        mItemPrefsBindings.setVisibility(View.GONE);

        if (is_shortcut || is_folder || is_dynamic_text || is_page_indicator) {
            updatePreferencesLabel();
            mItemPrefsLabel.setVisibility(View.VISIBLE);
        }

        if (is_shortcut || is_folder) {
            updatePreferencesIcon();
            mItemPrefsIcon.setVisibility(View.VISIBLE);
        }

        if (is_dynamic_text) {
            updatePreferencesDynamicText(dtc.source);
            mItemPrefsDtData.setVisibility(View.VISIBLE);
        }

        if (is_page_indicator) {
            updatePreferencesPageIndicator();
            mItemPrefsPageIndicator.setVisibility(View.VISIBLE);
        }

        if (is_shortcut || is_folder) {
            updatePreferencesLayout();
            mItemPrefsLayout.setVisibility(View.VISIBLE);
        }

        if (is_stop_point) {
            updatePreferencesStopPoint();
            mItemPrefsStopPoint.setVisibility(View.VISIBLE);
        }

        if(!is_stop_point) {
            updatePreferencesBox();
            mItemPrefsBox.setVisibility(View.VISIBLE);
        }

        if(mExpertMode && mItem != null && getContext().getClass()!= AppDrawerX.class) {
            mItemPrefsBindings.setVisibility(View.VISIBLE);
        }

        if(!mForPage) {
            updatePreferencesBindings();
        }

        if(!is_stop_point && (mPage==null || mPage.id != Page.APP_DRAWER_PAGE) && !is_embedded_folder && getContext().getClass()!= AppDrawerX.class) {
            updatePreferencesEvent();
            mItemPrefsEvent.setVisibility(View.VISIBLE);
        }

        updatePreferencesMisc();
        mItemPrefsMisc.setVisibility(VISIBLE);

        initPagerItems();

        if(previously_selected_view.getVisibility() == VISIBLE) {
            setCurrentView(previously_selected_view);
        }
    }

    private void updatePreferencesLabel() {
        ShortcutConfig sc = getShortcutConfig();

        boolean is_dynamic_text = mItem != null && mItem.getClass() == DynamicText.class;
        boolean is_page_indicator = mItem != null && mItem.getClass() == PageIndicator.class;
        if(is_dynamic_text || is_page_indicator) {
            mItemLabelDisplay.setVisible(false);
        } else {
            mItemLabelDisplay.setVisible(true);
        }
        mItemLabelDisplay.setValue(sc.labelVisibility || is_dynamic_text, sc_def == null ? null : sc_def.labelVisibility);
        mItemLabelSize.setValue(sc.labelFontSize, sc_def == null ? null : sc_def.labelFontSize);
        mItemLabelStyle.setValue(sc.labelFontStyle, sc_def == null ? null : sc_def.labelFontStyle);
        mItemLabelNumLines.setValue(sc.labelMaxLines, sc_def == null ? null : (float) sc_def.labelMaxLines);

        mItemLabelColorNormal.setValue(sc.labelFontColor, sc_def == null ? null : sc_def.labelFontColor);
        mItemLabelColorSelected.setValue(sc.selectionColorLabel, sc_def == null ? null : sc_def.selectionColorLabel);
        mItemLabelColorFocused.setValue(sc.focusColorLabel, sc_def == null ? null : sc_def.focusColorLabel);
        mItemLabelColorBasic.setValue(sc.labelFontColor, sc_def == null ? null : sc_def.labelFontColor);
        mItemLabelColorNormal.setVisible(mExpertMode);
        mItemLabelColorSelected.setVisible(mExpertMode);
        mItemLabelColorFocused.setVisible(mExpertMode);
        mItemLabelColorBasic.setVisible(!mExpertMode);

        mItemLabelShadow.setValue(sc.labelShadow, sc_def == null ? null : sc.labelShadow);
        mItemLabelShadowRadius.setValue(sc.labelShadowRadius, sc_def == null ? null : sc_def.labelShadowRadius);
        mItemLabelShadowOffsetX.setValue(sc.labelShadowOffsetX, sc_def == null ? null : sc_def.labelShadowOffsetX);
        mItemLabelShadowOffsetY.setValue(sc.labelShadowOffsetY, sc_def == null ? null : sc_def.labelShadowOffsetY);
        mItemLabelShadowColor.setValue(sc.labelShadowColor, sc_def == null ? null : sc_def.labelShadowColor);

        mItemPrefsLabel.refresh();
    }

    private void updatePreferencesIcon() {
        ShortcutConfig sc = getShortcutConfig();

        mItemIconDisplay.setValue(sc.iconVisibility, sc_def == null ? null : sc_def.iconVisibility);
        if((mItem != null && mItem instanceof Folder) || (mItem == null && !mPage.isFolder())) {
            FolderConfig fc = mForPage ? mPage.getFolderConfig() : ((FolderConfigStylable)mItem).getFolderConfig();
            mItemIconFolder.setVisible(true);
            mItemIconFolder.setValue(fc.iconStyle, fc_def == null ? null : fc_def.iconStyle);
        } else {
            mItemIconFolder.setVisible(false);
        }
        mItemIconSmooth.setValue(sc.iconFilter, sc_def == null ? null : sc_def.iconFilter);
        mItemIconSmooth.setVisible(mExpertMode);
        mItemIconScale.setValue(sc.iconScale, sc_def == null ? null : sc_def.iconScale);
        mItemIconSizeMode.setValue(sc.iconSizeMode, sc_def == null ? null : sc_def.iconSizeMode);
        mItemIconColorFilter.setValue(sc.iconColorFilter, sc_def == null ? null : sc_def.iconColorFilter);
        mItemIconEffectScale.setValue(sc.iconEffectScale, sc_def == null ? null : sc_def.iconEffectScale);
        mItemIconEffectCategory.setVisible(mExpertMode);
        mItemIconEffectBack.setVisible(mExpertMode);
        mItemIconEffectOver.setVisible(mExpertMode);
        mItemIconEffectMask.setVisible(mExpertMode);
        mItemIconEffectScale.setVisible(mExpertMode);
        mItemIconReflection.setValue(sc.iconReflection, sc_def == null ? null : sc_def.iconReflection);
        mItemIconReflectionOverlap.setValue(sc.iconReflectionOverlap, sc_def == null ? null : sc_def.iconReflectionOverlap);
        mItemIconReflectionSize.setValue(sc.iconReflectionSize, sc_def == null ? null : sc_def.iconReflectionSize);
        mItemIconReflectionScale.setValue(sc.iconReflectionScale, sc_def == null ? null : sc_def.iconReflectionScale);
        mItemIconReflectionSize.setVisible(mExpertMode);
        mItemIconReflectionScale.setVisible(mExpertMode);

        mItemPrefsIcon.refresh();
    }

    private void updatePreferencesDynamicText(DynamicTextConfig.Source source) {
        final Context context = getContext();

        ArrayList<LLPreference> prefs_dt_data = new ArrayList<LLPreference>();
        prefs_dt_data.add(mItemDtSource = new LLPreferenceList(context, ID_mItemDtSource, R.string.dt_source_t, R.array.dt_source_e, source, null));
        switch(source) {
            case DATE:
                int easy_date_format_index = getEasyDateFormatIndex(context, dtc.dateFormat);
                prefs_dt_data.add(mItemDtDateEasyFormat = new LLPreferenceList(context, ID_mItemDtDateEasyFormat, R.string.dt_date_ef_t, getResources().getStringArray(R.array.dt_date_ef_e), easy_date_format_index, null));
                prefs_dt_data.add(mItemDtDateExpertFormat = new LLPreferenceText(context, ID_mItemDtDateExpertFormat, R.string.dt_date_xf_t, R.string.dt_date_xf_s, dtc.dateFormat, null));
                break;
            case STORAGE:
                prefs_dt_data.add(mItemDtStorageSource = new LLPreferenceList(context, ID_mItemDtStorageSource, R.string.dt_storage_s_t, R.array.dt_storage_s_e, dtc.storageSource, null));
                prefs_dt_data.add(mItemDtStorageWhat = new LLPreferenceList(context, ID_mItemDtStorageWhat, R.string.dt_storage_w_t, R.array.dt_storage_w_e, dtc.storageWhat, null));
                prefs_dt_data.add(mItemDtStorageFormat = new LLPreferenceList(context, ID_mItemDtStorageFormat, R.string.dt_storage_f_t, R.array.dt_storage_f_e, dtc.storageFormat, null));
                prefs_dt_data.add(mItemDtTextFormat = new LLPreferenceText(context, ID_mItemDtTextFormat, R.string.dt_date_xf_t, R.string.dt_date_xf_s, dtc.textFormat, null));
                break;
            case BATTERY_LEVEL:
                prefs_dt_data.add(mItemDtCountFormat = new LLPreferenceText(context, ID_mItemDtCountFormat, R.string.dt_date_xf_t, R.string.dt_date_xf_s, dtc.countFormat, null));
                break;
            case HEAP_FREE:
            case HEAP_MAX:
                prefs_dt_data.add(mItemDtTextFormat = new LLPreferenceText(context, ID_mItemDtTextFormat, R.string.dt_date_xf_t, R.string.dt_date_xf_s, dtc.textFormat, null));
                break;
            default:
                if(source == DynamicTextConfig.Source.UNREAD_GMAIL && GmailContract.canReadLabels(context)) {
                    prefs_dt_data.add(mItemDtGmailLabel = new LLPreferenceList(context, ID_mItemDtGmailLabel, R.string.dt_gml_t, new String[0], 0, null));
                    AccountManager.get(context).getAccountsByTypeAndFeatures(GmailContract.ACCOUNT_TYPE_GOOGLE, GmailContract.FEATURES_MAIL,
                            new AccountManagerCallback<Account[]>() {
                                @Override
                                public void run(AccountManagerFuture<Account[]> future) {
                                    try {
                                        ArrayList<String> labels = new ArrayList<>();
                                        Account[] accounts = future.getResult();
                                        if (accounts != null && accounts.length > 0) {
                                            int selected_index = -1;
                                            final String current_label = dtc.gmailLabel;
                                            for (Account account : accounts) {
                                                Cursor c = context.getContentResolver().query(GmailContract.Labels.getLabelsUri(account.name), null, null, null, null);
                                                if (c != null) {
                                                    final int name_index = c.getColumnIndexOrThrow(GmailContract.Labels.NAME);
                                                    while (c.moveToNext()) {
                                                        String row_label = c.getString(name_index);
                                                        String full_label = account.name+" / "+row_label;
                                                        String label = accounts.length > 1 ? full_label : row_label;
                                                        labels.add(label);

                                                        if(current_label != null && (current_label.equals(full_label) || (account == accounts[0] && current_label.equals(row_label)))) {
                                                            selected_index = labels.size() - 1;
                                                        }
                                                    }
                                                }
                                            }

                                            String[] labels_array = new String[labels.size()];
                                            labels.toArray(labels_array);
                                            mItemDtGmailLabel.setLabels(labels_array);
                                            mItemDtGmailLabel.setValueIndex(selected_index);
                                            mItemPrefsDtData.refresh();
                                        }
                                    } catch (Exception e) {
                                        // pass
                                    }
                                }
                            }, null /* handler */);
                }
                prefs_dt_data.add(mItemDtDisplayEmpty = new LLPreferenceCheckBox(context, ID_mItemDtDisplayEmpty, R.string.dt_display_empty_t, 0, dtc.displayEmpty, null));
                prefs_dt_data.add(mItemDtCountFormat = new LLPreferenceText(context, ID_mItemDtCountFormat, R.string.dt_date_xf_t, R.string.dt_date_xf_s, dtc.countFormat, null));
                break;
        }
        mItemPrefsDtData.setPreferences(prefs_dt_data);
    }

    private void updatePreferencesPageIndicator() {
        final Context context = getContext();

        PageIndicator pi = (PageIndicator) mItem;
        ArrayList<LLPreference> prefs_pi = new ArrayList<>();
        final PageIndicator.Style style = pi.style;
        prefs_pi.add(mItemPIStyle = new LLPreferenceList(context, ID_mItemPIStyle, R.string.pi_style, R.array.pi_style_e, style, null));
        switch(style) {
            case DOTS:
                prefs_pi.add(mItemPIDotsOuterColor=new LLPreferenceColor(context, ID_mItemPIDotsOuterColor, R.string.pi_dmoc, 0, pi.dotsOuterColor, null, true));
                prefs_pi.add(mItemPIDotsOuterRadius =new LLPreferenceSlider(context, ID_mItemPIDotsOuterRadius, R.string.pi_dmor, 0, pi.dotsOuterRadius, null, LLPreferenceSlider.ValueType.INT, 0, 50, 1, null));
                prefs_pi.add(mItemPIDotsOuterStrokeWidth =new LLPreferenceSlider(context, ID_mItemPIDotsOuterStrokeWidth, R.string.pi_dmow, 0, pi.dotsOuterStrokeWidth, null, LLPreferenceSlider.ValueType.INT, 0, 50, 1, null));
                prefs_pi.add(mItemPIDotsInnerColor=new LLPreferenceColor(context, ID_mItemPIDotsInnerColor, R.string.pi_dmic, 0, pi.dotsInnerColor, null, true));
                prefs_pi.add(mItemPIDotsInnerRadius =new LLPreferenceSlider(context, ID_mItemPIDotsInnerRadius, R.string.pi_dmir, 0, pi.dotsInnerRadius, null, LLPreferenceSlider.ValueType.INT, 0, 50, 1, null));
                prefs_pi.add(mItemPIDotsMarginX=new LLPreferenceSlider(context, ID_mItemPIDotsMarginX, R.string.pi_dmx_t, 0, pi.dotsMarginX, null, LLPreferenceSlider.ValueType.INT, 0, 100, 1, null));
                prefs_pi.add(mItemPIDotsMarginY=new LLPreferenceSlider(context, ID_mItemPIDotsMarginY, R.string.pi_dmy_t, 0, pi.dotsMarginY, null, LLPreferenceSlider.ValueType.INT, 0, 100, 1, null));
                break;

            case RAW:
                prefs_pi.add(mItemPIRawFormat = new LLPreferenceText(context, ID_mItemPIRawFormat, R.string.pi_rf_t, R.string.np_d, pi.rawFormat, null));
                break;

            case MINIMAP:
                prefs_pi.add(mItemPIMiniMapOutStrokeColor=new LLPreferenceColor(context, ID_mItemPIMiniMapOutStrokeColor, R.string.pi_mosc, 0, pi.miniMapOutStrokeColor, null, true));
                prefs_pi.add(mItemPIMiniMapOutStrokeWidth=new LLPreferenceSlider(context, ID_mItemPIMiniMapOutStrokeWidth, R.string.pi_mosw, 0, pi.miniMapOutStrokeWidth, null, LLPreferenceSlider.ValueType.INT, 0, 50, 1, null));
                prefs_pi.add(mItemPIMiniMapOutFillColor=new LLPreferenceColor(context, ID_mItemPIMiniMapOutFillColor, R.string.pi_mofc, 0, pi.miniMapOutFillColor, null, true));
                prefs_pi.add(mItemPIMiniMapInStrokeColor=new LLPreferenceColor(context, ID_mItemPIMiniMapInStrokeColor, R.string.pi_misc, 0, pi.miniMapInStrokeColor, null, true));
                prefs_pi.add(mItemPIMiniMapInStrokeWidth=new LLPreferenceSlider(context, ID_mItemPIMiniMapInStrokeWidth, R.string.pi_misw, 0, pi.miniMapInStrokeWidth, null, LLPreferenceSlider.ValueType.INT, 0, 50, 1, null));
                prefs_pi.add(mItemPIMiniMapInFillColor=new LLPreferenceColor(context, ID_mItemPIMiniMapInFillColor, R.string.pi_mifc, 0, pi.miniMapInFillColor, null, true));
                break;

            case LINE_X:
            case LINE_Y:
                prefs_pi.add(mItemPILineBgWidth=new LLPreferenceSlider(context, ID_mItemPILineBgWidth, R.string.pi_lbw, 0, pi.lineBgWidth, null, LLPreferenceSlider.ValueType.INT, 0, 50, 1, null));
                prefs_pi.add(mItemPILineBgColor=new LLPreferenceColor(context, ID_mItemPILineBgColor, R.string.pi_lbc, 0, pi.lineBgColor, null, true));
                prefs_pi.add(mItemPILineFgWidth=new LLPreferenceSlider(context, ID_mItemPILineFgWidth, R.string.pi_lfw, 0, pi.lineFgWidth, null, LLPreferenceSlider.ValueType.INT, 0, 50, 1, null));
                prefs_pi.add(mItemPILineFgColor=new LLPreferenceColor(context, ID_mItemPILineFgColor, R.string.pi_lfc, 0, pi.lineFgColor, null, true));
                prefs_pi.add(mItemPILineGravity=new LLPreferenceList(context, ID_mItemPILineGravity, R.string.pi_lg, R.array.pi_lg_e, pi.lineGravity, null));
                break;
        }

        mItemPrefsPageIndicator.setPreferences(prefs_pi);
    }

    private void updatePreferencesBindings() {
        final Context context = getContext();

        Binding[] bindings = getItemConfig().bindings;
        if(bindings == null) {
            bindings = new Binding[0];
        }

        ArrayList<LLPreference> prefs_bindings = mItemPrefsBindings.getPreferences();
        if(prefs_bindings == null) {
            prefs_bindings = new ArrayList<>();
            prefs_bindings.add(mItemBindingsAdd = new LLPreference(context, ID_mItemBindingsAdd, R.string.bd_a, R.string.np_d));

            for (Binding binding : bindings) {
                LLPreferenceBinding pref = new LLPreferenceBinding(context, binding);
                prefs_bindings.add(pref);
            }

            mItemPrefsBindings.setPreferences(prefs_bindings);
        } else {
            int list_length = prefs_bindings.size();
            int bindings_length = bindings.length;
            if(list_length-1 < bindings_length) {
                int i = 1;
                for(; i<=list_length-1; i++) {
                    prefs_bindings.get(i).setValue(bindings[i-1], null);
                }
                for(; i<=bindings_length; i++) {
                    LLPreferenceBinding pref = new LLPreferenceBinding(context, bindings[i-1]);
                    prefs_bindings.add(pref);
                }
            } else {
                for(int i=0; i<bindings_length; i++) {
                    prefs_bindings.get(i+1).setValue(bindings[i], null);
                }
                for(int i=list_length-1; i>bindings_length; i--) {
                    prefs_bindings.remove(i);
                }
            }

            mItemPrefsBindings.refresh();
        }
    }

    private void updatePreferencesLayout() {
        ShortcutConfig sc = getShortcutConfig();

        mItemLayoutMargin.setValue(sc.labelVsIconMargin, sc_def == null ? null : (float) sc_def.labelVsIconMargin);
        mItemLayoutPosition.setValue(sc.labelVsIconPosition, sc_def == null ? null : sc_def.labelVsIconPosition);

        mItemPrefsLayout.refresh();
    }

    private void updatePreferencesBox() {
        ItemConfig ic = getItemConfig();
        boolean is_embedded_folder = mItem != null && mItem.getClass() == EmbeddedFolder.class;
        mItemBoxAlignH.setDisabled(is_embedded_folder);
        mItemBoxAlignV.setDisabled(is_embedded_folder);
        mItemBoxAlignH.setValue(ic.box.ah, ic_def == null ? null : ic_def.box.ah);
        mItemBoxAlignV.setValue(ic.box.av, ic_def == null ? null : ic_def.box.av);
        mItemBoxBox.setValue(ic.box, ic_def == null ? null : ic_def.box);

        mItemBoxSize.setValue(0, null);
        mItemBoxColorNormal.setValue(Color.BLACK, ic_def == null ? null : Color.BLACK);
        mItemBoxColorSelected.setValue(Color.BLACK, ic_def == null ? null : Color.BLACK);
        mItemBoxColorFocused.setValue(Color.BLACK, ic_def == null ? null : Color.BLACK);
        mItemBoxColorBasic.setValue(Color.BLACK, ic_def == null ? null : Color.BLACK);
        mItemBoxNpNormal.setVisible(mExpertMode);
        mItemBoxNpSelected.setVisible(mExpertMode);
        mItemBoxNpFocused.setVisible(mExpertMode);
        mItemBoxNpBasic.setVisible(!mExpertMode);
        mItemBoxColorNormal.setVisible(mExpertMode);
        mItemBoxColorSelected.setVisible(mExpertMode);
        mItemBoxColorFocused.setVisible(mExpertMode);
        mItemBoxColorBasic.setVisible(!mExpertMode);

        mItemBoxSize.setDisabled(true);
        mItemBoxColorNormal.setDisabled(true);
        mItemBoxColorSelected.setDisabled(true);
        mItemBoxColorFocused.setDisabled(true);
        mItemBoxColorBasic.setDisabled(true);

        updatePreferencesBoxValues();
    }

    private void updatePreferencesBoxValues() {
        int selection = mItemBoxBox.getSelection();
        boolean size_disabled = selection == 0 || selection == 1 << BoxEditorView.SEG_CONTENT;
        mItemBoxSize.setDisabled(size_disabled);
        boolean color_disabled = (selection & BoxEditorView.SEG_SELECTION_COLOR_MASK) == 0;
        mItemBoxColorNormal.setDisabled(color_disabled);
        mItemBoxColorSelected.setDisabled(color_disabled);
        mItemBoxColorFocused.setDisabled(color_disabled);
        mItemBoxColorBasic.setDisabled(color_disabled);

        ItemConfig ic = getItemConfig();

        int size = 0;
        int default_size = 0;
        for (int i = BoxEditorView.SEG_ML; i <= BoxEditorView.SEG_PB; i++) {
            if ((selection & 1 << i) != 0) {
                size = ic.box.size[i];
                if (ic_def != null) {
                    default_size = ic_def.box.size[i];
                }
                break;
            }
        }
        mItemBoxSize.setValue((float)size, ic_def==null ? null : (float)default_size);

        final int[] border_color = ic.box.border_color;
        final int[] default_border_color = ic_def == null ? null : ic_def.box.border_color;
        int[] colors = new int[6];
        if ((selection & 1 << (BoxEditorView.SEG_CONTENT)) != 0) {
            colors[0] = ic.box.ccn;
            colors[1] = ic.box.ccs;
            colors[2] = ic.box.ccf;
            if (default_border_color != null) {
                colors[3] = ic_def.box.ccn;
                colors[4] = ic_def.box.ccs;
                colors[5] = ic_def.box.ccf;
            }
        } else {
            for (int i = Box.BCL; i <= Box.BCB; i++) {
                if ((selection & 1 << (BoxEditorView.SEG_BL + i)) != 0) {
                    colors[0] = border_color[Box.COLOR_SHIFT_N + i];
                    colors[1] = border_color[Box.COLOR_SHIFT_S + i];
                    colors[2] = border_color[Box.COLOR_SHIFT_F + i];
                    if (default_border_color != null) {
                        colors[3] = default_border_color[Box.COLOR_SHIFT_N + i];
                        colors[4] = default_border_color[Box.COLOR_SHIFT_S + i];
                        colors[5] = default_border_color[Box.COLOR_SHIFT_F + i];
                    }
                    break;
                }
            }
        }
        mItemBoxColorNormal.setColor(colors[0]);
        mItemBoxColorSelected.setColor(colors[1]);
        mItemBoxColorFocused.setColor(colors[2]);
        mItemBoxColorBasic.setColor(colors[0]);
        if (default_border_color != null) {
            mItemBoxColorNormal.setDefaultColor(colors[3]);
            mItemBoxColorSelected.setDefaultColor(colors[4]);
            mItemBoxColorFocused.setDefaultColor(colors[5]);
            mItemBoxColorBasic.setDefaultColor(colors[3]);
        }

        mItemPrefsBox.refresh();
    }

    private void updatePreferencesMisc() {
        ItemConfig ic = getItemConfig();


        mItemMiscName.setVisible(mItem != null);
        mItemMiscSelectionEffect.setVisible(false);
        mItemMiscSelectionEffectMask.setVisible(false);
        mItemMiscLaunchAnimation.setVisible(false);
        mItemMiscPinMode.setVisible(false);
        mItemMiscRotate.setVisible(false);
        mItemMiscEnabled.setVisible(false);
        mItemMiscAlpha.setVisible(false);
        mItemMiscSmoothTransformed.setVisible(false);
        mItemMiscHardwareAccelerated.setVisible(false);
        if(mItem == null || (mItem.getClass() == Shortcut.class)) {
            mItemMiscLaunchAnimation.setVisible(true);
            mItemMiscLaunchAnimation.setValue(ic.launchAnimation, ic_def == null ? null : ic_def.launchAnimation);
        }
        if(mItem == null || mItem.getClass() != StopPoint.class) {
            if (mExpertMode) {
                mItemMiscSelectionEffect.setValue(ic.selectionEffect, ic_def == null ? null : ic_def.selectionEffect);
                mItemMiscSelectionEffect.setVisible(true);
                if (Build.VERSION.SDK_INT >= 21) {
                    mItemMiscSelectionEffectMask.setValue(ic.selectionEffectMask, ic_def == null ? null : ic_def.selectionEffectMask);
                    mItemMiscSelectionEffectMask.setDisabled(ic.selectionEffect != ItemConfig.SelectionEffect.MATERIAL);
                    mItemMiscSelectionEffectMask.setVisible(true);
                }
            }
            if (mItem != null) {
                mItemMiscPinMode.setValue(ic.pinMode, ic_def == null ? null : ic_def.pinMode);
                mItemMiscPinMode.setVisible(true);
            }

            mItemMiscRotate.setValue(ic.rotate, ic_def == null ? null : ic_def.rotate);
            mItemMiscRotate.setVisible(true);

            if (mExpertMode) {
                mItemMiscEnabled.setValue(ic.enabled, ic_def == null ? null : ic_def.enabled);
                mItemMiscEnabled.setVisible(true);
            }
            mItemMiscAlpha.setValue(ic.alpha, ic_def == null ? null : (float) ic_def.alpha);
            mItemMiscAlpha.setVisible(true);
            if (mExpertMode) {
                mItemMiscSmoothTransformed.setValue(ic.filterTransformed, ic_def == null ? null : ic_def.filterTransformed);
                mItemMiscSmoothTransformed.setVisible(true);
                mItemMiscHardwareAccelerated.setValue(ic.hardwareAccelerated, ic_def == null ? null : ic_def.hardwareAccelerated);
                mItemMiscHardwareAccelerated.setVisible(true);
            }
        }

        mItemPrefsMisc.refresh();
    }

    private void updatePreferencesEvent() {
        ItemConfig ic = getItemConfig();

        mItemMiscEventTap.setValue(ic.tap.clone(), ic_def==null ? null : ic_def.tap);
        mItemMiscEventLongTap.setValue(ic.longTap.clone(), ic_def==null ? null : ic_def.longTap);
        mItemMiscEventSwipeLeft.setValue(ic.swipeLeft.clone(), ic_def==null ? null : ic_def.swipeLeft);
        mItemMiscEventSwipeRight.setValue(ic.swipeRight.clone(), ic_def==null ? null : ic_def.swipeRight);
        mItemMiscEventSwipeUp.setValue(ic.swipeUp.clone(), ic_def==null ? null : ic_def.swipeUp );
        mItemMiscEventSwipeDown.setValue(ic.swipeDown.clone(), ic_def==null ? null : ic_def.swipeDown);
        mItemMiscEventTouch.setValue(ic.touch.clone(), ic_def == null ? null : ic_def.touch);
        mItemMiscEventTouch.setVisible(mExpertMode);
        mItemMiscEventPaused.setValue(ic.paused.clone(), ic_def == null ? null : ic_def.paused);
        mItemMiscEventPaused.setVisible(mExpertMode);
        mItemMiscEventResumed.setValue(ic.resumed.clone(), ic_def == null ? null : ic_def.resumed);
        mItemMiscEventResumed.setVisible(mExpertMode);
        mItemMiscEventMenu.setValue(ic.menu.clone(), ic_def == null ? null : ic_def.menu);
        mItemMiscEventMenu.setVisible(mExpertMode);

        if(mItem != null && mItem instanceof CustomView) {
            CustomView cv = (CustomView) mItem;
            setPreferenceSummaryForScript(mItemMiscEventCVCreate, cv.onCreate);
            setPreferenceSummaryForScript(mItemMiscEventCVDestroy, cv.onDestroy);
            mItemMiscEventCVCreate.setVisible(true);
            mItemMiscEventCVDestroy.setVisible(true);
        } else {
            mItemMiscEventCVCreate.setVisible(false);
            mItemMiscEventCVDestroy.setVisible(false);
        }

        mItemPrefsEvent.refresh();
    }

    private void updatePreferencesStopPoint() {
        StopPoint sp = (StopPoint) mItem;
        int direction = sp.getDirection();
        int what = sp.getWhat();
        boolean stop_scroll = (what&StopPoint.STOP_SCROLL)!=0;
        boolean stop_drag = (what&StopPoint.STOP_DRAG)!=0;
        final boolean left_to_right = (direction&StopPoint.DIRECTION_LEFT_TO_RIGHT)!=0;
        final boolean right_to_left = (direction&StopPoint.DIRECTION_RIGHT_TO_LEFT)!=0;
        final boolean top_to_bottom = (direction&StopPoint.DIRECTION_TOP_TO_BOTTOM)!=0;
        final boolean bottom_to_top = (direction&StopPoint.DIRECTION_BOTTOM_TO_TOP)!=0;
        final boolean barrier = sp.isBarrier();
        final boolean desktop_wide = sp.isDesktopWide();
        final boolean snapping = sp.isSnapping();
        final int match_edge = sp.getMatchEdge();
        final boolean match_edge_left = (match_edge&StopPoint.MATCH_EDGE_LEFT)!=0;
        final boolean match_edge_right = (match_edge&StopPoint.MATCH_EDGE_RIGHT)!=0;
        final boolean match_edge_top = (match_edge&StopPoint.MATCH_EDGE_TOP)!=0;
        final boolean match_edge_bottom = (match_edge&StopPoint.MATCH_EDGE_BOTTOM)!=0;

        mItemSpStopScroll.setValue(stop_scroll, null);
        mItemSpStopDrag.setValue(stop_drag, null);
        mItemSpBarrier.setValue(barrier, null);
        mItemSpDesktopWide.setValue(desktop_wide, null);
        mItemSpSnapping.setValue(snapping, null);
        mItemSpReachedEvent.setValue(sp.getReachedAction(), null);
        mItemSpDirLeftToRight.setValue(left_to_right, null);
        mItemSpDirRightToLeft.setValue(right_to_left, null);
        mItemSpDirTopToBottom.setValue(top_to_bottom, null);
        mItemSpDirBottomToTop.setValue(bottom_to_top, null);
        mItemSpMatchEdgeLeft.setValue(match_edge_left, null);
        mItemSpMatchEdgeRight.setValue(match_edge_right, null);
        mItemSpMatchEdgeTop.setValue(match_edge_top, null);
        mItemSpMatchEdgeBottom.setValue(match_edge_bottom, null);

        mItemPrefsStopPoint.refresh();
    }

    @Override
    public void onLLPreferenceClicked(final LLPreference preference) {
        if (preference == mItemIconEffectBack) {
            ImagePicker.startActivity(mDashboard, REQUEST_PICK_ICON_EFFECT_B);
        } else if (preference == mItemIconEffectOver) {
            ImagePicker.startActivity(mDashboard, REQUEST_PICK_ICON_EFFECT_O);
        } else if (preference == mItemIconEffectMask) {
            ImagePicker.startActivity(mDashboard, REQUEST_PICK_ICON_EFFECT_M);
        } else if (preference == mItemBoxNpNormal) {
            ImagePicker.startActivity(mDashboard, REQUEST_PICK_IMAGE_BOX_N);
        } else if (preference == mItemBoxNpSelected) {
            ImagePicker.startActivity(mDashboard, REQUEST_PICK_IMAGE_BOX_S);
        } else if (preference == mItemBoxNpFocused) {
            ImagePicker.startActivity(mDashboard, REQUEST_PICK_IMAGE_BOX_F);
        } else if (preference == mItemBoxNpBasic) {
            ImagePicker.startActivity(mDashboard, REQUEST_PICK_IMAGE_BOX_B);
        } else if (preference == mItemMiscName) {
            Utils.createTextInputDialog(getContext(), R.string.in_t,  mItem.getName(), new Utils.OnTextInputDialogDone() {
                @Override
                public void onTextInputDone(String value) {
                    mUndoStack.storeItemState(mItem);
                    mItem.setName(value);
                    mItem.notifyChanged();
                }
            }).show();
        } else if (preference == mItemLabelFont) {
            showFontSelectionDialog();
        } else if (preference == mItemMiscEventCVCreate || preference == mItemMiscEventCVDestroy) {
            CustomView masterCustomView = (CustomView) mItem;
            final boolean is_create = preference == mItemMiscEventCVCreate;
            String data = is_create ? masterCustomView.onCreate : masterCustomView.onDestroy;
            ScriptPickerDialog dialog = new ScriptPickerDialog(getContext(), mPage.getEngine(), data, Script.TARGET_NONE, new ScriptPickerDialog.OnScriptPickerEvent() {
                @Override
                public void onScriptPicked(String id_data, int target) {
                    mUndoStack.storeGroupStart();
                    for (Item item : mDashboard.getSelectedItems()) {
                        if(item instanceof CustomView) {
                            mUndoStack.storeItemState(item);
                            mPage.startItemChange(item);
                            CustomView cv = (CustomView) item;
                            if (is_create) {
                                cv.onCreate = id_data;
                            } else {
                                cv.onDestroy = id_data;
                            }
                            mPage.endItemChange(item);
                        }
                    }
                    mUndoStack.storeGroupStart();
                    setPreferenceSummaryForScript(preference, id_data);
                    mItemPrefsMisc.refresh();
                }

                @Override
                public void onScriptPickerCanceled() {
                    // pass
                }
            });
            dialog.show();
        } else if(preference == mItemBindingsAdd || preference instanceof LLPreferenceBinding) {
            final boolean for_add = preference == mItemBindingsAdd;
            Binding init_value = for_add ? null : ((LLPreferenceBinding)preference).getValue();

            new BindingEditDialog(getContext(), init_value, mItemLayout.getItemView(mItem), new BindingEditDialog.OnBindingEditDialogListener() {
                @Override
                public void onBindingEdited(Binding binding, boolean open_in_script_editor) {
                    ItemConfig ic = mItem.modifyItemConfig();
                    Binding[] bindings = ic.bindings;
                    if (bindings == null) {
                        bindings = new Binding[0];
                    }
                    int length = bindings.length;
                    Binding[] new_bindings;
                    if(for_add) {
                        new_bindings = new Binding[length + 1];
                        System.arraycopy(bindings, 0, new_bindings, 0, length);
                        new_bindings[length] = binding;
                    } else {
                        for(int i=0; i<length; i++) {
                            Binding b = bindings[i];
                            if(b.target.equals(binding.target)) {
                                bindings[i] = binding;
                            }
                        }
                        new_bindings = bindings;
                    }
                    ic.bindings = new_bindings;
                    updatePreferencesBindings();
                    updateBindings(!open_in_script_editor);
                    mItem.notifyChanged();

                    if (open_in_script_editor) {
                        Script script = mPage.getEngine().getScriptManager().createScriptForBinding(mItemLayout.getItemView(mItem), binding);
                        ScriptEditor.startActivity(getContext(), script.id, -1);
                    }
                }
            }).show();
        } else if (preference instanceof LLPreferenceEventAction) {
            editEventActionPreference((LLPreferenceEventAction)preference);
        }
    }

    @Override
    public void onLLPreferenceLongClicked(LLPreference preference) {
        PhoneUtils.showPreferenceHelp(getContext(), preference);
    }

    @Override
    public void onLLPreferenceChanged(LLPreference preference) {
        final int preferenceId = preference.getId();

        if(preferenceId == ID_mItemBoxBox) {
            // nothing to do except update enabled state of some other color preferences, this is only a selection box
            updatePreferencesBox();
            return;
        }

        if(preference instanceof LLPreferenceEventAction) {
            applyEventAction(preferenceId, ((LLPreferenceEventAction)preference).getValue());
            return;
        }

        if(preference instanceof LLPreferenceBinding) {
            // update bindings
            int l = mItemPrefsBindings.getCount();
            Binding[] bindings;
            if(l > 1) {
                bindings = new Binding[l-1];
                for (int i = 1; i < l; i++) {
                    LLPreferenceBinding pref = (LLPreferenceBinding) mItemPrefsBindings.getItemAtPosition(i);
                    bindings[i-1] = pref.getValue();
                }
            } else {
                bindings = null;
            }
            mItem.modifyItemConfig().bindings = bindings;

            updateBindings(true);
            return;
        }

        if(mForPage) {

            mUndoStack.storePageState(mPage);

            applyPreferenceOnStylableObject(mPage, preference);

            mPage.saveConfig();
            mPage.reload();
            updatePreferences();
        } else {
            boolean booleanValue = false;
            int intValue = 0;
            float floatValue = 0;
            Enum enumValue = null;
            EventAction eventActionValue = null;
            int colorValue = 0;
            int listIndex = 0;
            String listLabel = null;
            String textValue = null;


            ArrayList<Item> items = mDashboard.getSelectedItems();

            if(preference instanceof LLPreferenceCheckBox) {
                booleanValue = ((LLPreferenceCheckBox) preference).isChecked();
            } else if(preference instanceof LLPreferenceSlider) {
                intValue = (int)((LLPreferenceSlider) preference).getValue();
                floatValue = ((LLPreferenceSlider) preference).getValue();
            } else if(preference instanceof LLPreferenceColor) {
                colorValue = ((LLPreferenceColor)preference).getColor();
            } else if(preference instanceof LLPreferenceText) {
                textValue = ((LLPreferenceText)preference).getValue();
            } else if(preference instanceof LLPreferenceList) {
                final LLPreferenceList preferenceList = (LLPreferenceList) preference;
                final String[] labels = preferenceList.getLabels();
                listIndex = preferenceList.getValueIndex();
                if(labels != null && listIndex != -1) {
                    listLabel = labels[listIndex];
                }
                switch (preferenceList.getValueType()) {
                    case ENUM:
                        enumValue = preferenceList.getValueEnum();
                        break;

                    case EVENT_ACTION:
                        eventActionValue = preferenceList.getEventAction();
                        break;

                    case INDEX:
                        break;
                }
            }

            DynamicTextConfig.Source masterDynamicTextSource = mItem instanceof DynamicText ? masterDynamicTextSource = ((DynamicText)mItem).getDynamicTextConfig().source : null;
            String dynamicTextDateFormat = null;
            switch (preferenceId) {
                case ID_mItemDtSource:
                    DynamicTextConfig.Source source = (DynamicTextConfig.Source) enumValue;
                    boolean change_ok;
                    if (source == DynamicTextConfig.Source.UNREAD_GMAIL) {
                        change_ok = mDashboard.checkPermissions(
                                new String[]{Manifest.permission.GET_ACCOUNTS},
                                new int[]{R.string.pr_r10},
                                ResourceWrapperActivity.REQUEST_PERMISSION_BASE
                        );
                        mItemDtSource.setValueIndex(0);
                    } else if (source == DynamicTextConfig.Source.UNREAD_SMS) {
                        change_ok = mDashboard.checkPermissions(
                                new String[]{Manifest.permission.READ_SMS},
                                new int[]{R.string.pr_r12},
                                ResourceWrapperActivity.REQUEST_PERMISSION_BASE
                        );
                    } else if (source == DynamicTextConfig.Source.MISSED_CALLS) {
                        change_ok = mDashboard.checkPermissions(
                                new String[]{Manifest.permission.READ_CALL_LOG},
                                new int[]{R.string.pr_r11},
                                ResourceWrapperActivity.REQUEST_PERMISSION_BASE
                        );
                    } else {
                        change_ok = true;
                    }

                    if (change_ok) {
                        updatePreferencesDynamicText(source);
                    } else {
                        mItemDtSource.setValue(dtc.source, null);
                        return;
                    }
                    break;

                case ID_mItemDtDateEasyFormat:
                    String[] date_formats = getResources().getStringArray(R.array.dt_date_ef_v);
                    dynamicTextDateFormat = date_formats[mItemDtDateEasyFormat.getValueIndex()];
                    mItemDtDateExpertFormat.setValue(dynamicTextDateFormat);
                    break;

                case ID_mItemDtDateExpertFormat:
                    dynamicTextDateFormat = mItemDtDateExpertFormat.getValue();
                    mItemDtDateEasyFormat.setValueIndex(getEasyDateFormatIndex(getContext(), dynamicTextDateFormat));
                    try {
                        new SimpleDateFormat(dynamicTextDateFormat);
                    } catch (IllegalArgumentException e) {
                        Toast.makeText(getContext(), R.string.dt_format_error, Toast.LENGTH_SHORT).show();
                    }
                    break;

                case ID_mItemDtCountFormat:
                    try {
                        new DecimalFormat(mItemDtCountFormat.getValue());
                    } catch (IllegalArgumentException e) {
                        Toast.makeText(getContext(), R.string.dt_format_error, Toast.LENGTH_SHORT).show();
                    }
                    break;
            }

            mUndoStack.storeGroupStart();

            for (Item item : items) {
                Class<? extends Item> itemClass = item.getClass();
                boolean isShortcutConfigStylable = item instanceof ShortcutConfigStylable;
                boolean isFolder = itemClass == Folder.class;
                boolean isEmbeddedFolder = itemClass == EmbeddedFolder.class;
                boolean isDynamicText = itemClass == DynamicText.class;
                boolean isPageIndicator = itemClass == PageIndicator.class;
                boolean isStopPoint = itemClass == StopPoint.class;
                ShortcutConfigStylable scs = isShortcutConfigStylable ? (ShortcutConfigStylable) item : null;
                Folder folder = isFolder ? (Folder) item : null;
                DynamicText dynamicText = isDynamicText ? (DynamicText) item : null;
                PageIndicator pageIndicator = isPageIndicator ? (PageIndicator) item : null;
                StopPoint stopPoint = isStopPoint ? (StopPoint) item : null;

                boolean available = isPreferenceAvailableForItem(item, preferenceId);

                // dynamic texts have a custom management, apply the preference only if the dynamic text item has the same source as the master item
                available = available && (!isDynamicText || masterDynamicTextSource == null || dynamicText.getDynamicTextConfig().source == masterDynamicTextSource);

                if(available) {
                    mPage.startItemChange(item);

                    mUndoStack.storeItemState(item);

                    if(!applyPreferenceOnStylableObject(item, preference)) {
                        switch (preferenceId) {
                            case ID_mItemDtSource:
                                DynamicTextConfig dynamicTextConfig = dynamicText.modifyDynamicTextConfig();
                                DynamicTextConfig.Source source = (DynamicTextConfig.Source) enumValue;
                                DynamicTextConfig.Source prev_source = dynamicTextConfig.source;

                                dynamicTextConfig.source = source;

                                ComponentName icn = dynamicText.getIntent().getComponent();
                                if (DynamicText.getDefaultComponentNameForSource(prev_source).equals(icn)) {
                                    Intent intent = new Intent();
                                    intent.setComponent(DynamicText.getDefaultComponentNameForSource(source));
                                    dynamicText.setIntent(intent);
                                }
                                break;

                            case ID_mItemDtDateEasyFormat:
                            case ID_mItemDtDateExpertFormat:
                                dynamicText.modifyDynamicTextConfig().dateFormat = dynamicTextDateFormat;
                                break;

                            case ID_mItemDtStorageSource:
                                dynamicText.modifyDynamicTextConfig().storageSource = (DynamicTextConfig.StorageSource) enumValue;
                                break;

                            case ID_mItemDtStorageWhat:
                                dynamicText.modifyDynamicTextConfig().storageWhat = (DynamicTextConfig.StorageWhat) enumValue;
                                break;

                            case ID_mItemDtStorageFormat:
                                dynamicText.modifyDynamicTextConfig().storageFormat = (DynamicTextConfig.StorageFormat) enumValue;
                                break;

                            case ID_mItemDtTextFormat:
                                dynamicText.modifyDynamicTextConfig().textFormat = textValue;
                                break;

                            case ID_mItemDtCountFormat:
                                dynamicText.modifyDynamicTextConfig().countFormat = textValue;
                                break;

                            case ID_mItemDtDisplayEmpty:
                                dynamicText.modifyDynamicTextConfig().displayEmpty = booleanValue;
                                break;

                            case ID_mItemDtGmailLabel:
                                dynamicText.modifyDynamicTextConfig().gmailLabel = listLabel;
                                break;

                            case ID_mItemSpStopScroll:
                            case ID_mItemSpStopDrag:
                                stopPoint.setWhat((mItemSpStopScroll.isChecked() ? StopPoint.STOP_SCROLL : 0) | (mItemSpStopDrag.isChecked() ? StopPoint.STOP_DRAG : 0));
                                break;

                            case ID_mItemSpBarrier:
                                stopPoint.setBarrier(booleanValue);
                                break;

                            case ID_mItemSpDesktopWide:
                                stopPoint.setDesktopWide(booleanValue);
                                break;

                            case ID_mItemSpSnapping:
                                stopPoint.setSnapping(booleanValue);
                                break;

                            case ID_mItemSpDirLeftToRight:
                            case ID_mItemSpDirRightToLeft:
                            case ID_mItemSpDirTopToBottom:
                            case ID_mItemSpDirBottomToTop:
                                stopPoint.setDirection(
                                        (mItemSpDirLeftToRight.isChecked() ? StopPoint.DIRECTION_LEFT_TO_RIGHT : 0) |
                                                (mItemSpDirRightToLeft.isChecked() ? StopPoint.DIRECTION_RIGHT_TO_LEFT : 0) |
                                                (mItemSpDirTopToBottom.isChecked() ? StopPoint.DIRECTION_TOP_TO_BOTTOM : 0) |
                                                (mItemSpDirBottomToTop.isChecked() ? StopPoint.DIRECTION_BOTTOM_TO_TOP : 0)
                                );
                                break;

                            case ID_mItemSpMatchEdgeLeft:
                            case ID_mItemSpMatchEdgeRight:
                            case ID_mItemSpMatchEdgeTop:
                            case ID_mItemSpMatchEdgeBottom:
                                stopPoint.setMatchEdge(
                                        (mItemSpMatchEdgeLeft.isChecked() ? StopPoint.MATCH_EDGE_LEFT : 0) |
                                                (mItemSpMatchEdgeRight.isChecked() ? StopPoint.MATCH_EDGE_RIGHT : 0) |
                                                (mItemSpMatchEdgeTop.isChecked() ? StopPoint.MATCH_EDGE_TOP : 0) |
                                                (mItemSpMatchEdgeBottom.isChecked() ? StopPoint.MATCH_EDGE_BOTTOM : 0)
                                );
                                break;

                            case ID_mItemPIStyle:
                                pageIndicator.style = (PageIndicator.Style) mItemPIStyle.getValueEnum();
                                if(item == mItem) {
                                    updatePreferencesPageIndicator();
                                }
                                break;

                            case ID_mItemPIDotsOuterColor: pageIndicator.dotsOuterColor = colorValue; break;
                            case ID_mItemPIDotsOuterRadius: pageIndicator.dotsOuterRadius = intValue; break;
                            case ID_mItemPIDotsOuterStrokeWidth: pageIndicator.dotsOuterStrokeWidth = intValue; break;
                            case ID_mItemPIDotsInnerColor: pageIndicator.dotsInnerColor = colorValue; break;
                            case ID_mItemPIDotsInnerRadius: pageIndicator.dotsInnerRadius = intValue; break;
                            case ID_mItemPIDotsMarginX: pageIndicator.dotsMarginX = intValue; break;
                            case ID_mItemPIDotsMarginY: pageIndicator.dotsMarginY = intValue; break;
                            case ID_mItemPIRawFormat: pageIndicator.rawFormat = textValue; break;
                            case ID_mItemPIMiniMapOutStrokeColor: pageIndicator.miniMapOutStrokeColor = colorValue; break;
                            case ID_mItemPIMiniMapOutStrokeWidth: pageIndicator.miniMapOutStrokeWidth = intValue; break;
                            case ID_mItemPIMiniMapOutFillColor: pageIndicator.miniMapOutFillColor = colorValue; break;
                            case ID_mItemPIMiniMapInStrokeColor: pageIndicator.miniMapInStrokeColor = colorValue; break;
                            case ID_mItemPIMiniMapInStrokeWidth: pageIndicator.miniMapInStrokeWidth = intValue; break;
                            case ID_mItemPIMiniMapInFillColor: pageIndicator.miniMapInFillColor = colorValue; break;
                            case ID_mItemPILineBgWidth: pageIndicator.lineBgWidth = intValue; break;
                            case ID_mItemPILineBgColor: pageIndicator.lineBgColor = colorValue; break;
                            case ID_mItemPILineFgWidth: pageIndicator.lineFgWidth = intValue; break;
                            case ID_mItemPILineFgColor: pageIndicator.lineFgColor = colorValue; break;
                            case ID_mItemPILineGravity: pageIndicator.lineGravity = (PageIndicator.LineGravity) enumValue; break;
                        }
                    }
                    mPage.endItemChange(item);
                }
            }
            mUndoStack.storeGroupEnd();
        }
    }

    @Override
    public void onLLPreferenceBindingRemoved(LLPreferenceBinding preference) {
        ItemConfig ic = mItem.modifyItemConfig();
        Binding[] old_bindings = ic.bindings;
        int l = old_bindings.length;
        Binding[] new_bindings;
        if(l == 1) {
            new_bindings = null;
        } else {
            new_bindings = new Binding[l - 1];
            Binding removed = preference.getValue();
            for (int i = 0, j = 0; i < l; i++) {
                Binding b = old_bindings[i];
                if (!b.equals(removed)) {
                    new_bindings[j++] = b;
                }
            }
        }
        ic.bindings = new_bindings;

        updateBindings(true);

        updatePreferencesBindings();

        mItem.notifyChanged();
    }

    public void myOnActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_EDIT_EVENT_ACTION) {
            if(resultCode == Activity.RESULT_OK) {
                EventAction ea = EventActionSetup.getEventActionFromIntent(data);
                applyEventAction(mPickEventActionPreferenceId, ea);
            }
        } else if (requestCode == REQUEST_PICK_ICON_EFFECT_B || requestCode == REQUEST_PICK_ICON_EFFECT_O || requestCode == REQUEST_PICK_ICON_EFFECT_M) {
            if (resultCode == Activity.RESULT_OK) {
                File icon_dir = mPage.getAndCreateIconDir();

                ShortcutConfig pageShortcutConfig = mPage.getShortcutConfig();

                if(mForPage) {
                    mUndoStack.storePageState(mPage);

                    File file;
                    if (requestCode == REQUEST_PICK_ICON_EFFECT_B) {
                        file = ShortcutConfig.getIconBackFile(icon_dir, Item.NO_ID);
                    } else if (requestCode == REQUEST_PICK_ICON_EFFECT_O) {
                        file = ShortcutConfig.getIconOverFile(icon_dir, Item.NO_ID);
                    } else {//if (requestCode == REQUEST_PICK_ICON_EFFECT_M) {
                        file = ShortcutConfig.getIconMaskFile(icon_dir, Item.NO_ID);
                    }

                    Utils.copyOrDeleteFile(Utils.getTmpImageFile(), file);


                    pageShortcutConfig.iconBack = null;
                    pageShortcutConfig.iconOver = null;
                    pageShortcutConfig.iconMask = null;

                    mPage.saveConfig();
                    mPage.reload();
                } else {
                    ArrayList<Item> items = mDashboard.getSelectedItems();
                    mUndoStack.storeGroupStart();
                    for (Item item : items) {
                        if(item instanceof ShortcutConfigStylable) {
                            final int itemId = item.getId();

                            mUndoStack.storeItemState(item);

                            File file;
                            if (requestCode == REQUEST_PICK_ICON_EFFECT_B) {
                                file = ShortcutConfig.getIconBackFile(icon_dir, itemId);
                            } else if (requestCode == REQUEST_PICK_ICON_EFFECT_O) {
                                file = ShortcutConfig.getIconOverFile(icon_dir, itemId);
                            } else {//if (requestCode == REQUEST_PICK_ICON_EFFECT_M) {
                                file = ShortcutConfig.getIconMaskFile(icon_dir, itemId);
                            }

                            Utils.copyOrDeleteFile(Utils.getTmpImageFile(), file);

                            ShortcutConfig shortcutConfig = ((Shortcut) item).getShortcutConfig();
                            shortcutConfig.iconBack = pageShortcutConfig.iconBack;
                            shortcutConfig.iconOver = pageShortcutConfig.iconOver;
                            shortcutConfig.iconMask = pageShortcutConfig.iconMask;
                            shortcutConfig.loadAssociatedIcons(icon_dir, itemId);

                            item.notifyChanged();
                        }
                    }
                    mUndoStack.storeGroupEnd();
                }
            }
        } else if (requestCode == REQUEST_PICK_IMAGE_BOX_N || requestCode == REQUEST_PICK_IMAGE_BOX_S || requestCode == REQUEST_PICK_IMAGE_BOX_F || requestCode == REQUEST_PICK_IMAGE_BOX_B) {
            if(resultCode == Activity.RESULT_OK) {

                File icon_dir = mPage.getAndCreateIconDir();
                File tmp_image_file = Utils.getTmpImageFile();

                if(mForPage) {
                    mUndoStack.storePageState(mPage);

                    File drawable_file;
                    if (requestCode == REQUEST_PICK_IMAGE_BOX_N || requestCode == REQUEST_PICK_IMAGE_BOX_B) {
                        drawable_file = Box.getBoxBackgroundNormal(icon_dir, Item.NO_ID);
                    } else if (requestCode == REQUEST_PICK_IMAGE_BOX_S) {
                        drawable_file = Box.getBoxBackgroundSelected(icon_dir, Item.NO_ID);
                    } else { //if(requestCode == REQUEST_PICK_IMAGE_BOX_F) {
                        drawable_file = Box.getBoxBackgroundFocused(icon_dir, Item.NO_ID);
                    }

                    if (tmp_image_file.exists()) {
                        byte[] buffer = new byte[1024];
                        Utils.copyFileSafe(buffer, tmp_image_file, drawable_file);

                        if (requestCode == REQUEST_PICK_IMAGE_BOX_B) {
                            // basic mode: copy to selected and focused
                            File copy;
                            copy = Box.getBoxBackgroundSelected(icon_dir, Item.NO_ID);
                            Utils.copyFileSafe(buffer, drawable_file, copy);
                            copy = Box.getBoxBackgroundFocused(icon_dir, Item.NO_ID);
                            Utils.copyFileSafe(buffer, drawable_file, copy);
                        }
                    } else {
                        drawable_file.delete();
                        if (requestCode == REQUEST_PICK_IMAGE_BOX_B) {
                            Box.getBoxBackgroundSelected(icon_dir, Item.NO_ID).delete();
                            Box.getBoxBackgroundFocused(icon_dir, Item.NO_ID).delete();
                        }
                    }
                    ItemConfig pageItemConfig = mPage.getItemConfig();
                    pageItemConfig.box.bgNormal = null;
                    pageItemConfig.box.bgSelected = null;
                    pageItemConfig.box.bgFocused = null;
                    pageItemConfig.box.loadAssociatedDrawables(icon_dir, Item.NO_ID, true);

                    mPage.saveConfig();
                    mPage.reload();
                } else {
                    ArrayList<Item> items = mDashboard.getSelectedItems();
                    mUndoStack.storeGroupStart();
                    for (Item item : items) {
                        if(item instanceof ShortcutConfigStylable) {
                            final int itemId = item.getId();

                            mUndoStack.storeItemState(item);

                            File drawable_file;
                            if (requestCode == REQUEST_PICK_IMAGE_BOX_N || requestCode == REQUEST_PICK_IMAGE_BOX_B) {
                                drawable_file = Box.getBoxBackgroundNormal(icon_dir, itemId);
                            } else if (requestCode == REQUEST_PICK_IMAGE_BOX_S) {
                                drawable_file = Box.getBoxBackgroundSelected(icon_dir, itemId);
                            } else { //if(requestCode == REQUEST_PICK_IMAGE_BOX_F) {
                                drawable_file = Box.getBoxBackgroundFocused(icon_dir, itemId);
                            }

                            if (tmp_image_file.exists()) {
                                byte[] buffer = new byte[1024];
                                Utils.copyFileSafe(buffer, tmp_image_file, drawable_file);

                                if (requestCode == REQUEST_PICK_IMAGE_BOX_B) {
                                    // basic mode: copy to selected and focused
                                    File copy;
                                    copy = Box.getBoxBackgroundSelected(icon_dir, itemId);
                                    Utils.copyFileSafe(buffer, drawable_file, copy);
                                    copy = Box.getBoxBackgroundFocused(icon_dir, itemId);
                                    Utils.copyFileSafe(buffer, drawable_file, copy);
                                }
                            } else {
                                drawable_file.delete();
                                if (requestCode == REQUEST_PICK_IMAGE_BOX_B) {
                                    Box.getBoxBackgroundSelected(icon_dir, itemId).delete();
                                    Box.getBoxBackgroundFocused(icon_dir, itemId).delete();
                                }
                            }
                            ItemConfig itemConfig = item.modifyItemConfig();

                            itemConfig.box.bgNormal = null;
                            itemConfig.box.bgSelected = null;
                            itemConfig.box.bgFocused = null;
                            itemConfig.box.loadAssociatedDrawables(icon_dir, Item.NO_ID, true);
                            itemConfig.box.loadAssociatedDrawables(icon_dir, itemId, true);

                            item.notifyChanged();
                        }
                    }
                    mUndoStack.storeGroupEnd();
                }
            }
        }
    }

    private static int getEasyDateFormatIndex(Context context, String format) {
        String[] date_formats = context.getResources().getStringArray(R.array.dt_date_ef_v);
        for(int i=0; i<date_formats.length; i++) {
            if(date_formats[i].equals(format)) {
                return i;
            }
        }

        return -1;
    }

    private void setPreferenceSummaryForScript(LLPreference preference, String id_data) {
        preference.setSummary("");
        if(id_data != null) {
            int id = Script.decodeIdAndData(id_data).first;
            Script script = mPage.getEngine().getScriptManager().getOrLoadScript(id);
            if(script != null) {
                preference.setSummary(script.name);
            }
        }
    }

    private void applyBorderColors(int selection, LLPreference preference, int shift, Box box, Box box_def) {
        final LLPreferenceColor p = (LLPreferenceColor) preference;
        final int color = p.getColor();
        final boolean has_override = p.isShowingOverride();
        final boolean is_overriding = p.isOverriding();
        final int[] border_color = box.border_color;
        for (int i = Box.BCL; i <= Box.BCB; i++) {
            if ((selection & 1 << (BoxEditorView.SEG_BL + i)) != 0) {
                int n = shift + i;
                int c;
                if (has_override) {
                    c = is_overriding ? color : box_def.border_color[n];
                } else {
                    c = color;
                }
                border_color[n] = c;
            }
        }
    }

    private void editEventActionPreference(LLPreferenceEventAction preference) {
        mPickEventActionPreferenceId = preference.getId();

        ActionsDescription actions = preference.getActions();

        EventActionSetup.startActivityForResult(mDashboard, preference.getValue(), actions.isForItem(), actions.getType(), false, REQUEST_EDIT_EVENT_ACTION);
    }

    private void applyEventAction(int preferenceId, EventAction ea) {
        if(mForPage) {
            mUndoStack.storePageState(mPage);

            applyEventActionOnObject(mPage, preferenceId, ea);

            mPage.saveConfig();
            mPage.reload();
            updatePreferences();
        } else {
            mUndoStack.storeGroupStart();
            for (Item item : mDashboard.getSelectedItems()) {

                mUndoStack.storeItemState(item);

                if(isPreferenceAvailableForItem(item, preferenceId)) {
                    applyEventActionOnObject(item, preferenceId, ea);
                }

                item.notifyChanged();

            }
            mUndoStack.storeGroupEnd();
        }
    }

    private void applyEventActionOnObject(Object object, int preferenceId, EventAction eventAction) {
        EventAction ea = eventAction.clone();

        if(preferenceId == ID_mItemSpReachedEvent) {
            ((StopPoint) object).setReachedAction(ea);
        } else {
            final ItemConfig itemConfig;
            if (object instanceof Item) {
                itemConfig = ((Item) object).modifyItemConfig();
            } else {
                itemConfig = ((Page) object).config.defaultItemConfig;
            }
            switch (preferenceId) {
                case ID_mItemMiscEventSwipeLeft: itemConfig.swipeLeft = ea; break;
                case ID_mItemMiscEventSwipeRight: itemConfig.swipeRight = ea; break;
                case ID_mItemMiscEventSwipeUp: itemConfig.swipeUp = ea; break;
                case ID_mItemMiscEventSwipeDown: itemConfig.swipeDown = ea; break;
                case ID_mItemMiscEventTap: itemConfig.tap = ea; break;
                case ID_mItemMiscEventLongTap: itemConfig.longTap = ea; break;
                case ID_mItemMiscEventTouch: itemConfig.touch = ea; break;
                case ID_mPageEventPaused: itemConfig.paused = ea; break;
                case ID_mPageEventResumed: itemConfig.resumed = ea; break;
                case ID_mItemMiscEventMenu: itemConfig.menu = ea; break;
            }
        }
    }

    private void showFontSelectionDialog() {
        final Context context = getContext();

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        ListView listView = new ListView(context);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        listView.setAdapter(new FontAdapter(context, mFonts, getFontIndex(getShortcutConfig().labelFontTypeFace)));
        builder.setTitle(R.string.l_type_face);
        builder.setView(listView);
        builder.setNegativeButton(android.R.string.cancel, null);
        final Dialog dialog = builder.create();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    boolean ok = mDashboard.checkPermissions(
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            new int[] { R.string.pr_r3},
                            ResourceWrapperActivity.REQUEST_PERMISSION_FONT_PICKER);
                    if(ok) {
                        FileAndDirectoryPickerDialog.showForFont(mDashboard, mPreviouslyUsedDir, new FileAndDirectoryPickerDialog.FileAndDirectoryPickerDialogListener() {
                            @Override
                            public void onFileSelected(File file, File newDirectory) {
                                mPreviouslyUsedDir = newDirectory;
                                FileOutputStream fos = null;
                                FileInputStream fis = null;
                                try {
                                    File dst_dir = FileUtils.getFontsDir(getPageBaseDir());
                                    dst_dir.mkdirs();
                                    File dst = new File(dst_dir, file.getName());
                                    fos = new FileOutputStream(dst);
                                    fis = new FileInputStream(file);
                                    byte[] buffer = new byte[4096];
                                    int n;
                                    while ((n = fis.read(buffer)) > 0) {
                                        fos.write(buffer, 0, n);
                                    }
                                    file = dst;
                                    buildFontsList();
                                } catch (Exception e) {
                                    file= null;
                                } finally {
                                    if (fos != null) try { fos.close(); } catch (Exception e) { }
                                    if (fis != null) try { fis.close(); } catch (Exception e) { }
                                }
                                if (file != null) {
                                    if (mForPage) {
                                        mUndoStack.storePageState(mPage);

                                        mPage.modifyShortcutConfig().labelFontTypeFace = file.getAbsolutePath();

                                        mPage.saveConfig();
                                        mPage.reload();
                                    } else {
                                        ArrayList<Item> items = mDashboard.getSelectedItems();
                                        mUndoStack.storeGroupStart();
                                        for (Item item : items) {
                                            if(item instanceof ShortcutConfigStylable) {
                                                mUndoStack.storeItemState(item);
                                                ((ShortcutConfigStylable) item).modifyShortcutConfig().labelFontTypeFace = file.getAbsolutePath();
                                                item.notifyChanged();
                                            }
                                        }
                                        mUndoStack.storeGroupEnd();
                                    }
                                    mItemPrefsLabel.refresh();
                                }
                            }
                        });
                    }
                } else {
                    String labelFontTypeFace;
                    if (position == 1) {
                        labelFontTypeFace = API.SHORTCUT_SYSTEM_FONT;
                    } else if (position == 2) {
                        labelFontTypeFace = API.SHORTCUT_ICON_FONT;
                    } else {
                        labelFontTypeFace = FileUtils.getFontsDir(getPageBaseDir()) + "/" + mFonts[position];
                    }
                    if (mForPage) {
                        mUndoStack.storePageState(mPage);
                        mPage.modifyShortcutConfig().labelFontTypeFace = labelFontTypeFace;
                        mPage.saveConfig();
                        mPage.reload();
                    } else {
                        ArrayList<Item> items = mDashboard.getSelectedItems();
                        mUndoStack.storeGroupStart();
                        for (Item item : items) {
                            mUndoStack.storeItemState(item);
                            ((ShortcutConfigStylable)item).modifyShortcutConfig().labelFontTypeFace = labelFontTypeFace;
                        }
                        mUndoStack.storeGroupEnd();
                        for (Item item : items) {
                            item.notifyChanged();
                        }
                    }
                }
                dialog.dismiss();
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if(position > 2) {
                    showDeleteFontDialog(new File(FileUtils.getFontsDir(getPageBaseDir()), mFonts[position]));
                    dialog.dismiss();
                    return true;
                }

                return false;
            }
        });

        dialog.show();
    }

    private void showDeleteFontDialog(final File file) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.delete_file);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                file.delete();
                buildFontsList();
                showFontSelectionDialog();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.create().show();
    }

    private void buildFontsList() {
        String[] font_files = FileUtils.getFontsDir(getPageBaseDir()).list(
                new FilenameFilter() {
                    @SuppressLint("DefaultLocale")
                    @Override
                    public boolean accept(File dir, String filename) {
                        final String filename_l = filename.toLowerCase();
                        return filename_l.endsWith(".ttf") || filename_l.endsWith(".otf");
                    }
                });
        if (font_files == null) {
            font_files = new String[0];
        }
        int l = font_files.length;
        mFonts = new String[l + 3];
        Context context = getContext();
        mFonts[0] = context.getString(R.string.pick_a_font);
        mFonts[1] = context.getString(R.string.system_font);
        mFonts[2] = context.getString(R.string.icon_font);
        System.arraycopy(font_files, 0, mFonts, 3, l);
    }

    private File getPageBaseDir() {
        return mPage.getEngine().getBaseDir();
    }

    private int getFontIndex(String font) {
        if (API.SHORTCUT_SYSTEM_FONT.equals(font)) {
            return 1;
        } else if (API.SHORTCUT_ICON_FONT.equals(font)) {
            return 2;
        } else {
            String font_name = new File(font).getName();
            for (int i = 3; i < mFonts.length; i++) {
                if (mFonts[i].equals(font_name)) {
                    return i;
                }
            }
            return 1;
        }
    }

    private void updateBindings(boolean apply) {
        mItem.notifyBindingsChanged(apply);
    }

    private static final String FONT_ITEM_PREVIEW = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    @Override
    public void onSystemConfigChanged(SystemConfig newSystemConfig) {
        boolean expert_mode = newSystemConfig.expertMode;
        if(mExpertMode != expert_mode) {
            mExpertMode = expert_mode;
            updatePreferences();
        }
    }

    private class FontAdapter extends ArrayAdapter<String> {
        private HashMap<String,Typeface> mTypefaces = new HashMap<>();
        private File mFontsDir;
        private int mSelectedIndex;
        private int mCheckMarkDrawable;
        private LayoutInflater inflater;

        public FontAdapter(Context context, String[] fonts, int selected_index) {
            super(context, 0, fonts);
            mFontsDir = FileUtils.getFontsDir(getPageBaseDir());
            mSelectedIndex = selected_index;

            TypedValue typedValue = new TypedValue();
            context.getTheme().resolveAttribute(android.R.attr.listChoiceIndicatorSingle, typedValue, true);
            mCheckMarkDrawable = typedValue.resourceId;

            inflater = (LayoutInflater.from(context));
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView == null) {
                convertView = inflater.inflate(R.layout.two_lines_list_item_font, null);
            }

            String f = getItem(position);

            CheckedTextView line1 = (CheckedTextView) convertView.findViewById(android.R.id.text1);
            TextView line2 = (TextView) convertView.findViewById(android.R.id.text2);

            line1.setText(f);

            if(position == 0) {
                line1.setCheckMarkDrawable(null);
                line2.setVisibility(View.GONE);
            } else {
                line1.setCheckMarkDrawable(mCheckMarkDrawable);

                Typeface font;
                if(position == 1) {
                    font = Typeface.DEFAULT;
                } else if(position == 2) {
                    font = LLApp.get().getIconsTypeface();
                } else {
                    font = mTypefaces.get(f);
                    if (font == null) {
                        try {
                            font = Typeface.createFromFile(new File(mFontsDir, f));
                            mTypefaces.put(f, font);
                        } catch (Exception e) {
                            // pass, use a default font
                        }
                    }
                    if (font == null) {
                        font = Typeface.DEFAULT;
                        mTypefaces.put(f, font);
                    }
                }

                line2.setVisibility(View.VISIBLE);
                line2.setTypeface(font);
                line2.setText(FONT_ITEM_PREVIEW);
                line1.setChecked(position == mSelectedIndex);
            }

            return convertView;
        }
    }

    private boolean applyPreferenceOnStylableObject(Object object, LLPreference preference) {
        final int preferenceId = preference.getId();

        boolean booleanValue = false;
        int intValue = 0;
        float floatValue = 0;
        Enum enumValue = null;
        int colorValue = 0;
        final ItemConfig itemConfig;

        boolean processed = true;

        if(preference instanceof LLPreferenceCheckBox) {
            booleanValue = ((LLPreferenceCheckBox) preference).isChecked();
        } else if(preference instanceof LLPreferenceSlider) {
            intValue = (int)((LLPreferenceSlider) preference).getValue();
            floatValue = ((LLPreferenceSlider) preference).getValue();
        } else if(preference instanceof LLPreferenceColor) {
            colorValue = ((LLPreferenceColor)preference).getColor();
        } else if(preference instanceof LLPreferenceList) {
            final LLPreferenceList preferenceList = (LLPreferenceList) preference;
            switch (preferenceList.getValueType()) {
                case ENUM:
                    enumValue = preferenceList.getValueEnum();
                    break;

                case EVENT_ACTION:
                    break;

                case INDEX:
                    break;
            }
        }

        boolean isItemConfigStylable = object instanceof ItemConfigStylable;
        ItemConfigStylable ics = isItemConfigStylable ? (ItemConfigStylable) object : null;

        boolean isShortcutConfigStylable = object instanceof ShortcutConfigStylable;
        ShortcutConfigStylable scs = isShortcutConfigStylable ? (ShortcutConfigStylable) object : null;

        boolean isFolderConfigStylable = object instanceof FolderConfigStylable;
        FolderConfigStylable fcs = isFolderConfigStylable ? (FolderConfigStylable) object : null;


        switch (preferenceId) {
            case ID_mItemMiscLaunchAnimation:
                ics.modifyItemConfig().launchAnimation = (ItemConfig.LaunchAnimation) enumValue;
                break;

            case ID_mItemMiscSelectionEffect:
                ics.modifyItemConfig().selectionEffect = (ItemConfig.SelectionEffect) enumValue;
                break;

            case ID_mItemMiscSelectionEffectMask:
                ics.modifyItemConfig().selectionEffectMask = booleanValue;
                break;

            case ID_mItemMiscPinMode:
                final ItemConfig.PinMode pinMode = (ItemConfig.PinMode) enumValue;
                ics.modifyItemConfig().pinMode = pinMode;
                if(object instanceof Item) {
                    mDashboard.setItemPinMode(mItemLayout.getItemView((Item)object), pinMode);
                }
                break;

            case ID_mItemMiscRotate:
                ics.modifyItemConfig().rotate = booleanValue;
                break;

            case ID_mItemMiscEnabled:
                ics.modifyItemConfig().enabled = booleanValue;
                break;

            case ID_mItemMiscAlpha:
                ics.modifyItemConfig().alpha = intValue;
                if(object instanceof Item) {
                    ((Item)object).setAlpha(intValue);
                }
                break;

            case ID_mItemMiscSmoothTransformed:
                ics.modifyItemConfig().filterTransformed = booleanValue;
                break;

            case ID_mItemMiscHardwareAccelerated:
                ics.modifyItemConfig().hardwareAccelerated = booleanValue;
                break;

            case ID_mItemBoxAlignH:
                itemConfig = ics.modifyItemConfig();
                itemConfig.box.ah = (Box.AlignH) enumValue;
                itemConfig.box_s = itemConfig.box.toString(ic_def==null ? null : ic_def.box);
                break;

            case ID_mItemBoxAlignV:
                itemConfig = ics.modifyItemConfig();
                itemConfig.box.av = (Box.AlignV) enumValue;
                itemConfig.box_s = itemConfig.box.toString(ic_def==null ? null : ic_def.box);
                break;

            case ID_mItemBoxSize:
            case ID_mItemBoxColorNormal:
            case ID_mItemBoxColorSelected:
            case ID_mItemBoxColorFocused:
            case ID_mItemBoxColorBasic:
                int sel = mItemBoxBox.getSelection();
                itemConfig = ics.modifyItemConfig();
                final Box box = itemConfig.box;
                switch (preferenceId) {
                    case ID_mItemBoxSize:
                        final int[] box_sizes = box.size;
                        // BoxEditorView.SEG_ML to PB need to match Box.ML to PB
                        for (int i = BoxEditorView.SEG_ML; i <= BoxEditorView.SEG_PB; i++) {
                            if ((sel & 1 << i) != 0) {
                                box_sizes[i] = intValue;
                            }
                        }
                        break;

                    case ID_mItemBoxColorNormal:
                        applyBorderColors(sel, preference, Box.COLOR_SHIFT_N, box, ic_def == null ? null : ic_def.box);
                        if ((sel & 1 << BoxEditorView.SEG_CONTENT) != 0) {
                            box.ccn = colorValue;
                        }
                        break;

                    case ID_mItemBoxColorSelected:
                        applyBorderColors(sel, preference, Box.COLOR_SHIFT_S, box, ic_def == null ? null : ic_def.box);
                        if ((sel & 1 << BoxEditorView.SEG_CONTENT) != 0) {
                            box.ccs = colorValue;
                        }
                        break;

                    case ID_mItemBoxColorFocused:
                        applyBorderColors(sel, preference, Box.COLOR_SHIFT_F, box, ic_def == null ? null : ic_def.box);
                        if ((sel & 1 << BoxEditorView.SEG_CONTENT) != 0) {
                            box.ccf = colorValue;
                        }
                        break;

                    case ID_mItemBoxColorBasic:
                        applyBorderColors(sel, preference, Box.COLOR_SHIFT_N, box, ic_def == null ? null : ic_def.box);
                        applyBorderColors(sel, preference, Box.COLOR_SHIFT_S, box, ic_def == null ? null : ic_def.box);
                        applyBorderColors(sel, preference, Box.COLOR_SHIFT_F, box, ic_def == null ? null : ic_def.box);
                        if ((sel & 1 << BoxEditorView.SEG_CONTENT) != 0) {
                            box.ccn = colorValue;
                            box.ccs = colorValue;
                            box.ccf = colorValue;
                        }
                        break;
                }
                itemConfig.box_s = itemConfig.box.toString(ic_def==null ? null : ic_def.box);
                break;

            case ID_mItemLabelDisplay:
                scs.modifyShortcutConfig().labelVisibility = booleanValue;
                break;

            case ID_mItemLabelSize:
                scs.modifyShortcutConfig().labelFontSize = floatValue;
                break;

            case ID_mItemLabelStyle:
                scs.modifyShortcutConfig().labelFontStyle = (ShortcutConfig.FontStyle) enumValue;
                break;

            case ID_mItemLabelNumLines:
                scs.modifyShortcutConfig().labelMaxLines = intValue;
                break;

            case ID_mItemLabelColorNormal:
                scs.modifyShortcutConfig().labelFontColor = colorValue;
                break;

            case ID_mItemLabelColorSelected:
                scs.modifyShortcutConfig().selectionColorLabel = colorValue;
                break;

            case ID_mItemLabelColorFocused:
                scs.modifyShortcutConfig().focusColorLabel = colorValue;
                break;

            case ID_mItemTextColorBasic:
                final ShortcutConfig config = scs.modifyShortcutConfig();
                config.labelFontColor = colorValue;
                config.selectionColorLabel = colorValue;
                config.focusColorLabel = colorValue;
                break;

            case ID_mItemLabelShadow:
                scs.modifyShortcutConfig().labelShadow = booleanValue;
                break;

            case ID_mItemLabelShadowRadius:
                scs.modifyShortcutConfig().labelShadowRadius = floatValue;
                break;

            case ID_mItemLabelShadowOffsetX:
                scs.modifyShortcutConfig().labelShadowOffsetX = floatValue;
                break;

            case ID_mItemLabelShadowOffsetY:
                scs.modifyShortcutConfig().labelShadowOffsetY = floatValue;
                break;

            case ID_mItemLabelShadowColor:
                scs.modifyShortcutConfig().labelShadowColor = colorValue;
                break;

            case ID_mItemIconDisplay:
                scs.modifyShortcutConfig().iconVisibility = booleanValue;
                break;

            case ID_mItemIconSmooth:
                scs.modifyShortcutConfig().iconFilter = booleanValue;
                break;

            case ID_mItemIconSizeMode:
                scs.modifyShortcutConfig().iconSizeMode = (ShortcutConfig.IconSizeMode) enumValue;
                break;

            case ID_mItemIconScale:
                scs.modifyShortcutConfig().iconScale = floatValue;
                break;


            case ID_mItemIconColorFilter:
                scs.modifyShortcutConfig().iconColorFilter = colorValue;
                break;

            case ID_mItemIconEffectScale:
                scs.modifyShortcutConfig().iconEffectScale = floatValue;
                break;

            case ID_mItemIconReflection:
                scs.modifyShortcutConfig().iconReflection = booleanValue;
                break;

            case ID_mItemIconReflectionOverlap:
                scs.modifyShortcutConfig().iconReflectionOverlap = floatValue;
                break;

            case ID_mItemIconReflectionSize:
                scs.modifyShortcutConfig().iconReflectionSize = floatValue;
                break;

            case ID_mItemIconReflectionScale:
                scs.modifyShortcutConfig().iconReflectionScale = floatValue;
                break;

            case ID_mItemLayoutMargin:
                scs.modifyShortcutConfig().labelVsIconMargin = intValue;
                break;

            case ID_mItemLayoutPosition:
                scs.modifyShortcutConfig().labelVsIconPosition = (ShortcutConfig.LabelVsIconPosition) enumValue;
                break;

            case ID_mItemIconFolder:
                fcs.modifyFolderConfig().iconStyle = (FolderConfig.FolderIconStyle) enumValue;
                if(object instanceof Folder) {
                    Folder folder = (Folder) object;

                    // when switching to a style with a dynamic background, force the recreation of
                    // the background with a default one, which will in turn trigger the recreation
                    // of a dynamic background
                    Utils.resetDynamicFolderBackground(ShortcutConfig.getIconBackFile(mPage.getIconDir(), folder.getId()), folder.getStdIconSize());

                    Utils.updateFolderIcon(folder);
                } else {
                    Utils.updateFolderIconStyle((Page)object);
                }
                break;

            default:
                processed = false;
                break;
        }

        return processed;
    }

    private ItemConfig getItemConfig() {
        return mForPage ? mPage.getItemConfig() : mItem.getItemConfig();
    }

    private ShortcutConfig getShortcutConfig() {
        return mForPage ? mPage.getShortcutConfig() : ((ShortcutConfigStylable)mItem).getShortcutConfig();
    }

    private static final int ID_mItemLabelDisplay = 1;
//    private static final int ID_mItemLabelCustom = 2;
    private static final int ID_mItemLabelSize = 3;
    private static final int ID_mItemLabelStyle = 4;
    private static final int ID_mItemLabelFont = 5;
    private static final int ID_mItemLabelNumLines = 6;
    private static final int ID_mItemLabelColorNormal = 7;
    private static final int ID_mItemLabelColorSelected = 8;
    private static final int ID_mItemLabelColorFocused = 9;
    private static final int ID_mItemLabelShadow = 10;
    private static final int ID_mItemLabelShadowRadius = 11;
    private static final int ID_mItemLabelShadowOffsetX = 12;
    private static final int ID_mItemLabelShadowOffsetY = 13;
    private static final int ID_mItemLabelShadowColor = 14;
    private static final int ID_mItemIconDisplay = 15;
//    private static final int ID_mItemIconCustom = 16;
    private static final int ID_mItemIconFolder = 17;
    private static final int ID_mItemIconSmooth = 18;
    private static final int ID_mItemIconScale = 19;
    private static final int ID_mItemIconEffectBack = 25;
    private static final int ID_mItemIconEffectOver = 26;
    private static final int ID_mItemIconEffectMask = 27;
    private static final int ID_mItemIconEffectScale = 28;
    private static final int ID_mItemIconReflection = 29;
    private static final int ID_mItemIconReflectionOverlap = 30;
    private static final int ID_mItemIconReflectionSize = 31;
    private static final int ID_mItemIconReflectionScale = 32;
    private static final int ID_mItemLayoutMargin = 33;
    private static final int ID_mItemLayoutPosition = 34;
    private static final int ID_mItemBoxAlignH = 35;
    private static final int ID_mItemBoxAlignV = 36;
    private static final int ID_mItemBoxBox = 37;
    private static final int ID_mItemBoxSize = 38;
    private static final int ID_mItemBoxColorNormal = 39;
    private static final int ID_mItemBoxColorSelected = 40;
    private static final int ID_mItemBoxColorFocused = 41;
    private static final int ID_mItemMiscEnabled = 42;
    private static final int ID_mItemMiscAlpha = 43;
    private static final int ID_mItemMiscSmoothTransformed = 44;
    private static final int ID_mItemMiscEventSwipeLeft = 45;
    private static final int ID_mItemMiscEventSwipeRight = 46;
    private static final int ID_mItemMiscEventSwipeUp = 47;
    private static final int ID_mItemMiscEventSwipeDown = 48;
    private static final int ID_mItemMiscEventLongTap = 49;
    private static final int ID_mItemMiscEventTap = 107;
    private static final int ID_mItemMiscPinMode = 108;
    private static final int ID_mItemSpDirLeftToRight = 114;
    private static final int ID_mItemSpDirRightToLeft = 115;
    private static final int ID_mItemSpDirTopToBottom = 116;
    private static final int ID_mItemSpDirBottomToTop = 117;
    private static final int ID_mItemSpStopScroll = 118;
    private static final int ID_mItemSpStopDrag = 119;
    private static final int ID_mItemSpBarrier = 120;
    private static final int ID_mItemSpDesktopWide = 121;
    private static final int ID_mItemSpSnapping = 122;
    private static final int ID_mItemSpMatchEdgeLeft = 131;
    private static final int ID_mItemSpMatchEdgeRight = 132;
    private static final int ID_mItemSpMatchEdgeTop = 133;
    private static final int ID_mItemSpMatchEdgeBottom = 134;
    private static final int ID_ninePatch= 135;
    private static final int ID_mItemMiscSelectionEffect = 142;
    private static final int ID_mItemMiscRotate = 143;
    private static final int ID_mItemSpReachedEvent = 144;
    private static final int ID_mItemMiscName = 147;
    private static final int ID_mItemBoxColorBasic = 149;
    private static final int ID_mItemTextColorBasic = 150;
    private static final int ID_mPageEventPaused = 153;
    private static final int ID_mPageEventResumed = 154;
    private static final int ID_mItemMiscEventTouch = 160;
    private static final int ID_mItemIconColorFilter = 162;
    private static final int ID_mItemMiscSelectionEffectMask = 169;
    private static final int ID_mItemPIStyle = 170;
    private static final int ID_mItemPIRawFormat = 171;
    private static final int ID_mItemPIDotsMarginX = 172;
    private static final int ID_mItemPIDotsMarginY = 173;
    private static final int ID_mItemPIDotsOuterRadius = 174;
    private static final int ID_mItemPIDotsInnerRadius = 175;
    private static final int ID_mItemPIDotsOuterStrokeWidth = 176;
    private static final int ID_mItemPIDotsOuterColor = 177;
    private static final int ID_mItemPIDotsInnerColor = 178;
    private static final int ID_mItemPIMiniMapOutStrokeColor = 179;
    private static final int ID_mItemPIMiniMapOutFillColor = 180;
    private static final int ID_mItemPIMiniMapOutStrokeWidth = 181;
    private static final int ID_mItemPIMiniMapInStrokeColor = 182;
    private static final int ID_mItemPIMiniMapInFillColor = 183;
    private static final int ID_mItemPIMiniMapInStrokeWidth = 184;
    private static final int ID_mItemPILineBgWidth = 185;
    private static final int ID_mItemPILineBgColor = 186;
    private static final int ID_mItemPILineFgWidth = 187;
    private static final int ID_mItemPILineFgColor = 188;
    private static final int ID_mItemPILineGravity = 189;
    private static final int ID_mItemDtTextFormat = 193;
    private static final int ID_mItemMiscEventCVCreate = 195;
    private static final int ID_mItemMiscEventCVDestroy = 196;
    private static final int ID_mItemBindingsAdd = 197;
    private static final int ID_mItemMiscHardwareAccelerated = 198;
    private static final int ID_mItemMiscLaunchAnimation = 199;
    private static final int ID_mItemIconSizeMode = 200;
    private static final int ID_mItemMiscEventMenu = 204;

    private static final int ID_mItemDtSource = 10004;
    private static final int ID_mItemDtDateEasyFormat = 10005;
    private static final int ID_mItemDtDateExpertFormat = 10006;
    private static final int ID_mItemDtDisplayEmpty = 10007;
    private static final int ID_mItemDtCountFormat = 10008;
    private static final int ID_mItemDtStorageSource = 10009;
    private static final int ID_mItemDtStorageFormat = 10010;
    private static final int ID_mItemDtStorageWhat = 10011;
    private static final int ID_mItemDtGmailLabel = 10012;
}
