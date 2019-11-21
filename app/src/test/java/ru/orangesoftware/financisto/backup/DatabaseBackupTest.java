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

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import ru.orangesoftware.financisto.export.AbstractImportExportTest;
import ru.orangesoftware.financisto.export.Export;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.test.CategoryBuilder;
import ru.orangesoftware.financisto.test.DateTime;
import ru.orangesoftware.financisto.test.TransactionBuilder;
import ru.orangesoftware.financisto.utils.Utils;

import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.*;

public class DatabaseBackupTest extends AbstractImportExportTest {

    Account a1;
    private Map<String, Category> categoriesMap;

    public void setUp() throws Exception {
        super.setUp();
        a1 = createFirstAccount();
        categoriesMap = CategoryBuilder.createDefaultHierarchy(db);
        TransactionBuilder.withDb(db).dateTime(DateTime.date(2011, 8, 3).at(22, 34, 55, 10))
                .account(a1).amount(-123456).category(categoriesMap.get("AA1")).payee("P1").location("Home").project("P1").note("My note").create();
    }

    @Test
    public void should_backup_and_restore_total_amount_for_accounts() throws Exception {
        // given
        String backupFile = backupDatabase(false);
        String backupContent = fileAsString(backupFile);
        long expectedTotalAmount = db.getAccount(a1.id).totalAmount;
        assertThat(backupContent, containsString("total_amount:" + expectedTotalAmount));
        // when
        restoreDatabase(backupFile);
        // then
        List<Account> accounts = db.getAllAccountsList();
        assertEquals(1, accounts.size());
        assertEquals(expectedTotalAmount, accounts.get(0).totalAmount);
    }

    @Test
    public void should_restore_database_from_plain_text() throws Exception {
        String fileName = backupDatabase(false);
        assertHeader(fileName, false);
        restoreDatabase(fileName);
        assertAccounts();
    }

    @Test
    public void should_restore_database_from_gzipped_text() throws Exception {
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
        try (BufferedReader br = createFileReader(fileName, useGzip)) {
            PackageInfo pi = Utils.getPackageInfo(getContext());
            assertEquals("PACKAGE:" + pi.packageName, br.readLine());
            assertEquals("VERSION_CODE:" + pi.versionCode, br.readLine());
            assertEquals("VERSION_NAME:" + pi.versionName, br.readLine());
            assertEquals("DATABASE_VERSION:" + db.db().getVersion(), br.readLine());
            assertEquals("#START", br.readLine());
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
        List<Account> accounts = db.getAllAccountsList();
        assertEquals(1, accounts.size());
        assertEquals("My Cash Account", accounts.get(0).title);
        assertEquals("AAA BBB:CCC", accounts.get(0).note);
    }

    private String fileAsString(String backupFile) throws IOException {
        File backupPath = Export.getBackupFolder(context);
        File file = new File(backupPath, backupFile);
        return FileUtils.readFileToString(file, "UTF-8");
    }

}
