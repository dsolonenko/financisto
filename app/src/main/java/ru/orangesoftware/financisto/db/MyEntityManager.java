package ru.orangesoftware.financisto.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import ru.orangesoftware.financisto.blotter.BlotterFilter;
import ru.orangesoftware.financisto.datetime.Period;
import ru.orangesoftware.financisto.filter.Criteria;
import ru.orangesoftware.financisto.filter.WhereFilter;
import ru.orangesoftware.financisto.model.*;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.utils.MyPreferences;
import ru.orangesoftware.financisto.utils.MyPreferences.AccountSortOrder;
import ru.orangesoftware.financisto.utils.MyPreferences.LocationsSortOrder;
import ru.orangesoftware.financisto.utils.RecurUtils;
import ru.orangesoftware.financisto.utils.RecurUtils.Recur;
import ru.orangesoftware.financisto.utils.StringUtil;
import ru.orangesoftware.financisto.utils.Utils;
import ru.orangesoftware.orb.*;

import java.util.*;

import static ru.orangesoftware.financisto.db.DatabaseHelper.*;
import static ru.orangesoftware.financisto.utils.StringUtil.capitalize;

public abstract class MyEntityManager extends EntityManager {

    protected final Context context;

    public MyEntityManager(Context context) {
        super(DatabaseHelper_.getInstance_(context), new DatabaseFixPlugin());
        this.context = context;
    }

    public <T extends MyEntity> Cursor filterActiveEntities(Class<T> clazz, String titleLike) {
        return queryEntities(clazz, titleLike, false, true);
    }

    public <T extends MyEntity> Cursor queryEntities(Class<T> clazz, String titleLike, boolean include0, boolean onlyActive, Sort... sort) {
        Query<T> q = createQuery(clazz);
        Expression include0Ex = include0 ? Expressions.gte("id", 0) : Expressions.gt("id", 0);
        Expression whereEx = include0Ex;
        if (onlyActive) {
            whereEx = Expressions.and(include0Ex, Expressions.eq("isActive", 1));
        }
        if (!StringUtil.isEmpty(titleLike)) {
            titleLike = titleLike.replace(" ", "%");
            whereEx = Expressions.and(whereEx, Expressions.or(
                    Expressions.like("title", "%" + titleLike + "%"),
                    Expressions.like("title", "%" + capitalize(titleLike) + "%")
            ));
        }
        q.where(whereEx);
        if (sort != null && sort.length > 0) {
            q.sort(sort);
        } else {
            q.asc("title");
        }
        return q.execute();
    }

    public <T extends MyEntity> ArrayList<T> getAllEntitiesList(Class<T> clazz, boolean include0, boolean onlyActive, Sort... sort) {
        return getAllEntitiesList(clazz, include0, onlyActive, null, sort);
    }

