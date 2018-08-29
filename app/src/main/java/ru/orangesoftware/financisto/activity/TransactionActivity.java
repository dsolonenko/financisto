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
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;
import greendroid.widget.QuickActionGrid;
import greendroid.widget.QuickActionWidget;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.model.*;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.utils.*;

import java.io.*;
import java.util.*;

import static ru.orangesoftware.financisto.activity.CategorySelector.SelectorType.TRANSACTION;
import static ru.orangesoftware.financisto.utils.Utils.isNotEmpty;

public class TransactionActivity extends AbstractTransactionActivity {

    public static final String CURRENT_BALANCE_EXTRA = "accountCurrentBalance";
    public static final String AMOUNT_EXTRA = "accountAmount";
    public static final String ACTIVITY_STATE = "ACTIVITY_STATE";

    private static final int SPLIT_REQUEST = 5001;

    private final Currency currencyAsAccount = new Currency();

    private long idSequence = 0;
    private final IdentityHashMap<View, Transaction> viewToSplitMap = new IdentityHashMap<>();

    private TextView differenceText;
    private boolean isUpdateBalanceMode = false;
    private long currentBalance;
    private Utils u;

    private LinearLayout splitsLayout;
    private TextView unsplitAmountText;
    private TextView currencyText;

    private QuickActionWidget unsplitActionGrid;
    private long selectedOriginCurrencyId = -1;

    public TransactionActivity() {
    }

    protected int getLayoutId() {
        return MyPreferences.isUseFixedLayout(this) ? R.layout.transaction_fixed : R.layout.transaction_free;
    }

