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

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.MyEntityManager;
import ru.orangesoftware.financisto.model.MultiChoiceItem;
import ru.orangesoftware.financisto.utils.PinProtection;
import ru.orangesoftware.financisto.view.NodeInflater;

import java.util.List;

public abstract class AbstractActivity extends Activity implements ActivityLayoutListener {

	protected DatabaseAdapter db;
	protected MyEntityManager em;
	
	protected ActivityLayout x;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		LayoutInflater layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		NodeInflater nodeInflater = new NodeInflater(layoutInflater);
		x = new ActivityLayout(nodeInflater, this);
		db = new DatabaseAdapter(this);
		db.open();
		em = db.em();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
        if (shouldLock()) {
		    PinProtection.lock(this);
        }
	}

	@Override
	protected void onResume() {
		super.onResume();
        if (shouldLock()) {
		    PinProtection.unlock(this);
        }
	}
	
    protected boolean shouldLock() {
        return true;
    }

	@Override
	public void onClick(View v) {
		int id = v.getId();
		onClick(v, id);
	}

	protected abstract void onClick(View v, int id);


	@Override
	public void onSelected(int id, List<? extends MultiChoiceItem> items) {
	}

	@Override
	public void onSelectedId(int id, long selectedId) {
	}

	@Override
	public void onSelectedPos(int id, int selectedPos) {
	}

    protected boolean checkSelected(Object value, int messageResId) {
        if (value == null) {
            Toast.makeText(this, messageResId, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    protected boolean checkSelectedId(long value, int messageResId) {
		if (value <= 0) {
			Toast.makeText(this, messageResId, Toast.LENGTH_SHORT).show();
			return false;
		}
		return true;
	}

	public static void setVisibility(View v, int visibility) {
		v.setVisibility(visibility);
		Object o = v.getTag();
		if (o instanceof View) {
			((View)o).setVisibility(visibility);
		}
	}
		
	@Override
	protected void onDestroy() {
		db.close();
		super.onDestroy();		
	}
	
}
