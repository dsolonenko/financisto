/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * <p/>
 * Contributors:
 * Denis Solonenko - initial API and implementation
 ******************************************************************************/
package ru.orangesoftware.financisto.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import com.commonsware.cwac.wakeful.WakefulIntentService;
import java.util.Date;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.activity.AbstractTransactionActivity;
import ru.orangesoftware.financisto.activity.AccountWidget;
import ru.orangesoftware.financisto.activity.MassOpActivity;
import ru.orangesoftware.financisto.backup.DatabaseExport;
import ru.orangesoftware.financisto.blotter.BlotterFilter;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.export.Export;
import ru.orangesoftware.financisto.filter.WhereFilter;
import ru.orangesoftware.financisto.model.Transaction;
import ru.orangesoftware.financisto.model.TransactionInfo;
import ru.orangesoftware.financisto.model.TransactionStatus;
import ru.orangesoftware.financisto.recur.NotificationOptions;
import static ru.orangesoftware.financisto.service.DailyAutoBackupScheduler.scheduleNextAutoBackup;
import static ru.orangesoftware.financisto.service.SmsReceiver.SMS_TRANSACTION_BODY;
import static ru.orangesoftware.financisto.service.SmsReceiver.SMS_TRANSACTION_NUMBER;
import ru.orangesoftware.financisto.utils.MyPreferences;

public class FinancistoService extends WakefulIntentService {

    private static final String TAG = "FinancistoService";
    public static final String ACTION_SCHEDULE_ALL = "ru.orangesoftware.financisto.SCHEDULE_ALL";
    public static final String ACTION_SCHEDULE_ONE = "ru.orangesoftware.financisto.SCHEDULE_ONE";
    public static final String ACTION_SCHEDULE_AUTO_BACKUP = "ru.orangesoftware.financisto.ACTION_SCHEDULE_AUTO_BACKUP";
    public static final String ACTION_AUTO_BACKUP = "ru.orangesoftware.financisto.ACTION_AUTO_BACKUP";
    public static final String ACTION_NEW_TRANSACTION_SMS = "ru.orangesoftware.financisto.NEW_TRANSACTON_SMS";

    private static final int RESTORED_NOTIFICATION_ID = 0;

    private DatabaseAdapter db;
    private RecurrenceScheduler scheduler;
    private SmsTransactionProcessor smsProcessor;

