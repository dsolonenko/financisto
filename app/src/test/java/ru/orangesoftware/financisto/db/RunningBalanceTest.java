package ru.orangesoftware.financisto.db;

import org.junit.Test;

import java.util.List;
import java.util.Map;

import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.Transaction;
import ru.orangesoftware.financisto.test.AccountBuilder;
import ru.orangesoftware.financisto.test.CategoryBuilder;
import ru.orangesoftware.financisto.test.DateTime;
import ru.orangesoftware.financisto.test.TransactionBuilder;
import ru.orangesoftware.financisto.test.TransferBuilder;

import static org.junit.Assert.*;

public class RunningBalanceTest extends AbstractDbTest {

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
    public void should_update_running_balance_for_single_account() {
        Transaction t1 = TransactionBuilder.withDb(db).account(a1).amount(1000).create();
        Transaction t2 = TransactionBuilder.withDb(db).account(a1).amount(1234).create();
        db.rebuildRunningBalanceForAccount(a1);
        assertAccountBalanceForTransaction(t1, a1, 1000);
        assertAccountBalanceForTransaction(t2, a1, 2234);
        assertFinalBalanceForAccount(a1, 2234);
    }

    @Test
    public void should_update_running_balance_for_single_account_with_transactions_having_exactly_the_same_datetime() {
        DateTime dt = DateTime.fromTimestamp(System.currentTimeMillis());
        Transaction t1 = TransactionBuilder.withDb(db).account(a1).amount(1000).dateTime(dt).create();
        Transaction t2 = TransactionBuilder.withDb(db).account(a1).amount(2000).dateTime(dt).create();
        Transaction t3 = TransactionBuilder.withDb(db).account(a1).amount(3000).dateTime(dt).create();
        Transaction t4 = TransactionBuilder.withDb(db).account(a1).amount(4000).dateTime(dt).create();
        Transaction t5 = TransactionBuilder.withDb(db).account(a1).amount(5000).dateTime(dt).create();
        assertFinalBalanceForAccount(a1, 15000);
        assertAccountBalanceForTransaction(t1, a1, 1000);
        assertAccountBalanceForTransaction(t2, a1, 3000);
        assertAccountBalanceForTransaction(t3, a1, 6000);
        assertAccountBalanceForTransaction(t4, a1, 10000);
        assertAccountBalanceForTransaction(t5, a1, 15000);
        db.rebuildRunningBalanceForAccount(a1);
        assertFinalBalanceForAccount(a1, 15000);
        assertAccountBalanceForTransaction(t1, a1, 1000);
        assertAccountBalanceForTransaction(t2, a1, 3000);
        assertAccountBalanceForTransaction(t3, a1, 6000);
        assertAccountBalanceForTransaction(t4, a1, 10000);
        assertAccountBalanceForTransaction(t5, a1, 15000);
    }

    @Test
    public void should_not_duplicate_running_balance_with_splits() {
        // add new transaction before split
        Transaction t1 = TransactionBuilder.withDb(db).account(a1).amount(2000).create();
        db.rebuildRunningBalanceForAccount(a1);
        // insert new split
        Transaction t2 = TransactionBuilder.withDb(db).account(a1).amount(1000)
                .withSplit(categoriesMap.get("A"), 600)
                .withSplit(categoriesMap.get("B"), 400).create();
        assertAccountBalanceForTransaction(t1, a1, 2000);
        assertAccountBalanceForTransaction(t2, a1, 3000);
        assertFinalBalanceForAccount(a1, 3000);
        // add new transaction after split
        Transaction t3 = TransactionBuilder.withDb(db).account(a1).amount(-1500).create();
        assertAccountBalanceForTransaction(t1, a1, 2000);
        assertAccountBalanceForTransaction(t2, a1, 3000);
        assertAccountBalanceForTransaction(t3, a1, 1500);
        assertFinalBalanceForAccount(a1, 1500);
        // rebuild balance
        db.rebuildRunningBalances();
        assertAccountBalanceForTransaction(t1, a1, 2000);
        assertAccountBalanceForTransaction(t2, a1, 3000);
        assertAccountBalanceForTransaction(t3, a1, 1500);
        assertFinalBalanceForAccount(a1, 1500);
    }

    @Test
    public void should_update_running_balance_for_two_accounts() {
        Transaction t1 = TransactionBuilder.withDb(db).account(a1).amount(1000).create();
        Transaction t2 = TransactionBuilder.withDb(db).account(a2).amount(2000).create();
        Transaction t3 = TransferBuilder.withDb(db).fromAccount(a1).fromAmount(-500).toAccount(a2).toAmount(500).create();
        db.rebuildRunningBalances();
        assertAccountBalanceForTransaction(t1, a1, 1000);
        assertAccountBalanceForTransaction(t2, a2, 2000);
        assertAccountBalanceForTransaction(t3, a1, 500);
        assertAccountBalanceForTransaction(t3, a2, 2500);
        assertFinalBalanceForAccount(a1, 500);
        assertFinalBalanceForAccount(a2, 2500);
    }

