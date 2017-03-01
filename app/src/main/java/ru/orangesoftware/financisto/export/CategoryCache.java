/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.export;

import android.database.sqlite.SQLiteDatabase;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.CategoryTree;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static ru.orangesoftware.financisto.utils.Utils.isEmpty;

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 5/29/12 2:25 PM
 */
public class CategoryCache {

    public static String extractCategoryName(String name) {
        int i = name.indexOf('/');
        if (i != -1) {
            name = name.substring(0, i);
        }
        return name;
    }

    public Map<String, Category> categoryNameToCategory = new HashMap<String, Category>();
    public CategoryTree<Category> categoryTree = new CategoryTree<Category>();
    private AtomicLong seq = new AtomicLong(1);

    private boolean freshStart = true;
    
    public void loadExistingCategories(DatabaseAdapter db) {
        categoryTree = db.getCategoriesTree(false);
        long maxId = updateNameToCategoryMapping(categoryTree, 0);
        seq = new AtomicLong(maxId+1);
        freshStart = false;
    }

    private long updateNameToCategoryMapping(CategoryTree<Category> categoryTree, long maxId) {
        for (Category category : categoryTree) {
            String name = CategoryInfo.buildName(category);
            categoryNameToCategory.put(name, category);
            if (category.id > maxId) {
                maxId = category.id;
            }
            if (category.hasChildren()) {
                long childMaxId = updateNameToCategoryMapping(category.children, maxId);
                if (childMaxId > maxId) {
                    maxId = childMaxId;
                }
            }
        }
        return maxId;
    }

    public void insertCategories(DatabaseAdapter dbAdapter, Set<? extends CategoryInfo> categories) {
        for (CategoryInfo category : categories) {
            String name = extractCategoryName(category.name);
            insertCategory(name, category.isIncome);
        }
        if (freshStart) {
            categoryTree.sortByTitle();
        } else {
            categoryTree.reIndex();
        }
        SQLiteDatabase database = dbAdapter.db();
        database.beginTransaction();
        try {
            dbAdapter.insertCategoryTreeInTransaction(categoryTree);
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    private Category insertCategory(String name, boolean isIncome) {
        if (isChildCategory(name)) {
            return insertChildCategory(name, isIncome);
        } else {
            return insertRootCategory(name, isIncome);
        }
    }

    private boolean isChildCategory(String name) {
        return name.contains(":");
    }

    private Category insertRootCategory(String name, boolean income) {
        Category c = categoryNameToCategory.get(name);
        if (c == null) {
            c = createCategoryInCache(name, name, income);
            categoryTree.add(c);
        }
        return c;
    }

    private Category createCategoryInCache(String fullName, String name, boolean income) {
        Category c = new Category();
        c.id = seq.getAndIncrement();
        c.title = name;
        if (income) {
            c.makeThisCategoryIncome();
        }
        categoryNameToCategory.put(fullName, c);
        return c;
    }

    private Category insertChildCategory(String name, boolean income) {
        int i = name.lastIndexOf(':');
        String parentCategoryName = name.substring(0, i);
        String childCategoryName = name.substring(i+1);
        Category parent = insertCategory(parentCategoryName, income);
        Category child = categoryNameToCategory.get(name);
        if (child == null) {
            child = createCategoryInCache(name, childCategoryName, income);
            parent.addChild(child);
        }
        return child;
    }

    public Category findCategory(String category) {
        if (isEmpty(category)) {
            return null;
        }
        String name = extractCategoryName(category);
        return categoryNameToCategory.get(name);
    }

}
