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
package ru.orangesoftware.financisto.widget;

import ru.orangesoftware.financisto.activity.DateFilterActivity;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;

public class FilterButton extends ImageButton implements OnClickListener {
	
	public static interface FilterListener {
		void onFilterChanged(int type, long from, long to);
	}
	
	private Activity activity;
	
	public FilterButton(Context context) {
		super(context);
		setOnClickListener(this);
	}

	public FilterButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setOnClickListener(this);
	}

	public FilterButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		setOnClickListener(this);
	}
	
	public void setOwner(Activity activity) {
		this.activity = activity;
	}

	@Override
	public void onClick(View v) {
		Intent intent = new Intent(activity, DateFilterActivity.class);
		activity.startActivityForResult(intent, 1);
	}
	
}
