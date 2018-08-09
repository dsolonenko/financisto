/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.List;
import ru.orangesoftware.financisto.model.MyEntity;
import ru.orangesoftware.financisto.utils.Utils;

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 5/28/12 10:11 PM
 */
public class DatabaseUtils {
    
    public static long rawFetchId(DatabaseAdapter db, String query, String[] selectionArgs) {
        return rawFetchLong(db.db(), query, selectionArgs, -1);
    }

    public static long rawFetchLongValue(DatabaseAdapter db, String query, String[] selectionArgs) {
        return rawFetchLong(db.db(), query, selectionArgs, 0);
    }

    public static long rawFetchLong(SQLiteDatabase db, String query, String[] selectionArgs, long defaultValue) {
        try (Cursor c = db.rawQuery(query, selectionArgs)) {
            if (c.moveToFirst()) {
                return c.getLong(0);
            }
        }
        return defaultValue;
    }

    public static String generateSelectClause(String[] fields, String prefix) {
        StringBuilder res = new StringBuilder();
        for (String f : fields) {
            if (res.length() > 0) {
                res.append(", ");
            }
            if (Utils.isNotEmpty(prefix)) {
                res.append(prefix).append(".");
            }
            res.append(f);
        }
        return res.toString();
    }

    public static <T extends MyEntity> List<T> cursorToList(Cursor c, EntitySupplier<T> f) {
        // todo.mb: consider implementing limit here, e.g. 1000 items max to prevent memory issues
        List<T> res = new ArrayList<>(c.getCount());
        while (c.moveToNext()) {
            T a = f.fromCursor(c);
            res.add(a);
        }
        return res;
    }

    public interface EntitySupplier<T> {
        T fromCursor(Cursor c);
    }

}
