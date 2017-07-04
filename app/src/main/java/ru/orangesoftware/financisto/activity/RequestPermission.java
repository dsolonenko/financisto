package ru.orangesoftware.financisto.activity;

import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.content.ContextCompat;

class RequestPermission {

    static boolean isRequestingPermission(Context context, String permission) {
        int permissionCheck = ContextCompat.checkSelfPermission(context, permission);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            RequestPermissionActivity_.intent(context).requestedPermission(permission).start();
            return true;
        }
        return false;
    }

}
