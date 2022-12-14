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
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import net.pierrox.lightning_launcher.API;
import net.pierrox.lightning_launcher.LLApp;
import net.pierrox.lightning_launcher.LLAppPhone;
import net.pierrox.lightning_launcher.api.ScreenIdentity;
import net.pierrox.lightning_launcher.configuration.GlobalConfig;
import net.pierrox.lightning_launcher.data.Item;
import net.pierrox.lightning_launcher.data.LightningIntent;
import net.pierrox.lightning_launcher.data.Page;
import net.pierrox.lightning_launcher.data.Screen;
import net.pierrox.lightning_launcher.data.Shortcut;
import net.pierrox.lightning_launcher.data.Utils;
import net.pierrox.lightning_launcher.engine.LightningEngine;
import net.pierrox.lightning_launcher.util.PhoneUtils;
import net.pierrox.lightning_launcher.views.ItemLayout;
import net.pierrox.lightning_launcher.views.item.ItemView;
import net.pierrox.lightning_launcher_extreme.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class ScreenManager extends ResourceWrapperActivity implements OnClickListener, View.OnLongClickListener {
	private static final int DIALOG_LABEL = 2;
	private static final int DIALOG_CONFIRM_DELETE = 3;
	
	private static final int REQUEST_PICK_IMAGE = 2;

	private static final int SCREEN_SUBVIEWS_IDS[] = new int[] { R.id.screen, R.id.screen_icon, R.id.screen_label, R.id.screen_selected };
	
	private static final String SIS_CURRENT_PAGE = "c";
	
	private static final int SCREEN_MODE_EDIT = 1;
	private static final int SCREEN_MODE_SELECT = 2;
	private static final int SCREEN_MODE_SELECT_MULTIPLE = 3;
	private static final int SCREEN_MODE_GOTO = 4;
    private static final int SCREEN_MODE_SHORTCUT = 5;
    protected static final int SCREEN_MODE_LWP = 6;

    private LightningEngine mLightningEngine;
	private net.pierrox.lightning_launcher.engine.Screen mScreen;

	private int mMode;
	
	private ViewGroup mScreenContainer;
	
	private List<Screen> mScreens;
	private int mHomePage;
	private int mCurrentPage;
	
	private Drawable mDefaultIcon;
	
	private Rect mTmpRect = new Rect();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
        Utils.setTheme(this, Utils.APP_THEME_NO_ACTION_BAR);

		super.onCreate(savedInstanceState);

        mLightningEngine = LLApp.get().getAppEngine();

		mDefaultIcon = getResources().getDrawable(R.drawable.icon);
		
		Window w = getWindow();
        WallpaperManager wpm = WallpaperManager.getInstance(this);
        try {
            w.setBackgroundDrawable(wpm.getDrawable());
        } catch(Throwable e) {
            // fails on some devices
        }
		
		setContentView(R.layout.screen_manager);
		
		mScreenContainer = (ViewGroup) findViewById(R.id.sm_container);

		int[] btns = new int[] {
			R.id.sm_first,
			R.id.sm_previous,
			R.id.sm_next,
			R.id.sm_last,
			R.id.sm_home,
			R.id.sm_delete,
			R.id.sm_add,
			R.id.sm_clone,
		};
        String codes = "ABCDF476";
        Typeface typeface = LLApp.get().getIconsTypeface();
		for(int i=0; i<btns.length; i++) {
			Button btn = (Button)findViewById(btns[i]);
			btn.setOnClickListener(this);
			btn.setOnLongClickListener(this);
            btn.setText(codes.substring(i, i+1));
            btn.setTypeface(typeface);
            btn.setTextColor(0xff000000);
		}
		
		init();
		
		int page = Page.NONE;
		if(savedInstanceState != null) {
			page = savedInstanceState.getInt(SIS_CURRENT_PAGE);
		} else {
			if(mMode == SCREEN_MODE_LWP) {
				page = mLightningEngine.getGlobalConfig().lwpScreen;
			}
			if(page == Page.NONE) {
				page = mMode == SCREEN_MODE_SELECT || mMode == SCREEN_MODE_SHORTCUT || mMode == SCREEN_MODE_LWP ? mLightningEngine.readCurrentPage(Page.FIRST_DASHBOARD_PAGE) : mHomePage;
			}
		}
		selectScreenByPage(page);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		mScreen.destroy();
	}

    private void init() {
		mScreen = new net.pierrox.lightning_launcher.engine.Screen(this, 0) {
			@Override
			public ScreenIdentity getIdentity() {
				return ScreenIdentity.DESKTOP_PREVIEW;
			}

			@Override
			protected Resources getRealResources() {
				return ScreenManager.this.getRealResources();
			}

			@Override
			public void launchItem(ItemView itemView) {
				// pass
			}
		};
		mScreen.setWindow(getWindow());

        mScreens = new ArrayList<Screen>();

        GlobalConfig gc = mLightningEngine.getGlobalConfig();
        final int[] screens_order = gc.screensOrder;
        final String[] screens_name = gc.screensNames;
        int n = screens_order.length;
        for(int i=0; i<n; i++) {
            int page = screens_order[i];
            Bitmap icon;
            try {
                icon = BitmapFactory.decodeFile(Page.getPageIconFile(mLightningEngine.getBaseDir(), page).getAbsolutePath());
            } catch(Throwable t) {
                icon = null;
            }
            Screen screen = new Screen(page, icon, screens_name[i], true);
            mScreens.add(screen);
        }
        mHomePage = gc.homeScreen;

		mMode = getMode();
		if(mMode == SCREEN_MODE_LWP) {
			TextView tv = (TextView) findViewById(R.id.sm_title);
			tv.setText(R.string.sd);
			tv.setVisibility(View.VISIBLE);
		} else if(mMode == SCREEN_MODE_SELECT_MULTIPLE) {
			Intent intent = getIntent();
			int[] selected_pages = intent.getIntArrayExtra(API.SCREEN_PICKER_INTENT_EXTRA_SELECTED_SCREENS);
			for (Screen screen : mScreens) {
				screen.selected = false;
				for (int page : selected_pages) {
					if (screen.page == page) {
						screen.selected = true;
						break;
					}
				}
			}
			String title = intent.getStringExtra(API.SCREEN_PICKER_INTENT_EXTRA_TITLE);
			if (title != null) {
				TextView tv = (TextView) findViewById(R.id.sm_title);
				tv.setText(title);
				tv.setVisibility(View.VISIBLE);
			}
			Button ok = (Button) findViewById(R.id.sm_ok);
			ok.setOnClickListener(this);
			ok.setVisibility(View.VISIBLE);
		}
		
		mScreenContainer.removeAllViews();
		
		n = mScreens.size();
		for(int i=0; i<n; i++) {
			addScreen(i, i);
		}

		updateScreenViews();
		
		findViewById(R.id.sm_buttons).setVisibility(mMode==SCREEN_MODE_EDIT ? View.VISIBLE : View.GONE);
	}

	protected int getMode() {
		String action = getIntent().getAction();
		if (Intent.ACTION_PICK.equals(action)) {
			return SCREEN_MODE_SELECT;
		} else if (Intent.ACTION_CREATE_SHORTCUT.equals(action)) {
			return SCREEN_MODE_SHORTCUT;
		} else if (Intent.ACTION_RUN.equals(action)) {
			return SCREEN_MODE_GOTO;
		} else if (Intent.ACTION_EDIT.equals(action)) {
			return SCREEN_MODE_EDIT;
		} else {
			return SCREEN_MODE_SELECT_MULTIPLE;
		}
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putInt(SIS_CURRENT_PAGE, mCurrentPage);
	}
	
	@Override
	protected void onPause() {
		super.onPause();

		mScreen.pause();

		if(mMode==SCREEN_MODE_EDIT) {
			int current = mLightningEngine.readCurrentPage(Page.FIRST_DASHBOARD_PAGE);
			boolean current_exists = false;
			int n = mScreens.size();
			int[] screens_order = new int[n];
			String[] screens_names = new String[n];
			for(int i=0; i<n; i++) {
                Screen screen = mScreens.get(i);
				int p = screen.page;
				screens_order[i] = p;
                screens_names[i] = screen.label;
				if(p == current) {
					current_exists = true;
				}
			}
			
			GlobalConfig gc = mLightningEngine.getGlobalConfig();
			gc.homeScreen = mHomePage;
			gc.screensOrder = screens_order;
            gc.screensNames = screens_names;
			mLightningEngine.notifyGlobalConfigChanged();
			
			if(!current_exists) {
				(LLApp.get()).displayPagerPage(mHomePage, true);
			}
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		mScreen.resume();
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		final Screen screen = mScreens.get(getScreenIndex(mCurrentPage));
		
		AlertDialog.Builder builder;
		
		switch(id) {
		case DIALOG_LABEL:
			builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.l_custom);
			final EditText edit_text = new EditText(this);
			String label = screen.label!=null ? screen.label : String.valueOf(screen.page+1);
			edit_text.setText(label);
            edit_text.setSelection(label.length());
			FrameLayout l = new FrameLayout(this);
			l.setPadding(10, 10, 10, 10);
			l.addView(edit_text);
			builder.setView(l);
			builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
                    String old_label = screen.label;
					screen.label = edit_text.getText().toString();
					updateScreenViews();
                    updateScreenShortcuts(screen, false, true, old_label);
				}
			});
			builder.setNegativeButton(android.R.string.cancel, null);
			return builder.create();
			
		case DIALOG_CONFIRM_DELETE:
			builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.confirm_delete);
			builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					int index = getScreenIndex(mCurrentPage);
					mLightningEngine.getPageManager().removePage(mCurrentPage);
					GlobalConfig gc = mLightningEngine.getGlobalConfig();
					if(gc.lockScreen == mCurrentPage) {
                    	gc.lockScreen = Page.NONE;
						mLightningEngine.notifyGlobalConfigChanged();
                    }
                    if(gc.overlayScreen == mCurrentPage) {
                        gc.overlayScreen = Page.NONE;
						mLightningEngine.notifyGlobalConfigChanged();
                    }
                    Screen deleted = mScreens.remove(index);
					mScreenContainer.removeViewAt(index);
					Screen selected_screen = mScreens.get(index>0 ? index-1 : index);
					if(deleted.page==mHomePage) {
						mHomePage = selected_screen.page;
						(LLApp.get()).displayPagerPage(mHomePage, true);
					}
					updateScreenViews();
					selectScreenByPage(selected_screen.page);
				}
			});
			builder.setNegativeButton(android.R.string.cancel, null);
			return builder.create();
		}
		
		return null;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) { 
		super.onActivityResult(requestCode, resultCode, data); 

		if (requestCode == REQUEST_PICK_IMAGE) {
			if (resultCode == RESULT_OK) {
                FileOutputStream fos = null;
				try {
                    File tmpImageFile = Utils.getTmpImageFile();
                    File icon_file = Page.getPageIconFile(mLightningEngine.getBaseDir(), mCurrentPage);
                    Screen screen = mScreens.get(getScreenIndex(mCurrentPage));
                    if(tmpImageFile.exists()) {
                        Uri from_uri = Uri.fromFile(tmpImageFile);
                        Bitmap from = decodeUri(from_uri);

                        icon_file.getParentFile().mkdirs();

                        fos = new FileOutputStream(icon_file);
                        from.compress(CompressFormat.PNG, 100, fos);

                        screen.icon = from;
                    } else {
                        icon_file.delete();
                        screen.icon = null;
                    }
					
					updateScreenViews();

                    updateScreenShortcuts(screen, true, false, null);
				} catch (Exception e) {
					Toast.makeText(this, R.string.item_settings_icon_copy_failed, Toast.LENGTH_SHORT).show();
					e.printStackTrace();
				} finally {
					if(fos!=null) try { fos.close(); } catch(Exception e) {}
				}
			}
		}
	}

    @SuppressWarnings("deprecation")
	@Override
	public void onClick(final View v) {
        if(LLApp.get().isFreeVersion()) {
            LLApp.get().showFeatureLockedDialog(this);
            return;
        }

		if(mMode==SCREEN_MODE_SELECT) {
			int page = (Integer)v.getTag();
			Intent data = new Intent();
			data.putExtra(API.SCREEN_PICKER_INTENT_EXTRA_SCREEN, page);
			setResult(RESULT_OK, data);
			finish();
			return;
        } else if(mMode==SCREEN_MODE_SHORTCUT) {
            int p = (Integer)v.getTag();
            Screen screen = mScreens.get(getScreenIndex(p));
            Page page = mLightningEngine.getOrLoadPage(p);
            Intent shortcut = PhoneUtils.createDesktopBookmarkShortcut(this, null, page, screen.label, screen.icon, true);
            setResult(RESULT_OK, shortcut);
            finish();
		} else if(mMode==SCREEN_MODE_SELECT_MULTIPLE) {
            if(v.getId() == R.id.sm_ok) {
                saveSelectionResult();
                finish();
                return;
            } else {
                int page = (Integer)v.getTag();
                Screen screen = mScreens.get(getScreenIndex(page));
                screen.selected = !screen.selected;
                updateScreenViews();
                return;
            }
		} else if(mMode==SCREEN_MODE_GOTO) {
			int page = (Integer)v.getTag();
			LLApp.get().displayPagerPage(page, false);
			finish();
			return;
		} else if(mMode==SCREEN_MODE_LWP) {
			GlobalConfig gc = mLightningEngine.getGlobalConfig();
			gc.lwpScreen = (Integer)v.getTag();
			mLightningEngine.notifyGlobalConfigChanged();
			finish();
			return;
		}
		
		final View screen_view;
		Screen screen;
		int page;
		int index = getScreenIndex(mCurrentPage);
		screen_view = mScreenContainer.getChildAt(index);
		int num_screens_minus_one = mScreens.size()-1;
		boolean ensure_visible = false;
		
		switch(v.getId()) {
		case R.id.sm_first:
			if(index>0) {
				mScreenContainer.removeViewAt(index);
				mScreenContainer.addView(screen_view, 0);
				screen = mScreens.remove(index);
				mScreens.add(0, screen);
				ensure_visible = true;
			}
			break;
			
		case R.id.sm_previous:
			if(index>0) {
				mScreenContainer.removeViewAt(index);
				mScreenContainer.addView(screen_view, index-1);
				screen = mScreens.remove(index);
				mScreens.add(index-1, screen);
				ensure_visible = true;
			}
			break;
			
		case R.id.sm_next:
			if(index<num_screens_minus_one) {
				mScreenContainer.removeViewAt(index);
				mScreenContainer.addView(screen_view, index+1);
				screen = mScreens.remove(index);
				mScreens.add(index+1, screen);
				ensure_visible = true;
			}
			break;
			
		case R.id.sm_last:
			if(index<num_screens_minus_one) {
				mScreenContainer.removeViewAt(index);
				mScreenContainer.addView(screen_view, num_screens_minus_one);
				screen = mScreens.remove(index);
				mScreens.add(num_screens_minus_one, screen);
				ensure_visible = true;
			}
			break;
			
		case R.id.sm_delete:
			if(mScreens.size()>1) {
				showDialog(DIALOG_CONFIRM_DELETE);
			}
			break;
			
		case R.id.sm_home:
			mHomePage = mCurrentPage;
			updateScreenViews();
			break;
			
		case R.id.sm_add:
			page = Page.reservePage(mLightningEngine.getBaseDir(), false);
			if(page != Page.NONE) {
				mScreens.add(index+1, new Screen(page, null, getString(R.string.sm_an), false));
				addScreen(getScreenIndex(page), index+1);
				updateScreenViews();
				selectScreenByPage(page);
			}
			break;

        case R.id.sm_clone:
			LightningEngine.PageManager pm = mLightningEngine.getPageManager();
			page = pm.clonePage(pm.getOrLoadPage(mCurrentPage), false).id;
            mScreens.add(index+1, new Screen(page, null, getString(R.string.sm_cn)+" "+mScreens.get(getScreenIndex(mCurrentPage)).label, false));
            addScreen(getScreenIndex(page), index+1);
            updateScreenViews();
            selectScreenByPage(page);
            break;
			
		case R.id.screen_icon:
			page = (Integer)v.getTag();
			selectScreenByPage(page);
			ImagePicker.startActivity(this, REQUEST_PICK_IMAGE);
			break;

		case R.id.screen_label:
			page = (Integer)v.getTag();
			selectScreenByPage(page);
			try { removeDialog(DIALOG_LABEL); } catch(Exception e) {}
			showDialog(DIALOG_LABEL);
			break;
			
		case R.id.screen:
			page = (Integer)v.getTag();
			selectScreenByPage(page);
			break;
		}
		
		if(ensure_visible) {
			mScreenContainer.post(new Runnable() {
				@Override
				public void run() {
					screen_view.getHitRect(mTmpRect);
					mTmpRect.offsetTo(0, 0);
					HorizontalScrollView scroller = (HorizontalScrollView) findViewById(R.id.sm_scroller);
					scroller.requestChildRectangleOnScreen(screen_view, mTmpRect, false);
				}
			});
		}
	}


    @Override
    public boolean onLongClick(View view) {
        int label_res = 0;
        switch(view.getId()) {
            case R.id.sm_first: label_res = R.string.sm_f; break;
            case R.id.sm_previous:label_res = R.string.sm_p; break;
            case R.id.sm_next:label_res = R.string.sm_n; break;
            case R.id.sm_last:label_res = R.string.sm_l; break;
            case R.id.sm_delete:label_res = R.string.sm_d; break;
            case R.id.sm_home:label_res = R.string.sm_h; break;
            case R.id.sm_add:label_res = R.string.sm_a; break;
            case R.id.sm_clone:label_res = R.string.sm_c; break;

            default:
                int page = (Integer)view.getTag();
				((LLAppPhone)LLApp.get()).displayPagerPage(page, false);
                finish();
                startActivity(new Intent(this, Dashboard.class));
                break;
        }
        if(label_res != 0) {
            int[] location = new int[2];
            view.getLocationOnScreen(location);
            Toast toast = Toast.makeText(this, label_res, Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.TOP|Gravity.LEFT, location[0], location[1]);
            toast.show();
        }
        return true;
    }

    private void updateScreenViews() {
		int n = mScreenContainer.getChildCount();
		for(int i=0; i<n; i++) {
			View screen_view = mScreenContainer.getChildAt(i);
			int page = (Integer)screen_view.getTag();
			Screen screen = mScreens.get(getScreenIndex(page));

            TextView is_home_icon = (TextView) screen_view.findViewById(R.id.screen_home);
            is_home_icon.setTypeface(LLApp.get().getIconsTypeface());
            is_home_icon.setVisibility(page == mHomePage ? View.VISIBLE : View.INVISIBLE);
            is_home_icon.setTextColor(0xffffffff);
			
			CheckBox selected = (CheckBox)screen_view.findViewById(R.id.screen_selected);
			if(mMode==SCREEN_MODE_SELECT_MULTIPLE) {
				selected.setVisibility(View.VISIBLE);
				selected.setChecked(screen.selected);
			} else {
				selected.setVisibility(View.GONE);
			}
			
			((TextView)screen_view.findViewById(R.id.screen_label)).setText(screen.label!=null ? screen.label : String.valueOf(screen.page+1));
			ImageView iv = (ImageView)screen_view.findViewById(R.id.screen_icon);
			if(screen.icon == null) {
				iv.setImageDrawable(mDefaultIcon);
			} else {
				iv.setImageBitmap(screen.icon);
			}
		}
	}

    private void updateScreenShortcuts(Screen screen, boolean update_icon, boolean update_label, String old_label) {
        // look in all existing pages and shortcuts and update "screen&position" items matching this page

        ComponentName ll_component_name = new ComponentName(this, Dashboard.class);

        File screen_icon = Page.getPageIconFile(mLightningEngine.getBaseDir(), screen.page);
        byte[] buffer = new byte[512];

        for(int p : mLightningEngine.getPageManager().getAllPagesIds()) {
            Page page = mLightningEngine.getOrLoadPage(p);
            File icon_dir = page.getIconDir();
            for(Item i : page.items) {
                boolean modified = false;
                if(i.getClass()==Shortcut.class) {
                    Shortcut s=(Shortcut)i;
                    Intent intent = s.getIntent();
                    ComponentName cn=intent.getComponent();
                    if(cn!=null && cn.compareTo(ll_component_name)==0) {
                        if(intent.hasExtra(LightningIntent.INTENT_EXTRA_DESKTOP) && intent.getIntExtra(LightningIntent.INTENT_EXTRA_DESKTOP, 0)==screen.page) {
                            // it looks like a screen&position shortcut
                            if(update_icon) {
                            	icon_dir.mkdirs();
                                Utils.copyFileSafe(buffer, screen_icon, s.getDefaultIconFile());
                                modified = true;
                            }

                            if(update_label) {
                                if(old_label.equals(s.getLabel())) {
                                    s.setLabel(screen.label);
                                    modified = true;
                                }
                            }
                        }
                    }
                }
                if(modified) {
                    i.notifyChanged();
                }
            }
        }
    }
	
	private void selectScreenByPage(int page) {
		mCurrentPage = page;
		int n=mScreenContainer.getChildCount();
		for(int i=0; i<n; i++) {
			View screen_view = mScreenContainer.getChildAt(i);
			View border = screen_view.findViewById(R.id.screen_border);
			int screen_page = (Integer)screen_view.getTag();
			
			boolean selected = (mMode!=SCREEN_MODE_SELECT_MULTIPLE && screen_page==page);
			border.setBackgroundColor(selected ? 0x80ffffff : 0);
			border.setPadding(10, 10, 10, 10);
		}
	}
	
	private int getScreenIndex(int page) {
		for(int i=mScreens.size()-1; i>=0; i--) {
			if(mScreens.get(i).page==page) {
				return i;
			}
		}
		return 0;
	}
	
	private void addScreen(int index, int pos) {
		Screen screen = mScreens.get(index);
		int page = screen.page;
		View screen_view = getLayoutInflater().inflate(R.layout.screen, null);
		ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		screen_view.setLayoutParams(lp);
		
		for(int id: SCREEN_SUBVIEWS_IDS) {
			View v = screen_view.findViewById(id);
			v.setOnClickListener(this);
			v.setTag(page);
        }
        screen_view.setOnLongClickListener(this);

        ImageView preview = (ImageView) screen_view.findViewById(R.id.screen_preview);
		preview.setImageDrawable(new PageDrawable(page));
		
		mScreenContainer.addView(screen_view, pos);
	}
	
	private Bitmap decodeUri(Uri selectedImage) throws FileNotFoundException {

        // Decode image size
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage), null, o);

        // The new size we want to scale to
        final int REQUIRED_SIZE = Utils.getStandardIconSize();

        // Find the correct scale value. It should be the power of 2.
        int width_tmp = o.outWidth, height_tmp = o.outHeight;
        int scale = 1;
        while (true) {
            if (width_tmp / 2 < REQUIRED_SIZE
               || height_tmp / 2 < REQUIRED_SIZE) {
                break;
            }
            width_tmp /= 2;
            height_tmp /= 2;
            scale *= 2;
        }

        // Decode with inSampleSize
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        return BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage), null, o2);
    }

	private void saveSelectionResult() {
		int count=0;
		for(Screen screen : mScreens) {
			if(screen.selected) {
				count++;
			}
		}
		int[] selected_screens;
//		if(count == 0) {
//			selected_screens = new int[] { mHomePage };
//		} else {
			selected_screens = new int[count];
			count=0;
			for(Screen screen : mScreens) {
				if(screen.selected) {
					selected_screens[count++]=screen.page;
				}
			}
//		}
		Intent data = new Intent();
		data.putExtra(API.SCREEN_PICKER_INTENT_EXTRA_SELECTED_SCREENS, selected_screens);
		setResult(RESULT_OK, data);
	}

	private static final float PREVIEW_RATIO = 0.3f;


    private class PageDrawable extends Drawable {
		private int mPage;
		private ItemLayout il;
		private int mWidth;
		private int mHeight;
		
		public PageDrawable(int page) {
			mPage = page;
			
			DisplayMetrics metrics = getResources().getDisplayMetrics();
			mWidth = metrics.widthPixels;
			mHeight = metrics.heightPixels;
			
			Page p = mLightningEngine.getOrLoadPage(page);
            il = new ItemLayout(ScreenManager.this, null);
			il.setAllowDelayedViewInit(false);
			il.setScreen(mScreen);
			mScreen.takeItemLayoutOwnership(il);
            il.setPage(p);
			il.measure(MeasureSpec.makeMeasureSpec(mWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(mHeight, MeasureSpec.EXACTLY));
			il.layout(0, 0, mWidth, mHeight);
		}
		
		@Override
		public int getIntrinsicWidth() {
			return (int)(mWidth*PREVIEW_RATIO);
		}
		
		@Override
		public int getIntrinsicHeight() {
			return (int)(mHeight*PREVIEW_RATIO);
		}
		
		@Override
		public void draw(Canvas canvas) {
			canvas.scale(PREVIEW_RATIO, PREVIEW_RATIO);
			canvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG));
			il.draw(canvas);
		}

		@Override
		public int getOpacity() {
			return PixelFormat.TRANSLUCENT;
		}

		@Override
		public void setAlpha(int alpha) {
			// pass
		}

		@Override
		public void setColorFilter(ColorFilter cf) {
			// pass
		}
		
	}
}
