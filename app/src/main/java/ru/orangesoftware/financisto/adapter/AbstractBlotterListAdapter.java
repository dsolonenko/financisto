/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 ******************************************************************************/
package ru.orangesoftware.financisto.adapter;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.db.DatabaseAdapter;

public abstract class AbstractBlotterListAdapter extends AbstractGenericListAdapter {

	protected final int transferColor;
	protected final int futureColor;
	protected final Drawable icBlotterIncome;
	protected final Drawable icBlotterExpense;
	protected final Drawable icBlotterTransfer;	
	
	public AbstractBlotterListAdapter(DatabaseAdapter db, Context context, Cursor c) {
		super(db, context, c);
		transferColor = context.getResources().getColor(R.color.transfer_color);
		futureColor = context.getResources().getColor(R.color.future_color);
		icBlotterIncome = ContextCompat.getDrawable(context, R.drawable.ic_blotter_income);
		icBlotterExpense = ContextCompat.getDrawable(context, R.drawable.ic_blotter_expense);
		icBlotterTransfer = ContextCompat.getDrawable(context, R.drawable.ic_blotter_transfer);
	}
	
	protected abstract void bindView(GenericViewHolder v, Context context, Cursor cursor);
	
	protected void setIcon(GenericViewHolder v, long amount, boolean transfer) {
		// do nothing
	}

}
