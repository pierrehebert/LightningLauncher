/*
MIT License

Copyright (c) 2022 Pierre Hébert

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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;

import net.pierrox.lightning_launcher.LLApp;
import net.pierrox.lightning_launcher.data.Item;
import net.pierrox.lightning_launcher.data.Page;
import net.pierrox.lightning_launcher.engine.LightningEngine;
import net.pierrox.lightning_launcher.data.Shortcut;
import net.pierrox.lightning_launcher.data.Utils;

import java.io.File;
import java.util.ArrayList;

public class ShortcutReceiver extends BroadcastReceiver {
    private static final String ACTION_INSTALL_SHORTCUT="com.android.launcher.action.INSTALL_SHORTCUT";
    private static final String ACTION_UNINSTALL_SHORTCUT="com.android.launcher.action.UNINSTALL_SHORTCUT";
    private static final String EXTRA_SHORTCUT_DUPLICATE = "duplicate";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action=intent.getAction();

        if(ACTION_INSTALL_SHORTCUT.equals(action)) {
            installShortcut(context, intent);
        } else if(ACTION_UNINSTALL_SHORTCUT.equals(action)) {
            uninstallShortcut(context, intent);
        }
    }

    private void installShortcut(Context context, Intent data) {
        Intent shortcut_intent = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);

        if(shortcut_intent==null) {
            return;
        }

        if(shortcut_intent.getAction() == null) {
            shortcut_intent.setAction(Intent.ACTION_VIEW);
        }
        boolean duplicate = data.getBooleanExtra(EXTRA_SHORTCUT_DUPLICATE, true);

        LightningEngine engine = LLApp.get().getAppEngine();
        final int p = engine.getGlobalConfig().homeScreen;
        Page home_page = engine.getOrLoadPage(p);
        ArrayList<Item> items = home_page.items;

        Shortcut found=null;
        for(Item i : items) {
            if(i instanceof Shortcut) {
                Shortcut s=(Shortcut)i;
                if(shortcut_intent.filterEquals(s.getIntent())) {
                    found=s;
                    break;
                }
            }
        }
        if(duplicate || found==null) {
            int icon_size=(int)(home_page.config.defaultShortcutConfig.iconScale* Utils.getStandardIconSize());
            Utils.ShortcutDescription sd=Utils.createShortcutFromIntent(context, data, icon_size);
            if(sd != null) {
                int id = home_page.findFreeItemId();
                int[] cell = Utils.findFreeCell(home_page);
                Shortcut shortcut = new Shortcut(home_page);
                File icon_dir = home_page.getAndCreateIconDir();
                shortcut.init(id, new Rect(cell[0], cell[1], cell[0] + 1, cell[1] + 1), null, sd.name, sd.intent);

                if (sd.icon != null) {
                    File icon_file = shortcut.getDefaultIconFile();
                    Utils.saveIconToFile(icon_file, sd.icon);
                    sd.icon.recycle();
                }

                home_page.addItem(shortcut);
            }
        }
    }

    private void uninstallShortcut(Context context, Intent data) {
        // TODO: implement uninstall shortcut
    }
}
