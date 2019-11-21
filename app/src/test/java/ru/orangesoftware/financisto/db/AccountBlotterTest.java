/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.db;

import android.database.Cursor;

import org.junit.Test;

import java.util.Map;

import ru.orangesoftware.financisto.blotter.BlotterFilter;
import ru.orangesoftware.financisto.filter.Criteria;
import ru.orangesoftware.financisto.filter.WhereFilter;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.Total;
import ru.orangesoftware.financisto.test.AccountBuilder;
import ru.orangesoftware.financisto.test.CategoryBuilder;
import ru.orangesoftware.financisto.test.DateTime;
import ru.orangesoftware.financisto.test.TransactionBuilder;
import ru.orangesoftware.financisto.test.TransferBuilder;

import static org.junit.Assert.*;
import static ru.orangesoftware.financisto.db.DatabaseAdapter.enhanceFilterForAccountBlotter;
import static ru.orangesoftware.financisto.test.DateTime.date;

public class AccountBlotterTest extends AbstractDbTest {

    Account a1;
    Account a2;
    Map<String, Category> categoriesMap;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        a1 = AccountBuilder.createDefault(db);
        a2 = AccountBuilder.createDefault(db);
        categoriesMap = CategoryBuilder.createDefaultHierarchy(db);
    }

    @Test
    public void should_include_transfer_splits_into_blotter_for_account() {
        // regular transactions and transfers
        TransactionBuilder.withDb(db).dateTime(date(2012, 2, 8)).account(a1).amount(1000).create();
        TransactionBuilder.withDb(db).dateTime(date(2012, 2, 8)).account(a2).amount(200).create();
        assertAccountBlotter(a1, 1000);
        assertAccountBlotter(a2, 200);
        assertAccountBlotterTotal(a1, date(2012, 2, 1), date(2012, 2, 7), 0);
        assertAccountBlotterTotal(a1, date(2012, 2, 1), date(2012, 2, 8), 1000);
        assertAccountBlotterTotal(a2, date(2012, 2, 8), date(2012, 2, 9), 200);

        // regular transfer
        TransferBuilder.withDb(db).dateTime(date(2012, 2, 9)).fromAccount(a1).fromAmount(-100).toAccount(a2).toAmount(50).create();
        assertAccountBlotter(a1, -100, 1000);
        assertAccountBlotter(a2, 50, 200);
        assertAccountBlotterTotal(a1, date(2012, 2, 1), date(2012, 2, 9), 900);
        assertAccountBlotterTotal(a2, date(2012, 2, 8), date(2012, 2, 9), 250);

        // regular split
        TransactionBuilder.withDb(db).dateTime(date(2012, 2, 10)).account(a1).amount(-500)
                .withSplit(categoriesMap.get("A1"), -200)
                .withSplit(categoriesMap.get("A1"), -300)
                .create();
        assertAccountBlotter(a1, -500, -100, 1000);
        assertAccountBlotter(a2, 50, 200);
        assertAccountBlotterTotal(a1, date(2012, 2, 1), date(2012, 2, 10), 400);
        assertAccountBlotterTotal(a1, date(2012, 2, 9), date(2012, 2, 10), -600);
        assertAccountBlotterTotal(a2, date(2012, 2, 1), date(2012, 2, 9), 250);

        // transfer split
        TransactionBuilder.withDb(db).dateTime(date(2012, 2, 11)).account(a2).amount(-120)
                .withSplit(categoriesMap.get("B"), -20)
                .withTransferSplit(a1, -100, 200)
                .create();
        assertAccountBlotter(a1, 200, -500, -100, 1000);
        assertAccountBlotter(a2, -120, 50, 200);
        assertAccountBlotterTotal(a1, date(2012, 2, 1), date(2012, 2, 12), 600);
        assertAccountBlotterTotal(a2, date(2012, 2, 1), date(2012, 2, 12), 130);
    }

    @Test
    public void should_verify_running_balance_on_blotter_for_account() {
        // regular transactions and transfers
        TransactionBuilder.withDb(db).account(a1).amount(1000).create();
        TransactionBuilder.withDb(db).account(a2).amount(200).create();
        assertRunningBalance(a1, 1000);
        assertRunningBalance(a2, 200);
        assertTotals(1000, 200);

        // regular transfer
        TransferBuilder.withDb(db).fromAccount(a1).fromAmount(-100).toAccount(a2).toAmount(50).create();
        assertRunningBalance(a1, 900, 1000);
        assertRunningBalance(a2, 250, 200);
        assertTotals(900, 250);

        // regular split
        TransactionBuilder.withDb(db).account(a1).amount(-500)
                .withSplit(categoriesMap.get("A1"), -200)
                .withSplit(categoriesMap.get("A1"), -300)
                .create();
        assertRunningBalance(a1, 400, 900, 1000);
        assertRunningBalance(a2, 250, 200);
        assertTotals(400, 250);

        // transfer split
        TransactionBuilder.withDb(db).account(a2).amount(-120)
                .withSplit(categoriesMap.get("B"), -20)
                .withTransferSplit(a1, -100, 200)
                .create();
        assertRunningBalance(a1, 600, 400, 900, 1000);
        assertRunningBalance(a2, 130, 250, 200);
        assertTotals(600, 130);
    }

    private void assertAccountBlotter(Account account, long... amounts) {
        assertAccountBlotterColumn(account, DatabaseHelper.BlotterColumns.from_amount, amounts);
    }

    private void assertRunningBalance(Account account, long... amounts) {
        assertAccountBlotterColumn(account, DatabaseHelper.BlotterColumns.from_account_balance, amounts);
    }

    // blotter is from newest to oldest
    private void assertAccountBlotterColumn(Account account, DatabaseHelper.BlotterColumns column, long... values) {
        WhereFilter filter = createBlotterForAccountFilter(account);
        Cursor c = db.getBlotterForAccount(filter);
        try {
            int i = 0;
            while (c.moveToNext()) {
                if (i >= values.length) {
                    fail("Too many rows " + c.getCount() + ". Expected " + values.length);
                }
                long expectedAmount = values[i++];
                long amount = c.getLong(column.ordinal());
                assertEquals("Blotter row " + i, expectedAmount, amount);
            }
            if (i != values.length) {
                fail("Too few rows " + c.getCount() + ". Expected " + values.length);
            }
        } finally {
            c.close();
        }
    }

    private WhereFilter createBlotterForAccountFilter(Account account) {
        WhereFilter filter = WhereFilter.empty();
        filter.put(Criteria.eq(BlotterFilter.FROM_ACCOUNT_ID, String.valueOf(account.id)));
        return filter;
    }

    private void assertAccountBlotterTotal(Account a1, DateTime start, DateTime end, int total) {
        WhereFilter filter = enhanceFilterForAccountBlotter(WhereFilter.empty());
        filter.btw(BlotterFilter.DATETIME, String.valueOf(start.atMidnight().asLong()), String.valueOf(end.atDayEnd().asLong()));
        filter.eq(BlotterFilter.FROM_ACCOUNT_ID, String.valueOf(a1.id));
        TransactionsTotalCalculator calculator = new TransactionsTotalCalculator(db, filter);
        assertEquals(total, calculator.getAccountTotal().balance);
    }

    private void assertTotals(long... totalAmounts) {
        WhereFilter filter = WhereFilter.empty();
        TransactionsTotalCalculator calculator = new TransactionsTotalCalculator(db, filter);
        Total[] totals = calculator.getTransactionsBalance();
        assertEquals(totalAmounts.length, totals.length);
        for (int i = 0; i < totalAmounts.length; i++) {
            assertEquals("Total " + i, totalAmounts[i], totals[i].balance);
        }
    }

}
