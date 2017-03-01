package ru.orangesoftware.financisto.db;

import ru.orangesoftware.financisto.model.*;
import ru.orangesoftware.financisto.test.AccountBuilder;
import ru.orangesoftware.financisto.test.CategoryBuilder;
import ru.orangesoftware.financisto.test.TransactionBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MyDatabaseTest extends AbstractDbTest {

    Account a1;
    Map<String, Category> categoriesMap;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        a1 = AccountBuilder.createDefault(db);
        categoriesMap = CategoryBuilder.createDefaultHierarchy(db);
    }

    public void testShouldSavePayeeOnlyOnce() {
        // given
        String payee = "Payee1";
        // when
        Payee p1 = em.insertPayee(payee);
        Payee p2 = em.insertPayee(payee);
        List<Payee> payees = em.getAllPayeeList();
        // then
        assertEquals("Ids should be the same!", p1.id, p2.id);
        assertEquals("List should be of size 1!", 1, payees.size());
        assertEquals("The first payee should be the one!", payees.get(0).title, payee);
    }

    public void test_when_child_category_is_inserted_it_should_inherit_type_from_the_parent() {
        long a1Id = db.insertOrUpdate(createIncomeCategory("A1"), new ArrayList<Attribute>());
        long a2Id = db.insertOrUpdate(createExpenseCategory("A2"), new ArrayList<Attribute>());
        long a11id = db.insertChildCategory(a1Id, createExpenseCategory("a11"));
        long a21id = db.insertChildCategory(a2Id, createIncomeCategory("a21"));
        Category a1 = db.getCategory(a1Id);
        Category a2 = db.getCategory(a2Id);
        Category a11 = db.getCategory(a11id);
        Category a21 = db.getCategory(a21id);
        assertTrue(a1.isIncome());
        assertTrue(a2.isExpense());
        assertTrue(a11.isIncome());
        assertTrue(a21.isExpense());
    }

    public void test_when_category_moves_under_a_new_parent_it_should_inherit_its_type_from_the_new_parent() {
        long a1Id = db.insertOrUpdate(createIncomeCategory("A1"), new ArrayList<Attribute>());
        long a2Id = db.insertOrUpdate(createExpenseCategory("A2"), new ArrayList<Attribute>());
        long a11Id = db.insertChildCategory(a1Id, createExpenseCategory("a11"));
        long a111Id = db.insertChildCategory(a11Id, createExpenseCategory("a111"));
        Category a2 = db.getCategory(a2Id);
        Category a11 = db.getCategory(a11Id);
        assertTrue(a11.isIncome());
        a11.parent = a2;
        a11.title = "a21";
        long a21id = db.insertOrUpdate(a11, new ArrayList<Attribute>());
        Category a21 = db.getCategory(a21id);
        Category a211 = db.getCategory(a111Id);
        assertTrue("Category should inherit new type", a21.isExpense());
        assertTrue("Child category should inherit new type", a211.isExpense());
    }

    public void test_should_set_split_status_when_inserting_new_transaction() {
        // when
        Transaction t = TransactionBuilder.withDb(db).account(a1).amount(1000)
                .withSplit(categoriesMap.get("A1"), 100)
                .withSplit(categoriesMap.get("A2"), 900)
                .withStatus(TransactionStatus.CL)
                .create();
        // then
        List<Transaction> splits = em.getSplitsForTransaction(t.id);
        for (Transaction split : splits) {
            assertEquals(t.status, split.status);
        }
    }

    public void test_should_update_split_status_when_changing_status_of_the_parent_transaction() {
        // given
        Transaction t = TransactionBuilder.withDb(db).account(a1).amount(1000)
                .withSplit(categoriesMap.get("A1"), 100)
                .withSplit(categoriesMap.get("A2"), 900)
                .create();
        // when
        t.status = TransactionStatus.CL;
        db.insertOrUpdate(t);
        // then
        List<Transaction> splits = em.getSplitsForTransaction(t.id);
        for (Transaction split : splits) {
            assertEquals(t.status, split.status);
        }
    }

    private Category createIncomeCategory(String title) {
        Category c = new Category();
        c.title = title;
        c.makeThisCategoryIncome();
        return c;
    }

    private Category createExpenseCategory(String title) {
        Category c = new Category();
        c.title = title;
        c.makeThisCategoryExpense();
        return c;
    }

}
