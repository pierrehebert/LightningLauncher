package net.pierrox.lightning_launcher.activities;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import net.pierrox.lightning_launcher.LLApp;
import net.pierrox.lightning_launcher.Version;
import net.pierrox.lightning_launcher.data.ContainerPath;
import net.pierrox.lightning_launcher.data.Page;
import net.pierrox.lightning_launcher.data.Utils;
import net.pierrox.lightning_launcher.template.LLTemplateAPI;
import net.pierrox.lightning_launcher.util.PhoneUtils;
import net.pierrox.lightning_launcher_extreme.R;

public class RootSettings extends PreferenceActivity implements OnPreferenceClickListener {
	private static final String KEY_GLOBAL_CONFIG="g";
	private static final String KEY_CURRENT_PAGE="d";
	private static final String KEY_APP_DRAWER="a";
	private static final String KEY_BACKUP_RESTORE="br";
	private static final String KEY_CAT_SETTINGS="t";
	private static final String KEY_CAT_INFOS="i";
	private static final String KEY_COMMUNITY ="f";
	private static final String KEY_RATE="r";
	private static final String KEY_SELECT_LAUNCHER ="s";
    private static final String KEY_CONFIGURE_PAGES="p";
    private static final String KEY_CAT_TEMPLATES="tc";
    private static final String KEY_TEMPLATES_BROWSE="tb";
    private static final String KEY_TEMPLATES_APPLY="ta";
    private static final String KEY_UPGRADE="u";
    private static final String KEY_EXTENSIONS="e";

    private static final int REQUEST_SELECT_TEMPLATE = 1;

