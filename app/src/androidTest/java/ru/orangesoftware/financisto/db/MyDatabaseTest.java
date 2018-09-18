package ru.orangesoftware.financisto.db;

import junit.framework.Assert;
import ru.orangesoftware.financisto.model.*;
import ru.orangesoftware.financisto.test.AccountBuilder;
import ru.orangesoftware.financisto.test.CategoryBuilder;
import ru.orangesoftware.financisto.test.ProjectBuilder;
import ru.orangesoftware.financisto.test.TransactionBuilder;
import ru.orangesoftware.orb.Query;

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

    public void test_entity_filtering() {
//        Project p0 = ProjectBuilder.withDb(db).id(0).title("no project").setActive().create();
        Project p1 = ProjectBuilder.withDb(db).title("1 first").setActive().create();
        Project p2 = ProjectBuilder.withDb(db).title("2proj2").create();
        Project p3 = ProjectBuilder.withDb(db).title("3proj3").setActive().create();
        Project p4 = ProjectBuilder.withDb(db).title("4forth").setActive().create();

        List<Project> res = Query.readEntityList(db.queryEntities(Project.class, null, false, true), Project.class);
        Assert.assertEquals(3, res.size());

        res = Query.readEntityList(db.getAllEntities(Project.class), Project.class);
        Assert.assertEquals(4, res.size());

        res = Query.readEntityList(db.queryEntities(Project.class, null, true, true), Project.class);
        Assert.assertEquals(4, res.size());
        Assert.assertEquals("1 first", res.get(0).title);
        Assert.assertEquals("3proj3", res.get(1).title);
        Assert.assertEquals("4forth", res.get(2).title);
        Assert.assertEquals(Project.noProject().title, res.get(3).title);

        res = Query.readEntityList(db.queryEntities(Project.class, "proj", true, false), Project.class);
        Assert.assertEquals(3, res.size());
        Assert.assertEquals("2proj2", res.get(0).title);
        Assert.assertEquals("3proj3", res.get(1).title);
        Assert.assertEquals(Project.noProject().title, res.get(2).title);

        res = Query.readEntityList(db.queryEntities(Project.class, "proj", false, false), Project.class);
        Assert.assertEquals(2, res.size());
        Assert.assertEquals("2proj2", res.get(0).title);
        Assert.assertEquals("3proj3", res.get(1).title);

        res = Query.readEntityList(db.queryEntities(Project.class, "Proj", false, true), Project.class);
        Assert.assertEquals(1, res.size());
        Assert.assertEquals("3proj3", res.get(0).title);

        res = Query.readEntityList(db.queryEntities(Project.class, "o h", false, true), Project.class);
        Assert.assertEquals(1, res.size());
        Assert.assertEquals("4forth", res.get(0).title);
    }

    public void test_payee_sort_order() { // currently we ignore sort_order column
        db.findOrInsertPayee("Payee1");
        db.findOrInsertPayee("Payee2");
        List<Payee> payees = db.getAllPayeeList();

        assertEquals("Sort order must be incremented for p1!", 1, payees.get(0).sortOrder);
        assertEquals("Sort order must be incremented for p2!", 2, payees.get(1).sortOrder);

        Payee p3 = db.findOrInsertPayee("Payee3");
        Payee p4 = db.findOrInsertPayee("Payee4");

        p3.sortOrder = 4;
        p4.sortOrder = 3;

        db.saveOrUpdate(p3);
        db.saveOrUpdate(p4);

        payees = db.getAllPayeeList();

        assertEquals("sort order mismatch:", "Payee4", payees.get(3).title);
        assertEquals("sort order mismatch:", "Payee3", payees.get(2).title);
    }

    public void test_should_save_payee_once() {
        // given
        String payee = "Payee1";
        // when
        Payee p1 = db.findOrInsertPayee(payee);
        Payee p2 = db.findOrInsertPayee(payee);
        List<Payee> payees = db.getAllPayeeList();
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
        Category a1 = db.getCategoryWithParent(a1Id);
        Category a2 = db.getCategoryWithParent(a2Id);
        Category a11 = db.getCategoryWithParent(a11id);
        Category a21 = db.getCategoryWithParent(a21id);
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
        Category a2 = db.getCategoryWithParent(a2Id);
        Category a11 = db.getCategoryWithParent(a11Id);
        assertTrue(a11.isIncome());
        a11.parent = a2;
        a11.title = "a21";
        long a21id = db.insertOrUpdate(a11, new ArrayList<Attribute>());
        Category a21 = db.getCategoryWithParent(a21id);
        Category a211 = db.getCategoryWithParent(a111Id);
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
        List<Transaction> splits = db.getSplitsForTransaction(t.id);
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
        List<Transaction> splits = db.getSplitsForTransaction(t.id);
        for (Transaction split : splits) {
            assertEquals(t.status, split.status);
        }
    }

    public void test_should_run_mass_operations() {
        // given
        Transaction t1 = TransactionBuilder.withDb(db).account(a1).amount(1000).create();
        Transaction t2 = TransactionBuilder.withDb(db).account(a1).amount(-2000).create();
        long[] ids = {t1.id, t2.id};

        // when
        db.clearSelectedTransactions(ids);
        // then
        for (TransactionInfo info : db.getTransactionsForAccount(a1.id)) {
            assertEquals(info.status, TransactionStatus.CL);
        }

        // when
        db.reconcileSelectedTransactions(ids);
        // then
        for (TransactionInfo info : db.getTransactionsForAccount(a1.id)) {
            assertEquals(info.status, TransactionStatus.RC);
        }

        // when
        db.deleteSelectedTransactions(ids);
        // then
        assertEquals(0, db.getTransactionsForAccount(a1.id).size());
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
