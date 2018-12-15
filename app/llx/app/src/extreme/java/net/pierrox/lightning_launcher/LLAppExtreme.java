package net.pierrox.lightning_launcher;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Process;
import android.view.View;
import android.widget.Toast;

import com.google.android.vending.licensing.LicenseChecker;
import com.google.android.vending.licensing.LicenseCheckerCallback;
import com.google.android.vending.licensing.Policy;
import com.google.android.vending.licensing.ServerManagedPolicy;

import net.pierrox.lightning_launcher.engine.LightningEngine;
import net.pierrox.lightning_launcher_extreme.R;
import net.pierrox.lightning_launcher.data.FileUtils;
import net.pierrox.lightning_launcher.data.Page;
import net.pierrox.lightning_launcher.iab.IabHelper;
import net.pierrox.lightning_launcher.iab.IabResult;
import net.pierrox.lightning_launcher.iab.Inventory;
import net.pierrox.lightning_launcher.iab.Purchase;
import net.pierrox.lightning_launcher.prefs.LLPreference;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

public class LLAppExtreme extends LLAppPhone {
    private static final String key1 = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA";
    private static final String key2 = "5FKITc94MjDMThUW1wOMqqt/m0TZnAu5spcmrEol6qGuNz";
    private static final String key3 = "m4/";
    private static final String key4 = "hI3T9SPxlX4faIxSX0hwLJAtbb5IZWX5XvuQdQovF9W9";
    private static final String key5 = "vRdURFT6D7K01k+doWbMDZnbfQXiYKHaaBja+SlsZA4UsHF6RubVTi+nOET1xBlpjNwQ6wl69GdM+y8WA1WR47JBNph6wuCF0q7pz2KbuBDvh5vSvYaBGb9dflqnOKy2S47DSA7HOwffTUtxilskp";
    private static final String key6 = "JvKKBdyKwQoNTKyp7bjXUrFg/tlJOTo0je4RkcvBHiYCW/yEQKSPY43nlnapcy6L4P+0IV+GDHI+Zx1D+mPo6BmsTwIDAQAB";

    private LicenseCheckerCallback mLicenseCheckerCallback;
    private LicenseChecker mChecker;
    private boolean mIsLicensed = true;

    private IabHelper mIABHelper;
    private String mIabKey;
    private boolean mHasLWPIab;
    private boolean mHasLWPKey;

    @Override
    public void onCreate() {
        // obfuscation
        String three = String.valueOf((new Date().getYear()+2901)/1000);
        mIabKey = key1 + three + key2;

        mLicenseCheckerCallback = new MyLicenseCheckerCallback();

        mIabKey +=  three + key3 + three + key4 + getString(R.string.internal_version);

        // obfuscation
        mIabKey += key5 + three + key6;

        // Construct the LicenseChecker with a Policy.
        mChecker = new LicenseChecker(
                this, new ServerManagedPolicy(this),
                mIabKey  // Your public licensing key.
        );

        checkLicense();

        checkLwpKey();

        setupIAB();

        super.onCreate();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        mChecker.onDestroy();
    }

    @Override
    public boolean isFreeVersion() {
        return false;
    }

    @Override
    public boolean isTrialVersion() {
        return !mIsLicensed;
    }

    @Override
    public boolean isTrialVersionExpired() {
        return !mIsLicensed;
    }

    @Override
    public long getTrialLeft() {
        return 0;
    }

    @Override
    public View managePreferenceViewLockedFlag(LLPreference preference, View preference_view) {
        return preference_view;
    }

    @Override
    public void manageAddItemDialogLockedFlag(View add_item_view, boolean locked) {
        // pass
    }

    @Override
    public void showFeatureLockedDialog(Context context) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(Version.APP_STORE_INSTALL_PREFIX + context.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch(ActivityNotFoundException e) {
            // pass
        }
        Toast.makeText(context, "Couldn't validate the Play Store license. Please check your internet connectivity.", Toast.LENGTH_LONG).show();

        checkLicense();
    }

    @Override
    public void startUnlockProcess(Context context) {
        // pass
    }

    @Override
    public void installPromotionalIcons(Page dashboard) {
        // pass
    }

    @Override
    public void checkLicense() {
        mChecker.checkAccess(mLicenseCheckerCallback);
    }

    private class MyLicenseCheckerCallback implements LicenseCheckerCallback {
        public void allow(int reason) {
            mIsLicensed = true;
        }

        public void dontAllow(int reason) {

            if (reason == Policy.RETRY) {
                // If the reason received from the policy is RETRY, it was probably
                // due to a loss of connection with the service, so we should give the
                // user a chance to retry. So show a dialog to retry.
                mIsLicensed = true;
            } else {
                // Otherwise, the user is not licensed to use this app.
                // Your response should always inform the user that the application
                // is not licensed, but your behavior at that point can vary. You might
                // provide the user a limited access version of your app or you can
                // take them to Google Play to purchase the app.
                mIsLicensed = false;
            }
        }

        @Override
        public void applicationError(int errorCode) {
        }
    }

