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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

import android.content.res.Resources;
import android.graphics.Color;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.CategoryTree;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class CategoryListAdapter2 extends BaseAdapter {

	private final LayoutInflater inflater;
	private CategoryTree<Category> categories;
	private Map<Long, String> attributes;

	private final ArrayList<Category> list = new ArrayList<Category>();
	private final HashSet<Long> state = new HashSet<Long>();
	
	private final Drawable expandedDrawable;
	private final Drawable collapsedDrawable;
    private final int incomeColor;
    private final int expenseColor;

    private final int levelPadding;

	public CategoryListAdapter2(Context context, CategoryTree<Category> categories) {
		this.inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.categories = categories;
        Resources resources = context.getResources();
        this.expandedDrawable = resources.getDrawable(R.drawable.expander_ic_maximized);
		this.collapsedDrawable = resources.getDrawable(R.drawable.expander_ic_minimized);
        this.incomeColor = resources.getColor(R.color.category_type_income);
        this.expenseColor = resources.getColor(R.color.category_type_expense);
        this.levelPadding = resources.getDimensionPixelSize(R.dimen.category_padding);
		recreatePlainList();
	}
	
	private void recreatePlainList() {
		list.clear();
		addCategories(categories);
	}

	private void addCategories(CategoryTree<Category> categories) {
		if (categories == null || categories.isEmpty()) {
			return;
		}
		for (Category c : categories) {
			list.add(c);
			if (state.contains(c.id)) {
				addCategories(c.children);
			}
		}
	}

	@Override
	public int getCount() {
		return list.size();
	}

	@Override
	public Category getItem(int position) {
		return list.get(position);
	}

	@Override
	public long getItemId(int position) {
		return getItem(position).id;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		Holder h;
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.category_list_item2, parent, false);
			h = Holder.create(convertView);
		} else {
			h = (Holder)convertView.getTag();
		}
        TextView indicator = h.indicator;
		ImageView span = h.span;
		TextView title = h.title;
		TextView label = h.label;
		final Category c = getItem(position);
		title.setText(c.title);
        int padding  = levelPadding*(c.level-1);
		if (c.hasChildren()) {
			span.setImageDrawable(state.contains(c.id) ? expandedDrawable : collapsedDrawable);
			span.setClickable(true);
			span.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v) {
					onListItemClick(c.id);
				}
			});
            span.setPadding(padding, 0, 0, 0);
			span.setVisibility(View.VISIBLE);
            padding += collapsedDrawable.getMinimumWidth();
		} else {
            padding += levelPadding/2;
			span.setVisibility(View.GONE);
		}
        title.setPadding(padding, 0, 0, 0);
        label.setPadding(padding, 0, 0, 0);
		long id = c.id;
		if (attributes != null && attributes.containsKey(id)) {
			label.setText(attributes.get(id));
			label.setVisibility(View.VISIBLE);
		} else {
			label.setVisibility(View.GONE);
		}
        if (c.isIncome()) {
            indicator.setBackgroundColor(incomeColor);
        } else if (c.isExpense()) {
            indicator.setBackgroundColor(expenseColor);
        } else {
            indicator.setBackgroundColor(Color.WHITE);
        }
		return convertView;
	}
	
	public void onListItemClick(long id) {
		if (state.contains(id)) {
			state.remove(id);
		} else {
			state.add(id);
		}
		notifyDataSetChanged();
	}
	
	public void collapseAllCategories() {
		state.clear();
		notifyDataSetChanged();
	}

	public void expandAllCategories() {
		expandAllCategories(categories);
		notifyDataSetChanged();
	}
	
	private void expandAllCategories(CategoryTree<Category> categories) {
		if (categories == null || categories.isEmpty()) {
			return;
		}
		for (Category c : categories) {
			state.add(c.id);
			expandAllCategories(c.children);
		}		
	}
	
	@Override
	public void notifyDataSetChanged() {
		recreatePlainList();
		super.notifyDataSetChanged();		
	}

	public void setCategories(CategoryTree<Category> categories) {
		this.categories = categories;
		recreatePlainList();
	}

	public void setAttributes(Map<Long, String> attributes) {
		this.attributes = attributes;	
	}

	private static class Holder {
		
        public TextView indicator;
		public ImageView span;
		public TextView title;
		public TextView label;

		public static Holder create(View convertView) {
			Holder h = new Holder();
            h.indicator = (TextView)convertView.findViewById(R.id.indicator);
			h.span = (ImageView)convertView.findViewById(R.id.span);
			h.title = (TextView)convertView.findViewById(R.id.line1);
			h.label = (TextView)convertView.findViewById(R.id.label);
			convertView.setTag(h);
			return h;
		}
		
	}
	
}
