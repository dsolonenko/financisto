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

import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.os.Parcelable;
import android.util.Log;
import android.view.*;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.MyEntityManager;
import ru.orangesoftware.financisto.utils.MenuItemInfo;
import ru.orangesoftware.financisto.utils.PinProtection;
import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;

public abstract class AbstractListActivity extends ListActivity implements RefreshSupportedActivity {
	
	protected static final int MENU_VIEW = Menu.FIRST+1;
	protected static final int MENU_EDIT = Menu.FIRST+2;
	protected static final int MENU_DELETE = Menu.FIRST+3;
	protected static final int MENU_ADD = Menu.FIRST+4;
	
	private final int contentId;

    protected LayoutInflater inflater;
	protected Cursor cursor;
	protected ListAdapter adapter;
	protected DatabaseAdapter db;
	protected MyEntityManager em;
	protected ImageButton bAdd;

    protected boolean enablePin = true;

	protected AbstractListActivity(int contentId) {
		this.contentId = contentId;				
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		
		setContentView(contentId);
		
		db = new DatabaseAdapter(this);
		db.open();
		
		em = db.em();

        this.inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		internalOnCreate(savedInstanceState);
		
		cursor = createCursor();
		if (cursor != null) {
			startManagingCursor(cursor);
		}

        recreateAdapter();
		
		registerForContextMenu(getListView());
	}

    protected void recreateAdapter() {
        adapter = createAdapter(cursor);
        setListAdapter(adapter);
    }

    protected abstract Cursor createCursor();

	protected abstract ListAdapter createAdapter(Cursor cursor);

	protected void internalOnCreate(Bundle savedInstanceState) {
		bAdd = (ImageButton)findViewById(R.id.bAdd);
		bAdd.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				addItem();
			}
		});		
	}		
	
	@Override
	protected void onDestroy() {
		db.close();
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		super.onPause();
        if (enablePin) PinProtection.lock(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
        if (enablePin) PinProtection.unlock(this);
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		AdapterView.AdapterContextMenuInfo mi = (AdapterView.AdapterContextMenuInfo)menuInfo;
		String headerTitle = getContextMenuHeaderTitle(mi.position);
		if (headerTitle != null) {
			menu.setHeaderTitle(headerTitle);
		}
		List<MenuItemInfo> menus = createContextMenus(mi.id);
		int i = 0;
		for (MenuItemInfo m : menus) {
			if (m.enabled) {
				menu.add(0, m.menuId, i++, m.titleId);				
			}
		}
	}	
	
	protected String getContextMenuHeaderTitle(int position) {
		return "";
	}

	protected List<MenuItemInfo> createContextMenus(long id) {
		List<MenuItemInfo> menus = new LinkedList<MenuItemInfo>();
		menus.add(new MenuItemInfo(MENU_VIEW, R.string.view));
		menus.add(new MenuItemInfo(MENU_EDIT, R.string.edit));
		menus.add(new MenuItemInfo(MENU_DELETE, R.string.delete));
		return menus;
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		super.onContextItemSelected(item);
        AdapterView.AdapterContextMenuInfo mi = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
		switch (item.getItemId()) {
			case MENU_VIEW: {
				viewItem(mi.targetView, mi.position, mi.id);
				return true;
			} 			
			case MENU_EDIT: {
				editItem(mi.targetView, mi.position, mi.id);
				return true;
			} 			
			case MENU_DELETE: {
				deleteItem(mi.targetView, mi.position, mi.id);
				return true;
			} 			
		}
		return false;
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		onItemClick(v, position, id);
	}

    protected void onItemClick(View v, int position, long id) {
        viewItem(v, position, id);
    }

    protected void addItem() {
	}

	protected abstract void deleteItem(View v, int position, long id);

	protected abstract void editItem(View v, int position, long id);

	protected abstract void viewItem(View v, int position, long id);

	public void recreateCursor() {
		Log.i("AbstractListActivity", "Recreating cursor");
        Parcelable state = getListView().onSaveInstanceState();
        try {
            if (cursor != null) {
                stopManagingCursor(cursor);
                cursor.close();
            }
            cursor = createCursor();
            if (cursor != null) {
                startManagingCursor(cursor);
                recreateAdapter();
            }
        } finally {
            getListView().onRestoreInstanceState(state);
        }
	}

    @Override
    public void integrityCheck() {
        new IntegrityCheckTask(this).execute();
    }

    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			recreateCursor();
		}
	}

}
