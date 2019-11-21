package ru.orangesoftware.financisto.model.rates;

import org.junit.Test;

import ru.orangesoftware.financisto.db.AbstractDbTest;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.Total;
import ru.orangesoftware.financisto.rates.ExchangeRate;
import ru.orangesoftware.financisto.rates.ExchangeRateProvider;
import ru.orangesoftware.financisto.test.AccountBuilder;
import ru.orangesoftware.financisto.test.CurrencyBuilder;
import ru.orangesoftware.financisto.test.DateTime;
import ru.orangesoftware.financisto.test.RateBuilder;

import static ru.orangesoftware.financisto.model.rates.AssertExchangeRate.assertRate;
import static org.junit.Assert.*;

public class LatestExchangeRatesTest extends AbstractDbTest {

    Currency c1;
    Currency c2;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        c1 = CurrencyBuilder.withDb(db).name("USD").title("Dollar").symbol("$").create();
        c2 = CurrencyBuilder.withDb(db).name("EUR").title("Euro").symbol("€").create();
    }

    @Test
    public void should_find_the_most_actual_rate_for_every_currency() {
        Currency c1 = CurrencyBuilder.withDb(db).name("USD").title("Dollar").symbol("$").create();
        Currency c2 = CurrencyBuilder.withDb(db).name("EUR").title("Euro").symbol("€").create();
        Currency c3 = CurrencyBuilder.withDb(db).name("SGD").title("Singapore Dollar").symbol("S$").create();

        RateBuilder.withDb(db).from(c1).to(c2).at(DateTime.date(2012, 1, 17)).rate(0.78592f).create();
        RateBuilder.withDb(db).from(c1).to(c2).at(DateTime.date(2012, 1, 18)).rate(0.78635f).create();

        RateBuilder.withDb(db).from(c1).to(c3).at(DateTime.date(2012, 1, 15)).rate(0.111f).create();

        RateBuilder.withDb(db).from(c2).to(c3).at(DateTime.date(2012, 1, 16)).rate(0.222f).create();
        RateBuilder.withDb(db).from(c2).to(c3).at(DateTime.date(2012, 1, 14)).rate(0.333f).create();

        ExchangeRateProvider m = db.getLatestRates();

        ExchangeRate rate = m.getRate(c1, c2);
        assertRate(DateTime.date(2012, 1, 18), 0.78635f, rate);

        rate = m.getRate(c2, c1);
        assertRate(DateTime.date(2012, 1, 18), 1.0f / 0.78635f, rate);

        rate = m.getRate(c1, c3);
        assertRate(DateTime.date(2012, 1, 15), 0.111f, rate);

        rate = m.getRate(c2, c3);
        assertRate(DateTime.date(2012, 1, 16), 0.222f, rate);

        rate = m.getRate(c3, c2);
        assertRate(DateTime.date(2012, 1, 16), 1.0f / 0.222f, rate);
    }

    @Test public void should_return_error_if_rate_is_not_found() {
        ExchangeRateProvider m = db.getLatestRates();
        ExchangeRate rate = m.getRate(c1, c2);
        assertTrue(ExchangeRate.NA == rate);
    }

    @Test public void should_calculate_accounts_total_in_home_currency() {
        AccountBuilder.withDb(db).title("Cash").currency(c1).total(500).create();
        AccountBuilder.withDb(db).title("Bank").currency(c2).total(1200).create();
        RateBuilder.withDb(db).from(c1).to(c2).at(DateTime.date(2012, 1, 17)).rate(0.78592f).create();
        RateBuilder.withDb(db).from(c1).to(c2).at(DateTime.date(2012, 1, 18)).rate(0.78635f).create();

        // total in c1
        assertEquals((long) (500 + (1.0f / 0.78635f) * 1200), db.getAccountsTotal(c1).balance);

        // total in c2
        assertEquals((long) (1200 + (0.78635f) * 500), db.getAccountsTotal(c2).balance);

        // total in c3
        Currency c3 = CurrencyBuilder.withDb(db).name("SGD").title("Singapore Dollar").symbol("S$").create();
        assertTrue(db.getAccountsTotal(c3).isError());
    }

    @Test public void should_calculate_accounts_total_in_every_currency() {
        AccountBuilder.withDb(db).title("Cash1").currency(c1).total(500).create();

        Total[] totals = db.getAccountsTotal();
        assertTotal(totals, c1, 500);

        AccountBuilder.withDb(db).title("Bank1").currency(c2).total(-200).create();

        totals = db.getAccountsTotal();
        assertTotal(totals, c1, 500);
        assertTotal(totals, c2, -200);

        AccountBuilder.withDb(db).title("Cash2").currency(c1).total(400).create();
        AccountBuilder.withDb(db).title("Bank2").currency(c2).total(-100).create();

        totals = db.getAccountsTotal();
        assertTotal(totals, c1, 900);
        assertTotal(totals, c2, -300);
    }

    private void assertTotal(Total[] totals, Currency currency, long amount) {
        for (Total total : totals) {
            if (total.currency.id == currency.id) {
                assertEquals(amount, total.balance);
                return;
            }
        }
        fail("Unable to find total for " + currency);
    }

    @Test public void should_calculate_accounts_total_correctly_with_big_amounts() {
        AccountBuilder.withDb(db).title("Cash").currency(c1).total(36487931200L).create();
        assertEquals(36487931200L, db.getAccountsTotal(c1).balance);
    }

}
