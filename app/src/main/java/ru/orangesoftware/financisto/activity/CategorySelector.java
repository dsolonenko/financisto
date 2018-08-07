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
import ru.orangesoftware.financisto.model.Attribute;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.Transaction;
import ru.orangesoftware.financisto.model.TransactionAttribute;
import ru.orangesoftware.financisto.utils.TransactionUtils;
import ru.orangesoftware.financisto.view.AttributeView;
import ru.orangesoftware.financisto.view.AttributeViewFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public class CategorySelector {

    private final Activity activity;
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
    private final long excludingSubTreeId;

    public CategorySelector(Activity activity, DatabaseAdapter db, ActivityLayout x) {
        this(activity, db, x, -1);
    }

    public CategorySelector(Activity activity, DatabaseAdapter db, ActivityLayout x, long exclSubTreeId) {
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

    public void fetchCategories(boolean fetchAll) {
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

    public void setNode(TextView textNode) {
        categoryText = textNode;
    }

    public void createNode(LinearLayout layout, SelectorType type) {
        switch (type) {
            case TRANSACTION:
                Pair<TextView, AutoCompleteTextView> nodes = x.addListNodeCategory(layout, R.id.category_filter_toggle);
                categoryText = nodes.first;
                filterAutoCompleteTxt = nodes.second;
                break;
            case SPLIT:
            case TRANSFER:
                categoryText = x.addListNodePlus(layout, R.id.category, R.id.category_add, R.string.category, R.string.select_category);
                break;
            case PLAIN:
                categoryText = x.addListNode(layout, R.id.category, R.string.category, R.string.select_category);
                break;
            case PARENT:
                categoryText = x.addListNode(layout, R.id.category, R.string.parent, R.string.select_category);
                break;
            default:
                throw new IllegalArgumentException("unknown type: " + type);
        }
        categoryText.setText(R.string.no_category);
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
            selectCategory(id, false);
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
                if (!CategorySelectorActivity.pickCategory(activity, selectedCategoryId, excludingSubTreeId, showSplitCategory)) {
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
        }
    }

    public void onSelectedId(int id, long selectedId) {
        if (id == R.id.category) {
            selectCategory(selectedId);
        }
    }

    public long getSelectedCategoryId() {
        return selectedCategoryId;
    }

    public void selectCategory(long categoryId) {
        selectCategory(categoryId, true);
    }

    public void selectCategory(long categoryId, boolean selectLast) {
        if (selectedCategoryId != categoryId) {
            Category category = db.getCategoryWithParent(categoryId);
            if (category != null) {
                categoryText.setText(Category.getTitle(category.title, category.level));
                selectedCategoryId = categoryId;
                if (listener != null) {
                    listener.onCategorySelected(category, selectLast);
                }
            }
        }
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

    public void onDestroy() {
        if (autoCompleteAdapter != null) autoCompleteAdapter.changeCursor(null);
    }

    public interface CategorySelectorListener {
        void onCategorySelected(Category category, boolean selectLast);
    }


    public enum SelectorType {
        PLAIN, TRANSACTION, SPLIT, TRANSFER, PARENT
    }

}
