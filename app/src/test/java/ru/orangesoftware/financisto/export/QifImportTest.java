package ru.orangesoftware.financisto.export;

import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ru.orangesoftware.financisto.db.AbstractDbTest;
import ru.orangesoftware.financisto.export.qif.QifCategory;
import ru.orangesoftware.financisto.export.qif.QifDateFormat;
import ru.orangesoftware.financisto.export.qif.QifImport;
import ru.orangesoftware.financisto.export.qif.QifImportOptions;
import ru.orangesoftware.financisto.export.qif.QifParser;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.AccountType;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.CategoryTree;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.Project;
import ru.orangesoftware.financisto.model.TransactionInfo;
import ru.orangesoftware.financisto.test.DateTime;
import ru.orangesoftware.orb.Expressions;
import ru.orangesoftware.orb.Query;

import static ru.orangesoftware.financisto.export.qif.QifDateFormat.EU_FORMAT;
import static org.junit.Assert.*;

public class QifImportTest extends AbstractDbTest {

    QifParserTest qifParserTest = new QifParserTest();
    QifImport qifImport;

    public void setUp() throws Exception {
        super.setUp();
        db.db().execSQL("insert into currency(_id,title,name,symbol) values(0,'Default','?','$')");
    }

    @Test
    public void should_import_empty_account() throws IOException {
        qifParserTest.should_parse_empty_account();
        doImport();

        List<Account> accounts = db.getAllAccountsList();
        assertEquals(1, accounts.size());
        assertEquals("My Cash Account", accounts.get(0).title);
        assertEquals(AccountType.CASH.name(), accounts.get(0).type);
    }

    @Test
    public void should_import_a_couple_of_empty_accounts() throws IOException {
        qifParserTest.should_parse_a_couple_of_empty_accounts();
        doImport();

        List<Account> accounts = db.getAllAccountsList();
        sortAccountsById(accounts);

        assertEquals(2, accounts.size());
        assertEquals("My Cash Account", accounts.get(0).title);
        assertEquals(AccountType.CASH.name(), accounts.get(0).type);
        assertEquals("My Bank Account", accounts.get(1).title);
        assertEquals(AccountType.BANK.name(), accounts.get(1).type);
    }

    @Test
    public void should_import_categories() throws Exception {
        QifParser p = new QifParser(null, QifDateFormat.EU_FORMAT);
        //P1
        // - cc1
        // -- c1
        // -- c2
        // - cc2
        //P2
        // - x1
        p.categories.add(new QifCategory("P1:cc1:c1", true));
        p.categories.add(new QifCategory("P1:cc1", true));
        p.categories.add(new QifCategory("P1:cc1:c2", true));
        p.categories.add(new QifCategory("P2", false));
        p.categories.add(new QifCategory("P2:x1", false));
        p.categories.add(new QifCategory("P1", true));
        p.categories.add(new QifCategory("P1:cc2", true));

        //when
        doImport(p);

        //then
        CategoryTree<Category> categories = db.getCategoriesTree(false);
        assertNotNull(categories);
        assertEquals(2, categories.size());

        Category c = categories.getAt(0);
        assertCategory("P1", true, c);
        assertEquals(2, c.children.size());

        assertCategory("cc1", true, c.children.getAt(0));
        assertEquals(2, c.children.getAt(0).children.size());

        assertCategory("cc2", true, c.children.getAt(1));
        assertFalse(c.children.getAt(1).hasChildren());

        c = categories.getAt(1);
        assertCategory("P2", false, c);
        assertEquals(1, c.children.size());

        assertCategory("x1", false, c.children.getAt(0));
    }

