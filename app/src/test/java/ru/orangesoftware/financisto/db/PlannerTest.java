package ru.orangesoftware.financisto.db;

import android.util.Log;

import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import ru.orangesoftware.financisto.filter.DateTimeCriteria;
import ru.orangesoftware.financisto.filter.WhereFilter;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.Total;
import ru.orangesoftware.financisto.model.TransactionInfo;
import ru.orangesoftware.financisto.test.AccountBuilder;
import ru.orangesoftware.financisto.test.CategoryBuilder;
import ru.orangesoftware.financisto.test.CurrencyBuilder;
import ru.orangesoftware.financisto.test.DateTime;
import ru.orangesoftware.financisto.test.RateBuilder;
import ru.orangesoftware.financisto.test.TransactionBuilder;
import ru.orangesoftware.financisto.test.TransferBuilder;
import ru.orangesoftware.financisto.utils.CurrencyCache;
import ru.orangesoftware.financisto.utils.FuturePlanner;
import ru.orangesoftware.financisto.utils.MonthlyViewPlanner;
import ru.orangesoftware.financisto.utils.TransactionList;
import ru.orangesoftware.financisto.utils.Utils;

import static org.junit.Assert.*;
import static ru.orangesoftware.financisto.test.DateTime.NULL_DATE;
import static ru.orangesoftware.financisto.test.DateTime.date;

public class PlannerTest extends AbstractDbTest {

    Currency c1;
    Account a1;
    Currency c2;
    Account a2;
    Map<String, Category> categoriesMap;
    Currency homeCurrency;

    Date from = date(2011, 8, 1).atMidnight().asDate();
    Date to = date(2011, 8, 16).atDayEnd().asDate();
    Date now = date(2011, 8, 8).at(23, 20, 0, 0).asDate();

    @Override
    public void setUp() throws Exception {
        super.setUp();
        c1 = CurrencyBuilder.withDb(db).name("USD").title("Dollar").symbol("$").create();
        c2 = CurrencyBuilder.withDb(db).name("SGD").title("Singapore Dollar").symbol("S$").makeDefault().create();
        a1 = AccountBuilder.createDefault(db, c1);
        a2 = AccountBuilder.createDefault(db, c2);
        categoriesMap = CategoryBuilder.createDefaultHierarchy(db);
        homeCurrency = db.getHomeCurrency();
        CurrencyCache.initialize(db);
    }

    @Test
    public void should_generate_monthly_view_for_account() {
        prepareData();
        //a1
        //0  2011-08-08 +1000          t1
        //1  2011-08-09 -100 -> a2     t2
        //2  2011-08-09 +40            r2
        //3  2011-08-10 -500           t3
        //4  2011-08-10 -50            r1
        //5  2011-08-11 -100 -> a2     t4
        //6  2011-08-12 +200 <- a2     t5-s2
        //7  2011-08-12 +52  <- a2     r4
        //8  2011-08-12 -50            r1
        //9  2011-08-12 +30  <- a2     r6-s2
        //10 2011-08-14 -100           t7
        //11 2011-08-14 -50            r1
        //12 2011-08-15 +400           t6
        //13 2011-08-15 -210 -> a2     r3
        //14 2011-08-15 -105 -> a2     r5
        //15 2011-08-16 -50            r1
        //16 2011-08-16 +40            r2
        MonthlyViewPlanner planner = new MonthlyViewPlanner(db, a1, false, from, to, now);
        TransactionList list = planner.getPlannedTransactionsWithTotals();
        logTransactions(list.transactions);
        assertTransactions(list.transactions,
                date(2011, 8, 8), 1000,
                date(2011, 8, 9), -100,
                date(2011, 8, 9), 40,
                date(2011, 8, 10), -500,
                date(2011, 8, 10), -50,
                date(2011, 8, 11), -100,
                date(2011, 8, 12), 200,
                date(2011, 8, 12), 52,
                date(2011, 8, 12), -50,
                date(2011, 8, 12), 30,
                date(2011, 8, 14), -100,
                date(2011, 8, 14), -50,
                date(2011, 8, 15), 400,
                date(2011, 8, 15), -210,
                date(2011, 8, 15), -105,
                date(2011, 8, 16), -50,
                date(2011, 8, 16), 40
        );
        assertAmount(447, a1.currency, list.totals[0]);
    }

