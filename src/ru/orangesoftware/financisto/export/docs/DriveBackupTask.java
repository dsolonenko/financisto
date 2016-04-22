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
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.activity.MainActivity;
import ru.orangesoftware.financisto.backup.DatabaseExport;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.export.ImportExportAsyncTask;
import ru.orangesoftware.financisto.export.ImportExportException;
import ru.orangesoftware.financisto.utils.MyPreferences;

import java.io.IOException;

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
            String folder = MyPreferences.getBackupFolder(context);
            // check the backup folder registered on preferences
            if (folder == null || folder.equals("")) {
                throw new ImportExportException(R.string.gdocs_folder_not_configured);
            }
            String googleDriveAccount = MyPreferences.getGoogleDriveAccount(context);
            Drive drive = GoogleDriveClient.create(context, googleDriveAccount);
            return export.exportOnline(drive, folder);
        } catch (ImportExportException e) {
            throw e;
        } catch (GoogleAuthException e) {
            throw new ImportExportException(R.string.gdocs_connection_failed);
        } catch (IOException e) {
            throw new ImportExportException(R.string.gdocs_io_error);
        } catch (Exception e) {
            throw new ImportExportException(R.string.gdocs_service_error, e);
        }
    }

    @Override
    protected String getSuccessMessage(Object result) {
        return String.valueOf(result);
    }

}
