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
package ru.orangesoftware.orb;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityNotFoundException;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.PersistenceException;
import javax.persistence.Table;
import javax.persistence.Transient;

import ru.orangesoftware.financisto.db.DatabaseUtils;
import ru.orangesoftware.financisto.model.MyEntity;
import ru.orangesoftware.financisto.model.SortableEntity;

import static ru.orangesoftware.financisto.db.DatabaseHelper.SmsTemplateColumns.sort_order;

public abstract class EntityManager {

    public static final String DEF_ID_COL = "_id";
    public static final String DEF_TITLE_COL = "title";
    public static final String DEF_SORT_COL = "sort_order";

    private static final ConcurrentMap<Class<?>, EntityDefinition> definitions = new ConcurrentHashMap<>();

    // effectively immutable
    protected SQLiteOpenHelper databaseHelper;

    public EntityManager(SQLiteOpenHelper databaseHelper) {
        this.databaseHelper = databaseHelper;
    }

    public SQLiteDatabase db() {
        return databaseHelper.getWritableDatabase();
    }

    private static EntityDefinition parseDefinition(Class<?> clazz) {
        if (!clazz.isAnnotationPresent(Entity.class)) {
            throw new IllegalArgumentException("Class " + clazz + " is not an @Entity");
        }
        EntityDefinition.Builder edb = new EntityDefinition.Builder(clazz);
        try {
            Constructor<?> constructor = clazz.getConstructor();
            edb.withConstructor(constructor);
        } catch (Exception e) {
            throw new IllegalArgumentException("Entity must have an empty constructor");
        }
        if (clazz.isAnnotationPresent(Table.class)) {
            Table tableAnnotation = clazz.getAnnotation(Table.class);
            edb.withTable(tableAnnotation.name());
        }
        Field[] fields = clazz.getFields();
        if (fields != null) {
            int index = 0;
            for (Field f : fields) {
                if ((f.getModifiers() & Modifier.STATIC) == 0) {
                    if (f.isAnnotationPresent(Id.class)) {
                        edb.withIdField(parseField(f));
                    } else {
                        if (!f.isAnnotationPresent(Transient.class)) {
                            if (f.isAnnotationPresent(JoinColumn.class)) {
                                JoinColumn c = f.getAnnotation(JoinColumn.class);
                                edb.withField(FieldInfo.entity(index++, f, c.name(), c.required()));
                            } else {
                                edb.withField(parseField(f));
                            }
                        }
                    }
                }
            }
        }
        return edb.create();
    }

    private static FieldInfo parseField(Field f) {
        String columnName;
        if (f.isAnnotationPresent(Column.class)) {
            columnName = f.getAnnotation(Column.class).name();
        } else {
            columnName = f.getName().toUpperCase();
        }
        return FieldInfo.primitive(f, columnName);
    }

    static EntityDefinition getEntityDefinitionOrThrow(Class<?> clazz) {
        EntityDefinition ed = definitions.get(clazz);
        if (ed == null) {
            EntityDefinition ned = parseDefinition(clazz);
            ed = definitions.putIfAbsent(clazz, ned);
            if (ed == null) {
                ed = ned;
            }
        }
        return ed;
    }

    public <T extends MyEntity> long duplicate(Class<T> clazz, Object id) {
        T obj = load(clazz, id);
        if (obj == null) return -1;

        obj.id = -1;
        updateEntitySortOrder(obj, -1);
        return saveOrUpdate(obj);
    }

    public  <T extends MyEntity> boolean updateEntitySortOrder(T obj, long sortOrder) {
        if (obj instanceof SortableEntity) {
            final EntityDefinition ed = getEntityDefinitionOrThrow(obj.getClass());
            try {
                for (FieldInfo f : ed.fields) {
                    if (DEF_SORT_COL.equals(f.columnName)) {
                        f.field.set(obj, sortOrder);
                        return true;
                    }
                }
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(
                        String.format("Failed to reset sort order for %s", obj.getClass()), e);
            }
        }
        return false;
    }

