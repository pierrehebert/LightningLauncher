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
