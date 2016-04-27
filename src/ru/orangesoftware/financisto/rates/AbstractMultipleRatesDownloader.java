/*
 * Copyright (c) 2013 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.rates;

import ru.orangesoftware.financisto.model.Currency;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: dsolonenko
 * Date: 2/20/13
 * Time: 10:23 PM
 */
public abstract class AbstractMultipleRatesDownloader implements ExchangeRateProvider {

    @Override
    public List<ExchangeRate> getRates(List<Currency> currencies) {
        List<ExchangeRate> rates = new ArrayList<ExchangeRate>();
        int count = currencies.size();
        for (int i = 0; i < count; i++) {
            for (int j = i + 1; j < count; j++) {
                Currency fromCurrency = currencies.get(i);
                Currency toCurrency = currencies.get(j);
                rates.add(getRate(fromCurrency, toCurrency));
            }
        }
        return rates;
    }

}
