package net.pierrox.lightning_launcher.wear;

import android.content.Context;
import android.content.Intent;

import net.pierrox.lightning_launcher.LLApp;
import net.pierrox.lightning_launcher.data.Page;
import net.pierrox.lightning_launcher.wear.overlay.WearWindowService;

public class LLAppWear extends LLApp {

    @Override
    public void displayPagerPage(int page, boolean reset_navigation_history) {

    }

    @Override
    public Intent getLockscreenServiceIntent() {
        return null;
    }

    @Override
    public Intent getWindowServiceIntent() {
        return new Intent(this, WearWindowService.class);
    }

    @Override
    public boolean isFreeVersion() {
        return false;
    }

    @Override
    public boolean isTrialVersion() {
        return false;
    }

    @Override
    public boolean isTrialVersionExpired() {
        return false;
    }

    @Override
    public long getTrialLeft() {
        return 0;
    }

    @Override
    public void showFeatureLockedDialog(Context context) {

    }

    @Override
    public void startUnlockProcess(Context context) {

    }

    @Override
    public void installPromotionalIcons(Page dashboard) {

    }

    @Override
    public void checkLicense() {

    }

    @Override
    public boolean hasScriptEditor() {
        return false;
    }

    @Override
    public void startScriptEditor(int script_id, int line) {

    }

    @Override
    public boolean isLockScreenLocked() {
        return false;
    }

    @Override
    public void unlockLockScreen(boolean restore_previous_task) {

    }

    @Override
    public Class<?> getActivityClassForScriptExecutionTarget(int target) {
        return null;
    }

    @Override
    public void restart(boolean relaunch) {

    }

    @Override
    public boolean isLightningIntent(Intent intent) {
        return false;
    }
}
