/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.utils;

import ru.orangesoftware.financisto.db.DatabaseAdapter;

public class IntegrityFix {

    private final DatabaseAdapter db;

    public IntegrityFix(DatabaseAdapter db) {
        this.db = db;
    }

    public void fix() {
        db.restoreSystemEntities();
        db.recalculateAccountsBalances();
        db.rebuildRunningBalances();
    }

}
