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
package ru.orangesoftware.financisto.graph;

import java.math.BigDecimal;
import java.util.*;

import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.TotalError;
import ru.orangesoftware.financisto.report.IncomeExpense;

public class GraphUnit implements Comparable<GraphUnit>, Iterable<Amount> {

	public final long id;
	public final String name;
	public final GraphStyle style;
    public final Currency currency;

    private final IncomeExpenseAmount incomeExpenseAmount = new IncomeExpenseAmount();
    private final List<Amount> amounts = new LinkedList<Amount>();

    public long maxAmount;
    public TotalError error;
	
	public GraphUnit(long id, String name, Currency currency, GraphStyle style) {
		this.id = id;
		this.name = name != null ? name : "";
		this.style = style;
        this.currency = currency;
	}
	
	public void addAmount(BigDecimal amount, boolean forceIncome) {
        incomeExpenseAmount.add(amount, forceIncome);
	}

    public IncomeExpenseAmount getIncomeExpense() {
        return incomeExpenseAmount;
    }

    public void flatten(IncomeExpense incomeExpense) {
        if (amounts.isEmpty()) {
            incomeExpenseAmount.filter(incomeExpense);
            long income = incomeExpenseAmount.income.longValue();
            long expense = incomeExpenseAmount.expense.longValue();
            addToAmounts(income);
            addToAmounts(expense);
            Collections.sort(amounts);
            this.maxAmount = incomeExpenseAmount.max();
        }
    }

    private void addToAmounts(long amount) {
        if (amount != 0) {
            amounts.add(new Amount(currency, amount));
        }
    }

    @Override
	public int compareTo(GraphUnit that) {
		return that.maxAmount == this.maxAmount ? 0 : (that.maxAmount > this.maxAmount ? 1 : -1);
	}

    @Override
    public Iterator<Amount> iterator() {
        return amounts.iterator();
    }

    public int size() {
        return amounts.size();
    }
    
}
