package net.pierrox.lightning_launcher.views;

import android.app.Activity;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

public class SystemUIHelper {

    public static void setStatusBarVisibility(Window w, boolean visible, boolean overlap) {
        if (Build.VERSION.SDK_INT >= 16) {
            View decorView = w.getDecorView();
            decorView.setSystemUiVisibility(visible ? 0 : View.SYSTEM_UI_FLAG_FULLSCREEN);

            if(Build.VERSION.SDK_INT < 19) {
                if(!visible || overlap) {
                    w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
                    w.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR);
                } else {
                    w.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
                }
            }
        } else {
            if(visible) {
                w.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR);
            } else {
                w.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }

            if(visible) {
                if(overlap) {
                    w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
                } else {
                    w.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
                }
                w.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            } else {
                w.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
                w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
            }
        }
    }
}
