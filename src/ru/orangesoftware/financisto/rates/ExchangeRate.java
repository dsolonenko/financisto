/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.rates;

import android.content.ContentValues;
import android.database.Cursor;
import ru.orangesoftware.financisto.db.DatabaseHelper;

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 1/17/12 11:25 PM
 */
public class ExchangeRate implements Comparable<ExchangeRate> {

    public static final ExchangeRate ONE = new ExchangeRate();
    public static final ExchangeRate NA = new ExchangeRate();

    static {
        ONE.rate = 1.0d;
        NA.error = "N/A";
    }

    public static ExchangeRate fromCursor(Cursor c) {
        ExchangeRate r = new ExchangeRate();
        r.fromCurrencyId = c.getLong(DatabaseHelper.ExchangeRateColumns.from_currency_id.ordinal());
        r.toCurrencyId = c.getLong(DatabaseHelper.ExchangeRateColumns.to_currency_id.ordinal());
        r.date = c.getLong(DatabaseHelper.ExchangeRateColumns.rate_date.ordinal());
        r.rate = c.getFloat(DatabaseHelper.ExchangeRateColumns.rate.ordinal());
        return r;
    }

    public ContentValues toValues() {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.ExchangeRateColumns.from_currency_id.name(), fromCurrencyId);
        values.put(DatabaseHelper.ExchangeRateColumns.to_currency_id.name(), toCurrencyId);
        values.put(DatabaseHelper.ExchangeRateColumns.rate_date.name(), date);
        values.put(DatabaseHelper.ExchangeRateColumns.rate.name(), rate);
        return values;
    }

    public long fromCurrencyId;
    public long toCurrencyId;
    public long date;
    public double rate;
    public String error;

    public ExchangeRate flip() {
        ExchangeRate r = new ExchangeRate();
        r.fromCurrencyId = toCurrencyId;
        r.toCurrencyId = fromCurrencyId;
        r.date = date;
        r.rate = rate == 0 ? 0 : 1.0d/rate;
        return r;
    }

    @Override
    public int compareTo(ExchangeRate that) {
        long d0 = this.date;
        long d1 = that.date;
        return d0 > d1 ? -1 : (d0 < d1 ? 1 : 0);
    }

    public boolean isOk() {
        return error == null;
    }

    public String getErrorMessage() {
        return error != null ? error : "";
    }

}
