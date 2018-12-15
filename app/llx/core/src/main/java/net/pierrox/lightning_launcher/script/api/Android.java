package net.pierrox.lightning_launcher.script.api;

import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.Toast;

/**
 * The Android object gives access to some platform services.
 * @deprecated use the android.widget.Toast class instead
 */
public class Android {
	
	private Context mContext;
	
	/**
	 * @hide
	 */
	public Android(Context context) {
		mContext = context;
	}
	
	/**
	 * Create a toast (but does not display it). Typical use is:
	 * <pre>
	 * Android.makeNewToast("hello", true).show();
	 * </pre>
	 * 
	 * @param text message to display
	 * @param isShort whether this message is displayed during a short or long duration
	 * @return a new toast
	 */
	@SuppressLint("ShowToast")
	public Toast makeNewToast(String text, boolean isShort) {
		Toast t = Toast.makeText(mContext, text, isShort ? android.widget.Toast.LENGTH_SHORT : android.widget.Toast.LENGTH_LONG);
		return t;
	}
}