    @Test
    public void should_generate_credit_card_statement() {
        prepareData();
        MonthlyViewPlanner planner = new MonthlyViewPlanner(db, a1, true, from, to, now);
        TransactionList statement = planner.getCreditCardStatement();
        List<TransactionInfo> transactions = statement.transactions;
        logTransactions(transactions);
        assertTransactions(transactions,
                //payments
                DateTime.NULL_DATE, 0,
                date(2011, 8, 15), 400,
                //credits
                DateTime.NULL_DATE, 0,
                date(2011, 8, 8), 1000,
                date(2011, 8, 9), 40,
                date(2011, 8, 12), 200,
                date(2011, 8, 12), 52,
                date(2011, 8, 12), 30,
                date(2011, 8, 16), 40,
                //expenses
                DateTime.NULL_DATE, 0,
                date(2011, 8, 9), -100,
                date(2011, 8, 10), -500,
                date(2011, 8, 10), -50,
                date(2011, 8, 11), -100,
                date(2011, 8, 12), -50,
                date(2011, 8, 14), -100,
                date(2011, 8, 14), -50,
                date(2011, 8, 15), -210,
                date(2011, 8, 15), -105,
                date(2011, 8, 16), -50
        );
        // 400 gets excluded as payment
        assertAmount(47, a1.currency, statement.totals[0]);
    }

    @Test
    public void should_generate_monthly_preview_for_the_next_month_correctly() {
        prepareData();
        from = date(2011, 9, 1).atMidnight().asDate();
        to = date(2011, 9, 16).atDayEnd().asDate();

        //2011-09-02 -50            r1
        //2011-09-02 +52  <- a2     r4
        //2011-09-02 +30  <- a2     r6
        //2011-09-04 -50            r1
        //2011-09-06 -50            r1
        //2011-08-06 +40            r2
        //2011-09-08 -50            r1
        //2011-09-09 +52  <- a2     r4
        //2011-09-09 +30  <- a2     r6
        //2011-09-10 -50            r1
        //2011-09-12 -50            r1
        //2011-09-13 +40            r2
        //2011-09-14 -50            r1
        //2011-09-16 -50            r1
        //2011-09-16 +52  <- a2     r4
        //2011-09-16 +30  <- a2     r6
        MonthlyViewPlanner planner = new MonthlyViewPlanner(db, a1, false, from, to, now);
        List<TransactionInfo> transactions = planner.getPlannedTransactions();
        logTransactions(transactions);
        assertTransactions(transactions,
                date(2011, 9, 1), -50,
                date(2011, 9, 2), 52,
                date(2011, 9, 2), 30,
                date(2011, 9, 3), -50,
                date(2011, 9, 5), -50,
                date(2011, 9, 6), 40,
                date(2011, 9, 7), -50,
                date(2011, 9, 9), 52,
                date(2011, 9, 9), -50,
                date(2011, 9, 9), 30,
                date(2011, 9, 11), -50,
                date(2011, 9, 13), -50,
                date(2011, 9, 13), 40,
                date(2011, 9, 15), -50,
                date(2011, 9, 16), 52,
                date(2011, 9, 16), 30
        );

    }

    @Test
    public void should_generate_monthly_preview_for_the_previous_month_correctly() {
        prepareData();
        from = date(2011, 7, 1).atMidnight().asDate();
        to = date(2011, 7, 16).atDayEnd().asDate();

        MonthlyViewPlanner planner = new MonthlyViewPlanner(db, a1, false, from, to, now);
        List<TransactionInfo> transactions = planner.getPlannedTransactions();
        logTransactions(transactions);
        assertTransactions(transactions,
                date(2011, 7, 9), 122
        );

    }

