/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.utils;

import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.model.Account;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 8/16/12 7:55 PM
 */
public class IntegrityCheck {

    private final DatabaseAdapter db;

    public IntegrityCheck(DatabaseAdapter db) {
        this.db = db;
    }

    public boolean isBroken() {
        return isRunningBalanceBroken();
    }

    private boolean isRunningBalanceBroken() {
        List<Account> accounts = db.em().getAllAccountsList();
        for (Account account : accounts) {
            long totalFromAccount = account.totalAmount;
            long totalFromRunningBalance = db.getLastRunningBalanceForAccount(account);
            if (totalFromAccount != totalFromRunningBalance) {
                return true;
            }
        }
        return false;
    }

}
