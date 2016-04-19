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

import java.util.List;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.model.MyEntity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class EntityListAdapter<T extends MyEntity> extends BaseAdapter {
	
	private final LayoutInflater inflater;
	
	private List<T> entities;
	
	public EntityListAdapter(Context context, List<T> entities) {
		this.entities = entities;
		this.inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}
	
	public void setEntities(List<T> entities) {
		this.entities = entities;
		notifyDataSetChanged();
	}

	@Override
	public int getCount() {
		return entities.size();
	}

	@Override
	public T getItem(int i) {
		return entities.get(i);
	}

	@Override
	public long getItemId(int i) {
		return getItem(i).id;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		GenericViewHolder v;
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.generic_list_item, parent, false);
			v = GenericViewHolder.createAndTag(convertView);
		} else {
			v = (GenericViewHolder)convertView.getTag();
		}
		v.labelView.setVisibility(View.GONE);
		v.amountView.setVisibility(View.GONE);
		
		MyEntity e = getItem(position);
		v.lineView.setText(e.title);
		return convertView;
	}

}