    @Test
    public void should_generate_future_preview() {
        prepareData();
        TransactionList transactions = planTransactions(date(2011, 7, 1), date(2011, 7, 19));
        // well, this is going to be impossible to re-verify if something breaks :)
        assertTransactions2(transactions.transactions,
                0, date(2011, 7, 1), -50, "x1",
                1, date(2011, 7, 3), -50, "x1",
                2, date(2011, 7, 5), -50, "x1",
                3, date(2011, 7, 7), -50, "x1",
                4, date(2011, 7, 9), 122, "t0",
                5, date(2011, 7, 9), -50, "x1",
                6, date(2011, 7, 11), -50, "x1",
                7, date(2011, 7, 13), -50, "x1",
                8, date(2011, 7, 15), -50, "x1",
                9, date(2011, 7, 17), -50, "x1",
                10, date(2011, 7, 19), -50, "x1");
        assertTrue(transactions.totals[0].isError());

        RateBuilder.withDb(db).from(c1).to(c2).at(DateTime.date(2011, 7, 1)).rate(2.0f).create();
        transactions = planTransactions(date(2011, 7, 1), date(2011, 7, 19));
        assertFalse(transactions.totals[0].isError());
        assertAmount(2 * 122, 10 * (-50), homeCurrency, transactions.totals[0]);

        transactions = planTransactions(date(2011, 7, 20), date(2011, 8, 4));
        assertTransactions2(transactions.transactions,
                11, date(2011, 7, 21), -50, "x1",
                12, date(2011, 7, 23), -50, "x1",
                13, date(2011, 7, 25), -50, "x1",
                14, date(2011, 7, 27), -50, "x1",
                15, date(2011, 7, 29), -50, "x1",
                16, date(2011, 7, 31), -50, "x1",
                17, date(2011, 8, 2), -50, "r1",
                18, date(2011, 8, 2), -50, "x1",
                19, date(2011, 8, 2), 40, "r2",
                20, date(2011, 8, 4), -50, "r1",
                21, date(2011, 8, 4), -50, "x1");

        transactions = planTransactions(date(2011, 8, 5), date(2011, 8, 15));
        assertTransactions2(transactions.transactions,
                22, date(2011, 8, 5), -600, "r4",
                23, date(2011, 8, 5), -120, "r6",
                24, date(2011, 8, 6), -50, "r1",
                25, date(2011, 8, 6), -50, "x1",
                26, date(2011, 8, 8), 1000, "t1",
                27, date(2011, 8, 8), -50, "r1",
                28, date(2011, 8, 8), -50, "x1",
                29, date(2011, 8, 9), -100, "t2",
                30, date(2011, 8, 9), 40, "r2",
                31, date(2011, 8, 10), -300, "t3-s2",
                31, date(2011, 8, 10), -200, "t3-s1",
                32, date(2011, 8, 10), -50, "r1",
                33, date(2011, 8, 10), -50, "x1",
                34, date(2011, 8, 11), -100, "t4-s1",
                35, date(2011, 8, 12), -100, "t5-s2",
                35, date(2011, 8, 12), -20, "t5-s1",
                36, date(2011, 8, 12), -600, "r4",
                37, date(2011, 8, 12), -50, "r1",
                38, date(2011, 8, 12), -50, "x1",
                39, date(2011, 8, 12), -120, "r6",
                40, date(2011, 8, 14), -100, "t7",
                41, date(2011, 8, 14), -50, "r1",
                42, date(2011, 8, 14), -50, "x1",
                43, date(2011, 8, 15), 400, "t6",
                44, date(2011, 8, 15), -210, "r3",
                45, date(2011, 8, 15), -105, "r5"
        );
    }

