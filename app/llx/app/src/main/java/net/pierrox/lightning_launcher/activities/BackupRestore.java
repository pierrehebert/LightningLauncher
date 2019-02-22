package net.pierrox.lightning_launcher.activities;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

import net.pierrox.lightning_launcher.API;
import net.pierrox.lightning_launcher.LLApp;
import net.pierrox.lightning_launcher.data.BackupRestoreTool;
import net.pierrox.lightning_launcher.data.FileUtils;
import net.pierrox.lightning_launcher.data.Folder;
import net.pierrox.lightning_launcher.data.Item;
import net.pierrox.lightning_launcher.data.Page;
import net.pierrox.lightning_launcher.engine.LightningEngine;
import net.pierrox.lightning_launcher.data.Utils;
import net.pierrox.lightning_launcher.util.FileProvider;
import net.pierrox.lightning_launcher_extreme.R;

import org.json.JSONObject;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class BackupRestore extends ResourceWrapperActivity implements View.OnClickListener, AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener, OnLongClickListener {
    private static final int DIALOG_BACKUP_IN_PROGRESS=1;
    private static final int DIALOG_RESTORE_IN_PROGRESS=2;
    private static final int DIALOG_SELECT_ARCHIVE_NAME=3;
    private static final int DIALOG_SELECT_BACKUP_ACTION=4;
    private static final int DIALOG_CONFIRM_RESTORE=5;
    private static final int DIALOG_CONFIRM_DELETE=6;
    
    private static final int REQUEST_SELECT_PAGES_FOR_EXPORT = 1;

    private ListView mListView;
    private TextView mEmptyView;

    private boolean mSelectArchiveNameForBackup;
    private String mArchiveName;
    private Uri mArchiveUri;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.setTheme(this, Utils.APP_THEME);

        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.backup_restore);

        Button backup = (Button) findViewById(R.id.backup);
        backup.setText(R.string.backup_t);
        backup.setOnClickListener(this);
        
        Button export = (Button) findViewById(R.id.export);
        export.setText(R.string.tmpl_e_t);
        export.setOnClickListener(this);
        export.setOnLongClickListener(this);

        mListView = (ListView) findViewById(R.id.archives);
        mListView.setOnItemClickListener(this);
        mListView.setOnItemLongClickListener(this);

        mEmptyView = (TextView) findViewById(R.id.empty);
        mEmptyView.setText(R.string.no_backup_archive);

        loadArchivesList();

        if(LLApp.get().isTrialVersion()) {
            Toast.makeText(this, R.string.tr_fl_t, Toast.LENGTH_SHORT).show();
        } else {
            checkPermissions(
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    new int[] { R.string.pr_r1, R.string.pr_r2},
                    REQUEST_PERMISSION_BASE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if(areAllPermissionsGranted(grantResults, R.string.pr_f1)) {
            loadArchivesList();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        Intent intent = getIntent();
        if(Intent.ACTION_VIEW.equals(intent.getAction())) {
            intent.setAction(Intent.ACTION_MAIN);
            loadArchive(intent.getData(), null);
        }
    }

    private void loadArchivesList() {
        File[] archives = FileUtils.LL_EXT_DIR.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return !file.isDirectory();
            }
        });
        if(archives==null || archives.length==0) {
            mEmptyView.setVisibility(View.VISIBLE);
            mListView.setVisibility(View.GONE);
        } else {
            Arrays.sort(archives, new Comparator<File>() {
                @Override
                public int compare(File file1, File file2) {
                    long a = file1.lastModified();
                    long b = file2.lastModified();
                    if(a<b) return 1;
                    if(a>b) return -1;
                    return 0;
                }
            });
            String[] archives_names = new String[archives.length];
            for(int i=0; i<archives.length; i++) {
                archives_names[i] = archives[i].getName();
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, archives_names);
            mListView.setAdapter(adapter);
            mEmptyView.setVisibility(View.GONE);
            mListView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.backup:
                exportArchive(true);
                break;

            case R.id.export:
                exportArchive(false);
                break;
        }
    }
    
	@Override
	public boolean onLongClick(View arg0) {
		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.pierrox.net/cmsms/applications/lightning-launcher/templates.html")));
		return true;
	}

    @Override
    protected Dialog onCreateDialog(int id) {
        AlertDialog.Builder builder;
        ProgressDialog progress;

        switch(id) {
            case DIALOG_BACKUP_IN_PROGRESS:
                progress = new ProgressDialog(this);
                progress.setMessage(getString(R.string.backup_in_progress));
                progress.setCancelable(false);
                return progress;

            case DIALOG_RESTORE_IN_PROGRESS:
                progress = new ProgressDialog(this);
                progress.setMessage(getString(R.string.restore_in_progress));
                progress.setCancelable(false);
                return progress;

            case DIALOG_SELECT_ARCHIVE_NAME:
                builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.br_n);
                final String archive_name;
                if(mArchiveName == null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH-mm");
                    archive_name = getString(mSelectArchiveNameForBackup ? R.string.backup_d : R.string.tmpl_fn)+"-"+sdf.format(new Date())+".lla";
                } else {
                    archive_name = mArchiveName;
                }
                final EditText edit_text = new EditText(this);
                edit_text.setText(archive_name);
                edit_text.setSelection(archive_name.length());
                FrameLayout l = new FrameLayout(this);
                l.setPadding(10, 10, 10, 10);
                l.addView(edit_text);
                builder.setView(l);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String name = edit_text.getText().toString().trim();
                        String archive_path= FileUtils.LL_EXT_DIR +"/"+name;
                        File old_archive_file = mArchiveName==null ? null : new File(FileUtils.LL_EXT_DIR +"/"+mArchiveName);
                        if(old_archive_file!=null && old_archive_file.exists()) {
                            old_archive_file.renameTo(new File(archive_path));
                            loadArchivesList();
                        } else {
                        	if(mSelectArchiveNameForBackup) {
                        		new BackupTask().execute(archive_path);
                        	} else {
                        		mArchiveName = FileUtils.LL_EXT_DIR +"/"+name;
                        		selectDesktopsToExport();
                        	}
                        }
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, null);
                return builder.create();

            case DIALOG_CONFIRM_RESTORE:
                if(mArchiveName != null || mArchiveUri != null) {
                    builder = new AlertDialog.Builder(this);
                    builder.setTitle(R.string.br_rc);
                    builder.setMessage(mArchiveName==null ? mArchiveUri.getLastPathSegment() : mArchiveName);
                    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if(mArchiveName == null) {
                                new RestoreTask(mArchiveUri).execute();
                            } else {
                                String path;
                                if (!mArchiveName.startsWith("/")) {
                                    path = FileUtils.LL_EXT_DIR + "/" + mArchiveName;
                                } else {
                                    path = mArchiveName;
                                }
                                new RestoreTask(path).execute();
                            }
                        }
                    });
                    builder.setNegativeButton(android.R.string.cancel, null);
                    return builder.create();
                }
                break;

            case DIALOG_CONFIRM_DELETE:
                if(mArchiveName != null) {
                    builder = new AlertDialog.Builder(this);
                    builder.setTitle(R.string.br_dc);
                    builder.setMessage(mArchiveName);
                    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            new File(FileUtils.LL_EXT_DIR +"/"+mArchiveName).delete();
                            loadArchivesList();
                        }
                    });
                    builder.setNegativeButton(android.R.string.cancel, null);
                    return builder.create();
                }
                break;

            case DIALOG_SELECT_BACKUP_ACTION:
                if(mArchiveName != null) {
                    builder = new AlertDialog.Builder(this);
                    builder.setTitle(R.string.br_a);
                    builder.setItems(new String[] { getString(R.string.br_ob), getString(R.string.br_ot), getString(R.string.br_r), getString(R.string.br_s), getString(R.string.br_d)}, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            switch(i) {
                            	case 0:
                            		new BackupTask().execute(FileUtils.LL_EXT_DIR +"/"+mArchiveName);
                            		break;
                            		
                            	case 1:
                            		mArchiveName = FileUtils.LL_EXT_DIR +"/"+mArchiveName;
                            		selectDesktopsToExport();
                            		break;
                            		
                                case 2:
                                    try { removeDialog(DIALOG_SELECT_ARCHIVE_NAME); } catch(Exception e) {}
                                    showDialog(DIALOG_SELECT_ARCHIVE_NAME);
                                    break;
                                case 3:
                                    Intent shareIntent = new Intent();
                                    shareIntent.setAction(Intent.ACTION_SEND);
                                    Uri uri = FileProvider.getUriForFile(new File(FileUtils.LL_EXT_DIR +"/"+mArchiveName));
                                    shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                                    shareIntent.setType("application/zip");
                                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                    startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.br_s)));
                                    break;
                                case 4:
                                    try { removeDialog(DIALOG_CONFIRM_DELETE); } catch(Exception e) {}
                                    showDialog(DIALOG_CONFIRM_DELETE);
                                    break;
                            }
                        }
                    });
                    builder.setNegativeButton(android.R.string.cancel, null);
                    return builder.create();
                }
                break;
        }

        return super.onCreateDialog(id);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        loadArchive(null, adapterView.getAdapter().getItem(i).toString());
    }

    private void loadArchive(Uri archiveUri, String archiveName) {
        if(LLApp.get().isTrialVersion()) {
            LLApp.get().showFeatureLockedDialog(this);
        } else {
            mArchiveUri = archiveUri;
            mArchiveName = archiveName;
            try {
                removeDialog(DIALOG_CONFIRM_RESTORE);
            } catch (Exception e) {
            }
            showDialog(DIALOG_CONFIRM_RESTORE);
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
        mArchiveName = adapterView.getAdapter().getItem(i).toString();
        showDialog(DIALOG_SELECT_BACKUP_ACTION);
        return true;
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if(requestCode == REQUEST_SELECT_PAGES_FOR_EXPORT) {
            if(resultCode == RESULT_OK) {
                int[] selected_pages = data.getIntArrayExtra(API.SCREEN_PICKER_INTENT_EXTRA_SELECTED_SCREENS);
                
                ArrayList<Integer> all_pages = new ArrayList<Integer>();
                for(int p : selected_pages) {
                	all_pages.add(Integer.valueOf(p));
                	addSubPages(all_pages, p);
                }
                all_pages.add(Integer.valueOf(Page.APP_DRAWER_PAGE));
                all_pages.add(Integer.valueOf(Page.USER_MENU_PAGE));

                doExportTemplate(mArchiveName, all_pages);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
    
    private void addSubPages(ArrayList<Integer> all_pages, int p) {
        Page page = LLApp.get().getAppEngine().getOrLoadPage(p);
    	for(Item i : page.items) {
    		if(i instanceof Folder) {
    			int folder_page_id = ((Folder)i).getFolderPageId();
    			all_pages.add(Integer.valueOf(folder_page_id));
    			addSubPages(all_pages, folder_page_id);
    		}
    	}
    }
    
    
    private void exportArchive(boolean for_backup) {
    	mSelectArchiveNameForBackup = for_backup;
    	mArchiveName = null;
        try { removeDialog(DIALOG_SELECT_ARCHIVE_NAME); } catch(Exception e) {}
        showDialog(DIALOG_SELECT_ARCHIVE_NAME);
    }
    
	private void selectDesktopsToExport() {
    	Intent intent = new Intent(this, ScreenManager.class);
        intent.putExtra(API.SCREEN_PICKER_INTENT_EXTRA_SELECTED_SCREENS, LLApp.get().getAppEngine().getGlobalConfig().screensOrder);
        intent.putExtra(API.SCREEN_PICKER_INTENT_EXTRA_TITLE, getString(R.string.tmpl_s_p));
        startActivityForResult(intent, REQUEST_SELECT_PAGES_FOR_EXPORT);
    }

    private void doExportTemplate(final String backup_path, final ArrayList<Integer> included_pages) {
        Rect r = new Rect();
        getWindow().getDecorView().getWindowVisibleDisplayFrame(r);
        final int sb_height = r.top;

        new AsyncTask<Void, Void, Boolean>() {
            private ProgressDialog mDialog;

            @Override
            protected void onPreExecute() {
                mDialog = new ProgressDialog(BackupRestore.this);
                mDialog.setMessage(getString(R.string.tmpl_e_m));
                mDialog.setCancelable(false);
                mDialog.show();
            }

            @Override
            protected Boolean doInBackground(Void... voids) {
                BackupRestoreTool.BackupConfig backup_config=new BackupRestoreTool.BackupConfig();

                backup_config.context=BackupRestore.this;
                PackageManager pm=getPackageManager();
                try {
                    PackageInfo pi=pm.getPackageInfo(BackupRestore.this.getPackageName(), 0);
                    final String data_dir=pi.applicationInfo.dataDir+"/files";
                    backup_config.pathFrom=data_dir;
                } catch(PackageManager.NameNotFoundException e) {
                    // pass
                }
                backup_config.pathTo=backup_path;
                backup_config.includeWidgetsData=true;
                backup_config.includeWallpaper=true;
                backup_config.includeFonts=true;
                backup_config.forTemplate =true;
                backup_config.statusBarHeight = sb_height;
                backup_config.pagesToInclude = included_pages;


                return BackupRestoreTool.backup(backup_config)==null;
            }

            @Override
            protected void onPostExecute(Boolean ok) {
                mDialog.dismiss();
                Toast.makeText(BackupRestore.this, ok ? R.string.tmpl_e_d : R.string.tmpl_e_e, Toast.LENGTH_SHORT).show();
                loadArchivesList();
            }
        }.execute((Void) null);
    }
    
    private class BackupTask extends AsyncTask<String, Void, Exception> {
        private String mBackupFilePath;
        @Override
        protected void onPreExecute() {
            showDialog(DIALOG_BACKUP_IN_PROGRESS);
        }

        @Override
        protected Exception doInBackground(String... params) {
            mBackupFilePath=(String)params[0];

            BackupRestoreTool.BackupConfig backup_config=new BackupRestoreTool.BackupConfig();

            backup_config.context=BackupRestore.this;
            PackageManager pm=getPackageManager();
            try {
                PackageInfo pi=pm.getPackageInfo(BackupRestore.this.getPackageName(), 0);
                final String data_dir=pi.applicationInfo.dataDir+"/files";
                backup_config.pathFrom=data_dir;
            } catch(PackageManager.NameNotFoundException e) {
                // pass
            }
            backup_config.pathTo=mBackupFilePath;
            backup_config.includeWidgetsData=true;
            backup_config.includeWallpaper=true;
            backup_config.includeFonts=true;

            return BackupRestoreTool.backup(backup_config);
        }

        @Override
        protected void onPostExecute(Exception result) {
            removeDialog(DIALOG_BACKUP_IN_PROGRESS);

            if(result != null) {
                Writer out = new StringWriter(1000);

                PrintWriter printWriter = new PrintWriter(out);
                result.printStackTrace(printWriter);

                Intent email_intent=new Intent(android.content.Intent.ACTION_SEND, Uri.parse("mailto:"));
                email_intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                email_intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"pierrox@pierrox.net"});
                email_intent.putExtra(Intent.EXTRA_SUBJECT, "Backup error");
                email_intent.putExtra(Intent.EXTRA_TEXT, out.toString());
                email_intent.setType("message/rfc822");
                startActivity(Intent.createChooser(email_intent, "Backup error, please send a bug report by email"));
            }
            String msg=(result==null ? getString(R.string.backup_done, mBackupFilePath) : getString(R.string.backup_error));
            Toast.makeText(BackupRestore.this, msg, Toast.LENGTH_LONG).show();

            loadArchivesList();
        }
    }

    private class RestoreTask extends AsyncTask<String, Void, Integer> {
        private Uri mUri;

        private RestoreTask(String path) {
            mUri = Uri.fromFile(new File(path));
        }

        private RestoreTask(Uri uri) {
            mUri = uri;
        }

        @Override
        protected void onPreExecute() {
            showDialog(DIALOG_RESTORE_IN_PROGRESS);
        }

        @Override
        protected Integer doInBackground(String... params) {
            BackupRestoreTool.RestoreConfig restore_config=new BackupRestoreTool.RestoreConfig();

            InputStream is = null;
            try {
                ContentResolver cr = getContentResolver();
                is = cr.openInputStream(mUri);
                JSONObject manifest = BackupRestoreTool.readManifest(is);
                if(manifest != null) {
                    // it looks like a template
                    return 2;
                }
            } catch(Exception e) {
                // not a template, continue with normal restore
            } finally {
                if(is != null) try { is.close(); } catch (IOException e) {}
            }

            restore_config.context=BackupRestore.this;
            PackageManager pm=getPackageManager();
            try {
                PackageInfo pi=pm.getPackageInfo(BackupRestore.this.getPackageName(), 0);
                final String data_dir=pi.applicationInfo.dataDir+"/files";
                restore_config.pathTo=data_dir;
            } catch(PackageManager.NameNotFoundException e) {
                // pass
            }
            restore_config.uriFrom=mUri;
            restore_config.restoreWidgetsData=true;
            restore_config.restoreWallpaper=true;
            restore_config.restoreFonts=true;

            // ensure this directory at least is created with right permissions
            try {
                restore_config.context.createPackageContext(BackupRestore.this.getPackageName(), 0).getDir("files", Context.MODE_PRIVATE);
            } catch (PackageManager.NameNotFoundException e1) {
                return 0;
            }

            return BackupRestoreTool.restore(restore_config) ? 1 : 0;
        }

        @Override
        protected void onPostExecute(Integer result) {
            removeDialog(DIALOG_RESTORE_IN_PROGRESS);
            if(result == 1) {
                startActivity(new Intent(BackupRestore.this, Dashboard.class));
                System.exit(0);
            } else if(result == 2) {
                Intent intent = new Intent(BackupRestore.this, ApplyTemplate.class);
                intent.putExtra(ApplyTemplate.INTENT_EXTRA_URI, mUri);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(BackupRestore.this, R.string.restore_error, Toast.LENGTH_LONG).show();
            }
        }
    }
}
