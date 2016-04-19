/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.backup;

import android.content.Context;
import android.content.pm.PackageInfo;
import ru.orangesoftware.financisto.export.AbstractImportExportTest;
import ru.orangesoftware.financisto.export.Export;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.utils.Utils;

import java.io.*;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 1/4/12 8:42 PM
 */
public class DatabaseBackupTest extends AbstractImportExportTest {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        createFirstAccount();
    }

    public void test_should_restore_database_from_plain_text() throws Exception {
        String fileName = backupDatabase(false);
        assertHeader(fileName, false);
        restoreDatabase(fileName);
        assertAccounts();
    }

    public void test_should_restore_database_from_gzipped_text() throws Exception {
        String fileName = backupDatabase(true);
        assertHeader(fileName, true);
        restoreDatabase(fileName);
        assertAccounts();
    }

    private String backupDatabase(boolean useGzip) throws Exception {
        Context context = getContext();
        DatabaseExport databaseExport = new DatabaseExport(context, db.db(), useGzip);
        return databaseExport.export();
    }

    private void restoreDatabase(String fileName) throws IOException {
        Context context = getContext();
        DatabaseImport databaseImport = DatabaseImport.createFromFileBackup(context, db, fileName);
        databaseImport.importDatabase();
    }

    private void assertHeader(String fileName, boolean useGzip) throws Exception {
        BufferedReader br = createFileReader(fileName, useGzip);
        try {
            PackageInfo pi = Utils.getPackageInfo(getContext());
            assertEquals("PACKAGE:" + pi.packageName, br.readLine());
            assertEquals("VERSION_CODE:"+pi.versionCode, br.readLine());
            assertEquals("VERSION_NAME:"+pi.versionName, br.readLine());
            assertEquals("DATABASE_VERSION:"+db.db().getVersion(), br.readLine());
            assertEquals("#START", br.readLine());
        } finally {
            br.close();
        }

    }

    private BufferedReader createFileReader(String fileName, boolean useGzip) throws IOException {
        File backupPath = Export.getBackupFolder(getContext());
        File file = new File(backupPath, fileName);
        InputStream in = new FileInputStream(file);
        if (useGzip) {
            in = new GZIPInputStream(in);
        }
        InputStreamReader r = new InputStreamReader(in);
        return new BufferedReader(r);
    }

    private void assertAccounts() {
        List<Account> accounts = em.getAllAccountsList();
        assertEquals(1, accounts.size());
        assertEquals("My Cash Account", accounts.get(0).title);
        assertEquals("AAA BBB:CCC", accounts.get(0).note);
    }

}
