package net.pierrox.lightning_launcher.activities;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.NinePatch;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Filter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import net.margaritov.preference.colorpicker.ColorPickerDialog;
import net.pierrox.lightning_launcher.LLApp;
import net.pierrox.lightning_launcher.data.Page;
import net.pierrox.lightning_launcher.data.Utils;
import net.pierrox.lightning_launcher.util.LruCache;
import net.pierrox.lightning_launcher.views.EditTextIme;
import net.pierrox.lightning_launcher_extreme.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static android.content.ClipData.newPlainText;


public class ImagePicker extends ResourceWrapperActivity implements AdapterView.OnItemSelectedListener, AdapterView.OnItemClickListener, View.OnClickListener, ColorPickerDialog.OnColorChangedListener, View.OnLongClickListener, AdapterView.OnItemLongClickListener, EditTextIme.OnEditTextImeListener, TextWatcher {
    private static final int MODE_NONE = -1;
    private static final int MODE_ICON_PACK = 0;
    private static final int MODE_PATH = 1;
    private static final int MODE_PKG = 2;
    private static final int MODE_LAUNCHER_PAGE = 3;

    private static final String ANDROID = "Android";

    private static final String INTENT_EXTRA_CROP = "c";

    private static final int REQUEST_CAPTURE_IMAGE = 1;
    private static final int REQUEST_PICK_IMAGE = 2;
    private static final int REQUEST_CROP_IMAGE = 3;


    private static final String PREF_CURRENT_MODE = "ip_m";
    private static final String PREF_CURRENT_PATH = "ip_pa";
    private static final String PREF_CURRENT_PKG = "ip_pk";
    private static final String PREF_CURRENT_ICON_PACK = "ip_ip";
    private static final String PREF_CURRENT_LAUNCHER_PAGE = "ip_lp";
    private static final String PREF_CURRENT_SCROLL_PATH = "ip_spa";
    private static final String PREF_CURRENT_SCROLL_PKG = "ip_spk";
    private static final String PREF_CURRENT_SCROLL_ICON_PACK = "ip_sip";
    private static final String PREF_CURRENT_SCROLL_LAUNCHER_PAGE = "ip_slp";
    private static final String PREF_BACKGROUND_COLOR = "ip_bgc";

    private int mCurrentMode;
    private int mGridMode = MODE_NONE;
    private File mCurrentPath;
    private String mCurrentPkg;
    private String mCurrentIconPack;
    private String mCurrentLauncherPage;
    private int mCurrentScrollPath;
    private int mCurrentScrollPkg;
    private int mCurrentScrollIconPack;
    private int mCurrentScrollLauncherPage;
    private int mCurrentScrollIndex;
    private int mBackgroundColor;

    private Spinner mSourceSpinner;
    private Spinner mIconPackSpinner;
    private Spinner mPkgSpinner;
    private Spinner mLauncherPageSpinner;
    private View mPathTextGrp;
    private TextView mPathTextView;
    private GridView mGridView;
    private TextView mNoIconTextView;
    private EditTextIme mSearchText;
    private View mButtonBar;

    private PkgLabelAdapter mIconPackAdapter;
    private PkgLabelAdapter mPkgAdapter;
    private ArrayAdapter<String> mLauncherPageAdapter;

    private Bitmap mDefaultIcon;
    private int mStandardIconSize;
    private int mLauncherIconDensity;

    private View mClickedView;

    private static class BitmapInfo {
        Bitmap bitmap;
        boolean isNinePatch; // need to be stored because 9patch chunk is lost during resize

        private BitmapInfo(Bitmap bitmap, boolean isNinePatch) {
            this.bitmap = bitmap;
            this.isNinePatch = isNinePatch;
        }
    }
    private LruCache<String, BitmapInfo> mThumbnailCache;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.setTheme(this, Utils.APP_THEME_NO_ACTION_BAR);

        super.onCreate(savedInstanceState);

        if(savedInstanceState == null) {
            // if recreated because the activity has been disposed by the framework, don't remove
            // the temp file, it will be used for instance in the activity result callback
            Utils.getTmpImageFile().delete();
        }

        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 8;

        Method getByteCount;
        try {
            getByteCount = Bitmap.class.getMethod("getByteCount");
        } catch (NoSuchMethodException e) {
            getByteCount = null;
        }

        final Method finalGetByteCount = getByteCount;
        mThumbnailCache = new LruCache<String, BitmapInfo>(cacheSize) {
            @Override
            protected int sizeOf(String key, BitmapInfo bitmapInfo) {
                // The cache size will be measured in kilobytes rather than
                // number of items.
                if(finalGetByteCount != null) {
                    try {
                        return (Integer)finalGetByteCount.invoke(bitmapInfo.bitmap) / 1024;
                    } catch (Exception e) {
                        // pass and continue
                    }
                }
                return bitmapInfo.bitmap.getWidth()*bitmapInfo.bitmap.getHeight() * 4 /1024;
            }
        };

