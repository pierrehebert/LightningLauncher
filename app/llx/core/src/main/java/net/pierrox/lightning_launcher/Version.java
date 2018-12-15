package net.pierrox.lightning_launcher;

import android.content.ComponentName;
import android.net.Uri;

public class Version {
	public static String APP_STORE_INSTALL_PREFIX="market://details?id=";

    public static String SCRIPT_IMPORTER_PKG = "com.trianguloy.llscript.repository";

    public static Uri LANGUAGE_PACK_INSTALL_URI=Uri.parse("market://search?q="+LLApp.LL_PKG_NAME+".lp.");
	public static final boolean HAS_RATE_LINK=true;

    public static Uri BROWSE_TEMPLATES_URI=Uri.parse("market://search?q=lltemplate&c=apps");
    public static Uri BROWSE_SCRIPTS_URI=Uri.parse("http://directory.lightninglauncher.com/scripts/");
    public static Uri BROWSE_PLUGINS_URI=Uri.parse("http://directory.lightninglauncher.com/plugins/");

    public static String WIKI_PREFIX="http://www.lightninglauncher.com/wiki/doku.php?id=";
    public static String USER_COMMUNITY="https://plus.google.com/u/0/communities/109017480579703391739";
}
