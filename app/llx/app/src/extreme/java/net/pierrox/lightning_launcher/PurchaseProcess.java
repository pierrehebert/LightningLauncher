package net.pierrox.lightning_launcher;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import net.pierrox.lightning_launcher_extreme.R;
import net.pierrox.lightning_launcher.iab.IabHelper;
import net.pierrox.lightning_launcher.iab.IabResult;
import net.pierrox.lightning_launcher.iab.Inventory;
import net.pierrox.lightning_launcher.iab.Purchase;

import java.util.ArrayList;

public class PurchaseProcess extends Activity {
    private static final int REQUEST_PURCHASE_UNLOCK = 1;
    private static final String INTENT_EXTRA_SKU = "sku";

    private String mSku;

    private IabHelper mIABHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSku = getIntent().getStringExtra(INTENT_EXTRA_SKU);

        setContentView(R.layout.purchase_process);

        ((TextView)findViewById(R.id.au_msg)).setText(R.string.tr_eu);
        ((TextView)findViewById(R.id.au_pw)).setText(R.string.tr_pw);
        findViewById(R.id.au_ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        setWaitScreen(true, 0);

        mIABHelper = new IabHelper(this, ((LLAppExtreme) LLApp.get()).getIabKey());
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
                skus.add(mSku);
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
        mIABHelper.launchPurchaseFlow(this, mSku, REQUEST_PURCHASE_UNLOCK, mPurchaseFinishedListener, "");
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

            Purchase unlock_pro = inventory.getPurchase(mSku);
            boolean unlocked_pro = unlock_pro != null && verifyDeveloperPayload(unlock_pro);
            if(unlocked_pro) {
                setUnlocked(true);
                setWaitScreen(false, R.string.tr_ty);
            } else {
                mIABHelper.launchPurchaseFlow(PurchaseProcess.this, mSku, REQUEST_PURCHASE_UNLOCK, mPurchaseFinishedListener);
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
            if (sku.equals(mSku)) {
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
        ((LLAppExtreme)LLApp.get()).setProductStatus(mSku, unlocked);
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

    public static void startActivity(Context context, String sku) {
        Intent intent = new Intent(context, PurchaseProcess.class);
        intent.putExtra(INTENT_EXTRA_SKU, sku);
        context.startActivity(intent);
    }
}
