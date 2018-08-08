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
package ru.orangesoftware.financisto.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import ru.orangesoftware.financisto.blotter.BlotterFilter;
import ru.orangesoftware.financisto.datetime.Period;
import ru.orangesoftware.financisto.filter.Criteria;
import ru.orangesoftware.financisto.filter.WhereFilter;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.Budget;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.MyEntity;
import ru.orangesoftware.financisto.model.MyLocation;
import ru.orangesoftware.financisto.model.Payee;
import ru.orangesoftware.financisto.model.Project;
import ru.orangesoftware.financisto.model.SystemAttribute;
import ru.orangesoftware.financisto.model.Transaction;
import ru.orangesoftware.financisto.model.TransactionAttributeInfo;
import ru.orangesoftware.financisto.model.TransactionInfo;
import ru.orangesoftware.financisto.utils.MyPreferences;
import ru.orangesoftware.financisto.utils.MyPreferences.AccountSortOrder;
import ru.orangesoftware.financisto.utils.MyPreferences.LocationsSortOrder;
import ru.orangesoftware.financisto.utils.RecurUtils;
import ru.orangesoftware.financisto.utils.RecurUtils.Recur;
import ru.orangesoftware.financisto.utils.Utils;
import ru.orangesoftware.orb.EntityManager;
import ru.orangesoftware.orb.Expression;
import ru.orangesoftware.orb.Expressions;
import ru.orangesoftware.orb.Query;

import static ru.orangesoftware.financisto.db.DatabaseHelper.ACCOUNT_TABLE;
import static ru.orangesoftware.financisto.db.DatabaseHelper.AccountColumns;
import static ru.orangesoftware.financisto.db.DatabaseHelper.BUDGET_TABLE;
import static ru.orangesoftware.financisto.db.DatabaseHelper.CURRENCY_TABLE;
import static ru.orangesoftware.financisto.utils.StringUtil.capitalize;

public abstract class MyEntityManager extends EntityManager {

    protected final Context context;

    public MyEntityManager(Context context) {
        super(DatabaseHelper_.getInstance_(context));
        this.context = context;
    }

    private <T extends MyEntity> ArrayList<T> getAllEntitiesList(Class<T> clazz, boolean include0) {
        Query<T> q = createQuery(clazz);
        q.where(include0 ? Expressions.gte("id", 0) : Expressions.gt("id", 0));
//        if (SortableEntity.class.isAssignableFrom(clazz)) {
//            q.asc("sortOrder");
//        } else {
            q.asc("title");
//        }
        try (Cursor c = q.execute()) {
            T e0 = null;
            ArrayList<T> list = new ArrayList<>();
            while (c.moveToNext()) {
                T e = EntityManager.loadFromCursor(c, clazz);
                if (e.id == 0) {
                    e0 = e;
                } else {
                    list.add(e);
                }
            }
            if (e0 != null) {
                list.add(0, e0);
            }
            return list;
        }
    }

    private <T extends MyEntity> ArrayList<T> getAllEntitiesList(Class<T> clazz, boolean include0, boolean onlyActive) {
        Query<T> q = createQuery(clazz);
        Expression include0Ex = include0 ? Expressions.gte("id", 0) : Expressions.gt("id", 0);
        if (onlyActive) {
            q.where(Expressions.and(include0Ex, Expressions.eq("isActive", 1)));
        } else {
            q.where(include0Ex);
        }
        q.asc("title");
        try (Cursor c = q.execute()) {
            T e0 = null;
            ArrayList<T> list = new ArrayList<>();
            while (c.moveToNext()) {
                T e = EntityManager.loadFromCursor(c, clazz);
                if (e.id == 0) {
                    e0 = e;
                } else {
                    list.add(e);
                }
            }
            if (e0 != null) {
                list.add(0, e0);
            }
            return list;
        }
    }

	/* ===============================================
     * LOCATION
	 * =============================================== */

