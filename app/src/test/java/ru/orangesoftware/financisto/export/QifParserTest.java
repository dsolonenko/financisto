/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.export;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static ru.orangesoftware.financisto.export.qif.QifDateFormat.EU_FORMAT;
import static ru.orangesoftware.financisto.export.qif.QifDateFormat.US_FORMAT;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import ru.orangesoftware.financisto.export.qif.QifAccount;
import ru.orangesoftware.financisto.export.qif.QifBufferedReader;
import ru.orangesoftware.financisto.export.qif.QifCategory;
import ru.orangesoftware.financisto.export.qif.QifDateFormat;
import ru.orangesoftware.financisto.export.qif.QifParser;
import ru.orangesoftware.financisto.export.qif.QifTransaction;
import ru.orangesoftware.financisto.test.DateTime;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 9/25/11 9:53 PM
 */
public class QifParserTest {

    QifParser p;

    @Test
    public void should_parse_empty_file() throws IOException {
        parseQif("");
    }

    @Test
    public void should_parse_empty_account() throws IOException {
        parseQif(
                "!Account\n" +
                "NMy Cash Account\n" +
                "TCash\n" +
                "^\n");
        assertEquals(1, p.accounts.size());
        assertEquals("My Cash Account", p.accounts.get(0).memo);
        assertEquals("Cash", p.accounts.get(0).type);
    }

    @Test
    public void should_parse_a_couple_of_empty_accounts() throws IOException {
        parseQif(
                "!Account\n" +
                "NMy Cash Account\n" +
                "TCash\n" +
                "^\n" +
                "!Account\n" +
                "NMy Bank Account\n" +
                "TBank\n" +
                "^\n");
        assertEquals(2, p.accounts.size());
        assertEquals("My Cash Account", p.accounts.get(0).memo);
        assertEquals("Cash", p.accounts.get(0).type);
        assertEquals("My Bank Account", p.accounts.get(1).memo);
        assertEquals("Bank", p.accounts.get(1).type);
    }

    @Test
    public void should_parse_account_with_a_couple_of_transactions() throws Exception {
        parseQif(
                "!Type:Cat\n" +
                "NP1\n" +
                "E\n" +
                "^\n" +
                "NP1:c1\n" +
                "E\n" +
                "^\n" +
                "NP2\n" +
                "I\n" +
                "^\n" +
                "!Account\n" +
                "NMy Cash Account\n" +
                "TCash\n" +
                "^\n" +
                "!Type:Cash\n" +
                "D08/02/2011\n" +
                "T10.00\n" +
                "LP1\n" +
                "^\n" +
                "D07/02/2011\n" +
                "T-20.56\n" +
                "LP1:c1\n" +
                "PPayee 1\n" +
                "MSome note here...\n" +
                "^\n");

        assertEquals(3, p.categories.size());

        List<QifCategory> categories = getCategoriesList(p);
        assertEquals("P1", categories.get(0).name);
        assertFalse(categories.get(0).isIncome);
        assertEquals("P1:c1", categories.get(1).name);
        assertFalse(categories.get(1).isIncome);
        assertEquals("P2", categories.get(2).name);
        assertTrue(categories.get(2).isIncome);

        assertEquals(1, p.accounts.size());

        QifAccount a = p.accounts.get(0);
        assertEquals("My Cash Account", a.memo);
        assertEquals("Cash", a.type);

        assertEquals(2, a.transactions.size());
        QifTransaction t = a.transactions.get(0);
        assertEquals(DateTime.date(2011, 2, 8).atMidnight().asDate(), t.date);
        assertEquals(1000, t.amount);
        assertEquals("P1", t.category);

        t = a.transactions.get(1);
        assertEquals(DateTime.date(2011, 2, 7).atMidnight().asDate(), t.date);
        assertEquals(-2056, t.amount);
        assertEquals("P1:c1", t.category);
        assertEquals("Payee 1", t.payee);
        assertEquals("Some note here...", t.memo);
    }

    @Test
    public void should_parse_date_according_to_format() throws Exception {
        parseQif(
                "!Type:Cat\n" +
                        "NP1\n" +
                        "E\n" +
                        "^\n" +
                        "NP1:c1\n" +
                        "E\n" +
                        "^\n" +
                        "NP2\n" +
                        "I\n" +
                        "^\n" +
                        "!Account\n" +
                        "NMy Cash Account\n" +
                        "TCash\n" +
                        "^\n" +
                        "!Type:Cash\n" +
                        "D2.8'11\n" +
                        "T10.00\n" +
                        "LP1\n" +
                        "^\n" +
                        "D02/07/2011\n" +
                        "T-20.56\n" +
                        "LP1:c1\n" +
                        "PPayee 1\n" +
                        "MSome note here...\n" +
                        "^\n", US_FORMAT);

        assertEquals(1, p.accounts.size());

        QifAccount a = p.accounts.get(0);
        assertEquals(2, a.transactions.size());

        QifTransaction t = a.transactions.get(0);
        assertEquals(DateTime.date(2011, 2, 8).atMidnight().asDate(), t.date);

        t = a.transactions.get(1);
        assertEquals(DateTime.date(2011, 2, 7).atMidnight().asDate(), t.date);
    }

