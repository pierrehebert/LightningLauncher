package net.pierrox.lightning_launcher.script.api;

import net.pierrox.lightning_launcher.engine.variable.VariableManager;

import org.mozilla.javascript.ScriptRuntime;

import java.util.ArrayList;

/**
 * A VariableEditor is a tool to change the value of one or more variable.
 * An instance of this object can be retrieved with {@link VariableSet#edit()}. You can chain calls this way:
 * <pre><code>
 * 	variable_set.edit()
 * 		.setInteger("someVariable", some_value)
 * 		.setFloat("someOtherVariable", some_other_value)
 * 		.commit();
 * </code></pre>
 */
public class VariableEditor {

    private VariableManager mVariableManager;
    private ArrayList<String> mNames;
    private ArrayList<Object> mValues;

    /**
     * @hide
     */
    /*package*/ public VariableEditor(VariableManager vm) {
        mVariableManager = vm;

        mNames = new ArrayList<>();
        mValues = new ArrayList<>();
    }

    public VariableEditor setBoolean(String name, boolean value) {
        mNames.add(name);
        mValues.add(value);
        return this;
    }

    public VariableEditor setInteger(String name, long value) {
        mNames.add(name);
        mValues.add((int) value);
        return this;
    }

    public VariableEditor setFloat(String name, float value) {
        if(Float.isNaN(value)) {
            throw ScriptRuntime.constructError("setFloat", "Bad argument");
        }
        mNames.add(name);

        mValues.add(Float.valueOf(value));
        return this;
    }

    public VariableEditor setString(String name, String value) {
        mNames.add(name);
        mValues.add(value);
        return this;
    }

    public void commit() {
        int l = mNames.size();

        mVariableManager.edit();
        for(int i=0; i<l; i++) {
            mVariableManager.setVariable(mNames.get(i), mValues.get(i));
        }
        mVariableManager.commit();

        mNames.clear();
        mValues.clear();
    }
}