    private TransactionList planTransactions(DateTime start, DateTime end) {
        now = start.atMidnight().asDate();
        to = end.atDayEnd().asDate();
        WhereFilter filter = WhereFilter.empty();
        filter.put(new DateTimeCriteria(now.getTime(), to.getTime()));
        FuturePlanner planner = new FuturePlanner(db, filter, now);
        TransactionList data = planner.getPlannedTransactionsWithTotals();
        logTransactions(data.transactions);
        return data;
    }

    private void prepareData() {
        // regular transactions and transfers
        //t0
        TransactionBuilder.withDb(db).dateTime(date(2011, 7, 9).atNoon())
                .account(a1).amount(122).note("t0").create();
        //t1
        TransactionBuilder.withDb(db).dateTime(date(2011, 8, 8).atNoon())
                .account(a1).amount(1000).note("t1").create();

        // regular transfer
        //t2
        TransferBuilder.withDb(db).dateTime(date(2011, 8, 9).atNoon())
                .fromAccount(a1).fromAmount(-100).toAccount(a2).toAmount(50).note("t2").create();

        // regular split
        //t3
        TransactionBuilder.withDb(db).dateTime(date(2011, 8, 10).atNoon())
                .account(a1).amount(-500)
                .withSplit(categoriesMap.get("A1"), -200, "t3-s1")
                .withSplit(categoriesMap.get("A1"), -300, "t3-s2")
                .note("t3")
                .create();

        // transfer split
        //t4
        TransactionBuilder.withDb(db).dateTime(date(2011, 8, 11).atNoon())
                .account(a1).amount(-100)
                .withTransferSplit(a2, -100, 20, "t4-s1")
                .note("t4")
                .create();
        //t5
        TransactionBuilder.withDb(db).dateTime(date(2011, 8, 12).atNoon())
                .account(a2).amount(-120)
                .withSplit(categoriesMap.get("B"), -20, "t5-s1")
                .withTransferSplit(a1, -100, 200, "t5-s2")
                .note("t5")
                .create();

        // payment
        //t6
        TransactionBuilder.withDb(db).dateTime(date(2011, 8, 15).atNoon()).account(a1).amount(400).ccPayment().note("t6").create();

        //scheduled once
        //t7
        TransactionBuilder.withDb(db).scheduleOnce(date(2011, 8, 14).atNoon()).account(a1).amount(-100).note("t7").create();

        //scheduled recur
        //r1
        TransactionBuilder.withDb(db).scheduleRecur("2011-08-02T21:40:00~DAILY:interval@2#~INDEFINETELY:null")
                .account(a1).amount(-50).note("r1").create();
        //r2
        TransactionBuilder.withDb(db).scheduleRecur("2011-08-02T23:00:00~WEEKLY:days@TUE#interval@1#~INDEFINETELY:null")
                .account(a1).amount(+40).note("r2").create();

        //this should not be included because the account is differ
        TransactionBuilder.withDb(db).scheduleRecur("2011-07-01T21:40:00~DAILY:interval@2#~INDEFINETELY:null")
                .account(a2).amount(-50).note("x1").create();

        //these should not be included because the date is out of picture
        TransactionBuilder.withDb(db).scheduleOnce(date(2011, 10, 14).at(13, 0, 0, 0))
                .account(a1).amount(-500).note("x2?").create();
        TransactionBuilder.withDb(db).scheduleRecur("2011-10-01T21:40:00~DAILY:interval@2#~INDEFINETELY:null")
                .account(a1).amount(-500).note("x3?").create();

        //this is a scheduled transfer which should appear in the monthly view
        //r3
        TransferBuilder.withDb(db).scheduleOnce(date(2011, 8, 15).at(13, 0, 0, 0))
                .fromAccount(a1).fromAmount(-210).toAccount(a2).toAmount(51).note("r3").create();
        //r4
        TransferBuilder.withDb(db).scheduleRecur("2011-08-02T21:20:00~WEEKLY:days@FRI#interval@1#~INDEFINETELY:null")
                .fromAccount(a2).fromAmount(-600).toAccount(a1).toAmount(52).note("r4").create();

        //this is a scheduled split with a transfer which should appear in the monthly view
        //r5
        TransactionBuilder.withDb(db).scheduleOnce(date(2011, 8, 15).at(14, 0, 0, 0))
                .account(a1).amount(-105)
                .withSplit(categoriesMap.get("A1"), -5, "r5-s1")
                .withTransferSplit(a2, -100, 22, "r5-s2")
                .note("r5")
                .create();
        //r6
        TransactionBuilder.withDb(db).scheduleRecur("2011-08-02T22:30:00~WEEKLY:days@FRI#interval@1#~INDEFINETELY:null")
                .account(a2).amount(-120)
                .withSplit(categoriesMap.get("B"), -20, "r6-s1")
                .withTransferSplit(a1, -88, 30, "r6-s2")
                .note("r6")
                .create();
    }

