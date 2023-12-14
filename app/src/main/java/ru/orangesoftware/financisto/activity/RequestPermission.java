package ru.orangesoftware.financisto.activity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.content.ContextCompat;

import java.util.Objects;

import ru.orangesoftware.financisto.app.DependenciesHolder;
import ru.orangesoftware.financisto.persistance.BackupPreferences;

public class RequestPermission {
    static DependenciesHolder dependencies = new DependenciesHolder();

    public static boolean isRequestingPermission(Context context, String permission) {
        if (!checkPermission(context, permission)) {
            RequestPermissionActivity.intent(context).requestedPermission(permission).start();
            return true;
        }
        return false;
    }

    public static boolean checkPermission(Context ctx, String permission) {
        if (Objects.equals(permission, Manifest.permission.WRITE_EXTERNAL_STORAGE))
            return checkWritablePath(ctx);
        return ContextCompat.checkSelfPermission(ctx, permission) == PackageManager.PERMISSION_GRANTED;
    }

    static boolean isRequestingPermissions(Context context, String... permissions) {
        for (String permission : permissions) {
            if (isRequestingPermission(context, permission)) return true;
        }
        return false;
    }

    private static boolean checkWritablePath(Context ctx) {
        boolean isWritable;

        BackupPreferences backupPreferences = dependencies.getPreferencesStore().getBackupPreferencesRx().blockingFirst();
        isWritable = ctx.getContentResolver().getPersistedUriPermissions().stream().anyMatch(
                persistedUriPermission -> persistedUriPermission.getUri().equals(backupPreferences.getFolder())
        );
        return isWritable;
    }
}
