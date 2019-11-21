/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.export;

import android.util.Log;

import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ru.orangesoftware.financisto.export.csv.CsvImport;
import ru.orangesoftware.financisto.export.csv.CsvImportOptions;
import ru.orangesoftware.financisto.export.csv.CsvTransaction;
import ru.orangesoftware.financisto.filter.WhereFilter;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.CategoryTree;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.Payee;
import ru.orangesoftware.financisto.model.Project;
import ru.orangesoftware.financisto.model.TransactionInfo;
import ru.orangesoftware.financisto.test.CategoryBuilder;
import ru.orangesoftware.financisto.test.DateTime;
import static org.junit.Assert.*;

/**
 * Default format described here
 * https://docs.google.com/spreadsheet/ccc?key=0AiE-9LlEldfYdFMzVHUtenktTkhoN1dMd1FaOUJaY1E
 */
public class CsvImportTest extends AbstractImportExportTest {

    Map<String, Category> categories;
    CsvImport csvImport;
    CsvImportOptions defaultOptions;
    long defaultAccountId;

    public void setUp() throws Exception {
        super.setUp();
        defaultOptions = createDefaultOptions();
        defaultAccountId = defaultOptions.selectedAccountId;
    }

    @Test
    public void should_collect_all_categories_from_transactions() {
        //given
        csvImport = new CsvImport(db, defaultOptions);
        List<CsvTransaction> transactions = new LinkedList<CsvTransaction>();
        transactions.add(newCsvTransactionWithCategory(null, "A"));
        transactions.add(newCsvTransactionWithCategory("", "A"));
        transactions.add(newCsvTransactionWithCategory("A", "A1"));
        transactions.add(newCsvTransactionWithCategory("A", "A2"));
        transactions.add(newCsvTransactionWithCategory("A:A1", "AA1"));
        transactions.add(newCsvTransactionWithCategory("B:B1", "BB1"));
        transactions.add(newCsvTransactionWithCategory("B", "B2"));
        //when
        Set<CategoryInfo> categories = csvImport.collectCategories(transactions);
        //then
        assertEquals(asCategoryInfoSet("A", "A:A1", "A:A2", "A:A1:AA1", "B:B1:BB1", "B:B2"), categories);
    }

    @Test
    public void should_insert_all_categories_from_transactions() {
        //given
        csvImport = new CsvImport(db, defaultOptions);
        List<CsvTransaction> transactions = new LinkedList<CsvTransaction>();
        transactions.add(newCsvTransactionWithCategory(null, null));
        transactions.add(newCsvTransactionWithCategory("", ""));
        transactions.add(newCsvTransactionWithCategory(null, "A"));
        transactions.add(newCsvTransactionWithCategory("", "A"));
        transactions.add(newCsvTransactionWithCategory("A", "A1"));
        transactions.add(newCsvTransactionWithCategory("A", "A2"));
        transactions.add(newCsvTransactionWithCategory("A", "A1"));
        //when
        Map<String, Category> categories = csvImport.collectAndInsertCategories(transactions);
        //then
        CategoryTree<Category> categoriesTree = db.getCategoriesTree(false);
        assertEquals(1, categoriesTree.size());
        assertEquals(3, categories.size());
        //when
        transactions = new LinkedList<CsvTransaction>();
        transactions.add(newCsvTransactionWithCategory("B:B1", "BB1"));
        transactions.add(newCsvTransactionWithCategory("A:A1", "AA1"));
        categories = csvImport.collectAndInsertCategories(transactions);
        //then
        categoriesTree = db.getCategoriesTree(false);
        assertEquals(2, categoriesTree.size());
        assertEquals(7, categories.size());
    }

    @Test
    public void should_insert_all_projects_from_transactions() {
        //given
        csvImport = new CsvImport(db, defaultOptions);
        List<CsvTransaction> transactions = new LinkedList<CsvTransaction>();
        transactions.add(newCsvTransactionWithProject(null));
        transactions.add(newCsvTransactionWithProject(""));
        transactions.add(newCsvTransactionWithProject("No project"));
        transactions.add(newCsvTransactionWithProject("P1"));
        transactions.add(newCsvTransactionWithProject("P1"));
        transactions.add(newCsvTransactionWithProject("P2"));
        //when
        Map<String, Project> projects = csvImport.collectAndInsertProjects(transactions);
        //then
        List<Project> allProjects = db.getActiveProjectsList(false);
        assertEquals(2, allProjects.size());
        assertEquals(2, projects.size());
        //when
        transactions = new LinkedList<CsvTransaction>();
        transactions.add(newCsvTransactionWithProject("P1"));
        transactions.add(newCsvTransactionWithProject("P3"));
        projects = csvImport.collectAndInsertProjects(transactions);
        //then
        allProjects = db.getActiveProjectsList(false);
        assertEquals(3, allProjects.size());
        assertEquals(3, projects.size());
    }

