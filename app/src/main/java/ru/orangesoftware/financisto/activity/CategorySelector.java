/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.activity;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.support.v4.util.Pair;
import android.text.InputType;
import android.view.View;
import android.widget.*;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.DatabaseHelper;
import ru.orangesoftware.financisto.model.*;
import ru.orangesoftware.financisto.utils.ArrUtils;
import ru.orangesoftware.financisto.utils.TransactionUtils;
import ru.orangesoftware.financisto.utils.Utils;
import ru.orangesoftware.financisto.view.AttributeView;
import ru.orangesoftware.financisto.view.AttributeViewFactory;

import java.util.*;

import static java.util.Objects.requireNonNull;

public class CategorySelector<A extends AbstractActivity> {

    private final A activity;
    private final DatabaseAdapter db;
    private final ActivityLayout x;

    private TextView categoryText;
    private AutoCompleteTextView filterAutoCompleteTxt;
    private SimpleCursorAdapter autoCompleteAdapter;
    private Cursor categoryCursor;
    private ListAdapter categoryAdapter;
    private LinearLayout attributesLayout;

    private long selectedCategoryId = 0;
    private CategorySelectorListener listener;
    private boolean showSplitCategory = true;
    private boolean multiSelect, useMultiChoicePlainSelector;
    private final long excludingSubTreeId;
    private List<Category> categories = Collections.emptyList();
    private int emptyResId;

    public CategorySelector(A activity, DatabaseAdapter db, ActivityLayout x) {
        this(activity, db, x, -1);
    }

    public CategorySelector(A activity, DatabaseAdapter db, ActivityLayout x, long exclSubTreeId) {
        this.activity = activity;
        this.db = db;
        this.x = x;
        this.excludingSubTreeId = exclSubTreeId;
    }
    
    
    public void setListener(CategorySelectorListener listener) {
        this.listener = listener;
    }

    public void doNotShowSplitCategory() {
        this.showSplitCategory = false;
    }
    
    public void initMultiSelect() {
        this.multiSelect = true;
        this.categories = db.getCategoriesList(false);
        this.doNotShowSplitCategory();
        
    }

    public void setUseMultiChoicePlainSelector() {
        this.useMultiChoicePlainSelector = true;
    }

    public void setEmptyResId(int emptyResId) {
        this.emptyResId = emptyResId;
    }

    public int getEmptyResId() {
        return emptyResId;
    }

    public List<Category> getCategories() {
        return categories;
    }

    public String getCheckedTitles() {
        return MyEntitySelector.getCheckedTitles(categories);
    }

    public String getCheckedIdsAsStr() {
        return MyEntitySelector.getCheckedIdsAsStr(categories);
    }

    public String[] getCheckedCategoryIds() {
        return MyEntitySelector.getCheckedIds(categories);
    }

    public String[] getCheckedCategoryLeafs() {
        LinkedList<String> res = new LinkedList<>();
        for (Category c : categories) {
            if (c.checked) {
                res.add(String.valueOf(c.left));
                res.add(String.valueOf(c.right));
            }
        }
        return ArrUtils.strListToArr(res);
    }

    public void fetchCategories(boolean fetchAll) {
        if (!multiSelect) {
            if (fetchAll) {
                categoryCursor = db.getAllCategories();
            } else {
                if (excludingSubTreeId > 0) {
                    categoryCursor = db.getCategoriesWithoutSubtree(excludingSubTreeId, true);
                } else {
                    categoryCursor = db.getCategories(true);
                }
            }
            activity.startManagingCursor(categoryCursor);
            categoryAdapter = TransactionUtils.createCategoryAdapter(db, activity, categoryCursor);
        }
    }

    public void setNode(TextView textNode) {
        categoryText = textNode;
    }

