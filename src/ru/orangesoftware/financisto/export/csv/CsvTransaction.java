/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.export.csv;

import ru.orangesoftware.financisto.model.*;

import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 5/29/12 8:59 PM
 */
public class CsvTransaction {

    public long date;
    public long time;
    public long fromAccountId;
    public long fromAmount;
    public long originalAmount;
    public String originalCurrency;
    public String payee;
    public String category;
    public String categoryParent;
    public String note;
    public String project;
    public String currency;

    public Transaction createTransaction(Map<String, Currency> currencies, Map<String, Category> categories, Map<String, Project> projects, Map<String, Payee> payees) {
        Transaction t = new Transaction();
        t.dateTime = date+time;
        t.fromAccountId = fromAccountId;
        t.fromAmount = fromAmount;
        t.categoryId = getEntityIdOrZero(categories, category);
        t.payeeId = getEntityIdOrZero(payees, payee);
        t.projectId = getEntityIdOrZero(projects, project);
        if (originalAmount != 0) {
            Currency currency = currencies.get(originalCurrency);
            t.originalFromAmount = originalAmount;
            t.originalCurrencyId = currency.id;
        }
        t.note = note;
        return t;
    }
    
    public static <T extends MyEntity> long getEntityIdOrZero(Map<String, T> map, String value) {
        T e = map.get(value);
        return e != null ? e.id : 0;
    }
    
}
