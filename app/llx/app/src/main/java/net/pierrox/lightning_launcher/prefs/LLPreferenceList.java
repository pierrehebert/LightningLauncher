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

import net.pierrox.lightning_launcher.data.ActionsDescription;
import net.pierrox.lightning_launcher.data.EventAction;

/**
 * This preference is used to select an item in a list.
 */
public class LLPreferenceList extends LLPreference {

    /**
     * @hide
     */
    public enum ValueType {
        ENUM,
        EVENT_ACTION,
        INDEX
    }

	private String[] mLabels;
    private ActionsDescription mActions;

    /**
     * @hide
     */
	public LLPreferenceList(Context context, int id, int title,  String[] labels) {
		super(context, id, title, 0);
		
		mLabels = labels;
	}

    /**
     * Construct a new list preference.
     * @param id a unique number to identify the preference, use 0 if unused.
     * @param title	Displayed title.
     * @param labels an array of strings: the list of available choices
     * @param value the index of the currently selected choice
     * @param defaultValue the index of the default choice, or null if unused
     */
    public LLPreferenceList(int id, String title,  String[] labels, int value, Integer defaultValue) {
        super(id, title, null, value, defaultValue);
        mLabels = labels;
        setValue(value, defaultValue);
    }

    /**
     * @hide
     */
    public LLPreferenceList(Context context, int id, int title,  String[] labels, int value, Integer defaultValue) {
		super(context, id, title, 0);

		mLabels = labels;
        setValue(value, defaultValue);
	}

    /**
     * @hide
     */
    public LLPreferenceList(Context context, int id, int title, EventAction value, EventAction defaultValue, ActionsDescription actions) {
        super(context, id, title, 0);

        mActions = actions;
        mLabels = actions.getActionNames();
        setValue(value, defaultValue);
    }

    /**
     * @hide
     */
    public LLPreferenceList(Context context, int id, int title,  int labels, int item_count) {
        super(context, id, title, 0);

        mLabels = context.getResources().getStringArray(labels);
        if(item_count != 0 && item_count != mLabels.length) {
            String[] tmp = new String[item_count];
            System.arraycopy(mLabels, 0, tmp, 0, item_count);
            mLabels = tmp;
        }
    }

    /**
     * @hide
     */
	public LLPreferenceList(Context context, int id, int title,  int labels, Enum<?> value, Enum<?> defaultValue) {
        this(context, id, title, labels, value, defaultValue, 0);
    }

    /**
     * @hide
     */
	public LLPreferenceList(Context context, int id, int title,  int labels, Enum<?> value, Enum<?> defaultValue, int item_count) {
		this(context, id, title, labels, item_count);
		
		setValue(value, defaultValue);
        setSummary(value.ordinal());
	}

    /**
     * @hide
     */
    @Override
    public void setValue(Object value, Object defaultValue) {
        super.setValue(value, defaultValue);
        if(mValue != null) {
            setSummary(getValueIndex());
        }
    }

    /**
     * Set the list of values as an array of strings.
     */
    public void setLabels(String[] labels) {
		mLabels = labels;
        setSummary(getValueIndex());
	}

    /**
     * Return the list of values.
     */
	public String[] getLabels() {
		return mLabels;
	}

    /**
     * Retrieve the index of the currently selected value.
     */
	public int getValueIndex() {
        switch (getValueType()) {
            case ENUM: return ((Enum<?>)mValue).ordinal();
            case EVENT_ACTION: return mActions.getActionIndex(((EventAction)mValue).action);
            case INDEX: return (Integer)mValue;
            default: return 0;
        }
	}

    /**
     * @hide
     */
	@SuppressWarnings("rawtypes")
	public Enum getValueEnum() {
		return (Enum)mValue;
	}

    /**
     * @hide
     */
    public EventAction getEventAction() {
        return (EventAction)mValue;
    }

    /**
     * Set the current index of the value in the list
     */
	public void setValueIndex(int index) {
        switch (getValueType()) {
            case ENUM:
                mValue = ((Enum<?>)mValue).getDeclaringClass().getEnumConstants()[index];
                break;

            case EVENT_ACTION:
                ((EventAction) mValue).action = mActions.getActionAt(index);
                break;

            case INDEX:
                mValue = index;
                break;
        }
        setSummary(index);
	}

    /**
     * @hide
     */
    public void setValueAction(int action) {
        ((EventAction) mValue).action = action;
        setSummary(mActions.getActionIndex(action));
    }

    /**
     * @hide
     */
    public ValueType getValueType() {
        if(mValue instanceof Enum<?>) {
            return ValueType.ENUM;
        } else if(mValue instanceof EventAction) {
            return ValueType.EVENT_ACTION;
        } else {
            return ValueType.INDEX;
        }
    }

    /**
     * @hide
     */
	@Override
	public void reset() {
		super.reset();
		setSummary(getValueIndex());
		if(mValue instanceof EventAction) {
			mValue = ((EventAction)mDefaultValue).clone();
		}
	}

    private void setSummary(int i) {
        if(mLabels != null) {
            mSummary = i<0 || i>=mLabels.length ? "" : mLabels[i];
        }
    }
}
