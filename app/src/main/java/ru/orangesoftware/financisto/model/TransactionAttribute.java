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

import ru.orangesoftware.financisto.db.DatabaseHelper.TransactionAttributeColumns;
import android.content.ContentValues;
import android.database.Cursor;

public class TransactionAttribute {
	
	public long attributeId;
	public long transactionId;
	public String value;
	
	public static TransactionAttribute fromCursor(Cursor c) {
		TransactionAttribute v = new TransactionAttribute();
		v.attributeId = c.getLong(TransactionAttributeColumns.Indicies.ATTRIBUTE_ID);
		v.transactionId = c.getLong(TransactionAttributeColumns.Indicies.TRANSACTION_ID);
		v.value = c.getString(TransactionAttributeColumns.Indicies.VALUE);
		return v;
	}
	
	public ContentValues toValues() {
		ContentValues values = new ContentValues();
		values.put(TransactionAttributeColumns.TRANSACTION_ID, transactionId);
		values.put(TransactionAttributeColumns.ATTRIBUTE_ID, attributeId);
		values.put(TransactionAttributeColumns.VALUE, value);
		return values;
	}
}
