package net.pierrox.lightning_launcher.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import net.pierrox.lightning_launcher.LLApp;
import net.pierrox.lightning_launcher.LLAppTrial;
import net.pierrox.lightning_launcher.activities.ResourceWrapperActivity;
import net.pierrox.lightning_launcher.iab.IabHelper;
import net.pierrox.lightning_launcher.iab.IabResult;
import net.pierrox.lightning_launcher.iab.Inventory;
import net.pierrox.lightning_launcher.iab.Purchase;
import net.pierrox.lightning_launcher_extreme.BuildConfig;
import net.pierrox.lightning_launcher_extreme.R;

import java.util.ArrayList;

public class AppUnlocker extends ResourceWrapperActivity {
    public static final String ICON_DATA = "*<Gf*_0GCSqGS<b3D=EB_=U__4GN_DCBi=KBg=CD4txD(TS(wSCVRGr*e7ZVFjhTPSpDgoBlH_O8glTaTP";

    private static final int REQUEST_PURCHASE_UNLOCK = 1;

    private String SKU_UNLOCK_FREE;
    private String SKU_UNLOCK_TRIAL;

    private IabHelper mIABHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SKU_UNLOCK_FREE = getString(R.string.iab_unlock_free);
        SKU_UNLOCK_TRIAL = getString(R.string.iab_unlock_trial);

        setContentView(R.layout.app_unlocker);

        ((TextView)findViewById(R.id.au_msg)).setText(R.string.tr_eu);
        ((TextView)findViewById(R.id.au_pw)).setText(R.string.tr_pw);
        findViewById(R.id.au_ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        setWaitScreen(true, 0);

        String base64EncodedPublicKey = LLAppTrial.ICON_LOADER_DATA;
        mIABHelper = new IabHelper(this, base64EncodedPublicKey);
        mIABHelper.enableDebugLogging(BuildConfig.DEBUG);
        mIABHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                if (!result.isSuccess()) {
                    setWaitScreen(false, R.string.tr_eu);
                    return;
                }

                // Have we been disposed of in the meantime? If so, quit.
                if (mIABHelper == null) return;


                ArrayList<String> skus = new ArrayList<String>();
                skus.add(SKU_UNLOCK_FREE);
                skus.add(SKU_UNLOCK_TRIAL);
                mIABHelper.queryInventoryAsync(true, skus, mGotInventoryListener);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mIABHelper != null) {
            mIABHelper.dispose();
            mIABHelper = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(mIABHelper == null) {
            super.onActivityResult(requestCode, resultCode, data);
        }

        if (!mIABHelper.handleActivityResult(requestCode, resultCode, data)) {
            // not handled, so handle it ourselves (here's where you'd
            // perform any handling of activity results not related to in-app
            // billing...
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /*package*/ void purchaseUnlock() {
        mIABHelper.launchPurchaseFlow(this, SKU_UNLOCK_FREE, REQUEST_PURCHASE_UNLOCK, mPurchaseFinishedListener, "");
    }

    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        @Override
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            if (mIABHelper == null) return;

            // Is it a failure?
            if (result.isFailure()) {
                setWaitScreen(false, R.string.tr_eu);
                return;
            }

            Purchase unlock_free = inventory.getPurchase(SKU_UNLOCK_FREE);
            Purchase unlock_trial = inventory.getPurchase(SKU_UNLOCK_TRIAL);
            boolean unlocked_free = unlock_free != null && verifyDeveloperPayload(unlock_free);
            boolean unlocked_trial = unlock_trial != null && verifyDeveloperPayload(unlock_trial);
            if(unlocked_free || unlocked_trial) {
                setUnlocked(true);
                setWaitScreen(false, R.string.tr_ty);
            } else {
                mIABHelper.launchPurchaseFlow(AppUnlocker.this, LLApp.get().isFreeVersion() ? SKU_UNLOCK_FREE : SKU_UNLOCK_TRIAL, REQUEST_PURCHASE_UNLOCK, mPurchaseFinishedListener);
                setUnlocked(false);
            }
        }
    };

    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            if (mIABHelper == null) return;

            if (result.isFailure()) {
                setWaitScreen(false, result.getResponse() == IabHelper.IABHELPER_USER_CANCELLED ? R.string.tr_c : R.string.tr_eu);
                return;
            }
            if (!verifyDeveloperPayload(purchase)) {
                setWaitScreen(false, R.string.tr_eu);
                return;
            }

            final String sku = purchase.getSku();
            if (sku.equals(SKU_UNLOCK_FREE) || sku.equals(SKU_UNLOCK_TRIAL)) {
                setUnlocked(true);
                setWaitScreen(false, R.string.tr_ty);
            } else {
                setUnlocked(false);
                setWaitScreen(false, R.string.tr_eu);
            }
        }
    };

    private void setWaitScreen(boolean on, int msg_res_id) {
        findViewById(R.id.au_d).setVisibility(on ? View.INVISIBLE : View.VISIBLE);
        findViewById(R.id.au_w).setVisibility(on ? View.VISIBLE : View.INVISIBLE);
        if(msg_res_id != 0) {
            ((TextView)findViewById(R.id.au_msg)).setText(msg_res_id);
        }
    }

    private void setUnlocked(boolean unlocked) {
        ((LLAppTrial)LLApp.get()).setUnlocked(unlocked);
    }

    /** Verifies the developer payload of a purchase. */
    boolean verifyDeveloperPayload(Purchase p) {
        String payload = p.getDeveloperPayload();

        /*
         * TODO: verify that the developer payload of the purchase is correct. It will be
         * the same one that you sent when initiating the purchase.
         *
         * WARNING: Locally generating a random string when starting a purchase and
         * verifying it here might seem like a good approach, but this will fail in the
         * case where the user purchases an item on one device and then uses your app on
         * a different device, because on the other device you will not have access to the
         * random string you originally generated.
         *
         * So a good developer payload has these characteristics:
         *
         * 1. If two different users purchase an item, the payload is different between them,
         *    so that one user's purchase can't be replayed to another user.
         *
         * 2. The payload must be such that you can verify it even when the app wasn't the
         *    one who initiated the purchase flow (so that items purchased by the user on
         *    one device work on other devices owned by the user).
         *
         * Using your own server to store and verify developer payloads across app
         * installations is recommended.
         */

        return true;
    }
}
