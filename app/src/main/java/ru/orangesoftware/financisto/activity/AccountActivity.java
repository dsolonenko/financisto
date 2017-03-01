/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 *     Abdsandryk - adding bill filtering parameters
 ******************************************************************************/
package ru.orangesoftware.financisto.activity;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.adapter.EntityEnumAdapter;
import ru.orangesoftware.financisto.model.*;
import ru.orangesoftware.financisto.utils.TransactionUtils;
import ru.orangesoftware.financisto.utils.Utils;
import ru.orangesoftware.financisto.widget.AmountInput;

import static ru.orangesoftware.financisto.utils.Utils.text;

public class AccountActivity extends AbstractActivity {
	
	public static final String ACCOUNT_ID_EXTRA = "accountId";
	
	private static final int NEW_CURRENCY_REQUEST = 1;
	
	private AmountInput amountInput;
	private AmountInput limitInput;
	private View limitAmountView;
	private EditText accountTitle;

	private Cursor currencyCursor;
	private TextView currencyText;
	private View accountTypeNode;
	private View cardIssuerNode;
	private View issuerNode;
	private EditText numberText;
	private View numberNode;
	private EditText issuerName;
	private EditText sortOrderText;
	private CheckBox isIncludedIntoTotals;
    private EditText noteText;
    private EditText closingDayText;
    private EditText paymentDayText;
    private View closingDayNode;
    private View paymentDayNode;

	private EntityEnumAdapter<AccountType> accountTypeAdapter;
	private EntityEnumAdapter<CardIssuer> cardIssuerAdapter;
	private ListAdapter currencyAdapter;	

