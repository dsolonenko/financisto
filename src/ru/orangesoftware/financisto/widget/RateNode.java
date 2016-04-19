/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.widget;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.activity.ActivityLayout;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.rates.ExchangeRate;
import ru.orangesoftware.financisto.rates.ExchangeRateProvider;
import ru.orangesoftware.financisto.utils.MyPreferences;
import ru.orangesoftware.financisto.utils.Utils;

import java.text.DecimalFormat;

import static ru.orangesoftware.financisto.utils.NetworkUtils.isOnline;

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 1/19/12 11:24 PM
 */
public class RateNode {

    public static final int EDIT_RATE = 112;

    private final DecimalFormat nf = new DecimalFormat("0.00000");

    private final RateNodeOwner owner;
    private final ActivityLayout x;
    private final LinearLayout layout;
    private final DatabaseAdapter db;

    public View rateInfoNode;
    public TextView rateInfo;
    public EditText rate;
    public ImageButton bCalc;

    public ImageButton bDownload;

    public RateNode(RateNodeOwner owner, ActivityLayout x, LinearLayout layout) {
        this.owner = owner;
        this.x = x;
        this.layout = layout;
        this.db = new DatabaseAdapter(owner.getActivity());
        createUI();
    }

    private void createUI() {
        rateInfoNode = x.addRateNode(layout);
        rate = (EditText)rateInfoNode.findViewById(R.id.rate);
        rate.addTextChangedListener(rateWatcher);
        rate.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (b) {
                    rate.selectAll();
                }
            }
        });
        rateInfo = (TextView)rateInfoNode.findViewById(R.id.data);
        bCalc = (ImageButton)rateInfoNode.findViewById(R.id.rateCalculator);
        bCalc.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Activity activity = owner.getActivity();
                Intent intent = new Intent(activity, CalculatorInput.class);
                intent.putExtra(AmountInput.EXTRA_AMOUNT, String.valueOf(getRate()));
                activity.startActivityForResult(intent, EDIT_RATE);
            }
        });
        bDownload = (ImageButton)rateInfoNode.findViewById(R.id.rateDownload);
        bDownload.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                new RateDownloadTask().execute();
            }
        });
    }

    public void disableAll() {
        rate.setEnabled(false);
        bCalc.setEnabled(false);
        bDownload.setEnabled(false);
    }

    public void enableAll() {
        rate.setEnabled(true);
        bCalc.setEnabled(true);
        bDownload.setEnabled(true);
    }

    public float getRate() {
        try {
            String rateText = Utils.text(rate);
            if (rateText != null) {
                rateText = rateText.replace(',', '.');
                return Float.parseFloat(rateText);
            }
            return 0;
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    public void setRate(double r) {
        rate.removeTextChangedListener(rateWatcher);
        rate.setText(nf.format(Math.abs(r)));
        rate.addTextChangedListener(rateWatcher);
    }

    public void updateRateInfo() {
        double r = getRate();
        StringBuilder sb = new StringBuilder();
        Currency currencyFrom = owner.getCurrencyFrom();
        Currency currencyTo = owner.getCurrencyTo();
        if (currencyFrom != null && currencyTo != null) {
            sb.append("1").append(currencyFrom.name).append("=").append(nf.format(r)).append(currencyTo.name).append(", ");
            sb.append("1").append(currencyTo.name).append("=").append(nf.format(1.0/r)).append(currencyFrom.name);
        }
        rateInfo.setText(sb.toString());
    }

    private class RateDownloadTask extends AsyncTask<Void, Void, ExchangeRate> {

        private ProgressDialog progressDialog;
        private volatile boolean isOfflineRate = false;

        @Override
        protected ExchangeRate doInBackground(Void... args) {
            Currency fromCurrency = owner.getCurrencyFrom();
            Currency toCurrency = owner.getCurrencyTo();
            if (fromCurrency != null && toCurrency != null) {
                if (isOnline(owner.getActivity())) {
                    isOfflineRate = false;
                    return getProvider().getRate(fromCurrency, toCurrency);
                } else {
                    isOfflineRate = true;
                    return db.getLatestRates().getRate(fromCurrency, toCurrency);
                }
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            showProgressDialog();
            owner.onBeforeRateDownload();
        }

        private void showProgressDialog() {
            Context context = owner.getActivity();
            String message = context.getString(R.string.downloading_rate, owner.getCurrencyFrom(), owner.getCurrencyTo());
            progressDialog = ProgressDialog.show(context, null, message, true, true, new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    cancel(true);
                }
            });
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            owner.onAfterRateDownload();
        }

        @Override
        protected void onPostExecute(ExchangeRate result) {
            progressDialog.dismiss();
            owner.onAfterRateDownload();
            if (result != null) {
                if (result.isOk()) {
                    if (isOfflineRate) {
                        Toast.makeText(owner.getActivity(), R.string.offline_rate, Toast.LENGTH_LONG).show();
                    }
                    setRate(result.rate);
                    owner.onSuccessfulRateDownload();
                } else {
                    Toast.makeText(owner.getActivity(), result.getErrorMessage(), Toast.LENGTH_LONG).show();
                }
            }
        }

        private ExchangeRateProvider getProvider() {
            return MyPreferences.createExchangeRatesProvider(owner.getActivity());
        }

    }

    private final TextWatcher rateWatcher = new TextWatcher(){
        @Override
        public void afterTextChanged(Editable s) {
            owner.onRateChanged();
        }
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    };

}
