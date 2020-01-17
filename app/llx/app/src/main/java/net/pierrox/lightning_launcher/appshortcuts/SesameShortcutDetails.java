package net.pierrox.lightning_launcher.appshortcuts;

import android.app.ActivityOptions;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.content.Context;
import android.view.View;
import androidx.annotation.RequiresApi;
import ninja.sesame.lib.bridge.v1.SesameShortcut;

@RequiresApi(api = Build.VERSION_CODES.M)
public class SesameShortcutDetails implements ShortcutDetails {
	private final SesameShortcut shortcut;

	public SesameShortcutDetails(SesameShortcut shortcut) {
		this.shortcut = shortcut;
	}

	@Override
	public Drawable getIcon(Context context) {
		return Icon.createWithContentUri(shortcut.iconUri).loadDrawable(context);
	}

	@Override
	public CharSequence getLabel() {
		return shortcut.plainLabel;
	}

	@Override
	public String getId() {
		return shortcut.id;
	}

	@Override
	public Intent getIntent() {
		return shortcut.actions[0].intent;
	}

	@Override
	public void launch(View view) {
		ActivityOptions options = ActivityOptions.makeBasic();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			Rect bounds = new Rect();
			view.getHitRect(bounds);
			options.setLaunchBounds(bounds);
		}
		view.getContext().startActivity(shortcut.actions[0].intent, options.toBundle());
	}
}
