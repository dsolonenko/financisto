/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */
package ru.orangesoftware.financisto.model;

public class TotalError {

    public static TotalError lastRateError(Currency currency) {
        return new TotalError(currency, System.currentTimeMillis());
    }

    public static TotalError atDateRateError(Currency currency, long datetime) {
        return new TotalError(currency, datetime);
    }

    public final Currency currency;
	public final long datetime;

    private TotalError(Currency currency, long datetime) {
        this.currency = currency;
        this.datetime = datetime;
    }

}
