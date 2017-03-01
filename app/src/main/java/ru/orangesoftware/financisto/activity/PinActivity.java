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

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.utils.MyPreferences;
import ru.orangesoftware.financisto.utils.PinProtection;
import ru.orangesoftware.financisto.view.PinView;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class PinActivity extends Activity implements PinView.PinListener {
	
	public static final String SUCCESS = "PIN_SUCCESS";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		String pin = MyPreferences.getPin(this);
		if (pin == null) {
			onSuccess(null);
		} else {
			PinView v = new PinView(this, this, pin, R.layout.lock);
			setContentView(v.getView());
		}
	}

	@Override
	public void onConfirm(String pinBase64) {		
	}

	@Override
	public void onSuccess(String pinBase64) {
        PinProtection.pinUnlock(this);
		Intent data = new Intent();
		data.putExtra(SUCCESS, true);
		setResult(RESULT_OK, data);
		finish();
	}

	@Override
	public void onBackPressed() {
        moveTaskToBack(true);
	}

}
