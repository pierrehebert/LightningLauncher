package net.pierrox.lightning_launcher;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import net.pierrox.lightning_launcher.LLAppPhone;
import net.pierrox.lightning_launcher.activities.AppUnlocker;
import net.pierrox.lightning_launcher.data.FileUtils;
import net.pierrox.lightning_launcher.data.Page;
import net.pierrox.lightning_launcher.data.Shortcut;
import net.pierrox.lightning_launcher.data.Utils;
import net.pierrox.lightning_launcher.iab.IabHelper;
import net.pierrox.lightning_launcher.iab.IabResult;
import net.pierrox.lightning_launcher.iab.Inventory;
import net.pierrox.lightning_launcher.iab.Purchase;
import net.pierrox.lightning_launcher.prefs.LLPreference;
import net.pierrox.lightning_launcher_extreme.BuildConfig;
import net.pierrox.lightning_launcher_extreme.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

public class LLAppTrial extends LLAppPhone {

    // public key obfuscated tr "MIAQuk" "*<_=()" part 1
    private static final String SCREEN = "DOe78xjw(jymH98)VG+vHOExCvdl*2K62vw5FO50Nf9DGU(LWRR(R0<1_g8C_syfFNVgrtv5BPpU3tzgBb<XL)gdD";
    private static final String DRAWER = "qHo/EJ)=r)m1beW92FnBxFFRLaF=xh=)hPds=<D_=_B";
    // obfuscated: IAB public key
    public static String ICON_LOADER_DATA;

    private static final long TRIAL_SHIFT = 365L*2*86400*1000;
    private static final long TRIAL_DURATION = 7L*86400*1000 + 1987; //1987 to avoid clear text 7days

    private boolean mIsOldFreeUser;
    private boolean mIsUnlocked;
    private long mTrialPeriodStartDate;

    private IabHelper mIABHelper;

    @Override
    public void onCreate() {
        super.onCreate();

        generateKey();

        setupInstallType(mUpgradingOldFreeVersion);

        checkUnlocked();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        if(mIABHelper != null) {
            mIABHelper.dispose();
            mIABHelper = null;
        }
    }

    private File getDataFile() {
        File dir = new File(Environment.getExternalStorageDirectory(), "Android");
        dir.mkdirs();
        return new File(dir, ".mnd");
    }

    private void setupInstallType(boolean upgrading_old_free_version) {
        File data = getDataFile();

        Boolean is_old_free_user = null;
        if(data.exists()) {
            JSONObject o = FileUtils.readJSONObjectFromFile(data);
            if(o != null) {
                try {
                    if (o.has("f")) {
                        is_old_free_user = o.getBoolean("f");
                        if (!is_old_free_user) {
                            mTrialPeriodStartDate = o.getLong("s") + TRIAL_SHIFT;
                            if (getTrialLeft() > 7 * 86400000L) {
                                mTrialPeriodStartDate = 0;
                            }
                            mIsUnlocked = o.getBoolean("u");
                        }
                    }
                    if(o.has("u")) {
                        mIsUnlocked = o.getBoolean("u");
                    }
                } catch(JSONException e) {
                    // pass
                }
            }
        }

        if(is_old_free_user == null) {
            mTrialPeriodStartDate = System.currentTimeMillis();
            is_old_free_user = upgrading_old_free_version;

            try {
                JSONObject o = new JSONObject();
                o.put("f", is_old_free_user);
                if(!is_old_free_user) {
                    o.put("s", mTrialPeriodStartDate - TRIAL_SHIFT);
                }
                FileUtils.saveStringToFile(o.toString(), data);
            } catch(Exception e) {
                // pass
            }
        }

        mIsOldFreeUser = is_old_free_user;
    }

