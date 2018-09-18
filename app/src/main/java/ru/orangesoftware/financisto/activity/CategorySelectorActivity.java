package ru.orangesoftware.financisto.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.adapter.BlotterListAdapter;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.CategoryTree;
import ru.orangesoftware.financisto.model.CategoryTreeNavigator;
import ru.orangesoftware.financisto.utils.MenuItemInfo;
import ru.orangesoftware.financisto.utils.MyPreferences;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CategorySelectorActivity extends AbstractListActivity {

    public static final String SELECTED_CATEGORY_ID = "SELECTED_CATEGORY_ID";
    public static final String EXCLUDED_SUB_TREE_ID = "EXCLUDED_SUB_TREE_ID";
    public static final String INCLUDE_SPLIT_CATEGORY = "INCLUDE_SPLIT_CATEGORY";

    private int incomeColor;
    private int expenseColor;

    private CategoryTreeNavigator navigator;
    private Map<Long, String> attributes;

    private Button bBack;

    public CategorySelectorActivity() {
        super(R.layout.category_selector);
        enablePin = false;
    }

    @Override
    protected void internalOnCreate(Bundle savedInstanceState) {
        Resources resources = getResources();
        this.incomeColor = resources.getColor(R.color.category_type_income);
        this.expenseColor = resources.getColor(R.color.category_type_expense);

        long excTreeId = -1;
        if (getIntent() != null) {
            excTreeId = getIntent().getLongExtra(EXCLUDED_SUB_TREE_ID, -1);
        }
        navigator = new CategoryTreeNavigator(db, excTreeId);
        if (MyPreferences.isSeparateIncomeExpense(this)) {
            navigator.separateIncomeAndExpense();
        }
        attributes = db.getAllAttributesMap();

        bBack = findViewById(R.id.bBack);
        bBack.setOnClickListener(view -> {
            if (navigator.goBack()) {
                recreateAdapter();
            }
        });
        Button bSelect = findViewById(R.id.bSelect);
        bSelect.setOnClickListener(view -> confirmSelection());
        
        Intent intent = getIntent();
        if (intent != null) {
            boolean includeSplit = intent.getBooleanExtra(INCLUDE_SPLIT_CATEGORY, false);
            if (includeSplit) {
                navigator.addSplitCategoryToTheTop();
            }
            navigator.selectCategory(intent.getLongExtra(SELECTED_CATEGORY_ID, 0));
        }
        
    }

    private void confirmSelection() {
        Intent data = new Intent();
        data.putExtra(SELECTED_CATEGORY_ID, navigator.selectedCategoryId);
        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    protected List<MenuItemInfo> createContextMenus(long id) {
        return Collections.emptyList();
    }

    @Override
    protected Cursor createCursor() {
        return null;
    }

    @Override
    protected ListAdapter createAdapter(Cursor cursor) {
        bBack.setEnabled(navigator.canGoBack());
        return new CategoryAdapter(navigator.categories);
    }

    @Override
    protected void deleteItem(View v, int position, long id) {
    }

    @Override
    protected void editItem(View v, int position, long id) {
    }

    @Override
    protected void viewItem(View v, int position, long id) {
        if (navigator.navigateTo(id)) {
            recreateAdapter();
        } else {
            if (MyPreferences.isAutoSelectChildCategory(this)) {
                confirmSelection();
            }
        }
    }

    public static boolean pickCategory(Activity activity, boolean forceHierSelector, long selectedId, long excludingTreeId, boolean includeSplit) {
        if (forceHierSelector || MyPreferences.isUseHierarchicalCategorySelector(activity)) {
            Intent intent = new Intent(activity, CategorySelectorActivity.class);
            intent.putExtra(CategorySelectorActivity.SELECTED_CATEGORY_ID, selectedId);
            intent.putExtra(CategorySelectorActivity.EXCLUDED_SUB_TREE_ID, excludingTreeId);
            intent.putExtra(CategorySelectorActivity.INCLUDE_SPLIT_CATEGORY, includeSplit);
            activity.startActivityForResult(intent, R.id.category_pick);
            return true;
        }
        return false;
    }

    private class CategoryAdapter extends BaseAdapter {

        private final CategoryTree<Category> categories;

        private CategoryAdapter(CategoryTree<Category> categories) {
            this.categories = categories;
        }

        @Override
        public int getCount() {
            return categories.size();
        }

        @Override
        public Category getItem(int i) {
            return categories.getAt(i);
        }

        @Override
        public long getItemId(int i) {
            return getItem(i).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            BlotterListAdapter.BlotterViewHolder v;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.blotter_list_item, parent, false);
                v = new BlotterListAdapter.BlotterViewHolder(convertView);
                convertView.setTag(v);
            } else {
                v = (BlotterListAdapter.BlotterViewHolder)convertView.getTag();
            }
            Category c = getItem(position);
            if (c.id == CategoryTreeNavigator.INCOME_CATEGORY_ID) {
                v.centerView.setText(getString(R.string.income));                
            } else if (c.id == CategoryTreeNavigator.EXPENSE_CATEGORY_ID) {
                v.centerView.setText(getString(R.string.expense));
            } else {
                v.centerView.setText(c.title);
            }
            v.bottomView.setText(c.tag);
            v.indicator.setBackgroundColor(c.isIncome() ? incomeColor : expenseColor);
            v.rightCenterView.setVisibility(View.INVISIBLE);
            v.iconView.setVisibility(View.INVISIBLE);
            if (attributes != null && attributes.containsKey(c.id)) {
                v.rightView.setText(attributes.get(c.id));
                v.rightView.setVisibility(View.VISIBLE);
            } else {
                v.rightView.setVisibility(View.GONE);
            }
            v.topView.setVisibility(View.INVISIBLE);
            if (navigator.isSelected(c.id)) {
                v.layout.setBackgroundResource(R.drawable.list_selector_background_focus);
            } else {
                v.layout.setBackgroundResource(0);
            }
            return convertView;
        }

    }
    

}
