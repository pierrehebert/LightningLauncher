package net.pierrox.lightning_launcher.appshortcuts;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.graphics.drawable.Drawable;
import android.view.View;

public interface ShortcutDetails {
	Drawable getIcon(Context context);

	CharSequence getLabel();

	String getId();

	Intent getIntent();

	/**
	 * Launch this shortcut
	 *
	 * @param view the view this should be launched from
	 */
	void launch(View view);
}
