package net.pierrox.lightning_launcher.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Pair;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import net.pierrox.lightning_launcher.LLApp;
import net.pierrox.lightning_launcher.configuration.GlobalConfig;
import net.pierrox.lightning_launcher.data.ContainerPath;
import net.pierrox.lightning_launcher.data.LightningIntent;
import net.pierrox.lightning_launcher.data.Page;
import net.pierrox.lightning_launcher.data.TaskerPlugin;
import net.pierrox.lightning_launcher.data.Utils;
import net.pierrox.lightning_launcher.engine.variable.Variable;
import net.pierrox.lightning_launcher.script.Script;
import net.pierrox.lightning_launcher.util.ScriptPickerDialog;
import net.pierrox.lightning_launcher.util.SetVariableDialog;
import net.pierrox.lightning_launcher_extreme.R;

public class Shortcuts extends ResourceWrapperListActivity {
	private static final String INDENT = "    ";

	private String[] mItems;
	private boolean mIsForTaskerScript;
	private boolean mIsForTaskerVariable;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
        final Intent intent = getIntent();
        String className = intent.getComponent().getClassName();
        mIsForTaskerScript = className.endsWith("TS");
        mIsForTaskerVariable = className.endsWith("TV");

        Utils.setTheme(this, mIsForTaskerScript ? Utils.APP_THEME_TRANSLUCENT : Utils.APP_THEME);

		super.onCreate(savedInstanceState);

