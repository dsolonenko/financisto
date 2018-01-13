/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.db;

import android.database.Cursor;
import android.util.Log;
import ru.orangesoftware.financisto.filter.WhereFilter;
import ru.orangesoftware.financisto.filter.Criteria;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.Total;
import ru.orangesoftware.financisto.model.TotalError;
import ru.orangesoftware.financisto.model.TransactionInfo;
import ru.orangesoftware.financisto.rates.ExchangeRate;
import ru.orangesoftware.financisto.rates.ExchangeRateProvider;
import ru.orangesoftware.financisto.utils.CurrencyCache;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static ru.orangesoftware.financisto.db.DatabaseAdapter.enhanceFilterForAccountBlotter;
import static ru.orangesoftware.financisto.db.DatabaseHelper.V_BLOTTER_FOR_ACCOUNT_WITH_SPLITS;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 8/1/11 11:54 PM
 */
public class TransactionsTotalCalculator {

    public static final String[] BALANCE_PROJECTION = {
        "from_account_currency_id",
        "SUM(from_amount)"};

    public static final String BALANCE_GROUPBY = "from_account_currency_id";

    public static final String[] HOME_CURRENCY_PROJECTION = {
            "datetime",
            "from_account_currency_id",
            "from_amount",
            "to_account_currency_id",
            "to_amount",
            "original_currency_id",
            "original_from_amount"
    };

    private final DatabaseAdapter db;
    private final WhereFilter filter;

    public TransactionsTotalCalculator(DatabaseAdapter db, WhereFilter filter) {
        this.db = db;
        this.filter = filter;
    }

    public Total[] getTransactionsBalance() {
        WhereFilter filter = this.filter;
        if (filter.getAccountId() == -1) {
            filter = excludeAccountsNotIncludedInTotalsAndSplits(filter);
        }
        try (Cursor c = db.db().query(V_BLOTTER_FOR_ACCOUNT_WITH_SPLITS, BALANCE_PROJECTION,
                filter.getSelection(), filter.getSelectionArgs(),
                BALANCE_GROUPBY, null, null)) {
            int count = c.getCount();
            List<Total> totals = new ArrayList<Total>(count);
            while (c.moveToNext()) {
                long currencyId = c.getLong(0);
                long balance = c.getLong(1);
                Currency currency = CurrencyCache.getCurrency(db, currencyId);
                Total total = new Total(currency);
                total.balance = balance;
                totals.add(total);
            }
            return totals.toArray(new Total[totals.size()]);
        }
    }

    public Total getAccountTotal() {
        Total[] totals = getTransactionsBalance();
        return totals.length > 0 ? totals[0] : Total.ZERO;
    }

    public Total getBlotterBalanceInHomeCurrency() {
        Currency homeCurrency = db.getHomeCurrency();
        return getBlotterBalance(homeCurrency);
    }

    public Total getBlotterBalance(Currency toCurrency) {
        WhereFilter filter = excludeAccountsNotIncludedInTotalsAndSplits(this.filter);
        return getBalanceInHomeCurrency(V_BLOTTER_FOR_ACCOUNT_WITH_SPLITS, toCurrency, filter);
    }

    public Total getAccountBalance(Currency toCurrency, long accountId) {
        WhereFilter filter = selectedAccountOnly(this.filter, accountId);
        return getBalanceInHomeCurrency(V_BLOTTER_FOR_ACCOUNT_WITH_SPLITS, toCurrency, filter);
    }

    private WhereFilter selectedAccountOnly(WhereFilter filter, long accountId) {
        WhereFilter copy = enhanceFilterForAccountBlotter(filter);
        copy.put(Criteria.eq("from_account_id", String.valueOf(accountId)));
        return copy;
    }

    private Total getBalanceInHomeCurrency(String view, Currency toCurrency, WhereFilter filter) {
        Log.d("Financisto", "Query balance: "+filter.getSelection()+" => "+ Arrays.toString(filter.getSelectionArgs()));
        Cursor c = db.db().query(view, HOME_CURRENCY_PROJECTION,
                filter.getSelection(), filter.getSelectionArgs(),
                null, null, null);
        try {
            try {
                long balance = calculateTotalFromCursor(db, c, toCurrency);
                Total total = new Total(toCurrency);
                total.balance = balance;
                return total;
            } catch (UnableToCalculateRateException e) {
                return new Total(e.toCurrency, TotalError.atDateRateError(e.fromCurrency, e.datetime));
            }
        } finally {
            c.close();
        }
    }

