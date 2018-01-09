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

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupMenu;

import java.util.LinkedList;
import java.util.List;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.utils.MenuItemInfo;
import ru.orangesoftware.financisto.utils.MyPreferences;
import ru.orangesoftware.financisto.utils.PinProtection;

public abstract class AbstractListActivity extends ListActivity implements RefreshSupportedActivity {

    protected static final int MENU_VIEW = Menu.FIRST + 1;
    protected static final int MENU_EDIT = Menu.FIRST + 2;
    protected static final int MENU_DELETE = Menu.FIRST + 3;
    protected static final int MENU_ADD = Menu.FIRST + 4;

    private final int contentId;

    protected LayoutInflater inflater;
    protected Cursor cursor;
    protected ListAdapter adapter;
    protected DatabaseAdapter db;
    protected ImageButton bAdd;

    protected boolean enablePin = true;

    protected AbstractListActivity(int contentId) {
        this.contentId = contentId;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(MyPreferences.switchLocale(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        setContentView(contentId);

        db = new DatabaseAdapter(this);
        db.open();

        this.inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        internalOnCreate(savedInstanceState);

        cursor = createCursor();
        if (cursor != null) {
            startManagingCursor(cursor);
        }

        recreateAdapter();
        getListView().setOnItemLongClickListener((parent, view, position, id) -> {
            PopupMenu popupMenu = new PopupMenu(AbstractListActivity.this, view);
            Menu menu = popupMenu.getMenu();
            List<MenuItemInfo> menus = createContextMenus(id);
            int i = 0;
            for (MenuItemInfo m : menus) {
                if (m.enabled) {
                    menu.add(0, m.menuId, i++, m.titleId);
                }
            }
            popupMenu.setOnMenuItemClickListener(item -> onPopupItemSelected(item.getItemId(), view, position, id));
            popupMenu.show();
            return true;
        });
    }

    protected void recreateAdapter() {
        adapter = createAdapter(cursor);
        setListAdapter(adapter);
    }

    protected abstract Cursor createCursor();

    protected abstract ListAdapter createAdapter(Cursor cursor);

    protected void internalOnCreate(Bundle savedInstanceState) {
        bAdd = findViewById(R.id.bAdd);
        bAdd.setOnClickListener(arg0 -> addItem());
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

    protected List<MenuItemInfo> createContextMenus(long id) {
        List<MenuItemInfo> menus = new LinkedList<>();
        menus.add(new MenuItemInfo(MENU_VIEW, R.string.view));
        menus.add(new MenuItemInfo(MENU_EDIT, R.string.edit));
        menus.add(new MenuItemInfo(MENU_DELETE, R.string.delete));
        return menus;
    }

    public boolean onPopupItemSelected(int itemId, View view, int position, long id) {
        switch (itemId) {
            case MENU_VIEW: {
                viewItem(view, position, id);
                return true;
            }
            case MENU_EDIT: {
                editItem(view, position, id);
                return true;
            }
            case MENU_DELETE: {
                deleteItem(view, position, id);
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
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            recreateCursor();
        }
    }

}
