package net.pierrox.lightning_launcher.prefs;

import net.pierrox.lightning_launcher.data.Box;

/**
 * @hide
 */
public class LLPreferenceBox extends LLPreference {
	private int mSelection;
	
	public LLPreferenceBox(int id) {
		super(null, id, 0, 0);
	}

	public LLPreferenceBox(int id, Box value, Box defaultValue) {
		super(null, id, 0, 0, value, defaultValue);
	}

	public void setSelection(int selection) {
		mSelection = selection;
	}
	
	public int getSelection() {
		return mSelection;
	}
}