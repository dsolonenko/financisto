package ru.orangesoftware.financisto.test;

import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.Currency;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 3/2/11 9:07 PM
 */
public class AccountBuilder {

    private final DatabaseAdapter db;
    private final Account a = new Account();

    public static Account createDefault(DatabaseAdapter db) {
        Currency c = CurrencyBuilder.createDefault(db);
        return createDefault(db, c);
    }

    public static Account createDefault(DatabaseAdapter db, Currency c) {
        return withDb(db).title("Cash").currency(c).create();
    }

    public static AccountBuilder withDb(DatabaseAdapter db) {
        return new AccountBuilder(db);
    }

    public AccountBuilder id(long v) {
        a.id = v;
        return this;
    }

    private AccountBuilder(DatabaseAdapter db) {
        this.db = db;
    }

    public AccountBuilder title(String title) {
        a.title = title;
        return this;
    }

    public AccountBuilder currency(Currency c) {
        a.currency = c;
        return this;
    }

    public AccountBuilder number(String n) {
        a.number = n;
        return this;
    }

    public AccountBuilder issuer(String v) {
        a.issuer = v;
        return this;
    }
    
    public AccountBuilder total(long amount) {
        a.totalAmount = amount;
        return this;
    }

    public AccountBuilder doNotIncludeIntoTotals() {
        a.isIncludeIntoTotals = false;
        return this;
    }

    public AccountBuilder inactive() {
        a.isActive = false;
        return this;
    }

    public Account create() {
        db.saveAccount(a);
        return a;
    }
}
