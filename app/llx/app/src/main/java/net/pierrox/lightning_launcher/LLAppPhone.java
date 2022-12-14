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

package net.pierrox.lightning_launcher;

import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.view.View;
import android.widget.Toast;

import net.pierrox.lightning_launcher.activities.AppDrawerX;
import net.pierrox.lightning_launcher.activities.Dashboard;
import net.pierrox.lightning_launcher.activities.LockScreen;
import net.pierrox.lightning_launcher.activities.ScriptEditor;
import net.pierrox.lightning_launcher.api.ScreenIdentity;
import net.pierrox.lightning_launcher.data.Page;
import net.pierrox.lightning_launcher.engine.Screen;
import net.pierrox.lightning_launcher.overlay.WindowService;
import net.pierrox.lightning_launcher.prefs.LLPreference;
import net.pierrox.lightning_launcher.script.Script;
import net.pierrox.lightning_launcher.util.EmptyService;
import net.pierrox.lightning_launcher.util.MPReceiver;

public abstract class LLAppPhone extends LLApp {

    private MPReceiver mMPReceiver;

    @Override
    public void onCreate() {
        super.onCreate();

        // Implicit broadcasts cannot be registered anymore in the manifest starting at Android O,
        // however it is still possible to register them at runtime.
        // the consequence is that these events will not be received if the app is not running,
        // and a systematic check has to be made at startup.
        // TODO when the check is implemented, also delete the broadcast receiver entry in the manifest
        // and register the receiver here for all platform versions.
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mMPReceiver = new MPReceiver();

            IntentFilter intent_filter = new IntentFilter();
            intent_filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
            intent_filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
            intent_filter.addAction(Intent.ACTION_PACKAGE_ADDED);
            intent_filter.addDataScheme("package");

            registerReceiver(mMPReceiver, intent_filter);
        }
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        unregisterReceiver(mMPReceiver);
    }

    @Override
    public Intent getLockscreenServiceIntent() {
        return new Intent(this, EmptyService.class);
    }

    @Override
    public Intent getWindowServiceIntent() {
        return new Intent(this, WindowService.class);
    }

    @Override
    public boolean hasScriptEditor() {
        return true;
    }

    @Override
    public void startScriptEditor(int script_id, int line) {
        ScriptEditor.startActivity(this, script_id, line);
    }

    @Override
    public boolean isLockScreenLocked() {
        return LockScreen.sThis != null;
    }

    @Override
    public void unlockLockScreen(boolean restore_previous_task) {
        if(LockScreen.sThis != null) {
            LockScreen.sThis.unlock(restore_previous_task);
        } else {
            Toast.makeText(this, net.pierrox.lightning_launcher_extreme.R.string.nly, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public Class<?> getActivityClassForScriptExecutionTarget(int target) {
        if (target == Script.TARGET_DESKTOP) {
            return Dashboard.class;
        }

        if (target == Script.TARGET_APP_DRAWER) {
            return AppDrawerX.class;
        }

        if (target == Script.TARGET_LOCK_SCREEN && mAppEngine.getGlobalConfig().lockScreen != Page.NONE) {
            return LockScreen.class;
        }

        return null;
    }

    @Override
    public void restart(boolean relaunch) {
        for (Screen screen : getScreens()) {
            screen.pause();
        }
        saveAllData();
        if(relaunch) {
            Intent i = new Intent(this, Dashboard.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        }
        System.exit(0);
    }

    @Override
    public boolean isLightningIntent(Intent intent) {
        ComponentName cn = intent.getComponent();
        if(cn != null) {
            if(cn.getPackageName().equals(getPackageName()) && cn.getClassName().equals(Dashboard.class.getName())) {
                return true;
            }
        }
        return false;
    }

    // FIXME rename this to displayDashboardPage or better
    @Override
    public void displayPagerPage(int page, boolean reset_navigation_history) {
        Screen homeScreen = getScreen(ScreenIdentity.HOME);
        if(homeScreen != null) {
            homeScreen.loadRootItemLayout(page, reset_navigation_history, true, true);
        } else {
            mAppEngine.writeCurrentPage(page);
        }
    }

    public abstract View managePreferenceViewLockedFlag(LLPreference preference, View preference_view);

    public abstract void manageAddItemDialogLockedFlag(View add_item_view, boolean locked);
}