    @Test
    public void should_insert_all_payees_from_transactions() {
        //given
        csvImport = new CsvImport(db, defaultOptions);
        List<CsvTransaction> transactions = new LinkedList<CsvTransaction>();
        transactions.add(newCsvTransactionWithPayee(null));
        transactions.add(newCsvTransactionWithPayee(""));
        transactions.add(newCsvTransactionWithPayee("Payee1"));
        transactions.add(newCsvTransactionWithPayee("Payee1"));
        transactions.add(newCsvTransactionWithPayee("Payee2"));
        //when
        Map<String, Payee> payees = csvImport.collectAndInsertPayees(transactions);
        //then
        List<Payee> allPayees = db.getAllPayeeList();
        assertEquals(2, allPayees.size());
        assertEquals(2, payees.size());
        //when
        transactions = new LinkedList<CsvTransaction>();
        transactions.add(newCsvTransactionWithPayee("Payee1"));
        transactions.add(newCsvTransactionWithPayee("Payee3"));
        payees = csvImport.collectAndInsertPayees(transactions);
        //then
        allPayees = db.getAllPayeeList();
        assertEquals(3, allPayees.size());
        assertEquals(3, payees.size());
    }

    @Test
    public void should_import_empty_file() throws Exception {
        doImport("", defaultOptions);
    }

    @Test
    public void should_import_one_transaction_into_the_selected_account() throws Exception {
        categories = CategoryBuilder.createDefaultHierarchy(db);
        doImport("date,time,account,amount,currency,category,parent,payee,location,project,note\n" +
                "10.07.2011,07:13:17,AAA,-10.50,SGD,AA1,A:A1,P1,,,", defaultOptions);

        List<TransactionInfo> transactions = db.getTransactionsForAccount(defaultAccountId);
        assertEquals(1, transactions.size());

        TransactionInfo t = transactions.get(0);
        assertEquals(DateTime.date(2011, 7, 10).at(7, 13, 17, 0).asLong(), t.dateTime);
        assertEquals(defaultAccountId, t.fromAccount.id);
        assertEquals(-1050, t.fromAmount);
        assertEquals(categories.get("AA1").id, t.category.id);
        assertEquals("P1", t.payee.title);
    }

    @Test
    public void should_import_one_transaction_without_the_header() throws Exception {
        categories = CategoryBuilder.createDefaultHierarchy(db);
        defaultOptions.useHeaderFromFile = false;
        doImport(
                "11.07.2011,07:13:17,AAA,2100.56,SGD,1680.10,USD,B,\"\",P1,Current location,No project\n"+
                "10.07.2011,07:13:17,AAA,2100.56,SGD,\"\",\"\",B,\"\",P1,Current location,No project,", defaultOptions);

        List<TransactionInfo> transactions = db.getTransactionsForAccount(defaultAccountId);
        assertEquals(2, transactions.size());

        TransactionInfo t = transactions.get(0);
        assertEquals(DateTime.date(2011, 7, 11).at(7, 13, 17, 0).asLong(), t.dateTime);
        assertEquals(defaultAccountId, t.fromAccount.id);
        assertEquals(210056, t.fromAmount);
        assertEquals(168010, t.originalFromAmount);
        assertEquals("USD", t.originalCurrency.name);
        assertEquals(categories.get("B").id, t.category.id);
        assertEquals("P1", t.payee.title);

        t = transactions.get(1);
        //each transaction adds 1 ms to keep the original order
        assertEquals(DateTime.date(2011, 7, 10).at(7, 13, 17, 1).asLong(), t.dateTime);
        assertEquals(defaultAccountId, t.fromAccount.id);
        assertEquals(210056, t.fromAmount);
        assertEquals(categories.get("B").id, t.category.id);
        assertEquals("P1", t.payee.title);
    }

    private void doImport(String csv, CsvImportOptions options) throws Exception {
        File tmp = File.createTempFile("backup", ".csv");
        FileWriter w = new FileWriter(tmp);
        w.write(csv);
        w.close();
        Log.d("Financisto", "Created a temporary backup file: " + tmp.getAbsolutePath());
        options = new CsvImportOptions(options.currency, options.dateFormat.toPattern(),
                options.selectedAccountId, options.filter, tmp.getAbsolutePath(), options.fieldSeparator, options.useHeaderFromFile);
        csvImport = new CsvImport(db, options);
        csvImport.doImport();
    }

    private CsvTransaction newCsvTransactionWithCategory(String parent, String category) {
        CsvTransaction transaction = new CsvTransaction();
        transaction.categoryParent = parent;
        transaction.category = category;
        return transaction;
    }

    private CsvTransaction newCsvTransactionWithProject(String project) {
        CsvTransaction transaction = new CsvTransaction();
        transaction.project = project;
        return transaction;
    }

    private CsvTransaction newCsvTransactionWithPayee(String payee) {
        CsvTransaction transaction = new CsvTransaction();
        transaction.payee = payee;
        return transaction;
    }

    private CsvImportOptions createDefaultOptions() {
        Account a = createFirstAccount();
        Currency c = a.currency;
        return new CsvImportOptions(c, CsvImportOptions.DEFAULT_DATE_FORMAT, a.id, WhereFilter.empty(), null, ',', true);
    }

    private Set<CategoryInfo> asCategoryInfoSet(String...categories) {
        Set<CategoryInfo> set = new HashSet<CategoryInfo>();
        for (String category : categories) {
            set.add(new CategoryInfo(category, false));
        }
        return set;
    }

}