        if(mIsForTaskerScript) {
            pickScriptAndCreateShortcut();
            return;
        } else if(mIsForTaskerVariable) {
            pickVariableAndCreateShortcut();
            return;
        } else {
            mItems = new String[]{
                    getString(R.string.settings),                            // 0
                    getString(R.string.general_t),                            // 1
                    INDENT + getString(R.string.language_t),                    // 2
                    INDENT + getString(R.string.events_t),                    // 3
                    INDENT + getString(R.string.s_ls_t),                        // 4
                    INDENT + getString(R.string.ov_t),                        // 5
                    getString(R.string.dashboard_t),                        // 6
                    INDENT + getString(R.string.icons_t),                        // 7
                    INDENT + getString(R.string.background_t),                // 8
                    INDENT + getString(R.string.grid_t),                        // 9
                    INDENT + getString(R.string.folder_look_t),                // 10
                    INDENT + getString(R.string.sb_t),                        // 11
                    INDENT + getString(R.string.layout_t),                    // 12
                    INDENT + getString(R.string.zoom_scroll_t),                // 13
                    INDENT + getString(R.string.events_t),                    // 14
                    INDENT + getString(R.string.folder_feel_t),                // 15
                    INDENT + getString(R.string.pg_misc_t),                    // 16
                    getString(R.string.app_drawer_t),                        // 17
                    INDENT + getString(R.string.icons_t),                        // 18
                    INDENT + getString(R.string.background_t),                // 19
                    INDENT + getString(R.string.grid_t),                        // 20
                    INDENT + getString(R.string.folder_look_t),                // 21
                    INDENT + getString(R.string.sb_t),                        // 22
                    INDENT + getString(R.string.layout_t),                    // 23
                    INDENT + getString(R.string.zoom_scroll_t),                // 24
                    INDENT + getString(R.string.events_t),                    // 25
                    INDENT + getString(R.string.folder_feel_t),                // 26
                    INDENT + getString(R.string.adm_t),                // 27
                    INDENT + getString(R.string.pg_misc_t),                    // 28
                    getString(R.string.configure_pages_t),                    // 29
                    getString(R.string.backup_restore_t)                    // 30
            };

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mItems);
            setListAdapter(adapter);
        }
    }

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Intent intent;
		String name;
		
        if (position == 29) {
            intent = new Intent(this, ScreenManager.class);
            intent.setAction(Intent.ACTION_EDIT);
            name = mItems[position].trim();
        } else if (position == 30) {
            intent = new Intent(this, BackupRestore.class);
            name = mItems[position].trim();
        } else {
            intent = new Intent(this, position == 0 ? RootSettings.class : Customize.class);

            name = mItems[position].trim();
            String goto_ = null;
            switch (position) {
                case 2:
                    goto_ = Customize.INTENT_EXTRA_GOTO_GENERAL_LANGUAGE;
                    break;
                case 3:
                    goto_ = Customize.INTENT_EXTRA_GOTO_GENERAL_EVENTS;
                    break;
                case 4:
                    goto_ = Customize.INTENT_EXTRA_GOTO_GENERAL_LOCK_SCREEN;
                    break;
                case 5:
                    goto_ = Customize.INTENT_EXTRA_GOTO_GENERAL_OVERLAY;
                    break;
                case 7:
                case 18:
                    goto_ = Customize.INTENT_EXTRA_GOTO_DASHBOARD_ICONS;
                    break;
                case 8:
                case 19:
                    goto_ = Customize.INTENT_EXTRA_GOTO_DASHBOARD_BACKGROUND;
                    break;
                case 9:
                case 20:
                    goto_ = Customize.INTENT_EXTRA_GOTO_DASHBOARD_GRID;
                    break;
                case 10:
                case 21:
                    goto_ = Customize.INTENT_EXTRA_GOTO_DASHBOARD_FOLDER_LOOK;
                    break;
                case 11:
                case 22:
                    goto_ = Customize.INTENT_EXTRA_GOTO_DASHBOARD_SYSTEM_BARS;
                    break;
                case 12:
                case 23:
                    goto_ = Customize.INTENT_EXTRA_GOTO_DASHBOARD_LAYOUT;
                    break;
                case 13:
                case 24:
                    goto_ = Customize.INTENT_EXTRA_GOTO_DASHBOARD_ZOOM_SCROLL;
                    break;
                case 14:
                case 25:
                    goto_ = Customize.INTENT_EXTRA_GOTO_DASHBOARD_EVENTS;
                    break;
                case 15:
                case 26:
                    goto_ = Customize.INTENT_EXTRA_GOTO_DASHBOARD_FOLDER_FEEL;
                    break;
                case 27:
                    goto_ = Customize.INTENT_EXTRA_GOTO_DASHBOARD_AD_MODES;
                    break;

                case 16:
                case 28:
                    goto_ = Customize.INTENT_EXTRA_GOTO_DASHBOARD_MISC;
                    break;
            }
            if (goto_ != null) {
                intent.putExtra(Customize.INTENT_EXTRA_GOTO, goto_);
            }

            if (position >= 6 && position < 17) {
                int current_page = LLApp.get().getAppEngine().readCurrentPage(Page.FIRST_DASHBOARD_PAGE);
                intent.putExtra(Customize.INTENT_EXTRA_PAGE_PATH, new ContainerPath(current_page).toString());
            } else if (position >= 17 && position < 29) {
                intent.putExtra(Customize.INTENT_EXTRA_PAGE_ID, new ContainerPath(Page.APP_DRAWER_PAGE).toString());
            }
		}
		
		createShortcutForIntent(name, intent);
	}

    public static Intent getShortcutForIntent(Activity activity, String name, Intent intent) {
        if(forTasker(activity)) {
            Intent result = new Intent();

            Bundle bundle = new Bundle();
            bundle.putString("i", intent.toUri(0));
            result.putExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE, bundle);
            result.putExtra(com.twofortyfouram.locale.Intent.EXTRA_STRING_BLURB, name);
            return result;
        } else {
            int p;
            String path = intent.getStringExtra(Customize.INTENT_EXTRA_PAGE_PATH);
            if(path == null) {
                p = intent.getIntExtra(Customize.INTENT_EXTRA_PAGE_ID, Page.FIRST_DASHBOARD_PAGE);
            } else {
                p = new ContainerPath(path).getLast();
            }
            int icon = p == Page.APP_DRAWER_PAGE ? R.drawable.all_apps : R.drawable.icon;
            Parcelable icon_resource = Intent.ShortcutIconResource.fromContext(activity, icon);

            Intent shortcut = new Intent();
            shortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent);
            shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);
            shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, icon_resource);

            return shortcut;
        }
    }

    private void createShortcutForIntent(String name, Intent intent) {
        setResult(RESULT_OK, getShortcutForIntent(this, name, intent));
        finish();
    }

    private void createScriptShortcut(int id, String data, int target) {

        Script script = LLApp.get().getAppEngine().getScriptManager().getOrLoadScript(id);
        String extra_data = Script.encodeIdAndData(id, data);

        if(forTasker(this)) {
            // for scripts triggered by Tasker, pass the data as a string extra otherwise '%' is escaped in Intent.toUri
            Intent result = new Intent();

            Bundle bundle = new Bundle();
            bundle.putInt(LightningIntent.INTENT_EXTRA_ACTION, GlobalConfig.RUN_SCRIPT);
            bundle.putString(LightningIntent.INTENT_EXTRA_DATA, extra_data);
            bundle.putInt(LightningIntent.INTENT_EXTRA_TARGET, target);
            if (TaskerPlugin.Setting.hostSupportsOnFireVariableReplacement(this)) {
                TaskerPlugin.Setting.setVariableReplaceKeys(bundle, new String[]{LightningIntent.INTENT_EXTRA_DATA});
            }
            result.putExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE, bundle);
            String data_e;
            if(data == null) {
                data_e = "";
            } else {
                if(data.length()>30) {
                    data_e = data.substring(0, 30)+"…";
                } else {
                    data_e = data;
                }
            }
            result.putExtra(com.twofortyfouram.locale.Intent.EXTRA_STRING_BLURB, script.name+data_e);
            setResult(RESULT_OK, result);
        } else {
            Intent intent = new Intent(Shortcuts.this, Dashboard.class);
            intent.putExtra(LightningIntent.INTENT_EXTRA_ACTION, GlobalConfig.RUN_SCRIPT);

            intent.putExtra(LightningIntent.INTENT_EXTRA_DATA, extra_data);
            createShortcutForIntent(script.name, intent);
        }
    }

    private void pickScriptAndCreateShortcut() {
        boolean for_tasker = forTasker(this);

        String initial_id_data = for_tasker ? getIntent().getStringExtra(LightningIntent.INTENT_EXTRA_DATA) : null;
        int initial_target = for_tasker ? getIntent().getIntExtra(LightningIntent.INTENT_EXTRA_TARGET, Script.TARGET_BACKGROUND) : Script.TARGET_NONE;
        ScriptPickerDialog dialog = new ScriptPickerDialog(this, LLApp.get().getAppEngine(), initial_id_data, initial_target, new ScriptPickerDialog.OnScriptPickerEvent() {
            @Override
            public void onScriptPicked(String id_data, int target) {
                final Pair<Integer, String> pair = Script.decodeIdAndData(id_data);
                createScriptShortcut(pair.first, pair.second, target);
                if(mIsForTaskerScript) {
                    finish();
                }
            }

            @Override
            public void onScriptPickerCanceled() {
                if(mIsForTaskerScript) {
                    finish();
                }
            }
        });
        dialog.show();
    }

    private void pickVariableAndCreateShortcut() {
        String data = getIntent().getStringExtra(LightningIntent.INTENT_EXTRA_DATA);
        Variable v = Variable.decode(data);
        new SetVariableDialog(this, v, new SetVariableDialog.OnSetVariableDialogListener() {
            @Override
            public void onSetVariableEdited(Variable variable) {
                Intent result = new Intent();

                Bundle bundle = new Bundle();
                bundle.putInt(LightningIntent.INTENT_EXTRA_ACTION, GlobalConfig.SET_VARIABLE);
                bundle.putString(LightningIntent.INTENT_EXTRA_DATA,  variable.encode());
                if (TaskerPlugin.Setting.hostSupportsOnFireVariableReplacement(Shortcuts.this)) {
                    TaskerPlugin.Setting.setVariableReplaceKeys(bundle, new String[]{LightningIntent.INTENT_EXTRA_DATA});
                }
                result.putExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE, bundle);
                result.putExtra(com.twofortyfouram.locale.Intent.EXTRA_STRING_BLURB, variable.describe());
                setResult(RESULT_OK, result);

                finish();
            }

            @Override
            public void onSetVariableCancel() {
                finish();
            }
        }).show();
    }

    private static boolean forTasker(Activity activity) {
        final String action = activity.getIntent().getAction();
        return action != null && action.equals(com.twofortyfouram.locale.Intent.ACTION_EDIT_SETTING);
    }
}
