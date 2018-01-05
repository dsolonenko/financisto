package ru.orangesoftware.financisto.utils;

import android.content.Context;

import java.util.Date;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.datetime.DateUtils;

public class IntegrityCheckAutobackup implements IntegrityCheck {

    private final Context context;
    private final long threshold;

    public IntegrityCheckAutobackup(Context context, long threshold) {
        this.context = context;
        this.threshold = threshold;
    }

    @Override
    public Result check() {
        if (MyPreferences.isAutoBackupEnabled(context)) {
            if (MyPreferences.isAutoBackupWarningEnabled(context)) {
                MyPreferences.AutobackupStatus status = MyPreferences.getAutobackupStatus(context);
                if (status.notify) {
                    MyPreferences.notifyAutobackupSucceeded(context);
                    return new Result(Level.ERROR,
                            context.getString(R.string.autobackup_failed_message,
                                    DateUtils.getTimeFormat(context).format(new Date(status.timestamp)),
                                    status.errorMessage));
                }
            }
        } else {
            if (MyPreferences.isAutoBackupReminderEnabled(context)) {
                long lastCheck = MyPreferences.getLastAutobackupCheck(context);
                if (lastCheck == 0) {
                    MyPreferences.updateLastAutobackupCheck(context);
                } else {
                    long delta = System.currentTimeMillis() - lastCheck;
                    if (delta > threshold) {
                        MyPreferences.updateLastAutobackupCheck(context);
                        return new Result(Level.INFO, context.getString(R.string.auto_backup_is_not_enabled));
                    }
                }
            }
        }
        return Result.OK;
    }

}
