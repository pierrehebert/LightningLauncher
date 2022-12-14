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

package net.pierrox.lightning_launcher.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import net.pierrox.lightning_launcher.LLApp;
import net.pierrox.lightning_launcher_extreme.R;
import net.pierrox.lightning_launcher.activities.ScriptEditor;
import net.pierrox.lightning_launcher.data.Utils;
import net.pierrox.lightning_launcher.engine.LightningEngine;
import net.pierrox.lightning_launcher.script.Script;
import net.pierrox.lightning_launcher.script.ScriptManager;

import java.io.File;
import java.util.ArrayList;

public class ScriptPickerDialog extends AlertDialog implements AdapterView.OnItemSelectedListener {

    private final CheckBox mHasDataCheckBox;
    private final EditText mDataTextView;
    private final Spinner mTargetsSpinner;
    private final Spinner mScriptSpinner;
    private Button mSelectDirectoryButton;
    private ArrayAdapter<Script> mScriptAdapter;
    private ArrayList<Script> mAllScripts;
    private final ScriptManager mScriptManager;
    private Script mScript;

    private int mInitialTarget;
    private OnScriptPickerEvent mListener;

    private static boolean sShowSubDirs;
    private static File sCurrentDirectory;

    public interface OnScriptPickerEvent {

        public void onScriptPicked(String id_data, int target);
        public void onScriptPickerCanceled();
    }

