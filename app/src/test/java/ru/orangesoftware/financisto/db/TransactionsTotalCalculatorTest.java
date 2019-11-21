package ru.orangesoftware.financisto.db;

import org.junit.Test;

import java.util.Map;

import ru.orangesoftware.financisto.filter.WhereFilter;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.Total;
import ru.orangesoftware.financisto.model.Transaction;
import ru.orangesoftware.financisto.test.AccountBuilder;
import ru.orangesoftware.financisto.test.CategoryBuilder;
import ru.orangesoftware.financisto.test.CurrencyBuilder;
import ru.orangesoftware.financisto.test.DateTime;
import ru.orangesoftware.financisto.test.RateBuilder;
import ru.orangesoftware.financisto.test.TransactionBuilder;
import ru.orangesoftware.financisto.test.TransferBuilder;

import static org.junit.Assert.*;

public class TransactionsTotalCalculatorTest extends AbstractDbTest {

    Currency c1, c2, c3, c4;
    Account a1, a2, a3;
    Transaction a1t1_09th, a3t1_10th, a1t2_17th, a2t1_17th, a2t2_18th, a1t3_20th, a1t4_22nd, a1t5_23rd, a1t5_23rd_s1, a1t5_23rd_s2;

    float r_c1c2_17th = 0.78592f;
    float r_c1c2_18th = 0.78635f;
    float r_c1c3_5th = 0.62510f;
    float r_c2c3_5th = 0.12453f;

    float r_c2c1_17th = 1f / r_c1c2_17th;
    float r_c2c1_18th = 1f / r_c1c2_18th;

    TransactionsTotalCalculator c;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        Map<String, Category> categories = CategoryBuilder.createDefaultHierarchy(db);

        c1 = CurrencyBuilder.withDb(db).name("USD").title("Dollar").symbol("$").makeDefault().create();
        c2 = CurrencyBuilder.withDb(db).name("EUR").title("Euro").symbol("€").create();
        c3 = CurrencyBuilder.withDb(db).name("SGD").title("Singapore Dollar").symbol("S$").create();
        c4 = CurrencyBuilder.withDb(db).name("RUB").title("Russian Ruble").symbol("p.").create();

        c = new TransactionsTotalCalculator(db, WhereFilter.empty());

        RateBuilder.withDb(db).from(c1).to(c2).at(DateTime.date(2012, 1, 17)).rate(r_c1c2_17th).create();
        RateBuilder.withDb(db).from(c1).to(c2).at(DateTime.date(2012, 1, 18)).rate(r_c1c2_18th).create();
        RateBuilder.withDb(db).from(c1).to(c3).at(DateTime.date(2012, 1, 5)).rate(r_c1c3_5th).create();
        RateBuilder.withDb(db).from(c2).to(c3).at(DateTime.date(2012, 1, 5)).rate(r_c2c3_5th).create();

        a1 = AccountBuilder.withDb(db).title("Cash").currency(c1).create();
        a2 = AccountBuilder.withDb(db).title("Bank").currency(c2).create();
        a3 = AccountBuilder.withDb(db).title("Cash2").currency(c1).doNotIncludeIntoTotals().create();

        /*
        a1t1 09 A1 USD -100 (orig: EUR -20)
        a3t1_10th 10 A3 SGD +555
        a1t2_17th 17 A1 USD +100
        a2t1_17th 17 A2 EUR -100
        a2t2_18th 18 A2 EUR -250
        a1t3_20th 20 A1 USD -50 FT
        a2t3 20 A2 EUR +20 TT
        a1t4_22nd 22 A1 USD -450
        a1t5_23rd 23 A1 USD -50      S
             23 A1 USD -150 FT  S
             23 A2 EUR +100 TT  S
         */

