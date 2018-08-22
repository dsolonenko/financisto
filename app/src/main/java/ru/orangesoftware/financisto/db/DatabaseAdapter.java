/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 *     Abdsandryk - implement getAllExpenses method for bill filtering
 ******************************************************************************/
package ru.orangesoftware.financisto.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;
import org.androidannotations.annotations.EBean;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.blotter.BlotterFilter;
import ru.orangesoftware.financisto.datetime.DateUtils;
import ru.orangesoftware.financisto.db.DatabaseHelper.*;
import ru.orangesoftware.financisto.filter.Criteria;
import ru.orangesoftware.financisto.filter.WhereFilter;
import ru.orangesoftware.financisto.model.*;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.rates.*;
import ru.orangesoftware.financisto.utils.ArrUtils;
import ru.orangesoftware.financisto.utils.StringUtil;

import java.math.BigDecimal;
import java.util.*;

import static ru.orangesoftware.financisto.db.DatabaseHelper.*;
import static ru.orangesoftware.financisto.db.DatabaseHelper.SmsTemplateColumns.*;
import static ru.orangesoftware.financisto.utils.StringUtil.generateQueryPlaceholders;

@EBean(scope = EBean.Scope.Singleton)
public class DatabaseAdapter extends MyEntityManager {

    private boolean updateAccountBalance = true;

    public DatabaseAdapter(Context context) {
        super(context);
    }

    public void open() {
    }

    public void close() {
    }

    // ===================================================================
    // ACCOUNT
    // ===================================================================

    private static final String UPDATE_ORPHAN_TRANSACTIONS_1 = "UPDATE " + TRANSACTION_TABLE + " SET " +
            TransactionColumns.to_account_id + "=0, " +
            TransactionColumns.to_amount + "=0 " +
            "WHERE " + TransactionColumns.to_account_id + "=?";
    private static final String UPDATE_ORPHAN_TRANSACTIONS_2 = "UPDATE " + TRANSACTION_TABLE + " SET " +
            TransactionColumns.from_account_id + "=" + TransactionColumns.to_account_id + ", " +
            TransactionColumns.from_amount + "=" + TransactionColumns.to_amount + ", " +
            TransactionColumns.to_account_id + "=0, " +
            TransactionColumns.to_amount + "=0, " +
            TransactionColumns.parent_id + "=0 " +
            "WHERE " + TransactionColumns.from_account_id + "=? AND " +
            TransactionColumns.to_account_id + ">0";

    public int deleteAccount(long id) {
        SQLiteDatabase db = db();
        db.beginTransaction();
        try {
            String[] sid = new String[]{String.valueOf(id)};
            db.execSQL(UPDATE_ORPHAN_TRANSACTIONS_1, sid);
            db.execSQL(UPDATE_ORPHAN_TRANSACTIONS_2, sid);
            db.delete(TRANSACTION_ATTRIBUTE_TABLE, TransactionAttributeColumns.TRANSACTION_ID
                    + " in (SELECT _id from " + TRANSACTION_TABLE + " where " + TransactionColumns.from_account_id + "=?)", sid);
            db.delete(TRANSACTION_TABLE, TransactionColumns.from_account_id + "=?", sid);
            int count = db.delete(ACCOUNT_TABLE, "_id=?", sid);
            db.setTransactionSuccessful();
            return count;
        } finally {
            db.endTransaction();
        }

    }

    // ===================================================================
    // TRANSACTION
    // ===================================================================

    public Transaction getTransaction(long id) {
        Transaction t = get(Transaction.class, id);
        if (t != null) {
            t.systemAttributes = getSystemAttributesForTransaction(id);
            if (t.isSplitParent()) {
                t.splits = getSplitsForTransaction(t.id);
            }
            return t;
        }
        return new Transaction();
    }

    public Cursor getBlotter(WhereFilter filter) {
        String view = filter.isEmpty() ? V_BLOTTER : V_BLOTTER_FLAT_SPLITS;
        return getBlotter(view, filter);
    }

    public Cursor getBlotterForAccount(WhereFilter filter) {
        WhereFilter accountFilter = enhanceFilterForAccountBlotter(filter);
        return getBlotter(V_BLOTTER_FOR_ACCOUNT_WITH_SPLITS, accountFilter);
    }

    public static WhereFilter enhanceFilterForAccountBlotter(WhereFilter filter) {
        WhereFilter accountFilter = WhereFilter.copyOf(filter);
        accountFilter.put(Criteria.raw(BlotterColumns.parent_id + "=0 OR " + BlotterColumns.is_transfer + "=-1"));
        return accountFilter;
    }

    public Cursor getBlotterForAccountWithSplits(WhereFilter filter) {
        return getBlotter(V_BLOTTER_FOR_ACCOUNT_WITH_SPLITS, filter);
    }

    private Cursor getBlotter(String view, WhereFilter filter) {
        long t0 = System.currentTimeMillis();
        try {
            String sortOrder = getBlotterSortOrder(filter);
            return db().query(view, BlotterColumns.NORMAL_PROJECTION,
                    filter.getSelection(), filter.getSelectionArgs(), null, null,
                    sortOrder);
        } finally {
            long t1 = System.currentTimeMillis();
            Log.i("DB", "getBlotter " + (t1 - t0) + "ms");
        }
    }

    private String getBlotterSortOrder(WhereFilter filter) {
        String sortOrder = filter.getSortOrder();
        if (sortOrder == null || sortOrder.length() == 0) {
            sortOrder = BlotterFilter.SORT_NEWER_TO_OLDER + "," + BlotterFilter.SORT_NEWER_TO_OLDER_BY_ID;
        } else {
            if (sortOrder.contains(BlotterFilter.SORT_NEWER_TO_OLDER)) {
                sortOrder += "," + BlotterFilter.SORT_NEWER_TO_OLDER_BY_ID;
            } else {
                sortOrder += "," + BlotterFilter.SORT_OLDER_TO_NEWER_BY_ID;
            }
        }
        return sortOrder;
    }

    public Cursor getAllTemplates(WhereFilter filter, String sortBy) {
        long t0 = System.currentTimeMillis();
        try {
            return db().query(V_ALL_TRANSACTIONS, BlotterColumns.NORMAL_PROJECTION,
                    filter.getSelection(), filter.getSelectionArgs(), null, null,
                    sortBy);
        } finally {
            long t1 = System.currentTimeMillis();
            Log.i("DB", "getBlotter " + (t1 - t0) + "ms");
        }
    }

    public Cursor getBlotterWithSplits(String where) {
        return db().query(V_BLOTTER_FOR_ACCOUNT_WITH_SPLITS, BlotterColumns.NORMAL_PROJECTION, where, null, null, null,
                BlotterColumns.datetime + " DESC");
    }

    private static final String LOCATION_COUNT_UPDATE = "UPDATE " + LOCATIONS_TABLE
            + " SET count=count+(?) WHERE _id=?";

    private void updateLocationCount(long locationId, int count) {
        db().execSQL(LOCATION_COUNT_UPDATE, new Object[]{count, locationId});
    }

    private static final String ACCOUNT_LAST_CATEGORY_UPDATE = "UPDATE " + ACCOUNT_TABLE
            + " SET " + AccountColumns.LAST_CATEGORY_ID + "=? "
            + " WHERE " + AccountColumns.ID + "=?";

    private static final String ACCOUNT_LAST_ACCOUNT_UPDATE = "UPDATE " + ACCOUNT_TABLE
            + " SET " + AccountColumns.LAST_ACCOUNT_ID + "=? "
            + " WHERE " + AccountColumns.ID + "=?";

    private static final String PAYEE_LAST_CATEGORY_UPDATE = "UPDATE " + PAYEE_TABLE
            + " SET last_category_id=(?) WHERE _id=?";

    private static final String CATEGORY_LAST_LOCATION_UPDATE = "UPDATE " + CATEGORY_TABLE
            + " SET last_location_id=(?) WHERE _id=?";

    private static final String CATEGORY_LAST_PROJECT_UPDATE = "UPDATE " + CATEGORY_TABLE
            + " SET last_project_id=(?) WHERE _id=?";

    private void updateLastUsed(Transaction t) {
        SQLiteDatabase db = db();
        if (t.isTransfer()) {
            db.execSQL(ACCOUNT_LAST_ACCOUNT_UPDATE, new Object[]{t.toAccountId, t.fromAccountId});
        }
        db.execSQL(ACCOUNT_LAST_CATEGORY_UPDATE, new Object[]{t.categoryId, t.fromAccountId});
        db.execSQL(PAYEE_LAST_CATEGORY_UPDATE, new Object[]{t.categoryId, t.payeeId});
        db.execSQL(CATEGORY_LAST_LOCATION_UPDATE, new Object[]{t.locationId, t.categoryId});
        db.execSQL(CATEGORY_LAST_PROJECT_UPDATE, new Object[]{t.projectId, t.categoryId});
    }

    public long duplicateTransaction(long id) {
        return duplicateTransaction(id, 0, 1);
    }

    public long duplicateTransactionWithMultiplier(long id, int multiplier) {
        return duplicateTransaction(id, 0, multiplier);
    }

