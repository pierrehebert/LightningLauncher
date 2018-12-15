package net.pierrox.lightning_launcher.engine.variable;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.BatteryManager;

import net.pierrox.lightning_launcher.R;

/*package*/ class BatteryDataCollector implements DataCollector {
    private static final String VAR_LEVEL = "bat_level";
    private static final String VAR_STATUS = "bat_status";
    private static final String VAR_VOLTAGE = "bat_voltage";
    private static final String VAR_PLUGGED = "bat_plugged";

    private Context mContext;
    private BroadcastReceiver mBatteryReceiver;

    /*package*/ BatteryDataCollector(Context context, final VariableManager vm) {
        mContext = context;
        mBatteryReceiver=new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int level=intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
                int scale=intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
                int status=intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN);
                int voltage=intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
                int plugged=intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
                vm.setVariable(VAR_LEVEL, level*100/scale);
                vm.setVariable(VAR_STATUS, status);
                vm.setVariable(VAR_VOLTAGE, voltage);
                vm.setVariable(VAR_PLUGGED, plugged);
            }
        };
    }

    @Override
    public void onPause() {
        mContext.unregisterReceiver(mBatteryReceiver);
    }

    @Override
    public void onResume() {
        mContext.registerReceiver(mBatteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    @Override
    public void end() {

    }

    @Override
    public BuiltinVariable[] getBuiltinVariables(Resources resources) {
        return new BuiltinVariable[] {
                new BuiltinVariable(VAR_LEVEL, resources.getString(R.string.bv_batl)),
                new BuiltinVariable(VAR_STATUS, resources.getString(R.string.bv_batst)),
                new BuiltinVariable(VAR_VOLTAGE, resources.getString(R.string.bv_batv)),
                new BuiltinVariable(VAR_PLUGGED, resources.getString(R.string.bv_batp)),
        };
    }
}
