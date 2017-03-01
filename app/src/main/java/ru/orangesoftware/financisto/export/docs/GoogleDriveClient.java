/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.export.docs;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.export.ImportExportException;
import ru.orangesoftware.financisto.utils.MyPreferences;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 11/9/11 2:19 AM
 */
public class GoogleDriveClient {

    public static Drive create(Context context, String googleDriveAccount) throws IOException, GoogleAuthException, ImportExportException {
        if (googleDriveAccount == null) {
            throw new ImportExportException(R.string.google_drive_account_required);
        }
        try {
            List<String> scope = new ArrayList<String>();
            scope.add(DriveScopes.DRIVE_FILE);
            if (MyPreferences.isGoogleDriveFullReadonly(context)) {
                scope.add(DriveScopes.DRIVE_READONLY);
            }
            GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(context, scope);
            credential.setSelectedAccountName(googleDriveAccount);
            credential.getToken();
            return new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential).build();
        } catch (UserRecoverableAuthException e) {
            NotificationManager notificationManager = (NotificationManager) context
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            Intent authorizationIntent = e.getIntent();
            authorizationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).addFlags(
                    Intent.FLAG_FROM_BACKGROUND);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                    authorizationIntent, 0);
            Notification notification = new NotificationCompat.Builder(context)
                    .setSmallIcon(android.R.drawable.ic_dialog_alert)
                    .setTicker(context.getString(R.string.google_drive_permission_requested))
                    .setContentTitle(context.getString(R.string.google_drive_permission_requested))
                    .setContentText(context.getString(R.string.google_drive_permission_requested_for_account, googleDriveAccount))
                    .setContentIntent(pendingIntent).setAutoCancel(true).build();
            notificationManager.notify(0, notification);
            throw new ImportExportException(R.string.google_drive_permission_required);
        }
    }

    public static String getOrCreateDriveFolder(Drive drive, String targetFolder) throws IOException {
        String folderId = null;
        FileList folders = drive.files().list().setQ("mimeType='application/vnd.google-apps.folder'").execute();
        for (com.google.api.services.drive.model.File f : folders.getItems()) {
            if (f.getTitle().equals(targetFolder)) {
                folderId = f.getId();
            }
        }
        //if not found create it
        if (folderId == null) {
            com.google.api.services.drive.model.File body = new com.google.api.services.drive.model.File();
            body.setTitle(targetFolder);
            body.setMimeType("application/vnd.google-apps.folder");
            com.google.api.services.drive.model.File file = drive.files().insert(body).execute();
            folderId = file.getId();
        }
        return folderId;
    }

}