    public long duplicateTransactionAsTemplate(long id) {
        return duplicateTransaction(id, 1, 1);
    }

    private long duplicateTransaction(long id, int isTemplate, int multiplier) {
        SQLiteDatabase db = db();
        db.beginTransaction();
        try {
            long now = System.currentTimeMillis();
            Transaction transaction = getTransaction(id);
            if (transaction.isSplitChild()) {
                id = transaction.parentId;
                transaction = getTransaction(id);
            }
            transaction.lastRecurrence = now;
            updateTransaction(transaction);
            transaction.id = -1;
            transaction.isTemplate = isTemplate;
            transaction.dateTime = now;
            transaction.remoteKey = null;
            if (isTemplate == 0) {
                transaction.recurrence = null;
                transaction.notificationOptions = null;
            }
            if (multiplier > 1) {
                transaction.fromAmount *= multiplier;
                transaction.toAmount *= multiplier;
            }
            long transactionId = insertTransaction(transaction);
            Map<Long, String> attributesMap = getAllAttributesForTransaction(id);
            LinkedList<TransactionAttribute> attributes = new LinkedList<TransactionAttribute>();
            for (long attributeId : attributesMap.keySet()) {
                TransactionAttribute ta = new TransactionAttribute();
                ta.attributeId = attributeId;
                ta.value = attributesMap.get(attributeId);
                attributes.add(ta);
            }
            if (attributes.size() > 0) {
                insertAttributes(transactionId, attributes);
            }
            List<Transaction> splits = getSplitsForTransaction(id);
            if (multiplier > 1) {
                for (Transaction split : splits) {
                    split.fromAmount *= multiplier;
                    split.remoteKey = null;
                }
            }
            transaction.id = transactionId;
            transaction.splits = splits;
            insertSplits(transaction);
            db.setTransactionSuccessful();
            return transactionId;
        } finally {
            db.endTransaction();
        }
    }

    public long insertOrUpdate(Transaction transaction) {
        return insertOrUpdate(transaction, Collections.<TransactionAttribute>emptyList());
    }

    public long insertOrUpdate(Transaction transaction, List<TransactionAttribute> attributes) {
        SQLiteDatabase db = db();
        db.beginTransaction();
        try {
            long id = insertOrUpdateInTransaction(transaction, attributes);
            db.setTransactionSuccessful();
            return id;
        } finally {
            db.endTransaction();
        }
    }

    public long insertOrUpdateInTransaction(Transaction transaction, List<TransactionAttribute> attributes) {
        long transactionId;
        transaction.lastRecurrence = System.currentTimeMillis();
        if (transaction.id == -1) {
            transactionId = insertTransaction(transaction);
        } else {
            updateTransaction(transaction);
            transactionId = transaction.id;
            db().delete(TRANSACTION_ATTRIBUTE_TABLE, TransactionAttributeColumns.TRANSACTION_ID + "=?",
                    new String[]{String.valueOf(transactionId)});
            deleteSplitsForParentTransaction(transactionId);
        }
        if (attributes != null) {
            insertAttributes(transactionId, attributes);
        }
        transaction.id = transactionId;
        insertSplits(transaction);
        updateAccountLastTransactionDate(transaction.fromAccountId);
        updateAccountLastTransactionDate(transaction.toAccountId);
        return transactionId;
    }

    public void insertWithoutUpdatingBalance(Transaction transaction) {
        updateAccountBalance = false;
        try {
            transaction.id = insertTransaction(transaction);
            insertSplits(transaction);
        } finally {
            updateAccountBalance = true;
        }
    }

    private void insertAttributes(long transactionId, List<TransactionAttribute> attributes) {
        for (TransactionAttribute a : attributes) {
            a.transactionId = transactionId;
            ContentValues values = a.toValues();
            db().insert(TRANSACTION_ATTRIBUTE_TABLE, null, values);
        }
    }

    private void insertAttributes(long transactionId, Map<Long, String> categoryAttributes) {
        if (categoryAttributes != null && categoryAttributes.size() > 0) {
            List<TransactionAttribute> attributes = new LinkedList<TransactionAttribute>();
            for (Map.Entry<Long, String> e : categoryAttributes.entrySet()) {
                TransactionAttribute a = new TransactionAttribute();
                a.attributeId = e.getKey();
                a.value = e.getValue();
                attributes.add(a);
            }
            insertAttributes(transactionId, attributes);
        }
    }

    private void insertSplits(Transaction parent) {
        List<Transaction> splits = parent.splits;
        if (splits != null) {
            for (Transaction split : splits) {
                split.id = -1;
                split.parentId = parent.id;
                split.dateTime = parent.dateTime;
                split.fromAccountId = parent.fromAccountId;
                split.payeeId = parent.payeeId;
                split.isTemplate = parent.isTemplate;
                split.status = parent.status;
                updateSplitOriginalAmount(parent, split);
                long splitId = insertTransaction(split);
                insertAttributes(splitId, split.categoryAttributes);
            }
        }
    }

    private void updateSplitOriginalAmount(Transaction parent, Transaction split) {
        if (parent.originalCurrencyId > 0) {
            split.originalCurrencyId = parent.originalCurrencyId;
            split.originalFromAmount = split.fromAmount;
            split.fromAmount = calculateAmountInAccountCurrency(parent, split.fromAmount);
        }
    }

    private long calculateAmountInAccountCurrency(Transaction parent, long amount) {
        double rate = getRateFromParent(parent);
        return (long) (rate * amount);
    }

    private double getRateFromParent(Transaction parent) {
        if (parent.originalFromAmount != 0) {
            return Math.abs(1.0 * parent.fromAmount / parent.originalFromAmount);
        }
        return 0;
    }

    private long insertTransaction(Transaction t) {
        t.updatedOn = System.currentTimeMillis();
        long id = db().insert(TRANSACTION_TABLE, null, t.toValues());
        if (updateAccountBalance) {
            if (!t.isTemplateLike()) {
                if (t.isSplitChild()) {
                    if (t.isTransfer()) {
                        updateToAccountBalance(t, id);
                    }
                } else {
                    updateFromAccountBalance(t, id);
                    updateToAccountBalance(t, id);
                    updateLocationCount(t.locationId, 1);
                    updateLastUsed(t);
                }
            }
        }
        return id;
    }

    private void updateFromAccountBalance(Transaction t, long id) {
        updateAccountBalance(t.fromAccountId, t.fromAmount);
        insertRunningBalance(t.fromAccountId, id, t.dateTime, t.fromAmount, t.fromAmount);
    }

    private void updateToAccountBalance(Transaction t, long id) {
        updateAccountBalance(t.toAccountId, t.toAmount);
        insertRunningBalance(t.toAccountId, id, t.dateTime, t.toAmount, t.toAmount);
    }

    private void updateTransaction(Transaction t) {
        Transaction oldT = null;
        if (t.isNotTemplateLike()) {
            oldT = getTransaction(t.id);
            updateAccountBalance(oldT.fromAccountId, oldT.fromAmount, t.fromAccountId, t.fromAmount);
            updateAccountBalance(oldT.toAccountId, oldT.toAmount, t.toAccountId, t.toAmount);
            updateRunningBalance(oldT, t);
            if (oldT.locationId != t.locationId) {
                updateLocationCount(oldT.locationId, -1);
                updateLocationCount(t.locationId, 1);
            }
        }
        t.updatedOn = System.currentTimeMillis();
        db().update(TRANSACTION_TABLE, t.toValues(), TransactionColumns._id + "=?",
                new String[]{String.valueOf(t.id)});
        if (oldT != null) {
            updateAccountLastTransactionDate(oldT.fromAccountId);
            updateAccountLastTransactionDate(oldT.toAccountId);
        }
    }

    public void updateTransactionStatus(long id, TransactionStatus status) {
        Transaction t = getTransaction(id);
        t.status = status;
        updateTransaction(t);
    }

