package net.pierrox.lightning_launcher.engine.variable;

import android.content.Context;
import android.content.res.Resources;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.text.format.Formatter;

import net.pierrox.lightning_launcher.R;

import java.io.File;

/*package*/ class StorageDataCollector extends PollingDataCollector {
    private static final String VAR_EXT_TOTAL = "ext_total";
    private static final String VAR_EXT_FREE = "ext_free";
    private static final String VAR_INT_TOTAL = "int_total";
    private static final String VAR_INT_FREE = "int_free";
    private static final String VAR_EXT_TOTAL_H = "ext_total_h";
    private static final String VAR_EXT_FREE_H = "ext_free_h";
    private static final String VAR_INT_TOTAL_H = "int_total_h";
    private static final String VAR_INT_FREE_H = "int_free_h";

    private Context mContext;

    public StorageDataCollector(Context context, Handler handler, VariableManager vm) {
        super(10000, handler, vm);
        mContext = context;

        mSetVariablesRunnable.run();
    }

    @Override
    protected void collectData() {
        mHandler.post(mSetVariablesRunnable);
    }

    public BuiltinVariable[] getBuiltinVariables(Resources resources) {
        return new BuiltinVariable[] {
                new BuiltinVariable(VAR_EXT_TOTAL_H, resources.getString(R.string.bv_eth)),
                new BuiltinVariable(VAR_EXT_FREE_H, resources.getString(R.string.bv_efh)),
                new BuiltinVariable(VAR_INT_TOTAL_H, resources.getString(R.string.bv_ith)),
                new BuiltinVariable(VAR_INT_FREE_H, resources.getString(R.string.bv_ifh)),
                new BuiltinVariable(VAR_EXT_TOTAL, resources.getString(R.string.bv_et)),
                new BuiltinVariable(VAR_EXT_FREE, resources.getString(R.string.bv_ef)),
                new BuiltinVariable(VAR_INT_TOTAL, resources.getString(R.string.bv_it)),
                new BuiltinVariable(VAR_INT_FREE, resources.getString(R.string.bv_if)),
        };
    }

    private Runnable mSetVariablesRunnable = new Runnable() {
        @Override
        public void run() {
            long int_free;
            long int_total;
            long ext_free;
            long ext_total;

            File path;
            StatFs stat;
            long block_size;
            long total_blocks;
            long available_blocks;

            path = Environment.getDataDirectory();
            try {
                stat = new StatFs(path.getPath());
                block_size = stat.getBlockSize();
                total_blocks = stat.getBlockCount();
                available_blocks = stat.getAvailableBlocks();
                int_free = available_blocks*block_size;
                int_total = total_blocks*block_size;
            } catch(IllegalArgumentException e) {
                int_free = 0;
                int_total = 0;
            }

            path = Environment.getExternalStorageDirectory();
            try {
                stat = new StatFs(path.getPath());
                block_size = stat.getBlockSize();
                total_blocks = stat.getBlockCount();
                available_blocks = stat.getAvailableBlocks();
                ext_free = available_blocks * block_size;
                ext_total = total_blocks * block_size;
            } catch(IllegalArgumentException e) {
                ext_free = 0;
                ext_total = 0;
            }

            mVariableManager.edit();
            mVariableManager.setVariable(VAR_EXT_TOTAL, ext_total);
            mVariableManager.setVariable(VAR_EXT_FREE, ext_free);
            mVariableManager.setVariable(VAR_INT_TOTAL, int_total);
            mVariableManager.setVariable(VAR_INT_FREE, int_free);
            mVariableManager.setVariable(VAR_EXT_TOTAL_H, Formatter.formatFileSize(mContext, ext_total));
            mVariableManager.setVariable(VAR_EXT_FREE_H, Formatter.formatFileSize(mContext, ext_free));
            mVariableManager.setVariable(VAR_INT_TOTAL_H, Formatter.formatFileSize(mContext, int_total));
            mVariableManager.setVariable(VAR_INT_FREE_H, Formatter.formatFileSize(mContext, int_free));
            mVariableManager.commit();
        }
    };
}
