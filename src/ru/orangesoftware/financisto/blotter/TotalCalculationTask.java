/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */
package ru.orangesoftware.financisto.blotter;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.Total;
import ru.orangesoftware.financisto.utils.Utils;

public abstract class TotalCalculationTask extends AsyncTask<Object, Total, Total> {
	
	private volatile boolean isRunning = true;
	
	private final Context context;
	private final TextView totalText;

	public TotalCalculationTask(Context context, TextView totalText) {
		this.context = context;
		this.totalText = totalText;
	}

    @Override
	protected Total doInBackground(Object... params) {
		try {
			return getTotalInHomeCurrency();
		} catch (Exception ex) {
			Log.e("TotalBalance", "Unexpected error", ex);
			return Total.ZERO;
		}
	}

    public abstract Total getTotalInHomeCurrency();

    public abstract Total[] getTotals();

	@Override
	protected void onPostExecute(Total result) {
		if (isRunning) {
            if (result.currency == Currency.EMPTY) {
                Toast.makeText(context, R.string.currency_make_default_warning, Toast.LENGTH_LONG).show();
            }
            Utils u = new Utils(context);
    	    u.setTotal(totalText, result);
		}
	}
	
	public void stop() {
		isRunning = false;
	}
	
}
