package net.pierrox.lightning_launcher.data;

import android.accounts.*;
import android.content.*;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.provider.CallLog;
import android.text.format.Formatter;

import net.pierrox.lightning_launcher.configuration.DynamicTextConfig;
import net.pierrox.lightning_launcher.configuration.JsonFields;
import net.pierrox.lightning_launcher.configuration.ShortcutConfig;
import net.pierrox.lightning_launcher.R;
import net.pierrox.lightning_launcher.views.item.ItemView;
import net.pierrox.lightning_launcher.views.item.ShortcutView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DynamicText extends Shortcut {
    private DynamicTextConfig mDynamicTextConfig;

    private boolean mCreated;

    private SimpleDateFormat mDateFormat;

    private DecimalFormat mCountFormat;

    private Handler mHandler;
    private ContentObserver mContentObserver;
    private Cursor mCursor;

    private String mGMailInboxUri;

    private Context mBatteryContext;
    private int mBatteryLevel = -1;
    private BroadcastReceiver mBatteryReceiver;

    private int mCount;
    private boolean mEditMode;

    public DynamicText(Page page) {
        super(page);
    }

    public DynamicTextConfig getDynamicTextConfig() {
        return mDynamicTextConfig;
    }

    public void setDynamicTextConfig(DynamicTextConfig c) {
        mDynamicTextConfig = c;
    }

    public DynamicTextConfig modifyDynamicTextConfig() {
        DynamicTextConfig dtc = new DynamicTextConfig();
        dtc.copyFrom(mDynamicTextConfig);
        mDynamicTextConfig = dtc;
        return dtc;
    }

    @Override
    public void createFromJSONObject(JSONObject o) throws JSONException {
        JSONObject json_configuration=o.optJSONObject(JsonFields.DYNAMIC_TEXT_CONFIGURATION);
        DynamicTextConfig defaultDynamicTextConfig = mPage.config.defaultDynamicTextConfig;
        if(json_configuration!=null) {
            mDynamicTextConfig=DynamicTextConfig.readFromJsonObject(json_configuration, defaultDynamicTextConfig);
        } else {
            mDynamicTextConfig = defaultDynamicTextConfig;
        }

        super.createFromJSONObject(o);

        modifyShortcutConfig().iconVisibility = false;
    }

    public void init(int id, Rect cell_p, Rect cell_l, DynamicTextConfig.Source source) {
        Intent intent = new Intent();
        intent.setComponent(getDefaultComponentNameForSource(source));
        super.init(id, cell_p, cell_l, "", intent);
        mShortcutConfig=new ShortcutConfig();
        mShortcutConfig.iconVisibility = false;
        mSharedShortcutConfig=false;
        mDynamicTextConfig=new DynamicTextConfig();
        mDynamicTextConfig.source = source;
    }

    @Override
    public ItemView createView(Context context) {
        return new ShortcutView(context, this, 0, 0);
    }

    private void create() {
        final Context context = mPage.getEngine().getContext();
        switch(mDynamicTextConfig.source) {
            case DATE:
                mHandler = new Handler();
                try {
                    mDateFormat = new SimpleDateFormat(mDynamicTextConfig.dateFormat);
                } catch (IllegalArgumentException e) {
                    mDateFormat = new SimpleDateFormat("'"+context.getString(R.string.dt_format_error)+"'");
                }
                break;

            case STORAGE:
            case HEAP_FREE:
            case HEAP_MAX:
                mHandler = new Handler();
                break;

            case BATTERY_LEVEL:
                mBatteryContext = context.getApplicationContext();
                mBatteryReceiver=new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        int new_level=intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
                        if(new_level!=mBatteryLevel) {
                            mBatteryLevel=new_level;
                            updateText();
                        }
                    }
                };
                try {
                    mCountFormat = new DecimalFormat(mDynamicTextConfig.countFormat);
                } catch (IllegalArgumentException e) {
                    mCountFormat = new DecimalFormat("'"+context.getString(R.string.dt_format_error)+"'");
                }
                break;

            case MISSED_CALLS:
            case UNREAD_SMS:
            case UNREAD_GMAIL:
                try {
                    mCountFormat = new DecimalFormat(mDynamicTextConfig.countFormat);
                } catch (IllegalArgumentException e) {
                    mCountFormat = new DecimalFormat("'"+context.getString(R.string.dt_format_error)+"'");
                }

                mHandler = new Handler();
                mContentObserver=new ContentObserver(mHandler) {
                    @Override
                    public void onChange(boolean selfChange) {
                        updateText();
                    }
                };
                mCursor = null;

                try {
                    switch(mDynamicTextConfig.source) {
                        case MISSED_CALLS:
                            String[] projection = { CallLog.Calls.CACHED_NAME, CallLog.Calls.CACHED_NUMBER_LABEL, CallLog.Calls.TYPE };
                            String where = CallLog.Calls.TYPE+"="+CallLog.Calls.MISSED_TYPE+" and "+CallLog.Calls.NEW+"=1";
                            try {
                                mCursor = context.getContentResolver().query(CallLog.Calls.CONTENT_URI, projection, where, null, null);
                            } catch(SecurityException e) {
                                mPage.onItemError(this, Error.MISSING_PERMISSION_READ_CALL_LOG);
                            }
                            break;

                        case UNREAD_SMS:
                            Uri sms_content = Uri.parse("content://sms");
                            try {
                                mCursor = context.getContentResolver().query(sms_content, null, "read = 0", null, null);
                            } catch(SecurityException e) {
                                mPage.onItemError(this, Error.MISSING_PERMISSION_READ_SMS);
                            }
                            break;

                        case UNREAD_GMAIL:
                            if(GmailContract.canReadLabels(context)) {
                                AccountManager.get(context).getAccountsByTypeAndFeatures(GmailContract.ACCOUNT_TYPE_GOOGLE, GmailContract.FEATURES_MAIL,
                                        new AccountManagerCallback<Account[]>() {
                                            @Override
                                            public void run(AccountManagerFuture<Account[]> future) {
                                                Account[] accounts = null;
                                                try {
                                                    accounts = future.getResult();
                                                } catch (OperationCanceledException e) {
                                                } catch (IOException e) {
                                                } catch (AuthenticatorException e) {
                                                    // pass
                                                }
                                                onAccountResults(context, accounts);
                                            }
                                        }, null /* handler */);
                            }
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                }

                if(mCursor != null) {
                    setupMessagingCursor();
                }
                break;
        }

        mCreated = true;
    }

    @Override
    public void pause() {
        super.pause();
        if(!mCreated) return;
        switch(mDynamicTextConfig.source) {
            case DATE:
            case STORAGE:
            case HEAP_FREE:
            case HEAP_MAX:
                mHandler.removeCallbacks(mTimerRunnable);
                break;

            case MISSED_CALLS:
                break;

            case UNREAD_SMS:
                break;

            case UNREAD_GMAIL:
                break;

            case BATTERY_LEVEL:
                mBatteryContext.unregisterReceiver(mBatteryReceiver);
                break;
        }

        super.pause();
    }

    @Override
    public void resume() {
        super.resume();
        if(!mCreated) {
            create();
        }

//        Log.i("XXX", "DT resume "+mId);
        switch(mDynamicTextConfig.source) {
            case DATE:
            case STORAGE:
            case HEAP_FREE:
            case HEAP_MAX:
                mTimerRunnable.run();
                break;

            case MISSED_CALLS:
                break;

            case UNREAD_SMS:
                updateText();
                break;

            case UNREAD_GMAIL:
                break;

            case BATTERY_LEVEL:
                mBatteryContext.registerReceiver(mBatteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                break;

        }

        super.resume();
    }


    @Override
    public void onCreate() {
        super.onCreate();
//        create();
    }

    @Override
    public void onDestroy() {
        if(!mCreated) return;

//        Log.i("XXX", "DT destroy " + mId);
        if(mCursor != null) {
            mCursor.close();
        }

        mBatteryReceiver = null;
        mBatteryContext = null;

        mCreated = false;

        super.onDestroy();
    }

    public static ComponentName getDefaultComponentNameForSource(DynamicTextConfig.Source source) {
        String cn = null;
        switch(source) {
            case MISSED_CALLS: cn="com.android.contacts/.DialtactsActivity"; break;
            case UNREAD_SMS: cn="com.android.mms/.ui.ConversationList"; break;
            case UNREAD_GMAIL: cn="com.google.android.gm/.ConversationListActivityGmail"; break;
            case DATE: cn="com.google.android.deskclock/com.android.deskclock.DeskClock"; break;
            case STORAGE: cn="com.android.settings/.Settings"; break;
            case BATTERY_LEVEL: cn="com.android.settings/.BatteryInfo"; break;
            case HEAP_FREE:
            case HEAP_MAX: cn="com.android.settings/.Settings"; break;
        }
        return ComponentName.unflattenFromString(cn);
    }

    public void setEditMode(boolean edit_mode) {
        mEditMode = edit_mode;
        updateVisibility();
    }

    private Runnable mTimerRunnable = new Runnable() {
        @Override
        public void run() {
            updateText();
            mHandler.postDelayed(mTimerRunnable, 1000);
        }
    };

    private void onAccountResults(Context context, Account[] accounts) {
        if (accounts != null && accounts.length > 0) {
            try {
                for (Account account : accounts) {
                    mCursor = context.getContentResolver().query(GmailContract.Labels.getLabelsUri(account.name), null, null, null, null);

                    if (mCursor != null) {
                        String label = mDynamicTextConfig.gmailLabel;
                        final int match_index = mCursor.getColumnIndexOrThrow(label == null ? GmailContract.Labels.CANONICAL_NAME : GmailContract.Labels.NAME);
                        if (label == null) {
                            label = GmailContract.Labels.LabelCanonicalNames.CANONICAL_NAME_ALL_MAIL;
                        }
                        while (mCursor.moveToNext()) {
                            String row_label = mCursor.getString(match_index);
                            String full_label = account.name + " / " + row_label;
                            if (label.equals(full_label) || (account == accounts[0] && label.equals(row_label))) {
                                int n = mCursor.getColumnIndex(GmailContract.Labels.URI);
                                mGMailInboxUri = mCursor.getString(n);
                                break;
                            }
                        }
                        setupMessagingCursor();
                    }
                }
            } catch(Exception e) {
                // pass
                e.printStackTrace();
            }
        }
    }

    private void setupMessagingCursor() {
        mCursor.registerContentObserver(mContentObserver);
        updateText();
    }

    private void updateText() {

        String new_text = null;

        switch(mDynamicTextConfig.source) {
            case DATE:
                if(mDateFormat == null) {
                    mDateFormat = new SimpleDateFormat(DynamicTextConfig.DEFAULT_DATE_FORMAT);
                }
                new_text = mDateFormat.format(new Date());
                break;

            case STORAGE:
                File path = mDynamicTextConfig.storageSource == DynamicTextConfig.StorageSource.INTERNAL ? Environment.getDataDirectory() : Environment.getExternalStorageDirectory();
                StatFs stat = new StatFs(path.getPath());
                long block_size = stat.getBlockSize();
                long total_blocks = stat.getBlockCount();
                long available_blocks = stat.getAvailableBlocks();
                long value;
                switch(mDynamicTextConfig.storageWhat) {
                case LEFT: value = available_blocks; break;
                case USED: value = total_blocks-available_blocks; break;
                default: value = total_blocks;
                }
                value *= block_size;
                switch(mDynamicTextConfig.storageFormat) {
                case NORMAL:
                    new_text = Formatter.formatFileSize(mPage.getEngine().getContext(), value);
                    break;

                case SHORT:
                    new_text = Formatter.formatShortFileSize(mPage.getEngine().getContext(), value);
                    break;

                case PERCENT:
                    new_text = String.valueOf(value*100 / (total_blocks*block_size))+"%";
                    break;

                case BYTES:
                default:
                    new_text = String.valueOf(value);
                    break;
                }
                new_text = mDynamicTextConfig.textFormat.replace("%s", new_text);
                break;

            case HEAP_FREE:
            case HEAP_MAX:
                Runtime r = Runtime.getRuntime();
                long m = r.maxMemory();
                if(mDynamicTextConfig.source== DynamicTextConfig.Source.HEAP_FREE) {
                    r.gc();
                    m -= (r.totalMemory() - r.freeMemory());
                }
                new_text = Formatter.formatFileSize(mPage.getEngine().getContext(), m);
                new_text = mDynamicTextConfig.textFormat.replace("%s", new_text);
                break;

            case MISSED_CALLS:
            case UNREAD_SMS:
            case UNREAD_GMAIL:
                mCount = 0;
                if(mDynamicTextConfig.source== DynamicTextConfig.Source.UNREAD_GMAIL) {
                    if(mGMailInboxUri != null) {
                        Cursor c = mPage.getEngine().getContext().getContentResolver().query(Uri.parse(mGMailInboxUri), null, null, null, null);
                        c.moveToNext();
                        int n = c.getColumnIndex(GmailContract.Labels.NUM_UNREAD_CONVERSATIONS);
                        mCount = c.getInt(n);
                        new_text = mCountFormat.format(mCount);
                        c.close();
                    } else {
                        new_text = "   ";
                    }
                } else {
                	if(mCursor != null) {
	                    mCursor.requery();
	                    mCursor.moveToFirst();
                        mCount = mCursor.getCount();
                        new_text = mCountFormat.format(mCount);
                    } else {
                        new_text = "   ";
                	}
                }
                updateVisibility();
                break;

            case BATTERY_LEVEL:
                new_text = mBatteryLevel==-1 ? "" : mCountFormat.format(mBatteryLevel);
                break;
        }
        if(new_text != null) {
            if(!new_text.equals(getLabel())) {
                // TODO propagate changes to views
                setLabel(new_text);
            }
        }
    }

    private void updateVisibility() {
        setVisible(mCount != 0 || mDynamicTextConfig.displayEmpty || mEditMode);
    }
}
