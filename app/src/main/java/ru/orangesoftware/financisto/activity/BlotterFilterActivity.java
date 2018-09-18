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
import android.view.View;
import android.view.Window;
import android.widget.*;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.blotter.BlotterFilter;
import ru.orangesoftware.financisto.datetime.DateUtils;
import ru.orangesoftware.financisto.datetime.Period;
import ru.orangesoftware.financisto.filter.Criteria;
import ru.orangesoftware.financisto.filter.DateTimeCriteria;
import ru.orangesoftware.financisto.filter.WhereFilter;
import ru.orangesoftware.financisto.model.*;
import ru.orangesoftware.financisto.utils.EnumUtils;
import ru.orangesoftware.financisto.utils.TransactionUtils;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import static ru.orangesoftware.financisto.blotter.BlotterFilter.FROM_ACCOUNT_ID;

public class BlotterFilterActivity extends FilterAbstractActivity {
	
    public static final String IS_ACCOUNT_FILTER = "IS_ACCOUNT_FILTER";
	private static final TransactionStatus[] statuses = TransactionStatus.values();

	private static final int REQUEST_DATE_FILTER = 1;
	private static final int REQUEST_NOTE_FILTER = 2;

	private TextView period;
	private TextView account;
	private TextView currency;
	private TextView note;
	private TextView location;
	private TextView sortOrder;
	private TextView status;
	
	private DateFormat df;
	private String[] sortBlotterEntries;

    private long accountId;
    private boolean isAccountFilter;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.blotter_filter);
		
		df = DateUtils.getShortDateFormat(this);
		sortBlotterEntries = getResources().getStringArray(R.array.sort_blotter_entries);
        noFilterValue = getString(R.string.no_filter);
        
		LinearLayout layout = findViewById(R.id.layout);
		period = x.addFilterNodeMinus(layout, R.id.period, R.id.period_clear, R.string.period, R.string.no_filter);
		account = x.addFilterNodeMinus(layout, R.id.account, R.id.account_clear, R.string.account, R.string.no_filter);
		currency = x.addFilterNodeMinus(layout, R.id.currency, R.id.currency_clear, R.string.currency, R.string.no_filter);
		initCategorySelector(layout);
		initPayeeSelector(layout);
		initProjectSelector(layout);
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
			location.setText(loc != null ? loc.name : noFilterValue);
            showMinusButton(location);
		} else {
			location.setText(R.string.no_filter);
            hideMinusButton(location);
		}
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

	@Override
	protected void onClick(View v, int id) {
    	super.onClick(v, id);
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
		}
	}

	@Override
	public void onSelectedId(final int id, final long selectedId) {
    	super.onSelectedId(id, selectedId);
		switch (id) {
			case R.id.account:
				filter.put(Criteria.eq(FROM_ACCOUNT_ID, String.valueOf(selectedId)));
				updateAccountFromFilter();
				break;
			case R.id.currency:
				filter.put(Criteria.eq(BlotterFilter.FROM_ACCOUNT_CURRENCY_ID, String.valueOf(selectedId)));
				updateCurrencyFromFilter();
				break;
			case R.id.location:
				filter.put(Criteria.eq(BlotterFilter.LOCATION_ID, String.valueOf(selectedId)));
				updateLocationFromFilter();
				break;
		}
	}
	
	@Override
	public void onSelectedPos(int id, int selectedPos) {
    	super.onSelectedPos(id, selectedPos);
		switch (id) {
			case R.id.status:
				filter.put(Criteria.eq(BlotterFilter.STATUS, statuses[selectedPos].name()));
				updateStatusFromFilter();			
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
    	super.onSelected(id, items);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
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
		}
	}
}