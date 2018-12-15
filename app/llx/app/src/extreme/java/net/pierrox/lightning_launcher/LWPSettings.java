package net.pierrox.lightning_launcher;

import android.os.Bundle;
import android.view.View;

import net.pierrox.lightning_launcher_extreme.R;
import net.pierrox.lightning_launcher.activities.ScreenManager;

public class LWPSettings extends ScreenManager {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkHasLwp();
    }

    @Override
    protected int getMode() {
        return SCREEN_MODE_LWP;
    }

    @Override
    public void onClick(View v) {
        if(checkHasLwp()) {
            super.onClick(v);
        }
    }

    // return true if LWP is enabled, otherwise return false and display a dialog
    private boolean checkHasLwp() {
        LLAppExtreme app = (LLAppExtreme) LLApp.get();
        app.checkLwpKey();
        if(app.hasLWP()) {
            return true;
        } else {
            app.showProductLockedDialog(this, R.string.iab_ul_lwp_t, R.string.iab_ul_lwp_m, getString(R.string.iab_lwp));
            return false;
        }
    }
}
