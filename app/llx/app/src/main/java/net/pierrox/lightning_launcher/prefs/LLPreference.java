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

package net.pierrox.lightning_launcher.prefs;

import android.content.Context;


/**
 * The base class for all other preferences. It offers a set of common features shared with other specialized preferences.
 * Although this preference only displays a title and a summary, it can nevertheless be used to manage complex settings by using the {@link LLPreferenceListView.OnLLPreferenceListViewEventListener#onLLPreferenceClicked(LLPreference)} event to catch clicks. This preference holds an arbitrary value (an object).
 */
public class LLPreference {
	private int mId;
	protected String mTitle;
	protected String mSummary;
	protected boolean mDisabled;
    protected boolean mIsLocked;
    protected boolean mVisible = true;

	protected Object mValue;
	protected Object mDefaultValue;

	/**
	 * @hide
     */
	public LLPreference(Context context, int id, int title, int summary) {
		this(context, id, title, summary, null, null);
	}

	/**
	 * @hide
	 */
	public LLPreference(Context context, int id, int title, int summary, Object value, Object defaultValue) {
		this(id, title==0 ? null : context.getString(title), summary==0 ? null : context.getString(summary), value, defaultValue);
	}

	/**
	 * Create a new preference.
	 * @param id a unique number to identify the preference, use 0 if unused.
	 * @param title	Displayed title.
	 * @param summary Displayed summary, use null for none.
	 * @param value Value for the preference.
     * @param defaultValue Default value if displaying the override checkbox. Use null if unused.
     */
	public LLPreference(int id, String title, String summary, Object value, Object defaultValue) {
		mId = id;
		mTitle = title;
		mSummary = summary;
        setValue(value, defaultValue);
	}

	/**
	 * Set the value and optionally a default value.
	 * @param value the value stored by this preference
	 * @param defaultValue use null if unused
     */
    public void setValue(Object value, Object defaultValue) {
        mValue = value;
        mDefaultValue = defaultValue;
    }

	/**
	 * Return the unique identifier.
     */
    public int getId() {
		return mId;
	}

	/**
	 * Return the title.
     */
	public String getTitle() {
		return mTitle;
	}

	/**
	 * Set a new title.
     */
    public void setTitle(String title) {
		mTitle = title;
	}

	/**
	 * Return the summary, if any.
     */
	public String getSummary() {
		return mSummary;
	}

	/**
	 * Set a new summary.
     */
    public void setSummary(String summary) {
        mSummary = summary;
    }

	/**
	 * Disable or enable the preference (it will be grayed out and not clickable when disabled)
     */
	public void setDisabled(boolean disabled) {
		mDisabled = disabled;
	}

	/**
	 * Return true if the preference is currently disabled.
     */
	public boolean isDisabled() {
		return mDisabled;
	}

	/**
	 * Return true if this preference is showing the override checkbox.
     */
	public boolean isShowingOverride() {
		return mDefaultValue != null;
	}

	/**
	 * Return true if the value is different from the default value
     */
	public boolean isOverriding() {
		return !mValue.equals(mDefaultValue);
	}

	/**
	 * @hide
	 */
    public void setLocked(boolean locked) {
        mIsLocked = locked;
    }

	/**
	 * @hide
	 */
    public boolean isLocked() {
        return mIsLocked;
    }

	/**
	 * Show or hide this preference.
     */
    public void setVisible(boolean visible) {
        mVisible = visible;
    }

	/**
	 * Return true is the preference is currently visible.
     */
    public boolean isVisible() {
        return mVisible;
    }

	/**
	 * Set the value of this preference with the default value.
	 */
	public void reset() {
		mValue = mDefaultValue;
	}
}
