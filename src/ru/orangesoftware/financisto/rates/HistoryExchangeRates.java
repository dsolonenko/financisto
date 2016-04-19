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

import java.util.*;

/**
 * Not thread safe
 *
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 1/30/12 7:54 PM
 */
public class HistoryExchangeRates implements ExchangeRateProvider, ExchangeRatesCollection {

    private static final ExchangeRate r = new ExchangeRate();
    
    private final TLongObjectMap<TLongObjectMap<SortedSet<ExchangeRate>>> rates = new TLongObjectHashMap<TLongObjectMap<SortedSet<ExchangeRate>>>();

    @Override
    public void addRate(ExchangeRate r) {
        SortedSet<ExchangeRate> s = getRates(r.fromCurrencyId, r.toCurrencyId);
        s.add(r);
    }

    @Override
    public ExchangeRate getRate(Currency fromCurrency, Currency toCurrency) {
        SortedSet<ExchangeRate> s = getRates(fromCurrency.id, toCurrency.id);
        return s.first();
    }

    @Override
    public ExchangeRate getRate(Currency fromCurrency, Currency toCurrency, long atTime) {
        SortedSet<ExchangeRate> s = getRates(fromCurrency.id, toCurrency.id);
        r.date = atTime;
        // s.tailSet(r) still creates a new TreeSet object
        SortedSet<ExchangeRate> rates = s.tailSet(r);
        if (rates.isEmpty()) {
            ExchangeRate defaultRate = ExchangeRate.NA;
            s.add(defaultRate);
            return defaultRate;
        }
        return rates.first();
    }

    @Override
    public List<ExchangeRate> getRates(List<Currency> currencies) {
        throw new UnsupportedOperationException();
    }

    private SortedSet<ExchangeRate> getRates(long fromCurrencyId, long toCurrencyId) {
        TLongObjectMap<SortedSet<ExchangeRate>> map = getMapFor(fromCurrencyId);
        return getSetFor(map, toCurrencyId);
    }

    private TLongObjectMap<SortedSet<ExchangeRate>> getMapFor(long fromCurrencyId) {
        TLongObjectMap<SortedSet<ExchangeRate>> m = rates.get(fromCurrencyId);
        if (m == null) {
            m = new TLongObjectHashMap<SortedSet<ExchangeRate>>();
            rates.put(fromCurrencyId, m);
        }
        return m;
    }
    
    private SortedSet<ExchangeRate> getSetFor(TLongObjectMap<SortedSet<ExchangeRate>> rates, long date) {
        SortedSet<ExchangeRate> s = rates.get(date);
        if (s == null) {
            s = new TreeSet<ExchangeRate>();
            rates.put(date, s);
        }
        return s;
    }

}
