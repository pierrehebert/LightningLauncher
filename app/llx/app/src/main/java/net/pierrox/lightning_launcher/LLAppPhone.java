package net.pierrox.lightning_launcher;

import android.content.ComponentName;
import android.content.Intent;
import android.view.View;
import android.widget.Toast;

import net.pierrox.lightning_launcher.activities.AppDrawerX;
import net.pierrox.lightning_launcher.activities.Dashboard;
import net.pierrox.lightning_launcher.activities.LockScreen;
import net.pierrox.lightning_launcher.activities.ScriptEditor;
import net.pierrox.lightning_launcher.data.Page;
import net.pierrox.lightning_launcher.engine.Screen;
import net.pierrox.lightning_launcher.overlay.WindowService;
import net.pierrox.lightning_launcher.prefs.LLPreference;
import net.pierrox.lightning_launcher.script.Script;
import net.pierrox.lightning_launcher.util.EmptyService;

public abstract class LLAppPhone extends LLApp {

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
        Screen homeScreen = getScreen(Screen.Identity.HOME);
        if(homeScreen != null) {
            homeScreen.loadRootItemLayout(page, reset_navigation_history, true, true);
        } else {
            mAppEngine.writeCurrentPage(page);
        }
    }

    public abstract View managePreferenceViewLockedFlag(LLPreference preference, View preference_view);

    public abstract void manageAddItemDialogLockedFlag(View add_item_view, boolean locked);
}
