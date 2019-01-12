package net.pierrox.lightning_launcher.util;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import net.pierrox.lightning_launcher.LLApp;
import net.pierrox.lightning_launcher.data.Item;
import net.pierrox.lightning_launcher.engine.LightningEngine;
import net.pierrox.lightning_launcher.engine.variable.Binding;
import net.pierrox.lightning_launcher.engine.variable.BuiltinVariable;
import net.pierrox.lightning_launcher.engine.variable.Variable;
import net.pierrox.lightning_launcher.script.api.Property;
import net.pierrox.lightning_launcher.views.IconLabelView;
import net.pierrox.lightning_launcher.views.IconView;
import net.pierrox.lightning_launcher.views.SharedAsyncGraphicsDrawable;
import net.pierrox.lightning_launcher.views.item.ItemView;
import net.pierrox.lightning_launcher.views.item.ShortcutView;
import net.pierrox.android.lsvg.SvgDrawable;
import net.pierrox.android.lsvg.SvgElement;
import net.pierrox.android.lsvg.SvgGroup;
import net.pierrox.android.lsvg.SvgPath;
import net.pierrox.lightning_launcher_extreme.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BindingEditDialog extends AlertDialog implements DialogInterface.OnClickListener, View.OnClickListener {
    public interface OnBindingEditDialogListener {
        public void onBindingEdited(Binding binding, boolean open_in_script_editor);
    }

    private Binding mInitValue;
    private Binding[] mOtherBindings;
    private Property mSelectedProperty;
    private Button mTargetButton;
    private EditText mFormulaEditText;
    private Button mEditButton;
    private Button mOkButton;
    private OnBindingEditDialogListener mListener;
    private ArrayList<Pair<String,ArrayList<Property>>> mProperties;

    public BindingEditDialog(Context context, Binding init_value, ItemView itemView, OnBindingEditDialogListener listener) {
        super(context);

        Item item = itemView.getItem();
        mInitValue = init_value;
        mOtherBindings = item.getItemConfig().bindings;
        mListener = listener;

        // build the list of available properties minus the one already used
        Class<? extends Item> itemClass = item.getClass();
        List<Pair<String, Property[]>> all_properties = Arrays.asList(Property.getForItemClass(itemClass));

        mProperties = new ArrayList<>();
        for(Pair<String,Property[]> pair_from : all_properties) {
            ArrayList<Property> available_properties = new ArrayList<>();
            Pair<String,ArrayList<Property>> pair_to = new Pair<>(pair_from.first, available_properties);

            for(Property p : pair_from.second) {
                if(!isPropertyUsed(p.getName())) {
                    available_properties.add(p);
                }
            }

            mProperties.add(pair_to);
        }

        if(itemView.getClass() == ShortcutView.class) {
            IconLabelView il = ((ShortcutView) itemView).getIconLabelView();
            if(il != null) {
                IconView iv = il.getIconView();
                if(iv != null) {
                    addSvgProperties(R.string.svg_icon, "svg/icon/", mProperties, iv.getSharedAsyncGraphicsDrawable());
                }
            }
        }
        addSvgProperties(R.string.svg_bgn, "svg/bgn/", mProperties, item.getItemConfig().box.bgNormal);
        addSvgProperties(R.string.svg_bgs, "svg/bgs/", mProperties, item.getItemConfig().box.bgNormal);
        addSvgProperties(R.string.svg_bgf, "svg/bgf/", mProperties, item.getItemConfig().box.bgNormal);
    }

    private boolean isPropertyUsed(String name) {
        if (mOtherBindings != null) {
            for (Binding b : mOtherBindings) {
                if (name.equals(b.target)) {
                    return true;
                }
            }
        }

        return false;
    }

    private void addSvgProperties(int nameRes, String prefix, List<Pair<String, ArrayList<Property>>> all_properties, Drawable drawable) {
        if(!(drawable instanceof SharedAsyncGraphicsDrawable)) {
            return;
        }

        SharedAsyncGraphicsDrawable sd = (SharedAsyncGraphicsDrawable) drawable;

        if(sd.getType() != SharedAsyncGraphicsDrawable.TYPE_SVG) {
            return;
        }

        SvgDrawable svgDrawable = sd.getSvgDrawable();

        String name = getContext().getString(nameRes);
        ArrayList<Property> properties = new ArrayList<>();
        Pair<String,ArrayList<Property>> pair = new Pair<>(name, properties);

        addSvgPropertiesForElement(prefix, svgDrawable.getSvgRoot(), properties);

        Collections.sort(properties, new Comparator<Property>() {
            @Override
            public int compare(Property o1, Property o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        if(properties.size() > 0) {
            all_properties.add(pair);
        }
    }

    private void addSvgPropertiesForElement(String prefix, SvgElement element, ArrayList<Property> properties) {
        String id = element.getId();
        if(element instanceof SvgGroup) {
            for(SvgElement child : ((SvgGroup)element).getChildren()) {
                addSvgPropertiesForElement(prefix, child, properties);
            }
        }

        if(id != null) {
            Context context = getContext();
            if(element instanceof SvgPath) {
                properties.add(new Property(context.getString(R.string.svgp_path, id), prefix+id+"/path", Property.TYPE_STRING));
                properties.add(new Property(context.getString(R.string.svgp_style, id), prefix+id+"/style", Property.TYPE_STRING));
                properties.add(new Property(context.getString(R.string.svgp_transform, id), prefix+id+"/transform", Property.TYPE_STRING));
            } else if(element instanceof SvgGroup) {
                properties.add(new Property(context.getString(R.string.svgp_transform, id), prefix+id+"/transform", Property.TYPE_STRING));
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        View view = getLayoutInflater().inflate(R.layout.ll_pref_binding_dialog, null);

        ((TextView)view.findViewById(R.id.bd_tt)).setText(R.string.bd_p);
        ((TextView)view.findViewById(R.id.bd_tf)).setText(R.string.bd_v);

        mTargetButton = (Button) view.findViewById(R.id.bd_t);
        mSelectedProperty = mInitValue == null ? null : Property.getByName(mInitValue.target);
        mTargetButton.setText(mSelectedProperty == null ? getContext().getString(R.string.bd_s) : mSelectedProperty.getLabel());
        mTargetButton.setEnabled(mInitValue == null);
        mTargetButton.setOnClickListener(this);

        Button builtin = (Button) view.findViewById(R.id.bd_fb);
        builtin.setTypeface(LLApp.get().getIconsTypeface());
        builtin.setOnClickListener(this);

        mEditButton = (Button) view.findViewById(R.id.bd_fe);
        mEditButton.setTypeface(LLApp.get().getIconsTypeface());
        mEditButton.setOnClickListener(this);
        mEditButton.setEnabled(mInitValue != null);

        mFormulaEditText = (EditText) view.findViewById(R.id.bd_f);
        if(mInitValue != null) {
            mFormulaEditText.setText(mInitValue.formula);
        }

        setView(view);

        setButton(BUTTON_POSITIVE, getContext().getString(android.R.string.ok), this);
        setButton(BUTTON_NEGATIVE, getContext().getString(android.R.string.cancel), this);

        super.onCreate(savedInstanceState);

        mOkButton = getButton(BUTTON_POSITIVE);
        mOkButton.setEnabled(mInitValue != null);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case BUTTON_POSITIVE:
                save(false);
                break;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bd_t:
                new PropertySelectionDialog(getContext()).show();
                break;

            case R.id.bd_fb:
                new VariableSelectionDialog(getContext()).show();
                break;

            case R.id.bd_fe:
                save(true);
                dismiss();
                break;
        }
    }

    private void save(boolean open_in_script_editor) {
        String target =mSelectedProperty.getName();
        String formula = mFormulaEditText.getText().toString();
        Binding binding = new Binding(target, formula, mInitValue==null ? true : mInitValue.enabled);
        mListener.onBindingEdited(binding, open_in_script_editor);
    }

    private static final String ROOT_KEY = "r";
    private static final String CHILD_KEY = "c";

    private class PropertySelectionDialog extends Dialog implements ExpandableListView.OnChildClickListener {

        public PropertySelectionDialog(Context context) {
            super(context);
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            setTitle(R.string.bd_p);
            ExpandableListView list = new ExpandableListView(getContext());

            List<Map<String, String>> groupData = new ArrayList<>();
            List<List<Map<String, String>>> childData = new ArrayList<>();

            for(Pair<String,ArrayList<Property>> category : mProperties) {
                HashMap<String, String> value = new HashMap<>();
                value.put(ROOT_KEY, category.first);
                groupData.add(value);

                List<Map<String, String>> child_values = new ArrayList<>();
                for(Property p : category.second) {
                    HashMap<String, String> child_value = new HashMap<>();
                    child_value.put(CHILD_KEY, p.getLabel());
                    child_values.add(child_value);
                }
                childData.add(child_values);
            }

            SimpleExpandableListAdapter adapter = new SimpleExpandableListAdapter(
                    getContext(),

                    groupData,
                    android.R.layout.simple_expandable_list_item_1,
                    new String[] { ROOT_KEY },
                    new int[] { android.R.id.text1 },

                    childData,
                    android.R.layout.simple_expandable_list_item_1,
                    new String[] { CHILD_KEY },
                    new int[] { android.R.id.text1 }

            );
            list.setAdapter(adapter);
            list.setOnChildClickListener(this);
            setContentView(list);
        }

        @Override
        public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
            mSelectedProperty = mProperties.get(groupPosition).second.get(childPosition);
            mTargetButton.setText(mSelectedProperty.getLabel());
            mEditButton.setEnabled(true);
            mOkButton.setEnabled(true);
            dismiss();
            return true;
        }
    }

    private class VariableSelectionDialog extends Dialog implements ExpandableListView.OnChildClickListener {
        private SimpleExpandableListAdapter mAdapter;
        private Pair<String,BuiltinVariable[]>[] mBuiltinVariables;
        private ArrayList<Variable> mUserVariables;

        public VariableSelectionDialog(Context context) {
            super(context);
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            setTitle(R.string.bv_pick);
            ExpandableListView list = new ExpandableListView(getContext());


            // build the list of variables: builtins + existing other ones
            LightningEngine engine = LLApp.get().getAppEngine();
            mBuiltinVariables = engine.getBuiltinDataCollectors().getBuiltinVariables();
            mUserVariables = engine.getVariableManager().getUserVariables();

            // build the adapter
            List<Map<String, String>> groupData = new ArrayList<>();
            List<List<Map<String, String>>> childData = new ArrayList<>();

            for(Pair<String,BuiltinVariable[]> category : mBuiltinVariables) {
                HashMap<String, String> value = new HashMap<>();
                value.put(ROOT_KEY, category.first);
                groupData.add(value);

                List<Map<String, String>> child_values = new ArrayList<>();
                for(BuiltinVariable bv : category.second) {
                    HashMap<String, String> child_value = new HashMap<>();
                    child_value.put(CHILD_KEY, bv.label);
                    child_values.add(child_value);
                }
                childData.add(child_values);
            }

            HashMap<String, String> value = new HashMap<>();
            value.put(ROOT_KEY, getContext().getString(R.string.v_o));
            groupData.add(value);

            List<Map<String, String>> child_values = new ArrayList<>();
            for(Variable v : mUserVariables) {
                HashMap<String, String> child_value = new HashMap<>();
                child_value.put(CHILD_KEY, v.name);
                child_values.add(child_value);
            }
            childData.add(child_values);



            mAdapter = new SimpleExpandableListAdapter(
                    getContext(),

                    groupData,
                    android.R.layout.simple_expandable_list_item_1,
                    new String[] { ROOT_KEY },
                    new int[] { android.R.id.text1 },

                    childData,
                    android.R.layout.simple_expandable_list_item_1,
                    new String[] { CHILD_KEY },
                    new int[] { android.R.id.text1 }

            );
            list.setAdapter(mAdapter);
            list.setOnChildClickListener(this);
            setContentView(list);
        }

        @Override
        public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
            String name;
            if(groupPosition < mBuiltinVariables.length) {
                name = mBuiltinVariables[groupPosition].second[childPosition].name;
            } else {
                name = mUserVariables.get(childPosition).name;
            }
            mFormulaEditText.getText().replace(mFormulaEditText.getSelectionStart(), mFormulaEditText.getSelectionEnd(), "$"+name);
            dismiss();
            return true;
        }
    }
}
