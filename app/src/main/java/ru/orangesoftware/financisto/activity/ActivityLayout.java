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
package ru.orangesoftware.financisto.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.support.v4.util.Pair;
import android.view.View;
import android.widget.*;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.model.MultiChoiceItem;
import ru.orangesoftware.financisto.utils.Utils;
import ru.orangesoftware.financisto.view.NodeInflater;
import ru.orangesoftware.financisto.view.NodeInflater.*;

import java.util.List;

public class ActivityLayout {

	public final NodeInflater inflater;
	private final ActivityLayoutListener listener;

	public ActivityLayout(NodeInflater inflater, ActivityLayoutListener listener) {
		this.inflater = inflater;
		this.listener = listener;
	}

	public View addTitleNode(LinearLayout layout, int labelId) {
		Builder b = inflater.new Builder(layout, R.layout.select_entry_title);
		return b.withLabel(labelId).create();
	}

	public View addTitleNode(LinearLayout layout, String label) {
		Builder b = inflater.new Builder(layout, R.layout.select_entry_title);
		return b.withLabel(label).create();
	}

	public View addTitleNodeNoDivider(LinearLayout layout, int labelId) {
		Builder b = inflater.new Builder(layout, R.layout.select_entry_title);
		return b.withLabel(labelId).withNoDivider().create();
	}

	public View addTitleNodeNoDivider(LinearLayout layout, String label) {
		Builder b = inflater.new Builder(layout, R.layout.select_entry_title);
		return b.withLabel(label).withNoDivider().create();
	}

	public void addListNodeSingle(LinearLayout layout, int id, int labelId) {
		Builder b = inflater.new Builder(layout, R.layout.select_entry_simple_list);
		b.withId(id, listener).withLabel(labelId).create();
	}

	public void addInfoNodeSingle(LinearLayout layout, int id, int labelId) {
		Builder b = inflater.new Builder(layout, R.layout.select_entry_single);
		b.withId(id, listener).withLabel(labelId).create();
	}

	public TextView addInfoNodeSingle(LinearLayout layout, int id, String label) {
		Builder b = inflater.new Builder(layout, R.layout.select_entry_single);
		View v = b.withId(id, listener).withLabel(label).create();
		TextView labelView = v.findViewById(R.id.label);
		labelView.setTag(v);
		return labelView;
	}

	public TextView addInfoNode(LinearLayout layout, int id, int labelId, int defaultValueResId) {
		Builder b = inflater.new Builder(layout, R.layout.select_entry_simple);
		View v = b.withId(id, listener).withLabel(labelId).withData(defaultValueResId).create();
		TextView data = v.findViewById(R.id.data);
		data.setTag(v);
		return data;
	}

	public TextView addInfoNode(LinearLayout layout, int id, int labelId, String defaultValue) {
		Builder b = inflater.new Builder(layout, R.layout.select_entry_simple);
		View v = b.withId(id, listener).withLabel(labelId).withData(defaultValue).create();
		return (TextView)v.findViewById(R.id.data);
	}

	public TextView addInfoNode(LinearLayout layout, int id, String label, String defaultValue) {
		Builder b = inflater.new Builder(layout, R.layout.select_entry_simple);
		View v = b.withId(id, listener).withLabel(label).withData(defaultValue).create();
		TextView data = v.findViewById(R.id.data);
		data.setTag(v);
		return data;
	}

	public View addListNodeIcon(LinearLayout layout, int id, int labelId, int defaultValueResId) {
		Builder b = inflater.new Builder(layout, R.layout.select_entry_icon);
		return b.withId(id, listener).withLabel(labelId).withData(defaultValueResId).create();
	}

	public View addListNodeIcon(LinearLayout layout, int id, int labelId, String defaultValue) {
		Builder b = inflater.new Builder(layout, R.layout.select_entry_icon);
		return b.withId(id, listener).withLabel(labelId).withData(defaultValue).create();
	}

