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

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;

public class GraphStyle {

	public final int dy;
	public final int textDy;
	public final int lineHeight;
	public final int indent;
	public final int nameHeight;
	public final int amountHeight;
	
	public final Paint namePaint;
	public final Paint amountPaint;
	public final Paint linePaint;
	
	private GraphStyle(
			int dy, int textDy, int indent, 
			int lineHeight, int nameHeight, int amountHeight, 
			Paint namePaint, Paint amountPaint, Paint linePaint) {
		this.dy = dy;
		this.textDy = textDy;
		this.indent = indent;
		this.lineHeight = lineHeight;
		this.nameHeight = nameHeight;
		this.amountHeight = amountHeight;
		this.namePaint = namePaint;		
		this.amountPaint = amountPaint;
		this.linePaint = linePaint;
	}

	public static class Builder {

        private final Context context;

		private int dy = 2;
		private int textDy = 5;
		private int lineHeight = 30;
		private int nameTextSize = 14;
		private int amountTextSize = 12;
		private int indent = 0;

        public Builder(Context context) {
            this.context = context;
        }

        public Builder dy(int x) {
			this.dy = x;
			return this;
		}
		
		public Builder textDy(int x) {
			this.textDy = x;
			return this;
		}

		public Builder lineHeight(int x) {
			this.lineHeight = x;
			return this;
		}
		
		public Builder nameTextSize(int x) {
			this.nameTextSize = x;
			return this;
		}		
		
		public Builder amountTextSize(int x) {
			this.amountTextSize = x;
			return this;
		}		
		
		public Builder indent(int x) {
			this.indent = x;
			return this;
		}		

		public GraphStyle build() {
            float density = context.getResources().getDisplayMetrics().density;
			Rect rect = new Rect();
			Paint namePaint = new Paint();
			Paint amountPaint = new Paint();
			Paint linePaint = new Paint();
			namePaint.setColor(Color.WHITE);
			namePaint.setAntiAlias(true);
			namePaint.setTextAlign(Align.LEFT);
			namePaint.setTextSize(spToPx(nameTextSize, density));
			namePaint.setTypeface(Typeface.DEFAULT_BOLD);
			namePaint.getTextBounds("A", 0, 1, rect);		
			int nameHeight = rect.height();
			amountPaint.setColor(Color.WHITE);
			amountPaint.setAntiAlias(true);
			amountPaint.setTextSize(spToPx(amountTextSize, density));
			amountPaint.setTextAlign(Align.CENTER);
			amountPaint.getTextBounds("8", 0, 1, rect);		
			int amountHeight = rect.height();
			linePaint.setStyle(Style.FILL);
			return new GraphStyle(
					spToPx(dy, density),
                    spToPx(textDy, density),
                    spToPx(indent, density),
					spToPx(lineHeight, density),
                    nameHeight,
                    amountHeight,
					namePaint,
                    amountPaint,
                    linePaint);
		}

        private int spToPx(int textSizeSp, float density) {
            return (int)(0.5f+density*textSizeSp);
        }

    }
	
}