    public long saveOrUpdate(Object entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity is null");
        }
        SQLiteDatabase db = db();
        final EntityDefinition ed = getEntityDefinitionOrThrow(entity.getClass());
        final ContentValues values = getContentValues(ed, entity);
        long id = ed.getId(entity);
        values.remove("updated_on");
        values.put("updated_on", System.currentTimeMillis());
        if (id <= 0) {
            values.remove(ed.idField.columnName);
            if (values.containsKey(DEF_SORT_COL)
                && values.getAsLong(DEF_SORT_COL) <= 0) {
                values.put(DEF_SORT_COL, getMaxOrder(ed) + 1);
            }
            id = db.insertOrThrow(ed.tableName, null, values);
            ed.setId(entity, id);
            return id;
        } else {
            values.remove("updated_on");
            values.put("updated_on", System.currentTimeMillis());
            db.update(ed.tableName, values, ed.idField.columnName + "=?", new String[]{String.valueOf(id)});
            return id;
        }
    }

    private long getMaxOrder(EntityDefinition ed) {
        return DatabaseUtils.rawFetchLong(db(),
            String.format("select max(%s) from %s", DEF_SORT_COL, ed.tableName), new String[]{}, 0);
    }

    public long reInsert(Object entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity is null");
        }
        SQLiteDatabase db = db();
        EntityDefinition ed = getEntityDefinitionOrThrow(entity.getClass());
        ContentValues values = getContentValues(ed, entity);
        long id = ed.getId(entity);
        long newId = db.insertOrThrow(ed.tableName, null, values);
        if (id != newId) {
            throw new IllegalArgumentException("Unable to re-insert " + entity.getClass() + " with id " + id);
        }
        return id;
    }

    private ContentValues getContentValues(EntityDefinition ed, Object entity) {
        ContentValues values = new ContentValues();
        FieldInfo[] fields = ed.fields;
        for (FieldInfo fi : fields) {
            try {
                if (fi.type.isPrimitive()) {
                    Object value = fi.field.get(entity);
                    fi.type.setValue(values, fi.columnName, value);
                } else {
                    Object e = fi.field.get(entity);
                    if (e == null) {
                        values.putNull(fi.columnName);
                    } else {
                        EntityDefinition eed = getEntityDefinitionOrThrow(e.getClass());
                        FieldInfo ffi = eed.idField;
                        Object value = ffi.field.get(e);
                        ffi.type.setValue(values, fi.columnName, value);
                    }
                }
            } catch (Exception e) {
                throw new PersistenceException("Unable to create content values for " + entity, e);
            }
        }
        return values;
    }

    public <T> T load(Class<T> clazz, Object id) {
        T e = get(clazz, id);
        if (e != null) {
            return e;
        } else {
            throw new EntityNotFoundException(clazz, id);
        }
    }

    public <T> T get(Class<T> clazz, Object id) {
        if (id == null) {
            throw new IllegalArgumentException("Id can't be null");
        }
        EntityDefinition ed = getEntityDefinitionOrThrow(clazz);
        String sql = ed.sqlQuery + " where e_" + ed.idField.columnName + "=?";
        try (Cursor c = db().rawQuery(sql, new String[]{id.toString()})) {
            if (c.moveToFirst()) {
                try {
                    return (T) loadFromCursor("e", c, ed);
                } catch (Exception e) {
                    throw new PersistenceException("Unable to load entity of type " + clazz + " with id " + id, e);
                }
            }
        }
        return null;
    }

    public <T> List<T> list(Class<T> clazz) {
        EntityDefinition ed = getEntityDefinitionOrThrow(clazz);
        try (Cursor c = db().rawQuery(ed.sqlQuery, null)) {
            List<T> list = new LinkedList<>();
            while (c.moveToNext()) {
                try {
                    T t = loadFromCursor("e", c, ed);
                    list.add(t);
                } catch (Exception e) {
                    throw new PersistenceException("Unable to list entites of type " + clazz, e);
                }
            }
            return list;
        }
    }

    public static <T> T loadFromCursor(Cursor c, Class<T> clazz) {
        EntityDefinition ed = getEntityDefinitionOrThrow(clazz);
        try {
            return (T) loadFromCursor("e", c, ed);
        } catch (Exception e) {
            throw new PersistenceException("Unable to load entity of type " + clazz + " from cursor", e);
        }
    }

    private static <T> T loadFromCursor(String pe, Cursor c, EntityDefinition ed) throws Exception {
        int idIndex = c.getColumnIndexOrThrow(pe + "__id");
        if (c.isNull(idIndex)) {
            return null;
        }
        @SuppressWarnings("unchecked")
        T entity = (T) ed.constructor.newInstance();
        FieldInfo[] fields = ed.fields;
        for (FieldInfo fi : fields) {
            Object value;
            if (fi.type.isPrimitive()) {
                value = fi.type.getValueFromCursor(c, pe + "_" + fi.columnName);
            } else {
                EntityDefinition eed = getEntityDefinitionOrThrow(fi.field.getType());
                value = loadFromCursor(pe + fi.index, c, eed);
            }
            fi.field.set(entity, value);
        }
        return entity;
    }

    public <T> int delete(Class<T> clazz, Object id) {
        if (id == null) {
            throw new IllegalArgumentException("Id can't be null");
        }
        EntityDefinition ed = getEntityDefinitionOrThrow(clazz);
        return db().delete(ed.tableName, ed.idField.columnName + "=?", new String[]{id.toString()});
    }

    public <T> Query<T> createQuery(Class<T> clazz) {
        return new Query<>(this, clazz);
    }

    public <T extends SortableEntity> long getNextByOrder(Class<T> entityClass, long itemId) {
        final EntityDefinition ed = getEntityDefinitionOrThrow(entityClass);
        final T item = get(entityClass, itemId);
        long res = -1;
        if (item != null) {
            res = DatabaseUtils.rawFetchLong(db(),
                    String.format("select %1$s from %2$s where %3$s > ? order by %3$s asc limit 1", DEF_ID_COL, ed.tableName, DEF_SORT_COL),
                    new String[]{String.valueOf(item.getSortOrder())}, res);
        }
        return res;
    }

    public <T extends SortableEntity> boolean moveItemByChangingOrder(Class<T> entityClass, long movedId, long targetId) {
        if (movedId > 0 && targetId > 0 && movedId != targetId) {
            final EntityDefinition ed = getEntityDefinitionOrThrow(entityClass);

            final T sourceItem = load(entityClass, movedId);
            final long srcOrder = sourceItem.getSortOrder();
            final long targetOrder = load(entityClass, targetId).getSortOrder();
            final SQLiteDatabase db = db();
            db.beginTransaction();
            try {
                if (srcOrder > targetOrder) {
                    db.execSQL(String.format("update %1$s set %2$s = %2$s + 1 where %2$s >= ? and %2$s < ? ", ed.tableName, sort_order),
                        new String[]{"" + targetOrder, "" + srcOrder});
                } else if (srcOrder < targetOrder) {
                    db.execSQL(String.format("update %1$s set %2$s = %2$s - 1 where %2$s > ? and %2$s <= ? ", ed.tableName, sort_order),
                        new String[]{"" + srcOrder, "" + targetOrder});
                }
                final ContentValues cv = new ContentValues(1);
                cv.put(DEF_SORT_COL, targetOrder);
                db.update(ed.tableName, cv, DEF_ID_COL + "=?", new String[]{movedId + ""});
                
                db.setTransactionSuccessful();
                return true;
            } finally {
                db.endTransaction();
            }
        }
        return false;
    }
}