    @Override
    protected void internalOnCreate() {
        u = new Utils(this);
        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra(CURRENT_BALANCE_EXTRA)) {
                currentBalance = intent.getLongExtra(CURRENT_BALANCE_EXTRA, 0);
                isUpdateBalanceMode = true;
            } else if (intent.hasExtra(AMOUNT_EXTRA)) {
                currentBalance = intent.getLongExtra(AMOUNT_EXTRA, 0);
            }
        }
        if (transaction.isTemplateLike()) {
            setTitle(transaction.isTemplate() ? R.string.transaction_template : R.string.transaction_schedule);
            if (transaction.isTemplate()) {
                dateText.setEnabled(false);
                timeText.setEnabled(false);
            }
        }
        prepareUnsplitActionGrid();
        currencyAsAccount.name = getString(R.string.original_currency_as_account);
    }

    private void prepareUnsplitActionGrid() {
        unsplitActionGrid = new QuickActionGrid(this);
        unsplitActionGrid.addQuickAction(new MyQuickAction(this, R.drawable.ic_action_add, R.string.transaction));
        unsplitActionGrid.addQuickAction(new MyQuickAction(this, R.drawable.ic_action_transfer, R.string.transfer));
        unsplitActionGrid.addQuickAction(new MyQuickAction(this, R.drawable.ic_action_tick, R.string.unsplit_adjust_amount));
        unsplitActionGrid.addQuickAction(new MyQuickAction(this, R.drawable.ic_action_tick, R.string.unsplit_adjust_evenly));
        unsplitActionGrid.addQuickAction(new MyQuickAction(this, R.drawable.ic_action_tick, R.string.unsplit_adjust_last));
        unsplitActionGrid.setOnQuickActionClickListener(unsplitActionListener);
    }

    private QuickActionWidget.OnQuickActionClickListener unsplitActionListener = (widget, position) -> {
        switch (position) {
            case 0:
                createSplit(false);
                break;
            case 1:
                createSplit(true);
                break;
            case 2:
                unsplitAdjustAmount();
                break;
            case 3:
                unsplitAdjustEvenly();
                break;
            case 4:
                unsplitAdjustLast();
                break;
        }
    };

    private void unsplitAdjustAmount() {
        long splitAmount = calculateSplitAmount();
        rateView.setFromAmount(splitAmount);
        updateUnsplitAmount();
    }

    private void unsplitAdjustEvenly() {
        long unsplitAmount = calculateUnsplitAmount();
        if (unsplitAmount != 0) {
            List<Transaction> splits = new ArrayList<>(viewToSplitMap.values());
            SplitAdjuster.adjustEvenly(splits, unsplitAmount);
            updateSplits();
        }
    }

    private void unsplitAdjustLast() {
        long unsplitAmount = calculateUnsplitAmount();
        if (unsplitAmount != 0) {
            Transaction latestTransaction = null;
            for (Transaction t : viewToSplitMap.values()) {
                if (latestTransaction == null || latestTransaction.id > t.id) {
                    latestTransaction = t;
                }
            }
            if (latestTransaction != null) {
                SplitAdjuster.adjustSplit(latestTransaction, unsplitAmount);
                updateSplits();
            }
        }
    }

    private void updateSplits() {
        for (Map.Entry<View, Transaction> entry : viewToSplitMap.entrySet()) {
            View v = entry.getKey();
            Transaction split = entry.getValue();
            setSplitData(v, split);
        }
        updateUnsplitAmount();
    }

    @Override
    protected void fetchCategories() {
        categorySelector.fetchCategories(!isUpdateBalanceMode);
    }

    @Override
    protected void createListNodes(LinearLayout layout) {
        //account
        accountText = x.addListNode(layout, R.id.account, R.string.account, R.string.select_account);
        //payee
        isShowPayee = MyPreferences.isShowPayee(this);
        if (isShowPayee) {
            createPayeeNode(layout);
        }
        //category
        categorySelector.createNode(layout, TRANSACTION);
        //amount
        if (!isUpdateBalanceMode && MyPreferences.isShowCurrency(this)) {
            currencyText = x.addListNode(layout, R.id.original_currency, R.string.currency, R.string.original_currency_as_account);
        } else {
            currencyText = new TextView(this);
        }
        rateView.createTransactionUI();
        // difference
        if (isUpdateBalanceMode) {
            differenceText = x.addInfoNode(layout, -1, R.string.difference, "0");
            rateView.setFromAmount(currentBalance);
            rateView.setAmountFromChangeListener((oldAmount, newAmount) -> {
                long balanceDifference = newAmount - currentBalance;
                u.setAmountText(differenceText, rateView.getCurrencyFrom(), balanceDifference, true);
            });
            if (currentBalance > 0) {
                rateView.setIncome();
            } else {
                rateView.setExpense();
            }
        } else {
            if (currentBalance > 0) {
                rateView.setIncome();
            } else {
                rateView.setExpense();
            }
            createSplitsLayout(layout);
            rateView.setAmountFromChangeListener((oldAmount, newAmount) -> updateUnsplitAmount());
        }
    }

    private void selectLastCategoryForPayee(long id) {
        Payee p = db.get(Payee.class, id);
        if (p != null) {
            categorySelector.selectCategory(p.lastCategoryId);
        }
    }

    private void createSplitsLayout(LinearLayout layout) {
        splitsLayout = new LinearLayout(this);
        splitsLayout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(splitsLayout, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    }

    @Override
    protected void addOrRemoveSplits() {
        if (splitsLayout == null) {
            return;
        }
        if (categorySelector.isSplitCategorySelected()) {
            View v = x.addNodeUnsplit(splitsLayout);
            unsplitAmountText = v.findViewById(R.id.data);
            updateUnsplitAmount();
        } else {
            splitsLayout.removeAllViews();
        }
    }

    private void updateUnsplitAmount() {
        if (unsplitAmountText != null) {
            long amountDifference = calculateUnsplitAmount();
            u.setAmountText(unsplitAmountText, rateView.getCurrencyFrom(), amountDifference, false);
        }
    }

    private long calculateUnsplitAmount() {
        long splitAmount = calculateSplitAmount();
        return rateView.getFromAmount() - splitAmount;
    }

    private long calculateSplitAmount() {
        long amount = 0;
        for (Transaction split : viewToSplitMap.values()) {
            amount += split.fromAmount;
        }
        return amount;
    }

    protected void switchIncomeExpenseButton(Category category) {
        if (!isUpdateBalanceMode) {
            if (category.isIncome()) {
                rateView.setIncome();
            } else {
                rateView.setExpense();
            }
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus) {
            accountText.requestFocusFromTouch();
        }
    }

    @Override
    protected boolean onOKClicked() {
        if (checkSelectedId(getSelectedAccountId(), R.string.select_account) &&
                checkUnsplitAmount()) {
            updateTransactionFromUI();
            return true;
        }
        return false;
    }

    private boolean checkUnsplitAmount() {
        if (categorySelector.isSplitCategorySelected()) {
            long unsplitAmount = calculateUnsplitAmount();
            if (unsplitAmount != 0) {
                Toast.makeText(this, R.string.unsplit_amount_greater_than_zero, Toast.LENGTH_LONG).show();
                return false;
            }
        }
        return true;
    }

    @Override
    protected void editTransaction(Transaction transaction) {
        selectAccount(transaction.fromAccountId, false);
        commonEditTransaction(transaction);
        selectCurrency(transaction);
        fetchSplits();
        selectPayee(transaction.payeeId);
    }

    private void selectCurrency(Transaction transaction) {
        if (transaction.originalCurrencyId > 0) {
            selectOriginalCurrency(transaction.originalCurrencyId);
            rateView.setFromAmount(transaction.originalFromAmount);
            rateView.setToAmount(transaction.fromAmount);
        } else {
            if (transaction.fromAmount != 0) {
                rateView.setFromAmount(transaction.fromAmount);
            }
        }
    }

    private void fetchSplits() {
        List<Transaction> splits = db.getSplitsForTransaction(transaction.id);
        for (Transaction split : splits) {
            split.categoryAttributes = db.getAllAttributesForTransaction(split.id);
            if (split.originalCurrencyId > 0) {
                split.fromAmount = split.originalFromAmount;
            }
            addOrEditSplit(split);
        }
    }

    private void updateTransactionFromUI() {
        updateTransactionFromUI(transaction);
        transaction.fromAccountId = selectedAccount.id;
        long amount = rateView.getFromAmount();
        if (isUpdateBalanceMode) {
            amount -= currentBalance;
        }
        transaction.fromAmount = amount;
        updateTransactionOriginalAmount();
        if (categorySelector.isSplitCategorySelected()) {
            transaction.splits = new LinkedList<>(viewToSplitMap.values());
        } else {
            transaction.splits = null;
        }
    }

    private void updateTransactionOriginalAmount() {
        if (isDifferentCurrency()) {
            transaction.originalCurrencyId = selectedOriginCurrencyId;
            transaction.originalFromAmount = rateView.getFromAmount();
            transaction.fromAmount = rateView.getToAmount();
        } else {
            transaction.originalCurrencyId = 0;
            transaction.originalFromAmount = 0;
        }
    }

    private boolean isDifferentCurrency() {
        return selectedOriginCurrencyId > 0 && selectedOriginCurrencyId != selectedAccount.currency.id;
    }

    @Override
    protected Account selectAccount(long accountId, boolean selectLast) {
        Account a = super.selectAccount(accountId, selectLast);
        if (a != null) {
            if (selectLast && !isShowPayee && isRememberLastCategory) {
                categorySelector.selectCategory(a.lastCategoryId);
            }
        }
        if (selectedOriginCurrencyId > 0) {
            selectOriginalCurrency(selectedOriginCurrencyId);
        }
        return a;
    }

    @Override
    protected void onClick(View v, int id) {
        super.onClick(v, id);
        switch (id) {
            case R.id.unsplit_action:
                unsplitActionGrid.show(v);
                break;
            case R.id.add_split:
                createSplit(false);
                break;
            case R.id.add_split_transfer:
                if (selectedOriginCurrencyId > 0) {
                    Toast.makeText(this, R.string.split_transfer_not_supported_yet, Toast.LENGTH_LONG).show();
                    break;
                }
                createSplit(true);
                break;
            case R.id.delete_split:
                View parentView = (View) v.getParent();
                deleteSplit(parentView);
                break;
            case R.id.original_currency:
                List<Currency> currencies = db.getAllCurrenciesList();
                currencies.add(0, currencyAsAccount);
                ListAdapter adapter = TransactionUtils.createCurrencyAdapter(this, currencies);
                int selectedPos = MyEntity.indexOf(currencies, selectedOriginCurrencyId);
                x.selectItemId(this, R.id.currency, R.string.currency, adapter, selectedPos);
                break;
        }
        Transaction split = viewToSplitMap.get(v);
        if (split != null) {
            split.unsplitAmount = split.fromAmount + calculateUnsplitAmount();
            editSplit(split, split.toAccountId > 0 ? SplitTransferActivity.class : SplitTransactionActivity.class);
        }
    }


    @Override
    public void onSelectedId(int id, long selectedId) {
        super.onSelectedId(id, selectedId);
        switch (id) {
            case R.id.currency:
                selectOriginalCurrency(selectedId);
                break;
            case R.id.payee:
                if (isRememberLastCategory) {
                    selectLastCategoryForPayee(id);
                }
                break;
        }
    }

    private void selectOriginalCurrency(long selectedId) {
        selectedOriginCurrencyId = selectedId;
        if (selectedId == -1) {
            if (selectedAccount != null) {
                if (selectedAccount.currency.id == rateView.getCurrencyToId()) {
                    rateView.setFromAmount(rateView.getToAmount());
                }
            }
            selectAccountCurrency();
        } else {
            long toAmount = rateView.getToAmount();
            Currency currency = CurrencyCache.getCurrency(db, selectedId);
            rateView.selectCurrencyFrom(currency);
            if (selectedAccount != null) {
                if (selectedId == selectedAccount.currency.id) {
                    if (selectedId == rateView.getCurrencyToId()) {
                        rateView.setFromAmount(toAmount);
                    }
                    selectAccountCurrency();
                    return;
                }
                rateView.selectCurrencyTo(selectedAccount.currency);
            }
            currencyText.setText(currency.name);
        }
    }

    private void selectAccountCurrency() {
        rateView.selectSameCurrency(selectedAccount != null ? selectedAccount.currency : Currency.EMPTY);
        currencyText.setText(R.string.original_currency_as_account);
    }

    private void createSplit(boolean asTransfer) {
        Transaction split = new Transaction();
        split.id = --idSequence;
        split.fromAccountId = getSelectedAccountId();
        split.fromAmount = split.unsplitAmount = calculateUnsplitAmount();
        split.originalCurrencyId = selectedOriginCurrencyId;
        editSplit(split, asTransfer ? SplitTransferActivity.class : SplitTransactionActivity.class);
    }

    private void editSplit(Transaction split, Class splitActivityClass) {
        Intent intent = new Intent(this, splitActivityClass);
        split.toIntentAsSplit(intent);
        startActivityForResult(intent, SPLIT_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SPLIT_REQUEST) {
            if (resultCode == RESULT_OK) {
                Transaction split = Transaction.fromIntentAsSplit(data);
                addOrEditSplit(split);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d("Financisto", "onSaveInstanceState");
        try {
            if (categorySelector.isSplitCategorySelected()) {
                Log.d("Financisto", "Saving splits...");
                ActivityState state = new ActivityState();
                state.categoryId = categorySelector.getSelectedCategoryId();
                state.idSequence = idSequence;
                state.splits = new ArrayList<>(viewToSplitMap.values());
                try (ByteArrayOutputStream s = new ByteArrayOutputStream()) {
                    ObjectOutputStream out = new ObjectOutputStream(s);
                    out.writeObject(state);
                    outState.putByteArray(ACTIVITY_STATE, s.toByteArray());
                }
            }
        } catch (IOException e) {
            Log.e("Financisto", "Unable to save state", e);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.d("Financisto", "onRestoreInstanceState");
        byte[] bytes = savedInstanceState.getByteArray(ACTIVITY_STATE);
        if (bytes != null) {
            try {
                try (ByteArrayInputStream s = new ByteArrayInputStream(bytes)) {
                    ObjectInputStream in = new ObjectInputStream(s);
                    ActivityState state = (ActivityState) in.readObject();
                    if (state.categoryId == Category.SPLIT_CATEGORY_ID) {
                        Log.d("Financisto", "Restoring splits...");
                        viewToSplitMap.clear();
                        splitsLayout.removeAllViews();
                        idSequence = state.idSequence;
                        categorySelector.selectCategory(state.categoryId);
                        for (Transaction split : state.splits) {
                            addOrEditSplit(split);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("Financisto", "Unable to restore state", e);
            }
        }
    }

    private void addOrEditSplit(Transaction split) {
        View v = findView(split);
        if (v == null) {
            v = x.addSplitNodeMinus(splitsLayout, R.id.edit_aplit, R.id.delete_split, R.string.split, "");
        }
        setSplitData(v, split);
        viewToSplitMap.put(v, split);
        updateUnsplitAmount();
    }

    private View findView(Transaction split) {
        for (Map.Entry<View, Transaction> entry : viewToSplitMap.entrySet()) {
            Transaction s = entry.getValue();
            if (s.id == split.id) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void setSplitData(View v, Transaction split) {
        TextView label = v.findViewById(R.id.label);
        TextView data = v.findViewById(R.id.data);
        setSplitData(split, label, data);
    }

    private void setSplitData(Transaction split, TextView label, TextView data) {
        if (split.isTransfer()) {
            setSplitDataTransfer(split, label, data);
        } else {
            setSplitDataTransaction(split, label, data);
        }
    }

    private void setSplitDataTransaction(Transaction split, TextView label, TextView data) {
        label.setText(createSplitTransactionTitle(split));
        Currency currency = getCurrency();
        u.setAmountText(data, currency, split.fromAmount, false);
    }

    private String createSplitTransactionTitle(Transaction split) {
        StringBuilder sb = new StringBuilder();
        Category category = db.getCategoryWithParent(split.categoryId);
        sb.append(category.title);
        if (isNotEmpty(split.note)) {
            sb.append(" (").append(split.note).append(")");
        }
        return sb.toString();
    }

    private void setSplitDataTransfer(Transaction split, TextView label, TextView data) {
        Account fromAccount = db.getAccount(split.fromAccountId);
        Account toAccount = db.getAccount(split.toAccountId);
        u.setTransferTitleText(label, fromAccount, toAccount);
        u.setTransferAmountText(data, fromAccount.currency, split.fromAmount, toAccount.currency, split.toAmount);
    }

    private void deleteSplit(View v) {
        Transaction split = viewToSplitMap.remove(v);
        if (split != null) {
            removeSplitView(v);
            updateUnsplitAmount();
        }
    }

    private void removeSplitView(View v) {
        splitsLayout.removeView(v);
        View dividerView = (View) v.getTag();
        if (dividerView != null) {
            splitsLayout.removeView(dividerView);
        }
    }

    private Currency getCurrency() {
        if (selectedOriginCurrencyId > 0) {
            return CurrencyCache.getCurrency(db, selectedOriginCurrencyId);
        }
        if (selectedAccount != null) {
            return selectedAccount.currency;
        }
        return Currency.EMPTY;
    }

    private static class ActivityState implements Serializable {
        public long categoryId;
        public long idSequence;
        public List<Transaction> splits;
    }


}
