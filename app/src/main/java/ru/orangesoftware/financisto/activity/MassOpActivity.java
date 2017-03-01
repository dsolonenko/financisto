package ru.orangesoftware.financisto.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.adapter.BlotterListAdapter;
import ru.orangesoftware.financisto.filter.WhereFilter;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.utils.EnumUtils;
import ru.orangesoftware.financisto.utils.LocalizableEnum;

import java.util.Arrays;

public class MassOpActivity extends BlotterActivity {

	public MassOpActivity() {
		super(R.layout.blotter_mass_op);
	}

	@Override
	protected void internalOnCreate(Bundle savedInstanceState) {
		bFilter = (ImageButton)findViewById(R.id.bFilter);
		bFilter.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MassOpActivity.this, BlotterFilterActivity.class);
				blotterFilter.toIntent(intent);
				startActivityForResult(intent, FILTER_REQUEST);
			}
		});
		ImageButton bCheckAll = (ImageButton)findViewById(R.id.bCheckAll);
		bCheckAll.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				((BlotterListAdapter)getListAdapter()).checkAll();
			}
		});
		ImageButton bUncheckAll = (ImageButton)findViewById(R.id.bUncheckAll);
		bUncheckAll.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				((BlotterListAdapter)getListAdapter()).uncheckAll();
			}
		});
		
		final MassOp[] operations = MassOp.values();
		final Spinner spOperation = (Spinner)findViewById(R.id.spOperation);
		Button proceed = (Button)findViewById(R.id.proceed);
		proceed.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				MassOp op = operations[spOperation.getSelectedItemPosition()];
				applyMassOp(op);
			}
		});
		Intent intent = getIntent();
		if (intent != null) {			
			blotterFilter = WhereFilter.fromIntent(intent);
			applyFilter();
		}
		spOperation.setPrompt(getString(R.string.mass_operations));
		spOperation.setAdapter(EnumUtils.createSpinnerAdapter(this, operations));
        prepareTransactionActionGrid();
	}
	
	protected void applyMassOp(final MassOp op) {
		int count = ((BlotterListAdapter)getListAdapter()).getCheckedCount();
		if (count > 0) {
			new AlertDialog.Builder(this)
			.setMessage(getString(R.string.apply_mass_op, getString(op.getTitleId()), count))
			.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					BlotterListAdapter adapter = ((BlotterListAdapter)getListAdapter()); 
					long[] ids = adapter.getAllCheckedIds();
					Log.d("Financisto", "Will apply "+op+" on "+Arrays.toString(ids));
					op.apply(db, ids);
					adapter.uncheckAll();
					adapter.changeCursor(createCursor());
				}
			})
			.setNegativeButton(R.string.no, null)
			.show();
		} else {
			Toast.makeText(this, R.string.apply_mass_op_zero_count, Toast.LENGTH_SHORT).show();			
		}
	}

	@Override
	protected void applyFilter() {
		updateFilterImage();
	}
	
	@Override
	protected void calculateTotals() {
		// do nothing
	}

	@Override
	protected ListAdapter createAdapter(Cursor cursor) {
		return new BlotterListAdapter(this, db, R.layout.blotter_mass_op_list_item, cursor, true);
	}

	private enum MassOp implements LocalizableEnum{
		CLEAR(R.string.mass_operations_clear_all){
			@Override
			public void apply(DatabaseAdapter db, long[] ids) {
				db.clearSelectedTransactions(ids);
			}			
		}, 
		RECONCILE(R.string.mass_operations_reconcile){
			@Override
			public void apply(DatabaseAdapter db, long[] ids) {
				db.reconcileSelectedTransactions(ids);
			}			
		}, 
		DELETE(R.string.mass_operations_delete){
			@Override
			public void apply(DatabaseAdapter db, long[] ids) {
				db.deleteSelectedTransactions(ids);
                db.rebuildRunningBalances();
			}
		};
		
		private final int titleId;
		
		MassOp(int titleId) {
			this.titleId = titleId;
		}

		public abstract void apply(DatabaseAdapter db, long[] ids);

		@Override
		public int getTitleId() {
			return titleId;
		}
	}
	
}
