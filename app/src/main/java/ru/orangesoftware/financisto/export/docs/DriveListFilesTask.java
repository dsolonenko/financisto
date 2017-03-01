/*
 * Copyright (c) 2014 Denis Solonenko.
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
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.activity.MenuListItem;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.export.Export;
import ru.orangesoftware.financisto.export.ImportExportAsyncTask;
import ru.orangesoftware.financisto.export.ImportExportAsyncTaskListener;
import ru.orangesoftware.financisto.export.ImportExportException;
import ru.orangesoftware.financisto.utils.MyPreferences;

/**
 * Created with IntelliJ IDEA.
 * User: dsolonenko
 * Date: 1/20/14
 * Time: 11:58 PM
 */
public class DriveListFilesTask extends ImportExportAsyncTask {

    public DriveListFilesTask(final Activity context, ProgressDialog dialog) {
        super(context, dialog);
        this.setShowResultDialog(false);
        setListener(new ImportExportAsyncTaskListener() {
            @Override
            public void onCompleted(Object result) {
                MenuListItem.doImportFromGoogleDrive(context, (File[]) result);
            }
        });
    }

    @Override
    protected Object work(Context context, DatabaseAdapter db, String... params) throws Exception {
        try {
            String googleDriveAccount = MyPreferences.getGoogleDriveAccount(context);
            Drive drive = GoogleDriveClient.create(context,googleDriveAccount);
            String targetFolder = MyPreferences.getBackupFolder(context);

            if (targetFolder == null || targetFolder.equals("")) {
                throw new ImportExportException(R.string.gdocs_folder_not_configured);
            }

            String folderId = GoogleDriveClient.getOrCreateDriveFolder(drive, targetFolder);

            List<File> backupFiles = new ArrayList<File>();
            FileList files = drive.files().list().setQ("mimeType='" + Export.BACKUP_MIME_TYPE + "' and '" + folderId + "' in parents").execute();
            for (com.google.api.services.drive.model.File f : files.getItems()) {
                if ((f.getExplicitlyTrashed() == null || !f.getExplicitlyTrashed()) && f.getDownloadUrl() != null && f.getDownloadUrl().length() > 0) {
                    if (f.getFileExtension().equals("backup")) {
                        backupFiles.add(f);
                    }
                }
            }
            return backupFiles.toArray(new File[backupFiles.size()]);

        } catch (GoogleAuthException e) {
            throw new ImportExportException(R.string.gdocs_connection_failed);
        } catch (IOException e) {
            throw new ImportExportException(R.string.gdocs_io_error);
        } catch (Exception e) {
            throw new ImportExportException(R.string.gdocs_service_error);
        }
    }

    @Override
    protected String getSuccessMessage(Object result) {
        return null;
    }

}
