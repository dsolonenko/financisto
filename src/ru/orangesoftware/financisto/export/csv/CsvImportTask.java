/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.export.csv;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.activity.MainActivity;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.export.ImportExportAsyncTask;
import ru.orangesoftware.financisto.export.ImportExportAsyncTaskListener;
import ru.orangesoftware.financisto.export.ProgressListener;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 11/7/11 10:45 PM
 */
public class CsvImportTask extends ImportExportAsyncTask {

    private final CsvImportOptions options;
    private final Handler handler;

    public CsvImportTask(final MainActivity mainActivity, Handler handler, ProgressDialog dialog, CsvImportOptions options) {
        super(mainActivity, dialog);
        setListener(new ImportExportAsyncTaskListener() {
            public void onCompleted() {
                mainActivity.onTabChanged(mainActivity.getTabHost().getCurrentTabTag());
            }
        });
        this.options = options;
        this.handler = handler;
    }

    @Override
    protected Object work(Context context, DatabaseAdapter db, String... params) throws Exception {
        try {
            CsvImport csvimport = new CsvImport(db, options);
            csvimport.setProgressListener(new ProgressListener() {
                @Override
                public void onProgress(int percentage) {
                    publishProgress(String.valueOf(percentage));
                }
            });
            return csvimport.doImport();
        } catch (Exception e) {
            Log.e("Financisto", "Csv import error", e);
            String message = e.getMessage();
            if (message == null) {
                handler.sendEmptyMessage(R.string.csv_import_error);
            } else  if (message.equals("Import file not found"))
                handler.sendEmptyMessage(R.string.import_file_not_found);
            else if (message.equals("Unknown category in import line"))
                handler.sendEmptyMessage(R.string.import_unknown_category);
            else if (message.equals("Unknown project in import line"))
                handler.sendEmptyMessage(R.string.import_unknown_project);
            else if (message.equals("Wrong currency in import line"))
                handler.sendEmptyMessage(R.string.import_wrong_currency);
            else if (message.equals("IllegalArgumentException"))
                handler.sendEmptyMessage(R.string.import_illegal_argument_exception);
            else if (message.equals("ParseException"))
                handler.sendEmptyMessage(R.string.import_parse_error);
            else
                handler.sendEmptyMessage(R.string.csv_import_error);
            throw e;
        }

    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
        dialog.setMessage(context.getString(R.string.csv_import_inprogress_update, values[0]));
    }

    @Override
    protected String getSuccessMessage(Object result) {
        return String.valueOf(result);
    }

}
