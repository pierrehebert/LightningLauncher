package net.pierrox.lightning_launcher.prefs;

import android.content.Context;

/**
 * This preference is used to manage a boolean value using a checkbox.
 */
public class LLPreferenceCheckBox extends LLPreference {
	private LLPreference[] mDependences;
	private LLPreference[] mDependencesInvert;

	/**
	 * @hide
     */
	public LLPreferenceCheckBox(Context context, int id, int title, int summary) {
		super(context, id, title, summary);
	}

    public LLPreferenceCheckBox(int id, String title, String summary, boolean checked, Boolean def) {
		super(id, title, summary, Boolean.valueOf(checked), def);
	}

	/**
	 * @hide
	 */
	public LLPreferenceCheckBox(Context context, int id, int title, int summary, boolean checked, Boolean def) {
		super(context, id, title, summary, Boolean.valueOf(checked), def);
	}

	/**
	 * Return true if this preference is checked.
     */
	public boolean isChecked() {
		return (Boolean)mValue;
	}

	/**
	 * Set the checked state of this preference.
     */
	public void setChecked(boolean checked) {
		mValue = Boolean.valueOf(checked);
		updateDependencies();
	}

	/**
	 * @hide
	 */
    @Override
    public void setValue(Object value, Object defaultValue) {
        super.setValue(value, defaultValue);
        updateDependencies();
    }

	/**
	 * @hide
	 */
    @Override
	public void setDisabled(boolean disabled) {
		super.setDisabled(disabled);
		updateDependencies();
	}

	/**
	 * @hide
	 */
	@Override
	public void reset() {
		super.reset();
		updateDependencies();
	}

	/**
	 * Set a list of preferences that will be disabled or enabled depending on this preference value.
     */
	public void setDependencies(LLPreference[] dependencies, LLPreference[] invertedDependencies) {
		mDependences = dependencies;
		mDependencesInvert = invertedDependencies;
		updateDependencies();
	}
	
	private void updateDependencies() {
        if(mValue == null) {
            return;
        }

		boolean my_state = (Boolean)mValue;
		boolean disabled = mDisabled || !my_state;
		if(mDependences!=null) {
			for(LLPreference p : mDependences) {
				if(p != null) {
					p.setDisabled(disabled);
				}
			}
		}
		disabled = mDisabled || my_state;
		if(mDependencesInvert!=null) {
			for(LLPreference p : mDependencesInvert) {
				if(p != null) {
					p.setDisabled(disabled);
				}
			}
		}
	}
}