        a1t1_09th = TransactionBuilder.withDb(db).account(a1).dateTime(DateTime.date(2012, 1, 9)).amount(-100).originalAmount(c2, -20).create();
        a3t1_10th = TransactionBuilder.withDb(db).account(a3).dateTime(DateTime.date(2012, 1, 10)).amount(555).create();
        a1t2_17th = TransactionBuilder.withDb(db).account(a1).dateTime(DateTime.date(2012, 1, 17).at(13, 30, 0, 0)).amount(100).create();
        a2t1_17th = TransactionBuilder.withDb(db).account(a2).dateTime(DateTime.date(2012, 1, 17).at(13, 30, 0, 0)).amount(-100).create();
        a2t2_18th = TransactionBuilder.withDb(db).account(a2).dateTime(DateTime.date(2012, 1, 18).at(18, 40, 0, 0)).amount(-250).create();
        a1t3_20th = TransferBuilder.withDb(db).fromAccount(a1).toAccount(a2).dateTime(DateTime.date(2012, 1, 20).atNoon()).fromAmount(-50).toAmount(20).create();
        a1t4_22nd = TransactionBuilder.withDb(db).account(a1).dateTime(DateTime.date(2012, 1, 22).atMidnight()).amount(-450).create();
        a1t5_23rd = TransactionBuilder.withDb(db).account(a1).dateTime(DateTime.date(2012, 1, 23)).amount(-200)
                .withSplit(categories.get("A1"), -50)
                .withTransferSplit(a2, -150, 100)
                .create();
        a1t5_23rd_s1 = a1t5_23rd.splits.get(0);
        a1t5_23rd_s2 = a1t5_23rd.splits.get(1);
    }

    @Test
    public void should_return_error_if_exchange_rate_not_available() {
        TransactionBuilder.withDb(db).account(a1).dateTime(DateTime.date(2012, 1, 10)).amount(1).create();

        assertFalse(c.getAccountBalance(c1, a1.id).isError()); // no conversion
        assertFalse(c.getAccountBalance(c3, a1.id).isError()); // all rates are available

        Total total = c.getAccountBalance(c2, a1.id);
        assertTrue(total.isError()); // no rate available on 10th

        total = c.getAccountBalance(c4, a1.id);
        assertTrue(total.isError());  // no rates at all
    }

    @Test
    public void should_calculate_blotter_total_in_multiple_currencies() {
        Total[] totals = c.getTransactionsBalance();
        assertEquals(2, totals.length);
        assertEquals(-700, totals[0].balance);
        assertEquals(-230, totals[1].balance);
    }

    @Test
    public void should_calculate_blotter_total_in_home_currency() {
        assertEquals((long) (-100f + 100f - (1f / r_c1c2_17th) * 100f - (1f / r_c1c2_18th) * 250f - 50f + 50f - 450f - 50f - 150f + 150f), c.getBlotterBalance(c1).balance);
        assertEquals((long) (-20f + r_c1c2_17th * 100f - 100f - 250f - 20f + 20f - r_c1c2_18th * 450f - r_c1c2_18th * 50f - 100f + 100f), c.getBlotterBalance(c2).balance);
        assertEquals(c.getBlotterBalance(c1).balance, c.getBlotterBalanceInHomeCurrency().balance);
    }

    @Test
    public void should_calculate_account_total_in_home_currency() {
        //no conversion
        assertEquals(a1t1_09th.fromAmount + a1t2_17th.fromAmount + a1t3_20th.fromAmount + a1t4_22nd.fromAmount + a1t5_23rd.fromAmount, c.getAccountBalance(c1, a1.id).balance);

        //note that a1t3_20th is taken from the transfer without conversion
        assertEquals((long) (a1t1_09th.originalFromAmount + r_c1c2_17th * a1t2_17th.fromAmount - a1t3_20th.toAmount + r_c1c2_18th * a1t4_22nd.fromAmount + r_c1c2_18th * a1t5_23rd.fromAmount), c.getAccountBalance(c2, a1.id).balance);

        //no conversion
        assertEquals(a2t1_17th.fromAmount + a2t2_18th.fromAmount + a1t3_20th.toAmount + a1t5_23rd_s2.toAmount, c.getAccountBalance(c2, a2.id).balance);

        //conversion+transfers
        assertEquals((long) (r_c2c1_17th * a2t1_17th.fromAmount + r_c2c1_18th * a2t2_18th.fromAmount - a1t3_20th.fromAmount - a1t5_23rd_s2.fromAmount), c.getAccountBalance(c1, a2.id).balance);

        //conversions
        assertEquals((long) (r_c1c3_5th * (a1t1_09th.fromAmount + a1t2_17th.fromAmount + a1t3_20th.fromAmount + a1t4_22nd.fromAmount + a1t5_23rd.fromAmount)), c.getAccountBalance(c3, a1.id).balance);
        assertEquals((long) (r_c2c3_5th * (a2t1_17th.fromAmount + a2t2_18th.fromAmount + a1t3_20th.toAmount + a1t5_23rd_s2.toAmount)), c.getAccountBalance(c3, a2.id).balance);
    }

    @Test
    public void should_calculate_account_total_in_home_currency_with_big_amounts() {
        TransactionBuilder.withDb(db).account(a1).dateTime(DateTime.date(2012, 1, 10)).amount(45000000000L).create();
        //no conversion
        assertEquals(45000000000L + (long) (-100f + 100f - 50f - 450f - 50f - 150f), c.getAccountBalance(c1, a1.id).balance);
    }

}
