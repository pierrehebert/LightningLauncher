package net.pierrox.lightning_launcher.views;

import android.graphics.NinePatch;
import android.graphics.drawable.NinePatchDrawable;

public class MyNinePatchDrawable extends NinePatchDrawable {
    private NinePatch mNinePatch;

    public MyNinePatchDrawable(NinePatch np) {
        super(np);

        mNinePatch = np;
    }

    public NinePatch getNinePatch() {
        return mNinePatch;
    }
}
