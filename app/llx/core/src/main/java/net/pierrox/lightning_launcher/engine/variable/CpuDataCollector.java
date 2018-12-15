package net.pierrox.lightning_launcher.engine.variable;

import android.content.res.Resources;
import android.os.Handler;

import net.pierrox.lightning_launcher.R;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class CpuDataCollector extends PollingDataCollector {
//    private static String VAR_COUNT = "cpu_count";
    private static String VAR_USAGE = "cpu_usage";
    private static String VAR_USER = "cpu_user";
    private static String VAR_NICE = "cpu_nice";
    private static String VAR_SYSTEM = "cpu_system";
    private static String VAR_IDLE = "cpu_idle";
    private static String VAR_IOWAIT = "cpu_iowait";
    private static String VAR_IRQ = "cpu_irq";
    private static String VAR_SOFTIRQ = "cpu_softirq";

    private static class CpuUsage {
        long user;
        long nice;
        long system;
        long idle;
        long iowait;
        long irq;
        long softirq;
        long total;
    }

    private CpuUsage mPreviousUsage;
    private CpuUsage mCurrentUsage;

    public CpuDataCollector(Handler handler, VariableManager vm) {
        super(2500, handler, vm);

        mPreviousUsage = new CpuUsage();
        mCurrentUsage = new CpuUsage();
        readProcStat(mPreviousUsage);
    }

    @Override
    protected void collectData() {
        try {
            // the normal period will be 2500+500, the initial collect time will be 500 only (faster refresh at startup)
            Thread.sleep(500);
        } catch (InterruptedException e) {
            // pass
        }

        readProcStat(mCurrentUsage);
        mHandler.post(mSetVariablesRunnable);
    }

    @Override
    public BuiltinVariable[] getBuiltinVariables(Resources resources) {
        return new BuiltinVariable[] {
                new BuiltinVariable(VAR_USAGE, resources.getString(R.string.bv_cu)),
                new BuiltinVariable(VAR_USER, resources.getString(R.string.bv_cr)),
                new BuiltinVariable(VAR_NICE, resources.getString(R.string.bv_cn)),
                new BuiltinVariable(VAR_SYSTEM, resources.getString(R.string.bv_cs)),
                new BuiltinVariable(VAR_IDLE, resources.getString(R.string.bv_ci)),
                new BuiltinVariable(VAR_IOWAIT, resources.getString(R.string.bv_co)),
                new BuiltinVariable(VAR_IRQ, resources.getString(R.string.bv_cq)),
                new BuiltinVariable(VAR_SOFTIRQ, resources.getString(R.string.bv_cf)),
        };
    }

    private Runnable mSetVariablesRunnable = new Runnable() {
        @Override
        public void run() {
            long delta = mCurrentUsage.total - mPreviousUsage.total;
            if(delta != 0) {
                long user = (mCurrentUsage.total - mCurrentUsage.idle) - (mPreviousUsage.total - mPreviousUsage.idle);
                mVariableManager.edit();
                mVariableManager.setVariable(VAR_USAGE, percent(user, delta));
                mVariableManager.setVariable(VAR_USER, percent(mCurrentUsage.user - mPreviousUsage.user, delta));
                mVariableManager.setVariable(VAR_NICE, percent(mCurrentUsage.nice - mPreviousUsage.nice, delta));
                mVariableManager.setVariable(VAR_SYSTEM, percent(mCurrentUsage.system - mPreviousUsage.system, delta));
                mVariableManager.setVariable(VAR_IDLE, percent(mCurrentUsage.idle - mPreviousUsage.idle, delta));
                mVariableManager.setVariable(VAR_IOWAIT, percent(mCurrentUsage.iowait - mPreviousUsage.iowait, delta));
                mVariableManager.setVariable(VAR_IRQ, percent(mCurrentUsage.irq - mPreviousUsage.irq, delta));
                mVariableManager.setVariable(VAR_SOFTIRQ, percent(mCurrentUsage.softirq - mPreviousUsage.softirq, delta));
                mVariableManager.commit();

                CpuUsage tmp = mPreviousUsage;
                mPreviousUsage = mCurrentUsage;
                mCurrentUsage = tmp;
            }
        }
    };

    private static int percent(long value, long total) {
        return (int)(value*100/total);
    }

    private void readProcStat(CpuUsage out) {

        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader("/proc/stat"));
            String line = br.readLine();
            String[] tokens = line.split(" +");
            out.user = Long.parseLong(tokens[1]);
            out.nice = Long.parseLong(tokens[2]);
            out.system = Long.parseLong(tokens[3]);
            out.idle = Long.parseLong(tokens[4]);
            out.iowait = Long.parseLong(tokens[5]);
            out.irq = Long.parseLong(tokens[6]);
            out.softirq = Long.parseLong(tokens[7]);
            out.total = out.user + out.nice + out.system + out.idle + out.iowait + out.irq + out.softirq;
        } catch (IOException e) {
            // pass
        } finally {
            if(br != null) try { br.close(); } catch (IOException e) { /*pass*/ }
        }
    }
}
