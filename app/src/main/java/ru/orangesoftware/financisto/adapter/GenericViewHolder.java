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

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import ru.orangesoftware.financisto.R;

public class GenericViewHolder {
	public TextView lineView;
	public TextView labelView;
	public TextView numberView;
	public TextView amountView;
	public ImageView iconView;
	
	public static GenericViewHolder createAndTag(View view) {
		GenericViewHolder views = new GenericViewHolder();
		views.lineView = view.findViewById(R.id.line1);
		views.labelView = view.findViewById(R.id.label);
		views.numberView = view.findViewById(R.id.number);
		views.amountView = view.findViewById(R.id.date);
		views.iconView = view.findViewById(R.id.icon);
		view.setTag(views);
		return views;
	}
}