    public Cursor getAllLocations(boolean includeCurrentLocation) {
        Query<MyLocation> q = createQuery(MyLocation.class);
        if (!includeCurrentLocation) {
            q.where(Expressions.gt("id", 0));
        }
        LocationsSortOrder sortOrder = MyPreferences.getLocationsSortOrder(context);
        if (sortOrder.asc) {
            q.asc(sortOrder.property);
        } else {
            q.desc(sortOrder.property);
        }
        if (sortOrder != LocationsSortOrder.NAME) {
            q.asc(LocationsSortOrder.NAME.property);
        }
        return q.execute();
    }

    public List<MyLocation> getAllLocationsList(boolean includeNoLocation) {
        try (Cursor c = getAllLocations(includeNoLocation)) {
            MyLocation e0 = null;
            ArrayList<MyLocation> list = new ArrayList<>();
            while (c.moveToNext()) {
                MyLocation e = EntityManager.loadFromCursor(c, MyLocation.class);
                if (e.id == 0) {
                    e0 = e;
                } else {
                    list.add(e);
                }
            }
            if (e0 != null) {
                list.add(0, e0);
            }
            return list;
        }
    }

    public Map<Long, MyLocation> getAllLocationsByIdMap(boolean includeNoLocation) {
        List<MyLocation> locations = getAllLocationsList(includeNoLocation);
        Map<Long, MyLocation> map = new HashMap<>();
        for (MyLocation location : locations) {
            map.put(location.id, location);
        }
        return map;
    }

