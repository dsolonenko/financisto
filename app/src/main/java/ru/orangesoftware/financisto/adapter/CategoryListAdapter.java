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

import java.util.Map;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.DatabaseHelper.CategoryViewColumns;
import ru.orangesoftware.financisto.model.Category;
import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

public class CategoryListAdapter extends ResourceCursorAdapter {
	
	private final DatabaseAdapter db;
	private Map<Long, String> attributes;
	
	public CategoryListAdapter(DatabaseAdapter db, Context context, int layout, Cursor c) {
		super(context, layout, c);
		this.db = db;
	}		

	public void fetchAttributes() {
		this.attributes = db.getAllAttributesMap();
	}
	
	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		long id = cursor.getLong(CategoryViewColumns._id.ordinal());
		int level = cursor.getInt(CategoryViewColumns.level.ordinal());
		String title = cursor.getString(CategoryViewColumns.title.ordinal());
		TextView labelView = (TextView)view.findViewById(android.R.id.text1);
		if (labelView != null) {
			labelView.setText(Category.getTitle(title, level));
		} else {
			TextView spanView = (TextView)view.findViewById(R.id.span);
			if (level > 1) {
				spanView.setVisibility(View.VISIBLE);
				spanView.setText(Category.getTitleSpan(level));
			} else {
				spanView.setVisibility(View.GONE);
			}
			TextView titleView = (TextView)view.findViewById(R.id.line1);
			titleView.setText(title);
			TextView attributesView = (TextView)view.findViewById(R.id.label);
			if (attributes != null && attributes.containsKey(id)) {
				attributesView.setVisibility(View.VISIBLE);
				attributesView.setText(attributes.get(id));
			} else {
				attributesView.setVisibility(View.GONE);
			}		
		}
	}
	
}
