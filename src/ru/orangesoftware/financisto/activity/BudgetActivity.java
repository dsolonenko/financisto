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
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.model.*;
import ru.orangesoftware.financisto.utils.RecurUtils;
import ru.orangesoftware.financisto.utils.RecurUtils.Recur;
import ru.orangesoftware.financisto.utils.Utils;
import ru.orangesoftware.financisto.widget.AmountInput;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BudgetActivity extends AbstractActivity {
	
	public static final String BUDGET_ID_EXTRA = "budgetId";

	private static final int NEW_CATEGORY_REQUEST = 1;
	private static final int NEW_PROJECT_REQUEST = 2;
	private static final int RECUR_REQUEST = 3;
	
	private AmountInput amountInput;

	private EditText titleText;
	private TextView categoryText;
	private TextView projectText;
	private TextView accountText;
	private TextView periodRecurText;
	private CheckBox cbMode;
	private CheckBox cbIncludeSubCategories;
	private CheckBox cbIncludeCredit;
    private CheckBox cbSavingBudget;

	private Budget budget = new Budget();

    private List<AccountOption> accountOptions;
	private List<Category> categories;
	private List<Project> projects;

    private ListAdapter accountAdapter;
    private int selectedAccountOption;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.budget);

        accountOptions = createAccountsList();
        accountAdapter = new ArrayAdapter<AccountOption>(this, android.R.layout.simple_spinner_dropdown_item, accountOptions);

		categories = db.getCategoriesList(true);
		projects = em.getActiveProjectsList(true);
		
		LinearLayout layout = (LinearLayout) findViewById(R.id.list);

		titleText = new EditText(this);
		x.addEditNode(layout, R.string.title, titleText);

		accountText = x.addListNode(layout, R.id.account,
				R.string.account, R.string.select_account);
		categoryText = x.addListNodePlus(layout, R.id.category,
				R.id.category_add, R.string.categories, R.string.no_categories);
		projectText = x.addListNodePlus(layout, R.id.project,
				R.id.project_add, R.string.projects, R.string.no_projects);
		cbIncludeSubCategories = x.addCheckboxNode(layout,
				R.id.include_subcategories, R.string.include_subcategories,
				R.string.include_subcategories_summary, true);
		cbMode = x.addCheckboxNode(layout, R.id.budget_mode, R.string.budget_mode,
				R.string.budget_mode_summary, false);
        cbIncludeCredit = x.addCheckboxNode(layout,
                R.id.include_credit, R.string.include_credit,
                R.string.include_credit_summary, true);
        cbSavingBudget = x.addCheckboxNode(layout,
                R.id.type, R.string.budget_type_saving,
                R.string.budget_type_saving_summary, true);

		amountInput = new AmountInput(this);
		amountInput.setOwner(this);
        amountInput.setIncome();
        amountInput.disableIncomeExpenseButton();
		x.addEditNode(layout, R.string.amount, amountInput);

		periodRecurText = x.addListNode(layout, R.id.period_recur, R.string.period_recur, R.string.no_recur);

		Button bOK = (Button) findViewById(R.id.bOK);
		bOK.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if (checkSelected(budget.currency != null ? budget.currency : budget.account, R.string.select_account)) {
					updateBudgetFromUI();
					long id = em.insertBudget(budget);
					Intent intent = new Intent();
					intent.putExtra(BUDGET_ID_EXTRA, id);
					setResult(RESULT_OK, intent);
					finish();
				}
			}

		});

		Button bCancel = (Button) findViewById(R.id.bCancel);
		bCancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				setResult(RESULT_CANCELED);
				finish();
			}
		});

		Intent intent = getIntent();
		if (intent != null) {
			long id = intent.getLongExtra(BUDGET_ID_EXTRA, -1);
			if (id != -1) {
				budget = em.load(Budget.class, id);
				editBudget();
			} else {
				selectRecur(RecurUtils.createDefaultRecur().toString());
			}
		}

	}

    private List<AccountOption> createAccountsList() {
        List<AccountOption> accounts = new ArrayList<AccountOption>();
        List<Currency> currenciesList = em.getAllCurrenciesList("name");
        for (Currency currency : currenciesList) {
            String title = getString(R.string.account_by_currency, currency.name);
            accounts.add(new AccountOption(title, currency, null));
        }
        List<Account> accountsList = em.getAllAccountsList();
        for (Account account : accountsList) {
            accounts.add(new AccountOption(account.title, null, account));
        }
        return accounts;
    }

    private void editBudget() {
		titleText.setText(budget.title);
		amountInput.setAmount(budget.amount);
		updateEntities(this.categories, budget.categories);
		selectCategories();
		updateEntities(this.projects, budget.projects);
		selectProjects();
		selectAccount(budget);
		selectRecur(budget.recur);
		cbIncludeSubCategories.setChecked(budget.includeSubcategories);
		cbIncludeCredit.setChecked(budget.includeCredit);
		cbMode.setChecked(budget.expanded);
        cbSavingBudget.setChecked(budget.amount < 0);
	}

	private void updateEntities(List<? extends MyEntity> list, String selected) {
		if (!Utils.isEmpty(selected)) {
			String[] a = selected.split(",");
			for (String s : a) {
				long id = Long.parseLong(s);
				for (MyEntity e : list) {
					if (e.id == id) {
						e.checked = true;
						break;
					}
				}
			}
		}
	}
	
	private String getSelectedAsString(List<? extends MyEntity> list) {
		StringBuilder sb = new StringBuilder();
		for (MyEntity e : list) {
			if (e.checked) {
				if (sb.length() > 0) {
					sb.append(",");
				}
				sb.append(e.id);
			}
		}
		return sb.length() > 0 ? sb.toString() : "";
	}

	protected void updateBudgetFromUI() {
		budget.title = titleText.getText().toString();
		budget.amount = amountInput.getAmount();
        if (cbSavingBudget.isChecked()) {
            budget.amount = -budget.amount;
        }
		budget.includeSubcategories = cbIncludeSubCategories.isChecked();
		budget.includeCredit = cbIncludeCredit.isChecked();
		budget.expanded = cbMode.isChecked();
		budget.categories = getSelectedAsString(categories);
		budget.projects = getSelectedAsString(projects);
	}

    @Override
	protected void onClick(View v, int id) {
		switch (id) {
		case R.id.include_subcategories:
			cbIncludeSubCategories.performClick();
			break;
		case R.id.include_credit:
			cbIncludeCredit.performClick();
			break;
		case R.id.budget_mode:
			cbMode.performClick();
			break;
        case R.id.type:
            cbSavingBudget.performClick();
            break;
		case R.id.category:
			x.selectMultiChoice(this, R.id.category, R.string.categories, categories);
			break;
		case R.id.category_add: {
			Intent intent = new Intent(this, CategoryActivity.class);
			startActivityForResult(intent, NEW_CATEGORY_REQUEST);
			} break;
		case R.id.project:
			x.selectMultiChoice(this, R.id.project, R.string.projects, projects);
			break;
		case R.id.project_add: {
			Intent intent = new Intent(this, ProjectActivity.class);
			startActivityForResult(intent, NEW_PROJECT_REQUEST);
			} break;
		case R.id.account:
			x.selectPosition(this, R.id.account, R.string.account, accountAdapter, selectedAccountOption);
			break;
		case R.id.period_recur: {
			Intent intent = new Intent(this, RecurActivity.class);
			if (budget.recur != null) {
				intent.putExtra(RecurActivity.EXTRA_RECUR, budget.recur);
			}
			startActivityForResult(intent, RECUR_REQUEST);
			} break;
		}
	}

    @Override
    public void onSelectedPos(int id, int selectedPos) {
        switch (id) {
            case R.id.account:
                selectAccount(selectedPos);
                break;
        }
    }

    @Override
	public void onSelected(int id, List<? extends MultiChoiceItem> items) {
		switch (id) {
		case R.id.category:
			selectCategories();
			break;
		case R.id.project:
			selectProjects();
			break;
		}
	}

    private void selectAccount(Budget budget) {
        for (int i=0; i<accountOptions.size(); i++) {
            AccountOption option = accountOptions.get(i);
            if (option.matches(budget)) {
                selectAccount(i);
                break;
            }
        }
    }

    private void selectAccount(int selectedPos) {
        AccountOption option = accountOptions.get(selectedPos);
        option.updateBudget(budget);
        selectedAccountOption = selectedPos;
        accountText.setText(option.title);
        if (option.currency != null) {
            amountInput.setCurrency(option.currency);
        } else {
            amountInput.setCurrency(option.account.currency);
        }
    }

    private void selectProjects() {
		String selectedProjects = getCheckedEntities(this.projects);
		if (Utils.isEmpty(selectedProjects)) {
			projectText.setText(R.string.no_projects);
		} else {
			projectText.setText(selectedProjects);			
		}
	}

	private void selectCategories() {
		String selectedCategories = getCheckedEntities(this.categories);
		if (Utils.isEmpty(selectedCategories)) {
			categoryText.setText(R.string.no_categories);
		} else {
			categoryText.setText(selectedCategories);
		}
	}
	
	private String getCheckedEntities(List<? extends MyEntity> list) {
		StringBuilder sb = new StringBuilder();
		for (MyEntity e : list) {
			if (e.checked) {
				if (sb.length() > 0) {
					sb.append(", ");
				}
				sb.append(e.title);
			}			
		}		
		return sb.toString();
	}
	
	private void selectRecur(String recur) {
		if (recur != null) {
			budget.recur = recur;
			Recur r = RecurUtils.createFromExtraString(recur);
			periodRecurText.setText(r.toString(this));
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			if (amountInput.processActivityResult(requestCode, data)) {
				return;
			}
			switch (requestCode) {
                case NEW_CATEGORY_REQUEST:
                    categories = merge(categories, db.getCategoriesList(true));
                    break;
                case NEW_PROJECT_REQUEST:
                    projects = merge(projects, em.getActiveProjectsList(true));
                    break;
                case RECUR_REQUEST:
                    String recur = data.getStringExtra(RecurActivity.EXTRA_RECUR);
                    if (recur != null) {
                        selectRecur(recur);
                    }
                    break;
                default:
                    break;
			}
		}
	}

	private static <T extends MyEntity> List<T> merge(List<T> oldList, List<T> newList) {
		for (T newT : newList) {
			for (Iterator<T> i = oldList.iterator(); i.hasNext(); ) {
				T oldT = i.next();
				if (newT.id == oldT.id) {
					newT.checked = oldT.checked;
					i.remove();
					break;
				}
			}
		}
		return newList;
	}

    private static class AccountOption {

        public final String title;
        public final Currency currency;
        public final Account account;

        private AccountOption(String title, Currency currency, Account account) {
            this.title = title;
            this.currency = currency;
            this.account = account;
        }

        @Override
        public String toString() {
            return title;
        }

        public boolean matches(Budget budget) {
            return (currency != null && budget.currency != null && currency.id == budget.currency.id) ||
                   (account != null && budget.account != null && account.id == budget.account.id);
        }

        public void updateBudget(Budget budget) {
            budget.currency = currency;
            budget.account = account;
        }

    }

}