    public String getIabKey() {
        return mIabKey;
    }

    private void setupIAB() {
        readStoredUnlockedStatus();

        mIABHelper = new IabHelper(this, getIabKey());
        mIABHelper.enableDebugLogging(BuildConfig.DEBUG);
        mIABHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                if (!result.isSuccess()) {
                    return;
                }

                // Have we been disposed of in the meantime? If so, quit.
                if (mIABHelper == null) return;

                checkProducts(null);
            }
        });
    }

    private static final String LWP_PKG = "net.pierrox.lightning_launcher.lwp_key";
    private static final String PATH_TEST = "t";
    private static final String COLUMN_IS_LICENSED = "l";

    public void checkLwpKey() {
        // first step : the permission is granted meaning the package is installed
        mHasLWPKey = checkPermission(LWP_PKG, Process.myPid(), Process.myUid()) == PackageManager.PERMISSION_GRANTED;

        // second step, ask the key to check its license
        new AsyncTask<Void,Void,Boolean>() {

            @Override
            protected Boolean doInBackground(Void... voids) {
                boolean hasLicensedKey = false;
                try {
                    Cursor c = getContentResolver().query(Uri.parse("content://"+LWP_PKG+"/" + PATH_TEST), new String[]{COLUMN_IS_LICENSED}, null, null, null);
                    if (c != null) {
                        c.moveToNext();
                        hasLicensedKey = c.getInt(0) == 1;
                        c.close();
                    }
                } catch(Exception e) {
                    // pass
                }
                return hasLicensedKey;
            }

            @Override
            protected void onPostExecute(Boolean hasLicensedKey) {
                // key is installed, but no license
                if(mHasLWPKey && !hasLicensedKey) {
                    LightningEngine engine = getAppEngine();
                    engine.getGlobalConfig().lwpScreen = Page.NONE;
                    engine.notifyGlobalConfigChanged();
                }
                mHasLWPKey = hasLicensedKey;
            }
        }.execute((Void)null);

    }

    private File getUnlockInfoDataFile() {
        return new File(getFilesDir(), "products");
    }

    private void readStoredUnlockedStatus() {
        File data = getUnlockInfoDataFile();

        mHasLWPIab = false;

        if(data.exists()) {
            JSONObject o = FileUtils.readJSONObjectFromFile(data);
            if(o != null) {
                try {
                    mHasLWPIab = o.getBoolean(getString(R.string.iab_lwp));
                } catch(JSONException e) {
                    // pass
                }
            }
        }
    }

    public void setProductStatus(String sku, boolean purchased) {
        if(sku.equals(getString(R.string.iab_lwp))) {
            mHasLWPIab = purchased;
        }
        File data = getUnlockInfoDataFile();
        JSONObject o = new JSONObject();
        try {
            o.put(sku, purchased);
            FileUtils.saveStringToFile(o.toString(), data);
        } catch (Exception e) {
            // pass
        }
    }

    public interface UnlockResultReceiver {
        void setUnlocked(String sku, boolean unlocked);
    }

    public void checkProducts(final UnlockResultReceiver receiver) {
        // Have we been disposed of in the meantime? If so, quit.
        if (mIABHelper == null || !mIABHelper.isSetupDone()) return;

        ArrayList<String> skus = new ArrayList<>(1);
        final String iab_lwp = getString(R.string.iab_lwp);
        skus.add(iab_lwp);

        mIABHelper.queryInventoryAsync(true, skus, new IabHelper.QueryInventoryFinishedListener() {
            @Override
            public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
                if (result.isFailure()) {
                    return;
                }

                Purchase lwp = inventory.getPurchase(iab_lwp);
                boolean has_lwp = lwp != null /*&& verifyDeveloperPayload(unlock_pro)*/;
                if(has_lwp) {
                    setProductStatus(iab_lwp, true);
                    if(receiver != null) receiver.setUnlocked(iab_lwp, true);
                } else {
                    setProductStatus(iab_lwp, false);
                    if(receiver != null) receiver.setUnlocked(iab_lwp, false);
                }
            }
        });
    }

    public boolean hasLWP() {
        return mHasLWPIab || mHasLWPKey;
    }

    public void showProductLockedDialog(final Context context, int title, int message, final String sku) {
        // need an activity context to add a window
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getString(title));
        builder.setMessage(context.getString(message));
        builder.setPositiveButton(context.getString(R.string.iab_y_key), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                context.startActivity(Intent.createChooser(new Intent(Intent.ACTION_VIEW, Uri.parse(Version.APP_STORE_INSTALL_PREFIX+"net.pierrox.lightning_launcher.lwp_key")), ""));
            }
        });
        builder.setNegativeButton(context.getString(R.string.iab_y_app), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startPurchaseProcess(context, sku);
            }
        });
        builder.setNeutralButton(context.getString(R.string.iab_no), null);
        builder.create().show();
    }

    private void startPurchaseProcess(Context context, String sku) {
        PurchaseProcess.startActivity(context, sku);
    }
}
