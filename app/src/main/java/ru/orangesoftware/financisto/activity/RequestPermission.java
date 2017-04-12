package ru.orangesoftware.financisto.activity;

import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.content.ContextCompat;

public class RequestPermission {

    public final String permission;

    public RequestPermission(String permission) {
        this.permission = permission;
    }

    public static boolean requestPermissionIfNeeded(Context context, String permission) {
        int permissionCheck = ContextCompat.checkSelfPermission(context, permission);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            RequestPermissionActivity_.intent(context).start();
            return false;
        }
        return true;
    }

}