    public TextView createNode(LinearLayout layout, SelectorType type) {
        final Pair<TextView, AutoCompleteTextView> nodes;
        switch (type) {
            case TRANSACTION:
                setEmptyResId(R.string.select_category);
                nodes = x.addListNodeCategory(layout, R.id.category_filter_toggle);
                break;
            case SPLIT:
            case TRANSFER:
                if (emptyResId <=0) setEmptyResId(R.string.select_category);
                nodes = x.addListNodeWithButtonsAndFilter(layout, R.id.category, R.id.category_add, R.id.category_clear, R.string.category, emptyResId, R.id.category_filter_toggle);
                break;
            case FILTER:
                if (emptyResId <=0) setEmptyResId(R.string.no_filter);
                nodes = x.addListNodeWithClearButtonAndFilter(layout, R.id.category, R.id.category_clear, R.string.category, emptyResId, R.id.category_filter_toggle);
                break;
            case PARENT:
                if (emptyResId <=0) setEmptyResId(R.string.select_category);
                nodes = Pair.create(x.addListNode(layout, R.id.category, R.string.parent, R.string.select_category), null);
                break;
            default:
                throw new IllegalArgumentException("unknown type: " + type);
        }
        categoryText = nodes.first;
        filterAutoCompleteTxt = nodes.second;
        return categoryText;
    }

