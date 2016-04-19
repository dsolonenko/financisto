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
import ru.orangesoftware.financisto.model.MyLocation;
import ru.orangesoftware.orb.EntityManager;
import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

public class LocationListAdapter  extends ResourceCursorAdapter {

	public LocationListAdapter(DatabaseAdapter db, Context context, Cursor c) {
		super(context, R.layout.location_item, c);
	}
	
	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		View view = super.newView(context, cursor, parent);
		GenericViewHolder views = new GenericViewHolder();
		views.lineView = (TextView)view.findViewById(R.id.line1);
		views.labelView = (TextView)view.findViewById(R.id.label);
		view.setTag(views);
		return view;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		GenericViewHolder v = (GenericViewHolder)view.getTag();
		MyLocation loc = EntityManager.loadFromCursor(cursor, MyLocation.class);
		v.lineView.setText(loc.name);
		if (loc.resolvedAddress != null) {
			v.labelView.setText(loc.resolvedAddress);
		} else {
			StringBuilder sb = new StringBuilder();
			sb.append(loc.provider).append(", ").append("Lat: ").append(loc.latitude).append(", Lon: ").append(loc.longitude);
			if (loc.accuracy > 0) {
				sb.append(", Acc:").append(loc.accuracy).append("m");
			}
			v.labelView.setText(sb.toString());
		}
	}
	
}