	public View addListNode(LinearLayout layout, int id) {
		Builder b = inflater.new Builder(layout, R.layout.select_entry);
		return b.withId(id, listener).create();
	}

	public TextView addListNode(LinearLayout layout, int id, int labelId, int defaultValueResId) {
		Builder b = inflater.new Builder(layout, R.layout.select_entry);
		View v = b.withId(id, listener).withLabel(labelId).withData(defaultValueResId).create();
		TextView data = v.findViewById(R.id.data);
		data.setTag(v);
		return data;
	}

	public TextView addListNode(LinearLayout layout, int id, int labelId, String defaultValue) {
		Builder b = inflater.new Builder(layout, R.layout.select_entry);
		View v = b.withId(id, listener).withLabel(labelId).withData(defaultValue).create();
		return (TextView)v.findViewById(R.id.data);
	}

	public TextView addListNode(LinearLayout layout, int id, String label, String defaultValue) {
		Builder b = inflater.new Builder(layout, R.layout.select_entry);
		View v = b.withId(id, listener).withLabel(label).withData(defaultValue).create();
		return (TextView)v.findViewById(R.id.data);
	}

	public View addCheckboxNode(LinearLayout layout, int id) {
		Builder b = inflater.new Builder(layout, R.layout.select_entry_checkbox);
		return b.withId(id, listener).create();
	}

	public CheckBox addCheckboxNode(LinearLayout layout, int id, int labelId, int dataId, boolean checked) {
		CheckBoxBuilder b = inflater.new CheckBoxBuilder(layout);
		View v = b.withCheckbox(checked).withLabel(labelId).withId(id, listener).withData(dataId).create();
		return (CheckBox)v.findViewById(R.id.checkbox);
	}

	public void addInfoNodePlus(LinearLayout layout, int id, int plusId, int labelId) {
		ListBuilder b = inflater.new ListBuilder(layout, R.layout.select_entry_simple_plus);
		b.withButtonId(plusId, listener).withLabel(labelId).withId(id, listener).create();
	}

	public TextView addListNodePlusWithoutDivider(LinearLayout layout, int id, int plusId, int labelId, int defaultValueResId) {
		ListBuilder b = inflater.new ListBuilder(layout, R.layout.select_entry_plus);
		View v = b.withButtonId(plusId, listener).withId(id, listener).withLabel(labelId).withData(defaultValueResId).withNoDivider().create();
		return (TextView)v.findViewById(R.id.data);
	}

	public TextView addListNodePlusWithoutLabel(LinearLayout layout, int id, int plusId, int defaultValueResId) {
		ListBuilder b = inflater.new ListBuilder(layout, R.layout.select_entry_plus_no_label);
		View v = b.withButtonId(plusId, listener).withId(id, listener).withData(defaultValueResId).create();
		return (TextView)v.findViewById(R.id.data);
	}

	public TextView addListNodePlus(LinearLayout layout, int id, int plusId, int labelId, int defaultValueResId) {
		ListBuilder b = inflater.new ListBuilder(layout, R.layout.select_entry_plus);
		View v = b.withButtonId(plusId, listener)
				.withId(id, listener)
				.withLabel(labelId)
				.withData(defaultValueResId)
				.create();
		
		TextView textView = v.findViewById(R.id.data);
		textView.setTag(v);
		return textView;
	}
	
	public Pair<TextView, AutoCompleteTextView> addListNodeWithClearButtonAndFilter(LinearLayout layout, int id, int clearBtnId, int labelId, int defaultValueResId, int filterToggleId) {
		return addListNodeWithButtonsAndFilter(layout, R.layout.select_entry_with_2btn_and_filter, id, -1, clearBtnId, labelId, defaultValueResId, filterToggleId);
	}

