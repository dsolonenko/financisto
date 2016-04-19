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

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ResourceCursorAdapter;

public abstract class AbstractGenericListAdapter extends ResourceCursorAdapter {

	public AbstractGenericListAdapter(DatabaseAdapter db, Context context, Cursor c) {
		super(context, R.layout.generic_list_item, c);
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		View view = super.newView(context, cursor, parent);
		GenericViewHolder.createAndTag(view);
		return view;
	}
	
	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		GenericViewHolder views = (GenericViewHolder)view.getTag();
		bindView(views, context, cursor);
	}

	protected abstract void bindView(GenericViewHolder v, Context context, Cursor cursor);
	
}
