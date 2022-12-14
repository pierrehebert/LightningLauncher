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

package net.pierrox.lightning_launcher.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.readystatesoftware.systembartint.SystemBarTintManager;

import net.pierrox.lightning_launcher.API;
import net.pierrox.lightning_launcher.LLApp;
import net.pierrox.lightning_launcher.Version;
import net.pierrox.lightning_launcher.api.ScreenIdentity;
import net.pierrox.lightning_launcher.configuration.FolderConfig;
import net.pierrox.lightning_launcher.configuration.FolderConfig.FolderAnimation;
import net.pierrox.lightning_launcher.configuration.GlobalConfig;
import net.pierrox.lightning_launcher.configuration.GlobalConfig.PageAnimation;
import net.pierrox.lightning_launcher.configuration.ItemConfig;
import net.pierrox.lightning_launcher.configuration.JsonFields;
import net.pierrox.lightning_launcher.configuration.PageConfig;
import net.pierrox.lightning_launcher.configuration.PageConfig.OverScrollMode;
import net.pierrox.lightning_launcher.configuration.PageConfig.ScreenOrientation;
import net.pierrox.lightning_launcher.configuration.PageConfig.ScrollingDirection;
import net.pierrox.lightning_launcher.configuration.PageConfig.SizeMode;
import net.pierrox.lightning_launcher.configuration.ShortcutConfig;
import net.pierrox.lightning_launcher.configuration.SystemConfig;
import net.pierrox.lightning_launcher.data.Action;
import net.pierrox.lightning_launcher.data.ActionsDescription;
import net.pierrox.lightning_launcher.data.Box;
import net.pierrox.lightning_launcher.data.Box.AlignH;
import net.pierrox.lightning_launcher.data.Box.AlignV;
import net.pierrox.lightning_launcher.data.ContainerPath;
import net.pierrox.lightning_launcher.data.EmbeddedFolder;
import net.pierrox.lightning_launcher.data.EventAction;
import net.pierrox.lightning_launcher.data.FileUtils;
import net.pierrox.lightning_launcher.data.Folder;
import net.pierrox.lightning_launcher.data.IconPack;
import net.pierrox.lightning_launcher.data.Item;
import net.pierrox.lightning_launcher.data.JsonLoader;
import net.pierrox.lightning_launcher.data.Page;
import net.pierrox.lightning_launcher.data.StopPoint;
import net.pierrox.lightning_launcher.data.Utils;
import net.pierrox.lightning_launcher.engine.LightningEngine;
import net.pierrox.lightning_launcher.engine.Screen;
import net.pierrox.lightning_launcher.overlay.WindowService;
import net.pierrox.lightning_launcher.prefs.LLPreference;
import net.pierrox.lightning_launcher.prefs.LLPreferenceBinding;
import net.pierrox.lightning_launcher.prefs.LLPreferenceBox;
import net.pierrox.lightning_launcher.prefs.LLPreferenceCategory;
import net.pierrox.lightning_launcher.prefs.LLPreferenceCheckBox;
import net.pierrox.lightning_launcher.prefs.LLPreferenceColor;
import net.pierrox.lightning_launcher.prefs.LLPreferenceEventAction;
import net.pierrox.lightning_launcher.prefs.LLPreferenceList;
import net.pierrox.lightning_launcher.prefs.LLPreferenceListView;
import net.pierrox.lightning_launcher.prefs.LLPreferenceListView.OnLLPreferenceListViewEventListener;
import net.pierrox.lightning_launcher.prefs.LLPreferenceSlider;
import net.pierrox.lightning_launcher.prefs.LLPreferenceSlider.ValueType;
import net.pierrox.lightning_launcher.util.PhoneUtils;
import net.pierrox.lightning_launcher.util.Setup;
import net.pierrox.lightning_launcher.views.BoxEditorView;
import net.pierrox.lightning_launcher.views.BoxLayout;
import net.pierrox.lightning_launcher.views.HandleView;
import net.pierrox.lightning_launcher.views.ItemLayout;
import net.pierrox.lightning_launcher.views.ItemLayout.ItemLayoutListener;
import net.pierrox.lightning_launcher.views.MyViewPager;
import net.pierrox.lightning_launcher.views.MyViewPager.OnPageChangeListener;
import net.pierrox.lightning_launcher.views.NativeImage;
import net.pierrox.lightning_launcher.views.NativeWallpaperView;
import net.pierrox.lightning_launcher.views.item.ItemView;
import net.pierrox.lightning_launcher_extreme.BuildConfig;
import net.pierrox.lightning_launcher_extreme.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class Customize extends ResourceWrapperActivity implements
		OnLLPreferenceListViewEventListener, ItemLayoutListener,
		OnPageChangeListener {
	private static final int REQUEST_EDIT_EVENT_ACTION = 1;
	private static final int REQUEST_LOAD_STYLE = 2;
	private static final int REQUEST_SAVE_STYLE = 3;
	private static final int REQUEST_PICK_LANGUAGE_PACK = 8;
	private static final int REQUEST_PICK_SCREEN_WALLPAPER = 10;
    private static final int REQUEST_PICK_ICON_PACK_FOR_APPLY = 12;
    private static final int REQUEST_PICK_DESKTOP_LOCK_SCREEN = 15;
    private static final int REQUEST_PICK_IMAGE = 17;
    private static final int REQUEST_PICK_DESKTOP_OVERLAY = 18;
    private static final int REQUEST_OVERLAY_PERMISSION = 19;

	private static final int DIALOG_CONFIRM_RESET_PAGE = 1;
	private static final int DIALOG_CONFIRM_RELAYOUT = 2;
	private static final int DIALOG_SELECT_STYLE_NAME = 3;
    private static final int DIALOG_HELP_HINT = 7;
    private static final int DIALOG_PROGRESS = 8;
    private static final int DIALOG_LOCK_SCREEN_WARNING = 10;
    private static final int DIALOG_RATE = 11;

	/* package */public static final String INTENT_EXTRA_PAGE_ID = "p";
	/* package */public static final String INTENT_EXTRA_PAGE_PATH = "t";
	/* package */public static final String INTENT_EXTRA_LAUNCHED_FROM = "f";

	public static final String INTENT_EXTRA_GOTO = "g";
	/* package */static final String INTENT_EXTRA_GOTO_GENERAL_LANGUAGE = "g1";
	/* package */static final String INTENT_EXTRA_GOTO_GENERAL_EVENTS = "g2";
    /* package */static final String INTENT_EXTRA_GOTO_DASHBOARD_BACKGROUND = "g4";
    /* package */static final String INTENT_EXTRA_GOTO_DASHBOARD_GRID = "g5";
    /* package */static final String INTENT_EXTRA_GOTO_DASHBOARD_FOLDER_LOOK = "g6";
    /* package */static final String INTENT_EXTRA_GOTO_DASHBOARD_LAYOUT = "g7";
    /* package */static final String INTENT_EXTRA_GOTO_DASHBOARD_ZOOM_SCROLL = "g8";
    /* package */static final String INTENT_EXTRA_GOTO_DASHBOARD_FOLDER_FEEL = "g9";
    /* package */static final String INTENT_EXTRA_GOTO_DASHBOARD_MISC = "ga";
    /* package */static final String INTENT_EXTRA_GOTO_DASHBOARD_ICONS = "gb";
    public static final String INTENT_EXTRA_GOTO_GENERAL_LOCK_SCREEN = "gc";
    /* package */static final String INTENT_EXTRA_GOTO_DASHBOARD_EVENTS = "gd";
    /* package */static final String INTENT_EXTRA_GOTO_DASHBOARD_SYSTEM_BARS = "ge";
    /* package */static final String INTENT_EXTRA_GOTO_DASHBOARD_AD_MODES = "gf";
    /* package */static final String INTENT_EXTRA_GOTO_GENERAL_OVERLAY = "gg";

    private static final String STYLE_OPENER_FOLDER_CONFIG = "openerFolderConfig";
	private Screen mScreen;

	private enum GridMode {
        PORTRAIT_AND_LANDSCAPE,
        PORTRAIT,
        LANDSCAPE
    }

	private int mPreferenceScreenLevel = -1;
	private LLPreferenceListView[] mPreferenceScreens;

	private MyViewPager mMyViewPager;
	private ItemLayout mItemLayoutPage;
	private BoxLayout mItemLayoutPageBox;
	private NativeWallpaperView mWallpaperView;

	private boolean mModified;

	private LightningEngine mEngine;
	private SystemConfig mSystemConfig;
	private GlobalConfig mGlobalConfig;

	private ContainerPath mPagePath;
	private Page mPage;
	private Page mOpenerPage;
	private Folder mOpenerItem;

	private FolderConfig fc, fc_def;

	private String mStylePath;

	private LLPreference mPickedPreference;

	private String mIntentGoto;
	
    private GridMode mGridMode;

    private SystemBarTintManager mSystemBarTintManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
        // do not set the theme if we are a subclass
        if(getClass() == Customize.class) {
            Utils.setTheme(this, Utils.APP_THEME);
        }

		super.onCreate(savedInstanceState);

        if(Build.VERSION.SDK_INT>=19) {
            mSystemBarTintManager = new SystemBarTintManager(getWindow());
        }

		setContentView(R.layout.customize);

		mMyViewPager = (MyViewPager) findViewById(R.id.view_pager);
		mMyViewPager.setOnPageChangeListener(this);
		mPreferenceScreens = new LLPreferenceListView[2];
		mPreferenceScreens[0] = (LLPreferenceListView) findViewById(R.id.pref_screen_0);
		mPreferenceScreens[0].setListener(this);
		mPreferenceScreens[1] = (LLPreferenceListView) findViewById(R.id.pref_screen_1);
		mPreferenceScreens[1].setListener(this);

		mItemLayoutPageBox = (BoxLayout) findViewById(R.id.il_page_box);
		mItemLayoutPage = new ItemLayout(this, null);
		mItemLayoutPageBox.setChild(mItemLayoutPage, new Box());

		((TextView) findViewById(R.id.preview_hint))
				.setText(R.string.il_preview);

		mWallpaperView = (NativeWallpaperView) findViewById(R.id.wp);

        WallpaperManager wpm = WallpaperManager.getInstance(this);
        try {
        	findViewById(R.id.il_page_container).setBackgroundDrawable(wpm.getDrawable());
        } catch(Throwable e) {
        	// fails on some devices
        }

		LLApp app = LLApp.get();
		mEngine = app.getAppEngine();
		mSystemConfig = app.getSystemConfig();
		mGlobalConfig = mEngine.getGlobalConfig();

		Intent intent = getIntent();
		String path = intent.getStringExtra(INTENT_EXTRA_PAGE_PATH);
		if(path == null) {
			int p = intent.getIntExtra(INTENT_EXTRA_PAGE_ID, Page.NONE);
			mPagePath = new ContainerPath(p);
		} else {
			mPagePath = new ContainerPath(path);
		}

		int page = mPagePath.getLast();

		if (page == Page.NONE) {
			loadGlobalConfig();
			mMyViewPager.removeViewAt(1);
            setScreenTitle(getString(R.string.general_t));
		} else {
			mScreen = new Screen(this, 0) {
				@Override
				public ScreenIdentity getIdentity() {
					return ScreenIdentity.CUSTOMIZE;
				}

				@Override
				protected Resources getRealResources() {
					return Customize.this.getRealResources();
				}

				@Override
				public ItemLayout getCurrentRootItemLayout() {
					return mItemLayoutPage;
				}

				@Override
				public ItemLayout loadRootItemLayout(int page, boolean reset_navigation_history, boolean displayImmediately, boolean animate) {
					if(mItemLayoutPage.getPage().id == page) {
						return mItemLayoutPage;
					} else {
						return loadRootItemLayoutOffscreen(page, reset_navigation_history, displayImmediately, animate);
					}
				}

				@Override
				public void launchItem(ItemView itemView) {
					// pass
				}

				@Override
				public boolean runAction(LightningEngine engine, String source, EventAction ea, ItemLayout il, ItemView itemView) {
					switch (ea.action) {
						case GlobalConfig.SHOW_NOTIFICATIONS:
						case GlobalConfig.SHOW_HIDE_STATUS_BAR:
							processNextAction(engine, source, ea, il, itemView);
							return true;

						default:
							return super.runAction(engine, source, ea, il, itemView);
					}
				}
			};
			mScreen.setWindow(getWindow());
			mItemLayoutPage.setScreen(mScreen);
			mScreen.takeItemLayoutOwnership(mItemLayoutPage);
            loadPage(mPagePath);
		}

		mIntentGoto = intent.getStringExtra(INTENT_EXTRA_GOTO);
		if(mIntentGoto != null) {
			if(mIntentGoto.equals(INTENT_EXTRA_GOTO_GENERAL_EVENTS)) {
				pushPreferenceScreen(mPreferencesGlobalConfigEvents);
			} else if(mIntentGoto.equals(INTENT_EXTRA_GOTO_GENERAL_LANGUAGE)) {
				pushPreferenceScreen(mPreferencesGlobalConfigLanguage);
			} else if(mIntentGoto.equals(INTENT_EXTRA_GOTO_GENERAL_LOCK_SCREEN)) {
				pushPreferenceScreen(mPreferencesGlobalConfigLockScreen);
            } else if(mIntentGoto.equals(INTENT_EXTRA_GOTO_GENERAL_OVERLAY)) {
				pushPreferenceScreen(mPreferencesGlobalConfigOverlay);
			} else if(mIntentGoto.equals(INTENT_EXTRA_GOTO_DASHBOARD_BACKGROUND)) {
				pushPreferenceScreen(mPreferencesPageBackground);
			} else if(mIntentGoto.equals(INTENT_EXTRA_GOTO_DASHBOARD_GRID)) {
				pushPreferenceScreen(mPreferencesPageGrid);
			} else if(mIntentGoto.equals(INTENT_EXTRA_GOTO_DASHBOARD_FOLDER_LOOK)) {
				pushPreferenceScreen(mPreferencesPageFolderLook);
            } else if(mIntentGoto.equals(INTENT_EXTRA_GOTO_DASHBOARD_SYSTEM_BARS)) {
				pushPreferenceScreen(mPreferencesPageSystemBars);
			} else if(mIntentGoto.equals(INTENT_EXTRA_GOTO_DASHBOARD_LAYOUT)) {
				pushPreferenceScreen(mPreferencesPageLayout);
			} else if(mIntentGoto.equals(INTENT_EXTRA_GOTO_DASHBOARD_ZOOM_SCROLL)) {
				pushPreferenceScreen(mPreferencesPageZoomScroll);
			} else if(mIntentGoto.equals(INTENT_EXTRA_GOTO_DASHBOARD_EVENTS)) {
				pushPreferenceScreen(mPreferencesPageEvents);
			} else if(mIntentGoto.equals(INTENT_EXTRA_GOTO_DASHBOARD_FOLDER_FEEL)) {
				pushPreferenceScreen(mPreferencesPageFolderFeel);
			} else if(mIntentGoto.equals(INTENT_EXTRA_GOTO_DASHBOARD_MISC)) {
				pushPreferenceScreen(mPreferencesPageMisc);
            } else if(mIntentGoto.equals(INTENT_EXTRA_GOTO_DASHBOARD_AD_MODES)) {
				pushPreferenceScreen(mPreferencesPageADModes);
            } else if(mIntentGoto.equals(INTENT_EXTRA_GOTO_DASHBOARD_ICONS)) {
				pushPreferenceScreen(mPreferencesPageIcons);
			}
		}

        if(app.isTrialVersionExpired()) {
            app.showFeatureLockedDialog(this);
        } else {
            if ((mSystemConfig.hints&SystemConfig.HINT_CUSTOMIZE_HELP) == 0) {
                showDialog(DIALOG_HELP_HINT);
            } else if ((mSystemConfig.hints&SystemConfig.HINT_RATE) == 0 && BuildConfig.IS_TRIAL) {
                try {
                    PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
                    Field f = pi.getClass().getField("firstInstallTime");
                    long firstInstallTime = f.getLong(pi);
                    if ((System.currentTimeMillis() - firstInstallTime) > 20 * 86400 * 1000) {
                        showDialog(DIALOG_RATE);
                    }
                } catch (Exception e) {
                }
            }
        }
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if(mScreen != null) {
			mScreen.destroy();
		}
	}

	private void setScreenTitle(String title) {
        setTitle(title);
        try {
            Method getActionBar=getClass().getMethod("getActionBar");
            Object action_bar=getActionBar.invoke(this, (Object[])null);
            action_bar.getClass().getMethod("setTitle", String.class).invoke(action_bar, title);
        } catch(Exception e) {
            // pass, API level 11
        }
    }

    private boolean mDontSaveOnNextPause;
    
    @Override
	public void onPause() {
		super.onPause();

		if(mScreen != null) {
			mScreen.pause();
		}

		if (mModified && !mDontSaveOnNextPause) {
			if (mPage != null) {
				savePage();
			} else {
				saveSystemAndGlobalConfig();
			}
			mModified = false;
		}
		mDontSaveOnNextPause = false;
	}

	@Override
	protected void onResume() {
		super.onResume();

		if(mScreen != null) {
			mScreen.resume();
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		setIntent(intent);
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.customize, menu);
        menu.findItem(R.id.rs).setTitle(getString(R.string.menu_customize));
        menu.findItem(R.id.h).setTitle(getString(R.string.sc_help));
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.rs: PhoneUtils.startSettings(this, mPagePath, true); finish(); return true;
            case R.id.h: PhoneUtils.showPreferenceHelp(this, null); return true;
            default: return super.onOptionsItemSelected(item);
        }
    }

	@Override
	public void startActivityForResult(Intent intent, int requestCode) {
		super.startActivityForResult(intent, requestCode);
		mDontSaveOnNextPause = true;
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if(mMyViewPager.getCurrentItem() == 1) {
				mMyViewPager.setCurrentItem(0);
			} else if(mPreferenceScreenLevel > 0 && mIntentGoto==null) {
				popPreferenceScreen();
			} else {
				finish();
			}

			return true;
		}
		return super.onKeyUp(keyCode, event);
	}

	@Override
	public void onLLPreferenceClicked(final LLPreference preference) {
		if (preference == mGCEventsCategory) {
			pushPreferenceScreen(mPreferencesGlobalConfigEvents);
		} else if (preference == mGCLockScreenSelect) {
            Intent intent = new Intent(this, ScreenManager.class);
            intent.setAction(Intent.ACTION_PICK);
            startActivityForResult(intent, REQUEST_PICK_DESKTOP_LOCK_SCREEN);
        } else if (preference == mGCOverlayPermission) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
        } else if (preference == mGCOverlaySelect) {
            Intent intent = new Intent(this, ScreenManager.class);
            intent.setAction(Intent.ACTION_PICK);
            startActivityForResult(intent, REQUEST_PICK_DESKTOP_OVERLAY);
		} else if (preference == mGCLanguageCategory) {
			pushPreferenceScreen(mPreferencesGlobalConfigLanguage);
        } else if (preference == mGCLockScreenCategory) {
			pushPreferenceScreen(mPreferencesGlobalConfigLockScreen);
        } else if (preference == mGCOverlayCategory) {
			pushPreferenceScreen(mPreferencesGlobalConfigOverlay);
		} else if (preference == mGCLanguageSelect) {
			Intent i = new Intent(Intent.ACTION_PICK_ACTIVITY);
			i.putExtra(Intent.EXTRA_TITLE, getString(R.string.lg_pick_t));
			Intent filter = new Intent("net.pierrox.lightning_launcher.lp.ENUMERATE");
			i.putExtra(Intent.EXTRA_INTENT, filter);
			try {
				startActivityForResult(i, REQUEST_PICK_LANGUAGE_PACK);
			} catch (Exception e) {
			}
		} else if (preference == mGCLanguageInstall) {
			try {
				startActivity(new Intent(Intent.ACTION_VIEW,
						Version.LANGUAGE_PACK_INSTALL_URI));
			} catch (Exception e) {
			}
		} else if (preference == mGCLanguageContribute) {
			Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse("http://goo.gl/1RzBI"));
			startActivity(intent);
        } else if (preference == mPGIcons) {
            pushPreferenceScreen(mPreferencesPageIcons);
		} else if (preference == mPGBackground) {
			pushPreferenceScreen(mPreferencesPageBackground);
		} else if (preference == mPGGrid) {
			pushPreferenceScreen(mPreferencesPageGrid);
		} else if (preference == mPGFolderLook) {
			pushPreferenceScreen(mPreferencesPageFolderLook);
		} else if (preference == mPGSystemBars) {
			pushPreferenceScreen(mPreferencesPageSystemBars);
		} else if (preference == mPGLayout) {
			pushPreferenceScreen(mPreferencesPageLayout);
		} else if (preference == mPGZoomScroll) {
			pushPreferenceScreen(mPreferencesPageZoomScroll);
		} else if (preference == mPGEvents) {
			pushPreferenceScreen(mPreferencesPageEvents);
		} else if (preference == mPGFolderFeel) {
			pushPreferenceScreen(mPreferencesPageFolderFeel);
		} else if (preference == mPGADModes) {
			pushPreferenceScreen(mPreferencesPageADModes);
        } else if (preference == mPGMisc) {
			pushPreferenceScreen(mPreferencesPageMisc);
		} else if (preference instanceof LLPreferenceEventAction) {
			editEventActionPreference((LLPreferenceEventAction)preference);
		} else if (preference == mPGLoadStyle) {
			Intent intent = new Intent(this, StyleChooser.class);
			startActivityForResult(intent, REQUEST_LOAD_STYLE);
		} else if (preference == mPGSaveStyle) {
			Intent intent = new Intent(this, StyleChooser.class);
			intent.putExtra(StyleChooser.INTENT_EXTRA_NEW, true);
			startActivityForResult(intent, REQUEST_SAVE_STYLE);
		} else if (preference == mPGReset) {
			displayDialog(DIALOG_CONFIRM_RESET_PAGE);
		} else if (preference == mPGBackgroundSelectSystemWallpaper) {
			startActivity(Intent.createChooser(new Intent(Intent.ACTION_SET_WALLPAPER), getString(R.string.bg_sys_wp_select_t)));
		} else if (preference == mPGBackgroundSelectScreenWallpaper) {
            ImagePicker.startActivity(this, REQUEST_PICK_SCREEN_WALLPAPER);
        } else if (preference == mPGApplyIconPack) {
            applyIconPack();
        } else if (preference == mPGRevertCustomIcon) {
            revertCustomIcons();
        } else if (preference == mPGFolderBoxNpNormal || preference == mPGAppDrawerABBackground) {
            mPickedPreference = preference;
            ImagePicker.startActivity(this, REQUEST_PICK_IMAGE);
        }
	}
	
	@Override
	public void onLLPreferenceLongClicked(LLPreference preference) {
		PhoneUtils.showPreferenceHelp(this, preference);
	}

	@Override
	public void onLLPreferenceChanged(LLPreference preference) {
		if (mPage != null) {
            if (preference == mPGBackgroundColor) {
                mPage.config.bgColor = mPGBackgroundColor.getColor();
                updateWallpaper();
            } else if (preference == mPGBackgroundScaleType) {
                mPage.config.bgScaleType = (PageConfig.ScaleType) mPGBackgroundScaleType.getValueEnum();
                updateWallpaper();
            } else if (preference == mPGLayoutGridColumnMode
                    || preference == mPGLayoutGridColumnNum
                    || preference == mPGLayoutGridColumnSize
                    || preference == mPGLayoutGridRowMode
                    || preference == mPGLayoutGridRowNum
                    || preference == mPGLayoutGridRowSize) {
                updateGridLayoutDisableState();
            } else if (preference == mPGFolderBoxBox) {
                int selection = mPGFolderBoxBox.getSelection();
                boolean size_disabled = selection == 0
                        || selection == 1 << BoxEditorView.SEG_CONTENT;
                mPGFolderBoxSize.setDisabled(size_disabled);
                boolean color_disabled = (selection & BoxEditorView.SEG_SELECTION_COLOR_MASK) == 0;
                mPGFolderBoxColor.setDisabled(color_disabled);

                int size = 0;
                int default_size = 0;
                for (int i = BoxEditorView.SEG_ML; i <= BoxEditorView.SEG_PB; i++) {
                    if ((selection & 1 << i) != 0) {
                        size = fc.box.size[i];
                        if (fc_def != null) {
                            default_size = fc_def.box.size[i];
                        }
                        break;
                    }
                }
                mPGFolderBoxSize.setValue(size);
                mPGFolderBoxSize.setDefaultValue(default_size);

                final int[] border_color = fc.box.border_color;
                final int[] default_border_color = fc_def == null ? null
                        : fc_def.box.border_color;
                int[] colors = new int[2];
                if ((selection & 1 << (BoxEditorView.SEG_CONTENT)) != 0) {
                    colors[0] = fc.box.ccn;
                    if (default_border_color != null) {
                        colors[1] = fc_def.box.ccn;
                    }
                } else {
                    for (int i = Box.BCL; i <= Box.BCB; i++) {
                        if ((selection & 1 << (BoxEditorView.SEG_BL + i)) != 0) {
                            colors[0] = border_color[Box.COLOR_SHIFT_N + i];
                            if (default_border_color != null) {
                                colors[1] = default_border_color[Box.COLOR_SHIFT_N
                                        + i];
                            }
                            break;
                        }
                    }
                }
                mPGFolderBoxColor.setColor(colors[0]);
                if (default_border_color != null) {
                    mPGFolderBoxColor.setDefaultColor(colors[1]);
                }

                mPreferenceScreens[1].refresh();
            } else if (preference == mPGFolderBoxColor) {
                int color = mPGFolderBoxColor.getColor();
                applyBorderColors(mPGFolderBoxBox.getSelection(),
                        mPGFolderBoxColor, Box.COLOR_SHIFT_N, fc.box,
                        fc_def == null ? null : fc_def.box);
                if ((mPGFolderBoxBox.getSelection() & 1 << BoxEditorView.SEG_CONTENT) != 0)
                    fc.box.ccn = color;
            } else if (preference == mPGFolderBoxSize) {
                int selection = mPGFolderBoxBox.getSelection();
                int size = (int) mPGFolderBoxSize.getValue();
                final int[] box_sizes = fc.box.size;
                // BoxEditorView.SEG_ML to PB need to match Box.ML to PB
                for (int i = BoxEditorView.SEG_ML; i <= BoxEditorView.SEG_PB; i++) {
                    if ((selection & 1 << i) != 0)
                        box_sizes[i] = size;
                }
            } else if (preference == mPGLayoutGridTogglePL) {
                GridMode new_grid_mode = (GridMode) mPGLayoutGridTogglePL.getValueEnum();
                if (mGridMode != new_grid_mode) {
                    if (mGridMode != GridMode.PORTRAIT_AND_LANDSCAPE) {
                        switch (new_grid_mode) {
                            case PORTRAIT:
                                copyLandscapeGridPreferences(mPage.config);
                                mPGLayoutGridColumnMode.setValueIndex(mPage.config.gridPColumnMode.ordinal());
                                mPGLayoutGridColumnNum.setValue(mPage.config.gridPColumnNum);
                                mPGLayoutGridColumnSize.setValue(mPage.config.gridPColumnSize);
                                mPGLayoutGridRowMode.setValueIndex(mPage.config.gridPRowMode.ordinal());
                                mPGLayoutGridRowNum.setValue(mPage.config.gridPRowNum);
                                mPGLayoutGridRowSize.setValue(mPage.config.gridPRowSize);
                                break;

                            case LANDSCAPE:
                                copyPortraitGridPreferences(mPage.config);
                                mPGLayoutGridColumnMode.setValueIndex(mPage.config.gridLColumnMode.ordinal());
                                mPGLayoutGridColumnNum.setValue(mPage.config.gridLColumnNum);
                                mPGLayoutGridColumnSize.setValue(mPage.config.gridLColumnSize);
                                mPGLayoutGridRowMode.setValueIndex(mPage.config.gridLRowMode.ordinal());
                                mPGLayoutGridRowNum.setValue(mPage.config.gridLRowNum);
                                mPGLayoutGridRowSize.setValue(mPage.config.gridLRowSize);
                                break;
                        }
                    }
                    mGridMode = new_grid_mode;
                }
            } else if (preference == mPGFolderLookAlignH) {
                mPGFolderLookCustomX.setDisabled(mPGFolderLookAlignH.getValueEnum()!=AlignH.CUSTOM);
            } else if (preference == mPGFolderLookAlignV) {
                mPGFolderLookCustomY.setDisabled(mPGFolderLookAlignV.getValueEnum()!=AlignV.CUSTOM);
            } else if (mPreferencesPageADModes!=null && mPreferencesPageADModes.contains(preference)) {
                int modes = computeAppDrawerModes();
                if(modes == 0) {
                    ((LLPreferenceCheckBox)preference).setChecked(true);
                    Toast.makeText(this, R.string.adm_z, Toast.LENGTH_SHORT).show();
                }
            }
            copyPreferencesToPageConfiguration();
		} else {
            if(preference == mGCAppStyle || preference == mGCImagePoolSize || preference == mGCHotwords) {
                saveSystemAndGlobalConfig();
                LLApp.get().saveAllData();
                finish();
                startActivity(new Intent(Customize.this, Customize.class));
                System.exit(0);
            } else if(preference == mGCExpertMode) {
                saveSystemAndGlobalConfig();
				overridePendingTransition(0, 0);
				finish();
				overridePendingTransition(0, 0);
                startActivity(new Intent(Customize.this, Customize.class));
            } else if (mPreferencesGlobalConfigOverlay != null && mPreferencesGlobalConfigOverlay.contains(preference)) { // API 14
                // save config now so that changes are immediately visible
                saveSystemAndGlobalConfig();
			}
		}

		mModified = true;
	}

    @Override
    public void onLLPreferenceBindingRemoved(LLPreferenceBinding preference) {
        // pass
    }

    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_LOAD_STYLE) {
			if (resultCode == RESULT_OK) {
				mStylePath = data
						.getStringExtra(StyleChooser.INTENT_EXTRA_PATH);
				loadStyle(mStylePath);
			}
		} else if (requestCode == REQUEST_SAVE_STYLE) {
			if (resultCode == RESULT_OK) {
				mStylePath = data
						.getStringExtra(StyleChooser.INTENT_EXTRA_PATH);
				if (data.getBooleanExtra(StyleChooser.INTENT_EXTRA_NEW, false)) {
					displayDialog(DIALOG_SELECT_STYLE_NAME);
				} else {
					saveStyle(mStylePath);
				}
			}
		} else if (requestCode == REQUEST_PICK_SCREEN_WALLPAPER) {
			if (resultCode == RESULT_OK) {
                File tmp_image_file = Utils.getTmpImageFile();
                final File wallpaperFile = mPage.getWallpaperFile();
                if(tmp_image_file.exists()) {
                    Utils.copyFileSafe(null, tmp_image_file, wallpaperFile);
                    BitmapFactory.Options opts = new BitmapFactory.Options();
                    opts.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(tmp_image_file.getAbsolutePath(), opts);
                    if(opts.outWidth == 1 && opts.outHeight == 1) {
                        mPage.config.bgScaleType = PageConfig.ScaleType.FIT;
                        mPGBackgroundScaleType.setValue(mPage.config.bgScaleType, null);
                        mPreferenceScreens[1].refresh();
                    }
                } else {
                    wallpaperFile.delete();
                }
                NativeImage.deleteImage(NativeImage.getWallpaperKey(mPage.id));
                updateWallpaper();
                mModified = true;
			}
		} else if (requestCode == REQUEST_PICK_IMAGE) {
            if (resultCode == RESULT_OK) {
                FileOutputStream fos = null;
                File icon_dir = mPage.getAndCreateIconDir();
                File tmp_image_file = Utils.getTmpImageFile();
                byte[] buffer = new byte[4096];
                try {
                    if(mPickedPreference == mPGFolderBoxNpNormal || mPickedPreference == mPGAppDrawerABBackground) {
                        File drawable_file;
                        if(mPickedPreference == mPGAppDrawerABBackground) {
                            drawable_file = AppDrawerX.getAppDrawerActionBarBackgroundFile(mPage);
                        } else {
                            if(mOpenerItem != null) {
                                int id = mOpenerItem.getId();
                                icon_dir = mOpenerPage.getAndCreateIconDir();
                                drawable_file = Box.getBoxBackgroundFolder(icon_dir, id);
                            } else {
                                drawable_file = Box.getBoxBackgroundFolder(icon_dir, Item.NO_ID);
                            }
                        }

                        if(tmp_image_file.exists()) {
                            Utils.copyFileSafe(buffer, tmp_image_file, drawable_file);
                        } else {
                            icon_dir = mPage.getIconDir();
                            File icon;
                            if (mPickedPreference == mPGAppDrawerABBackground) {
                                icon = AppDrawerX.getAppDrawerActionBarBackgroundFile(mPage);
                            } else { // if (mPickedPreference == mItemBoxNpFolder) {
                                if(mOpenerItem == null) {
                                    icon = Box.getBoxBackgroundFolder(icon_dir, Item.NO_ID);
                                } else {
                                    icon_dir = mOpenerPage.getIconDir();
                                    icon = Box.getBoxBackgroundFolder(icon_dir, mOpenerItem.getId());
                                }
                                fc.box.bgFolder = fc_def==null ? null : fc_def.box.bgFolder;
                            }
                            icon.delete();
                            mModified = true;
                        }
                    }
                    mModified = true;
                } catch (Exception e) {
                    Toast.makeText(this, R.string.item_settings_icon_copy_failed, Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                } finally {
                    if (fos != null) try { fos.close(); } catch (Exception e) {}
                }
            }
		} else if (requestCode == REQUEST_PICK_LANGUAGE_PACK) {
			if (resultCode == RESULT_OK) {
				String pkg_name = data.getComponent().getPackageName();
				mSystemConfig.language = pkg_name;
				saveSystemAndGlobalConfig();
                LLApp.get().restart(true);
			}
        } else if (requestCode == REQUEST_PICK_ICON_PACK_FOR_APPLY) {
            if (resultCode == RESULT_OK) {
                String packageName = data.getComponent().getPackageName();
                if(packageName.equals(getPackageName())) {
                    mPage.config.iconPack = null;
                    mModified = true;
                } else {
                    doApplyIconPack(packageName);
                }
            }
        } else if (requestCode == REQUEST_PICK_DESKTOP_LOCK_SCREEN) {
            int lock_screen;
            if(resultCode == RESULT_OK) {
                lock_screen = data.getIntExtra(API.SCREEN_PICKER_INTENT_EXTRA_SCREEN, Page.FIRST_DASHBOARD_PAGE);
                showDialog(DIALOG_LOCK_SCREEN_WARNING);
            } else {
                Toast.makeText(this, R.string.s_ls_s_c, Toast.LENGTH_SHORT).show();
                lock_screen = Page.NONE;
            }
            mGlobalConfig.lockScreen = lock_screen;
            mModified = true;
        } else if (requestCode == REQUEST_PICK_DESKTOP_OVERLAY) {
            int overlay_screen;
            if(resultCode == RESULT_OK) {
                overlay_screen = data.getIntExtra(API.SCREEN_PICKER_INTENT_EXTRA_SCREEN, Page.FIRST_DASHBOARD_PAGE);
            } else {
                Toast.makeText(this, R.string.ov_d, Toast.LENGTH_SHORT).show();
                overlay_screen = Page.NONE;
            }
            mGlobalConfig.overlayScreen = overlay_screen;
            // save config now so that changes are immediately visible
            saveSystemAndGlobalConfig();
        } else if (requestCode == REQUEST_OVERLAY_PERMISSION) {
			boolean disabled = !WindowService.isPermissionAllowed(this);
			for (LLPreference p : mPreferencesGlobalConfigOverlay) {
				if (p != mGCOverlayPermission) {
					p.setDisabled(disabled);
				}
			}
			mPreferenceScreens[mPreferenceScreenLevel].refresh();
		} else if(requestCode == REQUEST_EDIT_EVENT_ACTION) {
			if(resultCode == RESULT_OK) {
				if(mEventActionEditPreference != null) {
					mEventActionEditPreference.setValue(EventActionSetup.getEventActionFromIntent(data));
					mPreferenceScreens[mPreferenceScreenLevel].refresh();
					mModified = true;
				}
			}
        } else {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

    @SuppressWarnings("deprecation")
	@Override
	protected Dialog onCreateDialog(int id) {
		final EditText edit_text;
		FrameLayout l;
		AlertDialog.Builder builder;
		String[] items;
        String label;
        final CheckBox dsa;
		switch (id) {
        case DIALOG_HELP_HINT:
            builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.dialog_help_hint_t);
			builder.setMessage(R.string.dialog_help_hint_m);
            dsa = new CheckBox(this);
            dsa.setText(R.string.dialog_help_hint_dsa);
            builder.setView(dsa);
            builder.setPositiveButton(android.R.string.ok,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if(dsa.isChecked()) {
								mSystemConfig.hints |= SystemConfig.HINT_CUSTOMIZE_HELP;
							}
                        }
                    });
            return builder.create();

        case DIALOG_RATE:
            builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.rate_s);
			builder.setMessage(R.string.tr_rm);
            dsa = new CheckBox(this);
            dsa.setText(R.string.dialog_help_hint_dsa);
            builder.setView(dsa);
            builder.setPositiveButton(android.R.string.ok,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mSystemConfig.hints |= SystemConfig.HINT_RATE;
                            startActivity(Intent.createChooser(new Intent(Intent.ACTION_VIEW, Uri.parse(Version.APP_STORE_INSTALL_PREFIX+getPackageName())), ""));
                        }
                    });
            builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(dsa.isChecked()) mSystemConfig.hints |= SystemConfig.HINT_RATE;
                }
            });
            builder.setCancelable(false);
            return builder.create();

		case DIALOG_CONFIRM_RESET_PAGE:
			builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.confirm_reset_page);
			builder.setPositiveButton(android.R.string.ok,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							ArrayList<Screen> screensToRestore = new ArrayList<Screen>();
							for (Screen screen : LLApp.get().getScreens()) {
								if(screen.getCurrentRootPage() == mPage) {
									screensToRestore.add(screen);
								}
							}

                            LightningEngine engine = mPage.getEngine();

                            mPage.remove();
							mPage.destroy();

							mPage.config = new PageConfig();
                            mPage.items = new ArrayList<>();
                            if(mPage.id == mGlobalConfig.homeScreen) {
                                Page drawer = engine.getOrLoadPage(Page.APP_DRAWER_PAGE);
                                Setup.setupDashboard(mPage, drawer);
                            } else if (mPage.id == Page.APP_DRAWER_PAGE) {
                                Setup.setupAppDrawer(mPage);
                            }
                            mPage.notifyModified();

							for (Screen screen : screensToRestore) {
								screen.loadRootItemLayout(mPage.id, false, true, true);
							}

							engine.getPageManager().savePagesSync();
							mItemLayoutPage.setScreen(mScreen);
							mScreen.takeItemLayoutOwnership(mItemLayoutPage);
							mItemLayoutPage.resume();
							loadPage(mPagePath);
                            if(mGlobalConfig.lockScreen == mPage.id) {
                            	mGlobalConfig.lockScreen = Page.NONE;
                                engine.notifyGlobalConfigChanged();
                            }
							if(mGlobalConfig.overlayScreen == mPage.id) {
                            	mGlobalConfig.overlayScreen = Page.NONE;
                                engine.notifyGlobalConfigChanged();
                            }
						}
					});
			builder.setNegativeButton(android.R.string.cancel, null);
			return builder.create();

		case DIALOG_CONFIRM_RELAYOUT:
			builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.relayout_items);
			builder.setPositiveButton(android.R.string.ok,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface arg0, int arg1) {
							loadStyle(mStylePath);
						}
					});
			builder.setNegativeButton(android.R.string.cancel, null);
			return builder.create();

		case DIALOG_SELECT_STYLE_NAME:
			builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.choose_style_name);
			edit_text = new EditText(this);
            label = mPage.config.l;
			edit_text.setText(label);
            if(label != null) {
                edit_text.setSelection(label.length());
            }
			l = new FrameLayout(this);
			l.setPadding(10, 10, 10, 10);
			l.addView(edit_text);
			builder.setView(l);
			builder.setPositiveButton(android.R.string.ok,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface arg0, int arg1) {
                            String label = edit_text.getText().toString();
                            if(label.trim().length() > 0) {
                                mPage.config.l = label;
                                saveStyle(mStylePath);
                            }
						}
					});
			builder.setNegativeButton(android.R.string.cancel, null);
			return builder.create();

        case DIALOG_PROGRESS:
            ProgressDialog pd = new ProgressDialog(this);
            pd.setMessage(getString(R.string.please_wait));
            pd.setCancelable(false);
            return pd;

        case DIALOG_LOCK_SCREEN_WARNING:
            builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.s_ls_t);
            builder.setMessage(R.string.s_ls_w);
            builder.setPositiveButton(android.R.string.ok, null);
            return builder.create();
		}

		return super.onCreateDialog(id);
	}

	@Override
	public void onPageScrolled(int position, float positionOffset,
			int positionOffsetPixels) {
		// pass
	}

	@Override
	public void onPageSelected(int position) {
		if (position == 1) {
			if(mModified) {
                savePage();
            }
		}
	}

	@Override
	public void onPageScrollStateChanged(int state) {
		// pass
	}

    private void setPreferenceLockFlag(ArrayList<LLPreference> preferences) {
        if(preferences != null) {
            setPreferenceLockFlag(preferences.toArray());
        }
    }

    private void setPreferenceLockFlag(Object[] preferences) {
        final boolean need_lock = LLApp.get().isFreeVersion();
        for(Object p : preferences) {
            if(p != null && p instanceof LLPreference) ((LLPreference)p).setLocked(need_lock);
        }
    }
    private boolean isLaunchedFromAppDrawer() {
        ComponentName cn = getIntent().getParcelableExtra(INTENT_EXTRA_LAUNCHED_FROM);
        return cn!=null && cn.equals(new ComponentName(this, AppDrawerX.class));
    }



	private void loadPage(ContainerPath path) {
		Pair<Page, Folder> pageAndOpener = LLApp.get().getAppEngine().getPageAndOpenerFromPath(path);
		mPage = pageAndOpener.first;
		mOpenerItem = pageAndOpener.second;
		mItemLayoutPage.setPage(mPage);

		final boolean is_folder_page = mOpenerItem!=null && mOpenerItem.getClass() == Folder.class;
		final boolean is_embedded_folder_page = mOpenerItem!=null && mOpenerItem.getClass() == EmbeddedFolder.class;
		final boolean is_app_drawer = mPage.id == Page.APP_DRAWER_PAGE;

		if (is_folder_page) {
			mOpenerPage = mOpenerItem.getPage();
		}
        setScreenTitle(Utils.formatPageName(mPage, mOpenerItem));


		final PageConfig pc = mPage.config;
		if (is_folder_page) {
			fc = new FolderConfig();
			fc.copyFrom(mOpenerItem.getFolderConfig());
			fc_def = mOpenerPage.config.defaultFolderConfig;
		} else {
			fc = pc.defaultFolderConfig;
			fc_def = null;
		}
		updateWallpaper();

        mGridMode = pc.gridPL ? GridMode.PORTRAIT_AND_LANDSCAPE : GridMode.PORTRAIT;

		mPreferencesPage = new ArrayList<LLPreference>(14);
		mPreferencesPage.add(new LLPreferenceCategory(this, R.string.appearance));
        mPreferencesPage.add(mPGIcons = new LLPreference(this, ID_mPGIcons, R.string.icons_t, R.string.icons_s));
        mPreferencesPageIcons = new ArrayList<LLPreference>();
        mPreferencesPageIcons.add(mPGApplyIconPack = new LLPreference(this, ID_mPGApplyIconPack, R.string.apply_icon_pack_t, R.string.apply_icon_pack_s));
        mPreferencesPageIcons.add(mPGRevertCustomIcon = new LLPreference(this, ID_mPGRevertCustomIcon, R.string.revert_custom_icons_t, R.string.revert_custom_icons_s));

		if (!is_folder_page && !is_embedded_folder_page) {
			mPreferencesPage.add(mPGBackground = new LLPreference(this, ID_mPGBackground, R.string.background_t, R.string.background_s));
			
			mPreferencesPageBackground = new ArrayList<LLPreference>();
			mPreferencesPageBackground.add(new LLPreferenceCategory(this, R.string.bg_sys_wp));
			mPreferencesPageBackground.add(mPGBackgroundSelectSystemWallpaper = new LLPreference(this, ID_mPGBackgroundSelectSystemWallpaper, R.string.bg_sys_wp_select_t, R.string.bg_sys_wp_select_s));
			mPreferencesPageBackground.add(mPGBackgroundSystemWallpaperScroll  = new LLPreferenceCheckBox(this, ID_mPGBackgroundSystemWallpaperScroll, R.string.wp_scroll_t, R.string.wp_scroll_s, pc.bgSystemWPScroll, null));
			mPreferencesPageBackground.add(mPGBackgroundSystemWallpaperWidth = new LLPreferenceSlider(this, ID_mPGBackgroundSystemWallpaperWidth, R.string.wp_w_t, R.string.wp_auto, pc.bgSystemWPWidth, null, ValueType.INT, 0, 2000, 1, null));
			mPreferencesPageBackground.add(mPGBackgroundSystemWallpaperHeight = new LLPreferenceSlider(this, ID_mPGBackgroundSystemWallpaperHeight, R.string.wp_h_t, R.string.wp_auto, pc.bgSystemWPHeight, null, ValueType.INT, 0, 2000, 1, null));
			if(android.os.Build.VERSION.SDK_INT>=9) {
				mPreferencesPageBackground.add(new LLPreferenceCategory(this, R.string.bg_screen_wp));
				mPreferencesPageBackground.add(mPGBackgroundSelectScreenWallpaper = new LLPreference(this, ID_mPGBackgroundSelectScreenWallpaper, R.string.bg_screen_wp_select_t, R.string.bg_screen_wp_select_s));
                mPreferencesPageBackground.add(mPGBackgroundScaleType = new LLPreferenceList(this, ID_mPGBackgroundScaleType, R.string.bg_st, R.array.bg_st_e, pc.bgScaleType, null));
			}
			mPreferencesPageBackground.add(new LLPreferenceCategory(this, R.string.bg_more));
			mPreferencesPageBackground.add(mPGBackgroundColor = new LLPreferenceColor(this, ID_mPGBackgroundColor, R.string.bg_more_color_t, R.string.bg_more_color_s, pc.bgColor, null, true));
		}

		mPreferencesPage.add(mPGGrid = new LLPreference(this, ID_mPGGrid, R.string.grid_t,
				R.string.grid_s));
		mPreferencesPageGrid = new ArrayList<LLPreference>();
		mPreferencesPageGrid.add(mPGGridHColor = new LLPreferenceColor(this, ID_mPGGridHColor,
				R.string.grid_h_color_t, R.string.grid_color_s,
				pc.gridLayoutModeHorizontalLineColor, null, true));
		mPreferencesPageGrid.add(mPGGridHSize = new LLPreferenceSlider(this, ID_mPGGridHSize,
				R.string.grid_h_size_t, R.string.grid_size_s,
				pc.gridLayoutModeHorizontalLineThickness, null,
				ValueType.FLOAT, 0, 200, 0.5f, null));
		mPreferencesPageGrid.add(mPGGridVColor = new LLPreferenceColor(this, ID_mPGGridVColor,
				R.string.grid_v_color_t, R.string.grid_color_s,
				pc.gridLayoutModeVerticalLineColor, null, true));
		mPreferencesPageGrid.add(mPGGridVSize = new LLPreferenceSlider(this, ID_mPGGridVSize,
				R.string.grid_v_size_t, R.string.grid_size_s,
				pc.gridLayoutModeVerticalLineThickness, null, ValueType.FLOAT,
				0, 200, 0.5f, null));
		mPreferencesPageGrid.add(mPGGridAbove = new LLPreferenceCheckBox(this, ID_mPGGridAbove,
				R.string.grid_above, 0, pc.gridAbove, null));

		//if (!is_app_drawer) {
            if(!is_embedded_folder_page) {
                mPreferencesPage.add(mPGFolderLook = new LLPreference(this, ID_mPGFolderLook,
                        is_folder_page ? R.string.this_folder_look_t
                                : R.string.folder_look_t,
                        is_folder_page ? R.string.this_folder_look_s
                                : R.string.folder_look_s));

                mPreferencesPageFolderLook = new ArrayList<LLPreference>();
                mPreferencesPageFolderLook.add(new LLPreferenceCategory(this,
                        R.string.folder_title));
                mPreferencesPageFolderLook
                        .add(mPGFolderLookTitleDisplay = new LLPreferenceCheckBox(
                                this, ID_mPGFolderLookTitleDisplay, R.string.display_title, 0,
                                fc.titleVisibility, fc_def == null ? null
                                : fc_def.titleVisibility));
                mPreferencesPageFolderLook
                        .add(mPGFolderLookTitleFontColor = new LLPreferenceColor(
                                this, ID_mPGFolderLookTitleFontColor, R.string.font_color, 0, fc.titleFontColor,
                                fc_def == null ? null : fc_def.titleFontColor, true));
                mPreferencesPageFolderLook
                        .add(mPGFolderLookTitleFontSize = new LLPreferenceSlider(
                                this, ID_mPGFolderLookTitleFontSize, R.string.font_size, 0, fc.titleFontSize,
                                fc_def == null ? null : fc_def.titleFontSize,
                                ValueType.FLOAT, 4, 100, 1, null));
                mPreferencesPageFolderLook.add(new LLPreferenceCategory(this, R.string.f_p));
                mPreferencesPageFolderLook.add(mPGFolderLookAlignH = new LLPreferenceList(this, ID_mPGFolderLookAlignH, R.string.b_alignh_t, R.array.b_alignh_e, fc.wAH, fc_def == null ? null : fc_def.wAH));
                mPreferencesPageFolderLook.add(mPGFolderLookAlignV = new LLPreferenceList(this, ID_mPGFolderLookAlignV, R.string.b_alignv_t, R.array.b_alignv_e, fc.wAV, fc_def == null ? null : fc_def.wAV));
                mPreferencesPageFolderLook.add(mPGFolderLookCustomX = new LLPreferenceSlider(this, ID_mPGFolderLookCustomX, R.string.f_l, R.string.f_ac, fc.wX, fc_def == null ? null : (float)fc_def.wX, ValueType.INT, 0, 2000, 1, null));
                mPreferencesPageFolderLook.add(mPGFolderLookCustomY = new LLPreferenceSlider(this, ID_mPGFolderLookCustomY, R.string.f_t, R.string.f_ac, fc.wY, fc_def == null ? null : (float)fc_def.wY, ValueType.INT, 0, 2000, 1, null));
                mPreferencesPageFolderLook.add(mPGFolderLookCustomW = new LLPreferenceSlider(this, ID_mPGFolderLookCustomW, R.string.wp_w_t, R.string.wp_auto, fc.wW, fc_def == null ? null : (float) fc_def.wW, ValueType.INT, 0, 2000, 1, null));
                mPreferencesPageFolderLook.add(mPGFolderLookCustomH = new LLPreferenceSlider(this, ID_mPGFolderLookCustomH, R.string.wp_h_t, R.string.wp_auto, fc.wH, fc_def == null ? null : (float) fc_def.wH, ValueType.INT, 0, 2000, 1, null));
                mPGFolderLookCustomX.setDisabled(fc.wAH != AlignH.CUSTOM);
                mPGFolderLookCustomY.setDisabled(fc.wAV!=AlignV.CUSTOM);
                if(mSystemConfig.expertMode) {
                    mPreferencesPageFolderLook.add(mPGFolderLookAutoFindOrigin = new LLPreferenceCheckBox(this, ID_mPGFolderLookAutoFindOrigin, R.string.f_afo, 0, fc.autoFindOrigin, fc_def == null ? null : fc_def.autoFindOrigin));
                }
                mPreferencesPageFolderLook.add(new LLPreferenceCategory(this, R.string.folder_anims));
                mPreferencesPageFolderLook.add(mPGFolderLookAnimOpen = new LLPreferenceList(this, ID_mPGFolderLookAnimOpen, R.string.anim_in, R.array.anim2_e, fc.animationIn, fc_def == null ? null : fc_def.animationIn));
                mPreferencesPageFolderLook.add(mPGFolderLookAnimClose = new LLPreferenceList(this, ID_mPGFolderLookAnimClose, R.string.anim_out, R.array.anim2_e, fc.animationOut, fc_def == null ? null : fc_def.animationOut));
                mPreferencesPageFolderLook.add(mPGFolderLookAnimFade = new LLPreferenceCheckBox(this, ID_mPGFolderLookAnimFade, R.string.f_af, 0, fc.animFade, fc_def == null ? null : fc_def.animFade));
                mPreferencesPageFolderLook.add(new LLPreferenceCategory(this, R.string.tab_box));
                mPreferencesPageFolderLook.add(mPGFolderBoxBox = new LLPreferenceBox(ID_mPGFolderBoxBox, fc.box, fc_def == null ? null : fc_def.box));
                mPreferencesPageFolderLook.add(mPGFolderBoxSize = new LLPreferenceSlider(this, ID_mPGFolderBoxSize, R.string.b_size, 0, 0, null, ValueType.INT, 0, 100, 1, null));
                mPreferencesPageFolderLook.add(mPGFolderBoxColor = new LLPreferenceColor(this, ID_mPGFolderBoxColor, R.string.b_color_normal, R.string.b_color_s, Color.BLACK, fc_def == null ? null : Color.BLACK, true));
                mPreferencesPageFolderLook.add(mPGFolderBoxNpNormal = new LLPreference(this, ID_ninePatch, R.string.np_r, R.string.np_d));

                if(!is_folder_page && !is_embedded_folder_page) {
                    mPreferencesPage.add(mPGSystemBars = new LLPreference(this, ID_mPGSystemBars, R.string.sb_t, R.string.sb_s));
                    mPreferencesPageSystemBars = new ArrayList<LLPreference>();

                    mPreferencesPageSystemBars.add(new LLPreferenceCategory(this, R.string.sb_c));
                    mPreferencesPageSystemBars.add(mPGSystemBarsHideStatusBar = new LLPreferenceCheckBox(this, ID_mPGSystemBarsHideStatusBar, R.string.hide_statusbar_t, 0, pc.statusBarHide, null));

                    // try to detect status bar capabilities on devices < API 19
                    boolean has_transparent_status_bar = false;
                    if (Build.VERSION.SDK_INT < 19) {
                        try {
                            Class<?> statusbarManager = Class.forName("android.app.StatusBarManager");
                            statusbarManager.getMethod("setStatusBarTransparent", boolean.class);
                            has_transparent_status_bar = true;
                        } catch (Exception e1) {
                        }

                        if (!has_transparent_status_bar) {
                            try {
                                Field field = View.class.getDeclaredField("SYSTEM_UI_FLAG_TRANSPARENT_BACKGROUND");
                                Class<?> t = field.getType();
                                if (t == int.class) {
                                    field.getInt(null);
                                    View.class.getMethod("setSystemUiVisibility", int.class);
                                    has_transparent_status_bar = true;
                                }
                            } catch (Exception e) {
                            }
                        }

                        if (has_transparent_status_bar) {
                            mPreferencesPageSystemBars.add(mPGSystemBarsTransparentStatusBar = new LLPreferenceCheckBox(this, ID_mPGSystemBarsTransparentStatusBar, R.string.trans_statusbar_t, R.string.trans_statusbar_s, pc.statusBarColor == 0, null));
                        }
                    }

                    if (Build.VERSION.SDK_INT >= 19) {
                        mPreferencesPageSystemBars.add(mPGSystemBarsStatusBarColor = new LLPreferenceColor(this, ID_mPGSystemBarsStatusBarColor, R.string.sbc_t, 0, pc.statusBarColor, null, true));
						if (Build.VERSION.SDK_INT >= 23) {
							mPreferencesPageSystemBars.add(mPGSystemBarsStatusBarLight = new LLPreferenceCheckBox(this, ID_mPGSystemBarsStatusBarLight, R.string.sbl_t, 0, pc.statusBarLight, null));
						}
                        mPreferencesPageSystemBars.add(mPGSystemBarsStatusBarOverlap = new LLPreferenceCheckBox(this, ID_mPGSystemBarsStatusBarOverlap, R.string.sbo_t, 0, pc.statusBarOverlap, null));

                        if (mSystemBarTintManager == null || (mSystemBarTintManager != null && mSystemBarTintManager.getConfig().hasNavigationBar())) {
                            mPreferencesPageSystemBars.add(new LLPreferenceCategory(this, R.string.nb_c));
                            mPreferencesPageSystemBars.add(mPGSystemBarsNavigationBarColor = new LLPreferenceColor(this, ID_mPGSystemBarsNavigationBarColor, R.string.nbc_t, 0, pc.navigationBarColor, null, true));
							if (Build.VERSION.SDK_INT >= 26) {
								mPreferencesPageSystemBars.add(mPGSystemBarsNavigationBarLight = new LLPreferenceCheckBox(this, ID_mPGSystemBarsNavigationBarLight, R.string.nbl_t, 0, pc.navigationBarLight, null));
							}
                            mPreferencesPageSystemBars.add(mPGSystemBarsNavBarOverlap = new LLPreferenceCheckBox(this, ID_mPGSystemBarsNavBarOverlap, R.string.nbo_t, 0, pc.navigationBarOverlap, null));
                        }
                    }

                    if (is_app_drawer) {
                        mPreferencesPageSystemBars.add(new LLPreferenceCategory(this, R.string.ad_ab_c_t));
                        mPreferencesPageSystemBars.add(mPGAppDrawerABTextColor = new LLPreferenceColor(this, ID_mPGAppDrawerABTextColor, R.string.ad_ab_tc, 0, pc.adActionBarTextColor, null, true));
                        mPreferencesPageSystemBars.add(mPGAppDrawerABBackground = new LLPreference(this, ID_mPGAppDrawerABBackground, R.string.ad_ab_b, 0));
                        mPreferencesPageSystemBars.add(mPGAppDrawerABHide = new LLPreferenceCheckBox(this, ID_mPGAppDrawerABHide, R.string.ad_ab_h, 0, pc.adHideActionBar, null));
                        mPreferencesPageSystemBars.add(mPGAppDrawerABDisplayOnScroll = new LLPreferenceCheckBox(this, ID_mPGAppDrawerABDisplayOnScroll, R.string.ad_ab_ss, 0, pc.adDisplayABOnScroll, null));
                        mPGAppDrawerABHide.setDependencies(new LLPreference[]{mPGAppDrawerABDisplayOnScroll}, null);
                    }
                }
            }
		//}

		mPreferencesPage.add(new LLPreferenceCategory(this, R.string.behaviour));

		mPreferencesPage.add(mPGLayout = new LLPreference(this, ID_mPGLayout, R.string.layout_t, R.string.layout_s));
		mPreferencesPageLayout = new ArrayList<LLPreference>();
		//mPreferencesPageLayout.add(new LLPreferenceCategory(this, R.string.grid_pl));
        mPreferencesPageLayout.add(mPGLayoutGridTogglePL = new LLPreferenceList(this, ID_mPGLayoutGridTogglePL, R.string.grid_pl_t, R.array.grid_pl_e, mGridMode, null));

        mPreferencesPageLayout.add(new LLPreferenceCategory(this, R.string.colums));
        mPreferencesPageLayout.add(mPGLayoutGridColumnMode = new LLPreferenceList(this, ID_mPGLayoutGridMode, R.string.mode_t, R.array.mode_e, pc.gridPColumnMode, null));
        mPreferencesPageLayout.add(mPGLayoutGridColumnNum = new LLPreferenceSlider(this, ID_mPGLayoutGridNum, R.string.number, R.string.col_num_s, pc.gridPColumnNum, null, ValueType.INT, 1, 50, 1, null));
        mPreferencesPageLayout.add(mPGLayoutGridColumnSize = new LLPreferenceSlider(this, ID_mPGLayoutGridSize, R.string.size, R.string.col_size_s, pc.gridPColumnSize, null, ValueType.INT, 0, 300, 1, null));

        mPreferencesPageLayout.add(new LLPreferenceCategory(this, R.string.rows));
        mPreferencesPageLayout.add(mPGLayoutGridRowMode = new LLPreferenceList(this, ID_mPGLayoutGridMode, R.string.mode_t, R.array.mode_e, pc.gridPRowMode, null));
        mPreferencesPageLayout.add(mPGLayoutGridRowNum = new LLPreferenceSlider(this, ID_mPGLayoutGridNum, R.string.number, R.string.row_num_s, pc.gridPRowNum, null, ValueType.INT, 1, 50, 1, null));
        mPreferencesPageLayout.add(mPGLayoutGridRowSize = new LLPreferenceSlider(this, ID_mPGLayoutGridSize, R.string.size, R.string.row_size_s, pc.gridPRowSize, null, ValueType.INT, 0, 300, 1, null));

        if(is_folder_page || is_embedded_folder_page) {
            mPreferencesPageLayout.add(new LLPreferenceCategory(this, R.string.container));
            mPreferencesPageLayout.add(mPGLayoutUseDesktopSize = new LLPreferenceCheckBox(this, ID_mPGLayoutUseDesktopSize, R.string.uds_t, R.string.uds_s, pc.useDesktopSize, null));
        }
        if(mSystemConfig.expertMode) {
        	mPreferencesPageLayout.add(new LLPreferenceCategory(this, R.string.placement));
	        if (!is_app_drawer) {
	            mPreferencesPageLayout.add(mPGLayoutFreeMode = new LLPreferenceCheckBox(this, ID_mPGLayoutFreeMode, R.string.free_mode_t, R.string.free_mode_s, !pc.newOnGrid, null));
	        }
	        mPreferencesPageLayout.add(mPGLayoutDualPosition = new LLPreferenceCheckBox(this, ID_mPGLayoutDualPosition, R.string.dual_pos_t, R.string.dual_pos_s, pc.allowDualPosition, null));
        }

		updateGridLayoutDisableState();

		mPreferencesPage.add(mPGZoomScroll = new LLPreference(this, ID_mPGZoomScroll,
				R.string.zoom_scroll_t, R.string.zoom_scroll_s));
		mPreferencesPageZoomScroll = new ArrayList<LLPreference>();
		mPreferencesPageZoomScroll.add(mPGZoomScrollDirection = new LLPreferenceList(this, ID_mPGZoomScrollDirection, R.string.scroll_dir_t, R.array.scroll_dir_e, pc.scrollingDirection, null));
		mPreferencesPageZoomScroll.add(mPGZoomScrollOver = new LLPreferenceList(this, ID_mPGZoomScrollOver, R.string.overscroll_t, R.array.overscroll_e, pc.overScrollMode, null));
		mPreferencesPageZoomScroll.add(mPGZoomScrollSnapToPages = new LLPreferenceCheckBox(this, ID_mPGZoomScrollSnapToPages, R.string.snap_to_pages_t, R.string.snap_to_pages_s, pc.snapToPages, null));
		mPreferencesPageZoomScroll.add(mPGZoomScrollWrapX = new LLPreferenceCheckBox(this, ID_mPGZoomScrollWrap, R.string.wrapx_t, R.string.wrap_s, pc.wrapX, null));
		mPreferencesPageZoomScroll.add(mPGZoomScrollWrapY = new LLPreferenceCheckBox(this, ID_mPGZoomScrollWrap, R.string.wrapy_t, R.string.wrap_s, pc.wrapY, null));

		//if (!is_folder_page) {
			if(mSystemConfig.expertMode) {
				mPreferencesPageZoomScroll.add(mPGZoomScrollFitDesktopToItems = new LLPreferenceCheckBox(this, ID_mPGZoomScrollFitDesktopToItems, R.string.fit_item_t, R.string.fit_item_s, pc.fitDesktopToItems, null));
				mPreferencesPageZoomScroll.add(mPGZoomScrollNoLimit = new LLPreferenceCheckBox(this, ID_mPGZoomScrollNoLimit, R.string.no_lim_t, R.string.no_lim_s, pc.noScrollLimit, null));
			}
		//}
		if(mSystemConfig.expertMode) mPreferencesPageZoomScroll.add(mPGZoomScrollDisableDiagonal = new LLPreferenceCheckBox(this, ID_mPGZoomScrollDisableDiagonal, R.string.no_diagonal_scrolling_t, R.string.no_diagonal_scrolling_s, pc.noDiagonalScrolling, null));
		if(!is_embedded_folder_page) mPreferencesPageZoomScroll.add(mPGZoomScrollEnablePinch = new LLPreferenceCheckBox(this, ID_mPGZoomScrollEnablePinch, R.string.pinch_zoom_t, 0, pc.pinchZoomEnable, null));

//		if(mSystemConfig.expertMode) {
            ActionsDescription actions = new ActionsDescription(this, is_app_drawer || isLaunchedFromAppDrawer() ? Action.FLAG_TYPE_APP_DRAWER : Action.FLAG_TYPE_DESKTOP, false);
            ActionsDescription scriptActions = new ActionsDescription(this, Action.FLAG_TYPE_SCRIPT, false);
			mPreferencesPage.add(mPGEvents = new LLPreference(this, ID_mGCEventsCategory, R.string.events_t, R.string.events_s));
			mPreferencesPageEvents = new ArrayList<LLPreference>();
			if (!is_app_drawer) {
                if (!is_folder_page && !is_embedded_folder_page) {
                    mPreferencesPageEvents.add(mPageEventHomeKey = new LLPreferenceEventAction(this, ID_NONE, R.string.ev_home, pc.homeKey, EventAction.UNSET(), actions));
                    mPreferencesPageEvents.add(mPageEventMenuKey = new LLPreferenceEventAction(this, ID_NONE, R.string.ev_menu, pc.menuKey, EventAction.UNSET(), actions));
                    mPreferencesPageEvents.add(mPageEventLongMenuKey = new LLPreferenceEventAction(this, ID_NONE, R.string.ev_menul, pc.longMenuKey, EventAction.UNSET(), actions));
                    mPreferencesPageEvents.add(mPageEventBackKey = new LLPreferenceEventAction(this, ID_NONE, R.string.ev_back, pc.backKey, EventAction.UNSET(), actions));
                    mPreferencesPageEvents.add(mPageEventLongBackKey = new LLPreferenceEventAction(this, ID_NONE, R.string.ev_backl, pc.longBackKey, EventAction.UNSET(), actions));
                    mPreferencesPageEvents.add(mPageEventSearchKey = new LLPreferenceEventAction(this, ID_NONE, R.string.ev_search, pc.searchKey, EventAction.UNSET(), actions));
                }
                mPreferencesPageEvents.add(mPageEventBgLongTap = new LLPreferenceEventAction(this, ID_NONE, R.string.ev_bg_ltap, pc.bgLongTap, EventAction.UNSET(), actions));
            }

            mPreferencesPageEvents.add(mPageEventBgTap = new LLPreferenceEventAction(this, ID_NONE, R.string.ev_bg_tap, pc.bgTap, EventAction.UNSET(), actions));
            mPreferencesPageEvents.add(mPageEventBgDoubleTap = new LLPreferenceEventAction(this, ID_NONE, R.string.ev_bg_dtap, pc.bgDoubleTap, EventAction.UNSET(), actions));
            if(!is_embedded_folder_page) {
                mPreferencesPageEvents.add(mPageEventSwipeLeft = new LLPreferenceEventAction(this, ID_NONE, R.string.ev_swipe_l, pc.swipeLeft, EventAction.UNSET(), actions));
                mPreferencesPageEvents.add(mPageEventSwipeRight = new LLPreferenceEventAction(this, ID_NONE, R.string.ev_swipe_r, pc.swipeRight, EventAction.UNSET(), actions));
                mPreferencesPageEvents.add(mPageEventSwipeUp = new LLPreferenceEventAction(this, ID_NONE, R.string.ev_swipe_u, pc.swipeUp, EventAction.UNSET(), actions));
                mPreferencesPageEvents.add(mPageEventSwipeDown = new LLPreferenceEventAction(this, ID_NONE, R.string.ev_swipe_d, pc.swipeDown, EventAction.UNSET(), actions));
            }
            if(!is_folder_page && !is_embedded_folder_page) {
                mPreferencesPageEvents.add(mPageEventSwipe2Left = new LLPreferenceEventAction(this, ID_NONE, R.string.ev_swipe2_l, pc.swipe2Left, EventAction.UNSET(), actions));
                mPreferencesPageEvents.add(mPageEventSwipe2Right = new LLPreferenceEventAction(this, ID_NONE, R.string.ev_swipe2_r, pc.swipe2Right, EventAction.UNSET(), actions));
                mPreferencesPageEvents.add(mPageEventSwipe2Up = new LLPreferenceEventAction(this, ID_NONE, R.string.ev_swipe2_u, pc.swipe2Up, EventAction.UNSET(), actions));
                mPreferencesPageEvents.add(mPageEventSwipe2Down = new LLPreferenceEventAction(this, ID_NONE, R.string.ev_swipe2_d, pc.swipe2Down, EventAction.UNSET(), actions));
                if(pc.screenOn.action!=GlobalConfig.UNSET) mPreferencesPageEvents.add(mPageEventScreenOn = new LLPreferenceEventAction(this, ID_NONE, R.string.ev_screen_on, pc.screenOn, EventAction.UNSET(), actions));
                if(pc.screenOff.action!=GlobalConfig.UNSET) mPreferencesPageEvents.add(mPageEventScreenOff = new LLPreferenceEventAction(this, ID_NONE, R.string.ev_screen_off, pc.screenOff, EventAction.UNSET(), actions));
                mPreferencesPageEvents.add(mPageEventOrientationPortrait = new LLPreferenceEventAction(this, ID_NONE, R.string.ev_op, pc.orientationPortrait, EventAction.UNSET(), actions));
                mPreferencesPageEvents.add(mPageEventOrientationLandscape = new LLPreferenceEventAction(this, ID_NONE, R.string.ev_ol, pc.orientationLandscape, EventAction.UNSET(), actions));
            }

            mPreferencesPageEvents.add(mPageEventPositionChanged = new LLPreferenceEventAction(this, ID_NONE, R.string.ev_pos, pc.posChanged, EventAction.UNSET(), actions));
			mPreferencesPageEvents.add(mPageEventLoad = new LLPreferenceEventAction(this, ID_NONE, R.string.ev_load, pc.load, EventAction.UNSET(), actions));
			mPreferencesPageEvents.add(mPageEventPaused = new LLPreferenceEventAction(this, ID_mPageEventPaused, R.string.ev_paused, pc.paused, EventAction.UNSET(), actions));
			mPreferencesPageEvents.add(mPageEventResumed = new LLPreferenceEventAction(this, ID_mPageEventResumed, R.string.ev_resumed, pc.resumed, EventAction.UNSET(), actions));
			mPreferencesPageEvents.add(mPageEventItemAdded = new LLPreferenceEventAction(this, ID_mPageEventItemAdded, R.string.ev_cia, pc.itemAdded, EventAction.UNSET(), actions));
			mPreferencesPageEvents.add(mPageEventItemRemoved = new LLPreferenceEventAction(this, ID_mPageEventItemRemoved, R.string.ev_cir, pc.itemRemoved, EventAction.UNSET(), actions));
			mPreferencesPageEvents.add(mPageEventMenu = new LLPreferenceEventAction(this, ID_mPageEventMenu, R.string.ev_m, pc.menu, EventAction.UNSET(), scriptActions));
//		}
		
		//if (!is_app_drawer) {
            if(!is_embedded_folder_page) {
                mPreferencesPage.add(mPGFolderFeel = new LLPreference(this, ID_mPGFolderFeel,
                        is_folder_page ? R.string.this_folder_feel_t
                                : R.string.folder_feel_t, R.string.folder_feel_s));
                mPreferencesPageFolderFeel = new ArrayList<LLPreference>();
                mPreferencesPageFolderFeel.add(mPGFolderFeelOutsideTapClose = new LLPreferenceCheckBox(this, ID_mPGFolderFeelOutsideTapClose, R.string.otc_t, 0, fc.outsideTapClose, fc_def == null ? null : fc_def.outsideTapClose));
                mPreferencesPageFolderFeel.add(mPGFolderFeelAutoClose = new LLPreferenceCheckBox(this, ID_mPGFolderFeelAutoClose, R.string.auto_close_t, R.string.auto_close_s, fc.autoClose, fc_def == null ? null : fc_def.autoClose));
                if(!is_folder_page) {
                    mPreferencesPageFolderFeel.add(mPGFolderFeelCloseOther = new LLPreferenceCheckBox(this, ID_mPGFolderFeelCloseOther, R.string.cof_t, R.string.cof_s, fc.closeOther, fc_def == null ? null : fc_def.closeOther));
                }
                if(mSystemConfig.expertMode) mPreferencesPageFolderFeel.add(mPGFolderFeelAnimGlitchFix = new LLPreferenceCheckBox(this, ID_mPGFolderFeelAnimGlitchFix, R.string.anim_glitch_fix_t, R.string.anim_glitch_fix_s, fc.animationGlitchFix, fc_def == null ? null : fc_def.animationGlitchFix));
            }
		//}

        if(is_app_drawer && mSystemConfig.expertMode) {
            int modes = pc.adDisplayedModes;
            mPreferencesPage.add(mPGADModes = new LLPreference(this, ID_mPGADCategories, R.string.adm_t, R.string.adm_s));
            mPreferencesPageADModes = new ArrayList<LLPreference>();
            mPreferencesPageADModes.add(mPGADModeCustom = new LLPreferenceCheckBox(this, 0, R.string.mi_mode_custom, 0, Utils.hasAppDrawerMode(modes, Utils.LAYOUT_MODE_CUSTOM), null));
            mPreferencesPageADModes.add(mPGADModeByName = new LLPreferenceCheckBox(this, 0, R.string.mi_mode_by_name, 0, Utils.hasAppDrawerMode(modes, Utils.LAYOUT_MODE_BY_NAME), null));
            mPreferencesPageADModes.add(mPGADModeFrequentlyUsed = new LLPreferenceCheckBox(this, 0, R.string.mi_mode_frequently_used, 0, Utils.hasAppDrawerMode(modes, Utils.LAYOUT_MODE_FREQUENTLY_USED), null));
            if(Build.VERSION.SDK_INT<21) {
                mPreferencesPageADModes.add(mPGADModeRecentApps = new LLPreferenceCheckBox(this, 0, R.string.mi_mode_recent_apps, 0, Utils.hasAppDrawerMode(modes, Utils.LAYOUT_MODE_RECENT_APPS), null));
            }
            mPreferencesPageADModes.add(mPGADModeRecentlyUpdated = new LLPreferenceCheckBox(this, 0, R.string.mi_mode_recently_updated, 0, Utils.hasAppDrawerMode(modes, Utils.LAYOUT_MODE_RECENTLY_UPDATED), null));
            mPreferencesPageADModes.add(mPGADModeRunning = new LLPreferenceCheckBox(this, 0, R.string.mi_mode_running, 0, Utils.hasAppDrawerMode(modes, Utils.LAYOUT_MODE_RUNNING), null));
        }

        if ((!is_folder_page && !is_embedded_folder_page) || mSystemConfig.expertMode) {
            mPreferencesPage.add(mPGMisc = new LLPreference(this, ID_mPGMisc, R.string.pg_misc_t, is_embedded_folder_page ? 0 : R.string.pg_misc_s));
        }
        mPreferencesPageMisc = new ArrayList<LLPreference>();
        if (!is_folder_page && !is_embedded_folder_page) {
			mPreferencesPageMisc.add(mPGMiscScreenOrientation = new LLPreferenceList(this, ID_mPGMiscScreenOrientation, R.string.screen_orient_t, R.array.screen_orient_e, pc.screenOrientation, null));
            if(is_app_drawer) {
                mPreferencesPageMisc.add(mPGMiscAutoExit = new LLPreferenceCheckBox(this, ID_mPGMiscAutoExit, R.string.auto_exit_t, R.string.auto_exit_s, pc.autoExit, null));
            }
		}
        if(mSystemConfig.expertMode) {
	        mPreferencesPageMisc.add(mPGMiscSwapItems = new LLPreferenceCheckBox(this, ID_mPGMiscSwapItems, R.string.swap_items_t, R.string.swap_items_s, pc.swapItems, null));
	        mPreferencesPageMisc.add(mPGMiscRearrangeItems = new LLPreferenceCheckBox(this, ID_mPGMiscRearrangeItems, R.string.rearrange_items_t, R.string.rearrange_items_s, pc.rearrangeItems, null));
        }
		if(mGlobalConfig.lwpScreen == mPage.id) {
			mPreferencesPageMisc.add(mPGMiscLWPStdEvents = new LLPreferenceCheckBox(this, ID_mPGMiscLWPStdEvents, R.string.lwp_ev_t, R.string.lwp_ev_s, pc.lwpStdEvents, null));
		}

		mPreferencesPage.add(new LLPreferenceCategory(this, R.string.manage));
		mPreferencesPage.add(mPGLoadStyle = new LLPreference(this, ID_mPGLoadStyle,
				R.string.load_style_t, R.string.load_style_s));
		mPreferencesPage.add(mPGSaveStyle = new LLPreference(this, ID_mPGSaveStyle,
				R.string.save_style_t, R.string.save_style_s));
		mPreferencesPage.add(mPGReset = new LLPreference(this, ID_mPGReset,
				R.string.reset_page_t, R.string.reset_page_s));

		mPreferenceScreenLevel = -1;
		pushPreferenceScreen(mPreferencesPage);

        if(is_app_drawer) {
            setPreferenceLockFlag(new LLPreference[]{mPGBackgroundSelectScreenWallpaper, mPGBackgroundScaleType, mPGZoomScrollWrapX, mPGZoomScrollWrapY, mPGFolderLook, mPGFolderFeel, mPGAppDrawerABBackground, mPGAppDrawerABDisplayOnScroll, mPGAppDrawerABHide, mPGEvents, mPGADModes, mPGAppDrawerABTextColor});
            setPreferenceLockFlag(mPreferencesPageFolderLook);
            setPreferenceLockFlag(mPreferencesPageEvents);
            setPreferenceLockFlag(mPreferencesPageFolderFeel);
            setPreferenceLockFlag(mPreferencesPageADModes);
        } else {
            setPreferenceLockFlag(new LLPreference[]{mPGBackgroundSelectScreenWallpaper, mPGZoomScrollWrapX, mPGZoomScrollWrapY});
        }
	}

	private void loadGlobalConfig() {
		mPreferencesGlobalConfig = new ArrayList<LLPreference>(4);
		mPreferencesGlobalConfig.add(mGCExpertMode = new LLPreferenceCheckBox(this, ID_mGCExpertMode, R.string.em_t, R.string.em_s, mSystemConfig.expertMode, null));
		mPreferencesGlobalConfig.add(mGCLanguageCategory = new LLPreference(
				this, ID_mGCLanguageCategory, R.string.language_t, R.string.language_s));
        if(Build.VERSION.SDK_INT>=13) {
            mPreferencesGlobalConfig.add(mGCAppStyle = new LLPreferenceList(this, ID_mGCAppStyle, R.string.gc_as_t, R.array.gc_as_e, mSystemConfig.appStyle, null));
        }
		mPreferencesGlobalConfig.add(mGCEventsCategory = new LLPreference(this, ID_mGCEventsCategory,
				R.string.events_t, R.string.events_s));
        if(Build.VERSION.SDK_INT>=8) {
            mPreferencesGlobalConfig.add(mGCLockScreenCategory = new LLPreference(this, ID_mGCLockScreenCategory, R.string.s_ls_t, R.string.s_ls_s));
            mPreferencesGlobalConfigLockScreen = new ArrayList<LLPreference>(3);
            mPreferencesGlobalConfigLockScreen.add(mGCLockScreenSelect = new LLPreference(this, ID_mGCLockScreenSelect, R.string.s_ls_s_t, R.string.s_ls_s_s));
            mPreferencesGlobalConfigLockScreen.add(mGCLockScreenLaunchUnlock = new LLPreferenceCheckBox(this, ID_mGCLockScreenLaunchUnlock, R.string.s_ls_lu_t, R.string.s_ls_lu_s, mGlobalConfig.launchUnlock, null));
            mPreferencesGlobalConfigLockScreen.add(mGCLockScreenDisableOverlay = new LLPreferenceCheckBox(this, ID_mGCLockScreenDisableOverlay, R.string.ldo_t, R.string.ldo_s, mGlobalConfig.lockDisableOverlay, null));
        }
        if(Build.VERSION.SDK_INT>=14) {
            mPreferencesGlobalConfig.add(mGCOverlayCategory = new LLPreference(this, ID_mGCOverlayCategory, R.string.ov_t, R.string.ov_s));
            mPreferencesGlobalConfigOverlay = new ArrayList<LLPreference>(3);
            if(Build.VERSION.SDK_INT >= 23) {
                mPreferencesGlobalConfigOverlay.add(mGCOverlayPermission = new LLPreference(this, ID_mGCOverlayPermission, R.string.ov_pt, R.string.ov_ps));
            }
            mPreferencesGlobalConfigOverlay.add(mGCOverlaySelect = new LLPreference(this, ID_mGCOverlaySelect, R.string.ov_st, R.string.ov_ss));
            mPreferencesGlobalConfigOverlay.add(mGCOverlayLaunchHide = new LLPreferenceCheckBox(this, ID_mGCOverlayLaunchHide, R.string.ov_lht, R.string.ov_lhs, mGlobalConfig.overlayLaunchHide, null));
            mPreferencesGlobalConfigOverlay.add(mGCOverlayDisplayHandles = new LLPreferenceCheckBox(this, ID_mGCOverlayDisplayHandles, R.string.ov_dht, R.string.ov_dhs, mGlobalConfig.overlayDisplayHandles, null));
            mPreferencesGlobalConfigOverlay.add(mGCOverlayShowHandleSize = new LLPreferenceSlider(this, ID_mGCOverlayHandleSize, R.string.ov_shs, 0, mGlobalConfig.overlayShowHandleSize, null, ValueType.FLOAT, 0, 1, 0.1f, "%"));
            mPreferencesGlobalConfigOverlay.add(mGCOverlayShowHandleWidth = new LLPreferenceSlider(this, ID_mGCOverlayHandleWidth, R.string.ov_shw, 0, mGlobalConfig.overlayShowHandleWidth, null, ValueType.FLOAT, 0, 0.2f, 0.01f, "%"));
            mPreferencesGlobalConfigOverlay.add(mGCOverlayShowHandlePosition = new LLPreferenceList(this, ID_mGCOverlayHandlePosition, R.string.ov_shp, R.array.ov_hp_e, mGlobalConfig.overlayShowHandlePosition, null));
            mPreferencesGlobalConfigOverlay.add(mGCOverlayHideHandleSize = new LLPreferenceSlider(this, ID_mGCOverlayHandleSize, R.string.ov_hhs, 0, mGlobalConfig.overlayHideHandleSize, null, ValueType.FLOAT, 0, 1, 0.1f, "%"));
            mPreferencesGlobalConfigOverlay.add(mGCOverlayHideHandleWidth = new LLPreferenceSlider(this, ID_mGCOverlayHandleWidth, R.string.ov_hhw, 0, mGlobalConfig.overlayHideHandleWidth, null, ValueType.FLOAT, 0, 0.2f, 0.01f, "%"));
            mPreferencesGlobalConfigOverlay.add(mGCOverlayHideHandlePosition = new LLPreferenceList(this, ID_mGCOverlayHandlePosition, R.string.ov_hhp, R.array.ov_hp_e, mGlobalConfig.overlayHideHandlePosition, null));
            if(Build.VERSION.SDK_INT >= 23) {
                boolean disabled = !WindowService.isPermissionAllowed(this);
                for (LLPreference p : mPreferencesGlobalConfigOverlay) {
                    if(p != mGCOverlayPermission) {
                        p.setDisabled(disabled);
                    }
                }
            }
        }
		mPreferencesGlobalConfig.add(mGCPageAnimation = new LLPreferenceList(this, ID_mGCPageAnimation, R.string.page_anim_t, R.array.page_anim_e, mGlobalConfig.pageAnimation, null));
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mPreferencesGlobalConfig.add(mGCHotwords = new LLPreferenceCheckBox(this, ID_mGCHotwords, R.string.hw_t, R.string.hw_s, mSystemConfig.hotwords, null));
        }
        if(mSystemConfig.expertMode) {
			mPreferencesGlobalConfig.add(mGCRunScripts = new LLPreferenceCheckBox(this, ID_mGCRunScripts, R.string.rs_t, R.string.rs_s, mGlobalConfig.runScripts && !LLApp.get().isFreeVersion(), null));
			mPreferencesGlobalConfig.add(mGCKeepInMemory = new LLPreferenceCheckBox(this, ID_mGCKeepInMemory, R.string.keep_in_memory_t, R.string.keep_in_memory_s, mSystemConfig.keepInMemory, null));
			mPreferencesGlobalConfig.add(mGCImagePoolSize = new LLPreferenceSlider(this, ID_mGCImagePoolSize, R.string.ips_t, R.string.ips_s, mSystemConfig.imagePoolSize, null, ValueType.FLOAT, 0, 1, 0.1f, "%"));

	        mPreferencesGlobalConfig.add(new LLPreferenceCategory(this, R.string.gc_edit));
	        mPreferencesGlobalConfig.add(mGCAutoEdit = new LLPreferenceCheckBox(this, ID_mGCAutoEdit, R.string.auto_edit_t, R.string.auto_edit_s, mSystemConfig.autoEdit, null));
//	        mPreferencesGlobalConfig.add(mGCHonourPinnedItemsEdit = new LLPreferenceCheckBox(this, ID_mGCHonourPinnedItemsEdit, R.string.gc_hpie_t, R.string.gc_hpie_s, mGlobalConfig.honourPinnedItemsEdit, null));
	        mPreferencesGlobalConfig.add(mGCAlwaysShowStopPoints = new LLPreferenceCheckBox(this, ID_mGCAlwaysShowStopPoints, R.string.assp_t, R.string.assp_s, mSystemConfig.alwaysShowStopPoints, null));
		}


		mPreferencesGlobalConfigLanguage = new ArrayList<LLPreference>(3);
		mPreferencesGlobalConfigLanguage.add(mGCLanguageSelect = new LLPreference(this, ID_NONE, R.string.lg_pick_t, R.string.lg_pick_s));
		mPreferencesGlobalConfigLanguage.add(mGCLanguageInstall = new LLPreference(this, ID_NONE, R.string.lg_install_t, R.string.lg_install_s));
		mPreferencesGlobalConfigLanguage.add(mGCLanguageContribute = new LLPreference(this, ID_NONE, R.string.lg_contrib_t, R.string.lg_contrib_s));

		ActionsDescription desktopActions = new ActionsDescription(this, Action.FLAG_TYPE_DESKTOP, false);
		ActionsDescription itemActions = new ActionsDescription(this, Action.FLAG_TYPE_DESKTOP, true);
		ActionsDescription scriptActions = new ActionsDescription(this, Action.FLAG_TYPE_SCRIPT, false);

		mPreferencesGlobalConfigEvents = new ArrayList<LLPreference>(16);
		mPreferencesGlobalConfigEvents.add(mGCEventHomeKey = new LLPreferenceEventAction(this, ID_NONE, R.string.ev_home, mGlobalConfig.homeKey, null, desktopActions));
		mPreferencesGlobalConfigEvents.add(mGCEventMenuKey = new LLPreferenceEventAction(this, ID_NONE, R.string.ev_menu, mGlobalConfig.menuKey, null, desktopActions));
		mPreferencesGlobalConfigEvents.add(mGCEventLongMenuKey = new LLPreferenceEventAction(this, ID_NONE, R.string.ev_menul, mGlobalConfig.longMenuKey, null, desktopActions));
		mPreferencesGlobalConfigEvents.add(mGCEventBackKey = new LLPreferenceEventAction(this, ID_NONE, R.string.ev_back, mGlobalConfig.backKey, null, desktopActions));
		mPreferencesGlobalConfigEvents.add(mGCEventLongBackKey = new LLPreferenceEventAction(this, ID_NONE, R.string.ev_backl, mGlobalConfig.longBackKey, null, desktopActions));
		mPreferencesGlobalConfigEvents.add(mGCEventSearchKey = new LLPreferenceEventAction(this, ID_NONE, R.string.ev_search, mGlobalConfig.searchKey, null, desktopActions));
		mPreferencesGlobalConfigEvents.add(mGCEventItemTap = new LLPreferenceEventAction(this, ID_NONE, R.string.ev_item_tap, mGlobalConfig.itemTap, null, itemActions));
		mPreferencesGlobalConfigEvents.add(mGCEventItemLongTap = new LLPreferenceEventAction(this, ID_NONE, R.string.ev_item_ltap, mGlobalConfig.itemLongTap, null, itemActions));
		mPreferencesGlobalConfigEvents.add(mGCEventBgTap = new LLPreferenceEventAction(this, ID_NONE, R.string.ev_bg_tap, mGlobalConfig.bgTap, null, desktopActions));
		mPreferencesGlobalConfigEvents.add(mGCEventBgDoubleTap = new LLPreferenceEventAction(this, ID_NONE, R.string.ev_bg_dtap, mGlobalConfig.bgDoubleTap, null, desktopActions));
		mPreferencesGlobalConfigEvents.add(mGCEventBgLongTap = new LLPreferenceEventAction(this, ID_NONE, R.string.ev_bg_ltap, mGlobalConfig.bgLongTap, null, desktopActions));
		mPreferencesGlobalConfigEvents.add(mGCEventSwipeLeft = new LLPreferenceEventAction(this, ID_NONE, R.string.ev_swipe_l, mGlobalConfig.swipeLeft, null, desktopActions));
		mPreferencesGlobalConfigEvents.add(mGCEventSwipeRight = new LLPreferenceEventAction(this, ID_NONE, R.string.ev_swipe_r, mGlobalConfig.swipeRight, null, desktopActions));
		mPreferencesGlobalConfigEvents.add(mGCEventSwipeUp = new LLPreferenceEventAction(this, ID_NONE, R.string.ev_swipe_u, mGlobalConfig.swipeUp, null, desktopActions));
		mPreferencesGlobalConfigEvents.add(mGCEventSwipeDown = new LLPreferenceEventAction(this, ID_NONE, R.string.ev_swipe_d, mGlobalConfig.swipeDown, null, desktopActions));
		mPreferencesGlobalConfigEvents.add(mGCEventSwipe2Left = new LLPreferenceEventAction(this, ID_NONE, R.string.ev_swipe2_l, mGlobalConfig.swipe2Left, null, desktopActions));
		mPreferencesGlobalConfigEvents.add(mGCEventSwipe2Right = new LLPreferenceEventAction(this, ID_NONE, R.string.ev_swipe2_r, mGlobalConfig.swipe2Right, null, desktopActions));
		mPreferencesGlobalConfigEvents.add(mGCEventSwipe2Up = new LLPreferenceEventAction(this, ID_NONE, R.string.ev_swipe2_u, mGlobalConfig.swipe2Up, null, desktopActions));
		mPreferencesGlobalConfigEvents.add(mGCEventSwipe2Down = new LLPreferenceEventAction(this, ID_NONE, R.string.ev_swipe2_d, mGlobalConfig.swipe2Down, null, desktopActions));
        mPreferencesGlobalConfigEvents.add(mGCEventScreenOn = new LLPreferenceEventAction(this, ID_NONE, R.string.ev_screen_on, mGlobalConfig.screenOn, null, desktopActions));
        mPreferencesGlobalConfigEvents.add(mGCEventScreenOff = new LLPreferenceEventAction(this, ID_NONE, R.string.ev_screen_off, mGlobalConfig.screenOff, null, desktopActions));
        mPreferencesGlobalConfigEvents.add(mGCEventOrientationPortrait = new LLPreferenceEventAction(this, ID_NONE, R.string.ev_op, mGlobalConfig.orientationPortrait, null, desktopActions));
        mPreferencesGlobalConfigEvents.add(mGCEventOrientationLandscape = new LLPreferenceEventAction(this, ID_NONE, R.string.ev_ol, mGlobalConfig.orientationLandscape, null, desktopActions));
        if(mSystemConfig.expertMode) {
            mPreferencesGlobalConfigEvents.add(mGCEventItemAdded = new LLPreferenceEventAction(this, ID_mPageEventItemAdded, R.string.ev_cia, mGlobalConfig.itemAdded, null, desktopActions));
            mPreferencesGlobalConfigEvents.add(mGCEventItemRemoved = new LLPreferenceEventAction(this, ID_mPageEventItemRemoved, R.string.ev_cir, mGlobalConfig.itemRemoved, null, desktopActions));
            mPreferencesGlobalConfigEvents.add(mGCEventMenu = new LLPreferenceEventAction(this, ID_mPageEventMenu, R.string.ev_m, mGlobalConfig.menu, null, scriptActions));
            mPreferencesGlobalConfigEvents.add(mGCEventStartup = new LLPreferenceEventAction(this, ID_mPageEventStartup, R.string.ev_st, mGlobalConfig.startup, null, desktopActions));
        }

		pushPreferenceScreen(mPreferencesGlobalConfig);

        setPreferenceLockFlag(new LLPreference[] {
                mGCRunScripts
        });
        if(LLApp.get().isFreeVersion() && getPackageManager().checkSignatures(getPackageName(), LLApp.LKP_PKG_NAME) != PackageManager.SIGNATURE_MATCH) {
            setPreferenceLockFlag(new LLPreference[] {
                    mGCLockScreenCategory, mGCLockScreenSelect, mGCLockScreenLaunchUnlock
            });
        }
	}

	private void savePage() {
        if(LLApp.get().isTrialVersionExpired()) {
            return;
        }

		mPage.setModified();

        final boolean is_folder_page = mOpenerItem!=null && mOpenerItem.getClass() == Folder.class;
		if (is_folder_page) {
            fc.loadAssociatedIcons(mOpenerPage.getIconDir(), mOpenerItem.getId());
			mOpenerItem.setFolderConfig(fc);
		}

		copyPreferencesToPageConfiguration();
		fc.box_s = fc.box.toString(fc_def == null ? new Box() : fc_def.box);

        if(mOpenerItem != null) {
            mOpenerItem.notifyChanged();
        }

        mPage.save();

        mPage.reload();

        mModified = false;
	}

	private void saveSystemAndGlobalConfig() {
        if(LLApp.get().isTrialVersionExpired()) {
            return;
        }

		copyPreferencesToSystemAndGlobalConfiguration();
        
		LLApp.get().notifySystemConfigChanged();
		LLApp.get().getAppEngine().notifyGlobalConfigChanged();
	}

	private void copyPreferencesToSystemAndGlobalConfiguration() {
        if(mGCAppStyle != null) mSystemConfig.appStyle = (SystemConfig.AppStyle) mGCAppStyle.getValueEnum();
		mGlobalConfig.pageAnimation = (PageAnimation) mGCPageAnimation.getValueEnum();
        if(mGCHotwords != null) mSystemConfig.hotwords = mGCHotwords.isChecked();
		if(mSystemConfig.expertMode) {
			if(!LLApp.get().isFreeVersion()) mGlobalConfig.runScripts = mGCRunScripts.isChecked();
			mSystemConfig.keepInMemory = mGCKeepInMemory.isChecked();
			mSystemConfig.imagePoolSize = mGCImagePoolSize.getValue();
			mSystemConfig.autoEdit = mGCAutoEdit.isChecked();
			mSystemConfig.alwaysShowStopPoints = mGCAlwaysShowStopPoints.isChecked();
		}
		mGlobalConfig.launchUnlock = mGCLockScreenLaunchUnlock.isChecked();
		mGlobalConfig.lockDisableOverlay = mGCLockScreenDisableOverlay.isChecked();

        if(Build.VERSION.SDK_INT>=14) {
            mGlobalConfig.overlayLaunchHide = mGCOverlayLaunchHide.isChecked();
            mGlobalConfig.overlayDisplayHandles = mGCOverlayDisplayHandles.isChecked();
            mGlobalConfig.overlayShowHandlePosition = (GlobalConfig.OverlayHandlePosition) mGCOverlayShowHandlePosition.getValueEnum();
            mGlobalConfig.overlayShowHandleSize = mGCOverlayShowHandleSize.getValue();
            mGlobalConfig.overlayShowHandleWidth = mGCOverlayShowHandleWidth.getValue();
            mGlobalConfig.overlayHideHandlePosition = (GlobalConfig.OverlayHandlePosition) mGCOverlayHideHandlePosition.getValueEnum();
            mGlobalConfig.overlayHideHandleSize = mGCOverlayHideHandleSize.getValue();
            mGlobalConfig.overlayHideHandleWidth = mGCOverlayHideHandleWidth.getValue();
        }

		mGlobalConfig.homeKey = mGCEventHomeKey.getValue();
		mGlobalConfig.menuKey = mGCEventMenuKey.getValue();
		mGlobalConfig.longMenuKey = mGCEventLongMenuKey.getValue();
        mGlobalConfig.backKey = mGCEventBackKey.getValue();
		mGlobalConfig.longBackKey = mGCEventLongBackKey.getValue();
		mGlobalConfig.searchKey = mGCEventSearchKey.getValue();
		mGlobalConfig.itemTap = mGCEventItemTap.getValue();
		mGlobalConfig.itemLongTap = mGCEventItemLongTap.getValue();
		mGlobalConfig.bgTap = mGCEventBgTap.getValue();
		mGlobalConfig.bgDoubleTap = mGCEventBgDoubleTap.getValue();
		mGlobalConfig.bgLongTap = mGCEventBgLongTap.getValue();
		mGlobalConfig.swipeLeft = mGCEventSwipeLeft.getValue();
		mGlobalConfig.swipeRight = mGCEventSwipeRight.getValue();
		mGlobalConfig.swipeUp = mGCEventSwipeUp.getValue();
		mGlobalConfig.swipeDown = mGCEventSwipeDown.getValue();
		mGlobalConfig.swipe2Left = mGCEventSwipe2Left.getValue();
		mGlobalConfig.swipe2Right = mGCEventSwipe2Right.getValue();
		mGlobalConfig.swipe2Up = mGCEventSwipe2Up.getValue();
		mGlobalConfig.swipe2Down = mGCEventSwipe2Down.getValue();
		mGlobalConfig.screenOn = mGCEventScreenOn.getValue();
		mGlobalConfig.screenOff = mGCEventScreenOff.getValue();
		mGlobalConfig.orientationPortrait = mGCEventOrientationPortrait.getValue();
		mGlobalConfig.orientationLandscape = mGCEventOrientationLandscape.getValue();
        if(mGCEventItemAdded != null) mGlobalConfig.itemAdded = mGCEventItemAdded.getValue();
        if(mGCEventItemRemoved != null) mGlobalConfig.itemRemoved = mGCEventItemRemoved.getValue();
        if(mGCEventMenu != null) mGlobalConfig.menu = mGCEventMenu.getValue();
        if(mGCEventStartup != null) mGlobalConfig.startup = mGCEventStartup.getValue();

		// this must be the last
		mSystemConfig.expertMode = mGCExpertMode.isChecked();
	}

	private void copyPreferencesToPageConfiguration() {
        final boolean is_folder = mOpenerItem!=null && mOpenerItem.getClass() == Folder.class;
        final boolean is_embedded_folder = mOpenerItem!=null && mOpenerItem.getClass() == EmbeddedFolder.class;
		final boolean is_app_drawer = mPage.id == Page.APP_DRAWER_PAGE;
		PageConfig pc = mPage.config;
		if (!is_folder && !is_embedded_folder) {
			pc.bgSystemWPScroll = mPGBackgroundSystemWallpaperScroll.isChecked();
			pc.bgSystemWPWidth = (int) mPGBackgroundSystemWallpaperWidth.getValue();
			pc.bgSystemWPHeight = (int) mPGBackgroundSystemWallpaperHeight.getValue();
			pc.bgColor = mPGBackgroundColor.getColor();
            if(mPGBackgroundScaleType != null) pc.bgScaleType = (PageConfig.ScaleType) mPGBackgroundScaleType.getValueEnum();
		}
		pc.gridLayoutModeHorizontalLineColor = mPGGridHColor.getColor();
		pc.gridLayoutModeHorizontalLineThickness = mPGGridHSize.getValue();
		pc.gridLayoutModeVerticalLineColor = mPGGridVColor.getColor();
		pc.gridLayoutModeVerticalLineThickness = mPGGridVSize.getValue();
		pc.gridAbove = mPGGridAbove.isChecked();
        if(is_folder || is_embedded_folder) {
            pc.useDesktopSize = mPGLayoutUseDesktopSize.isChecked();
        }
		if (!is_app_drawer) {
			if(mPGLayoutFreeMode!=null) pc.newOnGrid = !mPGLayoutFreeMode.isChecked();
        }
        if(mPGLayoutDualPosition!=null) pc.allowDualPosition = mPGLayoutDualPosition.isChecked();
        GridMode new_grid_mode = (GridMode)mPGLayoutGridTogglePL.getValueEnum();
        switch (new_grid_mode) {
            case PORTRAIT_AND_LANDSCAPE:
                pc.gridPL = true;
                copyPortraitGridPreferences(pc);
                copyLandscapeGridPreferences(pc);
                break;

            case PORTRAIT:
                pc.gridPL = false;
                copyPortraitGridPreferences(pc);
                break;

            case LANDSCAPE:
                pc.gridPL = false;
                copyLandscapeGridPreferences(pc);
                break;
        }

		pc.scrollingDirection = (ScrollingDirection) mPGZoomScrollDirection
				.getValueEnum();
//		pc.scrollingSpeed = mPGZoomScrollSpeed.getValue();
		pc.overScrollMode = (OverScrollMode) mPGZoomScrollOver.getValueEnum();
		pc.snapToPages = mPGZoomScrollSnapToPages.isChecked();
		pc.wrapX = mPGZoomScrollWrapX.isChecked();
		pc.wrapY = mPGZoomScrollWrapY.isChecked();
		if(mPGZoomScrollFitDesktopToItems!=null) pc.fitDesktopToItems = mPGZoomScrollFitDesktopToItems.isChecked();
		if(mPGZoomScrollNoLimit != null) pc.noScrollLimit = mPGZoomScrollNoLimit.isChecked();
		if(mPGZoomScrollDisableDiagonal != null) pc.noDiagonalScrolling = mPGZoomScrollDisableDiagonal.isChecked();
		if(mPGZoomScrollEnablePinch != null) pc.pinchZoomEnable = mPGZoomScrollEnablePinch.isChecked();
		if (!is_folder && !is_embedded_folder) {
			pc.screenOrientation = (ScreenOrientation) mPGMiscScreenOrientation.getValueEnum();
            if(mPGSystemBarsTransparentStatusBar != null) pc.statusBarColor = mPGSystemBarsTransparentStatusBar.isChecked() ? 0 : Color.BLACK;
            if(mPGSystemBarsStatusBarOverlap != null) pc.statusBarOverlap = mPGSystemBarsStatusBarOverlap.isChecked();
            if(mPGSystemBarsNavBarOverlap != null) pc.navigationBarOverlap = mPGSystemBarsNavBarOverlap.isChecked();
            if(mPGSystemBarsStatusBarColor != null) pc.statusBarColor = mPGSystemBarsStatusBarColor.getColor();
            if(mPGSystemBarsStatusBarLight != null) pc.statusBarLight = mPGSystemBarsStatusBarLight.isChecked();
            if(mPGSystemBarsNavigationBarColor != null) pc.navigationBarColor = mPGSystemBarsNavigationBarColor.getColor();
            if(mPGSystemBarsNavigationBarLight != null) pc.navigationBarLight = mPGSystemBarsNavigationBarLight.isChecked();
            pc.statusBarHide = mPGSystemBarsHideStatusBar.isChecked();
            if(is_app_drawer) {
                pc.autoExit = mPGMiscAutoExit.isChecked();
            }
			if(mPGMiscLWPStdEvents != null) pc.lwpStdEvents = mPGMiscLWPStdEvents.isChecked();
		}
        if(mPGMiscSwapItems != null) pc.swapItems = mPGMiscSwapItems.isChecked();
        if(mPGMiscRearrangeItems != null) pc.rearrangeItems = mPGMiscRearrangeItems.isChecked();

		//if (!is_app_drawer) {
            if(!is_embedded_folder) {
                fc.titleVisibility = mPGFolderLookTitleDisplay.isChecked();
                fc.titleFontColor = mPGFolderLookTitleFontColor.getColor();
                fc.titleFontSize = mPGFolderLookTitleFontSize.getValue();
                fc.wAH = (AlignH) mPGFolderLookAlignH.getValueEnum();
                fc.wAV = (AlignV) mPGFolderLookAlignV.getValueEnum();
                fc.wX = (int) mPGFolderLookCustomX.getValue();
                fc.wY = (int) mPGFolderLookCustomY.getValue();
                fc.wW = (int) mPGFolderLookCustomW.getValue();
                fc.wH = (int) mPGFolderLookCustomH.getValue();
                fc.animationIn = (FolderAnimation) mPGFolderLookAnimOpen.getValueEnum();
                fc.animationOut = (FolderAnimation) mPGFolderLookAnimClose.getValueEnum();
                fc.animFade = mPGFolderLookAnimFade.isChecked();
                fc.outsideTapClose = mPGFolderFeelOutsideTapClose.isChecked();
                fc.autoClose = mPGFolderFeelAutoClose.isChecked();
                if(!is_folder) {
                    fc.closeOther = mPGFolderFeelCloseOther.isChecked();
                }
                if(mPGFolderFeelAnimGlitchFix != null) fc.animationGlitchFix = mPGFolderFeelAnimGlitchFix.isChecked();
                if(mPGFolderLookAutoFindOrigin != null) fc.autoFindOrigin = mPGFolderLookAutoFindOrigin.isChecked();
            }
		//}

        if(mPGAppDrawerABTextColor != null) pc.adActionBarTextColor = mPGAppDrawerABTextColor.getColor();
        if(mPGAppDrawerABHide != null) pc.adHideActionBar = mPGAppDrawerABHide.isChecked();
        if(mPGAppDrawerABDisplayOnScroll != null) pc.adDisplayABOnScroll = mPGAppDrawerABDisplayOnScroll.isChecked();

        if(is_app_drawer && mSystemConfig.expertMode) {
            pc.adDisplayedModes = computeAppDrawerModes();
        }

        if(mPageEventHomeKey != null) pc.homeKey = mPageEventHomeKey.getValue();
        if(mPageEventMenuKey != null) pc.menuKey = mPageEventMenuKey.getValue();
        if(mPageEventLongMenuKey != null) pc.longMenuKey = mPageEventLongMenuKey.getValue();
        if(mPageEventBackKey != null) pc.backKey = mPageEventBackKey.getValue();
        if(mPageEventLongBackKey != null) pc.longBackKey = mPageEventLongBackKey.getValue();
        if(mPageEventSearchKey != null) pc.searchKey = mPageEventSearchKey.getValue();
    	if(mPageEventBgTap != null) pc.bgTap = mPageEventBgTap.getValue();
    	if(mPageEventBgDoubleTap != null) pc.bgDoubleTap = mPageEventBgDoubleTap.getValue();
    	if(mPageEventBgLongTap != null) pc.bgLongTap = mPageEventBgLongTap.getValue();
    	if(mPageEventSwipeLeft != null) pc.swipeLeft = mPageEventSwipeLeft.getValue();
    	if(mPageEventSwipeRight != null) pc.swipeRight = mPageEventSwipeRight.getValue();
    	if(mPageEventSwipeUp != null) pc.swipeUp = mPageEventSwipeUp.getValue();
    	if(mPageEventSwipeDown != null) pc.swipeDown = mPageEventSwipeDown.getValue();
		if(mPageEventSwipe2Left != null) pc.swipe2Left = mPageEventSwipe2Left.getValue();
		if(mPageEventSwipe2Right != null) pc.swipe2Right = mPageEventSwipe2Right.getValue();
		if(mPageEventSwipe2Up != null) pc.swipe2Up = mPageEventSwipe2Up.getValue();
		if(mPageEventSwipe2Down != null) pc.swipe2Down = mPageEventSwipe2Down.getValue();
		if(mPageEventScreenOn != null) pc.screenOn = mPageEventScreenOn.getValue();
		if(mPageEventScreenOff != null) pc.screenOff = mPageEventScreenOff.getValue();
		if(mPageEventOrientationPortrait != null) pc.orientationPortrait = mPageEventOrientationPortrait.getValue();
		if(mPageEventOrientationLandscape != null) pc.orientationLandscape = mPageEventOrientationLandscape.getValue();
		if(mPageEventPositionChanged != null) pc.posChanged = mPageEventPositionChanged.getValue();
        if(mPageEventLoad != null) pc.load = mPageEventLoad.getValue();
        if(mPageEventPaused != null) pc.paused = mPageEventPaused.getValue();
        if(mPageEventResumed != null) pc.resumed = mPageEventResumed.getValue();
        if(mPageEventItemAdded != null) pc.itemAdded = mPageEventItemAdded.getValue();
        if(mPageEventItemRemoved != null) pc.itemRemoved = mPageEventItemRemoved.getValue();
        if(mPageEventMenu != null) pc.menu = mPageEventMenu.getValue();
	}

    private int computeAppDrawerModes() {
        int modes = 0;
        if(mPGADModeCustom.isChecked()) modes |= (1<<Utils.LAYOUT_MODE_CUSTOM);
        if(mPGADModeByName.isChecked()) modes |= (1<<Utils.LAYOUT_MODE_BY_NAME);
        if(mPGADModeFrequentlyUsed.isChecked()) modes |= (1<<Utils.LAYOUT_MODE_FREQUENTLY_USED);
        if(mPGADModeRecentApps != null && mPGADModeRecentApps.isChecked()) modes |= (1<<Utils.LAYOUT_MODE_RECENT_APPS);
        if(mPGADModeRecentlyUpdated.isChecked()) modes |= (1<<Utils.LAYOUT_MODE_RECENTLY_UPDATED);
        if(mPGADModeRunning.isChecked()) modes |= (1<<Utils.LAYOUT_MODE_RUNNING);

        return modes;
    }

    private void copyPortraitGridPreferences(PageConfig pc) {
        pc.gridPColumnMode = (SizeMode) mPGLayoutGridColumnMode.getValueEnum();
        pc.gridPColumnNum = (int) mPGLayoutGridColumnNum.getValue();
        if (pc.gridPColumnNum < 1) pc.gridPColumnNum = 1;
        pc.gridPColumnSize = (int) mPGLayoutGridColumnSize.getValue();
        if (pc.gridPColumnSize < 1) pc.gridPColumnSize = 1;
        pc.gridPRowMode = (SizeMode) mPGLayoutGridRowMode.getValueEnum();
        pc.gridPRowNum = (int) mPGLayoutGridRowNum.getValue();
        if (pc.gridPRowNum < 1) pc.gridPRowNum = 1;
        pc.gridPRowSize = (int) mPGLayoutGridRowSize.getValue();
        if (pc.gridPRowSize < 1) pc.gridPRowSize = 1;
    }

    private void copyLandscapeGridPreferences(PageConfig pc) {
        pc.gridLColumnMode = (SizeMode) mPGLayoutGridColumnMode.getValueEnum();
        pc.gridLColumnNum = (int) mPGLayoutGridColumnNum.getValue();
        if (pc.gridLColumnNum < 1) pc.gridLColumnNum = 1;
        pc.gridLColumnSize = (int) mPGLayoutGridColumnSize.getValue();
        if (pc.gridLColumnSize < 1) pc.gridLColumnSize = 1;
        pc.gridLRowMode = (SizeMode) mPGLayoutGridRowMode.getValueEnum();
        pc.gridLRowNum = (int) mPGLayoutGridRowNum.getValue();
        if (pc.gridLRowNum < 1) pc.gridLRowNum = 1;
        pc.gridLRowSize = (int) mPGLayoutGridRowSize.getValue();
        if (pc.gridLRowSize < 1) pc.gridLRowSize = 1;
    }

	private void editEventActionPreference(LLPreferenceEventAction preference) {
		mEventActionEditPreference = preference;

		ActionsDescription actions = preference.getActions();

		EventActionSetup.startActivityForResult(this, mEventActionEditPreference.getValue(), actions.isForItem(), actions.getType(), false, REQUEST_EDIT_EVENT_ACTION);
	}

	@SuppressWarnings("deprecation")
	private void displayDialog(int d) {
		try {
			removeDialog(d);
		} catch (Exception e) {
		}
		showDialog(d);
	}

	private void applyBorderColors(int selection, LLPreferenceColor p, int shift, Box box, Box box_def) {
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

	private void pushPreferenceScreen(ArrayList<LLPreference> preferences) {
		if (mPreferenceScreenLevel >= 0) {
			mPreferenceScreens[mPreferenceScreenLevel].setVisibility(View.GONE);
		}
		mPreferenceScreenLevel++;
		LLPreferenceListView lv = mPreferenceScreens[mPreferenceScreenLevel];
		lv.setVisibility(View.VISIBLE);
		lv.setPreferences(preferences);
	}

	private void popPreferenceScreen() {
		mPreferenceScreens[mPreferenceScreenLevel].setVisibility(View.GONE);
		mPreferenceScreenLevel--;
		mPreferenceScreens[mPreferenceScreenLevel].setVisibility(View.VISIBLE);
	}

	private void updateGridLayoutDisableState() {
        SizeMode m;

        m = (SizeMode) mPGLayoutGridColumnMode.getValueEnum();
        mPGLayoutGridColumnMode.setDisabled(false);
        mPGLayoutGridColumnNum.setDisabled(m != SizeMode.NUM);
        mPGLayoutGridColumnSize.setDisabled(m != SizeMode.SIZE);

        m = (SizeMode) mPGLayoutGridRowMode.getValueEnum();
        mPGLayoutGridRowMode.setDisabled(false);
        mPGLayoutGridRowNum.setDisabled(m != SizeMode.NUM);
        mPGLayoutGridRowSize.setDisabled(m != SizeMode.SIZE);
	}



	private void loadStyle(String path) {
		int page = mPage.id;

		File from = new File(path);
		JSONObject json = FileUtils.readJSONObjectFromFile(from);
		if (json == null) {
			json = new JSONObject();
		}

		if (page == Page.APP_DRAWER_PAGE) {
			// force grid mode for app app_drawer
			try {
                json.put("newOnGrid", true);
			} catch (JSONException e) {
				// pass
			}
		} else {
			PageConfig theme_page_config = new PageConfig();
			theme_page_config.loadFieldsFromJSONObject(json, theme_page_config);

            if(mOpenerItem != null) {
                File icon_dir = mOpenerPage.getAndCreateIconDir();
                File np_to = Box.getBoxBackgroundFolder(icon_dir, mOpenerItem.getId());
                if(json.has(STYLE_OPENER_FOLDER_CONFIG)) {
                    JSONObject fc_json = json.optJSONObject(STYLE_OPENER_FOLDER_CONFIG);
                    fc.loadFieldsFromJSONObject(fc_json, mOpenerPage.config.defaultFolderConfig);
                    File np_from = new File(path+"np");
                    if(np_from.exists()) {
                        Utils.copyFileSafe(null, np_from, np_to);
                    } else {
                        np_to.delete();
                    }
                } else {
                    fc = new FolderConfig();
                    fc.copyFrom(mOpenerPage.config.defaultFolderConfig);
                    np_to.delete();
                }
                mOpenerItem.setFolderConfig(fc);
                mModified = true;
            }
        }

        try {
            FileUtils.saveStringToFile(json.toString(), mPage.getPageConfigFile());

            // copy effect icons if needed
            File theme_dir = from.getParentFile();
            String theme_name = from.getName();
            FileUtils.copyIcons(null, theme_dir, theme_name, mPage.getAndCreateIconDir(), "");

            mPage.reload();

            loadPage(mPagePath);
            Toast.makeText(this, R.string.style_loaded, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            // pass
        }
	}

	private void saveStyle(String path) {
		copyPreferencesToPageConfiguration();

		PageConfig default_page_config = new PageConfig();
		try {
			String name = null;
			File to = new File(path);
			if (to.exists()) {
				// retrieve and reuse the label of the theme we will overwrite
				JSONObject json = FileUtils.readJSONObjectFromFile(to);
				name = json.optString(JsonFields.LABEL, null);
			}

			JSONObject o = JsonLoader.toJSONObject(mPage.config, default_page_config);
			if (name != null) {
				o.put(JsonFields.LABEL, name);
			}
			o.put("defaultItemConfig", JsonLoader.toJSONObject(mPage.config.defaultItemConfig, new ItemConfig()));
			o.put("defaultShortcutConfig", JsonLoader.toJSONObject(mPage.config.defaultShortcutConfig, new ShortcutConfig()));
			o.put("defaultFolderConfig", JsonLoader.toJSONObject(fc, new FolderConfig()));
            if(mOpenerItem != null) {
                o.put(STYLE_OPENER_FOLDER_CONFIG, JsonLoader.toJSONObject(mOpenerItem.getFolderConfig(), mOpenerPage.config.defaultFolderConfig));
                File icon_dir = mOpenerPage.getIconDir();
                File np_from = Box.getBoxBackgroundFolder(icon_dir, mOpenerItem.getId());
                if(np_from.exists()) {
                    File np_to = new File(path+"np");
                    Utils.copyFileSafe(null, np_from, np_to);
                }
            }
			FileUtils.saveStringToFile(o.toString(), to);

			// copy effect icons if needed
			File theme_dir = to.getParentFile();
			String theme_name = to.getName();
			FileUtils.copyIcons(null, mPage.getIconDir(), "", theme_dir, theme_name);

			Toast.makeText(this, R.string.style_saved, Toast.LENGTH_SHORT)
					.show();
		} catch (IOException e) {
			// pass
		} catch (JSONException e) {
			// pass
		}
	}
	
	private void updateWallpaper() {
		PageConfig c = mPage.config;
		int p = mPage.id;
        File wp_file = mPage.getWallpaperFile();
        if(Color.alpha(c.bgColor)==0 && !wp_file.exists()) {
        	mWallpaperView.setVisibility(View.GONE);
        } else {
        	mWallpaperView.setVisibility(View.VISIBLE);
        	mWallpaperView.configure(p, wp_file, c.bgColor, c.bgScaleType);
        }
	}

    private void applyIconPack() {
        Intent i=new Intent(Intent.ACTION_PICK_ACTIVITY);
        i.putExtra(Intent.EXTRA_TITLE, getString(R.string.apply_icon_pack_t));
        Intent filter=new Intent("org.adw.launcher.icons.ACTION_PICK_ICON");
        i.putExtra(Intent.EXTRA_INTENT, filter);
        startActivityForResult(i, REQUEST_PICK_ICON_PACK_FOR_APPLY);
    }

    private void doApplyIconPack(final String package_name) {
        savePage();

        displayDialog(DIALOG_PROGRESS);

        mPage.config.iconPack = package_name;
        mModified = true;

        IconPack.applyIconPackAsync(this, package_name, mPage, Item.NO_ID, new IconPack.IconPackListener() {
            @Override
            public void onPackApplied(boolean success) {
                if(!success) {
                    Toast.makeText(Customize.this, R.string.icon_pack_no_appfilter, Toast.LENGTH_SHORT).show();
                }
                try { dismissDialog(DIALOG_PROGRESS); } catch(Exception e) {}
                finish();
            }
        });
    }

    private void revertCustomIcons() {
        mPage.config.iconPack = null;
        savePage();
        IconPack.removeCustomIcons(mPage);
        finish();
    }



	/**************************************** GLOBAL CONFIG *************************************/
	private ArrayList<LLPreference> mPreferencesGlobalConfig;
	private ArrayList<LLPreference> mPreferencesGlobalConfigLanguage;
	private ArrayList<LLPreference> mPreferencesGlobalConfigEvents;
	private ArrayList<LLPreference> mPreferencesGlobalConfigLockScreen;
	private ArrayList<LLPreference> mPreferencesGlobalConfigOverlay;

	private LLPreferenceCheckBox mGCExpertMode;
	
	private LLPreference mGCLanguageCategory;
	private LLPreference mGCEventsCategory;
	private LLPreference mGCLockScreenCategory;
	private LLPreference mGCOverlayCategory;
	private LLPreferenceCheckBox mGCHotwords;
	private LLPreferenceCheckBox mGCRunScripts;

	private LLPreferenceCheckBox mGCAutoEdit;
//    private LLPreferenceCheckBox mGCHonourPinnedItemsEdit;
	private LLPreferenceCheckBox mGCAlwaysShowStopPoints;
	private LLPreferenceList mGCPageAnimation;
	private LLPreferenceList mGCAppStyle;
	private LLPreferenceCheckBox mGCKeepInMemory;
	private LLPreferenceSlider mGCImagePoolSize;

	private LLPreference mGCLanguageSelect;
	private LLPreference mGCLanguageInstall;
	private LLPreference mGCLanguageContribute;

	private LLPreferenceEventAction mGCEventHomeKey;
	private LLPreferenceEventAction mGCEventMenuKey;
	private LLPreferenceEventAction mGCEventLongMenuKey;
	private LLPreferenceEventAction mGCEventBackKey;
	private LLPreferenceEventAction mGCEventLongBackKey;
	private LLPreferenceEventAction mGCEventSearchKey;
	private LLPreferenceEventAction mGCEventItemTap;
	private LLPreferenceEventAction mGCEventItemLongTap;
	private LLPreferenceEventAction mGCEventBgTap;
	private LLPreferenceEventAction mGCEventBgDoubleTap;
	private LLPreferenceEventAction mGCEventBgLongTap;
	private LLPreferenceEventAction mGCEventSwipeLeft;
	private LLPreferenceEventAction mGCEventSwipeRight;
	private LLPreferenceEventAction mGCEventSwipeUp;
	private LLPreferenceEventAction mGCEventSwipeDown;
	private LLPreferenceEventAction mGCEventSwipe2Left;
	private LLPreferenceEventAction mGCEventSwipe2Right;
	private LLPreferenceEventAction mGCEventSwipe2Up;
	private LLPreferenceEventAction mGCEventSwipe2Down;
	private LLPreferenceEventAction mGCEventScreenOn;
	private LLPreferenceEventAction mGCEventScreenOff;
	private LLPreferenceEventAction mGCEventOrientationPortrait;
	private LLPreferenceEventAction mGCEventOrientationLandscape;
	private LLPreferenceEventAction mGCEventItemAdded;
	private LLPreferenceEventAction mGCEventItemRemoved;
	private LLPreferenceEventAction mGCEventMenu;
	private LLPreferenceEventAction mGCEventStartup;

	private LLPreferenceEventAction mEventActionEditPreference;

    private LLPreference mGCLockScreenSelect;
    private LLPreferenceCheckBox mGCLockScreenLaunchUnlock;
    private LLPreferenceCheckBox mGCLockScreenDisableOverlay;

    private LLPreference mGCOverlayPermission;
    private LLPreference mGCOverlaySelect;
    private LLPreferenceCheckBox mGCOverlayLaunchHide;
    private LLPreferenceCheckBox mGCOverlayDisplayHandles;
    private LLPreferenceSlider mGCOverlayShowHandleSize;
    private LLPreferenceSlider mGCOverlayShowHandleWidth;
    private LLPreferenceList mGCOverlayShowHandlePosition;
    private LLPreferenceSlider mGCOverlayHideHandleSize;
    private LLPreferenceSlider mGCOverlayHideHandleWidth;
    private LLPreferenceList mGCOverlayHideHandlePosition;

	/**************************************** PAGE *************************************/
	private ArrayList<LLPreference> mPreferencesPage;
    private ArrayList<LLPreference> mPreferencesPageIcons;
	private ArrayList<LLPreference> mPreferencesPageBackground;
	private ArrayList<LLPreference> mPreferencesPageGrid;
	private ArrayList<LLPreference> mPreferencesPageFolderLook;
	private ArrayList<LLPreference> mPreferencesPageSystemBars;
	private ArrayList<LLPreference> mPreferencesPageLayout;
	private ArrayList<LLPreference> mPreferencesPageZoomScroll;
	private ArrayList<LLPreference> mPreferencesPageEvents;
	private ArrayList<LLPreference> mPreferencesPageFolderFeel;
	private ArrayList<LLPreference> mPreferencesPageADModes;
	private ArrayList<LLPreference> mPreferencesPageMisc;

    private LLPreference mPGIcons;
    private LLPreference mPGApplyIconPack;
    private LLPreference mPGRevertCustomIcon;

	private LLPreference mPGBackground;
	
	private LLPreference mPGBackgroundSelectSystemWallpaper;
	private LLPreferenceCheckBox mPGBackgroundSystemWallpaperScroll;
	private LLPreferenceSlider mPGBackgroundSystemWallpaperWidth;
	private LLPreferenceSlider mPGBackgroundSystemWallpaperHeight;
	private LLPreference mPGBackgroundSelectScreenWallpaper;
	private LLPreferenceColor mPGBackgroundColor;
	private LLPreferenceList mPGBackgroundScaleType;

	private LLPreference mPGGrid;
	private LLPreferenceColor mPGGridHColor;
	private LLPreferenceSlider mPGGridHSize;
	private LLPreferenceColor mPGGridVColor;
	private LLPreferenceSlider mPGGridVSize;
	private LLPreferenceCheckBox mPGGridAbove;

	private LLPreference mPGFolderLook;
	private LLPreferenceCheckBox mPGFolderLookTitleDisplay;
	private LLPreferenceColor mPGFolderLookTitleFontColor;
	private LLPreferenceSlider mPGFolderLookTitleFontSize;
	private LLPreferenceList mPGFolderLookAlignH;
    private LLPreferenceSlider mPGFolderLookCustomX;
    private LLPreferenceList mPGFolderLookAlignV;
    private LLPreferenceSlider mPGFolderLookCustomY;
    private LLPreferenceSlider mPGFolderLookCustomW;
    private LLPreferenceSlider mPGFolderLookCustomH;
	private LLPreferenceList mPGFolderLookAnimOpen;
	private LLPreferenceList mPGFolderLookAnimClose;
	private LLPreferenceCheckBox mPGFolderLookAnimFade;
	private LLPreferenceBox mPGFolderBoxBox;
	private LLPreferenceSlider mPGFolderBoxSize;
	private LLPreferenceColor mPGFolderBoxColor;
    private LLPreference mPGFolderBoxNpNormal;
    private LLPreferenceCheckBox mPGFolderLookAutoFindOrigin;

    private LLPreference mPGSystemBars;
    private LLPreferenceCheckBox mPGSystemBarsHideStatusBar;
    private LLPreferenceCheckBox mPGSystemBarsTransparentStatusBar;
    private LLPreferenceCheckBox mPGSystemBarsStatusBarOverlap;
    private LLPreferenceCheckBox mPGSystemBarsNavBarOverlap;
    private LLPreferenceColor mPGSystemBarsStatusBarColor;
    private LLPreferenceCheckBox mPGSystemBarsStatusBarLight;
    private LLPreferenceColor mPGSystemBarsNavigationBarColor;
    private LLPreferenceCheckBox mPGSystemBarsNavigationBarLight;

    private LLPreferenceColor mPGAppDrawerABTextColor;
    private LLPreference mPGAppDrawerABBackground;
    private LLPreferenceCheckBox mPGAppDrawerABHide;
    private LLPreferenceCheckBox mPGAppDrawerABDisplayOnScroll;


	private LLPreference mPGLayout;
    private LLPreferenceCheckBox mPGLayoutUseDesktopSize;
    private LLPreferenceCheckBox mPGLayoutFreeMode;
	private LLPreferenceCheckBox mPGLayoutDualPosition;

	private LLPreferenceList mPGLayoutGridTogglePL;
	private LLPreferenceList mPGLayoutGridColumnMode;
	private LLPreferenceSlider mPGLayoutGridColumnNum;
	private LLPreferenceSlider mPGLayoutGridColumnSize;
	private LLPreferenceList mPGLayoutGridRowMode;
	private LLPreferenceSlider mPGLayoutGridRowNum;
	private LLPreferenceSlider mPGLayoutGridRowSize;

	private LLPreference mPGZoomScroll;
	private LLPreferenceList mPGZoomScrollDirection;
//	private LLPreferenceSlider mPGZoomScrollSpeed;
	private LLPreferenceList mPGZoomScrollOver;
	private LLPreferenceCheckBox mPGZoomScrollSnapToPages;
	private LLPreferenceCheckBox mPGZoomScrollWrapX;
	private LLPreferenceCheckBox mPGZoomScrollWrapY;
	private LLPreferenceCheckBox mPGZoomScrollFitDesktopToItems;
	private LLPreferenceCheckBox mPGZoomScrollNoLimit;
	private LLPreferenceCheckBox mPGZoomScrollDisableDiagonal;
	private LLPreferenceCheckBox mPGZoomScrollEnablePinch;

	private LLPreference mPGEvents;
	private LLPreferenceEventAction mPageEventHomeKey;
	private LLPreferenceEventAction mPageEventMenuKey;
	private LLPreferenceEventAction mPageEventLongMenuKey;
	private LLPreferenceEventAction mPageEventBackKey;
	private LLPreferenceEventAction mPageEventLongBackKey;
	private LLPreferenceEventAction mPageEventSearchKey;
	private LLPreferenceEventAction mPageEventBgTap;
	private LLPreferenceEventAction mPageEventBgDoubleTap;
	private LLPreferenceEventAction mPageEventBgLongTap;
	private LLPreferenceEventAction mPageEventSwipeLeft;
	private LLPreferenceEventAction mPageEventSwipeRight;
	private LLPreferenceEventAction mPageEventSwipeUp;
	private LLPreferenceEventAction mPageEventSwipeDown;
	private LLPreferenceEventAction mPageEventSwipe2Left;
	private LLPreferenceEventAction mPageEventSwipe2Right;
	private LLPreferenceEventAction mPageEventSwipe2Up;
	private LLPreferenceEventAction mPageEventSwipe2Down;
	private LLPreferenceEventAction mPageEventScreenOn;
	private LLPreferenceEventAction mPageEventScreenOff;
	private LLPreferenceEventAction mPageEventOrientationPortrait;
	private LLPreferenceEventAction mPageEventOrientationLandscape;
	private LLPreferenceEventAction mPageEventPositionChanged;
	private LLPreferenceEventAction mPageEventLoad;
	private LLPreferenceEventAction mPageEventPaused;
	private LLPreferenceEventAction mPageEventResumed;
    private LLPreferenceEventAction mPageEventItemAdded;
	private LLPreferenceEventAction mPageEventItemRemoved;
	private LLPreferenceEventAction mPageEventMenu;

	private LLPreference mPGFolderFeel;
	private LLPreferenceCheckBox mPGFolderFeelOutsideTapClose;
	private LLPreferenceCheckBox mPGFolderFeelAutoClose;
	private LLPreferenceCheckBox mPGFolderFeelCloseOther;
	private LLPreferenceCheckBox mPGFolderFeelAnimGlitchFix;

    private LLPreference mPGADModes;
    private LLPreferenceCheckBox mPGADModeCustom;
    private LLPreferenceCheckBox mPGADModeByName;
    private LLPreferenceCheckBox mPGADModeFrequentlyUsed;
    private LLPreferenceCheckBox mPGADModeRecentApps;
    private LLPreferenceCheckBox mPGADModeRecentlyUpdated;
    private LLPreferenceCheckBox mPGADModeRunning;

	private LLPreference mPGMisc;
	private LLPreferenceList mPGMiscScreenOrientation;
	private LLPreferenceCheckBox mPGMiscSwapItems;
	private LLPreferenceCheckBox mPGMiscRearrangeItems;
    private LLPreferenceCheckBox mPGMiscAutoExit;
    private LLPreferenceCheckBox mPGMiscLWPStdEvents;

	private LLPreference mPGLoadStyle;
	private LLPreference mPGSaveStyle;
	private LLPreference mPGReset;

	private static final int ID_NONE = 0;

	private static final int ID_mPGBackground = 51;
	private static final int ID_mPGBackgroundSelectSystemWallpaper = 52;
	private static final int ID_mPGBackgroundSystemWallpaperScroll = 53;
	private static final int ID_mPGBackgroundSystemWallpaperWidth = 54;
	private static final int ID_mPGBackgroundSystemWallpaperHeight = 55;
	private static final int ID_mPGBackgroundSelectScreenWallpaper = 56;
	private static final int ID_mPGBackgroundColor = 57;
	private static final int ID_mPGGrid = 58;
	private static final int ID_mPGGridHColor = 59;
	private static final int ID_mPGGridHSize = 60;
	private static final int ID_mPGGridVColor = 61;
	private static final int ID_mPGGridVSize = 62;
	private static final int ID_mPGGridAbove = 63;
	private static final int ID_mPGFolderLook = 64;
	private static final int ID_mPGFolderLookTitleDisplay = 65;
	private static final int ID_mPGFolderLookTitleFontColor = 66;
	private static final int ID_mPGFolderLookTitleFontSize = 67;
	private static final int ID_mPGFolderLookAnimOpen = 68;
	private static final int ID_mPGFolderLookAnimClose = 69;
	private static final int ID_mPGFolderBoxBox = 70;
	private static final int ID_mPGFolderBoxSize = 71;
	private static final int ID_mPGFolderBoxColor = 72;
	private static final int ID_mPGLayout = 73;
	private static final int ID_mPGLayoutFreeMode = 74;
	private static final int ID_mPGLayoutDualPosition = 75;
//	private static final int ID_mPGLayoutFreeModeSnap = 76;
	private static final int ID_mPGLayoutGridMode = 77;
	private static final int ID_mPGLayoutGridNum = 78;
	private static final int ID_mPGLayoutGridSize = 79;
	private static final int ID_mPGZoomScroll = 83;
	private static final int ID_mPGZoomScrollDirection = 84;
	private static final int ID_mPGZoomScrollOver = 86;
	private static final int ID_mPGZoomScrollSnapToPages = 87;
//	private static final int ID_mPGZoomScrollAutoStop = 88;
	private static final int ID_mPGZoomScrollFitDesktopToItems = 89;
	private static final int ID_mPGZoomScrollDisableDiagonal = 90;
	private static final int ID_mPGZoomScrollEnablePinch = 91;
	private static final int ID_mPGFolderFeel = 92;
	private static final int ID_mPGFolderFeelOutsideTapClose = 515;
	private static final int ID_mPGFolderFeelAutoClose = 93;
	private static final int ID_mPGFolderFeelAnimGlitchFix = 94;
	private static final int ID_mPGMisc = 95;
	private static final int ID_mPGMiscScreenOrientation = 96;
	private static final int ID_mPGSystemBarsHideStatusBar = 97;
	private static final int ID_mPGLoadStyle = 98;
    private static final int ID_mPGSaveStyle = 99;
    private static final int ID_mPGReset = 100;
    private static final int ID_mGCKeepInMemory = 102;
    private static final int ID_mGCAutoEdit = 103;
    private static final int ID_mGCPageAnimation = 104;
    private static final int ID_mGCLanguageCategory = 105;
    private static final int ID_mGCEventsCategory = 106;
    private static final int ID_mPGSystemBarsTransparentStatusBar = 109;
    private static final int ID_mPGMiscSwapItems = 110;
    private static final int ID_mPGMiscRearrangeItems = 111;
    private static final int ID_mPGLayoutGridTogglePL = 112;
//    private static final int ID_mGCHonourPinnedItemsEdit = 113;
    private static final int ID_mGCAlwaysShowStopPoints = 123;
    private static final int ID_mPGFolderLookAlignH = 124;
    private static final int ID_mPGFolderLookAlignV = 125;
    private static final int ID_mPGFolderLookCustomX = 126;
    private static final int ID_mPGFolderLookCustomY = 127;
    private static final int ID_mPGFolderLookCustomW = 128;
    private static final int ID_mPGFolderLookCustomH = 129;
    private static final int ID_mPGFolderLookAnimFade= 130;
    private static final int ID_ninePatch= 135;
    private static final int ID_mPGFolderFeelCloseOther = 136;
//    private static final int ID_mGCSingleFingerDragEdit = 137;
//    private static final int ID_mPGSystemBarsTransparentNavigationBar = 138;
    private static final int ID_mPGLayoutUseDesktopSize = 139;
    private static final int ID_mGCLockScreenCategory = 140;
    private static final int ID_mGCLockScreenSelect = 141;
    private static final int ID_mGCLockScreenLaunchUnlock = 145;
    private static final int ID_mPGZoomScrollNoLimit = 146;
    private static final int ID_mGCExpertMode = 148;
    private static final int ID_mGCRunScripts = 151;
//    private static final int ID_mPGMiscTransparentStatusBarOverlap = 152;
    private static final int ID_mPageEventPaused = 153;
    private static final int ID_mPageEventResumed = 154;
//    private static final int ID_mPGAppDrawerAB = 155;
    private static final int ID_mPGAppDrawerABBackground = 156;
    private static final int ID_mPGAppDrawerABHide = 157;
    private static final int ID_mGCHotwords = 158;
    private static final int ID_mPGAppDrawerABDisplayOnScroll = 159;
    private static final int ID_mPGZoomScrollWrap = 161;
    private static final int ID_mPGSystemBarsStatusBarOverlap = 163;
    private static final int ID_mPGSystemBarsNavBarOverlap = 164;
    private static final int ID_mPGSystemBars = 165;
    private static final int ID_mPGSystemBarsStatusBarColor = 166;
    private static final int ID_mPGSystemBarsStatusBarLight = 513;
    private static final int ID_mPGSystemBarsNavigationBarColor = 167;
    private static final int ID_mPGSystemBarsNavigationBarLight = 514;
    private static final int ID_mGCAppStyle = 168;
    private static final int ID_mPGADCategories = 190;
    private static final int ID_mPGBackgroundScaleType = 191;
    private static final int ID_mGCImagePoolSize = 192;
    private static final int ID_mPGAppDrawerABTextColor = 194;
    private static final int ID_mGCOverlayCategory = 500;
    private static final int ID_mGCOverlaySelect = 501;
    private static final int ID_mGCOverlayLaunchHide = 502;
    private static final int ID_mGCOverlayDisplayHandles = 503;
    private static final int ID_mGCOverlayHandleSize = 504;
    private static final int ID_mGCOverlayHandleWidth = 505;
    private static final int ID_mGCOverlayHandlePosition = 506;
    private static final int ID_mPGFolderLookAutoFindOrigin = 507;
    private static final int ID_mPageEventItemAdded = 508;
    private static final int ID_mPageEventItemRemoved = 509;
    private static final int ID_mGCOverlayPermission = 510;
    private static final int ID_mGCLockScreenDisableOverlay = 511;
    private static final int ID_mPGMiscLWPStdEvents = 512;
    private static final int ID_mPageEventMenu = 204;
    private static final int ID_mPageEventStartup = 205;

    private static final int ID_mPGMiscAutoExit = 10000;
    private static final int ID_mPGIcons = 10001;
    private static final int ID_mPGApplyIconPack = 10002;
    private static final int ID_mPGRevertCustomIcon = 10003;

	@Override
	public void onItemLayoutPressed() {
		// pass
	}

	@Override
	public void onItemLayoutClicked(ItemLayout item_layout, int x, int y) {
		showPreviewToast();
	}

	@Override
	public void onItemLayoutDoubleClicked(ItemLayout item_layout, int x, int y) {
		item_layout.animateZoomTo(ItemLayout.POSITION_ORIGIN, 0);
	}

	@Override
	public void onItemLayoutLongClicked(ItemLayout item_layout, int x, int y) {
		showPreviewToast();
	}

	@Override
	public void onItemLayoutSwipeLeft(ItemLayout item_layout) {
		// pass
	}

	@Override
	public void onItemLayoutSwipeRight(ItemLayout item_layout) {
		// pass
	}

	@Override
	public void onItemLayoutSwipeUp(ItemLayout item_layout) {
		// pass
	}

	@Override
	public void onItemLayoutSwipeDown(ItemLayout item_layout) {
		// pass
	}

	@Override
	public void onItemLayoutSwipe2Left(ItemLayout item_layout) {
		// pass
	}

	@Override
	public void onItemLayoutSwipe2Right(ItemLayout item_layout) {
		// pass
	}

	@Override
	public void onItemLayoutSwipe2Up(ItemLayout item_layout) {
		// pass
	}

	@Override
	public void onItemLayoutSwipe2Down(ItemLayout item_layout) {
		// pass
	}

	@Override
	public void onItemLayoutZoomChanged(float scale) {
		// pass
	}

	@Override
	public void onItemLayoutPinchStart() {
		// pass
	}

	@Override
	public boolean onItemLayoutPinch(float scale) {
		return true;
	}

	@Override
	public void onItemLayoutPinchEnd(boolean from_user) {
		// pass
	}

	@Override
	public void onItemLayoutOnLayoutDone(ItemLayout item_layout) {
		// pass
		// if(mPage.page==Utils.APP_DRAWER_PAGE) {
		// int w = mItemLayoutPage.getCellWidth();
		// if(w > 0) {
		// int x=0, y=0,
		// max=mItemLayoutPage.getWidth()/w;//c.gridLayoutModeNumColumns;
		// for(Item i : mItemLayoutPage.getItems()) {
		// i.getCell().offsetTo(x, y);
		// x++;
		// if(x==max) {
		// x=0;
		// y++;
		// }
		// }
		// mItemLayoutPage.requestLayout();
		// }
		// }
	}

    @Override
    public void onItemLayoutSizeChanged(ItemLayout item_layout, int w, int h, int oldw, int oldh) {
        // pass
    }
    
    @Override
    public void onHandlePressed(HandleView.Handle h) {
        // pass
    }

    @Override
    public void onHandleMoved(HandleView.Handle h, float dx, float dy) {
        // pass
    }

    @Override
    public void onHandleUnpressed(HandleView.Handle h, float dx, float dy) {
        // pass
    }

    @Override
    public void onHandleClicked(HandleView.Handle h) {
        // pass
    }

    @Override
    public void onHandleLongClicked(HandleView.Handle h) {
        // pass
    }

    private void showPreviewToast() {
		Toast.makeText(this, R.string.preview_toast, Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onItemLayoutPositionChanged(ItemLayout il, float mCurrentDx, float mCurrentDy, float mCurrentScale) {
		// pass
	}

	@Override
	public void onItemLayoutStopPointReached(ItemLayout item_layout, StopPoint sp) {
		// pass
	}

    @Override
    public void onItemLayoutWindowSystemUiVisibility(ItemLayout il, int visibility) {
		// pass
    }

    @Override
    public void onItemLayoutMasterSelectedItemChanged(Item masterSelectedItem) {
		// pass
    }

	@Override
	public void onItemLayoutPageLoaded(ItemLayout itemLayout, Page oldPage, Page newPage) {
		// pass
	}

	@Override
	public void onItemLayoutAppShortcutDropped(ItemLayout itemLayout, Object shortcutInfo, float x, float y) {
		// pass
	}
}
