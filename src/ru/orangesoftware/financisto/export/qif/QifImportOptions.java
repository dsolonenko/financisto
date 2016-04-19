/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.export.qif;

import android.content.Intent;
import ru.orangesoftware.financisto.activity.QifImportActivity;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.utils.CurrencyCache;

import static ru.orangesoftware.financisto.export.qif.QifDateFormat.*;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 7/10/11 7:01 PM
 */
public class QifImportOptions {

    public final QifDateFormat dateFormat;
    public final String filename;
    public final Currency currency;

    public QifImportOptions(String filename, QifDateFormat dateFormat, Currency currency) {
        this.filename = filename;
        this.dateFormat = dateFormat;
        this.currency = currency;
    }

    public static QifImportOptions fromIntent(Intent data) {
        String filename = data.getStringExtra(QifImportActivity.QIF_IMPORT_FILENAME);
        int f = data.getIntExtra(QifImportActivity.QIF_IMPORT_DATE_FORMAT, 0);
        long currencyId = data.getLongExtra(QifImportActivity.QIF_IMPORT_CURRENCY, 1);
        Currency currency = CurrencyCache.getCurrencyOrEmpty(currencyId);
        return new QifImportOptions(filename, f == 0 ? EU_FORMAT : US_FORMAT, currency);
    }

}
