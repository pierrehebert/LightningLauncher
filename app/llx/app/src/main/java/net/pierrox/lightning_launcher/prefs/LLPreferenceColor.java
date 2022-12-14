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
 * This preference is used to manage a color. It will display a color preview and a color picker when clicked.
 */
public class LLPreferenceColor extends LLPreference {
	private boolean mHasAlpha;

	/**
	 * Create a new color preference
	 * @param id a unique number to identify the preference, use 0 if unused.
	 * @param title	Displayed title.
	 * @param summary Displayed summary, use null for none.
	 * @param color Color for the preference as a 32 bits ARGB value.
	 * @param default_color Default color if displaying the override checkbox. Use null if unused.
     * @param has_alpha true to allow transparency setting
     */
	public LLPreferenceColor(int id, String title, String summary, int color, Integer default_color, boolean has_alpha) {
		super(id, title, summary, color, default_color);
		mHasAlpha = has_alpha;
	}

	/**
	 * @hide
	 */
	public LLPreferenceColor(Context context, int id, int title, int summary, boolean has_alpha) {
		super(context, id, title, summary);
		mHasAlpha = has_alpha;
	}

	/**
	 * @hide
	 */
    public LLPreferenceColor(Context context, int id, int title, int summary, int color, Integer default_color, boolean has_alpha) {
		super(context, id, title, summary, Integer.valueOf(color), default_color);
		mHasAlpha = has_alpha;
	}

	/**
	 * Return the currently selected color.
     */
	public int getColor() {
		return (Integer)mValue;
	}

	/**
	 * Set this preference color.
	 * @param color a 32 bits ARGB value
     */
	public void setColor(int color) {
		mValue = Integer.valueOf(color);
	}

	/**
	 * Set this preference default color.
	 * @param color a 32 bits ARGB value
	 */
	public void setDefaultColor(int color) {
		mDefaultValue = Integer.valueOf(color);
	}

	/**
	 * Return true is this preference allows alpha configuration
     */
	public boolean hasAlpha() {
		return mHasAlpha;
	}
}
