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

import android.content.ContentValues;
import android.database.Cursor;
import ru.orangesoftware.financisto.db.DatabaseHelper.AttributeColumns;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import static ru.orangesoftware.financisto.db.DatabaseHelper.ATTRIBUTES_TABLE;
import static ru.orangesoftware.orb.EntityManager.DEF_SORT_COL;

@Entity
@Table(name = ATTRIBUTES_TABLE)
public class Attribute extends MyEntity implements SortableEntity {

    public static final int DELETE_AFTER_EXPIRED_ID = -1;

    public static Attribute deleteAfterExpired() {
        Attribute attribute = new Attribute();
        attribute.id = DELETE_AFTER_EXPIRED_ID;
        attribute.title = "DELETE_AFTER_EXPIRED";
        attribute.type = TYPE_CHECKBOX;
        attribute.defaultValue = "true";
        return attribute;
    }

    public static final int TYPE_TEXT = 1;
    public static final int TYPE_NUMBER = 2;
    public static final int TYPE_LIST = 3;
    public static final int TYPE_CHECKBOX = 4;

    @Column(name = "type")
    public int type;

    @Column(name = "list_values")
    public String listValues;

    @Column(name = "default_value")
    public String defaultValue;

    @Column(name = DEF_SORT_COL)
    public long sortOrder;

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
        a.title = c.getString(AttributeColumns.Indicies.TITLE);
        a.type = c.getInt(AttributeColumns.Indicies.TYPE);
        a.listValues = c.getString(AttributeColumns.Indicies.LIST_VALUES);
        a.defaultValue = c.getString(AttributeColumns.Indicies.DEFAULT_VALUE);
        return a;
    }

    public ContentValues toValues() {
        ContentValues values = new ContentValues();
        values.put(AttributeColumns.TITLE, title);
        values.put(AttributeColumns.TYPE, type);
        values.put(AttributeColumns.LIST_VALUES, listValues);
        values.put(AttributeColumns.DEFAULT_VALUE, defaultValue);
        return values;
    }

    @Override
    public long getSortOrder() {
        return sortOrder;
    }
}
