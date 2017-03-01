/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.rates;

import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import ru.orangesoftware.financisto.model.Currency;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 1/25/12 11:49 PM
 */
public class LatestExchangeRates implements ExchangeRateProvider, ExchangeRatesCollection {

    private final TLongObjectMap<TLongObjectMap<ExchangeRate>> rates = new TLongObjectHashMap<TLongObjectMap<ExchangeRate>>();

    @Override
    public ExchangeRate getRate(Currency fromCurrency, Currency toCurrency) {
        if (fromCurrency.id == toCurrency.id) {
            return ExchangeRate.ONE;
        }
        TLongObjectMap<ExchangeRate> rateMap = getMapFor(fromCurrency.id);
        ExchangeRate rate = rateMap.get(toCurrency.id);
        if (rate == null) {
            rate = ExchangeRate.NA;
            rateMap.put(toCurrency.id, rate);
        }
        return rate;
    }

    @Override
    public ExchangeRate getRate(Currency fromCurrency, Currency toCurrency, long atTime) {
        return getRate(fromCurrency, toCurrency);
    }

    @Override
    public List<ExchangeRate> getRates(List<Currency> currencies) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addRate(ExchangeRate r) {
        TLongObjectMap<ExchangeRate> rateMap = getMapFor(r.fromCurrencyId);
        rateMap.put(r.toCurrencyId, r);
    }

    private TLongObjectMap<ExchangeRate> getMapFor(long fromCurrencyId) {
        TLongObjectMap<ExchangeRate> m = rates.get(fromCurrencyId);
        if (m == null) {
            m = new TLongObjectHashMap<ExchangeRate>();
            rates.put(fromCurrencyId, m);
        }
        return m;
    }

}
