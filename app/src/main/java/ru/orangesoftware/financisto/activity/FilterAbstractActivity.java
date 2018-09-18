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

import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.blotter.BlotterFilter;
import ru.orangesoftware.financisto.filter.Criteria;
import ru.orangesoftware.financisto.filter.WhereFilter;
import ru.orangesoftware.financisto.model.*;
import ru.orangesoftware.financisto.utils.ArrUtils;

import java.util.LinkedList;
import java.util.List;

import static ru.orangesoftware.financisto.activity.CategorySelector.SelectorType.FILTER;
import static ru.orangesoftware.financisto.blotter.BlotterFilter.*;
import static ru.orangesoftware.financisto.filter.WhereFilter.Operation.BTW;
import static ru.orangesoftware.financisto.filter.WhereFilter.Operation.IN;

public abstract class FilterAbstractActivity extends AbstractActivity implements CategorySelector.CategorySelectorListener {

	protected WhereFilter filter = WhereFilter.empty();

	protected ProjectSelector<FilterAbstractActivity> projectSelector;
	protected PayeeSelector<FilterAbstractActivity> payeeSelector;
	protected CategorySelector<FilterAbstractActivity> categorySelector;

	protected TextView project;
	protected TextView payee;
	protected TextView categoryTxt;

	protected String noFilterValue;

	protected void initPayeeSelector(LinearLayout layout) {
		payeeSelector = new PayeeSelector<>(this, db, x, 0, R.id.payee_clear, R.string.no_filter);
		payeeSelector.initMultiSelect();
		payee = payeeSelector.createNode(layout);
	}

	protected void initProjectSelector(LinearLayout layout) {
		projectSelector = new ProjectSelector<>(this, db, x, 0, R.id.project_clear, R.string.no_filter);
		projectSelector.initMultiSelect();
		project = projectSelector.createNode(layout);
	}

	protected void initCategorySelector(LinearLayout layout) {
		categorySelector = new CategorySelector<>(this, db, x);
		categorySelector.setListener(this);
		categorySelector.initMultiSelect();
		categoryTxt = categorySelector.createNode(layout, FILTER);
	}

	protected void clear(String criteria, TextView textView) {
		filter.remove(criteria);
		textView.setText(R.string.no_filter);
		hideMinusButton(textView);
	}

	protected void clearCategory() {
		clear(CATEGORY_LEFT, categoryTxt);
	}

	@Override
	protected void onClick(View v, int id) {
		switch (id) {
			case R.id.category_filter_toggle:
			case R.id.category: {
				categorySelector.onClick(id);
			} break;
			case R.id.category_clear:
				categorySelector.onClick(id);
				clearCategory();
				break;
			case R.id.project: {
				Criteria c = filter.get(PROJECT_ID);
				if (c != null) projectSelector.updateCheckedEntities(c.getValues());
				projectSelector.onClick(id);
			} break;
			case R.id.project_clear:
				clear(PROJECT_ID, project);
				projectSelector.onClick(id);
				break;
			case R.id.project_filter_toggle:
				projectSelector.onClick(id);
				break;
			case R.id.payee: {
				Criteria c = filter.get(BlotterFilter.PAYEE_ID);
				if (c != null) projectSelector.updateCheckedEntities(c.getValues());
				payeeSelector.onClick(id);
			} break;
			case R.id.payee_clear:
				clear(BlotterFilter.PAYEE_ID, payee);
				payeeSelector.onClick(id);
				break;
			case R.id.payee_filter_toggle:
				payeeSelector.onClick(id);
				break;
		}
	}

	@Override
	public void onSelectedId(final int id, final long selectedId) {
		switch (id) {
			case R.id.project:
				projectSelector.onSelectedId(id, selectedId);
				filter.put(Criteria.in(PROJECT_ID, projectSelector.getCheckedIds()));
				updateProjectFromFilter();
				break;
			case R.id.payee:
				payeeSelector.onSelectedId(id, selectedId);
				if (selectedId == 0) {
					filter.put(Criteria.isNull(BlotterFilter.PAYEE_ID));
				} else {
					filter.put(Criteria.in(BlotterFilter.PAYEE_ID, payeeSelector.getCheckedIds()));
				}
				updatePayeeFromFilter();
				break;
			case R.id.category:
				categorySelector.onSelectedId(id, selectedId, false);
//                filter.put(Criteria.btw(CATEGORY_LEFT, categorySelector.getCheckedCategoryLeafs()));
//                updateCategoryFromFilter();
				break;
		}
	}