    private void checkUnlocked() {
        mIABHelper = new IabHelper(this, ICON_LOADER_DATA);
        mIABHelper.enableDebugLogging(BuildConfig.DEBUG);
        mIABHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                if (!result.isSuccess()) {
                    return;
                }

                // Have we been disposed of in the meantime? If so, quit.
                if (mIABHelper == null) return;


                ArrayList<String> skus = new ArrayList<String>();
                final String iab_unlock_free = getString(R.string.iab_unlock_free);
                final String iab_unlock_trial = getString(R.string.iab_unlock_trial);
                skus.add(iab_unlock_free);
                skus.add(iab_unlock_trial);

                mIABHelper.queryInventoryAsync(true, skus, new IabHelper.QueryInventoryFinishedListener() {
                    @Override
                    public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
                        if (result.isFailure()) {
                            return;
                        }

                        Purchase unlock_free = inventory.getPurchase(iab_unlock_free);
                        Purchase unlock_trial = inventory.getPurchase(iab_unlock_trial);
                        boolean unlocked_free = unlock_free != null /*&& verifyDeveloperPayload(unlock_free)*/;
                        boolean unlocked_trial = unlock_trial != null /*&& verifyDeveloperPayload(unlock_trial)*/;
                        if(unlocked_free || unlocked_trial) {
                            setUnlocked(true);
                        } else {
                            setUnlocked(false);
                        }
                    }
                });
            }
        });
    }

    public void setUnlocked(boolean unlocked) {
        mIsUnlocked = unlocked;
        File data = getDataFile();
        JSONObject o = new JSONObject();
        try {
            o.put("f", mIsOldFreeUser);
            if(!mIsOldFreeUser) {
                o.put("s", mTrialPeriodStartDate - TRIAL_SHIFT);
            }
            o.put("u", mIsUnlocked);
            FileUtils.saveStringToFile(o.toString(), data);
        } catch (Exception e) {
            // pass
        }
    }

    @Override
    public boolean isFreeVersion() {
        // should return true if existing setup and not unlocked
        LLAppTrial app = (LLAppTrial) LLAppTrial.get();
        return app.mIsOldFreeUser && !app.mIsUnlocked;
    }

    @Override
    public boolean isTrialVersion() {
        // should return true if new setup and not unlocked
        LLAppTrial app = (LLAppTrial) LLAppTrial.get();
        return !app.mIsOldFreeUser && !app.mIsUnlocked;
    }

    @Override
    public boolean isTrialVersionExpired() {
        // should return true if new setup and not unlocked and delay expired
        if(!isTrialVersion()) return false;
        final long diff = System.currentTimeMillis() - mTrialPeriodStartDate;
        return diff < 0 || diff > TRIAL_DURATION;
    }

    @Override
    public long getTrialLeft() {
        final long left = (mTrialPeriodStartDate+TRIAL_DURATION) - System.currentTimeMillis();
        return left < 0 ? 0 : left;
    }

    @Override
    public View managePreferenceViewLockedFlag(LLPreference preference, View preference_view) {
        if (preference.isLocked()) {
            final Context context = preference_view.getContext();
            FrameLayout fl = new FrameLayout(context);
            ImageView locked = new ImageView(context);
            locked.setImageResource(R.drawable.full);
            locked.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.RIGHT));
            fl.addView(preference_view);
            fl.addView(locked);
            return fl;
        } else {
            return preference_view;
        }
    }

    @Override
    public void manageAddItemDialogLockedFlag(View add_item_view, boolean locked) {
        add_item_view.findViewById(R.id.full).setVisibility(locked ? View.VISIBLE : View.GONE);
    }

    @Override
    public void showFeatureLockedDialog(final Context context) {
        // need an activity context to add a window
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.tr_fl_t);
        builder.setMessage(R.string.tr_fl_m);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startUnlockProcess(context);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.create().show();
    }

    @Override
    public void startUnlockProcess(Context context) {
        context.startActivity(new Intent(context, AppUnlocker.class));
    }

    @Override
    public void installPromotionalIcons(Page dashboard) {
        Drawable download_d = getResources().getDrawable(R.drawable.download);
        installPromotionalIcon(dashboard, download_d, 0, 3, R.drawable.p2, "Mini Golf'oid", "net.pierrox.mini_golfoid_free");
        installPromotionalIcon(dashboard, download_d, 1, 3, R.drawable.p1, "Baby's Games", "net.pierrox.baby_games");
        installPromotionalIcon(dashboard, download_d, 2, 3, R.drawable.p3, "Marine Compass", "net.pierrox.mcompass");
        installPromotionalIcon(dashboard, download_d, 3, 3, R.drawable.p4, "Let's Dance!", "net.pierrox.lets_dance");
    }

    @Override
    public void checkLicense() {
        // pass
    }

    private void installPromotionalIcon(Page dashboard, Drawable download_d, int x, int y, int icon, String title, String pkg_name) {
        Shortcut new_item=new Shortcut();
        int new_id=dashboard.findFreeItemId();
        File icon_dir=dashboard.getAndCreateIconDir();
        icon_dir.mkdirs();
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(Version.APP_STORE_INSTALL_PREFIX+pkg_name));
        new_item.init(new_id, new Rect(x, y, x+1, y+1), null, title, intent, dashboard.config, icon_dir);
        int size = Utils.getStandardIconSize();
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Drawable icon_d = getResources().getDrawable(icon);
        icon_d.setBounds(0, 0, size, size);
        icon_d.draw(canvas);
        download_d.setBounds(0, 0, size, size);
        download_d.draw(canvas);
        Utils.saveIconToFile(new_item.getDefaultIconFile(icon_dir), bitmap);
        dashboard.addItem(new_item);
    }

    // compute the IAB public key from obfuscated data
    private static void generateKey() {
        ICON_LOADER_DATA = AppUnlocker.ICON_DATA + "t" + SCREEN + "9" + DRAWER;
        String tr = "*M<I_A=Q(u)k";
        final int length = tr.length();
        for(int i=0; i<length;i+=2) {
            ICON_LOADER_DATA = ICON_LOADER_DATA.replace(tr.charAt(i), tr.charAt(i+1));
        }
    }
}
