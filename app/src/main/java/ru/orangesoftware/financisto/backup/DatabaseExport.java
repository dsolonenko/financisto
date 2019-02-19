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

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import ru.orangesoftware.financisto.export.Export;
import ru.orangesoftware.financisto.utils.Utils;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import static ru.orangesoftware.financisto.backup.Backup.*;
import static ru.orangesoftware.financisto.db.DatabaseHelper.ACCOUNT_TABLE;
import static ru.orangesoftware.orb.EntityManager.DEF_SORT_COL;

public class DatabaseExport extends Export {

    private final Context context;
    private final SQLiteDatabase db;

    public DatabaseExport(Context context, SQLiteDatabase db, boolean useGZip) {
        super(context, useGZip);
        this.context = context;
        this.db = db;
    }

    @Override
    protected String getExtension() {
        return ".backup";
    }

    @Override
    protected void writeHeader(BufferedWriter bw) throws IOException, NameNotFoundException {
        PackageInfo pi = Utils.getPackageInfo(context);
        bw.write("PACKAGE:");
        bw.write(pi.packageName);
        bw.write("\n");
        bw.write("VERSION_CODE:");
        bw.write(String.valueOf(pi.versionCode));
        bw.write("\n");
        bw.write("VERSION_NAME:");
        bw.write(pi.versionName);
        bw.write("\n");
        bw.write("DATABASE_VERSION:");
        bw.write(String.valueOf(db.getVersion()));
        bw.write("\n");
        bw.write("#START\n");
    }

    public static void copy(File source, File dest) throws IOException {
        FileChannel in = null, out = null;
        try {
            in = new FileInputStream(source).getChannel();
            out = new FileOutputStream(dest).getChannel();

            long size = in.size();
            MappedByteBuffer buf = in.map(FileChannel.MapMode.READ_ONLY, 0, size);

            out.write(buf);

        } finally {
            if (in != null) in.close();
            if (out != null) out.close();
        }
    }

    @Override
    protected void writeBody(BufferedWriter bw) throws IOException {
        for (String tableName : BACKUP_TABLES) {
            exportTable(bw, tableName);
        }
    }

    @Override
    protected void writeFooter(BufferedWriter bw) throws IOException {
        bw.write("#END");
    }

    private void exportTable(BufferedWriter bw, String tableName) throws IOException {
        final boolean orderedTable = tableHasOrder(tableName);
        final boolean customOrdered = ACCOUNT_TABLE.equals(tableName);
        String sql = "select * from " + tableName 
                + (tableHasSystemIds(tableName) ? " WHERE _id > 0 " : " ") 
                + (orderedTable ? " order by " + DEF_SORT_COL + " asc" : "");
        long row = 0;
        try (Cursor c = db.rawQuery(sql, null)) {
            final String[] columnNames = c.getColumnNames();
            int cols = columnNames.length;
            while (c.moveToNext()) {
                bw.write("$ENTITY:");
                bw.write(tableName);
                bw.write("\n");
                for (int i = 0; i < cols; i++) {
                    final String colName = columnNames[i];
                    if (!DEF_SORT_COL.equals(colName) || customOrdered) {
                        final String value = c.getString(i);
                        if (value != null) {
                            bw.write(colName);
                            bw.write(":");
                            bw.write(removeNewLine(value));
                            bw.write("\n");
                        }
                    }
                }
                bw.write("$$\n");
            }
        }
    }

    private static String removeNewLine(String value) {
        return value.replace('\n', ' ');
    }

}
