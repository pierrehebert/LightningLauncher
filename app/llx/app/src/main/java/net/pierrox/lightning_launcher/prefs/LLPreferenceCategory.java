package net.pierrox.lightning_launcher.prefs;

import android.content.Context;

/**
 * A preference category is a thin header element used to group preferences.
 */
public class LLPreferenceCategory extends LLPreference {

	public LLPreferenceCategory(int id, String title) {
		super(0, title, null, null, null);
	}

	/**
	 * @hide
     */
	public LLPreferenceCategory(Context context, int title) {
		super(context, 0, title, 0, null, null);
	}

}
