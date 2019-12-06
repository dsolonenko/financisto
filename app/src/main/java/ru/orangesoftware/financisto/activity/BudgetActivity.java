package ru.orangesoftware.financisto.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.*;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.Budget;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.MultiChoiceItem;
import ru.orangesoftware.financisto.utils.RecurUtils;
import ru.orangesoftware.financisto.utils.RecurUtils.Recur;
import ru.orangesoftware.financisto.widget.AmountInput;
import ru.orangesoftware.financisto.widget.AmountInput_;

import java.util.ArrayList;
import java.util.List;

import static ru.orangesoftware.financisto.activity.CategorySelector.SelectorType.FILTER;

public class BudgetActivity extends AbstractActivity {

    public static final String BUDGET_ID_EXTRA = "budgetId";

    private static final int RECUR_REQUEST = 3;

    private AmountInput amountInput;

    private EditText titleText;
    private TextView accountText;
    private TextView periodRecurText;
    private CheckBox cbMode;
    private CheckBox cbIncludeSubCategories;
    private CheckBox cbIncludeCredit;
    private CheckBox cbSavingBudget;

    private Budget budget = new Budget();

    private List<AccountOption> accountOptions;
    private ProjectSelector<BudgetActivity> projectSelector;
    private CategorySelector<BudgetActivity> categorySelector;

    private ListAdapter accountAdapter;
    private int selectedAccountOption;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.budget);

        accountOptions = createAccountsList();
        accountAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, accountOptions);

        categorySelector = new CategorySelector<>(this, db, x);
        categorySelector.setEmptyResId(R.string.no_categories);
        categorySelector.initMultiSelect();
        categorySelector.setUseMultiChoicePlainSelector();

        projectSelector = new ProjectSelector<>(this, db, x, R.string.no_projects);
        projectSelector.initMultiSelect();

        LinearLayout layout = findViewById(R.id.list);

        titleText = new EditText(this);
        titleText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        x.addEditNode(layout, R.string.title, titleText);

        accountText = x.addListNode(layout, R.id.account,
                R.string.account, R.string.select_account);

        categorySelector.createNode(layout, FILTER);

        projectSelector.createNode(layout);
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

        amountInput = AmountInput_.build(this);
        amountInput.setOwner(this);
        amountInput.setIncome();
        amountInput.disableIncomeExpenseButton();
        x.addEditNode(layout, R.string.amount, amountInput);

        periodRecurText = x.addListNode(layout, R.id.period_recur, R.string.period_recur, R.string.no_recur);

        Button bOK = findViewById(R.id.bOK);
        bOK.setOnClickListener(arg0 -> {
            if (checkSelected(budget.currency != null ? budget.currency : budget.account, R.string.select_account)) {
                updateBudgetFromUI();
                long id = db.insertBudget(budget);
                Intent intent = new Intent();
                intent.putExtra(BUDGET_ID_EXTRA, id);
                setResult(RESULT_OK, intent);
                finish();
            }
        });

        Button bCancel = findViewById(R.id.bCancel);
        bCancel.setOnClickListener(arg0 -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        Intent intent = getIntent();
        if (intent != null) {
            long id = intent.getLongExtra(BUDGET_ID_EXTRA, -1);
            if (id != -1) {
                budget = db.load(Budget.class, id);
                editBudget();
            } else {
                selectRecur(RecurUtils.createDefaultRecur().toString());
            }
        }

    }

    private List<AccountOption> createAccountsList() {
        List<AccountOption> accounts = new ArrayList<>();
        List<Currency> currenciesList = db.getAllCurrenciesList("name");
        for (Currency currency : currenciesList) {
            String title = getString(R.string.account_by_currency, currency.name);
            accounts.add(new AccountOption(title, currency, null));
        }
        List<Account> accountsList = db.getAllAccountsList();
        for (Account account : accountsList) {
            accounts.add(new AccountOption(account.title, null, account));
        }
        return accounts;
    }

    private void editBudget() {
        titleText.setText(budget.title);
        amountInput.setAmount(budget.amount);
        categorySelector.updateCheckedEntities(budget.categories);
        categorySelector.fillCategoryInUI();

        projectSelector.updateCheckedEntities(budget.projects);
        projectSelector.fillCheckedEntitiesInUI();
        selectAccount(budget);
        selectRecur(budget.recur);
        cbIncludeSubCategories.setChecked(budget.includeSubcategories);
        cbIncludeCredit.setChecked(budget.includeCredit);
        cbMode.setChecked(budget.expanded);
        cbSavingBudget.setChecked(budget.amount < 0);
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
        budget.categories = categorySelector.getCheckedIdsAsStr();
        budget.projects = projectSelector.getCheckedIdsAsStr();
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
            case R.id.category_clear:
            case R.id.category_show_list:
            case R.id.category_close_filter:
            case R.id.category_show_filter:
                categorySelector.onClick(id);
                break;
            case R.id.project:
            case R.id.project_clear:
            case R.id.project_show_filter:
            case R.id.project_close_filter:
                projectSelector.onClick(id);
                break;
            case R.id.account:
                x.selectPosition(this, R.id.account, R.string.account, accountAdapter, selectedAccountOption);
                break;
            case R.id.period_recur: {
                Intent intent = new Intent(this, RecurActivity.class);
                if (budget.recur != null) {
                    intent.putExtra(RecurActivity.EXTRA_RECUR, budget.recur);
                }
                startActivityForResult(intent, RECUR_REQUEST);
            }
            break;
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
    public void onSelectedId(int id, long selectedId) {
        switch (id) {
            case R.id.category:
                categorySelector.onSelectedId(id, selectedId);
                categorySelector.fillCategoryInUI();
                break;
            case R.id.project:
                projectSelector.onSelectedId(id, selectedId);
                break;
        }
    }

    @Override
    public void onSelected(int id, List<? extends MultiChoiceItem> items) {
        switch (id) {
            case R.id.category:
                categorySelector.onSelected(id, items);
                break;
            case R.id.project:
                projectSelector.onSelected(id, items);
                break;
        }
    }

    private void selectAccount(Budget budget) {
        for (int i = 0; i < accountOptions.size(); i++) {
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
        if (resultCode == RESULT_OK && requestCode == RECUR_REQUEST) {
            String recur = data.getStringExtra(RecurActivity.EXTRA_RECUR);
            if (recur != null) {
                selectRecur(recur);
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (categorySelector != null) categorySelector.onDestroy();
        super.onDestroy();
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
