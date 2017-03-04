/*
 * Copyright (c) 2013 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.rates;

import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;
import ru.orangesoftware.financisto.http.HttpClientWrapper;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.utils.StringUtil;

/**
 * Created with IntelliJ IDEA.
 * User: dsolonenko
 * Date: 2/16/13
 * Time: 6:27 PM
 */
//@NotThreadSafe
public class OpenExchangeRatesDownloader extends AbstractMultipleRatesDownloader {

    private static final String TAG = OpenExchangeRatesDownloader.class.getSimpleName();
    private static final String GET_LATEST = "http://openexchangerates.org/api/latest.json?app_id=";

    private final String appId;
    private final HttpClientWrapper httpClient;

    private JSONObject json;

    public OpenExchangeRatesDownloader(HttpClientWrapper httpClient, String appId) {
        this.httpClient = httpClient;
        this.appId = appId;
    }

    @Override
    public ExchangeRate getRate(Currency fromCurrency, Currency toCurrency) {
        ExchangeRate rate = createRate(fromCurrency, toCurrency);
        try {
            downloadLatestRates();
            if (hasError(json)) {
                rate.error = error(json);
            } else {
                updateRate(json, rate, fromCurrency, toCurrency);
            }
        } catch (Exception e) {
            rate.error = error(e);
        }
        return rate;
    }

    private ExchangeRate createRate(Currency fromCurrency, Currency toCurrency) {
        ExchangeRate r = new ExchangeRate();
        r.fromCurrencyId = fromCurrency.id;
        r.toCurrencyId = toCurrency.id;
        return r;
    }

    private void downloadLatestRates() throws Exception {
        if (json == null) {
            if (appIdIsNotSet()) {
                throw new RuntimeException("App ID is not set");
            }
            Log.i(TAG, "Downloading latest rates...");
            json = httpClient.getAsJson(getLatestUrl());
            Log.i(TAG, json.toString());
        }
    }

    private boolean appIdIsNotSet() {
        return StringUtil.isEmpty(appId);
    }

    private String getLatestUrl() {
        return GET_LATEST+appId;
    }

    private boolean hasError(JSONObject json) throws JSONException {
        return json.optBoolean("error", false);
    }

    private String error(JSONObject json) {
        String status = json.optString("status");
        String message = json.optString("message");
        String description = json.optString("description");
        return status+" ("+message+"): "+description;
    }

    private String error(Exception e) {
        return "Unable to get exchange rates: "+e.getMessage();
    }

    private void updateRate(JSONObject json, ExchangeRate exchangeRate, Currency fromCurrency, Currency toCurrency) throws JSONException {
        JSONObject rates = json.getJSONObject("rates");
        double usdFrom = rates.getDouble(fromCurrency.name);
        double usdTo = rates.getDouble(toCurrency.name);
        exchangeRate.rate = usdTo * (1 / usdFrom);
        exchangeRate.date = 1000*json.optLong("timestamp", System.currentTimeMillis());
    }

    @Override
    public ExchangeRate getRate(Currency fromCurrency, Currency toCurrency, long atTime) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

}
