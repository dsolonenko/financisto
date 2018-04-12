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
package ru.orangesoftware.financisto.adapter;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.SmsTemplate;
import ru.orangesoftware.financisto.utils.StringUtil;

@Deprecated // todo.mb: remove then
public class SmsTemplateListAdapter extends AbstractGenericListAdapter {

	public SmsTemplateListAdapter(DatabaseAdapter db, Context context, Cursor c) {
		super(db, context, c);
	}

	@Override
	protected void bindView(GenericViewHolder v, Context context, Cursor cursor) {
		SmsTemplate a = SmsTemplate.fromListCursor(cursor);
		v.lineView.setText(a.title);
		v.numberView.setText(StringUtil.getShortString(a.template, 40));
		v.amountView.setVisibility(View.VISIBLE);
		v.amountView.setText(Category.getTitle(a.categoryName, a.categoryLevel));
	}

}
