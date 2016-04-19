/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.blotter;

import android.content.Context;
import android.widget.TextView;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.TransactionsTotalCalculator;
import ru.orangesoftware.financisto.filter.WhereFilter;
import ru.orangesoftware.financisto.model.Total;

import static ru.orangesoftware.financisto.db.DatabaseAdapter.enhanceFilterForAccountBlotter;

public class AccountTotalCalculationTask extends TotalCalculationTask {

	private final DatabaseAdapter db;
	private final WhereFilter filter;

	public AccountTotalCalculationTask(Context context, DatabaseAdapter db, WhereFilter filter, TextView totalText) {
        super(context, totalText);
		this.db = db;
		this.filter = enhanceFilterForAccountBlotter(filter);
	}

    @Override
    public Total getTotalInHomeCurrency() {
        TransactionsTotalCalculator calculator = new TransactionsTotalCalculator(db, filter);
        return calculator.getAccountTotal();
    }

    @Override
    public Total[] getTotals() {
        TransactionsTotalCalculator calculator = new TransactionsTotalCalculator(db, filter);
        return calculator.getTransactionsBalance();
    }

}
