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
import ru.orangesoftware.financisto.adapter.CategoryListAdapter;
import ru.orangesoftware.financisto.adapter.MyEntityAdapter;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.DatabaseHelper.AccountColumns;
import ru.orangesoftware.financisto.db.MyEntityManager;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.Payee;
import ru.orangesoftware.financisto.model.Project;

import java.util.List;

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
		return new MyEntityAdapter<Project>(context, android.R.layout.simple_spinner_dropdown_item, android.R.id.text1, projects);
	}

    public static ListAdapter createPayeeAdapter(Context context, List<Payee> payees) {
        return new MyEntityAdapter<Payee>(context, android.R.layout.simple_spinner_dropdown_item, android.R.id.text1, payees);
    }

    public static ListAdapter createCurrencyAdapter(Context context, List<Currency> currencies) {
        return new MyEntityAdapter<Currency>(context, android.R.layout.simple_spinner_dropdown_item, android.R.id.text1, currencies);
    }

    public static ListAdapter createLocationAdapter(Context context, Cursor cursor) {
		return new SimpleCursorAdapter(context, android.R.layout.simple_spinner_dropdown_item, cursor, 
				new String[]{"e_name"}, new int[]{android.R.id.text1});
	}

    public static SimpleCursorAdapter createPayeeAdapter(Context context, DatabaseAdapter db) {
        final MyEntityManager em = db.em();
        return new SimpleCursorAdapter(context, android.R.layout.simple_dropdown_item_1line, null,
                new String[]{"e_title"}, new int[]{android.R.id.text1}){
            @Override
            public CharSequence convertToString(Cursor cursor) {
                return cursor.getString(cursor.getColumnIndex("e_title"));
            }

            @Override
            public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
                if (constraint == null) {
                    return em.getAllPayees();
                } else {
                    return em.getAllPayeesLike(constraint);
                }
            }
        };
    }
}
