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
package ru.orangesoftware.financisto.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.Toast;
import java.util.List;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.adapter.SmsTemplateListAdapter;
import ru.orangesoftware.financisto.db.DatabaseHelper.SmsTemplateColumns;
import ru.orangesoftware.financisto.model.SmsTemplate;
import ru.orangesoftware.financisto.utils.MenuItemInfo;

public class SmsTemplateListActivity extends AbstractListActivity {

	private static final int MENU_DUPLICATE = MENU_ADD + 1;

	public SmsTemplateListActivity() {
		super(R.layout.smstemplate_list);
	}

	@Override
	protected List<MenuItemInfo> createContextMenus(long id) {
		List<MenuItemInfo> menus = super.createContextMenus(id);
		for (MenuItemInfo m : menus) {
			if (m.menuId == MENU_VIEW) {
				m.enabled = false;
				break;
			}
		}
		menus.add(new MenuItemInfo(MENU_DUPLICATE, R.string.duplicate));
		return menus;
	}

	@Override
	public boolean onPopupItemSelected(int itemId, View view, int position, long id) {
		if (!super.onPopupItemSelected(itemId, view, position, id)) {
			switch (itemId) {
				case MENU_DUPLICATE:
					duplicateSmsTemplate(id);
					return true;
			}
		}
		return false;
	}

	private long duplicateSmsTemplate(long id) {
		long newId = db.duplicate(SmsTemplate.class, id);
		Toast.makeText(this, R.string.duplicate_sms_template, Toast.LENGTH_LONG).show();
		recreateCursor();
		return newId;
	}

	@Override
	protected void addItem() {
		Intent intent = new Intent(this, SmsTemplateActivity.class);
		startActivityForResult(intent, 1);
	}

	@Override
	protected ListAdapter createAdapter(Cursor cursor) {
		return new SmsTemplateListAdapter(db, this, cursor);
	}

	@Override
	protected Cursor createCursor() {
		return db.getSmsTemplatesWithFullInfo();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			cursor.requery();
		}
	}

	@Override
	protected void deleteItem(View v, int position, final long id) {
		new AlertDialog.Builder(this)
			.setTitle(R.string.delete)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setMessage(R.string.sms_delete_alert)
			.setPositiveButton(R.string.delete, (arg0, arg1) -> {
                db.delete(SmsTemplate.class, id);
                cursor.requery();
            })
			.setNegativeButton(R.string.cancel, null)
			.show();		
	}

	@Override
	public void editItem(View v, int position, long id) {
		Intent intent = new Intent(this, SmsTemplateActivity.class);
		intent.putExtra(SmsTemplateColumns._id.name(), id);
		startActivityForResult(intent, 2);		
	}	
	
	@Override
	protected void viewItem(View v, int position, long id) {
		editItem(v, position, id);
	}		

}
