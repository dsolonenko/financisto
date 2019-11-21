package ru.orangesoftware.financisto.model;

import android.content.Intent;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.orangesoftware.financisto.db.AbstractDbTest;
import ru.orangesoftware.financisto.test.AccountBuilder;
import ru.orangesoftware.financisto.test.CategoryBuilder;
import ru.orangesoftware.financisto.test.CurrencyBuilder;
import ru.orangesoftware.financisto.test.TransactionBuilder;

import static ru.orangesoftware.financisto.test.AttributeBuilder.attributeValue;

import static org.junit.Assert.*;

public class TransactionTest extends AbstractDbTest {

    Account a1;
    Account a2;
    Map<String, Category> categories;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        Currency c1 = CurrencyBuilder.withDb(db).name("USD").title("Dollar").symbol("$").create();
        Currency c2 = CurrencyBuilder.withDb(db).name("SGD").title("Singapore Dollar").symbol("S$").create();
        a1 = AccountBuilder.createDefault(db, c1);
        a2 = AccountBuilder.createDefault(db, c2);
        categories = CategoryBuilder.createDefaultHierarchy(db);
    }

    @Test
    public void should_create_splits() {
        Transaction t = TransactionBuilder.withDb(db).account(a1).amount(200).payee("P1").category(CategoryBuilder.split(db))
                .withSplit(categories.get("A1"), 60)
                .withSplit(categories.get("A2"), 40)
                .withTransferSplit(a2, 100, 50)
                .create();
        List<Transaction> splits = db.getSplitsForTransaction(t.id);
        assertEquals(3, splits.size());
        Transaction split1 = splits.get(0);
        assertEquals(t.payeeId, split1.payeeId);
        assertEquals(a1.id, split1.fromAccountId);
        assertEquals(60, split1.fromAmount);
        assertEquals(categories.get("A1").id, split1.categoryId);
        Transaction split2 = splits.get(1);
        assertEquals(t.payeeId, split2.payeeId);
        assertEquals(a1.id, split2.fromAccountId);
        assertEquals(40, split2.fromAmount);
        assertEquals(categories.get("A2").id, split2.categoryId);
        Transaction split3 = splits.get(2);
        assertEquals(t.payeeId, split3.payeeId);
        assertEquals(a1.id, split3.fromAccountId);
        assertEquals(a2.id, split3.toAccountId);
        assertEquals(100, split3.fromAmount);
        assertEquals(50, split3.toAmount);
    }

    @Test
    public void should_insert_and_update_attributes() {
        //given
        Category aa1 = categories.get("AA1");
        Attribute attr1 = aa1.attributes.get(0);
        Attribute attr2 = aa1.attributes.get(1);
        //when inserted
        Transaction t1 = TransactionBuilder.withDb(db).account(a1).amount(1000).category(aa1)
                .withAttributes(attributeValue(attr1, "value1"), attributeValue(attr2, "value2"))
                .create();
        Transaction t2 = TransactionBuilder.withDb(db).account(a2).amount(2000)
                .withSplit(aa1, 600, "Note1", null, attributeValue(attr1, "value11"))
                .withSplit(aa1, 1400, "Note2", null, attributeValue(attr2, "value21"))
                .create();
        //then
        assertAttributes(t1, attributeValue(attr1, "value1"), attributeValue(attr2, "value2"));
        List<Transaction> splits = db.getSplitsForTransaction(t2.id);
        assertAttributes(splits.get(0), attributeValue(attr1, "value11"));
        assertAttributes(splits.get(1), attributeValue(attr2, "value21"));
        //when modified
        db.insertOrUpdate(t1, Arrays.asList(attributeValue(attr2, "value3")));
        splits.get(0).categoryAttributes = asMap(attributeValue(attr1, "value111"), attributeValue(attr2, "value222"));
        splits.get(1).categoryAttributes = asMap(attributeValue(attr1, "value333"));
        t2.splits = splits;
        db.insertOrUpdate(t2);
        //then
        assertAttributes(t1, attributeValue(attr2, "value3"));
        splits = db.getSplitsForTransaction(t2.id);
        assertAttributes(splits.get(0), attributeValue(attr1, "value111"), attributeValue(attr2, "value222"));
        assertAttributes(splits.get(1), attributeValue(attr1, "value333"));
    }

    private Map<Long, String> asMap(TransactionAttribute... values) {
        Map<Long, String> map = new HashMap<Long, String>();
        for (TransactionAttribute value : values) {
            map.put(value.attributeId, value.value);
        }
        return map;
    }

    private void assertAttributes(Transaction t, TransactionAttribute... values) {
        Map<Long, String> attributes = db.getAllAttributesForTransaction(t.id);
        assertEquals(values.length, attributes.size());
        for (TransactionAttribute value : values) {
            assertEquals(value.value, attributes.get(value.attributeId));
        }
    }

    @Test
    public void should_duplicate_splits() {
        Transaction t = TransactionBuilder.withDb(db).account(a1).amount(-150).category(CategoryBuilder.split(db))
                .withSplit(categories.get("A1"), -60)
                .withSplit(categories.get("A2"), -40)
                .withTransferSplit(a2, -50, 40)
                .create();
        List<Transaction> splits = db.getSplitsForTransaction(t.id);
        assertEquals(3, splits.size());
        long newId = db.duplicateTransaction(t.id);
        assertNotSame(t.id, newId);
        List<Transaction> newSplits = db.getSplitsForTransaction(newId);
        assertEquals(3, newSplits.size());
        assertEquals(-150, newSplits.get(0).fromAmount + newSplits.get(1).fromAmount + newSplits.get(2).fromAmount);
    }

    @Test
    public void should_convert_split_into_regular_transaction() {
        Transaction t = TransactionBuilder.withDb(db).account(a1).amount(2000)
                .withSplit(categories.get("A1"), 500)
                .withSplit(categories.get("A2"), 1500)
                .create();
        List<Transaction> splits = db.getSplitsForTransaction(t.id);
        assertEquals(2, splits.size());
        t.categoryId = categories.get("A").id;
        t.splits = null;
        db.insertOrUpdate(t);
        splits = db.getSplitsForTransaction(t.id);
        assertEquals(0, splits.size());
    }

    @Test
    public void should_update_splits() {
        Transaction t = TransactionBuilder.withDb(db).account(a1).amount(-150).category(CategoryBuilder.split(db))
                .withSplit(categories.get("A1"), -60)
                .withSplit(categories.get("A2"), -40)
                .withTransferSplit(a2, -50, 40)
                .create();
        List<Transaction> splits = db.getSplitsForTransaction(t.id);
        assertEquals(3, splits.size());
        t.fromAmount = -250;
        splits.get(0).fromAmount = -70;
        splits.get(1).fromAmount = -50;
        splits.get(2).fromAmount = -130;
        splits.get(2).toAmount = 70;
        t.splits = splits;
        db.insertOrUpdate(t);
        splits = db.getSplitsForTransaction(t.id);
        assertEquals(3, splits.size());
    }

    @Test
    public void should_delete_splits() {
        Transaction t = TransactionBuilder.withDb(db).account(a1).amount(-150).category(CategoryBuilder.split(db))
                .withSplit(categories.get("A1"), -60)
                .withSplit(categories.get("A2"), -40)
                .withTransferSplit(a2, -50, 40)
                .create();
        List<Transaction> splits = db.getSplitsForTransaction(t.id);
        assertEquals(3, splits.size());
        db.deleteTransaction(t.id);
        splits = db.getSplitsForTransaction(t.id);
        assertEquals(0, splits.size());
    }

    @Test
    public void should_store_transaction_in_the_database() {
        Transaction t = new Transaction();
        t.fromAccountId = 1;
        t.fromAmount = 1000;
        t.categoryId = 5;
        t.accuracy = 6.0f;
        t.latitude = -11.0;
        t.isCCardPayment = 1;
        t.note = "My note";
        t.status = TransactionStatus.RS;
        long id = db.saveOrUpdate(t);
        assertTrue(id > 0);
        Transaction restored = db.load(Transaction.class, id);
        assertEquals(t.fromAccountId, restored.fromAccountId);
        assertEquals(t.fromAmount, restored.fromAmount);
        assertEquals(t.categoryId, restored.categoryId);
        assertEquals(t.note, restored.note);
        assertEquals(t.status, restored.status);
        assertEquals(t.accuracy, restored.accuracy, 0.001);
        assertEquals(t.latitude, restored.latitude, 0.001);
        assertEquals(t.isCCardPayment, restored.isCCardPayment);
    }

    @Test
    public void should_restore_split_from_intent() {
        Transaction split = new Transaction();
        split.id = -2;
        split.fromAccountId = 3;
        split.toAccountId = 5;
        split.categoryId = 7;
        split.fromAmount = -10000;
        split.toAmount = 4000;
        split.unsplitAmount = 300000;
        split.note = "My note";
        Intent intent = new Intent();
        split.toIntentAsSplit(intent);
        Transaction restored = Transaction.fromIntentAsSplit(intent);
        assertEquals(split.id, restored.id);
        assertEquals(split.fromAccountId, restored.fromAccountId);
        assertEquals(split.toAccountId, restored.toAccountId);
        assertEquals(split.categoryId, restored.categoryId);
        assertEquals(split.fromAmount, restored.fromAmount);
        assertEquals(split.toAmount, restored.toAmount);
        assertEquals(split.unsplitAmount, restored.unsplitAmount);
        assertEquals(split.note, restored.note);
    }

    @Test
    public void should_update_original_amount_for_splits() {
        Transaction t = TransactionBuilder.withDb(db).account(a1).category(CategoryBuilder.split(db))
                .amount(120).originalAmount(a2.currency, 100)
                .withSplit(categories.get("A1"), 60)
                .withSplit(categories.get("A2"), 40)
                .create();
        List<Transaction> splits = db.getSplitsForTransaction(t.id);
        assertEquals(2, splits.size());
        assertSplit(splits.get(0), t.originalCurrencyId, 60, 72);
        assertSplit(splits.get(1), t.originalCurrencyId, 40, 48);
    }

    private void assertSplit(Transaction split, long originalCurrencyId, long originalAmount, long accountAmount) {
        assertEquals(originalCurrencyId, split.originalCurrencyId);
        assertEquals(originalAmount, split.originalFromAmount);
        assertEquals(accountAmount, split.fromAmount);
    }

}
