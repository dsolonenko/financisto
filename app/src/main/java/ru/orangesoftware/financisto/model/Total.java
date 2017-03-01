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

import android.content.Context;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.utils.Utils;

public class Total {
	
	public static final Total ZERO = new Total(Currency.EMPTY);

    public static Total asIncomeExpense(Currency currency, long income, long expense) {
        Total total = new Total(currency);
        total.showIncomeExpense = true;
        total.income = income;
        total.expenses = expense;
        total.balance = income+expense;
        return total;
    }

	public final Currency currency;
	public final boolean showAmount;
    public final TotalError error;
                                
	public long amount;
	public long balance;

    public boolean showIncomeExpense;
    public long income;
    public long expenses;

	public Total(Currency currency, boolean showAmount) {
		this.currency = currency;
		this.showAmount = showAmount;
        this.showIncomeExpense = false;
        this.error = null;
	}

	public Total(Currency currency) {
		this.currency = currency;
		this.showAmount = false;
        this.showIncomeExpense = false;
        this.error = null;
	}

    public Total(Currency currency, TotalError error) {
        this.currency = currency;
        this.showAmount = false;
        this.showIncomeExpense = false;
        this.error = error;
    }
    
    public boolean isError() {
        return error != null;
    }

    public String getError(Context context) {
        if (error != null) {
            return context.getString(R.string.rate_not_available_on_date_error, Utils.formatRateDate(context, error.datetime), error.currency, this.currency);
        }
        return context.getString(R.string.not_available);
    }
}
