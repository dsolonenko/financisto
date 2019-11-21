package ru.orangesoftware.financisto.model.rates;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import ru.orangesoftware.financisto.db.AbstractDbTest;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.rates.ExchangeRate;
import ru.orangesoftware.financisto.rates.ExchangeRateProvider;
import ru.orangesoftware.financisto.test.CurrencyBuilder;
import ru.orangesoftware.financisto.test.DateTime;
import ru.orangesoftware.financisto.test.RateBuilder;

import static org.junit.Assert.*;

public class ExchangeRateTest extends AbstractDbTest {

    Currency c1;
    Currency c2;
    Currency c3;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        c1 = CurrencyBuilder.withDb(db).name("USD").title("Dollar").symbol("$").create();
        c2 = CurrencyBuilder.withDb(db).name("EUR").title("Euro").symbol("€").create();
        c3 = CurrencyBuilder.withDb(db).name("RUB").title("Ruble").symbol("p.").create();
    }

    @Test
    public void should_calculate_opposite_rate() {
        ExchangeRate rate = RateBuilder.withDb(db).from(c1).to(c2).at(DateTime.date(2012, 1, 17)).rate(0.78592f).create();
        ExchangeRate flip = rate.flip();
        assertEquals(c2.id, flip.fromCurrencyId);
        assertEquals(c1.id, flip.toCurrencyId);
        assertEquals(rate.date, flip.date);
        assertEquals(1.27239f, flip.rate, 0.00001f);
        ExchangeRate rate1 = flip.flip();
        assertEquals(rate.fromCurrencyId, rate1.fromCurrencyId);
        assertEquals(rate.toCurrencyId, rate1.toCurrencyId);
        assertEquals(rate.date, rate1.date);
        assertEquals(rate.rate, rate1.rate, 0.00001f);
    }

    @Test public void should_reset_time_to_midnight() {
        RateBuilder.withDb(db).from(c1).to(c2).at(DateTime.date(2012, 1, 17).at(12, 23, 45, 456)).rate(0.78592f).create();
        assertEquals(DateTime.date(2012, 1, 17).atMidnight().asLong(), db.findRate(c1, c2, DateTime.date(2012, 1, 17).asLong()).date);
    }

    @Test public void should_insert_currency_rate_for_both_sides() {
        RateBuilder.withDb(db).from(c1).to(c2).at(DateTime.date(2012, 1, 17)).rate(0.78592f).create();
        assertEquals(0.78592f, db.findRate(c1, c2, DateTime.date(2012, 1, 17).asLong()).rate, 0.00001f);
        assertEquals(1.27239f, db.findRate(c2, c1, DateTime.date(2012, 1, 17).asLong()).rate, 0.00001f);
    }

    @Test public void should_update_existing_rate() {
        RateBuilder.withDb(db).from(c1).to(c2).at(DateTime.date(2012, 1, 17)).rate(0.78592f).create();
        assertEquals(0.78592f, db.findRate(c1, c2, DateTime.date(2012, 1, 17).asLong()).rate, 0.00001f);
        assertEquals(1.27239f, db.findRate(c2, c1, DateTime.date(2012, 1, 17).asLong()).rate, 0.00001f);

        //replace rate c1->c2
        RateBuilder.withDb(db).from(c1).to(c2).at(DateTime.date(2012, 1, 17)).rate(0.92785f).create();
        assertEquals(1, db.findRates(c1).size());
        assertEquals(0.92785f, db.findRate(c1, c2, DateTime.date(2012, 1, 17).asLong()).rate, 0.00001f);
        assertEquals(1.07776f, db.findRate(c2, c1, DateTime.date(2012, 1, 17).asLong()).rate, 0.00001f);

        //replace rate c2->c1
        RateBuilder.withDb(db).from(c2).to(c1).at(DateTime.date(2012, 1, 17)).rate(1.5f).create();
        assertEquals(1, db.findRates(c2).size());
        assertEquals(0.66667f, db.findRate(c1, c2, DateTime.date(2012, 1, 17).asLong()).rate, 0.00001f);
        assertEquals(1.5f, db.findRate(c2, c1, DateTime.date(2012, 1, 17).asLong()).rate, 0.00001f);
    }

    @Test public void should_save_multiple_rates() {
        RateBuilder.withDb(db).from(c1).to(c2).at(DateTime.date(2012, 1, 17)).rate(0.78592f).create();
        RateBuilder.withDb(db).from(c1).to(c2).at(DateTime.date(2012, 1, 18)).rate(0.78635f).create();

        List<ExchangeRate> rates = db.findRates(c1);
        assertEquals(2, rates.size());

        assertEquals(0.78635f, rates.get(0).rate, 0.00001f);
        assertEquals(0.78592f, rates.get(1).rate, 0.00001f);

        rates = db.findRates(c2, c1);
        assertEquals(2, rates.size());

        assertEquals(1.0f / 0.78635f, rates.get(0).rate, 0.00001f);
        assertEquals(1.0f / 0.78592f, rates.get(1).rate, 0.00001f);
    }

    @Test public void should_delete_rate() {
        ExchangeRate r = RateBuilder.withDb(db).from(c1).to(c2).at(DateTime.date(2012, 1, 17)).rate(0.78592f).create();
        RateBuilder.withDb(db).from(c1).to(c2).at(DateTime.date(2012, 1, 18)).rate(0.78635f).create();

        assertEquals(2, db.findRates(c1).size());
        assertEquals(2, db.findRates(c2).size());

        db.deleteRate(r);
        assertEquals(1, db.findRates(c1).size());
        assertEquals(1, db.findRates(c2).size());

        ExchangeRateProvider rates = db.getLatestRates();
        assertEquals(0.78635f, rates.getRate(c1, c2, DateTime.date(2012, 1, 20).asLong()).rate, 0.00001f);
    }

    @Test public void should_replace_rate() {
        RateBuilder.withDb(db).from(c1).to(c2).at(DateTime.date(2012, 1, 17)).rate(0.78592f).create();

        assertEquals(1, db.findRates(c1).size());
        assertEquals(1, db.findRates(c2).size());
        assertEquals(0.78592f, db.findRate(c1, c2, DateTime.date(2012, 1, 17).asLong()).rate, 0.00001f);
        assertEquals(1f / 0.78592f, db.findRate(c2, c1, DateTime.date(2012, 1, 17).asLong()).rate, 0.00001f);

        ExchangeRate rate = RateBuilder.inMemory().from(c1).to(c2).at(DateTime.date(2012, 1, 18)).rate(0.888f).create();
        db.replaceRate(rate, DateTime.date(2012, 1, 17).asLong());

        assertEquals(1, db.findRates(c1).size());
        assertEquals(1, db.findRates(c2).size());
        assertEquals(0.888f, db.findRate(c1, c2, DateTime.date(2012, 1, 18).asLong()).rate, 0.00001f);
        assertEquals(1f / 0.888f, db.findRate(c2, c1, DateTime.date(2012, 1, 18).asLong()).rate, 0.00001f);
    }

    @Test public void should_save_downloaded_rates() {
        //given
        List<ExchangeRate> downloadedRates = Arrays.asList(
                RateBuilder.inMemory().from(c1).to(c2).at(DateTime.date(2012, 1, 18)).rate(0.1f).create(),
                RateBuilder.inMemory().from(c1).to(c3).at(DateTime.date(2012, 1, 18)).rate(0.2f).create(),
                RateBuilder.inMemory().from(c2).to(c3).at(DateTime.date(2012, 1, 18)).rate(0.3f).create(),
                RateBuilder.inMemory().from(c2).to(c3).at(DateTime.date(2012, 1, 19)).rate(0.3f).notOK().create()
        );
        //when
        db.saveDownloadedRates(downloadedRates);
        //then
        assertEquals(2, db.findRates(c1).size());
        assertEquals(2, db.findRates(c2).size());
        assertEquals(2, db.findRates(c3).size());
    }

}
