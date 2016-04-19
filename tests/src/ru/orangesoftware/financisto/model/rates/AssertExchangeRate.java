/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.model.rates;

import ru.orangesoftware.financisto.db.AbstractDbTest;
import ru.orangesoftware.financisto.rates.ExchangeRate;
import ru.orangesoftware.financisto.test.DateTime;

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 1/30/12 8:57 PM
 */
public abstract class AssertExchangeRate extends AbstractDbTest {

    public static void assertRate(DateTime date, double rate, ExchangeRate r) {
        assertEquals(rate, r.rate, 0.00001d);
        assertEquals(date.atMidnight().asLong(), r.date);
    }

}
