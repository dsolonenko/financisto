/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 *     Abdsandryk - menu option to call Credit Card Bill functionality
 ******************************************************************************/
package ru.orangesoftware.financisto.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.*;
import greendroid.widget.QuickActionGrid;
import greendroid.widget.QuickActionWidget;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.adapter.BlotterListAdapter;
import ru.orangesoftware.financisto.adapter.TransactionsListAdapter;
import ru.orangesoftware.financisto.blotter.AccountTotalCalculationTask;
import ru.orangesoftware.financisto.blotter.BlotterTotalCalculationTask;
import ru.orangesoftware.financisto.blotter.TotalCalculationTask;
import ru.orangesoftware.financisto.filter.WhereFilter;
import ru.orangesoftware.financisto.dialog.TransactionInfoDialog;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.AccountType;
import ru.orangesoftware.financisto.model.Transaction;
import ru.orangesoftware.financisto.utils.ExecutableEntityEnum;
import ru.orangesoftware.financisto.utils.MenuItemInfo;
import ru.orangesoftware.financisto.utils.MyPreferences;
import ru.orangesoftware.financisto.view.NodeInflater;

import java.util.List;

import static ru.orangesoftware.financisto.utils.AndroidUtils.isGreenDroidSupported;
import static ru.orangesoftware.financisto.utils.EnumUtils.showPickOneDialog;
import static ru.orangesoftware.financisto.utils.MyPreferences.isQuickMenuEnabledForTransaction;

public class BlotterActivity extends AbstractListActivity {
	
	public static final String SAVE_FILTER = "saveFilter";
	public static final String EXTRA_FILTER_ACCOUNTS = "filterAccounts";

	private static final int NEW_TRANSACTION_REQUEST = 1;
	private static final int NEW_TRANSFER_REQUEST = 3;
	private static final int NEW_TRANSACTION_FROM_TEMPLATE_REQUEST = 5;
	private static final int MONTHLY_VIEW_REQUEST = 6;
	private static final int BILL_PREVIEW_REQUEST = 7;
	
	protected static final int FILTER_REQUEST = 6;
	private static final int MENU_DUPLICATE = MENU_ADD+1;
	private static final int MENU_SAVE_AS_TEMPLATE = MENU_ADD+2;
	
	protected TextView totalText;
	protected ImageButton bFilter;
    protected ImageButton bTransfer;
    protected ImageButton bTemplate;

    private QuickActionWidget transactionActionGrid;
    private QuickActionWidget addButtonActionGrid;

	private TotalCalculationTask calculationTask;

	protected boolean saveFilter;
	protected WhereFilter blotterFilter = WhereFilter.empty();

    private boolean isAccountBlotter = false;
    private boolean showAllBlotterButtons = true;

    public BlotterActivity(int layoutId) {
		super(layoutId);
	}

    public BlotterActivity() {
		super(R.layout.blotter);
	}
        
	protected void calculateTotals() {	
		if (calculationTask != null) {
			calculationTask.stop();
			calculationTask.cancel(true);
		}
        calculationTask = createTotalCalculationTask();
		calculationTask.execute();
	}

    protected TotalCalculationTask createTotalCalculationTask() {
        WhereFilter filter = WhereFilter.copyOf(blotterFilter);
        if (filter.getAccountId() > 0) {
            return new AccountTotalCalculationTask(this, db, filter, totalText);
        } else {
            return new BlotterTotalCalculationTask(this, db, filter, totalText);
        }
    }

