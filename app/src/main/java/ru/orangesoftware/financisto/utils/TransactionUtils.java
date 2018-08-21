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
package ru.orangesoftware.financisto.utils;

import android.content.Context;
import android.database.Cursor;
import android.widget.ListAdapter;
import android.widget.SimpleCursorAdapter;

import java.util.List;

import ru.orangesoftware.financisto.adapter.CategoryListAdapter;
import ru.orangesoftware.financisto.adapter.MyEntityAdapter;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.DatabaseHelper.AccountColumns;
import ru.orangesoftware.financisto.db.MyEntityManager;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.MyEntity;
import ru.orangesoftware.financisto.model.MyLocation;
import ru.orangesoftware.financisto.model.Payee;
import ru.orangesoftware.financisto.model.Project;

public class TransactionUtils {

	public static ListAdapter createAccountAdapter(Context context, Cursor accountCursor) {
		return new SimpleCursorAdapter(context, android.R.layout.simple_spinner_dropdown_item, accountCursor, 
				new String[]{"e_"+AccountColumns.TITLE}, new int[]{android.R.id.text1});		
	}

    public static ListAdapter createAccountMultiChoiceAdapter(Context context, Cursor accountCursor) {
        return new SimpleCursorAdapter(context, android.R.layout.simple_list_item_multiple_choice, accountCursor,
                new String[]{"e_"+AccountColumns.TITLE}, new int[]{android.R.id.text1});
    }

	public static SimpleCursorAdapter createCurrencyAdapter(Context context, Cursor currencyCursor) {
		return new SimpleCursorAdapter(context, android.R.layout.simple_spinner_dropdown_item, currencyCursor, 
				new String[]{"e_name"}, new int[]{android.R.id.text1});		
	}

	public static ListAdapter createCategoryAdapter(DatabaseAdapter db, Context context, Cursor categoryCursor) {
		return new CategoryListAdapter(db, context, android.R.layout.simple_spinner_dropdown_item, categoryCursor);
	}

	public static ListAdapter createCategoryMultiChoiceAdapter(DatabaseAdapter db, Context context, Cursor categoryCursor) {
		return new CategoryListAdapter(db, context, android.R.layout.simple_list_item_multiple_choice, categoryCursor);
	}

	public static ListAdapter createProjectAdapter(Context context, List<Project> projects) {
		return new MyEntityAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, android.R.id.text1, projects);
	}

    public static ListAdapter createLocationAdapter(Context context, List<MyLocation> locations) {
        return new MyEntityAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, android.R.id.text1, locations);
    }

    public static ListAdapter createPayeeAdapter(Context context, List<Payee> payees) {
        return new MyEntityAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, android.R.id.text1, payees);
    }

    public static ListAdapter createCurrencyAdapter(Context context, List<Currency> currencies) {
        return new MyEntityAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, android.R.id.text1, currencies);
    }

    public static ListAdapter createLocationAdapter(Context context, Cursor cursor) {
		return new SimpleCursorAdapter(context, android.R.layout.simple_spinner_dropdown_item, cursor, 
				new String[]{"e_name"}, new int[]{android.R.id.text1});
	}

    public static SimpleCursorAdapter createPayeeAutoCompleteAdapter(Context context, final MyEntityManager db) {
        return new FilterSimpleCursorAdapter<MyEntityManager, Payee>(context, db, Payee.class) {
            @Override
            Cursor filterRows(CharSequence constraint) {
                return db.filterAllEntities(Payee.class, constraint.toString());
            }

            @Override
            Cursor getAllRows() {
                return db.filterAllEntities(Payee.class, null);
            }
        };
    }

    public static SimpleCursorAdapter createProjectAutoCompleteAdapter(Context context, final MyEntityManager db) {
        return new FilterSimpleCursorAdapter<>(context, db, Project.class);
    }

    public static SimpleCursorAdapter createLocationAutoCompleteAdapter(Context context, final MyEntityManager db) {
        return new FilterSimpleCursorAdapter<MyEntityManager, MyLocation>(context, db, MyLocation.class){
            @Override
            Cursor filterRows(CharSequence constraint) {
                return db.filterAllEntities(MyLocation.class, constraint.toString());
            }

            @Override
            Cursor getAllRows() {
                return db.getAllLocations(false);
            }
        };
    }

    public static SimpleCursorAdapter createCategoryFilterAdapter(Context context, final DatabaseAdapter db) {
        return new FilterSimpleCursorAdapter<DatabaseAdapter, MyLocation>(context, db, MyLocation.class, "title"){
            @Override
            Cursor getAllRows() {
                return db.getCategories(false);
            }

            @Override
            Cursor filterRows(CharSequence constraint) {
                return db.filterCategories(constraint);
            }
        };
    }

    static class FilterSimpleCursorAdapter<T extends MyEntityManager, E extends MyEntity> extends SimpleCursorAdapter {
        private final T db;
        private final String filterColumn;
        private final Class<E> entityClass;


        FilterSimpleCursorAdapter(Context context, final T db, Class<E> entityClass) {
            this(context, db, entityClass, "e_title");
        }
        
        FilterSimpleCursorAdapter(Context context, final T db, Class<E> entityClass, String filterColumn) {
            super(context, android.R.layout.simple_dropdown_item_1line, null, new String[]{filterColumn}, new int[]{android.R.id.text1});
            this.db = db;
            this.filterColumn = filterColumn;
            this.entityClass = entityClass;
        }

        @Override
        public CharSequence convertToString(Cursor cursor) {
            return cursor.getString(cursor.getColumnIndex(filterColumn));
        }

        @Override
        public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
            if (constraint == null || StringUtil.isEmpty(constraint.toString())) {
                return getAllRows();
            } else {
                return filterRows(constraint);
            }
        }

        Cursor filterRows(CharSequence constraint) {
            return db.filterActiveEntities(entityClass, constraint.toString());
        }

        Cursor getAllRows() {
            return db.filterActiveEntities(entityClass, null);
        }
    }
}
