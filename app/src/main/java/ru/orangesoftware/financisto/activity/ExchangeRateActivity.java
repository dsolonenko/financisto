/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.rates.ExchangeRate;
import ru.orangesoftware.financisto.datetime.DateUtils;
import ru.orangesoftware.financisto.widget.AmountInput;
import ru.orangesoftware.financisto.widget.RateNode;
import ru.orangesoftware.financisto.widget.RateNodeOwner;

import java.util.Calendar;

import static ru.orangesoftware.financisto.utils.Utils.formatRateDate;

public class ExchangeRateActivity extends AbstractActivity implements RateNodeOwner {

    public static final String RATE_DATE = "RATE_DATE";
    public static final String TO_CURRENCY_ID = "TO_CURRENCY_ID";
    public static final String FROM_CURRENCY_ID = "FROM_CURRENCY_ID";

    private Currency fromCurrency;
    private Currency toCurrency;
    private long originalDate;
    private long date;
    private double rate = 1;

    private TextView dateNode;
    private RateNode rateNode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.exchange_rate);

        Intent intent = getIntent();
        if (intent == null) {
            finish();
            return;
        }

        Button bOK = findViewById(R.id.bOK);
        bOK.setOnClickListener(arg0 -> {
            ExchangeRate rate = createRateFromUI();
            db.replaceRate(rate, originalDate);
            Intent data = new Intent();
            setResult(RESULT_OK, data);
            finish();
        });

        Button bCancel = findViewById(R.id.bCancel);
        bCancel.setOnClickListener(arg0 -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        if (validateIntent(intent)) {
            LinearLayout layout = findViewById(R.id.list);
            updateUI(layout);
        } else {
            finish();
        }
    }

    private ExchangeRate createRateFromUI() {
        ExchangeRate rate = new ExchangeRate();
        rate.fromCurrencyId = fromCurrency.id;
        rate.toCurrencyId = toCurrency.id;
        rate.date = date;
        rate.rate = rateNode.getRate();
        return rate;
    }

    private void updateUI(LinearLayout layout) {
        x.addInfoNode(layout, 0, R.string.rate_from_currency, fromCurrency.name);
        x.addInfoNode(layout, 0, R.string.rate_to_currency, toCurrency.name);
        dateNode = x.addInfoNode(layout, R.id.date, R.string.date, formatRateDate(this, date));
        rateNode = new RateNode(this, x, layout);
        rateNode.setRate(rate);
        rateNode.updateRateInfo();
    }

    private boolean validateIntent(Intent intent) {
        long fromCurrencyId = intent.getLongExtra(FROM_CURRENCY_ID, -1);
        fromCurrency = db.get(Currency.class, fromCurrencyId);
        if (fromCurrency == null) {
            finish();
            return false;
        }

        long toCurrencyId = intent.getLongExtra(TO_CURRENCY_ID, -1);
        toCurrency = db.get(Currency.class, toCurrencyId);
        if (toCurrency == null) {
            finish();
            return false;
        }

        long date = intent.getLongExtra(RATE_DATE, -1);
        if (date == -1) {
            date = DateUtils.atMidnight(System.currentTimeMillis());
        }
        this.originalDate = this.date = date;

        ExchangeRate rate = db.findRate(fromCurrency, toCurrency, date);
        if (rate != null) {
            this.rate = rate.rate;
        }

        return true;
    }

    @Override
    protected void onClick(View v, int id) {
        switch (id) {
            case R.id.date:
                editDate();
                break;
        }
    }

    private void editDate() {
        final Calendar c = Calendar.getInstance();
        c.setTimeInMillis(date);
        DatePickerDialog datePickerDialog = DatePickerDialog.newInstance(
                (view, year, monthOfYear, dayOfMonth) -> {
                    c.set(year, monthOfYear, dayOfMonth);
                    date = c.getTimeInMillis();
                    dateNode.setText(formatRateDate(ExchangeRateActivity.this, date));
                },
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show(getFragmentManager(), "DatePickerDialog");
    }

    @Override
    public void onBeforeRateDownload() {
        rateNode.disableAll();
    }

    @Override
    public void onAfterRateDownload() {
        rateNode.enableAll();
    }

    @Override
    public void onSuccessfulRateDownload() {
        rateNode.updateRateInfo();
    }

    @Override
    public void onRateChanged() {
        rateNode.updateRateInfo();
    }

    @Override
    public Activity getActivity() {
        return this;
    }

    @Override
    public Currency getCurrencyFrom() {
        return fromCurrency;
    }

    @Override
    public Currency getCurrencyTo() {
        return toCurrency;
    }

}