    @Override
    public void recreateCursor() {
        super.recreateCursor();
        calculateTotals();
    }

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		LayoutInflater layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater = new NodeInflater(layoutInflater);
        integrityCheck();
    }

    @Override
	protected void internalOnCreate(Bundle savedInstanceState) {
		super.internalOnCreate(savedInstanceState);

        showAllBlotterButtons = !MyPreferences.isCollapseBlotterButtons(this);

        if (showAllBlotterButtons) {
            bTransfer = (ImageButton)findViewById(R.id.bTransfer);
            bTransfer.setVisibility(View.VISIBLE);
            bTransfer.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View arg0) {
                    addItem(NEW_TRANSFER_REQUEST, TransferActivity.class);
                }
            });

            bTemplate = (ImageButton)findViewById(R.id.bTemplate);
            bTemplate.setVisibility(View.VISIBLE);
            bTemplate.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v) {
                    createFromTemplate();
                }
            });
        }

		bFilter = (ImageButton)findViewById(R.id.bFilter);
		bFilter.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(BlotterActivity.this, BlotterFilterActivity.class);
				blotterFilter.toIntent(intent);
                intent.putExtra(BlotterFilterActivity.IS_ACCOUNT_FILTER, isAccountBlotter && blotterFilter.getAccountId() > 0);
				startActivityForResult(intent, FILTER_REQUEST);
			}
		});

		totalText = (TextView)findViewById(R.id.total);
        totalText.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                showTotals();
            }
        });

		Intent intent = getIntent();
		if (intent != null) {			
			blotterFilter = WhereFilter.fromIntent(intent);
			saveFilter = intent.getBooleanExtra(SAVE_FILTER, false);
            isAccountBlotter = intent.getBooleanExtra(BlotterFilterActivity.IS_ACCOUNT_FILTER, false);
		}
		if (savedInstanceState != null) {
			blotterFilter = WhereFilter.fromBundle(savedInstanceState);
		}
		if (saveFilter && blotterFilter.isEmpty()) {
			blotterFilter = WhereFilter.fromSharedPreferences(getPreferences(0));
		}
		applyFilter();
		calculateTotals();
        prepareTransactionActionGrid();
        prepareAddButtonActionGrid();
	}

    private void showTotals() {
        Intent intent = new Intent(this, BlotterTotalsDetailsActivity.class);
        blotterFilter.toIntent(intent);
        startActivityForResult(intent, -1);
}

    protected void prepareTransactionActionGrid() {
        if (isGreenDroidSupported()) {
            transactionActionGrid = new QuickActionGrid(this);
            transactionActionGrid.addQuickAction(new MyQuickAction(this, R.drawable.gd_action_bar_info, R.string.info));
            transactionActionGrid.addQuickAction(new MyQuickAction(this, R.drawable.gd_action_bar_edit, R.string.edit));
            transactionActionGrid.addQuickAction(new MyQuickAction(this, R.drawable.gd_action_bar_trashcan, R.string.delete));
            transactionActionGrid.addQuickAction(new MyQuickAction(this, R.drawable.gd_action_bar_share, R.string.duplicate));
            transactionActionGrid.addQuickAction(new MyQuickAction(this, R.drawable.ic_action_bar_mark, R.string.clear));
            transactionActionGrid.addQuickAction(new MyQuickAction(this, R.drawable.ic_action_bar_double_mark, R.string.reconcile));
            transactionActionGrid.setOnQuickActionClickListener(transactionActionListener);
        }
    }

    private QuickActionWidget.OnQuickActionClickListener transactionActionListener = new QuickActionWidget.OnQuickActionClickListener() {
        public void onQuickActionClicked(QuickActionWidget widget, int position) {
            switch (position) {
                case 0:
                    showTransactionInfo(selectedId);
                    break;
                case 1:
                    editTransaction(selectedId);
                    break;
                case 2:
                    deleteTransaction(selectedId);
                    break;
                case 3:
                    duplicateTransaction(selectedId, 1);
                    break;
                case 4:
                    clearTransaction(selectedId);
                    break;
                case 5:
                    reconcileTransaction(selectedId);
                    break;
            }
        }

    };

    private void prepareAddButtonActionGrid() {
        if (isGreenDroidSupported()) {
            addButtonActionGrid = new QuickActionGrid(this);
            addButtonActionGrid.addQuickAction(new MyQuickAction(this, R.drawable.ic_input_add, R.string.transaction));
            addButtonActionGrid.addQuickAction(new MyQuickAction(this, R.drawable.ic_input_transfer, R.string.transfer));
            addButtonActionGrid.addQuickAction(new MyQuickAction(this, R.drawable.ic_input_templates, R.string.template));
            addButtonActionGrid.setOnQuickActionClickListener(addButtonActionListener);
        }
    }

    private QuickActionWidget.OnQuickActionClickListener addButtonActionListener = new QuickActionWidget.OnQuickActionClickListener() {
        public void onQuickActionClicked(QuickActionWidget widget, int position) {
            switch (position) {
                case 0:
                    addItem(NEW_TRANSACTION_REQUEST, TransactionActivity.class);
                    break;
                case 1:
                    addItem(NEW_TRANSFER_REQUEST, TransferActivity.class);
                    break;
                case 2:
                    createFromTemplate();
                    break;
            }
        }

    };

    private void clearTransaction(long selectedId) {
        new BlotterOperations(this, db, selectedId).clearTransaction();
        recreateCursor();
    }

    private void reconcileTransaction(long selectedId) {
        new BlotterOperations(this, db, selectedId).reconcileTransaction();
        recreateCursor();
    }

    @Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		blotterFilter.toBundle(outState);
	}

	protected void createFromTemplate() {
		Intent intent = new Intent(this, SelectTemplateActivity.class);
		startActivityForResult(intent, NEW_TRANSACTION_FROM_TEMPLATE_REQUEST);
	}

	@Override
	protected List<MenuItemInfo> createContextMenus(long id) {
		if (blotterFilter.isTemplate() || blotterFilter.isSchedule()) {
			return super.createContextMenus(id);			
		} else {
			List<MenuItemInfo> menus = super.createContextMenus(id);			
			menus.add(new MenuItemInfo(MENU_DUPLICATE, R.string.duplicate));
			menus.add(new MenuItemInfo(MENU_SAVE_AS_TEMPLATE, R.string.save_as_template));
			return menus;
		}
	}
	
	@Override
	protected String getContextMenuHeaderTitle(int position) {
		return getString(blotterFilter.isTemplate() ? R.string.template : R.string.transaction);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if (!super.onContextItemSelected(item)) {
            AdapterView.AdapterContextMenuInfo mi = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
			switch (item.getItemId()) {
			case MENU_DUPLICATE:
				duplicateTransaction(mi.id, 1);
				return true;
			case MENU_SAVE_AS_TEMPLATE:
                new BlotterOperations(this, db, mi.id).duplicateAsTemplate();
				Toast.makeText(this, R.string.save_as_template_success, Toast.LENGTH_SHORT).show();
				return true;
			}
		}
		return false;
	}

	private long duplicateTransaction(long id, int multiplier) {
        long newId = new BlotterOperations(this, db, id).duplicateTransaction(multiplier);
		String toastText;
		if (multiplier > 1) {
			toastText = getString(R.string.duplicate_success_with_multiplier, multiplier);
		} else {
			toastText = getString(R.string.duplicate_success);
		}
		Toast.makeText(this, toastText, Toast.LENGTH_LONG).show();
		recreateCursor();
        AccountWidget.updateWidgets(BlotterActivity.this);
		return newId;
	}

	@Override
	protected void addItem() {
        if (showAllBlotterButtons) {
            addItem(NEW_TRANSACTION_REQUEST, TransactionActivity.class);
        } else {
            if (isGreenDroidSupported()) {
                addButtonActionGrid.show(bAdd);
            } else {
                showPickOneDialog(this, R.string.add_transaction, TransactionQuickMenuEntities.values(), addButtonActionListener);
            }
        }
	}

    protected void addItem(int requestId, Class<? extends AbstractTransactionActivity> clazz) {
        Intent intent = new Intent(BlotterActivity.this, clazz);
        long accountId = blotterFilter.getAccountId();
        if (accountId != -1) {
            intent.putExtra(TransactionActivity.ACCOUNT_ID_EXTRA, accountId);
        }
        intent.putExtra(TransactionActivity.TEMPLATE_EXTRA, blotterFilter.getIsTemplate());
        startActivityForResult(intent, requestId);
    }

	@Override
	protected Cursor createCursor() {
		Cursor c;
		long accountId = blotterFilter.getAccountId();
		if (accountId != -1) {
			c = db.getBlotterForAccount(blotterFilter);
		} else {
			c = db.getBlotter(blotterFilter);
		}
		return c;
	}

	@Override
	protected ListAdapter createAdapter(Cursor cursor) {
		long accountId = blotterFilter.getAccountId();
		if (accountId != -1) {
			return new TransactionsListAdapter(this, db, cursor);
		} else {
			return new BlotterListAdapter(this, db, cursor);
		}		
	}
	
	@Override
	protected void deleteItem(View v, int position, final long id) {
        deleteTransaction(id);
	}

    private void deleteTransaction(long id) {
        new BlotterOperations(this, db, id).deleteTransaction();
    }

    protected void afterDeletingTransaction(long id) {
        recreateCursor();
        AccountWidget.updateWidgets(this);
    }

	@Override
	public void editItem(View v, int position, long id) {
		editTransaction(id);
	}

    private void editTransaction(long id) {
        new BlotterOperations(this, db, id).editTransaction();
    }

    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == FILTER_REQUEST) {
			if (resultCode == RESULT_FIRST_USER) {
				blotterFilter.clear();		
			} else if (resultCode == RESULT_OK) {
				blotterFilter = WhereFilter.fromIntent(data);			
			}	
			if (saveFilter) {
				saveFilter();
			}
			applyFilter();
			recreateCursor();			
		} else if (resultCode == RESULT_OK && requestCode == NEW_TRANSACTION_FROM_TEMPLATE_REQUEST) {
            createTransactionFromTemplate(data);
		}
		if (resultCode == RESULT_OK || resultCode == RESULT_FIRST_USER) {
			calculateTotals();
		}
	}

    private void createTransactionFromTemplate(Intent data) {
        long templateId = data.getLongExtra(SelectTemplateActivity.TEMPATE_ID, -1);
        int multiplier = data.getIntExtra(SelectTemplateActivity.MULTIPLIER, 1);
        boolean edit = data.getBooleanExtra(SelectTemplateActivity.EDIT_AFTER_CREATION, false);
        if (templateId > 0) {
            long id = duplicateTransaction(templateId, multiplier);
            Transaction t = db.getTransaction(id);
            if (t.fromAmount == 0 || edit) {
                new BlotterOperations(this, db, id).asNewFromTemplate().editTransaction();
            }
        }
    }

    private void saveFilter() {
		SharedPreferences preferences = getPreferences(0);
		blotterFilter.toSharedPreferences(preferences);
	}

	protected void applyFilter() {
		long accountId = blotterFilter.getAccountId();
		if (accountId != -1) {
			Account a = em.getAccount(accountId);
			bAdd.setVisibility(a != null && a.isActive ? View.VISIBLE : View.GONE);
            if (showAllBlotterButtons) {
                bTransfer.setVisibility(a != null && a.isActive ? View.VISIBLE : View.GONE);
            }
		}
		String title = blotterFilter.getTitle();
		if (title != null) {
			setTitle(getString(R.string.blotter)+" : "+title);
		}
		updateFilterImage();		
	}

	protected void updateFilterImage() {
		bFilter.setImageResource(blotterFilter.isEmpty() ? R.drawable.ic_menu_filter_off : R.drawable.ic_menu_filter_on);
	}

	private NodeInflater inflater;
    private long selectedId = -1;

    @Override
    protected void onItemClick(View v, int position, long id) {
        if (isQuickMenuEnabledForTransaction(this)) {
            selectedId = id;
            transactionActionGrid.show(v);
        } else {
            showTransactionInfo(id);
        }
    }

    @Override
	protected void viewItem(View v, int position, long id) {
        showTransactionInfo(id);
	}

    private void showTransactionInfo(long id) {
        TransactionInfoDialog transactionInfoView = new TransactionInfoDialog(this, db, inflater);
        transactionInfoView.show(this, id);
    }

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		long accountId = blotterFilter.getAccountId();
		
		// Funcionality available for account blotter
		if (accountId != -1) {
	
			// get account type
			Account account = em.getAccount(accountId);
			AccountType type = AccountType.valueOf(account.type);
			
			if (type.isCreditCard) {
				// Show menu for Credit Cards - bill
				MenuInflater inflater = getMenuInflater();
				inflater.inflate(R.menu.ccard_blotter_menu, menu);
			} else {
				// Show menu for other accounts - monthly view
				MenuInflater inflater = getMenuInflater();
				inflater.inflate(R.menu.blotter_menu, menu);
			}
			
			return true;
		} else {
			return super.onCreateOptionsMenu(menu);
		}
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {

		long accountId = blotterFilter.getAccountId();

		Intent intent = new Intent(this, MonthlyViewActivity.class);
		intent.putExtra(MonthlyViewActivity.ACCOUNT_EXTRA, accountId);
		
		switch (item.getItemId()) {
        	
	        case R.id.opt_menu_month:
	        	// call credit card bill activity sending account id
	    		intent.putExtra(MonthlyViewActivity.BILL_PREVIEW_EXTRA, false);
	    		startActivityForResult(intent, MONTHLY_VIEW_REQUEST);
	            return true;

	        case R.id.opt_menu_bill:
	    		if (accountId != -1) {
	    			Account account = em.getAccount(accountId);
	    		
		        	// call credit card bill activity sending account id
		        	if (account.paymentDay>0 && account.closingDay>0) {
			        	intent.putExtra(MonthlyViewActivity.BILL_PREVIEW_EXTRA, true);
			    		startActivityForResult(intent, BILL_PREVIEW_REQUEST);
			            return true;
					} else {	
						// display message: need payment and closing day
						AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(this);
				        dlgAlert.setMessage(R.string.statement_error);
				        dlgAlert.setTitle(R.string.ccard_statement);
				        dlgAlert.setPositiveButton(R.string.ok, null);
				        dlgAlert.setCancelable(true);
				        dlgAlert.create().show();
						return true;
					}
	    		} else {
	    			return true;
	    		}
	        default:
	            return super.onOptionsItemSelected(item);
        }
    }

    private enum TransactionQuickMenuEntities implements ExecutableEntityEnum<QuickActionWidget.OnQuickActionClickListener> {

        NEW_TRANSACTION(R.string.transaction, R.drawable.ic_input_add){
            @Override
            public void execute(QuickActionWidget.OnQuickActionClickListener listener) {
                listener.onQuickActionClicked(null, 0);
            }
        },
        NEW_TRANSFER(R.string.transfer, R.drawable.ic_input_transfer) {
            @Override
            public void execute(QuickActionWidget.OnQuickActionClickListener listener) {
                listener.onQuickActionClicked(null, 1);
            }
        },
        NEW_TEMPLATE(R.string.template, R.drawable.ic_input_templates) {
            @Override
            public void execute(QuickActionWidget.OnQuickActionClickListener listener) {
                listener.onQuickActionClicked(null, 2);
            }
        };

        private final int titleId;
        private final int iconId;

        private TransactionQuickMenuEntities(int titleId, int iconId) {
            this.titleId = titleId;
            this.iconId = iconId;
        }

        @Override
        public int getTitleId() {
            return titleId;
        }

        @Override
        public int getIconId() {
            return iconId;
        }

    }

}