    @Test
    public void should_update_running_balance_for_account_which_has_transfer_to_the_same_account() {
        Transaction t1 = TransactionBuilder.withDb(db).account(a1).amount(1000).create();
        Transaction t2 = TransactionBuilder.withDb(db).account(a2).amount(2000).create();
        Transaction t3 = TransferBuilder.withDb(db).fromAccount(a1).fromAmount(-500).toAccount(a1).toAmount(500).create();
        db.rebuildRunningBalances();
        assertAccountBalanceForTransaction(t1, a1, 1000);
        assertAccountBalanceForTransaction(t2, a2, 2000);
        assertAccountBalanceForTransaction(t3, a1, 0);
        assertFinalBalanceForAccount(a1, 1000);
        assertFinalBalanceForAccount(a2, 2000);
    }

    @Test
    public void should_update_running_balance_for_two_accounts_with_transfer_split() {
        Transaction t1 = TransactionBuilder.withDb(db).account(a1).amount(1000).create();
        Transaction t2 = TransactionBuilder.withDb(db).account(a2).amount(2000).create();
        db.rebuildRunningBalances();
        Transaction t3 = TransactionBuilder.withDb(db).account(a1).amount(-100)
                .withTransferSplit(a2, -100, 50).create();
        assertAccountBalanceForTransaction(t1, a1, 1000);
        assertAccountBalanceForTransaction(t2, a2, 2000);
        assertAccountBalanceForTransaction(t3, a1, 900);
        List<Transaction> splits = db.getSplitsForTransaction(t3.id);
        assertEquals(1, splits.size());
        // running balance is attach to the split, not to the parent transaction!
        assertAccountBalanceForTransaction(splits.get(0), a2, 2050);
        assertFinalBalanceForAccount(a1, 900);
        assertFinalBalanceForAccount(a2, 2050);
        db.rebuildRunningBalances();
        assertAccountBalanceForTransaction(splits.get(0), a2, 2050);
        assertFinalBalanceForAccount(a1, 900);
        assertFinalBalanceForAccount(a2, 2050);
    }

    @Test
    public void should_update_running_balance_for_two_accounts_with_transfer_split_with_multiple_transfers() {
        TransactionBuilder.withDb(db).account(a1).amount(2000).create();
        TransactionBuilder.withDb(db).account(a2).amount(3000).create();
        TransactionBuilder.withDb(db).account(a3).amount(4000).create();
        db.rebuildRunningBalances();
        Transaction t4 = TransactionBuilder.withDb(db).account(a1).amount(-1000)
                .withTransferSplit(a2, -100, 50)
                .withTransferSplit(a2, -200, 60)
                .withTransferSplit(a2, -300, 70)
                .withTransferSplit(a3, -100, 80)
                .withTransferSplit(a3, -300, 90)
                .create();
        assertFinalBalanceForAccount(a1, 1000);
        assertFinalBalanceForAccount(a2, 3180);
        assertFinalBalanceForAccount(a3, 4170);
        List<Transaction> splits = db.getSplitsForTransaction(t4.id);
        assertEquals(5, splits.size());
        assertAccountBalanceForTransaction(splits.get(0), a2, 3050);
        assertAccountBalanceForTransaction(splits.get(1), a2, 3110);
        assertAccountBalanceForTransaction(splits.get(2), a2, 3180);
        assertAccountBalanceForTransaction(splits.get(3), a3, 4080);
        assertAccountBalanceForTransaction(splits.get(4), a3, 4170);
        db.rebuildRunningBalances();
        assertFinalBalanceForAccount(a1, 1000);
        assertFinalBalanceForAccount(a2, 3180);
        assertFinalBalanceForAccount(a3, 4170);
        db.deleteTransaction(t4.id);
        assertFinalBalanceForAccount(a1, 2000);
        assertFinalBalanceForAccount(a2, 3000);
        assertFinalBalanceForAccount(a3, 4000);
    }

    @Test
    public void should_update_running_balance_for_two_accounts_when_updating_transfer_split() {
        Transaction t1 = TransactionBuilder.withDb(db).account(a1).amount(1000).create();
        Transaction t2 = TransactionBuilder.withDb(db).account(a2).amount(2000).create();
        Transaction t3 = TransactionBuilder.withDb(db).account(a1).amount(-100)
                .withTransferSplit(a2, -100, 50).create();
        Transaction t4 = TransactionBuilder.withDb(db).account(a1).amount(-100).create();
        Transaction t5 = TransactionBuilder.withDb(db).account(a2).amount(-100).create();
        db.rebuildRunningBalances();
        assertFinalBalanceForAccount(a1, 800);
        assertFinalBalanceForAccount(a2, 1950);
        // update split -100 -> +50 >>> -200 -> +100
        List<Transaction> splits = db.getSplitsForTransaction(t3.id);
        t3.fromAmount = -200;
        splits.get(0).fromAmount = -200;
        splits.get(0).toAmount = 100;
        t3.splits = splits;
        db.insertOrUpdate(t3);
        assertAccountBalanceForTransaction(t1, a1, 1000);
        assertAccountBalanceForTransaction(t2, a2, 2000);
        assertAccountBalanceForTransaction(t3, a1, 800);
        assertAccountBalanceForTransaction(t4, a1, 700);
        assertAccountBalanceForTransaction(t5, a2, 2000);
        assertFinalBalanceForAccount(a1, 700);
        assertFinalBalanceForAccount(a2, 2000);
        db.rebuildRunningBalances();
        assertFinalBalanceForAccount(a1, 700);
        assertFinalBalanceForAccount(a2, 2000);
    }

