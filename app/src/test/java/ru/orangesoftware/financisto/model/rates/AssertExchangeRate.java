package ru.orangesoftware.financisto.model.rates;

import ru.orangesoftware.financisto.db.AbstractDbTest;
import ru.orangesoftware.financisto.rates.ExchangeRate;
import ru.orangesoftware.financisto.test.DateTime;

import static org.junit.Assert.*;

public abstract class AssertExchangeRate extends AbstractDbTest {

    public static void assertRate(DateTime date, double rate, ExchangeRate r) {
        assertEquals(rate, r.rate, 0.00001d);
        assertEquals(date.atMidnight().asLong(), r.date);
    }

}
