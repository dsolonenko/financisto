/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.export.docs;

import java.io.IOException;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.activity.MainActivity;
import ru.orangesoftware.financisto.backup.DatabaseImport;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.export.ImportExportAsyncTask;
import ru.orangesoftware.financisto.export.ImportExportAsyncTaskListener;
import ru.orangesoftware.financisto.export.ImportExportException;
import ru.orangesoftware.financisto.utils.MyPreferences;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 11/9/11 2:16 AM
 */
public class DriveRestoreTask extends ImportExportAsyncTask {

    private final com.google.api.services.drive.model.File entry;

    public DriveRestoreTask(final Activity mainActivity, ProgressDialog dialog, File entry) {
        super(mainActivity, dialog);
        this.entry = entry;
    }

    @Override
    protected Object work(Context context, DatabaseAdapter db, String... params) throws Exception {
        try {
            String googleDriveAccount = MyPreferences.getGoogleDriveAccount(context);        	
            Drive drive = GoogleDriveClient.create(context,googleDriveAccount);
            DatabaseImport.createFromGoogleDriveBackup(context, db, drive, entry).importDatabase();
        } catch (GoogleAuthException e) {
            throw new ImportExportException(R.string.gdocs_connection_failed);
        } catch (IOException e) {
            throw new ImportExportException(R.string.gdocs_io_error);
        } catch (Exception e) {
            throw new ImportExportException(R.string.gdocs_service_error, e);
        }
        return true;
    }

    @Override
    protected String getSuccessMessage(Object result) {
        return context.getString(R.string.restore_database_success);
    }

}
