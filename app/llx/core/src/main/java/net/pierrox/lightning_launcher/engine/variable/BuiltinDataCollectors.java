package net.pierrox.lightning_launcher.engine.variable;

import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.util.Pair;

import net.pierrox.lightning_launcher.R;

public class BuiltinDataCollectors {
    private static final String VAR_SCREEN_ORIENTATION = "screen_orient";
    private static final String VAR_SCREEN_WIDTH = "screen_width";
    private static final String VAR_SCREEN_HEIGHT = "screen_height";

    private Pair<String,BuiltinVariable[]>[] mBuiltinVariables;
    private Handler mHandler;
    private VariableManager mVariableManager;
    private DataCollector[] mDataCollectors;
    private int mResumedCount;

    public BuiltinDataCollectors(Context context, VariableManager vm) {
        mVariableManager = vm;
        mHandler = new Handler();

        TimeDataCollector timeDataCollector = new TimeDataCollector(mHandler, mVariableManager);
        StorageDataCollector storageDataCollector = new StorageDataCollector(context, mHandler, mVariableManager);
        BatteryDataCollector batteryDataCollector = new BatteryDataCollector(context, mVariableManager);
        CpuDataCollector cpuDataCollector = new CpuDataCollector(mHandler, mVariableManager);
        mDataCollectors = new DataCollector[] {
                timeDataCollector,
                storageDataCollector,
                batteryDataCollector,
                cpuDataCollector,
        };

        Resources resources = context.getResources();

        BuiltinVariable[] screen_variables = new BuiltinVariable[] {
            new BuiltinVariable(VAR_SCREEN_ORIENTATION, resources.getString(R.string.bv_o)),
            new BuiltinVariable(VAR_SCREEN_WIDTH, resources.getString(R.string.gb_w)),
            new BuiltinVariable(VAR_SCREEN_HEIGHT, resources.getString(R.string.gb_h)),
        };

        mBuiltinVariables = new Pair[] {
                new Pair<>(resources.getString(R.string.bvc_dt), timeDataCollector.getBuiltinVariables(resources)),
                new Pair<>(resources.getString(R.string.bvc_screen), screen_variables),
                new Pair<>(resources.getString(R.string.bvc_stor), storageDataCollector.getBuiltinVariables(resources)),
                new Pair<>(resources.getString(R.string.bvc_bat), batteryDataCollector.getBuiltinVariables(resources)),
                new Pair<>(resources.getString(R.string.bvc_cpu), cpuDataCollector.getBuiltinVariables(resources)),
        };
    }

    public void end() {
        for(DataCollector collector : mDataCollectors) collector.end();
    }

    public void resume() {
        if(mResumedCount == 0) {
            for (DataCollector collector : mDataCollectors) collector.onResume();
        }
        mResumedCount++;
    }

    public void pause() {
        mResumedCount--;
        if(mResumedCount == 0) {
            for(DataCollector collector : mDataCollectors) collector.onPause();
        }
    }

    public Pair<String,BuiltinVariable[]>[] getBuiltinVariables() {
        return mBuiltinVariables;
    }

    public void setDisplayOrientationAndSize(int orientation, int width, int height) {
        mVariableManager.edit();
        mVariableManager.setVariable(VAR_SCREEN_ORIENTATION, orientation);
        mVariableManager.setVariable(VAR_SCREEN_WIDTH, width);
        mVariableManager.setVariable(VAR_SCREEN_HEIGHT, height);
        mVariableManager.commit();
    }
}
