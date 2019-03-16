package net.pierrox.lightning_launcher.script.api;

import net.pierrox.lightning_launcher.engine.variable.Value;
import net.pierrox.lightning_launcher.engine.variable.Variable;
import net.pierrox.lightning_launcher.engine.variable.VariableManager;

/**
 * A VariableSet object enumerates known variables with their type and value.
 * It is also the entry point to modify variable through
 *
 * An instance of this object can be retrieved with {@link Lightning#getVariables()}.
 */
public class VariableSet {

    private VariableManager mVariableManager;

    /**
     * @hide
     */
    /*package*/ public VariableSet(VariableManager vm) {
        mVariableManager = vm;
    }

    public String getType(String name) {
        Object value = mVariableManager.getVariable(name).value;
        if(value == null) {
            return "UNSET";
        } else {
            Class<?> cls = value.getClass();
            if(cls == Boolean.class) {
                return "BOOLEAN";
            } else if(cls == Integer.class) {
                return "INTEGER";
            } else if(cls == Float.class) {
                return "FLOAT";
            } else if(cls == String.class) {
                return "STRING";
            } else {
                return "UNSET";
            }
        }
    }
    public boolean getBoolean(String name) {
        return Value.asBoolean(mVariableManager.getVariable(name).value);
    }

    public float getFloat(String name) {
        return Value.asFloat(mVariableManager.getVariable(name).value);
    }

    public int getInteger(String name) {
        return Value.asInteger(mVariableManager.getVariable(name).value);
    }

    public String getString(String name) {
        return Value.asString(mVariableManager.getVariable(name).value);
    }

    public VariableEditor edit() {
        return new VariableEditor(mVariableManager);
    }
}
