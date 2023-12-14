/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * <p/>
 * Contributors:
 * Denis Solonenko - initial API and implementation
 ******************************************************************************/
package ru.orangesoftware.financisto.export;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Environment;

import androidx.documentfile.provider.DocumentFile;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.GZIPOutputStream;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.activity.RequestPermission;
import ru.orangesoftware.financisto.app.DependenciesHolder;
import ru.orangesoftware.financisto.export.dropbox.Dropbox;
import ru.orangesoftware.financisto.persistance.BackupPreferences;

public abstract class Export {

    public static final File DEFAULT_EXPORT_PATH = Environment.getExternalStoragePublicDirectory("financisto");
    public static final String BACKUP_MIME_TYPE = "application/x-gzip";

    private final Context context;
    private final boolean useGzip;

    static final DependenciesHolder dependencies = new DependenciesHolder();

    protected Export(Context context, boolean useGzip) {
        this.context = context;
        this.useGzip = useGzip;
    }

    public String export() throws Exception {
        if (!RequestPermission.checkPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            throw new ImportExportException(R.string.request_permissions_storage_not_granted);
        }
        DocumentFile path = getBackupFolder(context);
        String fileName = generateFilename();
        DocumentFile file = path.createFile("*/*", fileName);
        OutputStream outputStream = context.getContentResolver().openOutputStream(file.getUri());//new FileOutputStream(file);
        try {
            if (useGzip) {
                export(new GZIPOutputStream(outputStream));
            } else {
                export(outputStream);
            }
        } finally {
            outputStream.flush();
            outputStream.close();
        }
        return fileName;
    }

    protected void export(OutputStream outputStream) throws Exception {
        generateBackup(outputStream);
    }

    public String generateFilename() {
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd'_'HHmmss'_'SSS");
        return df.format(new Date()) + getExtension();
    }

    public byte[] generateBackupBytes() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        OutputStream out = new BufferedOutputStream(new GZIPOutputStream(outputStream));
        generateBackup(out);
        return outputStream.toByteArray();
    }

    private void generateBackup(OutputStream outputStream) throws Exception {
        OutputStreamWriter osw = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
        try (BufferedWriter bw = new BufferedWriter(osw, 65536)) {
            writeHeader(bw);
            writeBody(bw);
            writeFooter(bw);
        }
    }

    protected abstract void writeHeader(BufferedWriter bw) throws IOException, NameNotFoundException;

    protected abstract void writeBody(BufferedWriter bw) throws IOException;

    protected abstract void writeFooter(BufferedWriter bw) throws IOException;

    protected abstract String getExtension();

    public static DocumentFile getBackupFolder(Context context) {
        Uri path;
        BackupPreferences backupPreferences = dependencies.getPreferencesStore().getBackupPreferencesRx().blockingFirst();
        path = backupPreferences.getFolder();
        DocumentFile file = DocumentFile.fromTreeUri(context, path);
        if (file.isDirectory() && file.canWrite()) {
            return file;
        }
        file = DocumentFile.fromFile(Export.DEFAULT_EXPORT_PATH);
        return file;
    }

    public static DocumentFile getBackupFile(Context context, String backupFileName) {
        DocumentFile path = getBackupFolder(context);
        return path.findFile(backupFileName);
    }

    public static void uploadBackupFileToDropbox(Context context, String backupFileName) throws Exception {
        DocumentFile file = getBackupFile(context, backupFileName);
        Dropbox dropbox = new Dropbox(context);
        dropbox.uploadFile(new File(file.getUri().toString()));
    }

//    public static void uploadBackupFileToGoogleDrive(Context context, String backupFileName) throws Exception {
//        File file = getBackupFile(context, backupFileName);
//        GoogleDriveClient driveClient = GoogleDriveClient_.getInstance_(context);
//        driveClient.uploadFile(file);
//    }

}
