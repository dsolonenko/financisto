package ru.orangesoftware.financisto.db;

import android.database.Cursor;

import org.junit.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.Attribute;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.MyEntity;
import ru.orangesoftware.financisto.model.MyLocation;
import ru.orangesoftware.financisto.model.Payee;
import ru.orangesoftware.financisto.model.Project;
import ru.orangesoftware.financisto.model.RestoredTransaction;
import ru.orangesoftware.financisto.model.Transaction;
import ru.orangesoftware.financisto.test.AccountBuilder;
import ru.orangesoftware.financisto.test.CategoryBuilder;
import ru.orangesoftware.financisto.test.CurrencyBuilder;
import ru.orangesoftware.financisto.test.DateTime;
import ru.orangesoftware.financisto.test.TransactionBuilder;
import ru.orangesoftware.financisto.test.TransferBuilder;
import ru.orangesoftware.orb.EntityManager;

public class DatabaseAdapterTest extends AbstractDbTest {

    Account a1;
    Account a2;
    Map<String, Category> categoriesMap;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        a1 = AccountBuilder.createDefault(db);
        categoriesMap = CategoryBuilder.createDefaultHierarchy(db);
    }

    public void test_should_restore_split_transaction() {
        //given
        Transaction originalTransaction = TransactionBuilder.withDb(db).account(a1).amount(100)
                .withSplit(categoriesMap.get("A1"), 40)
                .withSplit(categoriesMap.get("B"), 60)
                .create();
        List<RestoredTransaction> transactionsToRestore = new ArrayList<RestoredTransaction>();
        transactionsToRestore.add(new RestoredTransaction(originalTransaction.id, DateTime.date(2011, 8, 16).atNoon().asDate()));
        //when
        long[] restoredIds = db.storeMissedSchedules(transactionsToRestore, DateTime.date(2011, 8, 16).atMidnight().asLong());
        //then
        assertNotNull(restoredIds);
        assertEquals(1, restoredIds.length);
        Transaction restoredTransaction = db.getTransaction(restoredIds[0]);
        assertNotNull(restoredTransaction);
        assertTrue(restoredTransaction.isSplitParent());
        List<Transaction> splits = db.getSplitsForTransaction(restoredIds[0]);
        assertNotNull(splits);
        assertEquals(2, splits.size());
    }

    public void test_should_remember_last_used_transaction_for_the_payee() {
        //when
        TransactionBuilder.withDb(db).account(a1).amount(1000).payee("Payee1").category(categoriesMap.get("A1")).create();
        //then
        Payee p = db.getPayee("Payee1");
        assertEquals(categoriesMap.get("A1").id, p.lastCategoryId);
    }

    public void test_should_search_payee_with_or_without_first_letter_capitalized() {
        // given
        db.findOrInsertPayee("Парковка");
        db.findOrInsertPayee("parking");

        //then
        assertEquals("parking", fetchFirstPayee("P"));
        assertEquals("parking", fetchFirstPayee("p"));
        assertEquals("parking", fetchFirstPayee("Pa"));
        assertEquals("parking", fetchFirstPayee("par"));
        assertEquals("Парковка", fetchFirstPayee("П"));
        assertEquals("Парковка", fetchFirstPayee("п"));
        assertEquals("Парковка", fetchFirstPayee("Па"));
        assertEquals("Парковка", fetchFirstPayee("пар"));
    }

    public void test_should_detect_multiple_account_currencies() {
        // one account only
        assertTrue(db.singleCurrencyOnly());

        // two accounts with the same currency
        AccountBuilder.withDb(db).currency(a1.currency).title("Account2").create();
        assertTrue(db.singleCurrencyOnly());

        //another account with a different currency, but not included into totals
        Currency c2 = CurrencyBuilder.withDb(db).name("USD").title("Dollar").symbol("$").create();
        AccountBuilder.withDb(db).currency(c2).title("Account3").doNotIncludeIntoTotals().create();
        assertTrue(db.singleCurrencyOnly());

        //this account is not active
        AccountBuilder.withDb(db).currency(c2).title("Account4").inactive().create();
        assertTrue(db.singleCurrencyOnly());

        //now it's two currencies
        AccountBuilder.withDb(db).currency(c2).title("Account5").create();
        assertFalse(db.singleCurrencyOnly());
    }

    public void test_should_not_return_split_category_as_parent_when_editing_a_category() {
        List<Category> list = db.getCategoriesWithoutSubtreeAsList(categoriesMap.get("A").id);
        for (Category category : list) {
            assertFalse("Should not be split", category.isSplit());
        }
    }

    public void test_should_return_only_valid_parent_categories_when_editing_a_category() {
        List<Category> list = db.getCategoriesWithoutSubtreeAsList(categoriesMap.get("A").id);
        assertEquals(2, list.size());
        assertEquals(Category.NO_CATEGORY_ID, list.get(0).id);
        assertEquals(categoriesMap.get("B").id, list.get(1).id);
    }

    public void test_should_return_id_of_the_nearest_transaction_which_is_older_than_specified_date() {
        //given
        a2 = AccountBuilder.createDefault(db);
        Transaction t8 = TransactionBuilder.withDb(db).dateTime(DateTime.date(2012, 5, 25).at(17, 30, 45, 0)).account(a2).amount(-234).create();
        Transaction t7 = TransactionBuilder.withDb(db).dateTime(DateTime.date(2012, 5, 25).at(16, 30, 45, 0)).account(a1).amount(-234).create();
        Transaction t6 = TransactionBuilder.withDb(db).dateTime(DateTime.date(2012, 5, 24).at(23, 59, 59, 999)).account(a1).amount(200).create();
        Transaction t5 = TransactionBuilder.withDb(db).scheduleOnce(DateTime.date(2012, 5, 23).at(0, 0, 0, 45)).account(a1).amount(100).create();
        Transaction t4 = TransactionBuilder.withDb(db).dateTime(DateTime.date(2012, 5, 23).at(0, 0, 0, 45)).account(a1).amount(100).create();
        Transaction t3 = TransactionBuilder.withDb(db).dateTime(DateTime.date(2012, 5, 22).at(12, 0, 12, 345)).account(a1).amount(10).create();
        Transaction t2 = TransactionBuilder.withDb(db).dateTime(DateTime.date(2012, 5, 22).at(12, 0, 12, 345)).account(a1).amount(10).makeTemplate().create();
        Transaction t1 = TransactionBuilder.withDb(db).dateTime(DateTime.date(2012, 5, 21).at(0, 0, 0, 0)).account(a1).amount(-20).create();
        //then
        assertEquals(-1, db.findNearestOlderTransactionId(a1, DateTime.date(2012, 5, 20).asLong()));
        assertEquals(t1.id, db.findNearestOlderTransactionId(a1, DateTime.date(2012, 5, 21).at(15, 30, 30, 456).asLong()));
        assertEquals(t3.id, db.findNearestOlderTransactionId(a1, DateTime.date(2012, 5, 22).asLong()));
        assertEquals(t4.id, db.findNearestOlderTransactionId(a1, DateTime.date(2012, 5, 23).asLong()));
        assertEquals(t6.id, db.findNearestOlderTransactionId(a1, DateTime.date(2012, 5, 24).asLong()));
        assertEquals(t7.id, db.findNearestOlderTransactionId(a1, DateTime.date(2012, 5, 25).asLong()));
        assertEquals(t7.id, db.findNearestOlderTransactionId(a1, DateTime.date(2012, 5, 26).asLong()));
        assertEquals(t8.id, db.findNearestOlderTransactionId(a2, DateTime.date(2012, 5, 26).asLong()));
    }

    public void test_should_delete_old_transactions() {
        //given
        a2 = AccountBuilder.createDefault(db);
        TransactionBuilder.withDb(db).dateTime(DateTime.date(2012, 5, 25).at(17, 30, 45, 0)).account(a2).amount(-234).create();
        TransactionBuilder.withDb(db).dateTime(DateTime.date(2012, 5, 24).at(12, 30, 0, 0)).account(a1).amount(-100)
                .withSplit(categoriesMap.get("A1"), -50)
                .withTransferSplit(a2, -50, 50)
                .create();
        TransactionBuilder.withDb(db).scheduleOnce(DateTime.date(2012, 5, 23).at(0, 0, 0, 45)).account(a1).amount(100).create();
        TransactionBuilder.withDb(db).dateTime(DateTime.date(2012, 5, 23).at(23, 59, 59, 999)).account(a1).amount(10).create();
        TransferBuilder.withDb(db).dateTime(DateTime.date(2012, 5, 22).at(22, 0, 0, 0))
                .fromAccount(a1).fromAmount(10)
                .toAccount(a2).toAmount(10).create();
        TransactionBuilder.withDb(db).dateTime(DateTime.date(2012, 5, 22).at(12, 0, 12, 345)).account(a1).amount(10).makeTemplate().create();
        TransactionBuilder.withDb(db).dateTime(DateTime.date(2012, 5, 21).at(0, 0, 0, 0)).account(a1).amount(-20).create();
        assertTransactionsCount(a1, 8);
        assertTransactionsCount(a2, 1);
        //then
        db.deleteOldTransactions(a1, DateTime.date(2012, 5, 20).asLong());
        assertTransactionsCount(a1, 8);
        assertTransactionsCount(a2, 1);
        //then
        db.deleteOldTransactions(a1, DateTime.date(2012, 5, 21).asLong());
        assertTransactionsCount(a1, 7);
        assertTransactionsCount(a2, 1);
        //then
        db.deleteOldTransactions(a1, DateTime.date(2012, 5, 22).asLong());
        assertTransactionsCount(a1, 6);
        assertTransactionsCount(a2, 1);
        //then
        db.deleteOldTransactions(a1, DateTime.date(2012, 5, 23).asLong());
        assertTransactionsCount(a1, 5);
        assertTransactionsCount(a2, 1);
        //then
        db.deleteOldTransactions(a1, DateTime.date(2012, 5, 25).asLong());
        assertTransactionsCount(a1, 2);
        assertTransactionsCount(a2, 1);
        //then
        db.deleteOldTransactions(a2, DateTime.date(2012, 5, 25).asLong());
        assertTransactionsCount(a1, 2);
        assertTransactionsCount(a2, 0);
    }

    public void test_should_find_latest_transaction_date_for_an_account() {
        //given
        Account a2 = AccountBuilder.createDefault(db);
        Account a3 = AccountBuilder.createDefault(db);
        Account a4 = AccountBuilder.createDefault(db);
        TransactionBuilder.withDb(db).dateTime(DateTime.date(2012, 5, 25).at(17, 30, 45, 0)).account(a2).amount(-234).create(); //L2
        TransactionBuilder.withDb(db).scheduleOnce(DateTime.date(2012, 5, 23).at(0, 0, 0, 45)).account(a1).amount(100).create();
        TransactionBuilder.withDb(db).dateTime(DateTime.date(2012, 5, 23).at(23, 59, 59, 999)).account(a1).amount(10).create(); //L1
        TransactionBuilder.withDb(db).dateTime(DateTime.date(2012, 5, 22).at(12, 30, 0, 0)).account(a1).amount(-100)
                .withSplit(categoriesMap.get("A1"), -50)
                .withTransferSplit(a3, -50, 50) //L3
                .create();
        TransactionBuilder.withDb(db).dateTime(DateTime.date(2012, 5, 21).at(12, 0, 12, 345)).account(a2).amount(10).makeTemplate().create();
        TransactionBuilder.withDb(db).dateTime(DateTime.date(2012, 5, 20).at(0, 0, 0, 0)).account(a2).amount(-20).create();
        //then
        assertEquals(DateTime.date(2012, 5, 23).at(23, 59, 59, 999).asLong(), db.findLatestTransactionDate(a1.id));
        assertEquals(DateTime.date(2012, 5, 25).at(17, 30, 45, 0).asLong(), db.findLatestTransactionDate(a2.id));
        assertEquals(DateTime.date(2012, 5, 22).at(12, 30, 0, 0).asLong(), db.findLatestTransactionDate(a3.id));
        assertEquals(0, db.findLatestTransactionDate(a4.id));
    }

    MyEntity[] systemEntities = new MyEntity[]{
            Category.noCategory(),
            Category.splitCategory(),
            Attribute.deleteAfterExpired(),
            Project.noProject(),
            MyLocation.currentLocation()
    };

    public void test_should_restore_system_entities() {
        //given
        givenSystemEntitiesHaveBeenDeleted();
        //when
        db.restoreSystemEntities();
        //then
        for (MyEntity e : systemEntities) {
            MyEntity myEntity = db.get(e.getClass(), e.id);
            assertNotNull(e.getClass() + ":" + e.getTitle(), myEntity);
        }
        Category c = db.get(Category.class, Category.NO_CATEGORY_ID);
        assertNotNull(c);
        assertEquals("<NO_CATEGORY>", c.title);
    }

    private void givenSystemEntitiesHaveBeenDeleted() {
        for (MyEntity e : systemEntities) {
            db.delete(e.getClass(), e.id);
            assertNull(db.get(e.getClass(), e.id));
        }
    }

    public void test_account_number_lookup() {
        Account account = AccountBuilder.withDb(db)
                .currency(CurrencyBuilder.createDefault(db))
                .title("SB").issuer("Sber").number("1111-2222-3333-5431").create();

        final List<Long> res = db.findAccountsByNumber("5431");
        Assert.assertTrue(res.size() == 1);
        Assert.assertEquals((Long) account.id, res.get(0));
    }

    private String fetchFirstPayee(String s) {
        Cursor c = db.getAllPayeesLike(s);
        try {
            if (c.moveToFirst()) {
                Payee p = EntityManager.loadFromCursor(c, Payee.class);
                return p.title;
            }
            return null;
        } finally {
            c.close();
        }
    }

}
