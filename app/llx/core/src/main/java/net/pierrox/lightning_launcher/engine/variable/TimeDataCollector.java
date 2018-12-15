package net.pierrox.lightning_launcher.engine.variable;

import android.content.res.Resources;
import android.os.Handler;

import net.pierrox.lightning_launcher.R;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeDataCollector extends PollingDataCollector {
    private static final String VAR_TIMESTAMP = "ll_timestamp";
    private static final String VAR_SECOND = "ll_second";
    private static final String VAR_MINUTE = "ll_minute";
    private static final String VAR_HOUR12 = "ll_hour12";
    private static final String VAR_HOUR24 = "ll_hour24";
    private static final String VAR_DAY = "ll_day";
    private static final String VAR_MONTH = "ll_month";
    private static final String VAR_WEEK = "ll_week";
    private static final String VAR_YEAR = "ll_year";
    private static final String VAR_AM_PM = "ll_am_pm";
    private static final String VAR_DAY_NAME = "ll_day_name";
    private static final String VAR_MONTH_NAME = "ll_month_name";

    private SimpleDateFormat mDfSecond = new SimpleDateFormat("ss");
    private SimpleDateFormat mDfMinute = new SimpleDateFormat("mm");
    private SimpleDateFormat mDfHour12 = new SimpleDateFormat("hh");
    private SimpleDateFormat mDfHour24 = new SimpleDateFormat("HH");
    private SimpleDateFormat mDfDay = new SimpleDateFormat("dd");
    private SimpleDateFormat mDfMonth = new SimpleDateFormat("MM");
    private SimpleDateFormat mDfWeek = new SimpleDateFormat("w");
    private SimpleDateFormat mDfYear = new SimpleDateFormat("yyyy");
    private SimpleDateFormat mDfAmPm = new SimpleDateFormat("a");
    private SimpleDateFormat mDfDayName = new SimpleDateFormat("EEEE");
    private SimpleDateFormat mDfMonthName = new SimpleDateFormat("MMMM");

    private int mPreviousSecond = -1;

    public TimeDataCollector(Handler handler, VariableManager vm) {
        super(1000, handler, vm);

        mSetVariablesRunnable.run();
    }

    @Override
    protected void collectData() {
        mHandler.post(mSetVariablesRunnable);
    }

    public BuiltinVariable[] getBuiltinVariables(Resources resources) {
        return new BuiltinVariable[] {
                new BuiltinVariable(VAR_SECOND, resources.getString(R.string.bv_sec)),
                new BuiltinVariable(VAR_MINUTE, resources.getString(R.string.bv_min)),
                new BuiltinVariable(VAR_HOUR12, resources.getString(R.string.bv_h12)),
                new BuiltinVariable(VAR_HOUR24, resources.getString(R.string.bv_h24)),
                new BuiltinVariable(VAR_AM_PM, resources.getString(R.string.bv_ampm)),
                new BuiltinVariable(VAR_DAY, resources.getString(R.string.bv_day)),
                new BuiltinVariable(VAR_MONTH, resources.getString(R.string.bv_mon)),
                new BuiltinVariable(VAR_WEEK, resources.getString(R.string.bv_week)),
                new BuiltinVariable(VAR_YEAR, resources.getString(R.string.bv_year)),
                new BuiltinVariable(VAR_DAY_NAME, resources.getString(R.string.bv_dayn)),
                new BuiltinVariable(VAR_MONTH_NAME, resources.getString(R.string.bv_monn)),
                new BuiltinVariable(VAR_TIMESTAMP, resources.getString(R.string.bv_ts)),
        };
    }

    private Runnable mSetVariablesRunnable = new Runnable() {
        @Override
        public void run() {
            // text fields, but some of them can be converted to integer
            Date now = new Date();
            int second = now.getSeconds();
            mVariableManager.edit();
            mVariableManager.setVariable(VAR_SECOND, mDfSecond.format(now));
            mVariableManager.setVariable(VAR_TIMESTAMP, now.getTime()/1000);
            if(mPreviousSecond == -1 || second<mPreviousSecond) {
                mVariableManager.setVariable(VAR_MINUTE, mDfMinute.format(now));
                mVariableManager.setVariable(VAR_HOUR12, mDfHour12.format(now));
                mVariableManager.setVariable(VAR_HOUR24, mDfHour24.format(now));
                mVariableManager.setVariable(VAR_DAY, mDfDay.format(now));
                mVariableManager.setVariable(VAR_WEEK, mDfWeek.format(now));
                mVariableManager.setVariable(VAR_MONTH, mDfMonth.format(now));
                mVariableManager.setVariable(VAR_YEAR, mDfYear.format(now));
                mVariableManager.setVariable(VAR_AM_PM, mDfAmPm.format(now));
                mVariableManager.setVariable(VAR_DAY_NAME, mDfDayName.format(now));
                mVariableManager.setVariable(VAR_MONTH_NAME, mDfMonthName.format(now));
            }
            mPreviousSecond = second;
            mVariableManager.commit();
        }
    };

    @Override
    public void onResume() {
        // force an immediate update
        mPreviousSecond = -1;
        super.onResume();
    }
}