    public <T extends MyEntity> ArrayList<T> getAllEntitiesList(Class<T> clazz, boolean include0, boolean onlyActive, String filter, Sort... sort) {
        try (Cursor c = queryEntities(clazz, filter, include0, onlyActive, sort)) {
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

    public ArrayList<MyLocation> getAllLocationsList(boolean includeNoLocation) {
        return getAllEntitiesList(MyLocation.class, includeNoLocation, false, locationSort());
    }

    public ArrayList<MyLocation> getAllActiveLocationsList() {
        return getAllEntitiesList(MyLocation.class, true, false, locationSort());
    }

    public ArrayList<MyLocation> getActiveLocationsList(boolean includeNoLocation) {
        return getAllEntitiesList(MyLocation.class, includeNoLocation, true, locationSort());
    }

    private Sort[] locationSort() {
        List<Sort> sort = new ArrayList<>();
        LocationsSortOrder sortOrder = MyPreferences.getLocationsSortOrder(context);
        sort.add(new Sort(sortOrder.property, sortOrder.asc));
        if (sortOrder != LocationsSortOrder.TITLE) {
            sort.add(new Sort(LocationsSortOrder.TITLE.property, sortOrder.asc));
        }
        return sort.toArray(new Sort[0]);
    }

    public Map<Long, MyLocation> getAllLocationsByIdMap(boolean includeNoLocation) {
        return entitiesAsIdMap(getAllLocationsList(includeNoLocation));
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

    public Project getProject(long id) {
        return get(Project.class, id);
    }

    public ArrayList<Project> getAllProjectsList(boolean includeNoProject) {
        ArrayList<Project> list = getAllEntitiesList(Project.class, includeNoProject, false, projectSort());
        if (includeNoProject) {
            addZeroEntity(list, Project.noProject());
        }
        return list;
    }

    public List<Project> getAllActiveProjectsList() {
        return getAllEntitiesList(Project.class, true, true, projectSort());
    }

    public ArrayList<Project> getActiveProjectsList(boolean includeNoProject) {
        return getAllEntitiesList(Project.class, includeNoProject, true, projectSort());
    }

    private Sort projectSort() {
        return new Sort("title", true);
    }

    private <T extends MyEntity> void addZeroEntity(ArrayList<T> list, T zeroEntity) {
        int zeroPos = -1;
        for (int i=0; i<list.size(); i++) {
            if (list.get(i).id == 0) {
                zeroPos = i;
                break;
            }
        }
        if (zeroPos >= 0) {
            list.add(0, list.remove(zeroPos));
        } else {
            list.add(0, zeroEntity);
        }
    }

    public Map<String, Project> getAllProjectsByTitleMap(boolean includeNoProject) {
        return entitiesAsTitleMap(getAllProjectsList(includeNoProject));
    }

    public Map<Long, Project> getAllProjectsByIdMap(boolean includeNoProject) {
        return entitiesAsIdMap(getAllProjectsList(includeNoProject));
    }

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

            switch (MyPreferences.getBudgetsSortOrder(context)) {
                case DATE:
                    q.desc("startDate");
                    break;

                case NAME:
                    q.asc("title");
                    break;

                case AMOUNT:
                    q.desc("amount");
                    break;
            }
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
        return getAllEntitiesList(Category.class, includeNoCategory, false);
    }

    public <T extends MyEntity> T findOrInsertEntityByTitle(Class<T> entityClass, String title) {
        if (Utils.isEmpty(title)) {
            return newEntity(entityClass);
        } else {
            T e = findEntityByTitle(entityClass, title);
            if (e == null) {
                e = newEntity(entityClass);
                e.title = title;
                e.id = saveOrUpdate(e);
            }
            return e;
        }
    }

    private <T extends MyEntity> T newEntity(Class<T> entityClass) {
        try {
            return entityClass.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public <T extends MyEntity> T findEntityByTitle(Class<T> entityClass, String title) {
        Query<T> q = createQuery(entityClass);
        q.where(Expressions.eq("title", title));
        return q.uniqueResult();
    }

    public <T extends MyEntity> Cursor getAllEntities(Class<T> entityClass) {
        return queryEntities(entityClass, null, false, false);
    }

    public List<Payee> getAllPayeeList() {
        return getAllEntitiesList(Payee.class, true, false, payeeSort());
    }

    public List<Payee> getAllActivePayeeList() {
        return getAllEntitiesList(Payee.class, true, true, payeeSort());
    }

    private Sort payeeSort() {
        return new Sort("title", true);
    }

    public Map<String, Payee> getAllPayeeByTitleMap() {
        return entitiesAsTitleMap(getAllPayeeList());
    }

    public Map<Long, Payee> getAllPayeeByIdMap() {
        return entitiesAsIdMap(getAllPayeeList());
    }

    public Cursor getAllPayeesLike(String constraint) {
        return filterAllEntities(Payee.class, constraint);
    }

    public <T extends MyEntity> Cursor filterAllEntities(Class<T> entityClass, String titleFilter) {
        return queryEntities(entityClass, StringUtil.emptyIfNull(titleFilter), false, false);
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