    @Test
    public void should_update_running_balance_when_deleting_transfer_split() {
        Transaction t1 = TransactionBuilder.withDb(db).account(a1).amount(1000).create();
        Transaction t2 = TransactionBuilder.withDb(db).account(a2).amount(2000).create();
        db.rebuildRunningBalances();
        assertFinalBalanceForAccount(a1, 1000);
        assertFinalBalanceForAccount(a2, 2000);
        Transaction t3 = TransactionBuilder.withDb(db).account(a1).amount(-200)
                .withTransferSplit(a2, -200, 100).create();
        assertFinalBalanceForAccount(a1, 800);
        assertFinalBalanceForAccount(a2, 2100);
        db.deleteTransaction(t3.id);
        assertFinalBalanceForAccount(a1, 1000);
        assertFinalBalanceForAccount(a2, 2000);
        db.rebuildRunningBalances();
        assertFinalBalanceForAccount(a1, 1000);
        assertFinalBalanceForAccount(a2, 2000);
        assertAccountBalanceForTransaction(t1, a1, 1000);
        assertAccountBalanceForTransaction(t2, a2, 2000);
    }

    @Test
    public void should_update_running_balance_when_inserting_new_transaction() {
        // *  | time  | amount | balance
        // t1 | 11:00 | +1000  | +1000
        // t2 | 11:05 | -500   | +500
        // t3 | 12:00 | -250   | +250
        Transaction t1 = TransactionBuilder.withDb(db).account(a1).amount(1000).dateTime(DateTime.today().at(11, 0, 0, 0)).create();
        Transaction t2 = TransactionBuilder.withDb(db).account(a1).amount(-500).dateTime(DateTime.today().at(11, 5, 0, 0)).create();
        Transaction t3 = TransactionBuilder.withDb(db).account(a1).amount(-250).dateTime(DateTime.today().at(12, 0, 0, 0)).create();
        db.rebuildRunningBalanceForAccount(a1);
        assertAccountBalanceForTransaction(t1, a1, 1000);
        assertAccountBalanceForTransaction(t2, a1, 500);
        assertAccountBalanceForTransaction(t3, a1, 250);
        assertFinalBalanceForAccount(a1, 250);
        assertLastTransactionDate(a1, DateTime.today().at(12, 0, 0, 0));
        // *  | time  | amount | balance
        // t1 | 11:00 | +1000  | +1000
        // t2 | 11:05 | -500   | +500
        // t3 | 12:00 | -250   | +250
        // t4 | 13:20 | +100   | +350 <- insert at the bottom
        Transaction t4 = TransactionBuilder.withDb(db).account(a1).amount(100).dateTime(DateTime.today().at(13, 20, 0, 0)).create();
        assertAccountBalanceForTransaction(t1, a1, 1000);
        assertAccountBalanceForTransaction(t2, a1, 500);
        assertAccountBalanceForTransaction(t3, a1, 250);
        assertAccountBalanceForTransaction(t4, a1, 350);
        assertFinalBalanceForAccount(a1, 350);
        assertLastTransactionDate(a1, DateTime.today().at(13, 20, 0, 0));
        // *  | time  | amount | balance
        // t1 | 11:00 | +1000  | +1000
        // t2 | 11:05 | -500   | +500
        // t5 | 11:10 | -50    | +450 <- insert in the middle
        // t3 | 12:00 | -250   | +200
        // t4 | 13:20 | +100   | +300
        Transaction t5 = TransactionBuilder.withDb(db).account(a1).amount(-50).dateTime(DateTime.today().at(11, 10, 0, 0)).create();
        assertAccountBalanceForTransaction(t1, a1, 1000);
        assertAccountBalanceForTransaction(t2, a1, 500);
        assertAccountBalanceForTransaction(t5, a1, 450);
        assertAccountBalanceForTransaction(t3, a1, 200);
        assertAccountBalanceForTransaction(t4, a1, 300);
        assertFinalBalanceForAccount(a1, 300);
        assertLastTransactionDate(a1, DateTime.today().at(13, 20, 0, 0));
        // *  | time  | amount | balance
        // t6 | 10:00 | +150   | +150 <- insert at the top
        // t1 | 11:00 | +1000  | +1150
        // t2 | 11:05 | -500   | +650
        // t5 | 11:10 | -50    | +600
        // t3 | 12:00 | -250   | +350
        // t4 | 13:20 | +100   | +450
        Transaction t6 = TransactionBuilder.withDb(db).account(a1).amount(150).dateTime(DateTime.today().at(10, 0, 0, 0)).create();
        assertAccountBalanceForTransaction(t6, a1, 150);
        assertAccountBalanceForTransaction(t1, a1, 1150);
        assertAccountBalanceForTransaction(t2, a1, 650);
        assertAccountBalanceForTransaction(t5, a1, 600);
        assertAccountBalanceForTransaction(t3, a1, 350);
        assertAccountBalanceForTransaction(t4, a1, 450);
        assertFinalBalanceForAccount(a1, 450);
        assertLastTransactionDate(a1, DateTime.today().at(13, 20, 0, 0));
    }


