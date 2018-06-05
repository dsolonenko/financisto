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
import ru.orangesoftware.financisto.export.qif.QifImport;
import ru.orangesoftware.financisto.export.qif.QifImportOptions;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.AccountType;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.TransactionInfo;
import ru.orangesoftware.financisto.test.DateTime;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static ru.orangesoftware.financisto.export.qif.QifDateFormat.EU_FORMAT;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 10/17/11 9:19 PM
 */
public class QifImportTestCases extends AbstractDbTest {

    QifImport qifImport;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        db.db().execSQL("insert into currency(_id,title,name,symbol) values(0,'Default','?','$')");
    }

    public void test_should_import_homebank_case_1() throws Exception {
        doImport("!Account\n" +
                "NMy Bank Account\n" +
                "^\n" +
                "!Type:Bank\n" +
                "D02/11/2011\n" +
                "T14.00\n" +
                "C\n" +
                "PP2\n" +
                "M(null)\n" +
                "LC2\n" +
                "^\n" +
                "D01/11/2011\n" +
                "T-35.40\n" +
                "C\n" +
                "P\n" +
                "M(null)\n" +
                "L[My Cash Account]\n" +
                "^\n" +
                "!Account\n" +
                "NMy Cash Account\n" +
                "^\n" +
                "!Type:Cash\n" +
                "D03/11/2011\n" +
                "T19.50\n" +
                "C\n" +
                "PP1\n" +
                "M(null)\n" +
                "LC1:c1\n" +
                "^\n" +
                "D01/11/2011\n" +
                "T35.40\n" +
                "C\n" +
                "P\n" +
                "M(null)\n" +
                "L[My Bank Account]\n" +
                "^");

        List<Account> accounts = db.getAllAccountsList();
        assertEquals(2, accounts.size());

        // as default sortOrder for Account is desc so accounts are retrived in the opposite order
        Account a = accounts.get(1); 
        assertEquals("My Bank Account", a.title);
        assertEquals(AccountType.BANK.name(), a.type);
        assertAccountTotal(a, -2140);
        assertFinalBalanceForAccount(a, -2140);

        List<TransactionInfo> transactions = db.getTransactionsForAccount(a.id);
        assertEquals(2, transactions.size());

        TransactionInfo t = transactions.get(0);
        assertEquals(DateTime.date(2011, 11, 2).atMidnight().asLong(), t.dateTime);
        assertEquals(1400, t.fromAmount);
        assertEquals("P2", t.payee.title);
        assertEquals("C2", t.category.title);

        t = transactions.get(1);
        assertTrue("Should be a transfer from bank to cash", t.isTransfer());
        assertEquals(DateTime.date(2011, 11, 1).atMidnight().asLong(), t.dateTime);
        assertEquals("My Bank Account", t.fromAccount.title);
        assertEquals(-3540, t.fromAmount);
        assertEquals("My Cash Account", t.toAccount.title);
        assertEquals(3540, t.toAmount);

        a = accounts.get(0);
        assertEquals("My Cash Account", a.title);
        assertEquals(AccountType.CASH.name(), a.type);
        assertAccountTotal(a, 5490);
        assertFinalBalanceForAccount(a, 5490);

        transactions = db.getTransactionsForAccount(a.id);
        assertEquals(1, transactions.size());

        t = transactions.get(0);
        assertEquals(DateTime.date(2011, 11, 3).atMidnight().asLong(), t.dateTime);
        assertEquals(1950, t.fromAmount);
        assertEquals("P1", t.payee.title);
        assertEquals("c1", t.category.title);
    }

    public void test_should_import_financisto_qif_export_case_1() throws IOException {
        doImport(
            "!Type:Cat\n" +
            "NA1\n" +
            "E\n" +
            "^\n" +
            "NA1:aa1\n" +
            "E\n" +
            "^\n" +
            "NA1:aa1:aaa1\n" +
            "E\n" +
            "^\n" +
            "NA1:aa2\n" +
            "E\n" +
            "^\n" +
            "NB1\n" +
            "I\n" +
            "^\n" +
            "NB1:bb1\n" +
            "I\n" +
            "^\n" +
            "NC1\n" +
            "E\n" +
            "^\n" +
            "ND1\n" +
            "E\n" +
            "^\n" +
            "!Account\n" +
            "NAAA\n" +
            "TCash\n" +
            "^\n" +
            "!Type:Cash\n" +
            "D17/02/2011\n" +
            "T15.00\n" +
            "L[BBB]\n" +
            "^\n" +
            "D17/02/2011\n" +
            "T-10.00\n" +
            "LA1\n" +
            "Pp1\n" +
            "^\n" +
            "D17/02/2011\n" +
            "T100.00\n" +
            "MOpening amount (AAA)\n" +
            "^\n" +
            "!Account\n" +
            "NBBB\n" +
            "TCash\n" +
            "^\n" +
            "!Type:Cash\n" +
            "D17/02/2011\n" +
            "T-14.00\n" +
            "LB1:bb1\n" +
            "^\n" +
            "D17/02/2011\n" +
            "T-15.00\n" +
            "L[AAA]\n" +
            "^\n" +
            "D17/02/2011\n" +
            "T-16.50\n" +
            "Pp2\n" +
            "^\n" +
            "D17/02/2011\n" +
            "T40.00\n" +
            "MOpening amount (BBB)\n" +
            "^");

        List<Account> accounts = db.getAllAccountsList();
        assertEquals(2, accounts.size());

        Account a = accounts.get(0);
        assertEquals("BBB", a.title); // as default sortOrder for Account is desc
        assertAccountTotal(a, -550);

        a = accounts.get(1);
        assertEquals("AAA", a.title);
        assertAccountTotal(a, 10500);
    }

    private void sortAccountsById(List<Account> accounts) {
        Collections.sort(accounts, (a1, a2) -> Long.compare(a1.id, a2.id));
    }

    private void doImport(String qif) throws IOException {
        File tmp = File.createTempFile("backup", ".qif");
        FileWriter w = new FileWriter(tmp);
        w.write(qif);
        w.close();
        Log.d("Financisto", "Created a temporary backup file: "+tmp.getAbsolutePath());
        QifImportOptions options = new QifImportOptions(tmp.getAbsolutePath(), EU_FORMAT, Currency.EMPTY);
        qifImport = new QifImport(getContext(), db, options);
        qifImport.importDatabase();
    }

}
