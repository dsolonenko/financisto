/*
 * Copyright (c) 2015 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.backup;

import android.content.Context;

import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import ru.orangesoftware.financisto.db.AbstractDbTest;
import ru.orangesoftware.financisto.export.Export;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.AccountType;
import ru.orangesoftware.financisto.model.Attribute;
import ru.orangesoftware.financisto.model.ElectronicPaymentType;
import ru.orangesoftware.financisto.model.MyLocation;
import ru.orangesoftware.financisto.model.TransactionInfo;
import ru.orangesoftware.financisto.utils.FileUtils;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class LegacyDatabaseRestoreTest extends AbstractDbTest {

    @Test
    public void should_restore_database_from_legacy_financisto1_backup_file() throws Exception {
        //given
        String backupFileContent = "PACKAGE:ru.orangesoftware.financisto\n" +
                "VERSION_CODE:88\n" +
                "VERSION_NAME:1.6.6\n" +
                "DATABASE_VERSION:204\n" +
                "#START\n" +
                "$ENTITY:currency\n" +
                "_id:3\n" +
                "name:EUR\n" +
                "title:European Euro\n" +
                "symbol:e\n" +
                "is_default:0\n" +
                "decimals:2\n" +
                "symbol_format:RS\n" +
                "updated_on:1363016900623\n" +
                "$$\n" +
                "$ENTITY:category\n" +
                "_id:0\n" +
                "title:No category\n" +
                "left:-1\n" +
                "right:82\n" +
                "last_location_id:0\n" +
                "last_project_id:0\n" +
                "sort_order:0\n" +
                "type:0\n" +
                "updated_on:0\n" +
                "remote_key:crebits-aaa::agxzfmZsb3d6ci1ocmRyEAsSCENhdGVnb3J5GISZPgyiAQtjcmViaXRzLWFhYQ\n" +
                "$$\n" +
                "$ENTITY:account\n" +
                "_id:20\n" +
                "title:PayPal\n" +
                "creation_date:1399552438975\n" +
                "currency_id:3\n" +
                "total_amount:375\n" +
                "type:PAYPAL\n" +
                "sort_order:0\n" +
                "is_active:1\n" +
                "is_include_into_totals:1\n" +
                "last_category_id:0\n" +
                "last_account_id:0\n" +
                "total_limit:0\n" +
                "card_issuer:VISA\n" +
                "closing_day:0\n" +
                "payment_day:0\n" +
                "last_transaction_date:1399552438985\n" +
                "updated_on:1399552438975\n" +
                "$$\n" +
                "$ENTITY:transactions\n" +
                "_id:2430\n" +
                "from_account_id:20\n" +
                "to_account_id:0\n" +
                "category_id:-1\n" +
                "project_id:0\n" +
                "location_id:0\n" +
                "from_amount:-1710\n" +
                "to_amount:0\n" +
                "datetime:1311064737834\n" +
                "provider:network\n" +
                "accuracy:1525\n" +
                "latitude:1.34163\n" +
                "longitude:103.97\n" +
                "is_template:0\n" +
                "status:UR\n" +
                "is_ccard_payment:0\n" +
                "last_recurrence:1311251678604\n" +
                "payee_id:0\n" +
                "parent_id:0\n" +
                "updated_on:0\n" +
                "original_currency_id:0\n" +
                "original_from_amount:0\n" +
                "$$\n" +
                "#END";
        //when
        restoreDatabase(backupFileContent);
        //then
        Account account = getAccount();
        assertEquals(AccountType.ELECTRONIC.name(), account.type);
        assertEquals(ElectronicPaymentType.PAYPAL.name(), account.cardIssuer);
        //and
        TransactionInfo transaction = getTransaction(account);
        assertTrue(transaction.isSplitParent());
    }

    @Test
    public void should_restore_newer_backup_into_older_database_version_by_removing_unknown_columns() throws Exception {
        //given
        String backupFileContent = "PACKAGE:ru.orangesoftware.financisto\n" +
                "VERSION_CODE:888\n" +
                "VERSION_NAME:3.0.0\n" +
                "DATABASE_VERSION:300\n" +
                "#START\n" +
                "$ENTITY:currency\n" +
                "_id:3\n" +
                "new_column:EUR\n" +
                "is_active:1\n" +
                "name:EUR\n" +
                "title:European Euro\n" +
                "symbol:e\n" +
                "is_default:0\n" +
                "decimals:2\n" +
                "symbol_format:RS\n" +
                "updated_on:1363016900623\n" +
                "$$\n" +
                "$ENTITY:category\n" +
                "_id:0\n" +
                "new_column:EUR\n" +
                "title:No category\n" +
                "left:-1\n" +
                "right:82\n" +
                "last_location_id:0\n" +
                "last_project_id:0\n" +
                "sort_order:0\n" +
                "type:0\n" +
                "updated_on:0\n" +
                "remote_key:crebits-aaa::agxzfmZsb3d6ci1ocmRyEAsSCENhdGVnb3J5GISZPgyiAQtjcmViaXRzLWFhYQ\n" +
                "$$\n" +
                "$ENTITY:category\n" +
                "_id:1\n" +
                "new_column:EUR\n" +
                "title:No category\n" +
                "left:-1\n" +
                "right:82\n" +
                "last_location_id:0\n" +
                "last_project_id:0\n" +
                "sort_order:0\n" +
                "type:0\n" +
                "updated_on:0\n" +
                "$$\n" +
                "$ENTITY:account\n" +
                "_id:20\n" +
                "new_column:EUR\n" +
                "title:PayPal\n" +
                "creation_date:1399552438975\n" +
                "currency_id:3\n" +
                "total_amount:375\n" +
                "type:PAYPAL\n" +
                "sort_order:0\n" +
                "is_active:1\n" +
                "is_include_into_totals:1\n" +
                "last_category_id:0\n" +
                "last_account_id:0\n" +
                "total_limit:0\n" +
                "card_issuer:VISA\n" +
                "closing_day:0\n" +
                "payment_day:0\n" +
                "last_transaction_date:1399552438985\n" +
                "updated_on:1399552438975\n" +
                "$$\n" +
                "$ENTITY:transactions\n" +
                "_id:2430\n" +
                "new_column:EUR\n" +
                "from_account_id:20\n" +
                "to_account_id:0\n" +
                "category_id:-1\n" +
                "project_id:0\n" +
                "location_id:0\n" +
                "from_amount:-1710\n" +
                "to_amount:0\n" +
                "datetime:1311064737834\n" +
                "provider:network\n" +
                "accuracy:1525\n" +
                "latitude:1.34163\n" +
                "longitude:103.97\n" +
                "is_template:0\n" +
                "status:UR\n" +
                "is_ccard_payment:0\n" +
                "last_recurrence:1311251678604\n" +
                "payee_id:0\n" +
                "parent_id:0\n" +
                "updated_on:0\n" +
                "original_currency_id:0\n" +
                "original_from_amount:0\n" +
                "$$\n" +
                "#END";
        //when
        restoreDatabase(backupFileContent);
        //then
        Account account = getAccount();
        assertEquals(AccountType.ELECTRONIC.name(), account.type);
        assertEquals(ElectronicPaymentType.PAYPAL.name(), account.cardIssuer);
        //and
        TransactionInfo transaction = getTransaction(account);
        assertTrue(transaction.isSplitParent());
    }

    @Test
    public void should_restore_account_totals() throws Exception {
        // given
        String backupFileContent = FileUtils.testFileAsString("20180116_125426_694.backup");
        // when
        restoreDatabase(backupFileContent);
        // then
        Account account = getAccount();
        assertThat(account.totalAmount, is(375L));
    }

    @Test
    public void should_restore_titles_for_attributes_and_locations() throws Exception {
        // given
        String backupFileContent = FileUtils.testFileAsString("20180116_125426_694.backup");
        // when
        restoreDatabase(backupFileContent);
        // then
        Attribute attribute = db.getAttribute(1);
        assertNotNull(attribute);
        assertThat(attribute.title, is("Кол-во поездок"));
        // and
        List<MyLocation> locations = db.getAllLocationsList(false);
        assertThat(locations.size(), is(1));
        assertThat(locations.get(0).title, is("Starbucks"));
    }

    protected Account getAccount() {
        List<Account> accounts = db.getAllAccountsList();
        assertEquals(1, accounts.size());
        return accounts.get(0);
    }

    private TransactionInfo getTransaction(Account account) {
        List<TransactionInfo> transactions = db.getTransactionsForAccount(account.id);
        assertEquals(1, transactions.size());
        return transactions.get(0);
    }

    private void restoreDatabase(String fileContent) throws IOException {
        Context context = getContext();
        String fileName = createBackupFile(fileContent);
        try {
            DatabaseImport databaseImport = DatabaseImport.createFromFileBackup(context, db, fileName);
            databaseImport.importDatabase();
        } finally {
            deleteBackupFile(fileName);
        }
    }

    private String createBackupFile(String fileContent) throws IOException {
        String fileName = "backup_" + System.currentTimeMillis() + ".backup";
        FileOutputStream out = new FileOutputStream(new File(Export.getBackupFolder(getContext()), fileName));
        out.write(fileContent.getBytes());
        out.flush();
        out.close();
        return fileName;
    }

    private void deleteBackupFile(String fileName) {
        new File(Export.getBackupFolder(getContext()), fileName).delete();
    }

}
