package net.pierrox.lightning_launcher.prefs;

import android.content.Context;

import net.pierrox.lightning_launcher.engine.variable.Binding;

/**
 * @hide
 */
public class LLPreferenceBinding extends LLPreference {
    public LLPreferenceBinding(Context context, Object value) {
        super(context, 0, 0, 0, value, null);
    }

    public Binding getValue() {
        return (Binding) mValue;
    }

    @Override
    public void setValue(Object value, Object defaultValue) {
        super.setValue(value, defaultValue);
        Binding binding = (Binding) value;
        setTitle(binding.target);
        setSummary(binding.formula);
    }
}
