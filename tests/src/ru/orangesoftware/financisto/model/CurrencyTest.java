package ru.orangesoftware.financisto.model;

import ru.orangesoftware.financisto.db.AbstractDbTest;
import ru.orangesoftware.financisto.test.AccountBuilder;
import ru.orangesoftware.financisto.test.CurrencyBuilder;
import ru.orangesoftware.financisto.utils.Utils;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 3/16/11 9:23 PM
 */
public class CurrencyTest extends AbstractDbTest {

    public void test_should_format_amount_according_to_the_selected_currency() {
        Currency c = new Currency();
        c.decimals = 1;
        c.decimalSeparator = "','";
        c.groupSeparator = "''";
        c.symbol = "$";
        String actualString = Utils.amountToString(c, 100000);
        assertEquals("1000,0 $", actualString);
    }

    public void test_should_format_symbol_according_to_the_selected_format() {
        //given
        Currency c = new Currency();
        c.decimals = 1;
        c.decimalSeparator = "'.'";
        c.groupSeparator = "','";
        c.symbol = "$";
        //when
        c.symbolFormat = SymbolFormat.RS;
        assertEquals("1,000.0 $", Utils.amountToString(c, 100000));
        assertEquals("+1,000.0 $", Utils.amountToString(c, 100000, true));
        assertEquals("-1,000.0 $", Utils.amountToString(c, -100000));
        //when
        c.symbolFormat = SymbolFormat.R;
        assertEquals("1,000.0$", Utils.amountToString(c, 100000));
        assertEquals("+1,000.0$", Utils.amountToString(c, 100000, true));
        assertEquals("-1,000.0$", Utils.amountToString(c, -100000));
        //when
        c.symbolFormat = SymbolFormat.LS;
        assertEquals("$ 1,000.0", Utils.amountToString(c, 100000));
        assertEquals("+$ 1,000.0", Utils.amountToString(c, 100000, true));
        assertEquals("-$ 1,000.0", Utils.amountToString(c, -100000));
        //when
        c.symbolFormat = SymbolFormat.L;
        assertEquals("$1,000.0", Utils.amountToString(c, 100000));
        assertEquals("+$1,000.0", Utils.amountToString(c, 100000, true));
        assertEquals("-$1,000.0", Utils.amountToString(c, -100000));
    }

    public void test_should_reset_default_flag() {
        Currency c1 = CurrencyBuilder.withDb(db).name("USD").title("Dollar").symbol("$").makeDefault().create();
        assertTrue(em.get(Currency.class, c1.id).isDefault);
        Currency c2 = CurrencyBuilder.withDb(db).name("SGD").title("Singapore Dollar").symbol("S$").makeDefault().create();
        //There can be only one!
        assertFalse(em.get(Currency.class, c1.id).isDefault);
        assertTrue(em.get(Currency.class, c2.id).isDefault);
    }

    public void test_should_return_home_currency() {
        CurrencyBuilder.withDb(db).name("USD").title("Dollar").symbol("$").makeDefault().create();
        CurrencyBuilder.withDb(db).name("SGD").title("Singapore Dollar").symbol("S$").makeDefault().create();
        assertEquals("SGD", em.getHomeCurrency().name);
    }

    public void test_should_return_empty_currency_if_home_is_not_set() {
        CurrencyBuilder.withDb(db).name("USD").title("Dollar").symbol("$").create();
        assertEquals("", em.getHomeCurrency().name);
    }

    public void test_should_return_empty_currency_if_there_are_no_currencies() {
        assertEquals("", em.getHomeCurrency().name);
    }

    public void test_should_set_home_currency_if_it_has_not_been_set_and_the_same_currency_is_used_in_all_accounts() {
        CurrencyBuilder.withDb(db).name("USD").title("Dollar").symbol("$").create();
        Currency c = CurrencyBuilder.withDb(db).name("SGD").title("Singapore Dollar").symbol("S$").create();
        AccountBuilder.createDefault(db, c);
        assertEquals(Currency.EMPTY, em.getHomeCurrency());
        db.setDefaultHomeCurrency();
        assertEquals(c, em.getHomeCurrency());
    }

}
