package net.pierrox.lightning_launcher.util;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import net.pierrox.lightning_launcher.LLApp;
import net.pierrox.lightning_launcher.engine.variable.Variable;
import net.pierrox.lightning_launcher_extreme.R;

import java.util.ArrayList;

public class SetVariableDialog extends AlertDialog implements DialogInterface.OnClickListener, View.OnClickListener {
    public interface OnSetVariableDialogListener {
        public void onSetVariableEdited(Variable variable);
        public void onSetVariableCancel();
    }

    private Variable mInitValue;
    private OnSetVariableDialogListener mListener;

    private EditText mNameEditText;
    private EditText mValueEditText;

    public SetVariableDialog(Context context, Variable init_value, OnSetVariableDialogListener listener) {
        super(context);

        mInitValue = init_value;
        mListener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        View view = getLayoutInflater().inflate(R.layout.set_var_dialog, null);

        ((TextView)view.findViewById(R.id.sv_nt)).setText(R.string.sv_n);
        ((TextView)view.findViewById(R.id.sv_vt)).setText(R.string.bd_v);

        mNameEditText = (EditText) view.findViewById(R.id.sv_n);
        mValueEditText = (EditText) view.findViewById(R.id.sv_v);

        Button select_var = (Button) view.findViewById(R.id.sv_ns);
        select_var.setTypeface(LLApp.get().getIconsTypeface());
        select_var.setOnClickListener(this);

        if(mInitValue != null) {
            mNameEditText.setText(mInitValue.name);
            mValueEditText.setText(mInitValue.value.toString());
        }

        setView(view);

        setButton(BUTTON_POSITIVE, getContext().getString(android.R.string.ok), this);
        setButton(BUTTON_NEGATIVE, getContext().getString(android.R.string.cancel), this);

        super.onCreate(savedInstanceState);

        final Button ok_button = getButton(BUTTON_POSITIVE);
        mNameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // pass
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // pass
            }

            @Override
            public void afterTextChanged(Editable s) {
                ok_button.setEnabled(s.length()>0);
            }
        });
    }

    @Override
    public void cancel() {
        super.cancel();
        mListener.onSetVariableCancel();
    }

    @Override
    public void onClick(View v) {
        new VariableSelectionDialog(getContext()).show();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case BUTTON_POSITIVE:
                mListener.onSetVariableEdited(new Variable(mNameEditText.getText().toString(), mValueEditText.getText().toString()));
                break;

            case BUTTON_NEGATIVE:
                mListener.onSetVariableCancel();
                break;
        }
    }

    private class VariableSelectionDialog extends AlertDialog implements AdapterView.OnItemClickListener {
        private ArrayList<Variable> mUserVariables;

        public VariableSelectionDialog(Context context) {
            super(context);

            mUserVariables = LLApp.get().getAppEngine().getVariableManager().getUserVariables();
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            if(mUserVariables.size() == 0) {
                setMessage(getContext().getString(R.string.uv_e));
                setButton(BUTTON_POSITIVE, getContext().getString(android.R.string.ok), (OnClickListener)null);
            } else {
                setTitle(R.string.bv_pick);

                ListView list_view = new ListView(getContext());
                list_view.setOnItemClickListener(this);

                list_view.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, mUserVariables));

                setView(list_view);
            }
            super.onCreate(savedInstanceState);
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            mNameEditText.setText(mUserVariables.get(position).name);
            dismiss();
        }
    }
}
