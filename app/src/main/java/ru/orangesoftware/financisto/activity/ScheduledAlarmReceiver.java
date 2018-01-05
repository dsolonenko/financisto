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
package ru.orangesoftware.financisto.activity;

import ru.orangesoftware.financisto.service.FinancistoService;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import ru.orangesoftware.financisto.service.RecurrenceScheduler;

public class ScheduledAlarmReceiver extends PackageReplaceReceiver {

    private static final String BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";
    private static final String SCHEDULED_BACKUP = "ru.orangesoftware.financisto.SCHEDULED_BACKUP";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("ScheduledAlarmReceiver", "Received " + intent.getAction());
        String action = intent.getAction();
        if (BOOT_COMPLETED.equals(action)) {
            requestScheduleAll(context);
            requestScheduleAutoBackup(context);
        } else if (SCHEDULED_BACKUP.equals(action)) {
            requestAutoBackup(context);
        } else {
            requestScheduleOne(context, intent);
        }
    }

    private void requestScheduleOne(Context context, Intent intent) {
        Intent serviceIntent = new Intent(FinancistoService.ACTION_SCHEDULE_ONE, null, context, FinancistoService.class);
        serviceIntent.putExtra(RecurrenceScheduler.SCHEDULED_TRANSACTION_ID, intent.getLongExtra(RecurrenceScheduler.SCHEDULED_TRANSACTION_ID, -1));
        FinancistoService.enqueueWork(context, serviceIntent);
    }

    private void requestAutoBackup(Context context) {
        Intent serviceIntent = new Intent(FinancistoService.ACTION_AUTO_BACKUP, null, context, FinancistoService.class);
        FinancistoService.enqueueWork(context, serviceIntent);
    }

}
