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
package ru.orangesoftware.financisto.backup;

import android.content.ContentValues;
import android.content.Context;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.services.drive.Drive;
import ru.orangesoftware.financisto.db.Database;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.DatabaseSchemaEvolution;
import ru.orangesoftware.financisto.export.Export;
import ru.orangesoftware.financisto.export.dropbox.Dropbox;

import java.io.*;
import java.util.zip.GZIPInputStream;

import static ru.orangesoftware.financisto.backup.Backup.RESTORE_SCRIPTS;

public class DatabaseImport extends FullDatabaseImport {

	private final DatabaseSchemaEvolution schemaEvolution;
    private final InputStream backupStream;

    public static DatabaseImport createFromFileBackup(Context context, DatabaseAdapter dbAdapter, String backupFile) throws FileNotFoundException {
        File backupPath = Export.getBackupFolder(context);
        File file = new File(backupPath, backupFile);
        FileInputStream inputStream = new FileInputStream(file);
        return new DatabaseImport(context, dbAdapter, inputStream);
    }

    public static DatabaseImport createFromGoogleDriveBackup(Context context, DatabaseAdapter dbAdapter, Drive drive, com.google.api.services.drive.model.File file)
            throws IOException {
        HttpResponse response = drive.getRequestFactory().buildGetRequest(new GenericUrl(file.getDownloadUrl())).execute();
        InputStream inputStream = response.getContent();
        InputStream in = new GZIPInputStream(inputStream);
        return new DatabaseImport(context, dbAdapter, in);
    }

    public static DatabaseImport createFromDropboxBackup(Context context, DatabaseAdapter dbAdapter, Dropbox dropbox, String backupFile)
            throws Exception {
        InputStream inputStream = dropbox.getFileAsStream(backupFile);
        InputStream in = new GZIPInputStream(inputStream);
        return new DatabaseImport(context, dbAdapter, in);
    }

    private DatabaseImport(Context context, DatabaseAdapter dbAdapter, InputStream backupStream) {
        super(context, dbAdapter);
        this.schemaEvolution = new DatabaseSchemaEvolution(context, Database.DATABASE_NAME, null, Database.DATABASE_VERSION);
        this.backupStream = backupStream;
	}

    @Override
    protected void restoreDatabase() throws IOException {
        InputStream s = decompressStream(backupStream);
        InputStreamReader isr = new InputStreamReader(s, "UTF-8");
        BufferedReader br = new BufferedReader(isr, 65535);
        try {
            recoverDatabase(br);
            runRestoreAlterscripts();
        } finally {
            br.close();
        }
    }

    private InputStream decompressStream(InputStream input) throws IOException {
        PushbackInputStream pb = new PushbackInputStream(input, 2);
        byte[] bytes = new byte[2];
        pb.read(bytes);
        pb.unread(bytes);
        int head = ((int) bytes[0] & 0xff) | ((bytes[1] << 8) & 0xff00);
        if (GZIPInputStream.GZIP_MAGIC == head)
            return new GZIPInputStream(pb);
        else
            return pb;
    }

    private void recoverDatabase(BufferedReader br) throws IOException {
        boolean insideEntity = false;
        ContentValues values = new ContentValues();
        String line;
        String tableName = null;
        while ((line = br.readLine()) != null) {
            if (line.startsWith("$")) {
                if ("$$".equals(line)) {
                    if (tableName != null && values.size() > 0) {
                        db.insert(tableName, null, values);
                        tableName = null;
                        insideEntity = false;
                    }
                } else {
                    int i = line.indexOf(":");
                    if (i > 0) {
                        tableName = line.substring(i+1);
                        insideEntity = true;
                        values.clear();
                    }
                }
            } else {
                if (insideEntity) {
                    int i = line.indexOf(":");
                    if (i > 0) {
                        String columnName = line.substring(0, i);
                        String value = line.substring(i+1);
                        values.put(columnName, value);
                    }
                }
            }
        }
	}

	private void runRestoreAlterscripts() throws IOException {
		for (String script : RESTORE_SCRIPTS) {
			schemaEvolution.runAlterScript(db, script);
		}
	}

}