    public FinancistoService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        db = new DatabaseAdapter(this);
        db.open();
        scheduler = new RecurrenceScheduler(db);
        smsProcessor = new SmsTransactionProcessor(db);
    }

    @Override
    public void onDestroy() {
        if (db != null) {
            db.close();
        }
        super.onDestroy();
    }

    @Override
    protected void doWakefulWork(Intent intent) {
        final String action = intent.getAction();
        if (ACTION_SCHEDULE_ALL.equals(action)) {
            scheduleAll();
        } else if (ACTION_SCHEDULE_ONE.equals(action)) {
            scheduleOne(intent);
        } else if (ACTION_SCHEDULE_AUTO_BACKUP.equals(action)) {
            scheduleNextAutoBackup(this);
        } else if (ACTION_AUTO_BACKUP.equals(action)) {
            doAutoBackup();
        } else if (ACTION_NEW_TRANSACTION_SMS.equals(action)) {
            processSmsTransaction(intent);
        }
    }

    private void processSmsTransaction(Intent intent) {
        String number = intent.getStringExtra(SMS_TRANSACTION_NUMBER);
        String body = intent.getStringExtra(SMS_TRANSACTION_BODY);
        if (number != null && body != null) {
            Transaction t = smsProcessor.createTransactionBySms(number, body);
            if (t != null) {
                TransactionInfo tInfo = db.getTransactionInfo(t.id);
                notifyUser(tInfo);
                AccountWidget.updateWidgets(this);
//                Toast.makeText(this, R.string.new_transaction_from_sms, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void scheduleAll() {
        int restoredTransactionsCount = scheduler.scheduleAll(this);
        if (restoredTransactionsCount > 0) {
            notifyUser(createRestoredNotification(restoredTransactionsCount), RESTORED_NOTIFICATION_ID);
        }
    }

    private void scheduleOne(Intent intent) {
        long scheduledTransactionId = intent.getLongExtra(RecurrenceScheduler.SCHEDULED_TRANSACTION_ID, -1);
        if (scheduledTransactionId > 0) {
            TransactionInfo transaction = scheduler.scheduleOne(this, scheduledTransactionId);
            if (transaction != null) {
                notifyUser(transaction);
                AccountWidget.updateWidgets(this);
            }
        }
    }

    private void doAutoBackup() {
        try {
            try {
                long t0 = System.currentTimeMillis();
                Log.e(TAG, "Auto-backup started at " + new Date());
                DatabaseExport export = new DatabaseExport(this, db.db(), true);
                String fileName = export.export();
                if (MyPreferences.isDropboxUploadAutoBackups(this)) {
                    try {
                        Export.uploadBackupFileToDropbox(this, fileName);
                    } catch (Exception e) {
                        Log.e(TAG, "Unable to upload auto-backup to Dropbox", e);
                    }
                }
                if (MyPreferences.isGoogleDriveUploadAutoBackups(this)) {
                    try {
                        Export.uploadBackupFileToGoogleDrive(this, fileName);
                    } catch (Exception e) {
                        Log.e(TAG, "Unable to upload auto-backup to Google Drive", e);
                    }
                }
                Log.e(TAG, "Auto-backup completed in " + (System.currentTimeMillis() - t0) + "ms");
            } catch (Exception e) {
                Log.e(TAG, "Auto-backup unsuccessful", e);
            }
        } finally {
            scheduleNextAutoBackup(this);
        }
    }

    private void notifyUser(TransactionInfo transaction) {
        Notification notification = createNotification(transaction);
        notifyUser(notification, (int) transaction.id);
    }

    private void notifyUser(Notification notification, int id) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(id, notification);
    }

    private Notification createRestoredNotification(int count) {
        long when = System.currentTimeMillis();
        String text = getString(R.string.scheduled_transactions_have_been_restored, count);
        String contentTitle = getString(R.string.scheduled_transactions_restored);

        Intent notificationIntent = new Intent(this, MassOpActivity.class);
        WhereFilter filter = new WhereFilter("");
        filter.eq(BlotterFilter.STATUS, TransactionStatus.RS.name());
        filter.toIntent(notificationIntent);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        return new NotificationCompat.Builder(this)
                .setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.notification_icon_transaction)
                .setWhen(when)
                .setTicker(text)
                .setContentText(text)
                .setContentTitle(contentTitle)
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .build();
    }

    private Notification createNotification(TransactionInfo t) {
        long when = System.currentTimeMillis();

        Intent notificationIntent = new Intent(this, t.getActivity());
        notificationIntent.putExtra(AbstractTransactionActivity.TRAN_ID_EXTRA, t.id);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        String tickerText = t.getNotificationTickerText(this);

        String contentTitle = t.getNotificationContentTitle(this);
        String text = t.getNotificationContentText(this);

        Notification notification = new NotificationCompat.Builder(this)
                .setContentIntent(contentIntent)
                .setSmallIcon(t.getNotificationIcon())
                .setWhen(when)
                .setTicker(tickerText)
                .setContentText(text)
                .setContentTitle(contentTitle)
                .setAutoCancel(true)
                .build();

        applyNotificationOptions(notification, t.notificationOptions);


        return notification;
    }

    private void applyNotificationOptions(Notification notification, String notificationOptions) {
        if (notificationOptions == null) {
            notification.defaults = Notification.DEFAULT_ALL;
        } else {
            NotificationOptions options = NotificationOptions.parse(notificationOptions);
            options.apply(notification);
        }
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

}
