package ru.orangesoftware.financisto.db;

import android.database.Cursor;

import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.blotter.BlotterFilter;
import ru.orangesoftware.financisto.filter.WhereFilter;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.Payee;
import ru.orangesoftware.financisto.model.Transaction;
import ru.orangesoftware.financisto.model.TransactionStatus;
import ru.orangesoftware.financisto.test.AccountBuilder;
import ru.orangesoftware.financisto.test.CategoryBuilder;
import ru.orangesoftware.financisto.test.DateTime;
import ru.orangesoftware.financisto.test.TransactionBuilder;
import ru.orangesoftware.financisto.test.TransferBuilder;

import static org.junit.Assert.*;
import static ru.orangesoftware.financisto.test.DateTime.date;

public class AccountPurgeTest extends AbstractDbTest {

    Account a1;
    Account a2;
    Map<String, Category> categoriesMap;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        a1 = AccountBuilder.createDefault(db);
        a2 = AccountBuilder.createDefault(db);
        categoriesMap = CategoryBuilder.createDefaultHierarchy(db);
        /*                        A1     A2
         * 29/05 A1 +10          |  40  |
         * 28/05 A1 -20          |  30  |
         * 27/05 A1->A2 -100 +20 |  50  | -40
         * 26/05 A1 +100         | 150  |
         * 25/05 A2->A1 -50 +10  |  50  | -60
         * 24/05 A1 +200         |  40  |
         * 24/05 A2 -20          |      | -10
         * 23/05 A1 -150         | -160 |
         *          -100         |      |
         *    -> A2 -50 +10      |      | 10
         * 22/05 A1 -20          | -10  |
         * 21/05 A1 +10          |  10  |
         * */
        TransactionBuilder.withDb(db).dateTime(date(2012, 5, 29)).account(a1).amount(10).create();
        TransactionBuilder.withDb(db).dateTime(date(2012, 5, 28)).account(a1).amount(-20).create();
        TransferBuilder.withDb(db).dateTime(date(2012, 5, 27))
                .fromAccount(a1).fromAmount(-100)
                .toAccount(a2).toAmount(20).create();
        TransactionBuilder.withDb(db).dateTime(date(2012, 5, 26)).account(a1).amount(100).create();
        TransferBuilder.withDb(db).dateTime(date(2012, 5, 25))
                .fromAccount(a2).fromAmount(-50)
                .toAccount(a1).toAmount(10).create();
        TransactionBuilder.withDb(db).dateTime(date(2012, 5, 24)).account(a1).amount(200).create();
        TransactionBuilder.withDb(db).dateTime(date(2012, 5, 24)).account(a2).amount(-20).create();
        TransactionBuilder.withDb(db).dateTime(date(2012, 5, 23)).account(a1).amount(-150)
                .withSplit(categoriesMap.get("A1"), -100)
                .withTransferSplit(a2, -50, 10)
                .create();
        TransactionBuilder.withDb(db).dateTime(date(2012, 5, 22)).account(a1).amount(-20).create();
        TransactionBuilder.withDb(db).dateTime(date(2012, 5, 21)).account(a1).amount(10).create();
    }

    @Test
    public void should_delete_first_account_correctly() {
        db.deleteAccount(a1.id);
        assertAccount(a2, -40);
        assertAccountBlotter(a2, 20, -50, -20, 10);
        assertAccountRunningBalance(a2, -40, -60, -10, 10);
    }

    @Test
    public void should_delete_second_account_correctly() {
        db.deleteAccount(a2.id);
        assertAccount(a1, 40);
        assertAccountBlotter(a1, 10, -20, -100, 100, 10, 200, -150, -20, 10);
        assertAccountRunningBalance(a1, 40, 30, 50, 150, 50, 40, -160, -10, 10);
    }

    @Test
    public void should_purge_transactions_older_than_specified_date() {
        //given
        assertAccounts();
        assertTransactionsCount(a1, 10);
        assertTransactionsCount(a2, 2);
        assertOldestTransaction(a1, date(2012, 5, 21), 10);
        assertAccountBlotter(a1, 10, -20, -100, 100, 10, 200, -150, -20, 10);
        assertAccountRunningBalance(a1, 40, 30, 50, 150, 50, 40, -160, -10, 10);
        assertAccountBlotter(a2, 20, -50, -20, 10);
        assertAccountRunningBalance(a2, -40, -60, -10, 10);
        assertAccounts();
        //when purged nothing, then nothing changes
        db.purgeAccountAtDate(a1, date(2012, 5, 20).asLong());
        assertOldestTransaction(a1, date(2012, 5, 21), 10);
        assertAccountBlotter(a1, 10, -20, -100, 100, 10, 200, -150, -20, 10);
        assertAccountRunningBalance(a1, 40, 30, 50, 150, 50, 40, -160, -10, 10);
        assertAccountBlotter(a2, 20, -50, -20, 10);
        assertAccountRunningBalance(a2, -40, -60, -10, 10);
        assertAccounts();
        //when purged last, then nothing changes
        db.purgeAccountAtDate(a1, date(2012, 5, 21).asLong());
        assertOldestTransaction(a1, date(2012, 5, 21).atDayEnd(), 10);
        assertAccountBlotter(a1, 10, -20, -100, 100, 10, 200, -150, -20, 10);
        assertAccountRunningBalance(a1, 40, 30, 50, 150, 50, 40, -160, -10, 10);
        assertAccountBlotter(a2, 20, -50, -20, 10);
        assertAccountRunningBalance(a2, -40, -60, -10, 10);
        assertAccounts();
        //when purged one, balance should stay
        db.purgeAccountAtDate(a1, date(2012, 5, 22).asLong());
        assertArchiveTransaction(a1, date(2012, 5, 22).atDayEnd(), -10);
        assertAccountBlotter(a1, 10, -20, -100, 100, 10, 200, -150, -10);
        assertAccountRunningBalance(a1, 40, 30, 50, 150, 50, 40, -160, -10);
        assertAccountBlotter(a2, 20, -50, -20, 10);
        assertAccountRunningBalance(a2, -40, -60, -10, 10);
        assertAccounts();
        //when purged split, then transfers should be restored
        db.purgeAccountAtDate(a1, date(2012, 5, 23).asLong());
        assertArchiveTransaction(a1, date(2012, 5, 23).atDayEnd(), -160);
        assertAccountBlotter(a1, 10, -20, -100, 100, 10, 200, -160);
        assertAccountRunningBalance(a1, 40, 30, 50, 150, 50, 40, -160);
        assertAccountBlotter(a2, 20, -50, -20, 10);
        assertAccountRunningBalance(a2, -40, -60, -10, 10);
        assertAccounts();
        //when purged 2 transactions together
        db.purgeAccountAtDate(a1, date(2012, 5, 24).asLong());
        assertArchiveTransaction(a1, date(2012, 5, 24).atDayEnd(), 40);
        assertAccountBlotter(a1, 10, -20, -100, 100, 10, 40);
        assertAccountRunningBalance(a1, 40, 30, 50, 150, 50, 40);
        assertAccountBlotter(a2, 20, -50, -20, 10);
        assertAccountRunningBalance(a2, -40, -60, -10, 10);
        assertAccounts();
        //when purged a transfer
        db.purgeAccountAtDate(a1, date(2012, 5, 27).asLong());
        assertArchiveTransaction(a1, date(2012, 5, 27).atDayEnd(), 50);
        assertAccountBlotter(a1, 10, -20, 50);
        assertAccountRunningBalance(a1, 40, 30, 50);
        assertAccountBlotter(a2, 20, -50, -20, 10);
        assertAccountRunningBalance(a2, -40, -60, -10, 10);
        assertAccounts();
        //when everything
        db.purgeAccountAtDate(a1, date(2012, 5, 29).asLong());
        assertArchiveTransaction(a1, date(2012, 5, 29).atDayEnd(), 40);
        assertAccountBlotter(a1, 40);
        assertAccountRunningBalance(a1, 40);
        assertAccountBlotter(a2, 20, -50, -20, 10);
        assertAccountRunningBalance(a2, -40, -60, -10, 10);
        assertAccounts();
    }

    private void assertAccountBlotter(Account account, long... expectedAmounts) {
        WhereFilter filter = WhereFilter.empty();
        filter.eq(BlotterFilter.FROM_ACCOUNT_ID, String.valueOf(account.id));
        Cursor c = db.getBlotterForAccount(filter);
        long[] actualAmounts = new long[c.getCount()];
        try {
            int i = 0;
            while (c.moveToNext()) {
                Transaction t = Transaction.fromBlotterCursor(c);
                actualAmounts[i++] = t.fromAmount;
            }
        } finally {
            c.close();
        }
        assertAmountsForAccount(account, expectedAmounts, actualAmounts);
    }

    private void assertAccountRunningBalance(Account account, long... expectedBalance) {
        Cursor c = db.db().rawQuery("select balance from running_balance where account_id=? order by datetime desc, transaction_id desc", new String[]{String.valueOf(account.id)});
        long[] actualBalance = new long[c.getCount()];
        try {
            int i = 0;
            while (c.moveToNext()) {
                actualBalance[i++] = c.getLong(0);
            }
        } finally {
            c.close();
        }
        assertAmountsForAccount(account, expectedBalance, actualBalance);
    }

    private void assertAmountsForAccount(Account account, long[] expectedAmounts, long[] amounts) {
        String expectedVsActual = "Account " + account.id + " -> Expected:" + Arrays.toString(expectedAmounts) + ", Actual:" + Arrays.toString(amounts);
        assertEquals("Too few or too many transactions. " + expectedVsActual, expectedAmounts.length, amounts.length);
        assertTrue(expectedVsActual, Arrays.equals(expectedAmounts, amounts));
    }

    private Transaction assertOldestTransaction(Account account, DateTime date, long expectedAmount) {
        Transaction t = getOldestTransaction(account);
        assertEquals(date.asLong(), t.dateTime);
        assertEquals(expectedAmount, t.fromAmount);
        // this is the very first transaction, so running balance == amount
        assertAccountBalanceForTransaction(t, account, expectedAmount);
        return t;
    }

    private void assertArchiveTransaction(Account account, DateTime date, long expectedAmount) {
        Transaction t = assertOldestTransaction(account, date, expectedAmount);
        Payee payee = db.get(Payee.class, t.payeeId);
        assertEquals(getContext().getString(R.string.purge_account_payee), payee.title);
        assertEquals(TransactionStatus.CL, t.status);
    }

    private Transaction getOldestTransaction(Account account) {
        long id = DatabaseUtils.rawFetchId(db,
                "select _id from transactions where from_account_id=? and is_template=0 order by datetime limit 1",
                new String[]{String.valueOf(account.id)});
        return db.get(Transaction.class, id);
    }

    private void assertAccounts() {
        assertAccount(a1, 40);
        assertAccount(a2, -40);
    }

    private void assertAccount(Account account, long accountTotal) {
        assertAccountTotal(account, accountTotal);
        assertFinalBalanceForAccount(account, accountTotal);
        db.rebuildRunningBalanceForAccount(account);
        assertAccountTotal(account, accountTotal);
        assertFinalBalanceForAccount(account, accountTotal);
    }

}