    @Test
    public void should_parse_account_with_a_couple_of_transactions_without_category_list() throws Exception {
        parseQif(
                "!Account\n" +
                "NMy Cash Account\n" +
                "TCash\n" +
                "^\n" +
                "!Type:Cash\n" +
                "D08/02/2011\n" +
                "T10.00\n" +
                "LP1\n" +
                "^\n" +
                "D07/02/2011\n" +
                "T-20.56\n" +
                "LP1:c1\n" +
                "PPayee 1\n" +
                "MSome note here...\n" +
                "^\n");

        assertEquals(2, p.categories.size());

        List<QifCategory> categories = getCategoriesList(p);
        assertEquals("P1", categories.get(0).name);
        assertFalse(categories.get(0).isIncome);
        assertEquals("P1:c1", categories.get(1).name);
        assertFalse(categories.get(1).isIncome);

        assertEquals(1, p.accounts.size());

        QifAccount a = p.accounts.get(0);
        assertEquals("My Cash Account", a.memo);
        assertEquals("Cash", a.type);

        assertEquals(2, a.transactions.size());
        QifTransaction t = a.transactions.get(0);
        assertEquals(DateTime.date(2011, 2, 8).atMidnight().asDate(), t.date);
        assertEquals(1000, t.amount);
        assertEquals("P1", t.category);

        t = a.transactions.get(1);
        assertEquals(DateTime.date(2011, 2, 7).atMidnight().asDate(), t.date);
        assertEquals(-2056, t.amount);
        assertEquals("P1:c1", t.category);
        assertEquals("Payee 1", t.payee);
        assertEquals("Some note here...", t.memo);
    }

    @Test
    public void should_parse_multiple_accounts() throws Exception {
        parseQif(
                "!Account\n" +
                "NMy Cash Account\n" +
                "TCash\n" +
                "^\n" +
                "!Type:Cash\n" +
                "D08/02/2011\n" +
                "T10.00\n" +
                "^\n" +
                "D07/02/2011\n" +
                "T-23.45\n" +
                "^\n" +
                "D01/01/2011\n" +
                "T-67.80\n" +
                "^\n" +
                "!Account\n" +
                "NMy Bank Account\n" +
                "TBank\n" +
                "^\n" +
                "!Type:Bank\n" +
                "D08/02/2011\n" +
                "T-20.00\n" +
                "^\n" +
                "D02/01/2011\n" +
                "T54.00\n" +
                "^\n");

        assertEquals(2, p.accounts.size());

        QifAccount a = p.accounts.get(0);
        assertEquals("My Cash Account", a.memo);
        assertEquals("Cash", a.type);

        assertEquals(3, a.transactions.size());

        QifTransaction t = a.transactions.get(0);
        assertEquals(DateTime.date(2011, 2, 8).atMidnight().asDate(), t.date);
        assertEquals(1000, t.amount);

        t = a.transactions.get(1);
        assertEquals(DateTime.date(2011, 2, 7).atMidnight().asDate(), t.date);
        assertEquals(-2345, t.amount);

        t = a.transactions.get(2);
        assertEquals(DateTime.date(2011, 1, 1).atMidnight().asDate(), t.date);
        assertEquals(-6780, t.amount);

        a = p.accounts.get(1);
        assertEquals("My Bank Account", a.memo);
        assertEquals("Bank", a.type);

        t = a.transactions.get(0);
        assertEquals(DateTime.date(2011, 2, 8).atMidnight().asDate(), t.date);
        assertEquals(-2000, t.amount);

        t = a.transactions.get(1);
        assertEquals(DateTime.date(2011, 1, 2).atMidnight().asDate(), t.date);
        assertEquals(5400, t.amount);
    }

