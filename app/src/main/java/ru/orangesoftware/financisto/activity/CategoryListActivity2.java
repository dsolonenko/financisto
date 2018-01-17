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

import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Map;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.adapter.CategoryListAdapter2;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.CategoryTree;

public class CategoryListActivity2 extends AbstractListActivity {

    private static final int NEW_CATEGORY_REQUEST = 1;
    private static final int EDIT_CATEGORY_REQUEST = 2;

    public CategoryListActivity2() {
        super(R.layout.category_list);
    }

    private CategoryTree<Category> categories;
    private Map<Long, String> attributes;

    @Override
    protected void internalOnCreate(Bundle savedInstanceState) {
        super.internalOnCreate(savedInstanceState);
        categories = db.getCategoriesTree(false);
        attributes = db.getAllAttributesMap();
        ImageButton b = findViewById(R.id.bAttributes);
        b.setOnClickListener(v -> {
            Intent intent = new Intent(CategoryListActivity2.this, AttributeListActivity.class);
            startActivityForResult(intent, 0);
        });
        b = findViewById(R.id.bCollapseAll);
        b.setOnClickListener(v -> ((CategoryListAdapter2) adapter).collapseAllCategories());
        b = findViewById(R.id.bExpandAll);
        b.setOnClickListener(v -> ((CategoryListAdapter2) adapter).expandAllCategories());
        b = findViewById(R.id.bSort);
        b.setOnClickListener(v -> sortByTitle());
        b = findViewById(R.id.bFix);
        b.setOnClickListener(v -> reIndex());
    }

    private void sortByTitle() {
        if (categories.sortByTitle()) {
            db.updateCategoryTree(categories);
            recreateCursor();
        }
    }

    private void reIndex() {
        db.restoreSystemEntities();
        recreateCursor();
    }

    @Override
    protected void addItem() {
        Intent intent = new Intent(CategoryListActivity2.this, CategoryActivity.class);
        startActivityForResult(intent, NEW_CATEGORY_REQUEST);
    }

    @Override
    protected ListAdapter createAdapter(Cursor cursor) {
        CategoryListAdapter2 a = new CategoryListAdapter2(this, categories);
        a.setAttributes(attributes);
        return a;
    }

    @Override
    protected Cursor createCursor() {
        return null;
    }

    @Override
    public void recreateCursor() {
        long t0 = System.currentTimeMillis();
        categories = db.getCategoriesTree(false);
        attributes = db.getAllAttributesMap();
        updateAdapter();
        long t1 = System.currentTimeMillis();
        Log.d("CategoryListActivity2", "Requery in " + (t1 - t0) + "ms");
    }

    private void updateAdapter() {
        ((CategoryListAdapter2) adapter).setCategories(categories);
        ((CategoryListAdapter2) adapter).setAttributes(attributes);
        notifyDataSetChanged();
    }

    @Override
    protected void deleteItem(View v, int position, final long id) {
        Category c = (Category) getListAdapter().getItem(position);
        new AlertDialog.Builder(this)
                .setTitle(c.getTitle())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage(R.string.delete_category_dialog)
                .setPositiveButton(R.string.yes, (arg0, arg1) -> {
                    db.deleteCategory(id);
                    recreateCursor();
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    @Override
    public void editItem(View v, int position, long id) {
        Intent intent = new Intent(CategoryListActivity2.this, CategoryActivity.class);
        intent.putExtra(CategoryActivity.CATEGORY_ID_EXTRA, id);
        startActivityForResult(intent, EDIT_CATEGORY_REQUEST);
    }

    @Override
    protected void viewItem(View v, final int position, long id) {
        final Category c = (Category) getListAdapter().getItem(position);
        final ArrayList<PositionAction> actions = new ArrayList<>();
        Category p = c.parent;
        CategoryTree<Category> categories;
        if (p == null) {
            categories = this.categories;
        } else {
            categories = p.children;
        }
        final int pos = categories.indexOf(c);
        if (pos > 0) {
            actions.add(top);
            actions.add(up);
        }
        if (pos < categories.size() - 1) {
            actions.add(down);
            actions.add(bottom);
        }
        if (c.hasChildren()) {
            actions.add(sortByTitle);
        }
        final ListAdapter a = new CategoryPositionListAdapter(actions);
        final CategoryTree<Category> tree = categories;
        new AlertDialog.Builder(this)
                .setTitle(c.getTitle())
                .setAdapter(a, (dialog, which) -> {
                    PositionAction action = actions.get(which);
                    if (action.execute(tree, pos)) {
                        db.updateCategoryTree(tree);
                        notifyDataSetChanged();
                    }
                })
                .show();
    }

    protected void notifyDataSetChanged() {
        ((CategoryListAdapter2) adapter).notifyDataSetChanged();
    }

    protected void notifyDataSetInvalidated() {
        ((CategoryListAdapter2) adapter).notifyDataSetInvalidated();
    }

    private abstract class PositionAction {
        final int icon;
        final int title;

        public PositionAction(int icon, int title) {
            this.icon = icon;
            this.title = title;
        }

        public abstract boolean execute(CategoryTree<Category> tree, int pos);
    }

    private final PositionAction top = new PositionAction(R.drawable.ic_btn_round_top, R.string.position_move_top) {
        @Override
        public boolean execute(CategoryTree<Category> tree, int pos) {
            return tree.moveCategoryToTheTop(pos);
        }
    };

    private final PositionAction up = new PositionAction(R.drawable.ic_btn_round_up, R.string.position_move_up) {
        @Override
        public boolean execute(CategoryTree<Category> tree, int pos) {
            return tree.moveCategoryUp(pos);
        }
    };

    private final PositionAction down = new PositionAction(R.drawable.ic_btn_round_down, R.string.position_move_down) {
        @Override
        public boolean execute(CategoryTree<Category> tree, int pos) {
            return tree.moveCategoryDown(pos);
        }
    };

    private final PositionAction bottom = new PositionAction(R.drawable.ic_btn_round_bottom, R.string.position_move_bottom) {
        @Override
        public boolean execute(CategoryTree<Category> tree, int pos) {
            return tree.moveCategoryToTheBottom(pos);
        }
    };

    private final PositionAction sortByTitle = new PositionAction(R.drawable.ic_btn_round_sort_by_title, R.string.sort_by_title) {
        @Override
        public boolean execute(CategoryTree<Category> tree, int pos) {
            return tree.sortByTitle();
        }
    };

    private class CategoryPositionListAdapter extends BaseAdapter {

        private final ArrayList<PositionAction> actions;

        public CategoryPositionListAdapter(ArrayList<PositionAction> actions) {
            this.actions = actions;
        }

        @Override
        public int getCount() {
            return actions.size();
        }

        @Override
        public PositionAction getItem(int position) {
            return actions.get(position);
        }

        @Override
        public long getItemId(int position) {
            return actions.get(position).hashCode();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.position_list_item, parent, false);
            }
            ImageView v = convertView.findViewById(R.id.icon);
            TextView t = convertView.findViewById(R.id.line1);
            PositionAction a = actions.get(position);
            v.setImageResource(a.icon);
            t.setText(a.title);
            return convertView;
        }

    }

}
