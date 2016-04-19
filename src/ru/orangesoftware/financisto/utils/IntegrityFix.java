/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.utils;

import android.util.Log;
import ru.orangesoftware.financisto.db.DatabaseAdapter;

import java.util.concurrent.TimeUnit;

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 8/19/12 11:49 PM
 */
public class IntegrityFix {

    private final DatabaseAdapter db;

    public IntegrityFix(DatabaseAdapter db) {
        this.db = db;
    }

    public void fix() {
        long t0 = System.currentTimeMillis();
        db.recalculateAccountsBalances();
        long t1 = System.currentTimeMillis();
        Log.i("Financisto", "IntegrityFix: Recalculating balances done in " + TimeUnit.MILLISECONDS.toSeconds(t1 - t0) + "s");
        db.rebuildRunningBalances();
        long t2 = System.currentTimeMillis();
        Log.i("Financisto", "IntegrityFix: Updating running balances done in " + TimeUnit.MILLISECONDS.toSeconds(t2 - t1) + "s");
        db.restoreNoCategory();
        long t3 = System.currentTimeMillis();
        Log.i("Financisto", "IntegrityFix: Restoring system entities done in " + TimeUnit.MILLISECONDS.toSeconds(t3 - t2) + "s");
    }

}