    @Test
    public void should_parse_categories_directly_from_transactions() throws Exception {
        parseQif(
                "!Account\n" +
                "NMy Cash Account\n" +
                "TCash\n" +
                "^\n" +
                "!Type:Cash\n" +
                "D08/02/2011\n" +
                "T10.00\n" +
                "LP1:к1\n" +
                "^\n" +
                "D07/02/2011\n" +
                "T11.00\n" +
                "LP1\n" +
                "^\n" +
                "D06/02/2011\n" +
                "T12.00\n" +
                "LP1:к1\n" +
                "^\n" +
                "D05/02/2011\n" +
                "T-13.80\n" +
                "LP1:c2\n" +
                "^\n" +
                "D04/02/2011\n" +
                "T-14.80\n" +
                "LP2:c1\n" +
                "^\n" +
                "D03/02/2011\n" +
                "T-15.80\n" +
                "LP2:c1\n" +
                "^\n" +
                "D02/02/2011\n" +
                "T-16.80\n" +
                "LP2\n" +
                "^\n");

        Set<QifCategory> categories = p.categories;
        assertEquals(5, categories.size());
    }

    @Test
    public void should_parse_classes() throws Exception {
        parseQif(
                "!Account\n" +
                "NMy Cash Account\n" +
                "TCash\n" +
                "^\n" +
                "!Type:Cash\n" +
                "D08/02/2011\n" +
                "T10.00\n" +
                "LP1/Class1\n" +
                "^\n" +
                "D07/02/2011\n" +
                "T-23.45\n" +
                "LP1:c1/Class1\n" +
                "^\n" +
                "D01/01/2011\n" +
                "T-67.80\n" +
                "LP1:c1/Class1:Subclass1\n" +
                "^\n" +
                "D01/01/2010\n" +
                "T-1.20\n" +
                "L/Class2\n" +
                "^\n");

        assertEquals(1, p.accounts.size());

        QifAccount a = p.accounts.get(0);
        assertEquals(4, a.transactions.size());

        QifTransaction t = a.transactions.get(0);
        assertEquals(DateTime.date(2011, 2, 8).atMidnight().asDate(), t.date);
        assertEquals(1000, t.amount);
        assertEquals("P1", t.category);
        assertEquals("Class1", t.categoryClass);

        t = a.transactions.get(1);
        assertEquals(DateTime.date(2011, 2, 7).atMidnight().asDate(), t.date);
        assertEquals(-2345, t.amount);
        assertEquals("P1:c1", t.category);
        assertEquals("Class1", t.categoryClass);

        t = a.transactions.get(2);
        assertEquals(DateTime.date(2011, 1, 1).atMidnight().asDate(), t.date);
        assertEquals(-6780, t.amount);
        assertEquals("P1:c1", t.category);
        assertEquals("Class1:Subclass1", t.categoryClass);

        t = a.transactions.get(3);
        assertEquals(DateTime.date(2010, 1, 1).atMidnight().asDate(), t.date);
        assertEquals(-120, t.amount);
        assertEquals("Class2", t.categoryClass);

        assertEquals(3, p.classes.size());
    }

    @Test
    public void should_parse_transfers() throws Exception {
        parseQif(
                "!Account\n" +
                "NMy Cash Account\n" +
                "TCash\n" +
                "^\n" +
                "!Type:Cash\n" +
                "D08/02/2011\n" +
                "T20.00\n" +
                "L[My Bank Account]\n" +
                "^\n" +
                "!Account\n" +
                "NMy Bank Account\n" +
                "TBank\n" +
                "^\n" +
                "!Type:Bank\n" +
                "D08/02/2011\n" +
                "T-20.00\n" +
                "L[My Cash Account]/Vacation\n" +
                "^\n");

        assertEquals(2, p.accounts.size());

        QifAccount a = p.accounts.get(0);
        assertEquals("My Cash Account", a.memo);
        assertEquals("Cash", a.type);

        assertEquals(1, a.transactions.size());

        QifTransaction t = a.transactions.get(0);
        assertEquals(DateTime.date(2011, 2, 8).atMidnight().asDate(), t.date);
        assertEquals("My Bank Account", t.toAccount);
        assertEquals(2000, t.amount);
        assertNull(t.category);

        a = p.accounts.get(1);
        assertEquals("My Bank Account", a.memo);
        assertEquals("Bank", a.type);

        assertEquals(1, a.transactions.size());

        t = a.transactions.get(0);
        assertEquals(DateTime.date(2011, 2, 8).atMidnight().asDate(), t.date);
        assertEquals("My Cash Account", t.toAccount);
        assertEquals("Vacation", t.categoryClass);
        assertEquals(-2000, t.amount);
        assertNull(t.category);

        assertEquals(1, p.classes.size());
        assertEquals("Vacation", p.classes.iterator().next());
    }

