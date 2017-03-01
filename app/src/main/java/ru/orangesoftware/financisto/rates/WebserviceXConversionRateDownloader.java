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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: dsolonenko
 * Date: 2/18/13
 * Time: 9:59 PM
 */
public class WebserviceXConversionRateDownloader extends AbstractMultipleRatesDownloader {

    private static final String TAG = WebserviceXConversionRateDownloader.class.getSimpleName();

    private final Pattern pattern = Pattern.compile("<double.*?>(.+?)</double>");
    private final HttpClientWrapper client;
    private final long dateTime;

    public WebserviceXConversionRateDownloader(HttpClientWrapper client, long dateTime) {
        this.client = client;
        this.dateTime = dateTime;
    }

    @Override
    public ExchangeRate getRate(Currency fromCurrency, Currency toCurrency) {
        ExchangeRate rate = createRate(fromCurrency, toCurrency);
        try {
            String s = getResponse(fromCurrency, toCurrency);
            Matcher m = pattern.matcher(s);
            if (m.find()) {
                rate.rate = parseRate(m.group(1));
            } else {
                rate.error = parseError(s);
            }
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
        String s = client.getAsString(url);
        Log.i(TAG, s);
        return s;
    }

    private double parseRate(String s) {
        return Double.parseDouble(s);
    }

    private String parseError(String s) {
        String[] x = s.split("\r\n");
        String error = "Service is not available, please try again later";
        if (x.length > 0) {
            error = "Something wrong with the exchange rates provider. Response from the service - "+x[0];
        }
        return error;
    }

    private String buildUrl(Currency fromCurrency, Currency toCurrency) {
        return "http://www.webservicex.net/CurrencyConvertor.asmx/ConversionRate?FromCurrency="+fromCurrency.name+"&ToCurrency="+toCurrency.name;
    }

    @Override
    public ExchangeRate getRate(Currency fromCurrency, Currency toCurrency, long atTime) {
        throw new UnsupportedOperationException("Not supported by WebserviceX.NET");
    }

}
