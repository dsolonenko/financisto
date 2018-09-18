package ru.orangesoftware.financisto.activity;

import android.database.Cursor;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.db.DatabaseHelper;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.utils.TransactionUtils;
import ru.orangesoftware.financisto.widget.RateLayoutView;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 4/21/11 7:17 PM
 */
public class SplitTransferActivity extends AbstractSplitActivity {

    private RateLayoutView rateView;

    protected TextView accountText;
    protected Cursor accountCursor;
    protected ListAdapter accountAdapter;

    public SplitTransferActivity() {
        super(R.layout.split_fixed);
    }

    @Override
    protected void createUI(LinearLayout layout) {
        accountText = x.addListNode(layout, R.id.account, R.string.account, R.string.select_to_account);
        rateView = new RateLayoutView(this, x, layout);
        rateView.createTransferUI();
        rateView.setAmountFromChangeListener((oldAmount, newAmount) -> setUnsplitAmount(split.unsplitAmount - newAmount));
    }

    @Override
    protected void fetchData() {
        accountCursor = db.getAllActiveAccounts();
        startManagingCursor(accountCursor);
        accountAdapter = TransactionUtils.createAccountAdapter(this, accountCursor);
    }

    @Override
    protected void updateUI() {
        super.updateUI();
        selectFromAccount(split.fromAccountId);
        selectToAccount(split.toAccountId);
        setFromAmount(split.fromAmount);
        setToAmount(split.toAmount);
    }

    @Override
    protected boolean updateFromUI() {
        super.updateFromUI();
        split.fromAmount = rateView.getFromAmount();
        split.toAmount = rateView.getToAmount();
        if (split.fromAccountId == split.toAccountId) {
            Toast.makeText(this, R.string.select_to_account_differ_from_to_account, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void selectFromAccount(long accountId) {
        if (accountId > 0) {
            Account account = db.getAccount(accountId);
            rateView.selectCurrencyFrom(account.currency);
        }
    }

    private void selectToAccount(long accountId) {
        if (accountId > 0) {
            Account account = db.getAccount(accountId);
            rateView.selectCurrencyTo(account.currency);
            accountText.setText(account.title);
            split.toAccountId = accountId;
        }
    }

    private void setFromAmount(long amount) {
        rateView.setFromAmount(amount);
    }

    private void setToAmount(long amount) {
        rateView.setToAmount(amount);
    }

    @Override
    protected void onClick(View v, int id) {
        super.onClick(v, id);
        if (id == R.id.account) {
            x.select(this, R.id.account, R.string.account_to, accountCursor, accountAdapter,
                    DatabaseHelper.AccountColumns.ID, split.toAccountId);
        }
    }

    @Override
    public void onSelectedId(int id, long selectedId) {
        super.onSelectedId(id, selectedId);
        switch(id) {
            case R.id.account:
                selectToAccount(selectedId);
                break;
        }
    }

}
