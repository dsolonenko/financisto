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

import ru.orangesoftware.financisto.R;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.view.View;

public class GraphWidget extends View {

	private static final int zeroColor = Resources.getSystem().getColor(android.R.color.secondary_text_dark);
	private static final int zeroLineColor = zeroColor;

	private final int positiveColor;
	private final int negativeColor;	
	private final int positiveLineColor = Color.argb(255, 124, 198, 35);
	private final int negativeLineColor = Color.argb(255, 239, 156, 0);	
	
	private final GraphUnit unit;

	private final long maxAmount;
	private final long maxAmountWidth;

	public GraphWidget(Context context, GraphUnit unit, long maxAmount, long maxAmountWidth) {
		super(context);		
		Resources r = context.getResources();
		positiveColor = r.getColor(R.color.positive_amount);
		negativeColor = r.getColor(R.color.negative_amount);		
		this.unit = unit;
		this.maxAmount = maxAmount;
		this.maxAmountWidth = maxAmountWidth;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		GraphStyle style = this.unit.style;
		int x = getPaddingLeft()+style.indent;
		int y = getPaddingTop();
		int w = getWidth()-getPaddingLeft()-getPaddingRight()-style.indent;
		GraphUnit u = this.unit;
		String name = u.name;
		canvas.drawText(name, x, y+style.nameHeight, style.namePaint);
		y += style.nameHeight+style.textDy;
		for (Amount a : u) {
			long amount = a.amount;
			int lineWidth = Math.max(1, (int)(1.0*Math.abs(amount)/maxAmount*(w-style.textDy-maxAmountWidth)));
			style.linePaint.setColor(amount == 0 ? zeroLineColor : (amount > 0 ? positiveLineColor : negativeLineColor));
			canvas.drawRect(x, y, x+lineWidth, y+style.lineHeight, style.linePaint);
			style.amountPaint.setColor(amount == 0 ? zeroColor : (amount > 0 ? positiveColor : negativeColor));
			canvas.drawText(a.getAmountText(), 
					x+lineWidth+style.textDy+a.amountTextWidth/2, 
					y+style.lineHeight/2+style.amountHeight/2, 
					style.amountPaint);
			y += style.lineHeight+style.dy;
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		GraphStyle style = this.unit.style;
		int specWidth = MeasureSpec.getSize(widthMeasureSpec);
		int h = 0;
		h += style.nameHeight + style.textDy;
		h += (style.lineHeight+style.dy)*unit.size();
		setMeasuredDimension(specWidth, getPaddingTop()+h+getPaddingBottom());
	}

}
