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
package ru.orangesoftware.financisto.utils;

import android.database.Cursor;
import gnu.trove.map.hash.TLongObjectHashMap;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.orb.EntityManager;
import ru.orangesoftware.orb.Query;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Collection;

public class CurrencyCache {

    //@ProtectedBy("this")
	private static final TLongObjectHashMap<Currency> CURRENCIES = new TLongObjectHashMap<Currency>();
	
	public static synchronized Currency getCurrency(EntityManager em, long currencyId) {
		Currency cachedCurrency = CURRENCIES.get(currencyId);
        if (cachedCurrency == null) {
            cachedCurrency = em.get(Currency.class, currencyId);
            if (cachedCurrency == null) {
                cachedCurrency = Currency.EMPTY;
            }
            CURRENCIES.put(currencyId, cachedCurrency);
        }
        return cachedCurrency;
	}
	
	public static synchronized Currency getCurrencyOrEmpty(long currencyId) {
		Currency c = CURRENCIES.get(currencyId);
		return c != null ? c : Currency.EMPTY;
	}

	public static synchronized void initialize(EntityManager em) {
        TLongObjectHashMap<Currency> currencies = new TLongObjectHashMap<Currency>();
		Query<Currency> q = em.createQuery(Currency.class);
		Cursor c = q.execute();
		try {
			while (c.moveToNext()) {
				Currency currency = EntityManager.loadFromCursor(c, Currency.class);
				currencies.put(currency.id, currency);
			}
		} finally {
			c.close();
		}
		CURRENCIES.putAll(currencies);
	}
	
	public static DecimalFormat createCurrencyFormat(Currency c) {
		DecimalFormatSymbols dfs = new DecimalFormatSymbols();
		dfs.setDecimalSeparator(charOrEmpty(c.decimalSeparator, dfs.getDecimalSeparator()));
		dfs.setGroupingSeparator(charOrEmpty(c.groupSeparator, dfs.getGroupingSeparator()));
		dfs.setMonetaryDecimalSeparator(dfs.getDecimalSeparator());
		dfs.setCurrencySymbol(c.symbol);

		DecimalFormat df = new DecimalFormat("#,##0.00", dfs);
		df.setGroupingUsed(dfs.getGroupingSeparator() > 0);
		df.setMinimumFractionDigits(c.decimals);
		df.setMaximumFractionDigits(c.decimals);
		df.setDecimalSeparatorAlwaysShown(false);
		return df;
	}

	private static char charOrEmpty(String s, char c) {
		return s != null ? (s.length() > 2 ? s.charAt(1) : 0): c;
	}

	public static synchronized Collection<Currency> getAllCurrencies() {
		return CURRENCIES.valueCollection();
	}


}
