/*
 * Copyright (c) 2013 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.rates;

import android.test.InstrumentationTestCase;
import org.apache.http.conn.ConnectTimeoutException;
import ru.orangesoftware.financisto.http.FakeHttpClientWrapper;
import ru.orangesoftware.financisto.model.Currency;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created with IntelliJ IDEA.
 * User: dsolonenko
 * Date: 2/18/13
 * Time: 10:33 PM
 */
public abstract class AbstractRatesDownloaderTest extends InstrumentationTestCase {

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
        client.error = new ConnectTimeoutException("Timeout");
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
