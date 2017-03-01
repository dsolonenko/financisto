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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import ru.orangesoftware.financisto.R;
import android.content.Context;

@Entity
@Table(name = "V_TRANSACTION_ATTRIBUTES")
public class TransactionAttributeInfo {

	@Id
	@Column(name = "_id")
	public long transactionId;

	@Id
	@Column(name = "attribute_id")
	public long attributeId;

	@Column(name = "attribute_type")
	public int type;

	@Column(name = "attribute_name")
	public String name;

	@Column(name = "attribute_value")
	public String value;
		
	@Column(name = "attribute_list_values")
	public String listValues;
	
	public String getValue(Context context) {
		if (type == Attribute.TYPE_CHECKBOX) {
			String[] values = listValues != null ? listValues.split(";") : null;
			boolean checked = Boolean.valueOf(value);
			if (values != null && values.length > 1) {
				return values[checked ? 0 : 1];
			}
			return checked ? context.getString(R.string.checkbox_values_yes) : context.getString(R.string.checkbox_values_no);
		} else {
			return value;
		}
	}
	
}