    @Test
    public void should_update_running_balance_when_updating_amount_on_existing_transaction() {
        // *  | time  | amount | balance
        // t1 | 11:00 | +1000  | +1000
        // t2 | 11:05 | -500   | +500
        // t3 | 12:00 | -250   | +250
        Transaction t1 = TransactionBuilder.withDb(db).account(a1).amount(1000).dateTime(DateTime.today().at(11, 0, 0, 0)).create();
        Transaction t2 = TransactionBuilder.withDb(db).account(a1).amount(-500).dateTime(DateTime.today().at(11, 5, 0, 0)).create();
        Transaction t3 = TransactionBuilder.withDb(db).account(a1).amount(-250).dateTime(DateTime.today().at(12, 0, 0, 0)).create();
        db.rebuildRunningBalanceForAccount(a1);
        assertAccountBalanceForTransaction(t1, a1, 1000);
        assertAccountBalanceForTransaction(t2, a1, 500);
        assertAccountBalanceForTransaction(t3, a1, 250);
        assertFinalBalanceForAccount(a1, 250);
        assertLastTransactionDate(a1, DateTime.today().at(12, 0, 0, 0));
        // *  | time  | amount | balance
        // t1 | 11:00 | +1000  | +1000
        // t2 | 11:05 | -500   | +500
        // t3 | 12:00 | -350   | +150 <- update at the bottom
        t3.fromAmount = -350;
        db.insertOrUpdate(t3);
        assertAccountBalanceForTransaction(t1, a1, 1000);
        assertAccountBalanceForTransaction(t2, a1, 500);
        assertAccountBalanceForTransaction(t3, a1, 150);
        assertFinalBalanceForAccount(a1, 150);
        assertLastTransactionDate(a1, DateTime.today().at(12, 0, 0, 0));
        // *  | time  | amount | balance
        // t1 | 11:00 | +1000  | +1000
        // t2 | 11:05 | -400   | +600 <- update in the middle
        // t3 | 12:00 | -350   | +250
        t2.fromAmount = -400;
        db.insertOrUpdate(t2);
        assertAccountBalanceForTransaction(t1, a1, 1000);
        assertAccountBalanceForTransaction(t2, a1, 600);
        assertAccountBalanceForTransaction(t3, a1, 250);
        assertFinalBalanceForAccount(a1, 250);
        assertLastTransactionDate(a1, DateTime.today().at(12, 0, 0, 0));
        // *  | time  | amount | balance
        // t1 | 11:00 | +1200  | +1200 <- update at the top
        // t2 | 11:05 | -400   | +800
        // t3 | 12:00 | -350   | +450
        t1.fromAmount = 1200;
        db.insertOrUpdate(t1);
        assertAccountBalanceForTransaction(t1, a1, 1200);
        assertAccountBalanceForTransaction(t2, a1, 800);
        assertAccountBalanceForTransaction(t3, a1, 450);
        assertFinalBalanceForAccount(a1, 450);
        assertLastTransactionDate(a1, DateTime.today().at(12, 0, 0, 0));
    }

    @Test
    public void should_update_running_balance_when_updating_datetime_on_existing_transaction() {
        // *  | time  | amount | balance
        // t1 | 11:00 | +1000  | +1000
        // t2 | 11:05 | -500   | +500
        // t3 | 12:00 | -250   | +250
        Transaction t1 = TransactionBuilder.withDb(db).account(a1).amount(1000).dateTime(DateTime.today().at(11, 0, 0, 0)).create();
        Transaction t2 = TransactionBuilder.withDb(db).account(a1).amount(-500).dateTime(DateTime.today().at(11, 5, 0, 0)).create();
        Transaction t3 = TransactionBuilder.withDb(db).account(a1).amount(-250).dateTime(DateTime.today().at(12, 0, 0, 0)).create();
        db.rebuildRunningBalanceForAccount(a1);
        assertAccountBalanceForTransaction(t1, a1, 1000);
        assertAccountBalanceForTransaction(t2, a1, 500);
        assertAccountBalanceForTransaction(t3, a1, 250);
        assertFinalBalanceForAccount(a1, 250);
        assertLastTransactionDate(a1, DateTime.today().at(12, 0, 0, 0));
        // *  | time  | amount | balance
        // t1 | 11:00 | +1000  | +1000
        // t3 | 11:01 | -250   | +750 <- move up
        // t2 | 11:05 | -500   | +250
        t3.dateTime = DateTime.today().at(11, 1, 0, 0).asLong();
        db.insertOrUpdate(t3);
        assertAccountBalanceForTransaction(t1, a1, 1000);
        assertAccountBalanceForTransaction(t3, a1, 750);
        assertAccountBalanceForTransaction(t2, a1, 250);
        assertFinalBalanceForAccount(a1, 250);
        assertLastTransactionDate(a1, DateTime.today().at(11, 5, 0, 0));
        // *  | time  | amount | balance
        // t1 | 11:00 | +1000  | +1000
        // t2 | 11:05 | -500   | +500
        // t3 | 12:05 | -250   | +250 <- move down
        t3.dateTime = DateTime.today().at(12, 5, 0, 0).asLong();
        db.insertOrUpdate(t3);
        assertAccountBalanceForTransaction(t1, a1, 1000);
        assertAccountBalanceForTransaction(t2, a1, 500);
        assertAccountBalanceForTransaction(t3, a1, 250);
        assertFinalBalanceForAccount(a1, 250);
        assertLastTransactionDate(a1, DateTime.today().at(12, 5, 0, 0));
    }

