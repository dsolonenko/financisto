package ru.orangesoftware.financisto.activity;

import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.TransactionAttribute;
import ru.orangesoftware.financisto.widget.AmountInput;
import ru.orangesoftware.financisto.widget.AmountInput_;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ru.orangesoftware.financisto.activity.CategorySelector.SelectorType.SPLIT;

public class SplitTransactionActivity extends AbstractSplitActivity implements CategorySelector.CategorySelectorListener {

    private TextView amountTitle;
    private AmountInput amountInput;

    private CategorySelector<SplitTransactionActivity> categorySelector;

    public SplitTransactionActivity() {
        super(R.layout.split_fixed);
    }

    @Override
    protected void createUI(LinearLayout layout) {
        categorySelector.createNode(layout, SPLIT);

        amountInput = AmountInput_.build(this);
        amountInput.setOwner(this);
        amountInput.setOnAmountChangedListener((oldAmount, newAmount) -> setUnsplitAmount(split.unsplitAmount - newAmount));
        View v = x.addEditNode(layout, R.string.amount, amountInput);
        amountTitle = v.findViewById(R.id.label);
        categorySelector.createAttributesLayout(layout);
    }

    @Override
    protected void fetchData() {
        categorySelector = new CategorySelector<>(this, db, x);
        categorySelector.setListener(this);
        categorySelector.doNotShowSplitCategory();
        categorySelector.fetchCategories(false);
    }

    @Override
    protected void updateUI() {
        super.updateUI();
        categorySelector.selectCategory(split.categoryId);
        setAmount(split.fromAmount);
    }

    @Override
    protected boolean updateFromUI() {
        super.updateFromUI();
        split.fromAmount = amountInput.getAmount();
        split.categoryAttributes = getAttributes();
        return true;
    }

    private Map<Long, String> getAttributes() {
        List<TransactionAttribute> attributeList = categorySelector.getAttributes();
        Map<Long, String> attributes = new HashMap<>();
        for (TransactionAttribute ta : attributeList) {
            attributes.put(ta.attributeId, ta.value);
        }
        return attributes;
    }

    @Override
    public void onCategorySelected(Category category, boolean selectLast) {
        if (category.isIncome()) {
            amountInput.setIncome();
        } else {
            amountInput.setExpense();
        }
        split.categoryId = category.id;
        categorySelector.addAttributes(split);
    }

    private void setAmount(long amount) {
        amountInput.setAmount(amount);
        Currency c = getCurrency();
        amountInput.setCurrency(c);
        amountTitle.setText(getString(R.string.amount)+" ("+c.name+")");
    }

    @Override
    protected void onClick(View v, int id) {
        super.onClick(v, id);
        categorySelector.onClick(id);
    }

    @Override
    public void onSelectedId(int id, long selectedId) {
        super.onSelectedId(id, selectedId);
        categorySelector.onSelectedId(id, selectedId);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        categorySelector.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    protected void onDestroy() {
        if (categorySelector != null) categorySelector.onDestroy();
        super.onDestroy();
    }
}
