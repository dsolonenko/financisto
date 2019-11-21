package ru.orangesoftware.financisto.model.rates;

import org.junit.Test;

import ru.orangesoftware.financisto.db.AbstractDbTest;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.rates.ExchangeRate;
import ru.orangesoftware.financisto.rates.ExchangeRateProvider;
import ru.orangesoftware.financisto.test.CurrencyBuilder;
import ru.orangesoftware.financisto.test.DateTime;
import ru.orangesoftware.financisto.test.RateBuilder;

import static ru.orangesoftware.financisto.model.rates.AssertExchangeRate.assertRate;
import static org.junit.Assert.*;

public class HistoryExchangeRatesTest extends AbstractDbTest {

    Currency c1;
    Currency c2;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        c1 = CurrencyBuilder.withDb(db).name("USD").title("Dollar").symbol("$").create();
        c2 = CurrencyBuilder.withDb(db).name("EUR").title("Euro").symbol("€").create();
    }

    @Test
    public void should_get_rates_for_every_date() {
        RateBuilder.withDb(db).from(c1).to(c2).at(DateTime.date(2012, 1, 7)).rate(0.78592f).create();
        RateBuilder.withDb(db).from(c1).to(c2).at(DateTime.date(2012, 1, 18)).rate(0.78654f).create();
        RateBuilder.withDb(db).from(c1).to(c2).at(DateTime.date(2012, 1, 20)).rate(0.78712f).create();

        ExchangeRateProvider rates = db.getHistoryRates();

        ExchangeRate rate = rates.getRate(c1, c2);
        assertRate(DateTime.date(2012, 1, 20), 0.78712f, rate);

        rate = rates.getRate(c1, c2, DateTime.date(2012, 1, 7).atMidnight().asLong());
        assertRate(DateTime.date(2012, 1, 7), 0.78592f, rate);

        rate = rates.getRate(c1, c2, DateTime.date(2012, 1, 7).at(15, 30, 25, 0).asLong());
        assertRate(DateTime.date(2012, 1, 7), 0.78592f, rate);

        rate = rates.getRate(c1, c2, DateTime.date(2012, 1, 18).at(0, 10, 25, 0).asLong());
        assertRate(DateTime.date(2012, 1, 18), 0.78654f, rate);

        rate = rates.getRate(c1, c2, DateTime.date(2012, 1, 19).at(23, 59, 59, 0).asLong());
        assertRate(DateTime.date(2012, 1, 18), 0.78654f, rate);

        rate = rates.getRate(c1, c2, DateTime.date(2012, 1, 21).at(23, 59, 59, 0).asLong());
        assertRate(DateTime.date(2012, 1, 20), 0.78712f, rate);

        rate = rates.getRate(c2, c1, DateTime.date(2012, 1, 21).at(23, 59, 59, 0).asLong());
        assertRate(DateTime.date(2012, 1, 20), 1.0f / 0.78712f, rate);
    }

    @Test public void should_return_error_non_existing_dates() {
        RateBuilder.withDb(db).from(c1).to(c2).at(DateTime.date(2012, 1, 18)).rate(0.78654f).create();
        RateBuilder.withDb(db).from(c1).to(c2).at(DateTime.date(2012, 1, 19)).rate(0.78712f).create();

        ExchangeRateProvider rates = db.getHistoryRates();
        ExchangeRate rate = rates.getRate(c1, c2, DateTime.date(2012, 1, 7).atMidnight().asLong());
        assertTrue(ExchangeRate.NA == rate);

        // default rate should be cached
        ExchangeRate rate2 = rates.getRate(c1, c2, DateTime.date(1979, 8, 2).atMidnight().asLong());
        assertTrue(ExchangeRate.NA == rate2);
    }

}
