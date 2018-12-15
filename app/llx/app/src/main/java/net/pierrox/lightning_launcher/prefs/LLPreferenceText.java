package net.pierrox.lightning_launcher.prefs;

import android.content.Context;

/**
 * This preference is used to store a text value
 */
public class LLPreferenceText extends LLPreference {
    private boolean mSetSummaryWithValue;

    /**
     * Construct a text preference. The value is displayed in place of the summary.
     */
    public LLPreferenceText(int id, String title, String value, String defaultValue) {
        super(id, title, value, value, defaultValue);
        mSetSummaryWithValue = true;
    }

    /**
     * @hide
     */
    public LLPreferenceText(Context context, int id, int title, int summary, String value, String defaultValue) {
        super(context, id, title, summary, value, defaultValue);
    }

    /**
     * Return the current preference text.
     */
    public String getValue() {
        return (String) mValue;
    }

    /**
     * Set the preference text.
     */
    public void setValue(String value) {
        mValue = value;
        if(mSetSummaryWithValue) {
            setSummary(value);
        }
    }
}
