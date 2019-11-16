package ru.orangesoftware.financisto.activity;

import android.content.Context;
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;

public class RequestPermission {

    public static boolean isRequestingPermission(Context context, String permission) {
        if (!checkPermission(context, permission)) {
            RequestPermissionActivity_.intent(context).requestedPermission(permission).start();
            return true;
        }
        return false;
    }

    public static boolean checkPermission(Context ctx, String permission) {
        return ContextCompat.checkSelfPermission(ctx, permission) == PackageManager.PERMISSION_GRANTED;
    }

    static boolean isRequestingPermissions(Context context, String... permissions) {
        for (String permission : permissions) {
            if (isRequestingPermission(context, permission)) return true;
        }
        return false;
    }

}
