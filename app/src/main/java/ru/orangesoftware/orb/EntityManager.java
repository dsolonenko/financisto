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

import ru.orangesoftware.financisto.db.DatabaseHelper;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public abstract class EntityManager {
	
	private static final ConcurrentMap<Class<?>, EntityDefinition> definitions = new ConcurrentHashMap<Class<?>, EntityDefinition>();
	
	protected final SQLiteOpenHelper databaseHelper;
	
	public EntityManager(SQLiteOpenHelper databaseHelper) {
		this.databaseHelper = databaseHelper;
	}
	
	public SQLiteDatabase db() {
		return databaseHelper.getWritableDatabase();
	}

	private static EntityDefinition parseDefinition(Class<?> clazz) {
		if (!clazz.isAnnotationPresent(Entity.class)) {
			throw new IllegalArgumentException("Class "+clazz+" is not an @Entity");
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
						if (f.isAnnotationPresent(Transient.class)) {
							continue;
						} else if (f.isAnnotationPresent(JoinColumn.class)) {
							JoinColumn c = f.getAnnotation(JoinColumn.class);
							edb.withField(FieldInfo.entity(index++, f, c.name(), c.required()));
						} else {
							edb.withField(parseField(f));							
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

	public long saveOrUpdate(Object entity) {
		if (entity == null) {
			throw new IllegalArgumentException("Entity is null");
		}
        SQLiteDatabase db = db();
		EntityDefinition ed = getEntityDefinitionOrThrow(entity.getClass());
		ContentValues values = getContentValues(ed, entity);
		long id = ed.getId(entity);
		values.remove("updated_on");
		values.put("updated_on", System.currentTimeMillis());			
		if (id <= 0) {
			values.remove(ed.idField.columnName);		
			id = db.insertOrThrow(ed.tableName, null, values);
            ed.setId(entity, id);
            return id;
		} else {
			values.remove("updated_on");
			values.put("updated_on", System.currentTimeMillis());
			db.update(ed.tableName, values, ed.idField.columnName+"=?", new String[]{String.valueOf(id)});
			return id;
		}
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
            throw new IllegalArgumentException("Unable to re-insert "+entity.getClass()+" with id "+id);
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
				throw new PersistenceException("Unable to create content values for "+entity, e);
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
		StringBuilder sb = new StringBuilder(ed.sqlQuery);
		sb.append(" where e_").append(ed.idField.columnName).append("=?");
		String sql = sb.toString();
		Cursor c = db().rawQuery(sql, new String[]{id.toString()});
		try {
			if (c.moveToFirst()) {
				try {
					return (T)loadFromCursor("e", c, ed);
				} catch (Exception e) {
					throw new PersistenceException("Unable to load entity of type "+clazz+" with id "+id, e);
				}
			}
		} finally {
			c.close();
		}
		return null;			
	}

	public <T> List<T> list(Class<T> clazz) {
		EntityDefinition ed = getEntityDefinitionOrThrow(clazz);
		Cursor c = db().rawQuery(ed.sqlQuery, null);
		try {
			List<T> list = new LinkedList<T>();
			while (c.moveToNext()) {
				try {
					T t = (T)loadFromCursor("e", c, ed);
					list.add(t);
				} catch (Exception e) {
					throw new PersistenceException("Unable to list entites of type "+clazz, e);
				}
			}
			return list;
		} finally {
			c.close();
		}
	}

	public static <T> T loadFromCursor(Cursor c, Class<T> clazz) {
		EntityDefinition ed = getEntityDefinitionOrThrow(clazz);
		try {
			return (T)loadFromCursor("e", c, ed);
		} catch (Exception e) {
			throw new PersistenceException("Unable to load entity of type "+clazz+" from cursor", e);
		}
	}

	private static <T> T loadFromCursor(String pe, Cursor c, EntityDefinition ed) throws Exception {
		int idIndex = c.getColumnIndexOrThrow(pe+"__id");
		if (c.isNull(idIndex)) {
			return null;
		}
		@SuppressWarnings("unchecked")
		T entity = (T)ed.constructor.newInstance();
		FieldInfo[] fields = ed.fields;
		int count = fields.length;
		for (int i = 0; i<count; i++) {
			FieldInfo fi = fields[i];
			Object value;
			if (fi.type.isPrimitive()) {
				value = fi.type.getValueFromCursor(c, pe+"_"+fi.columnName);				
			} else {
				EntityDefinition eed = getEntityDefinitionOrThrow(fi.field.getType());
				value = loadFromCursor(pe+fi.index, c, eed);
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
		for (FieldInfo fI: ed.fields) {
			if (fI.columnName=="remote_key") {				
				Cursor cursor = db().query(ed.tableName,
						new String[] { "remote_key"}, ed.idField.columnName + "=?",
						new String[] { String.valueOf(id) }, null, null, null, null);
			    if (cursor != null) {
			    	cursor.moveToFirst();
			    	ContentValues row = new ContentValues();			    	
					row.put(DatabaseHelper.deleteLogColumns.TABLE_NAME, ed.tableName);				
			    	row.put(DatabaseHelper.deleteLogColumns.REMOTE_KEY,cursor.getString(0));				
			    	row.put(DatabaseHelper.deleteLogColumns.DELETED_ON, System.currentTimeMillis());
			    	db().insert(DatabaseHelper.DELETE_LOG_TABLE, null, row);
			    	Log.e("financisto","stored deleteLog " + ed.tableName + " "+ cursor.getString(0));
			    }
			}
		}		
		
		return db().delete(ed.tableName, ed.idField.columnName+"=?", new String[]{id.toString()});
	}

	public <T> Query<T> createQuery(Class<T> clazz) {
		return new Query<T>(this, clazz);
	}

}
