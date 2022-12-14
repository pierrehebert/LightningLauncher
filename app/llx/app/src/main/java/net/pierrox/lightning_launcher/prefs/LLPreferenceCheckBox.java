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
