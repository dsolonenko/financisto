/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.export.csv;

import android.content.Intent;
import ru.orangesoftware.financisto.activity.CsvImportActivity;
import ru.orangesoftware.financisto.filter.WhereFilter;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.utils.CurrencyExportPreferences;

import java.text.SimpleDateFormat;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 7/10/11 7:01 PM
 */
public class CsvImportOptions {

    public static final String DEFAULT_DATE_FORMAT = "dd.MM.yyyy";

    public Currency currency;
    public SimpleDateFormat dateFormat;
    public char fieldSeparator;
    public WhereFilter filter;
    public long selectedAccountId;
    public String filename;
    public boolean useHeaderFromFile;

    public CsvImportOptions(Currency currency, String dateFormat, long selectedAccountId, WhereFilter filter, String filename, char fieldSeparator, boolean useHeaderFromFile) {
        this.currency = currency;
        this.dateFormat = new SimpleDateFormat(dateFormat);
        this.selectedAccountId = selectedAccountId;
        this.filter = filter;
        this.filename = filename;
        this.fieldSeparator = fieldSeparator;
        this.useHeaderFromFile = useHeaderFromFile;
    }

    public static CsvImportOptions fromIntent(Intent data) {
        WhereFilter filter = WhereFilter.fromIntent(data);
        Currency currency = CurrencyExportPreferences.fromIntent(data, "csv");
        char fieldSeparator = data.getCharExtra(CsvImportActivity.CSV_IMPORT_FIELD_SEPARATOR, ',');
        String dateFormat = data.getStringExtra(CsvImportActivity.CSV_IMPORT_DATE_FORMAT);
        long selectedAccountId = data.getLongExtra(CsvImportActivity.CSV_IMPORT_SELECTED_ACCOUNT_2, -1);
        String filename = data.getStringExtra(CsvImportActivity.CSV_IMPORT_FILENAME);
        boolean useHeaderFromFile = data.getBooleanExtra(CsvImportActivity.CSV_IMPORT_USE_HEADER_FROM_FILE, true);
        return new CsvImportOptions(currency, dateFormat, selectedAccountId, filter, filename, fieldSeparator, useHeaderFromFile);
    }

}
