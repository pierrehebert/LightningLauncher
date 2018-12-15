package net.pierrox.lightning_launcher.activities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import net.pierrox.lightning_launcher.API;
import net.pierrox.lightning_launcher.LLApp;
import net.pierrox.lightning_launcher.configuration.GlobalConfig;
import net.pierrox.lightning_launcher.data.BackupRestoreTool;
import net.pierrox.lightning_launcher.data.FileUtils;
import net.pierrox.lightning_launcher.data.Item;
import net.pierrox.lightning_launcher.data.Page;
import net.pierrox.lightning_launcher.engine.LightningEngine;
import net.pierrox.lightning_launcher.data.PageProcessor;
import net.pierrox.lightning_launcher.data.Utils;
import net.pierrox.lightning_launcher.data.Widget;
import net.pierrox.lightning_launcher.script.ScriptManager;
import net.pierrox.lightning_launcher.script.Script;
import net.pierrox.lightning_launcher.template.LLTemplateAPI;
import net.pierrox.lightning_launcher_extreme.R;

import org.json.JSONException;
import org.json.JSONObject;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.Toast;

public class ApplyTemplate extends ResourceWrapperActivity {
    private static final int DIALOG_CONFIRM_APPLY = 1;
    private static final int DIALOG_NEED_UPGRADE = 2;
    private static final int DIALOG_WARNING = 3;

    private static final int REQUEST_BIND_APPWIDGET = 1;
    private static final int REQUEST_SELECT_SCREEN_FOR_GOTO = 2;

    /*package*/ static final String INTENT_EXTRA_PATH = "p";
    /*package*/ static final String INTENT_EXTRA_URI = "u";

    private enum ApplyMode {
    	REPLACE,
    	MERGE,
    	UPDATE
    }
    
    private ComponentName mTemplateCN;
    private String mTemplateDisplayName;
    private File mTemplateFile;
    private Uri mTemplateUri;

    private boolean mBackupFirst;
    private ApplyMode mApplyMode = ApplyMode.MERGE;
    private boolean mLoadWallpaper;

    private int mLLVersionFrom;

    private boolean mWarningFreeVersion;
    private boolean mWarningScreen;
    private boolean mWarningWidget;

    private SparseArray<ComponentName> mAppWidgetsToBind;
    private ParcelableSparseIntArray mNewAppWidgetIds;

    private int mBindWidgetOldId;
    private int mBindWidgetNewId;
    private int mFromScreenDpi;
    private int mFromScreenWidth;
    private int mFromScreenHeight;
    private int mFromStatusBarHeight;

    private File mAppBaseDir;
    
    private ArrayList<Page> mImportedPages;
    
    private static Method sBindAppWidgetIdIfAllowed;

