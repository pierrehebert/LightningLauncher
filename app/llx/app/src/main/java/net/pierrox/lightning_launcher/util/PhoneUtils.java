/*
MIT License

Copyright (c) 2022 Pierre Hébert, f43nd1r

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

package net.pierrox.lightning_launcher.util;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Parcelable;

import net.pierrox.lightning_launcher.LLApp;
import net.pierrox.lightning_launcher.Version;
import net.pierrox.lightning_launcher.activities.Customize;
import net.pierrox.lightning_launcher.activities.Dashboard;
import net.pierrox.lightning_launcher.activities.RootSettings;
import net.pierrox.lightning_launcher.api.ScreenIdentity;
import net.pierrox.lightning_launcher.data.ContainerPath;
import net.pierrox.lightning_launcher.data.LightningIntent;
import net.pierrox.lightning_launcher.data.Page;
import net.pierrox.lightning_launcher.data.Utils;
import net.pierrox.lightning_launcher.engine.Screen;
import net.pierrox.lightning_launcher.prefs.LLPreference;
import net.pierrox.lightning_launcher.views.ItemLayout;
import net.pierrox.lightning_launcher_extreme.R;

public class PhoneUtils {
    public static Intent createDesktopBookmarkShortcut(Context context, ItemLayout il, Page page, String label, Bitmap icon, boolean animate) {
        Matrix transform = null;

        // arguments: either page is null (use il.getPage) or il is null (try to retrieve il from dashboard using page)
        if(il == null) {
            Screen screen = LLApp.get().getScreen(ScreenIdentity.HOME);
            if(screen != null) {
                ItemLayout[] ils = screen.getItemLayoutsForPage(page.id);
                if(ils.length > 0) {
                    transform = ils[0].getLocalTransform();
                }
            }
        } else {
            page = il.getPage();
            transform = il.getLocalTransform();
        }

        float[] matrix_values = new float[9];
        if(transform == null) {
            transform = new Matrix();
        }
        transform.getValues(matrix_values);
        float x = matrix_values[Matrix.MTRANS_X];
        float y = matrix_values[Matrix.MTRANS_Y];
        float s = matrix_values[Matrix.MSCALE_X];
        boolean absolute;
        if(il != null) {
            RectF r = il.getVirtualEditBordersBounds();
            if(r != null) {
                x -= r.left;
                y -= r.top;
            }
            x /= il.getWidth();
            y /= il.getHeight();
            absolute = false;
        } else {
            absolute = true;
        }

        Intent intent = getDesktopBookmarkIntent(context, page.id, x, y, s, animate, absolute);

        Intent shortcut = new Intent();
        shortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent);
        shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, label==null? context.getString(net.pierrox.lightning_launcher.R.string.shortcut_screen) : label);
        if(icon == null) {
            Parcelable icon_resource = Intent.ShortcutIconResource.fromContext(context, R.drawable.icon);
            shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, icon_resource);
        } else {
            shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON, icon);
        }

        return shortcut;
    }

    public static Intent getDesktopBookmarkIntent(Context context, int page, float x, float y, float scale, boolean animate, boolean absolute) {
        Intent intent = new Intent(context, Dashboard.class);

        intent.putExtra(LightningIntent.INTENT_EXTRA_DESKTOP, page);
        intent.putExtra(LightningIntent.INTENT_EXTRA_X, x);
        intent.putExtra(LightningIntent.INTENT_EXTRA_Y, y);
        intent.putExtra(LightningIntent.INTENT_EXTRA_SCALE, scale);
        intent.putExtra(LightningIntent.INTENT_EXTRA_ANIMATE, animate);
        intent.putExtra(LightningIntent.INTENT_EXTRA_ABSOLUTE, absolute);

        return intent;
    }

    public static void showPreferenceHelp(Context context, LLPreference preference) {
        int id;
        if(preference == null) {
            try { context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Version.WIKI_PREFIX + "start"))); } catch(ActivityNotFoundException e) {}
            return;
        } else {
            id = preference.getId();
            if (id == 0) {
                return;
            }
        }

        int ll_version = Utils.getMyPackageVersion(context);

        String url = "http://www.lightninglauncher.com/help/app/topic.php?id="+id+"&v="+ll_version;
        String language = LLApp.get().getSystemConfig().language;
        if(language != null) {
            url += "&lang="+language.substring(language.lastIndexOf('.')+1);
        }

        context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    public static void startSettings(Context context, ContainerPath path, boolean root) {
        Intent intent=new Intent(context, root ? RootSettings.class : Customize.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if(path != null) {
            intent.putExtra(Customize.INTENT_EXTRA_PAGE_PATH, path.toString());
        }
        intent.putExtra(Customize.INTENT_EXTRA_LAUNCHED_FROM, new ComponentName(context, context.getClass()));
        context.startActivity(intent);
    }

    public static void selectLauncher(Context context, boolean start) {
        PackageManager pm=context.getPackageManager();
        ComponentName cn=new ComponentName(context, net.pierrox.lightning_launcher.activities.dummy.D1.class);
        pm.setComponentEnabledSetting(cn, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
        Intent home=new Intent(Intent.ACTION_MAIN);
        home.addCategory(Intent.CATEGORY_HOME);
        if(start) {
            context.startActivity(home);
        }
        pm.setComponentEnabledSetting(cn, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
    }
}