    @Test
    public void should_update_running_balance_when_updating_account_on_existing_transaction() {
        // A1  | time  | amount | balance
        // t11 | 11:00 | +1000  | +1000
        // t12 | 11:05 | -500   | +500
        // A2  | time  | amount | balance
        // t21 | 11:00 | +900   | +900
        // t22 | 12:00 | -100   | +800
        Transaction t11 = TransactionBuilder.withDb(db).account(a1).amount(1000).dateTime(DateTime.today().at(11, 0, 0, 0)).create();
        Transaction t12 = TransactionBuilder.withDb(db).account(a1).amount(-500).dateTime(DateTime.today().at(11, 5, 0, 0)).create();
        Transaction t21 = TransactionBuilder.withDb(db).account(a2).amount(900).dateTime(DateTime.today().at(11, 0, 0, 0)).create();
        Transaction t22 = TransactionBuilder.withDb(db).account(a2).amount(-100).dateTime(DateTime.today().at(13, 0, 0, 0)).create();
        db.rebuildRunningBalances();
        assertFinalBalanceForAccount(a1, 500);
        assertFinalBalanceForAccount(a2, 800);
        assertLastTransactionDate(a1, DateTime.today().at(11, 5, 0, 0));
        assertLastTransactionDate(a2, DateTime.today().at(13, 0, 0, 0));
        // A1  | time  | amount | balance
        // t11 | 11:00 | +1000  | +1000
        // t12 | 11:05 | -500   | +500
        // t22 | 12:00 | -100   | +400
        // A2  | time  | amount | balance
        // t21 | 11:00 | +900   | +900
        t22.fromAccountId = a1.id;
        db.insertOrUpdate(t22);
        assertAccountBalanceForTransaction(t11, a1, 1000);
        assertAccountBalanceForTransaction(t12, a1, 500);
        assertAccountBalanceForTransaction(t22, a1, 400);
        assertFinalBalanceForAccount(a1, 400);
        assertAccountBalanceForTransaction(t21, a2, 900);
        assertFinalBalanceForAccount(a2, 900);
        assertLastTransactionDate(a1, DateTime.today().at(13, 0, 0, 0));
        assertLastTransactionDate(a2, DateTime.today().at(11, 0, 0, 0));
    }

    @Test
    public void should_update_running_balance_when_deleting_existing_transaction() {
        // *  | time  | amount | balance
        // t1 | 11:00 | +1000  | +1000
        // t2 | 11:05 | -500   | +500
        // t3 | 12:00 | -250   | +250
        // t4 | 13:00 | -50    | +200
        Transaction t1 = TransactionBuilder.withDb(db).account(a1).amount(1000).dateTime(DateTime.today().at(11, 0, 0, 0)).create();
        Transaction t2 = TransactionBuilder.withDb(db).account(a1).amount(-500).dateTime(DateTime.today().at(11, 5, 0, 0)).create();
        Transaction t3 = TransactionBuilder.withDb(db).account(a1).amount(-250).dateTime(DateTime.today().at(12, 0, 0, 0)).create();
        Transaction t4 = TransactionBuilder.withDb(db).account(a1).amount(-50).dateTime(DateTime.today().at(13, 0, 0, 0)).create();
        db.rebuildRunningBalanceForAccount(a1);
        assertAccountBalanceForTransaction(t1, a1, 1000);
        assertAccountBalanceForTransaction(t2, a1, 500);
        assertAccountBalanceForTransaction(t3, a1, 250);
        assertAccountBalanceForTransaction(t4, a1, 200);
        assertFinalBalanceForAccount(a1, 200);
        assertLastTransactionDate(a1, DateTime.today().at(13, 0, 0, 0));
        // *  | time  | amount | balance
        // t1 | 11:00 | +1000  | +1000
        // t2 | 11:05 | -500   | +500
        // t3 | 12:00 | -250   | +250
        // t4 | 13:00 | *      | * <- delete at the bottom
        db.deleteTransaction(t4.id);
        assertAccountBalanceForTransaction(t1, a1, 1000);
        assertAccountBalanceForTransaction(t2, a1, 500);
        assertAccountBalanceForTransaction(t3, a1, 250);
        assertFinalBalanceForAccount(a1, 250);
        assertLastTransactionDate(a1, DateTime.today().at(12, 0, 0, 0));
        // *  | time  | amount | balance
        // t1 | 11:00 | +1000  | +1000
        // t2 | 11:05 | *      | * <- delete in the middle
        // t3 | 12:00 | -250   | +750
        db.deleteTransaction(t2.id);
        assertAccountBalanceForTransaction(t1, a1, 1000);
        assertAccountBalanceForTransaction(t3, a1, 750);
        assertFinalBalanceForAccount(a1, 750);
        assertLastTransactionDate(a1, DateTime.today().at(12, 0, 0, 0));
        // *  | time  | amount | balance
        // t1 | 11:00 | *      | * <- delete at the top
        // t3 | 12:00 | -250   | -250
        db.deleteTransaction(t1.id);
        assertAccountBalanceForTransaction(t3, a1, -250);
        assertFinalBalanceForAccount(a1, -250);
        assertLastTransactionDate(a1, DateTime.today().at(12, 0, 0, 0));
    }

