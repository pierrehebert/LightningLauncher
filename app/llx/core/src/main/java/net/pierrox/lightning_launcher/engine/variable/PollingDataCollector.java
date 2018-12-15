package net.pierrox.lightning_launcher.engine.variable;

import android.os.Handler;

/*package*/ abstract class PollingDataCollector extends Thread implements DataCollector {
    private int mPeriod;
    protected Handler mHandler;
    protected VariableManager mVariableManager;
    private boolean mPaused;
    private boolean mEnd;

    public PollingDataCollector(int period, Handler handler, VariableManager vm) {
        mPeriod = period;
        mHandler = handler;
        mVariableManager = vm;

        start();
    }

    @Override
    public void run() {
        do {
            collectData();

            if(mEnd) break;

            try {
                sleep(mPeriod);
            } catch (InterruptedException e) {
                // interrupted, will likely end
            }

            if(mPaused) {
                synchronized (this) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        // interrupted, will likely end
                    }
                }
            }
        } while(!mEnd);
    }

    @Override
    public void end() {
        mEnd = true;
        interrupt();
    }

    @Override
    public void onPause() {
        mPaused = true;
    }

    @Override
    public void onResume() {
        synchronized (this) {
            mPaused = false;
            notify();
        }
    }

    protected abstract void collectData();
}
