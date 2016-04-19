/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.db;

import android.database.Cursor;

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 5/28/12 10:11 PM
 */
public class DatabaseUtils {
    
    public static long rawFetchId(DatabaseAdapter db, String query, String[] selectionArgs) {
        return rawFetchLong(db, query, selectionArgs, -1);
    }

    public static long rawFetchLongValue(DatabaseAdapter db, String query, String[] selectionArgs) {
        return rawFetchLong(db, query, selectionArgs, 0);
    }

    private static long rawFetchLong(DatabaseAdapter db, String query, String[] selectionArgs, long defaultValue) {
        Cursor c = db.db().rawQuery(query, selectionArgs);
        try {
            if (c.moveToFirst()) {
                return c.getLong(0);
            }
        } finally {
            c.close();
        }
        return defaultValue;
    }

}
