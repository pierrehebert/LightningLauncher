package net.pierrox.lightning_launcher.data;

import android.Manifest;

import net.pierrox.lightning_launcher.R;

public enum Error {
    MISSING_PERMISSION_READ_CALL_LOG(Manifest.permission.READ_CALL_LOG, R.string.pr_r11),
    MISSING_PERMISSION_READ_SMS(Manifest.permission.READ_SMS, R.string.pr_r12),
    MISSING_PERMISSION_CALL_PHONE(Manifest.permission.CALL_PHONE, R.string.pr_r13);

    String permission;
    int msg;
    Error(String permission, int msg) {
        this.permission = permission;
        this.msg = msg;
    }

    public boolean isPermission() {
        return permission != null;
    }

    public String getPermission() {
        return permission;
    }

    public int getMsg() {
        return msg;
    }
}
