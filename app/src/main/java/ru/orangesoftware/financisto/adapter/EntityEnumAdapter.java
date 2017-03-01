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
import ru.orangesoftware.financisto.utils.EntityEnum;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class EntityEnumAdapter<T extends EntityEnum> extends BaseAdapter {
	
	private final T[] values;
	private final LayoutInflater inflater;
	
	public EntityEnumAdapter(Context context, T[] values) {
		this.values = values;
		this.inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public int getCount() {
		return values.length;
	}

	@Override
	public T getItem(int i) {
		return values[i];
	}

	@Override
	public long getItemId(int i) {
		return i;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.entity_enum_list_item, parent, false);
		}
		ImageView icon = (ImageView)convertView.findViewById(R.id.icon);
		TextView title = (TextView)convertView.findViewById(R.id.line1);
		T v = values[position];
		icon.setImageResource(v.getIconId());
		title.setText(v.getTitleId());
		return convertView;
	}


}
