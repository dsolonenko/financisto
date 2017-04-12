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
package ru.orangesoftware.financisto.activity;

import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListAdapter;

import java.util.Map;

import ru.orangesoftware.financisto.adapter.TransactionsListAdapter;
import ru.orangesoftware.financisto.blotter.TotalCalculationTask;
import ru.orangesoftware.financisto.model.Budget;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.MyEntity;
import ru.orangesoftware.financisto.model.Project;
import ru.orangesoftware.financisto.model.Total;

public class BudgetBlotterActivity extends BlotterActivity {
	
	private Map<Long, Category> categories;
	private Map<Long, Project> projects;
	
    public BudgetBlotterActivity() {
		super();
	}
        
	@Override
	protected void internalOnCreate(Bundle savedInstanceState) {
		categories = MyEntity.asMap(db.getCategoriesList(true));
		projects = MyEntity.asMap(db.getActiveProjectsList(true));
		super.internalOnCreate(savedInstanceState);
		bFilter.setVisibility(View.GONE);
	}
	
	@Override
	protected Cursor createCursor() {
		long budgetId = blotterFilter.getBudgetId();
		return getBlotterForBudget(budgetId);
	}

	@Override
	protected ListAdapter createAdapter(Cursor cursor) {
		return new TransactionsListAdapter(this, db, cursor);
	}
	
	private Cursor getBlotterForBudget(long budgetId) {
		Budget b = db.load(Budget.class, budgetId);
		String where = Budget.createWhere(b, categories, projects);
		return db.getBlotterWithSplits(where);
	}

    @Override
    protected TotalCalculationTask createTotalCalculationTask() {
        return new TotalCalculationTask(this, totalText) {
            @Override
            public Total getTotalInHomeCurrency() {
                long t0 = System.currentTimeMillis();
                try {
                    try {
                        long budgetId = blotterFilter.getBudgetId();
                        Budget b = db.load(Budget.class, budgetId);
                        Total total = new Total(b.getBudgetCurrency());
                        total.balance = db.fetchBudgetBalance(categories, projects, b);
                        return total;
                    } finally {
                        long t1 = System.currentTimeMillis();
                        Log.d("BUDGET TOTALS", (t1-t0)+"ms");
                    }
                } catch (Exception ex) {
                    Log.e("BudgetTotals", "Unexpected error", ex);
                    return Total.ZERO;
                }
            }

            @Override
            public Total[] getTotals() {
                return new Total[0];
            }
        };
    }

}
