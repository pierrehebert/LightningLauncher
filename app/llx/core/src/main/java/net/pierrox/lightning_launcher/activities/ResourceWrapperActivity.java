package net.pierrox.lightning_launcher.activities;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Build;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.widget.Toast;

import net.pierrox.lightning_launcher.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class ResourceWrapperActivity extends Activity {
    public static final int REQUEST_PERMISSION_BASE = 1000000;
    public static final int REQUEST_PERMISSION_FONT_PICKER = REQUEST_PERMISSION_BASE + 1;

    private ResourcesWrapperHelper mResourcesWrapperHelper;

    @Override
    public final Resources getResources() {
        if(mResourcesWrapperHelper == null) {
            mResourcesWrapperHelper = new ResourcesWrapperHelper(this, super.getResources());
        }
        return mResourcesWrapperHelper.getResources();
    }

    public final Resources getRealResources() {
        return super.getResources();
    }

    @TargetApi(Build.VERSION_CODES.M)
    public boolean checkPermissions(String[] permissions, int[] rationales, final int requestCode) {
        if(Build.VERSION.SDK_INT >= 23) {
            final ArrayList<String> permissionsToRequest = new ArrayList<>();
            final ArrayList<String> permissionsToExplain = new ArrayList<>();
            for (String p : permissions) {
                if(checkSelfPermission(p) == PackageManager.PERMISSION_DENIED) {
                    if(shouldShowRequestPermissionRationale(p)) {
                        permissionsToExplain.add(p);
                    } else {
                        permissionsToRequest.add(p);
                    }
                }
            }

            if(permissionsToExplain.size() == 0) {
                if(permissionsToRequest.size() > 0) {
                    requestPermissions(listToArray(permissionsToRequest), requestCode);
                    return false;
                } else {
                    return true;
                }
            } else {
                permissionsToRequest.addAll(permissionsToExplain);
                final String[] permissionsToRequestArray = listToArray(permissionsToRequest);
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(getString(R.string.pr_t));
                SpannableStringBuilder message = new SpannableStringBuilder();
                message.append(getString(R.string.pr_s));
                int l = permissionsToRequest.size();
                for (int i=0; i<l; i++) {
                    String p = permissionsToRequest.get(i);
                    String short_p = p.substring(p.lastIndexOf('.')+1);
                    SpannableString bold_p = new SpannableString(short_p);
                    bold_p.setSpan(new StyleSpan(Typeface.BOLD), 0, short_p.length(), 0);
                    message.append("\n\n • ")
                           .append(bold_p)
                           .append('\n')
                           .append(getString(rationales[i]));
                }
                builder.setMessage(message);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        requestPermissions(permissionsToRequestArray, requestCode);
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int l = permissionsToRequestArray.length;
                        int[] grantResults = new int[l];
                        Arrays.fill(grantResults, PackageManager.PERMISSION_DENIED);
                        onRequestPermissionsResult(requestCode, permissionsToRequestArray, grantResults);
                    }
                });
                builder.create().show();
                return false;
            }
        } else {
            return true;
        }
    }

    protected boolean areAllPermissionsGranted(int[] grantResults, int errorToast) {
        boolean ok = true;
        for (int r : grantResults) {
            if(r == PackageManager.PERMISSION_DENIED) {
                ok = false;
                break;
            }
        }
        if(ok) {
            return true;
        } else {
            Toast.makeText(this, errorToast, Toast.LENGTH_LONG).show();
            finish();
            return false;
        }
    }

    private static String[] listToArray(List<String> l) {
        String[] p = new String[l.size()];
        l.toArray(p);
        return p;
    }
}
