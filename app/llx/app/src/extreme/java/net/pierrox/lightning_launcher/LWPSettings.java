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
