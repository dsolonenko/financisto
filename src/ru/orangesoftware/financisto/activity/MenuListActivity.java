/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * <p/>
 * Contributors:
 * Denis Solonenko - initial API and implementation
 * Abdsandryk Souza - implementing 2D chart reports
 ******************************************************************************/
package ru.orangesoftware.financisto.activity;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.export.csv.CsvExportOptions;
import ru.orangesoftware.financisto.export.csv.CsvImportOptions;
import ru.orangesoftware.financisto.export.qif.QifExportOptions;
import ru.orangesoftware.financisto.export.qif.QifImportOptions;
import ru.orangesoftware.financisto.utils.PinProtection;

import static ru.orangesoftware.financisto.service.DailyAutoBackupScheduler.scheduleNextAutoBackup;
import static ru.orangesoftware.financisto.service.FlowzrAutoSyncScheduler.scheduleNextAutoSync;

public class MenuListActivity extends ListActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_list);
        setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, menuItems()));
    }

    private String[] menuItems() {
        MenuListItem[] values = MenuListItem.values();
        String[] a = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            a[i] = getString(values[i].textResId);
        }
        return a;
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        MenuListItem.values()[position].call(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MenuListItem.ACTIVITY_CSV_EXPORT) {
            if (resultCode == RESULT_OK) {
                CsvExportOptions options = CsvExportOptions.fromIntent(data);
                MenuListItem.doCsvExport(this, options);
            }
        } else if (requestCode == MenuListItem.ACTIVITY_QIF_EXPORT) {
            if (resultCode == RESULT_OK) {
                QifExportOptions options = QifExportOptions.fromIntent(data);
                MenuListItem.doQifExport(this, options);
            }
        } else if (requestCode == MenuListItem.ACTIVITY_CSV_IMPORT) {
            if (resultCode == RESULT_OK) {
                CsvImportOptions options = CsvImportOptions.fromIntent(data);
                MenuListItem.doCsvImport(this, options);
            }
        } else if (requestCode == MenuListItem.ACTIVITY_QIF_IMPORT) {
            if (resultCode == RESULT_OK) {
                QifImportOptions options = QifImportOptions.fromIntent(data);
                MenuListItem.doQifImport(this, options);
            }
        } else if (requestCode == MenuListItem.ACTIVITY_CHANGE_PREFERENCES) {
            scheduleNextAutoBackup(this);
            scheduleNextAutoSync(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        PinProtection.lock(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        PinProtection.unlock(this);
    }

}
