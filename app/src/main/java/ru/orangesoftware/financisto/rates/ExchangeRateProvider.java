/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.rates;

import java.util.List;

import ru.orangesoftware.financisto.model.Currency;

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 1/30/12 7:46 PM
 */
public interface ExchangeRateProvider {

    ExchangeRate getRate(Currency fromCurrency, Currency toCurrency);

    ExchangeRate getRate(Currency fromCurrency, Currency toCurrency, long atTime);

    List<ExchangeRate> getRates(List<Currency> currencies);

}
