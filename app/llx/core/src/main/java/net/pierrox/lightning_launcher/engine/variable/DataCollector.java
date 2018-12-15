package net.pierrox.lightning_launcher.engine.variable;

import android.content.res.Resources;

public interface DataCollector {
    public void onPause();
    public void onResume();
    public void end();
    public BuiltinVariable[] getBuiltinVariables(Resources resources);
}
