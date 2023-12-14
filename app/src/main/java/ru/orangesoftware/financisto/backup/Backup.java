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
package ru.orangesoftware.financisto.backup;

import static ru.orangesoftware.financisto.db.DatabaseHelper.ACCOUNT_TABLE;
import static ru.orangesoftware.financisto.db.DatabaseHelper.ATTRIBUTES_TABLE;
import static ru.orangesoftware.financisto.db.DatabaseHelper.BUDGET_TABLE;
import static ru.orangesoftware.financisto.db.DatabaseHelper.CATEGORY_ATTRIBUTE_TABLE;
import static ru.orangesoftware.financisto.db.DatabaseHelper.CATEGORY_TABLE;
import static ru.orangesoftware.financisto.db.DatabaseHelper.CCARD_CLOSING_DATE_TABLE;
import static ru.orangesoftware.financisto.db.DatabaseHelper.CURRENCY_TABLE;
import static ru.orangesoftware.financisto.db.DatabaseHelper.EXCHANGE_RATES_TABLE;
import static ru.orangesoftware.financisto.db.DatabaseHelper.LOCATIONS_TABLE;
import static ru.orangesoftware.financisto.db.DatabaseHelper.PAYEE_TABLE;
import static ru.orangesoftware.financisto.db.DatabaseHelper.PROJECT_TABLE;
import static ru.orangesoftware.financisto.db.DatabaseHelper.SMS_TEMPLATES_TABLE;
import static ru.orangesoftware.financisto.db.DatabaseHelper.TRANSACTION_ATTRIBUTE_TABLE;
import static ru.orangesoftware.financisto.db.DatabaseHelper.TRANSACTION_TABLE;

import android.content.Context;

import androidx.documentfile.provider.DocumentFile;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import ru.orangesoftware.financisto.export.Export;

public final class Backup {

    public static final String[] BACKUP_TABLES = {
            ACCOUNT_TABLE, ATTRIBUTES_TABLE, CATEGORY_ATTRIBUTE_TABLE,
            TRANSACTION_ATTRIBUTE_TABLE, BUDGET_TABLE, CATEGORY_TABLE,
            CURRENCY_TABLE, LOCATIONS_TABLE, PROJECT_TABLE, TRANSACTION_TABLE,
            PAYEE_TABLE, CCARD_CLOSING_DATE_TABLE, SMS_TEMPLATES_TABLE,
            "split", /* todo: seems not used, found only in old 20110422_0051_create_split_table.sql, should be removed then */
            EXCHANGE_RATES_TABLE};

    public static final String[] BACKUP_TABLES_WITH_SYSTEM_IDS = {
            ATTRIBUTES_TABLE, CATEGORY_TABLE, PROJECT_TABLE, LOCATIONS_TABLE};

    public static final String[] BACKUP_TABLES_WITH_SORT_ORDER = {
            ACCOUNT_TABLE, SMS_TEMPLATES_TABLE, PROJECT_TABLE, PAYEE_TABLE, BUDGET_TABLE, CURRENCY_TABLE, LOCATIONS_TABLE, ATTRIBUTES_TABLE};

    public static final String[] RESTORE_SCRIPTS = {
            "20100114_1158_alter_accounts_types.sql",
            "20110903_0129_alter_template_splits.sql",
            "20171230_1852_alter_electronic_account_type.sql"
    };

    private Backup() {
    }

    public static DocumentFile[] listBackups(Context context) {
        DocumentFile backupPath = Export.getBackupFolder(context);
        List<DocumentFile> files = Arrays.stream(backupPath.listFiles()).filter(documentFile -> documentFile.getName().endsWith(".backup")).collect(Collectors.toList());
        if (files != null) {
            Collections.sort(files, (s1, s2) -> s2.getName().compareTo(s1.getName()));

            return files.toArray(new DocumentFile[0]);
        } else {
            return new DocumentFile[0];
        }
    }

    public static boolean tableHasSystemIds(String tableName) {
        for (String table : BACKUP_TABLES_WITH_SYSTEM_IDS) {
            if (table.equalsIgnoreCase(tableName)) {
                return true;
            }
        }
        return false;
    }

    public static boolean tableHasOrder(String tableName) {
        for (String table : BACKUP_TABLES_WITH_SORT_ORDER) {
            if (table.equalsIgnoreCase(tableName)) {
                return true;
            }
        }
        return false;
    }

}
