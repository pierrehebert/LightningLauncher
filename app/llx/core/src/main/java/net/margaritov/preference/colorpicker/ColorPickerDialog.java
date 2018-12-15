/*
 * Copyright (C) 2010 Daniel Nilsson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.margaritov.preference.colorpicker;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import net.pierrox.lightning_launcher.R;

public class ColorPickerDialog 
	extends 
		Dialog 
	implements
		ColorPickerView.OnColorChangedListener,
		View.OnClickListener, DialogInterface.OnCancelListener {

	private ColorPickerView mColorPicker;

	private ColorPickerPanelView mOldColor;
	private ColorPickerPanelView mNewColor;
	private EditText mHexEditor;

	private OnColorChangedListener mListener;

    @Override
    public void onCancel(DialogInterface dialogInterface) {
        if(mListener != null) mListener.onColorDialogCanceled();
    }

    public interface OnColorChangedListener {
		public void onColorChanged(int color);
		public void onColorDialogSelected(int color);
		public void onColorDialogCanceled();
	}
	
	public ColorPickerDialog(Context context, int initialColor) {
		super(context);

		init(initialColor);
	}

	private void init(int color) {
		// To fight color banding.
		getWindow().setFormat(PixelFormat.RGBA_8888);

		setUp(color);

	}

	private void setUp(int color) {
		
		LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		View layout = inflater.inflate(R.layout.dialog_color_picker, null);

		setContentView(layout);

		setTitle(R.string.dialog_color_picker);
		
		mColorPicker = (ColorPickerView) layout.findViewById(R.id.color_picker_view);
		mOldColor = (ColorPickerPanelView) layout.findViewById(R.id.old_color_panel);
		mNewColor = (ColorPickerPanelView) layout.findViewById(R.id.new_color_panel);
		mHexEditor = (EditText) layout.findViewById(R.id.hex_editor);
		mHexEditor.clearFocus();
		
		InputFilter filter = new InputFilter() { 
			@Override
			public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
				if ( dest.length() - (dend-dstart) + (end -start) > 8) {
					return "";
				}

				boolean ok=true;
				for (int i = start; i < end; i++) {
					char c = source.charAt(i);
					if ( !((c>='0' && c<='9') || (c>='a' && c<='f') || (c>='A' && c<='F')) ) { 
						ok=false;
						break;
					}
				}
				
				return ok ? null : "";
			} 
		};
		mHexEditor.setFilters(new InputFilter[] { filter });
		mHexEditor.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				// pass
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				// pass
			}

			@Override
			public void afterTextChanged(Editable s) {
				try {
					int color = Color.parseColor("#" + fillHex(s.toString()));
					if (color != mColorPicker.getColor()) {
						mColorPicker.setColor(color, false);
						mNewColor.setColor(color);
					}
				} catch(Exception e) {
					// pass
					e.printStackTrace();
				}
			}
		});
		
//		((LinearLayout) mOldColor.getParent()).setPadding(
//			Math.round(mColorPicker.getDrawingOffset()), 
//			0, 
//			Math.round(mColorPicker.getDrawingOffset()), 
//			0
//		);	
		
		mOldColor.setOnClickListener(this);
		mNewColor.setOnClickListener(this);
		mColorPicker.setOnColorChangedListener(this);
		mOldColor.setColor(color);
		mColorPicker.setColor(color, true);
        setOnCancelListener(this);
	}

	@Override
	public void onColorChanged(int color) {

		mNewColor.setColor(color);

		/*
		if (mListener != null) {
			mListener.onColorChanged(color);
		}
		*/

		String hex=Integer.toHexString(color);
		
		mHexEditor.setText(fillHex(hex));
	}
	
	private String fillHex(String hex) {
		while(hex.length()<8) {
			hex='0'+hex;
		}
		return hex;
	}

	public void setAlphaSliderVisible(boolean visible) {
		mColorPicker.setAlphaSliderVisible(visible);
	}
	
	/**
	 * Set a OnColorChangedListener to get notified when the color
	 * selected by the user has changed.
	 * @param listener
	 */
	public void setOnColorChangedListener(OnColorChangedListener listener){
		mListener = listener;
	}

	public int getColor() {
		return mColorPicker.getColor();
	}

	@Override
	public void onClick(View v) {
        if (mListener != null) {
            int color = mNewColor.getColor();
            int id = v.getId();
            if (id == R.id.new_color_panel) {
                mListener.onColorChanged(color);
            } else if (v == mNewColor) {
                mListener.onColorDialogSelected(color);
            } else if (v == mOldColor) {
                mListener.onColorDialogCanceled();
            }
        }
		dismiss();
	}
//	
//	@Override
//	public Bundle onSaveInstanceState() {
//		Bundle state = super.onSaveInstanceState();
//		state.putInt("old_color", mOldColor.getColor());
//		state.putInt("new_color", mNewColor.getColor());
//		return state;
//	}
//	
//	@Override
//	public void onRestoreInstanceState(Bundle savedInstanceState) {
//		super.onRestoreInstanceState(savedInstanceState);
//		mOldColor.setColor(savedInstanceState.getInt("old_color"));
//		mColorPicker.setColor(savedInstanceState.getInt("new_color"), true);
//	}
}
