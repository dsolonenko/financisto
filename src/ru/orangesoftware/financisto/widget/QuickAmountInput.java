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
package ru.orangesoftware.financisto.widget;

import java.math.BigDecimal;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.utils.CurrencyCache;
import ru.orangesoftware.financisto.utils.Utils;
import ru.orangesoftware.financisto.widget.AmountPicker.OnChangedListener;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

public class QuickAmountInput extends Activity {
	
	private AmountPicker picker;	
	private Currency currency;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Intent intent = getIntent();
		if (intent == null || intent.getExtras() == null) {
			throw new UnsupportedOperationException();
		}
		
		String amount = intent.getStringExtra(AmountInput.EXTRA_AMOUNT);
		long currencyId = intent.getLongExtra(AmountInput.EXTRA_CURRENCY, -1);
		currency = CurrencyCache.getCurrencyOrEmpty(currencyId);

		LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);        
        LinearLayout.LayoutParams lpWrapWrap = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lpWrapWrap.weight = 1;

        // picker        
        picker = new AmountPicker(this, currency.decimals);
		layout.addView(picker, lpWrapWrap);		
		if (amount != null) {
			picker.setCurrent(new BigDecimal(amount));
		}
		picker.setOnChangeListener(new OnChangedListener(){
			@Override
			public void onChanged(AmountPicker picker, BigDecimal oldVal, BigDecimal newVal) {
				setTitle();
			}
		});
		
		// buttons
		LinearLayout layout2 = new LinearLayout(this);
        layout2.setOrientation(LinearLayout.HORIZONTAL);        
        Button bOK = new Button(this);
        bOK.setText(R.string.ok);
        bOK.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent();
				intent.putExtra(AmountInput.EXTRA_AMOUNT, picker.getCurrent().toString());
				setResult(RESULT_OK, intent);
				finish();
			}
        });
        layout2.addView(bOK, lpWrapWrap);
        Button bClear = new Button(this);
        bClear.setText(R.string.reset);
        bClear.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				picker.setCurrent(BigDecimal.ZERO);
			}
        });
        layout2.addView(bClear, lpWrapWrap);
        Button bCancel = new Button(this);
        bCancel.setText(R.string.cancel);
        bCancel.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				setResult(RESULT_CANCELED);
				finish();
			}
        });
        layout2.addView(bCancel, lpWrapWrap);
        layout.addView(layout2, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		
        setTitle();
		setContentView(layout);		
	}

	private void setTitle() {
		setTitle(Utils.amountToString(currency, picker.getCurrent().multiply(Utils.HUNDRED)));
	}

	@Override
	protected void onRestoreInstanceState(Bundle inState) {
		super.onRestoreInstanceState(inState);
		if (inState != null) {
			String amount = inState.getString(AmountInput.EXTRA_AMOUNT);
			if (amount != null) {
				picker.setCurrent(new BigDecimal(amount).divide(Utils.HUNDRED));
			}
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		BigDecimal amount = picker.getCurrent();
		outState.putString(AmountInput.EXTRA_AMOUNT, amount.toString());
	}
		
}