    @Test
    public void should_import_classes_as_projects() throws Exception {
        qifParserTest.should_parse_classes();
        doImport();

        List<Project> projects = db.getAllProjectsList(false);
        assertEquals(3, projects.size());

        List<Account> accounts = db.getAllAccountsList();
        assertEquals(1, accounts.size());

        List<TransactionInfo> transactions = db.getTransactionsForAccount(accounts.get(0).id);
        assertEquals(4, transactions.size());

        TransactionInfo t = transactions.get(0);
        assertEquals(DateTime.date(2011, 2, 8).atMidnight().asLong(), t.dateTime);
        assertEquals(1000, t.fromAmount);
        assertEquals("P1", t.category.title);
        assertEquals("Class1", t.project.title);

        t = transactions.get(1);
        assertEquals(DateTime.date(2011, 2, 7).atMidnight().asLong(), t.dateTime);
        assertEquals(-2345, t.fromAmount);
        assertEquals("c1", t.category.title);
        assertEquals("Class1", t.project.title);

        t = transactions.get(2);
        assertEquals(DateTime.date(2011, 1, 1).atMidnight().asLong(), t.dateTime);
        assertEquals(-6780, t.fromAmount);
        assertEquals("c1", t.category.title);
        assertEquals("Class1:Subclass1", t.project.title);

        t = transactions.get(3);
        assertEquals(DateTime.date(2010, 1, 1).atMidnight().asLong(), t.dateTime);
        assertEquals(-120, t.fromAmount);
        assertEquals("Class2", t.project.title);
    }

    @Test
    public void should_import_account_with_a_couple_of_transactions() throws Exception {
        qifParserTest.should_parse_account_with_a_couple_of_transactions();
        doImport();

        List<Account> accounts = db.getAllAccountsList();
        assertEquals(1, accounts.size());

        List<TransactionInfo> transactions = db.getTransactionsForAccount(accounts.get(0).id);
        assertEquals(2, transactions.size());

        TransactionInfo t = transactions.get(0);
        assertEquals(DateTime.date(2011, 2, 8).atMidnight().asLong(), t.dateTime);
        assertEquals(1000, t.fromAmount);
        assertEquals("P1", t.category.title);
        assertNull(t.payee);

        t = transactions.get(1);
        assertEquals(DateTime.date(2011, 2, 7).atMidnight().asLong(), t.dateTime);
        assertEquals(-2056, t.fromAmount);
        assertEquals("Payee 1", t.payee.title);
        assertEquals("c1", t.category.title);
        assertEquals("Some note here...", t.note);
    }

    @Test
    public void should_import_multiple_accounts() throws Exception {
        qifParserTest.should_parse_multiple_accounts();
        doImport();

        List<Account> accounts = db.getAllAccountsList();
        assertEquals(2, accounts.size());

        Account a = accounts.get(0);
        assertEquals("My Bank Account", a.title);
        assertEquals(AccountType.BANK.name(), a.type);

        List<TransactionInfo> transactions = db.getTransactionsForAccount(a.id);
        assertEquals(2, transactions.size());

        TransactionInfo t = transactions.get(0);
        assertEquals(DateTime.date(2011, 2, 8).atMidnight().asLong(), t.dateTime);
        assertEquals(-2000, t.fromAmount);

        t = transactions.get(1);
        assertEquals(DateTime.date(2011, 1, 2).atMidnight().asLong(), t.dateTime);
        assertEquals(5400, t.fromAmount);

        a = accounts.get(1);
        assertEquals("My Cash Account", a.title);
        assertEquals(AccountType.CASH.name(), a.type);

        transactions = db.getTransactionsForAccount(a.id);
        assertEquals(3, transactions.size());

        t = transactions.get(0);
        assertEquals(DateTime.date(2011, 2, 8).atMidnight().asLong(), t.dateTime);
        assertEquals(1000, t.fromAmount);

        t = transactions.get(1);
        assertEquals(DateTime.date(2011, 2, 7).atMidnight().asLong(), t.dateTime);
        assertEquals(-2345, t.fromAmount);

        t = transactions.get(2);
        assertEquals(DateTime.date(2011, 1, 1).atMidnight().asLong(), t.dateTime);
        assertEquals(-6780, t.fromAmount);
    }

