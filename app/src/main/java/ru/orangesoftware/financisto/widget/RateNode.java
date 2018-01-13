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
import android.os.AsyncTask;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.activity.ActivityLayout;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.rates.ExchangeRate;
import ru.orangesoftware.financisto.rates.ExchangeRateProvider;
import ru.orangesoftware.financisto.utils.MyPreferences;
import ru.orangesoftware.financisto.utils.Utils;

public class RateNode {

    public static final int EDIT_RATE = 112;

    private final DecimalFormat nf = new DecimalFormat("0.00000");

    private final RateNodeOwner owner;
    private final ActivityLayout x;
    private final LinearLayout layout;

    View rateInfoNode;

    private TextView rateInfo;
    private EditText rate;

    private ImageButton bCalc;
    private ImageButton bDownload;

    public RateNode(RateNodeOwner owner, ActivityLayout x, LinearLayout layout) {
        this.owner = owner;
        this.x = x;
        this.layout = layout;
        createUI();
    }

    private void createUI() {
        rateInfoNode = x.addRateNode(layout);
        rate = rateInfoNode.findViewById(R.id.rate);
        rate.addTextChangedListener(rateWatcher);
        rate.setOnFocusChangeListener((view, b) -> {
            if (b) {
                rate.selectAll();
            }
        });
        rateInfo = rateInfoNode.findViewById(R.id.data);
        bCalc = rateInfoNode.findViewById(R.id.rateCalculator);
        bCalc.setOnClickListener(v -> {
            Activity activity = owner.getActivity();

            CalculatorInput input = CalculatorInput_.builder().amount(String.valueOf(getRate())).build();
            input.setListener(amount -> {
                try {
                    setRate(Float.parseFloat(amount));
                    updateRateInfo();
                    owner.onRateChanged();
                } catch (NumberFormatException ignored) {
                }
            });
            input.show(activity.getFragmentManager(), "calculator");
        });
        bDownload = rateInfoNode.findViewById(R.id.rateDownload);
        bDownload.setOnClickListener(v -> new RateDownloadTask().execute());
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
            sb.append("1").append(currencyTo.name).append("=").append(nf.format(1.0 / r)).append(currencyFrom.name);
        }
        rateInfo.setText(sb.toString());
    }

    private class RateDownloadTask extends AsyncTask<Void, Void, ExchangeRate> {

        private ProgressDialog progressDialog;

        @Override
        protected ExchangeRate doInBackground(Void... args) {
            Currency fromCurrency = owner.getCurrencyFrom();
            Currency toCurrency = owner.getCurrencyTo();
            if (fromCurrency != null && toCurrency != null) {
                return getProvider().getRate(fromCurrency, toCurrency);
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
            progressDialog = ProgressDialog.show(context, null, message, true, true, dialogInterface -> cancel(true));
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

    private final TextWatcher rateWatcher = new TextWatcher() {
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