    public void deleteLocation(long id) {
        SQLiteDatabase db = db();
        db.beginTransaction();
        try {
            delete(MyLocation.class, id);
            ContentValues values = new ContentValues();
            values.put("location_id", 0);
            db.update("transactions", values, "location_id=?", new String[]{String.valueOf(id)});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public long saveLocation(MyLocation location) {
        return saveOrUpdate(location);
    }

	/* ===============================================
     * TRANSACTION INFO
	 * =============================================== */

    public TransactionInfo getTransactionInfo(long transactionId) {
        return get(TransactionInfo.class, transactionId);
    }

    public List<TransactionAttributeInfo> getAttributesForTransaction(long transactionId) {
        Query<TransactionAttributeInfo> q = createQuery(TransactionAttributeInfo.class).asc("name");
        q.where(Expressions.and(
                Expressions.eq("transactionId", transactionId),
                Expressions.gte("attributeId", 0)
        ));
        try (Cursor c = q.execute()) {
            List<TransactionAttributeInfo> list = new LinkedList<>();
            while (c.moveToNext()) {
                TransactionAttributeInfo ti = loadFromCursor(c, TransactionAttributeInfo.class);
                list.add(ti);
            }
            return list;
        }

    }

    public TransactionAttributeInfo getSystemAttributeForTransaction(SystemAttribute sa, long transactionId) {
        Query<TransactionAttributeInfo> q = createQuery(TransactionAttributeInfo.class);
        q.where(Expressions.and(
                Expressions.eq("transactionId", transactionId),
                Expressions.eq("attributeId", sa.id)
        ));
        try (Cursor c = q.execute()) {
            if (c.moveToFirst()) {
                return loadFromCursor(c, TransactionAttributeInfo.class);
            }
            return null;
        }
    }

    /* ===============================================
     * ACCOUNT
     * =============================================== */
    public Cursor getAccountByNumber(String numberEnding) {
        Query<Account> q = createQuery(Account.class);
        q.where(Expressions.like(AccountColumns.NUMBER, "%" + numberEnding));
        return q.execute();
    }

    public Account getAccount(long id) {
        return get(Account.class, id);
    }

    public Cursor getAccountsForTransaction(Transaction t) {
        return getAllAccounts(true, t.fromAccountId, t.toAccountId);
    }

    public Cursor getAllActiveAccounts() {
        return getAllAccounts(true);
    }

    public Cursor getAllAccounts() {
        return getAllAccounts(false);
    }

    private Cursor getAllAccounts(boolean isActiveOnly, long... includeAccounts) {
        AccountSortOrder sortOrder = MyPreferences.getAccountSortOrder(context);
        Query<Account> q = createQuery(Account.class);
        if (isActiveOnly) {
            int count = includeAccounts.length;
            if (count > 0) {
                Expression[] ee = new Expression[count + 1];
                for (int i = 0; i < count; i++) {
                    ee[i] = Expressions.eq("id", includeAccounts[i]);
                }
                ee[count] = Expressions.eq("isActive", 1);
                q.where(Expressions.or(ee));
            } else {
                q.where(Expressions.eq("isActive", 1));
            }
        }
        q.desc("isActive");
        if (sortOrder.asc) {
            q.asc(sortOrder.property);
        } else {
            q.desc(sortOrder.property);
        }
        return q.asc("title").execute();
    }

    public long saveAccount(Account account) {
        return saveOrUpdate(account);
    }

    public List<Account> getAllAccountsList() {
        List<Account> list = new ArrayList<>();
        try (Cursor c = getAllAccounts()) {
            while (c.moveToNext()) {
                Account a = EntityManager.loadFromCursor(c, Account.class);
                list.add(a);
            }
        }
        return list;
    }

    public Map<Long, Account> getAllAccountsMap() {
        Map<Long, Account> accountsMap = new HashMap<>();
        List<Account> list = getAllAccountsList();
        for (Account account : list) {
            accountsMap.put(account.id, account);
        }
        return accountsMap;
    }

	/* ===============================================
	 * CURRENCY
	 * =============================================== */

    private static final String UPDATE_DEFAULT_FLAG = "update currency set is_default=0";

    public long saveOrUpdate(Currency currency) {
        SQLiteDatabase db = db();
        db.beginTransaction();
        try {
            if (currency.isDefault) {
                db.execSQL(UPDATE_DEFAULT_FLAG);
            }
            long id = super.saveOrUpdate(currency);
            db.setTransactionSuccessful();
            return id;
        } finally {
            db.endTransaction();
        }
    }

    public int deleteCurrency(long id) {
        String sid = String.valueOf(id);
        Currency c = load(Currency.class, id);
        return db().delete(CURRENCY_TABLE, "_id=? AND NOT EXISTS (SELECT 1 FROM " + ACCOUNT_TABLE + " WHERE " + AccountColumns.CURRENCY_ID + "=?)",
                new String[]{sid, sid});
    }

    public Cursor getAllCurrencies(String sortBy) {
        Query<Currency> q = createQuery(Currency.class);
        return q.desc("isDefault").asc(sortBy).execute();
    }

    public List<Currency> getAllCurrenciesList() {
        return getAllCurrenciesList("name");
    }

    public List<Currency> getAllCurrenciesList(String sortBy) {
        Query<Currency> q = createQuery(Currency.class);
        return q.desc("isDefault").asc(sortBy).list();
    }

    public Map<String, Currency> getAllCurrenciesByTtitleMap() {
        return entitiesAsTitleMap(getAllCurrenciesList("name"));
    }

	/* ===============================================
	 * TRANSACTIONS
	 * =============================================== */

//	public Cursor getBlotter(WhereFilter blotterFilter) {
//		long t0 = System.currentTimeMillis();
//		try {
//			Query<TransactionInfo> q = createQuery(TransactionInfo.class);
//			if (!blotterFilter.isEmpty()) {
//				q.where(blotterFilter.toWhereExpression());
//			}
//			q.desc("dateTime");
//			return q.list();
//		} finally {
//			Log.d("BLOTTER", "getBlotter executed in "+(System.currentTimeMillis()-t0)+"ms");
//		}
//	}
//
//	public Cursor getTransactions(WhereFilter blotterFilter) {
//		return null;
//	}

//	public Cursor getAllProjects(boolean includeNoProject) {
//		Query<Project> q = createQuery(Project.class);
//		if (!includeNoProject) {
//			q.where(Expressions.neq("id", 0));
//		}
//		return q.list();
//	}

    public Project getProject(long id) {
        return get(Project.class, id);
    }

    public ArrayList<Project> getAllProjectsList(boolean includeNoProject) {
        return getAllEntitiesList(Project.class, includeNoProject);
    }

    public ArrayList<Project> getActiveProjectsList(boolean includeNoProject) {
        return getAllEntitiesList(Project.class, includeNoProject, true);
    }

    public Map<String, Project> getAllProjectsByTitleMap(boolean includeNoProject) {
        return entitiesAsTitleMap(getAllProjectsList(includeNoProject));
    }

    public Map<Long, Project> getAllProjectsByIdMap(boolean includeNoProject) {
        return entitiesAsIdMap(getAllProjectsList(includeNoProject));
    }

//	public Category getCategoryByLeft(long left) {
//		Query<Category> q = createQuery(Category.class);
//		q.where(Expressions.eq("left", left));
//		return q.uniqueResult();
//	}
//
//	public Cursor getAllCategories(boolean includeNoCategory) {
//		Query<CategoryInfo> q = createQuery(CategoryInfo.class);
//		if (!includeNoCategory) {
//			q.where(Expressions.neq("id", 0));
//		}
//		return q.list();
//	}
//	
//	public Cursor getAllCategoriesWithoutSubtree(long id) {
//		Category c = load(Category.class, id);
//		Query<CategoryInfo> q = createQuery(CategoryInfo.class);
//		q.where(Expressions.not(Expressions.and(
//				Expressions.gte("left", c.left),
//				Expressions.lte("right", c.right)
//		)));
//		return q.list();
//	}

    public long insertBudget(Budget budget) {
        SQLiteDatabase db = db();
        budget.remoteKey = null;

        db.beginTransaction();
        try {
            if (budget.id > 0) {
                deleteBudget(budget.id);
            }
            long id = 0;
            Recur recur = RecurUtils.createFromExtraString(budget.recur);
            Period[] periods = RecurUtils.periods(recur);
            for (int i = 0; i < periods.length; i++) {
                Period p = periods[i];
                budget.id = -1;
                budget.parentBudgetId = id;
                budget.recurNum = i;
                budget.startDate = p.start;
                budget.endDate = p.end;
                long bid = super.saveOrUpdate(budget);
                if (i == 0) {
                    id = bid;
                }
            }
            db.setTransactionSuccessful();
            return id;
        } finally {
            db.endTransaction();
        }
    }

    public void deleteBudget(long id) {
        SQLiteDatabase db = db();
        db.delete(BUDGET_TABLE, "_id=?", new String[]{String.valueOf(id)});
        db.delete(BUDGET_TABLE, "parent_budget_id=?", new String[]{String.valueOf(id)});
    }

    public void deleteBudgetOneEntry(long id) {
        db().delete(BUDGET_TABLE, "_id=?", new String[]{String.valueOf(id)});
    }

    public ArrayList<Budget> getAllBudgets(WhereFilter filter) {
        Query<Budget> q = createQuery(Budget.class);
        Criteria c = filter.get(BlotterFilter.DATETIME);
        if (c != null) {
            long start = c.getLongValue1();
            long end = c.getLongValue2();
            q.where(Expressions.and(Expressions.lte("startDate", end), Expressions.gte("endDate", start)));
        }
        try (Cursor cursor = q.execute()) {
            ArrayList<Budget> list = new ArrayList<>();
            while (cursor.moveToNext()) {
                Budget b = MyEntityManager.loadFromCursor(cursor, Budget.class);
                list.add(b);
            }
            return list;
        }
    }

    public void deleteProject(long id) {
        SQLiteDatabase db = db();
        db.beginTransaction();
        try {
            delete(Project.class, id);
            ContentValues values = new ContentValues();
            values.put("project_id", 0);
            db.update("transactions", values, "project_id=?", new String[]{String.valueOf(id)});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public ArrayList<TransactionInfo> getAllScheduledTransactions() {
        Query<TransactionInfo> q = createQuery(TransactionInfo.class);
        q.where(Expressions.and(
                Expressions.eq("isTemplate", 2),
                Expressions.eq("parentId", 0)));
        return (ArrayList<TransactionInfo>) q.list();
    }

    public Category getCategory(long id) {
        return get(Category.class, id);
    }

    public ArrayList<Category> getAllCategoriesList(boolean includeNoCategory) {
        return getAllEntitiesList(Category.class, includeNoCategory);
    }

    public Payee insertPayee(String payee) {
        if (Utils.isEmpty(payee)) {
            return Payee.EMPTY;
        } else {
            Payee p = getPayee(payee);
            if (p == null) {
                p = new Payee();
                p.title = payee;
                p.id = saveOrUpdate(p);
            }
            return p;
        }
    }

    public Payee getPayee(String payee) {
        Query<Payee> q = createQuery(Payee.class);
        q.where(Expressions.eq("title", payee));
        return q.uniqueResult();
    }

    public Cursor getAllPayees() {
        return getAllEntities(Payee.class);
    }

    public <T extends MyEntity> Cursor getAllEntities(Class<T> entityClass) {
        Query<T> q = createQuery(entityClass);
        return q.asc("title").execute();
    }

    public List<Payee> getAllPayeeList() {
        return getAllEntitiesList(Payee.class, true);
    }

    public Map<String, Payee> getAllPayeeByTitleMap() {
        return entitiesAsTitleMap(getAllPayeeList());
    }

    public Map<Long, Payee> getAllPayeeByIdMap() {
        return entitiesAsIdMap(getAllPayeeList());
    }

    public Cursor getAllPayeesLike(CharSequence constraint) {
        return getAllEntitiesLike(constraint, Payee.class);
    }

    public <T extends MyEntity> Cursor getAllEntitiesLike(CharSequence constraint, Class<T> entityClass) {
        Query<T> q = createQuery(entityClass);
        q.where(Expressions.or(
                Expressions.like("title", "%" + constraint + "%"),
                Expressions.like("title", "%" + capitalize(constraint.toString()) + "%")
        ));
        return q.asc("title").execute();
    }

    public List<Transaction> getSplitsForTransaction(long transactionId) {
        Query<Transaction> q = createQuery(Transaction.class);
        q.where(Expressions.eq("parentId", transactionId));
        return q.list();
    }

    public List<TransactionInfo> getSplitsInfoForTransaction(long transactionId) {
        Query<TransactionInfo> q = createQuery(TransactionInfo.class);
        q.where(Expressions.eq("parentId", transactionId));
        return q.list();
    }

    public List<TransactionInfo> getTransactionsForAccount(long accountId) {
        Query<TransactionInfo> q = createQuery(TransactionInfo.class);
        q.where(Expressions.and(
                Expressions.eq("fromAccount.id", accountId),
                Expressions.eq("parentId", 0)
        ));
        q.desc("dateTime");
        return q.list();
    }

    void reInsertEntity(MyEntity e) {
        if (get(e.getClass(), e.id) == null) {
            reInsert(e);
        }
    }

    public Currency getHomeCurrency() {
        Query<Currency> q = createQuery(Currency.class);
        q.where(Expressions.eq("isDefault", "1")); //uh-oh
        Currency homeCurrency = q.uniqueResult();
        if (homeCurrency == null) {
            homeCurrency = Currency.EMPTY;
        }
        return homeCurrency;
    }

    private static <T extends MyEntity> Map<String, T> entitiesAsTitleMap(List<T> entities) {
        Map<String, T> map = new HashMap<>();
        for (T e : entities) {
            map.put(e.title, e);
        }
        return map;
    }

    private static <T extends MyEntity> Map<Long, T> entitiesAsIdMap(List<T> entities) {
        Map<Long, T> map = new HashMap<>();
        for (T e : entities) {
            map.put(e.id, e);
        }
        return map;
    }

}
