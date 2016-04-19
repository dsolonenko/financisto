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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.Editable;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.text.method.NumberKeyListener;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.utils.Utils;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

public class AmountInput extends LinearLayout {

    public static interface OnAmountChangedListener {
		void onAmountChanged(long oldAmount, long newAmount);
	}

	public static final String EXTRA_AMOUNT = "amount";
	public static final String EXTRA_CURRENCY = "currency";

	private static final AtomicInteger EDIT_AMOUNT_REQUEST = new AtomicInteger(2000);

	protected Activity owner;
	private Currency currency;
	private int decimals;

    private ToggleButton toggleView;
	private EditText primary;
	private EditText secondary;
	
	private int requestId;
	private OnAmountChangedListener onAmountChangedListener;
    private boolean incomeExpenseEnabled = true;

	public AmountInput(Context context, AttributeSet attrs) {
		super(context, attrs);
		initialize(context, attrs);
	}

	public AmountInput(Context context) {
		super(context);
		initialize(context, null);
	}
	
    public void disableIncomeExpenseButton() {
        incomeExpenseEnabled = false;
        toggleView.setEnabled(false);
    }

    public boolean isIncomeExpenseEnabled() {
        return incomeExpenseEnabled;
    }

    public void setIncome() {
        toggleView.setChecked(true);
    }

    public void setExpense() {
        toggleView.setChecked(false);
    }

