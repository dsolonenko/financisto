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
import android.database.Cursor;
import android.util.Log;

import com.dropbox.core.util.IOUtil;
import com.google.android.gms.drive.DriveContents;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import ru.orangesoftware.financisto.db.Database;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.DatabaseSchemaEvolution;
import ru.orangesoftware.financisto.export.Export;
import ru.orangesoftware.financisto.export.dropbox.Dropbox;

import static ru.orangesoftware.financisto.backup.Backup.RESTORE_SCRIPTS;
import static ru.orangesoftware.financisto.backup.Backup.tableHasOrder;
import static ru.orangesoftware.financisto.db.DatabaseHelper.ATTRIBUTES_TABLE;
import static ru.orangesoftware.financisto.db.DatabaseHelper.LOCATIONS_TABLE;
import static ru.orangesoftware.orb.EntityManager.DEF_SORT_COL;

public class DatabaseImport extends FullDatabaseImport {

    private final DatabaseSchemaEvolution schemaEvolution;
    private final InputStream backupStream;

    public static DatabaseImport createFromFileBackup(Context context, DatabaseAdapter dbAdapter, String backupFile) throws FileNotFoundException {
        File backupPath = Export.getBackupFolder(context);
        File file = new File(backupPath, backupFile);
        FileInputStream inputStream = new FileInputStream(file);
        return new DatabaseImport(context, dbAdapter, inputStream);
    }

    public static DatabaseImport createFromGoogleDriveBackup(Context context, DatabaseAdapter db, DriveContents driveFileContents)
            throws IOException {
        InputStream inputStream = driveFileContents.getInputStream();
        InputStream in = new GZIPInputStream(inputStream);
        return new DatabaseImport(context, db, in);
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
            IOUtil.closeInput(br);
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
        long rowNum = 0;
        while ((line = br.readLine()) != null) {
            if (line.startsWith("$")) {
                if ("$$".equals(line)) {
                    if (tableName != null && values.size() > 0) {
                        if (shouldRestoreTable(tableName)) {
                            cleanupValues(tableName, values);
                            if (values.size() > 0) {
                                // if old dump format - then just adding sequential default order
                                if (tableHasOrder(tableName) && !values.containsKey(DEF_SORT_COL)) {
                                    values.put(DEF_SORT_COL, ++rowNum);
                                }
                                db.insert(tableName, null, values);
                            }
                        }
                        tableName = null;
                        insideEntity = false;

                    }
                } else {
                    int i = line.indexOf(":");
                    if (i > 0) {
                        tableName = line.substring(i + 1);
                        insideEntity = true;
                        values.clear();
                    }
                }
            } else {
                if (insideEntity) {
                    int i = line.indexOf(":");
                    if (i > 0) {
                        String columnName = line.substring(0, i);
                        String value = line.substring(i + 1);
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

    private boolean shouldRestoreTable(String tableName) {
        return true;
    }

    private void cleanupValues(String tableName, ContentValues values) {
        // remove system entities
        Integer id = values.getAsInteger("_id");
        if (id != null && id <= 0) {
            Log.w("Financisto", "Removing system entity: " + values);
            values.clear();
            return;
        }
        // fix columns
        values.remove("updated_on");
        values.remove("remote_key");
        if (LOCATIONS_TABLE.equals(tableName)) {
            if (values.containsKey("name")) {
                values.put("title", values.getAsString("name"));
            }
        } else if (ATTRIBUTES_TABLE.equals(tableName)) {
            if (values.containsKey("name")) {
                values.put("title", values.getAsString("name"));
                values.remove("name");
            }
        }
        // remove unknown columns
        String sql = "select * from " + tableName + " WHERE 1=0";
        try (Cursor c = db.rawQuery(sql, null)) {
            final String[] columnNames = c.getColumnNames();
            removeUnknownColumns(values, columnNames, tableName);
        }

        /*
        if ("account".equals(tableName)) {
            values.remove("sort_order");
            String type = values.getAsString("type");
            if ("PAYPAL".equals(type)) {
                values.put("type", AccountType.ELECTRONIC.name());
                values.put("card_issuer", ElectronicPaymentType.PAYPAL.name());
            }
        }
        */
    }

    private void removeUnknownColumns(ContentValues values, String[] columnNames, String tableName) {
        Set<String> possibleKeys = new HashSet<>(Arrays.asList(columnNames));
        Set<String> keys = new HashSet<>(values.keySet());
        for (String key : keys) {
            if (!possibleKeys.contains(key)) {
                values.remove(key);
                Log.i("Financisto", "Removing "+key+" from backup values for "+tableName);
            }
        }
    }

}
