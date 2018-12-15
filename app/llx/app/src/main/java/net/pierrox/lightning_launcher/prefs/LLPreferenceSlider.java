package net.pierrox.lightning_launcher.prefs;

import android.content.Context;

/**
 * This preference is used to manage number settings, using a slider and -/+ buttons.
 */
public class LLPreferenceSlider extends LLPreference {
	/**
	 * @hide
	 */
	public enum ValueType { INT, FLOAT }

	private ValueType mValueType;
	private float mMinValue;
	private float mMaxValue;
	private float mInterval;
	private String mUnit;

	/**
	 * @hide
	 */
	public LLPreferenceSlider(Context context, int id, int title, int summary, ValueType value_type, float min, float max, float interval, String unit) {
		super(context, id, title, summary);

        mValueType = value_type;
        mMinValue = min;
        mMaxValue = max;
        mInterval = interval;
        mUnit = unit;
	}

	/**
	 * Construct a slider preference
	 * @param id a unique number to identify the preference, use 0 if unused.
	 * @param title	Displayed title.
	 * @param summary Displayed summary, use null for none.
	 * @param value Value for the preference.
	 * @param defaultValue Default value if displaying the override checkbox. Use null if unused.
	 * @param value_type one of INT (integer) or FLOAT (floating point value)
	 * @param min minimum value for the slider
	 * @param max maximum value for the slider
     * @param interval step to use when using +/- buttons
     * @param unit optional text to display units
     */
    public LLPreferenceSlider(int id, String title, String summary, float value, Float default_value, String value_type, float min, float max, float interval, String unit) {
		super(id, title, summary, null, null);

		try {
			mValueType = ValueType.valueOf(value_type);
		} catch (IllegalArgumentException e) {
			mValueType = ValueType.FLOAT;
		}
        mMinValue = min;
        mMaxValue = max;
        mInterval = interval;
        mUnit = unit;

        setValue(value, default_value);
	}

	/**
	 * @hide
	 */
    public LLPreferenceSlider(Context context, int id, int title, int summary, float value, Float default_value, ValueType value_type, float min, float max, float interval, String unit) {
		super(context, id, title, summary);

        mValueType = value_type;
        mMinValue = min;
        mMaxValue = max;
        mInterval = interval;
        mUnit = unit;

        setValue(value, default_value);
	}

	/**
	 * Return the value as a float, no matter what the input type is (integer or floating point)
     */
    public float getValue() {
		return (Float)mValue;
	}

	public void setValue(float value) {
		mValue = Float.valueOf(value);
	}

	/**
	 * @hide
	 */
    public void setValue(float value, Float default_value) {
        super.setValue(Float.valueOf(value), default_value);
    }

    //    public void setValue(float value, Float default_value) {
//        super.setValue(Float.valueOf(value), default_value);
//    }

	/**
	 * @hide
	 */
	public void setDefaultValue(float value) {
		mDefaultValue = Float.valueOf(value);
	}

	/**
	 * @hide
	 */
	public ValueType getValueType() {
		return mValueType;
	}

	/**
	 * @hide
	 */
	public float getMinValue() {
		return mMinValue;
	}

	/**
	 * @hide
	 */
	public float getMaxValue() {
		return mMaxValue;
	}

	/**
	 * @hide
	 */
	public float getInterval() {
		return mInterval;
	}

	/**
	 * @hide
	 */
	public String getUnit() {
		return mUnit;
	}

}
