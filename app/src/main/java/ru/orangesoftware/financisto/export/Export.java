/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * <p/>
 * Contributors:
 * Denis Solonenko - initial API and implementation
 ******************************************************************************/
package ru.orangesoftware.financisto.export;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Environment;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.GZIPOutputStream;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.activity.RequestPermission;
import ru.orangesoftware.financisto.export.drive.GoogleDriveClient;
import ru.orangesoftware.financisto.export.drive.GoogleDriveClient_;
import ru.orangesoftware.financisto.export.dropbox.Dropbox;
import ru.orangesoftware.financisto.utils.MyPreferences;

public abstract class Export {

    public static final File DEFAULT_EXPORT_PATH = Environment.getExternalStoragePublicDirectory("financisto");
    public static final String BACKUP_MIME_TYPE = "application/x-gzip";

    private final Context context;
    private final boolean useGzip;

    protected Export(Context context, boolean useGzip) {
        this.context = context;
        this.useGzip = useGzip;
    }

    public String export() throws Exception {
        if (!RequestPermission.checkPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            throw new ImportExportException(R.string.request_permissions_storage_not_granted);
        }
        File path = getBackupFolder(context);
        String fileName = generateFilename();
        File file = new File(path, fileName);
        FileOutputStream outputStream = new FileOutputStream(file);
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
        OutputStreamWriter osw = new OutputStreamWriter(outputStream, "UTF-8");
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

    public static File getBackupFolder(Context context) {
        String path = MyPreferences.getDatabaseBackupFolder(context);
        File file = new File(path);
        file.mkdirs();
        if (file.isDirectory() && file.canWrite()) {
            return file;
        }
        file = Export.DEFAULT_EXPORT_PATH;
        file.mkdirs();
        return file;
    }

    public static File getBackupFile(Context context, String backupFileName) {
        File path = getBackupFolder(context);
        return new File(path, backupFileName);
    }

    public static void uploadBackupFileToDropbox(Context context, String backupFileName) throws Exception {
        File file = getBackupFile(context, backupFileName);
        Dropbox dropbox = new Dropbox(context);
        dropbox.uploadFile(file);
    }

    public static void uploadBackupFileToGoogleDrive(Context context, String backupFileName) throws Exception {
        File file = getBackupFile(context, backupFileName);
        GoogleDriveClient driveClient = GoogleDriveClient_.getInstance_(context);
        driveClient.uploadFile(file);
    }

}
