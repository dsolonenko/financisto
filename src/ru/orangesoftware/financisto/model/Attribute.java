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
package ru.orangesoftware.financisto.model;

import ru.orangesoftware.financisto.db.DatabaseHelper.AttributeColumns;
import android.content.ContentValues;
import android.database.Cursor;

public class Attribute extends MyEntity {

	public static final int TYPE_TEXT = 1; 
	public static final int TYPE_NUMBER = 2;
	public static final int TYPE_LIST = 3;
	public static final int TYPE_CHECKBOX = 4;
	
	public long id = -1;
	public String name;
	public int type;
	public String listValues;
	public String defaultValue;
	
	public Attribute() {
    }
	
	public String getDefaultValue() {
		if (type == TYPE_CHECKBOX) {
			String[] values = listValues != null ? listValues.split(";") : null;
			boolean checked = Boolean.valueOf(defaultValue);
			if (values != null && values.length > 1) {
				return values[checked ? 0 : 1];
			}
			return String.valueOf(checked);
		} else {
			return defaultValue;
		}
	}
	
	public static Attribute fromCursor(Cursor c) {
		Attribute a = new Attribute();
        a.id = c.getLong(AttributeColumns.Indicies.ID);
        a.name = c.getString(AttributeColumns.Indicies.NAME);
		a.type = c.getInt(AttributeColumns.Indicies.TYPE);
		a.listValues = c.getString(AttributeColumns.Indicies.LIST_VALUES);
		a.defaultValue = c.getString(AttributeColumns.Indicies.DEFAULT_VALUE);
		a.remoteKey = c.getString(AttributeColumns.Indicies.REMOTE_KEY);
		return a;
	}

	public ContentValues toValues() {
		ContentValues values = new ContentValues();
		values.put(AttributeColumns.NAME, name);
		values.put(AttributeColumns.TYPE, type);
		values.put(AttributeColumns.LIST_VALUES, listValues);
		values.put(AttributeColumns.DEFAULT_VALUE, defaultValue);
		return values;
	}
	
}
