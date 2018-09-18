/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 ******************************************************************************/
package ru.orangesoftware.orb;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Query<T> {
	
	private final Class<T> clazz;
	private final EntityDefinition ed;
	private final SQLiteDatabase db;
	
	private final LinkedList<String> orderBy = new LinkedList<>();
	private String where;
	private String[] whereArgs;
	
	Query(EntityManager em, Class<T> clazz) {
		this.db = em.db();
		this.clazz = clazz;
		this.ed = EntityManager.getEntityDefinitionOrThrow(clazz);
	}
	
	public Query<T> where(Expression ex) {
		Selection s = ex.toSelection(ed);
		where = s.selection;
		List<String> args = s.selectionArgs; 
		whereArgs = args.toArray(new String[args.size()]);
		return this;
	}
	
	public Query<T> asc(String field) {
		orderBy.add(ed.getColumnForField(field)+" asc");
		return this;
	}
	
	public Query<T> desc(String field) {
		orderBy.add(ed.getColumnForField(field)+" desc");
		return this;
	}

	public Cursor execute() {
		String query = ed.sqlQuery;
		String where = this.where;
		String[] whereArgs = this.whereArgs;
		StringBuilder sb = new StringBuilder(query);
		if (where != null) {
			sb.append(" where ").append(where);			
		}
		if (orderBy.size() > 0) {
			sb.append(" order by ");
			boolean addComma = false;
			for (String order : orderBy) {
				if (addComma) {
					sb.append(", ");
				}
				sb.append(order);
				addComma = true;
			}			
		}
		query = sb.toString();
		Log.d("QUERY "+clazz, query);
		Log.d("WHERE", where != null ? where : "");
		Log.d("ARGS", whereArgs != null ? Arrays.toString(whereArgs) : "");
		return db.rawQuery(query, whereArgs);
	}

	public T uniqueResult() {
		try (Cursor c = execute()) {
			if (c.moveToFirst()) {
				return EntityManager.loadFromCursor(c, clazz);
			}
			return null;
		}
	}

	public List<T> list() {
		return readEntityList(execute(), clazz);

	}

	public static <T> List<T> readEntityList(Cursor cursorRes, Class<T> clazz) {
		try (Cursor c = cursorRes) {
			List<T> list = new ArrayList<>();
			while (c.moveToNext()) {
				T e = EntityManager.loadFromCursor(c, clazz);
				list.add(e);
			}
			return list;
		}
	}
}
