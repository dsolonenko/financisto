/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.export;

import android.util.Log;
import ru.orangesoftware.financisto.db.AbstractDbTest;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.AccountType;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.test.CurrencyBuilder;

import java.io.ByteArrayOutputStream;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 2/4/11 10:23 PM
 */
public abstract class AbstractImportExportTest extends AbstractDbTest {

    protected Account createFirstAccount() {
        Currency c = createCurrency("SGD");
        Account a = new Account();
        a.title = "My Cash Account";
        a.type = AccountType.CASH.name();
        a.currency = c;
        a.totalAmount = 10000;
        a.sortOrder = 100;
        a.note = "AAA\nBBB:CCC";
        em.saveAccount(a);
        assertNotNull(em.load(Account.class, a.id));
        return a;
    }

    protected Account createSecondAccount() {
        Currency c = createCurrency("CZK");
        Account a = new Account();
        a.title = "My Bank Account";
        a.type = AccountType.BANK.name();
        a.currency = c;
        a.totalAmount = 23450;
        a.sortOrder = 50;
        em.saveAccount(a);
        assertNotNull(em.load(Account.class, a.id));
        return a;
    }

    private Currency createCurrency(String currency) {
        Currency c = CurrencyBuilder.withDb(db)
                .title("Singapore Dollar")
                .name(currency)
                .separators("''", "'.'")
                .symbol("S$")
                .create();
        assertNotNull(em.load(Currency.class, c.id));
        return c;
    }

}