    private void logTransactions(List<TransactionInfo> transactions) {
        Log.d("PlannerTest", "===== Planned transactions: " + transactions.size() + " =====");
        SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
        for (TransactionInfo transaction : transactions) {
            Log.d("PlannerTest", df.format(new Date(transaction.dateTime)) + " " + Utils.amountToString(Currency.EMPTY, transaction.fromAmount) + " " + transaction.note);
        }
        Log.d("PlannerTest", "==========");
    }

    private void assertTransactions(List<TransactionInfo> transactions, Object... data) {
        int count = data.length / 2;
        if (count > transactions.size()) {
            fail("Too few transactions. Expected " + count + ", Got " + transactions.size());
        }
        if (count < transactions.size()) {
            fail("Too many transactions. Expected " + count + ", Got " + transactions.size());
        }
        for (int i = 0; i < count; i++) {
            assertTransaction("Row " + i, transactions.get(i), (DateTime) data[i * 2], (Integer) data[i * 2 + 1]);
        }
    }

    private void assertTransaction(String row, TransactionInfo t, DateTime expectedDate, long expectedAmount) {
        if (expectedDate == NULL_DATE) {
            assertEquals(row, 0, t.dateTime);
            assertEquals(row, 0, t.fromAmount);
        } else {
            assertEquals(row, asDate(expectedDate), asDate(t.dateTime));
            assertEquals(row, expectedAmount, t.fromAmount);
        }
    }

    private void assertTransactions2(List<TransactionInfo> transactions, Object... data) {
        int count = data.length / 4;
        if (count > transactions.size()) {
            fail("Too few transactions. Expected " + count + ", Got " + transactions.size());
        }
        if (count < transactions.size()) {
            fail("Too many transactions. Expected " + count + ", Got " + transactions.size());
        }
        for (int i = 0; i < count; i++) {
            assertTransaction2("Row " + (i + (Integer) data[i * 4]), transactions.get(i), (DateTime) data[i * 4 + 1], (Integer) data[i * 4 + 2], (String) data[i * 4 + 3]);
        }
    }

    private void assertTransaction2(String row, TransactionInfo t, DateTime date, long expectedAmount, String note) {
        assertEquals(row + ":" + note, asDate(date), asDate(t.dateTime));
        assertEquals(row + ":" + note, expectedAmount, t.fromAmount);
        assertEquals(row + ":" + note, note, t.note);
    }

    private Date asDate(DateTime date) {
        return date.atMidnight().asDate();
    }

    private Date asDate(long date) {
        return asDate(DateTime.fromTimestamp(date));
    }

    private void assertAmount(long expectedIncome, long expectedExpenses, Currency expectedCurrency, Total total) {
        assertEquals(expectedCurrency, total.currency);
        assertEquals(expectedIncome, total.income);
        assertEquals(expectedExpenses, total.expenses);
        assertEquals(expectedIncome + expectedExpenses, total.balance);
    }

    private void assertAmount(long expectedAmount, Currency expectedCurrency, Total total) {
        assertEquals(expectedCurrency, total.currency);
        assertEquals(expectedAmount, total.balance);
    }

}