        PackageManager pm = getPackageManager();
        Drawable d = pm.getDefaultActivityIcon();
        mDefaultIcon = Bitmap.createBitmap(Math.max(d.getIntrinsicWidth(), 1),
                Math.max(d.getIntrinsicHeight(), 1),
                Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(mDefaultIcon);
        d.setBounds(0, 0, mDefaultIcon.getWidth(), mDefaultIcon.getHeight());
        d.draw(c);

        mStandardIconSize =getResources().getDimensionPixelSize(android.R.dimen.app_icon_size);
        mLauncherIconDensity =0;

        try {
            Method getLauncherLargeIconSize=ActivityManager.class.getMethod("getLauncherLargeIconSize");
            Method getLauncherLargeIconDensity=ActivityManager.class.getMethod("getLauncherLargeIconDensity");
            ActivityManager am=(ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            mStandardIconSize =(Integer) getLauncherLargeIconSize.invoke(am, (Object[])null);
            mLauncherIconDensity =(Integer) getLauncherLargeIconDensity.invoke(am, (Object[])null);
        } catch (Exception e) {
            // pass API level 11, 15
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mCurrentMode = prefs.getInt(PREF_CURRENT_MODE, MODE_ICON_PACK);
        mCurrentPath = new File(prefs.getString(PREF_CURRENT_PATH, Environment.getExternalStorageDirectory().getAbsolutePath()));
        mCurrentPkg = prefs.getString(PREF_CURRENT_PKG, null);
        mCurrentIconPack = prefs.getString(PREF_CURRENT_ICON_PACK, null);
        mCurrentLauncherPage = prefs.getString(PREF_CURRENT_LAUNCHER_PAGE, String.valueOf(Page.FIRST_DASHBOARD_PAGE));
        mCurrentScrollPkg = prefs.getInt(PREF_CURRENT_SCROLL_PKG, 0);
        mCurrentScrollPath = prefs.getInt(PREF_CURRENT_SCROLL_PATH, 0);
        mCurrentScrollIconPack = prefs.getInt(PREF_CURRENT_SCROLL_ICON_PACK, 0);
        mCurrentScrollLauncherPage = prefs.getInt(PREF_CURRENT_SCROLL_LAUNCHER_PAGE, 0);




        setContentView(R.layout.image_picker);

        mSourceSpinner = (Spinner) findViewById(R.id.source);
        ArrayAdapter adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.ip_s));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSourceSpinner.setAdapter(adapter);
        mSourceSpinner.setOnItemSelectedListener(this);

        mIconPackSpinner = (Spinner) findViewById(R.id.icon_pack);
        mIconPackSpinner.setOnItemSelectedListener(this);

        mPkgSpinner = (Spinner) findViewById(R.id.pkg);
        mPkgSpinner.setOnItemSelectedListener(this);

        mLauncherPageSpinner = (Spinner) findViewById(R.id.launcher_page);
        mLauncherPageSpinner.setOnItemSelectedListener(this);

        mPathTextGrp = findViewById(R.id.path_grp);
        mPathTextView = (TextView) findViewById(R.id.path);
        Button up = (Button)findViewById(R.id.path_up);
        up.setText(R.string.file_picker_activity_up);
        up.setOnClickListener(this);

        mNoIconTextView = (TextView) findViewById(R.id.no_icon);
        mNoIconTextView.setText(R.string.ip_e);

        mGridView = (GridView) findViewById(R.id.grid);
        mGridView.setNumColumns(GridView.AUTO_FIT);
        mGridView.setColumnWidth(mStandardIconSize * 2);
        mGridView.setStretchMode(GridView.STRETCH_SPACING_UNIFORM);
        mGridView.setVerticalSpacing(mStandardIconSize / 3);
        mGridView.setTextFilterEnabled(true);
        mGridView.setOnItemClickListener(this);
        mGridView.setOnItemLongClickListener(this);
        mGridView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {

            }

            @Override
            public void onScroll(AbsListView absListView, int i, int i2, int i3) {
                mCurrentScrollIndex = i;
            }
        });

        mBackgroundColor = prefs.getInt(PREF_BACKGROUND_COLOR, Color.TRANSPARENT);
        mGridView.setBackgroundColor(mBackgroundColor);

        mSearchText = (EditTextIme) findViewById(R.id.search_text);
        mSearchText.setOnEditTextImeListener(this);
        mSearchText.addTextChangedListener(this);

        mButtonBar = findViewById(R.id.btn_bar);

        Typeface typeface = LLApp.get().getIconsTypeface();

        int[] ids = new int[] {
            R.id.none,
            R.id.ext_file,
            R.id.camera,
            R.id.bgcolor,
            R.id.solid,
            R.id.search
        };
        for(int id : ids) {
            View v = findViewById(id);
            v.setOnClickListener(this);
            v.setOnLongClickListener(this);
            ((Button)v).setTypeface(typeface);
        }

        if(mCurrentMode == MODE_ICON_PACK) {
            mIconPackAdapter = new PkgLabelAdapter(this, true);
            mIconPackSpinner.setAdapter(mIconPackAdapter);
            if(mIconPackAdapter.getCount()==0) {
                mCurrentMode = MODE_PKG;
            }
        }

        mSourceSpinner.setSelection(mCurrentMode);

        setSearchMode(false);

        checkPermissions(
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                new int[] { R.string.pr_r4, R.string.pr_r5},
                REQUEST_PERMISSION_BASE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if(areAllPermissionsGranted(grantResults, R.string.pr_f3)) {
            if(mCurrentMode == MODE_PATH) {
                setMode(MODE_PATH);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        storeGridViewScroll();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor ed = prefs.edit();
        ed.putInt(PREF_CURRENT_MODE, mCurrentMode);
        ed.putString(PREF_CURRENT_PKG, mCurrentPkg);
        ed.putString(PREF_CURRENT_ICON_PACK, mCurrentIconPack);
        ed.putString(PREF_CURRENT_PATH, mCurrentPath.getAbsolutePath());
        ed.putString(PREF_CURRENT_LAUNCHER_PAGE, mCurrentLauncherPage);
        ed.putInt(PREF_CURRENT_SCROLL_PKG, mCurrentScrollPkg);
        ed.putInt(PREF_CURRENT_SCROLL_PATH, mCurrentScrollPath);
        ed.putInt(PREF_CURRENT_SCROLL_ICON_PACK, mCurrentScrollIconPack);
        ed.putInt(PREF_CURRENT_SCROLL_LAUNCHER_PAGE, mCurrentScrollLauncherPage);
        ed.putInt(PREF_BACKGROUND_COLOR, mBackgroundColor);
        ed.commit();
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
//        Boolean init = (Boolean) adapterView.getTag();
//        if(init == null) {
//            adapterView.setTag(true);
//        } else {
            if (adapterView == mSourceSpinner) {
                setMode(i);
            } else if (adapterView == mIconPackSpinner) {
                String pkg = mIconPackAdapter.getItem(i).pkg;
                loadIconPack(pkg);
            } else if (adapterView == mPkgSpinner) {
                String pkg = mPkgAdapter.getItem(i).pkg;
                loadPkg(pkg);
            } else if (adapterView == mLauncherPageSpinner) {
                String p = mLauncherPageAdapter.getItem(i);
                loadLauncherPage(p);
            }
//        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
        // pass
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        if(adapterView == mGridView) {
            byte[] buffer = new byte[4096];
            Object item = mGridView.getItemAtPosition(position);
            File tmp_image_file = Utils.getTmpImageFile();
            if(mCurrentMode == MODE_PATH) {
                ImageFile imf = (ImageFile) item;
                if(imf.file.isDirectory()) {
                    loadPath(imf.file);
                } else {
                    Utils.copyFileSafe(buffer, imf.file, tmp_image_file);
                    imagePicked(true);
                }
            } else if(mCurrentMode == MODE_PKG || mCurrentMode == MODE_ICON_PACK) {
                ImageResource ir = (ImageResource) item;
                FileOutputStream fos=null;
                InputStream is = null;
                try {
                    Resources rsrc = ir.packageName.equals(ANDROID) ? getResources() : createPackageContext(ir.packageName, 0).getResources();
                    int id = ir.res;
                    boolean is_nine_patch = false;
                    Bitmap bmp = null;
                    if(Utils.sGetDrawableForDensity!=null) {
                        try {
                            BitmapDrawable d=(BitmapDrawable) Utils.sGetDrawableForDensity.invoke(rsrc, id, mLauncherIconDensity);
                            Bitmap orig_bmp = d.getBitmap();
                            is_nine_patch = NinePatch.isNinePatchChunk(orig_bmp.getNinePatchChunk());
                            bmp = Utils.createStandardSizedIcon(orig_bmp, mStandardIconSize);
                        } catch(Throwable e) {
                            // pass, continue with classic method
                        }
                    }
                    if(bmp == null) {
                        bmp = BitmapFactory.decodeResource(rsrc, id);
                        if(bmp != null) {
                            is_nine_patch = NinePatch.isNinePatchChunk(bmp.getNinePatchChunk());
                        }
                    }
                    if(bmp != null) {
                        if(is_nine_patch) {
                            // nine patches need to be copied as is
                            is = rsrc.openRawResource(ir.res);
                            fos=new FileOutputStream(tmp_image_file);
                            int n;
                            while((n=is.read(buffer))>0) {
                                fos.write(buffer, 0, n);
                            }
                        } else {
                            // icon are copied using the selected density
                            fos = new FileOutputStream(tmp_image_file);
                            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
                        }
                        imagePicked(!is_nine_patch);
                    } else {
                        tmp_image_file.delete();
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                    tmp_image_file.delete();
                } finally {
                    if(is!=null) try { is.close(); } catch(Exception e) {}
                    if(fos!=null) try { fos.close(); } catch(Exception e) {}
                }
            } else if(mCurrentMode == MODE_LAUNCHER_PAGE) {
                ImageFile imf = (ImageFile) item;
                Utils.copyFileSafe(buffer, imf.file, tmp_image_file);
                imagePicked(true);
            }
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
        String label;
        String clip_text;
        Object item = adapterView.getItemAtPosition(i);
        if(mCurrentMode == MODE_PATH || mCurrentMode == MODE_LAUNCHER_PAGE) {
            ImageFile f = (ImageFile) item;
            label = f.file.getAbsolutePath();
            clip_text = label;
        } else {
            ImageResource pkg = (ImageResource) item;
            String s = pkg.packageName.equals(ANDROID) ? "android" : pkg.packageName;
            label = s+"/"+pkg.label;
            clip_text = "\""+s+"\", \""+pkg.label+"\"";
        }
        if(Build.VERSION.SDK_INT>=11) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = newPlainText("llx", clip_text);
            clipboard.setPrimaryClip(clip);
        }
        Toast.makeText(this, label, Toast.LENGTH_LONG).show();
        return true;
    }

    @Override
    public void onClick(View view) {
        Intent intent;
        Intent chooser;
        ColorPickerDialog color_picker_dialog;

        mClickedView = view;

        final int id = view.getId();
        switch(id) {
            case R.id.none:
                Utils.getTmpImageFile().delete();
                imagePicked(false);
                break;

            case R.id.ext_file:
                intent = new Intent();
                intent.setType("*/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
//                intent.addCategory(Intent.CATEGORY_OPENABLE);
                chooser = Intent.createChooser(intent, null);
                startActivityForResult(chooser, REQUEST_PICK_IMAGE);
                break;

            case R.id.camera:
                File out = Utils.getTmpImageFile();
                intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(out));
                chooser = Intent.createChooser(intent, null);
                startActivityForResult(chooser, REQUEST_CAPTURE_IMAGE);
                break;

            case R.id.bgcolor:
            case R.id.solid:
                color_picker_dialog = new ColorPickerDialog(this, id ==R.id.bgcolor ? mBackgroundColor : Color.WHITE);
                color_picker_dialog.setAlphaSliderVisible(true);
                color_picker_dialog.setOnColorChangedListener(this);
                color_picker_dialog.show();
                break;

            case R.id.search:
                setSearchMode(true);
                break;

            case R.id.path_up:
                File parent = mCurrentPath.getParentFile();
                if(parent != null) {
                    loadPath(parent);
                }
                break;
        }
    }


    @Override
    public boolean onLongClick(View view) {
        int label_res = 0;
        switch(view.getId()) {
            case R.id.none: label_res = R.string.ip_none; break;
            case R.id.ext_file: label_res = R.string.ip_ext_file; break;
            case R.id.camera: label_res = R.string.ip_camera; break;
            case R.id.bgcolor: label_res = R.string.ip_bgcolor; break;
            case R.id.solid: label_res = R.string.ip_solid; break;
            case R.id.search: label_res = R.string.ip_search; break;
        }
        Toast.makeText(this, label_res, Toast.LENGTH_SHORT).show();
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CAPTURE_IMAGE:
                if(resultCode == RESULT_OK) {
                    imagePicked(true);
                }
                break;

            case REQUEST_PICK_IMAGE:
                if(resultCode == RESULT_OK) {
                    FileOutputStream fos = null;
                    InputStream is = null;
                    try {
                        fos = new FileOutputStream(Utils.getTmpImageFile());
                        is = getContentResolver().openInputStream(data.getData());
                        byte[] buffer = new byte[4096];
                        int n;
                        while((n=is.read(buffer))>0) {
                            fos.write(buffer, 0, n);
                        }
                        imagePicked(true);
                    } catch(IOException e) {
                        // pass
                    } finally {
                        if(fos != null) try { fos.close(); } catch(IOException e) {}
                        if(is != null) try { is.close(); } catch(IOException e) {}
                    }
                }
                break;

            case REQUEST_CROP_IMAGE:
                if(resultCode == RESULT_OK) {
                    setResult(RESULT_OK);
                    finish();
                }
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    public static void startActivity(Activity from, int requestCode) {
        final Intent intent = new Intent(from, ImagePicker.class);
        intent.putExtra(INTENT_EXTRA_CROP, true);
        from.startActivityForResult(intent, requestCode);
    }

    private void setGridViewAdapter(ListAdapter adapter) {
        mNoIconTextView.setVisibility(adapter == null || adapter.getCount() == 0 ? View.VISIBLE : View.GONE);
        mGridView.setAdapter(adapter);
    }

    private void setMode(int mode) {
        boolean icon_pack = false;
        boolean pkg = false;
        boolean path = false;
        boolean launcher_page = false;
        int pos = 0;
        int count;
        switch(mode) {
            case MODE_ICON_PACK:
                icon_pack = true;
                if(mIconPackAdapter == null) {
                    mIconPackAdapter = new PkgLabelAdapter(this, true);
                    mIconPackSpinner.setAdapter(mIconPackAdapter);
                }
                if(mIconPackAdapter.getCount() == 0) {
                    setGridViewAdapter(null);
                }

                count = mIconPackAdapter.getCount();
                for(int i=0; i<count; i++) {
                    if(mIconPackAdapter.getItem(i).pkg.equals(mCurrentIconPack)) {
                        pos = i;
                        break;
                    }
                }
                if(mIconPackSpinner.getSelectedItemPosition() != pos) {
                    mIconPackSpinner.setSelection(pos);
                } else {
                    loadIconPack(mCurrentIconPack);
                }
                break;

            case MODE_PATH:
                path = true;
                loadPath(mCurrentPath);
                break;

            case MODE_LAUNCHER_PAGE:
                launcher_page = true;
                File base_dir = LLApp.get().getAppEngine().getBaseDir();
                ArrayList<String> pages = new ArrayList<String>();
                if(mLauncherPageAdapter == null) {
                    for(int p = Page.FIRST_DASHBOARD_PAGE; p< Page.LAST_DASHBOARD_PAGE; p++) {
                        File f = Page.getIconDir(base_dir, p);
                        if(f.exists()) {
                            pages.add(String.valueOf(p));
                        }
                    }
                    pages.add(String.valueOf(Page.APP_DRAWER_PAGE));
                    for(int p = Page.FIRST_FOLDER_PAGE; p< Page.LAST_FOLDER_PAGE; p++) {
                        File f = Page.getIconDir(base_dir, p);
                        if(f.exists()) {
                            pages.add(String.valueOf(p));
                        }
                    }
                    mLauncherPageAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, pages);
                    mLauncherPageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    mLauncherPageSpinner.setAdapter(mLauncherPageAdapter);
                }

                count = mLauncherPageAdapter.getCount();
                for(int i=0; i<count; i++) {
                    if(mLauncherPageAdapter.getItem(i).equals(mCurrentLauncherPage)) {
                        pos = i;
                        break;
                    }
                }

                if(mLauncherPageSpinner.getSelectedItemPosition() != pos) {
                    mLauncherPageSpinner.setSelection(pos);
                } else {
                    loadLauncherPage(mCurrentLauncherPage);
                }

                break;

            case MODE_PKG:
                pkg = true;
                if(mPkgAdapter == null) {
                    mPkgAdapter = new PkgLabelAdapter(this, false);
                    mPkgSpinner.setAdapter(mPkgAdapter);
                }
                count = mPkgAdapter.getCount();
                for(int i=0; i<count; i++) {
                    if(mPkgAdapter.getItem(i).pkg.equals(mCurrentPkg)) {
                        pos = i;
                        break;
                    }
                }
                if(mPkgSpinner.getSelectedItemPosition() != pos) {
                    mPkgSpinner.setSelection(pos);
                } else {
                    loadPkg(mCurrentPkg);
                }
                break;
        }

        mIconPackSpinner.setVisibility(icon_pack ? View.VISIBLE : View.GONE);
        mPkgSpinner.setVisibility(pkg ? View.VISIBLE : View.GONE);
        mLauncherPageSpinner.setVisibility(launcher_page ? View.VISIBLE : View.GONE);
        mPathTextGrp.setVisibility(path ? View.VISIBLE : View.GONE);

        mCurrentMode = mode;
    }

    private void loadLauncherPage(String p) {
        loadDirectory(Page.getIconDir(LLApp.get().getAppEngine().getBaseDir(), Integer.parseInt(p)), false);
        mCurrentLauncherPage = p;

    }

    private void loadPath(File path) {
        loadDirectory(path, true);
        mCurrentPath = path;
    }

    private void loadDirectory(File path, boolean for_path) {
        mSearchText.setText("");

        ArrayList<ImageFile> images = new ArrayList<ImageFile>();

        File[] files = path.listFiles();

        if(files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    images.add(new ImageFile(f));
                } else {
                    BitmapFactory.Options o = new BitmapFactory.Options();
                    o.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(f.getAbsolutePath(), o);
                    if (o.outWidth > 0) {
                        images.add(new ImageFile(f));
                    }
                }
            }
            Collections.sort(images, new Comparator<ImageFile>() {
                @Override
                public int compare(ImageFile if1, ImageFile if2) {
                    File f1 = if1.file;
                    File f2 = if2.file;
                    boolean d1 = f1.isDirectory();
                    boolean d2 = f2.isDirectory();
                    if (d1 && d2) {
                        return Utils.sItemNameCollator.compare(f1.getName(), f2.getName());
                    } else if (!d1 && !d2) {
                        return Utils.sItemNameCollator.compare(f1.getName(), f2.getName());
                    } else {
                        if (d1 && !d2) {
                            return -1;
                        } else if (d2 && !d1) {
                            return 1;
                        } else {
                            return 0;
                        }
                    }
                }
            });
        }

        storeGridViewScroll();
        ImageFileAdapter adapter = new ImageFileAdapter(this, 0, images, true/*for_path*/);
        setGridViewAdapter(adapter);
        setGridViewScroll(for_path ? mCurrentScrollPath : mCurrentScrollLauncherPage);
        mPathTextView.setText(path.getAbsolutePath());
    }

    private void loadIconPack(String icon_pack) {
        ArrayList<ImageResource> pkgs = new ArrayList<ImageResource>();
        try {
            Context remoteContext = createPackageContext(icon_pack, 0);
            Resources rsrc = remoteContext.getResources();
            int list = rsrc.getIdentifier("icon_pack", "array", icon_pack);
            final String[] extras = rsrc.getStringArray(list);
            for (String extra : extras) {
                int res = rsrc.getIdentifier(extra, "drawable", icon_pack);
                if (res != 0) {
                    ImageResource ir = new ImageResource();
                    ir.packageName = icon_pack;
                    ir.label = rsrc.getResourceEntryName(res);
                    ir.res = res;
                    pkgs.add(ir);
                }
            }
        } catch (Exception e) {
            loadPkg(icon_pack);
            return;
        }
        storeGridViewScroll();
        ImageResourceAdapter adapter = new ImageResourceAdapter(this, 0, pkgs, true);
        setGridViewAdapter(adapter);
        setGridViewScroll(mCurrentScrollIconPack);
        mCurrentIconPack = icon_pack;
    }

    private void loadPkg(String packageName) {
        try {
            boolean isAndroidPackage = packageName.equals(ANDROID);
            Context remoteContext = isAndroidPackage ? this : createPackageContext(packageName, 0);

            ArrayList<ImageResource> pkgs = new ArrayList<ImageResource>();
            Resources rsrc = remoteContext.getResources();
            int start = isAndroidPackage ? 0x01080000 : 0x7f020000;
            int end = start+0x1000;
            for(int i=start; i<end; i++) {

                try {
                    String label = rsrc.getResourceEntryName(i);
                    ImageResource ir = new ImageResource();
                    ir.packageName = packageName;
                    ir.label = label;
                    ir.res = i;
                    pkgs.add(ir);
                } catch(Resources.NotFoundException e) {
                    break;
                }
            }

            storeGridViewScroll();
            ImageResourceAdapter adapter = new ImageResourceAdapter(this, 0, pkgs, true);
            setGridViewAdapter(adapter);
            setGridViewScroll(mCurrentScrollPkg);
            mCurrentPkg = packageName;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void storeGridViewScroll() {
        if(mGridMode != MODE_NONE) {
            int offset = mStandardIconSize / 3;
            int index = mCurrentScrollIndex;//mGridView.getFirstVisiblePosition();
            final View first = mGridView.getChildAt(0);
            if (null != first) {
                offset -= first.getTop();
            }

            int y = (index<<16) | offset;

            switch (mGridMode) {
                case MODE_PKG: mCurrentScrollPkg = y; break;
                case MODE_PATH: mCurrentScrollPath = y; break;
                case MODE_ICON_PACK: mCurrentScrollIconPack = y; break;
                case MODE_LAUNCHER_PAGE: mCurrentScrollLauncherPage = y; break;
            }
        }
    }

    private void setGridViewScroll(int scroll) {
        if(mGridMode == mCurrentMode) {
            mGridView.setSelection(0);
        } else {
            mGridMode = mCurrentMode;
            mGridView.setSelection(scroll >> 16);
//            mGridView.scrollBy(0, scroll&0xffff);
        }
    }

    @Override
    public void onColorChanged(int color) {
        switch(mClickedView.getId()) {
            case R.id.solid:
                Bitmap b = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
                b.eraseColor(color);
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(Utils.getTmpImageFile());
                    b.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    imagePicked(false);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } finally {
                    if(fos != null) try { fos.close(); } catch(IOException e) {}
                }


                break;

            case R.id.bgcolor:
                mBackgroundColor = color;
                mGridView.setBackgroundColor(color);
                break;
        }

    }

    @Override
    public void onColorDialogSelected(int color) {
        // pass
    }

    @Override
    public void onColorDialogCanceled() {
        // pass
    }

    private void imagePicked(boolean allow_crop) {
//        if(getIntent().getBooleanExtra(INTENT_EXTRA_CROP, false) && allow_crop) {
        File file = Utils.getTmpImageFile();
        if(allow_crop) {
            if(Utils.isSvgFile(file)) {
                allow_crop = false;
            }
        }

        if(allow_crop) {
            ImageCropper.startActivity(this, file, REQUEST_CROP_IMAGE);
        } else {
            setResult(RESULT_OK);
            finish();
        }
    }

    private void setSearchMode(boolean on) {
        InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
        View src_grp = findViewById(R.id.src_grp);
        if(on) {
            mSearchText.setVisibility(View.VISIBLE);
            mSearchText.setText("");
            mSearchText.requestFocus();
            mButtonBar.setVisibility(View.INVISIBLE);
            src_grp.setVisibility(View.GONE);
            imm.showSoftInput(mSearchText, 0);
        } else {
            mSearchText.setText("");
            mSearchText.setVisibility(View.INVISIBLE);
            mButtonBar.setVisibility(View.VISIBLE);
            src_grp.setVisibility(View.VISIBLE);
            imm.hideSoftInputFromWindow(mSearchText.getWindowToken(), 0);
        }
    }

    @Override
    public void onEditTextImeBackPressed() {
        setSearchMode(false);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        ArrayAdapter<?> adapter = (ArrayAdapter<?>) mGridView.getAdapter();
        if(adapter != null) {
            adapter.getFilter().filter(s.toString());
        }
    }

    @Override
    public void afterTextChanged(Editable s) {

    }

    private static class PkgLabel {
        String pkg;
        String label;

        private PkgLabel(String pkg, String label) {
            this.pkg = pkg;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private abstract class MyFilterArrayAdapter<T> extends ArrayAdapter<T> {
        private Filter mFilter;
        private List<T> mOriginalItems;
        private List<T> mItems;
        private final Object mLock = new Object();


        public MyFilterArrayAdapter(Context context, int resource) {
            super(context, resource);
        }

        public void setItems(List<T> items) {
            mItems = items;
        }

        @Override
        public int getCount() {
            return mItems.size();
        }

        @Override
        public T getItem(int position) {
            return mItems.get(position);
        }

        @Override
        public int getPosition(T item) {
            return mItems.indexOf(item);
        }

        @Override
        public Filter getFilter() {
            if (mFilter == null) {
                mFilter = new Filter() {
                    @Override
                    protected Filter.FilterResults performFiltering(CharSequence prefix) {
                        Filter.FilterResults results = new Filter.FilterResults();

                        if (mOriginalItems == null) {
                            synchronized (mLock) {
                                mOriginalItems = new ArrayList<T>(mItems);
                            }
                        }

                        if (prefix == null || prefix.length() == 0) {
                            ArrayList<T> list;
                            synchronized (mLock) {
                                list = new ArrayList<T>(mOriginalItems);
                            }
                            results.values = list;
                            results.count = list.size();
                        } else {
                            String prefixString = prefix.toString().toLowerCase();

                            ArrayList<T> values;
                            synchronized (mLock) {
                                values = new ArrayList<T>(mOriginalItems);
                            }

                            final int count = values.size();
                            final ArrayList<T> newValues = new ArrayList<T>();

                            for (int i = 0; i < count; i++) {
                                final T value = values.get(i);
                                final String valueText = value.toString().toLowerCase();

                                if (valueText.contains(prefixString)) {
                                    newValues.add(value);
                                }
                            }
                            results.values = newValues;
                            results.count = newValues.size();
                        }

                        return results;
                    }

                    @Override
                    protected void publishResults(CharSequence constraint, Filter.FilterResults results) {
                        //noinspection unchecked
                        mItems = (List<T>) results.values;
                        if (results.count > 0) {
                            notifyDataSetChanged();
                        } else {
                            notifyDataSetInvalidated();
                        }
                    }
                };
            }
            return mFilter;
        }
    }




    private class PkgLabelAdapter extends MyFilterArrayAdapter<PkgLabel> {

        public PkgLabelAdapter(Context context, boolean icon) {
            super(context, 0);

            PackageManager pm = getPackageManager();

            ArrayList<PkgLabel> items = new ArrayList<PkgLabel>();
            if(icon) {
                String myPackagename = getPackageName();
                Intent filter = new Intent("org.adw.launcher.icons.ACTION_PICK_ICON");
                List<ResolveInfo> ris = pm.queryIntentActivities(filter, 0);
                for (ResolveInfo ri : ris) {
                    String packageName = ri.activityInfo.packageName;
                    if(!myPackagename.equals(packageName)) {
                        PkgLabel pkg = new PkgLabel(packageName, ri.loadLabel(pm).toString());
                        items.add(pkg);
                    }
                }
            } else {
                List<PackageInfo> pis = pm.getInstalledPackages(0);
                for(PackageInfo pi : pis) {
                    PkgLabel pkg = new PkgLabel(pi.packageName, pi.applicationInfo.loadLabel(pm).toString());
                    items.add(pkg);
                }
            }
            Collections.sort(items, new Comparator<PkgLabel>() {
                @Override
                public int compare(PkgLabel pkg1, PkgLabel pkg2) {
                    return Utils.sItemNameCollator.compare(pkg1.label, pkg2.label);
                }
            });
            if(!icon) {
                PkgLabel android_pkg = new PkgLabel(ANDROID, "Android");
                items.add(0, android_pkg);
            }

            setItems(items);
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView == null) {
                convertView = getLayoutInflater().inflate(android.R.layout.simple_spinner_item, null);
            }

            ((TextView)convertView.findViewById(android.R.id.text1)).setText(getItem(position).label);

            return convertView;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            if(convertView == null) {
                convertView = getLayoutInflater().inflate(android.R.layout.simple_spinner_dropdown_item, parent, false);
            }

            ((TextView)convertView.findViewById(android.R.id.text1)).setText(getItem(position).label);

            return convertView;
        }
    }

    private static class ImageResource {
        int res;
        String packageName;
        String label;

        @Override
        public String toString() {
            return label;
        }
    }


    private class ImageResourceAdapter extends MyFilterArrayAdapter<ImageResource> {

        private boolean mDisplayLabels;

        public ImageResourceAdapter(Context context, int resource, List<ImageResource> objects, boolean display_labels) {
            super(context, resource);

            mDisplayLabels = display_labels;

            setItems(objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.item, null);
            }

            final ImageResource ir = getItem(position);

            final TextView title = (TextView) convertView.findViewById(R.id.label);
            final ImageView thumbnail = (ImageView) convertView.findViewById(R.id.icon);

            if(mDisplayLabels) {
                title.setText(ir.label);
                title.setVisibility(View.VISIBLE);
            } else {
                title.setVisibility(View.GONE);
            }
            BitmapInfo bitmap_info = mThumbnailCache.get(ir.packageName+ir.res);
            if(bitmap_info != null) {
                Bitmap bitmap = bitmap_info.bitmap;
                thumbnail.setImageBitmap(bitmap);
                title.setTypeface(null, bitmap_info.isNinePatch ? Typeface.BOLD_ITALIC : Typeface.NORMAL);
                convertView.setTag(null);
            } else {
                thumbnail.setTag(ir);
                thumbnail.setImageBitmap(null);
                new AsyncTask<ImageResource,Void,Bitmap>() {
                    private ImageResource mPackage;
                    private boolean mIsNinePatch;

                    @Override
                    protected Bitmap doInBackground(ImageResource... params) {
                        Bitmap bmp = null;
                        mPackage = params[0];
                        try {
                            int id = mPackage.res;
                            String pkg_name = ir.packageName;
                            Resources rsrc = pkg_name.equals(ANDROID) ? getResources() : createPackageContext(pkg_name, 0).getResources();
                            mIsNinePatch = false;
                            if(Utils.sGetDrawableForDensity!=null) {
                                try {
                                    BitmapDrawable d=(BitmapDrawable) Utils.sGetDrawableForDensity.invoke(rsrc, id, mLauncherIconDensity);
                                    Bitmap orig_bmp = d.getBitmap();
                                    mIsNinePatch = NinePatch.isNinePatchChunk(orig_bmp.getNinePatchChunk());
                                    bmp = Utils.createStandardSizedIcon(orig_bmp, mStandardIconSize);
                                } catch(Throwable e) {
                                    // pass, continue with classic method
                                }
                            }
                            if(bmp == null) {
                                bmp = BitmapFactory.decodeResource(rsrc, id);
                                if(bmp != null) {
                                    mIsNinePatch = NinePatch.isNinePatchChunk(bmp.getNinePatchChunk());
                                }
                            }
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                        if(bmp == null) {
                            bmp = mDefaultIcon;
                        }

                        mThumbnailCache.put(ir.packageName+ir.res, new BitmapInfo(bmp, mIsNinePatch));
                        return bmp;
                    }

                    @Override
                    protected void onPostExecute(Bitmap bmp) {
                        if(thumbnail.getTag() == mPackage) {
                            thumbnail.setImageBitmap(bmp);
                            title.setTypeface(null, mIsNinePatch ? Typeface.BOLD_ITALIC : Typeface.NORMAL);
                        }
                    }
                }.execute(ir);
            }

            return convertView;
        }
    }

    private static class ImageFile {
        File file;

        private ImageFile(File file) {
            this.file = file;
        }

        @Override
        public String toString() {
            return file.getName();
        }
    }

    private class ImageFileAdapter extends MyFilterArrayAdapter<ImageFile> {

        private boolean mDisplayLabels;
        private Bitmap mFolderIcon;

        public ImageFileAdapter(Context context, int resource, List<ImageFile> objects, boolean display_labels) {
            super(context, resource);

            mDisplayLabels = display_labels;

            int[] textSizeAttr = new int[] { android.R.attr.colorForeground };
            TypedArray a = context.obtainStyledAttributes(textSizeAttr);
            mFolderIcon = Utils.createIconFromText(Utils.getStandardIconSize(), "f", a.getColor(0, 0));

            setItems(objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.item, null);
            }

            final ImageFile imf = getItem(position);

            final TextView title = (TextView) convertView.findViewById(R.id.label);
            final ImageView thumbnail = (ImageView) convertView.findViewById(R.id.icon);

            if(mDisplayLabels) {
                title.setText(imf.file.getName());
                title.setVisibility(View.VISIBLE);
            } else {
                title.setVisibility(View.GONE);
            }
            if(imf.file.isDirectory()) {
                thumbnail.setImageBitmap(mFolderIcon);
            } else {
                BitmapInfo bitmap_info = mThumbnailCache.get(imf.file.getAbsolutePath());
                if (bitmap_info != null) {
                    Bitmap bitmap = bitmap_info.bitmap;
                    thumbnail.setImageBitmap(bitmap);
                    title.setTypeface(null, bitmap_info.isNinePatch ? Typeface.BOLD_ITALIC : Typeface.NORMAL);
                    convertView.setTag(null);
                } else {
                    thumbnail.setTag(imf);
                    thumbnail.setImageBitmap(null);
                    new AsyncTask<ImageFile, Void, Bitmap>() {
                        private boolean mIsNinePatch;

                        @Override
                        protected Bitmap doInBackground(ImageFile... params) {
                            Bitmap bmp = null;
                            mIsNinePatch = false;
                            try {
                                Bitmap orig_bmp = BitmapFactory.decodeStream(getContentResolver().openInputStream(Uri.fromFile(imf.file)));
                                mIsNinePatch = NinePatch.isNinePatchChunk(orig_bmp.getNinePatchChunk());
                                bmp = Utils.createStandardSizedIcon(orig_bmp, mStandardIconSize);
                            } catch (Throwable e) {
                                e.printStackTrace();
                            }
                            if (bmp == null) {
                                bmp = mDefaultIcon;
                            }

                            mThumbnailCache.put(imf.file.getAbsolutePath(), new BitmapInfo(bmp, mIsNinePatch));
                            return bmp;
                        }

                        @Override
                        protected void onPostExecute(Bitmap bmp) {
                            if (thumbnail.getTag() == imf) {
                                thumbnail.setImageBitmap(bmp);
                                title.setTypeface(null, mIsNinePatch ? Typeface.BOLD_ITALIC : Typeface.NORMAL);
                            }
                        }
                    }.execute();
                }
            }

            return convertView;
        }

//        public boolean getDisplayLabels() {
//            return mDisplayLabels;
//        }
    }

    private static class TextFilter extends Filter {

        TextFilter() {

        }

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            return null;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {

        }
    }
}
