/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.export.docs;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.api.services.drive.Drive;

import java.io.IOException;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.backup.DatabaseExport;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.export.ImportExportAsyncTask;
import ru.orangesoftware.financisto.export.ImportExportException;
import ru.orangesoftware.financisto.utils.MyPreferences;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 11/9/11 2:23 AM
 */
public class DriveBackupTask extends ImportExportAsyncTask {

    public DriveBackupTask(Activity mainActivity, ProgressDialog dialog) {
        super(mainActivity, dialog);
    }

    @Override
    protected Object work(Context context, DatabaseAdapter db, String... params) throws Exception {
        DatabaseExport export = new DatabaseExport(context, db.db(), true);
        try {
            Drive drive = createGoogleDriveClient(context);
            String folder = getFolderId(context, drive);
            return export.exportOnline(drive, folder);
        } catch (ImportExportException e) {
            throw e;
        } catch (GoogleAuthException e) {
            throw new ImportExportException(R.string.gdocs_connection_failed, e);
        } catch (IOException e) {
            throw new ImportExportException(R.string.gdocs_io_error, e);
        } catch (Exception e) {
            throw new ImportExportException(R.string.gdocs_service_error, e);
        }
    }

    public static Drive createGoogleDriveClient(Context context) throws Exception {
        String googleDriveAccount = MyPreferences.getGoogleDriveAccount(context);
        return GoogleDriveClient.create(context, googleDriveAccount);
    }

    public static String getFolderId(Context context, Drive drive) throws ImportExportException, IOException {
        // get drive first
        String folder = MyPreferences.getBackupFolder(context);
        // check the backup folder registered on preferences
        if (folder == null || folder.equals("")) {
            throw new ImportExportException(R.string.gdocs_folder_not_configured);
        }
        String folderId = GoogleDriveClient.getOrCreateDriveFolder(drive, folder);
        if (folderId == null) {
            throw new ImportExportException(R.string.gdocs_folder_not_found);
        }
        return folderId;
    }

    @Override
    protected String getSuccessMessage(Object result) {
        return String.valueOf(result);
    }

}