    static {
        try {
            sBindAppWidgetIdIfAllowed = AppWidgetManager.class.getMethod("bindAppWidgetIdIfAllowed", int.class, ComponentName.class);
        } catch (NoSuchMethodException e) {
        }
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.setTheme(this, Utils.APP_THEME);

        super.onCreate(savedInstanceState);
        
        mAppBaseDir = LLApp.get().getAppEngine().getBaseDir();

        if(getIntent().hasExtra(INTENT_EXTRA_PATH)) {
            mTemplateFile = new File(getIntent().getStringExtra(INTENT_EXTRA_PATH));
            mTemplateDisplayName = mTemplateFile.getName();
        } else if(getIntent().hasExtra(INTENT_EXTRA_URI)) {
            mTemplateUri = getIntent().getParcelableExtra(INTENT_EXTRA_URI);
            mTemplateDisplayName = mTemplateUri.getLastPathSegment();
        } else {
            mTemplateCN = getIntent().getParcelableExtra(LLTemplateAPI.INTENT_TEMPLATE_COMPONENT_NAME);
            if(mTemplateCN == null) {
                finish();
                return;
            }

            PackageManager pm = getPackageManager();
            Intent filter = new Intent();
            filter.setComponent(mTemplateCN);
            List<ResolveInfo> ris = pm.queryIntentActivities(filter, 0);
            if(ris.size() != 1) {
                finish();
                return;
            }

            mTemplateDisplayName = ris.get(0).activityInfo.loadLabel(pm).toString();
        }

        if(savedInstanceState != null) {
            mFromScreenDpi = savedInstanceState.getInt("sd");
            mFromScreenWidth = savedInstanceState.getInt("sw");
            mFromScreenHeight = savedInstanceState.getInt("sh");
            mFromStatusBarHeight = savedInstanceState.getInt("sbh");
            mTemplateDisplayName = savedInstanceState.getString("tn");
            mTemplateUri = savedInstanceState.getParcelable("tu");
            mWarningFreeVersion = savedInstanceState.getBoolean("wx");
            mWarningScreen = savedInstanceState.getBoolean("ws");
            mWarningWidget = savedInstanceState.getBoolean("ww");
            
            mAppWidgetsToBind = savedInstanceState.getSparseParcelableArray("wb");
            mNewAppWidgetIds = savedInstanceState.getParcelable("nw");
            mBindWidgetOldId = savedInstanceState.getInt("oi");
            mBindWidgetNewId = savedInstanceState.getInt("ni");
            int[] pages_id = savedInstanceState.getIntArray("ip");
            if(pages_id != null) {
            	mImportedPages = new ArrayList<Page>();
            	LightningEngine engine = LLApp.get().getEngine(getExtractedTemplateDir(), true);
            	for(int p : pages_id) {
            		mImportedPages.add(engine.getOrLoadPage(p));
            	}
            }
            mApplyMode = ApplyMode.values()[savedInstanceState.getInt("am")];
        }

        if(checkPermissions(
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                new int[] { R.string.pr_r6, R.string.pr_r7},
                REQUEST_PERMISSION_BASE)) {
            showDialog(DIALOG_CONFIRM_APPLY);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if(areAllPermissionsGranted(grantResults, R.string.pr_f4)) {
            showDialog(DIALOG_CONFIRM_APPLY);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("sd", mFromScreenDpi);
        outState.putInt("sw", mFromScreenWidth);
        outState.putInt("sh", mFromScreenHeight);
        outState.putInt("sbh", mFromStatusBarHeight);
        outState.putString("tn", mTemplateDisplayName);
        outState.putParcelable("tu", mTemplateUri);
        outState.putBoolean("wx", mWarningFreeVersion);
        outState.putBoolean("ws", mWarningScreen);
        outState.putBoolean("ww", mWarningWidget);
        
        outState.putSparseParcelableArray("wb", mAppWidgetsToBind);
        outState.putParcelable("nw", mNewAppWidgetIds);
        outState.putInt("oi", mBindWidgetOldId);
        outState.putInt("ni", mBindWidgetNewId);
        if(mImportedPages != null) {
        	int l = mImportedPages.size();
        	int[] pages_id = new int[l];
        	for(int i=0; i<l; i++) {
        		pages_id[i] = mImportedPages.get(i).id;
        	}
        	outState.putIntArray("ip", pages_id);
        }
        outState.putInt("am", mApplyMode.ordinal());
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        AlertDialog.Builder builder;

        switch(id) {
            case DIALOG_CONFIRM_APPLY:
                builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.apply_tmpl_t);
                final View content = getLayoutInflater().inflate(R.layout.dialog_apply_tmpl, null);
                final RadioButton rb_replace = (RadioButton) content.findViewById(R.id.tmpl_r);
                final RadioButton rb_merge = (RadioButton) content.findViewById(R.id.tmpl_m);
                final CheckBox cb_backup = (CheckBox) content.findViewById(R.id.tmpl_b);
                final CheckBox cb_wallpaper = (CheckBox) content.findViewById(R.id.tmpl_w);

                rb_replace.setText(R.string.tmpl_r);
                rb_merge.setText(R.string.tmpl_m);
                cb_backup.setText(R.string.tmpl_b);
                cb_wallpaper.setText(R.string.tmpl_w);

                builder.setView(content);
                builder.setMessage(getString(R.string.apply_tmpl_m, mTemplateDisplayName));
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if(rb_replace.isChecked()) {
                    		mApplyMode = ApplyMode.REPLACE;
                    	} else {
                            if(rb_merge.isChecked()) {
                                mApplyMode = ApplyMode.MERGE;
                            } else {
                                mApplyMode = ApplyMode.UPDATE;
                            }
                        }
                        mBackupFirst = cb_backup.isChecked();
                        mLoadWallpaper = cb_wallpaper.isChecked();
                        checkThenApplyTemplate();
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                });
                return builder.create();

            case DIALOG_NEED_UPGRADE:
                builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.tmpl_check_ut);
                builder.setMessage(R.string.tmpl_check_um);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Utils.startAppStore(ApplyTemplate.this, getPackageName());
                        finish();
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                });
                return builder.create();

            case DIALOG_WARNING:
                builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.tmpl_warn_t);
                String msg = getString(R.string.tmpl_warn_m);

                if(mWarningFreeVersion) {
                    msg += "\n\n" + getString(R.string.tmpl_warn_llx);
                }
                if(mWarningScreen) {
                    msg += "\n\n" + getString(R.string.tmpl_warn_screen);
                }
                if(mWarningWidget) {
                    msg += "\n\n" + getString(R.string.tmpl_warn_widget);
                }
                builder.setMessage(msg);
                builder.setPositiveButton(R.string.tmpl_warn_c, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        applyTemplate();
                    }
                });
                if(mWarningFreeVersion) {
                    builder.setNeutralButton(R.string.tmpl_get_llx, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Utils.startAppStore(ApplyTemplate.this, getPackageName());
                            finish();
                        }
                    });
                }
                builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                });
                return builder.create();

        }
        return super.onCreateDialog(id);
    }

    private void checkThenApplyTemplate() {
        new AsyncTask<Void, Void, JSONObject>() {
            private ProgressDialog mDialog;

            @Override
            protected void onPreExecute() {
                mDialog = new ProgressDialog(ApplyTemplate.this);
                mDialog.setMessage(getString(R.string.tmpl_check));
                mDialog.setCancelable(false);
                mDialog.show();
            }

            @Override
            protected JSONObject doInBackground(Void... voids) {
                InputStream is = openTemplateStream();

                if(is != null) {
                    JSONObject manifest = BackupRestoreTool.readManifest(is);
                    try { is.close(); } catch(IOException e) {}
                    return manifest;
                } else {
                    return null;
                }
            }

            @Override
            protected void onPostExecute(JSONObject manifest) {
                mDialog.dismiss();
                if(manifest == null) {
                    Toast.makeText(ApplyTemplate.this, R.string.tmpl_check_e, Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    try {
                        mLLVersionFrom = Utils.getVersionCode(manifest.getInt(BackupRestoreTool.MANIFEST_LL_VERSION));
                        if(mLLVersionFrom > Utils.getMyPackageVersion(ApplyTemplate.this)) {
                            showDialog(DIALOG_NEED_UPGRADE);
                            return;
                        }

                        mWarningFreeVersion = LLApp.get().isFreeVersion();

                        DisplayMetrics dm = getResources().getDisplayMetrics();
                        mFromScreenDpi = manifest.getInt(BackupRestoreTool.MANIFEST_SCREEN_DENSITY);
                        mFromScreenWidth = manifest.getInt(BackupRestoreTool.MANIFEST_SCREEN_WIDTH);
                        mFromScreenHeight = manifest.getInt(BackupRestoreTool.MANIFEST_SCREEN_HEIGHT);
                        mFromStatusBarHeight = manifest.getInt(BackupRestoreTool.MANIFEST_SB_HEIGHT);
                        int screenDensity = manifest.getInt(BackupRestoreTool.MANIFEST_SCREEN_DENSITY);
                        mWarningScreen = (mFromScreenWidth!=dm.widthPixels || mFromScreenHeight!=dm.heightPixels || screenDensity!=dm.densityDpi);

                        mWarningWidget = sBindAppWidgetIdIfAllowed==null;

                        if(mWarningFreeVersion || mWarningScreen || mWarningWidget) {
                            showDialog(DIALOG_WARNING);
                        } else {
                            applyTemplate();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.execute((Void)null);
    }

    private ProgressDialog mDialog;

    private File getExtractedTemplateDir() {
        String tempTemplatePath = FileUtils.LL_TMP_DIR+"/template";
        return new File(tempTemplatePath);
    }

    private void applyTemplate() {
        new AsyncTask<Void, Boolean, Integer>() {

        	private File mTemplateDir;
        	private LightningEngine mEngineFrom;
        	private LightningEngine mEngineTo;
        	private SparseIntArray mTranslatedPageIds;
        	private SparseIntArray mTranslatedScriptIds;
        	
            @Override
            protected void onPreExecute() {
            	mTemplateDir = getExtractedTemplateDir();
            	mEngineFrom = LLApp.get().getEngine(mTemplateDir, false);
            	mEngineTo = LLApp.get().getAppEngine();
            	mImportedPages = new ArrayList<>();
            	mTranslatedPageIds = null;
            	mTranslatedScriptIds = null;
            	
                mDialog = new ProgressDialog(ApplyTemplate.this);
                mDialog.setMessage(getString(mBackupFirst ? R.string.backup_in_progress : R.string.apply_tmpl_g));
                mDialog.setCancelable(false);
                mDialog.show();
            }

            @Override
            protected Integer doInBackground(Void... voids) {
                Context context = ApplyTemplate.this;
                if(mBackupFirst) {
                    SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH-mm");
                    String backup_name=sdf.format(new Date());
                    String backup_path= FileUtils.LL_EXT_DIR +"/"+getString(R.string.backup_d)+"-"+backup_name;

                    BackupRestoreTool.BackupConfig backup_config=new BackupRestoreTool.BackupConfig();

                    backup_config.context=context;
                    backup_config.pathFrom=mAppBaseDir.getAbsolutePath();
                    backup_config.pathTo=backup_path;
                    backup_config.includeWidgetsData=true;
                    backup_config.includeWallpaper=true;
                    backup_config.includeFonts=true;


                    Exception exception = BackupRestoreTool.backup(backup_config);
                    if(exception != null) {
                        exception.printStackTrace();
                        return 1;
                    }

                    publishProgress(true);
                }

                BackupRestoreTool.RestoreConfig restore_config=new BackupRestoreTool.RestoreConfig();

                restore_config.context=context;
                restore_config.pathTo=mApplyMode==ApplyMode.REPLACE ? mAppBaseDir.getAbsolutePath() : mTemplateDir.getAbsolutePath();
                restore_config.isFrom = openTemplateStream();
                restore_config.restoreWidgetsData=true;
                restore_config.restoreWallpaper=mLoadWallpaper;
                restore_config.restoreFonts=true;

                if(!BackupRestoreTool.restore(restore_config)) {
                	Utils.deleteDirectory(mTemplateDir, true);
                    return 2;
                }

                mEngineFrom.init();
                
                if(mApplyMode == ApplyMode.MERGE) {

                    // build the list of desktops to import recursively
                    ArrayList<Page> desktopPages = new ArrayList<Page>();
                    for (int p : mEngineFrom.getPageManager().getAllPagesIds()) {
                        if(Page.isDashboard(p) && p != Page.APP_DRAWER_PAGE) {
                            // skip folders and the app drawer pages when merging
                            desktopPages.add(mEngineFrom.getOrLoadPage(p));
                        }
                    }

                    // do the grunt work of importing
                    mTranslatedPageIds = mEngineTo.getPageManager().clonePages(desktopPages, true);
                    for(int i=0; i<mTranslatedPageIds.size(); i++) {
                        mImportedPages.add(mEngineTo.getOrLoadPage(mTranslatedPageIds.valueAt(i)));
                    }

	                
	                // update the list of desktops in the global config of the target engine
	                GlobalConfig template_gc = mEngineFrom.getGlobalConfig();

	                GlobalConfig app_gc = mEngineTo.getGlobalConfig();
	                int l = app_gc.screensOrder.length;
	                int n = l+desktopPages.size();
	                int[] new_screens_order = new int[n];
	                String[] new_screens_names = new String[n];
	                System.arraycopy(app_gc.screensOrder, 0, new_screens_order, 0, l);
	                System.arraycopy(app_gc.screensNames, 0, new_screens_names, 0, l);
	                int i=l;
	                for(Page page : desktopPages) {
	                	int old_page_id = page.id;
                        new_screens_names[i] = template_gc.screensNames[template_gc.getPageIndex(old_page_id)];
                        new_screens_order[i] = mTranslatedPageIds.get(old_page_id);
                        i++;
	                }
	                app_gc.screensNames = new_screens_names;
	                app_gc.screensOrder = new_screens_order;
	                mEngineTo.notifyGlobalConfigChanged();

                    // copy font files
                    File[] fonts = FileUtils.getFontsDir(mEngineFrom.getBaseDir()).listFiles();
                    File toDir = FileUtils.getFontsDir(mEngineTo.getBaseDir());
                    toDir.mkdirs();

                    byte[] buffer = new byte[1024];
                    for (File fromFont : fonts) {
                        File toFont = new File(toDir, fromFont.getName());
                        Utils.copyFileSafe(buffer, fromFont, toFont);
                    }

                    // translate scripts
	                
                    ScriptManager scriptManagerTo = mEngineTo.getScriptManager();
                    ArrayList<Script> my_scripts = scriptManagerTo.getAllScriptMatching(Script.FLAG_ALL);
	                mTranslatedScriptIds = new SparseIntArray();
                    ScriptManager scriptManagerFrom = mEngineFrom.getScriptManager();
                    ArrayList<Script> scriptsToImport = scriptManagerFrom.getAllScriptMatching(Script.FLAG_ALL);
                    String relativePath;
                    if(mTemplateFile != null) {
                        relativePath = mTemplateFile.getName();
                    } else if(mTemplateUri != null) {
                        relativePath = mTemplateUri.getLastPathSegment();
                    } else if(mTemplateCN != null) {
                        relativePath = "/" + mTemplateCN.getPackageName().replace('.', '/');
                    } else {
                        relativePath = "/";
                    }
                	for(Script script : scriptsToImport) {
                		Script matching_script = null;
                		for(Script my_script : my_scripts) {
                			if(my_script.name.equals(script.name) && my_script.getSourceText().equals(script.getSourceText())) {
                				matching_script = my_script;
                				break;
                			}
                		}
                		
                		int old_id = script.id;
                		int new_id;
                		if(matching_script != null) {
                			// reuse existing identical script
                			new_id = matching_script.id;
                		} else {
                			// copy script
                            Script importedScript = scriptManagerTo.importScript(script);
                            if(importedScript.getRelativePath().equals("/")) {
                                importedScript.setRelativePath(relativePath);
                            }
                            new_id = importedScript.id;
                		}
                		
                		mTranslatedScriptIds.put(old_id, new_id);
	                }

	                Utils.deleteDirectory(mTemplateDir, true);
                } else if(mApplyMode == ApplyMode.REPLACE) {
                    for (int p : mEngineTo.getPageManager().getAllPagesIds()) {
                        mImportedPages.add(mEngineTo.getOrLoadPage(p));
                    }
                }

                // build list of widgets to bind
                mAppWidgetsToBind = new SparseArray<>();
                for(Page page : mImportedPages) {
                	for(Item i : page.items) {
                		if(i.getClass() == Widget.class) {
                            Widget w = (Widget)i;
                            ComponentName cn = w.getComponentName();
                            int id = w.getAppWidgetId();
                            if(cn != null && id != Widget.NO_APP_WIDGET_ID) {
                                mAppWidgetsToBind.put(id, cn);
                            }
                        }
                	}
                }
                
                // replace package name and scale positions
                Resources resources = getResources();
                DisplayMetrics dm = resources.getDisplayMetrics();
                Rect r = new Rect();
                getWindow().getDecorView().getWindowVisibleDisplayFrame(r);
                int statusbar_height = r.top;

                PageProcessor page_processor = new PageProcessor();
                page_processor.setTranslatedPageIds(mTranslatedPageIds);
                page_processor.setTranslatedScriptIds(mTranslatedScriptIds);
                page_processor.setScreenCharacteristics(mFromScreenWidth, mFromScreenHeight, mFromStatusBarHeight, dm.widthPixels, dm.heightPixels, statusbar_height);
                page_processor.setEnforceHoloSelection(mLLVersionFrom < 230);
                page_processor.setProcessGlobalConfig(true);
                page_processor.postProcessPages(mImportedPages);

                // reload app drawer and clear state file
//                boolean is_llx = getPackageName().equals(LLApp.LLX_PKG_NAME);
//                if(!is_llx) {
                if(mApplyMode == ApplyMode.REPLACE) {
            		Page app_drawer = LLApp.get().getAppEngine().getOrLoadPage(Page.APP_DRAWER_PAGE);
            		app_drawer.items=Utils.loadAppDrawerShortcuts(app_drawer);
                    Utils.layoutItemsInTable(app_drawer.config, app_drawer.items, true);
            		app_drawer.setModified();
            		app_drawer.save();

                    FileUtils.getStateFile(mAppBaseDir).delete();

                    mEngineTo.reloadGlobalConfig();
            	} else {
                    mEngineTo.saveData();
                }

                return 0;
            }

			@Override
            protected void onPostExecute(Integer result) {
                switch(result) {
                    case 0:
                        if(sBindAppWidgetIdIfAllowed != null) {
                        	if(mApplyMode == ApplyMode.REPLACE) {
	                            AppWidgetHost h = LLApp.get().getAppWidgetHost();
	                            h.deleteHost();
	                            h.startListening();
                        	}
                            mNewAppWidgetIds = new ParcelableSparseIntArray();
                            bindNextAppWidget();
                        } else {
                            startActivity(new Intent(ApplyTemplate.this, Dashboard.class));
                            System.exit(0);
                        }
                        break;

                    default:
                        mDialog.dismiss();
                        Toast.makeText(ApplyTemplate.this, result==1 ? R.string.backup_error : R.string.apply_tmpl_e, Toast.LENGTH_SHORT).show();
                        finish();
                        break;
                }
            }

            @Override
            protected void onProgressUpdate(Boolean... values) {
                mDialog.setMessage(getString(R.string.apply_tmpl_g));
            }
        }.execute((Void) null);
    }

    private InputStream openTemplateStream() {
        if(mTemplateFile != null) {
            try {
                return new FileInputStream(mTemplateFile);
            } catch (IOException e) {
                e.printStackTrace();

                return null;
            }
        } else if(mTemplateUri != null) {
            try {
                return getContentResolver().openInputStream(mTemplateUri);
            } catch (IOException e) {
                e.printStackTrace();

                return null;
            }
        } else {
            Context template_context;
            try {
                template_context = createPackageContext(mTemplateCN.getPackageName(), 0);
            } catch (PackageManager.NameNotFoundException e) {
                return null;
            }


            try {
                return template_context.getAssets(). open("lightning_launcher/"+mTemplateCN.getClassName());
            } catch (IOException e) {
                e.printStackTrace();

                try {
                    String simple_name = mTemplateCN.getShortClassName().toLowerCase();
                    if(simple_name.charAt(0) == '.') simple_name = simple_name.substring(1);
                    int id = template_context.getResources().getIdentifier(simple_name, "raw", mTemplateCN.getPackageName());
                    return template_context.getResources().openRawResource(id);
                } catch(Resources.NotFoundException e1) {
                    e.printStackTrace();
                    return null;
                }
            }
        }
    }

    private void bindNextAppWidget() {
        if(sBindAppWidgetIdIfAllowed == null || mAppWidgetsToBind.size() == 0) {
            updateAppWidgetIds();
        } else {
            int old_id = mAppWidgetsToBind.keyAt(0);
            int new_id = LLApp.get().getAppWidgetHost().allocateAppWidgetId();
            ComponentName cn = mAppWidgetsToBind.valueAt(0);
            boolean ok;
            try {
                ok = (Boolean)sBindAppWidgetIdIfAllowed.invoke(AppWidgetManager.getInstance(this), new_id, cn);
            } catch (Exception e) {
                ok = false;
            }
            if(ok) {
                mNewAppWidgetIds.put(old_id, new_id);
                mAppWidgetsToBind.remove(old_id);
                bindNextAppWidget();
            } else {
                mBindWidgetOldId = old_id;
                mBindWidgetNewId = new_id;
                Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, new_id);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, cn);
                startActivityForResult(intent, REQUEST_BIND_APPWIDGET);
            }
        }
    }

    private LinkedList<Pair<Integer,File>> mAppWidgetsToLoad;

    private void updateAppWidgetIds() {
        mAppWidgetsToLoad = new LinkedList<Pair<Integer,File>>();

        AppWidgetManager awm = AppWidgetManager.getInstance(this);
        for(Page page : mImportedPages) {
            boolean modified = false;

            for(Item i : page.items) {
                if(i.getClass() == Widget.class) {
                    Widget w = (Widget)i;
                    int old_id = w.getAppWidgetId();
                    int new_id = mNewAppWidgetIds.get(old_id);
                    if(new_id != 0) {
                        w.setAppWidgetId(new_id);
                        AppWidgetProviderInfo appWidgetInfo = awm.getAppWidgetInfo(new_id);
                        // don't know why but sometimes appWidgetInfo can be null
                        if(appWidgetInfo != null) {
                            w.setAppWidgetLabel(appWidgetInfo.label);

                            File tmp_widget_file = new File(FileUtils.LL_TMP_DIR, String.valueOf(old_id));
                            if (tmp_widget_file.exists()) {
                                boolean patched = false;
                                DisplayMetrics dm = getResources().getDisplayMetrics();
                                Rect r = new Rect();
                                getWindow().getDecorView().getWindowVisibleDisplayFrame(r);
                                final int sb_height = r.top;

                                int screen_width, screen_height;
                                if (dm.widthPixels > dm.heightPixels) {
                                    screen_width = dm.heightPixels;
                                    screen_height = dm.widthPixels;
                                } else {
                                    screen_width = dm.widthPixels;
                                    screen_height = dm.heightPixels;
                                }
                                double ratio_width = screen_width / (double) mFromScreenWidth;
                                double ratio_height = (screen_height + (page.config.statusBarHide ? mFromStatusBarHeight : 0)) / (double) (mFromScreenHeight + (page.config.statusBarHide ? sb_height : 0));
                                double ratio_dpi = dm.densityDpi / (double) mFromScreenDpi;

                                double from_screen_phys_width = (double) mFromScreenWidth / mFromScreenDpi;
                                double to_screen_phys_width = (double) dm.widthPixels / dm.densityDpi;

                                File patched_widget_file = new File(FileUtils.LL_TMP_DIR, String.valueOf(old_id) + "p");
                                FileInputStream fis = null;
                                FileOutputStream fos = null;
                                ZipInputStream zis = null;
                                ZipOutputStream zos = null;
                                try {
                                    fis = new FileInputStream(tmp_widget_file);
                                    zis = new ZipInputStream(fis);
                                    fos = new FileOutputStream(patched_widget_file);
                                    zos = new ZipOutputStream(fos);
                                    byte[] buffer = new byte[2048];

                                    ZipEntry ze = zis.getNextEntry();
                                    do {
                                        ZipEntry ze_copy = new ZipEntry(ze.getName());
                                        zos.putNextEntry(ze_copy);
                                        int n;
                                        if (ze.getName().equals("preset.json")) {
                                            JSONObject json;
                                            n = zis.read(buffer, 0, buffer.length);
                                            json = new JSONObject(new String(buffer, 0, n));

                                            double widget_width = json.getDouble("preset_widgetwidth");
                                            double widget_height = json.getDouble("preset_widgetheight");

                                            double widget_dpiwidth = json.getDouble("preset_dpiwidth");
                                            double widget_dpiheight = json.getDouble("preset_dpiheight");
                                            double widget_scale = json.getDouble("pref_widget_scale");
//                                        json.put("pref_widget_scale", 100 * (widget_width>widget_height ? ratio_width : ratio_height) / ratio_dpi);


                                            int new_width = (int) (widget_width * ratio_width);
                                            int new_height = (int) (widget_height * ratio_height);
                                            json.put("preset_widgetwidth", new_width);
                                            json.put("preset_widgetheight", new_height);
//                                        json.put("preset_dpiwidth", new_width * 160 / dm.densityDpi);
//                                        json.put("preset_dpiheight", new_height * 160 / dm.densityDpi);
//                                        if(from_screen_phys_width > to_screen_phys_width) {
//                                            json.put("pref_widget_scale", 100);
//                                        } else if(from_screen_phys_width < to_screen_phys_width) {
//                                            json.put("pref_widget_scale", (int)(widget_scale*Math.min(ratio_width, ratio_height)/ratio_dpi));
//                                        }
                                            json.put("pref_widget_scale", (int) (widget_scale * Math.min(ratio_width, ratio_height) / ratio_dpi));
                                            //n7 2012 vers S5
//                                        json.put("preset_widgetwidth", (int)(widget_width * ratio_width));
//                                        json.put("preset_widgetheight", (int)(widget_height * ratio_height));
//                                        json.put("preset_dpiwidth", (int)(json.getDouble("preset_dpiwidth") * ratio_width / ratio_dpi));
//                                        json.put("preset_dpiheight", (int)(json.getDouble("preset_dpiheight") * ratio_height / ratio_dpi));
//                                        json.put("pref_widget_scale", widget_scale * (widget_width>widget_height ? ratio_width : ratio_height));

                                            // n7 2012 vers n7 2013
//                                        json.put("preset_widgetwidth", (int)(widget_width * ratio_width));
//                                        json.put("preset_widgetheight", (int)(widget_height * ratio_height));

                                            // n7 2012 vers xperia V
//                                        json.put("preset_widgetwidth", (int)(widget_width * ratio_width));
//                                        json.put("preset_widgetheight", (int)(widget_height * ratio_height));
//                                        json.put("preset_dpiwidth", (int)(json.getDouble("preset_dpiwidth") * ratio_width / ratio_dpi));
//                                        json.put("preset_dpiheight", (int)(json.getDouble("preset_dpiheight") * ratio_height / ratio_dpi));
//                                        json.put("pref_widget_scale", widget_scale * (widget_width>widget_height ? ratio_width : ratio_height));

                                            zos.write(json.toString().getBytes("utf-8"));
                                        } else {
                                            while ((n = zis.read(buffer, 0, buffer.length)) > 0) {
                                                zos.write(buffer, 0, n);
                                            }
                                        }
                                        zos.closeEntry();
                                        ze = zis.getNextEntry();
                                    } while (ze != null);
                                    patched = true;
                                } catch (IOException e) {
                                    // pass
                                    e.printStackTrace();
                                } catch (JSONException e) {
                                    // pass
                                } finally {
                                    if (zis != null) try {
                                        zis.close();
                                    } catch (IOException e) {
                                    }
                                    if (fis != null) try {
                                        fis.close();
                                    } catch (IOException e) {
                                    }
                                    if (zos != null) try {
                                        zos.close();
                                    } catch (IOException e) {
                                    }
                                    if (fos != null) try {
                                        fos.close();
                                    } catch (IOException e) {
                                    }
                                }
                                if (patched) {
                                    tmp_widget_file.delete();
                                    patched_widget_file.renameTo(tmp_widget_file);
                                } else {
                                    patched_widget_file.delete();
                                }
                                mAppWidgetsToLoad.add(new Pair<Integer, File>(Integer.valueOf(new_id), tmp_widget_file));
                            }
                        }

                        modified = true;
                    }
                }
            }

            if(modified) {
                page.setModified();
                page.save();
            }

        }

        loadNextWidgetOrStop();
    }

    private void loadNextWidgetOrStop() {
        if(mAppWidgetsToLoad.size() == 0) {
            if(mApplyMode == ApplyMode.MERGE) {
                selectDesktopToGoTo();
            } else {
                restart();
            }
        } else {
            loadNextWidget();
        }
    }

    private void loadNextWidget() {
        Pair<Integer, File> item = mAppWidgetsToLoad.get(0);
        mAppWidgetsToLoad.remove(0);

        final int new_id = item.first;
        final File widget_file = item.second;

        AppWidgetManager awm = AppWidgetManager.getInstance(this);
        AppWidgetProviderInfo app_widget_info=awm.getAppWidgetInfo(new_id);
        if(app_widget_info == null) {
            loadNextWidgetOrStop();
        } else {
            Uri tmp_widget_uri = Uri.fromFile(widget_file);
            Intent br = new Intent("com.buzzpia.aqua.appwidget.SET_CONFIG_DATA");
            br.putExtra("appWidgetId", new_id);
            br.setData(tmp_widget_uri);
            br.setComponent(app_widget_info.provider);

            BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    widget_file.delete();
                    loadNextWidgetOrStop();
                }
            };
            try {
                sendOrderedBroadcast(br, null, receiver, null, Activity.RESULT_OK, null, null);
            } catch (Exception e) {
                // FileUriExposedException
                receiver.onReceive(this, br);
            }
        }
    }
    
    private void selectDesktopToGoTo() {
    	LLApp.get().getAppEngine().getPageManager().clear();
    	Intent intent = new Intent(this, ScreenManager.class);
        intent.setAction(Intent.ACTION_PICK);
        startActivityForResult(intent, REQUEST_SELECT_SCREEN_FOR_GOTO);
    }
    
    private void restart() {
        Utils.deleteDirectory(FileUtils.LL_TMP_DIR, false);
        startActivity(new Intent(this, Dashboard.class));
        System.exit(0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_BIND_APPWIDGET) {
            if(resultCode == RESULT_OK) {
                mNewAppWidgetIds.put(mBindWidgetOldId, mBindWidgetNewId);
            } else {
                LLApp.get().getAppWidgetHost().deleteAppWidgetId(mBindWidgetNewId);
            }
            mAppWidgetsToBind.remove(mBindWidgetOldId);
            bindNextAppWidget();
        } else if(requestCode == REQUEST_SELECT_SCREEN_FOR_GOTO) {
        	if(resultCode == RESULT_OK) {
        		LLApp.get().getAppEngine().writeCurrentPage(data.getIntExtra(API.SCREEN_PICKER_INTENT_EXTRA_SCREEN, Page.FIRST_DASHBOARD_PAGE));
        	}
            restart();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
    
    private static class ParcelableSparseIntArray implements Cloneable, Parcelable {
        public static final Creator<ParcelableSparseIntArray> CREATOR = new Creator<ParcelableSparseIntArray>() {
            @Override
            public ParcelableSparseIntArray createFromParcel(Parcel source) {
                final int size = source.readInt();
                ParcelableSparseIntArray array = new ParcelableSparseIntArray(size);
                array.mSize = size;
                array.mKeys = source.createIntArray();
                array.mValues = source.createIntArray();
                return array;
            }

            @Override
            public ParcelableSparseIntArray[] newArray(int size) {
                return new ParcelableSparseIntArray[size];
            }
        };

        private static int binarySearch(int[] a, int start, int len, int key) {
            int high = start + len, low = start - 1, guess;
            while (high - low > 1) {
                guess = (high + low) / 2;
                if (a[guess] < key) {
                    low = guess;
                } else {
                    high = guess;
                }
            }
            if (high == start + len) {
                return ~(start + len);
            } else if (a[high] == key) {
                return high;
            } else {
                return ~high;
            }
        }

        private int[] mKeys;

        private int mSize;

        private int[] mValues;

        public ParcelableSparseIntArray() {
            this(10);
        }

        public ParcelableSparseIntArray(int initialCapacity) {
            mKeys = new int[initialCapacity];
            mValues = new int[initialCapacity];
            mSize = 0;
        }

//        public ParcelableSparseIntArray(ParcelableSparseIntArray arrayForCopy) {
//            if (arrayForCopy == null) {
//                int initialCapacity = arrayForCopy.mSize;
//                mKeys = new int[initialCapacity];
//                mValues = new int[initialCapacity];
//                mSize = 0;
//            } else {
//                mKeys = arrayForCopy.mKeys.clone();
//                mValues = arrayForCopy.mValues.clone();
//            }
//        }

        public void append(int key, int value) {
            if (mSize != 0 && key <= mKeys[mSize - 1]) {
                put(key, value);
                return;
            }
            int pos = mSize;
            if (pos >= mKeys.length) {
                int n = pos + 1;
                int[] nkeys = new int[n];
                int[] nvalues = new int[n];
                System.arraycopy(mKeys, 0, nkeys, 0, mKeys.length);
                System.arraycopy(mValues, 0, nvalues, 0, mValues.length);
                mKeys = nkeys;
                mValues = nvalues;
            }
            mKeys[pos] = key;
            mValues[pos] = value;
            mSize = pos + 1;
        }

        public void clear() {
            mSize = 0;
        }

        @Override
        public ParcelableSparseIntArray clone() {
        	ParcelableSparseIntArray clone = null;
            try {
                clone = (ParcelableSparseIntArray) super.clone();
                clone.mKeys = mKeys.clone();
                clone.mValues = mValues.clone();
                clone.mSize = mSize;
            } catch (CloneNotSupportedException cnse) {
            }
            return clone;
        }

        public void delete(int key) {
            int i = binarySearch(mKeys, 0, mSize, key);
            if (i >= 0) {
                removeAt(i);
            }
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public int get(int key) {
            return get(key, 0);
        }

        public int get(int key, int valueIfKeyNotFound) {
            int i = binarySearch(mKeys, 0, mSize, key);
            if (i < 0) {
                return valueIfKeyNotFound;
            } else {
                return mValues[i];
            }
        }

        public int indexOfKey(int key) {
            return binarySearch(mKeys, 0, mSize, key);
        }

        public int indexOfValue(int value) {
            for (int i = 0; i < mSize; i++) {
                if (mValues[i] == value) {
                    return i;
                }
            }
            return -1;
        }

        public int keyAt(int index) {
            return mKeys[index];
        }

        public void put(int key, int value) {
            int i = binarySearch(mKeys, 0, mSize, key);
            if (i >= 0) {
                mValues[i] = value;
            } else {
                i = ~i;
                if (mSize >= mKeys.length) {
                    int n = mSize + 1;
                    int[] nkeys = new int[n];
                    int[] nvalues = new int[n];
                    System.arraycopy(mKeys, 0, nkeys, 0, mKeys.length);
                    System.arraycopy(mValues, 0, nvalues, 0, mValues.length);
                    mKeys = nkeys;
                    mValues = nvalues;
                }
                if (mSize - i != 0) {
                    System.arraycopy(mKeys, i, mKeys, i + 1, mSize - i);
                    System.arraycopy(mValues, i, mValues, i + 1, mSize - i);
                }
                mKeys[i] = key;
                mValues[i] = value;
                mSize++;
            }
        }

        public void removeAt(int index) {
            System.arraycopy(mKeys, index + 1, mKeys, index, mSize - (index + 1));
            System.arraycopy(mValues, index + 1, mValues, index, mSize - (index + 1));
            mSize--;
        }

        public int size() {
            return mSize;
        }

        public int valueAt(int index) {
            return mValues[index];
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(mSize);
            dest.writeIntArray(mKeys);
            dest.writeIntArray(mValues);
        }
    }
}