    public void deleteTransaction(long id) {
        SQLiteDatabase db = db();
        db.beginTransaction();
        try {
            deleteTransactionNoDbTransaction(id);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void deleteTransactionNoDbTransaction(long id) {
        Transaction t = getTransaction(id);
        if (t.isNotTemplateLike()) {
            revertFromAccountBalance(t);
            revertToAccountBalance(t);
            updateAccountLastTransactionDate(t.fromAccountId);
            updateAccountLastTransactionDate(t.toAccountId);
            updateLocationCount(t.locationId, -1);
        }
        String[] sid = new String[]{String.valueOf(id)};
        SQLiteDatabase db = db();
        db.delete(TRANSACTION_ATTRIBUTE_TABLE, TransactionAttributeColumns.TRANSACTION_ID + "=?", sid);
        db.delete(TRANSACTION_TABLE, TransactionColumns._id + "=?", sid);
        deleteSplitsForParentTransaction(id);
    }

    private void deleteSplitsForParentTransaction(long parentId) {
        List<Transaction> splits = getSplitsForTransaction(parentId);
        SQLiteDatabase db = db();
        for (Transaction split : splits) {
            if (split.isTransfer()) {
                revertToAccountBalance(split);
            }
            db.delete(TRANSACTION_ATTRIBUTE_TABLE, TransactionAttributeColumns.TRANSACTION_ID + "=?",
                    new String[]{String.valueOf(split.id)});
        }

        db.delete(TRANSACTION_TABLE, TransactionColumns.parent_id + "=?", new String[]{String.valueOf(parentId)});

    }

    private void revertFromAccountBalance(Transaction t) {
        updateAccountBalance(t.fromAccountId, -t.fromAmount);
        deleteRunningBalance(t.fromAccountId, t.id, t.fromAmount, t.dateTime);
    }

    private void revertToAccountBalance(Transaction t) {
        updateAccountBalance(t.toAccountId, -t.toAmount);
        deleteRunningBalance(t.toAccountId, t.id, t.toAmount, t.dateTime);
    }

    private void updateAccountBalance(long oldAccountId, long oldAmount, long newAccountId, long newAmount) {
        if (oldAccountId == newAccountId) {
            updateAccountBalance(newAccountId, newAmount - oldAmount);
        } else {
            updateAccountBalance(oldAccountId, -oldAmount);
            updateAccountBalance(newAccountId, newAmount);
        }
    }

    private static final String ACCOUNT_TOTAL_AMOUNT_UPDATE = "UPDATE " + ACCOUNT_TABLE
            + " SET " + AccountColumns.TOTAL_AMOUNT + "=" + AccountColumns.TOTAL_AMOUNT + "+(?) "
            + " WHERE " + AccountColumns.ID + "=?";

    private void updateAccountBalance(long accountId, long deltaAmount) {
        if (accountId <= 0) {
            return;
        }
        db().execSQL(ACCOUNT_TOTAL_AMOUNT_UPDATE, new Object[]{deltaAmount, accountId});
    }

    private static final String INSERT_RUNNING_BALANCE =
            "insert or replace into running_balance(account_id,transaction_id,datetime,balance) values (?,?,?,?)";

    private static final String UPDATE_RUNNING_BALANCE =
            "update running_balance set balance = balance+(?) where account_id = ? and datetime > ?";

    private static final String DELETE_RUNNING_BALANCE =
            "delete from running_balance where account_id = ? and transaction_id = ?";

    private void insertRunningBalance(long accountId, long transactionId, long datetime, long amount, long deltaAmount) {
        if (accountId <= 0) {
            return;
        }
        long previousTransactionBalance = fetchAccountBalanceAtTheTime(accountId, datetime);
        SQLiteDatabase db = db();
        db.execSQL(INSERT_RUNNING_BALANCE, new Object[]{accountId, transactionId, datetime, previousTransactionBalance + amount});
        db.execSQL(UPDATE_RUNNING_BALANCE, new Object[]{deltaAmount, accountId, datetime});
    }

    private void updateRunningBalance(Transaction oldTransaction, Transaction newTransaction) {
        deleteRunningBalance(oldTransaction.fromAccountId, oldTransaction.id, oldTransaction.fromAmount, oldTransaction.dateTime);
        insertRunningBalance(newTransaction.fromAccountId, newTransaction.id, newTransaction.dateTime,
                newTransaction.fromAmount, newTransaction.fromAmount);
        deleteRunningBalance(oldTransaction.toAccountId, oldTransaction.id, oldTransaction.toAmount, oldTransaction.dateTime);
        insertRunningBalance(newTransaction.toAccountId, newTransaction.id, newTransaction.dateTime,
                newTransaction.toAmount, newTransaction.toAmount);
    }

    private void deleteRunningBalance(long accountId, long transactionId, long amount, long dateTime) {
        if (accountId <= 0) {
            return;
        }
        SQLiteDatabase db = db();
        db.execSQL(DELETE_RUNNING_BALANCE, new Object[]{accountId, transactionId});
        db.execSQL(UPDATE_RUNNING_BALANCE, new Object[]{-amount, accountId, dateTime});
    }

    private long fetchAccountBalanceAtTheTime(long accountId, long datetime) {
        return DatabaseUtils.rawFetchLongValue(this, "select balance from running_balance where account_id = ? and datetime <= ? order by datetime desc, transaction_id desc limit 1",
                new String[]{String.valueOf(accountId), String.valueOf(datetime)});
    }

    // ===================================================================
    // CATEGORY
    // ===================================================================

    public long insertOrUpdate(Category category, List<Attribute> attributes) {
        SQLiteDatabase db = db();
        db.beginTransaction();
        try {
            long id;
            if (category.id == -1) {
                id = insertCategory(category);
            } else {
                updateCategory(category);
                id = category.id;
            }
            addAttributes(id, attributes);
            category.id = id;
            db.setTransactionSuccessful();
            return id;
        } finally {
            db.endTransaction();
        }
    }

    private void addAttributes(long categoryId, List<Attribute> attributes) {
        SQLiteDatabase db = db();
        db.delete(CATEGORY_ATTRIBUTE_TABLE, CategoryAttributeColumns.CATEGORY_ID + "=?", new String[]{String.valueOf(categoryId)});
        if (attributes != null) {
            ContentValues values = new ContentValues();
            values.put(CategoryAttributeColumns.CATEGORY_ID, categoryId);
            for (Attribute a : attributes) {
                values.put(CategoryAttributeColumns.ATTRIBUTE_ID, a.id);
                db.insert(CATEGORY_ATTRIBUTE_TABLE, null, values);
            }
        }
    }

    private long insertCategory(Category category) {
        CategoryTree<Category> tree = getCategoriesTree(false);
        long parentId = category.getParentId();
        if (parentId == Category.NO_CATEGORY_ID) {
            if (!tree.isEmpty()) {
                return insertAsLast(category, tree);
            }
        } else {
            Map<Long, Category> map = tree.asMap();
            Category parent = map.get(parentId);
            if (parent != null && parent.hasChildren()) {
                CategoryTree<Category> children = parent.children;
                return insertAsLast(category, children);
            }
        }
        return insertChildCategory(parentId, category);
    }

    private long insertAsLast(Category category, CategoryTree<Category> tree) {
        long mateId = tree.getAt(tree.size() - 1).id;
        return insertMateCategory(mateId, category);
    }

    private long updateCategory(Category category) {
        Category oldCategory = getCategoryWithParent(category.id);
        if (oldCategory.getParentId() == category.getParentId()) {
            updateCategory(category.id, category.title, category.type);
            updateChildCategoriesType(category.type, category.left, category.right);
        } else {
            moveCategory(category);
        }
        return category.id;
    }

    private void moveCategory(Category category) {
        CategoryTree<Category> tree = getCategoriesTree(false);
        Map<Long, Category> map = tree.asMap();
        Category oldCategory = map.get(category.id);
        if (oldCategory != null) {
            Category oldParent = map.get(oldCategory.getParentId());
            if (oldParent != null) {
                oldParent.removeChild(oldCategory);
            } else {
                tree.remove(oldCategory);
            }
            Category newParent = map.get(category.getParentId());
            int newCategoryType = category.type;
            if (newParent != null) {
                newParent.addChild(oldCategory);
                newCategoryType = newParent.type;
            } else {
                tree.add(oldCategory);
            }
            tree.reIndex();
            updateCategoryTreeInTransaction(tree);
            updateCategory(category.id, category.title, newCategoryType);
            updateChildCategoriesType(newCategoryType, oldCategory.left, oldCategory.right);
        }
    }


    private static final String GET_PARENT_SQL = "(SELECT "
            + "parent." + CategoryColumns._id + " AS " + CategoryColumns._id
            + " FROM "
            + CATEGORY_TABLE + " AS node" + ","
            + CATEGORY_TABLE + " AS parent "
            + " WHERE "
            + " node." + CategoryColumns.left + " BETWEEN parent." + CategoryColumns.left + " AND parent." + CategoryColumns.right
            + " AND node." + CategoryColumns._id + "=?"
            + " AND parent." + CategoryColumns._id + "!=?"
            + " ORDER BY parent." + CategoryColumns.left + " DESC)";

    public Category getCategoryWithParent(long id) {
        SQLiteDatabase db = db();
        try (Cursor c = db.query(V_CATEGORY, CategoryViewColumns.NORMAL_PROJECTION,
            CategoryViewColumns._id + "=?", new String[]{String.valueOf(id)}, null, null, null)) {
            if (c.moveToNext()) {
                Category cat = Category.formCursor(c);
                String s = String.valueOf(id);
                try (Cursor c2 = db.query(GET_PARENT_SQL, new String[]{CategoryColumns._id.name()}, null, new String[]{s, s}, null, null, null, "1")) {
                    if (c2.moveToFirst()) {
                        cat.parent = new Category(c2.getLong(0));
                    }
                }
                return cat;
            } else {
                return new Category(-1);
            }
        }
    }

    public List<Long> getCategoryIdsByLeftIds(List<String> leftIds) {
        SQLiteDatabase db = db();
        List<Long> res = new LinkedList<>();
        try (Cursor c = db.query(V_CATEGORY, new String[]{CategoryViewColumns._id.name()},
                CategoryViewColumns.left + " IN (" + generateQueryPlaceholders(leftIds.size()) + ")",
                ArrUtils.strListToArr(leftIds), null, null, null)) {
            while (c.moveToNext()) {
                res.add(c.getLong(0));
            }
        }
        return res;
    }
    
    public Category getCategoryByLeft(long left) {
        SQLiteDatabase db = db();
        try (Cursor c = db.query(V_CATEGORY, CategoryViewColumns.NORMAL_PROJECTION,
            CategoryViewColumns.left + "=?", new String[]{String.valueOf(left)}, null, null, null)) {
            if (c.moveToNext()) {
                return Category.formCursor(c);
            } else {
                return new Category(-1);
            }
        }
    }

    public CategoryTree<Category> getCategoriesTreeWithoutSubTree(long excludingTreeId, boolean includeNoCategory) {
        try (Cursor c = excludingTreeId > 0
            ? getCategoriesWithoutSubtree(excludingTreeId, includeNoCategory) : getCategories(includeNoCategory)) {
            return CategoryTree.createFromCursor(c, Category::formCursor);
        }
    }

    public CategoryTree<Category> getCategoriesTree(boolean includeNoCategory) {
        return getCategoriesTreeWithoutSubTree(-1, includeNoCategory);
    }

    public CategoryTree<Category> getAllCategoriesTree() {
        try (Cursor c = getAllCategories()) {
            return CategoryTree.createFromCursor(c, Category::formCursor);
        }
    }

    public Map<Long, Category> getAllCategoriesMap() {
        return getAllCategoriesTree().asMap();
    }

    public List<Category> getCategoriesList(boolean includeNoCategory) {
        try (Cursor c = getCategories(includeNoCategory)) {
            return categoriesAsList(c);
        }
    }

    public Cursor getAllCategories() {
        return db().query(V_CATEGORY, CategoryViewColumns.NORMAL_PROJECTION, null, null, null, null, null);
    }

    public List<Category> getAllCategoriesList() {
        try (Cursor c = getAllCategories()) {
            return categoriesAsList(c);
        }
    }

    private List<Category> categoriesAsList(Cursor c) {
        List<Category> list = new ArrayList<>();

        while (c.moveToNext()) {
            Category category = Category.formCursor(c);
            list.add(category);
        }
        return list;
    }

    public Cursor getCategories(boolean includeNoCategory) {
        return getCategories(includeNoCategory, null);
    }

    public Cursor filterCategories(CharSequence titleFilter) {
        return getCategories(false, titleFilter);
    }
        
    
    public Cursor getCategories(boolean includeNoCategory, CharSequence titleFilter) {
        String query = CategoryViewColumns._id + (includeNoCategory ? ">=0" : ">0");
        String[] args = null;
        if (titleFilter != null) {
            query += " and (" + CategoryViewColumns.title + " like ? or " + CategoryViewColumns.title + " like ? )";
            args = new String[]{
                    "%" + titleFilter + "%", 
                    "%" + StringUtil.capitalize(titleFilter.toString()) + "%"};
        }
        return db().query(V_CATEGORY, 
                CategoryViewColumns.NORMAL_PROJECTION,
                query,
                args, null, null, null);
    }

    public Cursor getCategoriesWithoutSubtree(long id, boolean includeNoCategory) {
        SQLiteDatabase db = db();
        long left = 0, right = 0;
        try (Cursor c = db.query(CATEGORY_TABLE, new String[]{CategoryColumns.left.name(), CategoryColumns.right.name()},
                CategoryColumns._id + "=?", new String[]{String.valueOf(id)},null,null,null)) {
            if (c.moveToFirst()) {
                left = c.getLong(0);
                right = c.getLong(1);
            }
        }
        return db.query(V_CATEGORY, CategoryViewColumns.NORMAL_PROJECTION,
                "(NOT (" + CategoryViewColumns.left + ">=? AND " + CategoryColumns.right + "<=?)) AND "
                    + CategoryViewColumns._id + (includeNoCategory ? ">=0" : ">0"),
                new String[]{String.valueOf(left), String.valueOf(right)}, null, null, null);
    }

    public List<Category> getCategoriesWithoutSubtreeAsList(long categoryId) {
        List<Category> list = new ArrayList<>();
        try (Cursor c = getCategoriesWithoutSubtree(categoryId, true)) {
            while (c.moveToNext()) {
                Category category = Category.formCursor(c);
                list.add(category);
            }
            return list;
        }
    }

    private static final String INSERT_CATEGORY_UPDATE_RIGHT = "UPDATE " + CATEGORY_TABLE + " SET " + CategoryColumns.right + "=" + CategoryColumns.right + "+2 WHERE " + CategoryColumns.right + ">?";
    private static final String INSERT_CATEGORY_UPDATE_LEFT = "UPDATE " + CATEGORY_TABLE + " SET " + CategoryColumns.left + "=" + CategoryColumns.left + "+2 WHERE " + CategoryColumns.left + ">?";

    public long insertChildCategory(long parentId, Category category) {
        //DECLARE v_leftkey INT UNSIGNED DEFAULT 0;
        //SELECT l INTO v_leftkey FROM `nset` WHERE `id` = ParentID;
        //UPDATE `nset` SET `r` = `r` + 2 WHERE `r` > v_leftkey;
        //UPDATE `nset` SET `l` = `l` + 2 WHERE `l` > v_leftkey;
        //INSERT INTO `nset` (`name`, `l`, `r`) VALUES (NodeName, v_leftkey + 1, v_leftkey + 2);
        int type = getActualCategoryType(parentId, category);
        return insertCategory(CategoryColumns.left.name(), parentId, category.title, type);
    }

    public long insertMateCategory(long categoryId, Category category) {
        //DECLARE v_rightkey INT UNSIGNED DEFAULT 0;
        //SELECT `r` INTO v_rightkey FROM `nset` WHERE `id` = MateID;
        //UPDATE `	nset` SET `r` = `r` + 2 WHERE `r` > v_rightkey;
        //UPDATE `nset` SET `l` = `l` + 2 WHERE `l` > v_rightkey;
        //INSERT `nset` (`name`, `l`, `r`) VALUES (NodeName, v_rightkey + 1, v_rightkey + 2);
        Category mate = getCategoryWithParent(categoryId);
        long parentId = mate.getParentId();
        int type = getActualCategoryType(parentId, category);
        return insertCategory(CategoryColumns.right.name(), categoryId, category.title, type);
    }

    private int getActualCategoryType(long parentId, Category category) {
        int type = category.type;
        if (parentId > 0) {
            Category parent = getCategoryWithParent(parentId);
            type = parent.type;
        }
        return type;
    }

    private long insertCategory(String field, long categoryId, String title, int type) {
        int num = 0;
        SQLiteDatabase db = db();
        Cursor c = db.query(CATEGORY_TABLE, new String[]{field},
                CategoryColumns._id + "=?", new String[]{String.valueOf(categoryId)}, null, null, null);
        try {
            if (c.moveToFirst()) {
                num = c.getInt(0);
            }
        } finally {
            c.close();
        }
        String[] args = new String[]{String.valueOf(num)};
        db.execSQL(INSERT_CATEGORY_UPDATE_RIGHT, args);
        db.execSQL(INSERT_CATEGORY_UPDATE_LEFT, args);
        ContentValues values = new ContentValues();
        values.put(CategoryColumns.title.name(), title);
        int left = num + 1;
        int right = num + 2;
        values.put(CategoryColumns.left.name(), left);
        values.put(CategoryColumns.right.name(), right);
        values.put(CategoryColumns.type.name(), type);
        long id = db.insert(CATEGORY_TABLE, null, values);
        updateChildCategoriesType(type, left, right);
        return id;
    }

    private static final String CATEGORY_UPDATE_CHILDREN_TYPES = "UPDATE " + CATEGORY_TABLE + " SET " + CategoryColumns.type + "=? WHERE " + CategoryColumns.left + ">? AND " + CategoryColumns.right + "<?";

    private void updateChildCategoriesType(int type, int left, int right) {
        db().execSQL(CATEGORY_UPDATE_CHILDREN_TYPES, new Object[]{type, left, right});
    }

    private static final String DELETE_CATEGORY_UPDATE1 = "UPDATE " + TRANSACTION_TABLE
            + " SET " + TransactionColumns.category_id + "=0 WHERE "
            + TransactionColumns.category_id + " IN ("
            + "SELECT " + CategoryColumns._id + " FROM " + CATEGORY_TABLE + " WHERE "
            + CategoryColumns.left + " BETWEEN ? AND ?)";
    private static final String DELETE_CATEGORY_UPDATE2 = "UPDATE " + CATEGORY_TABLE
            + " SET " + CategoryColumns.left + "=(CASE WHEN " + CategoryColumns.left + ">%s THEN "
            + CategoryColumns.left + "-%s ELSE " + CategoryColumns.left + " END),"
            + CategoryColumns.right + "=" + CategoryColumns.right + "-%s"
            + " WHERE " + CategoryColumns.right + ">%s";

    public void deleteCategory(long categoryId) {
        //DECLARE v_leftkey, v_rightkey, v_width INT DEFAULT 0;
        //
        //SELECT
        //	`l`, `r`, `r` - `l` + 1 INTO v_leftkey, v_rightkey, v_width
        //FROM `nset`
        //WHERE
        //	`id` = NodeID;
        //
        //DELETE FROM `nset` WHERE `l` BETWEEN v_leftkey AND v_rightkey;
        //
        //UPDATE `nset`
        //SET
        //	`l` = IF(`l` > v_leftkey, `l` - v_width, `l`),
        //	`r` = `r` - v_width
        //WHERE
        //	`r` > v_rightkey;
        SQLiteDatabase db = db();
        int left = 0, right = 0;
        Cursor c = db.query(CATEGORY_TABLE, new String[]{CategoryColumns.left.name(), CategoryColumns.right.name()},
                CategoryColumns._id + "=?", new String[]{String.valueOf(categoryId)}, null, null, null);
        try {
            if (c.moveToFirst()) {
                left = c.getInt(0);
                right = c.getInt(1);
            }
        } finally {
            c.close();
        }
        db.beginTransaction();
        try {
            int width = right - left + 1;
            String[] args = new String[]{String.valueOf(left), String.valueOf(right)};
            db.execSQL(DELETE_CATEGORY_UPDATE1, args);
            db.delete(CATEGORY_TABLE, CategoryColumns.left + " BETWEEN ? AND ?", args);
            db.execSQL(String.format(DELETE_CATEGORY_UPDATE2, left, width, width, right));
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private void updateCategory(long id, String title, int type) {
        ContentValues values = new ContentValues();
        values.put(CategoryColumns.title.name(), title);
        values.put(CategoryColumns.type.name(), type);
        db().update(CATEGORY_TABLE, values, CategoryColumns._id + "=?", new String[]{String.valueOf(id)});
    }

    public void insertCategoryTreeInTransaction(CategoryTree<Category> tree) {
        db().delete("category", "_id > 0", null);
        insertCategoryInTransaction(tree);
        updateCategoryTreeInTransaction(tree);
    }

    private void insertCategoryInTransaction(CategoryTree<Category> tree) {
        for (Category category : tree) {
            reInsertEntity(category);
            if (category.hasChildren()) {
                insertCategoryInTransaction(category.children);
            }
        }
    }

    public void updateCategoryTree(CategoryTree<Category> tree) {
        SQLiteDatabase db = db();
        db.beginTransaction();
        try {
            updateCategoryTreeInTransaction(tree);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private static final String WHERE_CATEGORY_ID = CategoryColumns._id + "=?";

    private void updateCategoryTreeInTransaction(CategoryTree<Category> tree) {
        int left = 1;
        int right = 2;
        ContentValues values = new ContentValues();
        String[] sid = new String[1];
        for (Category c : tree) {
            values.put(CategoryColumns.left.name(), c.left);
            values.put(CategoryColumns.right.name(), c.right);
            sid[0] = String.valueOf(c.id);
            db().update(CATEGORY_TABLE, values, WHERE_CATEGORY_ID, sid);
            if (c.hasChildren()) {
                updateCategoryTreeInTransaction(c.children);
            }
            if (c.left < left) {
                left = c.left;
            }
            if (c.right > right) {
                right = c.right;
            }
        }
        values.put(CategoryColumns.left.name(), left - 1);
        values.put(CategoryColumns.right.name(), right + 1);
        sid[0] = String.valueOf(Category.NO_CATEGORY_ID);
        db().update(CATEGORY_TABLE, values, WHERE_CATEGORY_ID, sid);
    }

    // ===================================================================
    // SMS TEMPLATES >>
    // ===================================================================

    public List<SmsTemplate> getSmsTemplatesForCategory(long categoryId) {
        try (Cursor c = db().query(SMS_TEMPLATES_TABLE, NORMAL_PROJECTION, category_id + "=?",
                new String[]{String.valueOf(categoryId)}, null, null, title.name())) {
            List<SmsTemplate> res = new ArrayList<>(c.getCount());
            while (c.moveToNext()) {
                SmsTemplate a = SmsTemplate.fromCursor(c);
                res.add(a);
            }
            return res;
        }
    }

    public List<SmsTemplate> getSmsTemplatesByNumber(String smsNumber) {
        try (Cursor c = db().rawQuery(
                String.format("select %s from %s where %s=? order by %s, length(%s) desc",
                    DatabaseUtils.generateSelectClause(NORMAL_PROJECTION, null),
                    SMS_TEMPLATES_TABLE, title, sort_order, template), new String[]{smsNumber})) {
            List<SmsTemplate> res = new ArrayList<>(c.getCount());
            while (c.moveToNext()) {
                SmsTemplate a = SmsTemplate.fromCursor(c);
                res.add(a);
            }
            return res;
        }
    }

    public Set<String> findAllSmsTemplateNumbers() {
        try (Cursor c = db().rawQuery("select distinct " + title + " from " + SMS_TEMPLATES_TABLE +
                " where " + SmsTemplateColumns.template + " is not null", null)) {
            Set<String> res = new HashSet<>(c.getCount());
            while (c.moveToNext()) {
                res.add(c.getString(0));
            }
            return res;
        }
    }

    public Cursor getAllSmsTemplates() {
        return db().query(SMS_TEMPLATES_TABLE, NORMAL_PROJECTION,
                SmsTemplateColumns.template + " is not null", null, null, null, title.name());
    }

    public Cursor getSmsTemplatesWithFullInfo() {
        return getSmsTemplatesWithFullInfo(null);
    }
    
    public Cursor getSmsTemplatesWithFullInfo(final String filter) {
        String nativeQuery = String.format(
                "select %s, c.%s as %s, c.%s as %s " +
                "from %s t left outer join %s c on t.%s = c.%s ",
                DatabaseUtils.generateSelectClause(NORMAL_PROJECTION, "t"),
                CategoryViewColumns.title, SmsTemplateListColumns.cat_name, CategoryViewColumns.level, SmsTemplateListColumns.cat_level,
                SMS_TEMPLATES_TABLE,
                V_CATEGORY,
                category_id, CategoryViewColumns._id);
        if (!StringUtil.isEmpty(filter)) {
            nativeQuery += String.format("where t.%s like '%%%s%%' or t.%s like '%%%2$s%%' ",
                    CategoryViewColumns.title, filter, SmsTemplateColumns.template);
        }
        nativeQuery += "order by t." + sort_order;
        
        return db().rawQuery(nativeQuery, new String[]{});
    }

    public long duplicateSmsTemplateBelowOriginal(long id) {
        long newId = duplicate(SmsTemplate.class, id);
        long nextOrderItem = getNextByOrder(SmsTemplate.class, id);
        if (nextOrderItem > 0) {
            moveItemByChangingOrder(SmsTemplate.class, newId, nextOrderItem);
        }
        return newId;
    }

    // ===================================================================
    // ATTRIBUTES
    // ===================================================================

    public ArrayList<Attribute> getAttributesForCategory(long categoryId) {
        try (Cursor c = db().query(V_ATTRIBUTES, AttributeColumns.NORMAL_PROJECTION,
                CategoryAttributeColumns.CATEGORY_ID + "=?", new String[]{String.valueOf(categoryId)},
                null, null, AttributeColumns.TITLE)) {
            ArrayList<Attribute> list = new ArrayList<Attribute>(c.getCount());
            while (c.moveToNext()) {
                Attribute a = Attribute.fromCursor(c);
                list.add(a);
            }
            return list;
        }
    }

    public ArrayList<Attribute> getAllAttributesForCategory(long categoryId) {
        Category category = getCategoryWithParent(categoryId);
        try (Cursor c = db().query(V_ATTRIBUTES, AttributeColumns.NORMAL_PROJECTION,
                AttributeViewColumns.CATEGORY_LEFT + "<= ? AND " + AttributeViewColumns.CATEGORY_RIGHT + " >= ?",
                new String[]{String.valueOf(category.left), String.valueOf(category.right)},
                null, null, AttributeColumns.TITLE)) {
            ArrayList<Attribute> list = new ArrayList<>(c.getCount());
            while (c.moveToNext()) {
                Attribute a = Attribute.fromCursor(c);
                list.add(a);
            }
            return list;
        }
    }

    public Attribute getSystemAttribute(SystemAttribute a) {
        Attribute sa = getAttribute(a.id);
        sa.title = context.getString(a.titleId);
        return sa;
    }

    public Attribute getAttribute(long id) {
        try (Cursor c = db().query(ATTRIBUTES_TABLE, AttributeColumns.NORMAL_PROJECTION,
                AttributeColumns.ID + "=?", new String[]{String.valueOf(id)},
                null, null, null)) {
            if (c.moveToFirst()) {
                return Attribute.fromCursor(c);
            }
        }
        return new Attribute();
    }

    public long insertOrUpdate(Attribute attribute) {
        if (attribute.id == -1) {
            return insertAttribute(attribute);
        } else {
            updateAttribute(attribute);
            return attribute.id;
        }
    }

    public void deleteAttribute(long id) {
        SQLiteDatabase db = db();
        db.beginTransaction();
        try {
            Attribute attr = getAttribute(id);
            String[] p = new String[]{String.valueOf(id)};
            db.delete(ATTRIBUTES_TABLE, AttributeColumns.ID + "=?", p);
            db.delete(CATEGORY_ATTRIBUTE_TABLE, CategoryAttributeColumns.ATTRIBUTE_ID + "=?", p);
            db.delete(TRANSACTION_ATTRIBUTE_TABLE, TransactionAttributeColumns.ATTRIBUTE_ID + "=?", p);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private long insertAttribute(Attribute attribute) {
        return db().insert(ATTRIBUTES_TABLE, null, attribute.toValues());
    }

    private void updateAttribute(Attribute attribute) {
        db().update(ATTRIBUTES_TABLE, attribute.toValues(), AttributeColumns.ID + "=?", new String[]{String.valueOf(attribute.id)});
    }

    public Cursor getAllAttributes() {
        return db().query(ATTRIBUTES_TABLE, AttributeColumns.NORMAL_PROJECTION,
                AttributeColumns.ID + ">0", null, null, null, AttributeColumns.TITLE);
    }

    public Map<Long, String> getAllAttributesMap() {
        try (Cursor c = db().query(V_ATTRIBUTES, AttributeViewColumns.NORMAL_PROJECTION, null, null, null, null,
                AttributeViewColumns.CATEGORY_ID + ", " + AttributeViewColumns.TITLE)) {
            HashMap<Long, String> attributes = new HashMap<Long, String>();
            StringBuilder sb = null;
            long prevCategoryId = -1;
            while (c.moveToNext()) {
                long categoryId = c.getLong(AttributeViewColumns.Indicies.CATEGORY_ID);
                String name = c.getString(AttributeViewColumns.Indicies.NAME);
                if (prevCategoryId != categoryId) {
                    if (sb != null) {
                        attributes.put(prevCategoryId, sb.append("]").toString());
                        sb.setLength(1);
                    } else {
                        sb = new StringBuilder();
                        sb.append("[");
                    }
                    prevCategoryId = categoryId;
                }
                if (sb.length() > 1) {
                    sb.append(", ");
                }
                sb.append(name);
            }
            if (sb != null) {
                attributes.put(prevCategoryId, sb.append("]").toString());
            }
            return attributes;
        }
    }

    public Map<Long, String> getAllAttributesForTransaction(long transactionId) {
        Cursor c = db().query(TRANSACTION_ATTRIBUTE_TABLE, TransactionAttributeColumns.NORMAL_PROJECTION,
                TransactionAttributeColumns.TRANSACTION_ID + "=? AND " + TransactionAttributeColumns.ATTRIBUTE_ID + ">=0",
                new String[]{String.valueOf(transactionId)},
                null, null, null);
        try {
            HashMap<Long, String> attributes = new HashMap<Long, String>();
            while (c.moveToNext()) {
                long attributeId = c.getLong(TransactionAttributeColumns.Indicies.ATTRIBUTE_ID);
                String value = c.getString(TransactionAttributeColumns.Indicies.VALUE);
                attributes.put(attributeId, value);
            }
            return attributes;
        } finally {
            c.close();
        }
    }

    public EnumMap<SystemAttribute, String> getSystemAttributesForTransaction(long transactionId) {
        Cursor c = db().query(TRANSACTION_ATTRIBUTE_TABLE, TransactionAttributeColumns.NORMAL_PROJECTION,
                TransactionAttributeColumns.TRANSACTION_ID + "=? AND " + TransactionAttributeColumns.ATTRIBUTE_ID + "<0",
                new String[]{String.valueOf(transactionId)},
                null, null, null);
        try {
            EnumMap<SystemAttribute, String> attributes = new EnumMap<SystemAttribute, String>(SystemAttribute.class);
            while (c.moveToNext()) {
                long attributeId = c.getLong(TransactionAttributeColumns.Indicies.ATTRIBUTE_ID);
                String value = c.getString(TransactionAttributeColumns.Indicies.VALUE);
                attributes.put(SystemAttribute.forId(attributeId), value);
            }
            return attributes;
        } finally {
            c.close();
        }
    }


    /**
     * Sets status=CL (Cleared) for the selected transactions
     *
     * @param ids selected transactions' ids
     */
    public void clearSelectedTransactions(long[] ids) {
        String sql = "UPDATE " + TRANSACTION_TABLE + " SET " + TransactionColumns.status + "='" + TransactionStatus.CL + "'";
        runInTransaction(sql, ids);
    }

    /**
     * Sets status=RC (Reconciled) for the selected transactions
     *
     * @param ids selected transactions' ids
     */
    public void reconcileSelectedTransactions(long[] ids) {
        String sql = "UPDATE " + TRANSACTION_TABLE + " SET " + TransactionColumns.status + "='" + TransactionStatus.RC + "'";
        runInTransaction(sql, ids);
    }

    /**
     * Deletes the selected transactions
     *
     * @param ids selected transactions' ids
     */
    public void deleteSelectedTransactions(long[] ids) {
        SQLiteDatabase db = db();
        db.beginTransaction();
        try {
            for (long id : ids) {
                deleteTransactionNoDbTransaction(id);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private void runInTransaction(String sql, long[] ids) {
        SQLiteDatabase db = db();
        db.beginTransaction();
        try {
            int count = ids.length;
            int bucket = 100;
            int num = 1 + count / bucket;
            for (int i = 0; i < num; i++) {
                int x = bucket * i;
                int y = Math.min(count, bucket * (i + 1));
                String script = createSql(sql, ids, x, y);
                db.execSQL(script);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private String createSql(String updateSql, long[] ids, int x, int y) {
        StringBuilder sb = new StringBuilder(updateSql)
                .append(" WHERE ")
                .append(TransactionColumns.is_template)
                .append("=0 AND ")
                .append(TransactionColumns.parent_id)
                .append("=0 AND ")
                .append(TransactionColumns._id)
                .append(" IN (");
        for (int i = x; i < y; i++) {
            if (i > x) {
                sb.append(",");
            }
            sb.append(ids[i]);
        }
        sb.append(")");
        return sb.toString();
    }

    private static final String UPDATE_LAST_RECURRENCE =
            "UPDATE " + TRANSACTION_TABLE + " SET " + TransactionColumns.last_recurrence + "=? WHERE " + TransactionColumns._id + "=?";

    public long[] storeMissedSchedules(List<RestoredTransaction> restored, long now) {
        SQLiteDatabase db = db();
        db.beginTransaction();
        try {
            int count = restored.size();
            long[] restoredIds = new long[count];
            HashMap<Long, Transaction> transactions = new HashMap<Long, Transaction>();
            for (int i = 0; i < count; i++) {
                RestoredTransaction rt = restored.get(i);
                long transactionId = rt.transactionId;
                Transaction t = transactions.get(transactionId);
                if (t == null) {
                    t = getTransaction(transactionId);
                    transactions.put(transactionId, t);
                }
                t.id = -1;
                t.dateTime = rt.dateTime.getTime();
                t.status = TransactionStatus.RS;
                t.isTemplate = 0;
                restoredIds[i] = insertOrUpdate(t);
                t.id = transactionId;
            }
            for (Transaction t : transactions.values()) {
                db.execSQL(UPDATE_LAST_RECURRENCE, new Object[]{now, t.id});
            }
            db.setTransactionSuccessful();
            return restoredIds;
        } finally {
            db.endTransaction();
        }
    }

    /**
     * @param accountId
     * @param period
     * @return
     */
    public int getCustomClosingDay(long accountId, int period) {
        String where = CreditCardClosingDateColumns.ACCOUNT_ID + "=? AND " +
                CreditCardClosingDateColumns.PERIOD + "=?";

        Cursor c = db().query(CCARD_CLOSING_DATE_TABLE, new String[]{CreditCardClosingDateColumns.CLOSING_DAY},
                where, new String[]{Long.toString(accountId), Integer.toString(period)}, null, null, null);

        int res = 0;
        try {
            if (c != null) {
                if (c.getCount() > 0) {
                    c.moveToFirst();
                    res = c.getInt(0);
                } else {
                    res = 0;
                }
            } else {
                // there is no custom closing day in database for the given account id an period
                res = 0;
            }
        } catch (SQLiteException e) {
            res = 0;
        } finally {
            c.close();
        }
        return res;
    }


    public void setCustomClosingDay(long accountId, int period, int closingDay) {
        ContentValues values = new ContentValues();
        values.put(CreditCardClosingDateColumns.ACCOUNT_ID, Long.toString(accountId));
        values.put(CreditCardClosingDateColumns.PERIOD, Integer.toString(period));
        values.put(CreditCardClosingDateColumns.CLOSING_DAY, Integer.toString(closingDay));
        db().insert(CCARD_CLOSING_DATE_TABLE, null, values);
    }

    public void deleteCustomClosingDay(long accountId, int period) {
        String where = CreditCardClosingDateColumns.ACCOUNT_ID + "=? AND " +
                CreditCardClosingDateColumns.PERIOD + "=?";
        String[] args = new String[]{Long.toString(accountId), Integer.toString(period)};
        db().delete(CCARD_CLOSING_DATE_TABLE, where, args);
    }

    public void updateCustomClosingDay(long accountId, int period, int closingDay) {
        // delete previous content
        deleteCustomClosingDay(accountId, period);

        // save new value
        setCustomClosingDay(accountId, period, closingDay);
    }

    /**
     * Re-populates running_balance table for all accounts
     */
    public void rebuildRunningBalances() {
        List<Account> accounts = getAllAccountsList();
        for (Account account : accounts) {
            rebuildRunningBalanceForAccount(account);
        }
    }

    /**
     * Re-populates running_balance for specific account
     *
     * @param account selected account
     */
    public void rebuildRunningBalanceForAccount(Account account) {
        SQLiteDatabase db = db();
        db.beginTransaction();
        try {
            String accountId = String.valueOf(account.getId());
            db.execSQL("delete from running_balance where account_id=?", new Object[]{accountId});
            WhereFilter filter = new WhereFilter("");
            filter.put(Criteria.eq(BlotterFilter.FROM_ACCOUNT_ID, accountId));
            filter.asc("datetime");
            filter.asc("_id");
            Object[] values = new Object[4];
            values[0] = accountId;
            try (Cursor c = getBlotterForAccountWithSplits(filter)) {
                long balance = 0;
                while (c.moveToNext()) {
                    long parentId = c.getLong(BlotterColumns.parent_id.ordinal());
                    int isTransfer = c.getInt(BlotterColumns.is_transfer.ordinal());
                    if (parentId > 0) {
                        if (isTransfer >= 0) {
                            // we only interested in the second part of the transfer-split
                            // which is marked with is_transfer=-1 (see v_blotter_for_account_with_splits)
                            continue;
                        }
                    }
                    long fromAccountId = c.getLong(BlotterColumns.from_account_id.ordinal());
                    long toAccountId = c.getLong(BlotterColumns.to_account_id.ordinal());
                    if (toAccountId > 0 && toAccountId == fromAccountId) {
                        // weird bug when a transfer is done from an account to the same account
                        continue;
                    }
                    balance += c.getLong(BlotterColumns.from_amount.ordinal());
                    values[1] = c.getString(BlotterColumns._id.ordinal());
                    values[2] = c.getString(BlotterColumns.datetime.ordinal());
                    values[3] = balance;
                    db.execSQL("insert into running_balance(account_id,transaction_id,datetime,balance) values (?,?,?,?)", values);
                }
            }
            updateAccountLastTransactionDate(account.id);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private static final String[] SUM_FROM_AMOUNT = new String[]{"sum(from_amount)"};

    public long fetchBudgetBalance(Map<Long, Category> categories, Map<Long, Project> projects, Budget b) {
        String where = Budget.createWhere(b, categories, projects);
        Cursor c = db().query(V_BLOTTER_FOR_ACCOUNT_WITH_SPLITS, SUM_FROM_AMOUNT, where, null, null, null, null);
        try {
            if (c.moveToNext()) {
                return c.getLong(0);
            }
        } finally {
            c.close();
        }
        return 0;
    }

    public void recalculateAccountsBalances() {
        SQLiteDatabase db = db();
        db.beginTransaction();
        try {
            for (Account account : getAllAccountsList()) {
                recalculateAccountBalances(account.id);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private void recalculateAccountBalances(long accountId) {
        TransactionsTotalCalculator calculator = new TransactionsTotalCalculator(this,
                enhanceFilterForAccountBlotter(WhereFilter.empty()
                        .eq(BlotterFilter.FROM_ACCOUNT_ID, String.valueOf(accountId))));
        Total total = calculator.getAccountTotal();
        ContentValues values = new ContentValues();
        values.put(AccountColumns.TOTAL_AMOUNT, total.balance);
        db().update(ACCOUNT_TABLE, values, AccountColumns.ID + "=?", new String[]{String.valueOf(accountId)});
        Log.i("DatabaseImport", "Recalculating amount for " + accountId);
    }

    private long fetchAccountBalance(long accountId) {
        try (Cursor c = db().query(V_BLOTTER_FOR_ACCOUNT_WITH_SPLITS, new String[]{"SUM(" + BlotterColumns.from_amount + ")"},
                BlotterColumns.from_account_id + "=? and (" + BlotterColumns.parent_id + "=0 or " + BlotterColumns.is_transfer + "=-1)",
                new String[]{String.valueOf(accountId)}, null, null, null)) {
            if (c.moveToFirst()) {
                return c.getLong(0);
            }
            return 0;
        }
    }

    public void saveRate(ExchangeRate r) {
        replaceRate(r, r.date);
    }

    public void replaceRate(ExchangeRate rate, long originalDate) {
        SQLiteDatabase db = db();
        db.beginTransaction();
        try {
            replaceRateInTransaction(rate, originalDate, db);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private void replaceRateInTransaction(ExchangeRate rate, long originalDate, SQLiteDatabase db) {
        deleteRateInTransaction(rate.fromCurrencyId, rate.toCurrencyId, originalDate, db);
        saveBothRatesInTransaction(rate, db);
    }

    private void saveBothRatesInTransaction(ExchangeRate r, SQLiteDatabase db) {
        r.date = DateUtils.atMidnight(r.date);
        saveRateInTransaction(db, r);
        saveRateInTransaction(db, r.flip());
    }

    private void saveRateInTransaction(SQLiteDatabase db, ExchangeRate r) {
        db.insert(EXCHANGE_RATES_TABLE, null, r.toValues());
    }

    public void saveDownloadedRates(List<ExchangeRate> downloadedRates) {
        SQLiteDatabase db = db();
        db.beginTransaction();
        try {
            for (ExchangeRate r : downloadedRates) {
                if (r.isOk()) {
                    replaceRateInTransaction(r, r.date, db);
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public ExchangeRate findRate(Currency fromCurrency, Currency toCurrency, long date) {
        long day = DateUtils.atMidnight(date);
        Cursor c = db().query(EXCHANGE_RATES_TABLE, ExchangeRateColumns.NORMAL_PROJECTION, ExchangeRateColumns.NORMAL_PROJECTION_WHERE,
                new String[]{String.valueOf(fromCurrency.id), String.valueOf(toCurrency.id), String.valueOf(day)}, null, null, null);
        try {
            if (c.moveToFirst()) {
                return ExchangeRate.fromCursor(c);
            }
        } finally {
            c.close();
        }
        return null;
    }

    public List<ExchangeRate> findRates(Currency fromCurrency) {
        List<ExchangeRate> rates = new ArrayList<ExchangeRate>();
        Cursor c = db().query(EXCHANGE_RATES_TABLE, ExchangeRateColumns.NORMAL_PROJECTION, ExchangeRateColumns.from_currency_id + "=?",
                new String[]{String.valueOf(fromCurrency.id)}, null, null, ExchangeRateColumns.rate_date + " desc");
        try {
            while (c.moveToNext()) {
                rates.add(ExchangeRate.fromCursor(c));
            }
        } finally {
            c.close();
        }
        return rates;
    }

    public List<ExchangeRate> findRates(Currency fromCurrency, Currency toCurrency) {
        List<ExchangeRate> rates = new ArrayList<ExchangeRate>();
        Cursor c = db().query(EXCHANGE_RATES_TABLE, ExchangeRateColumns.NORMAL_PROJECTION,
                ExchangeRateColumns.from_currency_id + "=? and " + ExchangeRateColumns.to_currency_id + "=?",
                new String[]{String.valueOf(fromCurrency.id), String.valueOf(toCurrency.id)},
                null, null, ExchangeRateColumns.rate_date + " desc");
        try {
            while (c.moveToNext()) {
                rates.add(ExchangeRate.fromCursor(c));
            }
        } finally {
            c.close();
        }
        return rates;
    }

    public ExchangeRateProvider getLatestRates() {
        LatestExchangeRates m = new LatestExchangeRates();
        Cursor c = db().query(EXCHANGE_RATES_TABLE, ExchangeRateColumns.LATEST_RATE_PROJECTION, null, null, ExchangeRateColumns.LATEST_RATE_GROUP_BY, null, null);
        fillRatesCollection(m, c);
        return m;
    }

    public ExchangeRateProvider getHistoryRates() {
        HistoryExchangeRates m = new HistoryExchangeRates();
        Cursor c = db().query(EXCHANGE_RATES_TABLE, ExchangeRateColumns.NORMAL_PROJECTION, null, null, null, null, null);
        fillRatesCollection(m, c);
        return m;
    }

    private void fillRatesCollection(ExchangeRatesCollection m, Cursor c) {
        try {
            while (c.moveToNext()) {
                ExchangeRate r = ExchangeRate.fromCursor(c);
                m.addRate(r);
            }
        } finally {
            c.close();
        }
    }

    public void deleteRate(ExchangeRate rate) {
        deleteRate(rate.fromCurrencyId, rate.toCurrencyId, rate.date);
    }

    public void deleteRate(long fromCurrencyId, long toCurrencyId, long date) {
        SQLiteDatabase db = db();
        db.beginTransaction();
        try {
            deleteRateInTransaction(fromCurrencyId, toCurrencyId, date, db);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private void deleteRateInTransaction(long fromCurrencyId, long toCurrencyId, long date, SQLiteDatabase db) {
        long d = DateUtils.atMidnight(date);
        db.delete(EXCHANGE_RATES_TABLE, ExchangeRateColumns.DELETE_CLAUSE,
                new String[]{String.valueOf(fromCurrencyId), String.valueOf(toCurrencyId), String.valueOf(d)});
        db.delete(EXCHANGE_RATES_TABLE, ExchangeRateColumns.DELETE_CLAUSE,
                new String[]{String.valueOf(toCurrencyId), String.valueOf(fromCurrencyId), String.valueOf(d)});
    }

    public Total getAccountsTotalInHomeCurrency() {
        Currency homeCurrency = getHomeCurrency();
        return getAccountsTotal(homeCurrency);
    }

    /**
     * Calculates total in every currency for all accounts
     */
    public Total[] getAccountsTotal() {
        List<Account> accounts = getAllAccountsList();
        Map<Currency, Total> totalsMap = new HashMap<Currency, Total>();
        for (Account account : accounts) {
            if (account.shouldIncludeIntoTotals()) {
                Currency currency = account.currency;
                Total total = totalsMap.get(currency);
                if (total == null) {
                    total = new Total(currency);
                    totalsMap.put(currency, total);
                }
                total.balance += account.totalAmount;
            }
        }
        Collection<Total> values = totalsMap.values();
        return values.toArray(new Total[values.size()]);
    }

    /**
     * Calculates total in home currency for all accounts
     */
    public Total getAccountsTotal(Currency homeCurrency) {
        ExchangeRateProvider rates = getLatestRates();
        List<Account> accounts = getAllAccountsList();
        BigDecimal total = BigDecimal.ZERO;
        for (Account account : accounts) {
            if (account.shouldIncludeIntoTotals()) {
                if (account.currency.id == homeCurrency.id) {
                    total = total.add(BigDecimal.valueOf(account.totalAmount));
                } else {
                    ExchangeRate rate = rates.getRate(account.currency, homeCurrency);
                    if (rate == ExchangeRate.NA) {
                        return new Total(homeCurrency, TotalError.lastRateError(account.currency));
                    } else {
                        total = total.add(BigDecimal.valueOf(rate.rate * account.totalAmount));
                    }
                }
            }
        }
        Total result = new Total(homeCurrency);
        result.balance = total.longValue();
        return result;
    }

    public List<Long> findAccountsByNumber(String numberEnding) {
        try (Cursor c = db().rawQuery(
                "select " + AccountColumns.ID + " from " + ACCOUNT_TABLE +
                        " where " + AccountColumns.NUMBER + " like ?",
                new String[]{"%" + numberEnding})) {

            List<Long> res = new ArrayList<>(c.getCount());
            while (c.moveToNext()) {
                res.add(c.getLong(0));
            }
            return res;
        }
    }

    public boolean singleCurrencyOnly() {
        long currencyId = getSingleCurrencyId();
        return currencyId > 0;
    }

    private long getSingleCurrencyId() {
        try (Cursor c = db().rawQuery("select distinct " + AccountColumns.CURRENCY_ID + " from " + ACCOUNT_TABLE +
                " where " + AccountColumns.IS_INCLUDE_INTO_TOTALS + "=1 and " + AccountColumns.IS_ACTIVE + "=1", null)) {

            if (c.getCount() == 1) {
                c.moveToFirst();
                return c.getLong(0);
            }
            return -1;
        }
    }

    public void setDefaultHomeCurrency() {
        Currency homeCurrency = getHomeCurrency();
        long singleCurrencyId = getSingleCurrencyId();
        if (homeCurrency == Currency.EMPTY && singleCurrencyId > 0) {
            Currency c = get(Currency.class, singleCurrencyId);
            c.isDefault = true;
            saveOrUpdate(c);
        }
    }

    public void purgeAccountAtDate(Account account, long date) {
        long nearestTransactionId = findNearestOlderTransactionId(account, date);
        if (nearestTransactionId > 0) {
            SQLiteDatabase db = db();
            db.beginTransaction();
            try {
                Transaction newTransaction = createTransactionFromNearest(account, nearestTransactionId);
                breakSplitTransactions(account, date);
                deleteOldTransactions(account, date);
                insertWithoutUpdatingBalance(newTransaction);
                db.execSQL(INSERT_RUNNING_BALANCE, new Object[]{account.id, newTransaction.id, newTransaction.dateTime, newTransaction.fromAmount});
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
    }

    private Transaction createTransactionFromNearest(Account account, long nearestTransactionId) {
        Transaction nearestTransaction = get(Transaction.class, nearestTransactionId);
        long balance = getAccountBalanceForTransaction(account, nearestTransaction);
        Transaction newTransaction = new Transaction();
        newTransaction.fromAccountId = account.id;
        newTransaction.dateTime = DateUtils.atDayEnd(nearestTransaction.dateTime);
        newTransaction.fromAmount = balance;
        Payee payee = findOrInsertPayee(context.getString(R.string.purge_account_payee));
        newTransaction.payeeId = payee != null ? payee.id : 0;
        newTransaction.status = TransactionStatus.CL;
        return newTransaction;
    }

    private static final String BREAK_SPLIT_TRANSACTIONS_1 = UPDATE_ORPHAN_TRANSACTIONS_1 + " " +
            "AND " + TransactionColumns.datetime + "<=?";
    private static final String BREAK_SPLIT_TRANSACTIONS_2 = UPDATE_ORPHAN_TRANSACTIONS_2 + " " +
            "AND " + TransactionColumns.datetime + "<=?";

    private void breakSplitTransactions(Account account, long date) {
        SQLiteDatabase db = db();
        long dayEnd = DateUtils.atDayEnd(date);
        db.execSQL(BREAK_SPLIT_TRANSACTIONS_1, new Object[]{account.id, dayEnd});
        db.execSQL(BREAK_SPLIT_TRANSACTIONS_2, new Object[]{account.id, dayEnd});
        db.delete(TRANSACTION_ATTRIBUTE_TABLE, TransactionAttributeColumns.TRANSACTION_ID
                        + " in (SELECT _id from " + TRANSACTION_TABLE + " where " + TransactionColumns.datetime + "<=?)",
                new String[]{String.valueOf(dayEnd)});
    }

    public void deleteOldTransactions(Account account, long date) {
        SQLiteDatabase db = db();
        long dayEnd = DateUtils.atDayEnd(date);
        db.delete("transactions", "from_account_id=? and datetime<=? and is_template=0",
                new String[]{String.valueOf(account.id), String.valueOf(dayEnd)});
        db.delete("running_balance", "account_id=? and datetime<=?",
                new String[]{String.valueOf(account.id), String.valueOf(dayEnd)});
    }

    public long getAccountBalanceForTransaction(Account a, Transaction t) {
        return DatabaseUtils.rawFetchLongValue(this, "select balance from running_balance where account_id=? and transaction_id=?",
                new String[]{String.valueOf(a.id), String.valueOf(t.id)});
    }

    public long findNearestOlderTransactionId(Account account, long date) {
        return DatabaseUtils.rawFetchId(this,
                "select _id from v_blotter where from_account_id=? and datetime<=? order by datetime desc limit 1",
                new String[]{String.valueOf(account.id), String.valueOf(DateUtils.atDayEnd(date))});
    }

    public long findLatestTransactionDate(long accountId) {
        return DatabaseUtils.rawFetchLongValue(this,
                "select datetime from running_balance where account_id=? order by datetime desc limit 1",
                new String[]{String.valueOf(accountId)});
    }

    private static final String ACCOUNT_LAST_TRANSACTION_DATE_UPDATE = "UPDATE " + ACCOUNT_TABLE
            + " SET " + AccountColumns.LAST_TRANSACTION_DATE + "=? WHERE " + AccountColumns.ID + "=?";

    private void updateAccountLastTransactionDate(long accountId) {
        if (accountId <= 0) {
            return;
        }
        long lastTransactionDate = findLatestTransactionDate(accountId);
        db().execSQL(ACCOUNT_LAST_TRANSACTION_DATE_UPDATE, new Object[]{lastTransactionDate, accountId});
    }

    public void updateAccountsLastTransactionDate() {
        List<Account> accounts = getAllAccountsList();
        for (Account account : accounts) {
            updateAccountLastTransactionDate(account.id);
        }
    }

    public void restoreSystemEntities() {
        SQLiteDatabase db = db();
        db.beginTransaction();
        try {
            restoreCategories();
            restoreAttributes();
            restoreProjects();
            restoreLocations();
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e("Financisto", "Unable to restore system entities", e);
        } finally {
            db.endTransaction();
        }
    }

    private void restoreCategories() {
        reInsertEntity(Category.noCategory());
        reInsertEntity(Category.splitCategory());
        CategoryTree<Category> tree = getCategoriesTree(false);
        tree.reIndex();
        updateCategoryTree(tree);
    }

    private void restoreAttributes() {
        reInsertEntity(Attribute.deleteAfterExpired());
    }

    private void restoreProjects() {
        reInsertEntity(Project.noProject());
    }

    private void restoreLocations() {
        reInsertEntity(MyLocation.currentLocation());
    }

    public long getLastRunningBalanceForAccount(Account account) {
        return DatabaseUtils.rawFetchLongValue(this, "select balance from running_balance where account_id=? order by datetime desc, transaction_id desc limit 1",
                new String[]{String.valueOf(account.id)});
    }
}

