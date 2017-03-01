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
import ru.orangesoftware.financisto.report.ReportType;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ReportListAdapter extends BaseAdapter {

	private final ReportType[] reports;
	private final LayoutInflater inflater;
	
	public ReportListAdapter(Context context, ReportType[] reports) {
		this.reports = reports;
		this.inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public int getCount() {
		return reports.length;
	}

	@Override
	public Object getItem(int position) {
		return reports[position];
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Holder h;
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.report_list_item, parent, false);
			h = new Holder();
			h.icon = (ImageView)convertView.findViewById(R.id.icon);
			h.title = (TextView)convertView.findViewById(R.id.line1);
			h.label = (TextView)convertView.findViewById(R.id.label);
			convertView.setTag(h);
		} else {
			h = (Holder)convertView.getTag();
		}
		ReportType r = reports[position];
		h.title.setText(r.titleId);
		h.label.setText(r.summaryId);
		h.icon.setImageResource(r.iconId);
		return convertView;
	}
	
	private static final class Holder {
		public ImageView icon;
		public TextView title;
		public TextView label;
	}

}