	public Pair<TextView, AutoCompleteTextView> addListNodeWithButtonsAndFilter(LinearLayout layout, int id, int actBtnId, int clearBtnId, int labelId, int defaultValueResId, int filterToggleId) {
		return addListNodeWithButtonsAndFilter(layout, R.layout.select_entry_with_2btn_and_filter, id, actBtnId, clearBtnId, labelId, defaultValueResId, filterToggleId);
	}

	public Pair<TextView, AutoCompleteTextView> addListNodeWithButtonsAndFilter(LinearLayout layout, int nodeLayoutId, int id, int actBtnId, int clearBtnId, int labelId, int defaultValueResId, int filterToggleId) {
		ListBuilder b = inflater.new ListBuilder(layout, nodeLayoutId);
		final View v = b.withButtonId(actBtnId, listener)
				.withClearButtonId(clearBtnId, listener)
				.withAutoCompleteFilter(listener, filterToggleId)
				.withId(id, listener)
				.withLabel(labelId)
				.withData(defaultValueResId)
				.create();
		
		if (actBtnId > 0) {
			showButton(v, actBtnId);
		}

		AutoCompleteTextView filterTxt = v.findViewById(R.id.autocomplete_filter);
		ToggleButton toggleBtn = v.findViewById(filterToggleId);
		filterTxt.setTag(toggleBtn);


        TextView textView = v.findViewById(R.id.data);
		textView.setTag(R.id.bMinus, v.findViewById(clearBtnId));
		textView.setTag(v);
		return Pair.create(textView, filterTxt);
	}

	public Pair<TextView, AutoCompleteTextView> addListNodeCategory(LinearLayout layout, int filterToggleId) {
		ListBuilder b = inflater.new ListBuilder(layout, R.layout.select_entry_category);
		View v = b.withButtonId(R.id.category_add, listener)
				.withClearButtonId(R.id.category_clear, listener)
				.withAutoCompleteFilter(listener, filterToggleId)
				.withId(R.id.category, listener).withLabel(R.string.category).withData(R.string.select_category)
				.create();
		
		ImageView transferImageView = v.findViewById(R.id.split);
		transferImageView.setId(R.id.category_split);
		transferImageView.setOnClickListener(listener);
		
		ToggleButton toggleBtn = v.findViewById(filterToggleId);
		AutoCompleteTextView filterTxt = v.findViewById(R.id.autocomplete_filter);
		filterTxt.setTag(toggleBtn);
		TextView entityNameTxt = v.findViewById(R.id.data);
		entityNameTxt.setTag(R.id.bMinus, v.findViewById(R.id.category_clear));
		return Pair.create(entityNameTxt, filterTxt);
	}

	public View addNodeUnsplit(LinearLayout layout) {
		ListBuilder b = inflater.new ListBuilder(layout, R.layout.select_entry_unsplit);
		View v = b.withButtonId(R.id.add_split, listener).withId(R.id.unsplit_action, listener).withLabel(R.string.unsplit_amount).withData("0").create();
		ImageView transferImageView = v.findViewById(R.id.add_split_transfer);
		transferImageView.setOnClickListener(listener);
		return v;
	}

	public View addSplitNodeMinus(LinearLayout layout, int id, int minusId, int labelId, String defaultValue) {
		ListBuilder b = inflater.new ListBuilder(layout, R.layout.select_entry_minus);
		return b.withButtonId(minusId, listener).withoutMoreButton().withId(id, listener).withLabel(labelId).withData(defaultValue).create();
	}

	public TextView addFilterNodeMinus(LinearLayout layout, int id, int minusId, int labelId, int defaultValueResId) {
		return addFilterNodeMinus(layout, id, minusId, labelId, defaultValueResId, null);
	}

	public TextView addFilterNodeMinus(LinearLayout layout, int id, int minusId, int labelId, int defaultValueResId, String defaultValue) {
		Builder b = inflater.new ListBuilder(layout, R.layout.select_entry_minus).withButtonId(minusId, listener).withId(id, listener).withLabel(labelId);
		if (defaultValue != null) {
			b.withData(defaultValue);
		} else {
			b.withData(defaultValueResId);
		}
		View v = b.create();
		ImageView clearBtn = hideButton(v, minusId);
		TextView text = v.findViewById(R.id.data);
		text.setTag(R.id.bMinus, clearBtn); // needed for dynamic toggling in any activity with filters
		return text;
	}

