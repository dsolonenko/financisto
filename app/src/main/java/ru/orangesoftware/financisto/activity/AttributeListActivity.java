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

import java.util.List;

import android.view.View;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.adapter.AttributeListAdapter;
import ru.orangesoftware.financisto.db.DatabaseHelper.AttributeColumns;
import ru.orangesoftware.financisto.utils.MenuItemInfo;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.widget.ListAdapter;

public class AttributeListActivity extends AbstractListActivity {
	
	public AttributeListActivity() {
		super(R.layout.attributes_list);
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
		return menus;
	}

	@Override
	protected void addItem() {
		Intent intent = new Intent(this, AttributeActivity.class);
		startActivityForResult(intent, 1);
	}

	@Override
	protected ListAdapter createAdapter(Cursor cursor) {
		return new AttributeListAdapter(db, this, cursor);
	}

	@Override
	protected Cursor createCursor() {
		return db.getAllAttributes();
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
			.setMessage(R.string.attribute_delete_alert)			
			.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					db.deleteAttribute(id);
					cursor.requery();
				}				
			})
			.setNegativeButton(R.string.cancel, null)
			.show();		
	}

	@Override
	public void editItem(View v, int position, long id) {
		Intent intent = new Intent(this, AttributeActivity.class);
		intent.putExtra(AttributeColumns.ID, id);
		startActivityForResult(intent, 2);		
	}	
	
	@Override
	protected void viewItem(View v, int position, long id) {
		editItem(v, position, id);
	}		

}
