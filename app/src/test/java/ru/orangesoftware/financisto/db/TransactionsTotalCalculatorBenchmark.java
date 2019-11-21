package ru.orangesoftware.financisto.db;

import android.util.Log;

import org.junit.Test;

import java.util.Calendar;

import ru.orangesoftware.financisto.filter.WhereFilter;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.test.AccountBuilder;
import ru.orangesoftware.financisto.test.CurrencyBuilder;
import ru.orangesoftware.financisto.test.DateTime;
import ru.orangesoftware.financisto.test.RateBuilder;
import ru.orangesoftware.financisto.test.TransactionBuilder;

import static org.junit.Assert.*;

public class TransactionsTotalCalculatorBenchmark extends AbstractDbTest {

    Currency c1;
    Currency c2;

    Account a1;

    TransactionsTotalCalculator c;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        c1 = CurrencyBuilder.withDb(db).name("USD").title("Dollar").symbol("$").create();
        c2 = CurrencyBuilder.withDb(db).name("EUR").title("Euro").symbol("€").create();

        c = new TransactionsTotalCalculator(db, WhereFilter.empty());

        a1 = AccountBuilder.withDb(db).title("Cash").currency(c1).create();
    }

    @Test
    public void should_benchmark_blotter_total_in_home_currency() {
        long t0 = System.currentTimeMillis();
        int count = 366;
        Calendar calendar = Calendar.getInstance();
        while (--count > 0) {
            DateTime date = DateTime.fromTimestamp(calendar.getTimeInMillis());
            RateBuilder.withDb(db).from(c1).to(c2).at(date).rate(1f / count).create();
            TransactionBuilder.withDb(db).account(a1).dateTime(date.atMidnight()).amount(1000).create();
            TransactionBuilder.withDb(db).account(a1).dateTime(date.atNoon()).amount(2000).create();
            TransactionBuilder.withDb(db).account(a1).dateTime(date.atDayEnd()).amount(3000).create();
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        long t1 = System.currentTimeMillis();
        Log.d("TransactionsTotalCalculatorBenchmark", "Time to create a year amount of data: " + (t1 - t0) + "ms");
        c.getAccountBalance(c2, a1.id);
        long t2 = System.currentTimeMillis();
        Log.d("TransactionsTotalCalculatorBenchmark", "Time to get account total: " + (t2 - t1) + "ms");
    }

}
