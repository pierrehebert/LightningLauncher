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

package net.pierrox.lightning_launcher.util;

import android.app.ActivityManager;
import android.app.Service;
import android.content.*;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import net.pierrox.lightning_launcher.LLApp;
import net.pierrox.lightning_launcher.activities.Dashboard;
import net.pierrox.lightning_launcher.activities.LockScreen;
import net.pierrox.lightning_launcher.data.Page;

import java.lang.reflect.Method;
import java.util.List;


public class EmptyService extends Service {
    private BroadcastReceiver mBroadcastReceiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                boolean launched = false;
                if(LockScreen.sThis==null && LLApp.get().getAppEngine().getGlobalConfig().lockScreen != Page.NONE) {
                    TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                    if(tm.getCallState() == TelephonyManager.CALL_STATE_IDLE) {
                        Intent locked = new Intent(Dashboard.BROADCAST_ACTION_LOCKED);
                        sendBroadcast(locked);

                        Intent intent1 = new Intent(EmptyService.this, LockScreen.class);
                        intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent1);
                        launched = true;
                    }
                }

                if((launched || LockScreen.sThis!=null) && mMoveTaskToFront != null) {
                    mMode = MODE_CHECK_ON_TOP;
                    mHandler.postDelayed(mCheckOnTop, 3000);
                }
            } else if(LockScreen.sMyTaskId != null && action.equals(Intent.ACTION_SCREEN_ON)) {
                if(mMoveTaskToFront != null) {
                    mHandler.removeCallbacks(mCheckOnTop);
                }

//                List<ActivityManager.RunningTaskInfo> tasks = mActivityManager.getRunningTasks(1);
//                String cn = tasks.get(0).topActivity.flattenToShortString();
//                Log.i("XXX", cn);
//                if(!cn.contains("com.android.phone") &&
//                   !cn.contains("com.sonyericsson.organizer") &&
//                   !cn.contains("com.handcent.nextsms") &&
//                   !cn.contains("ch.bitspin.timely")) {
//                    try {
//                        mMoveTaskToFront.invoke(mActivityManager, LockScreen.sMyTaskId, 0);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }

                // com.sonyericsson.organizer/.Organizer
                // com.android.phone


//                if(mIsKeyguardLocked != null) {
//                    try {
//                        boolean locked = (boolean) mIsKeyguardLocked.invoke(mKeyguardManager);
//                        if(locked) {
//                            Log.i("XXX", "locked");
//                            mMoveTaskToFront.invoke(mActivityManager, LockScreen.sMyTaskId, 0);
//                        }
//                    } catch (Exception e) {
//                        // pass
//                    }
//                }

//                TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
//                if(tm.getCallState() == TelephonyManager.CALL_STATE_IDLE) {
//                    try {
//                        mMoveTaskToFront.invoke(mActivityManager, LockScreen.sMyTaskId, 0);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
            }
        }
    };

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

    private ActivityManager mActivityManager;
    private Method mMoveTaskToFront;
    private ComponentName mLockScreenComponenentName;
    private Handler mHandler;

    private static final int MODE_CHECK_ON_TOP = 0;
    private static final int MODE_MOVE_ON_TOP_PENDING = 1;
    private int mMode;

    @Override
    public void onCreate() {
        super.onCreate();

        mActivityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        try {
            mMoveTaskToFront = ActivityManager.class.getMethod("moveTaskToFront", int.class, int.class);
            mLockScreenComponenentName = new ComponentName(this, LockScreen.class);
            mHandler = new Handler();
        } catch (NoSuchMethodException e) {
            mMoveTaskToFront = null;
        }

        if(Build.VERSION.SDK_INT>=8) {
            IntentFilter intent_filter=new IntentFilter();
            intent_filter.addAction(Intent.ACTION_SCREEN_OFF);
            if(mMoveTaskToFront != null) {
                intent_filter.addAction(Intent.ACTION_SCREEN_ON);
            }
            registerReceiver(mBroadcastReceiver, intent_filter);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if(Build.VERSION.SDK_INT>=8) {
            unregisterReceiver(mBroadcastReceiver);
        }
    }


    private Runnable mCheckOnTop = new Runnable() {
        @Override
        public void run() {
            if(mMode == MODE_CHECK_ON_TOP) {
                if(LockScreen.sThis != null) {
                    List<ActivityManager.RunningTaskInfo> tasks = mActivityManager.getRunningTasks(1);
                    if(mLockScreenComponenentName.compareTo(tasks.get(0).topActivity) != 0) {
                        mMode = MODE_MOVE_ON_TOP_PENDING;
                        mHandler.postDelayed(mCheckOnTop, 22000);
                    } else {
                        mHandler.postDelayed(mCheckOnTop, 2000);
                    }
                }
            } else {
                if(LockScreen.sThis != null) {
                    try {
                        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                        if(tm.getCallState() == TelephonyManager.CALL_STATE_IDLE) {
                            mMoveTaskToFront.invoke(mActivityManager, LockScreen.sMyTaskId, 0);
                        }
                        mMode = MODE_CHECK_ON_TOP;
                        mHandler.postDelayed(mCheckOnTop, 3000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    };
}