    private ResourcesWrapperHelper mResourcesWrapperHelper;
    @Override
    public final Resources getResources() {
        if(mResourcesWrapperHelper == null) {
            mResourcesWrapperHelper = new ResourcesWrapperHelper(this, super.getResources());
        }
        return mResourcesWrapperHelper.getResources();
    }

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
        Utils.setTheme(this, Utils.APP_THEME);

		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.preference_root);
		
		setupPreference(KEY_CAT_SETTINGS, R.string.settings, 0);
		setupPreference(KEY_GLOBAL_CONFIG, R.string.general_t, R.string.general_s);
		setupPreference(KEY_CURRENT_PAGE, R.string.dashboard_t, R.string.dashboard_s);
		setupPreference(KEY_APP_DRAWER, R.string.app_drawer_t, R.string.app_drawer_s);
		setupPreference(KEY_EXTENSIONS, R.string.extensions, R.string.extensions_s);

        final LLApp app = LLApp.get();
        setupPreference(KEY_CONFIGURE_PAGES, R.string.configure_pages_t, app.isFreeVersion() ? R.string.tr_br_s : R.string.configure_pages_s);
		setupPreference(KEY_BACKUP_RESTORE, R.string.backup_restore_t, app.isTrialVersion() ? R.string.tr_br_s : 0);

		setupPreference(KEY_CAT_INFOS, R.string.app_name, 0);
		setupPreference(KEY_SELECT_LAUNCHER, R.string.select_launcher_title, 0);
		setupPreference(KEY_COMMUNITY, R.string.facebook_t, R.string.facebook_s);
        setupPreference(KEY_RATE, R.string.rate_t, R.string.rate_s);

        setupPreference(KEY_UPGRADE, R.string.tr_rs_t, R.string.tr_rs_s);
        if(app.isTrialVersion()) {
            final long left = app.getTrialLeft();
            long d = left==0 ? 0 : 1 + left / 86400000L;
            findPreference(KEY_UPGRADE).setSummary(getString(R.string.tr_l, d));
        }

        setupPreference(KEY_CAT_TEMPLATES, R.string.tmpl_t, R.string.tmpl_s);
        setupPreference(KEY_TEMPLATES_BROWSE, R.string.tmpl_b_t, R.string.tmpl_b_s);
        setupPreference(KEY_TEMPLATES_APPLY, R.string.tmpl_a_t, R.string.tmpl_a_s);

        PreferenceCategory pc=(PreferenceCategory)getPreferenceScreen().findPreference(KEY_CAT_INFOS);
        if(!Version.HAS_RATE_LINK || app.isFreeVersion() || app.isTrialVersion()) {
            pc.removePreference(findPreference(KEY_RATE));
        }
        if(!app.isFreeVersion() && !app.isTrialVersion()) {
            pc.removePreference(findPreference(KEY_UPGRADE));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.root_settings, menu);
        menu.findItem(R.id.h).setTitle(getString(R.string.sc_help));
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.h: PhoneUtils.showPreferenceHelp(this, null); return true;
            default: return super.onOptionsItemSelected(item);
        }
    }
	
	private void setupPreference(String key, int title, int summary) {
		@SuppressWarnings("deprecation")
		Preference p=findPreference(key);
        if(key.equals(KEY_CAT_INFOS)) {
            try {
                PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
                p.setTitle(getString(title) + "  v"+pi.versionName);
            } catch(NameNotFoundException e) {
                // pass, cannot fail
            }
        } else {
		    p.setTitle(title);
        }
		if(summary!=0) p.setSummary(summary);
		p.setOnPreferenceClickListener(this);
	}

	@Override
	public boolean onPreferenceClick(Preference pref) {
		String key=pref.getKey();
		if(KEY_CONFIGURE_PAGES.equals(key)) {
            Intent intent = new Intent(this, ScreenManager.class);
            intent.setAction(Intent.ACTION_EDIT);
            startActivity(intent);
		} else if(KEY_BACKUP_RESTORE.equals(key)) {
			startActivity(new Intent(this, BackupRestore.class));
		} else if(KEY_SELECT_LAUNCHER.equals(key)) {
			PhoneUtils.selectLauncher(this, true);
        } else if(KEY_UPGRADE.equals(key)) {
            LLApp.get().startUnlockProcess(this);
        } else if(KEY_TEMPLATES_BROWSE.equals(key)) {
            startActivity(Intent.createChooser(new Intent(Intent.ACTION_VIEW, Version.BROWSE_TEMPLATES_URI), ""));
        } else if(KEY_TEMPLATES_APPLY.equals(key)) {
            Intent i=new Intent(Intent.ACTION_PICK_ACTIVITY);
            i.putExtra(Intent.EXTRA_TITLE, getString(R.string.tmpl_c));
            Intent filter=new Intent(LLTemplateAPI.INTENT_QUERY_TEMPLATE);
            i.putExtra(Intent.EXTRA_INTENT, filter);
            startActivityForResult(i, REQUEST_SELECT_TEMPLATE);
		} else if (KEY_EXTENSIONS.equals(key)) {
			Intent i = new Intent(this, Extensions.class);
			startActivity(i);
		} else if(!KEY_RATE.equals(key) && !KEY_COMMUNITY.equals(key)) {
			Intent intent=new Intent(this, Customize.class);
            ContainerPath path = null;
            if(KEY_CURRENT_PAGE.equals(key)) {
                int current_page = LLApp.get().getAppEngine().readCurrentPage(Page.FIRST_DASHBOARD_PAGE);
                String s = getIntent().getStringExtra(Customize.INTENT_EXTRA_PAGE_PATH);
                if(s == null) {
                    path = new ContainerPath(getIntent().getIntExtra(Customize.INTENT_EXTRA_PAGE_ID, current_page));
                } else {
                    path = new ContainerPath(s);
                    if(path.getLast() == Page.APP_DRAWER_PAGE) {
                        path = new ContainerPath(current_page);
                    }
                }
            } if(KEY_APP_DRAWER.equals(key)) {
                path = new ContainerPath(Page.APP_DRAWER_PAGE);
            }
            if(path != null) {
                intent.putExtra(Customize.INTENT_EXTRA_PAGE_PATH, path.toString());
            }
			startActivity(intent);
		}
		return false;
	}

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_SELECT_TEMPLATE) {
            if(resultCode == RESULT_OK) {
                ComponentName component_name=data.getComponent();
                if(component_name != null) {
                    Intent intent = new Intent(this, ApplyTemplate.class);
                    intent.putExtra(LLTemplateAPI.INTENT_TEMPLATE_COMPONENT_NAME, component_name);
                    startActivity(intent);
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