	@Override
	public void onSelected(int id, List<? extends MultiChoiceItem> items) {
		switch (id) {
			case R.id.category:
				if (ArrUtils.isEmpty(categorySelector.getCheckedCategoryLeafs())) {
					clearCategory();
				} else {
					filter.put(Criteria.btw(CATEGORY_LEFT, categorySelector.getCheckedCategoryLeafs()));
					updateCategoryFromFilter();
				}
				break;
			case R.id.project:
				if (ArrUtils.isEmpty(projectSelector.getCheckedIds())) {
					clear(PROJECT_ID, project);
				} else {
					filter.put(Criteria.in(PROJECT_ID, projectSelector.getCheckedIds()));
					updateProjectFromFilter();
				}
				break;
			case R.id.payee:
				if (ArrUtils.isEmpty(payeeSelector.getCheckedIds())) {
					clear(PAYEE_ID, payee);
				} else {
					filter.put(Criteria.in(PAYEE_ID, payeeSelector.getCheckedIds()));
					updatePayeeFromFilter();
				}
				break;
		}
	}

	@Override
	public void onSelectedPos(int id, int selectedPos) { // todo.mb: not used in case of multi-select, so remove then
		switch (id) {
			case R.id.project:
				projectSelector.onSelectedPos(id, selectedPos);
				filter.put(Criteria.eq(PROJECT_ID, String.valueOf(projectSelector.getSelectedEntityId())));
				updateProjectFromFilter();
				break;
			case R.id.payee:
				payeeSelector.onSelectedPos(id, selectedPos);
				filter.put(Criteria.eq(PAYEE_ID, String.valueOf(payeeSelector.getSelectedEntityId())));
				updatePayeeFromFilter();
				break;
		}
	}

	protected void updateCategoryFromFilter() {
		Criteria c = filter.get(CATEGORY_LEFT);
		if (c != null) {
			if (c.operation != BTW) throw new UnsupportedOperationException(c.operation.name());
			
			List<String> checkedLeftIds = getLeftCategoryNodesFromFilter(c);
			List<Long> catIds = db.getCategoryIdsByLeftIds(checkedLeftIds);
			categorySelector.updateCheckedEntities(catIds);
			categorySelector.fillCategoryInUI();
		}
	}

	private List<String> getLeftCategoryNodesFromFilter(Criteria catCriteria) {
		List<String> res = new LinkedList<>();
		for (int i = 0; i < catCriteria.getValues().length; i += 2) {
			res.add(catCriteria.getValues()[i]);
		}
		return res;
	}

	protected <T extends MyEntity> void updateEntityFromFilter(String filterCriteriaName, Class<T> entityClass, TextView filterView) {
		Criteria c = filter.get(filterCriteriaName);
		if (c != null && !c.isNull()) {
			String filterText = noFilterValue;
			if (c.operation == IN) {
				filterText = getSelectedTitles(c, filterCriteriaName);
			} else {
				long entityId = c.getLongValue1();
				T e = db.get(entityClass, entityId);
				if (e != null) filterText = e.title;
			}
			if (!TextUtils.isEmpty(filterText)) {
				filterView.setText(filterText);
				showMinusButton(filterView);
			}
		} else {
			filterView.setText(R.string.no_filter);
			hideMinusButton(filterView);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case R.id.category_pick:
			case R.id.category_add:
				categorySelector.onActivityResult(requestCode, resultCode, data);
				break;
		}
	}

	@Override
	public void onCategorySelected(Category cat, boolean selectLast) {
		clearCategory();
		if (categorySelector.isMultiSelect()) {
			filter.put(Criteria.btw(CATEGORY_LEFT, categorySelector.getCheckedCategoryLeafs()));
		} else {
			if (cat.id > 0) {
				filter.put(Criteria.btw(CATEGORY_LEFT, String.valueOf(cat.left), String.valueOf(cat.right)));
			} else {
				clearCategory();
			}
		}
		updateCategoryFromFilter();
	}

	protected void updateProjectFromFilter() {
		updateEntityFromFilter(PROJECT_ID, Project.class, project);
	}

	protected void updatePayeeFromFilter() {
		updateEntityFromFilter(BlotterFilter.PAYEE_ID, Payee.class, payee);
	}

	protected String getSelectedTitles(Criteria c, String filterCriteriaName) {
		if (filterCriteriaName.equals(PROJECT_ID)) {
			projectSelector.updateCheckedEntities(c.getValues());
			return projectSelector.getCheckedTitles();
		} else if (filterCriteriaName.equals(PAYEE_ID)) {
			payeeSelector.updateCheckedEntities(c.getValues());
			return payeeSelector.getCheckedTitles();
		}
		throw new UnsupportedOperationException(filterCriteriaName + ": titles not implemented");
	}

	protected void showMinusButton(TextView textView) {
		ImageView v = findMinusButton(textView);
		v.setVisibility(View.VISIBLE);
	}

	protected void hideMinusButton(TextView textView) {
		ImageView v = findMinusButton(textView);
		v.setVisibility(View.GONE);
	}

	protected ImageView findMinusButton(TextView textView) {
		return (ImageView) textView.getTag(R.id.bMinus);
	}

	@Override
	protected void onDestroy() {
		if (payeeSelector != null) payeeSelector.onDestroy();
		if (projectSelector != null) projectSelector.onDestroy();
		if (categorySelector != null) categorySelector.onDestroy();
		super.onDestroy();
	}
}
