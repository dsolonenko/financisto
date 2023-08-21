package ru.orangesoftware.financisto.activity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import android.os.Build.VERSION_CODES;
import androidx.core.content.ContextCompat;

public class RequestPermission {

    public static boolean isRequestingPermission(Context context, String permission) {
        if (permission == Manifest.permission.WRITE_EXTERNAL_STORAGE) {
            if (Build.VERSION.SDK_INT > VERSION_CODES.P) return false;
        }
        if (!checkPermission(context, permission)) {
            RequestPermissionActivity_.intent(context).requestedPermission(permission).start();
            return true;
        }
        return false;
    }

    public static boolean checkPermission(Context ctx, String permission) {
        if (permission == Manifest.permission.WRITE_EXTERNAL_STORAGE){
            if (Build.VERSION.SDK_INT > VERSION_CODES.P) return true;
        }
        return ContextCompat.checkSelfPermission(ctx, permission) == PackageManager.PERMISSION_GRANTED;
    }

    static boolean isRequestingPermissions(Context context, String... permissions) {
        for (String permission : permissions) {
            if (isRequestingPermission(context, permission)) return true;
        }
        return false;
    }

}