    @Test
    public void should_update_running_balance_when_inserting_new_transfer() {
        // A1  | time  | amount | balance
        // t11 | 11:00 | +1000  | +1000
        // t12 | 11:05 | -500   | +500
        // t13 | 12:00 | -250   | +250
        // A2  | time  | amount | balance
        // t21 | 11:00 | +900   | +900
        // t22 | 12:00 | -100   | +800
        Transaction t11 = TransactionBuilder.withDb(db).account(a1).amount(1000).dateTime(DateTime.today().at(11, 0, 0, 0)).create();
        Transaction t12 = TransactionBuilder.withDb(db).account(a1).amount(-500).dateTime(DateTime.today().at(11, 5, 0, 0)).create();
        Transaction t13 = TransactionBuilder.withDb(db).account(a1).amount(-250).dateTime(DateTime.today().at(12, 0, 0, 0)).create();
        db.rebuildRunningBalanceForAccount(a1);
        assertAccountBalanceForTransaction(t11, a1, 1000);
        assertAccountBalanceForTransaction(t12, a1, 500);
        assertAccountBalanceForTransaction(t13, a1, 250);
        assertFinalBalanceForAccount(a1, 250);
        Transaction t21 = TransactionBuilder.withDb(db).account(a2).amount(900).dateTime(DateTime.today().at(11, 0, 0, 0)).create();
        Transaction t22 = TransactionBuilder.withDb(db).account(a2).amount(-100).dateTime(DateTime.today().at(13, 0, 0, 0)).create();
        db.rebuildRunningBalanceForAccount(a2);
        assertAccountBalanceForTransaction(t21, a2, 900);
        assertAccountBalanceForTransaction(t22, a2, 800);
        assertFinalBalanceForAccount(a2, 800);
        // A1  | time  | amount | balance
        // t11 | 11:00 | +1000  | +1000
        // t12 | 11:05 | -500   | +500
        // t13 | 12:00 | -250   | +250
        // t14 | 12:30 | -100   | +150 -> A2
        // A2  | time  | amount | balance
        // t21 | 11:00 | +900   | +900
        // t14 | 12:30 | +100   | +1000 <- A1
        // t22 | 13:00 | -100   | +900
        Transaction t14 = TransferBuilder.withDb(db).fromAccount(a1).fromAmount(-100).toAccount(a2).toAmount(100).dateTime(DateTime.today().at(12, 30, 0, 0)).create();
        assertAccountBalanceForTransaction(t11, a1, 1000);
        assertAccountBalanceForTransaction(t12, a1, 500);
        assertAccountBalanceForTransaction(t13, a1, 250);
        assertAccountBalanceForTransaction(t14, a1, 150);
        assertFinalBalanceForAccount(a1, 150);
        assertAccountBalanceForTransaction(t21, a2, 900);
        assertAccountBalanceForTransaction(t14, a2, 1000);
        assertAccountBalanceForTransaction(t22, a2, 900);
        assertFinalBalanceForAccount(a2, 900);
        // A1  | time  | amount | balance
        // t11 | 11:00 | +1000  | +1000
        // t12 | 11:05 | -500   | +500
        // t13 | 12:00 | -250   | +250
        // t14 | 12:30 | -100   | +150 -> A2
        // t15 | 13:30 | -50    | +100 -> A2 (rate=0.4)
        // A2  | time  | amount | balance
        // t21 | 11:00 | +900   | +900
        // t14 | 12:30 | +100   | +1000 <- A1
        // t22 | 13:00 | -100   | +900
        // t15 | 13:30 | +20    | +920 <- A1
        Transaction t15 = TransferBuilder.withDb(db).fromAccount(a1).fromAmount(-50).toAccount(a2).toAmount(20).dateTime(DateTime.today().at(13, 30, 0, 0)).create();
        assertAccountBalanceForTransaction(t15, a1, 100);
        assertFinalBalanceForAccount(a1, 100);
        assertAccountBalanceForTransaction(t15, a2, 920);
        assertFinalBalanceForAccount(a2, 920);
    }