    @Test
    public void should_import_transfers() throws Exception {
        qifParserTest.should_parse_transfers();
        doImport();

        List<Account> accounts = db.getAllAccountsList();
        assertEquals(2, accounts.size());

        Account a = accounts.get(0);
        assertEquals("My Bank Account", a.title);
        assertEquals(AccountType.BANK.name(), a.type);

        List<TransactionInfo> transactions = db.getTransactionsForAccount(a.id);
        assertEquals(1, transactions.size());

        TransactionInfo t = transactions.get(0);
        assertTrue("Should be a transfer from bank to cash", t.isTransfer());
        assertEquals(DateTime.date(2011, 2, 8).atMidnight().asLong(), t.dateTime);
        assertEquals("My Bank Account", t.fromAccount.title);
        assertEquals(-2000, t.fromAmount);
        assertEquals("My Cash Account", t.toAccount.title);
        assertEquals(2000, t.toAmount);
        assertEquals("Vacation", t.project.title);

        a = accounts.get(1);
        assertEquals("My Cash Account", a.title);
        assertEquals(AccountType.CASH.name(), a.type);

        transactions = db.getTransactionsForAccount(a.id);
        assertEquals(0, transactions.size());
    }

    @Test
    public void should_import_convert_unknown_transfers_into_regular_transactions_with_a_special_note() throws Exception {
        qifParserTest.parseQif(
            "!Account\n" +
            "NMy Cash Account\n" +
            "TCash\n" +
            "^\n" +
            "!Type:Cash\n" +
            "D08/02/2011\n" +
            "T25.00\n" +
            "L[My Bank Account]\n" +
            "^\n" +
            "D07/02/2011\n" +
            "T55.00\n" +
            "L[Some Account 1]\n" +
            "^\n" +
            "!Account\n" +
            "NMy Bank Account\n" +
            "TBank\n" +
            "^\n" +
            "!Type:Bank\n" +
            "D08/02/2011\n" +
            "T-20.00\n" +
            "MNote on transfer\n" +
            "L[Some Account 2]\n" +
            "^\n"+
            "D07/02/2011\n" +
            "T-30.00\n" +
            "L[My Cash Account]\n" +
            "^\n");
        doImport();

        List<Account> accounts = db.getAllAccountsList();
        assertEquals(2, accounts.size());

        Account a = accounts.get(0);
        assertEquals("My Bank Account", a.title);
        assertEquals(AccountType.BANK.name(), a.type);

        List<TransactionInfo> transactions = db.getTransactionsForAccount(a.id);
        assertEquals(2, transactions.size());

        TransactionInfo t = transactions.get(0);
        assertFalse("Should not be a transfer", t.isTransfer());
        assertEquals(DateTime.date(2011, 2, 8).atMidnight().asLong(), t.dateTime);
        assertEquals("My Bank Account", t.fromAccount.title);
        assertEquals(-2000, t.fromAmount);
        assertEquals("Transfer: Some Account 2 | Note on transfer", t.note);

        t = transactions.get(1);
        assertFalse("Should not be a transfer", t.isTransfer());
        assertEquals(DateTime.date(2011, 2, 7).atMidnight().asLong(), t.dateTime);
        assertEquals("My Bank Account", t.fromAccount.title);
        assertEquals(-3000, t.fromAmount);
        assertEquals("Transfer: My Cash Account", t.note);

        a = accounts.get(1);
        assertEquals("My Cash Account", a.title);
        assertEquals(AccountType.CASH.name(), a.type);

        transactions = db.getTransactionsForAccount(a.id);
        assertEquals(2, transactions.size());

        t = transactions.get(0);
        assertFalse("Should not be a transfer", t.isTransfer());
        assertEquals(DateTime.date(2011, 2, 8).atMidnight().asLong(), t.dateTime);
        assertEquals("My Cash Account", t.fromAccount.title);
        assertEquals(2500, t.fromAmount);
        assertEquals("Transfer: My Bank Account", t.note);

        t = transactions.get(1);
        assertFalse("Should not be a transfer", t.isTransfer());
        assertEquals(DateTime.date(2011, 2, 7).atMidnight().asLong(), t.dateTime);
        assertEquals("My Cash Account", t.fromAccount.title);
        assertEquals(5500, t.fromAmount);
        assertEquals("Transfer: Some Account 1", t.note);
    }

