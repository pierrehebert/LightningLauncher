package net.pierrox.lightning_launcher.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.EditText;

public class EditTextIme extends EditText {
	private OnEditTextImeListener mListener;
	
	public interface OnEditTextImeListener {
		public void onEditTextImeBackPressed();
	}
	
	public EditTextIme(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void setOnEditTextImeListener(OnEditTextImeListener listener) {
		mListener = listener;
	}

    private boolean mHadKeyDown;

	@Override
	public boolean onKeyPreIme (int keyCode, KeyEvent event) {
		if(keyCode==KeyEvent.KEYCODE_BACK) {
            switch(event.getAction()) {
                case KeyEvent.ACTION_DOWN:
                    mHadKeyDown = true;
                    return false;

                case KeyEvent.ACTION_UP:
                    if(mHadKeyDown && mListener != null) {
                        mListener.onEditTextImeBackPressed();
                    }
                    mHadKeyDown = false;
                    return true;
            }
		}
		return false;
	}
}
