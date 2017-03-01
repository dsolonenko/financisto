/*
 * Copyright (c) 2013 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.rates;

import android.util.Log;
import ru.orangesoftware.financisto.http.HttpClientWrapper;
import ru.orangesoftware.financisto.model.Currency;

/**
 * Created with IntelliJ IDEA.
 * User: dsolonenko
 * Date: 2/18/13
 * Time: 9:59 PM
 */
public class FlowzrRateDownloader extends AbstractMultipleRatesDownloader {

    private static final String TAG = FlowzrRateDownloader.class.getSimpleName();

    private final HttpClientWrapper client;
    private final long dateTime;

    public FlowzrRateDownloader(HttpClientWrapper client, long dateTime) {
        this.client = client;
        this.dateTime = dateTime;
    }

    @Override
    public ExchangeRate getRate(Currency fromCurrency, Currency toCurrency) {
        ExchangeRate rate = createRate(fromCurrency, toCurrency);
        try {
            String s = getResponse(fromCurrency, toCurrency);
            rate.rate = Double.parseDouble(s);
            return rate;
        } catch (Exception e) {
            rate.error = "Unable to get exchange rates: "+e.getMessage();
        }
        return rate;
    }

    private ExchangeRate createRate(Currency fromCurrency, Currency toCurrency) {
        ExchangeRate rate = new ExchangeRate();
        rate.fromCurrencyId = fromCurrency.id;
        rate.toCurrencyId = toCurrency.id;
        rate.date = dateTime;
        return rate;
    }

    private String getResponse(Currency fromCurrency, Currency toCurrency) throws Exception {
        String url = buildUrl(fromCurrency, toCurrency);
        Log.i(TAG, url);
        String s = client.getAsStringIfOk(url);
        Log.i(TAG, s);
        return s;
    }

    private String buildUrl(Currency fromCurrency, Currency toCurrency) {
        return "http://flowzr-hrd.appspot.com/?action=currencyRateDownload&from_currency="+fromCurrency.name+"&to_currency="+toCurrency.name;
    }

    @Override
    public ExchangeRate getRate(Currency fromCurrency, Currency toCurrency, long atTime) {
        throw new UnsupportedOperationException("Not supported by Flowzr");
    }

}
