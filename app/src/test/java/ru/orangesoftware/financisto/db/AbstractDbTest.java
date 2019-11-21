package ru.orangesoftware.financisto.db;

import android.Manifest;
import android.app.Application;
import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowApplication;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.Transaction;
import ru.orangesoftware.financisto.test.DateTime;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public abstract class AbstractDbTest {

    private DatabaseHelper dbHelper;
    protected DatabaseAdapter db;
    protected Context context;

    @Before
    public void setUp() throws Exception {

        Application application = ApplicationProvider.getApplicationContext();
        ShadowApplication app = Shadows.shadowOf(application);
        app.grantPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE);

        context = application;
        dbHelper = new DatabaseHelper(context);
        db = new TestDatabaseAdapter(context, dbHelper);
        db.open();
    }

    @After
    public void tearDown() {
        dbHelper.close();
    }

    protected Context getContext() {
        return context;
    }

    public void assertAccountTotal(Account account, long total) {
        Account a = db.getAccount(account.id);
        assertEquals("Account " + account.id + " total", total, a.totalAmount);
    }

    public void assertLastTransactionDate(Account account, DateTime dateTime) {
        Account a = db.getAccount(account.id);
        assertEquals("Account " + account.id + " last transaction date", dateTime.asLong(), a.lastTransactionDate);
    }

    public void assertFinalBalanceForAccount(Account account, long expectedBalance) {
        long balance = db.getLastRunningBalanceForAccount(account);
        assertEquals("Account " + account.id + " final balance", expectedBalance, balance);
    }

    public void assertAccountBalanceForTransaction(Transaction t, Account a, long expectedBalance) {
        long balance = db.getAccountBalanceForTransaction(a, t);
        assertEquals(expectedBalance, balance);
    }

    public void assertTransactionsCount(Account account, long expectedCount) {
        long count = DatabaseUtils.rawFetchLongValue(db,
                "select count(*) from transactions where from_account_id=?",
                new String[]{String.valueOf(account.id)});
        assertEquals("Transaction for account " + account.id, expectedCount, count);
    }

    public void assertCategory(String name, boolean isIncome, Category c) {
        assertEquals(name, c.title);
        assertEquals(isIncome, c.isIncome());
    }

    public static <T> Set<T> asSet(T... values) {
        return new HashSet<T>(Arrays.asList(values));
    }

}
