package net.pierrox.lightning_launcher.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.mobeta.android.dslv.DragSortListView;

import net.pierrox.lightning_launcher.LLApp;
import net.pierrox.lightning_launcher.configuration.GlobalConfig;
import net.pierrox.lightning_launcher.data.Action;
import net.pierrox.lightning_launcher.data.ActionsDescription;
import net.pierrox.lightning_launcher.data.EventAction;
import net.pierrox.lightning_launcher.data.JsonLoader;
import net.pierrox.lightning_launcher.data.LightningIntent;
import net.pierrox.lightning_launcher.data.Utils;
import net.pierrox.lightning_launcher.engine.LightningEngine;
import net.pierrox.lightning_launcher.engine.variable.Variable;
import net.pierrox.lightning_launcher.script.Script;
import net.pierrox.lightning_launcher.util.ScriptPickerDialog;
import net.pierrox.lightning_launcher.util.SetVariableDialog;
import net.pierrox.lightning_launcher_extreme.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class EventActionSetup extends ResourceWrapperActivity implements AdapterView.OnItemClickListener {
    private static final String INTENT_EXTRA_FOR_ITEM = "i";
    private static final String INTENT_EXTRA_FOR_SHORTCUT = "s";
    private static final String INTENT_EXTRA_TYPE = "t";
    private static final String INTENT_EXTRA_EVENT_ACTION_LIST = "l";

    private static final String SIS_EVENT_ACTION_PICK = "p";
    private static final String SIS_EVENT_ACTION_PICK_NEW = "n";

    private static final int REQUEST_PICK_ACTIVITY = 1;
    private static final int REQUEST_PICK_SHORTCUT1 = 2;
    private static final int REQUEST_PICK_SHORTCUT2 = 3;
    private static final int REQUEST_PICK_DESKTOP_POSITION_SHORTCUT = 4;


    private ArrayList<EventAction> mEventActions;
    private boolean mForItem;
    private int mType;
    private boolean mForShortcut;

    private ActionsDescription mActions;
    private EventActionAdapter mAdapter;

    private EventAction mEventActionForPick;
    private boolean mEventActionForPickNew;
    private LightningEngine mAppEngine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.setTheme(this, Utils.APP_THEME_NO_ACTION_BAR);

        super.onCreate(savedInstanceState);

        mAppEngine = LLApp.get().getAppEngine();

        mEventActions = new ArrayList<>();

        Intent intent = getIntent();
        mForItem = intent.getBooleanExtra(INTENT_EXTRA_FOR_ITEM, false);
        mForShortcut = intent.getBooleanExtra(INTENT_EXTRA_FOR_SHORTCUT, true);
        mType = intent.getIntExtra(INTENT_EXTRA_TYPE, Action.FLAG_TYPE_DESKTOP);

        String s;
        if(savedInstanceState != null) {
            s = savedInstanceState.getString(INTENT_EXTRA_EVENT_ACTION_LIST);

            String p = savedInstanceState.getString(SIS_EVENT_ACTION_PICK);
            mEventActionForPickNew = savedInstanceState.getBoolean(SIS_EVENT_ACTION_PICK_NEW);
            if(p != null) {
                mEventActionForPick = new EventAction();
                try {
                    JsonLoader.loadFieldsFromJSONObject(mEventActionForPick, new JSONObject(p), null);
                } catch (JSONException e) {
                    // pass
                }
            }
        } else {
            s = intent.getStringExtra(INTENT_EXTRA_EVENT_ACTION_LIST);
        }

        deserializeEventActionList(s);

        mActions = new ActionsDescription(this, mType, mForItem);

        // XXX horrible hack : reuse the layout made by alert dialog, can't access the xml layout directly
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.eas_ttl);
        builder.setView(getLayoutInflater().inflate(R.layout.event_action_setup, null));
        builder.setPositiveButton(R.string.eas_done, null);
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.setNeutralButton(R.string.eas_add, null);

        AlertDialog dialog = builder.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveAndFinish();
            }
        });
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addAction();
            }
        });

        ViewGroup decorView = (ViewGroup) dialog.getWindow().getDecorView();
        Window window = getWindow();
        window.setBackgroundDrawable(decorView.getBackground());
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        View dialogContent = ((ViewGroup)decorView.findViewById(android.R.id.content)).getChildAt(0);
        ((ViewGroup)dialogContent.getParent()).removeView(dialogContent);
        dialog.dismiss();

        setContentView(dialogContent);

        final DragSortListView list = (DragSortListView) dialogContent.findViewById(android.R.id.list);
        mAdapter = new EventActionAdapter(this, mEventActions);
        list.setAdapter(mAdapter);
        list.setOnItemClickListener(this);
        list.setDropListener(new DragSortListView.DragSortListener() {
            @Override
            public void drag(int from, int to) {

            }

            @Override
            public void onItemHovered(int position) {

            }

            @Override
            public void drop(int from, int to) {
                EventAction removed = mEventActions.remove(from);
                mEventActions.add(to, removed);
                mAdapter.notifyDataSetChanged();
            }

            @Override
            public void remove(int which) {
                mEventActions.remove(which);
                mAdapter.notifyDataSetChanged();
            }
        });

        if(mEventActions.size() == 0) {
            addAction();
        }
    }

    @Override
    public void onBackPressed() {
        saveAndFinish();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(INTENT_EXTRA_EVENT_ACTION_LIST, serializeEventAction());

        if(mEventActionForPick != null) {
            outState.putString(SIS_EVENT_ACTION_PICK, JsonLoader.toJSONObject(mEventActionForPick, null).toString());
            outState.putBoolean(SIS_EVENT_ACTION_PICK_NEW, mEventActionForPickNew);
        }
    }

    public static void startActivityForResult(Activity activity, EventAction ea, boolean forItem, int type, boolean forShortcut, int requestCode) {
        Intent intent = new Intent(activity, EventActionSetup.class);
        intent.putExtra(INTENT_EXTRA_FOR_ITEM, forItem);
        intent.putExtra(INTENT_EXTRA_FOR_SHORTCUT, forShortcut);
        intent.putExtra(INTENT_EXTRA_TYPE, type);
        if(ea != null) {
            JSONObject out = new JSONObject();
            JsonLoader.toJSONObject(out, ea, null);
            intent.putExtra(INTENT_EXTRA_EVENT_ACTION_LIST, out.toString());
        }
        activity.startActivityForResult(intent, requestCode);
    }

    public static EventAction getEventActionFromIntent(Intent intent) {
        String s = intent.getStringExtra(INTENT_EXTRA_EVENT_ACTION_LIST);
        if(s != null) {
            try {
                JSONObject json = new JSONObject(s);
                EventAction ea = new EventAction();
                JsonLoader.loadFieldsFromJSONObject(ea, json, null);
                return ea;
            } catch (JSONException e) {
                // pass
            }
        }

        return null;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        editAction(mAdapter.getItem(i), false);
    }

    private void deserializeEventActionList(String s) {
        mEventActions = new ArrayList<>();
        if(s != null) {
            try {
                JSONObject json = new JSONObject(s);
                EventAction ea = new EventAction();
                JsonLoader.loadFieldsFromJSONObject(ea, json, null);

                while(ea != null) {
                    if(ea.action != GlobalConfig.UNSET) {
                        mEventActions.add(ea);
                    }
                    EventAction ea_tmp = ea.next;
                    ea.next = null;
                    ea = ea_tmp;
                }
            } catch (JSONException e) {
                // pass
            }
        }
    }

    private String serializeEventAction() {
        EventAction current = null;
        EventAction first = null;
        for (EventAction ea : mEventActions) {
            if(ea.action == GlobalConfig.UNSET) continue;
            if(current == null) {
                current = ea;
                first = ea;
            } else {
                current.next = ea;
                current = ea;
            }
        }

        if(first == null) {
            first = new EventAction(GlobalConfig.UNSET, null);
        }
        JSONObject out = JsonLoader.toJSONObject(first, null);

        // remove next links so that the array is now flat again
        for (EventAction ea : mEventActions) {
            ea.next = null;
        }

        return out.toString();
    }

    private void saveAndFinish() {
        String serializedEventAction = serializeEventAction();
        Intent intent;
        if(mForShortcut) {
            Intent i = new Intent(this, Dashboard.class);
            i.putExtra(LightningIntent.INTENT_EXTRA_EVENT_ACTION, serializedEventAction);
            String name = mActions.getActionName(mEventActions.size() == 0 ? GlobalConfig.UNSET : mEventActions.get(0).action);
            intent = Shortcuts.getShortcutForIntent(this, name, i);
        } else {
            intent = new Intent();
            intent.putExtra(INTENT_EXTRA_EVENT_ACTION_LIST, serializedEventAction);
        }

        setResult(RESULT_OK, intent);
        finish();
    }

    private void addAction() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.br_a);
        ListView list = new ListView(this);
        final ActionsAdapter adapter = new ActionsAdapter(this, mActions);
        list.setAdapter(adapter);
        builder.setView(list);
        final AlertDialog dialog = builder.create();
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Action action = adapter.getItem(i);
                EventAction ea = new EventAction(action.action, null);
                mEventActions.add(ea);
                mAdapter.notifyDataSetChanged();
                dialog.dismiss();
                editAction(ea, true);
            }
        });
        dialog.show();
    }

    private void editAction(EventAction ea, boolean forNew) {
        mEventActionForPick = ea;
        mEventActionForPickNew = forNew;
        int new_action = ea.action;
        if (new_action == GlobalConfig.LAUNCH_APP) {
            Intent picker = new Intent(this, AppDrawerX.class);
            picker.setAction(Intent.ACTION_PICK_ACTIVITY);
            startActivityForResult(picker, REQUEST_PICK_ACTIVITY);
        } else if (new_action == GlobalConfig.LAUNCH_SHORTCUT) {
            Intent i = new Intent(Intent.ACTION_PICK_ACTIVITY);
            i.putExtra(Intent.EXTRA_INTENT, new Intent(Intent.ACTION_CREATE_SHORTCUT));
            i.putExtra(Intent.EXTRA_TITLE, getString(R.string.tools_pick_shortcut));
            try { startActivityForResult(i, REQUEST_PICK_SHORTCUT1); } catch (Exception e) { }
        } else if (new_action == GlobalConfig.RUN_SCRIPT) {
            ScriptPickerDialog dialog = new ScriptPickerDialog(this, mAppEngine, ea.data, Script.TARGET_NONE, new ScriptPickerDialog.OnScriptPickerEvent() {
                @Override
                public void onScriptPicked(String id_data, int target) {
                    mEventActionForPick.data = id_data;
                    mAdapter.notifyDataSetChanged();
                }

                @Override
                public void onScriptPickerCanceled() {
                    if(mEventActionForPickNew) {
                        mEventActions.remove(mEventActionForPick);
                        mAdapter.notifyDataSetChanged();
                    }
                }
            });
            dialog.show();
        } else if (new_action == GlobalConfig.OPEN_FOLDER) {
            Utils.createFolderSelectionDialog(this, LLApp.get().getAppEngine(), new Utils.OnFolderSelectionDialogDone() {
                @Override
                public void onFolderSelected(String name, int page) {
                    mEventActionForPick.data = String.valueOf(page);
                    mAdapter.notifyDataSetChanged();
                }

                @Override
                public void onNoFolderSelected() {
                    if(mEventActionForPickNew) {
                        mEventActions.remove(mEventActionForPick);
                        mAdapter.notifyDataSetChanged();
                    }
                }
            }).show();
        } else if (new_action == GlobalConfig.GO_DESKTOP_POSITION) {
            Intent intent = new Intent(Intent.ACTION_CREATE_SHORTCUT);
            intent.setClass(this, ScreenManager.class);
            startActivityForResult(intent, REQUEST_PICK_DESKTOP_POSITION_SHORTCUT);
        } else if (new_action == GlobalConfig.SET_VARIABLE) {
            Variable v = Variable.decode(ea.data);
            new SetVariableDialog(this, v, new SetVariableDialog.OnSetVariableDialogListener() {
                @Override
                public void onSetVariableEdited(Variable variable) {
                    mEventActionForPick.data = variable.encode();
                    mAdapter.notifyDataSetChanged();
                }

                @Override
                public void onSetVariableCancel() {
                    if(mEventActionForPickNew) {
                        mEventActions.remove(mEventActionForPick);
                        mAdapter.notifyDataSetChanged();
                    }
                }
            }).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_PICK_SHORTCUT1) {
            if (resultCode == RESULT_OK) {
                startActivityForResult(data, REQUEST_PICK_SHORTCUT2);
            } else {
                if(mEventActionForPickNew) {
                    mEventActions.remove(mEventActionForPick);
                    mAdapter.notifyDataSetChanged();
                }
            }
        } else if (requestCode == REQUEST_PICK_ACTIVITY || requestCode == REQUEST_PICK_SHORTCUT2 || requestCode == REQUEST_PICK_DESKTOP_POSITION_SHORTCUT) {
            if (resultCode == RESULT_OK) {
                try {
                    Intent i = requestCode == REQUEST_PICK_ACTIVITY ? data : (Intent) data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
                    mEventActionForPick.data = i.toUri(0);
                    mAdapter.notifyDataSetChanged();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                if(mEventActionForPickNew) {
                    mEventActions.remove(mEventActionForPick);
                    mAdapter.notifyDataSetChanged();
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private class EventActionAdapter extends ArrayAdapter<EventAction> implements View.OnClickListener {

        public EventActionAdapter(Context context, List<EventAction> objects) {
            super(context, 0, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.event_action_item, parent, false);
                TextView drag = (TextView) convertView.findViewById(R.id.drag);
                drag.setTypeface(LLApp.get().getIconsTypeface());
                TextView delete = (TextView) convertView.findViewById(R.id.delete);
                delete.setOnClickListener(this);
                delete.setTypeface(LLApp.get().getIconsTypeface());
            }

            EventAction ea = getItem(position);

            TextView title = (TextView) convertView.findViewById(android.R.id.text1);
            title.setText(mActions.getActionName(ea.action));

            String summaryText = ea.describe(mAppEngine);
            TextView summary = (TextView) convertView.findViewById(android.R.id.text2);
            if(summaryText == null) {
                title.setSingleLine(false);
                title.setMaxLines(2);
                summary.setVisibility(View.GONE);
            } else {
                title.setSingleLine(true);
                summary.setVisibility(View.VISIBLE);
                summary.setText(summaryText);
            }

            convertView.findViewById(R.id.delete).setTag(ea);

            return convertView;
        }

        @Override
        public void onClick(View view) {
            EventAction ea = (EventAction) view.getTag();
            mEventActions.remove(ea);
            mAdapter.notifyDataSetChanged();
        }
    }

    private static class ActionsAdapter extends ArrayAdapter<Action> {

        private final LayoutInflater mInflater;
        private int mPrefLayout;
        private int mPrefCategoryLayout;

        public ActionsAdapter(Context context, ActionsDescription actionsDescription) {
            super(context, 0, filterActions(actionsDescription));


            mInflater = LayoutInflater.from(context);
            mPrefLayout = new Preference(context).getLayoutResource();
            mPrefCategoryLayout = new PreferenceCategory(context).getLayoutResource();
        }

        private static ArrayList<Action> filterActions(ActionsDescription actionsDescription) {
            ArrayList<Action> actions = actionsDescription.getActions();
            ArrayList<Action> filteredActions = new ArrayList<>(actions.size());

            for (Action action : actions) {
                if(action.action != GlobalConfig.UNSET) {
                    filteredActions.add(action);
                }
            }

            return filteredActions;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            return getItem(position).action == GlobalConfig.CATEGORY ? 0 : 1;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Action action = getItem(position);
            boolean isCategory = action.action == GlobalConfig.CATEGORY;
            if(convertView == null) {
                convertView = mInflater.inflate(isCategory ? mPrefCategoryLayout : mPrefLayout, parent, false);
            }

            TextView title_view = (TextView) convertView.findViewById(android.R.id.title);
            title_view.setText(action.label);
            if(!isCategory) {
                title_view.setSingleLine(false);
                title_view.setMaxLines(2);
                convertView.findViewById(android.R.id.summary).setVisibility(View.GONE);
                View icon = convertView.findViewById(android.R.id.icon);
                if(icon != null) icon.setVisibility(View.GONE);
            }

            return convertView;
        }
    }
}