    private static long calculateTotalFromCursor(DatabaseAdapter db, Cursor c, Currency toCurrency) throws UnableToCalculateRateException {
        ExchangeRateProvider rates = db.getHistoryRates();
        BigDecimal balance = BigDecimal.ZERO;
        while (c.moveToNext()) {
            balance = balance.add(getAmountFromCursor(db, c, toCurrency, rates, 0));
        }
        return balance.longValue();
    }

    public static Total calculateTotalFromListInHomeCurrency(DatabaseAdapter db, List<TransactionInfo> list) {
        try {
            Currency toCurrency = db.getHomeCurrency();
            long[] balance = calculateTotalFromList(db, list, toCurrency);
            return Total.asIncomeExpense(toCurrency, balance[0], balance[1]);
        } catch (UnableToCalculateRateException e) {
            return new Total(e.toCurrency, TotalError.atDateRateError(e.fromCurrency, e.datetime));
        }
    }

    public static long[] calculateTotalFromList(DatabaseAdapter db, List<TransactionInfo> list, Currency toCurrency) throws UnableToCalculateRateException {
        ExchangeRateProvider rates = db.getHistoryRates();
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expenses = BigDecimal.ZERO;
        for (TransactionInfo t : list) {
            BigDecimal amount = getAmountFromTransaction(db, t, toCurrency, rates);
            if (amount.signum() > 0) {
                income = income.add(amount);
            } else {
                expenses = expenses.add(amount);
            }
        }
        return new long[]{income.longValue(),expenses.longValue()};
    }

    public static BigDecimal getAmountFromCursor(MyEntityManager em, Cursor c, Currency toCurrency, ExchangeRateProvider rates, int index) throws UnableToCalculateRateException {
        long datetime = c.getLong(index++);
        long fromCurrencyId = c.getLong(index++);
        long fromAmount = c.getLong(index++);
        long toCurrencyId = c.getLong(index++);
        long toAmount = c.getLong(index++);
        long originalCurrencyId = c.getLong(index++);
        long originalAmount = c.getLong(index);
        return getConvertedAmount(em, toCurrency, rates, datetime, fromCurrencyId, fromAmount, toCurrencyId, toAmount, originalCurrencyId, originalAmount);
    }

    public static BigDecimal getAmountFromTransaction(MyEntityManager em, TransactionInfo ti, Currency toCurrency, ExchangeRateProvider rates)
            throws UnableToCalculateRateException {
        long datetime = ti.dateTime;
        long fromCurrencyId = ti.fromAccount.currency.id;
        long fromAmount = ti.fromAmount;
        long toCurrencyId = ti.toAccount != null ? ti.toAccount.currency.id : 0;
        long toAmount = ti.toAmount;
        long originalCurrencyId = ti.originalCurrency != null ? ti.originalCurrency.id : 0;
        long originalAmount = ti.originalFromAmount;
        return getConvertedAmount(em, toCurrency, rates, datetime, fromCurrencyId, fromAmount, toCurrencyId, toAmount, originalCurrencyId, originalAmount);
    }

    private static BigDecimal getConvertedAmount(MyEntityManager em, Currency toCurrency, ExchangeRateProvider rates, long datetime,
                                                 long fromCurrencyId, long fromAmount,
                                                 long toCurrencyId, long toAmount,
                                                 long originalCurrencyId, long originalAmount) throws UnableToCalculateRateException {
        if (fromCurrencyId == toCurrency.id) {
            return BigDecimal.valueOf(fromAmount);
        } else if (toCurrencyId > 0 && toCurrencyId == toCurrency.id) {
            return BigDecimal.valueOf(-toAmount);
        } else if (originalCurrencyId > 0 && originalCurrencyId == toCurrency.id) {
            return BigDecimal.valueOf(originalAmount);
        } else {
            Currency fromCurrency = CurrencyCache.getCurrency(em, fromCurrencyId);
            ExchangeRate exchangeRate = rates.getRate(fromCurrency, toCurrency, datetime);
            if (exchangeRate == ExchangeRate.NA) {
                throw new UnableToCalculateRateException(fromCurrency, toCurrency, datetime);
            } else {
                double rate = exchangeRate.rate;
                return BigDecimal.valueOf(fromAmount).multiply(BigDecimal.valueOf(rate));
            }
        }
    }

    private WhereFilter excludeAccountsNotIncludedInTotalsAndSplits(WhereFilter filter) {
        WhereFilter copy = WhereFilter.copyOf(filter);
        copy.eq("from_account_is_include_into_totals", "1");
        copy.neq("category_id", "-1");
        return copy;
    }

}