    @Test
    public void should_update_running_balance_when_updating_amount_on_existing_transfer() {
        // A1  | time  | amount | balance
        // t11 | 11:00 | +1000  | +1000
        // t12 | 11:05 | -500   | +500
        // t13 | 12:00 | -250   | +250
        // t14 | 12:30 | -100   | +150 -> A2
        // A2  | time  | amount | balance
        // t21 | 11:00 | +900   | +900
        // t14 | 12:30 | +100   | +1000 <- A1
        // t22 | 13:00 | -100   | +900
        Transaction t11 = TransactionBuilder.withDb(db).account(a1).amount(1000).dateTime(DateTime.today().at(11, 0, 0, 0)).create();
        Transaction t12 = TransactionBuilder.withDb(db).account(a1).amount(-500).dateTime(DateTime.today().at(11, 5, 0, 0)).create();
        Transaction t13 = TransactionBuilder.withDb(db).account(a1).amount(-250).dateTime(DateTime.today().at(12, 0, 0, 0)).create();
        Transaction t21 = TransactionBuilder.withDb(db).account(a2).amount(900).dateTime(DateTime.today().at(11, 0, 0, 0)).create();
        Transaction t22 = TransactionBuilder.withDb(db).account(a2).amount(-100).dateTime(DateTime.today().at(13, 0, 0, 0)).create();
        Transaction t14 = TransferBuilder.withDb(db).fromAccount(a1).fromAmount(-100).toAccount(a2).toAmount(100).dateTime(DateTime.today().at(12, 30, 0, 0)).create();
        db.rebuildRunningBalanceForAccount(a1);
        db.rebuildRunningBalanceForAccount(a2);
        assertAccountBalanceForTransaction(t11, a1, 1000);
        assertAccountBalanceForTransaction(t12, a1, 500);
        assertAccountBalanceForTransaction(t13, a1, 250);
        assertAccountBalanceForTransaction(t14, a1, 150);
        assertFinalBalanceForAccount(a1, 150);
        assertAccountBalanceForTransaction(t21, a2, 900);
        assertAccountBalanceForTransaction(t14, a2, 1000);
        assertAccountBalanceForTransaction(t22, a2, 900);
        assertFinalBalanceForAccount(a2, 900);
        // A1  | time  | amount | balance
        // t11 | 11:00 | +1000  | +1000
        // t12 | 11:05 | -500   | +500
        // t13 | 12:00 | -250   | +250
        // t14 | 12:30 | -200   | +50 -> A2 <- update amount
        // A2  | time  | amount | balance
        // t21 | 11:00 | +900   | +900
        // t14 | 12:30 | +250   | +1150 <- A1 <- update amount
        // t22 | 13:00 | -100   | +1050
        t14.fromAmount = -200;
        t14.toAmount = +250;
        db.insertOrUpdate(t14);
        assertAccountBalanceForTransaction(t11, a1, 1000);
        assertAccountBalanceForTransaction(t12, a1, 500);
        assertAccountBalanceForTransaction(t13, a1, 250);
        assertAccountBalanceForTransaction(t14, a1, 50);
        assertFinalBalanceForAccount(a1, 50);
        assertAccountBalanceForTransaction(t21, a2, 900);
        assertAccountBalanceForTransaction(t14, a2, 1150);
        assertAccountBalanceForTransaction(t22, a2, 1050);
        assertFinalBalanceForAccount(a2, 1050);
    }

    @Test
    public void should_update_running_balance_when_updating_datetime_on_existing_transfer() {
        // A1  | time  | amount | balance
        // t11 | 11:00 | +1000  | +1000
        // t12 | 11:05 | -500   | +500
        // t13 | 12:00 | -250   | +250
        // t14 | 12:30 | -100   | +150 -> A2
        // A2  | time  | amount | balance
        // t21 | 12:00 | +900   | +900
        // t14 | 12:30 | +100   | +1000 <- A1
        // t22 | 13:00 | -100   | +900
        Transaction t11 = TransactionBuilder.withDb(db).account(a1).amount(1000).dateTime(DateTime.today().at(11, 0, 0, 0)).create();
        Transaction t12 = TransactionBuilder.withDb(db).account(a1).amount(-500).dateTime(DateTime.today().at(11, 5, 0, 0)).create();
        Transaction t13 = TransactionBuilder.withDb(db).account(a1).amount(-250).dateTime(DateTime.today().at(12, 0, 0, 0)).create();
        Transaction t21 = TransactionBuilder.withDb(db).account(a2).amount(900).dateTime(DateTime.today().at(12, 0, 0, 0)).create();
        Transaction t22 = TransactionBuilder.withDb(db).account(a2).amount(-100).dateTime(DateTime.today().at(13, 0, 0, 0)).create();
        Transaction t14 = TransferBuilder.withDb(db).fromAccount(a1).fromAmount(-100).toAccount(a2).toAmount(100).dateTime(DateTime.today().at(12, 30, 0, 0)).create();
        db.rebuildRunningBalanceForAccount(a1);
        db.rebuildRunningBalanceForAccount(a2);
        assertAccountBalanceForTransaction(t11, a1, 1000);
        assertAccountBalanceForTransaction(t12, a1, 500);
        assertAccountBalanceForTransaction(t13, a1, 250);
        assertAccountBalanceForTransaction(t14, a1, 150);
        assertFinalBalanceForAccount(a1, 150);
        assertAccountBalanceForTransaction(t21, a2, 900);
        assertAccountBalanceForTransaction(t14, a2, 1000);
        assertAccountBalanceForTransaction(t22, a2, 900);
        assertFinalBalanceForAccount(a2, 900);
        // A1  | time  | amount | balance
        // t11 | 11:00 | +1000  | +1000
        // t12 | 11:05 | -500   | +500
        // t14 | 11:10 | -100   | +400 -> A2 <- move up
        // t13 | 12:00 | -250   | +150
        // A2  | time  | amount | balance
        // t14 | 11:10 | +100   | +100 <- A1
        // t21 | 12:00 | +900   | +1000
        // t22 | 13:00 | -100   | +900
        t14.dateTime = DateTime.today().at(11, 10, 0, 0).asLong();
        db.insertOrUpdate(t14);
        assertAccountBalanceForTransaction(t11, a1, 1000);
        assertAccountBalanceForTransaction(t12, a1, 500);
        assertAccountBalanceForTransaction(t14, a1, 400);
        assertAccountBalanceForTransaction(t13, a1, 150);
        assertFinalBalanceForAccount(a1, 150);
        assertAccountBalanceForTransaction(t14, a2, 100);
        assertAccountBalanceForTransaction(t21, a2, 1000);
        assertAccountBalanceForTransaction(t22, a2, 900);
        assertFinalBalanceForAccount(a2, 900);
    }