    private void initAutoCompleteFilter(final AutoCompleteTextView filterTxt) { // init only after it's toggled
        autoCompleteAdapter = TransactionUtils.createCategoryFilterAdapter(activity, db);
        filterTxt.setInputType(InputType.TYPE_CLASS_TEXT 
                        | InputType.TYPE_TEXT_FLAG_CAP_WORDS 
                        | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                        | InputType.TYPE_TEXT_VARIATION_FILTER);
        filterTxt.setThreshold(1);
        filterTxt.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                filterTxt.setAdapter(requireNonNull(autoCompleteAdapter));
                filterTxt.selectAll();
            }
        });
        filterTxt.setOnItemClickListener((parent, view, position, id) -> {
            activity.onSelectedId(R.id.category, id);
            ToggleButton toggleBtn = (ToggleButton) filterTxt.getTag();
            toggleBtn.performClick();
        });
    }

    public void createDummyNode() {
        categoryText = new EditText(activity);
    }

    public void onClick(int id) {
        switch (id) {
            case R.id.category: {
                if (useMultiChoicePlainSelector) {
                    x.selectMultiChoice(activity, R.id.category, R.string.categories, categories);
                } else if (!CategorySelectorActivity.pickCategory(activity, multiSelect, selectedCategoryId, excludingSubTreeId, showSplitCategory)) {
                    x.select(activity, R.id.category, R.string.category, categoryCursor, categoryAdapter,
                        DatabaseHelper.CategoryViewColumns._id.name(), selectedCategoryId);
                    
                }
                break;
            }
            case R.id.category_add: {
                Intent intent = new Intent(activity, CategoryActivity.class);
                activity.startActivityForResult(intent, R.id.category_add);
                break;
            }
            case R.id.category_split:
                selectCategory(Category.SPLIT_CATEGORY_ID);
                break;
            case R.id.category_filter_toggle:
                if (autoCompleteAdapter == null) initAutoCompleteFilter(filterAutoCompleteTxt);
                break;
            case R.id.category_clear:
                clearCategory();
                break;
        }
    }

    private void clearCategory() {
        categoryText.setText(emptyResId);
        selectedCategoryId = 0;
        for (MyEntity e : categories) e.setChecked(false);
        showHideMinusBtn(false);
    }

    public void onSelectedId(int id, long selectedId) {
        onSelectedId(id, selectedId, true);
    }

    public void onSelectedId(int id, long selectedId, boolean selectLast) {
        if (id == R.id.category) {
            selectCategory(selectedId, selectLast);
        }
    }

    public void onSelected(int id, List<? extends MultiChoiceItem> ignore) {
        if (id == R.id.category) fillCategoryInUI();
    }

    public void fillCategoryInUI() {
        String selected = getCheckedTitles();
        if (Utils.isEmpty(selected)) {
            clearCategory();
        } else {
            categoryText.setText(selected);
            showHideMinusBtn(true);
        }
    }
    
    public long getSelectedCategoryId() {
        return selectedCategoryId;
    }

    public void selectCategory(long categoryId) {
        selectCategory(categoryId, true);
    }

    public void selectCategory(long categoryId, boolean selectLast) {
        if (multiSelect) {
            updateCheckedEntities("" + categoryId);
            selectedCategoryId = categoryId;
            fillCategoryInUI();
            if (listener != null) listener.onCategorySelected(null, false);
        } else {
            if (selectedCategoryId != categoryId) {
                Category category = db.getCategoryWithParent(categoryId);
                if (category != null) {
                    categoryText.setText(Category.getTitle(category.title, category.level));
                    showHideMinusBtn(true);
                }
                selectedCategoryId = categoryId;
                if (listener != null) listener.onCategorySelected(category, selectLast);
            }
        }
    }

    public void updateCheckedEntities(String checkedCommaIds) {
        MyEntitySelector.updateCheckedEntities(this.categories, checkedCommaIds);
    }

    public void updateCheckedEntities(String[] checkedIds) {
        MyEntitySelector.updateCheckedEntities(this.categories, checkedIds);
    }

    public void updateCheckedEntities(List<Long> checkedIds) {
        for (Long id : checkedIds) {
            for (MyEntity e : categories) {
                if (e.id == id) {
                    e.checked = true;
                    break;
                }
            }
        }
    }
    
    private void showHideMinusBtn(boolean show) {
        ImageView minusBtn = (ImageView) categoryText.getTag(R.id.bMinus);
        if (minusBtn != null) minusBtn.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    public void createAttributesLayout(LinearLayout layout) {
        attributesLayout = new LinearLayout(activity);
        attributesLayout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(attributesLayout, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    }

    protected List<TransactionAttribute> getAttributes() {
        List<TransactionAttribute> list = new LinkedList<TransactionAttribute>();
        long count = attributesLayout.getChildCount();
        for (int i=0; i<count; i++) {
            View v = attributesLayout.getChildAt(i);
            Object o = v.getTag();
            if (o instanceof AttributeView) {
                AttributeView av = (AttributeView)o;
                TransactionAttribute ta = av.newTransactionAttribute();
                list.add(ta);
            }
        }
        return list;
    }

    public void addAttributes(Transaction transaction) {
        attributesLayout.removeAllViews();
        ArrayList<Attribute> attributes = db.getAllAttributesForCategory(selectedCategoryId);
        Map<Long, String> values = transaction.categoryAttributes;
        for (Attribute a : attributes) {
            AttributeView av = inflateAttribute(a);
            String value = values != null ? values.get(a.id) : null;
            if (value == null) {
                value = a.defaultValue;
            }
            View v = av.inflateView(attributesLayout, value);
            v.setTag(av);
        }
    }

    private AttributeView inflateAttribute(Attribute attribute) {
        return AttributeViewFactory.createViewForAttribute(activity, attribute);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case R.id.category_add: {
                    categoryCursor.requery();
                    long categoryId = data.getLongExtra(DatabaseHelper.CategoryColumns._id.name(), -1);
                    if (categoryId != -1) {
                        selectCategory(categoryId);
                    }
                    break;
                }
                case R.id.category_pick: {
                    long categoryId = data.getLongExtra(CategorySelectorActivity.SELECTED_CATEGORY_ID, 0);
                    selectCategory(categoryId);
                    break;
                }
            }
        }
    }

    public boolean isSplitCategorySelected() {
        return Category.isSplit(selectedCategoryId);
    }

    @Deprecated // todo.mb: it seems not much sense in it, better do it in single place - activity.onSelectedId
    public interface CategorySelectorListener {
        @Deprecated
        void onCategorySelected(Category category, boolean selectLast);
    }
    public boolean isMultiSelect() {
        return multiSelect;
    }


    public void onDestroy() {
        if (autoCompleteAdapter != null) {
            autoCompleteAdapter.changeCursor(null);
            autoCompleteAdapter = null;
        }
    }

    public enum SelectorType {
        TRANSACTION, SPLIT, TRANSFER, FILTER, PARENT;
    }
}
