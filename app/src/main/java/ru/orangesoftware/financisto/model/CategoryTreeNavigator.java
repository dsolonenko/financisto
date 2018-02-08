/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.model;

import java.util.List;
import java.util.Map;
import java.util.Stack;
import ru.orangesoftware.financisto.db.DatabaseAdapter;

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 3/18/12 8:21 PM
 */
public class CategoryTreeNavigator {

    public static final long INCOME_CATEGORY_ID = -101;
    public static final long EXPENSE_CATEGORY_ID = -102;

    private final DatabaseAdapter db;
    private final Stack<CategoryTree<Category>> categoriesStack = new Stack<>();
    private final long excludedTreeId;

    public CategoryTree<Category> categories;
    public long selectedCategoryId = 0;

    public CategoryTreeNavigator(DatabaseAdapter db) {
        this(db, -1);
    }

    public CategoryTreeNavigator(DatabaseAdapter db, long excludedTreeId) {
        this.db = db;
        this.excludedTreeId = excludedTreeId;
        this.categories = db.getCategoriesTreeWithoutSubTree(excludedTreeId, false);
        Category noCategory = db.getCategoryWithParent(Category.NO_CATEGORY_ID);
        tagCategories(noCategory);
    }

    public void selectCategory(long selectedCategoryId) {
        Map<Long, Category> map = categories.asMap();
        Category selectedCategory = map.get(selectedCategoryId);
        if (selectedCategory != null) {
            Stack<Long> path = new Stack<>();
            Category parent = selectedCategory.parent;
            while (parent != null) {
                path.push(parent.id);
                parent = parent.parent;
            }
            while (!path.isEmpty()) {
                navigateTo(path.pop());
            }
            this.selectedCategoryId = selectedCategoryId;
        }
    }

    public void tagCategories(Category parent) {
        if (categories.size() > 0 && categories.getAt(0).id != parent.id) {
            Category copy = new Category();
            copy.id = parent.id;
            copy.title = parent.title;
            if (parent.isIncome()) {
                copy.makeThisCategoryIncome();
            }
            categories.insertAtTop(copy);
        }
        StringBuilder sb = new StringBuilder();
        for (Category c : categories) {
            if (c.tag == null && c.hasChildren()) {
                sb.setLength(0);
                CategoryTree<Category> children = c.children;
                for (Category child : children) {
                    if (sb.length() > 0) {
                        sb.append(",");
                    }
                    sb.append(child.title);
                }
                c.tag = sb.toString();
            }
        }
    }

    public boolean goBack() {
        if (!categoriesStack.isEmpty()) {
            Category selectedCategory = findCategory(selectedCategoryId);
            if (selectedCategory != null) {
                selectedCategoryId = selectedCategory.getParentId();
            }
            categories = categoriesStack.pop();
            return true;
        }
        return false;
    }

    public boolean canGoBack() {
        return !categoriesStack.isEmpty();
    }

    public boolean navigateTo(long categoryId) {
        Category selectedCategory = findCategory(categoryId);
        if (selectedCategory != null) {
            selectedCategoryId = selectedCategory.id;
            if (selectedCategory.hasChildren()) {
                categoriesStack.push(categories);
                categories = selectedCategory.children;
                tagCategories(selectedCategory);
                return true;
            }
        }
        return false;
    }

    private Category findCategory(long categoryId) {
        for (Category category : categories) {
            if (category.id == categoryId) {
                return category;
            }
        }
        return null;
    }

    public boolean isSelected(long categoryId) {
        return selectedCategoryId == categoryId;
    }
    
    public List<Category> getSelectedRoots() {
        return categories.getRoots();
    }

    public void addSplitCategoryToTheTop() {
        Category splitCategory = db.getCategoryWithParent(Category.SPLIT_CATEGORY_ID);
        categories.insertAtTop(splitCategory);
    }

    public void separateIncomeAndExpense() {
        CategoryTree<Category> newCategories = new CategoryTree<>();
        Category income = new Category();
        income.id = INCOME_CATEGORY_ID;
        income.makeThisCategoryIncome();
        income.title = "<INCOME>";
        Category expense = new Category();
        expense.id = EXPENSE_CATEGORY_ID;
        expense.makeThisCategoryExpense();
        expense.title = "<EXPENSE>";
        for (Category category : categories) {
            if (category.id <= 0) {
                newCategories.add(category);
            } else {
                if (category.isIncome()) {
                    income.addChild(category);
                } else {
                    expense.addChild(category);
                }
            }
        }
        if (income.hasChildren()) {
            newCategories.add(income);
        }
        if (expense.hasChildren()) {
            newCategories.add(expense);
        }
        categories = newCategories;
    }

    public long getExcludedTreeId() {
        return excludedTreeId;
    }
}
