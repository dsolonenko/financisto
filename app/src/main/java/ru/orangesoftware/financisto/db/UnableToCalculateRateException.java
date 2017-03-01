/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.db;

import ru.orangesoftware.financisto.model.Currency;

/**
 * Created by IntelliJ IDEA.
 * User: solomonk
 * Date: 4/24/12
 * Time: 11:06 PM
 */
public class UnableToCalculateRateException extends Exception {

    public final Currency fromCurrency;
    public final Currency toCurrency;
    public final long datetime;

    public UnableToCalculateRateException(Currency fromCurrency, Currency toCurrency, long datetime) {
        this.fromCurrency = fromCurrency;
        this.toCurrency = toCurrency;
        this.datetime = datetime;
    }

}
