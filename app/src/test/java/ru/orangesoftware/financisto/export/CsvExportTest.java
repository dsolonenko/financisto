/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.export;

import org.junit.Test;

import java.util.Map;

import ru.orangesoftware.financisto.export.csv.CsvExport;
import ru.orangesoftware.financisto.export.csv.CsvExportOptions;
import ru.orangesoftware.financisto.filter.WhereFilter;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.test.CategoryBuilder;
import ru.orangesoftware.financisto.test.CurrencyBuilder;
import ru.orangesoftware.financisto.test.DateTime;
import ru.orangesoftware.financisto.test.TransactionBuilder;
import ru.orangesoftware.financisto.test.TransferBuilder;
import ru.orangesoftware.financisto.utils.CurrencyCache;
import static org.junit.Assert.*;
/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 8/3/11 12:04 AM
 */
public class CsvExportTest extends AbstractExportTest<CsvExport, CsvExportOptions> {

    Account a1;
    Account a2;
    Map<String, Category> categoriesMap;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        a1 = createFirstAccount();
        a2 = createSecondAccount();
        categoriesMap = CategoryBuilder.createDefaultHierarchy(db);
        CurrencyCache.initialize(db);
    }

    @Test
    public void should_include_header() throws Exception {
        CsvExportOptions options = new CsvExportOptions(Currency.EMPTY, ',', true, false, false, WhereFilter.empty(), false);
        assertEquals("date,time,account,amount,currency,original amount,original currency,category,parent,payee,location,project,note\n", exportAsString(options));
    }

    @Test
    public void should_export_regular_transaction() throws Exception {
        CsvExportOptions options = new CsvExportOptions(createExportCurrency(), ',', false, false, false, WhereFilter.empty(), false);
        TransactionBuilder.withDb(db).dateTime(DateTime.date(2011, 8, 3).at(22, 34, 55, 10))
                .account(a1).amount(-123456).category(categoriesMap.get("AA1")).payee("P1").location("Home").project("P1").note("My note").create();
        TransactionBuilder.withDb(db).dateTime(DateTime.date(2011, 8, 4).at(23, 34, 55, 10))
                .account(a1).amount(-789).originalAmount(a2.currency, -888).category(categoriesMap.get("AA1")).payee("P1").location("Home").project("P1").note("My note").create();
        assertEquals(
                "2011-08-04,23:34:55,My Cash Account,-7.89,SGD,-8.88,CZK,AA1,A:A1,P1,Home,P1,My note\n"+
                "2011-08-03,22:34:55,My Cash Account,-1234.56,SGD,\"\",\"\",AA1,A:A1,P1,Home,P1,My note\n",
                exportAsString(options));
    }

    @Test
    public void should_export_regular_transfer() throws Exception {
        CsvExportOptions options = new CsvExportOptions(createExportCurrency(), ',', false, false, false, WhereFilter.empty(), false);
        TransferBuilder.withDb(db).dateTime(DateTime.date(2011, 8, 3).at(22, 46, 0, 0))
                .fromAccount(a1).fromAmount(-450000).toAccount(a2).toAmount(25600).create();
        assertEquals(
                "2011-08-03,22:46:00,My Cash Account,-4500.00,SGD,\"\",\"\",\"\",\"\",\"\",Transfer Out,<NO_PROJECT>,\n"+
                "2011-08-03,22:46:00,My Bank Account,256.00,CZK,\"\",\"\",\"\",\"\",\"\",Transfer In,<NO_PROJECT>,\n",
                exportAsString(options));
    }

    @Test
    public void should_export_split_transaction() throws Exception {
        CsvExportOptions options = new CsvExportOptions(createExportCurrency(), ',', false, true, false, WhereFilter.empty(), false);
        TransactionBuilder.withDb(db).dateTime(DateTime.date(2011, 8, 3).at(22, 34, 55, 10))
                .account(a1).amount(-2000).payee("P1").location("Home").project("R1").note("My note")
                .withSplit(categoriesMap.get("A1"), -500)
                .withSplit(categoriesMap.get("A2"), -1500)
                .create();
        assertEquals(
                "2011-08-03,22:34:55,My Cash Account,-20.00,SGD,\"\",\"\",SPLIT,\"\",P1,Home,R1,My note\n"+
                "~,\"\",My Cash Account,-5.00,SGD,\"\",\"\",A1,A,P1,\"\",<NO_PROJECT>,\n"+
                "~,\"\",My Cash Account,-15.00,SGD,\"\",\"\",A2,A,P1,\"\",<NO_PROJECT>,\n",
                exportAsString(options));
    }

    @Test
    public void should_export_split_transfer() throws Exception {
        CsvExportOptions options = new CsvExportOptions(createExportCurrency(), ',', false, true, false, WhereFilter.empty(), false);
        TransactionBuilder.withDb(db).dateTime(DateTime.date(2011, 8, 3).at(22, 34, 55, 10))
                .account(a1).amount(-500).payee("P1").location("Home").project("R1").note("My note")
                .withTransferSplit(a2, -500, +100)
                .create();
        assertEquals(
                "2011-08-03,22:34:55,My Cash Account,-5.00,SGD,\"\",\"\",SPLIT,\"\",P1,Home,R1,My note\n"+
                        "~,\"\",My Cash Account,-5.00,SGD,\"\",\"\",\"\",\"\",\"\",Transfer Out,<NO_PROJECT>,\n"+
                        "~,\"\",My Bank Account,1.00,CZK,\"\",\"\",\"\",\"\",\"\",Transfer In,<NO_PROJECT>,\n",
                exportAsString(options));
    }

    @Test
    public void should_not_export_split_transactions_if_not_set_in_options() throws Exception {
        CsvExportOptions options = new CsvExportOptions(createExportCurrency(), ',', false, false, false, WhereFilter.empty(), false);
        TransactionBuilder.withDb(db).dateTime(DateTime.date(2011, 8, 3).at(22, 34, 55, 10))
                .account(a1).amount(-2000).payee("P1").location("Home").project("R1").note("My note")
                .withSplit(categoriesMap.get("A1"), -500)
                .withSplit(categoriesMap.get("A2"), -1500)
                .create();
        assertEquals(
                "2011-08-03,22:34:55,My Cash Account,-20.00,SGD,\"\",\"\",SPLIT,\"\",P1,Home,R1,My note\n",
                exportAsString(options));
    }


    private Currency createExportCurrency() {
        Currency c = CurrencyBuilder.withDb(db)
                .title("USD")
                .name("USD")
                .symbol("$")
                .separators("''", "'.'")
                .create();
        assertNotNull(db.load(Currency.class, c.id));
        return c;
    }

    @Override
    protected CsvExport createExport(CsvExportOptions options) {
        return new CsvExport(getContext(), db, options);
    }

}