    @Test
    public void should_import_splits() throws Exception {
        qifParserTest.should_parse_splits();
        doImport();

        List<Account> accounts = db.getAllAccountsList();
        assertEquals(1, accounts.size());

        Account a = accounts.get(0);
        assertEquals("My Cash Account", a.title);
        assertEquals(AccountType.CASH.name(), a.type);

        List<TransactionInfo> transactions = db.getTransactionsForAccount(a.id);
        assertEquals(1, transactions.size());

        TransactionInfo t = transactions.get(0);
        assertEquals(-260066, t.fromAmount);

        List<TransactionInfo> splits = getSplitsForTransaction(t.id);
        assertEquals(3, splits.size());

        TransactionInfo s = splits.get(0);
        assertEquals("A1", s.category.title);
        assertEquals(-110056, s.fromAmount);
        assertEquals("Note on first split", s.note);

        s = splits.get(1);
        assertEquals("A2", s.category.title);
        assertEquals(-100000, s.fromAmount);

        s = splits.get(2);
        assertEquals("<NO_CATEGORY>", s.category.title);
        assertEquals(50010, s.fromAmount);
        assertEquals("Note on third split", s.note);
    }

    @Test
    public void should_import_transfer_splits() throws Exception {
        qifParserTest.should_parse_transfer_splits();
        doImport();

        List<Account> accounts = db.getAllAccountsList();
        assertEquals(2, accounts.size());

        Account a = accounts.get(0);
        assertEquals("My Bank Account", a.title);
        assertEquals(AccountType.BANK.name(), a.type);

        List<TransactionInfo> transactions = db.getTransactionsForAccount(a.id);
        assertEquals(0, transactions.size());

        a = accounts.get(1);
        assertEquals("My Cash Account", a.title);
        assertEquals(AccountType.CASH.name(), a.type);

        transactions = db.getTransactionsForAccount(a.id);
        assertEquals(1, transactions.size());

        TransactionInfo t = transactions.get(0);
        assertEquals(-210000, t.fromAmount);

        List<TransactionInfo> splits = getSplitsForTransaction(t.id);
        assertEquals(2, splits.size());

        TransactionInfo s = splits.get(0);
        assertEquals("A1", s.category.title);
        assertEquals(-110000, s.fromAmount);
        assertEquals("Note on first split", s.note);

        s = splits.get(1);
        assertTrue(s.isTransfer());
        assertEquals("My Bank Account", s.toAccount.title);
        assertEquals(-100000, s.fromAmount);
        assertEquals(100000, s.toAmount);
    }

    private List<TransactionInfo> getSplitsForTransaction(long transactionId) {
        Query<TransactionInfo> q = db.createQuery(TransactionInfo.class);
        q.where(Expressions.eq("parentId", transactionId));
        return q.list();
    }

    private void sortAccountsById(List<Account> accounts) {
        Collections.sort(accounts, new Comparator<Account>() {
            @Override
            public int compare(Account a1, Account a2) {
                return Long.compare(a1.id, a2.id);
            }
        });
    }

    private void doImport() {
        doImport(qifParserTest.p);
    }

    private void doImport(QifParser p) {
        QifImportOptions options = new QifImportOptions("", EU_FORMAT, Currency.EMPTY);
        qifImport = new QifImport(getContext(), db, options);
        qifImport.doImport(p);
    }


}
