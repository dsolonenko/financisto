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
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.blotter.BlotterFilter;
import ru.orangesoftware.financisto.datetime.DateUtils;
import ru.orangesoftware.financisto.datetime.Period;
import ru.orangesoftware.financisto.filter.Criteria;
import ru.orangesoftware.financisto.filter.DateTimeCriteria;
import ru.orangesoftware.financisto.filter.SingleCategoryCriteria;
import ru.orangesoftware.financisto.filter.WhereFilter;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.MultiChoiceItem;
import ru.orangesoftware.financisto.model.MyEntity;
import ru.orangesoftware.financisto.model.MyLocation;
import ru.orangesoftware.financisto.model.Payee;
import ru.orangesoftware.financisto.model.Project;
import ru.orangesoftware.financisto.model.TransactionStatus;
import ru.orangesoftware.financisto.utils.ArrUtils;
import ru.orangesoftware.financisto.utils.EnumUtils;
import ru.orangesoftware.financisto.utils.TransactionUtils;

import static ru.orangesoftware.financisto.activity.CategorySelector.SelectorType.FILTER;
import static ru.orangesoftware.financisto.blotter.BlotterFilter.CATEGORY_ID;
import static ru.orangesoftware.financisto.blotter.BlotterFilter.CATEGORY_LEFT;
import static ru.orangesoftware.financisto.blotter.BlotterFilter.FROM_ACCOUNT_ID;
import static ru.orangesoftware.financisto.blotter.BlotterFilter.PAYEE_ID;
import static ru.orangesoftware.financisto.blotter.BlotterFilter.PROJECT_ID;
import static ru.orangesoftware.financisto.filter.WhereFilter.Operation.BTW;
import static ru.orangesoftware.financisto.filter.WhereFilter.Operation.IN;
import static ru.orangesoftware.financisto.utils.ArrUtils.asArr;

// todo.mb: 1) add entity selector for location 2) multiselect for category
public class BlotterFilterActivity extends FilterAbstractActivity implements CategorySelector.CategorySelectorListener {
	
    public static final String IS_ACCOUNT_FILTER = "IS_ACCOUNT_FILTER";
	private static final TransactionStatus[] statuses = TransactionStatus.values();

	private static final int REQUEST_DATE_FILTER = 1;
	private static final int REQUEST_NOTE_FILTER = 2;

	private WhereFilter filter = WhereFilter.empty();
	
	private TextView period;
	private TextView account;
	private TextView currency;
	private TextView categoryTxt;
	private TextView project;
    private TextView payee;
	private TextView note;
	private TextView location;
	private TextView sortOrder;
	private TextView status;
	
	private DateFormat df;
	private String[] sortBlotterEntries;

