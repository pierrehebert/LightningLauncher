package net.pierrox.lightning_launcher.appshortcuts;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherApps;
import android.content.pm.ShortcutInfo;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import androidx.annotation.RequiresApi;
import net.pierrox.lightning_launcher.activities.Dashboard;
import net.pierrox.lightning_launcher.data.Shortcut;
import net.pierrox.lightning_launcher.data.Utils;
import net.pierrox.lightning_launcher_extreme.R;

@RequiresApi(api = Build.VERSION_CODES.N_MR1)
public class LauncherAppsShortcutDetails implements ShortcutDetails {
	private final ShortcutInfo shortcut;

	public LauncherAppsShortcutDetails(ShortcutInfo shortcut) {
		this.shortcut = shortcut;
	}

	@Override
	public Drawable getIcon(Context context) {
		LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
		return launcherApps.getShortcutIconDrawable(shortcut, Utils.getLauncherIconDensity());
	}

	@Override
	public CharSequence getLabel() {
		CharSequence label = shortcut.getLongLabel();
		return label != null && label.length() != 0 ? label : shortcut.getShortLabel();
	}

	@Override
	public String getId() {
		return shortcut.getId();
	}

	@Override
	public Intent getIntent() {
		Intent intent = new Intent(Shortcut.INTENT_ACTION_APP_SHORTCUT);
		intent.putExtra(Shortcut.INTENT_EXTRA_APP_SHORTCUT_ID, shortcut.getId());
		intent.putExtra(Shortcut.INTENT_EXTRA_APP_SHORTCUT_PKG, shortcut.getPackage());
		intent.putExtra(Shortcut.INTENT_EXTRA_APP_SHORTCUT_DISABLED_MSG, shortcut.getDisabledMessage());
		return intent;
	}

	@Override
	public void launch(View view) {
		LauncherApps launcherApps = (LauncherApps) view.getContext().getSystemService(Context.LAUNCHER_APPS_SERVICE);
		if(launcherApps.hasShortcutHostPermission()) {
			Rect bounds = new Rect();
			view.getHitRect(bounds);
			launcherApps.startShortcut(shortcut, bounds, null);
		} else {
			((Activity) view.getContext()).showDialog(Dashboard.DIALOG_LAUNCHER_APPS_NO_HOST_PERMISSION);
		}
	}
}