    public ScriptPickerDialog(final Context context, LightningEngine engine, String initial_id_data, final int initial_target, final OnScriptPickerEvent listener) {
        super(context);


        mInitialTarget = initial_target;
        mListener = listener;

        mScriptManager = engine.getScriptManager();
        if(sCurrentDirectory == null) {
            sCurrentDirectory = mScriptManager.getScriptsDir();
        }

        mAllScripts = mScriptManager.getAllScriptMatching(Script.FLAG_ALL);

        setTitle(R.string.sst);
        View content = ((LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.dialog_select_script, null);
        setView(content);
        mScriptSpinner = (Spinner) content.findViewById(R.id.script);
        mTargetsSpinner = (Spinner) content.findViewById(R.id.tg);
        final Button sc_new = (Button) content.findViewById(R.id.sc_new);
        mHasDataCheckBox = (CheckBox) content.findViewById(R.id.has_data);
        mDataTextView = (EditText) content.findViewById(R.id.data);
        ((TextView)content.findViewById(R.id.tgt)).setText(R.string.tg);
        ((TextView)content.findViewById(R.id.sc_path)).setText(R.string.sc_path);
        ((TextView)content.findViewById(R.id.sc_name)).setText(R.string.sc_name);
        sc_new.setText(R.string.sc_new);
        Button editButton = (Button) content.findViewById(R.id.edit);
        editButton.setTypeface(LLApp.get().getIconsTypeface());
        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mScript != null) {
                    ScriptEditor.startActivity(context, mScript.id, 1);
                }
            }
        });

        mSelectDirectoryButton = (Button) content.findViewById(R.id.sc_d);
        mSelectDirectoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FileAndDirectoryPickerDialog.showForScriptDirectory(context, sCurrentDirectory, mScriptManager, new FileAndDirectoryPickerDialog.FileAndDirectoryPickerDialogListener() {
                    @Override
                    public void onFileSelected(File file, File newDirectory) {
                        sCurrentDirectory = file;
                        updateSelectDirectoryButton();
                        updateScriptsSpinner();
                        if(!mAllScripts.contains(mScript) && mAllScripts.size() > 0) {
                            mScript = mAllScripts.get(0);
                        }
                    }
                });
            }
        });
        updateSelectDirectoryButton();

        CheckBox showSubDirs = (CheckBox) content.findViewById(R.id.sc_sd);
        showSubDirs.setText(R.string.sc_all);
        showSubDirs.setChecked(sShowSubDirs);
        showSubDirs.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                sShowSubDirs = isChecked;
                updateScriptsSpinner();
                if(!mAllScripts.contains(mScript) && mAllScripts.size() > 0) {
                    mScript = mAllScripts.get(0);
                }
            }
        });

        sc_new.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int id = mScriptManager.findFreeScriptId();
                Script new_script = mScriptManager.createScriptForFile(mScriptManager.getDefaultScriptName(context), "/");
                mScriptManager.saveScript(new_script);

                scriptPicked(id, getSelectedData(), getSelectedTarget());

                LLApp.get().startScriptEditor(id, -1);
            }
        });
        mHasDataCheckBox.setText(R.string.sc_data);

        mScriptAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, mAllScripts);
        mScriptAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        updateScriptsSpinner();

        mScriptSpinner.setOnItemSelectedListener(this);
        mScriptSpinner.setAdapter(mScriptAdapter);
        mScriptSpinner.requestFocus();

        View targets_group = content.findViewById(R.id.tgg);
        if(mInitialTarget == Script.TARGET_NONE) {
            targets_group.setVisibility(View.GONE);
        } else {
            String[] target_names = new String[]{
                    context.getString(R.string.tg_d),
                    context.getString(R.string.tg_ad),
                    context.getString(R.string.tg_ls),
                    context.getString(R.string.tg_bg),
            };
            ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, target_names);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mTargetsSpinner.setAdapter(adapter);
            mTargetsSpinner.setSelection(mInitialTarget);
            targets_group.setVisibility(View.VISIBLE);
        }

        mHasDataCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mDataTextView.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                if(isChecked) mDataTextView.requestFocus();
                else mDataTextView.clearFocus();
            }
        });
        if(initial_id_data != null) {
            final Pair<Integer, String> pair = Script.decodeIdAndData(initial_id_data);
            mScript = mScriptManager.getOrLoadScript(pair.first);
            if(mScript != null) {
                if (sShowSubDirs) {
                    File root = mScriptManager.getScriptsDir();
                    for(;;) {
                        updateScriptsSpinner();
                        if(sCurrentDirectory.equals(root)) {
                            break;
                        }

                        if(mAllScripts.contains(mScript)) {
                            break;
                        }

                        sCurrentDirectory = sCurrentDirectory.getParentFile();
                    }
                } else {
                    sCurrentDirectory = mScript.getFile().getParentFile();
                    updateScriptsSpinner();
                }
                updateSelectDirectoryButton();
            }
            final boolean had_data = pair.second != null;
            mHasDataCheckBox.setChecked(had_data);
            mDataTextView.setText(had_data ? pair.second : "");
            mDataTextView.setVisibility(had_data ? View.VISIBLE : View.GONE);
        } else {
            mDataTextView.setVisibility(View.GONE);
        }

        setButton(BUTTON_POSITIVE, context.getString(android.R.string.ok), new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                scriptPicked(getSelectedScriptId(), getSelectedData(), getSelectedTarget());
            }
        });
        setButton(BUTTON_NEGATIVE, context.getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mListener.onScriptPickerCanceled();
                dismiss();
            }
        });
        setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                listener.onScriptPickerCanceled();
            }
        });
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        Script script = (Script) adapterView.getItemAtPosition(i);
        mScript = script;
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
        // pass
    }

    private String getSelectedData() {
        return mHasDataCheckBox.isChecked() ? mDataTextView.getText().toString() : null;
    }

    private int getSelectedTarget() {
        return mInitialTarget == Script.TARGET_NONE ? Script.TARGET_NONE : mTargetsSpinner.getSelectedItemPosition();
    }

    private int getSelectedScriptId() {
        return mAllScripts.get(mScriptSpinner.getSelectedItemPosition()).id;
    }

    private void scriptPicked(int id, String data, int target) {
        mListener.onScriptPicked(Script.encodeIdAndData(id, data), target);
        dismiss();
    }

    private void updateScriptsSpinner() {
        ArrayList<Script> allScripts = mScriptManager.getAllScriptMatching(Script.FLAG_ALL);
        String currentPath = mScriptManager.getRelativePath(sCurrentDirectory);
        if(!currentPath.endsWith("/")) {
            // ensure a trailing slash to avoid false match (like /ab and /ac whith current = /a)
            currentPath += "/";
        }
        mAllScripts.clear();
        for (Script script : allScripts) {
            if(script.getType() == Script.TYPE_FILE) {
                String relativePath = script.getRelativePath();
                if(!relativePath.endsWith("/")) {
                    relativePath += "/";
                }
                if((sShowSubDirs && relativePath.startsWith(currentPath)) || relativePath.equals(currentPath)) {
                    mAllScripts.add(script);
                }
            }
        }
        Utils.sortScripts(mAllScripts);

        mScriptAdapter.notifyDataSetChanged();
        int position = mAllScripts.indexOf(mScript);
        if(position != -1) {
            mScriptSpinner.setSelection(position);
        }
    }

    private void updateSelectDirectoryButton() {
        mSelectDirectoryButton.setText(mScriptManager.getRelativePath(sCurrentDirectory));
    }
}