    @Test
    public void should_update_running_balance_when_updating_account_on_existing_transfer() {
        // A1  | time  | amount | balance
        // t11 | 11:00 | +1000  | +1000
        // t12 | 12:30 | -100   | +900 -> A2
        // A2  | time  | amount | balance
        // t21 | 12:00 | +500   | +500
        // t12 | 12:30 | +100   | +600 <- A1
        // A3  | time  | amount | balance
        // t31 | 13:00 | +100   | +100
        Transaction t11 = TransactionBuilder.withDb(db).account(a1).amount(1000).dateTime(DateTime.today().at(11, 0, 0, 0)).create();
        Transaction t21 = TransactionBuilder.withDb(db).account(a2).amount(500).dateTime(DateTime.today().at(12, 0, 0, 0)).create();
        Transaction t12 = TransferBuilder.withDb(db).fromAccount(a1).fromAmount(-100).toAccount(a2).toAmount(100).dateTime(DateTime.today().at(12, 30, 0, 0)).create();
        Transaction t31 = TransactionBuilder.withDb(db).account(a3).amount(100).dateTime(DateTime.today().at(13, 0, 0, 0)).create();
        db.rebuildRunningBalanceForAccount(a1);
        db.rebuildRunningBalanceForAccount(a2);
        db.rebuildRunningBalanceForAccount(a3);
        assertAccountBalanceForTransaction(t11, a1, 1000);
        assertAccountBalanceForTransaction(t12, a1, 900);
        assertFinalBalanceForAccount(a1, 900);
        assertAccountBalanceForTransaction(t21, a2, 500);
        assertAccountBalanceForTransaction(t12, a2, 600);
        assertFinalBalanceForAccount(a2, 600);
        assertAccountBalanceForTransaction(t31, a3, 100);
        assertFinalBalanceForAccount(a3, 100);
        // A1  | time  | amount | balance
        // t11 | 11:00 | +1000  | +1000
        // t12 | 12:30 | -100   | +900 -> A3 <- update account
        // A2  | time  | amount | balance
        // t21 | 12:00 | +500   | +500
        // A3  | time  | amount | balance
        // t12 | 12:30 | +100   | +100 <- A1
        // t31 | 13:00 | +100   | +200
        t12.toAccountId = a3.id;
        db.insertOrUpdate(t12);
        assertAccountBalanceForTransaction(t11, a1, 1000);
        assertAccountBalanceForTransaction(t12, a1, 900);
        assertFinalBalanceForAccount(a1, 900);
        assertAccountBalanceForTransaction(t21, a2, 500);
        assertFinalBalanceForAccount(a2, 500);
        assertAccountBalanceForTransaction(t12, a3, 100);
        assertAccountBalanceForTransaction(t31, a3, 200);
        assertFinalBalanceForAccount(a3, 200);
    }

    @Test
    public void should_update_accounts_last_transaction_date() {
        TransactionBuilder.withDb(db).account(a1).amount(1000).dateTime(DateTime.today().at(11, 0, 0, 0)).create();
        TransactionBuilder.withDb(db).account(a1).amount(-500).dateTime(DateTime.today().at(11, 5, 0, 0)).create();
        TransactionBuilder.withDb(db).account(a2).amount(900).dateTime(DateTime.today().at(11, 0, 0, 0)).create();
        TransactionBuilder.withDb(db).account(a2).amount(-100).dateTime(DateTime.today().at(13, 0, 0, 0)).create();
        resetLastTransaction(a1);
        resetLastTransaction(a2);
        assertLastTransactionDate(a1, DateTime.NULL_DATE);
        assertLastTransactionDate(a2, DateTime.NULL_DATE);
        db.updateAccountsLastTransactionDate();
        assertLastTransactionDate(a1, DateTime.today().at(11, 5, 0, 0));
        assertLastTransactionDate(a2, DateTime.today().at(13, 0, 0, 0));
    }

    private void resetLastTransaction(Account a) {
        db.db().execSQL("update account set last_transaction_date=0 where _id=?", new String[]{String.valueOf(a.id)});
    }

}