	private Account account = new Account();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.account);
		
		accountTitle = new EditText(this);
		accountTitle.setSingleLine();
		
		issuerName = new EditText(this);
		issuerName.setSingleLine();

		numberText = new EditText(this);
		numberText.setHint(R.string.card_number_hint);
		numberText.setSingleLine();

		sortOrderText = new EditText(this);
		sortOrderText.setInputType(InputType.TYPE_CLASS_NUMBER);
        sortOrderText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(3)});
		sortOrderText.setSingleLine();
		
		closingDayText = new EditText(this);
		closingDayText.setInputType(InputType.TYPE_CLASS_NUMBER);
		closingDayText.setHint(R.string.closing_day_hint);
		closingDayText.setSingleLine();
		
		paymentDayText = new EditText(this);
		paymentDayText.setInputType(InputType.TYPE_CLASS_NUMBER);
		paymentDayText.setHint(R.string.payment_day_hint);
		paymentDayText.setSingleLine();

		amountInput = new AmountInput(this);
		amountInput.setOwner(this);

		limitInput = new AmountInput(this);
		limitInput.setOwner(this);

		LinearLayout layout = (LinearLayout)findViewById(R.id.layout);		

		accountTypeAdapter = new EntityEnumAdapter<AccountType>(this, AccountType.values());
		accountTypeNode = x.addListNodeIcon(layout, R.id.account_type, R.string.account_type, R.string.account_type);
		
		cardIssuerAdapter = new EntityEnumAdapter<CardIssuer>(this, CardIssuer.values());
		cardIssuerNode = x.addListNodeIcon(layout, R.id.card_issuer, R.string.card_issuer, R.string.card_issuer);
		setVisibility(cardIssuerNode, View.GONE);
		
		issuerNode = x.addEditNode(layout, R.string.issuer, issuerName);
		setVisibility(issuerNode, View.GONE);
		
		numberNode = x.addEditNode(layout, R.string.card_number, numberText);
		setVisibility(numberNode, View.GONE);
		
		closingDayNode = x.addEditNode(layout, R.string.closing_day, closingDayText);
		setVisibility(closingDayNode, View.GONE);
		
		paymentDayNode = x.addEditNode(layout, R.string.payment_day, paymentDayText);
		setVisibility(paymentDayNode, View.GONE);		

		currencyCursor = em.getAllCurrencies("name");
		startManagingCursor(currencyCursor);		
		currencyAdapter = TransactionUtils.createCurrencyAdapter(this, currencyCursor);

		x.addEditNode(layout, R.string.title, accountTitle);		
		currencyText = x.addListNodePlus(layout, R.id.currency, R.id.currency_add, R.string.currency, R.string.select_currency);
		
		limitInput.setExpense();
		limitInput.disableIncomeExpenseButton();
		limitAmountView = x.addEditNode(layout, R.string.limit_amount, limitInput);
		setVisibility(limitAmountView, View.GONE);

		Intent intent = getIntent();
		if (intent != null) {
			long accountId = intent.getLongExtra(ACCOUNT_ID_EXTRA, -1);
			if (accountId != -1) {
				this.account = em.getAccount(accountId);
				if (this.account == null) {
					this.account = new Account();
				}
			} else {
				selectAccountType(AccountType.valueOf(account.type));
			}
		}

		if (account.id == -1) {
			x.addEditNode(layout, R.string.opening_amount, amountInput);
            amountInput.setIncome();
		}

        noteText = new EditText(this);
        noteText.setLines(2);
        x.addEditNode(layout, R.string.note, noteText);

		x.addEditNode(layout, R.string.sort_order, sortOrderText);
		isIncludedIntoTotals = x.addCheckboxNode(layout,
				R.id.is_included_into_totals, R.string.is_included_into_totals,
				R.string.is_included_into_totals_summary, true);
		
		if (account.id > 0) {
			editAccount();
		}

		Button bOK = (Button)findViewById(R.id.bOK);
		bOK.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				if (account.currency == null) {
					Toast.makeText(AccountActivity.this, R.string.select_currency, Toast.LENGTH_SHORT).show();
					return;	
				}
				if (Utils.isEmpty(accountTitle)) {
					accountTitle.setError(getString(R.string.title));
					return;
				}
				AccountType type = AccountType.valueOf(account.type);
				if (type.hasIssuer) {
					account.issuer = Utils.text(issuerName);
				}
				if (type.hasNumber) {
					account.number = Utils.text(numberText);
				}
				
				/********** validate closing and payment days **********/
				if (type.isCreditCard) {
					String closingDay = Utils.text(closingDayText);
					account.closingDay = closingDay == null ? 0 : Integer.parseInt(closingDay);
					if (account.closingDay != 0) {
						if (account.closingDay>31) {
							Toast.makeText(AccountActivity.this, R.string.closing_day_error, Toast.LENGTH_SHORT).show();
							return;	
						}	
					}
					
					String paymentDay = Utils.text(paymentDayText);
					account.paymentDay = paymentDay == null ? 0 : Integer.parseInt(paymentDay);	
					if (account.paymentDay != 0) {
						if (account.paymentDay>31) {
							Toast.makeText(AccountActivity.this, R.string.payment_day_error, Toast.LENGTH_SHORT).show();
							return;	
						}
					}
				}	

				account.title = text(accountTitle);
				account.creationDate = System.currentTimeMillis();
				String sortOrder = text(sortOrderText);
				account.sortOrder = sortOrder == null ? 0 : Integer.parseInt(sortOrder);
				account.isIncludeIntoTotals  = isIncludedIntoTotals.isChecked();
				account.limitAmount = -Math.abs(limitInput.getAmount());
                account.note = text(noteText);
				
				long accountId = em.saveAccount(account);
				long amount = amountInput.getAmount();
				if (amount != 0) {
					Transaction t = new Transaction();
					t.fromAccountId = accountId;
					t.categoryId = 0;
					t.note = getResources().getText(R.string.opening_amount) + " (" +account.title + ")";
					t.fromAmount = amount;
					db.insertOrUpdate(t, null);
				}
				Intent intent = new Intent();
				intent.putExtra(ACCOUNT_ID_EXTRA, accountId);
				setResult(RESULT_OK, intent);
				finish();
			}

		});

		Button bCancel = (Button)findViewById(R.id.bCancel);
		bCancel.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				setResult(RESULT_CANCELED);
				finish();
			}			
		});
		
	}

	@Override
	protected void onClick(View v, int id) {
		switch(id) {
			case R.id.is_included_into_totals:
				isIncludedIntoTotals.performClick();
				break;
			case R.id.account_type:
				x.selectPosition(this, R.id.account_type, R.string.account_type, accountTypeAdapter, AccountType.valueOf(account.type).ordinal());
				break;
			case R.id.card_issuer:				
				x.selectPosition(this, R.id.card_issuer, R.string.card_issuer, cardIssuerAdapter, 
						account.cardIssuer != null ? CardIssuer.valueOf(account.cardIssuer).ordinal() : 0);
				break;
			case R.id.currency:				
				x.select(this, R.id.currency, R.string.currency, currencyCursor, currencyAdapter, 
						"_id", account.currency != null ? account.currency.id : -1);
				break;
			case R.id.currency_add:
                addNewCurrency();
				break;
		}
	}

    private void addNewCurrency() {
        new CurrencySelector(this, em, new CurrencySelector.OnCurrencyCreatedListener() {
            @Override
            public void onCreated(long currencyId) {
                if (currencyId == 0) {
                    Intent intent = new Intent(AccountActivity.this, CurrencyActivity.class);
                    startActivityForResult(intent, NEW_CURRENCY_REQUEST);
                } else {
                    currencyCursor.requery();
                    selectCurrency(currencyId);
                }
            }
        }).show();
    }

    @Override
	public void onSelectedId(int id, long selectedId) {
		switch(id) {
			case R.id.currency:
				selectCurrency(selectedId);
				break;
		}
	}

	@Override
	public void onSelectedPos(int id, int selectedPos) {
		switch(id) {
			case R.id.account_type:
				AccountType type = AccountType.values()[selectedPos];
				selectAccountType(type);
				break;
			case R.id.card_issuer:
				CardIssuer issuer = CardIssuer.values()[selectedPos];
				selectCardIssuer(issuer);
				break;
		}
	}

	private void selectAccountType(AccountType type) {
		ImageView icon = (ImageView)accountTypeNode.findViewById(R.id.icon);
		icon.setImageResource(type.iconId);
		TextView label = (TextView)accountTypeNode.findViewById(R.id.label);
		label.setText(type.titleId);

		setVisibility(cardIssuerNode, type.isCard ? View.VISIBLE : View.GONE);
		setVisibility(issuerNode, type.hasIssuer ? View.VISIBLE : View.GONE);
		setVisibility(numberNode, type.hasNumber ? View.VISIBLE : View.GONE);
		setVisibility(closingDayNode, type.isCreditCard ? View.VISIBLE : View.GONE);
		setVisibility(paymentDayNode, type.isCreditCard ? View.VISIBLE : View.GONE);

		setVisibility(limitAmountView, type == AccountType.CREDIT_CARD ? View.VISIBLE : View.GONE);
		account.type = type.name();
		selectCardIssuer(account.cardIssuer != null 
				? CardIssuer.valueOf(account.cardIssuer)
				: CardIssuer.VISA);
	}

	private void selectCardIssuer(CardIssuer issuer) {
		ImageView icon = (ImageView)cardIssuerNode.findViewById(R.id.icon);
		icon.setImageResource(issuer.iconId);
		TextView label = (TextView)cardIssuerNode.findViewById(R.id.label);
		label.setText(issuer.titleId);
		account.cardIssuer = issuer.name();
	}

	private void selectCurrency(long currencyId) {
        Currency c = em.get(Currency.class, currencyId);
		if (c != null) {
			selectCurrency(c);
		}
	}
	
	private void selectCurrency(Currency c) {
		currencyText.setText(c.name);
		amountInput.setCurrency(c);
		limitInput.setCurrency(c);
		account.currency = c;		
	}

	private void editAccount() {
		selectAccountType(AccountType.valueOf(account.type));
		if (account.cardIssuer != null) {
			selectCardIssuer(CardIssuer.valueOf(account.cardIssuer));
		}
		selectCurrency(account.currency);
		accountTitle.setText(account.title);
		issuerName.setText(account.issuer);
		numberText.setText(account.number);
		sortOrderText.setText(String.valueOf(account.sortOrder));
		
		/******** bill filtering ********/
		if (account.closingDay>0) {
			closingDayText.setText(String.valueOf(account.closingDay));
		} 
		if (account.paymentDay>0) {
			paymentDayText.setText(String.valueOf(account.paymentDay));
		}
		/********************************/		
		
		isIncludedIntoTotals.setChecked(account.isIncludeIntoTotals);
		if (account.limitAmount != 0) {
			limitInput.setAmount(-Math.abs(account.limitAmount));
		}
        noteText.setText(account.note);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			if (amountInput.processActivityResult(requestCode, data)) {
				return;
			}
			if (limitInput.processActivityResult(requestCode, data)) {
				return;
			}
			switch(requestCode) {
			case NEW_CURRENCY_REQUEST:
				currencyCursor.requery();
				long currencyId = data.getLongExtra(CurrencyActivity.CURRENCY_ID_EXTRA, -1);
				if (currencyId != -1) {
					selectCurrency(currencyId);
				}
				break;
			}
		}
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}	
		
}
