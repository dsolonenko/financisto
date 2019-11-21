package ru.orangesoftware.financisto.model;

import org.junit.Test;

import java.util.Map;

import ru.orangesoftware.financisto.db.AbstractDbTest;
import ru.orangesoftware.financisto.test.AccountBuilder;
import ru.orangesoftware.financisto.test.CategoryBuilder;
import ru.orangesoftware.financisto.test.DateTime;
import ru.orangesoftware.financisto.test.TransactionBuilder;

import static org.junit.Assert.*;

public class BudgetTest extends AbstractDbTest {

    Budget budgetOne;
    Account account;
    Project project;
    Map<String, Category> categoriesMap;
    Map<Long, Category> categories;
    Map<Long, Project> projects;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        account = AccountBuilder.createDefault(db);
        categoriesMap = CategoryBuilder.createDefaultHierarchy(db);
        categories = MyEntity.asMap(db.getCategoriesList(true));
        project = new Project();
        project.title = "P1";
        db.saveOrUpdate(project);
        projects = MyEntity.asMap(db.getAllProjectsList(true));
        createBudget();
    }

    private void createBudget() {
        budgetOne = new Budget();
        budgetOne.currency = account.currency;
        budgetOne.amount = 1000;
        budgetOne.categories = String.valueOf(categoriesMap.get("A").id);
        budgetOne.projects = String.valueOf(project.id);
        budgetOne.expanded = true;
        budgetOne.includeSubcategories = true;
        budgetOne.startDate = DateTime.date(2011, 4, 1).atMidnight().asLong();
        budgetOne.endDate = DateTime.date(2011, 4, 30).at(23, 59, 59, 999).asLong();
        db.saveOrUpdate(budgetOne);
    }

    @Test
    public void should_calculate_budget_correctly_with_regular_transactions() {
        // zero initially
        long spent = db.fetchBudgetBalance(categories, projects, budgetOne);
        assertEquals(0, spent);
        // yes, should affect budget
        TransactionBuilder.withDb(db).account(account).dateTime(DateTime.date(2011, 4, 1).atNoon()).amount(-100).category(categoriesMap.get("A")).create();
        spent = db.fetchBudgetBalance(categories, projects, budgetOne);
        assertEquals(-100, spent);
        // no, period is out
        TransactionBuilder.withDb(db).account(account).dateTime(DateTime.date(2011, 5, 1).atNoon()).amount(-200).category(categoriesMap.get("A")).create();
        spent = db.fetchBudgetBalance(categories, projects, budgetOne);
        assertEquals(-100, spent);
        // no, category is out
        TransactionBuilder.withDb(db).account(account).dateTime(DateTime.date(2011, 4, 1).atNoon()).amount(-200).category(categoriesMap.get("B")).create();
        spent = db.fetchBudgetBalance(categories, projects, budgetOne);
        assertEquals(-100, spent);
        // yes, child category
        TransactionBuilder.withDb(db).account(account).dateTime(DateTime.date(2011, 4, 2).atNoon()).amount(-200).category(categoriesMap.get("A1")).create();
        spent = db.fetchBudgetBalance(categories, projects, budgetOne);
        assertEquals(-300, spent);
    }

    @Test public void should_calculate_budget_correctly_with_splits() {
        // zero initially
        long spent = db.fetchBudgetBalance(categories, projects, budgetOne);
        assertEquals(0, spent);
        // yes, should affect budget
        Transaction t = TransactionBuilder.withDb(db).account(account).dateTime(DateTime.date(2011, 4, 1).atNoon())
                .amount(-100)
                .category(CategoryBuilder.split(db))
                .withSplit(categoriesMap.get("A1"), -60)
                .withSplit(categoriesMap.get("B"), -30)
                .withSplit(categoriesMap.get("B"), project, -10)
                .create();
        spent = db.fetchBudgetBalance(categories, projects, budgetOne);
        assertEquals(-70, spent);
        // back to zero when split gets deleted
        db.deleteTransaction(t.id);
        spent = db.fetchBudgetBalance(categories, projects, budgetOne);
        assertEquals(0, spent);
    }

    @Test public void should_calculate_budget_total() {

    }

}
