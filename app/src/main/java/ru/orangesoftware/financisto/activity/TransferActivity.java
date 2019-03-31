package ru.orangesoftware.financisto.activity;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.db.DatabaseHelper.AccountColumns;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.Transaction;
import ru.orangesoftware.financisto.utils.MyPreferences;

import static ru.orangesoftware.financisto.activity.CategorySelector.SelectorType.TRANSFER;

public class TransferActivity extends AbstractTransactionActivity {

    private TextView accountFromText;
    private TextView accountToText;

    private long selectedAccountFromId = -1;
    private long selectedAccountToId = -1;

    public TransferActivity() {
    }

    @Override
    protected void internalOnCreate() {
        if (transaction.isTemplateLike()) {
            setTitle(transaction.isTemplate() ? R.string.transfer_template : R.string.transfer_schedule);
            if (transaction.isTemplate()) {
                dateText.setEnabled(false);
                timeText.setEnabled(false);
            }
        }
    }

    protected void fetchCategories() {
        categorySelector.fetchCategories(false);
        categorySelector.doNotShowSplitCategory();
    }

    protected int getLayoutId() {
        return MyPreferences.isUseFixedLayout(this) ? R.layout.transfer_fixed : R.layout.transfer_free;
    }

    @Override
    protected void createListNodes(LinearLayout layout) {
        accountFromText = x.addListNode(layout, R.id.account_from, R.string.account_from, R.string.select_account);
        accountToText = x.addListNode(layout, R.id.account_to, R.string.account_to, R.string.select_account);
        // amounts
        rateView.createTransferUI();
        // payee
        isShowPayee = MyPreferences.isShowPayeeInTransfers(this);
        if (isShowPayee) {
            createPayeeNode(layout);
        }
        // category
        if (MyPreferences.isShowCategoryInTransferScreen(this)) {
            categorySelector.createNode(layout, TRANSFER);
        } else {
            categorySelector.createDummyNode();
        }
    }

    @Override
    protected void editTransaction(Transaction transaction) {
        if (transaction.fromAccountId > 0) {
            Account fromAccount = db.getAccount(transaction.fromAccountId);
            selectAccount(fromAccount, accountFromText, false);
            rateView.selectCurrencyFrom(fromAccount.currency);
            rateView.setFromAmount(transaction.fromAmount);
            selectedAccountFromId = transaction.fromAccountId;
        }
        commonEditTransaction(transaction);
        if (transaction.toAccountId > 0) {
            Account toAccount = db.getAccount(transaction.toAccountId);
            selectAccount(toAccount, accountToText, false);
            rateView.selectCurrencyTo(toAccount.currency);
            rateView.setToAmount(transaction.toAmount);
            selectedAccountToId = transaction.toAccountId;
        }
        selectPayee(transaction.payeeId);
    }

    @Override
    protected boolean onOKClicked() {
        if (selectedAccountFromId == -1) {
            Toast.makeText(this, R.string.select_from_account, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (selectedAccountToId == -1) {
            Toast.makeText(this, R.string.select_to_account, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (selectedAccountFromId == selectedAccountToId) {
            Toast.makeText(this, R.string.select_to_account_differ_from_to_account, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (checkSelectedEntities()) {
            updateTransferFromUI();
            return true;
        }
        return false;
    }

    private void updateTransferFromUI() {
        updateTransactionFromUI(transaction);
        transaction.fromAccountId = selectedAccountFromId;
        transaction.toAccountId = selectedAccountToId;
        transaction.fromAmount = rateView.getFromAmount();
        transaction.toAmount = rateView.getToAmount();
    }

    @Override
    protected void onClick(View v, int id) {
        super.onClick(v, id);
        switch (id) {
            case R.id.account_from:
                x.select(this, R.id.account_from, R.string.account, accountCursor, accountAdapter,
                        AccountColumns.ID, selectedAccountFromId);
                break;
            case R.id.account_to:
                x.select(this, R.id.account_to, R.string.account, accountCursor, accountAdapter,
                        AccountColumns.ID, selectedAccountToId);
                break;
        }
    }

    @Override
    public void onSelectedId(int id, long selectedId) {
        super.onSelectedId(id, selectedId);
        switch (id) {
            case R.id.account_from:
                selectFromAccount(selectedId);
                break;
            case R.id.account_to:
                selectToAccount(selectedId);
                break;
        }
    }

    private void selectFromAccount(long selectedId) {
        selectAccount(selectedId, true);
    }

    private void selectToAccount(long selectedId) {
        Account account = db.getAccount(selectedId);
        if (account != null) {
            selectAccount(account, accountToText, false);
            selectedAccountToId = selectedId;
            rateView.selectCurrencyTo(account.currency);
        }
    }

    @Override
    protected Account selectAccount(long accountId, boolean selectLast) {
        Account account = db.getAccount(accountId);
        if (account != null) {
            selectAccount(account, accountFromText, selectLast);
            selectedAccountFromId = accountId;
            rateView.selectCurrencyFrom(account.currency);
        }
        return account;
    }

    protected void selectAccount(Account account, TextView accountText, boolean selectLast) {
        accountText.setText(account.title);
        if (selectLast && isRememberLastAccount) {
            selectToAccount(account.lastAccountId);
        }
    }

}
