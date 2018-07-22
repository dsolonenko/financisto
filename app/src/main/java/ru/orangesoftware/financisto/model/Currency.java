/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 *     Abdsandryk Souza - adding default currency and fromCursor
 ******************************************************************************/
package ru.orangesoftware.financisto.model;

import ru.orangesoftware.financisto.utils.CurrencyCache;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import static ru.orangesoftware.financisto.db.DatabaseHelper.CURRENCY_TABLE;
import static ru.orangesoftware.orb.EntityManager.DEF_SORT_COL;

@Entity
@Table(name = CURRENCY_TABLE)
public class Currency extends MyEntity implements SortableEntity {

	public static final Currency EMPTY = new Currency();
	
	static {
        EMPTY.id = 0;
        EMPTY.name = "";
        EMPTY.title = "Default";
		EMPTY.symbol = "";
        EMPTY.symbolFormat = SymbolFormat.RS;
		EMPTY.decimals = 2;
        EMPTY.decimalSeparator = "'.'";
        EMPTY.groupSeparator = "','";
	}

	@Column(name = "name")
	public String name;

	@Column(name = "symbol")
	public String symbol;

    @Column(name = "symbol_format")
    public SymbolFormat symbolFormat = SymbolFormat.RS;

	@Column(name = "is_default")
	public boolean isDefault;

	@Column(name = "decimals")
	public int decimals = 2;

	@Column(name = "decimal_separator")
	public String decimalSeparator;

	@Column(name = "group_separator")
	public String groupSeparator;

	@Column(name = DEF_SORT_COL)
	public long sortOrder;

    @Transient
	private volatile DecimalFormat format;

    @Override
    public String toString() {
        return name;
    }

    public NumberFormat getFormat() {
		DecimalFormat f = format;
		if (f == null) {
			f = CurrencyCache.createCurrencyFormat(this);
			format = f;
		}
		return f;
	}
	
	public static Currency defaultCurrency() {
		Currency c = new Currency();
		c.id = 2;
		c.name = "USD";
		c.title = "American Dollar";
		c.symbol = "$";
		c.decimals = 2;
		return c;
	}

	@Override
	public long getSortOrder() {
		return sortOrder;
	}
}
