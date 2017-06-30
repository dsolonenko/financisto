package ru.orangesoftware.financisto.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 7/25/11 7:16 PM
 */
public class AndroidUtils {

    private AndroidUtils(){}

    // Kudos to http://code.google.com/p/csipsimple/source/browse/trunk/CSipSimple/src/com/csipsimple/utils/Compatibility.java
    public static boolean isInstalledOnSdCard(Context context) {
        // check for API level 8 and higher
        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
            ApplicationInfo ai = pi.applicationInfo;
            return (ai.flags & 0x00040000 /*ApplicationInfo.FLAG_EXTERNAL_STORAGE*/) == 0x00040000 /*ApplicationInfo.FLAG_EXTERNAL_STORAGE*/;
        } catch (PackageManager.NameNotFoundException e) {
            // ignore
        }

        // check for API level 7 - check files dir
        try {
            String filesDir = context.getFilesDir().getAbsolutePath();
            if (filesDir.startsWith("/data/")) {
                return false;
            } else if (filesDir.contains(Environment.getExternalStorageDirectory().getPath())) {
                return true;
            }
        } catch (Throwable e) {
            // ignore
        }

        return false;
    }

}