    private String filterValueNotFound;
    private long accountId;
    private boolean isAccountFilter;
	private CategorySelector<BlotterFilterActivity> categorySelector;
	private ProjectSelector<BlotterFilterActivity> projectSelector;
	private PayeeSelector<BlotterFilterActivity> payeeSelector;
	private Category category = null;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.blotter_filter);
		
		df = DateUtils.getShortDateFormat(this);
		sortBlotterEntries = getResources().getStringArray(R.array.sort_blotter_entries);
        filterValueNotFound = getString(R.string.filter_value_not_found);
		
        projectSelector = new ProjectSelector<>(this, db, x, 0, R.id.project_clear, R.string.no_filter);
        projectSelector.setMultiSelect(true);
		projectSelector.fetchEntities();

		payeeSelector = new PayeeSelector<>(this, db, x, 0, R.id.payee_clear, R.string.no_filter);
		payeeSelector.setMultiSelect(true);
		payeeSelector.fetchEntities();
        
		initCategorySelector();
        
		LinearLayout layout = findViewById(R.id.layout);
		period = x.addFilterNodeMinus(layout, R.id.period, R.id.period_clear, R.string.period, R.string.no_filter);
		account = x.addFilterNodeMinus(layout, R.id.account, R.id.account_clear, R.string.account, R.string.no_filter);
		currency = x.addFilterNodeMinus(layout, R.id.currency, R.id.currency_clear, R.string.currency, R.string.no_filter);
		categoryTxt = categorySelector.createNode(layout, FILTER);
        payee = payeeSelector.createNode(layout);//x.addFilterNodeMinus(layout, R.id.payee, R.id.payee_clear, R.string.payee, R.string.no_filter);
		project = projectSelector.createNode(layout);
		note = x.addFilterNodeMinus(layout, R.id.note, R.id.note_clear, R.string.note, R.string.no_filter);
		location = x.addFilterNodeMinus(layout, R.id.location, R.id.location_clear, R.string.location, R.string.no_filter);
		status = x.addFilterNodeMinus(layout, R.id.status, R.id.status_clear, R.string.transaction_status, R.string.no_filter);
		sortOrder = x.addFilterNodeMinus(layout, R.id.sort_order, R.id.sort_order_clear, R.string.sort_order, 0, sortBlotterEntries[0]);

		Button bOk = findViewById(R.id.bOK);
		bOk.setOnClickListener(v -> {
            Intent data = new Intent();
            filter.toIntent(data);
            setResult(RESULT_OK, data);
            finish();
        });
		
		Button bCancel = findViewById(R.id.bCancel);
		bCancel.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });
		
		ImageButton bNoFilter = findViewById(R.id.bNoFilter);
		bNoFilter.setOnClickListener(v -> {
			if (isAccountFilter()) {
				Intent data = new Intent();
				Criteria.eq(FROM_ACCOUNT_ID, String.valueOf(accountId)).toIntent(filter.getTitle(), data);
				setResult(RESULT_OK, data);
				finish();
			} else {
				setResult(RESULT_FIRST_USER);
				finish();
			}
		});
		
		Intent intent = getIntent();
		if (intent != null) {
			filter = WhereFilter.fromIntent(intent);
            getAccountIdFromFilter(intent);
            updatePeriodFromFilter();
			updateAccountFromFilter();
			updateCurrencyFromFilter();
			updateCategoryFromFilter();
			updateProjectFromFilter();
            updatePayeeFromFilter();
			updateNoteFromFilter();
			updateLocationFromFilter();
			updateSortOrderFromFilter();
			updateStatusFromFilter();
            disableAccountResetButtonIfNeeded();
		}
	}

	private void initCategorySelector() {
		categorySelector = new CategorySelector<>(this, db, x);
		categorySelector.setListener(this);
//		categorySelector.initMultiSelect();
		categorySelector.fetchCategories(false);
		categorySelector.doNotShowSplitCategory();
	}

    private boolean isAccountFilter() {
        return isAccountFilter && accountId > 0;
    }

    private void getAccountIdFromFilter(Intent intent) {
        isAccountFilter = intent.getBooleanExtra(IS_ACCOUNT_FILTER, false);
        accountId = filter.getAccountId();
    }

    private void disableAccountResetButtonIfNeeded() {
        if (isAccountFilter()) {
            hideMinusButton(account);
        }
    }

    private void updateSortOrderFromFilter() {
		String s = filter.getSortOrder();
		if (BlotterFilter.SORT_OLDER_TO_NEWER.equals(s)) {
			sortOrder.setText(sortBlotterEntries[1]);
		} else {
			sortOrder.setText(sortBlotterEntries[0]);
		}
	}

	private void updateLocationFromFilter() {
		Criteria c = filter.get(BlotterFilter.LOCATION_ID);
		if (c != null) {
			MyLocation loc = db.get(MyLocation.class, c.getLongValue1());
			location.setText(loc != null ? loc.name : filterValueNotFound);
            showMinusButton(location);
		} else {
			location.setText(R.string.no_filter);
            hideMinusButton(location);
		}
	}

	private void updateProjectFromFilter() {
        updateEntityFromFilter(PROJECT_ID, Project.class, project);
	}

    private void updatePayeeFromFilter() {
        updateEntityFromFilter(BlotterFilter.PAYEE_ID, Payee.class, payee);
    }
    
    private List<String> getLeftCategoryNodesFromFilter(Criteria catCriteria) {
        List<String> res = new LinkedList<>();
        for (int i = 0; i < catCriteria.getValues().length; i += 2) {
            res.add(catCriteria.getValues()[i]);
        }
        return res;
    }
    
    
	private void updateCategoryFromFilter() {
		Criteria c = filter.get(CATEGORY_LEFT);
		if (c != null && c.operation == BTW) {
			categoryTxt.setText(filterValueNotFound);
            String categoryUiTxt = null;
		    if (c.getValues().length > 2) { // multiple selected including sub-categories
                List<String> checkedLeftIds = getLeftCategoryNodesFromFilter(c);
                List<Long> catIds = db.getCategoryIdsByLeftIds(checkedLeftIds);
                categorySelector.updateCheckedEntities(catIds);
                categoryUiTxt = categorySelector.getCheckedTitles();
            } else { // single selection
                category = db.getCategoryByLeft(c.getLongValue1());
                if (category.id > 0) {
                    categoryUiTxt = Category.getTitle(category.title, category.level);
					categorySelector.updateCheckedEntities(asArr(category.id + ""));
                }
		    }
		    if (!TextUtils.isEmpty(categoryUiTxt)) {
				categoryTxt.setText(categoryUiTxt);
				showMinusButton(categoryTxt);
			}
		} else {
            c = filter.get(CATEGORY_ID);
            if (c != null) {
                String categoryUiTxt = null;
                if (c.operation == IN) { // multiple choice without subcategories
                    categorySelector.updateCheckedEntities(c.getValues());
                    categoryUiTxt = categorySelector.getCheckedTitles();
                } else {
                    long categoryId = c.getLongValue1();
                    category = db.getCategoryWithParent(categoryId);
                    categorySelector.updateCheckedEntities(asArr(category.id + ""));
                    categoryUiTxt = category.title;
                }
                if (!TextUtils.isEmpty(categoryUiTxt)) {
                    categoryTxt.setText(categoryUiTxt);
                    showMinusButton(categoryTxt);
                }
            } else {
			    categoryTxt.setText(R.string.no_filter);
                hideMinusButton(categoryTxt);
            }
		}
		// todo.mb: fix here category filling
	}

	private void updatePeriodFromFilter() {
		DateTimeCriteria c = (DateTimeCriteria)filter.get(BlotterFilter.DATETIME);
		if (c != null) {
			Period p = c.getPeriod();
			if (p.isCustom()) {
				long periodFrom = c.getLongValue1();
				long periodTo = c.getLongValue2();
				period.setText(df.format(new Date(periodFrom))+"-"+df.format(new Date(periodTo)));
			} else {
				period.setText(p.type.titleId);
			}
            showMinusButton(period);
		} else {
            clear(BlotterFilter.DATETIME, period);
		}
	}

	private void updateAccountFromFilter() {
        updateEntityFromFilter(FROM_ACCOUNT_ID, Account.class, account);
	}

	private void updateCurrencyFromFilter() {
        updateEntityFromFilter(BlotterFilter.FROM_ACCOUNT_CURRENCY_ID, Currency.class, currency);
	}

	private void updateNoteFromFilter() {
		Criteria c = filter.get(BlotterFilter.NOTE);
		if (c != null) {
			String v = c.getStringValue();
			note.setText(String.format(getString(R.string.note_text_containing_value),
					v.substring(1, v.length() - 1).replace("%", " ")));
			showMinusButton(note);
		} else {
			note.setText(R.string.no_filter);
			hideMinusButton(note);
		}
	}

	private void updateStatusFromFilter() {
		Criteria c = filter.get(BlotterFilter.STATUS);
		if (c != null) {
			TransactionStatus s = TransactionStatus.valueOf(c.getStringValue());
			status.setText(getString(s.titleId));
            showMinusButton(status);
		} else {
			status.setText(R.string.no_filter);
            hideMinusButton(status);
		}
	}

    private <T extends MyEntity> void updateEntityFromFilter(String filterCriteriaName, Class<T> entityClass, TextView filterView) {
        Criteria c = filter.get(filterCriteriaName);
        if (c != null && !c.isNull()) {
        	String filterText = filterValueNotFound;
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

	private String getSelectedTitles(Criteria c, String filterCriteriaName) {
    	if (filterCriteriaName.equals(PROJECT_ID)) {
    		projectSelector.updateCheckedEntities(c.getValues());
    		return projectSelector.getCheckedTitles();
		} else if (filterCriteriaName.equals(PAYEE_ID)) {
			payeeSelector.updateCheckedEntities(c.getValues());
			return payeeSelector.getCheckedTitles();
		}
		throw new UnsupportedOperationException(filterCriteriaName + ": titles not implemented");
	}


	@Override
	protected void onClick(View v, int id) {
		Intent intent;
		switch (id) {
			case R.id.period:
				intent = new Intent(this, DateFilterActivity.class);
				filter.toIntent(intent);
				startActivityForResult(intent, REQUEST_DATE_FILTER);
				break;
			case R.id.period_clear:
				clear(BlotterFilter.DATETIME, period);
				break;
			case R.id.account: {
				if (isAccountFilter()) {
					return;
				}
				Cursor cursor = db.getAllAccounts();
				startManagingCursor(cursor);
				ListAdapter adapter = TransactionUtils.createAccountAdapter(this, cursor);
				Criteria c = filter.get(FROM_ACCOUNT_ID);
				long selectedId = c != null ? c.getLongValue1() : -1;
				x.select(this, R.id.account, R.string.account, cursor, adapter, "_id", selectedId);
			} break;
			case R.id.account_clear:
				if (isAccountFilter()) {
					return;
				}
				clear(FROM_ACCOUNT_ID, account);
				break;
			case R.id.currency: {
				Cursor cursor = db.getAllCurrencies("name");
				startManagingCursor(cursor);
				ListAdapter adapter = TransactionUtils.createCurrencyAdapter(this, cursor);
				Criteria c = filter.get(BlotterFilter.FROM_ACCOUNT_CURRENCY_ID);
				long selectedId = c != null ? c.getLongValue1() : -1;
				x.select(this, R.id.currency, R.string.currency, cursor, adapter, "_id", selectedId);
			} break;
			case R.id.currency_clear:
				clear(BlotterFilter.FROM_ACCOUNT_CURRENCY_ID, currency);
				break;
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
				if (projectSelector.isMultiSelect()) {
					if (c != null) projectSelector.updateCheckedEntities(c.getValues());
				} else {
					// todo.mb: single project mode is not used in filters now, can be removed then (+for payees too):
					long selectedId = c != null ? c.getLongValue1() : -1;
					projectSelector.selectEntity(selectedId);
				}
				projectSelector.onClick(id);
			} break;
			case R.id.project_clear:
				clear(PROJECT_ID, project);
				projectSelector.onClick(id);
				break;
			case R.id.payee: {
				Criteria c = filter.get(BlotterFilter.PAYEE_ID);
				if (projectSelector.isMultiSelect()) {
					if (c != null) projectSelector.updateCheckedEntities(c.getValues());
				} else {
					long selectedId = c != null ? c.getLongValue1() : -1;
					payeeSelector.selectEntity(selectedId);
				}
				payeeSelector.onClick(id);
			} break;
			case R.id.payee_clear:
				clear(BlotterFilter.PAYEE_ID, payee);
				payeeSelector.onClick(id);
				break;
			case R.id.note:
				intent = new Intent(this, NoteFilterActivity.class);
				filter.toIntent(intent);
				startActivityForResult(intent, REQUEST_NOTE_FILTER);
				break;
			case R.id.note_clear:
				clear(BlotterFilter.NOTE, note);
				break;
			case R.id.location: {
				Cursor cursor = db.getAllLocations(true);
				startManagingCursor(cursor);
				ListAdapter adapter = TransactionUtils.createLocationAdapter(this, cursor);
				Criteria c = filter.get(BlotterFilter.LOCATION_ID);
				long selectedId = c != null ? c.getLongValue1() : -1;
				x.select(this, R.id.location, R.string.location, cursor, adapter, "_id", selectedId);
			} break;
			case R.id.location_clear:
				clear(BlotterFilter.LOCATION_ID, location);
				break;
			case R.id.sort_order: {
				ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, sortBlotterEntries);
				int selectedId = BlotterFilter.SORT_OLDER_TO_NEWER.equals(filter.getSortOrder()) ? 1 : 0;
				x.selectPosition(this, R.id.sort_order, R.string.sort_order, adapter, selectedId);
			} break;
			case R.id.sort_order_clear:
				filter.resetSort();
				filter.desc(BlotterFilter.DATETIME);
				updateSortOrderFromFilter();
				break;
			case R.id.status: {
				ArrayAdapter<String> adapter = EnumUtils.createDropDownAdapter(this, statuses);
				Criteria c = filter.get(BlotterFilter.STATUS);
				int selectedPos = c != null ? TransactionStatus.valueOf(c.getStringValue()).ordinal() : -1;
				x.selectPosition(this, R.id.status, R.string.transaction_status, adapter, selectedPos);
			} break;
			case R.id.status_clear:
				clear(BlotterFilter.STATUS, status);
				break;
			case R.id.project_filter_toggle:
				projectSelector.onClick(id);
				break;
			case R.id.payee_filter_toggle:
				payeeSelector.onClick(id);
				break;
		}
	}

    private void clearCategory() {
        clear(CATEGORY_LEFT, categoryTxt);
        clear(CATEGORY_ID, categoryTxt);
    }

    private Payee noPayee() {
        Payee p = new Payee();
        p.id = 0;
        p.title = getString(R.string.no_payee);
        return p;
    }

    private void clear(String criteria, TextView textView) {
		filter.remove(criteria);
		textView.setText(R.string.no_filter);
        hideMinusButton(textView);
	}

	@Override
	public void onSelectedId(final int id, final long selectedId) {
		switch (id) {
			case R.id.account:
				filter.put(Criteria.eq(FROM_ACCOUNT_ID, String.valueOf(selectedId)));
				updateAccountFromFilter();
				break;
			case R.id.currency:
				filter.put(Criteria.eq(BlotterFilter.FROM_ACCOUNT_CURRENCY_ID, String.valueOf(selectedId)));
				updateCurrencyFromFilter();
				break;
			case R.id.category:
				categorySelector.onSelectedId(id, selectedId, false);
//                filter.put(Criteria.btw(CATEGORY_LEFT, categorySelector.getCheckedCategoryLeafs()));
//                updateCategoryFromFilter();
				break;
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
			case R.id.location:
				filter.put(Criteria.eq(BlotterFilter.LOCATION_ID, String.valueOf(selectedId)));
				updateLocationFromFilter();
				break;
		}
	}
	
	@Override
	public void onSelectedPos(int id, int selectedPos) {
		switch (id) {
			case R.id.status:
				filter.put(Criteria.eq(BlotterFilter.STATUS, statuses[selectedPos].name()));
				updateStatusFromFilter();			
				break;
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
			case R.id.sort_order:
				filter.resetSort();
				if (selectedPos == 1) {
					filter.asc(BlotterFilter.DATETIME);
				} else {
					filter.desc(BlotterFilter.DATETIME);
				}
				updateSortOrderFromFilter();
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
					filter.put(Criteria.in(CATEGORY_ID, categorySelector.getCheckedCategoryIds()));
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
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case REQUEST_DATE_FILTER:
				if (resultCode == RESULT_FIRST_USER) {
					onClick(period, R.id.period_clear);
				} else if (resultCode == RESULT_OK) {
					DateTimeCriteria c = WhereFilter.dateTimeFromIntent(data);
					filter.put(c);
					updatePeriodFromFilter();
				}
				break;

			case REQUEST_NOTE_FILTER:
				if (resultCode == RESULT_FIRST_USER) {
					onClick(note, R.id.note_clear);
				} else if (resultCode == RESULT_OK) {
					filter.put(new Criteria(BlotterFilter.NOTE, WhereFilter.Operation.LIKE,
							data.getStringExtra(NoteFilterActivity.NOTE_CONTAINING)));
					updateNoteFromFilter();
				}
				break;
			case R.id.category_pick:
				categorySelector.onActivityResult(requestCode, resultCode, data);
				break;
		}
	}

	@Override
	public void onCategorySelected(Category cat, boolean selectLast) {
		clearCategory();
		if (categorySelector.isMultiSelect()) {
			if (categorySelector.isHierarchicalSelector()) {
				filter.put(Criteria.btw(CATEGORY_LEFT, categorySelector.getCheckedCategoryLeafs()));
			} else {
				filter.put(Criteria.in(CATEGORY_ID, categorySelector.getCheckedCategoryIds()));
			}
		} else {
			if (cat.id > 0) {
				filter.put(Criteria.btw(CATEGORY_LEFT, String.valueOf(cat.left), String.valueOf(cat.right)));
			} else {
				filter.put(new SingleCategoryCriteria(0));
			}
		}
		updateCategoryFromFilter();
	}
}