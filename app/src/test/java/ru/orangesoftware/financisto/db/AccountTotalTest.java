package ru.orangesoftware.financisto.db;

import org.junit.Test;

import java.util.List;
import java.util.Map;

import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.Transaction;
import ru.orangesoftware.financisto.test.AccountBuilder;
import ru.orangesoftware.financisto.test.CategoryBuilder;
import ru.orangesoftware.financisto.test.TransactionBuilder;
import ru.orangesoftware.financisto.test.TransferBuilder;

import static org.junit.Assert.*;

public class AccountTotalTest extends AbstractDbTest {

    Account a1;
    Account a2;
    Account a3;
    Map<String, Category> categoriesMap;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        a1 = AccountBuilder.createDefault(db);
        a2 = AccountBuilder.createDefault(db);
        a3 = AccountBuilder.createDefault(db);
        categoriesMap = CategoryBuilder.createDefaultHierarchy(db);
    }

    @Test
    public void should_update_account_total_when_credit_transaction_is_added() {
        TransactionBuilder.withDb(db).account(a1).amount(1000).create();
        assertAccountTotal(a1, 1000);

        TransactionBuilder.withDb(db).account(a1).amount(1234).create();
        assertAccountTotal(a1, 2234);

        db.recalculateAccountsBalances();
        assertAccountTotal(a1, 2234);
    }

    @Test
    public void should_update_account_total_when_debit_transaction_is_added() {
        TransactionBuilder.withDb(db).account(a1).amount(-1000).create();
        assertAccountTotal(a1, -1000);

        TransactionBuilder.withDb(db).account(a1).amount(-1234).create();
        assertAccountTotal(a1, -2234);

        db.recalculateAccountsBalances();
        assertAccountTotal(a1, -2234);
    }

    @Test
    public void should_update_account_total_when_credit_transaction_is_updated_with_bigger_amount() {
        Transaction t = TransactionBuilder.withDb(db).account(a1).amount(1000).create();
        assertAccountTotal(a1, 1000);

        t.fromAmount = 1234;
        db.insertOrUpdate(t);
        assertAccountTotal(a1, 1234);

        db.recalculateAccountsBalances();
        assertAccountTotal(a1, 1234);
    }

    @Test
    public void should_update_account_total_when_credit_transaction_is_updated_with_lesser_amount() {
        Transaction t = TransactionBuilder.withDb(db).account(a1).amount(1000).create();
        assertAccountTotal(a1, 1000);

        t.fromAmount = 900;
        db.insertOrUpdate(t);
        assertAccountTotal(a1, 900);

        db.recalculateAccountsBalances();
        assertAccountTotal(a1, 900);
    }

    @Test
    public void should_update_account_total_when_debit_transaction_is_updated_with_lesser_amount() {
        Transaction t = TransactionBuilder.withDb(db).account(a1).amount(-1000).create();
        assertAccountTotal(a1, -1000);

        t.fromAmount = -900;
        db.insertOrUpdate(t);
        assertAccountTotal(a1, -900);

        db.recalculateAccountsBalances();
        assertAccountTotal(a1, -900);
    }

    @Test
    public void should_update_account_total_when_debit_transaction_is_updated_with_bigger_amount() {
        Transaction t = TransactionBuilder.withDb(db).account(a1).amount(-1000).create();
        assertAccountTotal(a1, -1000);

        t.fromAmount = -1920;
        db.insertOrUpdate(t);
        assertAccountTotal(a1, -1920);

        db.recalculateAccountsBalances();
        assertAccountTotal(a1, -1920);
    }

    @Test
    public void should_update_account_total_when_credit_transaction_is_converted_to_debit_and_back() {
        Transaction t = TransactionBuilder.withDb(db).account(a1).amount(1000).create();
        assertAccountTotal(a1, 1000);

        t.fromAmount = -550;
        db.insertOrUpdate(t);
        assertAccountTotal(a1, -550);

        t.fromAmount = 226;
        db.insertOrUpdate(t);
        assertAccountTotal(a1, 226);

        db.recalculateAccountsBalances();
        assertAccountTotal(a1, 226);
    }

    @Test
    public void should_update_account_total_when_debit_transaction_is_converted_to_credit_and_back() {
        Transaction t = TransactionBuilder.withDb(db).account(a1).amount(-1000).create();
        assertAccountTotal(a1, -1000);

        t.fromAmount = 245;
        db.insertOrUpdate(t);
        assertAccountTotal(a1, 245);

        t.fromAmount = -110;
        db.insertOrUpdate(t);
        assertAccountTotal(a1, -110);

        db.recalculateAccountsBalances();
        assertAccountTotal(a1, -110);
    }

    @Test
    public void should_update_account_total_after_mix_of_debit_and_credit_transactions() {
        Transaction t = TransactionBuilder.withDb(db).account(a1).amount(1000).create();
        assertAccountTotal(a1, 1000);

        t.fromAmount = 900; // 1000 - 100
        db.insertOrUpdate(t);
        assertAccountTotal(a1, 900);

        t = TransactionBuilder.withDb(db).account(a1).amount(-123).create();  // 900 - 123
        assertAccountTotal(a1, 777);

        t.fromAmount = -120; // 777 + 3
        db.insertOrUpdate(t);
        assertAccountTotal(a1, 780);

        TransactionBuilder.withDb(db).account(a1).amount(100).create();  // 780 + 100
        assertAccountTotal(a1, 880);

        TransactionBuilder.withDb(db).account(a1).amount(-80).create();  // 880 - 80
        assertAccountTotal(a1, 800);

        db.recalculateAccountsBalances();
        assertAccountTotal(a1, 800);
    }

    @Test
    public void should_update_account_total_when_transaction_is_deleted() {
        TransactionBuilder.withDb(db).account(a1).amount(1000).create();
        assertAccountTotal(a1, 1000);

        Transaction t = TransactionBuilder.withDb(db).account(a1).amount(1234).create();
        assertAccountTotal(a1, 2234);

        TransactionBuilder.withDb(db).account(a1).amount(-234).create();
        assertAccountTotal(a1, 2000);

        db.deleteTransaction(t.id);
        assertAccountTotal(a1, 766);

        db.recalculateAccountsBalances();
        assertAccountTotal(a1, 766);
    }

    @Test
    public void should_update_accounts_total_when_transfer_is_added_a() {
        TransactionBuilder.withDb(db).account(a1).amount(1000).create();
        TransactionBuilder.withDb(db).account(a2).amount(2000).create();
        assertAccountTotal(a1, 1000);
        assertAccountTotal(a2, 2000);

        TransferBuilder.withDb(db).fromAccount(a1).fromAmount(-500).toAccount(a2).toAmount(400).create();
        assertAccountTotal(a1, 500);
        assertAccountTotal(a2, 2400);

        db.recalculateAccountsBalances();
        assertAccountTotal(a1, 500);
        assertAccountTotal(a2, 2400);
    }

    @Test
    public void should_update_accounts_total_when_transfer_is_updated() {
        TransactionBuilder.withDb(db).account(a1).amount(1000).create();
        TransactionBuilder.withDb(db).account(a2).amount(2000).create();
        assertAccountTotal(a1, 1000);
        assertAccountTotal(a2, 2000);

        Transaction t = TransferBuilder.withDb(db).fromAccount(a1).fromAmount(-500).toAccount(a2).toAmount(400).create();
        assertAccountTotal(a1, 500);
        assertAccountTotal(a2, 2400);

        t.fromAmount = -600;
        t.toAmount = 500;
        db.insertOrUpdate(t);
        assertAccountTotal(a1, 400);
        assertAccountTotal(a2, 2500);

        db.recalculateAccountsBalances();
        assertAccountTotal(a1, 400);
        assertAccountTotal(a2, 2500);
    }

    @Test
    public void should_update_accounts_total_when_transfer_is_deleted() {
        TransactionBuilder.withDb(db).account(a1).amount(1000).create();
        TransactionBuilder.withDb(db).account(a2).amount(2000).create();
        assertAccountTotal(a1, 1000);
        assertAccountTotal(a2, 2000);

        Transaction t = TransferBuilder.withDb(db).fromAccount(a1).fromAmount(-500).toAccount(a2).toAmount(400).create();
        assertAccountTotal(a1, 500);
        assertAccountTotal(a2, 2400);

        db.deleteTransaction(t.id);
        assertAccountTotal(a1, 1000);
        assertAccountTotal(a2, 2000);

        db.recalculateAccountsBalances();
        assertAccountTotal(a1, 1000);
        assertAccountTotal(a2, 2000);
    }

    @Test
    public void should_update_account_total_when_regular_split_is_added() {
        TransactionBuilder.withDb(db).account(a1).amount(1000).create();
        assertAccountTotal(a1, 1000);

        TransactionBuilder.withDb(db).account(a1).amount(-500)
                .withSplit(categoriesMap.get("A1"), -400)
                .withSplit(categoriesMap.get("A2"), -100)
                .create();
        assertAccountTotal(a1, 500);

        db.recalculateAccountsBalances();
        assertAccountTotal(a1, 500);
    }

    @Test
    public void should_update_account_total_when_regular_split_is_updated() {
        TransactionBuilder.withDb(db).account(a1).amount(1000).create();
        assertAccountTotal(a1, 1000);

        Transaction t = TransactionBuilder.withDb(db).account(a1).amount(500)
                .withSplit(categoriesMap.get("A1"), 400)
                .withSplit(categoriesMap.get("A2"), 100)
                .create();
        assertAccountTotal(a1, 1500);

        List<Transaction> splits = db.getSplitsForTransaction(t.id);
        assertEquals(2, splits.size());

        t.fromAmount = 800;
        splits.get(0).fromAmount = 500;
        splits.get(1).fromAmount = 300;
        t.splits = splits;
        db.insertOrUpdate(t);
        assertAccountTotal(a1, 1800);

        db.recalculateAccountsBalances();
        assertAccountTotal(a1, 1800);
    }

    @Test
    public void should_update_account_total_when_regular_split_is_deleted() {
        TransactionBuilder.withDb(db).account(a1).amount(1000).create();
        assertAccountTotal(a1, 1000);

        Transaction t = TransactionBuilder.withDb(db).account(a1).amount(500)
                .withSplit(categoriesMap.get("A1"), 400)
                .withSplit(categoriesMap.get("A2"), 100)
                .create();
        assertAccountTotal(a1, 1500);

        db.deleteTransaction(t.id);
        assertAccountTotal(a1, 1000);

        db.recalculateAccountsBalances();
        assertAccountTotal(a1, 1000);
    }

    @Test
    public void should_update_account_total_when_transfer_split_is_added() {
        TransactionBuilder.withDb(db).account(a1).amount(1000).create();
        TransactionBuilder.withDb(db).account(a2).amount(2000).create();
        assertAccountTotal(a1, 1000);
        assertAccountTotal(a2, 2000);

        TransactionBuilder.withDb(db).account(a1).amount(-500)
                .withTransferSplit(a2, -500, 200)
                .create();
        assertAccountTotal(a1, 500);
        assertAccountTotal(a2, 2200);

        db.recalculateAccountsBalances();
        assertAccountTotal(a1, 500);
        assertAccountTotal(a2, 2200);
    }

    @Test
    public void should_update_account_total_when_transfer_split_is_updated() {
        TransactionBuilder.withDb(db).account(a1).amount(1000).create();
        TransactionBuilder.withDb(db).account(a2).amount(2000).create();
        assertAccountTotal(a1, 1000);
        assertAccountTotal(a2, 2000);

        Transaction t = TransactionBuilder.withDb(db).account(a1).amount(-500)
                .withTransferSplit(a2, -500, 200)
                .create();
        assertAccountTotal(a1, 500);
        assertAccountTotal(a2, 2200);

        List<Transaction> splits = db.getSplitsForTransaction(t.id);
        t.fromAmount = -400;
        splits.get(0).fromAmount = -400;
        splits.get(0).toAmount = 100;
        t.splits = splits;
        db.insertOrUpdate(t);
        assertAccountTotal(a1, 600);
        assertAccountTotal(a2, 2100);

        db.recalculateAccountsBalances();
        assertAccountTotal(a1, 600);
        assertAccountTotal(a2, 2100);
    }

    @Test
    public void should_update_account_total_when_transfer_split_is_deleted() {
        TransactionBuilder.withDb(db).account(a1).amount(1000).create();
        TransactionBuilder.withDb(db).account(a2).amount(2000).create();
        assertAccountTotal(a1, 1000);
        assertAccountTotal(a2, 2000);

        Transaction t = TransactionBuilder.withDb(db).account(a1).amount(-500)
                .withTransferSplit(a2, -500, 200)
                .create();
        assertAccountTotal(a1, 500);
        assertAccountTotal(a2, 2200);

        db.deleteTransaction(t.id);
        assertAccountTotal(a1, 1000);
        assertAccountTotal(a2, 2000);

        db.recalculateAccountsBalances();
        assertAccountTotal(a1, 1000);
        assertAccountTotal(a2, 2000);
    }

    @Test
    public void should_update_account_total_when_transfer_split_with_multiple_transfers_is_added() {
        TransactionBuilder.withDb(db).account(a1).amount(2000).create();
        TransactionBuilder.withDb(db).account(a2).amount(3000).create();
        TransactionBuilder.withDb(db).account(a3).amount(4000).create();
        Transaction t = TransactionBuilder.withDb(db).account(a1).amount(-1000)
                .withTransferSplit(a2, -100, 50)
                .withTransferSplit(a2, -200, 60)
                .withTransferSplit(a2, -300, 70)
                .withTransferSplit(a3, -100, 80)
                .withTransferSplit(a3, -300, 90)
                .create();
        assertAccountTotal(a1, 1000);
        assertAccountTotal(a2, 3180);
        assertAccountTotal(a3, 4170);

        db.deleteTransaction(t.id);
        assertAccountTotal(a1, 2000);
        assertAccountTotal(a2, 3000);
        assertAccountTotal(a3, 4000);

        db.recalculateAccountsBalances();
        assertAccountTotal(a1, 2000);
        assertAccountTotal(a2, 3000);
        assertAccountTotal(a3, 4000);
    }

}
