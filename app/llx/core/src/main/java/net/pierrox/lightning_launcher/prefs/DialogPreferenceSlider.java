package net.pierrox.lightning_launcher.prefs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import net.pierrox.lightning_launcher.R;

import java.text.DecimalFormat;

public class DialogPreferenceSlider extends AlertDialog implements SeekBar.OnSeekBarChangeListener, View.OnClickListener, DialogInterface.OnClickListener, DialogInterface.OnCancelListener {
    public interface OnDialogPreferenceSliderListener {
        public void onDialogPreferenceSliderValueSet(float value);
        public void onDialogPreferenceSliderCancel();
    }

    private boolean mIsFloat;
    private float mMinValue;
    private float mMaxValue;
    private float mInterval;
    private String mUnit;

    private SeekBar mDialogSeekBar;
    private EditText mDialogEditText;
    private float mDialogValue;
    private OnDialogPreferenceSliderListener mListener;

    public DialogPreferenceSlider(Context context, float value, boolean is_float, float min, float max, float interval, String unit, OnDialogPreferenceSliderListener listener) {
        super(context);

        mIsFloat = is_float;
        mMinValue = min;
        mMaxValue = max;
        mInterval = interval;
        mUnit = unit;
        mDialogValue = value;
        mListener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View view = inflater.inflate(R.layout.llpref_slider_dialog, null);

        view.findViewById(R.id.minus).setOnClickListener(this);
        view.findViewById(R.id.plus).setOnClickListener(this);
        mDialogSeekBar = (SeekBar) view.findViewById(R.id.seek_bar);
        mDialogSeekBar.setMax((int)((mMaxValue - mMinValue)/mInterval));
        mDialogSeekBar.setProgress(getProgressForValue(mDialogValue));
        mDialogSeekBar.setOnSeekBarChangeListener(this);
        mDialogEditText = (EditText) view.findViewById(R.id.value);
        if(mIsFloat) {
            mDialogEditText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | (mMinValue < 0 ? InputType.TYPE_NUMBER_FLAG_SIGNED : InputType.TYPE_NUMBER_VARIATION_NORMAL));
        } else {
            mDialogEditText.setInputType(InputType.TYPE_CLASS_NUMBER | (mMinValue<0 ? InputType.TYPE_NUMBER_FLAG_SIGNED : InputType.TYPE_NUMBER_VARIATION_NORMAL));
        }
        String value = valueAsText(mIsFloat, mUnit, mDialogValue, mInterval);
        mDialogEditText.setText(value);
        mDialogEditText.setSelection(value.length());
        mDialogEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // pass
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                    int after) {
                // pass
            }

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    float value=Float.parseFloat(s.toString().replace(',', '.'));
                    if("%".equals(mUnit)) {
                        value/=100;
                    }
                    mDialogSeekBar.setProgress(getProgressForValue(value));
                    mDialogValue = value;
                } catch(NumberFormatException e) {
                    // pass
                }
            }
        });

        TextView unitv = (TextView) view.findViewById(R.id.unit);
        unitv.setText(mUnit);

        setView(view);

        setButton(BUTTON_POSITIVE, getContext().getString(android.R.string.ok), this);
        setButton(BUTTON_NEGATIVE, getContext().getString(android.R.string.cancel), this);
        setOnCancelListener(this);

        super.onCreate(savedInstanceState);
    }


    @Override
    public void onClick(View v) {
        if(v.getId()==R.id.minus) {
            mDialogValue-=mInterval;
            if(mDialogValue<mMinValue) mDialogValue=mMinValue;
        } else {
            mDialogValue+=mInterval;
            if(mDialogValue>mMaxValue) mDialogValue=mMaxValue;
        }
        String value = valueAsText(mIsFloat, mUnit, mDialogValue, mInterval);
        mDialogEditText.setText(value);
        mDialogEditText.setSelection(value.length());
        mDialogSeekBar.setProgress(getProgressForValue(mDialogValue));
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch(which) {
        case BUTTON_POSITIVE:
            mListener.onDialogPreferenceSliderValueSet(mDialogValue);
            break;
        case BUTTON_NEGATIVE:
            mListener.onDialogPreferenceSliderCancel();
            break;
        }
    }

    @Override
    public void onCancel(DialogInterface dialogInterface) {
        mListener.onDialogPreferenceSliderCancel();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if(fromUser) {
            String value = valueAsText(mIsFloat, mUnit, getValueForCurrentProgress(), mInterval);
            mDialogEditText.setText(value);
            mDialogEditText.setSelection(value.length());
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        mDialogValue = getValueForCurrentProgress();

    }

    private float getValueForCurrentProgress() {
        return mDialogSeekBar.getProgress()*(mMaxValue-mMinValue)/mDialogSeekBar.getMax()+mMinValue;
    }

    private int getProgressForValue(float value) {
        return (int)((value-mMinValue)/(mMaxValue-mMinValue)*mDialogSeekBar.getMax());
    }

    private static DecimalFormat sFloatFormat0 = new DecimalFormat("0.#");
    private static DecimalFormat sFloatFormat1 = new DecimalFormat("0.0");
    /*package*/ static String valueAsText(boolean is_float, String unit, float value, float interval) {
        String text;
        if (is_float) {
            if(unit!=null && unit.equals("%")) {
                text=String.valueOf(Math.round(value*100));
            } else {
                DecimalFormat format=interval<1 ? sFloatFormat1 : sFloatFormat0;
                text=format.format(value);
            }
        } else {
            text=String.valueOf(Math.round(value));
        }
        return text;
    }
}
