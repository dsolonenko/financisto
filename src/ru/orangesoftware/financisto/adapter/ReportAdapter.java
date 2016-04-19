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
package ru.orangesoftware.financisto.adapter;

import android.content.Context;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import ru.orangesoftware.financisto.graph.Amount;
import ru.orangesoftware.financisto.graph.GraphUnit;
import ru.orangesoftware.financisto.graph.GraphWidget;

import java.util.List;

public class ReportAdapter extends BaseAdapter {
		
	private final Context context; 
	private final List<GraphUnit> units;

	private long maxAmount = 0;
	private long maxAmountWidth = 0;
	
	public ReportAdapter(Context context, List<GraphUnit> units) {
		this.context = context;
		this.units = units;
		Rect rect = new Rect();
		for (GraphUnit u : units) {
			for (Amount a : u) {
				String amountText = a.getAmountText();
				u.style.amountPaint.getTextBounds(amountText, 0, amountText.length(), rect);
				a.amountTextWidth = rect.width();
				a.amountTextHeight = rect.height();
				maxAmount = Math.max(maxAmount, Math.abs(a.amount));
				maxAmountWidth = Math.max(maxAmountWidth, a.amountTextWidth);			
			}			
		}
	}

	@Override
	public int getCount() {
		return units.size();
	}

	@Override
	public Object getItem(int position) {
		return units.get(position);
	}

	@Override
	public long getItemId(int position) {
		return units.get(position).id;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		GraphUnit unit = units.get(position);
		GraphWidget w = new GraphWidget(context, unit, maxAmount, maxAmountWidth);
		w.setPadding(5, 10, 5, 5);
		return w;
	}

}