    public boolean isExpense() {
        return !toggleView.isChecked();
    }

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		Utils.setEnabled(this, enabled);
        if (!incomeExpenseEnabled) {
            disableIncomeExpenseButton();
        }
	}

	public void setOnAmountChangedListener(
			OnAmountChangedListener onAmountChangedListener) {
		this.onAmountChangedListener = onAmountChangedListener;
	}

	private final TextWatcher textWatcher = new TextWatcher() {
		private long oldAmount;

		@Override
		public void afterTextChanged(Editable s) {
			if (onAmountChangedListener != null) {
				long amount = getAmount();
				onAmountChangedListener.onAmountChanged(oldAmount, amount);
				oldAmount = amount;
			}
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
			oldAmount = getAmount();
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
		}
	};

	private void initialize(Context context, AttributeSet attrs) {
		requestId = EDIT_AMOUNT_REQUEST.incrementAndGet();
		LayoutInflater layoutInflater = LayoutInflater.from(context);
		layoutInflater.inflate(R.layout.amount_input, this, true);
        findViewById(R.id.amount_input).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startInputActivity(QuickAmountInput.class);
			}
		});
		findViewById(R.id.calculator).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startInputActivity(CalculatorInput.class);
			}
		});
		toggleView = (ToggleButton) findViewById(R.id.toggle);
        toggleView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (onAmountChangedListener != null) {
                    long amount = getAmount();
                    onAmountChangedListener.onAmountChanged(-amount, amount);
                }
            }
        });
		primary = (EditText) findViewById(R.id.primary);
		primary.setKeyListener(keyListener);
		primary.addTextChangedListener(textWatcher);
		primary.setOnFocusChangeListener(selectAllOnFocusListener);
		secondary = (EditText) findViewById(R.id.secondary);
		secondary.setKeyListener(new DigitsKeyListener(false, false){
			
			@Override
			public boolean onKeyDown(View view, Editable content, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_DEL) {
					if (content.length() == 0) {
						primary.requestFocus();
						int pos = primary.getText().length(); 
						primary.setSelection(pos, pos);
						return true;
					}
				}
				return super.onKeyDown(view, content, keyCode, event);
			}

			@Override
			public int getInputType() {
				return InputType.TYPE_CLASS_PHONE;
			}			

		});
		secondary.addTextChangedListener(textWatcher);
		secondary.setOnFocusChangeListener(selectAllOnFocusListener);
	}
	
	private static final char[] acceptedChars = new char[]{'0','1','2','3','4','5','6','7','8','9'};
	private static final char[] commaChars = new char[]{'.', ','};
	
	private final NumberKeyListener keyListener = new NumberKeyListener() {
		
		@Override
		public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
			if (end - start == 1) {
				char c = source.charAt(0);
				if (c == '.' || c == ',') {
					onDotOrComma();
					return "";
				}
                if (isIncomeExpenseEnabled()) {
                    if (c == '-') {
                        setExpense();
                        return "";
                    }
                    if (c == '+') {
                        setIncome();
                        return "";
                    }
                }
			}
			return super.filter(source, start, end, dest, dstart, dend);
		}

		@Override
		public boolean onKeyDown(View view, Editable content, int keyCode, KeyEvent event) {
			char c = event.getMatch(commaChars);
			if (c == '.' || c == ',') {
				onDotOrComma();
				return true;
			}
			return super.onKeyDown(view, content, keyCode, event);
		}
		
		@Override
		protected char[] getAcceptedChars() {
			return acceptedChars;
		}

		@Override
		public int getInputType() {
			return InputType.TYPE_CLASS_PHONE;
		}
	};
	
	private final View.OnFocusChangeListener selectAllOnFocusListener = new View.OnFocusChangeListener() {
		
		@Override
		public void onFocusChange(View v, boolean hasFocus) {
			EditText t = (EditText) v;
			if (hasFocus) {
				t.selectAll();
			}
		}
	};

	protected <T extends Activity> void startInputActivity(Class<T> clazz) {
		Intent intent = new Intent(getContext(), clazz);
		if (currency != null) {
			intent.putExtra(EXTRA_CURRENCY, currency.id);
		}
		intent.putExtra(EXTRA_AMOUNT, getAbsAmountString());
		owner.startActivityForResult(intent, requestId);
	}

	protected void onDotOrComma() {
		secondary.requestFocus();
	}

	public Currency getCurrency() {
		return currency;
	}

	public int getDecimals() {
		return decimals;
	}

	public void setCurrency(Currency currency) {
		this.currency = currency;
	}

	public void setOwner(Activity owner) {
		this.owner = owner;
	}

	public boolean processActivityResult(int requestCode, Intent data) {
		if (requestCode == requestId) {
			String amount = data.getStringExtra(EXTRA_AMOUNT);
			if (amount != null) {
				try {
					BigDecimal d = new BigDecimal(amount).setScale(2,
							BigDecimal.ROUND_HALF_UP);
                    boolean isExpense = isExpense();
					setAmount(d.unscaledValue().longValue());
                    if (isExpense) {
                        setExpense();
                    }
					return true;
				} catch (NumberFormatException ex) {
					return false;
				}
			}
		}
		return false;
	}

	public void setAmount(long amount) {
		long absAmount = Math.abs(amount);
		long x = absAmount / 100;
		long y = absAmount - 100 * x;
		primary.setText(String.valueOf(x));
		secondary.setText(String.format("%02d", y));
        if (isIncomeExpenseEnabled() && amount != 0) {
            if (amount > 0) {
                setIncome();
            } else {
                setExpense();
            }
        }
	}

	public long getAmount() {
		String p = primary.getText().toString();
		String s = secondary.getText().toString();
		long x = 100 * toLong(p);
		long y = toLong(s);
		long amount = x + (s.length() == 1 ? 10 * y : y);
        return isExpense() ? -amount : amount;
	}

	private String getAbsAmountString() {
		String p = primary.getText().toString().trim();
		String s = secondary.getText().toString().trim();
        return (Utils.isNotEmpty(p) ? p : "0") + "."
                + (Utils.isNotEmpty(s) ? s : "0");
	}

	private long toLong(String s) {
		return s == null || s.length() == 0 ? 0 : Long.parseLong(s);
	}

	public void setColor(int color) {
		primary.setTextColor(color);
		secondary.setTextColor(color);
	}

    public void openCalculator() {
        startInputActivity(CalculatorInput.class);
    }

}