    @Test
    public void should_parse_splits() throws Exception {
        parseQif(
            "!Type:Cat\nNA\nE\n^\nNA:A1\nE\n^\nNA:A1:AA1\nE\n^\nNA:A2\nE\n^\nNB\nE\n^\n"+ // this is not important
            "!Account\n"+
            "NMy Cash Account\n"+
            "TCash\n"+
            "^\n"+
            "!Type:Cash\n"+
            "D12/07/2011\n"+
            "T-2,600.66\n"+
            "SA:A1\n"+
            "$-1,100.56\n"+
            "ENote on first split\n"+
            "SA:A2\n"+
            "$-1,000.00\n"+
            "S<NO_CATEGORY>\n"+
            "$500.10\n"+
            "ENote on third split\n"+
            "^\n");
        assertEquals(1, p.accounts.size());

        QifAccount a = p.accounts.get(0);
        assertEquals(1, a.transactions.size());

        QifTransaction t = a.transactions.get(0);
        assertEquals(-260066, t.amount);
        assertEquals(DateTime.date(2011, 7, 12).atMidnight().asDate(), t.date);
        assertEquals(3, t.splits.size());

        QifTransaction s = t.splits.get(0);
        assertEquals("A:A1", s.category);
        assertEquals(-110056, s.amount);
        assertEquals(DateTime.date(2011, 7, 12).atMidnight().asDate(), s.date);
        assertEquals("Note on first split", s.memo);

        s = t.splits.get(1);
        assertEquals("A:A2", s.category);
        assertEquals(-100000, s.amount);
        assertEquals(DateTime.date(2011, 7, 12).atMidnight().asDate(), s.date);

        s = t.splits.get(2);
        assertEquals("<NO_CATEGORY>", s.category);
        assertEquals(50010, s.amount);
        assertEquals(DateTime.date(2011, 7, 12).atMidnight().asDate(), s.date);
        assertEquals("Note on third split", s.memo);
    }

    @Test
    public void should_parse_transfer_splits() throws Exception {
        parseQif(
            "!Type:Cat\nNA\nE\n^\nNA:A1\nE\n^\nNA:A1:AA1\nE\n^\nNA:A2\nE\n^\nNB\nE\n^\n"+ // this is not important
            "!Account\n"+
            "NMy Cash Account\n"+
            "TCash\n"+
            "^\n"+
            "!Type:Cash\n"+
            "D12/07/2011\n"+
            "T-2,100.00\n"+
            "SA:A1\n"+
            "$-1,100.00\n"+
            "ENote on first split\n"+
            "S[My Bank Account]\n"+
            "$-1,000.00\n"+
            "^\n"+
            "!Account\n" +
            "NMy Bank Account\n" +
            "TBank\n" +
            "^\n" +
            "!Type:Bank\n" +
            "D12/07/2011\n" +
            "T1000.00\n" +
            "L[My Cash Account]\n" +
            "^\n"
        );
        assertEquals(2, p.accounts.size());

        QifAccount a = p.accounts.get(0);
        assertEquals("My Cash Account", a.memo);
        assertEquals("Cash", a.type);
        assertEquals(1, a.transactions.size());

        QifTransaction t = a.transactions.get(0);
        assertEquals(-210000, t.amount);
        assertEquals(2, t.splits.size());

        QifTransaction s = t.splits.get(0);
        assertEquals("A:A1", s.category);
        assertEquals(-110000, s.amount);
        assertEquals("Note on first split", s.memo);

        s = t.splits.get(1);
        assertTrue(s.isTransfer());
        assertEquals("My Bank Account", s.toAccount);
        assertEquals(-100000, s.amount);

        a = p.accounts.get(1);
        assertEquals("My Bank Account", a.memo);
        assertEquals("Bank", a.type);
        assertEquals(1, a.transactions.size());

        t = a.transactions.get(0);
        assertTrue(t.isTransfer());
        assertEquals("My Cash Account", t.toAccount);
        assertEquals(100000, t.amount);
    }

    void parseQif(String fileContent) throws IOException {
        parseQif(fileContent, EU_FORMAT);
    }

    private void parseQif(String fileContent, QifDateFormat dateFormat) throws IOException {
        QifBufferedReader r = new QifBufferedReader(new BufferedReader(new InputStreamReader(new ByteArrayInputStream(fileContent.getBytes()), StandardCharsets.UTF_8)));
        p = new QifParser(r, dateFormat);
        p.parse();
    }

    private List<QifCategory> getCategoriesList(QifParser p) {
        List<QifCategory> categories = new ArrayList<>(p.categories.size());
        categories.addAll(p.categories);
        Collections.sort(categories, (c1, c2) -> c1.name.compareTo(c2.name));
        return categories;
    }


}