	private ImageView hideButton(View v, int btnId) {
		ImageView plusImageView = v.findViewById(btnId);
		plusImageView.setVisibility(View.GONE);
		return plusImageView;
	}

	private void showButton(View v, int btnId) {
		ImageView plusImageView = v.findViewById(btnId);
		plusImageView.setVisibility(View.VISIBLE);
	}

	public ImageView addPictureNodeMinus(Context context, LinearLayout layout, int id, int minusId, int labelId, int defaultLabelResId) {
		PictureBuilder b = inflater.new PictureBuilder(layout);
		View v = b.withPicture(context, null).withButtonId(minusId, listener).withId(id, listener)
				.withLabel(labelId).withData(defaultLabelResId).create();
		return (ImageView)v.findViewById(R.id.picture);
	}

	public View addEditNode(LinearLayout layout, int labelId, View view) {
		EditBuilder b = inflater.new EditBuilder(layout, view);
		return b.withLabel(labelId).create();
	}

	private void selectSingleChoice(Context context, int titleId, ListAdapter adapter, int checkedItem,
									DialogInterface.OnClickListener onClickListener) {
		new AlertDialog.Builder(context)
				.setSingleChoiceItems(adapter, checkedItem, onClickListener)
				.setTitle(titleId)
				.show();
	}

	public void selectMultiChoice(Context context, final int id, int titleId, final List<? extends MultiChoiceItem> items) {
		int count = items.size();
		String[] titles = new String[count];
		boolean[] checked = new boolean[count];
		for (int i=0; i<count; i++) {
			titles[i] = items.get(i).getTitle();
			checked[i] = items.get(i).isChecked();
		}
		new AlertDialog.Builder(context)
				.setMultiChoiceItems(titles, checked, (dialog, which, isChecked) -> items.get(which).setChecked(isChecked))
				.setPositiveButton(R.string.ok, (dialog, which) -> listener.onSelected(id, items))
				.setNegativeButton(R.string.cancel, (dialog, which) -> {

                })
				.setTitle(titleId)
				.show();
	}

	public void selectPosition(Context context, final int id, int titleId,
							   final ListAdapter adapter, int selectedPosition) {
		selectSingleChoice(context, titleId, adapter, selectedPosition,
				(dialog, which) -> {
                    dialog.cancel();
                    listener.onSelectedPos(id, which);
                });
	}

	public void selectItemId(Context context, final int id, int titleId,
							 final ListAdapter adapter, int selectedPosition) {
		selectSingleChoice(context, titleId, adapter, selectedPosition,
				(dialog, which) -> {
                    dialog.cancel();
                    long selectedId = adapter.getItemId(which);
                    listener.onSelectedId(id, selectedId);
                });
	}

	public void select(Context context, final int id, int titleId,
					   final Cursor cursor, final ListAdapter adapter,
					   final String idColumn, long valueId) {
		int pos = Utils.moveCursor(cursor, idColumn, valueId);
		selectSingleChoice(context, titleId, adapter, pos,
				(dialog, which) -> {
                    dialog.cancel();
                    cursor.moveToPosition(which);
                    long selectedId = cursor.getLong(cursor.getColumnIndexOrThrow(idColumn));
                    listener.onSelectedId(id, selectedId);
                });
	}

	public void addDivider(LinearLayout layout) {
		inflater.addDivider(layout);
	}

	public View addRateNode(LinearLayout layout) {
		return inflater.new Builder(layout, R.layout.select_entry_rate)
				.withLabel(R.string.rate)
				.withData(R.string.no_rate)
				.create();
	}

}
