package ru.orangesoftware.financisto.rates;

import org.apache.http.conn.ConnectTimeoutException;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import ru.orangesoftware.financisto.http.FakeHttpClientWrapper;
import ru.orangesoftware.financisto.model.Currency;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public abstract class AbstractRatesDownloaderTest {

    private final Map<String, Currency> nameToCurrency = new HashMap<String, Currency>();
    private final AtomicLong counter = new AtomicLong(1);

    FakeHttpClientWrapper client = new FakeHttpClientWrapper();

    abstract ExchangeRateProvider service();

    ExchangeRate downloadRate(String from, String to) {
        return service().getRate(currency(from), currency(to));
    }

    Currency currency(String name) {
        Currency c = nameToCurrency.get(name);
        if (c == null) {
            c = new Currency();
            c.id = counter.getAndIncrement();
            c.name = name;
            nameToCurrency.put(name, c);
        }
        return c;
    }

    List<Currency> currencies(String... currencies) {
        List<Currency> list = new ArrayList<Currency>();
        for (String name : currencies) {
            list.add(currency(name));
        }
        return list;
    }

    void givenResponseFromWebService(String url, String response) {
        client.givenResponse(url, response);
    }

    void givenExceptionWhileRequestingWebService() {
        client.error = new Exception("Timeout");
    }

    void assertRate(ExchangeRate exchangeRate, String fromCurrency, String toCurrency) {
        assertEquals("Expected "+fromCurrency, currency(fromCurrency).id, exchangeRate.fromCurrencyId);
        assertEquals("Expected "+toCurrency, currency(toCurrency).id, exchangeRate.toCurrencyId);
    }

    void assertRate(ExchangeRate exchangeRate, String fromCurrency, String toCurrency, double rate, long date) {
        assertRate(exchangeRate, fromCurrency, toCurrency);
        assertEquals(rate, exchangeRate.rate, 0.000001);
        assertEquals(date, exchangeRate.date);
    }

    static String anyUrl() {
        return "*";
    }

}
