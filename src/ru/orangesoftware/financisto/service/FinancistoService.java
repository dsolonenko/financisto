/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 ******************************************************************************/
package ru.orangesoftware.financisto.service;

import static ru.orangesoftware.financisto.service.DailyAutoBackupScheduler.scheduleNextAutoBackup;
import static ru.orangesoftware.financisto.service.FlowzrAutoSyncScheduler.scheduleNextAutoSync;

import java.util.Date;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.activity.AbstractTransactionActivity;
import ru.orangesoftware.financisto.activity.AccountWidget;
import ru.orangesoftware.financisto.activity.MassOpActivity;
import ru.orangesoftware.financisto.backup.DatabaseExport;
import ru.orangesoftware.financisto.blotter.BlotterFilter;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.export.Export;
import ru.orangesoftware.financisto.export.flowzr.FlowzrSyncEngine;
import ru.orangesoftware.financisto.export.flowzr.FlowzrSyncTask;
import ru.orangesoftware.financisto.filter.WhereFilter;
import ru.orangesoftware.financisto.model.TransactionInfo;
import ru.orangesoftware.financisto.model.TransactionStatus;
import ru.orangesoftware.financisto.recur.NotificationOptions;
import ru.orangesoftware.financisto.utils.MyPreferences;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.IBinder;
import android.util.Log;

import com.commonsware.cwac.wakeful.WakefulIntentService;

public class FinancistoService extends WakefulIntentService {

	private static final String TAG = "FinancistoService";
    public static final String ACTION_SCHEDULE_ALL = "ru.orangesoftware.financisto.SCHEDULE_ALL";
    public static final String ACTION_SCHEDULE_ONE = "ru.orangesoftware.financisto.SCHEDULE_ONE";
    public static final String ACTION_SCHEDULE_AUTO_BACKUP = "ru.orangesoftware.financisto.ACTION_SCHEDULE_AUTO_BACKUP";
    public static final String ACTION_AUTO_BACKUP = "ru.orangesoftware.financisto.ACTION_AUTO_BACKUP";
    public static final String ACTION_SCHEDULE_AUTO_SYNC = "ru.orangesoftware.financisto.ACTION_SCHEDULE_AUTO_SYNC";
    public static final String ACTION_AUTO_SYNC = "ru.orangesoftware.financisto.ACTION_AUTO_SYNC";
    
	private static final int RESTORED_NOTIFICATION_ID = 0;

	private DatabaseAdapter db;
    private RecurrenceScheduler scheduler;

    public FinancistoService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        db = new DatabaseAdapter(this);
        db.open();
        scheduler = new RecurrenceScheduler(db);
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
        String action = intent.getAction();
        if (ACTION_SCHEDULE_ALL.equals(action)) {
            scheduleAll();
        } else if (ACTION_SCHEDULE_ONE.equals(action)) {
            scheduleOne(intent);
        } else if (ACTION_SCHEDULE_AUTO_BACKUP.equals(action)) {
            scheduleNextAutoBackup(this);
        } else if (ACTION_AUTO_BACKUP.equals(action)) {
            doAutoBackup();
        } else if (ACTION_SCHEDULE_AUTO_SYNC.equals(action)) {
            scheduleNextAutoSync(this);
        } else if (ACTION_AUTO_SYNC.equals(action)) {
            doAutoSync();
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
    
    private void doAutoSync() {
    	try {
    		Log.i(TAG, "Auto-sync started at " + new Date());    		
			if (isPushSyncNeed(MyPreferences.getFlowzrLastSync(getApplicationContext()))) {
        		if (FlowzrSyncEngine.isRunning) {
	        		Log.i(TAG,"sync already in progess");
        			return;
        		}
				new FlowzrSyncTask(getApplicationContext()).execute();
    		} else {
				Log.i(TAG,"no changes to push since " + new Date(MyPreferences.getFlowzrLastSync(getApplicationContext())).toString());
			}
    	} finally {
    		scheduleNextAutoSync(this);
    	}
    }
    
    private boolean isPushSyncNeed(long lastSyncLocalTimestamp) {
        String sql = "select count(*) from transactions where updated_on > " + lastSyncLocalTimestamp;
        Cursor c = db.db().rawQuery(sql, null);
        try {
            c.moveToFirst();
            long total = c.getLong(0);
            return total != 0;
        } finally {
            c.close();
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
                    Export.uploadBackupFileToDropbox(this, fileName);
                }
                Log.e(TAG, "Auto-backup completed in " +(System.currentTimeMillis()-t0)+"ms");
            } catch (Exception e) {
                Log.e(TAG, "Auto-backup unsuccessful", e);
            }
        } finally {
            scheduleNextAutoBackup(this);
        }
    }

    private void notifyUser(TransactionInfo transaction) {
		Notification notification = createNotification(transaction);
		notifyUser(notification, (int)transaction.id);
	}

	private void notifyUser(Notification notification, int id) {
		NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(id, notification);		
	}

	private Notification createRestoredNotification(int count) {
		long when = System.currentTimeMillis();
		String text = getString(R.string.scheduled_transactions_have_been_restored, count);
		Notification notification = new Notification(R.drawable.notification_icon_transaction, text, when);
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		notification.defaults = Notification.DEFAULT_ALL;
		Intent notificationIntent = new Intent(this, MassOpActivity.class);
		WhereFilter filter = new WhereFilter("");
		filter.eq(BlotterFilter.STATUS, TransactionStatus.RS.name());
		filter.toIntent(notificationIntent);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		notification.setLatestEventInfo(this, getString(R.string.scheduled_transactions_restored), text, contentIntent);
		return notification;
	}

	private Notification createNotification(TransactionInfo t) {
		long when = System.currentTimeMillis();
		Notification notification = new Notification(t.getNotificationIcon(), t.getNotificationTickerText(this), when);
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		applyNotificationOptions(notification, t.notificationOptions);
		Context context = getApplicationContext();
		Intent notificationIntent = new Intent(this, t.getActivity());
		notificationIntent.putExtra(AbstractTransactionActivity.TRAN_ID_EXTRA, t.id);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		notification.setLatestEventInfo(context, t.getNotificationContentTitle(this), t.getNotificationContentText(this), contentIntent);	
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
