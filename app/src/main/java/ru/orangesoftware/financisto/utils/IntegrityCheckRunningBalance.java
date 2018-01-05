/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.utils;

import android.content.Context;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.model.Account;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 8/16/12 7:55 PM
 */
public class IntegrityCheckRunningBalance implements IntegrityCheck {

    private final Context context;
    private final DatabaseAdapter db;

    public IntegrityCheckRunningBalance(Context context, DatabaseAdapter db) {
        this.context = context;
        this.db = db;
    }

    @Override
    public Result check() {
        if (isRunningBalanceBroken()) {
            return new Result(Level.ERROR, context.getString(R.string.integrity_error));
        } else {
            return Result.OK;
        }
    }

    private boolean isRunningBalanceBroken() {
        List<Account> accounts = db.getAllAccountsList();
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
