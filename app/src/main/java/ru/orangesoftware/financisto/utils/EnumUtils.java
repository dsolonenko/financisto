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

import android.app.AlertDialog;
import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;

import ru.orangesoftware.financisto.adapter.EntityEnumAdapter;

public abstract class EnumUtils {

	public static String[] getLocalizedValues(Context context, LocalizableEnum[] values) {
		int count = values.length;
		String[] items = new String[count];
		for (int i = 0; i<count; i++) {
			LocalizableEnum r = values[i];
			items[i] = context.getString(r.getTitleId());
		}
		return items;
	}
	
	public static ArrayAdapter<String> createDropDownAdapter(Context context, LocalizableEnum[] values) {
		String[] items = getLocalizedValues(context, values);
		return new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, items);
	}
	
	public static <T extends EntityEnum> EntityEnumAdapter<T> createEntityEnumAdapter(Context context, T[] values) {
		return new EntityEnumAdapter<>(context, values, true);
	}

	public static ArrayAdapter<String> createSpinnerAdapter(Context context, LocalizableEnum[] values) {
		String[] items = getLocalizedValues(context, values);
		ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, items);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		return adapter;
	}

    public static <V, T extends ExecutableEntityEnum<? super V>> void showPickOneDialog(Context context, int titleId, final T[] items, final V value) {
        ListAdapter adapter = EnumUtils.createEntityEnumAdapter(context, items);
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setAdapter(adapter, (dialog1, which) -> {
                    dialog1.dismiss();
                    T e = items[which];
                    e.execute(value);
                })
                .create();
        dialog.setTitle(titleId);
        dialog.show();
    }

    public static String[] asStringArray(Enum[] values) {
        int count = values.length;
        String[] a = new String[count];
        for (int i=0; i<count; i++) {
            a[i] = values[i].name();
        }
        return a;
    }

	public static <E extends Enum> E selectEnum(Class<E> enumType, String enumValue, E defaultValue) {
		if (enumValue == null) return defaultValue;
		E[] constants = enumType.getEnumConstants();
		for (E e : constants) {
			if (enumValue.equals(e.name())) return e;
		}
		return defaultValue;
	}

}
