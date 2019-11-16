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

import android.content.Context;
import androidx.core.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.utils.SummaryEntityEnum;

public class SummaryEntityListAdapter extends BaseAdapter {

    private final Context context;
    private final SummaryEntityEnum[] entities;
    private final LayoutInflater inflater;

    public SummaryEntityListAdapter(Context context, SummaryEntityEnum[] reports) {
        this.entities = reports;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.context = context;
    }

    @Override
    public int getCount() {
        return entities.length;
    }

    @Override
    public Object getItem(int position) {
        return entities[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Holder h;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.summary_entity_list_item, parent, false);
            h = new Holder();
            h.icon = convertView.findViewById(R.id.icon);
            h.title = convertView.findViewById(R.id.line1);
            h.label = convertView.findViewById(R.id.label);
            convertView.setTag(h);
        } else {
            h = (Holder) convertView.getTag();
        }
        SummaryEntityEnum r = entities[position];
        h.title.setText(r.getTitleId());
        h.label.setText(r.getSummaryId());
        if (r.getIconId() > 0) {
            h.icon.setImageResource(r.getIconId());
            h.icon.setColorFilter(ContextCompat.getColor(context, R.color.colorPrimary));
        }
        return convertView;
    }

    private static final class Holder {
        public ImageView icon;
        public TextView title;
        public TextView label;
    }

}
